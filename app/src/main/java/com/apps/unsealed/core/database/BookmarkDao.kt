package com.apps.unsealed.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Upsert
    suspend fun upsert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarked_letters WHERE letterId = :letterId")
    suspend fun deleteById(letterId: String)

    @Query("DELETE FROM bookmarked_letters WHERE letterId IN (:letterIds)")
    suspend fun deleteByIds(letterIds: List<String>)

    @Query("DELETE FROM bookmarked_letters")
    suspend fun deleteAll()

    @Query("SELECT * FROM bookmarked_letters ORDER BY bookmarkedAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarked_letters WHERE letterId = :letterId")
    suspend fun getById(letterId: String): BookmarkEntity?
}
