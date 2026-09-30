package com.apps.unsealed.ui.screens.tracking.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeAspectRatio
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeDesign
import com.apps.unsealed.ui.theme.BrandCardDark
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandGoldDeep
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.NunitoFontFamily
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

/**
 * High-celebration overlay triggered when user feeds/boosts Peni
 * to instant-deliver an incoming letter.
 *
 * Rendered as an in-screen overlay with a translucent dim scrim so the map
 * remains visible underneath with the festive confetti shower and floating envelope.
 */
@Composable
fun DeliveredCelebrationModal(
    senderName: String,
    envelopeDesignName: String?,
    envelopeCompositeImageUrl: String?,
    onOpenLetterClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val konfettiParties = remember {
        listOf(
            Party(
                speed = 10f,
                maxSpeed = 35f,
                damping = 0.9f,
                spread = 360,
                colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def, 0xd4af37, 0x3e9fdb),
                emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(60),
                position = Position.Relative(0.5, 0.25),
            ),
            Party(
                speed = 15f,
                maxSpeed = 40f,
                damping = 0.88f,
                angle = 45,
                spread = 70,
                colors = listOf(0xfce18a, 0xd4af37, 0xff726d),
                emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(35),
                position = Position.Relative(0.0, 0.0),
            ),
            Party(
                speed = 15f,
                maxSpeed = 40f,
                damping = 0.88f,
                angle = 135,
                spread = 70,
                colors = listOf(0xfce18a, 0x3e9fdb, 0xb48def),
                emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(35),
                position = Position.Relative(1.0, 0.0),
            ),
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "envelopeFloat")
    val floatY = infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floatY",
    )
    val glowPulse = infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    val envelopeRes = remember(envelopeDesignName) {
        if (!envelopeDesignName.isNullOrBlank()) {
            EnvelopeDesign.fromApiString(envelopeDesignName).drawableRes
        } else {
            R.drawable.envelope_1
        }
    }

    val envelopeInteractionSource = remember { MutableInteractionSource() }
    val envelopePressScale = rememberPressScale(envelopeInteractionSource)

    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.50f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}, // capture clicks so they don't leak to the map
            ),
        contentAlignment = Alignment.Center,
    ) {
        // ── Card Dialog Content ──────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = BrandCardDark.copy(alpha = 0.96f),
            shadowElevation = 20.dp,
            border = BorderStroke(1.5.dp, BrandGold.copy(alpha = 0.65f)),
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .padding(vertical = 24.dp)
                .graphicsLayer {
                    shadowElevation = 24f
                },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            ) {
                // Celebration Pill Header
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(BrandGoldDeep.copy(alpha = 0.85f), BrandGold.copy(alpha = 0.95f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.letter_tracking_celebration_badge),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrandInkDeep,
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Title
                Text(
                    text = stringResource(R.string.letter_tracking_celebration_title),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(6.dp))

                val subtitle = if (senderName.isNotBlank()) {
                    stringResource(R.string.letter_tracking_celebration_body_named, senderName)
                } else {
                    stringResource(R.string.letter_tracking_celebration_body_generic)
                }

                Text(
                    text = subtitle,
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.80f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                Spacer(Modifier.height(20.dp))

                // ── Interactive Envelope Item ────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .graphicsLayer {
                            translationY = floatY.value
                            scaleX = envelopePressScale
                            scaleY = envelopePressScale
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 2.dp,
                            color = BrandGold.copy(alpha = glowPulse.value),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clickable(
                            interactionSource = envelopeInteractionSource,
                            indication = null,
                            onClick = onOpenLetterClick,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!envelopeCompositeImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = envelopeCompositeImageUrl,
                            contentDescription = stringResource(R.string.letter_tracking_celebration_envelope_desc),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(EnvelopeAspectRatio)
                                .clip(RoundedCornerShape(16.dp)),
                        )
                    } else {
                        Image(
                            painter = painterResource(envelopeRes),
                            contentDescription = stringResource(R.string.letter_tracking_celebration_envelope_desc),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(EnvelopeAspectRatio)
                                .clip(RoundedCornerShape(16.dp)),
                        )
                    }

                    // Tap Hint Chip Floating on Envelope
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mail,
                                contentDescription = null,
                                tint = BrandGold,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.letter_tracking_celebration_tap_hint),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontFamily = NunitoFontFamily,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Primary CTA: Buka Surat Sekarang ────────────────────
                Button(
                    onClick = onOpenLetterClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGold,
                        contentColor = BrandInkDeep,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.letter_tracking_celebration_cta_open),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }

                Spacer(Modifier.height(10.dp))

                // ── Secondary CTA: Kembali ke Kotak Surat ───────────────
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.85f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                ) {
                    Text(
                        text = stringResource(R.string.letter_tracking_celebration_cta_back),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        // ── Konfetti Animation Layer ─────────────────────────────────────
        // Drawn last so it renders in front of the card instead of behind it.
        KonfettiView(
            modifier = Modifier.fillMaxSize(),
            parties = konfettiParties,
        )
    }
}
