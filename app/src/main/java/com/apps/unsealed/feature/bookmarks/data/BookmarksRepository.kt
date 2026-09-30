package com.apps.unsealed.feature.bookmarks.data

import com.apps.unsealed.core.database.BookmarkDao
import com.apps.unsealed.core.database.BookmarkEntity
import com.apps.unsealed.ui.screens.penpals.state.PenpalLetter
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeDesign
import com.apps.unsealed.ui.screens.selectrecipient.constants.PenpalRegion
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** PenPals "saved letters" — local-only (Room), no backend endpoint; see
 * `BookmarkEntity`'s doc comment for why this is a plausible v1 scope call. */
@Singleton
class BookmarksRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao,
) {
    fun observeAll(): Flow<List<BookmarkEntity>> = bookmarkDao.observeAll()

    /** Adds [letter] if not already saved, removes it if it is. Returns the
     * resulting bookmarked state (true = now saved) so the caller can choose
     * the right snackbar message without a second query. */
    suspend fun toggleBookmark(letter: PenpalLetter): Boolean {
        val existing = bookmarkDao.getById(letter.id)
        return if (existing != null) {
            bookmarkDao.deleteById(letter.id)
            false
        } else {
            bookmarkDao.upsert(letter.toBookmarkEntity())
            true
        }
    }

    suspend fun removeAll(letterIds: Set<String>) = bookmarkDao.deleteByIds(letterIds.toList())

    /** Wipes all local bookmarks — called on sign-out (see `AuthRepository.signOut`)
     * since `bookmarked_letters` has no `userId` column: without this, a second
     * account signing in on the same device would see the previous user's
     * saved letters. */
    suspend fun clearAll() = bookmarkDao.deleteAll()
}

private fun PenpalLetter.toBookmarkEntity() = BookmarkEntity(
    letterId = id,
    senderId = senderId,
    senderName = senderName,
    region = region.apiLabel,
    envelope = envelope.name,
    envelopeImageUrl = envelopeImageUrl,
    stampId = stampId,
    envelopeStickerId = envelopeStickerId,
    envelopeCompositeImageUrl = envelopeCompositeImageUrl,
    compositeImageUrl = compositeImageUrl,
    bodyText = bodyText,
    postedAt = postedAt,
    likeCount = likeCount,
    viewerHasLiked = viewerHasLiked,
    isOnline = isOnline,
    bookmarkedAt = System.currentTimeMillis(),
)

fun BookmarkEntity.toPenpalLetter() = PenpalLetter(
    id = letterId,
    senderId = senderId,
    senderName = senderName,
    region = PenpalRegion.fromApiString(region),
    envelope = EnvelopeDesign.fromApiString(envelope),
    envelopeImageUrl = envelopeImageUrl,
    stampId = stampId,
    envelopeStickerId = envelopeStickerId,
    envelopeCompositeImageUrl = envelopeCompositeImageUrl,
    compositeImageUrl = compositeImageUrl,
    bodyText = bodyText,
    postedAt = postedAt,
    likeCount = likeCount,
    viewerHasLiked = viewerHasLiked,
    isOnline = isOnline,
)
