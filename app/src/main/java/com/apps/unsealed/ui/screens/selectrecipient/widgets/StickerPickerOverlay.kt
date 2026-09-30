package com.apps.unsealed.ui.screens.selectrecipient.widgets

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apps.unsealed.R
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.ui.components.collectibles.CollectibleEmptyShowcaseCard
import com.apps.unsealed.ui.components.collectibles.CollectibleSectionHeader
import com.apps.unsealed.ui.components.collectibles.FindMoreCollectiblesCard
import com.apps.unsealed.ui.components.collectibles.sampleStickerTeasers
import com.apps.unsealed.ui.theme.BrandGold

private val StickerCardBorderSelected = Color(0xFF1F2937)

/** Shared-bounds key connecting the envelope's sticker-slot placeholder box
 * (`EnvelopeCard.kt`'s bottom-end anchor) to this overlay's outer panel —
 * mirrors [StampSheetSharedKey]'s relationship with the stamp corner box. */
internal const val StickerSheetSharedKey = "stickerSheetContainer"

/** Reuses [StampPickerOverlay]'s bespoke select-bounce spring shape, tuned
 * the same way — see that file's doc comment for the motion-rules.md ref. */
private val StickerSelectSpring = spring<Float>(dampingRatio = 0.3f, stiffness = 300f)
private val BouncyDpSpring = spring<Dp>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)

/**
 * "Add a Sticker" panel — features categorized sections for "Koleksi Dasar"
 * (built-in stickers) and "Koleksi Saya" (creator collectibles showcase preview with direct link to Peny Store).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.StickerPickerOverlay(
    stickers: List<CatalogItemDto>,
    selectedSticker: CatalogItemDto?,
    onStickerSelect: (CatalogItemDto?) -> Unit,
    onDismiss: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    onNavigateToStore: ((source: String) -> Unit)? = null,
) {
    val basicStickers = remember(stickers) { stickers.filter { !it.isPremium } }
    val creatorStickers = remember(stickers) { stickers.filter { it.isPremium } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        Surface(
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = StickerSheetSharedKey),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            with(animatedVisibilityScope) {
                Column(
                    Modifier
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                        .animateEnterExit(enter = fadeIn(), exit = fadeOut()),
                ) {
                    Text(
                        stringResource(R.string.sticker_picker_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(18.dp))

                    // ── Section 1: Koleksi Dasar ─────────────────────────────
                    CollectibleSectionHeader(
                        title = stringResource(R.string.picker_category_basic),
                        countText = stringResource(R.string.picker_collectibles_count, basicStickers.size),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        basicStickers.forEach { sticker ->
                            val isSelected = sticker.id == selectedSticker?.id
                            StickerCard(
                                sticker = sticker,
                                isSelected = isSelected,
                                onClick = { onStickerSelect(if (isSelected) null else sticker) },
                                modifier = Modifier.width(96.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Section 2: Koleksi Saya (Kreator) ─────────────────────
                    CollectibleSectionHeader(
                        title = stringResource(R.string.picker_category_collectibles),
                        countText = if (creatorStickers.isNotEmpty()) stringResource(R.string.picker_collectibles_count, creatorStickers.size) else null,
                        isCreatorSection = true,
                        actionText = if (creatorStickers.isNotEmpty()) stringResource(R.string.picker_collectibles_find_more_short) else null,
                        onActionClick = if (creatorStickers.isNotEmpty()) {
                            {
                                onDismiss()
                                onNavigateToStore?.invoke("header_action")
                            }
                        } else null,
                    )
                    Spacer(Modifier.height(10.dp))

                    if (creatorStickers.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            creatorStickers.forEach { sticker ->
                                val isSelected = sticker.id == selectedSticker?.id
                                StickerCard(
                                    sticker = sticker,
                                    isSelected = isSelected,
                                    isCreatorCollectible = true,
                                    onClick = { onStickerSelect(if (isSelected) null else sticker) },
                                    modifier = Modifier.width(96.dp),
                                )
                            }
                            FindMoreCollectiblesCard(
                                onClick = {
                                    onDismiss()
                                    onNavigateToStore?.invoke("find_more_card")
                                },
                                title = stringResource(R.string.picker_collectibles_sticker_find_more_title),
                                subtitle = stringResource(R.string.picker_collectibles_find_more_short),
                                modifier = Modifier
                                    .width(96.dp)
                                    .aspectRatio(0.78f),
                            )
                        }
                    } else {
                        CollectibleEmptyShowcaseCard(
                            teaserItems = sampleStickerTeasers(),
                            title = stringResource(R.string.picker_collectibles_sticker_pitch),
                            description = stringResource(R.string.picker_collectibles_sticker_desc),
                            ctaText = stringResource(R.string.picker_collectibles_find_more_short),
                            onBuyCollectiblesClick = {
                                onDismiss()
                                onNavigateToStore?.invoke("empty_showcase_cta")
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerCard(
    sticker: CatalogItemDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCreatorCollectible: Boolean = false,
) {
    val isReducedMotion = rememberIsReducedMotion()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(isSelected) {
        if (isSelected) {
            scale.animateTo(1.15f, reducedMotionSpring(StickerSelectSpring, isReducedMotion))
            scale.animateTo(1f, reducedMotionSpring(StickerSelectSpring, isReducedMotion))
        } else {
            scale.animateTo(1f, reducedMotionSpring(StickerSelectSpring, isReducedMotion))
        }
    }
    val borderWidth by animateDpAsState(
        targetValue = when {
            isSelected -> 2.dp
            isCreatorCollectible -> 1.5.dp
            else -> 1.dp
        },
        animationSpec = reducedMotionSpring(BouncyDpSpring, isReducedMotion),
        label = "stickerCardBorderWidth",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> StickerCardBorderSelected
            isCreatorCollectible -> BrandGold
            else -> StickerCardBorderSelected.copy(alpha = 0.15f)
        },
        label = "stickerCardBorderColor",
    )

    Box(
        modifier = modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(16.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StampImage(
                item = sticker,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
            )
            Spacer(Modifier.height(6.dp))
            Text(sticker.name, style = MaterialTheme.typography.labelSmall)
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.sticker_picker_remove_desc),
                tint = StickerCardBorderSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}
