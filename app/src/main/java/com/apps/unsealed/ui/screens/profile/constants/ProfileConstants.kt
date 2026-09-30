package com.apps.unsealed.ui.screens.profile.constants

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import com.apps.unsealed.R
import com.apps.unsealed.ui.screens.profile.state.AddressBookEntry
import com.apps.unsealed.ui.screens.profile.state.StampBookEntry
import com.apps.unsealed.ui.screens.profile.state.TipEntry
import com.apps.unsealed.ui.screens.selectrecipient.constants.StampDesign

val dummyAddressBook = listOf(
    AddressBookEntry("addr_1", "user_1", "Yui — Osaka", "Japan"),
    AddressBookEntry("addr_2", "user_2", "Mateo — Lisbon", "Portugal"),
    AddressBookEntry("addr_3", "user_3", "Amara — Lagos", "Nigeria"),
    AddressBookEntry("addr_4", "user_4", "Freya — Oslo", "Norway"),
    AddressBookEntry("addr_5", "user_5", "Diego — Buenos Aires", "Argentina"),
    AddressBookEntry("addr_6", "user_6", "Noor — Amman", "Jordan"),
)

val dummyStampBook = listOf(
    StampBookEntry(StampDesign.ROSE, isCollected = true),
    StampBookEntry(StampDesign.OCEAN, isCollected = true),
    StampBookEntry(StampDesign.MARIGOLD, isCollected = true),
    StampBookEntry(StampDesign.SAGE, isCollected = true),
    StampBookEntry(StampDesign.PLUM, isCollected = false),
    StampBookEntry(StampDesign.INK, isCollected = false),
)

val dummyTips = listOf(
    TipEntry("tip_delivery_time", R.string.profile_tips_delivery_time_title, R.string.profile_tips_delivery_time_body, Icons.Filled.Schedule),
    TipEntry("tip_first_letter", R.string.profile_tips_first_letter_title, R.string.profile_tips_first_letter_body, Icons.Filled.Edit),
    TipEntry("tip_customize_letter", R.string.profile_tips_customize_letter_title, R.string.profile_tips_customize_letter_body, Icons.Filled.Palette),
    TipEntry("tip_drafts", R.string.profile_tips_drafts_title, R.string.profile_tips_drafts_body, Icons.Filled.Drafts),
    TipEntry("tip_stamps", R.string.profile_tips_stamps_title, R.string.profile_tips_stamps_body, Icons.Filled.CollectionsBookmark),
    TipEntry("tip_penpals", R.string.profile_tips_penpals_title, R.string.profile_tips_penpals_body, Icons.Filled.Explore),
    TipEntry("tip_postal_collection", R.string.profile_tips_postal_collection_title, R.string.profile_tips_postal_collection_body, Icons.Filled.AutoAwesome),
    TipEntry("tip_delivery_hours", R.string.profile_tips_delivery_hours_title, R.string.profile_tips_delivery_hours_body, Icons.Filled.NotificationsActive),
    TipEntry("tip_sign_in", R.string.profile_tips_sign_in_title, R.string.profile_tips_sign_in_body, Icons.AutoMirrored.Filled.Login),
    TipEntry("tip_safety", R.string.profile_tips_safety_title, R.string.profile_tips_safety_body, Icons.Filled.Shield),
    TipEntry("tip_envelope_quality", R.string.profile_tips_envelope_quality_title, R.string.profile_tips_envelope_quality_body, Icons.Filled.PhotoSizeSelectActual),
)
