package com.fitnessquest.rpg.data.wear

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.fitnessquest.shared.wear.WearCapabilities
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Sideloads the embedded Wear OS APK.
 *
 * Modern Wear watches expose Wireless debugging (e.g. port 41825) with TLS pairing,
 * not classic Debug-over-Wi‑Fi :5555. Direct install works on :5555 when open;
 * otherwise export the APK for Wear Installer (handles pairing).
 */
class WearApkInstaller(context: Context) {

    private val app = context.applicationContext

    suspend fun isWearAppInstalled(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val cap = Wearable.getCapabilityClient(app)
                .getCapability(WearCapabilities.WEAR, CapabilityClient.FILTER_ALL)
                .await()
            cap.nodes.isNotEmpty()
        }.getOrDefault(false)
    }

    suspend fun hasEmbeddedApk(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            app.assets.open(ASSET_PATH).use { true }
        }.getOrDefault(false)
    }

    suspend fun isPortOpen(host: String, port: Int, timeoutMs: Int = 1500): Boolean =
        withContext(Dispatchers.IO) {
            if (host.isBlank()) return@withContext false
            runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host.trim(), port), timeoutMs)
                    true
                }
            }.getOrDefault(false)
        }

    /** Probe common ports and describe what the watch is offering. */
    suspend fun diagnose(host: String): String = withContext(Dispatchers.IO) {
        if (host.isBlank()) return@withContext "Enter the watch IP from Wireless debugging."
        val open5555 = isPortOpen(host, 5555)
        val openWireless = listOf(41825, 37100, 37000, 44000, 5555)
            .filter { it != 5555 }
            .firstOrNull { isPortOpen(host, it) }
        when {
            open5555 -> "Classic ADB is open on 5555 — you can Install directly."
            openWireless != null ->
                "Wireless debugging is open on $openWireless (5555 is closed). " +
                    "Use Share Wear APK + Wear Installer, or enable Debug over Wi‑Fi if your watch still has it."
            else ->
                "No ADB port is open on $host. On the watch turn on Wireless debugging (or Debug over Wi‑Fi) and retry."
        }
    }

    suspend fun install(host: String, port: Int = DEFAULT_PORT): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(host.isNotBlank()) { "Enter the watch IP address." }
                if (!isPortOpen(host, port)) {
                    error(
                        if (port == 5555) {
                            "Port 5555 is closed. Your watch is on Wireless debugging — tap Share Wear APK and install with Wear Installer."
                        } else {
                            "Nothing listening on $host:$port. Confirm Wireless debugging is still on, or Share Wear APK for Wear Installer."
                        }
                    )
                }
                if (port > 10_000) {
                    error(
                        "Port $port is Wireless debugging (TLS). This build cannot pair that handshake yet — " +
                            "tap Share Wear APK and open it in Wear Installer (Play Store)."
                    )
                }
                val apk = extractApk()
                val keyPair = loadOrCreateKeyPair()
                Dadb.create(host.trim(), port, keyPair).use { dadb ->
                    dadb.install(apk)
                }
                "FitnessRPG is on your watch. Open it from the watch app drawer."
            }.recoverCatching { e ->
                throw IllegalStateException(e.message ?: "Install failed.", e)
            }
        }

    /** Writes the embedded APK to cache and returns a share/view Intent. */
    fun shareApkIntent(): Intent {
        val apk = extractApkSync()
        val uri: Uri = FileProvider.getUriForFile(
            app,
            "${app.packageName}.fileprovider",
            apk
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "FitnessRPG Wear APK — open in Wear Installer after enabling Wireless debugging on the watch."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** Saves APK into public Downloads for Wear Installer → Custom APK. */
    suspend fun saveApkToDownloads(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val apk = extractApkSync()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "fitnessrpg-wear.apk")
                    put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = app.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("Could not create Downloads entry.")
                resolver.openOutputStream(uri)?.use { out ->
                    apk.inputStream().use { it.copyTo(out) }
                } ?: error("Could not write APK.")
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                val dest = File(dir, "fitnessrpg-wear.apk")
                apk.copyTo(dest, overwrite = true)
            }
            "Saved fitnessrpg-wear.apk to Downloads. In Wear Installer choose Custom APK."
        }
    }

    fun openWearInstallerPlayStoreIntent(): Intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=org.freepoc.wearinstaller2")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun extractApk(): File = extractApkSync()

    private fun extractApkSync(): File {
        val out = File(app.cacheDir, "fitnessrpg-wear.apk")
        app.assets.open(ASSET_PATH).use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        check(out.length() > 10_000L) { "Embedded Wear APK is empty or missing." }
        return out
    }

    private fun loadOrCreateKeyPair(): AdbKeyPair {
        val dir = File(app.filesDir, "adb").apply { mkdirs() }
        val privateKey = File(dir, "adbkey")
        val publicKey = File(dir, "adbkey.pub")
        if (!privateKey.exists() || !publicKey.exists()) {
            AdbKeyPair.generate(privateKey, publicKey)
        }
        return AdbKeyPair.read(privateKey, publicKey)
    }

    companion object {
        const val ASSET_PATH = "wear/fitnessrpg-wear.apk"
        const val DEFAULT_PORT = 5555
    }
}
