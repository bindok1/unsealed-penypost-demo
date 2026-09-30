package com.apps.unsealed.ui.screens.compose.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.theme.LemonYellow
import kotlinx.coroutines.delay

private val WandTouchTargetSize = 40.dp
private val WandIconSize = 28.dp
private val WandGlowSize = 36.dp
private const val PenyWandAssetFileName = "wired-flat-3181-magician-wand-hover-pinch.json"

/**
 * Mode Peny's entry point — replaces the old separate sparkle
 * `PenyTriggerGlyph` + decorative wand pair with a single wand glyph that
 * does everything: toggles the mode, carries the glow states, and surfaces
 * a first-use discovery dot. (Two side-by-side icons doing overlapping jobs
 * in the same corner was more confusing than one icon doing one job well.)
 *
 * **Glow** (peny-mode-spec.md §5.2, unchanged from the old glyph):
 * - inactive: no glow
 * - active: steady glow
 * - processing (waiting on `POST /peny/reply`): breathing pulse — the
 *   `infiniteRepeatable`/`LinearEasing` here is exempted from
 *   motion-rules.md's linear/loop ban because it's a loading indicator, not
 *   decorative motion (§3.10 precedent).
 *
 * **Discovery dot** ([showDiscoveryBadge]): a plain [Badge] dot, the same
 * primitive `BottomNavBar` uses for the unread-mailbox count — present
 * until the caller reports the user has activated Mode Peny at least once
 * ([ComposeUiState.hasTriedPenyMode]), then gone for good. Static, no
 * motion of its own, so it can't run afoul of the "no decorative loop" rule
 * by construction.
 *
 * **Wand clip**: plays its rest→cast→rest cycle once on entrance (a beat
 * after the trigger becomes available — mirrors the FAB's bounce-in,
 * motion-rules.md §3.3) and again on every tap, exempt from the same "responsive, not
 * decorative" rule as the glow pulse above: entrance-once and tap-response aren't autoplay loops.
 */
@Composable
fun PenyWandHint(
    isPenyModeActive: Boolean,
    isPenyReplyLoading: Boolean,
    showDiscoveryBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val contentDesc = stringResource(R.string.compose_peny_trigger_content_desc)

    val infiniteTransition = rememberInfiniteTransition(label = "penyGlowPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "penyGlowPulseAlpha",
    )
    val glowAlpha = when {
        !isPenyModeActive -> 0f
        isPenyReplyLoading -> pulseAlpha
        else -> 0.7f
    }

    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset(PenyWandAssetFileName),
    )
    val animatable = rememberLottieAnimatable()
    var tapCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(composition, isReducedMotion) {
        val comp = composition ?: return@LaunchedEffect
        if (isReducedMotion) return@LaunchedEffect
        // Small beat so the flourish reads as "hey, look here" after the
        // letter has settled in, not as part of the screen-enter transition.
        delay(600)
        animatable.animate(comp, iterations = 1)
    }
    LaunchedEffect(tapCount) {
        val comp = composition ?: return@LaunchedEffect
        if (tapCount == 0 || isReducedMotion) return@LaunchedEffect
        animatable.snapTo(comp, progress = 0f)
        animatable.animate(comp, iterations = 1)
    }

    BadgedBox(
        badge = { if (showDiscoveryBadge) Badge() },
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(WandTouchTargetSize)
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        tapCount++
                        onClick()
                    },
                )
                .semantics { contentDescription = contentDesc },
            contentAlignment = Alignment.Center,
        ) {
            if (glowAlpha > 0f) {
                Box(
                    Modifier
                        .size(WandGlowSize)
                        .blur(WandGlowSize / 2)
                        .background(LemonYellow.copy(alpha = glowAlpha), CircleShape),
                )
            }
            LottieAnimation(
                composition = composition,
                progress = { if (isReducedMotion) 0f else animatable.progress },
                modifier = Modifier.size(WandIconSize),
            )
        }
    }
}
