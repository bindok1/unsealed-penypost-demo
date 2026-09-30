package com.apps.unsealed.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideUnsealedDatabase(@ApplicationContext context: Context): UnsealedDatabase =
        Room.databaseBuilder(context, UnsealedDatabase::class.java, "unsealed.db")
            // Pre-release, no real user data riding on schema stability yet —
            // simpler than writing a real Migration for each version bump.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    @Singleton
    fun provideDraftDao(database: UnsealedDatabase): DraftDao = database.draftDao()

    @Provides
    @Singleton
    fun provideBookmarkDao(database: UnsealedDatabase): BookmarkDao = database.bookmarkDao()
}
