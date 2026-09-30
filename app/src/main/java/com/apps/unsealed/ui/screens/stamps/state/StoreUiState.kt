package com.apps.unsealed.ui.screens.stamps.state

import com.apps.unsealed.ui.screens.stamps.constants.StoreFilterCategory

/**
 * UI-layer domain models for the Peny Store tab.
 * These are mapped from [feature/store/data] DTOs and are the single source
 * of truth consumed by the composables — they contain no Retrofit/Moshi types.
 */

data class StoreSection(
    val isLoadingShowcase: Boolean = false,
    val isLoadingCatalog: Boolean = false,
    /** Admin-pinned banner items for the top carousel. */
    val showcase: List<ShowcaseItemUi> = emptyList(),
    /** Paginated catalog grid items. */
    val catalog: List<CatalogItemUi> = emptyList(),
    /** Cursor for the next page — null means end of catalog. */
    val nextCursor: String? = null,
    val selectedFilter: StoreFilterCategory = StoreFilterCategory.ALL,
    val isLoadingMore: Boolean = false,
    /** Non-null → ProductDetailBottomSheet is open for this item ID. */
    val selectedItemId: String? = null,
    val itemDetail: ItemDetailUi? = null,
    val isLoadingDetail: Boolean = false,
    val errorMessage: String? = null,
    /** True while RevenueCat purchasePackage() is in progress. */
    val isPurchasing: Boolean = false,
    /** Non-null item ID while RevenueCat purchasePackage() is in progress for that specific item. */
    val purchasingItemId: String? = null,
    /** True after a successful purchase — triggers the success dialog. */
    val showPurchaseSuccess: Boolean = false,
    /** Non-null when a purchase fails — shown in LetterlyTopSnackbar. */
    val purchaseError: String? = null,
    /** Set of item IDs owned by the current user. */
    val ownedItemIds: Set<String> = emptySet(),
    /**
     * Localized price strings fetched from RevenueCat, keyed by tier (e.g. "tier_1" → "$3.99").
     * Populated lazily when the STORE tab loads. Empty means RC prices haven't loaded yet —
     * the UI falls back to [CatalogItemUi.priceIdr] formatted as IDR in that case.
     */
    val localizedPriceByTier: Map<String, String> = emptyMap(),
)

// ─── Showcase ────────────────────────────────────────────────────────────────

data class ShowcaseItemUi(
    val id: String,
    val title: String,
    val subDescription: String,
    /** Full-bleed banner URL (16:9). Falls back to [thumbnailUrl] if blank. */
    val bannerUrl: String,
    val thumbnailUrl: String,
    val tier: String,
    val category: String,
    val creatorName: String,
    val priceIdr: Long,
    val priceUsd: Double,
    val viewerOwns: Boolean = false,
)

// ─── Catalog grid item ───────────────────────────────────────────────────────

data class CatalogItemUi(
    val id: String,
    val title: String,
    val subDescription: String,
    val thumbnailUrl: String,
    val category: String,
    val tier: String,
    val priceIdr: Long,
    val priceUsd: Double,
    val creatorId: String,
    val creatorName: String,
    val stampsCount: Int,
    val stickersCount: Int,
    val papersCount: Int,
    val envelopesCount: Int,
    val likesCount: Long,
    val viewerHasLiked: Boolean,
    val viewerOwns: Boolean = false,
    val assets: List<AssetUi> = emptyList(),
)

// ─── Item detail (bottom sheet) ───────────────────────────────────────────────

data class ItemDetailUi(
    val id: String,
    val title: String,
    val description: String,
    val subDescription: String,
    val thumbnailUrl: String,
    val bannerUrl: String,
    val tier: String,
    val priceIdr: Long,
    val priceUsd: Double,
    val category: String,
    val stampsCount: Int,
    val stickersCount: Int,
    val papersCount: Int,
    val envelopesCount: Int,
    val likesCount: Long,
    val viewerHasLiked: Boolean,
    val viewerOwns: Boolean,
    val assets: List<AssetUi>,
    val creator: CreatorUi,
)

data class AssetUi(
    /** "STAMP" | "PAPER" | "STICKER" | "ENVELOPE" */
    val assetType: String,
    val assetUrl: String,
    val sortOrder: Int,
)

data class CreatorUi(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val bio: String,
    val externalLink: String,
)
