package com.apps.unsealed.ui.screens.compose.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.ui.theme.LemonYellow

/** One soft glow blob's position/size, expressed relative to the containing
 * box so the effect scales with whatever area it's given (margin around the
 * paper, not the paper itself — see call site in ComposeScreen.kt, this sits
 * *behind* [LetterCanvas] in the same scrollable Box, deliberately outside
 * LetterCanvas's own `clipToBounds()`). [phaseOffsetMillis] staggers each
 * blob's pulse so they don't breathe in lockstep — that's what reads as
 * "alive/flowing" rather than one uniform flash. */
private data class AuraBlob(
    val alignmentX: Float,
    val alignmentY: Float,
    val radiusFraction: Float,
    val phaseOffsetMillis: Int,
)

private val AuraBlobs = listOf(
    AuraBlob(alignmentX = -0.9f, alignmentY = -0.85f, radiusFraction = 0.55f, phaseOffsetMillis = 0),
    AuraBlob(alignmentX = 0.95f, alignmentY = -0.6f, radiusFraction = 0.45f, phaseOffsetMillis = 550),
    AuraBlob(alignmentX = -0.85f, alignmentY = 0.9f, radiusFraction = 0.5f, phaseOffsetMillis = 1100),
    AuraBlob(alignmentX = 0.9f, alignmentY = 0.85f, radiusFraction = 0.55f, phaseOffsetMillis = 300),
)

private const val AuraPulseDurationMillis = 2600
private const val AuraMinAlpha = 0.35f
private const val AuraMaxAlpha = 0.9f
private const val AuraReducedMotionAlpha = 0.6f

/**
 * Ambient "magic" glow rendered in the margin *around* the paper while Mode
 * Peny is active — draft product spec's "kertas hidup" atmosphere for the
 * area outside the writing surface itself (peny-mode-spec.md §5 covers the
 * paper's own trigger/glow/reveal; this is the surrounding desk area, not
 * covered there yet).
 *
 * Deliberately plain [Brush.radialGradient] circles, not `Modifier.blur()`
 * or a `RenderEffect`/AGSL shader — same performance rationale
 * `PenyReplyOverlay` already documents (peny-mode-spec.md §5.3: MVP avoids
 * shader/RenderEffect bleed across the wide Android device range this app
 * supports). A radial gradient's own falloff to alpha 0 gives a soft edge
 * "for free" without a blur pass.
 *
 * Caller is responsible for sizing/placement — pass e.g.
 * `Modifier.matchParentSize()` from within the same scrollable Box that also
 * hosts [LetterCanvas], positioned *before* it in composition order so the
 * glow sits behind the paper in z-order.
 */
@Composable
fun PenyAuraGlow(
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()
    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn(tween(500)),
        exit = fadeOut(tween(350)),
        modifier = modifier,
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "penyAuraPulse")
        Box(Modifier.fillMaxSize()) {
            AuraBlobs.forEach { blob ->
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = AuraMinAlpha,
                    targetValue = AuraMaxAlpha,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = AuraPulseDurationMillis,
                            easing = LinearEasing,
                            delayMillis = blob.phaseOffsetMillis,
                        ),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "penyAuraBlobAlpha",
                )
                val alpha = if (isReducedMotion) AuraReducedMotionAlpha else pulseAlpha
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val shortSide = minOf(size.width, size.height)
                            val radius = shortSide * blob.radiusFraction
                            val center = Offset(
                                x = size.width / 2f + blob.alignmentX * size.width / 2f,
                                y = size.height / 2f + blob.alignmentY * size.height / 2f,
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        LemonYellow.copy(alpha = alpha * 0.5f),
                                        LemonYellow.copy(alpha = 0f),
                                    ),
                                    center = center,
                                    radius = radius,
                                ),
                                radius = radius,
                                center = center,
                            )
                        },
                )
            }
        }
    }
}
