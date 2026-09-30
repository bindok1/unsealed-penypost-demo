package com.apps.unsealed.feature.catalog.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** One entry in `GET /api/v1/stamps` or `GET /api/v1/envelopes` — a
 * CMS-managed design an admin can add without an app release. Same shape for
 * both catalogs (see docs/be/api_contract.md's `GET /stamps`, which
 * `GET /envelopes` mirrors). [id] is what's sent as the letter's `envelope`/
 * `stamp` field on `POST /letters` — for the 5/6 designs bundled in the APK
 * today it's the legacy enum name (`"ENVELOPE_2"`, `"OCEAN"`) so existing
 * letters keep resolving; CMS-added designs get their own ids. */
@JsonClass(generateAdapter = true)
data class CatalogItemDto(
    val id: String,
    val name: String = "",
    @Json(name = "image_url") val imageUrl: String = "",
    @Json(name = "is_premium") val isPremium: Boolean = false,
    val orientation: String = "portrait",
    @Json(name = "creator_name") val creatorName: String? = null,
) {
    val isLandscape: Boolean get() = orientation.equals("landscape", ignoreCase = true)
}
