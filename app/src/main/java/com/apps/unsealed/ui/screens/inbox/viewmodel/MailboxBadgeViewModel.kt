package com.apps.unsealed.ui.screens.inbox.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.unsealed.feature.mailbox.data.MailboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * App-scoped (hiltViewModel() at [com.apps.unsealed.MainActivity]'s top level, not
 * per-screen) so the bottom nav Mailbox badge reflects unread mail as soon as the
 * app opens, not only after the user has visited the Mailbox tab at least once.
 */
@HiltViewModel
class MailboxBadgeViewModel @Inject constructor(
    private val mailboxRepository: MailboxRepository,
) : ViewModel() {

    val unreadCount: StateFlow<Int> = mailboxRepository.totalUnreadCount

    init {
        viewModelScope.launch { mailboxRepository.getMailbox() }
    }
}
