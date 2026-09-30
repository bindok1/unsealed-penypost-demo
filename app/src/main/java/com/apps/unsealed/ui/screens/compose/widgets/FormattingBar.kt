package com.apps.unsealed.ui.screens.compose.widgets

import com.apps.unsealed.ui.screens.compose.state.*

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.ui.components.LetterlySlider
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first

private val IconButtonSize = 48.dp
private val TextSizePopupWidth = 240.dp
private const val PopupExitMillis = 250

/**
 * Bar above the keyboard per compose-screen-spec.md §4. Alignment,
 * hide-keyboard, Bold/Italic, Indent, and the Aa shortcut (opens the same
 * Font Picker sheet as the toolbar's Aa icon) are functional. Bold/Italic/
 * size/Indent are selection-aware (see [ComposeViewModel.onBoldToggle] et
 * al. and [TextStyleRuns.kt][StyleRun]) — with an active text selection they
 * restyle only that range; with just a cursor they set a "pending" style
 * that applies to the next characters typed instead. Bold/Italic use
 * [PillIconToggleButton] for proper motion-rules.md §3.1/§4 feedback (no
 * ripple, scale+pill); the pre-existing buttons in this bar still use
 * default IconButton ripple, left as-is (out of scope here).
 *
 * Text size and ink color live here now instead of inside the Font Picker
 * sheet ("Choose Writing Style") — they're changed often enough while
 * writing that burying them one sheet deep made them hard to reach/see.
 * [TextSizeButton] pops a small slider card above the bar (reusing
 * [LetterlySlider]); ink color reuses the exact same [AnnotateColorButton]
 * trigger+popup already built for Annotate Mode's ink picker.
 */
@Composable
fun FormattingBar(
    paperColor: Color,
    textAlignment: TextAlign,
    isBold: Boolean,
    isItalic: Boolean,
    fontSize: Float,
    inkColor: Color,
    onFontClick: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onInkColorChange: (Color) -> Unit,
    onAlignmentClick: () -> Unit,
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onIndentClick: () -> Unit,
    onHideKeyboardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // WindowInsets.ime's bottom value already includes the navigation bar's
    // height (the keyboard docks above the nav bar, and ime is measured from
    // the true screen bottom) — but MainActivity's Scaffold already reserved
    // that same navigation-bar space via innerPadding on the NavHost. A bare
    // .imePadding() here would double-subtract it, pushing this bar up past
    // the real keyboard and leaving a gap. Subtracting navigationBars back
    // out leaves just the keyboard's own height.
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val navBarInsets = WindowInsets.navigationBars
    fun rawKeyboardPaddingPx() = (imeInsets.getBottom(density) - navBarInsets.getBottom(density)).coerceAtLeast(0)

    // The platform's floating Cut/Copy/Paste selection toolbar causes
    // WindowInsets.ime to fluctuate while the keyboard stays open — not just a
    // one-frame glitch, so a plain debounce still lets those changes through
    // and the bar keeps resizing. Instead, take the height once it settles
    // (no change for 80ms) and then LOCK it: stop observing WindowInsets.ime
    // entirely for the rest of this composable's lifetime. FormattingBar only
    // enters composition while the keyboard is visible (see ComposeScreen's
    // isKeyboardVisible branch) and leaves it when the keyboard closes, so the
    // lock naturally resets and re-settles the next time the keyboard opens.
    var keyboardOnlyPadding by remember { mutableStateOf(with(density) { rawKeyboardPaddingPx().toDp() }) }
    LaunchedEffect(imeInsets, navBarInsets, density) {
        val settledPx = snapshotFlow { rawKeyboardPaddingPx() }
            .debounce(80)
            .first()
        keyboardOnlyPadding = with(density) { settledPx.toDp() }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            // Order matters: padding before background keeps the painted bar
            // pinned to its compact content height instead of the color
            // bleeding into the reserved keyboard space below it.
            .padding(bottom = keyboardOnlyPadding)
            .background(paperColor.copy(alpha = 0.9f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onFontClick) {
            Icon(Icons.Filled.TextFields, contentDescription = stringResource(R.string.formatting_bar_font_desc), tint = Color.Black)
        }
        TextSizeButton(fontSize = fontSize, onFontSizeChange = onFontSizeChange)
        AnnotateColorButton(
            selectedColor = inkColor,
            onColorSelect = onInkColorChange,
        )
        IconButton(onClick = onAlignmentClick) {
            Icon(
                imageVector = when (textAlignment) {
                    TextAlign.Center -> Icons.Filled.FormatAlignCenter
                    TextAlign.End -> Icons.Filled.FormatAlignRight
                    else -> Icons.Filled.FormatAlignLeft
                },
                contentDescription = stringResource(R.string.formatting_bar_alignment_desc),
                tint = Color.Black,
            )
        }
        PillIconToggleButton(
            icon = Icons.Filled.FormatBold,
            contentDescription = stringResource(R.string.font_picker_bold_desc),
            isSelected = isBold,
            onClick = onBoldClick,
            unselectedTint = Color.Black,
        )
        PillIconToggleButton(
            icon = Icons.Filled.FormatItalic,
            contentDescription = stringResource(R.string.font_picker_italic_desc),
            isSelected = isItalic,
            onClick = onItalicClick,
            unselectedTint = Color.Black,
        )
        IconButton(onClick = onIndentClick) {
            Icon(Icons.Filled.FormatIndentIncrease, contentDescription = stringResource(R.string.formatting_bar_indent_desc), tint = Color.Black)
        }
        IconButton(onClick = onHideKeyboardClick) {
            Icon(Icons.Filled.KeyboardHide, contentDescription = stringResource(R.string.formatting_bar_hide_keyboard_desc), tint = Color.Black)
        }
    }
}

/**
 * Icon trigger + anchored popup for [ComposeUiState.fontSize], following the
 * same self-contained trigger/[Popup] pattern as [AnnotateColorButton] and
 * [OverflowMenuButton] — Gentle-in/Stiff-out scale+alpha (motion-rules.md
 * §3.4), dark chrome [Surface] matching every other floating picker on this
 * screen. Popup grows upward since this bar sits at the bottom of the
 * screen, above the keyboard.
 */
@Composable
private fun TextSizeButton(
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    var isPopupVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val reduced = rememberIsReducedMotion()

    LaunchedEffect(showPicker) {
        if (showPicker) isPopupVisible = true
    }

    Box {
        IconButton(onClick = { showPicker = true }) {
            Icon(
                Icons.Filled.FormatSize,
                contentDescription = stringResource(R.string.formatting_bar_text_size_desc),
                tint = Color.Black,
            )
        }

        if (showPicker) {
            Popup(
                alignment = Alignment.BottomCenter,
                offset = with(density) { IntOffset(0, -(IconButtonSize + 12.dp).roundToPx()) },
                onDismissRequest = { isPopupVisible = false },
                properties = PopupProperties(focusable = true),
            ) {
                val popupScale by animateFloatAsState(
                    targetValue = if (isPopupVisible) 1f else 0.85f,
                    animationSpec = reducedMotionSpring(
                        if (isPopupVisible) LetterlySpring.Gentle else LetterlySpring.Stiff,
                        reduced,
                    ),
                    label = "textSizePopupScale",
                )
                val popupAlpha by animateFloatAsState(
                    targetValue = if (isPopupVisible) 1f else 0f,
                    animationSpec = tween(if (isPopupVisible) 380 else PopupExitMillis),
                    label = "textSizePopupAlpha",
                    finishedListener = { finalValue -> if (finalValue <= 0f) showPicker = false },
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF2A241C),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = popupScale
                            scaleY = popupScale
                            alpha = popupAlpha
                        }
                        .width(TextSizePopupWidth),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.formatting_bar_text_size_small),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        LetterlySlider(
                            value = fontSize,
                            onValueChange = onFontSizeChange,
                            valueRange = ComposeFontSizeMin..ComposeFontSizeMax,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        Text(
                            stringResource(R.string.formatting_bar_text_size_large),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}
