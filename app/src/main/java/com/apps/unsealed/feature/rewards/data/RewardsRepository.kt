package com.apps.unsealed.feature.rewards.data

import android.content.Context
import com.apps.unsealed.R
import com.apps.unsealed.core.network.toUserFacingMessage
import com.apps.unsealed.core.network.unwrap
import com.apps.unsealed.feature.auth.data.AuthResult
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Retrofit

@Singleton
class RewardsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val crashlytics: FirebaseCrashlytics,
    private val retrofit: Retrofit,
) {
    private val api: RewardsApi by lazy { retrofit.create(RewardsApi::class.java) }

    /** Fetches the viewer's current daily check-in status and 7-day reward calendar. */
    suspend fun getDailyStatus(): AuthResult<DailyRewardStatusDto> =
        runCatching {
            api.getDailyStatus().unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_generic), it)
            },
        )

    /** Claims today's daily check-in reward. */
    suspend fun claimDailyReward(): AuthResult<ClaimDailyRewardResponseData> =
        runCatching {
            api.claimDailyReward().unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_generic), it)
            },
        )

    /** Fetches active daily quests and user progress. */
    suspend fun getDailyQuests(): AuthResult<List<DailyQuestDto>> =
        runCatching {
            api.getDailyQuests().unwrap().items
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_generic), it)
            },
        )

    /** Claims reward for a completed daily quest. */
    suspend fun claimQuest(questId: String): AuthResult<ClaimQuestResponseData> =
        runCatching {
            api.claimQuest(questId).unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_generic), it)
            },
        )

    /** Unlocks a stamp using energy. */
    suspend fun unlockStamp(stampId: String): AuthResult<UnlockStampResponseData> =
        runCatching {
            api.unlockStamp(stampId).unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_generic), it)
            },
        )

    /** Unlocks a letter paper using energy. */
    suspend fun unlockPaper(paperId: String): AuthResult<UnlockPaperResponseData> =
        runCatching {
            api.unlockPaper(paperId).unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_generic), it)
            },
        )
}
