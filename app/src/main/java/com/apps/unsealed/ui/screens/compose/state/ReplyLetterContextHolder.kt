package com.apps.unsealed.ui.screens.compose.state

import com.apps.unsealed.ui.screens.penpals.state.PenpalLetter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton holder for the original letter context when a user taps "Balas Surat" /
 * "Mulai Percakapan" from PenPals feed or Mailbox, so [com.apps.unsealed.ui.screens.compose.screen.ComposeScreen]
 * can show a floating sticky bubble and let the user inspect the original letter anytime.
 */
@Singleton
class ReplyLetterContextHolder @Inject constructor() {
    private val _activeReplyLetter = MutableStateFlow<PenpalLetter?>(null)
    val activeReplyLetter: StateFlow<PenpalLetter?> = _activeReplyLetter.asStateFlow()

    fun setReplyLetter(letter: PenpalLetter?) {
        _activeReplyLetter.value = letter
    }

    fun clear() {
        _activeReplyLetter.value = null
    }
}
