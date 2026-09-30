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

private val Context.welcomeDataStore: DataStore<Preferences> by preferencesDataStore(name = "welcome_prefs")

private val HasSeenWelcomeKey = booleanPreferencesKey("has_seen_welcome")

/**
 * Tracks whether the user has already been through the first-install
 * Welcome carousel, so [com.apps.unsealed.ui.screens.auth.SplashScreen] only
 * shows it once — after that, guests land straight on the main app.
 */
@Singleton
class WelcomePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val hasSeenWelcome: Flow<Boolean> =
        context.welcomeDataStore.data.map { it[HasSeenWelcomeKey] ?: false }

    suspend fun markWelcomeSeen() {
        context.welcomeDataStore.edit { it[HasSeenWelcomeKey] = true }
    }
}
