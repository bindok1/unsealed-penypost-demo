package com.apps.unsealed.ui.screens.compose.widgets

import com.apps.unsealed.ui.screens.compose.state.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.theme.PaperTemplate

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import com.apps.unsealed.ui.components.collectibles.CollectibleEmptyShowcaseCard
import com.apps.unsealed.ui.components.collectibles.CollectibleSectionHeader
import com.apps.unsealed.ui.components.collectibles.samplePaperTeasers
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.SurfaceCream

import coil3.compose.AsyncImage
import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.ui.components.collectibles.FindMoreCollectiblesCard

import androidx.compose.material3.rememberModalBottomSheetState

private val ThumbnailWidth = 64.dp
private val ThumbnailHeight = 90.dp

/**
 * "Ganti Kertas" sheet (compose-screen-spec.md §7): features categorized sections
 * for "Koleksi Dasar" (8 classic textured templates) and "Koleksi Saya" (creator collectibles
 * showcase preview with direct link to Peny Store).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperPickerSheet(
    selectedTemplate: PaperTemplate?,
    selectedPaperUrl: String? = null,
    creatorPapers: List<CatalogItemDto> = emptyList(),
    onTemplateSelect: (PaperTemplate) -> Unit,
    onPaperUrlSelect: (String) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToStore: ((source: String) -> Unit)? = null,
) {
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
                text = stringResource(R.string.paper_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = BrandInkDeep,
                fontFamily = NunitoFontFamily,
            )

            Spacer(Modifier.height(18.dp))

            // ── Section 1: Koleksi Dasar ─────────────────────────────
            CollectibleSectionHeader(
                title = stringResource(R.string.picker_category_basic),
                countText = stringResource(R.string.picker_collectibles_count, PaperTemplate.entries.size),
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(PaperTemplate.entries) { template ->
                    PaperTemplateThumbnail(
                        template = template,
                        isSelected = template == selectedTemplate && selectedPaperUrl == null,
                        onClick = { onTemplateSelect(template) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Section 2: Koleksi Saya (Kreator) ─────────────────────
            CollectibleSectionHeader(
                title = stringResource(R.string.picker_category_collectibles),
                countText = if (creatorPapers.isNotEmpty()) stringResource(R.string.picker_collectibles_count, creatorPapers.size) else null,
                isCreatorSection = true,
                actionText = if (creatorPapers.isNotEmpty()) stringResource(R.string.picker_collectibles_find_more_short) else null,
                onActionClick = if (creatorPapers.isNotEmpty()) {
                    {
                        onDismiss()
                        onNavigateToStore?.invoke("header_action")
                    }
                } else null,
            )
            Spacer(Modifier.height(10.dp))

            if (creatorPapers.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(creatorPapers, key = { it.id }) { item ->
                        CreatorPaperThumbnail(
                            item = item,
                            isSelected = item.id == selectedPaperUrl,
                            onClick = {
                                onPaperUrlSelect(item.id)
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
                            title = stringResource(R.string.picker_collectibles_paper_find_more_title),
                            subtitle = stringResource(R.string.picker_collectibles_find_more_short),
                            modifier = Modifier
                                .size(width = 100.dp, height = ThumbnailHeight),
                        )
                    }
                }
            } else {
                // Showcase empty preview with CTA
                CollectibleEmptyShowcaseCard(
                    teaserItems = samplePaperTeasers(),
                    title = stringResource(R.string.picker_collectibles_paper_pitch),
                    description = stringResource(R.string.picker_collectibles_paper_desc),
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
private fun PaperTemplateThumbnail(
    template: PaperTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    val label = stringResource(template.labelRes)
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(width = ThumbnailWidth, height = ThumbnailHeight)
            .clip(shape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.1f),
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.BottomEnd,
    ) {
        Image(
            painter = painterResource(template.drawableRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun CreatorPaperThumbnail(
    item: CatalogItemDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(width = ThumbnailWidth, height = ThumbnailHeight)
            .clip(shape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) BrandInkDeep else Color.Black.copy(alpha = 0.12f),
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
            model = item.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(BrandInkDeep),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
