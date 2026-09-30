package com.apps.unsealed.feature.catalog.data

import com.apps.unsealed.core.network.ApiResponse
import retrofit2.http.GET

interface CatalogApi {
    /** Already live — see docs/be/api_contract.md. */
    @GET("api/v1/stamps")
    suspend fun getStamps(): ApiResponse<List<CatalogItemDto>>

    /** New endpoint the backend needs to build, mirroring `GET /stamps` exactly
     * (not `GET /stamps/catalog` from docs/be/stamps_api.md — that's the
     * separate premium-purchase store, a different concern). */
    @GET("api/v1/envelopes")
    suspend fun getEnvelopes(): ApiResponse<List<CatalogItemDto>>

    /** New endpoint the backend needs to build — see `docs/be/stickers_api.md`,
     * whole roadmap unbuilt as of `docs/envelope-sticker-spec.md`. */
    @GET("api/v1/stickers")
    suspend fun getStickers(): ApiResponse<List<CatalogItemDto>>
}
