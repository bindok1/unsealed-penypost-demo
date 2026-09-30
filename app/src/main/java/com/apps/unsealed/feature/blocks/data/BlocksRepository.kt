package com.apps.unsealed.feature.blocks.data

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

@Singleton
class BlocksRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashlytics: FirebaseCrashlytics,
    private val retrofit: Retrofit,
) {
    private val api: BlocksApi by lazy { retrofit.create(BlocksApi::class.java) }

    /** Blocks [userId] — see [BlocksApi.blockUser]. */
    suspend fun blockUser(userId: String): AuthResult<BlockUserResponseData> =
        runCatching {
            api.blockUser(BlockUserRequest(blockedUserId = userId)).unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_block_user), it)
            },
        )

    /** Unblocks [userId] — see [BlocksApi.unblockUser]. */
    suspend fun unblockUser(userId: String): AuthResult<Unit> =
        runCatching {
            val response = api.unblockUser(userId)
            if (!response.isSuccessful) error("Unblock failed: HTTP ${response.code()}")
        }.fold(
            onSuccess = { AuthResult.Success(Unit) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_unblock_user), it)
            },
        )

    /** Lists blocked users — see [BlocksApi.getBlockedUsers]. */
    suspend fun getBlockedUsers(): AuthResult<List<BlockedUserDto>> =
        runCatching {
            api.getBlockedUsers().unwrap().items
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_blocked_users_load), it)
            },
        )
}
