package com.apps.unsealed.ui.screens.inbox.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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

/** Compact "still in transit" row for one letter inside
 * [com.apps.unsealed.ui.screens.inbox.screen.MailboxThreadScreen]'s chat-style
 * thread list — frosted skeuomorphic envelope card with centered lock,
 * title, delivery ETA, and tap-to-open-early CTA. */
@Composable
fun IncomingMailContent(
    item: ThreadLetterItem,
    onTrackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(EnvelopeAspectRatio)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(cardShape)
            .border(1.dp, BrandGold.copy(alpha = 0.35f), cardShape)
            // Both the card body and the pill button below open the tracking
            // map — that's where the real "feed Peni" energy boost CTA lives
            // now (DeliveryTrackingScreen.kt), so there's no separate
            // open-early stub left to wire up.
            .clickable(interactionSource = interactionSource, indication = null, onClick = onTrackClick),
    ) {
        // ── 1. Blurred Envelope Background ────────────────────────────────────
        Image(
            painter = painterResource(item.envelope.drawableRes),
            contentDescription = stringResource(R.string.open_letter_envelope_icon_desc),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(10.dp)
                .graphicsLayer { alpha = 0.55f },
        )

        // ── 2. Frosted Scrim Overlay ──────────────────────────────────────────
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

        // ── 3. Centered Content (Badge, Title, ETA, Open Early CTA) ───────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Lock badge with subtle gold frosted ring
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.5.dp, BrandGold.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = BrandGold,
                    modifier = Modifier.size(14.dp),
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.open_letter_incoming_mail_title),
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 16.sp,
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
                lineHeight = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(8.dp))

            OpenEarlyPillButton(onClick = onTrackClick)
        }
    }
}

@Composable
private fun OpenEarlyPillButton(
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
            .padding(horizontal = 14.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = BrandGold,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.open_letter_open_early),
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
