package com.apps.unsealed.ui.screens.stamps.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.unsealed.R
import com.apps.unsealed.ui.screens.stamps.constants.StoreFilterCategory
import com.apps.unsealed.ui.screens.stamps.state.CatalogItemUi
import com.apps.unsealed.ui.screens.stamps.state.ShowcaseItemUi
import com.apps.unsealed.ui.screens.stamps.state.StoreSection
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.NunitoFontFamily

/**
 * Root composable for the Peny Store tab. Uses a [LazyColumn] where each
 * catalog item renders via [ProductCard] — displaying the product's title,
 * price/buy CTA, creator, description, and directly showing the horizontally
 * scrollable list of its artworks (stamps, papers, stickers, envelopes).
 */
@Composable
fun PenyStoreSection(
    store: StoreSection,
    onProductTap: (String) -> Unit,
    onBuyProduct: (CatalogItemUi) -> Unit,
    onBuyShowcaseItem: (ShowcaseItemUi) -> Unit = {},
    onLikeClick: (String) -> Unit,
    onFilterSelect: (StoreFilterCategory) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Detect when scrolling near the end of the catalog to trigger pagination
    LaunchedEffect(listState, store.catalog.size, store.nextCursor, store.isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null &&
                    store.nextCursor != null &&
                    !store.isLoadingMore &&
                    lastIndex >= (listState.layoutInfo.totalItemsCount - 3).coerceAtLeast(0)
                ) {
                    onLoadMore()
                }
            }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier.fillMaxSize(),
    ) {

        // ── Showcase Banner Carousel ───────────────────────────────────────
        if (store.showcase.isNotEmpty()) {
            item {
                ShowcaseBannerCarousel(
                    items = store.showcase,
                    onItemClick = onProductTap,
                    onBuyClick = { if (!it.viewerOwns) onBuyShowcaseItem(it) },
                    isPurchasing = store.isPurchasing,
                    purchasingItemId = store.purchasingItemId,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // ── Filter Chips ────────────────────────────────────────────────────
        item {
            StoreFilterChips(
                selected = store.selectedFilter,
                onSelect = onFilterSelect,
            )
        }

        // ── Catalog skeleton (first load) ─────────────────────────────────
        if (store.isLoadingCatalog && store.catalog.isEmpty()) {
            item {
                StoreSkeletonSection(showBanner = store.showcase.isEmpty())
            }
        }

        // ── Catalog items ──────────────────────────────────────────────────
        val isPaperFilter = store.selectedFilter == StoreFilterCategory.PAPER
        val isEnvelopeFilter = store.selectedFilter == StoreFilterCategory.ENVELOPE

        itemsIndexed(store.catalog, key = { _, item -> item.id }) { index, item ->
            val isPurchasingThisItem = store.isPurchasing && store.purchasingItemId == item.id
            val isEnvelopeItem = isEnvelopeFilter || item.category.equals("envelope", ignoreCase = true)
            val localizedPrice = store.localizedPriceByTier[item.tier]

            when {
                // If envelope: full-width landscape thumbnail card with EnvelopeAspectRatio
                isEnvelopeItem -> {
                    LandscapeEnvelopeProductCard(
                        item = item,
                        onClick = { onProductTap(item.id) },
                        onBuyClick = { if (!item.viewerOwns) onBuyProduct(item) },
                        onLikeClick = { onLikeClick(item.id) },
                        isPurchasing = isPurchasingThisItem,
                        localizedPrice = localizedPrice,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // If user filters paper: full-width square thumbnail view with overlaid buy button, title, & desc
                isPaperFilter -> {
                    SquarePaperProductCard(
                        item = item,
                        onClick = { onProductTap(item.id) },
                        onBuyClick = { if (!item.viewerOwns) onBuyProduct(item) },
                        onLikeClick = { onLikeClick(item.id) },
                        isPurchasing = isPurchasingThisItem,
                        localizedPrice = localizedPrice,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // Visual variation every 3-4 cards: rounded rectangle banner card with overlaid buy button, title, & desc
                (index + 1) % 4 == 0 -> {
                    FeaturedThumbnailProductCard(
                        item = item,
                        onClick = { onProductTap(item.id) },
                        onBuyClick = { if (!item.viewerOwns) onBuyProduct(item) },
                        onLikeClick = { onLikeClick(item.id) },
                        isPurchasing = isPurchasingThisItem,
                        localizedPrice = localizedPrice,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // Standard product card directly showing horizontal scroll of artworks
                else -> {
                    ProductCard(
                        item = item,
                        onClick = { onProductTap(item.id) },
                        onBuyClick = { if (!item.viewerOwns) onBuyProduct(item) },
                        onLikeClick = { onLikeClick(item.id) },
                        isPurchasing = isPurchasingThisItem,
                        localizedPrice = localizedPrice,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // ── Infinite scroll loading indicator ──────────────────────────────
        if (store.isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = BrandGold,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        // ── Empty state ───────────────────────────────────────────────────
        if (!store.isLoadingCatalog && store.catalog.isEmpty() && store.errorMessage == null) {
            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.store_empty_catalog),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                )
            }
        }

        // ── Error state ───────────────────────────────────────────────────
        store.errorMessage?.let { error ->
            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = error,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                )
            }
        }
    }
}
