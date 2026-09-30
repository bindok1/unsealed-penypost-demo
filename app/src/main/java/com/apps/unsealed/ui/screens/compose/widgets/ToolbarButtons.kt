package com.apps.unsealed.ui.screens.compose.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.theme.LemonYellow
import com.apps.unsealed.ui.theme.ToolbarFrostedDark

/**
 * Shared no-ripple icon toggle used by [AnnotateToolbar] (tool selection) and
 * [FormattingBar] (Bold/Italic) — scale-on-press + [LemonYellow] pill when
 * selected, per motion-rules.md §3.1/§4.
 */
@Composable
fun PillIconToggleButton(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    unselectedTint: Color = Color.White,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduced = rememberIsReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = reducedMotionSpring(
            if (isPressed) LetterlySpring.Snappy else LetterlySpring.Bouncy,
            reduced,
        ),
        label = "pillToggleScale",
    )
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(if (isSelected) LemonYellow else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) Color.Black else unselectedTint,
        )
    }
}

private val SendButtonSize = 48.dp

/** Shared frosted circular Send button — used by [ComposeToolbar] (outside
 * its pill, a distinct final action) and by the Select Recipient screen's
 * header, so both screens' Send affordance reads as one component instead of
 * two near-identical copies.
 *
 * [highlighted] (motion-rules.md §3.5 "confirmation pulse") swaps the frosted
 * dark fill for [LemonYellow] + a one-shot bounce, so Select Recipient can
 * visually signal "everything's picked, ready to send" the moment it flips
 * true — [ComposeToolbar] never sets it, so its Send stays visually inert. */
@Composable
internal fun SendButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val isReducedMotion = rememberIsReducedMotion()
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(highlighted) {
        if (highlighted) {
            pulse.animateTo(1.15f, reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion))
            pulse.animateTo(1f, reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion))
        }
    }
    val backgroundColor by animateColorAsState(
        targetValue = if (highlighted) LemonYellow else ToolbarFrostedDark,
        label = "sendButtonBackground",
    )
    val iconTint by animateColorAsState(
        targetValue = if (highlighted) Color.Black else Color.White,
        label = "sendButtonIconTint",
    )
    val shape = CircleShape
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale * pulse.value
                scaleY = pressScale * pulse.value
            }
            .size(SendButtonSize)
            .shadow(elevation = 8.dp, shape = shape, clip = false)
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Send, contentDescription = stringResource(R.string.toolbar_send_desc), tint = iconTint)
    }
}
