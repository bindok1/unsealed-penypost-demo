package com.apps.unsealed.feature.storage.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** `POST /api/v1/storage/presign` request — see docs/be/api_contract.md's "storage" section. */
@JsonClass(generateAdapter = true)
data class PresignRequest(
    @Json(name = "file_name") val fileName: String,
    @Json(name = "content_type") val contentType: String,
)

/** [uploadUrl] is a short-lived R2 presigned PUT target; [publicUrl] is the
 * permanent CDN URL to reference afterward (e.g. as `composite_image_url` on
 * `POST /letters`, or `photo_url` on the profile endpoint). */
@JsonClass(generateAdapter = true)
data class PresignResponse(
    @Json(name = "upload_url") val uploadUrl: String,
    @Json(name = "public_url") val publicUrl: String,
    val key: String,
)
