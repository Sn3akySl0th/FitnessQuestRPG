package com.fitnessquest.rpg.data.ai

import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.fitnessquest.rpg.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

data class LocalModelSpec(
    val id: String,
    val displayName: String,
    val version: String,
    val approxSizeMb: Int,
    val downloadUrl: String,
    val description: String = "",
    val expectedBytes: Long? = null,
    val sha256: String = "",
    val format: String = "MediaPipe GenAI .bin",
    val source: LocalModelSource = LocalModelSource.CURATED,
    val requiresAuthToken: Boolean = false,
    val recommended: Boolean = false,
    val enabled: Boolean = true
)

enum class LocalModelSource {
    FITQUEST_HOSTED,
    CURATED,
    CUSTOM
}

object ModelCatalog {
    val HOSTED_MODEL = LocalModelSpec(
        id = "fitquest_recommended",
        displayName = "Fitness Quest RPG Recommended Offline AI",
        version = BuildConfig.FITQUEST_LLM_MODEL_VERSION,
        approxSizeMb = BuildConfig.FITQUEST_LLM_MODEL_SIZE_MB,
        downloadUrl = BuildConfig.FITQUEST_LLM_MODEL_URL,
        description = "One-tap Fitness Quest RPG hosted model for quota-free workout names, coaching, battle narration, and import cleanup.",
        expectedBytes = BuildConfig.FITQUEST_LLM_MODEL_BYTES.takeIf { it > 0L },
        sha256 = BuildConfig.FITQUEST_LLM_MODEL_SHA256,
        source = LocalModelSource.FITQUEST_HOSTED,
        recommended = true,
        enabled = BuildConfig.FITQUEST_LLM_MODEL_URL.isNotBlank() && BuildConfig.FITQUEST_LLM_MODEL_SIZE_MB > 0
    )

    val CURATED_MODELS = emptyList<LocalModelSpec>()

    val BUILTIN_MODELS = buildList {
        if (HOSTED_MODEL.enabled) add(HOSTED_MODEL)
    }

    val hasHostedModel: Boolean get() = HOSTED_MODEL.enabled
}



sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long, val progressPercent: Int) : DownloadState()
    object Ready : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class LocalModelDownloader(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .callTimeout(60, TimeUnit.MINUTES)
        .build()
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    private var activeCall: okhttp3.Call? = null

    fun getModelFile(modelId: String): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val modelsDir = File(baseDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()
        val cleanId = modelId.lowercase().replace(Regex("[^a-z0-9_]"), "_")
        return File(modelsDir, "local_llm_${cleanId}.bin")
    }

    fun getDownloadedSizeMb(spec: LocalModelSpec): Int {
        val file = getModelFile(spec.id)
        return if (file.exists()) (file.length() / (1024 * 1024L)).toInt() else 0
    }

    fun isModelReady(spec: LocalModelSpec): Boolean {
        val file = getModelFile(spec.id)
        if (!file.exists()) return false
        spec.expectedBytes?.let { expected ->
            if (file.length() != expected) return false
        }
        if (spec.sha256.isNotBlank() && !file.sha256Matches(spec.sha256)) return false
        // Require 99% of expected size to consider it valid weights.
        val minSizeBytes = (spec.approxSizeMb * 0.99f * 1024 * 1024L).toLong()
        return file.length() >= minSizeBytes
    }

    fun migrateFromPlayAsset(playAssetFile: File?): Boolean {
        if (playAssetFile == null || !playAssetFile.exists() || playAssetFile.length() < 100_000_000L) return false
        val target = getModelFile("gemma_2b_it")
        if (target.exists() && target.length() >= playAssetFile.length() * 0.95f) return true
        return try {
            val temp = File(target.parentFile, "${target.name}.tmp_migrated")
            playAssetFile.copyTo(temp, overwrite = true)
            temp.renameTo(target)
        } catch (e: Exception) {
            Log.e("FitQuest", "PAD migration failed: ${e.message}", e)
            false
        }
    }

    suspend fun startDownload(spec: LocalModelSpec, authToken: String = "") {
        startDownloadUrl(spec.id, spec.downloadUrl, spec.approxSizeMb, authToken, spec)
    }

    suspend fun startDownloadUrl(
        modelId: String,
        downloadUrl: String,
        approxSizeMb: Int,
        authToken: String = "",
        spec: LocalModelSpec? = null
    ): Unit = withContext(Dispatchers.IO) {

        if (_downloadState.value is DownloadState.Downloading) return@withContext
        if (downloadUrl.isBlank()) {
            _downloadState.value = DownloadState.Error("No model download URL is configured for this build.")
            return@withContext
        }

        val targetFile = getModelFile(modelId)
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FitQuest:ModelDownload")

        try {
            wakeLock?.acquire(30 * 60 * 1000L) // 30 min safety lock

            val requestBuilder = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "FitQuest-Android-Downloader")

            if (existingBytes > 0) {
                requestBuilder.header("Range", "bytes=$existingBytes-")
            }

            if (authToken.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer ${authToken.trim()}")
            }

            val call = httpClient.newCall(requestBuilder.build())
            activeCall = call
            Log.d("FitQuest", "Starting download from $downloadUrl")
            var response = call.execute()

            // Handle multi-hop redirects (e.g. Host -> CDN -> Pre-signed S3/Cloudflare)
            var currentUrl = downloadUrl
            var redirectCount = 0
            val maxRedirects = 10

            while (response.isRedirect && redirectCount < maxRedirects) {
                redirectCount++
                val location = response.header("Location") ?: break
                val newHttpUrl = currentUrl.toHttpUrlOrNull()?.resolve(location) ?: break
                val newUrl = newHttpUrl.toString()
                response.close()

                val originalHost = currentUrl.toHttpUrlOrNull()?.host
                val newHost = newHttpUrl.host
                val isSameHost = originalHost.equals(newHost, ignoreCase = true)

                val redirectRequestBuilder = Request.Builder()
                    .url(newUrl)
                    .header("User-Agent", "FitQuest-Android-Downloader")

                if (existingBytes > 0) {
                    redirectRequestBuilder.header("Range", "bytes=$existingBytes-")
                }
                if (isSameHost && authToken.isNotBlank()) {
                    redirectRequestBuilder.header("Authorization", "Bearer ${authToken.trim()}")
                }

                currentUrl = newUrl
                val nextCall = httpClient.newCall(redirectRequestBuilder.build())
                activeCall = nextCall
                response = nextCall.execute()
            }

            if (!response.isSuccessful && response.code != 416) {

                val errorMsg = when (response.code) {
                    401 -> "HTTP 401: Unauthorized access to model URL."
                    403 -> "HTTP 403: Access forbidden to model URL."
                    404 -> "HTTP 404: Model file not found at URL."
                    else -> "HTTP ${response.code}: Could not download model weights."
                }
                Log.e("FitQuest", "Download failed: $errorMsg")
                _downloadState.value = DownloadState.Error(errorMsg)
                return@withContext
            }


            val body = response.body
            if (body == null) {
                _downloadState.value = DownloadState.Error("Empty download response from server.")
                return@withContext
            }

            val isPartial = response.code == 206
            val appendMode = isPartial && existingBytes > 0
            val totalContentLength = body.contentLength()
            val totalBytes = if (isPartial) (existingBytes + totalContentLength) else if (totalContentLength > 0) totalContentLength else approxSizeMb * 1024 * 1024L

            var downloadedBytes = if (appendMode) existingBytes else 0L
            _downloadState.value = DownloadState.Downloading(downloadedBytes, totalBytes, ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 99))

            body.byteStream().use { input ->
                FileOutputStream(tempFile, appendMode).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        val percent = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 99)
                        _downloadState.value = DownloadState.Downloading(downloadedBytes, totalBytes, percent)
                    }
                }
            }

            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            val validationIssue = spec?.validateDownloadedFile(targetFile)
            if (validationIssue != null) {
                targetFile.delete()
                _downloadState.value = DownloadState.Error(validationIssue)
                return@withContext
            }
            _downloadState.value = DownloadState.Ready
        } catch (e: Exception) {
            if (activeCall?.isCanceled() == true) {
                _downloadState.value = DownloadState.Idle
            } else {
                _downloadState.value = DownloadState.Error("Download interrupted: ${e.localizedMessage ?: "Network timeout"}. Resume anytime!")
            }
        } finally {
            activeCall = null
            if (wakeLock?.isHeld == true) {
                try { wakeLock.release() } catch (_: Exception) {}
            }
        }
    }

    fun cancelDownload() {
        activeCall?.cancel()
        _downloadState.value = DownloadState.Idle
    }

    fun deleteModel(modelId: String): Boolean {
        cancelDownload()
        val file = getModelFile(modelId)
        val temp = File(file.parentFile, "${file.name}.tmp")
        if (temp.exists()) temp.delete()
        val deleted = if (file.exists()) file.delete() else true
        _downloadState.value = DownloadState.Idle
        return deleted
    }

    private fun LocalModelSpec.validateDownloadedFile(file: File): String? {
        if (!file.exists()) return "Downloaded model file was not created."
        expectedBytes?.let { expected ->
            if (file.length() != expected) {
                return "Downloaded model size did not match the expected version. Try again or update the model URL."
            }
        }
        if (sha256.isNotBlank() && !file.sha256Matches(sha256)) {
            return "Downloaded model failed integrity verification. The file was removed for safety."
        }
        val minSizeBytes = (approxSizeMb * 0.99f * 1024 * 1024L).toLong()
        if (file.length() < minSizeBytes) {
            return "Downloaded model was smaller than expected. Try again to resume the download."
        }
        return null
    }

    private fun File.sha256Matches(expected: String): Boolean {
        val normalizedExpected = expected.trim().lowercase(Locale.US)
        if (normalizedExpected.isBlank()) return true
        return sha256().equals(normalizedExpected, ignoreCase = true)
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
