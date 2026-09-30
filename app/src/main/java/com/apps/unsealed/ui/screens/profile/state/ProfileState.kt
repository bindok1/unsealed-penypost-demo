package com.apps.unsealed.ui.screens.profile.state

import androidx.compose.ui.graphics.vector.ImageVector
import com.apps.unsealed.feature.letters.data.ShowcaseItemDto
import com.apps.unsealed.feature.letters.data.ShowcaseStatus
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeDesign
import com.apps.unsealed.ui.screens.selectrecipient.constants.StampDesign
import com.apps.unsealed.ui.theme.PaperTemplate

data class AddressBookEntry(
    val id: String,
    val userId: String,
    val name: String,
    val country: String,
    val photoUrl: String? = null,
    val isOnline: Boolean = false,
)

sealed class AddressBookUiState {
    object Loading : AddressBookUiState()
    data class Success(val items: List<AddressBookEntry>) : AddressBookUiState()
    data class Error(val message: String) : AddressBookUiState()
}

data class DraftEntry(
    val id: String,
    val recipientName: String,
    val snippet: String,
    val lastEditedLabel: String,
    val paperTemplate: PaperTemplate,
)

data class StampBookEntry(
    val design: StampDesign,
    val isCollected: Boolean,
)

data class TipEntry(
    val id: String,
    val titleRes: Int,
    val bodyRes: Int,
    val icon: ImageVector,
)

/** Drives the "Your Invite Code" card on [com.apps.unsealed.ui.screens.profile.screen.ProfileInviteFriendsScreen]. */
sealed class InviteGenerateUiState {
    object Loading : InviteGenerateUiState()
    data class Success(val code: String, val deepLinkUrl: String) : InviteGenerateUiState()
    data class Error(val message: String) : InviteGenerateUiState()
}

/** Drives the "Have a Friend's Invite Code?" card on the same screen — [Idle] before any
 * submission, distinct from [Error] so the redeem field doesn't show a stale error on first load. */
sealed class InviteRedeemUiState {
    object Idle : InviteRedeemUiState()
    object Loading : InviteRedeemUiState()
    data class Success(val inviterName: String) : InviteRedeemUiState()
    data class Error(val message: String) : InviteRedeemUiState()
}

/** One row of the "Blocked Users" screen — [blockedAtLabel] is pre-formatted for display. */
data class BlockedUserEntry(
    val userId: String,
    val userName: String,
    val blockedAtLabel: String,
)

sealed class BlockedUsersUiState {
    object Loading : BlockedUsersUiState()
    data class Success(val items: List<BlockedUserEntry>) : BlockedUsersUiState()
    data class Error(val message: String) : BlockedUsersUiState()
}

/** One card in `UnsealedProfileContent`'s Postal Collection Showcase — a
 * letter the owner has opted (via the compose-screen "Show in Postal
 * Collection" toggle) to show off publicly. [envelope]/[paperTemplate] are
 * only ever used as the `AsyncImage` placeholder/error fallback — the real
 * artwork is [envelopeCompositeImageUrl]/[compositeImageUrl], same pattern
 * as `MailboxFullLetterViewer.kt`. [showcaseStatus] drives the "···" menu —
 * paused items keep rendering exactly like visible ones (same card, same
 * image), only the menu action swaps from Pause to Reactivate. */
data class PostalCollectionItem(
    val letterId: String,
    val envelope: EnvelopeDesign,
    val envelopeCompositeImageUrl: String?,
    val paperTemplate: PaperTemplate,
    val compositeImageUrl: String?,
    val showcaseStatus: ShowcaseStatus,
    /** Only non-null when [compositeImageUrl] is null (sender's device
     * skipped compositing) — rendered over [paperTemplate]'s texture instead
     * of the missing image, same fallback [OpenLetterOverlay] already uses
     * for seed letters. See `docs/be/letters_api.md` §"Update (2026-08-18)". */
    val bodyText: String? = null,
)

fun ShowcaseItemDto.toPostalCollectionItem() = PostalCollectionItem(
    letterId = id,
    envelope = EnvelopeDesign.fromApiString(envelope),
    envelopeCompositeImageUrl = envelopeCompositeImageUrl,
    paperTemplate = PaperTemplate.entries.firstOrNull { it.name == paperTemplate } ?: PaperTemplate.PLAIN_WHITE,
    compositeImageUrl = compositeImageUrl,
    showcaseStatus = ShowcaseStatus.entries.firstOrNull { it.apiValue == showcaseStatus } ?: ShowcaseStatus.VISIBLE,
    bodyText = bodyText,
)

sealed interface PostalCollectionUiState {
    data object Loading : PostalCollectionUiState
    data class Success(val items: List<PostalCollectionItem>) : PostalCollectionUiState
    data class Error(val message: String) : PostalCollectionUiState
}

/** Drives the "···" menu's Pause/Delete feedback — mirrors
 * [com.apps.unsealed.ui.screens.inbox.state.ThreadReportState]'s convention
 * (separate per-action state, resolved via `LaunchedEffect` + Toast). On
 * success the caller just re-triggers [PostalCollectionUiState] reload
 * rather than tracking a dedicated Success variant here. */
sealed interface ShowcaseActionState {
    data object Idle : ShowcaseActionState
    data class Error(val message: String) : ShowcaseActionState
}
