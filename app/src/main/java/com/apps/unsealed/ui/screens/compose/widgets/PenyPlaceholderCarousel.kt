package com.apps.unsealed.ui.screens.compose.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberIsReducedMotion
import kotlinx.coroutines.delay

/** Time an idle prompt stays fully visible before swapping to the next one
 * — long enough to read (`R.array.compose_peny_input_placeholders` items are
 * short, 2-6 words), short enough that the "living paper" effect is
 * noticeable within one glance at an empty draft. */
private const val PenyPlaceholderCycleMillis = 6_000L

/** Outgoing prompt's fade-out duration — slower than a typical tap-response
 * fade (motion-rules.md's 350ms ceiling is for feedback, not this ambient
 * loop) so the old line dissolves gradually instead of snapping off, closer
 * to ink fading from paper than a UI element disappearing. */
private const val PenyPlaceholderExitFadeMillis = 700

/**
 * Rotates Mode Peny's empty-input hint through a handful of variations
 * instead of one static line. The incoming prompt writes itself in
 * word-by-word via [InkWritingText] — the same "ink writing itself in"
 * effect [PenyReplyOverlay] uses for Peny's replies (Tom Riddle's diary
 * reference) — while the outgoing one just fades away, so the empty state
 * reads as part of the same magic rather than a separate animation
 * language. Entrance uses [EnterTransition.None] at the [AnimatedContent]
 * level deliberately: [InkWritingText] already drives its own per-word
 * fade-in, stacking a second outer fade on top of it would only wash out
 * the first frames.
 *
 * Caller controls mounting — this only cycles while composed, so wrapping
 * it in `if (uiState.penyDraftInput.isEmpty())` (as [LetterCanvas] already
 * does for the old static placeholder) naturally pauses the loop the
 * instant the user starts typing, no explicit "paused" flag needed.
 */
@Composable
fun PenyPlaceholderCarousel(
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()
    val prompts = stringArrayResource(R.array.compose_peny_input_placeholders)
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(prompts, isReducedMotion) {
        if (isReducedMotion || prompts.size <= 1) return@LaunchedEffect
        while (true) {
            delay(PenyPlaceholderCycleMillis)
            index = (index + 1) % prompts.size
        }
    }

    AnimatedContent(
        targetState = index,
        transitionSpec = { EnterTransition.None togetherWith fadeOut(tween(PenyPlaceholderExitFadeMillis)) },
        modifier = modifier,
        label = "penyPlaceholderCarousel",
    ) { i ->
        InkWritingText(
            text = prompts.getOrElse(i) { "" },
            style = style,
            isReducedMotion = isReducedMotion,
            textAlign = TextAlign.Start,
        )
    }
}
