package com.apps.unsealed.ui.screens.selectrecipient.state

import com.apps.unsealed.feature.addressbook.data.AddressBookContactDto
import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.ui.screens.compose.state.LetterFont
import com.apps.unsealed.ui.screens.selectrecipient.constants.PenpalRegion

/** Placeholder used only until [SelectRecipientViewModel] overwrites it with
 * the first real item from the loaded envelope catalog (network or bundled
 * fallback, see [localEnvelopeCatalog]) right after construction — never
 * actually shown to the user as-is. */
private val PlaceholderEnvelope = CatalogItemDto(id = "ENVELOPE_1", name = "Envelope 1", imageUrl = "", isPremium = false)

data class SelectRecipientUiState(
    val recipientMode: RecipientMode = RecipientMode.NONE,
    val selectedRegion: PenpalRegion? = null,
    val selectedContact: AddressBookContactDto? = null,
    /** Only set when [recipientMode] is [RecipientMode.KNOWN_USER] — arrived via
     * reply nav-args (Open Letter -> Compose -> here) or picked from Address Book. */
    val recipientId: String? = null,
    val recipientName: String? = null,
    /** Raw continent string for the [RecipientMode.KNOWN_USER] flow */
    val recipientContinent: String? = null,
    val isReplyFlow: Boolean = false,
    /** Handwriting font used to render address & recipient on the envelope */
    val selectedFont: LetterFont = LetterFont.CAVEAT,
    val isFontPickerOpen: Boolean = false,
    /** CMS-driven catalog entries (`feature/catalog/data/`) */
    val selectedEnvelope: CatalogItemDto = PlaceholderEnvelope,
    val selectedStamp: CatalogItemDto? = null,
    val selectedSticker: CatalogItemDto? = null,
    val isRecipientPickerOpen: Boolean = false,
    val initialPickerTab: Int = 0, // 0 = Regions, 1 = Address Book
    val isEnvelopePickerOpen: Boolean = false,
    val isStampPickerOpen: Boolean = false,
    val isStickerPickerOpen: Boolean = false,
    val comingSoonFeature: ComingSoonFeature? = null,
    val isSending: Boolean = false,
    val sendError: String? = null,
    val sentEstimatedArrivalAt: String? = null,
    val sentLetterId: String? = null,
    /** `"public"`/`"private"` — mirrors the `visibility` sent on `POST /letters`
     * (see `ComposeUiState.isPublicShowcase`), threaded through so
     * `LetterSentScreen` can show the right post-send CTAs. */
    val sentVisibility: String = "private",
    /** Auto-prompted once per region pick (not on Address Book/reply — see
     * `SelectRecipientViewModel.onRegionSelect`) rather than left to the
     * hidden compose-screen overflow-menu toggle, so the "this becomes
     * public" consent shows right when it's decision-relevant. */
    val showPublicShowcaseDialog: Boolean = false,
    /** Same value already uploaded/sent as `SendLetterRequest.compositeImageUrl` —
     * reused here (not re-fetched) so `LetterSentScreen` can offer Share
     * without re-rendering the letter card. */
    val sentCompositeImageUrl: String? = null,
) {
    /** Drives the screen title swap ("Select Recipient" -> "Ready to Send"). */
    val isReadyToSend: Boolean get() = selectedStamp != null

    val hasRecipient: Boolean get() = recipientMode != RecipientMode.NONE

    val canSend: Boolean get() = hasRecipient && isReadyToSend

    /** Alias for backwards compatibility if any legacy callers reference isRegionPickerOpen */
    val isRegionPickerOpen: Boolean get() = isRecipientPickerOpen
}

enum class RecipientMode { NONE, PENPAL_REGION, KNOWN_USER }

enum class ComingSoonFeature { TEXT, STICKER, SHARE_CONTACT }
