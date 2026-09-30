package com.apps.unsealed.core.reminder

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Local time of day [UnreadReminderWorker] runs at. */
private const val ReminderHourOfDay = 19 // 7 PM — evening check-in, once a day

private const val WorkName = "unread_mail_reminder"

/**
 * Schedules/cancels [UnreadReminderWorker]'s once-a-day "you still have
 * unread mail" check. Called from [com.apps.unsealed.feature.auth.AuthViewModel]
 * next to `syncFcmToken()` (schedule on reaching `Authenticated`) and from
 * `signOut()` (cancel — no point nagging a signed-out device).
 */
@Singleton
class UnreadReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Idempotent — [ExistingPeriodicWorkPolicy.KEEP] means a call on every
     * app-open/login (same cadence as `syncFcmToken()`) leaves an
     * already-scheduled reminder's [ReminderHourOfDay] alignment alone
     * instead of re-anchoring it to "whatever time the user happened to
     * open the app today".
     */
    fun scheduleDaily() {
        val request = PeriodicWorkRequestBuilder<UnreadReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNextReminderHour(), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WorkName, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WorkName)
    }

    private fun millisUntilNextReminderHour(): Long {
        val now = Calendar.getInstance()
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, ReminderHourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
