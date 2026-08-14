package com.fitnessquest.rpg.data.ai

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.assetpacks.AssetPackLocation
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.AssetPackException
import com.google.android.play.core.assetpacks.model.AssetPackErrorCode
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.io.File

data class PlayAssetModelState(
    val installed: Boolean = false,
    val downloading: Boolean = false,
    val progressPercent: Int = 0,
    val message: String = "",
    val canRetry: Boolean = false,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val statusCode: Int = AssetPackStatus.UNKNOWN,
    val errorCode: Int = AssetPackErrorCode.NO_ERROR,
)

class PlayAssetModelProvider(private val context: Context) {
    private val manager = AssetPackManagerFactory.getInstance(context)

    @Volatile
    private var cachedModelFile: File? = null
    @Volatile
    private var lastFileCheckTime: Long = 0L

    @Synchronized
    fun getModelFile(forceRefresh: Boolean = false): File? {
        val now = System.currentTimeMillis()
        if (!forceRefresh && (now - lastFileCheckTime) < 10_000L) {
            cachedModelFile?.takeIf { it.exists() && it.length() >= MIN_MODEL_BYTES }?.let { return it }
        }

        val location = runCatching { manager.getPackLocation(PACK_NAME) }.getOrNull()
        val file = if (location != null) {
            location.findAsset(MODEL_ASSET_PATH)?.takeIf { it.exists() && it.length() >= MIN_MODEL_BYTES }
                ?: location.assetsPath()?.let { File(it) }?.takeIf { it.exists() }?.let { assetsDir ->
                    assetsDir.walkTopDown().firstOrNull { f ->
                        f.isFile && f.extension.equals("bin", ignoreCase = true) && f.length() >= MIN_MODEL_BYTES
                    }
                }
        } else null

        cachedModelFile = file
        lastFileCheckTime = now
        return file
    }

    fun isModelReady(): Boolean = getModelFile() != null

    private val _state = MutableStateFlow(currentState())
    val state: StateFlow<PlayAssetModelState> = _state.asStateFlow()

    private val listener = AssetPackStateUpdateListener { packState ->
        if (packState.name() != PACK_NAME) return@AssetPackStateUpdateListener
        val newState = packState.toModelState()
        if (_state.value != newState) {
            _state.value = newState
        }
    }

    init {
        runCatching { manager.registerListener(listener) }
    }

    fun observeState(): StateFlow<PlayAssetModelState> = state

    suspend fun requestModel(activity: Activity? = null): PlayAssetModelState {
        val readyBefore = isModelReady()
        if (readyBefore) {
            val readyState = PlayAssetModelState(installed = true, downloading = false, progressPercent = 100, message = "Official Play Store model is installed.")
            _state.value = readyState
            return readyState
        }

        _state.value = PlayAssetModelState(
            installed = false,
            downloading = true,
            progressPercent = 0,
            message = "Connecting to Google Play..."
        )

        val result = runCatching {
            manager.fetch(listOf(PACK_NAME)).await()
            pollUntilSettled(activity)
        }.getOrElse { error ->
            val playErrorCode = (error as? AssetPackException)?.errorCode ?: AssetPackErrorCode.NO_ERROR
            PlayAssetModelState(
                installed = isModelReady(),
                downloading = false,
                progressPercent = 0,
                message = requestFailureMessage(playErrorCode),
                canRetry = true,
                statusCode = AssetPackStatus.FAILED,
                errorCode = playErrorCode,
            )
        }

        _state.value = result
        return result
    }

    suspend fun showConsentDialog(activity: Activity): Boolean {
        return runCatching {
            Log.d("FitQuest", "Triggering Play Store consent dialog on activity $activity")
            val status = queryPackState()?.statusCode ?: _state.value.statusCode
            if (status == AssetPackStatus.REQUIRES_USER_CONFIRMATION) {
                manager.showConfirmationDialog(activity).await()
            } else {
                manager.showCellularDataConfirmation(activity).await()
            }
            refreshState()
            true
        }.onFailure { e ->
            Log.e("FitQuest", "Consent dialog primary call failed", e)
            runCatching {
                manager.showConfirmationDialog(activity).await()
                refreshState()
            }
        }.getOrDefault(false)
    }

    suspend fun showCellularConfirmation(activity: Activity): Boolean = showConsentDialog(activity)

    suspend fun refreshState(): PlayAssetModelState {

        val refreshed = queryPackState() ?: currentState()
        _state.value = refreshed
        return refreshed
    }

    suspend fun removeModel(): Boolean {
        runCatching { manager.removePack(PACK_NAME).await() }
        cachedModelFile = null
        lastFileCheckTime = 0L
        val ready = isModelReady()
        _state.value = currentState(if (!ready) "Model removed." else "Could not remove model.")
        return !ready
    }

    private fun currentState(message: String = ""): PlayAssetModelState {
        val ready = isModelReady()
        return PlayAssetModelState(
            installed = ready,
            downloading = false,
            progressPercent = if (ready) 100 else 0,
            message = message
        )
    }

    private suspend fun pollUntilSettled(activity: Activity?): PlayAssetModelState {
        var lastStatus = AssetPackStatus.UNKNOWN
        repeat(120) {
            var queried = queryPackState()
            if (queried != null) {
                if (activity != null) {
                    when (queried.statusCode) {
                        AssetPackStatus.REQUIRES_USER_CONFIRMATION -> {
                            if (lastStatus != AssetPackStatus.REQUIRES_USER_CONFIRMATION) {
                                Log.d("FitQuest", "Requesting PAD Confirmation Dialog")
                                runCatching { manager.showConfirmationDialog(activity).await() }
                                queried = queryPackState() ?: queried
                            }
                        }
                        AssetPackStatus.WAITING_FOR_WIFI -> {
                            if (lastStatus != AssetPackStatus.WAITING_FOR_WIFI) {
                                Log.d("FitQuest", "Requesting PAD Cellular Confirmation")
                                runCatching { manager.showCellularDataConfirmation(activity).await() }
                                queried = queryPackState() ?: queried
                            }
                        }
                    }
                }
                lastStatus = queried.statusCode

                _state.value = queried
                if (!queried.downloading) return queried
            }
            delay(1_000L)
        }
        return queryPackState() ?: PlayAssetModelState(
            installed = isModelReady(),
            downloading = false,
            progressPercent = 0,
            message = "Google Play did not report download progress. Close and reopen Settings, or try again on Wi-Fi.",
            canRetry = true
        )
    }

    private suspend fun queryPackState(): PlayAssetModelState? {
        val states = runCatching { manager.getPackStates(listOf(PACK_NAME)).await() }.getOrNull() ?: return null
        val packState = states.packStates()[PACK_NAME] ?: return currentState("Offline AI model is not installed.")
        return packState.toModelState()
    }

    private fun AssetPackState.toModelState(): PlayAssetModelState {
        val status = status()
        val isInstalled = status == AssetPackStatus.COMPLETED && getModelFile(forceRefresh = true) != null
        val isDownloading = status in setOf(
            AssetPackStatus.PENDING,
            AssetPackStatus.DOWNLOADING,
            AssetPackStatus.TRANSFERRING,
            AssetPackStatus.WAITING_FOR_WIFI,
            AssetPackStatus.REQUIRES_USER_CONFIRMATION
        )
        val downloaded = bytesDownloaded().coerceAtLeast(0L)
        val total = totalBytesToDownload().coerceAtLeast(0L)
        val progress = when {
            isInstalled -> 100
            status == AssetPackStatus.TRANSFERRING -> transferProgressPercentage().coerceIn(0, 100)
            total > 0L -> ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
            else -> 0
        }
        return PlayAssetModelState(
            installed = isInstalled,
            downloading = isDownloading,
            progressPercent = progress,
            message = statusMessage(status, errorCode()),
            canRetry = status in setOf(AssetPackStatus.FAILED, AssetPackStatus.CANCELED, AssetPackStatus.NOT_INSTALLED),
            downloadedBytes = downloaded,
            totalBytes = total,
            statusCode = status,
            errorCode = errorCode(),
        )
    }

    private fun AssetPackLocation.findAsset(relativePath: String): File? {
        val assetsPath = assetsPath() ?: return null
        return File(assetsPath, relativePath)
    }

    private fun statusMessage(status: Int, errorCode: Int = AssetPackErrorCode.NO_ERROR): String = when (status) {
        AssetPackStatus.PENDING -> "Waiting for Google Play."
        AssetPackStatus.DOWNLOADING -> "Downloading from Google Play."
        AssetPackStatus.TRANSFERRING -> "Installing model."
        AssetPackStatus.COMPLETED -> if (isModelReady()) "Offline AI model installed." else "Asset pack installed, but model file is missing."
        AssetPackStatus.FAILED -> "Download failed: ${errorMessage(errorCode)}"
        AssetPackStatus.CANCELED -> "Google Play model download canceled."
        AssetPackStatus.WAITING_FOR_WIFI -> "Waiting for Wi-Fi."
        AssetPackStatus.REQUIRES_USER_CONFIRMATION -> "Google Play needs confirmation for this large download."
        AssetPackStatus.NOT_INSTALLED -> "Offline AI model is not installed."
        AssetPackStatus.UNKNOWN -> "Google Play does not recognize this asset pack for the installed app version."
        else -> "Unknown Play Asset status ($status)."
    }

    private fun errorMessage(code: Int): String = when (code) {
        AssetPackErrorCode.NO_ERROR -> "no error reported"
        AssetPackErrorCode.APP_UNAVAILABLE -> "this app version is unavailable in Google Play"
        AssetPackErrorCode.PACK_UNAVAILABLE -> "the model pack is not attached to this Play release"
        AssetPackErrorCode.INVALID_REQUEST -> "invalid asset-pack request"
        AssetPackErrorCode.DOWNLOAD_NOT_FOUND -> "Google Play could not find this download"
        AssetPackErrorCode.API_NOT_AVAILABLE -> "Play Asset Delivery is unavailable on this device"
        AssetPackErrorCode.NETWORK_ERROR -> "network error"
        AssetPackErrorCode.ACCESS_DENIED -> "Google Play denied access; check the signed-in Play account"
        AssetPackErrorCode.INSUFFICIENT_STORAGE -> "insufficient device storage"
        AssetPackErrorCode.APP_NOT_OWNED -> "this Google account has not acquired the app from Play"
        AssetPackErrorCode.UNRECOGNIZED_INSTALLATION -> "this app was not installed by Google Play"
        AssetPackErrorCode.INTERNAL_ERROR -> "Google Play internal error"
        else -> "Play error $code"
    }

    private fun requestFailureMessage(code: Int): String = when (code) {
        AssetPackErrorCode.PACK_UNAVAILABLE ->
            "This Play Store release does not include the offline AI pack. Update FitQuest after a model-enabled release is published."
        AssetPackErrorCode.APP_UNAVAILABLE ->
            "This FitQuest version is not available to your Google Play account or testing track."
        AssetPackErrorCode.APP_NOT_OWNED,
        AssetPackErrorCode.UNRECOGNIZED_INSTALLATION ->
            "Install or update FitQuest directly from Google Play with the tester account, then try again."
        AssetPackErrorCode.INSUFFICIENT_STORAGE ->
            "Not enough storage for the offline AI model. Free at least 2 GB and try again."
        AssetPackErrorCode.NETWORK_ERROR ->
            "Google Play could not download the model. Check your connection and try again."
        AssetPackErrorCode.ACCESS_DENIED ->
            "Google Play denied the download. Check that the device is signed into an enrolled tester account."
        AssetPackErrorCode.API_NOT_AVAILABLE ->
            "Play Asset Delivery is unavailable on this device or Google Play needs an update."
        else -> "Google Play could not install the offline AI model: ${errorMessage(code)}."
    }

    companion object {
        const val PACK_NAME = "local_ai_model"
        const val MODEL_ASSET_PATH = "gemma-1.1-2b-it-cpu-int4.bin"
        private const val MIN_MODEL_BYTES = 100L * 1024L * 1024L
    }
}
