package com.apps.unsealed.ui.screens.inbox.screen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.apps.unsealed.R
import com.apps.unsealed.ui.components.LetterlyCenterDialog
import com.apps.unsealed.ui.components.ReportMailDialog
import com.apps.unsealed.ui.components.ReportReason
import com.apps.unsealed.ui.screens.compose.widgets.ComingSoonBottomSheet
import com.apps.unsealed.ui.screens.inbox.state.LetterDeliveryStatus
import com.apps.unsealed.ui.screens.inbox.state.MailboxThreadUiState
import com.apps.unsealed.ui.screens.inbox.state.ThreadBlockState
import com.apps.unsealed.ui.screens.inbox.state.ThreadLetterItem
import com.apps.unsealed.ui.screens.inbox.state.ThreadReportState
import com.apps.unsealed.ui.screens.inbox.viewmodel.MailboxThreadViewModel
import com.apps.unsealed.ui.screens.inbox.widgets.EnvelopePaperReveal
import com.apps.unsealed.ui.screens.inbox.widgets.FullLetterViewMode
import com.apps.unsealed.ui.screens.inbox.widgets.IncomingMailContent
import com.apps.unsealed.ui.screens.inbox.widgets.MailboxFullLetterViewer
import com.apps.unsealed.ui.screens.inbox.widgets.ModeCaption
import com.apps.unsealed.ui.screens.inbox.widgets.OutgoingTrackingContent
import com.apps.unsealed.ui.screens.inbox.widgets.ThreadHeader
import com.apps.unsealed.ui.screens.inbox.widgets.WriteLetterButton
import com.apps.unsealed.ui.screens.publicprofile.PublicProfileBottomSheet
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.ToolbarFrostedDark

/**
 * Chat-style thread view pushed from [com.apps.unsealed.ui.screens.inbox.screen.MailboxScreen]
 * when a room is tapped. Fetches the whole conversation once
 * (`GET /mailbox/{threadId}`) and renders every letter inline — no per-letter
 * re-fetch, unlike the old single-letter `OpenLetterScreen`. Each letter is
 * aligned left (received) or right (sent) — "chat kiri-kanan" — a plain
 * alignment choice, not a speech-bubble shape.
 *
 * Background (moody bokeh) and bottom-nav visibility are owned by
 * [com.apps.unsealed.MainActivity] for this route, same as [MailboxScreen] —
 * this composable is fully transparent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailboxThreadScreen(
    onBackClick: () -> Unit,
    onWriteLetterClick: (recipientId: String, recipientName: String, recipientContinent: String) -> Unit,
    // Carries the correspondent's id/name/continent alongside draftId (not
    // just draftId) so the eventual Send step already knows the recipient
    // instead of falling back to the full SelectRecipient picker — see
    // UnsealedNavHost's WriteReplyDraftRoutePattern.
    onContinueDraftClick: (draftId: String, recipientId: String, recipientName: String, recipientContinent: String) -> Unit,
    onTrackLetterClick: (letterId: String, otherPartyContinent: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MailboxThreadViewModel = hiltViewModel(),
) {
    var comingSoonFeatureRes by remember { mutableStateOf<Int?>(null) }
    var showPublicProfileSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showUnblockConfirmDialog by remember { mutableStateOf(false) }
    var selectedOutgoingLetter by remember { mutableStateOf<ThreadLetterItem?>(null) }
    var fullViewRequest by remember { mutableStateOf<FullViewRequest?>(null) }
    var isFullViewOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val reportState by viewModel.reportState.collectAsState()
    val blockState by viewModel.blockState.collectAsState()

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadThread(isSilent = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(reportState) {
        when (val state = reportState) {
            is ThreadReportState.Submitted -> {
                showReportDialog = false
                Toast.makeText(
                    context,
                    context.getString(R.string.public_profile_report_submitted_toast),
                    Toast.LENGTH_SHORT,
                ).show()
                viewModel.resetReportState()
            }
            is ThreadReportState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetReportState()
            }
            else -> Unit
        }
    }

    LaunchedEffect(blockState) {
        when (val state = blockState) {
            is ThreadBlockState.Blocked -> {
                Toast.makeText(context, context.getString(R.string.block_user_blocked_toast), Toast.LENGTH_SHORT).show()
                viewModel.resetBlockState()
            }
            is ThreadBlockState.Unblocked -> {
                Toast.makeText(context, context.getString(R.string.block_user_unblocked_toast), Toast.LENGTH_SHORT).show()
                viewModel.resetBlockState()
            }
            is ThreadBlockState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetBlockState()
            }
            else -> Unit
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is MailboxThreadUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White)
            }
            is MailboxThreadUiState.Error -> Box(
                modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.message,
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            is MailboxThreadUiState.Success -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp),
                    ) {
                        Spacer(Modifier.height(8.dp))

                        ThreadHeader(
                            correspondentName = state.correspondentName,
                            correspondentContinent = state.correspondentContinent,
                            correspondentAvatarUrl = state.correspondentAvatarUrl,
                            onBackClick = onBackClick,
                            onViewProfileClick = { showPublicProfileSheet = true },
                            onReportUserClick = { showReportDialog = true },
                            onAddFriendClick = { comingSoonFeatureRes = R.string.open_letter_add_friend },
                            onBlockUserClick = { showBlockConfirmDialog = true },
                            isBlocked = state.isBlocked,
                            onUnblockUserClick = { showUnblockConfirmDialog = true },
                        )

                        Spacer(Modifier.height(20.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 88.dp),
                        ) {
                            items(state.letters, key = { it.id }) { letter ->
                                ThreadLetterRow(
                                    letter = letter,
                                    correspondentName = state.correspondentName,
                                    onTrackClick = { onTrackLetterClick(letter.id, state.correspondentContinent) },
                                    onOutgoingLetterClick = { selectedOutgoingLetter = letter },
                                    onViewFullClick = { mode ->
                                        fullViewRequest = FullViewRequest(letter, mode)
                                        isFullViewOpen = true
                                    },
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }

                    if (state.isBlocked) {
                        BlockedThreadBanner(
                            correspondentName = state.correspondentName,
                            onUnblockClick = { showUnblockConfirmDialog = true },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 16.dp),
                        )
                    } else {
                        WriteLetterButton(
                            onClick = {
                                val draftId = state.draftId
                                if (draftId != null) {
                                    onContinueDraftClick(
                                        draftId,
                                        state.correspondentId,
                                        state.correspondentName,
                                        state.correspondentContinent,
                                    )
                                } else {
                                    onWriteLetterClick(state.correspondentId, state.correspondentName, state.correspondentContinent)
                                }
                            },
                            hasDraft = state.draftId != null,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 16.dp),
                        )
                    }
                }
            }
        }

        fullViewRequest?.let { request ->
            BackHandler(enabled = isFullViewOpen) { isFullViewOpen = false }
            MailboxFullLetterViewer(
                letter = request.letter,
                mode = request.mode,
                isVisible = isFullViewOpen,
                onModeChange = { newMode -> fullViewRequest = request.copy(mode = newMode) },
                onDismiss = { isFullViewOpen = false },
                onFullyClosed = { fullViewRequest = null },
            )
        }
    }

    comingSoonFeatureRes?.let { titleRes ->
        ComingSoonBottomSheet(
            title = stringResource(titleRes),
            onDismiss = { comingSoonFeatureRes = null },
        )
    }

    if (showPublicProfileSheet) {
        val correspondentId = (uiState as? MailboxThreadUiState.Success)?.correspondentId
        if (correspondentId != null) {
            PublicProfileBottomSheet(
                userId = correspondentId,
                onDismiss = { showPublicProfileSheet = false },
            )
        }
    }

    if (showReportDialog) {
        val correspondentId = (uiState as? MailboxThreadUiState.Success)?.correspondentId
        ReportMailDialog(
            reasons = listOf(
                ReportReason.HARASSMENT,
                ReportReason.SPAM,
                ReportReason.INAPPROPRIATE_CONTENT,
                ReportReason.UNDERAGE,
                ReportReason.IMPERSONATION,
                ReportReason.OTHER,
            ),
            isSubmitting = reportState is ThreadReportState.Submitting,
            onSubmit = { reason, note ->
                correspondentId?.let { viewModel.reportUser(it, reason.apiValue, note) }
            },
            onDismissRequest = { showReportDialog = false },
            titleRes = R.string.report_user_dialog_title,
            bodyRes = R.string.report_user_dialog_body,
        )
    }

    if (showBlockConfirmDialog) {
        val successState = uiState as? MailboxThreadUiState.Success
        LetterlyCenterDialog(
            title = stringResource(R.string.block_user_confirm_dialog_title, successState?.correspondentName.orEmpty()),
            body = stringResource(R.string.block_user_confirm_dialog_body),
            primaryCtaText = stringResource(R.string.block_user_confirm_dialog_cta_confirm),
            onPrimaryClick = {
                showBlockConfirmDialog = false
                successState?.correspondentId?.let { viewModel.blockUser(it) }
            },
            secondaryCtaText = stringResource(R.string.block_user_confirm_dialog_cta_cancel),
            onSecondaryClick = { showBlockConfirmDialog = false },
            onDismissRequest = { showBlockConfirmDialog = false },
        )
    }

    if (showUnblockConfirmDialog) {
        val successState = uiState as? MailboxThreadUiState.Success
        LetterlyCenterDialog(
            title = stringResource(R.string.unblock_user_confirm_dialog_title, successState?.correspondentName.orEmpty()),
            body = stringResource(R.string.unblock_user_confirm_dialog_body),
            primaryCtaText = stringResource(R.string.unblock_user_confirm_dialog_cta_confirm),
            onPrimaryClick = {
                showUnblockConfirmDialog = false
                successState?.correspondentId?.let { viewModel.unblockUser(it) }
            },
            secondaryCtaText = stringResource(R.string.unblock_user_confirm_dialog_cta_cancel),
            onSecondaryClick = { showUnblockConfirmDialog = false },
            onDismissRequest = { showUnblockConfirmDialog = false },
        )
    }

    selectedOutgoingLetter?.let { outgoingLetter ->
        val successState = uiState as? MailboxThreadUiState.Success
        val correspondentName = successState?.correspondentName.orEmpty()
        val correspondentContinent = successState?.correspondentContinent.orEmpty()
        LetterlyCenterDialog(
            title = stringResource(R.string.mailbox_outgoing_dialog_title),
            body = stringResource(R.string.mailbox_outgoing_dialog_body, correspondentName),
            primaryCtaText = stringResource(R.string.mailbox_outgoing_dialog_cta_read),
            onPrimaryClick = {
                val letterToView = outgoingLetter
                selectedOutgoingLetter = null
                fullViewRequest = FullViewRequest(letterToView, FullLetterViewMode.LETTER)
                isFullViewOpen = true
            },
            secondaryCtaText = stringResource(R.string.mailbox_outgoing_dialog_cta_track),
            onSecondaryClick = {
                val letterId = outgoingLetter.id
                selectedOutgoingLetter = null
                onTrackLetterClick(letterId, correspondentContinent)
            },
            onDismissRequest = { selectedOutgoingLetter = null },
        )
    }
}

/** Banner shown in place of [WriteLetterButton] when the correspondent is blocked. */
@Composable
private fun BlockedThreadBanner(
    correspondentName: String,
    onUnblockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ToolbarFrostedDark,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Block,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.mailbox_thread_blocked_banner_text, correspondentName),
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.90f),
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = onUnblockClick,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.mailbox_thread_unblock_cta),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BrandGold,
                )
            }
        }
    }
}

/** Which letter + side (envelope/letter) [MailboxFullLetterViewer] is
 * currently showing (or animating closed). */
private data class FullViewRequest(val letter: ThreadLetterItem, val mode: FullLetterViewMode)

/** One letter, aligned left (received) or right (sent) — "chat kiri-kanan". */
@Composable
private fun ThreadLetterRow(
    letter: ThreadLetterItem,
    correspondentName: String,
    onTrackClick: () -> Unit,
    onOutgoingLetterClick: () -> Unit,
    onViewFullClick: (FullLetterViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (letter.isFromViewer) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(modifier = Modifier.fillMaxWidth(0.82f)) {
            AnimatedContent(
                targetState = letter.deliveryStatus,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(600, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(600, easing = FastOutSlowInEasing)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(350)) +
                                scaleOut(targetScale = 1.04f, animationSpec = tween(350))
                        )
                },
                label = "threadLetterTransition",
            ) { targetStatus ->
                when (targetStatus) {
                    LetterDeliveryStatus.INCOMING -> IncomingMailContent(
                        item = letter,
                        onTrackClick = onTrackClick,
                    )
                    LetterDeliveryStatus.OUTGOING_IN_TRANSIT -> OutgoingTrackingContent(
                        item = letter,
                        correspondentName = correspondentName,
                        onCardClick = onOutgoingLetterClick,
                    )
                    LetterDeliveryStatus.DELIVERED -> {
                        var isOpened by rememberSaveable(letter.id) { mutableStateOf(false) }
                        Column {
                            EnvelopePaperReveal(
                                item = letter,
                                isOpened = isOpened,
                                onOpenClick = {
                                    isOpened = true
                                    onViewFullClick(FullLetterViewMode.LETTER)
                                },
                            )
                            Spacer(Modifier.height(8.dp))
                            ModeCaption(item = letter, isOpened = isOpened)
                        }
                    }
                }
            }
        }
    }
}
