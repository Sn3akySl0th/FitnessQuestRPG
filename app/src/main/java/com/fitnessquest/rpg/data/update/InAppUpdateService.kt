package com.fitnessquest.rpg.data.update

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class InAppUpdateStatus {
    data object Idle : InAppUpdateStatus()
    data object Checking : InAppUpdateStatus()
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : InAppUpdateStatus()
    data object Downloaded : InAppUpdateStatus()
    data class Failed(val error: String) : InAppUpdateStatus()
}

data class AppUpdateData(
    val isAvailable: Boolean,
    val isFlexibleAllowed: Boolean,
    val clientStalenessDays: Int? = null,
    val updatePriority: Int = 0,
    val rawInfo: AppUpdateInfo? = null,
)

data class UpdateInstallState(
    val status: Int,
    val bytesDownloaded: Long,
    val totalBytesToDownload: Long,
)

interface AppUpdateClient {
    suspend fun getAppUpdateInfo(): Result<AppUpdateData>
    fun startFlexibleUpdate(
        data: AppUpdateData,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ): Result<Unit>
    fun registerInstallListener(listener: (UpdateInstallState) -> Unit)
    fun unregisterInstallListener(listener: (UpdateInstallState) -> Unit)
    fun completeUpdate(): Result<Unit>
}

class PlayAppUpdateClient(context: Context) : AppUpdateClient {
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
    private val listenersMap = mutableMapOf<(UpdateInstallState) -> Unit, InstallStateUpdatedListener>()

    override suspend fun getAppUpdateInfo(): Result<AppUpdateData> = runCatching {
        val info = appUpdateManager.appUpdateInfo.await()
        val isAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
        AppUpdateData(
            isAvailable = isAvailable,
            isFlexibleAllowed = info.isFlexibleUpdateAllowed,
            clientStalenessDays = info.clientVersionStalenessDays(),
            updatePriority = info.updatePriority(),
            rawInfo = info,
        )
    }

    override fun startFlexibleUpdate(
        data: AppUpdateData,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ): Result<Unit> = runCatching {
        val raw = data.rawInfo ?: error("Missing AppUpdateInfo")
        val options = AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE)
        appUpdateManager.startUpdateFlowForResult(raw, launcher, options)
    }

    override fun registerInstallListener(listener: (UpdateInstallState) -> Unit) {
        val playListener = InstallStateUpdatedListener { state: InstallState ->
            listener(
                UpdateInstallState(
                    status = state.installStatus(),
                    bytesDownloaded = state.bytesDownloaded(),
                    totalBytesToDownload = state.totalBytesToDownload(),
                ),
            )
        }
        listenersMap[listener] = playListener
        appUpdateManager.registerListener(playListener)
    }

    override fun unregisterInstallListener(listener: (UpdateInstallState) -> Unit) {
        listenersMap.remove(listener)?.let { playListener ->
            appUpdateManager.unregisterListener(playListener)
        }
    }

    override fun completeUpdate(): Result<Unit> = runCatching {
        appUpdateManager.completeUpdate()
    }
}

class InAppUpdateService(
    private val client: AppUpdateClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) {
    private val _updateStatus = MutableStateFlow<InAppUpdateStatus>(InAppUpdateStatus.Idle)
    val updateStatus: StateFlow<InAppUpdateStatus> = _updateStatus.asStateFlow()

    private val installListener: (UpdateInstallState) -> Unit = { state ->
        when (state.status) {
            InstallStatus.DOWNLOADING -> {
                _updateStatus.value = InAppUpdateStatus.Downloading(
                    bytesDownloaded = state.bytesDownloaded,
                    totalBytes = state.totalBytesToDownload,
                )
            }
            InstallStatus.DOWNLOADED -> {
                _updateStatus.value = InAppUpdateStatus.Downloaded
            }
            InstallStatus.FAILED -> {
                _updateStatus.value = InAppUpdateStatus.Failed("Download failed")
            }
            InstallStatus.CANCELED -> {
                _updateStatus.value = InAppUpdateStatus.Idle
            }
            else -> Unit
        }
    }

    init {
        client.registerInstallListener(installListener)
    }

    fun checkForUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        if (_updateStatus.value is InAppUpdateStatus.Downloading || _updateStatus.value is InAppUpdateStatus.Downloaded) {
            return
        }

        _updateStatus.value = InAppUpdateStatus.Checking
        scope.launch {
            client.getAppUpdateInfo()
                .onSuccess { updateData ->
                    if (updateData.isAvailable && updateData.isFlexibleAllowed) {
                        client.startFlexibleUpdate(updateData, launcher)
                            .onFailure { error ->
                                _updateStatus.value = InAppUpdateStatus.Failed(error.message ?: "Failed to start update")
                            }
                    } else {
                        _updateStatus.value = InAppUpdateStatus.Idle
                    }
                }
                .onFailure { error ->
                    _updateStatus.value = InAppUpdateStatus.Failed(error.message ?: "Update check failed")
                }
        }
    }

    fun onResume() {
        scope.launch {
            client.getAppUpdateInfo().onSuccess { data ->
                val raw = data.rawInfo
                if (raw?.installStatus() == InstallStatus.DOWNLOADED) {
                    _updateStatus.value = InAppUpdateStatus.Downloaded
                }
            }
        }
    }

    fun completeUpdate() {
        if (_updateStatus.value !is InAppUpdateStatus.Downloaded) {
            return
        }
        client.completeUpdate().onFailure { error ->
            _updateStatus.value = InAppUpdateStatus.Failed(error.message ?: "Could not complete update")
        }
    }

    fun onDestroy() {
        client.unregisterInstallListener(installListener)
    }
}
