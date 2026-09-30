package com.apps.unsealed.ui.screens.inbox.widgets

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.screens.inbox.state.ThreadLetterItem
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeAspectRatio
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.ToolbarFrostedDark

/** Which side of one [ThreadLetterItem] [MailboxFullLetterViewer] is showing. */
enum class FullLetterViewMode { ENVELOPE, LETTER }

/**
 * Full-screen envelope/letter viewer for [com.apps.unsealed.ui.screens.inbox.screen.MailboxThreadScreen],
 * opened from the two trigger icons in [ModeCaption]. Unlike PenPals'
 * `OpenLetterOverlay.kt` (full-bleed card + like/comment/share action rail),
 * this is a focused reader: content is centered and sized to fill device
 * width edge-to-edge (no side margin), height following the envelope/paper
 * aspect ratio — never cropped/zoomed to fill the whole screen.
 *
 * Follows the same "caller holds a request, component owns mount/animate"
 * convention as [AnchoredDropdownMenu][com.apps.unsealed.ui.components.AnchoredDropdownMenu]
 * (see docs/component-library.md): caller flips [isVisible] to request open/close,
 * this composable animates and only then calls [onFullyClosed] so the caller
 * can drop the letter reference without cutting the exit animation short.
 */
@Composable
fun MailboxFullLetterViewer(
    letter: ThreadLetterItem,
    mode: FullLetterViewMode,
    isVisible: Boolean,
    onModeChange: (FullLetterViewMode) -> Unit,
    onDismiss: () -> Unit,
    onFullyClosed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = reducedMotionSpring(
            if (isVisible) LetterlySpring.Gentle else LetterlySpring.Stiff,
            isReducedMotion,
        ),
        label = "fullViewScrimAlpha",
        finishedListener = { fraction -> if (fraction <= 0.01f) onFullyClosed() },
    )
    val contentScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.92f,
        animationSpec = reducedMotionSpring(
            if (isVisible) LetterlySpring.Gentle else LetterlySpring.Stiff,
            isReducedMotion,
        ),
        label = "fullViewContentScale",
    )
    // Envelope (1720:900, wide) <-> paper (1240:1754, tall portrait) — same
    // Envelope spring EnvelopePaperReveal.kt uses for its inline reveal, so
    // switching mode here has the same "heavy paper" weight (motion-rules §1).
    val aspectRatio by animateFloatAsState(
        targetValue = if (mode == FullLetterViewMode.ENVELOPE) EnvelopeAspectRatio else LetterPaperAspectRatio,
        animationSpec = reducedMotionSpring(LetterlySpring.Envelope, isReducedMotion),
        label = "fullViewAspectRatio",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = scrimAlpha }
            .background(BrandInkDeep.copy(alpha = 0.95f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Edge-to-edge width, centered vertically — sized by aspect ratio,
        // never cropped/zoomed to fill the screen (per user request).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .graphicsLayer { scaleX = contentScale; scaleY = contentScale }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}, // swallow taps so the card itself doesn't dismiss
                ),
        ) {
            Crossfade(targetState = mode, animationSpec = tween(220), label = "fullViewModeCrossfade") { current ->
                when (current) {
                    FullLetterViewMode.ENVELOPE -> AsyncImage(
                        model = letter.envelopeImageUrl,
                        contentDescription = stringResource(R.string.open_letter_envelope_icon_desc),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(letter.envelope.drawableRes),
                        error = painterResource(letter.envelope.drawableRes),
                        modifier = Modifier.fillMaxSize(),
                    )
                    FullLetterViewMode.LETTER -> if (letter.compositeImageUrl != null) {
                        AsyncImage(
                            model = letter.compositeImageUrl,
                            contentDescription = stringResource(R.string.open_letter_paper_icon_desc),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(letter.letterPaper.drawableRes),
                            error = painterResource(letter.letterPaper.drawableRes),
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        PlainTextLetterPaper(
                            bodyText = letter.bodyText,
                            paperTemplate = letter.letterPaper,
                            paperUrl = letter.paperUrl,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        FullViewBackButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
                .graphicsLayer { alpha = scrimAlpha },
        )

        FullViewModeToggle(
            mode = mode,
            onModeChange = onModeChange,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .graphicsLayer { alpha = scrimAlpha },
        )
    }
}

@Composable
private fun FullViewBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .size(40.dp)
            .clip(CircleShape)
            .background(ToolbarFrostedDark)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = stringResource(R.string.open_letter_back_desc),
            tint = Color.White,
        )
    }
}

/** Envelope/letter toggle so the reader can switch full-view side without
 * backing out to chat first. */
@Composable
private fun FullViewModeToggle(
    mode: FullLetterViewMode,
    onModeChange: (FullLetterViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        FullViewModeToggleIcon(
            icon = Icons.Filled.Mail,
            contentDescription = stringResource(R.string.mailbox_full_view_envelope_desc),
            isSelected = mode == FullLetterViewMode.ENVELOPE,
            onClick = { onModeChange(FullLetterViewMode.ENVELOPE) },
        )
        Spacer(Modifier.width(8.dp))
        FullViewModeToggleIcon(
            icon = Icons.Filled.Description,
            contentDescription = stringResource(R.string.mailbox_full_view_letter_desc),
            isSelected = mode == FullLetterViewMode.LETTER,
            onClick = { onModeChange(FullLetterViewMode.LETTER) },
        )
    }
}

@Composable
private fun FullViewModeToggleIcon(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isSelected) BrandGold.copy(alpha = 0.28f) else ToolbarFrostedDark)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) BrandGold else Color.White,
        )
    }
}
