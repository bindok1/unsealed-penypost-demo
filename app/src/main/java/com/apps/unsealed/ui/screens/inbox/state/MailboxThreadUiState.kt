package com.apps.unsealed.ui.screens.inbox.state

sealed class MailboxThreadUiState {
    object Loading : MailboxThreadUiState()
    data class Success(
        val correspondentId: String,
        val correspondentName: String,
        val correspondentContinent: String,
        val letters: List<ThreadLetterItem>,
        val correspondentAvatarUrl: String? = null,
        val isBlocked: Boolean = false,
        /** Most recently touched local draft addressed to this correspondent,
         * if any — drives [com.apps.unsealed.ui.screens.inbox.widgets.WriteLetterButton]'s
         * "Continue Writing" state and, when tapped, resumes that draft
         * instead of starting a fresh letter. */
        val draftId: String? = null,
    ) : MailboxThreadUiState()
    data class Error(val message: String) : MailboxThreadUiState()
}

/** State of an in-flight/completed report against the thread's correspondent. */
sealed class ThreadReportState {
    object Idle : ThreadReportState()
    object Submitting : ThreadReportState()
    object Submitted : ThreadReportState()
    data class Error(val message: String) : ThreadReportState()
}

/** State of an in-flight/completed block/unblock against the thread's correspondent. */
sealed class ThreadBlockState {
    object Idle : ThreadBlockState()
    object Blocking : ThreadBlockState()
    object Blocked : ThreadBlockState()
    object Unblocking : ThreadBlockState()
    object Unblocked : ThreadBlockState()
    data class Error(val message: String) : ThreadBlockState()
}
