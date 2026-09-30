package com.apps.unsealed.feature.addressbook.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Request body for `POST /api/v1/address-book` — see `docs/be/address_book_api.md`. */
@JsonClass(generateAdapter = true)
data class AddToAddressBookRequest(
    @Json(name = "penpal_user_id") val penpalUserId: String,
    @Json(name = "label") val label: String? = null,
)

/** `data` payload of `POST /api/v1/address-book`'s `200` response. */
@JsonClass(generateAdapter = true)
data class AddToAddressBookResponseData(
    @Json(name = "id") val id: String,
)

/** One contact row in `GET /api/v1/address-book` response list. */
@JsonClass(generateAdapter = true)
data class AddressBookContactDto(
    @Json(name = "id") val id: String,
    @Json(name = "penpal_user_id") val penpalUserId: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "user_name") val userName: String? = null,
    @Json(name = "continent") val continent: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "photo_url") val photoUrl: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "is_online") val isOnline: Boolean? = null,
    @Json(name = "last_active_at") val lastActiveAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
) {
    val targetUserId: String get() = penpalUserId ?: userId ?: id
    val displayName: String get() = nickname ?: userName ?: "Penpal"
    val location: String get() = continent ?: country ?: "Global"
}
