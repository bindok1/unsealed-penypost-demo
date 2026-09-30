package com.apps.unsealed.ui.screens.tracking.state

import com.apps.unsealed.ui.screens.selectrecipient.constants.PenpalRegion

sealed interface DeliveryTrackingUiState {
    data object Loading : DeliveryTrackingUiState
    data class Error(val message: String) : DeliveryTrackingUiState
    data class Success(
        val senderContinent: PenpalRegion,
        val recipientContinent: PenpalRegion,
        val transportMode: TransportMode,
        val progress: Float,
        val sentAtLabel: String,
        val estimatedArrivalAtLabel: String,
        val isDelivered: Boolean,
        /** Only ever known for whichever side is the viewer (their own
         * profile) — the backend doesn't expose the other party's identity
         * on `GET /letters/{id}`, see `docs/be/letters_api.md`'s roadmap gap
         * note. `null` on the other side falls back to a continent-only pin
         * label in [com.apps.unsealed.ui.screens.tracking.widgets.DeliveryRouteMap]. */
        val senderName: String? = null,
        val recipientName: String? = null,
        val letterId: String = "",
        /** Gates the "feed Peni to speed up" boost CTA — only the recipient
         * can spend energy to unlock their own incoming letter early. */
        val isViewerRecipient: Boolean = false,
        /** Viewer's energy balance, fetched once alongside [senderName] and
         * refreshed locally after a successful unlock (see
         * [com.apps.unsealed.ui.screens.tracking.viewmodel.DeliveryTrackingViewModel]) —
         * `null` while still loading. */
        val viewerEnergy: Int? = null,
        val envelope: String? = null,
        val envelopeCompositeImageUrl: String? = null,
    ) : DeliveryTrackingUiState
}

/** Drives [com.apps.unsealed.ui.screens.tracking.screen.DeliveryTrackingScreen]'s
 * boost confirm dialog — mirrors [com.apps.unsealed.ui.screens.inbox.state.ThreadReportState]'s
 * convention (separate per-action state, resolved to UI via `LaunchedEffect` + Toast). */
sealed interface UnlockLetterState {
    data object Idle : UnlockLetterState
    data object Unlocking : UnlockLetterState
    data class Success(val energyRemaining: Int) : UnlockLetterState
    /** The proactive "jemput bola" upsell moment — instead of a generic
     * error, the screen offers a "buy more Energy" dialog with the exact
     * shortfall (see `docs/copywriting/cp.md`). */
    data class InsufficientEnergy(val required: Int, val available: Int) : UnlockLetterState
    data class Error(val message: String) : UnlockLetterState
}
