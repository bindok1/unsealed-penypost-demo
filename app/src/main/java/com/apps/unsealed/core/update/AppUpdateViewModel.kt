package com.apps.unsealed.core.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.apps.unsealed.BuildConfig
import com.apps.unsealed.core.util.RemoteConfigKeys
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

const val APP_UPDATE_REQUEST_CODE = 9912

sealed interface AppUpdateUiState {
    data object UpToDate : AppUpdateUiState
    data class UpdateAvailable(
        val isMandatory: Boolean,
        val storeUrl: String,
    ) : AppUpdateUiState
}

/**
 * Manages app version checks combining Firebase Remote Config (policy/gatekeeping)
 * and Google Play In-App Updates (background APK download / installation).
 */
@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfig: FirebaseRemoteConfig,
) : ViewModel() {

    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(context)
    }

    private val _updateState = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.UpToDate)
    val updateState: StateFlow<AppUpdateUiState> = _updateState.asStateFlow()

    private val _isUpdateDownloaded = MutableStateFlow(false)
    val isUpdateDownloaded: StateFlow<Boolean> = _isUpdateDownloaded.asStateFlow()

    private var cachedAppUpdateInfo: AppUpdateInfo? = null
    private var dismissedThisSession = false

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            _isUpdateDownloaded.value = true
        }
    }

    init {
        checkVersion()
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            checkVersion()
        }
        setupRealtimeListener()
        checkPlayStoreUpdate()
        try {
            appUpdateManager.registerListener(installStateUpdatedListener)
        } catch (e: Exception) {
            Log.d("AppUpdateViewModel", "Failed to register install listener: ${e.message}")
        }
    }

    private fun checkPlayStoreUpdate() {
        try {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                cachedAppUpdateInfo = info
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    _isUpdateDownloaded.value = true
                }
            }.addOnFailureListener { e ->
                Log.d("AppUpdateViewModel", "Play Store update check: ${e.message}")
            }
        } catch (e: Exception) {
            Log.d("AppUpdateViewModel", "AppUpdateManager not available: ${e.message}")
        }
    }

    fun onResumeCheck(activity: Activity) {
        try {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                cachedAppUpdateInfo = info
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    _isUpdateDownloaded.value = true
                } else if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    // Resume immediate update flow if already triggered
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        activity,
                        AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
                        APP_UPDATE_REQUEST_CODE,
                    )
                }
            }
        } catch (e: Exception) {
            Log.d("AppUpdateViewModel", "onResumeCheck failed: ${e.message}")
        }
    }

    fun checkVersion() {
        val currentVersion = BuildConfig.VERSION_CODE
        val minRequiredVersion = remoteConfig.getLong(RemoteConfigKeys.MIN_REQUIRED_VERSION_CODE).toInt()
        val latestVersion = remoteConfig.getLong(RemoteConfigKeys.LATEST_VERSION_CODE).toInt()
        val storeUrl = remoteConfig.getString(RemoteConfigKeys.UPDATE_PLAY_STORE_URL).ifBlank {
            "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
        }

        when {
            currentVersion < minRequiredVersion -> {
                _updateState.value = AppUpdateUiState.UpdateAvailable(
                    isMandatory = true,
                    storeUrl = storeUrl,
                )
            }
            currentVersion < latestVersion && !dismissedThisSession -> {
                _updateState.value = AppUpdateUiState.UpdateAvailable(
                    isMandatory = false,
                    storeUrl = storeUrl,
                )
            }
            else -> {
                _updateState.value = AppUpdateUiState.UpToDate
            }
        }
    }

    /**
     * Starts the update flow.
     * If Google Play In-App Update is available and supported on this device/account:
     * - Uses [AppUpdateType.IMMEDIATE] for mandatory updates
     * - Uses [AppUpdateType.FLEXIBLE] for soft updates (downloads in the background)
     * Otherwise falls back to launching the Play Store market intent.
     */
    fun startUpdate(activity: Activity, isMandatory: Boolean, fallbackUrl: String) {
        val info = cachedAppUpdateInfo
        val updateType = if (isMandatory) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE

        if (info != null && info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(updateType)) {
            try {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    activity,
                    AppUpdateOptions.defaultOptions(updateType),
                    APP_UPDATE_REQUEST_CODE,
                )
                return
            } catch (e: Exception) {
                Log.w("AppUpdateViewModel", "startUpdateFlowForResult failed, falling back to store URL", e)
            }
        }
        openStore(activity, fallbackUrl)
    }

    fun completeUpdate() {
        try {
            appUpdateManager.completeUpdate()
        } catch (e: Exception) {
            Log.w("AppUpdateViewModel", "Failed to complete update", e)
        }
    }

    fun dismissSoftUpdate() {
        dismissedThisSession = true
        _updateState.value = AppUpdateUiState.UpToDate
    }

    fun openStore(context: Context, fallbackUrl: String) {
        val marketUri = Uri.parse("market://details?id=${context.packageName}")
        val marketIntent = Intent(Intent.ACTION_VIEW, marketUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(marketIntent)
        } catch (e: ActivityNotFoundException) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun setupRealtimeListener() {
        try {
            remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    if (configUpdate.updatedKeys.contains(RemoteConfigKeys.MIN_REQUIRED_VERSION_CODE) ||
                        configUpdate.updatedKeys.contains(RemoteConfigKeys.LATEST_VERSION_CODE) ||
                        configUpdate.updatedKeys.contains(RemoteConfigKeys.UPDATE_PLAY_STORE_URL)
                    ) {
                        remoteConfig.activate().addOnCompleteListener {
                            checkVersion()
                            checkPlayStoreUpdate()
                        }
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.w("AppUpdateViewModel", "Remote config real-time listener error: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.w("AppUpdateViewModel", "Failed to register real-time update listener", e)
        }
    }
}
