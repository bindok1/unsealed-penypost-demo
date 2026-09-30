package com.apps.unsealed.ui.screens.inbox.state

import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.feature.mailbox.data.MailboxLetterDto
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeDesign
import com.apps.unsealed.ui.screens.selectrecipient.constants.resolveImageUrl
import com.apps.unsealed.ui.screens.selectrecipient.constants.resolveItem
import com.apps.unsealed.ui.theme.PaperTemplate

/** Backend doesn't return `paper_template` on `GET /mailbox/{threadId}` yet
 * (same gap as `GET /letters/inbox`/`{id}`, see `docs/be/letters_api.md`) —
 * [ThreadLetterItem.letterPaper] stays a fixed placeholder here until that
 * field exists on the wire. */
private val PlaceholderLetterPaper = PaperTemplate.AGED_KRAFT

fun MailboxLetterDto.toThreadLetterItem(
    viewerId: String,
    envelopeCatalog: List<CatalogItemDto>,
    stampCatalog: List<CatalogItemDto>,
): ThreadLetterItem {
    // Wire uses IN_TRANSIT/DELIVERED; client's LetterDeliveryStatus predates
    // the backend contract and splits the not-yet-arrived state by direction
    // — INCOMING (waiting on the viewer) vs OUTGOING_IN_TRANSIT (viewer sent
    // it, nothing to "open early", but it's trackable on the map).
    val isFromViewer = senderId == viewerId
    val estimatedArrivalMillis = runCatching {
        java.time.Instant.parse(estimatedArrivalAt).toEpochMilli()
    }.getOrDefault(Long.MAX_VALUE)
    val isDelivered = status == "DELIVERED" || (estimatedArrivalMillis <= System.currentTimeMillis() && estimatedArrivalMillis > 0)
    val deliveryStatus = when {
        isDelivered -> LetterDeliveryStatus.DELIVERED
        isFromViewer -> LetterDeliveryStatus.OUTGOING_IN_TRANSIT
        else -> LetterDeliveryStatus.INCOMING
    }
    val metaLabel = when (deliveryStatus) {
        LetterDeliveryStatus.DELIVERED -> "Delivered on ${(deliveredAt ?: sentAt).formatMetaLabel()}"
        LetterDeliveryStatus.INCOMING, LetterDeliveryStatus.OUTGOING_IN_TRANSIT -> "Posted on Apps, ${sentAt.formatMetaLabel()}"
    }
    return ThreadLetterItem(
        id = id,
        senderId = senderId,
        isFromViewer = isFromViewer,
        bodyText = bodyText,
        deliveryStatus = deliveryStatus,
        metaLabel = metaLabel,
        expectedDeliveryLabel = if (deliveryStatus != LetterDeliveryStatus.DELIVERED) {
            formatExpectedDelivery(estimatedArrivalAt)
        } else {
            null
        },
        sentAt = sentAt,
        estimatedArrivalAt = estimatedArrivalAt,
        stamp = stampCatalog.resolveItem(stamp),
        // fromApiString(), not valueOf() — a CMS-added envelope id must not
        // crash this mapping; the real art comes from envelopeImageUrl below.
        envelope = EnvelopeDesign.fromApiString(envelope),
        envelopeImageUrl = envelopeCatalog.resolveImageUrl(envelope),
        letterPaper = PlaceholderLetterPaper,
        compositeImageUrl = compositeImageUrl,
        paperUrl = paperUrl,
    )
}
