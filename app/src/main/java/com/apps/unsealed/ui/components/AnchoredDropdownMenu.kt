package com.apps.unsealed.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.core.util.rememberPressScale

private val DropdownDefaultWidth = 220.dp
private const val DropdownExitMillis = 250

/**
 * Shared dark-chrome anchored dropdown shell — the same floating-Popup +
 * scale/alpha-in/out pattern originally built for [OverflowMenuButton]
 * (compose-screen-spec.md §6). Extracted per `docs/component-library.md`'s
 * rule: once a control (here, "a dark anchored dropdown menu with icon+label
 * rows") is needed a second time — the per-image actions menu in
 * `LetterCanvas.kt` — it becomes a shared component instead of a second
 * copy-pasted implementation.
 *
 * The caller only owns a single [expanded] boolean (true = open, false =
 * request-close, e.g. from an item's `onClick` or a dismiss tap) — this
 * composable manages the mount-then-animate-in / animate-out-then-unmount
 * lifecycle internally so the exit animation always finishes before the
 * `Popup` leaves composition, matching motion-rules.md §3.4.
 */
@Composable
fun AnchoredDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopEnd,
    offset: IntOffset = IntOffset.Zero,
    transformOrigin: TransformOrigin = TransformOrigin(1f, 0f),
    width: Dp = DropdownDefaultWidth,
    content: @Composable () -> Unit,
) {
    var isPresent by remember { mutableStateOf(expanded) }
    var isVisible by remember { mutableStateOf(false) }
    val reduced = rememberIsReducedMotion()

    LaunchedEffect(expanded) {
        if (expanded) {
            isPresent = true
            isVisible = true
        } else {
            isVisible = false
        }
    }

    if (isPresent) {
        Popup(
            alignment = alignment,
            offset = offset,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
        ) {
            val menuScale by animateFloatAsState(
                targetValue = if (isVisible) 1f else 0.85f,
                animationSpec = reducedMotionSpring(
                    if (isVisible) LetterlySpring.Gentle else LetterlySpring.Stiff,
                    reduced,
                ),
                label = "dropdownMenuScale",
            )
            val menuAlpha by animateFloatAsState(
                targetValue = if (isVisible) 1f else 0f,
                animationSpec = tween(if (isVisible) 380 else DropdownExitMillis),
                label = "dropdownMenuAlpha",
                finishedListener = { finalValue -> if (finalValue <= 0f) isPresent = false },
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2A241C),
                shadowElevation = 12.dp,
                modifier = modifier
                    .width(width)
                    .graphicsLayer {
                        scaleX = menuScale
                        scaleY = menuScale
                        alpha = menuAlpha
                        this.transformOrigin = transformOrigin
                    },
            ) {
                Column(Modifier.padding(vertical = 6.dp)) { content() }
            }
        }
    }
}

/** One icon+label row inside an [AnchoredDropdownMenu] — scale-only press
 * feedback via [rememberPressScale], no ripple (motion-rules.md §3.1/§4).
 *
 * [disabled] only dims the row (e.g. a feature this device can't do — see
 * `DeviceCapabilityBottomSheet.kt`) — [onClick] still fires either way, so
 * the caller can show an explainer instead of just no-oping the tap. */
@Composable
fun DropdownMenuAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    val contentAlpha = if (disabled) 0.4f else 1f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = contentAlpha),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = contentAlpha),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 14.dp),
        )
    }
}
