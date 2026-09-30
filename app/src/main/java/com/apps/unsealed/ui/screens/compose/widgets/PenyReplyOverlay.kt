package com.apps.unsealed.ui.screens.compose.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.ui.theme.LetterBodyStyle

/** Fraction of the paper's measured width the reveal text is allowed to
 * wrap to — draft product spec §7's "±70-80%". */
private const val ReplyMaxWidthFraction = 0.75f

/** Per-word timing for the "writing itself in" reveal (see [InkWritingText])
 * — the Tom Riddle's-diary reference design feedback asked for: words
 * materialize in sequence rather than the whole block fading as one flat
 * unit. [InkWordFadeMillis] is deliberately much longer than
 * [InkWordStaggerMillis] so consecutive words' fades overlap heavily —
 * several words are mid-fade at once, which reads as one smooth continuous
 * wave instead of a fast word-by-word flicker (first pass used a fade
 * window equal to the stagger interval, which felt too fast/jerky). */
private const val InkWordStaggerMillis = 90
private const val InkWordFadeMillis = 420

/**
 * Reveal zone for Mode Peny's replies (AI, Lapis 2 local, or the
 * energy/error fallback copy — caller passes whichever is currently active
 * as [reply], the overlay itself doesn't know the source, see
 * peny-mode-spec.md §7). Positioned center-of-paper, deliberately separate
 * from the writing zone.
 *
 * The block itself fades/scales in via [AnimatedVisibility]; the words
 * inside materialize progressively rather than as one flat fade — see
 * [InkWritingText]. Still no shader/`RenderEffect` bleed (draft product spec
 * §7 explicitly picked plain compositing over that for performance across
 * the wide Android device range this app supports, same rationale as
 * `MinCompositingDensity` gating heavy compositing elsewhere) — the "writing
 * itself in" feel comes from a per-word alpha stagger, not a bleed shader.
 *
 * [modifier] must already carry sizing from the caller (e.g.
 * `Modifier.matchParentSize()` from within `LetterCanvas`'s outer `Box`,
 * same as its `AnnotateCanvas`/images layers) — this composable only adds
 * centering, it doesn't size itself.
 */
@Composable
fun PenyReplyOverlay(
    reply: String?,
    canvasWidthPx: Float,
    inkColor: Color,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()
    val density = LocalDensity.current
    val maxWidth = if (canvasWidthPx > 0f) {
        with(density) { (canvasWidthPx * ReplyMaxWidthFraction).toDp() }
    } else {
        280.dp
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = reply != null,
            enter = fadeIn(reducedMotionSpring(tween(350), isReducedMotion)) +
                scaleIn(
                    reducedMotionSpring(LetterlySpring.Gentle, isReducedMotion),
                    initialScale = 0.94f,
                ),
            exit = fadeOut(reducedMotionSpring(tween(400), isReducedMotion)),
        ) {
            InkWritingText(
                text = reply.orEmpty(),
                style = LetterBodyStyle.copy(color = inkColor),
                isReducedMotion = isReducedMotion,
                maxWidth = maxWidth,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Reveals [text] word-by-word rather than as one flat fade — the "ink
 * writing itself in" look design feedback asked for (Tom Riddle's diary
 * reference). A single [Animatable] drives a shared reveal-progress clock in
 * elapsed milliseconds; each word starts fading [InkWordStaggerMillis] after
 * the previous one but takes [InkWordFadeMillis] (longer than the stagger)
 * to finish its own fade, so several words are mid-fade at once — a smooth
 * overlapping wave rather than a fast word-by-word flicker.
 *
 * Built as one [Text] with per-word [SpanStyle] alpha (not N separate `Text`
 * composables in a `FlowRow`) so line-wrapping still comes from a single
 * real text layout instead of manual word-wrap math.
 *
 * Not private — [PenyPlaceholderCarousel] reuses the same reveal for the
 * empty-input hint's rotation, so the "living paper" effect reads as one
 * consistent magic language rather than two different animations.
 * [maxWidth] is optional since the placeholder caller wants the field's
 * natural width instead of [PenyReplyOverlay]'s centered-bubble constraint.
 * [modifier] is merged onto the underlying [Text] — e.g. [LetterCanvas]'s
 * idle-hint placeholder attaches `Modifier.clickable` here so tapping the
 * writing-itself-in prompt activates Mode Peny directly.
 */
@Composable
fun InkWritingText(
    text: String,
    style: TextStyle,
    isReducedMotion: Boolean,
    maxWidth: Dp? = null,
    textAlign: TextAlign = TextAlign.Center,
    modifier: Modifier = Modifier,
) {
    // Split keeping trailing whitespace attached to the preceding word
    // (lookbehind), so re-concatenating the chunks reproduces the original
    // text/spacing exactly.
    val chunks = remember(text) { text.split(Regex("(?<=\\s)")).filter { it.isNotEmpty() } }
    val totalDurationMillis = remember(chunks) {
        ((chunks.size - 1).coerceAtLeast(0) * InkWordStaggerMillis) + InkWordFadeMillis
    }
    val revealProgress = remember(text) { Animatable(0f) }
    LaunchedEffect(text, isReducedMotion) {
        if (isReducedMotion) {
            revealProgress.snapTo(totalDurationMillis.toFloat())
        } else {
            revealProgress.snapTo(0f)
            revealProgress.animateTo(
                targetValue = totalDurationMillis.toFloat(),
                animationSpec = tween(durationMillis = totalDurationMillis, easing = LinearEasing),
            )
        }
    }
    val elapsedMillis = revealProgress.value
    val baseColor = style.color
    val annotated = remember(chunks, elapsedMillis, baseColor) {
        buildAnnotatedString {
            chunks.forEachIndexed { index, chunk ->
                val startMillis = index * InkWordStaggerMillis
                val localT = ((elapsedMillis - startMillis) / InkWordFadeMillis).coerceIn(0f, 1f)
                val eased = FastOutSlowInEasing.transform(localT)
                withStyle(SpanStyle(color = baseColor.copy(alpha = baseColor.alpha * eased))) {
                    append(chunk)
                }
            }
        }
    }
    Text(
        text = annotated,
        style = style,
        textAlign = textAlign,
        modifier = modifier.then(if (maxWidth != null) Modifier.widthIn(max = maxWidth) else Modifier),
    )
}
