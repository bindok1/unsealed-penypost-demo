package com.apps.unsealed.feature.penpals.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** One item in `GET /api/v1/penpals/feed`. */
@JsonClass(generateAdapter = true)
data class FeedItemDto(
    val id: String,
    @Json(name = "sender_id")        val senderId: String,
    @Json(name = "sender_name")      val senderName: String? = null,
    @Json(name = "sender_photo_url") val senderPhotoUrl: String? = null,
    val region: String? = null,
    val envelope: String? = null,
    val stamp: String? = null,
    @Json(name = "envelope_sticker_id")          val envelopeStickerId: String? = null,
    @Json(name = "envelope_composite_image_url") val envelopeCompositeImageUrl: String? = null,
    @Json(name = "composite_image_url")          val compositeImageUrl: String? = null,
    @Json(name = "body_text")        val bodyText: String? = null,
    @Json(name = "paper_url")        val paperUrl: String? = null,
    @Json(name = "posted_at")        val postedAt: String? = null,            // ISO 8601
    @Json(name = "like_count")       val likeCount: Int? = null,
    @Json(name = "viewer_has_liked") val viewerHasLiked: Boolean? = null,
    @Json(name = "is_online")        val isOnline: Boolean? = null,
)

/** Wrapper for paginated `GET /api/v1/penpals/feed` response. */
@JsonClass(generateAdapter = true)
data class FeedResponse(
    val items: List<FeedItemDto>,
    @Json(name = "next_cursor") val nextCursor: String?,
)

/** Response from `POST /api/v1/penpals/feed/{id}/like` (toggle). */
@JsonClass(generateAdapter = true)
data class LikeResponse(
    @Json(name = "like_count")       val likeCount: Int,
    @Json(name = "viewer_has_liked") val viewerHasLiked: Boolean,
)

/**
 * Response from `POST /api/v1/penpals/feed/refresh` — same item shape as
 * [FeedResponse] (stack mode) plus the energy ledger, see
 * `docs/be/daily_penpal_stack.md` §6.
 */
@JsonClass(generateAdapter = true)
data class RefreshFeedResponseData(
    val items: List<FeedItemDto>,
    @Json(name = "next_cursor") val nextCursor: String?,
    @Json(name = "energy_spent") val energySpent: Int,
    @Json(name = "energy_remaining") val energyRemaining: Int,
)

/** One item in `GET /api/v1/penpals/regions` response. */
@JsonClass(generateAdapter = true)
data class RegionCountDto(
    val region: String,
    val count: Int,
)

/** `POST /api/v1/penpals/match` request — "start a new conversation with
 * someone in this region," distinct from `GET /feed` (public, paginated,
 * for browsing) — see [PenpalsApi.matchPenpal]. */
@JsonClass(generateAdapter = true)
data class MatchRequest(val region: String)

@JsonClass(generateAdapter = true)
data class MatchResponse(
    @Json(name = "recipient_id") val recipientId: String,
    @Json(name = "recipient_name") val recipientName: String,
)
