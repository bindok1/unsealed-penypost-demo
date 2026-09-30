package com.apps.unsealed.feature.auth.data

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.apps.unsealed.BuildConfig
import com.apps.unsealed.R
import com.apps.unsealed.core.data.ComposeDraftHolder
import com.apps.unsealed.core.network.isNetworkOrConnectionIssue
import com.apps.unsealed.core.network.toUserFacingMessage
import com.apps.unsealed.core.network.unwrap
import com.apps.unsealed.feature.bookmarks.data.BookmarksRepository
import com.apps.unsealed.feature.draft.data.DraftRepository
import coil3.imageLoader
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : AuthResult<Nothing>()
}

/**
 * Whether this user profile still needs the onboarding flow.
 * Onboarding is considered complete when:
 * - TOS has been accepted ([UserDto.tosAcceptedAt] non-null)
 * - gender and birthday are set
 * - at least one interest is chosen
 *
 * Existing real users whose `tos_accepted_at` is NULL (migration only
 * backfilled is_seed=true rows) are re-routed to onboarding to accept TOS
 * before they can send letters or match penpals again.
 */
fun UserDto.needsOnboarding(): Boolean =
    tosAcceptedAt == null || gender == null || birthday == null || interests.isEmpty()

@Singleton
class AuthRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val crashlytics: FirebaseCrashlytics,
    private val retrofit: Retrofit,
    private val draftRepository: DraftRepository,
    private val bookmarksRepository: BookmarksRepository,
    private val composeDraftHolder: ComposeDraftHolder,
) {

    private val api: AuthApi by lazy { retrofit.create(AuthApi::class.java) }

    val currentUser: FirebaseUser? get() = auth.currentUser

    /**
     * Triggers Google Sign-In via Credential Manager and signs into Firebase.
     * Must be called from an Activity context (required by CredentialManager).
     */
    suspend fun signInWithGoogle(context: Context): AuthResult<FirebaseUser> = runCatching {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            // Show all accounts, not just previously used ones
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context = context, request = request)
        val credential = result.credential

        check(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) { "Unexpected credential type: ${credential.type}" }

        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(
            googleIdTokenCredential.idToken,
            /* idpToken = */ null,
        )
        val authResult = auth.signInWithCredential(firebaseCredential).await()
        authResult.user ?: error("Firebase sign-in succeeded but user is null")
    }.fold(
        onSuccess = { user ->
            crashlytics.setUserId(user.uid)
            AuthResult.Success(user)
        },
        onFailure = { throwable ->
            when {
                throwable.isNetworkOrConnectionIssue() -> {
                    Log.w(TAG, "Google sign-in Firebase network/handshake issue: ${throwable.message}")
                    AuthResult.Error(context.getString(R.string.error_network), throwable)
                }
                throwable is GetCredentialCancellationException -> {
                    Log.d(TAG, "User cancelled Google sign-in flow")
                    AuthResult.Error(context.getString(R.string.error_auth_signin), throwable)
                }
                else -> {
                    Log.w(TAG, "Google sign-in unexpected failure: ${throwable.message}", throwable)
                    crashlytics.recordException(throwable)
                    AuthResult.Error(throwable.toUserFacingMessage(context, R.string.error_auth_signin), throwable)
                }
            }
        },
    )

    /** Creates a brand-new Firebase account from [email]/[password] — the
     *  Email/Password counterpart to [signInWithGoogle]. Firebase requires
     *  passwords to be at least 6 characters (surfaced as
     *  [FirebaseAuthWeakPasswordException] otherwise). Like Google sign-in,
     *  this only creates the *Firebase* identity — the backend profile
     *  (nickname/continent) is still collected by `RegisterScreen` afterward,
     *  same as any other provider (`AuthViewModel.checkAuthState` routes to
     *  [com.apps.unsealed.feature.auth.AuthUiState.NeedsRegister] regardless
     *  of how the Firebase user was created). */
    suspend fun signUpWithEmail(email: String, password: String): AuthResult<FirebaseUser> = runCatching {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        authResult.user ?: error("Firebase sign-up succeeded but user is null")
    }.fold(
        onSuccess = { user ->
            crashlytics.setUserId(user.uid)
            AuthResult.Success(user)
        },
        onFailure = { throwable ->
            if (throwable.isNetworkOrConnectionIssue()) {
                Log.w(TAG, "Email sign-up Firebase network/handshake issue: ${throwable.message}")
            } else {
                Log.w(TAG, "Email sign-up failure: ${throwable.message}", throwable)
                crashlytics.recordException(throwable)
            }
            AuthResult.Error(throwable.toEmailAuthUserFacingMessage(context), throwable)
        },
    )

    /** Signs into an existing Firebase account with [email]/[password]. */
    suspend fun signInWithEmail(email: String, password: String): AuthResult<FirebaseUser> = runCatching {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        authResult.user ?: error("Firebase sign-in succeeded but user is null")
    }.fold(
        onSuccess = { user ->
            crashlytics.setUserId(user.uid)
            AuthResult.Success(user)
        },
        onFailure = { throwable ->
            if (throwable.isNetworkOrConnectionIssue()) {
                Log.w(TAG, "Email sign-in Firebase network/handshake issue: ${throwable.message}")
            } else {
                Log.w(TAG, "Email sign-in failure: ${throwable.message}", throwable)
                crashlytics.recordException(throwable)
            }
            AuthResult.Error(throwable.toEmailAuthUserFacingMessage(context), throwable)
        },
    )

    /** Sends a Firebase-hosted password-reset email — purely client-SDK,
     *  no backend endpoint involved (the backend doesn't store email at
     *  all, see `docs/be/profile_api.md`). Firebase doesn't reveal whether
     *  the address is registered (enumeration protection), so this
     *  succeeds even for unknown emails; only malformed addresses error. */
    suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }.fold(
        onSuccess = { AuthResult.Success(Unit) },
        onFailure = { throwable ->
            if (throwable.isNetworkOrConnectionIssue()) {
                Log.w(TAG, "Password reset Firebase network/handshake issue: ${throwable.message}")
            } else {
                Log.w(TAG, "Password reset failure: ${throwable.message}", throwable)
                crashlytics.recordException(throwable)
            }
            AuthResult.Error(throwable.toEmailAuthUserFacingMessage(context), throwable)
        },
    )

    /**
     * Checks if the current Firebase user has a backend profile.
     * Returns `null` inside [AuthResult.Success] if the user has never registered
     * (backend returns 404). Returns [AuthResult.Error] on network/server errors.
     *
     * 401 handling: [TokenRefreshAuthenticator] will try to force-refresh the
     * Firebase token and retry the request automatically. If the backend still
     * returns 401 after retry (token revoked / user deleted), we sign out from
     * Firebase silently — [AuthViewModel.checkAuthState] will then see
     * `currentUser == null` and transition to [AuthUiState.Unauthenticated]
     * without showing an error to the user.
     */
    suspend fun fetchMe(): AuthResult<UserDto?> = runCatching {
        val response = api.getMe()
        when {
            response.isSuccessful -> response.body()?.data
            response.code() == 404 -> null // brand-new user, needs /register
            response.code() == 401 -> {
                // TokenRefreshAuthenticator already tried a force-refresh and failed.
                // Session is truly invalid — sign out silently so the next
                // currentUser check in checkAuthState() returns null.
                signOut()
                error("Session expired — signed out silently")
            }
            else -> error("GET /auth/me failed: HTTP ${response.code()}")
        }
    }.fold(
        onSuccess = { user ->
            user?.id?.let { crashlytics.setUserId(it) }
            AuthResult.Success(user)
        },
        onFailure = {
            crashlytics.recordException(it)
            AuthResult.Error(it.toUserFacingMessage(context, R.string.error_auth_profile), it)
        },
    )

    suspend fun register(
        nickname: String,
        continent: String,
        utcOffsetMinutes: Int? = null,
    ): AuthResult<UserDto> =
        runCatching {
            api.register(
                RegisterRequest(
                    nickname = nickname,
                    continent = continent,
                    utcOffsetMinutes = utcOffsetMinutes,
                ),
            ).unwrap()
        }.fold(
            onSuccess = { user ->
                crashlytics.setUserId(user.id)
                AuthResult.Success(user)
            },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_auth_register), it)
            },
        )

    suspend fun patchMe(request: PatchMeRequest): AuthResult<UserDto> =
        runCatching {
            api.patchMe(request).unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_auth_update_profile), it)
            },
        )

    suspend fun putInterests(ids: List<String>): AuthResult<UserDto> =
        runCatching {
            api.putInterests(InterestsRequest(interestIds = ids)).unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_auth_update_interests), it)
            },
        )

    suspend fun getInterests(): AuthResult<List<InterestDto>> =
        runCatching {
            api.getInterests().unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_auth_interests), it)
            },
        )

    suspend fun putLanguages(languages: List<LanguageProficiencyDto>): AuthResult<UserDto> =
        runCatching {
            api.putLanguages(LanguagesRequest(languages = languages)).unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_auth_update_languages), it)
            },
        )

    /**
     * Fetches the public profile for any user by ID.
     *
     * Returns `null` inside [AuthResult.Success] when the server responds with
     * 404 — this covers both "user does not exist" and "a bidirectional block
     * exists between the viewer and the target". The two cases are deliberately
     * indistinguishable (same behaviour as [fetchMe] for NeedsRegister).
     */
    suspend fun fetchPublicProfile(userId: String): AuthResult<PublicProfileDto?> =
        runCatching {
            val response = api.getPublicProfile(userId)
            when {
                response.isSuccessful -> response.body()?.data
                response.code() == 404 -> null
                else -> error("GET /users/$userId failed: HTTP ${response.code()}")
            }
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_generic), it)
            },
        )

    fun signOut() {
        auth.signOut()
        crashlytics.setUserId("")
    }

    /**
     * User-initiated "Log Out" (Settings) only — [signOut] alone is also used
     * internally by [fetchMe]'s silent 401 recovery path, where a token
     * merely expired/got revoked but the same person is likely still sitting
     * at the device; wiping their drafts there would be a surprising data
     * loss unrelated to anything they did. This full variant additionally
     * wipes local-only user data that isn't scoped by `userId` (`drafts`,
     * `bookmarked_letters` — see their Room DAOs' `deleteAll()` doc comments)
     * — otherwise a second account signing in on the same device would
     * inherit the previous user's drafts/bookmarks. [WelcomePreferences] is
     * deliberately left alone: it's a device-level "seen the onboarding
     * carousel" flag, not user data.
     */
    suspend fun signOutAndClearLocalData() {
        signOut()
        draftRepository.clearAll()
        bookmarksRepository.clearAll()
        composeDraftHolder.clear()
        // Caller (AuthViewModel.signOut) awaits this whole function before
        // navigating away, so this blocking disk I/O must not run on
        // viewModelScope's Dispatchers.Main.immediate — deleting a sizeable
        // image cache directly on the main thread froze the UI for a
        // visible moment before the screen switched to Login, reading as
        // "logout didn't do anything".
        withContext(Dispatchers.IO) {
            runCatching {
                context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                context.codeCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                context.imageLoader.diskCache?.clear()
                context.imageLoader.memoryCache?.clear()
            }
        }
    }


    /**
     * Self-service account deletion (DELETE /api/v1/auth/me).
     * Calls backend to soft-delete profile, then caller invokes [signOutAndClearLocalData].
     */
    suspend fun deleteAccount(): AuthResult<Unit> = runCatching {
        val response = api.deleteMe()
        when {
            response.isSuccessful -> Unit
            else -> error("DELETE /auth/me failed: HTTP ${response.code()}")
        }
    }.fold(
        onSuccess = { AuthResult.Success(Unit) },
        onFailure = {
            crashlytics.recordException(it)
            AuthResult.Error(it.toUserFacingMessage(context, R.string.error_generic), it)
        },
    )

    /**
     * Maps Firebase Auth's Email/Password exception types to specific,
     * actionable messages — unlike [toUserFacingMessage] (built for our own
     * backend's HTTP error envelope), these are thrown directly by the
     * Firebase SDK and need their own mapping. Order matters:
     * [FirebaseAuthWeakPasswordException] and [FirebaseAuthUserCollisionException]
     * are subtypes of [FirebaseAuthInvalidCredentialsException] in some SDK
     * versions, so the more specific checks must come first.
     */
    private fun Throwable.toEmailAuthUserFacingMessage(context: Context): String = when {
        this is FirebaseAuthWeakPasswordException -> context.getString(R.string.error_auth_email_weak_password)
        this is FirebaseAuthUserCollisionException -> context.getString(R.string.error_auth_email_already_in_use)
        this is FirebaseAuthInvalidUserException -> context.getString(R.string.error_auth_email_user_not_found)
        this is FirebaseAuthInvalidCredentialsException -> context.getString(R.string.error_auth_email_invalid_credentials)
        this.isNetworkOrConnectionIssue() -> context.getString(R.string.error_network)
        else -> toUserFacingMessage(context, R.string.error_auth_email_generic)
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}

