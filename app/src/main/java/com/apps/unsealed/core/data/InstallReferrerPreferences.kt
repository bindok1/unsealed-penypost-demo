package com.apps.unsealed.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.installReferrerDataStore: DataStore<Preferences> by preferencesDataStore(name = "install_referrer_prefs")

private val HasCheckedReferrerKey = booleanPreferencesKey("has_checked_install_referrer")
private val PendingInviteCodeKey = stringPreferencesKey("pending_referral_invite_code")

/**
 * Persists the invite code (if any) read from the Play Install Referrer string on first launch
 * (see `docs/be_updet/invite_friend_api.md` §5.1.A + [com.apps.unsealed.feature.invite.data.InstallReferrerReader]).
 * Unlike [PendingInviteCodeHolder] (in-memory, App-Links case — code arrives fresh via an
 * already-running app), this must survive process death: the referrer can only be read once
 * near install time, but redeeming it requires a registered backend user, which may not exist
 * yet until the user finishes sign-up — possibly several app launches later.
 */
@Singleton
class InstallReferrerPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Whether the Install Referrer API has already been queried once — it's meant to be read
     * a single time near install, so repeat launches shouldn't re-trigger the native call. */
    suspend fun hasCheckedReferrer(): Boolean =
        context.installReferrerDataStore.data.first()[HasCheckedReferrerKey] ?: false

    suspend fun markReferrerChecked() {
        context.installReferrerDataStore.edit { it[HasCheckedReferrerKey] = true }
    }

    suspend fun savePendingInviteCode(code: String) {
        context.installReferrerDataStore.edit { it[PendingInviteCodeKey] = code }
    }

    /** Returns the pending code (if any) and clears it — a redeem attempt is only ever fired once. */
    suspend fun consumePendingInviteCode(): String? {
        val code = context.installReferrerDataStore.data.first()[PendingInviteCodeKey]
        if (code != null) {
            context.installReferrerDataStore.edit { it.remove(PendingInviteCodeKey) }
        }
        return code
    }
}
