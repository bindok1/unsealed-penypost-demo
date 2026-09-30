package com.apps.unsealed.ui.screens.profile.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apps.unsealed.R
import com.apps.unsealed.core.util.staggerEntrance
import com.apps.unsealed.ui.components.LetterlyCenterDialog
import com.apps.unsealed.ui.components.LetterlyScreenHeader
import com.apps.unsealed.ui.screens.profile.state.DraftEntry
import com.apps.unsealed.ui.screens.profile.widgets.DraftCard

/** Draft sub-screen: user's saved-but-unsent letters (Draft-on-Exit,
 * `docs/todo.md` #5) — [uiState] is backed by `ProfileDraftViewModel`'s
 * `DraftDao.observeAll()`, the first ViewModel in the Profile sub-screen tree. */
@Composable
fun ProfileDraftScreen(
    uiState: List<DraftEntry>,
    onBackClick: () -> Unit,
    onDraftClick: (DraftEntry) -> Unit,
    onDeleteDraft: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        LetterlyScreenHeader(title = stringResource(R.string.profile_draft_title), onBackClick = onBackClick)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.size) { index ->
                val draft = uiState[index]
                DraftCard(
                    draft = draft,
                    onClick = { onDraftClick(draft) },
                    onDeleteClick = { pendingDeleteId = draft.id },
                    modifier = Modifier.staggerEntrance(index),
                )
            }
            item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
        }
    }

    val deleteId = pendingDeleteId
    if (deleteId != null) {
        LetterlyCenterDialog(
            title = stringResource(R.string.delete_draft_dialog_title),
            body = stringResource(R.string.delete_draft_dialog_body),
            primaryCtaText = stringResource(R.string.delete_draft_dialog_cta_confirm),
            onPrimaryClick = {
                onDeleteDraft(deleteId)
                pendingDeleteId = null
            },
            secondaryCtaText = stringResource(R.string.delete_draft_dialog_cta_cancel),
            onSecondaryClick = { pendingDeleteId = null },
            onDismissRequest = { pendingDeleteId = null },
            primaryContainerColor = Color(0xFFE53935),
            primaryContentColor = Color.White,
        )
    }
}
