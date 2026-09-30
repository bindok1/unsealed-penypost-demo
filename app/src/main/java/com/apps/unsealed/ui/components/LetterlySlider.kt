package com.apps.unsealed.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.apps.unsealed.core.util.rememberPressScale

/**
 * The one slider look for the whole app. Replaces Material3's default
 * [Slider] styling — which carries a ripple/state-layer and reads as a stock
 * Android control — with Letterly's own: no ripple, a filled+bordered thumb,
 * and scale-only press feedback via [rememberPressScale], per
 * motion-rules.md §3.1/§4. See docs/component-library.md for the full spec.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterlySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.White,
    inactiveTrackColor: Color = Color.White.copy(alpha = 0.15f),
    thumbColor: Color = trackColor,
    thumbBorderColor: Color = Color.White,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        interactionSource = interactionSource,
        thumb = { LetterlySliderThumb(interactionSource, thumbColor, thumbBorderColor) },
        colors = SliderDefaults.colors(
            activeTrackColor = trackColor,
            inactiveTrackColor = inactiveTrackColor,
        ),
        modifier = modifier,
    )
}

@Composable
private fun LetterlySliderThumb(
    interactionSource: MutableInteractionSource,
    color: Color,
    borderColor: Color,
) {
    val scale = rememberPressScale(interactionSource)
    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(20.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, borderColor, CircleShape),
    )
}
