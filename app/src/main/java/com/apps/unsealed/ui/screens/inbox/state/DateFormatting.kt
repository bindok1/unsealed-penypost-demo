package com.apps.unsealed.ui.screens.inbox.state

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val MetaLabelFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
private val ExpectedDeliveryFormatter = DateTimeFormatter.ofPattern("MMM d")

internal fun String.toLocalDateTime() =
    Instant.parse(this).atZone(ZoneId.systemDefault())

internal fun String.formatMetaLabel(): String = toLocalDateTime().format(MetaLabelFormatter)

/** Formats a raw `estimated_arrival_at` ISO instant the same way the mailbox
 * mappers do for a letter's expected-delivery caption — reused by
 * `LetterSentScreen` so the post-send confirmation shows the identical date
 * format the thread view uses, not a second hand-rolled pattern. */
fun formatExpectedDelivery(estimatedArrivalAt: String): String =
    estimatedArrivalAt.toLocalDateTime().format(ExpectedDeliveryFormatter)

/** Formats a room's `last_sent_at` for the [com.apps.unsealed.ui.screens.inbox.screen.MailboxScreen]
 * row — reuses [MetaLabelFormatter] rather than a third hand-rolled pattern.
 * Named "last activity", not "delivered", since `GET /mailbox` carries no
 * `last_delivered_at` field (see `docs/be/letters_api.md` gap note). */
fun formatLastActivity(lastSentAt: String): String =
    lastSentAt.toLocalDateTime().format(MetaLabelFormatter)
