package com.apps.unsealed.feature.rewards.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DailyRewardScheduleDto(
    val day: Int,
    val energy: Int,
    @Json(name = "stamp_id") val stampId: String?,
    @Json(name = "is_claimed") val isClaimed: Boolean,
    @Json(name = "is_current") val isCurrent: Boolean,
)

@JsonClass(generateAdapter = true)
data class DailyRewardStatusDto(
    @Json(name = "can_claim_today") val canClaimToday: Boolean,
    @Json(name = "current_streak") val currentStreak: Int,
    @Json(name = "longest_streak") val longestStreak: Int,
    @Json(name = "last_claimed_at") val lastClaimedAt: String?,
    @Json(name = "next_reset_at") val nextResetAt: String?,
    val schedule: List<DailyRewardScheduleDto>,
)

@JsonClass(generateAdapter = true)
data class ClaimDailyRewardResponseData(
    val streak: Int,
    @Json(name = "energy_granted") val energyGranted: Int,
    @Json(name = "current_energy_balance") val currentEnergyBalance: Int,
    @Json(name = "unlocked_stamp") val unlockedStamp: String? = null,
    @Json(name = "unlocked_item") val unlockedItem: String? = null,
    @Json(name = "claimed_at") val claimedAt: String,
    @Json(name = "next_reset_at") val nextResetAt: String,
) {
    val grantedSpecialItem: String? get() = unlockedStamp ?: unlockedItem
}

@JsonClass(generateAdapter = true)
data class DailyQuestDto(
    val id: String,
    val title: String,
    val description: String,
    @Json(name = "target_count") val targetCount: Int,
    @Json(name = "current_count") val currentCount: Int,
    @Json(name = "energy_reward") val energyReward: Int,
    @Json(name = "is_completed") val isCompleted: Boolean,
    @Json(name = "is_claimed") val isClaimed: Boolean,
)

@JsonClass(generateAdapter = true)
data class DailyQuestsResponseData(
    val items: List<DailyQuestDto>,
)

@JsonClass(generateAdapter = true)
data class ClaimQuestResponseData(
    @Json(name = "quest_id") val questId: String,
    @Json(name = "energy_granted") val energyGranted: Int,
    @Json(name = "current_energy_balance") val currentEnergyBalance: Int,
    @Json(name = "claimed_at") val claimedAt: String,
)

@JsonClass(generateAdapter = true)
data class UnlockStampResponseData(
    @Json(name = "stamp_id") val stampId: String,
    @Json(name = "energy_spent") val energySpent: Int,
    @Json(name = "current_energy_balance") val currentEnergyBalance: Int,
    @Json(name = "unlocked_at") val unlockedAt: String,
)

@JsonClass(generateAdapter = true)
data class UnlockPaperResponseData(
    @Json(name = "paper_id") val paperId: String,
    @Json(name = "energy_spent") val energySpent: Int,
    @Json(name = "current_energy_balance") val currentEnergyBalance: Int,
    @Json(name = "unlocked_at") val unlockedAt: String,
)
