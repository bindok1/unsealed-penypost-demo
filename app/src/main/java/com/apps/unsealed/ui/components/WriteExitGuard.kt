package com.apps.unsealed.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.apps.unsealed.R
import com.apps.unsealed.core.data.WriteExitCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Thin vehicle for pulling the [WriteExitCoordinator] singleton into a
 * [Composable] scope via [hiltViewModel] — see [rememberWriteExitGuard]. */
@HiltViewModel
internal class WriteExitViewModel @Inject constructor(
    val coordinator: WriteExitCoordinator,
) : ViewModel()

/** State + actions returned by [rememberWriteExitGuard]. */
data class WriteExitGuardState(
    val isDialogVisible: Boolean,
    /** Call from either trigger site (system back, or a bottom-nav tab tap
     * away from Write) with the navigation that should happen once resolved.
     * Runs [onExit] immediately if there's nothing unsaved. */
    val requestExit: (onExit: () -> Unit) -> Unit,
    /** Silent variant of [requestExit] — saves (if there's unsaved content)
     * and exits immediately, skipping the confirmation dialog. See
     * [WriteExitCoordinator.saveAndExit]. */
    val saveAndExit: (onExit: () -> Unit) -> Unit,
    val confirmSave: () -> Unit,
    val confirmDiscard: () -> Unit,
    val cancel: () -> Unit,
)

/**
 * Hook backing the Draft-on-Exit dialog (`docs/todo.md` #5). Because
 * [WriteExitCoordinator] is `@Singleton`, every call site — whether it's
 * `MainActivity`'s `UnsealedApp` (bottom-nav tab tap) or `WriteRouteContent`
 * (`BackHandler`) — resolves the *same* coordinator instance despite each
 * pulling in its own, differently-scoped [WriteExitViewModel] via
 * [hiltViewModel]. This is what lets both trigger sites share one pending-exit
 * request and one dialog.
 */
@Composable
fun rememberWriteExitGuard(): WriteExitGuardState {
    val coordinator = hiltViewModel<WriteExitViewModel>().coordinator
    val pendingExit by coordinator.pendingExit.collectAsState()
    return WriteExitGuardState(
        isDialogVisible = pendingExit != null,
        requestExit = coordinator::requestExit,
        saveAndExit = coordinator::saveAndExit,
        confirmSave = coordinator::confirmSave,
        confirmDiscard = coordinator::confirmDiscard,
        cancel = coordinator::cancel,
    )
}

/** "Save to Draft?" — shown when the user tries to leave Write with unsaved
 * text. Copy per docs/copywriting/cp.md's Peni voice: draft only keeps text,
 * images/annotations/stickers are dropped either way. */
@Composable
fun SaveDraftDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    LetterlyCenterDialog(
        title = stringResource(R.string.save_draft_dialog_title),
        body = stringResource(R.string.save_draft_dialog_body),
        primaryCtaText = stringResource(R.string.save_draft_dialog_cta_save),
        onPrimaryClick = onSave,
        secondaryCtaText = stringResource(R.string.save_draft_dialog_cta_discard),
        onSecondaryClick = onDiscard,
        onDismissRequest = onDismissRequest,
    )
}
