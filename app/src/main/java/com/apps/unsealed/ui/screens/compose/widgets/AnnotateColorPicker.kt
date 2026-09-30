package com.apps.unsealed.ui.screens.compose.widgets

import com.apps.unsealed.ui.screens.compose.state.*

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.components.LetterlySlider
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val ButtonSize = 36.dp
private val PickerWidth = 280.dp
private val SpectrumHeight = 140.dp
private val PickerIndicatorSize = 20.dp
private const val GridHueCols = 12
private const val GridColorRows = 8
private val GridCellHeight = 16.dp
private const val HexCopyFlashMillis = 1200L
private const val PopupExitMillis = 250

private enum class PickerMode { SPECTRUM, GRID, SLIDER }

/** Fresh Color-typed spring mirroring [LetterlySpring.Bouncy]'s constants — the
 * shared object only exposes `spring<Float>`, which can't back [animateColorAsState]. */
private val BouncyColorSpring = spring<Color>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMedium,
)

/**
 * Single "current ink color" trigger for Annotate Mode, replacing a row of
 * fixed preset swatches. Tapping it opens a picker popup (spectrum drag-pick
 * or a discrete grid, plus an opacity slider) so the user can pick any
 * color/alpha, not just a handful of presets.
 */
@Composable
fun AnnotateColorButton(
    selectedColor: Color,
    onColorSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    var isPopupVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val reduced = rememberIsReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)

    LaunchedEffect(showPicker) {
        if (showPicker) isPopupVisible = true
    }

    Box(modifier = modifier.size(ButtonSize)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(selectedColor)
                .border(2.dp, Color.White, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { showPicker = true },
                ),
        )

        if (showPicker) {
            Popup(
                alignment = Alignment.BottomCenter,
                offset = with(density) { IntOffset(0, -(ButtonSize + 12.dp).roundToPx()) },
                onDismissRequest = { isPopupVisible = false },
                properties = PopupProperties(focusable = true),
            ) {
                val popupScale by animateFloatAsState(
                    targetValue = if (isPopupVisible) 1f else 0.85f,
                    animationSpec = reducedMotionSpring(
                        if (isPopupVisible) LetterlySpring.Gentle else LetterlySpring.Stiff,
                        reduced,
                    ),
                    label = "colorPopupScale",
                )
                val popupAlpha by animateFloatAsState(
                    targetValue = if (isPopupVisible) 1f else 0f,
                    animationSpec = tween(if (isPopupVisible) 380 else PopupExitMillis),
                    label = "colorPopupAlpha",
                    finishedListener = { finalValue -> if (finalValue <= 0f) showPicker = false },
                )

                AnnotateColorPickerPopup(
                    initialColor = selectedColor,
                    onColorSelect = onColorSelect,
                    modifier = Modifier.graphicsLayer {
                        scaleX = popupScale
                        scaleY = popupScale
                        alpha = popupAlpha
                    },
                )
            }
        }
    }
}

@Composable
private fun AnnotateColorPickerPopup(
    initialColor: Color,
    onColorSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(PickerMode.SPECTRUM) }
    var baseColor by remember { mutableStateOf(initialColor.copy(alpha = 1f)) }
    var alpha by remember { mutableStateOf(initialColor.alpha) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2A241C),
        shadowElevation = 12.dp,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp).width(PickerWidth)) {
            PickerModeToggle(
                selectedMode = mode,
                onModeChange = { mode = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            when (mode) {
                PickerMode.SPECTRUM -> SpectrumColorPicker(
                    onColorPick = { picked ->
                        baseColor = picked
                        onColorSelect(picked.copy(alpha = alpha))
                    },
                    modifier = Modifier.fillMaxWidth().height(SpectrumHeight),
                )
                PickerMode.GRID -> GridColorPicker(
                    onColorPick = { picked ->
                        baseColor = picked
                        onColorSelect(picked.copy(alpha = alpha))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                PickerMode.SLIDER -> SliderColorPicker(
                    color = baseColor,
                    onColorPick = { picked ->
                        baseColor = picked
                        onColorSelect(picked.copy(alpha = alpha))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))
            OpacitySlider(
                alpha = alpha,
                trackColor = baseColor,
                onAlphaChange = { newAlpha ->
                    alpha = newAlpha
                    onColorSelect(baseColor.copy(alpha = newAlpha))
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PickerModeToggle(
    selectedMode: PickerMode,
    onModeChange: (PickerMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        PickerMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            val interactionSource = remember { MutableInteractionSource() }
            val scale = rememberPressScale(interactionSource)
            val reduced = rememberIsReducedMotion()
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) Color.White.copy(alpha = 0.18f) else Color.Transparent,
                animationSpec = reducedMotionSpring(BouncyColorSpring, reduced),
                label = "pickerTabBackground",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(10.dp))
                    .background(backgroundColor)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onModeChange(mode) },
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        when (mode) {
                            PickerMode.GRID -> R.string.annotate_color_mode_grid
                            PickerMode.SPECTRUM -> R.string.annotate_color_mode_spectrum
                            PickerMode.SLIDER -> R.string.annotate_color_mode_slider
                        },
                    ),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/**
 * Hue (x-axis) × saturation/value (y-axis) gradient box. Top half fades the
 * hue toward white (saturation 0 -> 1, value pinned at 1), bottom half fades
 * toward black (value 1 -> 0, saturation pinned at 1) — mathematically exact
 * for HSV since HSV(h, s, 1) == lerp(white, HSV(h, 1, 1), s) and
 * HSV(h, 1, v) == lerp(black, HSV(h, 1, 1), v), so alpha-blended overlays
 * reproduce true HSV traversal without rendering a per-pixel bitmap.
 */
@Composable
private fun SpectrumColorPicker(
    onColorPick: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerPosition by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(SpectrumHueStops))
                .background(
                    Brush.verticalGradient(
                        0f to Color.White,
                        0.5f to Color.Transparent,
                        1f to Color.Transparent,
                    ),
                )
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to Color.Transparent,
                        1f to Color.Black,
                    ),
                )
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val clamped = offset.coerceInBox(size)
                        pickerPosition = clamped
                        onColorPick(colorForHueAndLightness(clamped.x / size.width * 360f, clamped.y / size.height))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            // A drag isn't clipped to this Box's bounds once
                            // captured — the finger can move past the edges
                            // and Compose keeps reporting positions outside
                            // [0, size], which fed straight into hue/yFraction
                            // and crashed Color.hsv() (out-of-range hue).
                            val clamped = change.position.coerceInBox(size)
                            pickerPosition = clamped
                            onColorPick(
                                colorForHueAndLightness(
                                    clamped.x / size.width * 360f,
                                    clamped.y / size.height,
                                ),
                            )
                        },
                    )
                },
        )
        pickerPosition?.let { pos ->
            val halfIndicatorPx = with(density) { (PickerIndicatorSize / 2).toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (pos.x - halfIndicatorPx).roundToInt(),
                            (pos.y - halfIndicatorPx).roundToInt(),
                        )
                    }
                    .size(PickerIndicatorSize)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape),
            )
        }
    }
}

/**
 * Discrete alternative to [SpectrumColorPicker]: a grayscale ramp row on top
 * (white -> black) plus [GridColorRows] rows of tappable swatches sampling
 * the same hue/lightness formula as the spectrum, just quantized into chips.
 */
@Composable
private fun GridColorPicker(
    onColorPick: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(Modifier.fillMaxWidth()) {
            for (col in 0 until GridHueCols) {
                val fraction = col / (GridHueCols - 1f)
                val color = lerp(Color.White, Color.Black, fraction)
                GridSwatch(color = color, onClick = { onColorPick(color) }, modifier = Modifier.weight(1f))
            }
        }
        for (row in 0 until GridColorRows) {
            val yFraction = row / (GridColorRows - 1f)
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until GridHueCols) {
                    val hue = col / (GridHueCols - 1f) * 360f
                    val color = colorForHueAndLightness(hue, yFraction)
                    GridSwatch(color = color, onClick = { onColorPick(color) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GridSwatch(
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    Box(
        modifier = modifier
            .height(GridCellHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(color)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    )
}

/**
 * RGB channel sliders ("Penggeser") plus an sRGB hex readout the user can
 * copy to the clipboard — for precise/reproducible color entry, unlike the
 * free-hand [SpectrumColorPicker] or the quantized [GridColorPicker].
 */
@Composable
private fun SliderColorPicker(
    color: Color,
    onColorPick: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    var justCopied by remember { mutableStateOf(false) }
    LaunchedEffect(justCopied) {
        if (justCopied) {
            delay(HexCopyFlashMillis)
            justCopied = false
        }
    }

    val red = (color.red * 255f).roundToInt().coerceIn(0, 255)
    val green = (color.green * 255f).roundToInt().coerceIn(0, 255)
    val blue = (color.blue * 255f).roundToInt().coerceIn(0, 255)
    val hex = "#%02X%02X%02X".format(red, green, blue)

    Column(modifier = modifier) {
        RgbChannelSlider(
            label = "R",
            value = red,
            trackColor = Color(red = red, green = 0, blue = 0),
            onValueChange = { onColorPick(Color(red = it, green = green, blue = blue)) },
        )
        Spacer(Modifier.height(8.dp))
        RgbChannelSlider(
            label = "G",
            value = green,
            trackColor = Color(red = 0, green = green, blue = 0),
            onValueChange = { onColorPick(Color(red = red, green = it, blue = blue)) },
        )
        Spacer(Modifier.height(8.dp))
        RgbChannelSlider(
            label = "B",
            value = blue,
            trackColor = Color(red = 0, green = 0, blue = blue),
            onValueChange = { onColorPick(Color(red = red, green = green, blue = it)) },
        )
        Spacer(Modifier.height(12.dp))
        val hexRowInteractionSource = remember { MutableInteractionSource() }
        val hexRowScale = rememberPressScale(hexRowInteractionSource)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = hexRowScale; scaleY = hexRowScale }
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(
                    interactionSource = hexRowInteractionSource,
                    indication = null,
                    onClick = {
                        clipboardManager.setText(AnnotatedString(hex))
                        justCopied = true
                    },
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = hex,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
            )
            Icon(
                imageVector = if (justCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                contentDescription = stringResource(
                    if (justCopied) R.string.annotate_hex_copied_desc else R.string.annotate_hex_copy_desc,
                ),
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RgbChannelSlider(
    label: String,
    value: Int,
    trackColor: Color,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            RgbValueField(value = value, onValueChange = onValueChange)
        }
        LetterlySlider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(0, 255)) },
            valueRange = 0f..255f,
            trackColor = trackColor,
        )
    }
}

/**
 * Typable numeric field for a single 0-255 RGB channel, so the user can key
 * in an exact value instead of only dragging the slider. Keyed on [value] so
 * external changes (slider drag, a fresh color pick) resync the field, while
 * an in-progress edit that doesn't parse yet (e.g. a cleared field) is left
 * alone since [value] hasn't changed and the key doesn't fire.
 */
@Composable
private fun RgbValueField(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    BasicTextField(
        value = text,
        onValueChange = { newText ->
            val digitsOnly = newText.filter { it.isDigit() }.take(3)
            text = digitsOnly
            digitsOnly.toIntOrNull()?.let { parsed -> onValueChange(parsed.coerceIn(0, 255)) }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.labelSmall.copy(color = Color.White, textAlign = TextAlign.End),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        cursorBrush = SolidColor(Color.White),
        modifier = modifier
            .width(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
private fun OpacitySlider(
    alpha: Float,
    trackColor: Color,
    onAlphaChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.annotate_opacity_label),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                "${(alpha * 100).roundToInt()}%",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        LetterlySlider(
            value = alpha,
            onValueChange = onAlphaChange,
            valueRange = 0f..1f,
            trackColor = trackColor,
        )
    }
}

private val SpectrumHueStops = (0..12).map { step -> Color.hsv(step * 30f, saturation = 1f, value = 1f) }

/** Clamps a pointer position (tap/drag) to this box's own bounds — a drag
 * isn't clipped once captured, so raw positions can land outside [0, size]. */
private fun Offset.coerceInBox(size: IntSize): Offset =
    Offset(x.coerceIn(0f, size.width.toFloat()), y.coerceIn(0f, size.height.toFloat()))

/** [hue] is also defensively clamped here (not just at the [SpectrumColorPicker]
 * call site) since [Color.hsv] throws for anything outside 0..360 — this
 * keeps the function safe for any caller, not just ones that already clamp. */
private fun colorForHueAndLightness(hue: Float, yFraction: Float): Color {
    val clampedHue = hue.coerceIn(0f, 360f)
    val clampedY = yFraction.coerceIn(0f, 1f)
    return if (clampedY <= 0.5f) {
        Color.hsv(clampedHue, saturation = clampedY / 0.5f, value = 1f)
    } else {
        Color.hsv(clampedHue, saturation = 1f, value = 1f - (clampedY - 0.5f) / 0.5f)
    }
}
