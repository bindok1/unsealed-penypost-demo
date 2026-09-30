package com.apps.unsealed.ui.screens.selectrecipient.viewmodel

import android.content.Context
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.unsealed.core.analytics.AnalyticsRepository
import com.apps.unsealed.core.data.ComposeDraft
import com.apps.unsealed.core.data.ComposeDraftHolder
import com.apps.unsealed.core.data.RecipientSelectionsDraft
import com.apps.unsealed.feature.addressbook.data.AddressBookContactDto
import com.apps.unsealed.feature.addressbook.data.AddressBookRepository
import com.apps.unsealed.feature.auth.data.AuthResult
import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.feature.catalog.data.CatalogRepository
import com.apps.unsealed.feature.draft.data.DraftRepository
import com.apps.unsealed.feature.letters.data.LettersRepository
import com.apps.unsealed.feature.letters.data.SendLetterRequest
import com.apps.unsealed.feature.penpals.data.PenpalsRepository
import com.apps.unsealed.feature.storage.data.StorageRepository
import com.apps.unsealed.feature.store.data.StoreRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import com.apps.unsealed.navigation.SelectRecipientContinentArg
import com.apps.unsealed.navigation.SelectRecipientNameArg
import com.apps.unsealed.navigation.SelectRecipientUserIdArg
import com.apps.unsealed.ui.screens.compose.state.LetterFont
import com.apps.unsealed.ui.screens.selectrecipient.constants.PenpalRegion
import com.apps.unsealed.ui.screens.selectrecipient.constants.localEnvelopeCatalog
import com.apps.unsealed.ui.screens.selectrecipient.constants.localStampCatalog
import com.apps.unsealed.ui.screens.selectrecipient.constants.localStickerCatalog
import com.apps.unsealed.ui.screens.selectrecipient.state.ComingSoonFeature
import com.apps.unsealed.ui.screens.selectrecipient.state.RecipientMode
import com.apps.unsealed.ui.screens.selectrecipient.state.SelectRecipientUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SelectRecipientViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val analytics: AnalyticsRepository,
    private val lettersRepository: LettersRepository,
    private val penpalsRepository: PenpalsRepository,
    private val addressBookRepository: AddressBookRepository,
    private val catalogRepository: CatalogRepository,
    private val composeDraftHolder: ComposeDraftHolder,
    private val draftRepository: DraftRepository,
    private val storageRepository: StorageRepository,
    private val storeRepository: StoreRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelectRecipientUiState())
    val uiState: StateFlow<SelectRecipientUiState> = _uiState.asStateFlow()

    /** Live region counts from `GET /penpals/regions` (region apiLabel → count).
     *  Empty until the call returns; [RegionPickerSheet] falls back to
     *  [PenpalRegion.dummyPenpalCount] when a key is absent. */
    private val _regionCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val regionCounts: StateFlow<Map<String, Int>> = _regionCounts.asStateFlow()

    private val _addressBookContacts = MutableStateFlow<List<AddressBookContactDto>>(emptyList())
    val addressBookContacts: StateFlow<List<AddressBookContactDto>> = _addressBookContacts.asStateFlow()

    private val _isAddressBookLoading = MutableStateFlow(false)
    val isAddressBookLoading: StateFlow<Boolean> = _isAddressBookLoading.asStateFlow()

    /** CMS-driven envelope/stamp catalogs (see `feature/catalog/data/`) —
     * [EnvelopePickerSheet]/[StampPickerOverlay] render from these instead of
     * the bundled [EnvelopeDesign]/[StampDesign] enums directly, falling back
     * to [localEnvelopeCatalog]/[localStampCatalog] (same 5/6 bundled assets,
     * reshaped) until the backend catalog is reachable — see
     * `docs/backend-roadmap.md` M4. */
    private val _envelopeCatalog = MutableStateFlow(localEnvelopeCatalog(context))
    val envelopeCatalog: StateFlow<List<CatalogItemDto>> = _envelopeCatalog.asStateFlow()

    private val _stampCatalog = MutableStateFlow(localStampCatalog(context))
    val stampCatalog: StateFlow<List<CatalogItemDto>> = _stampCatalog.asStateFlow()

    private val _stickerCatalog = MutableStateFlow(localStickerCatalog(context))
    val stickerCatalog: StateFlow<List<CatalogItemDto>> = _stickerCatalog.asStateFlow()

    init {
        val recipientId = savedStateHandle.get<String>(SelectRecipientUserIdArg)
        val recipientName = savedStateHandle.get<String>(SelectRecipientNameArg)
        val recipientContinent = savedStateHandle.get<String>(SelectRecipientContinentArg)
        val draft = composeDraftHolder.get()
        val draftFont = draft?.fontId?.let { id -> LetterFont.entries.firstOrNull { it.name == id } } ?: LetterFont.CAVEAT

        val cachedRecipientDraft = composeDraftHolder.getRecipientDraft()

        if (recipientId != null && recipientName != null) {
            _uiState.update {
                it.copy(
                    recipientMode = RecipientMode.KNOWN_USER,
                    recipientId = recipientId,
                    recipientName = recipientName,
                    recipientContinent = recipientContinent,
                    isReplyFlow = true,
                    selectedFont = draftFont,
                )
            }
        } else if (cachedRecipientDraft != null) {
            val restoredMode = try {
                RecipientMode.valueOf(cachedRecipientDraft.recipientModeName)
            } catch (_: Exception) {
                RecipientMode.PENPAL_REGION
            }
            val restoredRegion = cachedRecipientDraft.selectedRegionName?.let { name ->
                PenpalRegion.entries.firstOrNull { it.name == name }
            }
            val restoredFont = cachedRecipientDraft.selectedFontName?.let { name ->
                LetterFont.entries.firstOrNull { it.name == name }
            } ?: draftFont

            _uiState.update {
                it.copy(
                    recipientMode = restoredMode,
                    selectedRegion = restoredRegion,
                    recipientId = cachedRecipientDraft.recipientId,
                    recipientName = cachedRecipientDraft.recipientName,
                    recipientContinent = cachedRecipientDraft.recipientContinent,
                    selectedFont = restoredFont,
                )
            }
        } else {
            _uiState.update { it.copy(selectedFont = draftFont) }
        }
        val initialEnvelope = cachedRecipientDraft?.selectedEnvelopeId?.let { id ->
            _envelopeCatalog.value.firstOrNull { it.id == id }
        } ?: _envelopeCatalog.value.first()
        _uiState.update { it.copy(selectedEnvelope = initialEnvelope) }
        loadRegions()
        loadAddressBook()
        loadCollectibles()
    }

    /** Loads per-region penpal counts once on screen entry. Silently ignored
     *  on failure — the sheet shows [PenpalRegion.dummyPenpalCount] as fallback. */
    private fun loadRegions() {
        viewModelScope.launch {
            when (val result = penpalsRepository.getRegions()) {
                is AuthResult.Success -> {
                    _regionCounts.value = result.data.associate { it.region to it.count }
                }
                is AuthResult.Error -> {
                    // Non-critical — UI falls back to dummyPenpalCount gracefully.
                }
            }
        }
    }

    fun loadAddressBook() {
        viewModelScope.launch {
            _isAddressBookLoading.value = true
            when (val result = addressBookRepository.getAddressBook()) {
                is AuthResult.Success -> {
                    _addressBookContacts.value = result.data
                }
                is AuthResult.Error -> Unit
            }
            _isAddressBookLoading.value = false
        }
    }

    /**
     * Loads base CMS catalogs (envelopes, stamps, stickers) and merges them with
     * the user's purchased creator items from Peny Store (GET /api/v1/me/inventory).
     */
    fun loadCollectibles(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            coroutineScope {
                val envelopesDeferred = async { catalogRepository.getEnvelopes() }
                val stampsDeferred = async { catalogRepository.getStamps() }
                val stickersDeferred = async { catalogRepository.getStickers(forceRefresh) }
                val inventoryDeferred = async { storeRepository.getMyInventory() }

                val envelopesResult = envelopesDeferred.await()
                val stampsResult = stampsDeferred.await()
                val stickersResult = stickersDeferred.await()
                val inventoryResult = inventoryDeferred.await()

                val ownedItems = (inventoryResult as? AuthResult.Success)?.data.orEmpty()

                // 1. Envelopes
                val baseEnvelopes = (envelopesResult as? AuthResult.Success)?.data.orEmpty()
                    .ifEmpty { localEnvelopeCatalog(context) }
                val creatorEnvelopes = ownedItems.flatMap { ownedItem ->
                    val assets = ownedItem.assets.filter { it.assetType.equals("ENVELOPE", ignoreCase = true) }
                    assets.mapIndexed { index, asset ->
                        CatalogItemDto(
                            id = asset.assetUrl,
                            name = if (assets.size > 1) "${ownedItem.title} #${index + 1}" else ownedItem.title,
                            imageUrl = asset.assetUrl,
                            isPremium = true,
                            orientation = "landscape",
                            creatorName = ownedItem.creatorName,
                        )
                    }
                }
                _envelopeCatalog.value = (baseEnvelopes + creatorEnvelopes).distinctBy { it.id }

                // 2. Stamps
                val baseStamps = (stampsResult as? AuthResult.Success)?.data.orEmpty()
                    .ifEmpty { localStampCatalog(context) }
                val creatorStamps = ownedItems.flatMap { ownedItem ->
                    val assets = ownedItem.assets.filter { it.assetType.equals("STAMP", ignoreCase = true) }
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
                _stampCatalog.value = (baseStamps + creatorStamps).distinctBy { it.id }

                // 3. Stickers
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

                val cached = composeDraftHolder.getRecipientDraft()
                if (cached != null) {
                    _uiState.update { current ->
                        val restoredEnvelope = cached.selectedEnvelopeId?.let { id ->
                            _envelopeCatalog.value.firstOrNull { it.id == id }
                        } ?: current.selectedEnvelope

                        val restoredStamp = cached.selectedStampId?.let { id ->
                            _stampCatalog.value.firstOrNull { it.id == id }
                        } ?: current.selectedStamp

                        val restoredSticker = cached.selectedStickerId?.let { id ->
                            _stickerCatalog.value.firstOrNull { it.id == id }
                        } ?: current.selectedSticker

                        current.copy(
                            selectedEnvelope = restoredEnvelope,
                            selectedStamp = restoredStamp,
                            selectedSticker = restoredSticker,
                        )
                    }
                }
            }
        }
    }

    fun onFontPickerOpenChange(open: Boolean) {
        _uiState.update { it.copy(isFontPickerOpen = open) }
    }

    fun onFontSelect(font: LetterFont) {
        _uiState.update { it.copy(selectedFont = font, isFontPickerOpen = false) }
        saveRecipientDraft(_uiState.value)
    }

    fun onFindMoreCollectiblesClick(itemType: String, source: String = "unknown") {
        val params = bundleOf(
            "item_type" to itemType,
            "source" to source,
            "screen" to "select_recipient",
        )
        analytics.logEvent("collectibles_find_more_clicked", params)
        analytics.logEvent("find_more_clicked", params)
    }

    fun onShareContactClick() {
        loadAddressBook()
        _uiState.update { it.copy(isRecipientPickerOpen = true, initialPickerTab = 1) }
    }

    fun onPenpalsClick() {
        _uiState.update { it.copy(isRecipientPickerOpen = true, initialPickerTab = 0) }
    }

    fun onEditRecipientClick() {
        loadAddressBook()
        val currentTab = if (_uiState.value.recipientMode == RecipientMode.KNOWN_USER) 1 else 0
        _uiState.update { it.copy(isRecipientPickerOpen = true, initialPickerTab = currentTab) }
    }

    fun onRecipientPickerOpenChange(open: Boolean) {
        if (open) loadAddressBook()
        _uiState.update { it.copy(isRecipientPickerOpen = open) }
    }

    fun onRegionPickerOpenChange(open: Boolean) {
        onRecipientPickerOpenChange(open)
    }

    fun onRegionSelect(region: PenpalRegion) {
        _uiState.update {
            it.copy(
                recipientMode = RecipientMode.PENPAL_REGION,
                selectedRegion = region,
                selectedContact = null,
                recipientId = null,
                recipientName = null,
                isRecipientPickerOpen = false,
                // Only prompt if not already public — avoids re-asking a user
                // who already opted in via the compose-screen overflow menu.
                showPublicShowcaseDialog = composeDraftHolder.get()?.isPublicShowcase != true,
            )
        }
        saveRecipientDraft(_uiState.value)
    }

    /** User confirmed "Yes, publish" on the auto-prompted dialog — see
     * [onRegionSelect]. Flips the *same* flag the compose-screen overflow
     * menu toggle would, via [ComposeDraftHolder.setPublicShowcase], so
     * there's one source of truth regardless of which prompt set it. */
    fun onPublicShowcaseConfirm() {
        composeDraftHolder.setPublicShowcase(true)
        _uiState.update { it.copy(showPublicShowcaseDialog = false) }
    }

    fun onPublicShowcaseDismiss() {
        _uiState.update { it.copy(showPublicShowcaseDialog = false) }
    }

    fun onContactSelect(contact: AddressBookContactDto) {
        _uiState.update {
            it.copy(
                recipientMode = RecipientMode.KNOWN_USER,
                recipientId = contact.targetUserId,
                recipientName = contact.displayName,
                recipientContinent = contact.location,
                selectedContact = contact,
                selectedRegion = null,
                isRecipientPickerOpen = false,
            )
        }
        saveRecipientDraft(_uiState.value)
    }

    fun onEnvelopePickerOpenChange(open: Boolean) {
        if (open) {
            loadCollectibles()
            analytics.logEvent("envelope_picker_opened")
        }
        _uiState.update { it.copy(isEnvelopePickerOpen = open) }
    }

    fun onEnvelopeSelect(item: CatalogItemDto) {
        analytics.logEvent(
            "collectible_applied",
            bundleOf(
                "item_type" to "envelope",
                "item_id" to item.id,
                "category" to if (item.creatorName != null) "creator" else "classic",
            ),
        )
        _uiState.update { it.copy(selectedEnvelope = item) }
        saveRecipientDraft(_uiState.value)
    }

    fun onStampPickerOpenChange(open: Boolean) {
        if (open) {
            loadCollectibles()
            analytics.logEvent("stamp_picker_opened")
        }
        _uiState.update { it.copy(isStampPickerOpen = open) }
    }

    fun onStampSelect(item: CatalogItemDto) {
        analytics.logEvent(
            "collectible_applied",
            bundleOf(
                "item_type" to "stamp",
                "item_id" to item.id,
                "category" to if (item.creatorName != null) "creator" else "classic",
            ),
        )
        _uiState.update { it.copy(selectedStamp = item, isStampPickerOpen = false) }
        saveRecipientDraft(_uiState.value)
    }

    fun onStickerToggleRequest() {
        onStickerPickerOpenChange(true)
    }

    fun onStickerPickerOpenChange(open: Boolean) {
        _uiState.update { it.copy(isStickerPickerOpen = open) }
        if (open) {
            loadCollectibles(forceRefresh = true)
            analytics.logEvent("sticker_picker_opened", bundleOf("screen" to "select_recipient"))
        }
    }

    /** [item] is `null` to deselect — unlike [onStampSelect], the sticker slot
     * is optional and can be cleared, see [StickerPickerOverlay]. */
    fun onStickerSelect(item: CatalogItemDto?) {
        if (item != null) {
            analytics.logEvent(
                "collectible_applied",
                bundleOf(
                    "item_type" to "sticker",
                    "item_id" to item.id,
                    "category" to if (item.creatorName != null) "creator" else "classic",
                    "screen" to "select_recipient",
                ),
            )
        }
        _uiState.update { it.copy(selectedSticker = item, isStickerPickerOpen = false) }
        saveRecipientDraft(_uiState.value)
    }

    private fun saveRecipientDraft(state: SelectRecipientUiState) {
        composeDraftHolder.setRecipientDraft(
            RecipientSelectionsDraft(
                recipientModeName = state.recipientMode.name,
                selectedRegionName = state.selectedRegion?.name,
                selectedContactId = state.selectedContact?.targetUserId,
                recipientId = state.recipientId,
                recipientName = state.recipientName,
                recipientContinent = state.recipientContinent,
                selectedEnvelopeId = state.selectedEnvelope.id,
                selectedStampId = state.selectedStamp?.id,
                selectedStickerId = state.selectedSticker?.id,
                selectedFontName = state.selectedFont.name,
            ),
        )
    }

    fun onComingSoonRequest(feature: ComingSoonFeature) {
        _uiState.update { it.copy(comingSoonFeature = feature) }
    }

    fun onComingSoonDismiss() {
        _uiState.update { it.copy(comingSoonFeature = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(sendError = null) }
    }

    /** Presign + PUT the envelope composite (background + sticker, flattened
     * client-side), same shape as `ComposeViewModel.compositeAndSaveDraftForSend`'s
     * inline flow — kept private/local since only [onSendClick]'s two branches
     * need it (`docs/envelope-sticker-spec.md` §9/§12: the cross-file share with
     * `ComposeViewModel` is explicitly optional, deferred as a follow-up). */
    private suspend fun uploadEnvelopeComposite(bytes: ByteArray): AuthResult<String> {
        val fileName = "envelope_${UUID.randomUUID()}.webp"
        val presign = when (val result = storageRepository.presign(fileName, "image/webp")) {
            is AuthResult.Success -> result.data
            is AuthResult.Error -> return AuthResult.Error(result.message, result.cause)
        }
        val upload = storageRepository.uploadToPresignedUrl(presign.uploadUrl, bytes, "image/webp")
        if (upload is AuthResult.Error) return upload
        return AuthResult.Success(presign.publicUrl)
    }

    /**
     * [RecipientMode.KNOWN_USER] (reply flow): calls `POST /letters` with the
     * known [SelectRecipientUiState.recipientId] — success flips [isSending].
     * `visibility` is always sent as `"private"` here regardless of the
     * public-showcase toggle: a direct-addressed letter can never satisfy the
     * backend's `recipient_id == "penpal:<region>"` rule for public letters
     * (`docs/be/letters_api.md` "Public penpal posts"), so there's no valid
     * public form of a reply/address-book send.
     *
     * [RecipientMode.PENPAL_REGION] (region-picker flow): if the public-showcase
     * toggle is off, resolves the selected region to a real penpal via
     * `GET /penpals/feed?region=X&limit=1` and calls `POST /letters` with that
     * penpal's `sender_id` as `recipient_id`. If the toggle is on, skips
     * resolution entirely and sends the literal `"penpal:<region>"` form
     * instead — public posts have no specific recipient, so resolving one
     * would violate the same backend rule above. Shows
     * [SelectRecipientUiState.sendError] if no penpals are available in the
     * chosen region (private-send path only).
     *
     * [envelopeCompositeBytes] is captured by the caller (`SelectRecipientScreen`)
     * from a `GraphicsLayer` — that capture is a suspend call tied to the
     * composition, so it can't happen inside this ViewModel directly, see
     * `docs/envelope-sticker-spec.md` §9. `null` on devices below
     * `MinCompositingDensity`, where the receiver falls back to reconstructing
     * the envelope from `envelope`/`envelopeStickerId` client-side instead.
     */
    fun onSendClick(envelopeCompositeBytes: ByteArray?) {
        val state = _uiState.value
        if (!state.canSend || state.isSending) return

        if (state.recipientMode == RecipientMode.KNOWN_USER) {
            val recipientId = state.recipientId
            val draft = composeDraftHolder.get()
            val stamp = state.selectedStamp
            if (recipientId == null || draft == null || stamp == null) return

            viewModelScope.launch {
                val envelopeCompositeImageUrl = envelopeCompositeBytes?.let { bytes ->
                    when (val result = uploadEnvelopeComposite(bytes)) {
                        is AuthResult.Success -> result.data
                        is AuthResult.Error -> {
                            _uiState.update { it.copy(sendError = result.message) }
                            return@launch
                        }
                    }
                }
                val request = SendLetterRequest(
                    recipientId   = recipientId,
                    dearName      = state.recipientName.orEmpty(),
                    bodyText      = draft.bodyText,
                    envelope      = state.selectedEnvelope.id,
                    stamp         = stamp.id,
                    paperTemplate = draft.paperTemplate,
                    paperColor    = draft.paperColor,
                    fontId        = draft.fontId,
                    compositeImageUrl = draft.compositeImageUrl,
                    paperUrl      = draft.paperUrl,
                    visibility = "private",
                    envelopeStickerId = state.selectedSticker?.id,
                    envelopeCompositeImageUrl = envelopeCompositeImageUrl,
                )
                when (val result = lettersRepository.sendLetter(request)) {
                    is AuthResult.Success -> {
                        // Sent from a saved draft — drop the row so it doesn't
                        // linger as both "sent" and "draft" (docs/todo.md #5).
                        draft.draftId?.let { id -> viewModelScope.launch { draftRepository.delete(id) } }
                        composeDraftHolder.clear()
                        analytics.logEvent(
                            "letter_sent",
                            bundleOf(
                                "recipient_type" to "direct",
                                "visibility" to "private",
                            ),
                        )
                        logLetterCustomized(draft, state, stamp)
                        _uiState.update {
                            it.copy(
                                isSending = true,
                                sendError = null,
                                sentEstimatedArrivalAt = result.data.estimatedArrivalAt,
                                sentLetterId = result.data.id,
                                sentVisibility = request.visibility,
                                sentCompositeImageUrl = request.compositeImageUrl,
                            )
                        }
                    }
                    is AuthResult.Error -> _uiState.update { it.copy(sendError = result.message) }
                }
            }

        } else if (state.recipientMode == RecipientMode.PENPAL_REGION) {
            val region = state.selectedRegion ?: return
            val draft  = composeDraftHolder.get()
            val stamp  = state.selectedStamp
            if (draft == null || stamp == null) return

            viewModelScope.launch {
                // Public showcase posts go to the region's open feed — no
                // specific recipient, so the backend requires the literal
                // "penpal:<region>" form instead of a resolved UID
                // (docs/be/letters_api.md "Public penpal posts"); resolving
                // an actual penpal in this case would just get rejected.
                val (resolvedRecipientId, resolvedRecipientName) = if (draft.isPublicShowcase) {
                    "penpal:${region.apiLabel}" to ""
                } else {
                    // dear_name is required by the backend for private letters
                    // (ValidateSendRequest) — both branches below must resolve a
                    // name alongside the id, not just the id.
                    when (val matchResult = penpalsRepository.matchPenpal(region.apiLabel)) {
                        is AuthResult.Success -> matchResult.data.recipientId to matchResult.data.recipientName
                        is AuthResult.Error -> {
                            val feedResult = penpalsRepository.getFeed(region = region.apiLabel, limit = 1)
                            val recipient = (feedResult as? AuthResult.Success)?.data?.items?.firstOrNull()
                            if (recipient == null) {
                                _uiState.update { it.copy(sendError = matchResult.message) }
                                return@launch
                            }
                            recipient.senderId to recipient.senderName.orEmpty()
                        }
                    }
                }

                val envelopeCompositeImageUrl = envelopeCompositeBytes?.let { bytes ->
                    when (val result = uploadEnvelopeComposite(bytes)) {
                        is AuthResult.Success -> result.data
                        is AuthResult.Error -> {
                            _uiState.update { it.copy(sendError = result.message) }
                            return@launch
                        }
                    }
                }
                val request = SendLetterRequest(
                    recipientId   = resolvedRecipientId,
                    dearName      = resolvedRecipientName,
                    bodyText      = draft.bodyText,
                    envelope      = state.selectedEnvelope.id,
                    stamp         = stamp.id,
                    paperTemplate = draft.paperTemplate,
                    paperColor    = draft.paperColor,
                    fontId        = draft.fontId,
                    compositeImageUrl = draft.compositeImageUrl,
                    paperUrl      = draft.paperUrl,
                    visibility = if (draft.isPublicShowcase) "public" else "private",
                    envelopeStickerId = state.selectedSticker?.id,
                    envelopeCompositeImageUrl = envelopeCompositeImageUrl,
                )
                when (val result = lettersRepository.sendLetter(request)) {
                    is AuthResult.Success -> {
                        draft.draftId?.let { id -> viewModelScope.launch { draftRepository.delete(id) } }
                        composeDraftHolder.clear()
                        analytics.logEvent(
                            "letter_sent",
                            bundleOf(
                                "recipient_type" to "penpal_region",
                                "visibility" to request.visibility,
                                "region" to region.apiLabel,
                            ),
                        )
                        logLetterCustomized(draft, state, stamp)
                        _uiState.update {
                            it.copy(
                                isSending = true,
                                sendError = null,
                                sentEstimatedArrivalAt = result.data.estimatedArrivalAt,
                                sentLetterId = result.data.id,
                                sentVisibility = request.visibility,
                                sentCompositeImageUrl = request.compositeImageUrl,
                            )
                        }
                    }
                    is AuthResult.Error -> _uiState.update { it.copy(sendError = result.message) }
                }
            }
        }
    }

    private fun logLetterCustomized(
        draft: ComposeDraft,
        state: SelectRecipientUiState,
        stamp: CatalogItemDto,
    ) {
        val paperId = draft.paperUrl ?: draft.paperTemplate
        val stickerId = state.selectedSticker?.id ?: draft.bodyStickerId ?: "none"
        val envelopeId = state.selectedEnvelope.id
        val stampId = stamp.id
        val hasCustomPaper = draft.paperTemplate != "PLAIN_WHITE" || draft.paperUrl != null
        val hasSticker = state.selectedSticker != null || draft.hasBodyStickers
        val hasGalleryImage = draft.hasGalleryImage || draft.hasPhoto
        val hasCustomEnvelope = state.selectedEnvelope.id != "envelope_1"
        val hasCreatorItem = draft.paperUrl != null ||
            state.selectedEnvelope.creatorName != null ||
            state.selectedSticker?.creatorName != null ||
            stamp.creatorName != null

        analytics.logEvent(
            "letter_customized",
            bundleOf(
                "paper_id" to paperId,
                "sticker_id" to stickerId,
                "envelope_id" to envelopeId,
                "stamp_id" to stampId,
                "has_paper" to hasCustomPaper,
                "has_custom_paper" to hasCustomPaper,
                "has_sticker" to hasSticker,
                "has_gallery_image" to hasGalleryImage,
                "has_photo" to hasGalleryImage,
                "gallery_image_count" to draft.galleryImageCount,
                "has_envelope" to hasCustomEnvelope,
                "has_custom_envelope" to hasCustomEnvelope,
                "has_creator_item" to hasCreatorItem,
            ),
        )
    }
}
