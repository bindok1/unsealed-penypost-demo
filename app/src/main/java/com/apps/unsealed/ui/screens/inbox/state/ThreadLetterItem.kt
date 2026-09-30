package com.apps.unsealed.ui.screens.inbox.state

import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeDesign
import com.apps.unsealed.ui.theme.PaperTemplate

/** One letter row inside [com.apps.unsealed.ui.screens.inbox.screen.MailboxThreadScreen],
 * from `GET /mailbox/{threadId}`. */
data class ThreadLetterItem(
    val id: String,
    val senderId: String,
    /** `senderId == viewer uid` — drives left/right ("chat kiri-kanan") alignment. */
    val isFromViewer: Boolean,
    val bodyText: String,
    val deliveryStatus: LetterDeliveryStatus,
    /** e.g. "Delivered on Aug 13, 6:25 PM" / "Posted on Apps, Aug 13, 9:00 AM". */
    val metaLabel: String,
    /** Only set when [deliveryStatus] is [LetterDeliveryStatus.INCOMING]. */
    val expectedDeliveryLabel: String?,
    /** Raw ISO instants (unlike [metaLabel]/[expectedDeliveryLabel], which are
     * pre-formatted for display) — needed to hand off to the delivery
     * tracking map's progress math, see `DeliveryTrackingViewModel`. */
    val sentAt: String,
    val estimatedArrivalAt: String,
    /** Resolved against the CMS-driven stamp catalog — see
     * [com.apps.unsealed.ui.screens.selectrecipient.constants.resolveItem]. */
    val stamp: CatalogItemDto,
    /** Placeholder/error fallback art only — [envelopeImageUrl] is the real
     * render source (resolved against the CMS-driven envelope catalog, so a
     * CMS-added design still renders correctly instead of falling back to
     * this enum's closed 5-name set). */
    val envelope: EnvelopeDesign,
    val envelopeImageUrl: String,
    /** Fallback texture shown until [compositeImageUrl] loads, or for legacy
     * letters with no composite image. */
    val letterPaper: PaperTemplate,
    val compositeImageUrl: String?,
    val paperUrl: String? = null,
)
