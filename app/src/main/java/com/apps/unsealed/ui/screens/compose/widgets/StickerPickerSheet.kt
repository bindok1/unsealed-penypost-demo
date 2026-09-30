package com.apps.unsealed.ui.screens.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.ui.theme.BrandInk
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.SurfaceCardLight
import com.apps.unsealed.ui.theme.SurfaceCream

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.apps.unsealed.ui.components.collectibles.CollectibleEmptyShowcaseCard
import com.apps.unsealed.ui.components.collectibles.CollectibleSectionHeader
import com.apps.unsealed.ui.components.collectibles.FindMoreCollectiblesCard
import com.apps.unsealed.ui.components.collectibles.sampleStickerTeasers
import com.apps.unsealed.ui.theme.BrandGold

/**
 * Sticker picker bottom sheet for ComposeScreen — features categorized sections
 * for "Koleksi Dasar" (built-in stickers) and "Koleksi Saya" (creator collectibles
 * showcase preview with direct link to Peny Store).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPickerSheet(
    stickers: List<CatalogItemDto>,
    onStickerSelect: (CatalogItemDto) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToStore: ((source: String) -> Unit)? = null,
) {
    val basicStickers = remember(stickers) { stickers.filter { !it.isPremium } }
    val creatorStickers = remember(stickers) { stickers.filter { it.isPremium } }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCream,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.sticker_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = BrandInkDeep,
                fontFamily = NunitoFontFamily,
            )

            Spacer(Modifier.height(18.dp))

            // ── Section 1: Koleksi Dasar ─────────────────────────────
            CollectibleSectionHeader(
                title = stringResource(R.string.picker_category_basic),
                countText = stringResource(R.string.picker_collectibles_count, basicStickers.size),
            )
            Spacer(Modifier.height(10.dp))

            if (basicStickers.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(basicStickers, key = { it.id }) { sticker ->
                        StickerItemCard(
                            sticker = sticker,
                            onClick = { onStickerSelect(sticker) },
                        )
                    }
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
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(creatorStickers, key = { it.id }) { sticker ->
                        StickerItemCard(
                            sticker = sticker,
                            isCreatorCollectible = true,
                            onClick = { onStickerSelect(sticker) },
                        )
                    }
                    item {
                        FindMoreCollectiblesCard(
                            onClick = {
                                onDismiss()
                                onNavigateToStore?.invoke("find_more_card")
                            },
                            title = stringResource(R.string.picker_collectibles_sticker_find_more_title),
                            subtitle = stringResource(R.string.picker_collectibles_find_more_short),
                            modifier = Modifier
                                .width(96.dp)
                                .aspectRatio(0.85f),
                        )
                    }
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

@Composable
private fun StickerItemCard(
    sticker: CatalogItemDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCreatorCollectible: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .width(96.dp)
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCardLight)
            .border(
                width = if (isCreatorCollectible) 1.5.dp else 1.dp,
                color = if (isCreatorCollectible) BrandGold else Color(0xFF1F2937).copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AsyncImage(
                model = sticker.imageUrl,
                contentDescription = sticker.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(52.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = sticker.name,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = BrandInkDeep,
                maxLines = 1,
            )
        }
    }
}
