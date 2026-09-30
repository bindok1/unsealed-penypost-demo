package com.apps.unsealed.core.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory bridge between `ComposeViewModel` (Write tab) and
 * `SelectRecipientViewModel` (Send flow) — the two screens don't share a nav
 * scope, so the letter content set while writing needs to survive the hop to
 * Send. Stepping stone toward Draft Auto-Save (`docs/todo.md` #5,
 * Room-backed) — in-memory for now, swap the backing store later without
 * touching either ViewModel's call site.
 */
@Singleton
class ComposeDraftHolder @Inject constructor() {
    private var draft: ComposeDraft? = null

    fun set(draft: ComposeDraft) {
        this.draft = draft
    }

    fun get(): ComposeDraft? = draft

    /** Flips [ComposeDraft.isPublicShowcase] on the currently-held draft —
     * used by `SelectRecipientViewModel`'s auto-prompted publish dialog
     * (region-match flow), which can set this *after* [set] already ran
     * (compose time), unlike `ComposeUiState`'s own toggle which is the only
     * writer before this point. No-op if nothing is held. */
    fun setPublicShowcase(value: Boolean) {
        draft = draft?.copy(isPublicShowcase = value)
    }

    private var recipientDraft: RecipientSelectionsDraft? = null

    fun setRecipientDraft(draft: RecipientSelectionsDraft?) {
        this.recipientDraft = draft
    }

    fun getRecipientDraft(): RecipientSelectionsDraft? = recipientDraft

    fun clear() {
        draft = null
        recipientDraft = null
    }
}

data class ComposeDraft(
    val bodyText: String,
    val paperTemplate: String,
    val fontId: String,
    val paperColor: String,
    /** Non-null if this letter originated from a saved [com.apps.unsealed.core.database.DraftEntity]
     * row — lets a direct Send (which never goes through the exit dialog) still know which
     * draft row to delete on send success. */
    val draftId: String? = null,
    /** Public R2 URL of the flattened letter WebP — always populated by the time
     * this draft is set (see `ComposeViewModel.compositeAndSaveDraftForSend`),
     * every letter is composited before Send. Carried here rather than
     * recomputed because `LetterCanvas` only exists live on the Compose
     * screen, not on Select Recipient. */
    val compositeImageUrl: String? = null,
    /** Mirrors [com.apps.unsealed.ui.screens.compose.state.ComposeUiState.isPublicShowcase]
     * at the moment Send was tapped — read by `SelectRecipientViewModel` to
     * set `SendLetterRequest.visibility` ("public"/"private"). */
    val isPublicShowcase: Boolean = false,
    val paperUrl: String? = null,
    val hasBodyStickers: Boolean = false,
    val bodyStickerId: String? = null,
    val hasPhoto: Boolean = false,
    val hasGalleryImage: Boolean = false,
    val galleryImageCount: Int = 0,
)

data class RecipientSelectionsDraft(
    val recipientModeName: String = "PENPAL_REGION",
    val selectedRegionName: String? = null,
    val selectedContactId: String? = null,
    val recipientId: String? = null,
    val recipientName: String? = null,
    val recipientContinent: String? = null,
    val selectedEnvelopeId: String? = null,
    val selectedStampId: String? = null,
    val selectedStickerId: String? = null,
    val selectedFontName: String? = null,
)
