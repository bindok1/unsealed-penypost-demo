package com.apps.unsealed.ui.screens.penpals.widgets

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.util.captureIntoGraphicsLayer
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberGraphicsLayer
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.core.util.saveImageToGallery
import com.apps.unsealed.ui.components.AnchoredDropdownMenu
import com.apps.unsealed.ui.components.DropdownMenuAction
import com.apps.unsealed.ui.components.LetterlyCenterDialog
import com.apps.unsealed.ui.components.ReportMailDialog
import com.apps.unsealed.ui.components.ReportReason
import com.apps.unsealed.ui.screens.compose.widgets.ComingSoonBottomSheet
import com.apps.unsealed.ui.screens.penpals.state.PenpalLetter
import com.apps.unsealed.ui.screens.penpals.state.formattedPostedAt
import com.apps.unsealed.ui.theme.BrandCardDark
import com.apps.unsealed.ui.theme.BrandCardStroke
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.CaveatFontFamily
import com.apps.unsealed.ui.theme.InkDefault
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.PaperTemplate
import com.apps.unsealed.ui.theme.SurfaceCream
import com.apps.unsealed.ui.theme.ToolbarFrostedDark
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

// ─── Local springs ────────────────────────────────────────────────────────────

/** Slide-up entrance: gentle settle, slight overshoot — feels like paper placed down. */
private val SlideUpEnterSpec = spring<Float>(
    dampingRatio = 0.78f,
    stiffness = 280f,
)

/** Slide-down exit: fast, no bounce — feels like swiping away cleanly. */
private val SlideDownExitSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
)

private val GentleDpSpring = spring<Dp>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/** Background choices for the plain-text fallback — excludes [PaperTemplate.INDIGO]. */
private val TextModePaperTemplates = PaperTemplate.entries.filter { it != PaperTemplate.INDIGO }

// ─── Open Letter Overlay ──────────────────────────────────────────────────────

/**
 * Full-screen letter view that slides up from below the envelope card on open
 * and slides back down on close. Supports tapping anywhere to toggle overlay UI controls.
 */
@Composable
fun OpenLetterOverlay(
    letters: List<PenpalLetter>,
    initialIndex: Int,
    isOpen: Boolean,
    onClose: () -> Unit,
    onFullyClosed: () -> Unit,
    onPageChanged: (Int) -> Unit = {},
    onReplyClick: (PenpalLetter) -> Unit = {},
    onToggleLike: (PenpalLetter) -> Unit = {},
    onToggleBookmark: (PenpalLetter) -> Unit = {},
    bookmarkedIds: Set<String> = emptySet(),
    onViewBookmarksClick: () -> Unit = {},
    onViewProfileClick: (senderId: String) -> Unit = {},
    onReportLetter: suspend (letterId: String, reason: ReportReason, note: String?) -> Boolean = { _, _, _ -> false },
    onBlockUser: suspend (userId: String) -> Boolean = { false },
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()

    val pagerState = rememberPagerState(initialPage = initialIndex) { letters.size }
    if (letters.isEmpty()) return
    val currentLetter = letters[pagerState.currentPage.coerceIn(letters.indices)]

    var controlsVisible by remember { mutableStateOf(true) }

    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    // A deliberate/fast swipe should flip the letter immediately instead of
    // requiring the inner verticalScroll to first exhaust its full content
    // height — see OpenLetterOverlay.kt page content below. 900dp/s required a
    // near-flick gesture, so a normal-paced "next letter" swipe on a text
    // letter just scrolled the body instead of changing pages (image-mode
    // letters rarely have scrollable overflow, so they never hit this at
    // all, which is why swiping felt easy there but not here) — lowered to
    // match a more typical swipe velocity.
    val pageFlingHandoffVelocityPx = with(density) { 450.dp.toPx() }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .drop(1)
            .collect { page ->
                onPageChanged(page)
                controlsVisible = true
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
    }

    val slideOffsetFraction by animateFloatAsState(
        targetValue = if (isOpen) 0f else 1f,
        animationSpec = reducedMotionSpring(
            if (isOpen) SlideUpEnterSpec else SlideDownExitSpec,
            isReducedMotion,
        ),
        label = "slideOffset",
        finishedListener = { fraction ->
            if (fraction >= 0.98f) onFullyClosed()
        },
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = reducedMotionSpring(tween(200), isReducedMotion),
        label = "overlayAlpha",
    )

    var showBody by remember { mutableStateOf(false) }
    var showFooter by remember { mutableStateOf(false) }
    LaunchedEffect(isOpen) {
        if (isOpen) {
            delay(100); showBody = true
            delay(100); showFooter = true
        } else {
            showBody = false; showFooter = false
        }
    }

    val bodyAlpha by animateFloatAsState(
        targetValue = if (showBody) 1f else 0f,
        animationSpec = reducedMotionSpring(tween(260), isReducedMotion),
        label = "bodyAlpha",
    )
    val bodyOffsetY by animateDpAsState(
        targetValue = if (showBody) 0.dp else 20.dp,
        animationSpec = reducedMotionSpring(GentleDpSpring, isReducedMotion),
        label = "bodyOffsetY",
    )
    val footerAlpha by animateFloatAsState(
        targetValue = if (showFooter) 1f else 0f,
        animationSpec = reducedMotionSpring(tween(260), isReducedMotion),
        label = "footerAlpha",
    )
    val footerOffsetY by animateDpAsState(
        targetValue = if (showFooter) 0.dp else 20.dp,
        animationSpec = reducedMotionSpring(GentleDpSpring, isReducedMotion),
        label = "footerOffsetY",
    )

    // Smooth toggle animation for overlay controls (close button, action rail, sender info)
    val controlsAlpha by animateFloatAsState(
        targetValue = if (isOpen && controlsVisible) 1f else 0f,
        animationSpec = reducedMotionSpring(tween(220), isReducedMotion),
        label = "controlsAlpha",
    )
    val topControlsOffsetY by animateDpAsState(
        targetValue = if (isOpen && controlsVisible) 0.dp else (-18).dp,
        animationSpec = reducedMotionSpring(GentleDpSpring, isReducedMotion),
        label = "topControlsOffsetY",
    )
    val rightControlsOffsetX by animateDpAsState(
        targetValue = if (isOpen && controlsVisible) 0.dp else 24.dp,
        animationSpec = reducedMotionSpring(GentleDpSpring, isReducedMotion),
        label = "rightControlsOffsetX",
    )
    val bottomControlsOffsetY by animateDpAsState(
        targetValue = if (isOpen && controlsVisible) 0.dp else 24.dp,
        animationSpec = reducedMotionSpring(GentleDpSpring, isReducedMotion),
        label = "bottomControlsOffsetY",
    )

    var comingSoonFeatureRes by remember { mutableStateOf<Int?>(null) }
    var showReplyConfirm by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var isReporting by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var isShareSheetVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val letterCardGraphicsLayer = rememberGraphicsLayer()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.share_letter_saved_toast)
    val saveFailedMessage = stringResource(R.string.share_letter_save_failed_toast)
    val viewActionLabel = stringResource(R.string.share_letter_saved_snackbar_action)
    val bookmarkSavedMessage = stringResource(R.string.open_letter_bookmark_saved_snackbar)
    val bookmarkRemovedMessage = stringResource(R.string.open_letter_bookmark_removed_snackbar)
    val bookmarkViewActionLabel = stringResource(R.string.open_letter_bookmark_view_action)
    val isCurrentBookmarked = currentLetter.id in bookmarkedIds

    BoxWithConstraints(modifier.fillMaxSize()) {
        val screenHeightPx = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandInkDeep.copy(alpha = 0.95f))
                .graphicsLayer {
                    translationY = screenHeightPx * slideOffsetFraction
                    alpha = overlayAlpha
                },
        ) {
            VerticalPager(
                state = pagerState,
                userScrollEnabled = isOpen,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = bodyAlpha
                        translationY = bodyOffsetY.toPx()
                    },
                key = { letters[it].id },
            ) { page ->
                val letter = letters[page]
                val scrollState = rememberScrollState()
                // ScrollState is remembered per letter id (via the pager's
                // `key`), so it survives being scrolled off-screen — revisiting
                // a letter you'd already scrolled into (e.g. swiping forward
                // then back) reopened it mid-scroll instead of at the top,
                // which read as the letter's opening lines being "cut off".
                // Snap it back to the top as soon as this page stops being
                // the current one so every visit starts fresh.
                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage != page) {
                        scrollState.scrollTo(0)
                    }
                }
                // Lets a fast/deliberate swipe hand off to the pager right
                // away instead of first fully draining the inner
                // verticalScroll's range (which on a full-length letter can
                // mean multiple screens' worth of dragging before the page
                // actually changes). Slow drags below the threshold are
                // untouched and still scroll the letter body normally.
                //
                // Gated on the scroll already being at the boundary in the
                // swipe's direction — without this, a mid-scroll swipe back
                // up to re-read earlier lines (past pageFlingHandoffVelocityPx,
                // which is easy to hit on a normal "scroll up" gesture) got
                // misread as "go to previous letter" and yanked the pager
                // over mid-scroll, cutting off the text being scrolled to.
                val pageFlingConnection = remember(page, letters.size, scrollState) {
                    object : NestedScrollConnection {
                        override suspend fun onPreFling(available: Velocity): Velocity {
                            if (abs(available.y) < pageFlingHandoffVelocityPx) return Velocity.Zero
                            val scrollingToNext = available.y < 0f
                            val atBoundary = if (scrollingToNext) {
                                scrollState.value >= scrollState.maxValue
                            } else {
                                scrollState.value <= 0
                            }
                            if (!atBoundary) return Velocity.Zero
                            val targetPage = page + if (scrollingToNext) 1 else -1
                            if (targetPage !in letters.indices) return Velocity.Zero
                            scope.launch { pagerState.animateScrollToPage(targetPage) }
                            return available
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(pageFlingConnection)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                controlsVisible = !controlsVisible
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(
                                top = 76.dp,
                                bottom = 124.dp,
                                start = 16.dp,
                                end = 72.dp,
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                // A minimum height only makes sense for the plain-text
                                // fallback (so a short letter doesn't look like a tiny
                                // stub) — for the composite image, forcing this floor
                                // when the image's own proportional height (after the
                                // asymmetric side padding above) comes in under 500dp
                                // left a visible strip of SurfaceCream around it since
                                // FillWidth never stretches height to match.
                                .then(
                                    if (letter.compositeImageUrl == null) {
                                        Modifier.heightIn(min = 500.dp)
                                    } else {
                                        Modifier
                                    },
                                )
                                .background(SurfaceCream)
                                .border(1.dp, Color.White.copy(alpha = 0.18f))
                                .graphicsLayer {
                                    shadowElevation = 16.dp.toPx()
                                    clip = true
                                }
                                // Only the on-screen page should record into the
                                // shared layer — VerticalPager can keep adjacent
                                // pages composed too, and they'd otherwise race
                                // to overwrite the same GraphicsLayer.
                                .then(
                                    if (page == pagerState.currentPage) {
                                        Modifier.captureIntoGraphicsLayer(letterCardGraphicsLayer)
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            if (letter.compositeImageUrl != null) {
                                AsyncImage(
                                    model = letter.compositeImageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    placeholder = painterResource(PaperTemplate.LINED.drawableRes),
                                    error = painterResource(PaperTemplate.LINED.drawableRes),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                val fallbackPaper = remember(letter.id) {
                                    val index = (letter.id.hashCode() and Int.MAX_VALUE) % TextModePaperTemplates.size
                                    TextModePaperTemplates[index]
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 500.dp),
                                ) {
                                    if (!letter.paperUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = letter.paperUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            placeholder = painterResource(fallbackPaper.drawableRes),
                                            error = painterResource(fallbackPaper.drawableRes),
                                            modifier = Modifier.matchParentSize(),
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(fallbackPaper.drawableRes),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize(),
                                        )
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                    ) {
                                        Text(
                                            text = letter.bodyText,
                                            fontFamily = CaveatFontFamily,
                                            fontSize = 20.sp,
                                            lineHeight = 30.sp,
                                            color = InkDefault,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (page == letters.size - 1) {
                            LetterSessionEndCard(
                                onCloseClick = onClose,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            // Top gradient overlay for contrast
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .align(Alignment.TopStart)
                    .background(
                        brush = Brush.verticalGradient(
                            0f to Color(0x99000000),
                            1f to Color.Transparent,
                        ),
                    )
                    .graphicsLayer {
                        alpha = overlayAlpha * controlsAlpha
                    },
            )

            // Bottom gradient overlay for contrast
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.BottomStart)
                    .background(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color(0x99000000),
                        ),
                    )
                    .graphicsLayer {
                        alpha = overlayAlpha * controlsAlpha
                    },
            )

            CloseButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .graphicsLayer {
                        alpha = overlayAlpha * controlsAlpha
                        translationY = topControlsOffsetY.toPx()
                    },
            )

            ActionRail(
                likeCount = currentLetter.likeCount,
                viewerHasLiked = currentLetter.viewerHasLiked,
                isBookmarked = isCurrentBookmarked,
                onLikeClick = { onToggleLike(currentLetter) },
                onCommentClick = { showReplyConfirm = true },
                onBookmarkClick = {
                    val wasBookmarked = isCurrentBookmarked
                    onToggleBookmark(currentLetter)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = if (wasBookmarked) bookmarkRemovedMessage else bookmarkSavedMessage,
                            actionLabel = if (wasBookmarked) null else bookmarkViewActionLabel,
                            duration = SnackbarDuration.Short,
                        )
                        if (!wasBookmarked && result == SnackbarResult.ActionPerformed) {
                            onViewBookmarksClick()
                        }
                    }
                },
                onShareClick = { isShareSheetVisible = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .graphicsLayer {
                        alpha = overlayAlpha * controlsAlpha
                        translationX = rightControlsOffsetX.toPx()
                    },
            )

            SenderInfoBar(
                letter = currentLetter,
                onStartConversationClick = { showReplyConfirm = true },
                onViewProfileClick = { onViewProfileClick(currentLetter.senderId) },
                onShareMailClick = { isShareSheetVisible = true },
                onReportMailClick = { showReportDialog = true },
                onBlockUserClick = { showBlockConfirm = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .graphicsLayer {
                        alpha = footerAlpha * controlsAlpha
                        translationY = (footerOffsetY + bottomControlsOffsetY).toPx()
                    },
            )
        }
    }

    comingSoonFeatureRes?.let { titleRes ->
        ComingSoonBottomSheet(
            title = stringResource(titleRes),
            onDismiss = { comingSoonFeatureRes = null },
        )
    }

    if (isShareSheetVisible) {
        // ShareLetterSheet is a plain Box overlay (not Dialog/Popup like the
        // other sheets/dialogs above, which get their own Window and so
        // ignore Compose z-order entirely) — it's a sibling of this
        // composable's own fillMaxSize() content inside the caller's Box
        // (PenPalsScreen.kt passes modifier.zIndex(2000f) to *that* content
        // only), so without an explicit zIndex here it rendered underneath
        // it and was invisible.
        ShareLetterSheet(
            letter = currentLetter,
            graphicsLayer = letterCardGraphicsLayer,
            onDismiss = { isShareSheetVisible = false },
            onSaveImageRequested = { bytes ->
                // Uses this composable's own longer-lived `scope`, not
                // ShareLetterSheet's — that one gets torn down the moment
                // the sheet finishes its close animation, which would
                // cancel the save mid-write if it hadn't finished yet.
                scope.launch {
                    val uri = saveImageToGallery(context, bytes, "letter_${currentLetter.id}.webp")
                    val result = snackbarHostState.showSnackbar(
                        message = if (uri != null) savedMessage else saveFailedMessage,
                        actionLabel = if (uri != null) viewActionLabel else null,
                        duration = SnackbarDuration.Short,
                    )
                    if (uri != null && result == SnackbarResult.ActionPerformed) {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "image/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                },
                            )
                        }
                    }
                }
            },
            modifier = Modifier.zIndex(2001f),
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().zIndex(2002f),
        contentAlignment = Alignment.BottomCenter,
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.navigationBarsPadding().padding(16.dp),
        )
    }

    if (showReplyConfirm) {
        LetterlyCenterDialog(
            title = stringResource(R.string.reply_confirm_dialog_title),
            body = stringResource(R.string.reply_confirm_dialog_body, currentLetter.senderName),
            primaryCtaText = stringResource(R.string.reply_confirm_dialog_cta_start),
            onPrimaryClick = { showReplyConfirm = false; onReplyClick(currentLetter) },
            secondaryCtaText = stringResource(R.string.reply_confirm_dialog_cta_cancel),
            onSecondaryClick = { showReplyConfirm = false },
            onDismissRequest = { showReplyConfirm = false },
        )
    }

    if (showReportDialog) {
        val reportFailedMessage = stringResource(R.string.error_report_letter)
        ReportMailDialog(
            reasons = listOf(
                ReportReason.SPAM,
                ReportReason.HARASSMENT,
                ReportReason.INAPPROPRIATE_CONTENT,
                ReportReason.EMPTY_MAIL,
            ),
            isSubmitting = isReporting,
            onSubmit = { reason, note ->
                isReporting = true
                scope.launch {
                    val letterId = currentLetter.id
                    val ok = onReportLetter(letterId, reason, note)
                    isReporting = false
                    if (ok) {
                        showReportDialog = false
                        onClose()
                    } else {
                        Toast.makeText(context, reportFailedMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismissRequest = { showReportDialog = false },
        )
    }

    if (showBlockConfirm) {
        val blockFailedMessage = stringResource(R.string.error_block_user)
        LetterlyCenterDialog(
            title = stringResource(R.string.block_user_confirm_dialog_title, currentLetter.senderName),
            body = stringResource(R.string.block_user_confirm_dialog_body),
            primaryCtaText = stringResource(R.string.block_user_confirm_dialog_cta_confirm),
            onPrimaryClick = {
                showBlockConfirm = false
                val senderId = currentLetter.senderId
                scope.launch {
                    if (onBlockUser(senderId)) {
                        onClose()
                    } else {
                        Toast.makeText(context, blockFailedMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            secondaryCtaText = stringResource(R.string.block_user_confirm_dialog_cta_cancel),
            onSecondaryClick = { showBlockConfirm = false },
            onDismissRequest = { showBlockConfirm = false },
        )
    }
}

// ─── Close button ─────────────────────────────────────────────────────────────

@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .size(40.dp)
            .clip(CircleShape)
            .background(ToolbarFrostedDark)
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.open_letter_close_desc),
            tint = Color.White,
        )
    }
}

// ─── Action rail ──────────────────────────────────────────────────────────────

@Composable
private fun ActionRail(
    likeCount: Int,
    viewerHasLiked: Boolean,
    isBookmarked: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ActionRailItem(
            icon = if (viewerHasLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            label = likeCount.toString(),
            tint = if (viewerHasLiked) BrandGold else Color.White,
            onClick = onLikeClick,
        )
        ActionRailItem(Icons.Filled.ChatBubbleOutline, null, onCommentClick)
        ActionRailItem(
            icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            label = null,
            tint = if (isBookmarked) BrandGold else Color.White,
            onClick = onBookmarkClick,
        )
        ActionRailItem(Icons.Filled.Share, null, onShareClick)
    }
}

@Composable
private fun ActionRailItem(
    icon: ImageVector,
    label: String?,
    onClick: () -> Unit,
    tint: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                .size(44.dp)
                .clip(CircleShape)
                .background(ToolbarFrostedDark)
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
        if (label != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─── Sender info bar ──────────────────────────────────────────────────────────

private val MoreButtonSize = 36.dp
private val MoreDropdownGap = 6.dp

@Composable
private fun SenderInfoBar(
    letter: PenpalLetter,
    onStartConversationClick: () -> Unit,
    onViewProfileClick: () -> Unit,
    onShareMailClick: () -> Unit,
    onReportMailClick: () -> Unit,
    onBlockUserClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(BrandCardDark.copy(alpha = 0.92f))
            .border(1.dp, BrandCardStroke, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BrandGold.copy(alpha = 0.2f))
                .border(1.dp, BrandGold.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (!letter.senderPhotoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = letter.senderPhotoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clip(CircleShape),
                )
            } else {
                Icon(Icons.Filled.Person, contentDescription = null, tint = BrandGold)
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = letter.senderName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                if (letter.isOnline) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34C759)),
                    )
                }
            }
            Text(
                text = stringResource(letter.region.labelRes),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
            )
            Text(
                text = stringResource(R.string.open_letter_posted_on, letter.formattedPostedAt()),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
            )
        }

        var moreMenuExpanded by remember { mutableStateOf(false) }
        val density = LocalDensity.current
        val moreSource = remember { MutableInteractionSource() }
        val morePressScale = rememberPressScale(moreSource)
        Box {
            Box(
                modifier = Modifier
                    .graphicsLayer { scaleX = morePressScale; scaleY = morePressScale }
                    .size(MoreButtonSize)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .clickable(
                        interactionSource = moreSource,
                        indication = null,
                        onClick = { moreMenuExpanded = true },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.open_letter_more_desc),
                    tint = Color.White,
                )
            }

            AnchoredDropdownMenu(
                expanded = moreMenuExpanded,
                onDismissRequest = { moreMenuExpanded = false },
                alignment = Alignment.BottomEnd,
                offset = with(density) { IntOffset(0, -(MoreButtonSize + MoreDropdownGap).roundToPx()) },
                transformOrigin = TransformOrigin(1f, 1f),
            ) {
                DropdownMenuAction(
                    icon = Icons.Filled.ChatBubbleOutline,
                    label = stringResource(R.string.open_letter_menu_start_conversation),
                    onClick = { moreMenuExpanded = false; onStartConversationClick() },
                )
                DropdownMenuAction(
                    icon = Icons.Filled.Person,
                    label = stringResource(R.string.open_letter_menu_view_profile),
                    onClick = { moreMenuExpanded = false; onViewProfileClick() },
                )
                DropdownMenuAction(
                    icon = Icons.Filled.Share,
                    label = stringResource(R.string.open_letter_menu_share_mail),
                    onClick = { moreMenuExpanded = false; onShareMailClick() },
                )
                DropdownMenuAction(
                    icon = Icons.Filled.Flag,
                    label = stringResource(R.string.open_letter_menu_report_mail),
                    onClick = { moreMenuExpanded = false; onReportMailClick() },
                )
                DropdownMenuAction(
                    icon = Icons.Filled.Block,
                    label = stringResource(R.string.open_letter_block_user),
                    onClick = { moreMenuExpanded = false; onBlockUserClick() },
                )
            }
        }
    }
}

// ─── End of Mail Session Indicator Card ───────────────────────────────────────

@Composable
private fun LetterSessionEndCard(
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .background(SurfaceCream.copy(alpha = 0.95f), shape)
            .border(1.5.dp, BrandGold, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onCloseClick,
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BrandGold.copy(alpha = 0.15f))
                .border(1.dp, BrandGold.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = BrandGold,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.open_letter_end_of_session_title),
            color = BrandInkDeep,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            fontFamily = NunitoFontFamily,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.open_letter_end_of_session_subtitle),
            color = BrandInkDeep.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontFamily = NunitoFontFamily,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
        )
    }
}


