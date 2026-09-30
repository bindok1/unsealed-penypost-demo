package com.apps.unsealed.feature.letters.data

import com.apps.unsealed.core.network.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LettersApi {
    @POST("api/v1/letters")
    suspend fun sendLetter(@Body body: SendLetterRequest): ApiResponse<SendLetterResponse>

    @GET("api/v1/letters/inbox")
    suspend fun getInbox(
        @Query("status") status: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = null,
    ): ApiResponse<LetterListResponse>

    @GET("api/v1/letters/{id}")
    suspend fun getLetter(@Path("id") id: String): ApiResponse<LetterDetailDto>

    @POST("api/v1/letters/{id}/unlock")
    suspend fun unlockLetter(@Path("id") id: String): ApiResponse<UnlockLetterResponse>

    /** Public Postal Collection Showcase — always `showcase_status = "visible"`
     * server-side, viewable both on the owner's own profile and anyone
     * else's view of it. See `docs/be/letters_api.md`. */
    @GET("api/v1/users/{id}/showcase")
    suspend fun getUserShowcase(
        @Path("id") userId: String,
        @Query("cursor") cursor: String? = null,
    ): ApiResponse<ShowcaseListResponse>

    /** Owner-only management view — unlike [getUserShowcase], not filtered to
     * `"visible"`, so paused/deleted items still show up (each with its own
     * `showcase_status`). Defaults to `"all"` (not a [ShowcaseStatus] value —
     * that enum only covers the 3 states a single item can be *set to* via
     * [updateShowcaseStatus], not this endpoint's broader query filter). */
    @GET("api/v1/letters/me/showcase")
    suspend fun getMyShowcase(
        @Query("status") status: String = "all",
        @Query("cursor") cursor: String? = null,
    ): ApiResponse<ShowcaseListResponse>

    /** Pause/delete/(re)show one showcase item — 403 if not the sender, 400
     * if the letter's `visibility` isn't `"public"`. */
    @PATCH("api/v1/letters/{id}/showcase")
    suspend fun updateShowcaseStatus(
        @Path("id") id: String,
        @Body body: ShowcaseStatusRequest,
    ): ApiResponse<ShowcaseItemDto>
}
