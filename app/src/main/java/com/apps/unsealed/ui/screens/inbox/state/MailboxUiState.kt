package com.apps.unsealed.ui.screens.inbox.state

sealed class MailboxUiState {
    object Loading : MailboxUiState()
    data class Success(val rooms: List<MailboxRoomItem>) : MailboxUiState()
    data class Error(val message: String) : MailboxUiState()
}
