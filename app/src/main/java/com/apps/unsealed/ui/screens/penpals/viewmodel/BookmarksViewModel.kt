package com.apps.unsealed.ui.screens.penpals.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.unsealed.feature.bookmarks.data.BookmarksRepository
import com.apps.unsealed.feature.bookmarks.data.toPenpalLetter
import com.apps.unsealed.ui.screens.penpals.state.PenpalLetter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val repository: BookmarksRepository,
) : ViewModel() {

    val bookmarkedLetters: StateFlow<List<PenpalLetter>> = repository.observeAll()
        .map { entities -> entities.map { it.toPenpalLetter() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedIds: StateFlow<Set<String>> = repository.observeAll()
        .map { entities -> entities.map { it.letterId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** Fire-and-forget toggle — [bookmarkedIds] (Room `Flow`) reconciles the
     * UI shortly after, same "trust the local write" reasoning as
     * [com.apps.unsealed.ui.screens.penpals.viewmodel.PenPalsViewModel.toggleLike]'s
     * optimistic update, minus the revert-on-error path since there's no
     * network round trip here to fail. */
    fun toggleBookmark(letter: PenpalLetter) {
        viewModelScope.launch { repository.toggleBookmark(letter) }
    }

    fun removeBookmarks(letterIds: Set<String>) {
        viewModelScope.launch { repository.removeAll(letterIds) }
    }
}
