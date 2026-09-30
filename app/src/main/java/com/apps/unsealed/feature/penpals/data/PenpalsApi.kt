package com.apps.unsealed.feature.penpals.data

import com.apps.unsealed.core.network.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PenpalsApi {

    /**
     * Paginated public penpal feed.
     *
     * @param cursor  Opaque keyset cursor for next-page (null = first page).
     * @param limit   Max items per page (server default ~20).
     * @param region  Optional filter — one of the 7 [PenpalRegion.apiLabel] values,
     *                e.g. `"Asia"`, `"North America"`. Null = all regions.
     */
    @GET("api/v1/penpals/feed")
    suspend fun getFeed(
        @Query("cursor") cursor: String? = null,
        @Query("limit")  limit: Int? = null,
        @Query("region") region: String? = null,
    ): ApiResponse<FeedResponse>

    /**
     * Toggle like on a feed post. Idempotent per (user, letter) — if the viewer
     * has already liked, this call unlikes; otherwise it likes.
     */
    @POST("api/v1/penpals/feed/{id}/like")
    suspend fun toggleLike(
        @Path("id") id: String,
    ): ApiResponse<LikeResponse>

    /** Per-region penpal counts (real users + seed accounts). */
    @GET("api/v1/penpals/regions")
    suspend fun getRegions(): ApiResponse<List<RegionCountDto>>

    /**
     * Resolves a region into one concrete recipient to start a new
     * conversation with — new endpoint, distinct from [getFeed] (public,
     * paginated, browse-oriented). Server picks e.g. a random/least-recently-
     * matched eligible user in that region, excluding the caller.
     */
    @POST("api/v1/penpals/match")
    suspend fun matchPenpal(@Body body: MatchRequest): ApiResponse<MatchResponse>

    /**
     * Rerolls the viewer's today stack early, paid for with energy — no daily
     * limit, energy itself is the natural rate limiter. No request body.
     * See `docs/be/daily_penpal_stack.md` §6. 400 with `error.code ==
     * "INSUFFICIENT_ENERGY"` when the viewer can't afford it.
     */
    @POST("api/v1/penpals/feed/refresh")
    suspend fun refreshFeed(): ApiResponse<RefreshFeedResponseData>
}
