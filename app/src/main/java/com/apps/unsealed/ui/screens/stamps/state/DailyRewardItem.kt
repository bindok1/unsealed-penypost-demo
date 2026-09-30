package com.apps.unsealed.ui.screens.stamps.state

data class DailyRewardScheduleItem(
    val day: Int,
    val energy: Int,
    val stampId: String?,
    val isClaimed: Boolean,
    val isCurrent: Boolean,
)

data class DailyRewardStatusItem(
    val canClaimToday: Boolean,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastClaimedAt: String?,
    val nextResetAt: String?,
    val schedule: List<DailyRewardScheduleItem>,
)

data class DailyQuestItem(
    val id: String,
    val title: String,
    val description: String,
    val targetCount: Int,
    val currentCount: Int,
    val energyReward: Int,
    val isCompleted: Boolean,
    val isClaimed: Boolean,
) {
    val progressPercent: Float
        get() = if (targetCount > 0) (currentCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f) else 0f
}
