package com.apps.unsealed.feature.penpals.data

import android.content.Context
import com.apps.unsealed.R
import com.apps.unsealed.core.network.toUserFacingMessage
import com.apps.unsealed.core.network.unwrap
import com.apps.unsealed.feature.auth.data.AuthResult
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException
import retrofit2.Retrofit

@Singleton
class PenpalsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashlytics: FirebaseCrashlytics,
    private val retrofit: Retrofit,
) {
    private val api: PenpalsApi by lazy { retrofit.create(PenpalsApi::class.java) }

    suspend fun getFeed(
        cursor: String? = null,
        limit: Int? = null,
        region: String? = null,
    ): AuthResult<FeedResponse> =
        runCatching { api.getFeed(cursor, limit, region).unwrap() }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_penpals_feed), it)
            },
        )

    suspend fun toggleLike(id: String): AuthResult<LikeResponse> =
        runCatching { api.toggleLike(id).unwrap() }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_penpals_like), it)
            },
        )

    suspend fun getRegions(): AuthResult<List<RegionCountDto>> =
        runCatching { api.getRegions().unwrap() }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_penpals_regions), it)
            },
        )

    suspend fun matchPenpal(region: String): AuthResult<MatchResponse> =
        runCatching { api.matchPenpal(MatchRequest(region)).unwrap() }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_penpals_match), it)
            },
        )

    /**
     * Rerolls today's stack early — see [PenpalsApi.refreshFeed]. The 400
     * "not enough energy" case comes back as `{"error": {"code": "INSUFFICIENT_ENERGY", ...}}`,
     * which doesn't fit the rest of the app's `error: String` envelope
     * ([com.apps.unsealed.core.network.ApiResponse]), so it's detected from
     * the raw error body here rather than via the normal [unwrap] path.
     */
    suspend fun refreshFeed(): AuthResult<RefreshFeedResponseData> =
        runCatching { api.refreshFeed().unwrap() }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                val message = if (it.isInsufficientEnergyError()) {
                    context.getString(R.string.error_penpals_refresh_insufficient_energy)
                } else {
                    it.toUserFacingMessage(context, R.string.error_penpals_refresh)
                }
                AuthResult.Error(message, it)
            },
        )

    private fun Throwable.isInsufficientEnergyError(): Boolean {
        if (this !is HttpException) return false
        val rawBody = runCatching { response()?.errorBody()?.string() }.getOrNull() ?: return false
        return rawBody.contains("INSUFFICIENT_ENERGY")
    }
}
