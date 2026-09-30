package com.apps.unsealed.core.review

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reviewDataStore: DataStore<Preferences> by preferencesDataStore(name = "review_prefs")

private val HasPromptedReviewKey = booleanPreferencesKey("has_prompted_review")

private const val TAG = "InAppReviewManager"

/**
 * Google Play's in-app review flow — a bottom sheet for a star rating (and
 * optional written review) shown without leaving the app, backed by the
 * `com.google.android.play:review-ktx` library.
 *
 * Called from [com.apps.unsealed.feature.auth.AuthViewModel] right when
 * [com.apps.unsealed.ui.screens.selectrecipient.screen.LetterSentScreen]
 * appears — the "user just finished sending a letter" moment of delight,
 * same spirit as [com.apps.unsealed.core.reminder.UnreadReminderScheduler]
 * living next to that ViewModel's other one-shot/lifecycle hooks.
 *
 * [maybeRequestReview] only ever *attempts* the flow once per install
 * ([HasPromptedReviewKey]), regardless of whether Play Services actually
 * shows the dialog. That's deliberate, not a limitation to work around:
 * Google throttles the dialog per-account on its own (quietly no-op'ing a
 * request if the same user saw it recently, even on a fresh install of a
 * *different* app), so there's no reliable signal to retry on — and per
 * Google's own guidance this must never be wired to a manual "Rate Us"
 * button, since a throttled no-op would then look like a broken button.
 * Call this unconditionally from any future "moment of delight" call site;
 * the one-shot flag makes every call site safe to fire without coordinating
 * with the others.
 */
@Singleton
class InAppReviewManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun maybeRequestReview(activity: Activity) {
        val alreadyPrompted = context.reviewDataStore.data.first()[HasPromptedReviewKey] ?: false
        if (alreadyPrompted) return

        // Marked *before* the request resolves — this is a best-effort, ask-
        // once nudge, not a guaranteed-shown dialog, so a failed/no-op attempt
        // isn't worth retrying at the next satisfying moment either.
        context.reviewDataStore.edit { it[HasPromptedReviewKey] = true }

        val manager = ReviewManagerFactory.create(context)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                manager.launchReviewFlow(activity, request.result)
            } else {
                Log.w(TAG, "In-app review flow unavailable", request.exception)
            }
        }
    }
}
