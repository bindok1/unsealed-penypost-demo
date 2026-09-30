package com.apps.unsealed.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    @Upsert
    suspend fun upsert(draft: DraftEntity)

    @Query("DELETE FROM drafts WHERE draftId = :draftId")
    suspend fun deleteById(draftId: String)

    @Query("DELETE FROM drafts")
    suspend fun deleteAll()

    @Query("SELECT * FROM drafts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DraftEntity>>

    @Query("SELECT * FROM drafts WHERE draftId = :draftId")
    suspend fun getById(draftId: String): DraftEntity?

    /** Most recently touched draft addressed to this recipient, if any —
     * backs the "Continue Writing" state of [com.apps.unsealed.ui.screens.inbox.widgets.WriteLetterButton]
     * on [com.apps.unsealed.ui.screens.inbox.screen.MailboxThreadScreen]. */
    @Query("SELECT * FROM drafts WHERE recipientId = :recipientId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestByRecipientId(recipientId: String): DraftEntity?
}
