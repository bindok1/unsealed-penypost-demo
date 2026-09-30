package com.apps.unsealed.ui.screens.compose.screen

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apps.unsealed.R
import com.apps.unsealed.core.util.MinCompositingDensity
import com.apps.unsealed.core.util.buildSystemChooserIntent
import com.apps.unsealed.core.util.captureIntoGraphicsLayer
import com.apps.unsealed.core.util.rememberGraphicsLayer
import com.apps.unsealed.core.util.saveImageToGallery
import com.apps.unsealed.core.util.toCompositeBytes
import com.apps.unsealed.core.util.writeShareCacheFile
import com.apps.unsealed.ui.components.LetterlyCenterDialog
import com.apps.unsealed.ui.components.LetterlyTopSnackbar
import com.apps.unsealed.ui.components.SnackbarStyle
import com.apps.unsealed.ui.screens.compose.constants.*
import com.apps.unsealed.ui.screens.compose.state.*
import com.apps.unsealed.ui.screens.compose.viewmodel.*
import com.apps.unsealed.ui.screens.compose.widgets.*
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch

/** Horizontal inset shared by the floating toolbar and the letter canvas so
 * the paper's width lines up exactly with the toolbar above it, instead of
 * bleeding edge-to-edge while only the toolbar has margins. */
private val FloatingHorizontalMargin = 16.dp

/** Vertical gutter reserved above/below the paper so the wood desk texture
 * (see MainActivity's root background) always peeks in on all 4 sides,
 * matching [FloatingHorizontalMargin] — without this the paper's min height
 * exactly equals the scroll viewport height and it never has room to reveal
 * wood or scroll for short/medium letters. */
private val PaperVerticalMargin = 16.dp

/**
 * Letterly Compose Screen.
 */
@Composable
fun ComposeScreen(
    onSendClick: () -> Unit,
    onBuyEnergyClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    onNavigateToStore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: ComposeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val stickerCatalog by viewModel.stickerCatalog.collectAsState()
    val creatorPapers by viewModel.creatorPapers.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current

    BackHandler(enabled = uiState.isPenyModeActive) {
        viewModel.onPenyModeToggle()
    }
    val hazeState = rememberHazeState()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val compositeGraphicsLayer = rememberGraphicsLayer()
    val isCompositingSupported = density.density >= MinCompositingDensity
    var isDeviceCapabilitySheetVisible by remember { mutableStateOf(false) }
    var savePhotoFeedback by remember { mutableStateOf<String?>(null) }
    var showPublicShowcaseDialog by remember { mutableStateOf(false) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::onGalleryImageInsert) }
    var replacingImageId by remember { mutableStateOf<String?>(null) }
    val imageReplaceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        replacingImageId?.let { id -> uri?.let { viewModel.onImageReplace(id, it) } }
        replacingImageId = null
    }
    val addStickerTitle = stringResource(R.string.overflow_menu_add_sticker)
    val saveToPhotosTitle = stringResource(R.string.overflow_menu_save_photos)
    val shareTitle = stringResource(R.string.overflow_menu_share)

    // Measured sizes feeding the "paper flows behind the toolbar/status bar"
    // effect below — both start at 0 and settle after the first layout pass.
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }
    var toolbarHeightPx by remember { mutableFloatStateOf(0f) }
    val statusBarHeightDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarHeightPx = with(density) { statusBarHeightDp.toPx() }

    // basePageHeightPx now needs both measurements (viewport + toolbar,
    // measured independently) combined, so it's derived here instead of
    // inline in a single onSizeChanged like before.
    LaunchedEffect(viewportHeightPx, toolbarHeightPx) {
        val verticalMarginPx = with(density) { (PaperVerticalMargin * 2).toPx() }
        viewModel.onPageViewportSizeChanged(viewportHeightPx - toolbarHeightPx - verticalMarginPx)
    }

    Column(modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .onSizeChanged { size -> viewportHeightPx = size.height.toFloat() },
        ) {
            // The paper is a single fixed-height page (LetterCanvas's
            // MaxPageHeightMultiplier = 1) that scrolls as a whole "card"
            // (texture + text + annotate + image layers) rather than
            // exposing an inner scroll region — this scroll only exists for
            // the toolbar/status-bar reveal effect below, not to page
            // through overflow content, since the page itself never grows.
            //
            // This Box is deliberately taller than its parent by
            // statusBarHeightDp and shifted up by the same amount: the outer
            // Scaffold (MainActivity) already reserves status-bar space via
            // Modifier.padding(innerPadding), so without this the paper could
            // structurally never reach behind the status bar no matter how
            // far it's scrolled. Extending + shifting cancels that reserved
            // gap for this layer only, while LetterCanvas's own top padding
            // below keeps the visible "rest" position unchanged — a short
            // letter still starts right below the toolbar with wood peeking
            // above it exactly as before. Only once a letter is long enough
            // to scroll does real paper (not just the wood gutter) pass up
            // behind the toolbar and status bar, where Haze's existing blur
            // (see ComposeToolbar's hazeEffect) picks up paper color instead
            // of wood — continuous, not a hard cutoff.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(with(density) { (viewportHeightPx + statusBarHeightPx).toDp() })
                    .offset(y = -statusBarHeightDp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Sits behind LetterCanvas (composed first) and outside its
                // own clipToBounds(), so the glow bleeds into the wood-margin
                // area around the paper rather than being clipped to it.
                PenyAuraGlow(
                    isActive = uiState.isPenyModeActive,
                    modifier = Modifier.matchParentSize(),
                )
                LetterCanvas(
                    uiState = uiState,
                    focusRequester = focusRequester,
                    onBodyTextChange = viewModel::onBodyTextChange,
                    onFocusChanged = { focused ->
                        viewModel.onKeyboardVisibilityChanged(focused)
                        // Typing takes over intent from image editing — deselect
                        // so the change/delete buttons don't linger over the text.
                        if (focused) viewModel.onImageSelect(null)
                    },
                    onAnnotatePathCommit = viewModel::onAnnotatePathCommit,
                    onAnnotateErase = viewModel::onAnnotateErase,
                    onCanvasSizeChanged = viewModel::onCanvasSizeChanged,
                    onImageSelect = viewModel::onImageSelect,
                    onImageTransform = viewModel::onImageTransform,
                    onImageBaseSizeMeasured = viewModel::onImageBaseSizeMeasured,
                    onImageReplaceClick = { id ->
                        replacingImageId = id
                        imageReplaceLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onImageCropModeToggle = viewModel::onImageCropModeToggle,
                    onImageCropChange = viewModel::onImageCropChange,
                    onImageDuplicateClick = viewModel::onImageDuplicate,
                    onImageSendToBackClick = viewModel::onImageSendToBack,
                    onImageBringToFrontClick = viewModel::onImageBringToFront,
                    onImageDeleteClick = viewModel::onImageDelete,
                    onImageResetScaleClick = viewModel::onImageResetScale,
                    onPenyDraftInputChange = viewModel::onPenyDraftInputChange,
                    hazeState = hazeState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FloatingHorizontalMargin)
                        .padding(
                            top = statusBarHeightDp + with(density) { toolbarHeightPx.toDp() } + PaperVerticalMargin,
                            bottom = PaperVerticalMargin,
                        )
                        .captureIntoGraphicsLayer(compositeGraphicsLayer),
                )
            }

            // Toolbar & Sticky Reply Banner float directly over the scrollable paper
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onSizeChanged { toolbarHeightPx = it.height.toFloat() }
                    .statusBarsPadding()
                    .padding(horizontal = FloatingHorizontalMargin)
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ComposeToolbar(
                    isAnnotateMode = uiState.isAnnotateMode,
                    isKeyboardVisible = uiState.isKeyboardVisible,
                    isPenyModeActive = uiState.isPenyModeActive,
                    isPenyModeAvailable = uiState.isPenyModeAvailable,
                    isPenyReplyLoading = uiState.isPenyReplyLoading,
                    showPenyDiscoveryBadge = uiState.hasTriedPenyMode == false,
                    canUndo = uiState.canUndo,
                    canRedo = uiState.canRedo,
                    hazeState = hazeState,
                    onUndoClick = viewModel::onUndoClick,
                    onRedoClick = viewModel::onRedoClick,
                    onFontClick = { viewModel.onFontPickerOpenChange(true) },
                    onKeyboardToggleClick = {
                        if (uiState.isKeyboardVisible) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        } else {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    },
                    onAnnotateClick = {
                        // Ornaments are locked while Mode Peny is active
                        // (peny-mode-spec.md §5.4) — belt-and-suspenders with
                        // ComposeToolbar disabling the icon itself.
                        if (uiState.isPenyModeActive) return@ComposeToolbar
                        if (!isCompositingSupported) {
                            isDeviceCapabilitySheetVisible = true
                            return@ComposeToolbar
                        }
                        val enteringAnnotate = !uiState.isAnnotateMode
                        viewModel.onToggleAnnotateMode()
                        if (enteringAnnotate) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    },
                    onChangePaperClick = {
                        if (uiState.isPenyModeActive) return@ComposeToolbar
                        viewModel.onPaperPickerOpenChange(true)
                    },
                    onInsertImageClick = {
                        if (uiState.isPenyModeActive) return@ComposeToolbar
                        if (!isCompositingSupported) {
                            isDeviceCapabilitySheetVisible = true
                            return@ComposeToolbar
                        }
                        imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onAddStickerClick = {
                        if (uiState.isPenyModeActive) return@ComposeToolbar
                        if (!isCompositingSupported) {
                            isDeviceCapabilitySheetVisible = true
                            return@ComposeToolbar
                        }
                        viewModel.onStickerPickerOpenChange(true)
                    },
                    onSaveToPhotosClick = {
                        coroutineScope.launch {
                            val compositeBytes = captureCleanComposite(
                                isCompositingSupported = isCompositingSupported,
                                isFieldFocused = uiState.isKeyboardVisible,
                                selectedImageId = uiState.selectedImageId ?: uiState.croppingImageId,
                                focusManager = focusManager,
                                keyboardController = keyboardController,
                                focusRequester = focusRequester,
                                onDeselectImage = { viewModel.onImageSelect(null) },
                                onReselectImage = { id -> viewModel.onImageSelect(id) },
                                graphicsLayer = compositeGraphicsLayer,
                                restoreState = true,
                            )
                            if (compositeBytes != null) {
                                val uri = saveImageToGallery(context, compositeBytes, "unsealed_draft_${System.currentTimeMillis()}.webp")
                                if (uri != null) {
                                    savePhotoFeedback = context.getString(R.string.share_letter_saved_toast)
                                }
                            }
                        }
                    },
                    onShareClick = {
                        coroutineScope.launch {
                            val compositeBytes = captureCleanComposite(
                                isCompositingSupported = isCompositingSupported,
                                isFieldFocused = uiState.isKeyboardVisible,
                                selectedImageId = uiState.selectedImageId ?: uiState.croppingImageId,
                                focusManager = focusManager,
                                keyboardController = keyboardController,
                                focusRequester = focusRequester,
                                onDeselectImage = { viewModel.onImageSelect(null) },
                                onReselectImage = { id -> viewModel.onImageSelect(id) },
                                graphicsLayer = compositeGraphicsLayer,
                                restoreState = true,
                            )
                            if (compositeBytes != null) {
                                val uri = writeShareCacheFile(context, compositeBytes, "draft_${System.currentTimeMillis()}")
                                val shareIntent = buildSystemChooserIntent(
                                    imageUri = uri,
                                    caption = uiState.bodyText,
                                    chooserTitle = context.getString(R.string.overflow_menu_share),
                                )
                                context.startActivity(shareIntent)
                            } else {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, uiState.bodyText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.overflow_menu_share)))
                            }
                        }
                    },
                    onTogglePublicShowcaseClick = {
                        if (uiState.isPublicShowcase) {
                            viewModel.setPublicShowcase(false)
                        } else {
                            showPublicShowcaseDialog = true
                        }
                    },
                    onPenyModeToggle = viewModel::onPenyModeToggle,
                    isPublicShowcase = uiState.isPublicShowcase,
                    isSending = uiState.isSending,
                    isCompositingSupported = isCompositingSupported,
                    onSendClick = {
                        coroutineScope.launch {
                            val compositeBytes = captureCleanComposite(
                                isCompositingSupported = isCompositingSupported,
                                isFieldFocused = uiState.isKeyboardVisible,
                                selectedImageId = uiState.selectedImageId ?: uiState.croppingImageId,
                                focusManager = focusManager,
                                keyboardController = keyboardController,
                                focusRequester = null,
                                onDeselectImage = { viewModel.onImageSelect(null) },
                                graphicsLayer = compositeGraphicsLayer,
                                restoreState = false,
                            )
                            if (viewModel.compositeAndSaveDraftForSend(compositeBytes)) {
                                onSendClick()
                            }
                        }
                    },
                )

                val replyName = uiState.replyRecipientName
                if (!replyName.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    StickyReplyBanner(
                        recipientName = replyName,
                        hasLetterBody = uiState.replyLetter != null,
                        onClick = {
                            if (uiState.replyLetter != null) {
                                viewModel.onOriginalLetterSheetToggle(true)
                            } else {
                                onBackClick?.invoke()
                            }
                        },
                    )
                }
            }

            LetterlyTopSnackbar(
                message = uiState.error ?: savePhotoFeedback,
                onDismiss = {
                    if (uiState.error != null) viewModel.dismissError()
                    savePhotoFeedback = null
                },
                style = if (uiState.error != null) SnackbarStyle.Warning else SnackbarStyle.Info,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }

        if (uiState.isPenyModeActive) {
            // No FormattingBar/AnnotateToolbar while Mode Peny is active —
            // it's a plain-text conversation, not rich-text editing
            // (peny-mode-spec.md §5.4).
        } else if (uiState.isAnnotateMode) {
            AnnotateToolbar(
                annotateState = uiState.annotateState,
                onToolSelect = viewModel::onAnnotateToolSelect,
                onColorSelect = viewModel::onAnnotateColorSelect,
            )
        } else if (uiState.isKeyboardVisible) {
            FormattingBar(
                paperColor = uiState.paperColor.color,
                textAlignment = uiState.textAlignment,
                isBold = uiState.effectiveBold,
                isItalic = uiState.effectiveItalic,
                fontSize = uiState.effectiveFontSize,
                inkColor = uiState.inkColor,
                onFontClick = { viewModel.onFontPickerOpenChange(true) },
                onFontSizeChange = viewModel::onFontSizeChange,
                onInkColorChange = viewModel::onInkColorChange,
                onAlignmentClick = { viewModel.onTextAlignmentChange(nextAlignment(uiState.textAlignment)) },
                onBoldClick = viewModel::onBoldToggle,
                onItalicClick = viewModel::onItalicToggle,
                onIndentClick = viewModel::onIndentClick,
                onHideKeyboardClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
            )
        }
    }

    if (uiState.isOriginalLetterSheetOpen && uiState.replyLetter != null) {
        OriginalLetterBottomSheet(
            letter = uiState.replyLetter!!,
            onDismiss = { viewModel.onOriginalLetterSheetToggle(false) },
        )
    }
    if (uiState.isFontPickerOpen) {
        FontPickerSheet(
            selectedFont = uiState.selectedFont,
            selectedGoogleFontName = uiState.selectedGoogleFontName,
            googleFontStatuses = uiState.googleFontStatuses,
            onFontSelect = viewModel::onFontSelect,
            onGoogleFontDownloadStart = viewModel::onGoogleFontDownloadStart,
            onGoogleFontDownloaded = viewModel::onGoogleFontDownloaded,
            onGoogleFontDownloadFailed = viewModel::onGoogleFontDownloadFailed,
            onGoogleFontSelect = viewModel::onGoogleFontSelect,
            onDismiss = { viewModel.onFontPickerOpenChange(false) },
        )
    }
    if (uiState.isPaperPickerOpen) {
        PaperPickerSheet(
            selectedTemplate = uiState.selectedPaperTemplate,
            selectedPaperUrl = uiState.selectedPaperUrl,
            creatorPapers = creatorPapers,
            onTemplateSelect = viewModel::onPaperTemplateSelect,
            onPaperUrlSelect = viewModel::onPaperUrlSelect,
            onDismiss = { viewModel.onPaperPickerOpenChange(false) },
            onNavigateToStore = { source ->
                viewModel.onFindMoreCollectiblesClick("paper", source)
                onNavigateToStore?.invoke()
            },
        )
    }
    if (uiState.isStickerPickerOpen) {
        StickerPickerSheet(
            stickers = stickerCatalog,
            onStickerSelect = viewModel::onStickerInsert,
            onDismiss = { viewModel.onStickerPickerOpenChange(false) },
            onNavigateToStore = { source ->
                viewModel.onFindMoreCollectiblesClick("sticker", source)
                onNavigateToStore?.invoke()
            },
        )
    }
    if (isDeviceCapabilitySheetVisible) {
        DeviceCapabilityBottomSheet(onDismiss = { isDeviceCapabilitySheetVisible = false })
    }
    if (showPublicShowcaseDialog) {
        LetterlyCenterDialog(
            title = stringResource(R.string.compose_public_showcase_dialog_title),
            body = stringResource(R.string.compose_public_showcase_dialog_body),
            primaryCtaText = stringResource(R.string.compose_public_showcase_dialog_confirm),
            onPrimaryClick = {
                viewModel.setPublicShowcase(true)
                showPublicShowcaseDialog = false
            },
            secondaryCtaText = stringResource(R.string.compose_public_showcase_dialog_cancel),
            onSecondaryClick = { showPublicShowcaseDialog = false },
            onDismissRequest = { showPublicShowcaseDialog = false },
        )
    }
    if (uiState.showPenyEnergyPaywall) {
        LetterlyCenterDialog(
            title = stringResource(R.string.compose_peny_energy_paywall_title),
            body = stringResource(R.string.compose_peny_energy_paywall_body),
            primaryCtaText = stringResource(R.string.compose_peny_energy_paywall_cta),
            onPrimaryClick = {
                viewModel.onDismissPenyEnergyPaywall()
                onBuyEnergyClick()
            },
            secondaryCtaText = stringResource(R.string.compose_peny_energy_paywall_dismiss),
            onSecondaryClick = viewModel::onDismissPenyEnergyPaywall,
            onDismissRequest = viewModel::onDismissPenyEnergyPaywall,
        )
    }
    if (uiState.showPenyCopyDialog && uiState.penyTextToCopy != null) {
        val textToCopy = uiState.penyTextToCopy.orEmpty()
        val singleLinePreview = textToCopy.trim().replace(Regex("\\s+"), " ")
        val previewText = if (singleLinePreview.length > 80) singleLinePreview.take(77) + "…" else singleLinePreview
        val dialogBody = if (previewText.isNotEmpty()) {
            "\"$previewText\"\n\n${stringResource(R.string.compose_peny_copy_dialog_body)}"
        } else {
            stringResource(R.string.compose_peny_copy_dialog_body)
        }
        LetterlyCenterDialog(
            title = stringResource(R.string.compose_peny_copy_dialog_title),
            body = dialogBody,
            primaryCtaText = stringResource(R.string.compose_peny_copy_dialog_confirm),
            onPrimaryClick = {
                clipboardManager.setText(AnnotatedString(textToCopy))
                viewModel.onConfirmCopyPenyText()
            },
            secondaryCtaText = stringResource(R.string.compose_peny_copy_dialog_dismiss),
            onSecondaryClick = viewModel::onDiscardPenyTextAndExit,
            onDismissRequest = viewModel::onCancelPenyCopyDialog,
        )
    }
}

private fun nextAlignment(current: TextAlign): TextAlign = when (current) {
    TextAlign.Start -> TextAlign.Center
    TextAlign.Center -> TextAlign.End
    else -> TextAlign.Start
}

/**
 * Captures [graphicsLayer] to bytes, ensuring all editing artifacts are cleared:
 * 1. Clears text field focus/cursor and hides the keyboard.
 * 2. Clears any selected image (handles, dashed border, action menu) and crop mode.
 * 3. Waits two frames ([withFrameNanos]) so Compose recomposition reaches the draw phase
 *    before [GraphicsLayer.toCompositeBytes] records the canvas.
 *
 * For Save-to-Photos and Share (which stay on this screen), [restoreState] restores
 * the previous focus and image selection afterward. For Send (which navigates away),
 * [restoreState] is false.
 */
private suspend fun captureCleanComposite(
    isCompositingSupported: Boolean,
    isFieldFocused: Boolean,
    selectedImageId: String?,
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController?,
    focusRequester: FocusRequester?,
    onDeselectImage: () -> Unit,
    onReselectImage: ((String) -> Unit)? = null,
    graphicsLayer: GraphicsLayer,
    restoreState: Boolean = false,
): ByteArray? {
    if (!isCompositingSupported) return null

    focusManager.clearFocus()
    keyboardController?.hide()
    if (selectedImageId != null) {
        onDeselectImage()
    }

    withFrameNanos {}
    withFrameNanos {}

    val bytes = graphicsLayer.toCompositeBytes()

    if (restoreState) {
        if (selectedImageId != null && onReselectImage != null) {
            onReselectImage(selectedImageId)
        }
        if (isFieldFocused && focusRequester != null) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    return bytes
}
