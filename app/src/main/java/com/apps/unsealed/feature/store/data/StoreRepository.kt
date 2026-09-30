package com.apps.unsealed.feature.store.data

import android.content.Context
import com.apps.unsealed.R
import com.apps.unsealed.core.network.toUserFacingMessage
import com.apps.unsealed.core.network.unwrap
import com.apps.unsealed.feature.auth.data.AuthResult
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val crashlytics: FirebaseCrashlytics,
    private val retrofit: Retrofit,
) {
    private val api: StoreApi by lazy { retrofit.create(StoreApi::class.java) }

    /** Fetches admin-pinned showcase banner items for the carousel header. */
    suspend fun getShowcase(): AuthResult<List<ShowcaseItemDto>> =
        runCatching {
            api.getShowcase().unwrap().items
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_generic), it)
            },
        )

    /**
     * Fetches a paginated catalog of PUBLISHED items.
     * @param category optional backend category filter (e.g. "stamp", "paper", "bundle").
     * @param after optional cursor string from the previous page's [CatalogResponseDto.nextCursor].
     */
    suspend fun getCatalog(
        category: String? = null,
        after: String? = null,
    ): AuthResult<CatalogResponseDto> =
        runCatching {
            api.getCatalog(category = category, after = after).unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_generic), it)
            },
        )

    /** Fetches full item detail including assets and creator profile. */
    suspend fun getItemDetail(id: String): AuthResult<ItemDetailDto> =
        runCatching {
            api.getItemDetail(id).unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_generic), it)
            },
        )

    /**
     * Toggles the current user's like on an item.
     * Returns updated [LikeToggleDto] with server-confirmed has_liked and likes_count.
     */
    suspend fun toggleLike(id: String): AuthResult<LikeToggleDto> =
        runCatching {
            api.toggleLike(id).unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_generic), it)
            },
        )

    /**
     * Increments the view counter for an item. Fire-and-forget — launched on [Dispatchers.IO]
     * and not awaited by callers. Backend processes this non-blocking.
     */
    fun incrementViewAsync(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { api.incrementView(id) }
        }
    }

    /**
     * Fetches the current user's owned marketplace items (Peny Store).
     */
    suspend fun getMyInventory(): AuthResult<List<OwnedItemDto>> =
        runCatching {
            api.getMyInventory().unwrap().items
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_generic), it)
            },
        )
}
