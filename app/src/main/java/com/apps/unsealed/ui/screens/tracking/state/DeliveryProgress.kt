package com.apps.unsealed.ui.screens.tracking.state

import androidx.annotation.StringRes
import com.apps.unsealed.R
import java.time.Instant

/** Linear interpolation between `sent_at` and `estimated_arrival_at` — the
 * whole progress bar is client-side wall-clock math, no server push needed
 * (see `docs/message-tracking.md`'s Progress Calculation section). */
fun calculateProgress(sentAtMillis: Long, estimatedArrivalAtMillis: Long, nowMillis: Long): Float {
    val total = estimatedArrivalAtMillis - sentAtMillis
    if (total <= 0L) return 1f
    val elapsed = nowMillis - sentAtMillis
    return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

/** Parses a raw ISO-8601 instant (e.g. `estimated_arrival_at`) to epoch millis. */
fun String.toEpochMillis(): Long = Instant.parse(this).toEpochMilli()

/** Flavor-text bucket per [TransportMode] × progress range, ported from
 * `docs/message-tracking.md`'s per-transport tables. */
@StringRes
fun flavorTextFor(transportMode: TransportMode, progress: Float): Int = when (transportMode) {
    TransportMode.WALK -> when {
        progress < 0.2f -> R.string.letter_tracking_flavor_walk_0
        progress < 0.5f -> R.string.letter_tracking_flavor_walk_20
        progress < 0.8f -> R.string.letter_tracking_flavor_walk_50
        else -> R.string.letter_tracking_flavor_walk_80
    }
    TransportMode.SWIM -> when {
        progress < 0.2f -> R.string.letter_tracking_flavor_swim_0
        progress < 0.4f -> R.string.letter_tracking_flavor_swim_20
        progress < 0.6f -> R.string.letter_tracking_flavor_swim_40
        progress < 0.8f -> R.string.letter_tracking_flavor_swim_60
        else -> R.string.letter_tracking_flavor_swim_80
    }
}
