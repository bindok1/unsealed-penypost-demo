package com.apps.unsealed.ui.screens.compose.widgets

import com.apps.unsealed.ui.screens.compose.state.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apps.unsealed.R
import com.apps.unsealed.ui.theme.LemonYellow
import com.apps.unsealed.ui.theme.ToolbarFrostedDark
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Floating "liquid glass" pill toolbar per compose-screen-spec.md §1 —
 * deliberately NOT a flush edge-to-edge Material app bar (that read as
 * generic Android). It floats over the paper with margin on all sides,
 * fully rounded stadium shape, a real backdrop blur of the paper behind it
 * (via Haze's [hazeEffect], reading from [LetterCanvas]'s [hazeSource]),
 * a faint glass-edge highlight border, and a soft shadow so it visually
 * lifts off the canvas. On API < 31 (no RenderEffect support) Haze
 * automatically falls back to a plain tint instead of a real blur.
 *
 * Send deliberately lives OUTSIDE that pill, in its own solid (non-blurred)
 * circular [SendButton] separated by a gap — it's the one control here that
 * isn't a toggle/formatting affordance but a distinct, final action, so it
 * shouldn't visually read as "one more icon in the group."
 *
 * Pill icon buttons are sized down to [ToolbarIconButtonSize] (40dp) instead
 * of Material3 IconButton's 48dp default — shrinking just the glyph inside
 * (`Icon`'s own size) doesn't change the button's own touch-target box, so
 * on narrow screens the pill was still wide enough to push the "···" button
 * out of view. At 40dp, six buttons + the pill's padding fit inside a
 * typical ~360dp-wide phone alongside [SendButton] with room to spare. The
 * pill is *also* `weight(1f, fill = false)` + horizontally scrollable as a
 * safety net for anything narrower still — an unweighted `Row` doesn't
 * shrink, it just overflows past the screen edge and clips [SendButton]
 * down to almost nothing, which is the bug this whole structure avoids.
 *
 * Undo/Redo are mode-aware (compose-screen-spec.md §1): [canUndo]/[canRedo]
 * and their click handlers reflect whichever history is relevant for the
 * current mode (text vs. Annotate strokes) — see
 * [ComposeUiState.canUndo]/[ComposeViewModel.onUndoClick].
 */
private val ToolbarIconButtonSize = 40.dp
private val ToolbarIconSize = 20.dp

@Composable
fun ComposeToolbar(
    isAnnotateMode: Boolean,
    isKeyboardVisible: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    isPenyModeActive: Boolean = false,
    isPenyModeAvailable: Boolean = false,
    isPenyReplyLoading: Boolean = false,
    showPenyDiscoveryBadge: Boolean = false,
    hazeState: HazeState,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onFontClick: () -> Unit,
    onKeyboardToggleClick: () -> Unit,
    onAnnotateClick: () -> Unit,
    onChangePaperClick: () -> Unit,
    onInsertImageClick: () -> Unit,
    onAddStickerClick: () -> Unit,
    onSaveToPhotosClick: () -> Unit,
    onShareClick: () -> Unit,
    onTogglePublicShowcaseClick: () -> Unit,
    onPenyModeToggle: () -> Unit = {},
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSending: Boolean = false,
    isCompositingSupported: Boolean = true,
    isPublicShowcase: Boolean = false,
) {
    val pillShape = RoundedCornerShape(percent = 50)
    val hazeStyle = remember {
        HazeDefaults.style(
            backgroundColor = ToolbarFrostedDark,
            tint = HazeTint(ToolbarFrostedDark.copy(alpha = 0.55f)),
            blurRadius = 20.dp,
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .shadow(elevation = 16.dp, shape = pillShape, clip = false)
                .clip(pillShape)
                .hazeEffect(state = hazeState, style = hazeStyle)
                .border(1.dp, Color.White.copy(alpha = 0.16f), pillShape)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onUndoClick, enabled = canUndo, modifier = Modifier.size(ToolbarIconButtonSize)) {
                Icon(
                    Icons.Filled.Undo,
                    contentDescription = stringResource(R.string.toolbar_undo_desc),
                    tint = Color.White.copy(alpha = if (canUndo) 1f else 0.3f),
                    modifier = Modifier.size(ToolbarIconSize),
                )
            }
            IconButton(onClick = onRedoClick, enabled = canRedo, modifier = Modifier.size(ToolbarIconButtonSize)) {
                Icon(
                    Icons.Filled.Redo,
                    contentDescription = stringResource(R.string.toolbar_redo_desc),
                    tint = Color.White.copy(alpha = if (canRedo) 1f else 0.3f),
                    modifier = Modifier.size(ToolbarIconSize),
                )
            }
            IconButton(onClick = onFontClick, modifier = Modifier.size(ToolbarIconButtonSize)) {
                Icon(
                    Icons.Filled.TextFields,
                    contentDescription = stringResource(R.string.toolbar_font_desc),
                    tint = Color.White,
                    modifier = Modifier.size(ToolbarIconSize),
                )
            }
            IconButton(onClick = onKeyboardToggleClick, modifier = Modifier.size(ToolbarIconButtonSize)) {
                Icon(
                    imageVector = if (isKeyboardVisible) Icons.Filled.KeyboardHide else Icons.Filled.Keyboard,
                    contentDescription = stringResource(R.string.toolbar_keyboard_desc),
                    tint = Color.White,
                    modifier = Modifier.size(ToolbarIconSize),
                )
            }
            IconButton(
                onClick = onAnnotateClick,
                enabled = !isPenyModeActive,
                modifier = Modifier
                    .size(ToolbarIconButtonSize)
                    .then(if (isAnnotateMode) Modifier.background(LemonYellow, CircleShape) else Modifier),
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.toolbar_annotate_desc),
                    tint = when {
                        isPenyModeActive || !isCompositingSupported -> Color.White.copy(alpha = 0.3f)
                        isAnnotateMode -> Color.Black
                        else -> Color.White.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.size(ToolbarIconSize),
                )
            }
            // Mode Peny's entry point — grouped in here with the other tool
            // icons instead of floating over the paper (where it used to
            // live, see PenyWandHint's own doc comment) so the toolbar stays
            // the one place all of Compose's interactive controls live.
            // Gated the same way the old glyph was: hidden entirely rather
            // than rendered-then-disabled when unavailable (peny-mode-spec.md
            // §6). Placed right before the overflow "···" button — the pill's
            // existing horizontalScroll (see this composable's doc comment)
            // already covers the case where this pushes it past 6 buttons on
            // a narrow screen.
            if (isPenyModeAvailable) {
                PenyWandHint(
                    isPenyModeActive = isPenyModeActive,
                    isPenyReplyLoading = isPenyReplyLoading,
                    showDiscoveryBadge = showPenyDiscoveryBadge,
                    onClick = onPenyModeToggle,
                )
            }
            OverflowMenuButton(
                onChangePaperClick = onChangePaperClick,
                onInsertImageClick = onInsertImageClick,
                onAddStickerClick = onAddStickerClick,
                onSaveToPhotosClick = onSaveToPhotosClick,
                onShareClick = onShareClick,
                onTogglePublicShowcaseClick = onTogglePublicShowcaseClick,
                buttonSize = ToolbarIconButtonSize,
                iconSize = ToolbarIconSize,
                isCompositingSupported = isCompositingSupported,
                isPublicShowcase = isPublicShowcase,
                isPenyModeActive = isPenyModeActive,
                onPenyModeToggle = onPenyModeToggle,
            )
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(start = 8.dp)) {
            SendButton(onClick = { if (!isSending) onSendClick() })
            // Compositing + presign-upload (see LetterCompositor.kt/ComposeViewModel.
            // compositeAndSaveDraftForSend) takes a real network round trip now
            // instead of being instant — this both signals progress and guards
            // against a double-tap firing two uploads.
            if (isSending) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
