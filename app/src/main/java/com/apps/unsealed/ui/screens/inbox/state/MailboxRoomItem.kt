package com.apps.unsealed.ui.screens.inbox.state

import com.apps.unsealed.feature.catalog.data.CatalogItemDto

/** One row in [com.apps.unsealed.ui.screens.inbox.screen.MailboxScreen] — a
 * conversation "room" with one correspondent, from `GET /mailbox`. */
data class MailboxRoomItem(
    val correspondentId: String,
    val correspondentName: String,
    val correspondentContinent: String,
    /** Formatted `last_sent_at` — "last activity", not a confirmed delivery
     * time (see `formatLastActivity`'s doc for why). */
    val lastActivityLabel: String,
    /** Resolved against the CMS-driven stamp catalog — see
     * [com.apps.unsealed.ui.screens.selectrecipient.constants.resolveItem]. */
    val stamp: CatalogItemDto,
    val unreadCount: Int,
    /** Raw `last_status` (`"IN_TRANSIT"`/`"DELIVERED"`) from `GET /mailbox` —
     * drives the "In Transit" badge; the actual per-letter tracking action
     * lives inside the thread (a room can have multiple in-transit letters,
     * both directions), see `MailboxThreadScreen`. */
    val lastStatus: String,
) {
    val isUnread: Boolean get() = unreadCount > 0
    val isLastLetterInTransit: Boolean get() = lastStatus == "IN_TRANSIT"
}
