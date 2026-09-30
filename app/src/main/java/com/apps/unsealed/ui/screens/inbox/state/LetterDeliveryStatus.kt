package com.apps.unsealed.ui.screens.inbox.state

/** Whether a letter has physically "arrived" yet, and — for not-yet-arrived
 * letters — which side is waiting. Direction matters: a letter the viewer
 * *sent* that's still in transit has nothing to "open early" (they wrote it),
 * so it gets its own [OUTGOING_IN_TRANSIT] state and tracking-map affordance
 * instead of [INCOMING]'s locked-envelope treatment. */
enum class LetterDeliveryStatus { DELIVERED, INCOMING, OUTGOING_IN_TRANSIT }
