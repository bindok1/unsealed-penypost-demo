package com.apps.unsealed.core.util

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Named spring presets from /docs/motion-rules.md §2.
 * Reuse these everywhere instead of hardcoding spring params.
 */
object LetterlySpring {

    /** SNAPPY — tombol, chip, toggle kecil. Respons cepat, pantulan minimal. */
    val Snappy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    /** BOUNCY — FAB, card tap, stamp. Efek "muncul" yang playful. */
    val Bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** GENTLE — modal, bottom sheet, page transition. */
    val Gentle = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** STIFF — error shake, confirmation pulse. Gerakan tegas tanpa pantulan. */
    val Stiff = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    /** ENVELOPE — animasi amplop/kertas. Berat, lambat. */
    val Envelope = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = 180f,
    )
}
