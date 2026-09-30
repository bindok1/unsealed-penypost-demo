package com.apps.unsealed.ui.screens.profile.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LegalLinks
import com.apps.unsealed.core.util.staggerEntrance
import com.apps.unsealed.ui.components.LegalDocumentBottomSheet
import com.apps.unsealed.ui.components.LetterlyCenterDialog
import com.apps.unsealed.ui.components.LetterlyMenuRow
import com.apps.unsealed.ui.components.LetterlyScreenHeader
import com.apps.unsealed.ui.screens.compose.widgets.ComingSoonBottomSheet
import com.apps.unsealed.ui.screens.profile.viewmodel.ProfileSettingsViewModel
import com.apps.unsealed.ui.screens.profile.widgets.LanguagePickerBottomSheet
import com.apps.unsealed.ui.screens.profile.widgets.NotifyHourPickerBottomSheet
import com.apps.unsealed.ui.screens.profile.widgets.NotifyHourPickerOptions
import java.util.Locale

/**
 * Settings sub-screen: rows for configuring app preferences, managing account,
 * and checking legal/support docs.
 * - Cache: shows real cache size and opens confirm dialog to wipe cache & drafts.
 * - Language: opens [LanguagePickerBottomSheet] to switch between ID and EN.
 * - Privacy Policy: opens [LegalDocumentBottomSheet] with [LegalLinks.PRIVACY_POLICY_URL].
 * - Blocked Users: opens [BlockedUsersScreen].
 * - Logout: confirms and invokes [onLogoutClick] (`AuthViewModel.signOut()`).
 * - Delete Account: confirms and invokes [onDeleteAccountClick] (`AuthViewModel.deleteAccount()`
 *   — `DELETE /api/v1/auth/me`, wipes all local storage). Deliberately routed through the same
 *   Activity-scoped `AuthViewModel` as [onLogoutClick] rather than [ProfileSettingsViewModel]'s
 *   own repository call, so the shared `uiState` LoginScreen observes actually flips to
 *   `Unauthenticated` before redirecting there — see `UnsealedNavHost.kt`'s `ProfileSettingsRoute`.
 * - Other rows open [ComingSoonBottomSheet].
 */
@Composable
fun ProfileSettingsScreen(
    onBackClick: () -> Unit,
    onBlockedUsersClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val cacheSizeText by viewModel.cacheSizeText.collectAsStateWithLifecycle()
    val notifyHourPref by viewModel.notifyHourPref.collectAsStateWithLifecycle()

    var comingSoonTitle by remember { mutableStateOf<String?>(null) }
    var isLogoutConfirmVisible by remember { mutableStateOf(false) }
    var isDeleteAccountConfirmVisible by remember { mutableStateOf(false) }
    var isClearCacheConfirmVisible by remember { mutableStateOf(false) }
    var isPrivacyPolicyVisible by remember { mutableStateOf(false) }
    var isLanguagePickerVisible by remember { mutableStateOf(false) }
    var isNotifyHourPickerVisible by remember { mutableStateOf(false) }

    val cacheTitle = stringResource(R.string.profile_settings_cache)
    val languageTitle = stringResource(R.string.profile_settings_language)
    val contactUsTitle = stringResource(R.string.profile_settings_contact_us)
    val contactUsEmailSubject = stringResource(R.string.profile_settings_contact_us_email_subject)
    val noEmailAppText = stringResource(R.string.profile_settings_contact_us_no_email_app)
    val privacyPolicyTitle = stringResource(R.string.profile_settings_privacy_policy)
    val deleteAccountTitle = stringResource(R.string.profile_settings_delete_account)

    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLangCode = if (!currentLocales.isEmpty) {
        currentLocales.toLanguageTags()
    } else {
        Locale.getDefault().language
    }
    val currentLangLabel = if (currentLangCode.startsWith("id", ignoreCase = true) || currentLangCode.startsWith("in", ignoreCase = true)) {
        "🇮🇩 Bahasa Indonesia"
    } else {
        "🇬🇧 English"
    }

    Column(modifier = modifier.fillMaxSize()) {
        LetterlyScreenHeader(title = stringResource(R.string.profile_settings_title), onBackClick = onBackClick)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                LetterlyMenuRow(
                    icon = Icons.Filled.Storage,
                    title = cacheTitle,
                    subtitle = stringResource(R.string.profile_settings_cache_size, cacheSizeText),
                    onClick = { isClearCacheConfirmVisible = true },
                    modifier = Modifier.staggerEntrance(0),
                )
            }
            item {
                LetterlyMenuRow(
                    icon = Icons.Filled.Language,
                    title = languageTitle,
                    subtitle = currentLangLabel,
                    onClick = { isLanguagePickerVisible = true },
                    modifier = Modifier.staggerEntrance(1),
                )
            }
            item {
                val currentNotifyHourLabel = NotifyHourPickerOptions
                    .firstOrNull { it.value == (notifyHourPref ?: "morning") }
                    ?.titleRes
                    ?.let { stringResource(it) }
                LetterlyMenuRow(
                    icon = Icons.Filled.Notifications,
                    title = stringResource(R.string.profile_settings_notify_hour),
                    subtitle = currentNotifyHourLabel,
                    onClick = { isNotifyHourPickerVisible = true },
                    modifier = Modifier.staggerEntrance(2),
                )
            }
            item {
                LetterlyMenuRow(
                    icon = Icons.Filled.Email,
                    title = contactUsTitle,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$CONTACT_US_EMAIL")
                            putExtra(Intent.EXTRA_SUBJECT, contactUsEmailSubject)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, noEmailAppText, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.staggerEntrance(3),
                )
            }
            item {
                LetterlyMenuRow(
                    icon = Icons.Filled.PrivacyTip,
                    title = privacyPolicyTitle,
                    onClick = { isPrivacyPolicyVisible = true },
                    modifier = Modifier.staggerEntrance(4),
                )
            }
            item {
                LetterlyMenuRow(
                    icon = Icons.Filled.Block,
                    title = stringResource(R.string.profile_settings_blocked_users),
                    onClick = onBlockedUsersClick,
                    modifier = Modifier.staggerEntrance(5),
                )
            }
            item {
                LetterlyMenuRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = stringResource(R.string.profile_settings_logout),
                    iconTint = LogoutRed,
                    onClick = { isLogoutConfirmVisible = true },
                    modifier = Modifier.staggerEntrance(6),
                )
            }
            item {
                LetterlyMenuRow(
                    icon = Icons.Filled.DeleteForever,
                    title = deleteAccountTitle,
                    iconTint = LogoutRed,
                    onClick = { isDeleteAccountConfirmVisible = true },
                    modifier = Modifier.staggerEntrance(7),
                )
            }
            item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
        }
    }

    val title = comingSoonTitle
    if (title != null) {
        ComingSoonBottomSheet(title = title, onDismiss = { comingSoonTitle = null })
    }

    if (isPrivacyPolicyVisible) {
        LegalDocumentBottomSheet(
            title = privacyPolicyTitle,
            url = LegalLinks.PRIVACY_POLICY_URL,
            onDismiss = { isPrivacyPolicyVisible = false },
        )
    }

    if (isLanguagePickerVisible) {
        LanguagePickerBottomSheet(
            currentLanguageCode = currentLangCode,
            onLanguageSelected = { langCode ->
                val localeList = if (langCode == "id") {
                    LocaleListCompat.forLanguageTags("id,in")
                } else {
                    LocaleListCompat.forLanguageTags(langCode)
                }
                AppCompatDelegate.setApplicationLocales(localeList)
            },
            onDismiss = { isLanguagePickerVisible = false },
        )
    }

    if (isNotifyHourPickerVisible) {
        NotifyHourPickerBottomSheet(
            currentValue = notifyHourPref,
            onValueSelected = { value ->
                viewModel.updateNotifyHourPref(value) { errorMsg ->
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { isNotifyHourPickerVisible = false },
        )
    }

    if (isClearCacheConfirmVisible) {
        LetterlyCenterDialog(
            title = stringResource(R.string.clear_cache_dialog_title),
            body = stringResource(R.string.clear_cache_dialog_body),
            primaryCtaText = stringResource(R.string.clear_cache_dialog_cta_confirm),
            onPrimaryClick = {
                viewModel.clearCache {
                    Toast.makeText(context, context.getString(R.string.clear_cache_success), Toast.LENGTH_SHORT).show()
                    isClearCacheConfirmVisible = false
                }
            },
            secondaryCtaText = stringResource(R.string.clear_cache_dialog_cta_cancel),
            onSecondaryClick = { isClearCacheConfirmVisible = false },
            onDismissRequest = { isClearCacheConfirmVisible = false },
            primaryContainerColor = LogoutRed,
            primaryContentColor = Color.White,
        )
    }

    if (isLogoutConfirmVisible) {
        LetterlyCenterDialog(
            title = stringResource(R.string.logout_dialog_title),
            body = stringResource(R.string.logout_dialog_body),
            primaryCtaText = stringResource(R.string.logout_dialog_cta_confirm),
            onPrimaryClick = {
                isLogoutConfirmVisible = false
                onLogoutClick()
            },
            secondaryCtaText = stringResource(R.string.logout_dialog_cta_cancel),
            onSecondaryClick = { isLogoutConfirmVisible = false },
            onDismissRequest = { isLogoutConfirmVisible = false },
            primaryContainerColor = LogoutRed,
            primaryContentColor = Color.White,
        )
    }

    if (isDeleteAccountConfirmVisible) {
        LetterlyCenterDialog(
            title = stringResource(R.string.delete_account_dialog_title),
            body = stringResource(R.string.delete_account_dialog_body),
            primaryCtaText = stringResource(R.string.delete_account_dialog_cta_confirm),
            onPrimaryClick = {
                onDeleteAccountClick(
                    { isDeleteAccountConfirmVisible = false },
                    { errorMsg ->
                        isDeleteAccountConfirmVisible = false
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    },
                )
            },
            secondaryCtaText = stringResource(R.string.delete_account_dialog_cta_cancel),
            onSecondaryClick = { isDeleteAccountConfirmVisible = false },
            onDismissRequest = { isDeleteAccountConfirmVisible = false },
            primaryContainerColor = LogoutRed,
            primaryContentColor = Color.White,
        )
    }
}

private const val CONTACT_US_EMAIL = "akug53040@gmail.com"

private val LogoutRed = Color(0xFFE53935)
