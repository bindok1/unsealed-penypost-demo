package com.apps.unsealed.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/** First local DB in this project (everything else so far is either backend-fetched
 * or the single narrow `WelcomePreferences` DataStore boolean). `exportSchema = false`
 * is a deliberate v1 simplification — one table, no released schema to preserve yet.
 * v2 added [BookmarkEntity] (PenPals "saved letters") — see [DatabaseModule]'s
 * `fallbackToDestructiveMigration()`, a deliberate choice pre-release (no real
 * user data at stake yet) over writing a real `Migration`. */
@Database(entities = [DraftEntity::class, BookmarkEntity::class], version = 2, exportSchema = false)
abstract class UnsealedDatabase : RoomDatabase() {
    abstract fun draftDao(): DraftDao
    abstract fun bookmarkDao(): BookmarkDao
}
