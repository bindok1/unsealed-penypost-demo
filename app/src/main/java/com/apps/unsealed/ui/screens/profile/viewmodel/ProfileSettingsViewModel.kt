package com.apps.unsealed.ui.screens.profile.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.imageLoader
import com.apps.unsealed.feature.auth.data.AuthRepository
import com.apps.unsealed.feature.auth.data.AuthResult
import com.apps.unsealed.feature.auth.data.PatchMeRequest
import com.apps.unsealed.feature.draft.data.DraftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import javax.inject.Inject

@HiltViewModel
class ProfileSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val draftRepository: DraftRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _cacheSizeText = MutableStateFlow("0 B")
    val cacheSizeText: StateFlow<String> = _cacheSizeText.asStateFlow()

    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache: StateFlow<Boolean> = _isClearingCache.asStateFlow()

    /** Local copy fetched/updated independently of [AuthViewModel]'s shared
     * `uiState` — same per-screen pattern as `StampsViewModel.userEnergy`,
     * avoids relying on `AuthViewModel.patchMe`'s success path (which only
     * invokes its `onSuccess` callback and doesn't restore `Authenticated`
     * on its own, fine for onboarding's navigate-away call sites but not for
     * a screen that stays put). */
    private val _notifyHourPref = MutableStateFlow<String?>(null)
    val notifyHourPref: StateFlow<String?> = _notifyHourPref.asStateFlow()

    init {
        refreshCacheSize()
        viewModelScope.launch {
            (authRepository.fetchMe() as? AuthResult.Success)?.data?.notifyHourPref?.let {
                _notifyHourPref.value = it
            }
        }
    }

    /** Patches `notify_hour_pref` — see `docs/be/daily_penpal_stack.md` §3. */
    fun updateNotifyHourPref(value: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = authRepository.patchMe(PatchMeRequest(notifyHourPref = value))) {
                is AuthResult.Success -> _notifyHourPref.value = result.data.notifyHourPref
                is AuthResult.Error -> onError(result.message)
            }
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val formatted = withContext(Dispatchers.IO) {
                formatBytes(calculateCacheBytes())
            }
            _cacheSizeText.value = formatted
        }
    }

    fun clearCache(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isClearingCache.value = true
            withContext(Dispatchers.IO) {
                // Clear app cache directories
                runCatching {
                    context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                    context.codeCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                    context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                }

                // Clear Coil image cache
                runCatching {
                    context.imageLoader.diskCache?.clear()
                    context.imageLoader.memoryCache?.clear()
                }

                // Wiping drafts as part of cache clearing
                draftRepository.clearAll()
            }
            _isClearingCache.value = false
            refreshCacheSize()
            onComplete()
        }
    }

    private fun calculateCacheBytes(): Long {
        var total = 0L
        fun addDir(dir: File?) {
            if (dir == null || !dir.exists()) return
            dir.walkTopDown().forEach { file ->
                if (file.isFile) total += file.length()
            }
        }
        addDir(context.cacheDir)
        addDir(context.codeCacheDir)
        addDir(context.externalCacheDir)
        return total
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val formatted = DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble()))
        return "$formatted ${units[digitGroups.coerceIn(0, units.size - 1)]}"
    }
}
