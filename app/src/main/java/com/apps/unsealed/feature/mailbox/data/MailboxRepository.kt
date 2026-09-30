package com.apps.unsealed.feature.mailbox.data

import android.content.Context
import com.apps.unsealed.R
import com.apps.unsealed.core.network.toUserFacingMessage
import com.apps.unsealed.core.network.unwrap
import com.apps.unsealed.feature.auth.data.AuthResult
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Retrofit

@Singleton
class MailboxRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashlytics: FirebaseCrashlytics,
    private val retrofit: Retrofit,
) {
    private val api: MailboxApi by lazy { retrofit.create(MailboxApi::class.java) }

    // Shared across every caller (screen-scoped MailboxViewModel and the
    // app-scoped nav-bar badge alike) since this repository is a @Singleton —
    // whichever caller fetches the mailbox first/most-recently keeps this
    // total in sync for the bottom nav badge without a separate poll.
    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount: StateFlow<Int> = _totalUnreadCount.asStateFlow()

    suspend fun getMailbox(): AuthResult<List<MailboxRoomDto>> =
        runCatching { api.getMailbox().unwrap() }.fold(
            onSuccess = {
                _totalUnreadCount.value = it.sumOf { room -> room.unreadCount }
                AuthResult.Success(it)
            },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_mailbox_list), it)
            },
        )

    suspend fun getThread(threadId: String): AuthResult<List<MailboxLetterDto>> =
        runCatching { api.getThread(threadId).unwrap() }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_mailbox_thread), it)
            },
        )
}
