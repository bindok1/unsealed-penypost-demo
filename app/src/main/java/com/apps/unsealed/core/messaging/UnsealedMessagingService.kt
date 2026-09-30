package com.apps.unsealed.core.messaging

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.apps.unsealed.MainActivity
import com.apps.unsealed.R
import com.apps.unsealed.feature.auth.data.AuthRepository
import com.apps.unsealed.feature.auth.data.PatchMeRequest
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val ChannelId = "letter_arrivals"

/** `letter_id` key in the FCM `data` payload — see docs/be/notifications_api.md §4. */
const val FcmExtraLetterId = "letter_id"

/**
 * `event` value for the "today's penpal stack is ready" push — see
 * docs/be/daily_penpal_stack.md §5. No dedicated handling needed today:
 * [onMessageReceived] already renders any `notification.title`/`body`
 * generically, and this event carries no `letter_id` so a tap just opens
 * [MainActivity] at its default destination, which is already the PenPals
 * feed. Kept as a named constant for when per-event branching is added,
 * rather than a magic string. Note the BE payload key is `event` here vs.
 * `type` for `letter_delivered` (`FcmExtraLetterId`'s doc) — a naming
 * inconsistency across the two payload shapes, not a client bug.
 */
const val FcmEventDailyStackReady = "daily_stack_ready"

/**
 * Receives FCM token rotations and incoming push messages.
 *
 * Token registration has two paths, deliberately redundant against the "token
 * went stale and notifications silently stopped" failure mode:
 * - [onNewToken] fires on rotation (or first install) and pushes immediately
 *   *if* a Firebase session already exists.
 * - [AuthViewModel.syncFcmToken] re-reads the current token and re-PATCHes it
 *   every time the user reaches [AuthUiState.Authenticated] (app open, login,
 *   onboarding finish) — this is the self-healing path for tokens that
 *   rotated *before* login (so this service had nobody to PATCH as yet) or
 *   were missed for any other reason.
 */
@AndroidEntryPoint
class UnsealedMessagingService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // No Firebase session yet (token rotated pre-login) — nothing to PATCH
        // against. AuthViewModel.syncFcmToken() re-reads the (by-then current)
        // token once the user does log in, so this isn't lost.
        if (authRepository.currentUser == null) return

        serviceScope.launch {
            runCatching { authRepository.patchMe(PatchMeRequest(fcmToken = token)) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val letterId = message.data[FcmExtraLetterId]

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            letterId?.let { putExtra(FcmExtraLetterId, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            letterId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        ensureNotificationChannel()

        val notification = NotificationCompat.Builder(this, ChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .apply {
                // API 26+ plays whatever sound is on the channel (set in
                // ensureNotificationChannel) and ignores this — only needed as
                // a fallback for the pre-channel API <26 slice of minSdk 24.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    @Suppress("DEPRECATION")
                    setSound(notificationSoundUri)
                }
            }
            .build()

        // POST_NOTIFICATIONS can be declined (OnboardingPermissionsScreen's Skip,
        // or a later system-settings toggle) — notify() would throw without this
        // check on API 33+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this).notify(letterId?.hashCode() ?: 0, notification)
    }

    private val notificationSoundUri: Uri
        get() = Uri.parse("android.resource://$packageName/${R.raw.bubble_pop_notif}")

    /**
     * A channel's sound is locked in at creation time — recreating it here
     * with the same [ChannelId] does **not** change the sound on a device
     * that already has this channel (e.g. from an earlier build without a
     * custom sound). Bump [ChannelId] (e.g. `"letter_arrivals_v2"`) if the
     * sound ever needs to change again post-release.
     */
    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            ChannelId,
            getString(R.string.fcm_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.fcm_notification_channel_description)
            setSound(
                notificationSoundUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        manager.createNotificationChannel(channel)
    }
}
