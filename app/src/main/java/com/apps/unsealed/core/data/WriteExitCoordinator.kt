package com.apps.unsealed.core.data

import com.apps.unsealed.core.database.DraftEntity
import com.apps.unsealed.feature.draft.data.DraftRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Cross-scope bridge for the Draft-on-Exit dialog (`docs/todo.md` #5). The
 * "does Write have unsaved content" state lives inside `ComposeViewModel`,
 * scoped to a specific `NavBackStackEntry`; the bottom-nav tab-tap handler
 * that can trigger leaving Write lives in `MainActivity`, a different
 * composition scope entirely. `rememberAuthGuard` (see `AuthGuardBottomSheet.kt`)
 * doesn't fit here because its guarded state and guarded action both resolve
 * at the same call site — this needs a real bridge instead, same
 * "in-memory cross-scope holder" family as [ComposeDraftHolder].
 *
 * Both trigger sites (the `BackHandler` in `WriteRouteContent` and the
 * bottom-nav tap lambda in `MainActivity`) call [requestExit] with the same
 * API; a single dialog mounted in `MainActivity` reacts to [pendingExit].
 */
@Singleton
class WriteExitCoordinator @Inject constructor(
    private val draftRepository: DraftRepository,
) {
    /** Registered by the active `ComposeViewModel` on init, cleared onCleared(). */
    data class Session(
        val hasUnsavedContent: () -> Boolean,
        val snapshot: () -> DraftEntity,
    )

    // Deliberately not viewModelScope: confirmSave() may run after the Write
    // NavBackStackEntry (and its ViewModelStore) is already being torn down
    // by the popBackStack()/navigate() that follows — a scope tied to that
    // entry could be cancelled mid-write, silently dropping the save.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var activeSession: Session? = null

    private val _pendingExit = MutableStateFlow<(() -> Unit)?>(null)
    val pendingExit: StateFlow<(() -> Unit)?> = _pendingExit.asStateFlow()

    fun register(session: Session) {
        activeSession = session
    }

    fun unregister(session: Session) {
        if (activeSession === session) activeSession = null
    }

    /** Call from either trigger site. Runs [onExit] immediately when there's
     * nothing unsaved (no active session, or its content is blank) — the
     * common case behaves exactly like an unguarded exit. Otherwise defers
     * [onExit] until the user resolves the dialog. */
    fun requestExit(onExit: () -> Unit) {
        val session = activeSession
        if (session == null || !session.hasUnsavedContent()) {
            onExit()
            return
        }
        _pendingExit.value = onExit
    }

    /** Saves the current draft (if there's an active session with unsaved
     * content) and runs [onExit] immediately — no confirmation dialog. For
     * trigger sites where leaving isn't really *leaving* (e.g. Compose's
     * sticky reply banner popping back to peek at the thread — the user is
     * coming right back to this same draft) the Save/Discard choice
     * [requestExit] offers doesn't apply; this always behaves like "Save"
     * without asking. */
    fun saveAndExit(onExit: () -> Unit) {
        val session = activeSession
        if (session == null || !session.hasUnsavedContent()) {
            onExit()
            return
        }
        val entity = session.snapshot()
        scope.launch {
            draftRepository.upsert(entity)
            withContext(Dispatchers.Main) { onExit() }
        }
    }

    fun confirmSave() {
        val onExit = _pendingExit.value ?: return
        val entity = activeSession?.snapshot()
        _pendingExit.value = null
        scope.launch {
            if (entity != null) draftRepository.upsert(entity)
            withContext(Dispatchers.Main) { onExit() }
        }
    }

    /** No DB write — leaves whatever was last saved (if anything) untouched. */
    fun confirmDiscard() {
        val onExit = _pendingExit.value ?: return
        _pendingExit.value = null
        onExit()
    }

    fun cancel() {
        _pendingExit.value = null
    }
}
