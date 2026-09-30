package com.apps.unsealed.feature.blocks.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Request body for `POST /api/v1/blocks` — see `docs/be/trust_and_safety_api.md` §2. */
@JsonClass(generateAdapter = true)
data class BlockUserRequest(
    @Json(name = "blocked_user_id") val blockedUserId: String,
)

/** `data` payload of `POST /api/v1/blocks`'s `200` response. */
@JsonClass(generateAdapter = true)
data class BlockUserResponseData(
    @Json(name = "blocked_user_id") val blockedUserId: String,
)

/** One row of `GET /api/v1/blocks`'s `items` list — backs the "Blocked Users" settings screen. */
@JsonClass(generateAdapter = true)
data class BlockedUserDto(
    @Json(name = "user_id") val userId: String,
    @Json(name = "user_name") val userName: String,
    @Json(name = "blocked_at") val blockedAt: String,
)

/** `data` payload of `GET /api/v1/blocks`'s `200` response. */
@JsonClass(generateAdapter = true)
data class BlockedUsersResponseData(
    val items: List<BlockedUserDto>,
)
