package com.apps.unsealed.ui.screens.penpals.screen

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.unsealed.R
import com.apps.unsealed.feature.auth.AuthUiState
import com.apps.unsealed.feature.auth.AuthViewModel
import com.apps.unsealed.ui.components.LoginPromptBottomSheet
import com.apps.unsealed.ui.components.rememberAuthGuard
import com.apps.unsealed.ui.screens.penpals.constants.ArchiveCardSeed
import com.apps.unsealed.ui.screens.publicprofile.PublicProfileBottomSheet
import com.apps.unsealed.ui.screens.penpals.constants.DeskCardWidth
import com.apps.unsealed.ui.screens.penpals.constants.DeskFocusScrimZIndex
import com.apps.unsealed.ui.screens.penpals.constants.DeskHeaderZIndex
import com.apps.unsealed.ui.screens.penpals.constants.DeskScatterTopGap
import com.apps.unsealed.ui.screens.penpals.constants.DeskScrimAlpha
import com.apps.unsealed.ui.screens.penpals.constants.RefreshFeedEnergyCost
import com.apps.unsealed.ui.screens.penpals.constants.deskScatterOffset
import com.apps.unsealed.ui.screens.penpals.constants.deskScatterRotation
import com.apps.unsealed.ui.screens.penpals.state.DeskCardState
import com.apps.unsealed.ui.screens.penpals.state.PenPalsViewMode
import com.apps.unsealed.ui.screens.penpals.state.defaultPenPalsViewMode
import com.apps.unsealed.ui.screens.penpals.viewmodel.BookmarksViewModel
import com.apps.unsealed.ui.screens.penpals.viewmodel.PenPalsViewModel
import com.apps.unsealed.ui.screens.penpals.widgets.ArchiveBottomCard
import com.apps.unsealed.ui.screens.penpals.widgets.BookmarksBottomSheet
import com.apps.unsealed.ui.screens.penpals.widgets.DeskCard
import com.apps.unsealed.ui.screens.penpals.widgets.EnvelopeStackShimmer
import com.apps.unsealed.ui.screens.penpals.widgets.OpenLetterOverlay
import com.apps.unsealed.ui.screens.penpals.widgets.PenPalsGridContent
import com.apps.unsealed.ui.screens.penpals.widgets.PenPalsGridShimmer
import com.apps.unsealed.ui.screens.penpals.widgets.PenPalsHeader
import com.apps.unsealed.ui.screens.penpals.widgets.PeniRefreshFeedBottomSheet
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeAspectRatio
import com.apps.unsealed.ui.theme.ToolbarFrostedDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PenPalsScreen(
    onProfileClick: () -> Unit = {},
    onReplyClick: (recipientId: String, recipientName: String, recipientContinent: String) -> Unit = { _, _, _ -> },
    onNavigateToLogin: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val penPalsViewModel: PenPalsViewModel = hiltViewModel()
    val letters by penPalsViewModel.letters.collectAsStateWithLifecycle()
    val isLoading by penPalsViewModel.isLoading.collectAsStateWithLifecycle()
    val error by penPalsViewModel.error.collectAsStateWithLifecycle()
    val stickerCatalog by penPalsViewModel.stickerCatalog.collectAsStateWithLifecycle()
    val stampCatalog by penPalsViewModel.stampCatalog.collectAsStateWithLifecycle()
    val energyBalance by penPalsViewModel.energyBalance.collectAsStateWithLifecycle()
    val isRefreshingStack by penPalsViewModel.isRefreshingStack.collectAsStateWithLifecycle()
    val refreshStackError by penPalsViewModel.refreshStackError.collectAsStateWithLifecycle()

    var openedLetterIndex by remember { mutableStateOf<Int?>(null) }
    var isLetterOpen by remember { mutableStateOf(false) }

    fun openLetter(index: Int) {
        openedLetterIndex = index
        isLetterOpen = true
    }
    fun closeLetter() { isLetterOpen = false }

    BackHandler(enabled = isLetterOpen) { closeLetter() }

    val bookmarksViewModel: BookmarksViewModel = hiltViewModel()
    val bookmarkedLetters by bookmarksViewModel.bookmarkedLetters.collectAsStateWithLifecycle()
    val bookmarkedIds by bookmarksViewModel.bookmarkedIds.collectAsStateWithLifecycle()

    var isBookmarksSheetVisible by remember { mutableStateOf(false) }
    var openedBookmarkIndex by remember { mutableStateOf<Int?>(null) }
    var isBookmarkViewerOpen by remember { mutableStateOf(false) }

    fun openBookmark(index: Int) {
        openedBookmarkIndex = index
        isBookmarkViewerOpen = true
        isBookmarksSheetVisible = false
    }
    fun closeBookmarkViewer() { isBookmarkViewerOpen = false }

    BackHandler(enabled = isBookmarkViewerOpen) { closeBookmarkViewer() }

    var focusedLetterId by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = focusedLetterId != null && !isLetterOpen) { focusedLetterId = null }

    // Shared Activity-scoped instance (same one MainActivity's authGuard uses)
    // rather than the NavBackStackEntry-scoped default — otherwise this
    // screen's own private `uiState` never learns about a sign-in that
    // happened through a *different* AuthViewModel instance (e.g. the one
    // created fresh once this route enters an unauthenticated state), which
    // is what left the "Hi, Guest" greeting stuck after a successful
    // envelope-tap → Google sign-in.
    val authViewModel: AuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
    val authGuard = rememberAuthGuard(authViewModel)
    val context = LocalContext.current

    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val userName = when (val state = authUiState) {
        is AuthUiState.Authenticated -> state.user.nickname
        is AuthUiState.NeedsOnboarding -> state.user.nickname
        else -> stringResource(R.string.penpals_greeting_guest_name)
    }
    val userPhotoUrl = when (val state = authUiState) {
        is AuthUiState.Authenticated -> state.user.photoUrl
        is AuthUiState.NeedsOnboarding -> state.user.photoUrl
        else -> null
    }

    // Tracks the retry button's own tap → refetch round trip, separate from
    // penPalsViewModel.isLoading — a no-network retry fails almost instantly
    // (DNS/connect error, no timeout wait), so relying on isLoading alone
    // made the tap look like it did nothing. Enforcing a short minimum
    // "retrying" duration here guarantees a perceivable state change.
    var isRetrying by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var viewMode by remember { mutableStateOf(defaultPenPalsViewMode()) }
    // Server already caps this to today's <=20-item stack (`daily_penpal_stack.md`
    // §2) — no client-side truncation needed anymore, just a display sort.
    val visibleLetters = remember(letters) { letters.sortedByDescending { it.postedAt } }
    // "Refresh early" teaser shown once the stack has actually loaded — reroll
    // is useful even on an empty/short stack, not just once it's exhausted.
    // Hidden mid-reroll too, since the shimmer below already covers the desk.
    val showRefreshCard = !isLoading && !isRefreshingStack && error == null
    var isRefreshFeedSheetVisible by remember { mutableStateOf(false) }
    /** Non-null while the public profile bottom sheet is open; holds the target sender ID. */
    var viewProfileUserId by remember { mutableStateOf<String?>(null) }

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    // Reroll can legitimately return the exact same stack back (dummy/thin
    // seed data has no anti-repeat logic yet — `daily_penpal_stack.md`
    // §Known limitations), so the letter list alone isn't a reliable "it
    // worked" signal. This fires once per completed reroll (success only —
    // the sheet already surfaces its own error text) regardless of whether
    // the content visibly changed, so the tap always feels acknowledged.
    var wasRefreshingStack by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshingStack) {
        if (wasRefreshingStack && !isRefreshingStack && refreshStackError == null) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            Toast.makeText(
                context,
                context.getString(R.string.penpals_refresh_success_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
        wasRefreshingStack = isRefreshingStack
    }

    var headerHeightDp by remember { mutableStateOf(200.dp) }

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val deskWidthPx = with(density) { maxWidth.toPx() }
            val deskHeightPx = with(density) { maxHeight.toPx() }
            val cardWidthPx = with(density) { DeskCardWidth.toPx() }
            val cardHeightPx = cardWidthPx / EnvelopeAspectRatio
            val scatterTopPx = with(density) { (headerHeightDp + DeskScatterTopGap).toPx() }

            // ── Error state: retry button centred below header ─────────────────
            // isRetrying keeps this block (and its button's spinner) mounted
            // through the brief window where loadFeed() has already flipped
            // isLoading back to true — otherwise this whole block would
            // disappear the instant retry is tapped, before the spinner ever
            // gets a chance to render.
            if (letters.isEmpty() && (isRetrying || (!isLoading && error != null))) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Guarded on error != null (not just a ?: fallback) — while
                    // isRetrying is what's keeping this block mounted, a retry
                    // that actually succeeded (but with a genuinely empty feed)
                    // would otherwise flash a false "Something went wrong" for
                    // isRetrying's remaining artificial delay window.
                    val currentError = error
                    if (currentError != null) {
                        Text(
                            text = currentError,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ToolbarFrostedDark)
                            .clickable(enabled = !isRetrying) {
                                isRetrying = true
                                penPalsViewModel.loadFeed()
                                coroutineScope.launch {
                                    delay(500)
                                    isRetrying = false
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isRetrying) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp),
                            )
                        } else {
                            Text(
                                text = "Retry",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            when (viewMode) {
                PenPalsViewMode.LooseDesk -> {
                    val cardStates = remember { mutableStateMapOf<String, DeskCardState>() }
                    val topZIndex = remember { mutableFloatStateOf(visibleLetters.size.toFloat() + 1f) }

                    LaunchedEffect(visibleLetters, deskWidthPx, deskHeightPx, scatterTopPx) {
                        val scatterAreaHeightPx = (deskHeightPx - scatterTopPx).coerceAtLeast(cardHeightPx)
                        visibleLetters.forEachIndexed { index, letter ->
                            if (!cardStates.containsKey(letter.id)) {
                                val seed = letter.id.hashCode()
                                val offset = deskScatterOffset(
                                    seed = seed,
                                    areaWidthPx = deskWidthPx,
                                    areaHeightPx = scatterAreaHeightPx,
                                    cardWidthPx = cardWidthPx,
                                    cardHeightPx = cardHeightPx,
                                )
                                cardStates[letter.id] = DeskCardState(
                                    offsetX = Animatable(offset.x),
                                    offsetY = Animatable(scatterTopPx + offset.y),
                                    rotationZ = deskScatterRotation(seed),
                                    zIndex = mutableFloatStateOf((visibleLetters.size - index).toFloat()),
                                )
                            }
                        }
                    }

                    // Also covers a reroll in flight — a dummy/thin-seed reroll
                    // can legitimately return the exact same letters, so the
                    // shimmer (not a content diff) is what tells the user
                    // something actually happened while it's in progress.
                    if ((isLoading && letters.isEmpty()) || isRefreshingStack) {
                        EnvelopeStackShimmer(
                            deskWidthPx = deskWidthPx,
                            deskHeightPx = deskHeightPx,
                            scatterTopPx = scatterTopPx,
                            cardWidth = DeskCardWidth,
                            count = 4,
                        )
                    }

                    if (showRefreshCard) {
                        val scatterAreaHeightPx = (deskHeightPx - scatterTopPx).coerceAtLeast(cardHeightPx)
                        val archiveOffset = deskScatterOffset(
                            seed = ArchiveCardSeed,
                            areaWidthPx = deskWidthPx,
                            areaHeightPx = scatterAreaHeightPx,
                            cardWidthPx = cardWidthPx,
                            cardHeightPx = cardHeightPx,
                        )
                        ArchiveBottomCard(
                            onClick = { authGuard.guard { isRefreshFeedSheetVisible = true } },
                            modifier = Modifier
                                .width(DeskCardWidth)
                                .graphicsLayer {
                                    translationX = archiveOffset.x
                                    translationY = scatterTopPx + archiveOffset.y
                                    rotationZ = deskScatterRotation(ArchiveCardSeed)
                                }
                                .zIndex(0f),
                        )
                    }

                    if (!isRefreshingStack) {
                        visibleLetters.forEachIndexed { index, letter ->
                            val state = cardStates[letter.id] ?: return@forEachIndexed
                            DeskCard(
                                letter = letter,
                                index = index,
                                state = state,
                                cardWidth = DeskCardWidth,
                                cardWidthPx = cardWidthPx,
                                cardHeightPx = cardHeightPx,
                                deskWidthPx = deskWidthPx,
                                deskHeightPx = deskHeightPx,
                                scatterTopPx = scatterTopPx,
                                isFocused = letter.id == focusedLetterId,
                                isAnyFocused = focusedLetterId != null,
                                onDragStart = {
                                    topZIndex.floatValue += 1f
                                    state.zIndex.floatValue = topZIndex.floatValue
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                },
                                onRequestFocus = { focusedLetterId = letter.id },
                                onRequestOpen = { authGuard.guard { openLetter(index) } },
                                stickerCatalog = stickerCatalog,
                                stampCatalog = stampCatalog,
                            )
                        }
                    }

                    if (focusedLetterId != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(DeskFocusScrimZIndex)
                                .background(Color.Black.copy(alpha = DeskScrimAlpha))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { focusedLetterId = null },
                                ),
                        )
                    }
                }

                PenPalsViewMode.Grid -> {
                    if ((isLoading && letters.isEmpty()) || isRefreshingStack) {
                        PenPalsGridShimmer(
                            modifier = Modifier.padding(top = headerHeightDp + DeskScatterTopGap),
                        )
                    } else {
                        PenPalsGridContent(
                            letters = visibleLetters,
                            hasArchive = showRefreshCard,
                            onLetterClick = { index -> authGuard.guard { openLetter(index) } },
                            onArchiveClick = { authGuard.guard { isRefreshFeedSheetVisible = true } },
                            stickerCatalog = stickerCatalog,
                            stampCatalog = stampCatalog,
                            modifier = Modifier.padding(top = headerHeightDp + DeskScatterTopGap),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .zIndex(DeskHeaderZIndex)
                    .onGloballyPositioned { coords ->
                        with(density) { headerHeightDp = coords.size.height.toDp() }
                    },
            ) {
                PenPalsHeader(
                    userName = userName,
                    photoUrl = userPhotoUrl,
                    viewMode = viewMode,
                    onViewModeChange = { viewMode = it },
                    onProfileClick = onProfileClick,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 48.dp, bottom = 18.dp),
                )
            }

            if (visibleLetters.isNotEmpty()) {
                openedLetterIndex?.let { startIndex ->
                    OpenLetterOverlay(
                        letters = visibleLetters,
                        initialIndex = startIndex,
                        isOpen = isLetterOpen,
                        onClose = ::closeLetter,
                        onFullyClosed = { openedLetterIndex = null },
                        onReplyClick = { letter ->
                            penPalsViewModel.prepareReplyLetter(letter)
                            closeLetter()
                            onReplyClick(letter.senderId, letter.senderName, letter.region.apiLabel)
                        },
                        onToggleLike = { letter -> penPalsViewModel.toggleLike(letter.id) },
                        onToggleBookmark = { letter -> bookmarksViewModel.toggleBookmark(letter) },
                        bookmarkedIds = bookmarkedIds,
                        onViewBookmarksClick = { isBookmarksSheetVisible = true },
                        onViewProfileClick = { senderId -> viewProfileUserId = senderId },
                        onReportLetter = { letterId, reason, note ->
                            penPalsViewModel.reportLetter(letterId, reason, note)
                        },
                        onBlockUser = { userId -> penPalsViewModel.blockUser(userId) },
                        modifier = Modifier.fillMaxSize().zIndex(2000f),
                    )
                }
            }

            if (bookmarkedLetters.isNotEmpty()) {
                openedBookmarkIndex?.let { startIndex ->
                    OpenLetterOverlay(
                        letters = bookmarkedLetters,
                        initialIndex = startIndex,
                        isOpen = isBookmarkViewerOpen,
                        onClose = ::closeBookmarkViewer,
                        onFullyClosed = { openedBookmarkIndex = null },
                        onReplyClick = { letter ->
                            penPalsViewModel.prepareReplyLetter(letter)
                            closeBookmarkViewer()
                            onReplyClick(letter.senderId, letter.senderName, letter.region.apiLabel)
                        },
                        onToggleLike = { letter -> penPalsViewModel.toggleLike(letter.id) },
                        onToggleBookmark = { letter -> bookmarksViewModel.toggleBookmark(letter) },
                        bookmarkedIds = bookmarkedIds,
                        onViewBookmarksClick = { isBookmarksSheetVisible = true },
                        onViewProfileClick = { senderId -> viewProfileUserId = senderId },
                        onReportLetter = { letterId, reason, note ->
                            penPalsViewModel.reportLetter(letterId, reason, note)
                        },
                        onBlockUser = { userId -> penPalsViewModel.blockUser(userId) },
                        modifier = Modifier.fillMaxSize().zIndex(2500f),
                    )
                }
            }

            if (isBookmarksSheetVisible) {
                BookmarksBottomSheet(
                    letters = bookmarkedLetters,
                    onDismiss = { isBookmarksSheetVisible = false },
                    onLetterClick = { index -> openBookmark(index) },
                    onRemove = { ids -> bookmarksViewModel.removeBookmarks(ids) },
                )
            }

            if (authGuard.isPromptVisible) {
                LoginPromptBottomSheet(
                    onDismiss = authGuard.dismiss,
                    onSignInClick = onNavigateToLogin,
                )
            }

            if (isRefreshFeedSheetVisible) {
                PeniRefreshFeedBottomSheet(
                    energyCost = RefreshFeedEnergyCost,
                    energyBalance = energyBalance,
                    isRefreshing = isRefreshingStack,
                    errorMessage = refreshStackError,
                    onConfirm = { penPalsViewModel.refreshFeed() },
                    onDismiss = {
                        isRefreshFeedSheetVisible = false
                        penPalsViewModel.clearRefreshStackError()
                    },
                )
            }

            viewProfileUserId?.let { userId ->
                PublicProfileBottomSheet(
                    userId = userId,
                    onDismiss = { viewProfileUserId = null },
                )
            }
        }
    }
}
