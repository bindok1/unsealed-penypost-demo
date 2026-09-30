package com.apps.unsealed.ui.screens.compose.widgets

import com.apps.unsealed.ui.screens.compose.constants.*
import com.apps.unsealed.ui.screens.compose.state.*

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import coil3.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.ui.theme.LetterBodyStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

/** Mirrors `LetterlySpring.Envelope`'s constants (0.7f damping / 180f
 * stiffness — "berat, lambat, terasa seperti kertas fisik", motion-rules.md
 * §2) as a Color-typed spring, since the shared object only exposes
 * `spring<Float>`. Same precedent as `AnnotateColorPicker.kt`'s
 * `BouncyColorSpring`. Used for the ink-color crossfade when Mode Peny
 * flips [ComposeUiState.inkColor] to white — a paper/ink property changing
 * state, not a tap response, so it gets the paper-weight spring rather than
 * Snappy/Bouncy. */
private val EnvelopeColorSpring = spring<Color>(
    dampingRatio = 0.7f,
    stiffness = 180f,
)

/**
 * Layer 2 (template PNG) from the design spec renders a textured
 * [ComposeUiState.selectedPaperTemplate] full-bleed over the flat
 * [ComposeUiState.paperColor] when one is picked (compose-screen-spec.md
 * §7) — the two are mutually exclusive from the picker's side, but the flat
 * color is always drawn underneath so texture assets with any transparency
 * still have a sane background. The paper texture + BlendMode.Multiply
 * overlay described in the original spec is still deferred in favor of this
 * simpler full-image swap. Layer 5 (Sticker Overlay, §8) is still unbuilt for
 * real stickers, but user-inserted photos (from the overflow menu's
 * "Sisipkan Gambar") are full editable layers — see `EditableImage.kt` and
 * `docs/image-edit-mode-handoff.md`.
 */
@Composable
fun LetterCanvas(
    uiState: ComposeUiState,
    focusRequester: FocusRequester,
    onBodyTextChange: (TextFieldValue) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onAnnotatePathCommit: (AnnotatePath) -> Unit,
    onAnnotateErase: (Offset, Float) -> Unit,
    onCanvasSizeChanged: (widthPx: Float, heightPx: Float) -> Unit,
    onImageSelect: (String?) -> Unit,
    onImageTransform: (id: String, pan: Offset, zoom: Float, rotationDeg: Float) -> Unit,
    onImageBaseSizeMeasured: (id: String, widthPx: Float, heightPx: Float) -> Unit,
    onImageReplaceClick: (id: String) -> Unit,
    onImageCropModeToggle: (id: String?) -> Unit,
    onImageCropChange: (id: String, cropRect: Rect) -> Unit,
    onImageDuplicateClick: (id: String) -> Unit,
    onImageSendToBackClick: (id: String) -> Unit,
    onImageBringToFrontClick: (id: String) -> Unit,
    onImageDeleteClick: (id: String) -> Unit,
    onImageResetScaleClick: (id: String) -> Unit,
    onPenyDraftInputChange: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    topContentPadding: Dp = 20.dp,
) {
    // The paper is a single fixed-height sheet — one page, like a real A4
    // letter — living inside a scrolling viewport Box in ComposeScreen (the
    // scroll only really engages for the toolbar/status-bar reveal effect,
    // not for overflow, since the page no longer grows). basePageHeightPx
    // (captured only while the keyboard is closed, see
    // ComposeViewModel.onPageViewportSizeChanged) is the stable "one page"
    // reference used for the fixed height below — the *live* viewport height
    // isn't used because it shrinks/grows with the keyboard, which would
    // make the page size drift every time the keyboard opens or closes.
    // Body text has no character limit; clipToBounds below simply hides
    // whatever overflows past the bottom of the page instead of scrolling to
    // it — the user notices missing text and shrinks the font themselves.
    // hazeSource marks this content as the blur source for ComposeToolbar's
    // frosted-glass effect.
    val density = LocalDensity.current
    val isReducedMotion = rememberIsReducedMotion()

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Mode Peny flips paperColor/selectedPaperTemplate/inkColor directly in
    // ComposeUiState (ComposeViewModel.onPenyModeToggle) — this is just the
    // render-layer smoothing so that flip crossfades/eases instead of
    // snapping, motion-rules.md §2's Envelope spring ("berat, lambat, terasa
    // seperti kertas fisik") since it's a paper/ink property changing, not a
    // tap response.
    val animatedInkColor by animateColorAsState(
        targetValue = uiState.inkColor,
        animationSpec = reducedMotionSpring(EnvelopeColorSpring, isReducedMotion),
        label = "letterInkColorAnim",
    )

    Box(
        modifier
            .then(
                if (uiState.basePageHeightPx > 0f) {
                    val pageHeightDp = with(density) { uiState.basePageHeightPx.toDp() }
                    Modifier.heightIn(min = pageHeightDp, max = pageHeightDp)
                } else {
                    Modifier
                },
            )
            // clipToBounds keeps annotate strokes inside the paper rect.
            .clipToBounds()
            .background(uiState.paperColor.color)
            .hazeSource(state = hazeState)
            .onSizeChanged { onCanvasSizeChanged(it.width.toFloat(), it.height.toFloat()) },
    ) {
        // Crossfade (not a bare uiState.selectedPaperTemplate?.let) so
        // switching templates — including Mode Peny's indigo swap — fades
        // smoothly instead of cutting instantly between textures.
        Crossfade(
            targetState = uiState.selectedPaperTemplate to uiState.selectedPaperUrl,
            animationSpec = reducedMotionSpring(LetterlySpring.Envelope, isReducedMotion),
            label = "paperTemplateCrossfade",
            modifier = Modifier.matchParentSize(),
        ) { (template, paperUrl) ->
            if (template != null) {
                // fillMaxSize here, not matchParentSize — Crossfade's content
                // lambda has no BoxScope receiver to call matchParentSize
                // with. The outer Crossfade's own `modifier` above already
                // carries matchParentSize, so this Image just fills that
                // already-bounded region without driving the outer Box's own
                // size (same end result as the original matchParentSize).
                Image(
                    painter = painterResource(template.drawableRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (!paperUrl.isNullOrBlank()) {
                AsyncImage(
                    model = paperUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // bodyAnnotated rebuilds the whole AnnotatedString from bodyText +
        // bodyStyleRuns (including an O(length) newline scan for paragraph
        // indent, see TextStyleRuns.buildStyledBody) — reading it as a bare
        // getter inside BasicTextField's `value` reran that work on every
        // LetterCanvas recomposition, including ones triggered by unrelated
        // uiState fields (image drag, annotate mode, etc.), not just actual
        // keystrokes. That synchronous rebuild sitting on the IME round-trip
        // was dropping/duplicating characters when typing fast. Memoizing on
        // the two inputs that actually matter keeps it to one rebuild per
        // real text edit.
        val bodyAnnotated = remember(uiState.bodyText, uiState.bodyStyleRuns) {
            buildStyledBody(uiState.bodyText, uiState.bodyStyleRuns)
        }

        // Local mutable TextFieldValue ensures immediate, synchronous rendering
        // on every keystroke/backspace, preventing IME desync, dropped keystrokes,
        // and rendering delay caused by asynchronous StateFlow collectAsState round-trips.
        // It also preserves the IME's active composition range, which was previously
        // destroyed because TextFieldValue(annotatedString, selection) defaulted composition to null.
        var bodyFieldValue by remember {
            mutableStateOf(
                TextFieldValue(
                    annotatedString = bodyAnnotated,
                    selection = uiState.bodySelection,
                ),
            )
        }

        var currentStyleRuns by remember { mutableStateOf(uiState.bodyStyleRuns) }

        SideEffect {
            currentStyleRuns = uiState.bodyStyleRuns
            if (bodyFieldValue.text != uiState.bodyText) {
                // External text mutation (Undo/Redo, draft load, or Mode Peny text confirmed)
                bodyFieldValue = TextFieldValue(
                    annotatedString = bodyAnnotated,
                    selection = uiState.bodySelection,
                )
            } else {
                // Synchronize span styling changes (e.g. bold toggle, size change)
                // or external selection changes, while keeping the active IME composition intact!
                if (bodyFieldValue.annotatedString != bodyAnnotated) {
                    bodyFieldValue = bodyFieldValue.copy(annotatedString = bodyAnnotated)
                }
                if (bodyFieldValue.selection != uiState.bodySelection) {
                    bodyFieldValue = bodyFieldValue.copy(selection = uiState.bodySelection)
                }
            }
        }

        // Mode Peny "takes over" this same TextField instead of being a
        // separate widget (peny-mode-spec.md §4.1/§4.4) — bodyText/bodyStyleRuns
        // are never read/written while isPenyModeActive is true, and vice
        // versa, so the two draft sources can't cross-contaminate.
        var penyFieldValue by remember {
            mutableStateOf(
                TextFieldValue(
                    text = uiState.penyDraftInput,
                    selection = TextRange(uiState.penyDraftInput.length),
                ),
            )
        }

        SideEffect {
            if (penyFieldValue.text != uiState.penyDraftInput) {
                penyFieldValue = TextFieldValue(
                    text = uiState.penyDraftInput,
                    selection = TextRange(uiState.penyDraftInput.length),
                )
            }
        }

        BasicTextField(
            // TextFieldValue(AnnotatedString) overload (rather than the plain
            // String one) so bodyStyleRuns' per-range bold/italic/size spans
            // (see TextStyleRuns.kt) actually render inline instead of only
            // being visible via a single uniform textStyle below.
            value = if (uiState.isPenyModeActive) penyFieldValue else bodyFieldValue,
            onValueChange = { new ->
                if (uiState.isPenyModeActive) {
                    penyFieldValue = new
                    onPenyDraftInputChange(new.text)
                } else {
                    val styledAnnotated = if (new.text == bodyFieldValue.text) {
                        bodyFieldValue.annotatedString
                    } else {
                        val updatedRuns = currentStyleRuns.afterTextChange(bodyFieldValue.text, new.text) { previousRun ->
                            StyleRun(
                                start = 0,
                                end = 0,
                                bold = uiState.pendingBold ?: previousRun?.bold ?: false,
                                italic = uiState.pendingItalic ?: previousRun?.italic ?: false,
                                fontSize = uiState.pendingFontSize ?: previousRun?.fontSize ?: uiState.fontSize,
                                indentLevel = previousRun?.indentLevel ?: 0,
                            )
                        }
                        currentStyleRuns = updatedRuns
                        buildStyledBody(new.text, updatedRuns)
                    }
                    bodyFieldValue = new.copy(annotatedString = styledAnnotated)
                    onBodyTextChange(new)
                }
            },
            // Blocked only while actually waiting on `POST /peny/reply` — the
            // absorb animation (Lapis 1) always accepts typing otherwise,
            // including through the debounce window.
            enabled = !(uiState.isPenyModeActive && uiState.isPenyReplyLoading),
            textStyle = LetterBodyStyle.copy(
                fontFamily = uiState.activeFontFamily(),
                fontSize = uiState.fontSize.sp,
                color = animatedInkColor,
                textAlign = uiState.textAlignment,
            ),
            modifier = Modifier
                .fillMaxWidth()
                // Measures with an unbounded max height so CoreTextField's
                // built-in "scroll to keep cursor in view" behavior never
                // kicks in (it only activates when the incoming height
                // constraint is bounded, which it otherwise is here thanks
                // to the parent's fixed pageHeightDp). Overflow past the
                // page's bottom edge is left to the parent Box's
                // clipToBounds to hide, matching a real sheet of paper
                // instead of a scrollable text view.
                .wrapContentHeight(unbounded = true, align = Alignment.Top)
                .padding(horizontal = 40.dp)
                .padding(top = topContentPadding + 40.dp, bottom = 40.dp)
                .focusRequester(focusRequester)
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                    if (focusState.isFocused) {
                        coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                    }
                },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default,
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (uiState.isPenyModeActive) {
                        if (uiState.penyDraftInput.isEmpty()) {
                            PenyPlaceholderCarousel(
                                style = LetterBodyStyle.copy(
                                    fontFamily = uiState.activeFontFamily(),
                                    fontSize = uiState.fontSize.sp,
                                    color = animatedInkColor.copy(alpha = 0.4f),
                                ),
                            )
                        }
                    } else if (uiState.bodyText.isEmpty()) {
                        Text(
                            text = stringResource(R.string.compose_body_placeholder),
                            style = LetterBodyStyle.copy(
                                fontFamily = uiState.activeFontFamily(),
                                fontSize = uiState.fontSize.sp,
                                color = animatedInkColor.copy(alpha = 0.4f),
                            ),
                        )
                    }
                    innerTextField()
                }
            },
        )
        // Center reveal for Mode Peny's replies — sits above the paper
        // texture but below the TextField/images in z-order, which is fine
        // since the TextField itself is swapped to the (short) Peny draft
        // while this mode is active (peny-mode-spec.md §5.3).
        PenyReplyOverlay(
            reply = if (uiState.isPenyModeActive) uiState.penyVisibleReply ?: uiState.penyEnergyErrorMessage else null,
            canvasWidthPx = uiState.canvasWidthPx,
            inkColor = animatedInkColor,
            modifier = Modifier.matchParentSize(),
        )
        AnnotateCanvas(
            annotateState = uiState.annotateState,
            isAnnotateMode = uiState.isAnnotateMode,
            onPathCommit = onAnnotatePathCommit,
            onErase = onAnnotateErase,
            // matchParentSize, not fillMaxSize — same reasoning as the paper
            // texture Image above.
            modifier = Modifier.matchParentSize(),
        )
        // "Tap elsewhere confirms placement": while an image is selected, a
        // transparent full-canvas catcher sits below the images (so an
        // image's own gestures still win inside its own bounds — composed
        // later below, Compose hit-tests topmost-first) but above the text
        // field, so a tap anywhere else deselects instead of placing a text
        // cursor. Suppressed while actively cropping — that has its own
        // explicit Reset/Selesai controls instead.
        if (uiState.selectedImageId != null && uiState.croppingImageId == null) {
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(uiState.selectedImageId) {
                        detectTapGestures { onImageSelect(null) }
                    },
            )
        }
        // Layer 5 (Sticker Overlay) from the design spec isn't built for real
        // stickers yet (compose-screen-spec.md §8), but user-inserted photos
        // are full editable layers: drag/pinch/rotate/crop/duplicate/reorder
        // via EditableImage.kt.
        //
        // Images are anchored inside a fixed-height inner Box (basePageHeightPx,
        // not the outer Box's own — possibly content-grown — height) so
        // Alignment.Center resolves against a stable reference. Without this,
        // an image placed while the letter was short would visually drift
        // downward as the outer Box grows taller with more typed text — see
        // ComposeViewModel.onImageTransform's matching basePageHeightPx clamp.
        if (uiState.basePageHeightPx > 0f) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(with(density) { uiState.basePageHeightPx.toDp() }),
            ) {
                uiState.images.forEach { image ->
                    key(image.id) {
                        if (image.id == uiState.croppingImageId) {
                            ImageCropOverlay(
                                image = image,
                                onCropChange = { rect -> onImageCropChange(image.id, rect) },
                                onReset = { onImageCropChange(image.id, FullCropRect) },
                                onDone = { onImageCropModeToggle(null) },
                                modifier = Modifier.align(Alignment.Center),
                            )
                        } else {
                            EditableImage(
                                image = image,
                                isSelected = image.id == uiState.selectedImageId,
                                onSelect = { onImageSelect(image.id) },
                                onTransform = { pan, zoom, rot -> onImageTransform(image.id, pan, zoom, rot) },
                                onBaseSizeMeasured = { w, h -> onImageBaseSizeMeasured(image.id, w, h) },
                                onReplaceClick = { onImageReplaceClick(image.id) },
                                onCropClick = { onImageCropModeToggle(image.id) },
                                onDuplicateClick = { onImageDuplicateClick(image.id) },
                                onSendToBackClick = { onImageSendToBackClick(image.id) },
                                onBringToFrontClick = { onImageBringToFrontClick(image.id) },
                                onDeleteClick = { onImageDeleteClick(image.id) },
                                onResetScaleClick = { onImageResetScaleClick(image.id) },
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
            }
        }
        // Mode Peny's entry point now lives in ComposeToolbar (PenyWandHint)
        // instead of floating over the paper here — grouping it with the
        // other tool icons instead of scattering it across a different
        // visual layer. See ComposeToolbar.kt for the trigger itself.
    }
}
