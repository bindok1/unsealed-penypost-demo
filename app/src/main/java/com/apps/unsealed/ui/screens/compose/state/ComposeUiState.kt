package com.apps.unsealed.ui.screens.compose.state

import android.net.Uri
import com.apps.unsealed.ui.screens.compose.constants.FontDownloadStatus
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import com.apps.unsealed.ui.theme.ArchitectsDaughterFontFamily
import com.apps.unsealed.ui.theme.CaveatFontFamily
import com.apps.unsealed.ui.theme.IndieFlowerFontFamily
import com.apps.unsealed.ui.theme.InkDefault
import com.apps.unsealed.ui.theme.KalamFontFamily
import com.apps.unsealed.ui.theme.NothingYouCouldDoFontFamily
import com.apps.unsealed.ui.theme.PaperColor
import com.apps.unsealed.ui.theme.PaperTemplate
import com.apps.unsealed.ui.theme.ShadowsIntoLightFontFamily

const val ComposeFontSizeMin = 12f
const val ComposeFontSizeMax = 24f

data class ComposeUiState(
    val dearName: String = "",
    /** Plain characters only — all styling lives in [bodyStyleRuns], never
     * inline in this string. Kept as the editing source of truth (rather than
     * [AnnotatedString] itself) because [bodyStyleRuns] needs stable
     * start/end offsets to survive undo/redo snapshots and range-diffing in
     * [TextStyleRuns.afterTextChange] cleanly. */
    val bodyText: String = "",
    /** Per-range bold/italic/size/indent overrides — always fully partitions
     * `[0, bodyText.length)`, see [StyleRun]. [bodyAnnotated] renders this
     * into what [LetterCanvas]'s `BasicTextField` actually displays/edits. */
    val bodyStyleRuns: List<StyleRun> = emptyList(),
    /** Current selection/cursor position in [bodyText], hoisted here (rather
     * than local Compose state) so [ComposeViewModel]'s FormattingBar actions
     * know which range of [bodyStyleRuns] to restyle. */
    val bodySelection: TextRange = TextRange.Zero,
    /** Bold/italic/size the user toggled with [bodySelection] collapsed (a
     * bare cursor, nothing highlighted) — applies to the *next* characters
     * typed rather than retroactively to existing text. `null` means
     * "inherit whatever's already at the cursor", matching the style shown
     * by [effectiveBold]/[effectiveItalic]/[effectiveFontSize]. Cleared back
     * to null by [ComposeViewModel] whenever the selection moves without a
     * text edit (e.g. tapping elsewhere), so the toolbar doesn't keep
     * showing a stale toggle from wherever the cursor used to be. */
    val pendingBold: Boolean? = null,
    val pendingItalic: Boolean? = null,
    val pendingFontSize: Float? = null,
    val signOff: String = "",
    val selectedFont: LetterFont = LetterFont.CAVEAT,
    val selectedGoogleFontName: String? = null,
    val googleFontStatuses: Map<String, FontDownloadStatus> = emptyMap(),
    /** Default/base size — only actually used to seed [bodyStyleRuns] for the
     * very first character ever typed into an empty letter. Once any text
     * exists, sizing is per-selection via [bodyStyleRuns]/[pendingFontSize];
     * this field is no longer mutated by FormattingBar's size slider. */
    val fontSize: Float = 18f,
    val textAlignment: TextAlign = TextAlign.Start,
    val inkColor: Color = InkDefault,
    val paperColor: PaperColor = PaperColor.CREAM,
    val selectedPaperTemplate: PaperTemplate? = null,
    val selectedPaperUrl: String? = null,
    val stickers: List<StickerInstance> = emptyList(),
    val images: List<ImageInstance> = emptyList(),
    val selectedImageId: String? = null,
    val croppingImageId: String? = null,
    /** Measured px size of the paper canvas — grows with `bodyText` content
     * beyond one page (see [basePageHeightPx]). [ComposeViewModel.onImageTransform]
     * clamps an image's horizontal center against [canvasWidthPx] (stable,
     * width never grows), but no longer uses [canvasHeightPx] for the
     * vertical clamp — see [basePageHeightPx]. 0f until `LetterCanvas`
     * reports its first layout pass. */
    val canvasWidthPx: Float = 0f,
    val canvasHeightPx: Float = 0f,
    /** Stable "one page" reference height (px) — captured only while the
     * keyboard is closed (see [ComposeViewModel.onPageViewportSizeChanged]).
     * Deliberately not the live viewport height, which shrinks/grows with
     * the keyboard. Used for `LetterCanvas`'s min/max-height bounds, the
     * page-full height cap, and the fixed center reference images anchor
     * against — unlike [canvasWidthPx]/[canvasHeightPx], which grow with
     * content and would make images drift downward as more text is typed if
     * used for anchoring. 0f until the first keyboard-closed layout pass. */
    val basePageHeightPx: Float = 0f,
    val annotateState: AnnotateState = AnnotateState(),
    val visibility: LetterVisibility = LetterVisibility.OPEN,
    /** Opt-in per letter: shows this letter's envelope/paper in the sender's
     * Postal Collection showcase (own profile + anyone viewing their public
     * profile) — see `docs/be/profile_api.md`'s `PUT /auth/me/languages`-
     * adjacent showcase spec. Unrelated to [visibility]/[LetterVisibility],
     * which is a separate unbuilt pen-pal/matching concept — this is a
     * distinct field to avoid colliding the two meanings. Defaults off since
     * most letters are private correspondence, not showcase material. */
    val isPublicShowcase: Boolean = false,
    val crisisTag: CrisisTag = CrisisTag.NONE,
    val isKeyboardVisible: Boolean = false,
    val isAnnotateMode: Boolean = false,
    val isFontPickerOpen: Boolean = false,
    val isPaperPickerOpen: Boolean = false,
    val isTemplatePicker: Boolean = false,
    val isStickerPickerOpen: Boolean = false,
    val isSending: Boolean = false,
    val isDraft: Boolean = false,
    val error: String? = null,
    val canUndoText: Boolean = false,
    val canRedoText: Boolean = false,
    val canUndoAnnotate: Boolean = false,
    val canRedoAnnotate: Boolean = false,
    val replyRecipientName: String? = null,
    val replyLetter: com.apps.unsealed.ui.screens.penpals.state.PenpalLetter? = null,
    val isOriginalLetterSheetOpen: Boolean = false,

    /** True while Mode Peny is active — the TextField is "taken over" by a
     * temporary conversation (see draft product spec §3). [bodyText] is NOT
     * touched while true; the paper shows [penyHistory]/[penyDraftInput]
     * instead of the letter body. Can only be true when [isPenyModeAvailable]. */
    val isPenyModeActive: Boolean = false,

    /** `GET /peny/status`'s `is_enabled` — used to be combined with
     * `isPremium` from `GET /auth/me`, but the backend dropped subscription
     * gating for Mode Peny (Energy spent per-reply is the only cost now), so
     * this is just `is_enabled` on its own. false means the trigger is never
     * rendered at all, not rendered-then-disabled (see
     * peny_mode_mobile_integration.md §2). */
    val isPenyModeAvailable: Boolean = false,

    /** From [com.apps.unsealed.core.data.PenyPreferences] — null while still
     * loading from disk (see `ComposeViewModel.init`), false until the user
     * has activated Mode Peny at least once. Drives `PenyWandHint`'s "new
     * feature" discovery dot: shown only while this is `false`, gone for
     * good the first time the user actually taps in (never re-shown, even if
     * they exit Mode Peny again afterward). */
    val hasTriedPenyMode: Boolean? = null,

    /** This session's conversation — **in-memory only**, never written to
     * Room/DataStore, never part of a Draft-on-Exit snapshot. Dropped
     * entirely the moment [isPenyModeActive] goes back to false. */
    val penyHistory: List<PenyTurn> = emptyList(),

    /** The raw draft the user is typing into the TextField while Mode Peny is
     * active — the Peny-conversation analogue of [bodyText], kept separate so
     * [bodyText] is never touched while this mode is on. Reset to empty once
     * a turn is absorbed into [penyHistory]. */
    val penyDraftInput: String = "",

    /** True from the moment `POST /peny/reply` is sent until a response or
     * error comes back — drives the glow's "processing" state and disables
     * input meanwhile. */
    val isPenyReplyLoading: Boolean = false,

    /** Non-null while Peny's reply (or the first chunk of a multi-part
     * fallback) is showing in the center reveal zone. Null = reply zone
     * empty. */
    val penyVisibleReply: String? = null,

    /** Remaining chunks of a multi-part fallback reply — normally empty. Once
     * non-empty, the first entry pops into [penyVisibleReply] as soon as the
     * current one finishes its fade-out. */
    val penyPendingReplyChunks: List<String> = emptyList(),

    /** Peny-voiced error copy for the `INSUFFICIENT_ENERGY` case — never a
     * raw API error. Null = no active error. */
    val penyEnergyErrorMessage: String? = null,

    /** True right when [penyEnergyErrorMessage] is set from the
     * `INSUFFICIENT_ENERGY` case — drives a proactive "buy more Energy?"
     * center dialog (the "jemput bola" upsell, docs/copywriting/cp.md)
     * layered on top of the in-character bubble instead of leaving the user
     * at a dead end. Dismissed independently of [penyEnergyErrorMessage],
     * which stays showing in the reply bubble either way. */
    val showPenyEnergyPaywall: Boolean = false,

    /** True while showing the confirmation dialog asking whether the last text
     * from Mode Peny should be copied into the letter body upon exit. */
    val showPenyCopyDialog: Boolean = false,

    /** The candidate text to be copied into the letter body if confirmed. */
    val penyTextToCopy: String? = null,
) {
    /** Toolbar's single Undo/Redo pair is mode-aware (compose-screen-spec.md
     * §1): text history when typing, stroke history in Annotate Mode — see
     * [ComposeViewModel.onUndoClick]/[ComposeViewModel.onRedoClick]. */
    val canUndo: Boolean get() = if (isAnnotateMode) canUndoAnnotate else canUndoText
    val canRedo: Boolean get() = if (isAnnotateMode) canRedoAnnotate else canRedoText

    /** What [LetterCanvas]'s `BasicTextField` actually renders/edits —
     * [bodyText] with [bodyStyleRuns] turned into real Compose spans. */
    val bodyAnnotated: AnnotatedString get() = buildStyledBody(bodyText, bodyStyleRuns)

    /** The run "at" [bodySelection] — the run owning the selection's start
     * for a range selection, or the run just *before* a collapsed cursor
     * (matching where a new keystroke would land), falling back to whatever
     * owns index 0 at the very start of an empty/fresh letter. */
    private val runAtSelection: StyleRun?
        get() {
            val probeIndex = if (bodySelection.collapsed) {
                (bodySelection.start - 1).coerceAtLeast(0)
            } else {
                bodySelection.start
            }
            return bodyStyleRuns.runAt(probeIndex) ?: bodyStyleRuns.runAt(0)
        }

    /** What FormattingBar's Bold/Italic/size controls should show as
     * currently active — [pendingBold]/[pendingItalic]/[pendingFontSize] if
     * the user just toggled them with the cursor collapsed, else whatever's
     * already styling the selection/cursor position. */
    val effectiveBold: Boolean get() = pendingBold ?: runAtSelection?.bold ?: false
    val effectiveItalic: Boolean get() = pendingItalic ?: runAtSelection?.italic ?: false
    val effectiveFontSize: Float get() = pendingFontSize ?: runAtSelection?.fontSize ?: fontSize
}

enum class LetterFont(val fontFamily: FontFamily, val label: String) {
    CAVEAT(CaveatFontFamily, "Caveat"),
    KALAM(KalamFontFamily, "Kalam"),
    ARCHITECTS_DAUGHTER(ArchitectsDaughterFontFamily, "Architects Daughter"),
    INDIE_FLOWER(IndieFlowerFontFamily, "Indie Flower"),
    NOTHING_YOU_COULD_DO(NothingYouCouldDoFontFamily, "Nothing You Could Do"),
    SHADOWS_INTO_LIGHT(ShadowsIntoLightFontFamily, "Shadows Into Light"),
}

// --- Stub shapes kept for shape-fidelity with the full spec; unused in P0 ---

data class StickerInstance(
    val id: String,
    val stickerId: String,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
)

/** A user-inserted photo on the letter canvas (Layer 5, compose-screen-spec.md
 * §8's "sisipkan gambar sebagai layer draggable"). [offsetX]/[offsetY] are a
 * px delta from the canvas center (parent-space, i.e. pre-rotation/-scale —
 * see [EditableImage] for why); [scale] and [rotation] (degrees) come from
 * pinch/rotate gestures on the image itself. Z-order is implicit in
 * [ComposeUiState.images]'s list order (first = back, last = front) rather
 * than a separate field — see `onImageSendToBack`/`onImageBringToFront` in
 * [ComposeViewModel].
 *
 * [cropRect] is a fractional window (0f..1f per edge, left/top/right/bottom)
 * into the image's *natural* rendered box — `Rect(0f, 0f, 1f, 1f)` (the
 * [FullCropRect] default) means uncropped. [baseWidthPx]/[baseHeightPx] cache
 * that natural box's measured size in px (0f until first measured, see
 * `EditableImage`'s `onSizeChanged`) so the crop can be re-applied at a fixed
 * pixel scale even while cropped — see `docs/image-edit-mode-handoff.md`. */
data class ImageInstance(
    val id: String,
    val uri: Uri,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val cropRect: Rect = FullCropRect,
    val baseWidthPx: Float = 0f,
    val baseHeightPx: Float = 0f,
)

val FullCropRect = Rect(0f, 0f, 1f, 1f)

enum class AnnotateTool { PEN, FINE, WAVE, DECO, ERASER }

data class AnnotatePath(
    val points: List<Offset> = emptyList(),
    val color: Color = Color(0xFFE53935),
    val strokeWidth: Float = 4f,
    val tool: AnnotateTool = AnnotateTool.PEN,
)

data class AnnotateState(
    val isActive: Boolean = false,
    val selectedTool: AnnotateTool = AnnotateTool.PEN,
    val inkColor: Color = Color(0xFFE53935),
    val strokeWidth: Float = 4f,
    val paths: List<AnnotatePath> = emptyList(),
)

/** Inferred from the design spec's `LetterVisibility.OPEN` default — the full
 * value set wasn't specified, likely tied to an unbuilt pen-pal/matching
 * feature. Revisit once that feature is designed. */
enum class LetterVisibility { OPEN, ANONYMOUS }

/** Inferred — same caveat as [LetterVisibility], likely tied to an unbuilt
 * content-moderation/crisis-support feature. Revisit once designed. */
enum class CrisisTag { NONE, FLAGGED }
