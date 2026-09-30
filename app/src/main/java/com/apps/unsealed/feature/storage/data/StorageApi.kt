package com.apps.unsealed.feature.storage.data

import com.apps.unsealed.core.network.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface StorageApi {
    @POST("api/v1/storage/presign")
    suspend fun presign(@Body body: PresignRequest): ApiResponse<PresignResponse>
}
