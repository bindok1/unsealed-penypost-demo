package com.apps.unsealed.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Local persistence for the Draft-on-Exit feature (`docs/todo.md` #5,
 * `docs/compose-screen-spec.md` §11). Deliberately text-only — [ImageInstance],
 * [AnnotateState] strokes, and stickers are never persisted here (see
 * `docs/compose-screen-spec.md` §11's "Scope v1" note); every field below
 * maps 1:1 onto a text-only [com.apps.unsealed.ui.screens.compose.ComposeUiState]
 * field. Enum fields are stored as their `.name` string; [TextAlign] and
 * [Color] aren't real Kotlin enums so they go through manual mapping helpers
 * in `ComposeViewModel` instead. */
@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val draftId: String,
    val recipientId: String?,
    val recipientName: String?,
    val dearName: String,
    val bodyText: String,
    val bodyStyleRunsJson: String,
    val signOff: String,
    val selectedFont: String,
    val selectedGoogleFontName: String?,
    val fontSize: Float,
    val textAlignment: String,
    val inkColor: Long,
    val paperColor: String,
    val selectedPaperTemplate: String?,
    val visibility: String,
    val crisisTag: String,
    val createdAt: Long,
    val updatedAt: Long,
)
