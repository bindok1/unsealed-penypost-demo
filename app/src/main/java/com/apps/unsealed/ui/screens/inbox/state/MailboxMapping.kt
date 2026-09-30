package com.apps.unsealed.ui.screens.inbox.state

import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.feature.mailbox.data.MailboxRoomDto
import com.apps.unsealed.ui.screens.selectrecipient.constants.resolveItem

fun MailboxRoomDto.toMailboxRoomItem(stampCatalog: List<CatalogItemDto>): MailboxRoomItem = MailboxRoomItem(
    correspondentId = correspondentId,
    correspondentName = correspondentName,
    correspondentContinent = correspondentContinent,
    lastActivityLabel = formatLastActivity(lastSentAt),
    stamp = stampCatalog.resolveItem(lastStamp),
    unreadCount = unreadCount,
    lastStatus = lastStatus,
)
