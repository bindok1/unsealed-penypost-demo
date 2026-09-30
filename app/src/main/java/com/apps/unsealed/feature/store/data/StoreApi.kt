package com.apps.unsealed.feature.store.data

import com.apps.unsealed.core.network.ApiResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StoreApi {

    @GET("api/v1/shop/showcase")
    suspend fun getShowcase(): ApiResponse<ShowcaseResponseDto>

    @GET("api/v1/items")
    suspend fun getCatalog(
        @Query("category") category: String? = null,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<CatalogResponseDto>

    @GET("api/v1/items/{id}")
    suspend fun getItemDetail(
        @Path("id") id: String,
    ): ApiResponse<ItemDetailDto>

    @POST("api/v1/items/{id}/like")
    suspend fun toggleLike(
        @Path("id") id: String,
    ): ApiResponse<LikeToggleDto>

    @POST("api/v1/items/{id}/view")
    suspend fun incrementView(
        @Path("id") id: String,
    ): ApiResponse<Unit>

    @GET("api/v1/me/inventory")
    suspend fun getMyInventory(): ApiResponse<UserInventoryResponseDto>
}
