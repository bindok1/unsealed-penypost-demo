package com.apps.unsealed.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.penyDataStore: DataStore<Preferences> by preferencesDataStore(name = "peny_prefs")

private val HasTriedPenyModeKey = booleanPreferencesKey("has_tried_peny_mode")

/**
 * Tracks whether the user has ever activated Mode Peny at least once, so
 * `PenyWandHint`'s discovery dot only shows until they've actually found the
 * feature — same mark-once-and-forget pattern as [WelcomePreferences].
 */
@Singleton
class PenyPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val hasTriedPenyMode: Flow<Boolean> =
        context.penyDataStore.data.map { it[HasTriedPenyModeKey] ?: false }

    suspend fun markPenyModeTried() {
        context.penyDataStore.edit { it[HasTriedPenyModeKey] = true }
    }
}
