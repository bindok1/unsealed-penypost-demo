package com.apps.unsealed.ui.screens.stamps.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.ui.screens.stamps.state.DailyRewardScheduleItem
import com.apps.unsealed.ui.screens.stamps.state.DailyRewardStatusItem
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandGoldDim
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.LemonYellow
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.ToolbarFrostedDark

@Composable
fun DailyStreakRewardCard(
    dailyStatus: DailyRewardStatusItem?,
    isClaiming: Boolean,
    onClaimClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()
    val collectScale = remember { Animatable(1f) }
    var selectedMilestone by remember { mutableStateOf<DailyRewardScheduleItem?>(null) }

    LaunchedEffect(dailyStatus?.canClaimToday) {
        if (dailyStatus?.canClaimToday == false) {
            collectScale.animateTo(1.25f, reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion))
            collectScale.animateTo(1f, reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion))
        }
    }

    val streak = dailyStatus?.currentStreak ?: 0
    val canClaim = dailyStatus?.canClaimToday == true

    val defaultSchedule = remember(streak) {
        listOf(
            DailyRewardScheduleItem(day = 1, energy = 10, stampId = null, isClaimed = streak >= 1, isCurrent = streak == 0),
            DailyRewardScheduleItem(day = 2, energy = 15, stampId = null, isClaimed = streak >= 2, isCurrent = streak == 1),
            DailyRewardScheduleItem(day = 3, energy = 20, stampId = "seal_wax_gold", isClaimed = streak >= 3, isCurrent = streak == 2),
            DailyRewardScheduleItem(day = 4, energy = 25, stampId = null, isClaimed = streak >= 4, isCurrent = streak == 3),
            DailyRewardScheduleItem(day = 5, energy = 30, stampId = null, isClaimed = streak >= 5, isCurrent = streak == 4),
            DailyRewardScheduleItem(day = 6, energy = 40, stampId = null, isClaimed = streak >= 6, isCurrent = streak == 5),
            DailyRewardScheduleItem(day = 7, energy = 50, stampId = "stamp_peni_aurora", isClaimed = streak >= 7, isCurrent = streak == 6),
        )
    }

    val schedule = dailyStatus?.schedule?.takeIf { it.isNotEmpty() } ?: defaultSchedule
    val currentDayReward = schedule.find { it.isCurrent }?.energy ?: 20

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ToolbarFrostedDark,
        border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = collectScale.value
                scaleY = collectScale.value
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header: Streak Fire & Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BrandGold.copy(alpha = 0.18f))
                        .border(1.dp, BrandGold.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.stamps_streak_header, streak),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                    Text(
                        text = stringResource(R.string.stamps_streak_subtitle),
                        fontFamily = NunitoFontFamily,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 7-Day Timeline Node Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                schedule.forEach { dayItem ->
                    StreakDayNode(
                        item = dayItem,
                        onClick = { selectedMilestone = dayItem },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Claim CTA Button
            Button(
                onClick = onClaimClick,
                enabled = canClaim && !isClaiming,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGold,
                    contentColor = BrandInkDeep,
                    disabledContainerColor = Color.White.copy(alpha = 0.12f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (isClaiming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = BrandInkDeep,
                        strokeWidth = 2.dp,
                    )
                } else if (canClaim) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = BrandInkDeep,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.stamps_claim_daily_cta, currentDayReward),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.stamps_claimed_today_label),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }

            // Next Daily Reset Countdown Pill (shown when user already claimed today)
            if (!canClaim && dailyStatus != null) {
                Spacer(Modifier.height(10.dp))
                NextClaimCountdownPill(nextResetAt = dailyStatus.nextResetAt)
            }
        }
    }

    // Milestone Details Dialog
    selectedMilestone?.let { milestone ->
        MilestoneRewardDialog(
            item = milestone,
            onDismiss = { selectedMilestone = null },
        )
    }
}

@Composable
private fun StreakDayNode(
    item: DailyRewardScheduleItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDay7 = item.day == 7
    val isDay3 = item.day == 3
    val infiniteTransition = rememberInfiniteTransition(label = "nodePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 1.dp, vertical = 2.dp),
    ) {
        Text(
            text = "D${item.day}",
            fontFamily = NunitoFontFamily,
            fontWeight = if (item.isCurrent) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            color = if (item.isCurrent) BrandGold else Color.White.copy(alpha = 0.6f),
            maxLines = 1,
            softWrap = false,
        )

        Spacer(Modifier.height(4.dp))

        val nodeShape = if (isDay7) RoundedCornerShape(10.dp) else CircleShape

        Box(
            modifier = Modifier
                .size(if (isDay7) 36.dp else 30.dp)
                .clip(nodeShape)
                .then(
                    if (isDay7 && !item.isClaimed) {
                        Modifier.background(Brush.linearGradient(listOf(BrandGold, LemonYellow)), nodeShape)
                    } else {
                        Modifier.background(
                            when {
                                item.isClaimed -> BrandGold.copy(alpha = 0.25f)
                                item.isCurrent -> BrandGold.copy(alpha = 0.18f)
                                else -> Color.White.copy(alpha = 0.08f)
                            },
                            nodeShape,
                        )
                    }
                )
                .border(
                    width = if (item.isCurrent) 1.5.dp else 1.dp,
                    color = when {
                        item.isClaimed -> BrandGold.copy(alpha = 0.8f)
                        item.isCurrent -> BrandGold.copy(alpha = pulseAlpha)
                        isDay7 -> BrandGold
                        else -> Color.White.copy(alpha = 0.15f)
                    },
                    shape = nodeShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                item.isClaimed -> {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(15.dp),
                    )
                }
                isDay7 -> {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = BrandInkDeep,
                        modifier = Modifier.size(16.dp),
                    )
                }
                item.isCurrent -> {
                    Text(
                        text = "+${item.energy}",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        color = BrandGold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                else -> {
                    Icon(
                        imageVector = if (isDay3) Icons.Filled.Star else Icons.Filled.Lock,
                        contentDescription = null,
                        tint = if (isDay3) BrandGold else Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = if (isDay7) "🎁" else if (isDay3) "🌟" else "+${item.energy}",
            fontFamily = NunitoFontFamily,
            fontSize = 10.sp,
            color = if (item.isClaimed || item.isCurrent) BrandGoldDim else Color.White.copy(alpha = 0.45f),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun MilestoneRewardDialog(
    item: DailyRewardScheduleItem,
    onDismiss: () -> Unit,
) {
    val isDay7 = item.day == 7
    val isDay3 = item.day == 3

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ToolbarFrostedDark,
            border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(BrandGold.copy(alpha = 0.2f))
                        .border(1.5.dp, BrandGold, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isDay7 || isDay3) Icons.Filled.Star else Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.stamps_milestone_dialog_title, item.day),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.White,
                )

                Spacer(Modifier.height(8.dp))

                // Energy Reward
                Text(
                    text = "+${item.energy} Energi Peni ⚡",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandGold,
                )

                // Special Milestone Bonus text if applicable
                if (isDay3) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.stamps_milestone_day3_item),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                } else if (isDay7) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.stamps_milestone_day7_item),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = LemonYellow,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Status Badge
                val statusText = when {
                    item.isClaimed -> stringResource(R.string.stamps_milestone_status_claimed)
                    item.isCurrent -> stringResource(R.string.stamps_milestone_status_available)
                    else -> stringResource(R.string.stamps_milestone_status_locked)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (item.isClaimed) BrandGold.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, if (item.isClaimed) BrandGold.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f)),
                ) {
                    Text(
                        text = statusText,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = if (item.isClaimed) BrandGold else Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGold,
                        contentColor = BrandInkDeep,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                ) {
                    Text(
                        text = stringResource(R.string.stamps_milestone_close),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

/**
 * Displays a live countdown timer until the next daily reward check-in becomes available.
 */
@Composable
private fun NextClaimCountdownPill(
    nextResetAt: String?,
    modifier: Modifier = Modifier,
) {
    var remainingMillis by remember(nextResetAt) {
        val target = parseNextResetMillis(nextResetAt)
        mutableStateOf(maxOf(0L, target - System.currentTimeMillis()))
    }

    LaunchedEffect(nextResetAt) {
        val target = parseNextResetMillis(nextResetAt)
        while (true) {
            val diff = target - System.currentTimeMillis()
            remainingMillis = maxOf(0L, diff)
            if (remainingMillis <= 0L) break
            delay(1000L)
        }
    }

    val countdownText = remember(remainingMillis) {
        val totalSecs = remainingMillis / 1000
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val seconds = totalSecs % 60
        String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BrandGold.copy(alpha = 0.10f))
            .border(BorderStroke(1.dp, BrandGold.copy(alpha = 0.25f)), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = null,
            tint = BrandGold,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (remainingMillis > 0) {
                stringResource(R.string.stamps_next_claim_in, countdownText)
            } else {
                stringResource(R.string.stamps_next_claim_available_now)
            },
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = BrandGold,
        )
    }
}

private fun parseNextResetMillis(nextResetAt: String?): Long {
    if (!nextResetAt.isNullOrBlank()) {
        val parsed = runCatching { java.time.Instant.parse(nextResetAt).toEpochMilli() }.getOrNull()
        if (parsed != null && parsed > System.currentTimeMillis()) {
            return parsed
        }
    }
    // Fallback: Next local midnight
    val now = java.time.Instant.now()
    return runCatching {
        now.atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrDefault(System.currentTimeMillis() + 86400000L)
}
