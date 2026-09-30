package com.apps.unsealed.ui.screens.profile.viewmodel

import android.text.format.DateUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.unsealed.core.database.DraftEntity
import com.apps.unsealed.feature.draft.data.DraftRepository
import com.apps.unsealed.ui.screens.profile.state.DraftEntry
import com.apps.unsealed.ui.theme.PaperTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SnippetMaxLength = 80

/** First ViewModel for a Profile sub-screen (see `docs/profile-screen-spec.md`
 * — every other Profile sub-screen deliberately stays `remember`-only). */
@HiltViewModel
class ProfileDraftViewModel @Inject constructor(
    private val draftRepository: DraftRepository,
) : ViewModel() {

    val uiState: StateFlow<List<DraftEntry>> = draftRepository.observeAll()
        .map { entities -> entities.map { it.toDraftEntry() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onDeleteDraft(draftId: String) {
        viewModelScope.launch { draftRepository.delete(draftId) }
    }
}

private fun DraftEntity.toDraftEntry() = DraftEntry(
    id = draftId,
    recipientName = recipientName ?: dearName.ifBlank { "" },
    snippet = bodyText.take(SnippetMaxLength),
    lastEditedLabel = DateUtils.getRelativeTimeSpanString(
        updatedAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString(),
    paperTemplate = selectedPaperTemplate?.let(PaperTemplate::valueOf) ?: PaperTemplate.PLAIN_WHITE,
)
