package com.apps.unsealed.feature.mailbox.data

import com.apps.unsealed.core.network.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface MailboxApi {
    @GET("api/v1/mailbox")
    suspend fun getMailbox(): ApiResponse<List<MailboxRoomDto>>

    @GET("api/v1/mailbox/{threadId}")
    suspend fun getThread(@Path("threadId") threadId: String): ApiResponse<List<MailboxLetterDto>>
}
