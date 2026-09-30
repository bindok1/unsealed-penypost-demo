package com.apps.unsealed.ui.screens.penpals.state

import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.feature.penpals.data.FeedItemDto
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeDesign
import com.apps.unsealed.ui.screens.selectrecipient.constants.PenpalRegion
import com.apps.unsealed.ui.screens.selectrecipient.constants.resolveImageUrl

/**
 * Domain model for a single penpal feed post, mapped from [FeedItemDto].
 */
data class PenpalLetter(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderPhotoUrl: String? = null,
    val region: PenpalRegion,
    /** Placeholder/error fallback art only — see [envelopeImageUrl]. */
    val envelope: EnvelopeDesign,
    /** Resolved against the CMS-driven envelope catalog. */
    val envelopeImageUrl: String,
    /** Stamp catalog id, used to look up art in the stamp catalog for the
     * overlay fallback when [envelopeCompositeImageUrl] is null — mirrors
     * [envelopeStickerId], see `docs/penpals-screen-spec.md` §Context. */
    val stampId: String? = null,
    /** Single-slot sticker id, used to look up art in the sticker catalog for
     * the overlay fallback when [envelopeCompositeImageUrl] is null — see
     * `docs/envelope-sticker-spec.md` §10. */
    val envelopeStickerId: String? = null,
    /** Backend-composited envelope (background + sticker flattened); null for
     * legacy posts or senders whose device skipped client-side compositing. */
    val envelopeCompositeImageUrl: String? = null,
    /** Flattened letter paper (text + paper texture + annotations, same idea
     * as [envelopeCompositeImageUrl] but for the letter body) — mirrors
     * `LetterDetailDto.compositeImageUrl` (`docs/be/letters_api.md`). Null for
     * legacy posts or senders below `MinCompositingDensity`, in which case
     * [OpenLetterOverlay] falls back to rendering [bodyText] over a generic
     * paper texture (the sender's actual chosen paper isn't sent back by
     * `GET /penpals/feed` today — same gap as `InboxItem.letterPaper`). */
    val compositeImageUrl: String? = null,
    val paperUrl: String? = null,
    val bodyText: String,
    val postedAt: String,           // ISO 8601 — use formattedPostedAt() for display
    val likeCount: Int,
    val viewerHasLiked: Boolean,
    val isOnline: Boolean,
)

/** Maps a backend [FeedItemDto] to the client domain model. */
fun FeedItemDto.toPenpalLetter(envelopeCatalog: List<CatalogItemDto>): PenpalLetter {
    val envelopeId = envelope.orEmpty()
    return PenpalLetter(
        id            = id,
        senderId      = senderId,
        senderName    = senderName.orEmpty(),
        senderPhotoUrl = senderPhotoUrl,
        region        = PenpalRegion.fromApiString(region.orEmpty()),
        envelope      = EnvelopeDesign.fromApiString(envelopeId),
        envelopeImageUrl = envelopeCatalog.resolveImageUrl(envelopeId),
        stampId       = stamp,
        envelopeStickerId = envelopeStickerId,
        envelopeCompositeImageUrl = envelopeCompositeImageUrl,
        compositeImageUrl = compositeImageUrl,
        paperUrl      = paperUrl,
        bodyText      = bodyText.orEmpty(),
        postedAt      = postedAt.orEmpty(),
        likeCount     = likeCount ?: 0,
        viewerHasLiked = viewerHasLiked ?: false,
        isOnline      = isOnline ?: false,
    )
}

/**
 * Formats the raw ISO 8601 [PenpalLetter.postedAt] string into a locale-aware
 * "d MMM yyyy" display label (e.g. "13 Jul 2026").
 */
fun PenpalLetter.formattedPostedAt(): String = runCatching {
    val instant = java.time.Instant.parse(postedAt)
    val zdt = instant.atZone(java.time.ZoneId.systemDefault())
    java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy").format(zdt)
}.getOrDefault(postedAt)
