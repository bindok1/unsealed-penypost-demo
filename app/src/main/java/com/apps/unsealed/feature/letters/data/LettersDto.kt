package com.apps.unsealed.feature.letters.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SendLetterRequest(
    @Json(name = "recipient_id") val recipientId: String,
    @Json(name = "dear_name") val dearName: String,
    @Json(name = "body_text") val bodyText: String,
    val envelope: String,
    val stamp: String,
    @Json(name = "paper_template") val paperTemplate: String,
    @Json(name = "paper_color") val paperColor: String,
    @Json(name = "font_id") val fontId: String,
    @Json(name = "composite_image_url") val compositeImageUrl: String? = null,
    @Json(name = "crisis_tag") val crisisTag: String? = null,
    val visibility: String = "private",
    @Json(name = "sticker_data") val stickerData: List<String> = emptyList(),
    @Json(name = "envelope_sticker_id") val envelopeStickerId: String? = null,
    @Json(name = "envelope_composite_image_url") val envelopeCompositeImageUrl: String? = null,
    @Json(name = "paper_url") val paperUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class SendLetterResponse(
    val id: String,
    val status: String,
    @Json(name = "estimated_arrival_at") val estimatedArrivalAt: String,
)

/** Shape of one item in `GET /letters/inbox` and `GET /letters/sent`. */
@JsonClass(generateAdapter = true)
data class LetterListItemDto(
    val id: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "sender_name") val senderName: String,
    @Json(name = "sender_continent") val senderContinent: String,
    @Json(name = "recipient_id") val recipientId: String? = null,
    @Json(name = "recipient_name") val recipientName: String? = null,
    @Json(name = "posted_at") val postedAt: String,
    @Json(name = "delivered_at") val deliveredAt: String?,
    @Json(name = "estimated_arrival_at") val estimatedArrivalAt: String,
    val stamp: String,
    val envelope: String,
    @Json(name = "is_unread") val isUnread: Boolean,
    @Json(name = "delivery_status") val deliveryStatus: String,
)

@JsonClass(generateAdapter = true)
data class LetterListResponse(
    val items: List<LetterListItemDto>,
    @Json(name = "next_cursor") val nextCursor: String?,
)

/** `GET /letters/{id}` — same fields as [LetterListItemDto] plus full content. */
@JsonClass(generateAdapter = true)
data class LetterDetailDto(
    val id: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "sender_name") val senderName: String,
    @Json(name = "sender_continent") val senderContinent: String,
    @Json(name = "recipient_id") val recipientId: String? = null,
    @Json(name = "recipient_name") val recipientName: String? = null,
    @Json(name = "posted_at") val postedAt: String,
    @Json(name = "delivered_at") val deliveredAt: String?,
    @Json(name = "estimated_arrival_at") val estimatedArrivalAt: String,
    val stamp: String,
    val envelope: String,
    @Json(name = "is_unread") val isUnread: Boolean,
    @Json(name = "delivery_status") val deliveryStatus: String,
    @Json(name = "dear_name") val dearName: String,
    @Json(name = "body_text") val bodyText: String,
    @Json(name = "composite_image_url") val compositeImageUrl: String?,
    @Json(name = "envelope_sticker_id") val envelopeStickerId: String? = null,
    @Json(name = "envelope_composite_image_url") val envelopeCompositeImageUrl: String? = null,
    @Json(name = "paper_url") val paperUrl: String? = null,
)

/** `POST /letters/{id}/unlock` success response — same full shape as
 * [LetterDetailDto] plus `energy_remaining`, but the client only needs the
 * energy count (it re-fetches via `getLetter` for the now-delivered state
 * instead of duplicating the letter-mapping logic for this one-off call) —
 * Moshi silently ignores the other JSON fields it doesn't declare here. */
@JsonClass(generateAdapter = true)
data class UnlockLetterResponse(
    @Json(name = "energy_remaining") val energyRemaining: Int,
)

/** Error body shape specific to unlock's `400 "not enough energy"` case —
 * see `docs/be/letters_api.md`. Not the generic `ApiResponse` error shape
 * (which has no `meta`), so parsed separately in `LettersRepository`. */
@JsonClass(generateAdapter = true)
data class UnlockErrorBody(
    val success: Boolean,
    val error: String?,
    val meta: UnlockErrorMeta?,
)

@JsonClass(generateAdapter = true)
data class UnlockErrorMeta(
    val required: Int,
    val available: Int,
)

/** One item in `GET /users/{id}/showcase` or `GET /letters/me/showcase` —
 * see `docs/be/letters_api.md`'s "Postal Collection Showcase". Deliberately
 * excludes `recipient_id`/`dear_name` (not the public/owner's business who
 * this was sent to) — only the craft (envelope/paper/stamp) plus whatever
 * content is already baked into the composite images. */
@JsonClass(generateAdapter = true)
data class ShowcaseItemDto(
    val id: String,
    val envelope: String,
    @Json(name = "envelope_composite_image_url") val envelopeCompositeImageUrl: String?,
    @Json(name = "paper_template") val paperTemplate: String,
    @Json(name = "composite_image_url") val compositeImageUrl: String?,
    val stamp: String,
    @Json(name = "posted_at") val postedAt: String,
    /** Only present on `GET /letters/me/showcase` (owner-only management
     * view) — `null` on the public `GET /users/{id}/showcase` response,
     * which is always implicitly `"visible"` since that's the only status
     * it ever returns. */
    @Json(name = "showcase_status") val showcaseStatus: String? = null,
    /** Only sent by the backend when [compositeImageUrl] is `null` (sender's
     * device skipped compositing, see `docs/be/device_capability_tiering.md`)
     * — see `docs/be/letters_api.md` §"Update (2026-08-18)". Otherwise the
     * image already represents the content, so this stays `null`. */
    @Json(name = "body_text") val bodyText: String? = null,
)

@JsonClass(generateAdapter = true)
data class ShowcaseListResponse(
    val items: List<ShowcaseItemDto>,
    @Json(name = "next_cursor") val nextCursor: String?,
)

/** `PATCH /letters/{id}/showcase` request body. [status] must be one of
 * `"visible"`/`"paused"`/`"deleted"` — see [ShowcaseStatus]. */
@JsonClass(generateAdapter = true)
data class ShowcaseStatusRequest(
    val status: String,
)

/** Mirrors the backend's `showcase_status` enum (`docs/be/letters_api.md`).
 * "Delete" is a soft un-list (`"deleted"`), not a hard delete — the
 * underlying letter and mailbox thread are untouched. */
enum class ShowcaseStatus(val apiValue: String) {
    VISIBLE("visible"),
    PAUSED("paused"),
    DELETED("deleted"),
}
