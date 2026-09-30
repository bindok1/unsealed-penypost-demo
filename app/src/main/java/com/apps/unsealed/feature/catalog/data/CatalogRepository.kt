package com.apps.unsealed.feature.catalog.data

import android.content.Context
import com.apps.unsealed.R
import com.apps.unsealed.core.network.toUserFacingMessage
import com.apps.unsealed.core.network.unwrap
import com.apps.unsealed.feature.auth.data.AuthResult
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Retrofit

/** Envelope/stamp designs, CMS-managed so an admin can add new ones without
 * an app release — see docs/be/api_contract.md's `GET /stamps` (already
 * live) and the new `GET /envelopes` it's mirrored by. Both cached
 * in-memory per process: pickers (Select Recipient) and renderers (Inbox,
 * PenPals) all resolve the same small list repeatedly in one session, and an
 * admin swapping art mid-session is an acceptable staleness window. */
@Singleton
class CatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashlytics: FirebaseCrashlytics,
    private val retrofit: Retrofit,
) {
    private val api: CatalogApi by lazy { retrofit.create(CatalogApi::class.java) }

    private var cachedEnvelopes: List<CatalogItemDto>? = null
    private var cachedStamps: List<CatalogItemDto>? = null
    private var cachedStickers: List<CatalogItemDto>? = null

    suspend fun getEnvelopes(): AuthResult<List<CatalogItemDto>> {
        cachedEnvelopes?.let { return AuthResult.Success(it) }
        return runCatching { api.getEnvelopes().unwrap() }.fold(
            onSuccess = { cachedEnvelopes = it; AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_catalog_envelopes), it)
            },
        )
    }

    suspend fun getStamps(): AuthResult<List<CatalogItemDto>> {
        cachedStamps?.let { return AuthResult.Success(it) }
        return runCatching { api.getStamps().unwrap() }.fold(
            onSuccess = { cachedStamps = it; AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_catalog_stamps), it)
            },
        )
    }

    suspend fun getStickers(forceRefresh: Boolean = false): AuthResult<List<CatalogItemDto>> {
        if (!forceRefresh) {
            cachedStickers?.let { return AuthResult.Success(it) }
        }
        return runCatching { api.getStickers().unwrap() }.fold(
            onSuccess = { cachedStickers = it; AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_catalog_stickers), it)
            },
        )
    }
}
