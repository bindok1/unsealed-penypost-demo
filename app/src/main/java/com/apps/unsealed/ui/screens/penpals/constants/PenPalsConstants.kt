package com.apps.unsealed.ui.screens.penpals.constants

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ─── Postcard & Desk Constants ─────────────────────────────────────────────

/** Fixed card width for every postcard on the desk. */
val DeskCardWidth = 240.dp

/** Gap below the header before the scatter area starts. */
val DeskScatterTopGap = 24.dp

/** Bottom-of-stack "refresh early" teaser card, scattered like any other
 * desk card — see `docs/be/daily_penpal_stack.md` §6. */
val ArchiveCardSeed = "penpals-archive-bottom-card".hashCode()

/** Energy cost of `POST /penpals/feed/refresh` (`RefreshFeedEnergyCost` in
 * `app/module/penpal/service/penpal_service.go`) — BE-documented as a
 * placeholder, not a final product decision. */
const val RefreshFeedEnergyCost = 20

val DeskCardRestElevation = 2.dp
val DeskCardLiftedElevation = 14.dp
val DeskCardFocusedElevation = 28.dp
const val DeskCardRestScale = 1f
const val DeskCardLiftedScale = 1.03f
const val DeskCardFocusedScale = 1.1f

/** Stable random rotation range per card, in degrees: [-DeskRotationRangeDeg, DeskRotationRangeDeg]. */
const val DeskRotationRangeDeg = 6f

/** Other cards dim to this alpha while one card is focused. */
const val DeskDimmedAlpha = 0.4f
const val DeskScrimAlpha = 0.4f

/** zIndex bands for UI layering. */
const val DeskHeaderZIndex = 1000f
const val DeskFocusScrimZIndex = 1100f
const val DeskFocusedCardZIndex = 1200f

// ─── Desk Scatter Math Helpers ──────────────────────────────────────────────

/**
 * Deterministic scattered top-left position within a `[0, areaWidthPx] x
 * [0, areaHeightPx]` rectangle — same [seed] always produces the same spot.
 */
fun deskScatterOffset(
    seed: Int,
    areaWidthPx: Float,
    areaHeightPx: Float,
    cardWidthPx: Float,
    cardHeightPx: Float,
): Offset {
    val rnd = Random(seed)
    val angle = rnd.nextFloat() * (2 * PI).toFloat()
    val radiusFraction = sqrt(rnd.nextFloat())
    val maxRx = ((areaWidthPx - cardWidthPx) / 2f).coerceAtLeast(0f)
    val maxRy = ((areaHeightPx - cardHeightPx) / 2f).coerceAtLeast(0f)
    val centerX = (areaWidthPx - cardWidthPx) / 2f
    val centerY = (areaHeightPx - cardHeightPx) / 2f
    return Offset(
        x = (centerX + cos(angle) * maxRx * radiusFraction)
            .coerceIn(0f, (areaWidthPx - cardWidthPx).coerceAtLeast(0f)),
        y = (centerY + sin(angle) * maxRy * radiusFraction)
            .coerceIn(0f, (areaHeightPx - cardHeightPx).coerceAtLeast(0f)),
    )
}

/** Stable random rotation in degrees, [-DeskRotationRangeDeg, DeskRotationRangeDeg]. */
fun deskScatterRotation(seed: Int): Float =
    Random(seed).nextFloat() * (2 * DeskRotationRangeDeg) - DeskRotationRangeDeg
