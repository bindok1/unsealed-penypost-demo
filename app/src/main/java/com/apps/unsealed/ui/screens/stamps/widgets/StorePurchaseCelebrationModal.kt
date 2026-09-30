package com.apps.unsealed.ui.screens.stamps.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.screens.stamps.state.ItemDetailUi
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
 * Celebration modal presented immediately upon a successful creator pack purchase.
 *
 * Modeled after [DeliveredCelebrationModal] and [SendSuccessOverlay], featuring:
 * - A celebratory Konfetti shower with a warm palette
 * - Gentle ambient float and golden glow pulsation
 * - Warm, heartfelt copywriting thanking the user for supporting independent artists
 * - Interactive preview card of the purchased collectible
 * - Action buttons to save to desk collection or explore more pieces
 */
@Composable
fun StorePurchaseCelebrationModal(
    itemDetail: ItemDetailUi?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onExploreMore: (() -> Unit)? = null,
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

    val infiniteTransition = rememberInfiniteTransition(label = "purchaseModalFloat")
    val floatY = infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floatY",
    )
    val glowPulse = infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    val itemCardInteractionSource = remember { MutableInteractionSource() }
    val itemCardScale = rememberPressScale(itemCardInteractionSource)

    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}, // Scrim captures clicks
            ),
        contentAlignment = Alignment.Center,
    ) {
        // ── Card Dialog Content ──────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = BrandCardDark.copy(alpha = 0.97f),
            shadowElevation = 24.dp,
            border = BorderStroke(1.5.dp, BrandGold.copy(alpha = 0.70f)),
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .padding(vertical = 24.dp)
                .graphicsLayer {
                    shadowElevation = 24f
                },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
            ) {
                // ── Celebration Pill Header ──────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(BrandGoldDeep.copy(alpha = 0.85f), BrandGold.copy(alpha = 0.95f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.store_purchase_celebration_badge),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = BrandInkDeep,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        lineHeight = 15.sp,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Warm Headline Title ───────────────────────────────────
                Text(
                    text = stringResource(R.string.store_purchase_success_title),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 27.sp,
                )

                Spacer(Modifier.height(8.dp))

                // ── Warm Stationery Body Copy ─────────────────────────────
                Text(
                    text = stringResource(R.string.store_purchase_success_body),
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.82f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                Spacer(Modifier.height(20.dp))

                // ── Purchased Collectible Showcase Card ───────────────────
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.2.dp, BrandGold.copy(alpha = glowPulse.value)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = floatY.value
                            scaleX = itemCardScale
                            scaleY = itemCardScale
                        }
                        .clickable(
                            interactionSource = itemCardInteractionSource,
                            indication = null,
                            onClick = onDismiss,
                        ),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(14.dp),
                    ) {
                        val heroUrl = itemDetail?.bannerUrl?.ifBlank { null }
                            ?: itemDetail?.assets?.firstOrNull()?.assetUrl
                            ?: itemDetail?.thumbnailUrl

                        if (!heroUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = heroUrl,
                                contentDescription = itemDetail?.title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                            Spacer(Modifier.height(10.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(BrandGold.copy(alpha = 0.20f))
                                    .border(1.5.dp, BrandGold.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = BrandGold,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        Text(
                            text = itemDetail?.title ?: stringResource(R.string.store_purchase_success_title),
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        val creatorName = itemDetail?.creator?.name
                        if (!creatorName.isNullOrBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = stringResource(R.string.store_by_creator, creatorName),
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = BrandGold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }

                        // Asset badges summary
                        if (itemDetail != null && itemDetail.assets.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val stampsCount = itemDetail.assets.count { it.assetType.equals("STAMP", ignoreCase = true) }
                                val stickersCount = itemDetail.assets.count { it.assetType.equals("STICKER", ignoreCase = true) }
                                val paperCount = itemDetail.assets.count { it.assetType.equals("PAPER", ignoreCase = true) }
                                val envelopesCount = itemDetail.assets.count { it.assetType.equals("ENVELOPE", ignoreCase = true) }

                                if (stampsCount > 0) AssetCountChip(stringResource(R.string.store_badge_stamps_count, stampsCount))
                                if (stickersCount > 0) AssetCountChip(stringResource(R.string.store_badge_stickers_count, stickersCount))
                                if (paperCount > 0) AssetCountChip(stringResource(R.string.store_badge_paper_count, paperCount))
                                if (envelopesCount > 0) AssetCountChip(stringResource(R.string.store_badge_envelopes_count, envelopesCount))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // ── Primary Action: Simpan ke Meja Tulis ───────────────────
                // Uses wrapContentHeight so text is never clipped if locale string is longer
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGold,
                        contentColor = BrandInkDeep,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(
                        text = stringResource(R.string.store_purchase_celebration_keep_cta),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // ── Secondary Action: Lihat Koleksi Lainnya ───────────────
                if (onExploreMore != null) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = onExploreMore,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.store_purchase_celebration_explore_cta),
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.70f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // ── Festive Konfetti Shower ──────────────────────────────────────
        KonfettiView(
            parties = konfettiParties,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun AssetCountChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(0.8.dp, BrandGold.copy(alpha = 0.35f)),
    ) {
        Text(
            text = text,
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
