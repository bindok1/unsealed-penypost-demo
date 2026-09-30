package com.apps.unsealed.feature.store.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ShowcaseResponseDto(
    @Json(name = "items") val items: List<ShowcaseItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ShowcaseItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "sub_description") val subDescription: String = "",
    @Json(name = "description") val description: String = "",
    @Json(name = "showcase_banner_url") val showcaseBannerUrl: String = "",
    @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
    @Json(name = "tier") val tier: String,
    @Json(name = "category") val category: String,
    @Json(name = "creator_name") val creatorName: String = "",
    @Json(name = "price_idr") val priceIdr: Long = 0,
    @Json(name = "price_usd") val priceUsd: Double = 0.0,
    @Json(name = "viewer_owns") val viewerOwns: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class CatalogResponseDto(
    @Json(name = "items") val items: List<CatalogItemDto> = emptyList(),
    @Json(name = "next_cursor") val nextCursor: String? = null,
)

@JsonClass(generateAdapter = true)
data class CatalogItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "sub_description") val subDescription: String = "",
    @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
    @Json(name = "category") val category: String,
    @Json(name = "tier") val tier: String,
    @Json(name = "price_idr") val priceIdr: Long = 0,
    @Json(name = "price_usd") val priceUsd: Double = 0.0,
    @Json(name = "creator_id") val creatorId: String,
    @Json(name = "creator_name") val creatorName: String,
    @Json(name = "stamps_count") val stampsCount: Int = 0,
    @Json(name = "stickers_count") val stickersCount: Int = 0,
    @Json(name = "papers_count") val papersCount: Int = 0,
    @Json(name = "envelopes_count") val envelopesCount: Int = 0,
    @Json(name = "likes_count") val likesCount: Long = 0,
    @Json(name = "viewer_has_liked") val viewerHasLiked: Boolean = false,
    @Json(name = "viewer_owns") val viewerOwns: Boolean = false,
    @Json(name = "assets") val assets: List<ItemAssetDto> = emptyList(),
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class PublicCreatorDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "avatar_url") val avatarUrl: String = "",
    @Json(name = "bio") val bio: String = "",
    @Json(name = "external_link") val externalLink: String = "",
)

@JsonClass(generateAdapter = true)
data class ItemAssetDto(
    @Json(name = "asset_type") val assetType: String, // "STAMP" | "PAPER" | "STICKER" | "ENVELOPE"
    @Json(name = "asset_url") val assetUrl: String,
    @Json(name = "sort_order") val sortOrder: Int = 0,
)

@JsonClass(generateAdapter = true)
data class ItemDetailDto(
    @Json(name = "id") val id: String,
    @Json(name = "creator_id") val creatorId: String,
    @Json(name = "creator_name") val creatorName: String,
    @Json(name = "creator") val creator: PublicCreatorDto,
    @Json(name = "category") val category: String,
    @Json(name = "tier") val tier: String,
    @Json(name = "price_idr") val priceIdr: Long = 0,
    @Json(name = "price_usd") val priceUsd: Double = 0.0,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "sub_description") val subDescription: String = "",
    @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
    @Json(name = "showcase_banner_url") val showcaseBannerUrl: String = "",
    @Json(name = "stamps_count") val stampsCount: Int = 0,
    @Json(name = "stickers_count") val stickersCount: Int = 0,
    @Json(name = "papers_count") val papersCount: Int = 0,
    @Json(name = "envelopes_count") val envelopesCount: Int = 0,
    @Json(name = "views_count") val viewsCount: Long = 0,
    @Json(name = "likes_count") val likesCount: Long = 0,
    @Json(name = "viewer_has_liked") val viewerHasLiked: Boolean = false,
    @Json(name = "viewer_owns") val viewerOwns: Boolean = false,
    @Json(name = "assets") val assets: List<ItemAssetDto> = emptyList(),
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class LikeToggleDto(
    @Json(name = "has_liked") val hasLiked: Boolean,
    @Json(name = "likes_count") val likesCount: Long,
)

@JsonClass(generateAdapter = true)
data class UserInventoryResponseDto(
    @Json(name = "items") val items: List<OwnedItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class OwnedItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
    @Json(name = "category") val category: String = "",
    @Json(name = "creator_name") val creatorName: String? = null,
    @Json(name = "unlocked_via") val unlockedVia: String = "",
    @Json(name = "unlocked_at") val unlockedAt: String = "",
    @Json(name = "assets") val assets: List<ItemAssetDto> = emptyList(),
)
