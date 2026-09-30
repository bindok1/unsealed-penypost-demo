package com.apps.unsealed.feature.reports.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Request body for `POST /api/v1/reports` — see `docs/be/trust_and_safety_api.md` §1. */
@JsonClass(generateAdapter = true)
data class ReportRequest(
    @Json(name = "target_type") val targetType: String,
    @Json(name = "target_id")   val targetId: String,
    @Json(name = "reason")      val reason: String,
    @Json(name = "letter_id")   val letterId: String? = null,
    @Json(name = "note")        val note: String? = null,
)

/** `data` payload of `POST /api/v1/reports`'s `200` response. */
@JsonClass(generateAdapter = true)
data class ReportResponseData(
    @Json(name = "report_id") val reportId: String,
)
