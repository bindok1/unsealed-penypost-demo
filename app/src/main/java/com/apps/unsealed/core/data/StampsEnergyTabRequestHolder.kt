package com.apps.unsealed.core.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot hint read by `StampsViewModel` so a proactive "buy Energy" prompt
 * elsewhere in the app — Delivery Tracking's boost dialog, Mode Peny's
 * insufficient-Energy dialog in Compose, the "jemput bola" upsell moments
 * documented in `docs/copywriting/cp.md` — can land the user directly on the
 * Energi tab instead of Stamps' default Hadiah tab.
 *
 * Same one-shot hand-off shape as [com.apps.unsealed.ui.screens.compose.state.ReplyLetterContextHolder]:
 * call [request] right before navigating to
 * [com.apps.unsealed.navigation.Destinations.Stamps], and [consume]
 * unconditionally reads + clears the flag so a later plain bottom-nav tap
 * into Stamps never inherits a stale request (see that holder's `clear()`
 * doc comment for the bug this shape avoids).
 */
@Singleton
class StampsEnergyTabRequestHolder @Inject constructor() {
    @Volatile
    private var requested = false

    fun request() {
        requested = true
    }

    fun consume(): Boolean {
        val value = requested
        requested = false
        return value
    }
}
