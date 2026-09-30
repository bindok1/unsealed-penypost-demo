package com.apps.unsealed.ui.screens.inbox.widgets

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.ui.screens.inbox.state.MailboxRoomItem
import com.apps.unsealed.ui.screens.selectrecipient.widgets.StampImage
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.NunitoFontFamily
import kotlinx.coroutines.delay

/**
 * One mailbox row: stamp thumbnail (vertical rect) + correspondent name +
 * region · last-activity time.
 *
 * Motion: follows motion-rules.md §3.2 (EnvelopeCard) — scale + rotation on press,
 * no ripple. Stagger entrance per §3.7 (40 ms delay × [entranceIndex]).
 */
@Composable
fun MailboxRoomCard(
    room: MailboxRoomItem,
    entranceIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()

    // ── Stagger entrance ──────────────────────────────────────────────────────
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(entranceIndex * 40L)
        isVisible = true
    }

    val entranceOffsetX by animateDpAsState(
        targetValue = if (isVisible) 0.dp else (-24).dp,
        animationSpec = if (isReducedMotion) tween(0) else tween(220),
        label = "mailbox_card_offset_$entranceIndex",
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = if (isReducedMotion) tween(0) else tween(220),
        label = "mailbox_card_alpha_$entranceIndex",
    )

    // ── Press feedback (motion-rules §3.2) ────────────────────────────────────
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = LetterlySpring.Snappy,
        label = "mailbox_card_scale_$entranceIndex",
    )
    val cardRotation by animateFloatAsState(
        targetValue = if (isPressed) -1.5f else 0f,
        animationSpec = LetterlySpring.Snappy,
        label = "mailbox_card_rotation_$entranceIndex",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = entranceOffsetX.toPx()
                alpha = entranceAlpha
                scaleX = cardScale
                scaleY = cardScale
                rotationZ = cardRotation
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,   // no ripple — motion-rules §4
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        // ── Stamp thumbnail (52×72 dp portrait or 64×48 dp landscape) ─────────
        val isStampLandscape = room.stamp.isLandscape
        Box(
            modifier = Modifier
                .width(if (isStampLandscape) 64.dp else 52.dp)
                .height(if (isStampLandscape) 48.dp else 72.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            StampImage(
                item = room.stamp,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // ── Correspondent info ────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = room.correspondentName,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = stringResource(
                    R.string.mailbox_correspondent_country,
                    room.correspondentContinent,
                    room.lastActivityLabel,
                ),
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.60f),
            )
        }

        // ── In Transit badge + unread indicator dot ───────────────────────────
        Column(horizontalAlignment = Alignment.End) {
            if (room.isLastLetterInTransit) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BrandGold.copy(alpha = 0.18f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = stringResource(R.string.mailbox_in_transit_badge),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        color = BrandGold,
                    )
                }
            }
            if (room.isUnread) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE07A9A)),  // rose accent — matches StampDesign.ROSE
                )
            }
        }
    }
}
