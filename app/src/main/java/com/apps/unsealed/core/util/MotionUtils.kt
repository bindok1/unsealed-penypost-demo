package com.apps.unsealed.core.util

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Reads the system "remove animations" accessibility setting.
 * Used instead of LocalReduceMotion, whose exact API shape varies across
 * Compose UI versions — the system animator-duration-scale is a stable,
 * version-independent signal for the same user preference.
 */
@Composable
fun rememberIsReducedMotion(): Boolean {
    val context = LocalContext.current
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return scale == 0f
}

/** Swaps [base] for an instant [snap] when [reduced] is true, per /docs/motion-rules.md §5. */
fun <T> reducedMotionSpring(base: FiniteAnimationSpec<T>, reduced: Boolean): FiniteAnimationSpec<T> =
    if (reduced) snap() else base

/**
 * Animates a displayed Energy/count balance toward [target] instead of
 * snapping straight to it — a top-up (or spend) reads as the number
 * climbing/falling rather than an instant jump-cut. Shared by every screen
 * that shows a live Energy count (Stamps' header pill, Delivery Tracking's
 * boost pill, ...) so a purchase or reward claim always animates the same
 * way. Snaps immediately when reduced-motion is on, same as every other
 * animation in this file.
 */
@Composable
fun rememberAnimatedCount(target: Int, durationMillis: Int = 600): Int {
    val isReducedMotion = rememberIsReducedMotion()
    val animated by animateIntAsState(
        targetValue = target,
        animationSpec = if (isReducedMotion) tween(0) else tween(durationMillis, easing = FastOutSlowInEasing),
        label = "animatedCount",
    )
    return animated
}

/**
 * Press-state scale shared by every tappable element in the app, per
 * motion-rules.md §3.1/§4: no ripple anywhere, scale is the only feedback
 * (Snappy down on press, Bouncy back on release), reduced-motion aware.
 */
@Composable
fun rememberPressScale(interactionSource: MutableInteractionSource): Float {
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduced = rememberIsReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = reducedMotionSpring(
            if (isPressed) LetterlySpring.Snappy else LetterlySpring.Bouncy,
            reduced,
        ),
        label = "pressScale",
    )
    return scale
}

/** Stagger entrance per motion-rules.md §3.7: translateX -20dp -> 0 + fade,
 * 40ms per running [index] across the whole screen (not reset per section) so
 * the cascade reads as one continuous reveal. Shared by every screen with a
 * cascading list entrance (Stamps, Profile, ...) — see StampsScreen.kt's
 * original recipe, extracted here once a second caller needed it
 * (docs/component-library.md's 2+-use extraction rule). */
@Composable
fun Modifier.staggerEntrance(index: Int): Modifier {
    val isReducedMotion = rememberIsReducedMotion()
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 40L)
        isVisible = true
    }
    val offsetX by animateDpAsState(
        targetValue = if (isVisible) 0.dp else (-20).dp,
        animationSpec = if (isReducedMotion) tween(0) else tween(220),
        label = "staggerEntranceOffset",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = if (isReducedMotion) tween(0) else tween(220),
        label = "staggerEntranceAlpha",
    )
    return this.graphicsLayer {
        translationX = offsetX.toPx()
        this.alpha = alpha
    }
}
