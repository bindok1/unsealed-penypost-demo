package com.apps.unsealed.ui.screens.compose.state

/** One turn of Mode Peny's in-memory conversation — resent in full on every
 * `POST /peny/reply` request (peny_mode_mobile_integration.md §2 step 4), so
 * its shape mirrors the `history` array the backend expects rather than
 * being a free-form UI model. Never persisted (Room/DataStore) and never
 * part of Draft-on-Exit — see [ComposeUiState.penyHistory]. */
data class PenyTurn(
    val role: PenyRole,
    val text: String,
)

enum class PenyRole { USER, PENY }
