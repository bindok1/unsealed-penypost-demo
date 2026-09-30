package com.apps.unsealed.ui.screens.tracking.state

import com.apps.unsealed.ui.screens.selectrecipient.constants.PenpalRegion

/** How Peni is depicted carrying a letter between [PenpalRegion]s — purely
 * cosmetic (the letter's actual `estimated_arrival_at` comes from the
 * backend), chosen from the continent pair per `docs/message-tracking.md`.
 * Only two modes — no paper-plane mode, dropped in favor of keeping it to
 * the two custom Peni illustrations (`drawable/peni_mail_walk.xml`,
 * `drawable/peny_swim.xml`). */
enum class TransportMode { WALK, SWIM }

fun transportModeFor(sender: PenpalRegion, recipient: PenpalRegion): TransportMode =
    if (sender == recipient) TransportMode.WALK else TransportMode.SWIM
