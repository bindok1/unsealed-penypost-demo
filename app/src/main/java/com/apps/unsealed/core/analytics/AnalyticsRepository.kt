package com.apps.unsealed.core.analytics

import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around [FirebaseAnalytics] — keeps all SDK calls out of
 * ViewModels and Screens, consistent with the project's "UI never touches
 * Firebase directly" rule (see architecture.md §4).
 *
 * Usage in a ViewModel:
 * ```kotlin
 * @HiltViewModel
 * class SomeViewModel @Inject constructor(
 *     private val analytics: AnalyticsRepository,
 * ) : ViewModel() {
 *     fun onSomethingHappened() {
 *         analytics.logEvent("something_happened")
 *     }
 * }
 * ```
 */
@Singleton
class AnalyticsRepository @Inject constructor(
    private val analytics: FirebaseAnalytics,
) {

    /**
     * Links all subsequent events to [userId] in Firebase Analytics.
     * Call this right after the user reaches [AuthUiState.Authenticated],
     * and with `null` when the user signs out.
     */
    fun setUserId(userId: String?) {
        analytics.setUserId(userId)
    }

    /**
     * Logs a custom event. [params] is optional — pass key/value pairs via
     * [bundleOf] for extra context, e.g.:
     * ```kotlin
     * analytics.logEvent("letter_sent", bundleOf("recipient_continent" to "Asia"))
     * ```
     */
    fun logEvent(name: String, params: Bundle? = null) {
        analytics.logEvent(name, params)
    }
}
