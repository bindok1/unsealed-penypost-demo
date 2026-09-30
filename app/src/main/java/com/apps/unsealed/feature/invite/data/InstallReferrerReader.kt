package com.apps.unsealed.feature.invite.data

import android.content.Context
import android.net.Uri
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Query-param key the Play Store install link (`docs/be_updet/invite_friend_api.md` §5.1.A)
 * carries the invite code under, e.g. `...&referrer=invite_code%3DK7XQ2A`. */
private const val InviteCodeReferrerParam = "invite_code"

/**
 * One-shot wrapper around [InstallReferrerClient] — reads the referrer string Play Store
 * attaches at install time and pulls out the [InviteCodeReferrerParam] value, if present. Not
 * useful to call more than once per install (Play only guarantees the referrer is available for
 * a limited window after first install), so callers should gate this behind
 * `InstallReferrerPreferences.hasCheckedReferrer()`.
 */
@Singleton
class InstallReferrerReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashlytics: FirebaseCrashlytics,
) {
    /** Returns the invite code from the install referrer, or null if there isn't one (organic
     * install, referrer unavailable, or the connection/query failed for any reason). */
    suspend fun readInviteCode(): String? = suspendCancellableCoroutine { continuation ->
        val client = InstallReferrerClient.newBuilder(context).build()
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                val code = runCatching {
                    if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        client.installReferrer.installReferrer?.let(::extractInviteCode)
                    } else {
                        null
                    }
                }.onFailure { crashlytics.recordException(it) }.getOrNull()
                client.endConnection()
                if (continuation.isActive) continuation.resume(code)
            }

            override fun onInstallReferrerServiceDisconnected() {
                // No retry here — a disconnect mid-query just resolves null for this attempt.
            }
        })
        continuation.invokeOnCancellation { runCatching { client.endConnection() } }
    }

    private fun extractInviteCode(referrerUrl: String): String? =
        Uri.parse("https://unsealed.app/?$referrerUrl").getQueryParameter(InviteCodeReferrerParam)
}
