package com.apps.unsealed.feature.peny.data

import com.apps.unsealed.core.network.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PenyApi {

    @GET("api/v1/peny/status")
    suspend fun getStatus(): ApiResponse<PenyStatusDto>

    @POST("api/v1/peny/reply")
    suspend fun reply(@Body request: PenyReplyRequest): ApiResponse<PenyReplyResponseData>
}
