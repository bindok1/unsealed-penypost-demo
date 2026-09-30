package com.apps.unsealed.ui.screens.stamps.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.unsealed.R
import com.apps.unsealed.ui.screens.stamps.state.DailyQuestItem
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.ToolbarFrostedDark

@Composable
fun DailyQuestsSection(
    quests: List<DailyQuestItem>,
    claimingQuestIds: Set<String>,
    onClaimQuestClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (quests.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.stamps_quests_section_title),
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White,
        )

        Spacer(Modifier.height(12.dp))

        quests.forEach { quest ->
            DailyQuestRow(
                quest = quest,
                isClaiming = quest.id in claimingQuestIds,
                onClaimClick = { onClaimQuestClick(quest.id) },
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
    }
}

@Composable
private fun DailyQuestRow(
    quest: DailyQuestItem,
    isClaiming: Boolean,
    onClaimClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ToolbarFrostedDark,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            // Energy Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BrandGold.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.5f)),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "+${quest.energyReward}",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = BrandGold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Quest Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.title,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                )
                Text(
                    text = quest.description,
                    fontFamily = NunitoFontFamily,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f),
                )

                Spacer(Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { quest.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = BrandGold,
                    trackColor = Color.White.copy(alpha = 0.12f),
                )
            }

            Spacer(Modifier.width(12.dp))

            // Action Button
            if (quest.isClaimed) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.08f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.stamps_quest_claimed_label),
                            fontFamily = NunitoFontFamily,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    }
                }
            } else if (quest.isCompleted) {
                Button(
                    onClick = onClaimClick,
                    enabled = !isClaiming,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGold,
                        contentColor = BrandInkDeep,
                    ),
                    modifier = Modifier.height(34.dp),
                ) {
                    if (isClaiming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = BrandInkDeep,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.stamps_quest_claim_cta),
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }
            } else {
                Text(
                    text = "${quest.currentCount}/${quest.targetCount}",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}
