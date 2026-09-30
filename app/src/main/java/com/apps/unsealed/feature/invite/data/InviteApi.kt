package com.apps.unsealed.feature.invite.data

import com.apps.unsealed.core.network.ApiResponse
import retrofit2.http.POST
import retrofit2.http.Path

interface InviteApi {

    /**
     * Generates a new invite code for the viewer — see `docs/be_updet/invite_friend_api.md` §2
     * `POST /api/v1/invites`.
     */
    @POST("api/v1/invites")
    suspend fun generateInvite(): ApiResponse<GenerateInviteResponseData>

    /**
     * Redeems [code], connecting the viewer and the code's owner as address-book contacts —
     * see `docs/be_updet/invite_friend_api.md` §2 `POST /api/v1/invites/{code}/redeem`.
     */
    @POST("api/v1/invites/{code}/redeem")
    suspend fun redeemInvite(@Path("code") code: String): ApiResponse<RedeemInviteResponseData>
}
