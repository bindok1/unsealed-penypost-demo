package com.apps.unsealed.ui.screens.stamps.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.ToolbarFrostedDark

/**
 * Entry point into [com.apps.unsealed.ui.screens.profile.screen.ProfileInviteFriendsScreen]
 * from the Stamps/Rewards screen — surfaces the "invite a friend, earn energy"
 * growth loop where users are already primed to think about energy, instead
 * of it living only inside Profile where a first-time user has no reason to
 * expect it.
 *
 * Deliberately a plain always-visible promo card, not a [DailyQuestsSection]
 * row: the actual server-tracked referral quest (`docs/be_updet/invite_friend_reward_api.md`,
 * not built yet) will show up automatically in that list once BE ships it
 * (§2 of that doc — zero client changes needed there), keyed to one specific
 * invite's redeem/first-letter progress. This card is unconditional and has
 * no completion state of its own — it's the "did you know?" discovery nudge
 * that exists whether or not the user has ever invited anyone, same spirit
 * as [DailyStreakRewardCard] always being visible regardless of streak state.
 */
@Composable
fun InviteFriendsPromoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ToolbarFrostedDark,
        border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandGold.copy(alpha = 0.15f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Filled.GroupAdd,
                    contentDescription = null,
                    tint = BrandGold,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.stamps_invite_promo_title),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.stamps_invite_promo_body),
                    fontFamily = NunitoFontFamily,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}
