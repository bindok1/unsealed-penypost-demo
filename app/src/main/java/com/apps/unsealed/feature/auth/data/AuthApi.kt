package com.apps.unsealed.feature.auth.data

import com.apps.unsealed.core.network.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AuthApi {
    /**
     * Returns the signed-in user's profile wrapped in {"success":true,"data":{...}}.
     * 404 = user has never called /register (brand-new Firebase account).
     * Using [Response] wrapper so the caller can inspect the HTTP status code.
     */
    @GET("api/v1/auth/me")
    suspend fun getMe(): Response<ApiResponse<UserDto>>

    /** First-time registration. Only required once per Firebase account. */
    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<UserDto>

    /** Partial profile update — send only the fields you want to change. */
    @PATCH("api/v1/auth/me")
    suspend fun patchMe(@Body body: PatchMeRequest): ApiResponse<UserDto>

    /** Replaces the user's entire interest set (not additive). */
    @PUT("api/v1/auth/me/interests")
    suspend fun putInterests(@Body body: InterestsRequest): ApiResponse<UserDto>

    /** Full interest catalog — same for all users, used to render the picker. */
    @GET("api/v1/interests")
    suspend fun getInterests(): ApiResponse<List<InterestDto>>

    /** Replaces the user's entire spoken-languages set (not additive). */
    @PUT("api/v1/auth/me/languages")
    suspend fun putLanguages(@Body body: LanguagesRequest): ApiResponse<UserDto>

    /**
     * Fetches the public-facing profile of any user by their ID.
     *
     * - 200 → [PublicProfileDto] wrapped in the standard success envelope.
     * - 404 → user does not exist **or** a bidirectional block exists between
     *         the viewer and the target. The two cases are deliberately
     *         indistinguishable to avoid leaking block status to either party.
     *
     * Using [Response] wrapper so the caller can inspect the HTTP status code
     * (mirrors the pattern used by [getMe]).
     */
    @GET("api/v1/users/{id}")
    suspend fun getPublicProfile(
        @Path("id") userId: String,
    ): Response<ApiResponse<PublicProfileDto>>

    /**
     * Self-service account deletion (DELETE /api/v1/auth/me).
     * Soft-deletes the user profile on the backend (status = 'deleted').
     */
    @retrofit2.http.DELETE("api/v1/auth/me")
    suspend fun deleteMe(): Response<ApiResponse<Map<String, Any>>>
}

