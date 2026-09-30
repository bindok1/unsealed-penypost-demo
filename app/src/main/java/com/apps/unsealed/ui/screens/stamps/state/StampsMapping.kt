package com.apps.unsealed.ui.screens.stamps.state

import com.apps.unsealed.feature.rewards.data.DailyQuestDto
import com.apps.unsealed.feature.rewards.data.DailyRewardScheduleDto
import com.apps.unsealed.feature.rewards.data.DailyRewardStatusDto

fun DailyRewardScheduleDto.toDailyRewardScheduleItem(): DailyRewardScheduleItem =
    DailyRewardScheduleItem(
        day = day,
        energy = energy,
        stampId = stampId,
        isClaimed = isClaimed,
        isCurrent = isCurrent,
    )

fun DailyRewardStatusDto.toDailyRewardStatusItem(): DailyRewardStatusItem =
    DailyRewardStatusItem(
        canClaimToday = canClaimToday,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastClaimedAt = lastClaimedAt,
        nextResetAt = nextResetAt,
        schedule = schedule.map { it.toDailyRewardScheduleItem() },
    )

fun DailyQuestDto.toDailyQuestItem(): DailyQuestItem =
    DailyQuestItem(
        id = id,
        title = title,
        description = description,
        targetCount = targetCount,
        currentCount = currentCount,
        energyReward = energyReward,
        isCompleted = isCompleted,
        isClaimed = isClaimed,
    )
