package com.apps.unsealed.ui.screens.inbox.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.components.AnchoredDropdownMenu
import com.apps.unsealed.ui.components.DropdownMenuAction
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.ToolbarFrostedDark

private val HeaderButtonSize = 40.dp
private val DropdownGap = 6.dp

/** Thread-level header shown once per [com.apps.unsealed.ui.screens.inbox.screen.MailboxThreadScreen]
 * (back button + centered profile avatar/name/region + report/add-friend/block menu). */
@Composable
fun ThreadHeader(
    correspondentName: String,
    correspondentContinent: String,
    onBackClick: () -> Unit,
    onViewProfileClick: () -> Unit,
    onReportUserClick: () -> Unit,
    onAddFriendClick: () -> Unit,
    onBlockUserClick: () -> Unit,
    modifier: Modifier = Modifier,
    correspondentAvatarUrl: String? = null,
    isBlocked: Boolean = false,
    onUnblockUserClick: () -> Unit = {},
) {
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        FrostedCircleButton(
            icon = Icons.Filled.ArrowBack,
            contentDescription = stringResource(R.string.open_letter_back_desc),
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.TopStart),
        )

        val nameRowInteractionSource = remember { MutableInteractionSource() }
        val nameRowPressScale = rememberPressScale(nameRowInteractionSource)
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { scaleX = nameRowPressScale; scaleY = nameRowPressScale }
                .clip(RoundedCornerShape(50))
                .clickable(
                    interactionSource = nameRowInteractionSource,
                    indication = null,
                    onClick = onViewProfileClick,
                )
                .padding(horizontal = 48.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(1.5.dp, BrandGold.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (!correspondentAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = correspondentAvatarUrl,
                        contentDescription = correspondentName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column {
                Text(
                    text = correspondentName,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = correspondentContinent,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        LetterMoreMenu(
            isBlocked = isBlocked,
            onViewProfileClick = onViewProfileClick,
            onReportUserClick = onReportUserClick,
            onAddFriendClick = onAddFriendClick,
            onBlockUserClick = onBlockUserClick,
            onUnblockUserClick = onUnblockUserClick,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun LetterMoreMenu(
    isBlocked: Boolean,
    onViewProfileClick: () -> Unit,
    onReportUserClick: () -> Unit,
    onAddFriendClick: () -> Unit,
    onBlockUserClick: () -> Unit,
    onUnblockUserClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    Box(modifier = modifier) {
        FrostedCircleButton(
            icon = Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.open_letter_more_desc),
            onClick = { expanded = true },
        )

        AnchoredDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            alignment = Alignment.TopEnd,
            offset = with(density) { IntOffset(0, (HeaderButtonSize + DropdownGap).roundToPx()) },
            transformOrigin = TransformOrigin(1f, 0f),
        ) {
            DropdownMenuAction(
                icon = Icons.Filled.Person,
                label = stringResource(R.string.open_letter_menu_view_profile),
                onClick = { expanded = false; onViewProfileClick() },
            )
            DropdownMenuAction(
                icon = Icons.Filled.Flag,
                label = stringResource(R.string.open_letter_report_user),
                onClick = { expanded = false; onReportUserClick() },
            )
            DropdownMenuAction(
                icon = Icons.Filled.PersonAdd,
                label = stringResource(R.string.open_letter_add_friend),
                onClick = { expanded = false; onAddFriendClick() },
            )
            if (isBlocked) {
                DropdownMenuAction(
                    icon = Icons.Filled.LockOpen,
                    label = stringResource(R.string.open_letter_unblock_user),
                    onClick = { expanded = false; onUnblockUserClick() },
                )
            } else {
                DropdownMenuAction(
                    icon = Icons.Filled.Block,
                    label = stringResource(R.string.open_letter_block_user),
                    onClick = { expanded = false; onBlockUserClick() },
                )
            }
        }
    }
}

/** Shared 40dp frosted circular icon button — same shape as
 * `OpenLetterOverlay`'s `CloseButton` (PenPals feature), reused here for both
 * the back arrow and the 3-dot trigger. */
@Composable
private fun FrostedCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .size(HeaderButtonSize)
            .clip(CircleShape)
            .background(ToolbarFrostedDark)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White)
    }
}
