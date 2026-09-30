package com.apps.unsealed.feature.rewards.data

import com.apps.unsealed.core.network.ApiResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RewardsApi {

    @GET("api/v1/rewards/daily")
    suspend fun getDailyStatus(): ApiResponse<DailyRewardStatusDto>

    @POST("api/v1/rewards/daily/claim")
    suspend fun claimDailyReward(): ApiResponse<ClaimDailyRewardResponseData>

    @GET("api/v1/rewards/quests")
    suspend fun getDailyQuests(): ApiResponse<DailyQuestsResponseData>

    @POST("api/v1/rewards/quests/{quest_id}/claim")
    suspend fun claimQuest(
        @Path("quest_id") questId: String,
    ): ApiResponse<ClaimQuestResponseData>

    @POST("api/v1/stamps/{stamp_id}/unlock")
    suspend fun unlockStamp(
        @Path("stamp_id") stampId: String,
    ): ApiResponse<UnlockStampResponseData>

    @POST("api/v1/papers/{paper_id}/unlock")
    suspend fun unlockPaper(
        @Path("paper_id") paperId: String,
    ): ApiResponse<UnlockPaperResponseData>
}
