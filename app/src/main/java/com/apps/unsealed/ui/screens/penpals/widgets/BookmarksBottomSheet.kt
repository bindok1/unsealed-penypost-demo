package com.apps.unsealed.ui.screens.penpals.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.components.LetterlyButton
import com.apps.unsealed.ui.components.LetterlyCenterDialog
import com.apps.unsealed.ui.screens.penpals.state.PenpalLetter
import com.apps.unsealed.ui.screens.penpals.state.formattedPostedAt
import com.apps.unsealed.ui.theme.BrandCardDark
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInk

private val DestructiveRed = Color(0xFFE53935)

/**
 * PenPals "saved letters" list, opened from [OpenLetterOverlay]'s bookmark
 * snackbar action. Row tap opens the reading view at that letter (caller's
 * responsibility — see `onLetterClick`'s index-into-[letters] contract,
 * matching [OpenLetterOverlay]'s own `letters`/`initialIndex` shape so the
 * caller can reuse the same overlay for browsing bookmarks). "Select" toggles
 * a multi-select mode for batch removal — removing here only forgets the
 * local bookmark, never touches the letter itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksBottomSheet(
    letters: List<PenpalLetter>,
    onDismiss: () -> Unit,
    onLetterClick: (Int) -> Unit,
    onRemove: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var pendingRemoveConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrandCardDark,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.bookmarks_sheet_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                )
                if (letters.isNotEmpty()) {
                    val toggleInteractionSource = remember { MutableInteractionSource() }
                    Text(
                        text = if (isSelectionMode) {
                            stringResource(R.string.bookmarks_sheet_cancel)
                        } else {
                            stringResource(R.string.bookmarks_sheet_select)
                        },
                        color = BrandGold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = toggleInteractionSource,
                                indication = null,
                                onClick = {
                                    isSelectionMode = !isSelectionMode
                                    selectedIds = emptySet()
                                },
                            )
                            .padding(8.dp),
                    )
                }
            }

            if (letters.isEmpty()) {
                BookmarksEmptyState(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(letters, key = { _, letter -> letter.id }) { index, letter ->
                        BookmarkRow(
                            letter = letter,
                            isSelectionMode = isSelectionMode,
                            isSelected = letter.id in selectedIds,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedIds = if (letter.id in selectedIds) {
                                        selectedIds - letter.id
                                    } else {
                                        selectedIds + letter.id
                                    }
                                } else {
                                    onLetterClick(index)
                                }
                            },
                        )
                    }
                }

                if (isSelectionMode && selectedIds.isNotEmpty()) {
                    LetterlyButton(
                        text = stringResource(R.string.bookmarks_sheet_remove_selected, selectedIds.size),
                        onClick = { pendingRemoveConfirm = true },
                        containerColor = DestructiveRed,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 20.dp),
                    )
                }
            }
        }
    }

    if (pendingRemoveConfirm) {
        LetterlyCenterDialog(
            title = stringResource(R.string.bookmarks_remove_confirm_title),
            body = stringResource(R.string.bookmarks_remove_confirm_body),
            primaryCtaText = stringResource(R.string.bookmarks_remove_confirm_cta),
            onPrimaryClick = {
                onRemove(selectedIds)
                selectedIds = emptySet()
                isSelectionMode = false
                pendingRemoveConfirm = false
            },
            secondaryCtaText = stringResource(R.string.bookmarks_remove_confirm_cancel),
            onSecondaryClick = { pendingRemoveConfirm = false },
            onDismissRequest = { pendingRemoveConfirm = false },
            primaryContainerColor = DestructiveRed,
            primaryContentColor = Color.White,
        )
    }
}

@Composable
private fun BookmarkRow(
    letter: PenpalLetter,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelectionMode) {
            SelectionCheckCircle(isSelected = isSelected)
            Spacer(Modifier.width(10.dp))
        }
        AsyncImage(
            model = letter.envelopeImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(letter.envelope.drawableRes),
            error = painterResource(letter.envelope.drawableRes),
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
        )
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = letter.senderName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = letter.bodyText.take(80),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = letter.formattedPostedAt(),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
            )
        }
        if (!isSelectionMode) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun SelectionCheckCircle(isSelected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (isSelected) BrandGold else Color.Transparent)
            .border(1.5.dp, if (isSelected) BrandGold else Color.White.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = BrandInk,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun BookmarksEmptyState(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Filled.BookmarkBorder,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.bookmarks_sheet_empty_title),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.bookmarks_sheet_empty_subtitle),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}
