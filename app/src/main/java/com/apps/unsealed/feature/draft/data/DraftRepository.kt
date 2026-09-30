package com.apps.unsealed.feature.draft.data

import com.apps.unsealed.core.database.DraftDao
import com.apps.unsealed.core.database.DraftEntity
import com.apps.unsealed.ui.screens.compose.state.StyleRun
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class DraftRepository @Inject constructor(
    private val draftDao: DraftDao,
    moshi: Moshi,
) {
    private val styleRunsAdapter =
        moshi.adapter<List<StyleRun>>(Types.newParameterizedType(List::class.java, StyleRun::class.java))

    fun observeAll(): Flow<List<DraftEntity>> = draftDao.observeAll()

    suspend fun getById(draftId: String): DraftEntity? = draftDao.getById(draftId)

    suspend fun getLatestByRecipientId(recipientId: String): DraftEntity? =
        draftDao.getLatestByRecipientId(recipientId)

    suspend fun delete(draftId: String) = draftDao.deleteById(draftId)

    /** Wipes all local drafts — called on sign-out (see `AuthRepository.signOut`)
     * since `drafts` has no `userId` column: without this, a second account
     * signing in on the same device would see the previous user's drafts. */
    suspend fun clearAll() = draftDao.deleteAll()

    /** Preserves the original [DraftEntity.createdAt] on update (looked up
     * fresh here rather than trusted from the caller); [DraftEntity.updatedAt]
     * is always stamped to now. */
    suspend fun upsert(draft: DraftEntity) {
        val existing = draftDao.getById(draft.draftId)
        draftDao.upsert(
            draft.copy(
                createdAt = existing?.createdAt ?: draft.createdAt,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    fun encodeStyleRuns(runs: List<StyleRun>): String = styleRunsAdapter.toJson(runs)

    fun decodeStyleRuns(json: String): List<StyleRun> = styleRunsAdapter.fromJson(json).orEmpty()
}
