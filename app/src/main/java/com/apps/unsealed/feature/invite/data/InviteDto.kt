package com.apps.unsealed.feature.invite.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** `data` payload of `POST /api/v1/invites`'s `201` response. */
@JsonClass(generateAdapter = true)
data class GenerateInviteResponseData(
    @Json(name = "code") val code: String,
    @Json(name = "deep_link_url") val deepLinkUrl: String,
)

/** `data` payload of `POST /api/v1/invites/{code}/redeem`'s `200` response. */
@JsonClass(generateAdapter = true)
data class RedeemInviteResponseData(
    @Json(name = "inviter_user_id") val inviterUserId: String,
    @Json(name = "inviter_name") val inviterName: String,
)
