package com.apps.unsealed.feature.blocks.data

import com.apps.unsealed.core.network.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BlocksApi {

    /** Blocks a user — see `docs/be/trust_and_safety_api.md` §2. */
    @POST("api/v1/blocks")
    suspend fun blockUser(@Body body: BlockUserRequest): ApiResponse<BlockUserResponseData>

    /**
     * Unblocks a user. Response `200` has no body per spec, so this returns a raw
     * [Response] instead of [ApiResponse] — the shared [ApiResponse]/`unwrap()` pattern
     * expects a non-null `data` field, which an empty body can't satisfy.
     */
    @DELETE("api/v1/blocks/{blockedUserId}")
    suspend fun unblockUser(@Path("blockedUserId") blockedUserId: String): Response<Unit>

    /** Lists blocked users — backs the "Blocked Users" settings screen. */
    @GET("api/v1/blocks")
    suspend fun getBlockedUsers(): ApiResponse<BlockedUsersResponseData>
}
