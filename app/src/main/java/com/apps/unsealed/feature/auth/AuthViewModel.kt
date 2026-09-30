package com.apps.unsealed.feature.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.unsealed.BuildConfig
import com.apps.unsealed.R
import com.apps.unsealed.core.analytics.AnalyticsRepository
import com.apps.unsealed.core.data.InstallReferrerPreferences
import com.apps.unsealed.core.data.WelcomePreferences
import com.apps.unsealed.core.reminder.UnreadReminderScheduler
import com.apps.unsealed.core.review.InAppReviewManager
import com.apps.unsealed.feature.auth.data.AuthRepository
import com.apps.unsealed.feature.auth.data.AuthResult
import com.revenuecat.purchases.Purchases
import com.apps.unsealed.feature.auth.data.InterestDto
import com.apps.unsealed.feature.auth.data.LanguageProficiencyDto
import com.apps.unsealed.feature.auth.data.PatchMeRequest
import com.apps.unsealed.feature.auth.data.UserDto
import com.apps.unsealed.feature.auth.data.needsOnboarding
import com.apps.unsealed.feature.invite.data.InviteRepository
import com.apps.unsealed.feature.letters.data.LettersRepository
import com.apps.unsealed.feature.letters.data.ShowcaseStatus
import com.apps.unsealed.feature.storage.data.StorageRepository
import com.apps.unsealed.ui.screens.profile.state.PostalCollectionUiState
import com.apps.unsealed.ui.screens.profile.state.ShowcaseActionState
import com.apps.unsealed.ui.screens.profile.state.toPostalCollectionItem
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "AuthViewModel"

sealed class AuthUiState {
    /** Initial splash check — do not navigate yet */
    object Loading : AuthUiState()
    /** No Firebase user → show Login screen */
    object Unauthenticated : AuthUiState()
    /** Firebase user exists but no backend record → show Register screen */
    object NeedsRegister : AuthUiState()
    /** Backend profile exists but onboarding incomplete → show Onboarding flow */
    data class NeedsOnboarding(val user: UserDto) : AuthUiState()
    /** Fully authenticated and onboarded → navigate to main app */
    data class Authenticated(val user: UserDto) : AuthUiState()
    /** Any unrecoverable error — show message, fall back to login */
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val analytics: AnalyticsRepository,
    private val welcomePreferences: WelcomePreferences,
    private val storageRepository: StorageRepository,
    private val lettersRepository: LettersRepository,
    private val inviteRepository: InviteRepository,
    private val installReferrerPreferences: InstallReferrerPreferences,
    private val unreadReminderScheduler: UnreadReminderScheduler,
    private val inAppReviewManager: InAppReviewManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Separate from [uiState] so a failed profile-photo upload (onboarding
     *  photo step) shows an inline error instead of kicking the whole
     *  onboarding flow back to a global [AuthUiState.Error] screen. */
    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

    private val _photoUploadError = MutableStateFlow<String?>(null)
    val photoUploadError: StateFlow<String?> = _photoUploadError.asStateFlow()

    /** Interest catalog for onboarding step — loaded lazily */
    private val _interests = MutableStateFlow<List<InterestDto>>(emptyList())
    val interests: StateFlow<List<InterestDto>> = _interests.asStateFlow()

    /** Own profile's "Postal Collection Showcase" — only letters the owner
     * opted public (compose screen's "Show in Postal Collection" toggle),
     * with `showcase_status` still `"visible"`. See `loadPostalCollection`. */
    private val _postalCollection = MutableStateFlow<PostalCollectionUiState>(PostalCollectionUiState.Loading)
    val postalCollection: StateFlow<PostalCollectionUiState> = _postalCollection.asStateFlow()

    /** Feedback for the "···" Pause/Delete menu — separate from
     * [postalCollection] so a failed action doesn't blow away the grid that's
     * already showing (same convention as [ThreadReportState]-style flows). */
    private val _showcaseActionState = MutableStateFlow<ShowcaseActionState>(ShowcaseActionState.Idle)
    val showcaseActionState: StateFlow<ShowcaseActionState> = _showcaseActionState.asStateFlow()

    /** Null while still loading from disk — callers must wait before routing on it. */
    val hasSeenWelcome: StateFlow<Boolean?> = welcomePreferences.hasSeenWelcome
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    /**
     * Cheap synchronous check for guard sites — Firebase caches [FirebaseUser]
     * locally, so this avoids a `fetchMe()` network round-trip just to decide
     * whether a guest needs to see the login prompt.
     */
    val isLoggedIn: Boolean get() = repository.currentUser != null

    fun markWelcomeSeen() {
        viewModelScope.launch { welcomePreferences.markWelcomeSeen() }
    }

    /**
     * Fires Google Play's in-app review bottom sheet at a "moment of delight"
     * — see [InAppReviewManager] for why this is safe to call unconditionally
     * (it's a one-shot no-op after the first attempt) and why there's no
     * "Rate Us" button anywhere calling this directly.
     */
    fun maybeRequestInAppReview(activity: Activity) {
        viewModelScope.launch { inAppReviewManager.maybeRequestReview(activity) }
    }

    init {
        checkAuthState()
    }

    /**
     * Central routing logic. Called on init and whenever the user's
     * authentication or registration state changes.
     */
    fun checkAuthState() {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val firebaseUser = repository.currentUser
            Log.d(TAG, "checkAuthState: firebaseUser=${firebaseUser?.uid ?: "null"}")

            if (firebaseUser == null) {
                Log.d(TAG, "checkAuthState: no Firebase user → Unauthenticated")
                _uiState.value = AuthUiState.Unauthenticated
                return@launch
            }

            Log.d(TAG, "checkAuthState: Firebase user found, calling fetchMe...")
            when (val result = repository.fetchMe()) {
                is AuthResult.Success -> {
                    val user = result.data
                    Log.d(TAG, "checkAuthState: fetchMe success, user=${user?.id ?: "null (NeedsRegister)"}")
                    _uiState.value = when {
                        user == null -> AuthUiState.NeedsRegister
                        user.needsOnboarding() -> {
                            Log.d(TAG, "checkAuthState: needsOnboarding → gender=${user.gender}, birthday=${user.birthday}, interests=${user.interests.size}")
                            AuthUiState.NeedsOnboarding(user)
                        }
                        else -> {
                            Log.d(TAG, "checkAuthState: fully authenticated → main app")
                            analytics.setUserId(user.id)
                            if (BuildConfig.REVENUECAT_API_KEY.isNotBlank() && Purchases.isConfigured) {
                                Purchases.sharedInstance.logIn(user.id)
                            }
                            AuthUiState.Authenticated(user)
                        }
                    }
                    if (_uiState.value is AuthUiState.Authenticated) syncFcmToken()
                }
                is AuthResult.Error -> {
                    Log.e(TAG, "checkAuthState: fetchMe ERROR → message=${result.message}", result.cause)
                    if (repository.currentUser == null) {
                        Log.d(TAG, "checkAuthState: currentUser is null after error (401 sign-out) → Unauthenticated")
                        _uiState.value = AuthUiState.Unauthenticated
                    } else {
                        Log.e(TAG, "checkAuthState: setting Error state")
                        _uiState.value = AuthUiState.Error(result.message)
                    }
                }
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            Log.d(TAG, "signInWithGoogle: starting...")
            when (val result = repository.signInWithGoogle(context)) {
                is AuthResult.Success -> {
                    Log.d(TAG, "signInWithGoogle: success, uid=${result.data.uid}")
                    checkAuthState()
                }
                is AuthResult.Error -> {
                    Log.e(TAG, "signInWithGoogle: ERROR → ${result.message}", result.cause)
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    /**
     * Email/Password sign-up — same downstream routing as [signInWithGoogle]
     * ([checkAuthState] decides Register vs Onboarding vs main app), but
     * errors go to [onError] instead of [uiState] so the email form can show
     * a specific, actionable message (wrong password, weak password, email
     * already in use) right next to the fields rather than a generic
     * full-screen error. On failure, [uiState] reverts to [AuthUiState.Unauthenticated]
     * (not [AuthUiState.Error]) so the Login screen — and the email form on
     * it — stays visible with the fields still filled in.
     */
    fun signUpWithEmail(email: String, password: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = repository.signUpWithEmail(email, password)) {
                is AuthResult.Success -> checkAuthState()
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState.Unauthenticated
                    onError(result.message)
                }
            }
        }
    }

    /** Email/Password sign-in for an existing account — see [signUpWithEmail]. */
    fun signInWithEmail(email: String, password: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = repository.signInWithEmail(email, password)) {
                is AuthResult.Success -> checkAuthState()
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState.Unauthenticated
                    onError(result.message)
                }
            }
        }
    }

    /** Fire-and-report password-reset email — doesn't touch [uiState] at all,
     *  the Login screen shows the outcome as inline text next to the "Forgot
     *  password?" link instead. */
    fun sendPasswordResetEmail(email: String, onResult: (success: Boolean, message: String?) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.sendPasswordResetEmail(email)) {
                is AuthResult.Success -> onResult(true, null)
                is AuthResult.Error -> onResult(false, result.message)
            }
        }
    }

    fun register(nickname: String, continent: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val utcOffsetMinutes = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000
            when (val result = repository.register(nickname, continent, utcOffsetMinutes)) {
                is AuthResult.Success -> {
                    val user = result.data
                    _uiState.value = if (user.needsOnboarding()) {
                        AuthUiState.NeedsOnboarding(user)
                    } else {
                        analytics.setUserId(user.id)
                        AuthUiState.Authenticated(user)
                    }
                    if (_uiState.value is AuthUiState.Authenticated) syncFcmToken()
                    redeemPendingReferralInviteIfAny()
                }
                is AuthResult.Error -> _uiState.value = AuthUiState.Error(result.message)
            }
        }
    }

    /** Partial profile patch used across onboarding steps and profile edits. [onSuccess]
     *  is called with the updated user if the patch succeeds. */
    fun patchMe(request: PatchMeRequest, onSuccess: ((UserDto) -> Unit)? = null) {
        viewModelScope.launch {
            when (val result = repository.patchMe(request)) {
                is AuthResult.Success -> {
                    val updatedUser = result.data
                    _uiState.value = if (updatedUser.needsOnboarding()) {
                        AuthUiState.NeedsOnboarding(updatedUser)
                    } else {
                        AuthUiState.Authenticated(updatedUser)
                    }
                    onSuccess?.invoke(updatedUser)
                }
                is AuthResult.Error -> _uiState.value = AuthUiState.Error(result.message)
            }
        }
    }

    /** Presigns + uploads [bytes] to R2 (see `StorageRepository`), then patches
     *  the profile's `photo_url` with the resulting public CDN URL. Used by
     *  the onboarding photo step and the profile-photo editor.
     *  [contentType]/[fileExtension] should come from the picked file's own
     *  MIME type (e.g. `"image/jpeg"` / `"jpeg"`) — presign signs the upload
     *  URL against that content-type, so it must match what's actually PUT. */
    fun uploadProfilePhoto(
        bytes: ByteArray,
        contentType: String,
        fileExtension: String,
        onSuccess: (UserDto) -> Unit,
    ) {
        viewModelScope.launch {
            _isUploadingPhoto.value = true
            _photoUploadError.value = null

            val fileName = "profile_${UUID.randomUUID()}.$fileExtension"
            val presign = when (
                val result = storageRepository.presign(fileName, contentType, R.string.error_storage_presign_photo)
            ) {
                is AuthResult.Success -> result.data
                is AuthResult.Error -> {
                    _isUploadingPhoto.value = false
                    _photoUploadError.value = result.message
                    return@launch
                }
            }

            val upload = storageRepository.uploadToPresignedUrl(
                presign.uploadUrl, bytes, contentType, R.string.error_storage_upload_photo,
            )
            if (upload is AuthResult.Error) {
                _isUploadingPhoto.value = false
                _photoUploadError.value = upload.message
                return@launch
            }

            when (val patchResult = repository.patchMe(PatchMeRequest(photoUrl = presign.publicUrl))) {
                is AuthResult.Success -> {
                    _isUploadingPhoto.value = false
                    val updatedUser = patchResult.data
                    _uiState.value = if (updatedUser.needsOnboarding()) {
                        AuthUiState.NeedsOnboarding(updatedUser)
                    } else {
                        AuthUiState.Authenticated(updatedUser)
                    }
                    onSuccess(updatedUser)
                }
                is AuthResult.Error -> {
                    _isUploadingPhoto.value = false
                    _photoUploadError.value = patchResult.message
                }
            }
        }
    }

    /** Replaces the user's entire interest set and marks auth as complete. */
    fun putInterests(ids: List<String>, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            Log.d(TAG, "putInterests: starting with ids=$ids")
            _uiState.value = AuthUiState.Loading
            when (val result = repository.putInterests(ids)) {
                is AuthResult.Success -> {
                    Log.d(TAG, "putInterests: success, interests=${result.data.interests}")
                    _uiState.value = AuthUiState.Authenticated(result.data)
                    syncFcmToken()
                    Log.d(TAG, "putInterests: invoking onSuccess callback")
                    onSuccess?.invoke()
                    Log.d(TAG, "putInterests: onSuccess callback returned")
                }
                is AuthResult.Error -> {
                    Log.e(TAG, "putInterests: ERROR → ${result.message}", result.cause)
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    /** Replaces the user's entire spoken-languages set. */
    fun putLanguages(languages: List<LanguageProficiencyDto>, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = repository.putLanguages(languages)) {
                is AuthResult.Success -> {
                    _uiState.value = AuthUiState.Authenticated(result.data)
                    onSuccess?.invoke()
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    /** Fetches the interest catalog for the picker screen. Non-fatal on failure. */
    fun loadInterests() {
        viewModelScope.launch {
            when (val result = repository.getInterests()) {
                is AuthResult.Success -> _interests.value = result.data
                is AuthResult.Error -> Unit // grid stays empty; user can retry from the screen
            }
        }
    }

    /** Fetches the owner's own showcase items — `status=all` (the default),
     * so paused items still render in the grid (same card, just a different
     * "···" menu action) instead of vanishing; only `"deleted"` ones are
     * dropped, client-side, since there's no undo UI for those. Reused after
     * a successful Pause/Delete/Reactivate (simpler than patching the list
     * in place, and it's a cheap call). */
    fun loadPostalCollection() {
        _postalCollection.value = PostalCollectionUiState.Loading
        viewModelScope.launch {
            _postalCollection.value = when (val result = lettersRepository.getMyShowcase()) {
                is AuthResult.Success -> PostalCollectionUiState.Success(
                    result.data.items
                        .map { it.toPostalCollectionItem() }
                        .filter { it.showcaseStatus != ShowcaseStatus.DELETED },
                )
                is AuthResult.Error -> PostalCollectionUiState.Error(result.message)
            }
        }
    }

    /** Pause or delete one showcase item from the "···" menu — both are a
     * soft un-list server-side (see `ShowcaseStatus`), so on success we just
     * reload the grid rather than tracking a per-item optimistic update. */
    fun updateShowcaseStatus(letterId: String, status: ShowcaseStatus) {
        viewModelScope.launch {
            when (val result = lettersRepository.updateShowcaseStatus(letterId, status)) {
                is AuthResult.Success -> loadPostalCollection()
                is AuthResult.Error -> _showcaseActionState.value = ShowcaseActionState.Error(result.message)
            }
        }
    }

    fun resetShowcaseActionState() {
        _showcaseActionState.value = ShowcaseActionState.Idle
    }

    /**
     * [onComplete] must be awaited before the caller navigates away — this
     * ViewModel is typically scoped to the screen that triggered it (e.g.
     * `ProfileSettingsScreen`'s NavBackStackEntry), and navigating with
     * `popUpTo` immediately after calling this without waiting would tear
     * down [viewModelScope] mid-cleanup, cancelling
     * [AuthRepository.signOutAndClearLocalData] before it finishes clearing
     * local drafts/bookmarks/cache (only the synchronous `auth.signOut()`
     * part at its start is guaranteed to have run).
     */
    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            analytics.setUserId(null)
            if (BuildConfig.REVENUECAT_API_KEY.isNotBlank() && Purchases.isConfigured) {
                Purchases.sharedInstance.logOut()
            }
            repository.signOutAndClearLocalData()
            unreadReminderScheduler.cancel()
            _uiState.value = AuthUiState.Unauthenticated
            onComplete()
        }
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.deleteAccount()) {
                is AuthResult.Success -> {
                    analytics.setUserId(null)
                    if (BuildConfig.REVENUECAT_API_KEY.isNotBlank() && Purchases.isConfigured) {
                        Purchases.sharedInstance.logOut()
                    }
                    repository.signOutAndClearLocalData()
                    unreadReminderScheduler.cancel()
                    _uiState.value = AuthUiState.Unauthenticated
                    onSuccess()
                }
                is AuthResult.Error -> {
                    onError(result.message)
                }
            }
        }
    }


    /**
     * Re-reads the current FCM token and re-PATCHes it to the backend every
     * time the user reaches [AuthUiState.Authenticated]. This is the
     * self-healing counterpart to [UnsealedMessagingService]'s `onNewToken` —
     * that callback only fires *once*, at rotation time, and is a no-op if it
     * fires before the user is logged in (nobody to PATCH against yet). Since
     * `FirebaseMessaging.getInstance().token` always returns the *current*
     * cached token (not just newly-rotated ones), calling this on every
     * successful auth transition means a token that rotated pre-login, or
     * whose PATCH silently failed once, gets picked up again next time the
     * app is opened — instead of the backend being stuck with a stale token
     * indefinitely and notifications quietly never arriving.
     *
     * Deliberately silent (no [_uiState] error surfaced) — this runs on every
     * app open and a transient failure here shouldn't block or interrupt
     * navigation into the main app.
     *
     * Also (re-)arms [UnreadReminderScheduler]'s daily "you still have
     * unread mail" nudge — same "every time we reach Authenticated" cadence,
     * and [UnreadReminderScheduler.scheduleDaily] is itself idempotent so
     * calling it again on every app open is harmless.
     */
    private fun syncFcmToken() {
        viewModelScope.launch {
            val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
            val utcOffsetMinutes = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000
            repository.patchMe(PatchMeRequest(fcmToken = token, utcOffsetMinutes = utcOffsetMinutes))
        }
        unreadReminderScheduler.scheduleDaily()
    }

    /**
     * Redeems an invite code captured from the Play Install Referrer at install time (see
     * [InstallReferrerPreferences], `docs/be_updet/invite_friend_api.md` §2/§5.1.A), right after
     * this user's backend record is first created — the earliest point `redeemed_by_user_id`
     * can reference a valid row. Deliberately silent, same fire-and-forget convention as
     * [syncFcmToken]: a missing/invalid/already-redeemed code shouldn't interrupt registration
     * or onboarding, and the inviter already gets their own confirmation via push notification.
     */
    private fun redeemPendingReferralInviteIfAny() {
        viewModelScope.launch {
            val code = installReferrerPreferences.consumePendingInviteCode() ?: return@launch
            inviteRepository.redeemInvite(code)
        }
    }
}
