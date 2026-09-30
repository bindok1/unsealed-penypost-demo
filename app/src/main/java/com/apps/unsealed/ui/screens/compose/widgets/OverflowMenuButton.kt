package com.apps.unsealed.ui.screens.compose.widgets

import com.apps.unsealed.ui.screens.compose.state.*

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.apps.unsealed.R
import com.apps.unsealed.ui.components.AnchoredDropdownMenu
import com.apps.unsealed.ui.components.DropdownMenuAction

private val DefaultButtonSize = 48.dp
private val DefaultIconSize = 24.dp
private val DropdownGap = 6.dp

/**
 * Real overflow menu (compose-screen-spec.md §6), replacing the
 * `ComingSoonBottomSheet` stub previously shown for the "···" toolbar
 * button. Built on the shared [AnchoredDropdownMenu] chrome (also used by
 * the per-image actions menu in `LetterCanvas.kt`) instead of a
 * `ModalBottomSheet` — the trigger sits at the top of the screen, so the menu
 * unfurls downward from the icon instead of sliding up from the bottom edge.
 * "Ganti Kertas" and "Sisipkan Gambar" are wired to real flows by the caller;
 * the other three items are still stubs the caller routes to
 * `ComingSoonBottomSheet`.
 *
 * [buttonSize]/[iconSize] default to Material3's own IconButton/Icon sizes,
 * but [ComposeToolbar] overrides both smaller — shrinking just the icon
 * glyph doesn't shrink the button's own touch-target box, so both need to
 * move together to actually save horizontal space in the pill.
 */
@Composable
fun OverflowMenuButton(
    onChangePaperClick: () -> Unit,
    onInsertImageClick: () -> Unit,
    onAddStickerClick: () -> Unit,
    onSaveToPhotosClick: () -> Unit,
    onShareClick: () -> Unit,
    onTogglePublicShowcaseClick: () -> Unit,
    onPenyModeToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    buttonSize: Dp = DefaultButtonSize,
    iconSize: Dp = DefaultIconSize,
    isCompositingSupported: Boolean = true,
    isPublicShowcase: Boolean = false,
    isPenyModeActive: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(buttonSize)) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.toolbar_more_desc),
                tint = Color.White,
                modifier = Modifier.size(iconSize),
            )
        }

        AnchoredDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            alignment = Alignment.TopEnd,
            offset = with(density) { IntOffset(0, (buttonSize + DropdownGap).roundToPx()) },
            transformOrigin = TransformOrigin(1f, 0f),
        ) {
            DropdownMenuAction(
                icon = Icons.Filled.Description,
                label = stringResource(R.string.overflow_menu_change_paper),
                onClick = { expanded = false; onChangePaperClick() },
                disabled = isPenyModeActive,
            )
            DropdownMenuAction(
                icon = Icons.Filled.Image,
                label = stringResource(R.string.overflow_menu_insert_image),
                onClick = { expanded = false; onInsertImageClick() },
                disabled = !isCompositingSupported || isPenyModeActive,
            )
            DropdownMenuAction(
                icon = Icons.Filled.EmojiEmotions,
                label = stringResource(R.string.overflow_menu_add_sticker),
                onClick = { expanded = false; onAddStickerClick() },
                disabled = !isCompositingSupported || isPenyModeActive,
            )
            DropdownMenuAction(
                icon = Icons.Filled.Download,
                label = stringResource(R.string.overflow_menu_save_photos),
                onClick = { expanded = false; onSaveToPhotosClick() },
            )
            DropdownMenuAction(
                icon = Icons.Filled.Share,
                label = stringResource(R.string.overflow_menu_share),
                onClick = { expanded = false; onShareClick() },
            )
            DropdownMenuAction(
                icon = if (isPublicShowcase) Icons.Filled.Public else Icons.Filled.Lock,
                label = stringResource(
                    if (isPublicShowcase) R.string.overflow_menu_public_showcase_disable
                    else R.string.overflow_menu_public_showcase_enable,
                ),
                onClick = { expanded = false; onTogglePublicShowcaseClick() },
            )
        }
    }
}
