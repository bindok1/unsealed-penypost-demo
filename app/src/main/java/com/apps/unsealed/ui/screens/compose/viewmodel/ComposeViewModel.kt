package com.apps.unsealed.ui.screens.compose.viewmodel

import android.content.Context
import com.apps.unsealed.ui.screens.compose.constants.*
import com.apps.unsealed.ui.screens.compose.state.*
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.os.bundleOf
import com.apps.unsealed.R
import com.apps.unsealed.core.analytics.AnalyticsRepository
import com.apps.unsealed.core.data.ComposeDraft
import com.apps.unsealed.core.data.ComposeDraftHolder
import com.apps.unsealed.core.data.PenyPreferences
import com.apps.unsealed.core.data.WriteExitCoordinator
import com.apps.unsealed.core.database.DraftEntity
import com.apps.unsealed.feature.auth.data.AuthResult
import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.feature.catalog.data.CatalogRepository
import com.apps.unsealed.feature.draft.data.DraftRepository
import com.apps.unsealed.feature.peny.data.PenyReplyResult
import com.apps.unsealed.feature.peny.data.PenyRepository
import com.apps.unsealed.feature.storage.data.StorageRepository
import com.apps.unsealed.feature.store.data.StoreRepository
import com.apps.unsealed.navigation.WriteDraftIdArg
import com.apps.unsealed.navigation.WriteReplyNameArg
import com.apps.unsealed.navigation.WriteReplyUserIdArg
import com.apps.unsealed.ui.screens.selectrecipient.constants.localStickerCatalog
import com.apps.unsealed.ui.theme.PaperColor
import com.apps.unsealed.ui.theme.PaperTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Resize gesture clamp on [ImageInstance.scale] — loose enough to go from a
 * thumbnail to near-full-bleed, tight enough that a stray pinch can't shrink
 * an image to invisible or blow it up past the paper. */
private const val ImageMinScale = 0.3f
private const val ImageMaxScale = 4f

/** Default scale when inserting a photo vs a decorative sticker. */
private const val DefaultPhotoScale = 1.0f
private const val DefaultStickerScale = 0.45f

/** Nudges a duplicated image away from its source so the copy is visibly a
 * distinct layer instead of sitting exactly on top of the original. */
private const val ImageDuplicateOffsetPx = 24f

/** How long a typing pause has to be before it's treated as its own undo
 * step — short enough to feel responsive, long enough that undo reverts a
 * whole burst of typing instead of one keystroke at a time. */
private const val TextUndoDebounceMillis = 600L

/** Caps undo stack growth for very long editing sessions. */
private const val MaxUndoDepth = 50

/** compose-screen-spec.md §10 validation bound — no upper bound: users can
 * always shrink the font size to fit more text, so length is never rejected. */
private const val MinBodyTextLength = 20

/** How long a typing pause has to be before a Mode Peny turn is considered
 * "done" and eligible for submission — draft product spec's "1.5-2 detik"
 * jeda ketik. Same debounce-since-last-keystroke pattern as
 * [TextUndoDebounceMillis], different constant since the two are unrelated. */
private const val PenyReplyDebounceMillis = 1_800L

/** Below this word count, a message is treated as still-in-progress and the
 * debounce just waits for more typing rather than submitting — unless it
 * matches Lapis 2's local responses (see [PenyLocalResponses.matchLocal]).
 * Lowered from the original spec's 4: [PenyLocalResponses.matchLocal] only
 * treats a single bare word as "ambiguous" now (2-word phrases like "kamu
 * siapa" turned out to be real questions, not ambiguous invites — see its
 * doc comment), so this threshold has to cover everything Lapis 2 no longer
 * does, or a 2-3 word message would match neither layer and get silently
 * dropped instead of ever reaching Peny. */
private const val PenyMinWordThreshold = 2

@HiltViewModel
class ComposeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val composeDraftHolder: ComposeDraftHolder,
    private val draftRepository: DraftRepository,
    private val writeExitCoordinator: WriteExitCoordinator,
    private val storageRepository: StorageRepository,
    private val replyLetterContextHolder: ReplyLetterContextHolder,
    private val catalogRepository: CatalogRepository,
    private val penyRepository: PenyRepository,
    private val penyPreferences: PenyPreferences,
    private val storeRepository: StoreRepository,
    private val analytics: AnalyticsRepository,
) : ViewModel() {

    /** Null on the `write/draft/{draftId}` Continue Editing route (it carries
     * no `WriteReplyUserIdArg`/`WriteReplyNameArg` of its own) until
     * [applyDraftEntity] backfills it from the loaded [DraftEntity] — kept as
     * a `var` (not the `val` it used to be) so a resumed reply draft still
     * records its recipient on the *next* Draft-on-Exit save instead of
     * silently losing it. */
    private var replyRecipientId = savedStateHandle.get<String>(WriteReplyUserIdArg)
    private var replyRecipientName = savedStateHandle.get<String>(WriteReplyNameArg)

    /** Null until either the first Draft-on-Exit save creates a fresh row, or
     * this VM was opened via the `write/draft/{draftId}` Continue Editing
     * route — reused for every subsequent save in this VM's lifetime so
     * repeat saves update the same row instead of creating duplicates. */
    private var currentDraftId: String? = savedStateHandle.get<String>(WriteDraftIdArg)
    private var lastInsertedStickerId: String? = null

    private val _uiState = MutableStateFlow(ComposeUiState())
    val uiState: StateFlow<ComposeUiState> = _uiState.asStateFlow()

    private val _stickerCatalog = MutableStateFlow(localStickerCatalog(context))
    val stickerCatalog: StateFlow<List<CatalogItemDto>> = _stickerCatalog.asStateFlow()

    private val _creatorPapers = MutableStateFlow<List<CatalogItemDto>>(emptyList())
    val creatorPapers: StateFlow<List<CatalogItemDto>> = _creatorPapers.asStateFlow()

    // Draft-on-Exit (docs/todo.md #5): registers this VM's current content
    // with the singleton coordinator so MainActivity's bottom-nav tab-tap
    // handler (a different composition scope) can decide whether to show the
    // exit dialog — see WriteExitCoordinator's doc comment.
    private val writeExitSession = WriteExitCoordinator.Session(
        hasUnsavedContent = { hasMeaningfulContent(_uiState.value) },
        snapshot = ::buildDraftEntity,
    )

    init {
        writeExitCoordinator.register(writeExitSession)
        // One-shot hand-off from PenPalsViewModel.prepareReplyLetter: must be
        // cleared right after being read here, otherwise it leaks into the
        // next ComposeViewModel instance (e.g. "Write New Letter" after Send
        // pops the backstack via popUpTo(0), which recreates this VM) and
        // shows a stale "membalas ke ..." sticky banner on an unrelated
        // fresh letter.
        val activeReply = replyLetterContextHolder.activeReplyLetter.value
        replyLetterContextHolder.clear()
        val resolvedReplyName = replyRecipientName ?: activeReply?.senderName
        if (resolvedReplyName != null || activeReply != null) {
            _uiState.update {
                it.copy(
                    replyRecipientName = resolvedReplyName,
                    replyLetter = activeReply,
                )
            }
        }
        analytics.logEvent(
            "compose_opened",
            bundleOf(
                "is_reply" to (resolvedReplyName != null || activeReply != null),
                "is_draft" to (currentDraftId != null),
            ),
        )
        currentDraftId?.let { id ->
            viewModelScope.launch {
                draftRepository.getById(id)?.let { entity -> applyDraftEntity(entity) }
            }
        }
        loadCollectibles(forceRefresh = true)
        // Mode Peny gating (peny_mode_mobile_integration.md §2 step 1-2):
        // used to be `is_enabled` (webadmin config) AND isPremium (`GET
        // /auth/me`) collapsed into one client-side boolean — the backend
        // dropped subscription gating for Mode Peny entirely (Energy spent
        // per-reply, handled by PenyRepository/InsufficientEnergy, is the
        // only cost now), so `is_enabled` on its own is the whole gate.
        viewModelScope.launch {
            val statusEnabled = when (val result = penyRepository.getStatus()) {
                is AuthResult.Success -> result.data.isEnabled
                is AuthResult.Error -> false // fail-closed: network error hides the trigger, not a crash
            }
            _uiState.update { it.copy(isPenyModeAvailable = statusEnabled) }
        }
        viewModelScope.launch {
            penyPreferences.hasTriedPenyMode.collect { seen ->
                _uiState.update { it.copy(hasTriedPenyMode = seen) }
            }
        }
    }

    fun onOriginalLetterSheetToggle(open: Boolean) {
        _uiState.update { it.copy(isOriginalLetterSheetOpen = open) }
    }

    override fun onCleared() {
        writeExitCoordinator.unregister(writeExitSession)
        super.onCleared()
    }

    private fun hasMeaningfulContent(state: ComposeUiState): Boolean =
        state.bodyText.isNotBlank() || state.dearName.isNotBlank()

    /** Builds the text-only [DraftEntity] snapshot saved by the Draft-on-Exit
     * dialog — images/annotate strokes/stickers are deliberately excluded
     * (compose-screen-spec.md §11 "Scope v1"). Generates a fresh id on first
     * call if this letter never had one. */
    private fun buildDraftEntity(): DraftEntity {
        val id = currentDraftId ?: UUID.randomUUID().toString().also { currentDraftId = it }
        val state = _uiState.value
        val now = System.currentTimeMillis()
        return DraftEntity(
            draftId = id,
            recipientId = replyRecipientId,
            recipientName = replyRecipientName,
            dearName = state.dearName,
            bodyText = state.bodyText,
            bodyStyleRunsJson = draftRepository.encodeStyleRuns(state.bodyStyleRuns),
            signOff = state.signOff,
            selectedFont = state.selectedFont.name,
            selectedGoogleFontName = state.selectedGoogleFontName,
            fontSize = state.fontSize,
            textAlignment = state.textAlignment.toDraftStorageName(),
            inkColor = state.inkColor.toArgb().toLong(),
            paperColor = state.paperColor.name,
            selectedPaperTemplate = state.selectedPaperTemplate?.name,
            visibility = state.visibility.name,
            crisisTag = state.crisisTag.name,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun applyDraftEntity(entity: DraftEntity) {
        // Continue Editing (write/draft/{draftId}) carries no
        // WriteReplyUserIdArg/WriteReplyNameArg of its own — restore both
        // from the draft row itself so a reply-in-progress draft doesn't (a)
        // resume looking like a blank fresh letter (StickyReplyBanner needs
        // replyRecipientName) and (b) lose its recipientId on the *next*
        // Draft-on-Exit save (buildDraftEntity reads replyRecipientId).
        replyRecipientId = replyRecipientId ?: entity.recipientId
        replyRecipientName = replyRecipientName ?: entity.recipientName
        _uiState.update {
            it.copy(
                replyRecipientName = replyRecipientName,
                dearName = entity.dearName,
                bodyText = entity.bodyText,
                bodyStyleRuns = draftRepository.decodeStyleRuns(entity.bodyStyleRunsJson),
                signOff = entity.signOff,
                selectedFont = LetterFont.valueOf(entity.selectedFont),
                selectedGoogleFontName = entity.selectedGoogleFontName,
                fontSize = entity.fontSize,
                textAlignment = entity.textAlignment.toTextAlignFromDraft(),
                inkColor = Color(entity.inkColor.toInt()),
                paperColor = PaperColor.valueOf(entity.paperColor),
                selectedPaperTemplate = entity.selectedPaperTemplate?.let(PaperTemplate::valueOf),
                visibility = LetterVisibility.valueOf(entity.visibility),
                crisisTag = CrisisTag.valueOf(entity.crisisTag),
            )
        }
    }

    // Undo/Redo history (compose-screen-spec.md §1): kept as private
    // ViewModel-only buffers rather than in ComposeUiState so the
    // UI-facing state stays lean — only the derived canUndo*/canRedo*
    // booleans are exposed. Text and Annotate stacks are independent; the
    // toolbar's single Undo/Redo pair picks whichever is relevant for the
    // current mode (see ComposeUiState.canUndo/canRedo).
    private data class TextSnapshot(val text: String, val runs: List<StyleRun>)

    private val textUndoStack = ArrayDeque<TextSnapshot>()
    private val textRedoStack = ArrayDeque<TextSnapshot>()
    private var lastCommittedText = TextSnapshot("", emptyList())
    private var textSnapshotJob: Job? = null

    private val annotateRedoStack = ArrayDeque<AnnotatePath>()

    private var penyDebounceJob: Job? = null

    /** Called right before navigating to Send. On devices that support
     * compositing (see `MinCompositingDensity` in `LetterCompositor.kt`),
     * every letter is flattened to a single WebP and uploaded first — see
     * docs/be/letters_api.md's compositing rationale and `LetterCompositor.kt`.
     * [compositeBytes] is captured by `ComposeScreen` from the live
     * `LetterCanvas` (via `GraphicsLayer.toCompositeBytes()`) immediately
     * before calling this, since the canvas can only be captured while still
     * composed on screen — pass `null` on devices below that density
     * threshold (`ComposeScreen` skips the capture+upload round trip
     * entirely for them; the draft is saved with `compositeImageUrl = null`
     * and `SelectRecipientViewModel` sends a plain-text letter instead).
     *
     * Runs client-side validation first (compose-screen-spec.md §10, never
     * previously implemented) so an invalid letter fails fast instead of
     * paying for a presign+upload round trip. Returns `true` only once
     * [ComposeDraftHolder] is ready for `SelectRecipientViewModel` to read
     * (see `core/data/ComposeDraftHolder.kt`). */
    suspend fun compositeAndSaveDraftForSend(compositeBytes: ByteArray?): Boolean {
        val state = _uiState.value
        val validationError = validationErrorFor(state)
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return false
        }

        _uiState.update { it.copy(isSending = true, error = null) }

        val compositeImageUrl = if (compositeBytes != null) {
            val fileName = "letter_${UUID.randomUUID()}.webp"
            val presign = when (val result = storageRepository.presign(fileName, "image/webp")) {
                is AuthResult.Success -> result.data
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isSending = false, error = result.message) }
                    return false
                }
            }

            val upload = storageRepository.uploadToPresignedUrl(presign.uploadUrl, compositeBytes, "image/webp")
            if (upload is AuthResult.Error) {
                _uiState.update { it.copy(isSending = false, error = upload.message) }
                return false
            }
            presign.publicUrl
        } else {
            null
        }

        composeDraftHolder.set(
            ComposeDraft(
                bodyText = state.bodyText,
                paperTemplate = (state.selectedPaperTemplate ?: PaperTemplate.PLAIN_WHITE).name,
                fontId = state.selectedGoogleFontName ?: state.selectedFont.name,
                paperColor = state.paperColor.name,
                draftId = currentDraftId,
                compositeImageUrl = compositeImageUrl,
                isPublicShowcase = state.isPublicShowcase,
                paperUrl = state.selectedPaperUrl,
                hasBodyStickers = state.images.any { it.uri.scheme != "content" && it.uri.scheme != "file" } || lastInsertedStickerId != null,
                bodyStickerId = lastInsertedStickerId,
                hasPhoto = state.images.any { it.uri.scheme == "content" || it.uri.scheme == "file" },
                hasGalleryImage = state.images.any { it.uri.scheme == "content" || it.uri.scheme == "file" },
                galleryImageCount = state.images.count { it.uri.scheme == "content" || it.uri.scheme == "file" },
            ),
        )
        // ComposeViewModel survives the Send -> SelectRecipient -> Back round
        // trip (Write's back-stack entry is pushed-not-popped), so without
        // this reset a stale `true` here makes FormattingBar reappear the
        // instant the user comes back, racing the IME's own settle animation
        // (see ComposeScreen.onSendClick, which also clears focus/hides the
        // real keyboard before navigating).
        _uiState.update { it.copy(isSending = false, isKeyboardVisible = false) }
        return true
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun validationErrorFor(state: ComposeUiState): String? = when {
        state.bodyText.length < MinBodyTextLength -> context.getString(R.string.compose_validation_body_too_short)
        else -> null
    }

    /** [value]'s [TextFieldValue.text] is always plain — [LetterCanvas]'s
     * `BasicTextField` renders [ComposeUiState.bodyAnnotated] but reports
     * edits back through this same String+selection shape either way. When
     * only the selection moved (no text edit, e.g. tapping elsewhere or
     * arrow keys) this just updates [ComposeUiState.bodySelection] and clears
     * any pending toggle rather than touching [ComposeUiState.bodyStyleRuns]. */
    fun onBodyTextChange(value: TextFieldValue) {
        val state = _uiState.value
        val oldText = state.bodyText
        val newText = value.text

        if (newText == oldText) {
            _uiState.update {
                it.copy(
                    bodySelection = value.selection,
                    pendingBold = null,
                    pendingItalic = null,
                    pendingFontSize = null,
                )
            }
            return
        }

        val newRuns = state.bodyStyleRuns.afterTextChange(oldText, newText) { previousRun ->
            StyleRun(
                start = 0,
                end = 0,
                bold = state.pendingBold ?: previousRun?.bold ?: false,
                italic = state.pendingItalic ?: previousRun?.italic ?: false,
                fontSize = state.pendingFontSize ?: previousRun?.fontSize ?: state.fontSize,
                indentLevel = previousRun?.indentLevel ?: 0,
            )
        }

        _uiState.update {
            it.copy(
                bodyText = newText,
                bodyStyleRuns = newRuns,
                bodySelection = value.selection,
                canRedoText = false,
                error = if (it.error != null && newText.length >= MinBodyTextLength) null else it.error,
            )
        }
        textRedoStack.clear()

        textSnapshotJob?.cancel()
        textSnapshotJob = viewModelScope.launch {
            delay(TextUndoDebounceMillis)
            if (lastCommittedText.text != newText) {
                textUndoStack.addLast(lastCommittedText)
                if (textUndoStack.size > MaxUndoDepth) textUndoStack.removeFirst()
                lastCommittedText = TextSnapshot(newText, newRuns)
                _uiState.update { it.copy(canUndoText = true) }
            }
        }
    }

    fun onUndoClick() {
        if (_uiState.value.isAnnotateMode) undoAnnotate() else undoText()
    }

    fun onRedoClick() {
        if (_uiState.value.isAnnotateMode) redoAnnotate() else redoText()
    }

    private fun undoText() {
        val previous = textUndoStack.removeLastOrNull() ?: return
        textSnapshotJob?.cancel()
        val current = _uiState.value
        textRedoStack.addLast(TextSnapshot(current.bodyText, current.bodyStyleRuns))
        lastCommittedText = previous
        _uiState.update {
            it.copy(
                bodyText = previous.text,
                bodyStyleRuns = previous.runs,
                bodySelection = TextRange(previous.text.length),
                pendingBold = null,
                pendingItalic = null,
                pendingFontSize = null,
                canUndoText = textUndoStack.isNotEmpty(),
                canRedoText = true,
            )
        }
    }

    private fun redoText() {
        val next = textRedoStack.removeLastOrNull() ?: return
        textSnapshotJob?.cancel()
        val current = _uiState.value
        textUndoStack.addLast(TextSnapshot(current.bodyText, current.bodyStyleRuns))
        lastCommittedText = next
        _uiState.update {
            it.copy(
                bodyText = next.text,
                bodyStyleRuns = next.runs,
                bodySelection = TextRange(next.text.length),
                pendingBold = null,
                pendingItalic = null,
                pendingFontSize = null,
                canUndoText = true,
                canRedoText = textRedoStack.isNotEmpty(),
            )
        }
    }

    private fun undoAnnotate() {
        val paths = _uiState.value.annotateState.paths
        val last = paths.lastOrNull() ?: return
        annotateRedoStack.addLast(last)
        val remaining = paths.dropLast(1)
        _uiState.update {
            it.copy(
                annotateState = it.annotateState.copy(paths = remaining),
                canUndoAnnotate = remaining.isNotEmpty(),
                canRedoAnnotate = true,
            )
        }
    }

    private fun redoAnnotate() {
        val path = annotateRedoStack.removeLastOrNull() ?: return
        _uiState.update {
            it.copy(
                annotateState = it.annotateState.copy(paths = it.annotateState.paths + path),
                canUndoAnnotate = true,
                canRedoAnnotate = annotateRedoStack.isNotEmpty(),
            )
        }
    }

    fun onTextAlignmentChange(alignment: TextAlign) {
        _uiState.update { it.copy(textAlignment = alignment) }
    }

    fun onKeyboardVisibilityChanged(visible: Boolean) {
        _uiState.update { it.copy(isKeyboardVisible = visible) }
    }

    fun onToggleAnnotateMode() {
        _uiState.update { current ->
            val next = !current.isAnnotateMode
            current.copy(
                isAnnotateMode = next,
                isKeyboardVisible = if (next) false else current.isKeyboardVisible,
                selectedImageId = if (next) null else current.selectedImageId,
                croppingImageId = if (next) null else current.croppingImageId,
            )
        }
    }

    fun onFontPickerOpenChange(open: Boolean) {
        _uiState.update { it.copy(isFontPickerOpen = open) }
    }

    fun onPaperPickerOpenChange(open: Boolean) {
        if (open) {
            loadCollectibles()
            analytics.logEvent("paper_picker_opened")
        }
        _uiState.update { it.copy(isPaperPickerOpen = open) }
    }

    fun onStickerPickerOpenChange(open: Boolean) {
        _uiState.update { it.copy(isStickerPickerOpen = open) }
        if (open) {
            loadCollectibles(forceRefresh = true)
            analytics.logEvent("sticker_picker_opened", bundleOf("screen" to "compose"))
        }
    }

    fun loadCollectibles(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            coroutineScope {
                val stickersDeferred = async { catalogRepository.getStickers(forceRefresh) }
                val inventoryDeferred = async { storeRepository.getMyInventory() }

                val stickersResult = stickersDeferred.await()
                val inventoryResult = inventoryDeferred.await()

                val ownedItems = (inventoryResult as? AuthResult.Success)?.data.orEmpty()

                // 1. Stickers
                val baseStickers = (stickersResult as? AuthResult.Success)?.data.orEmpty()
                    .ifEmpty { localStickerCatalog(context) }
                val creatorStickers = ownedItems.flatMap { ownedItem ->
                    val assets = ownedItem.assets.filter { it.assetType.equals("STICKER", ignoreCase = true) }
                    assets.mapIndexed { index, asset ->
                        CatalogItemDto(
                            id = asset.assetUrl,
                            name = if (assets.size > 1) "${ownedItem.title} #${index + 1}" else ownedItem.title,
                            imageUrl = asset.assetUrl,
                            isPremium = true,
                            orientation = "portrait",
                            creatorName = ownedItem.creatorName,
                        )
                    }
                }
                _stickerCatalog.value = (baseStickers + creatorStickers).distinctBy { it.id }

                // 2. Creator Papers
                val creatorPapers = ownedItems.flatMap { ownedItem ->
                    val assets = ownedItem.assets.filter { it.assetType.equals("PAPER", ignoreCase = true) }
                    assets.mapIndexed { index, asset ->
                        CatalogItemDto(
                            id = asset.assetUrl,
                            name = if (assets.size > 1) "${ownedItem.title} #${index + 1}" else ownedItem.title,
                            imageUrl = asset.assetUrl,
                            isPremium = true,
                            orientation = "portrait",
                            creatorName = ownedItem.creatorName,
                        )
                    }
                }
                _creatorPapers.value = creatorPapers.distinctBy { it.id }
            }
        }
    }

    fun onStickerInsert(sticker: CatalogItemDto) {
        lastInsertedStickerId = sticker.id
        analytics.logEvent(
            "collectible_applied",
            bundleOf(
                "item_type" to "sticker",
                "item_id" to sticker.id,
                "category" to if (sticker.creatorName != null) "creator" else "classic",
                "screen" to "compose",
            ),
        )
        onImageInsert(Uri.parse(sticker.imageUrl), initialScale = DefaultStickerScale)
        onStickerPickerOpenChange(false)
    }

    /** Toggles the "show in Postal Collection" opt-in. The caller (ComposeScreen)
     * only invokes this directly to turn it *off*; turning it *on* goes through
     * a confirmation dialog first, since it makes this letter's envelope/paper
     * (with body text baked in) visible to anyone viewing the sender's profile. */
    fun setPublicShowcase(enabled: Boolean) {
        _uiState.update { it.copy(isPublicShowcase = enabled) }
    }

    fun onPaperTemplateSelect(template: PaperTemplate) {
        analytics.logEvent(
            "collectible_applied",
            bundleOf(
                "item_type" to "paper",
                "item_id" to template.name,
                "category" to "classic",
            ),
        )
        _uiState.update { it.copy(selectedPaperTemplate = template, selectedPaperUrl = null) }
    }

    fun onPaperUrlSelect(paperUrl: String) {
        analytics.logEvent(
            "collectible_applied",
            bundleOf(
                "item_type" to "paper",
                "item_id" to paperUrl,
                "category" to "creator",
            ),
        )
        _uiState.update { it.copy(selectedPaperUrl = paperUrl, selectedPaperTemplate = null) }
    }

    fun onFindMoreCollectiblesClick(itemType: String, source: String = "unknown") {
        val params = bundleOf(
            "item_type" to itemType,
            "source" to source,
            "screen" to "compose",
        )
        analytics.logEvent("collectibles_find_more_clicked", params)
        analytics.logEvent("find_more_clicked", params)
    }

    /** New images always drop in already-selected (compose-screen-spec.md
     * §8's "layer draggable") so the user lands straight in edit mode —
     * handles visible, ready to move/resize/rotate — instead of an inert
     * image they'd have no way to interact with. */
    fun onImageInsert(uri: Uri, initialScale: Float = DefaultPhotoScale) {
        val id = UUID.randomUUID().toString()
        _uiState.update {
            it.copy(
                images = it.images + ImageInstance(id = id, uri = uri, scale = initialScale),
                selectedImageId = id,
            )
        }
    }

    /** Selecting a different image (or nothing) always exits crop mode for
     * whichever image was being cropped — crop mode only makes sense for the
     * currently-selected image, so switching away implicitly cancels it. */
    fun onImageSelect(id: String?) {
        _uiState.update { current ->
            current.copy(
                selectedImageId = id,
                croppingImageId = current.croppingImageId.takeIf { it == id },
            )
        }
    }

    /** Reported by `LetterCanvas`'s outer paper `Box` on every layout pass —
     * lets [onImageTransform] clamp an image's position to the actual canvas
     * size instead of letting it drag off indefinitely. */
    fun onCanvasSizeChanged(widthPx: Float, heightPx: Float) {
        _uiState.update {
            if (it.canvasWidthPx == widthPx && it.canvasHeightPx == heightPx) {
                it
            } else {
                it.copy(canvasWidthPx = widthPx, canvasHeightPx = heightPx)
            }
        }
    }

    /** Reported by the scrollable viewport `Box` in `ComposeScreen` — this is
     * the stable "one page" reference height, deliberately only accepted
     * while the keyboard is closed so it doesn't drift every time the
     * keyboard opens/closes (see [ComposeUiState.basePageHeightPx]). */
    fun onPageViewportSizeChanged(heightPx: Float) {
        _uiState.update { current ->
            if (current.isKeyboardVisible || current.basePageHeightPx == heightPx) {
                current
            } else {
                current.copy(basePageHeightPx = heightPx)
            }
        }
    }

    /** Applies a live gesture delta from [EditableImage]'s pointer input.
     * [panDelta] is in parent (pre-rotation/-scale) px space — see
     * [ImageInstance] — so it adds directly onto the stored offset.
     *
     * The resulting offset is clamped so the image's *center* can never
     * leave the canvas — the image itself can still hang partway off an
     * edge, but dragging can't send it drifting off the letter entirely with
     * no way to get it back. Skipped (no clamp) until the canvas has
     * reported a real size.
     *
     * The vertical bound uses [ComposeUiState.basePageHeightPx], not
     * [ComposeUiState.canvasHeightPx] — the canvas can grow taller than one
     * page as the user types, but images are rendered anchored to the fixed
     * one-page height (see `LetterCanvas`'s images layer), so the clamp must
     * match that same fixed reference or dragging behavior and rendered
     * position would disagree. */
    fun onImageTransform(id: String, panDelta: Offset, zoomDelta: Float, rotationDeltaDegrees: Float) {
        _uiState.update { current ->
            val halfCanvasWidth = current.canvasWidthPx / 2f
            val halfCanvasHeight = current.basePageHeightPx / 2f
            current.copy(
                images = current.images.map { image ->
                    if (image.id != id) return@map image
                    val rawOffsetX = image.offsetX + panDelta.x
                    val rawOffsetY = image.offsetY + panDelta.y
                    image.copy(
                        offsetX = if (halfCanvasWidth > 0f) rawOffsetX.coerceIn(-halfCanvasWidth, halfCanvasWidth) else rawOffsetX,
                        offsetY = if (halfCanvasHeight > 0f) rawOffsetY.coerceIn(-halfCanvasHeight, halfCanvasHeight) else rawOffsetY,
                        scale = (image.scale * zoomDelta).coerceIn(ImageMinScale, ImageMaxScale),
                        rotation = image.rotation + rotationDeltaDegrees,
                    )
                },
            )
        }
    }

    fun onGalleryImageInsert(uri: Uri) {
        analytics.logEvent(
            "gallery_image_inserted",
            bundleOf(
                "screen" to "compose",
                "source" to "gallery",
            ),
        )
        onImageInsert(uri, initialScale = DefaultPhotoScale)
    }

    /** Swaps the photo behind an existing image instance while keeping its
     * position/size/rotation — "ganti gambar" without losing the user's
     * placement work. */
    fun onImageReplace(id: String, uri: Uri) {
        analytics.logEvent(
            "gallery_image_replaced",
            bundleOf(
                "screen" to "compose",
                "source" to "gallery",
            ),
        )
        _uiState.update { current ->
            current.copy(images = current.images.map { if (it.id == id) it.copy(uri = uri) else it })
        }
    }

    fun onImageDelete(id: String) {
        _uiState.update { current ->
            current.copy(
                images = current.images.filterNot { it.id == id },
                selectedImageId = current.selectedImageId.takeUnless { it == id },
                croppingImageId = current.croppingImageId.takeUnless { it == id },
            )
        }
    }

    /** Resets the scale of [id] back to 1.0f (or default) without affecting position/rotation. */
    fun onImageResetScale(id: String) {
        _uiState.update { current ->
            current.copy(
                images = current.images.map { if (it.id == id) it.copy(scale = 1f) else it },
            )
        }
    }

    /** Inserts a copy of [id] directly above it in z-order (list order),
     * nudged so it reads as a distinct new layer, and selects the copy. */
    fun onImageDuplicate(id: String) {
        _uiState.update { current ->
            val index = current.images.indexOfFirst { it.id == id }
            if (index == -1) return@update current
            val copy = current.images[index].copy(
                id = UUID.randomUUID().toString(),
                offsetX = current.images[index].offsetX + ImageDuplicateOffsetPx,
                offsetY = current.images[index].offsetY + ImageDuplicateOffsetPx,
            )
            current.copy(
                images = current.images.toMutableList().apply { add(index + 1, copy) },
                selectedImageId = copy.id,
                croppingImageId = null,
            )
        }
    }

    /** Z-order is implicit in [ComposeUiState.images]'s list order (drawn —
     * and hit-tested — first-to-last, so the last entry is frontmost); these
     * just move one entry to either end of that list. */
    fun onImageSendToBack(id: String) {
        _uiState.update { current ->
            val image = current.images.find { it.id == id } ?: return@update current
            current.copy(images = listOf(image) + current.images.filterNot { it.id == id })
        }
    }

    fun onImageBringToFront(id: String) {
        _uiState.update { current ->
            val image = current.images.find { it.id == id } ?: return@update current
            current.copy(images = current.images.filterNot { it.id == id } + image)
        }
    }

    fun onImageCropModeToggle(id: String?) {
        _uiState.update { it.copy(croppingImageId = id) }
    }

    fun onImageCropChange(id: String, cropRect: Rect) {
        _uiState.update { current ->
            current.copy(images = current.images.map { if (it.id == id) it.copy(cropRect = cropRect) else it })
        }
    }

    /** [EditableImage] reports its natural (uncropped) rendered size once
     * Coil finishes laying out the image, so cropping — which needs a fixed
     * pixel scale to crop against — has something to crop relative to even
     * before the user has ever opened crop mode. */
    fun onImageBaseSizeMeasured(id: String, widthPx: Float, heightPx: Float) {
        _uiState.update { current ->
            current.copy(
                images = current.images.map { image ->
                    if (image.id == id && (image.baseWidthPx != widthPx || image.baseHeightPx != heightPx)) {
                        image.copy(baseWidthPx = widthPx, baseHeightPx = heightPx)
                    } else {
                        image
                    }
                },
            )
        }
    }

    fun onFontSelect(font: LetterFont) {
        _uiState.update { it.copy(selectedFont = font, selectedGoogleFontName = null) }
    }

    fun onGoogleFontDownloadStart(name: String) {
        _uiState.update { it.copy(googleFontStatuses = it.googleFontStatuses + (name to FontDownloadStatus.DOWNLOADING)) }
    }

    fun onGoogleFontDownloaded(name: String) {
        _uiState.update {
            it.copy(
                googleFontStatuses = it.googleFontStatuses + (name to FontDownloadStatus.DOWNLOADED),
                selectedGoogleFontName = name,
            )
        }
    }

    fun onGoogleFontDownloadFailed(name: String) {
        _uiState.update { it.copy(googleFontStatuses = it.googleFontStatuses + (name to FontDownloadStatus.FAILED)) }
    }

    fun onGoogleFontSelect(name: String) {
        _uiState.update { it.copy(selectedGoogleFontName = name) }
    }

    /** Selection-aware, unlike the old whole-letter toggle: with an active
     * selection this restyles only that range; with just a cursor (nothing
     * highlighted) it sets [ComposeUiState.pendingFontSize] instead, so it
     * applies going forward as the user types rather than to the rest of
     * the letter. Same split for [onBoldToggle]/[onItalicToggle]/[onIndentClick]. */
    fun onFontSizeChange(size: Float) {
        val clamped = size.coerceIn(ComposeFontSizeMin, ComposeFontSizeMax)
        val state = _uiState.value
        val selection = state.bodySelection
        if (!selection.collapsed) {
            val newRuns = state.bodyStyleRuns.applyOverride(selection.min, selection.max) { it.copy(fontSize = clamped) }
            _uiState.update { it.copy(bodyStyleRuns = newRuns) }
        } else {
            _uiState.update { it.copy(pendingFontSize = clamped) }
        }
    }

    fun onInkColorChange(color: Color) {
        _uiState.update { it.copy(inkColor = color) }
    }

    fun onBoldToggle() {
        val state = _uiState.value
        val selection = state.bodySelection
        if (!selection.collapsed) {
            val allBold = state.bodyStyleRuns
                .filter { it.start < selection.max && it.end > selection.min }
                .all { it.bold }
            val newRuns = state.bodyStyleRuns.applyOverride(selection.min, selection.max) { it.copy(bold = !allBold) }
            _uiState.update { it.copy(bodyStyleRuns = newRuns) }
        } else {
            _uiState.update { it.copy(pendingBold = !state.effectiveBold) }
        }
    }

    fun onItalicToggle() {
        val state = _uiState.value
        val selection = state.bodySelection
        if (!selection.collapsed) {
            val allItalic = state.bodyStyleRuns
                .filter { it.start < selection.max && it.end > selection.min }
                .all { it.italic }
            val newRuns = state.bodyStyleRuns.applyOverride(selection.min, selection.max) { it.copy(italic = !allItalic) }
            _uiState.update { it.copy(bodyStyleRuns = newRuns) }
        } else {
            _uiState.update { it.copy(pendingItalic = !state.effectiveItalic) }
        }
    }

    /** Indents whichever whole paragraph(s) the selection touches (see
     * [paragraphRangesTouching] — indent is always paragraph-wide, never
     * per-character, so it can't fragment a line into two). Cycles back to 0
     * after [MaxIndentLevel] since there's only an "increase" button, no
     * separate outdent. */
    fun onIndentClick() {
        val state = _uiState.value
        val paragraphs = paragraphRangesTouching(state.bodyText, state.bodySelection)
        val firstParagraph = paragraphs.firstOrNull() ?: return
        val currentLevel = state.bodyStyleRuns.runAt(firstParagraph.start)?.indentLevel
            ?: state.bodyStyleRuns.runAt((firstParagraph.start - 1).coerceAtLeast(0))?.indentLevel
            ?: 0
        val nextLevel = (currentLevel + 1) % (MaxIndentLevel + 1)
        var newRuns = state.bodyStyleRuns
        for (paragraph in paragraphs) {
            newRuns = newRuns.applyOverride(paragraph.start, paragraph.end) { it.copy(indentLevel = nextLevel) }
        }
        _uiState.update { it.copy(bodyStyleRuns = newRuns) }
    }

    fun onAnnotateToolSelect(tool: AnnotateTool) {
        _uiState.update { current ->
            current.copy(
                annotateState = current.annotateState.copy(
                    selectedTool = tool,
                    strokeWidth = presetStrokeWidth(tool),
                ),
            )
        }
    }

    fun onAnnotateColorSelect(color: Color) {
        _uiState.update { it.copy(annotateState = it.annotateState.copy(inkColor = color)) }
    }

    fun onAnnotatePathCommit(path: AnnotatePath) {
        annotateRedoStack.clear()
        _uiState.update {
            it.copy(
                annotateState = it.annotateState.copy(paths = it.annotateState.paths + path),
                canUndoAnnotate = true,
                canRedoAnnotate = false,
            )
        }
    }

    fun onAnnotateErase(point: Offset, radiusPx: Float) {
        _uiState.update { current ->
            val remaining = current.annotateState.paths.filterNot { isHitByEraser(it, point, radiusPx) }
            current.copy(
                annotateState = current.annotateState.copy(paths = remaining),
                canUndoAnnotate = remaining.isNotEmpty(),
            )
        }
    }

    private fun presetStrokeWidth(tool: AnnotateTool): Float = when (tool) {
        AnnotateTool.PEN -> 6f
        AnnotateTool.FINE -> 3f
        AnnotateTool.WAVE -> 5f
        AnnotateTool.DECO -> 40f // repurposed as stamp render size, not a stroke width
        AnnotateTool.ERASER -> 6f // unused, eraser never paints a stroke
    }

    private fun isHitByEraser(path: AnnotatePath, point: Offset, radiusPx: Float): Boolean {
        val hitDistance = radiusPx + path.strokeWidth / 2f
        if (path.points.size < 2) {
            val single = path.points.firstOrNull() ?: return false
            return (point - single).getDistance() <= hitDistance
        }
        return path.points.zipWithNext().any { (a, b) -> distanceToSegment(point, a, b) <= hitDistance }
    }

    private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
        val abx = b.x - a.x
        val aby = b.y - a.y
        val lengthSq = abx * abx + aby * aby
        if (lengthSq == 0f) return (p - a).getDistance()
        val t = (((p.x - a.x) * abx + (p.y - a.y) * aby) / lengthSq).coerceIn(0f, 1f)
        return (p - Offset(a.x + t * abx, a.y + t * aby)).getDistance()
    }

    // --- Mode Peny (peny-mode-spec.md §4) ---

    /** Tap on `PenyWandHint`. `penyHistory` is dropped on both the way in
     * and the way out (not just on exit) — a session that gets reopened
     * within the same screen visit starts clean rather than resuming stale
     * history (peny-mode-spec.md §4.4, stricter than the screen-lifecycle
     * "secrets that vanish" rule). [bodyText] is never touched here — that's
     * the whole point of this being a state toggle, not a new layer.
     *
     * Activating also swaps the paper to [PaperTemplate.INDIGO] and the ink
     * to white as a visual "wizard mode" indicator — reusing the existing
     * [ComposeUiState.selectedPaperTemplate]/[ComposeUiState.inkColor]
     * fields (the same ones the paper/ink pickers write to) rather than new
     * ones, so it's a plain state flip and `LetterCanvas` just crossfades
     * it like any other paper/ink change. Deactivating deliberately does
     * NOT restore the previous paper/ink — no snapshot is taken, so the
     * letter just stays indigo/white and the user changes it back manually
     * via the normal pickers if they want to, same as any other edit. */
    fun onPenyModeToggle() {
        val current = uiState.value
        if (current.isPenyModeActive) {
            // Exiting Mode Peny ("nulis mode ai beres")
            val candidateText = current.penyHistory.lastOrNull { it.role == PenyRole.PENY }?.text
                ?: current.penyDraftInput.takeIf { it.isNotBlank() }

            if (!candidateText.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        showPenyCopyDialog = true,
                        penyTextToCopy = candidateText,
                    )
                }
                return
            }
            onDiscardPenyTextAndExit()
        } else {
            penyDebounceJob?.cancel()
            _uiState.update {
                it.copy(
                    isPenyModeActive = true,
                    penyHistory = emptyList(),
                    penyDraftInput = "",
                    penyVisibleReply = null,
                    penyPendingReplyChunks = emptyList(),
                    isPenyReplyLoading = false,
                    penyEnergyErrorMessage = null,
                    showPenyCopyDialog = false,
                    penyTextToCopy = null,
                    selectedPaperTemplate = PaperTemplate.INDIGO,
                    inkColor = Color.White,
                    // Optimistic local flip alongside the DataStore write below
                    // — hasTriedPenyMode also gets set for real once
                    // markPenyModeTried()'s write round-trips back through
                    // penyPreferences.hasTriedPenyMode's collector (init
                    // above), but PenyWandHint's discovery dot should drop
                    // the instant the user actually taps in, not a frame
                    // later once that write lands.
                    hasTriedPenyMode = true,
                )
            }
            if (current.hasTriedPenyMode != true) {
                viewModelScope.launch { penyPreferences.markPenyModeTried() }
            }
        }
    }

    /** User confirmed copying the last text from Mode Peny into the letter body. */
    fun onConfirmCopyPenyText() {
        val textToCopy = uiState.value.penyTextToCopy ?: return
        penyDebounceJob?.cancel()
        textSnapshotJob?.cancel()

        val state = _uiState.value
        val oldText = state.bodyText
        val separator = when {
            oldText.isBlank() -> ""
            oldText.endsWith("\n\n") -> ""
            oldText.endsWith("\n") -> "\n"
            else -> "\n\n"
        }
        val newText = if (oldText.isBlank()) textToCopy else "$oldText$separator$textToCopy"
        val newRuns = state.bodyStyleRuns.afterTextChange(oldText, newText) { previousRun ->
            StyleRun(
                start = 0,
                end = 0,
                bold = state.pendingBold ?: previousRun?.bold ?: false,
                italic = state.pendingItalic ?: previousRun?.italic ?: false,
                fontSize = state.pendingFontSize ?: previousRun?.fontSize ?: state.fontSize,
                indentLevel = previousRun?.indentLevel ?: 0,
            )
        }

        textUndoStack.addLast(TextSnapshot(oldText, state.bodyStyleRuns))
        if (textUndoStack.size > MaxUndoDepth) textUndoStack.removeFirst()
        lastCommittedText = TextSnapshot(newText, newRuns)
        textRedoStack.clear()

        _uiState.update {
            it.copy(
                bodyText = newText,
                bodyStyleRuns = newRuns,
                bodySelection = TextRange(newText.length),
                canUndoText = true,
                canRedoText = false,
                error = if (it.error != null && newText.length >= MinBodyTextLength) null else it.error,
                isPenyModeActive = false,
                showPenyCopyDialog = false,
                penyTextToCopy = null,
                penyHistory = emptyList(),
                penyDraftInput = "",
                penyVisibleReply = null,
                penyPendingReplyChunks = emptyList(),
                isPenyReplyLoading = false,
                penyEnergyErrorMessage = null,
            )
        }
    }

    /** User opted to exit Mode Peny without copying the text. */
    fun onDiscardPenyTextAndExit() {
        penyDebounceJob?.cancel()
        _uiState.update {
            it.copy(
                isPenyModeActive = false,
                showPenyCopyDialog = false,
                penyTextToCopy = null,
                penyHistory = emptyList(),
                penyDraftInput = "",
                penyVisibleReply = null,
                penyPendingReplyChunks = emptyList(),
                isPenyReplyLoading = false,
                penyEnergyErrorMessage = null,
            )
        }
    }

    /** User dismissed the copy dialog (tapped outside or back), stays in Mode Peny. */
    fun onCancelPenyCopyDialog() {
        _uiState.update {
            it.copy(
                showPenyCopyDialog = false,
                penyTextToCopy = null,
            )
        }
    }

    /** User resolved the "buy more Energy?" dialog (either CTA, or tapped
     * outside) — [ComposeUiState.penyEnergyErrorMessage] stays showing in the
     * reply bubble regardless, only the dialog itself closes. */
    fun onDismissPenyEnergyPaywall() {
        _uiState.update { it.copy(showPenyEnergyPaywall = false) }
    }

    /** Lapis 1 (absorb animation, always runs, zero-network) — called from
     * `LetterCanvas`'s TextField instead of [onBodyTextChange] while
     * [ComposeUiState.isPenyModeActive] is true. Purely updates the draft;
     * threshold/submission logic lives in [handlePenyTurnSubmit] below,
     * fired after [PenyReplyDebounceMillis] of no further typing. Starting a
     * new turn also clears whatever reply/error is currently showing so the
     * center reveal has room for the next one. */
    fun onPenyDraftInputChange(text: String) {
        _uiState.update { current ->
            current.copy(
                penyDraftInput = text,
                penyVisibleReply = if (text.isNotBlank()) null else current.penyVisibleReply,
                penyEnergyErrorMessage = if (text.isNotBlank()) null else current.penyEnergyErrorMessage,
            )
        }
        penyDebounceJob?.cancel()
        penyDebounceJob = viewModelScope.launch {
            delay(PenyReplyDebounceMillis)
            handlePenyTurnSubmit(text)
        }
    }

    /** Lapis 2 (local, zero-network) then Lapis 3 (AI) threshold check — see
     * peny-mode-spec.md §4.2-4.3.
     *
     * Both the ambiguous-single-word local match and the [PenyMinWordThreshold]
     * "wait for more typing" gate below are opener-only (`isFirstTurn`) — they
     * exist to disambiguate a vague *conversation starter* ("capek", "libur")
     * before ever bothering Peny/the AI with it. Once the conversation is
     * underway, a short reply is overwhelmingly a direct answer to whatever
     * Peny just asked ("iya", "boleh", "nggak") and must reach Lapis 3 as-is —
     * gating it the same way as an opener either fired an unrelated canned
     * "ambiguous invite" reply or silently dropped the message forever
     * (nothing re-triggers `handlePenyTurnSubmit` until the user types again),
     * both of which read as the app ignoring what the user just said. */
    private fun handlePenyTurnSubmit(message: String) {
        if (message.isBlank()) return
        val isFirstTurn = uiState.value.penyHistory.isEmpty()
        val local = PenyLocalResponses.matchLocal(context, message, isFirstTurn)
        if (local != null) {
            appendPenyTurn(message, local)
            return
        }
        val wordCount = message.trim().split(Regex("\\s+")).size
        if (isFirstTurn && wordCount < PenyMinWordThreshold) return // not meaningful enough yet, wait for more typing
        requestPenyReply(message)
    }

    /** Lapis 3 — the one and only network call in this whole feature
     * (peny_mode_mobile_integration.md §1). */
    private fun requestPenyReply(message: String) {
        _uiState.update { it.copy(isPenyReplyLoading = true, penyEnergyErrorMessage = null) }
        viewModelScope.launch {
            // Must NOT include this turn — the backend appends it server-side
            // once it replies (peny_mode_mobile_integration.md §2 step 4).
            val historyBeforeThisTurn = uiState.value.penyHistory
            when (val result = penyRepository.reply(historyBeforeThisTurn, message)) {
                is PenyReplyResult.Success -> appendPenyTurn(message, result.data.reply)
                PenyReplyResult.InsufficientEnergy -> {
                    // Not appended to penyHistory — the failed turn doesn't
                    // "count", so the user can retry once they have Energy
                    // again (integration doc §3). showPenyEnergyPaywall
                    // layers a proactive "buy more Energy?" dialog on top of
                    // the in-character bubble — the "jemput bola" upsell,
                    // docs/copywriting/cp.md.
                    _uiState.update {
                        it.copy(
                            penyDraftInput = "",
                            penyEnergyErrorMessage = context.resources
                                .getStringArray(R.array.compose_peny_energy_insufficient_messages)
                                .random(),
                            showPenyEnergyPaywall = true,
                        )
                    }
                }
                is PenyReplyResult.Error -> {
                    // Collapses the 400/403/500 cases from
                    // peny_mode_mobile_integration.md §3 into one soft,
                    // in-character fallback shown through the same reveal
                    // channel as a real reply — the 400 case is a
                    // should-never-happen client bug (already logged via
                    // crashlytics in PenyRepository) and 403 shouldn't occur
                    // since §6's gating already hides the trigger, so neither
                    // needs a distinct message — 500 (transient, retry-safe)
                    // is the only realistically expected case here.
                    _uiState.update {
                        it.copy(
                            penyDraftInput = "",
                            penyVisibleReply = context.resources
                                .getStringArray(R.array.compose_peny_reply_failed_messages)
                                .random(),
                        )
                    }
                }
            }
            _uiState.update { it.copy(isPenyReplyLoading = false) }
        }
    }

    private fun appendPenyTurn(userMessage: String, penyReply: String) {
        _uiState.update {
            it.copy(
                penyDraftInput = "", // "absorbed" — TextField empties again
                penyHistory = it.penyHistory + PenyTurn(PenyRole.USER, userMessage) + PenyTurn(PenyRole.PENY, penyReply),
                penyVisibleReply = penyReply,
                penyEnergyErrorMessage = null,
            )
        }
    }
}

/** [TextAlign] is a Compose value class, not a real Kotlin enum — no `.name`/
 * `valueOf()` — so [DraftEntity.textAlignment] round-trips through these
 * instead. */
private fun TextAlign.toDraftStorageName(): String = when (this) {
    TextAlign.Center -> "CENTER"
    TextAlign.End -> "END"
    TextAlign.Justify -> "JUSTIFY"
    TextAlign.Left -> "LEFT"
    TextAlign.Right -> "RIGHT"
    else -> "START"
}

private fun String.toTextAlignFromDraft(): TextAlign = when (this) {
    "CENTER" -> TextAlign.Center
    "END" -> TextAlign.End
    "JUSTIFY" -> TextAlign.Justify
    "LEFT" -> TextAlign.Left
    "RIGHT" -> TextAlign.Right
    else -> TextAlign.Start
}
