package com.apps.unsealed.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Local-only "saved letters" list (PenPals bookmark feature) — flattens
 * [com.apps.unsealed.ui.screens.penpals.state.PenpalLetter] 1:1 so the saved
 * list and reading view work fully offline, with no backend endpoint. [region]
 * stores [com.apps.unsealed.ui.screens.selectrecipient.constants.PenpalRegion.apiLabel]
 * and [envelope] stores [com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeDesign.name] —
 * both round-trip through their existing `fromApiString` factories. */
@Entity(tableName = "bookmarked_letters")
data class BookmarkEntity(
    @PrimaryKey val letterId: String,
    val senderId: String,
    val senderName: String,
    val region: String,
    val envelope: String,
    val envelopeImageUrl: String,
    val stampId: String?,
    val envelopeStickerId: String?,
    val envelopeCompositeImageUrl: String?,
    val compositeImageUrl: String?,
    val bodyText: String,
    val postedAt: String,
    val likeCount: Int,
    val viewerHasLiked: Boolean,
    val isOnline: Boolean,
    val bookmarkedAt: Long,
)
