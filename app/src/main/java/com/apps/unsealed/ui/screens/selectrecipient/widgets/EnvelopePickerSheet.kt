package com.apps.unsealed.ui.screens.selectrecipient.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeAspectRatio
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeDesign
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.BrandInkMid

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.apps.unsealed.ui.components.collectibles.CollectibleEmptyShowcaseCard
import com.apps.unsealed.ui.components.collectibles.CollectibleSectionHeader
import com.apps.unsealed.ui.components.collectibles.FindMoreCollectiblesCard
import com.apps.unsealed.ui.components.collectibles.sampleEnvelopeTeasers

private val ThumbnailWidth = 140.dp

/**
 * "Change Envelope" bottom sheet — features categorized sections for "Koleksi Dasar"
 * (standard envelopes) and "Koleksi Saya" (creator collectibles showcase preview with direct link to Peny Store).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvelopePickerSheet(
    envelopes: List<CatalogItemDto>,
    selectedEnvelope: CatalogItemDto,
    onEnvelopeSelect: (CatalogItemDto) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToStore: ((source: String) -> Unit)? = null,
) {
    val basicEnvelopes = remember(envelopes) { envelopes.filter { !it.isPremium } }
    val creatorEnvelopes = remember(envelopes) { envelopes.filter { it.isPremium } }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrandInkMid,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.envelope_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(18.dp))

            // ── Section 1: Koleksi Dasar ─────────────────────────────
            CollectibleSectionHeader(
                title = stringResource(R.string.picker_category_basic),
                countText = stringResource(R.string.picker_collectibles_count, basicEnvelopes.size),
                isDarkTheme = true,
            )
            Spacer(Modifier.height(10.dp))

            if (basicEnvelopes.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(basicEnvelopes, key = { it.id }) { item ->
                        EnvelopeThumbnail(
                            item = item,
                            isSelected = item.id == selectedEnvelope.id,
                            onClick = {
                                onEnvelopeSelect(item)
                                onDismiss()
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Section 2: Koleksi Saya (Kreator) ─────────────────────
            CollectibleSectionHeader(
                title = stringResource(R.string.picker_category_collectibles),
                countText = if (creatorEnvelopes.isNotEmpty()) stringResource(R.string.picker_collectibles_count, creatorEnvelopes.size) else null,
                isCreatorSection = true,
                isDarkTheme = true,
                actionText = if (creatorEnvelopes.isNotEmpty()) stringResource(R.string.picker_collectibles_find_more_short) else null,
                onActionClick = if (creatorEnvelopes.isNotEmpty()) {
                    {
                        onDismiss()
                        onNavigateToStore?.invoke("header_action")
                    }
                } else null,
            )
            Spacer(Modifier.height(10.dp))

            if (creatorEnvelopes.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(creatorEnvelopes, key = { it.id }) { item ->
                        EnvelopeThumbnail(
                            item = item,
                            isSelected = item.id == selectedEnvelope.id,
                            onClick = {
                                onEnvelopeSelect(item)
                                onDismiss()
                            },
                        )
                    }
                    item {
                        FindMoreCollectiblesCard(
                            onClick = {
                                onDismiss()
                                onNavigateToStore?.invoke("find_more_card")
                            },
                            title = stringResource(R.string.picker_collectibles_envelope_find_more_title),
                            subtitle = stringResource(R.string.picker_collectibles_find_more_short),
                            isDarkTheme = true,
                            modifier = Modifier
                                .width(ThumbnailWidth)
                                .aspectRatio(EnvelopeAspectRatio),
                        )
                    }
                }
            } else {
                CollectibleEmptyShowcaseCard(
                    teaserItems = sampleEnvelopeTeasers(),
                    title = stringResource(R.string.picker_collectibles_envelope_pitch),
                    description = stringResource(R.string.picker_collectibles_envelope_desc),
                    ctaText = stringResource(R.string.picker_collectibles_find_more_short),
                    onBuyCollectiblesClick = {
                        onDismiss()
                        onNavigateToStore?.invoke("empty_showcase_cta")
                    },
                    isDarkTheme = true,
                )
            }

            Spacer(Modifier.height(12.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun EnvelopeThumbnail(
    item: CatalogItemDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    val shape = RoundedCornerShape(14.dp)

    val imageModel: Any = if (item.imageUrl.isNotBlank()) {
        item.imageUrl
    } else {
        EnvelopeDesign.fromApiString(item.id).drawableRes
    }

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .width(ThumbnailWidth)
            .aspectRatio(EnvelopeAspectRatio)
            .clip(shape)
            .background(BrandInkDeep)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) BrandGold else Color.White.copy(alpha = 0.15f),
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = item.name },
        contentAlignment = Alignment.BottomEnd,
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(BrandGold),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = BrandInkMid,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
