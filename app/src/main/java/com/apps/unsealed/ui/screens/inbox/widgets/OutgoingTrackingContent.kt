package com.apps.unsealed.ui.screens.inbox.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.screens.inbox.state.ThreadLetterItem
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeAspectRatio
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.ToolbarFrostedDark

/** Compact "my letter is still in transit" row for one letter inside
 * [com.apps.unsealed.ui.screens.inbox.screen.MailboxThreadScreen]'s
 * chat-style thread list — frosted skeuomorphic envelope card with centered
 * tracking pin, title, delivery ETA, and tap-to-track CTA. */
@Composable
fun OutgoingTrackingContent(
    item: ThreadLetterItem,
    correspondentName: String,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val cardShape = RoundedCornerShape(16.dp)

    // BoxWithConstraints (not a plain `.aspectRatio(EnvelopeAspectRatio)`)
    // so the card can pick whichever is taller: the envelope's natural
    // ~1.91:1 landscape proportion, or a floor tall enough to fit the
    // stacked badge+title+ETA+TrackPillButton content. Plain aspectRatio
    // alone clips that content's bottom (the pill's text) on narrow/small
    // screens where the derived height comes out too short; a bare height
    // floor with no ceiling (tried first) instead let the card blow up tall
    // on every other screen, since nothing bounded its height from above.
    // This keeps the normal case looking like a landscape envelope while
    // still guaranteeing room on narrow ones.
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cardHeight = maxOf(maxWidth / EnvelopeAspectRatio, MinTrackingCardHeight)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                .clip(cardShape)
                .border(1.dp, BrandGold.copy(alpha = 0.35f), cardShape)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onCardClick),
        ) {
            // ── 1. Blurred Envelope Background ────────────────────────────
            Image(
                painter = painterResource(item.envelope.drawableRes),
                contentDescription = stringResource(R.string.open_letter_envelope_icon_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(10.dp)
                    .graphicsLayer { alpha = 0.55f },
            )

            // ── 2. Frosted Scrim Overlay ───────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ToolbarFrostedDark.copy(alpha = 0.70f),
                                ToolbarFrostedDark.copy(alpha = 0.88f),
                            ),
                        ),
                    ),
            )

            // ── 3. Centered Content (Badge, Title, ETA, Track CTA) ─────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Location / Track badge with subtle gold frosted ring
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.5.dp, BrandGold.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(17.dp),
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.mailbox_outgoing_tracking_title, correspondentName),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = stringResource(R.string.open_letter_expected_delivery, item.expectedDeliveryLabel.orEmpty()),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(8.dp))

                TrackPillButton(onClick = onCardClick)
            }
        }
    }
}

/** Floor for [OutgoingTrackingContent]'s card height — comfortably fits the
 * badge+title+ETA+TrackPillButton stack (~134dp) plus headroom for larger
 * system font scales, so it only kicks in on screens narrow enough that
 * `EnvelopeAspectRatio` alone would come out shorter than this. */
private val MinTrackingCardHeight = 152.dp

@Composable
private fun TrackPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(shape)
            .background(BrandGold.copy(alpha = 0.18f))
            .border(1.dp, BrandGold.copy(alpha = 0.55f), shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = BrandGold,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.mailbox_outgoing_tracking_cta),
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Color.White,
        )
    }
}
