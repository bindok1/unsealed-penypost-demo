package com.apps.unsealed.feature.mailbox.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** One room in `GET /mailbox` — one per correspondent, `data` is a bare array. */
@JsonClass(generateAdapter = true)
data class MailboxRoomDto(
    @Json(name = "correspondent_id") val correspondentId: String,
    @Json(name = "correspondent_name") val correspondentName: String,
    @Json(name = "correspondent_continent") val correspondentContinent: String,
    @Json(name = "last_body_text") val lastBodyText: String,
    @Json(name = "last_sent_at") val lastSentAt: String,
    @Json(name = "last_status") val lastStatus: String,
    @Json(name = "last_estimated_arrival_at") val lastEstimatedArrivalAt: String,
    @Json(name = "last_stamp") val lastStamp: String,
    @Json(name = "last_envelope") val lastEnvelope: String,
    @Json(name = "last_composite_image_url") val lastCompositeImageUrl: String?,
    @Json(name = "unread_count") val unreadCount: Int,
)

/** One letter in `GET /mailbox/{threadId}` — `data` is a bare array, `sent_at ASC`. */
@JsonClass(generateAdapter = true)
data class MailboxLetterDto(
    val id: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "body_text") val bodyText: String,
    val status: String,
    @Json(name = "sent_at") val sentAt: String,
    @Json(name = "estimated_arrival_at") val estimatedArrivalAt: String,
    val stamp: String,
    val envelope: String,
    @Json(name = "composite_image_url") val compositeImageUrl: String?,
    @Json(name = "paper_url") val paperUrl: String? = null,
    @Json(name = "delivered_at") val deliveredAt: String?,
    @Json(name = "read_at") val readAt: String?,
)
