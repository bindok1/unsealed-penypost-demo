package com.apps.unsealed.core.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.apps.unsealed.MainActivity
import com.apps.unsealed.R
import com.apps.unsealed.feature.auth.data.AuthRepository
import com.apps.unsealed.feature.auth.data.AuthResult
import com.apps.unsealed.feature.mailbox.data.MailboxRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val ChannelId = "unread_mail_reminder"
private const val NotificationId = 9001

/**
 * Once-a-day background check (scheduled by [UnreadReminderScheduler]) that
 * nudges the user with a local notification if they have unread mail sitting
 * in [MailboxRepository] they haven't opened — the "lupa ada pesan yang
 * belum dibaca" case. Silent no-op when signed out, offline, or already
 * caught up (zero unread), so it never fires a false "you have mail" ping.
 */
@HiltWorker
class UnreadReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepository,
    private val mailboxRepository: MailboxRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Not signed in — nothing to remind about, and nothing to retry either.
        if (authRepository.currentUser == null) return Result.success()

        val unreadCount = when (val result = mailboxRepository.getMailbox()) {
            is AuthResult.Success -> result.data.sumOf { it.unreadCount }
            // Offline / backend hiccup — try again later rather than silently
            // skipping today's reminder.
            is AuthResult.Error -> return Result.retry()
        }
        if (unreadCount > 0) showNotification(unreadCount)
        return Result.success()
    }

    private fun showNotification(unreadCount: Int) {
        val context = applicationContext
        ensureNotificationChannel(context)

        // Same "just open MainActivity" convention as
        // UnsealedMessagingService — no dedicated mailbox deep link exists
        // yet, so a tap lands on the app's default destination.
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NotificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.unread_reminder_notification_title))
            .setContentText(
                context.resources.getQuantityString(
                    R.plurals.unread_reminder_notification_body,
                    unreadCount,
                    unreadCount,
                ),
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // POST_NOTIFICATIONS can be declined — see UnsealedMessagingService.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(NotificationId, notification)
    }

    /** Low-key channel (IMPORTANCE_DEFAULT, default system sound) — this is a
     * passive daily nudge, not the urgent "letter just arrived" ping that
     * [ChannelId] over in UnsealedMessagingService's `letter_arrivals` uses. */
    private fun ensureNotificationChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            ChannelId,
            context.getString(R.string.unread_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.unread_reminder_channel_description)
        }
        manager.createNotificationChannel(channel)
    }
}
