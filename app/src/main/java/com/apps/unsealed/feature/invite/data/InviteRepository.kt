package com.apps.unsealed.feature.invite.data

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
class InviteRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashlytics: FirebaseCrashlytics,
    private val retrofit: Retrofit,
) {
    private val api: InviteApi by lazy { retrofit.create(InviteApi::class.java) }

    /** Generates a fresh invite code — see [InviteApi.generateInvite]. */
    suspend fun generateInvite(): AuthResult<GenerateInviteResponseData> =
        runCatching {
            api.generateInvite().unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_invite_generate), it)
            },
        )

    /** Redeems [code] — see [InviteApi.redeemInvite]. */
    suspend fun redeemInvite(code: String): AuthResult<RedeemInviteResponseData> =
        runCatching {
            api.redeemInvite(code).unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_invite_redeem), it)
            },
        )
}
