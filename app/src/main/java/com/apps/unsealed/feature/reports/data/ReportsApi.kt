package com.apps.unsealed.feature.reports.data

import com.apps.unsealed.core.network.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ReportsApi {

    /**
     * Files a report against a user or a specific letter — see
     * `docs/be/trust_and_safety_api.md` §1. Idempotent per
     * (reporter_id, target_type, target_id) within 24h server-side.
     */
    @POST("api/v1/reports")
    suspend fun createReport(@Body body: ReportRequest): ApiResponse<ReportResponseData>
}
