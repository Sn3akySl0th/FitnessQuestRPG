package com.fitnessquest.rpg.ui.components

import androidx.core.net.toUri

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitnessquest.rpg.data.wear.WearApkInstaller
import kotlinx.coroutines.launch

@Composable
fun WearInstallDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val installer = remember { WearApkInstaller(context) }
    val scope = rememberCoroutineScope()

    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("5555") }
    var busy by remember { mutableStateOf(value = false) }
    var status by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var alreadyInstalled by remember { mutableStateOf(false) }
    var apkReady by remember { mutableStateOf(false) }
    var diagnosis by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        alreadyInstalled = installer.isWearAppInstalled()
        apkReady = installer.hasEmbeddedApk()
        status = when {
            alreadyInstalled -> "Watch app already detected on a paired Wear device."
            !apkReady -> "Wear APK is not embedded in this build yet. Rebuild the phone app first."
            else -> null
        }
        success = alreadyInstalled
    }

    LaunchedEffect(ip) {
        if (ip.length >= 7) {
            diagnosis = installer.diagnose(ip)
            if (installer.isPortOpen(ip, 41825) && !installer.isPortOpen(ip, 5555)) {
                port = "41825"
            } else if (installer.isPortOpen(ip, 5555)) {
                port = "5555"
            }
        } else {
            diagnosis = null
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Install on watch") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "We checked your watch: port 5555 is usually closed; Wireless debugging (e.g. 41825) needs pairing. " +
                        "Recommended path: Share Wear APK → Wear Installer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent.createChooser(installer.shareApkIntent(), "Share Wear APK")
                            )
                        }.onFailure {
                            status = it.message
                            success = false
                        }
                    },
                    enabled = apkReady && !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Share Wear APK") }

                OutlinedButton(
                    onClick = {
                        busy = true
                        status = null
                        success = false
                        scope.launch {
                            val result = installer.saveApkToDownloads()
                            success = result.isSuccess
                            status = result.fold(
                                onSuccess = { it },
                                onFailure = { it.message ?: "Could not save APK." }
                            )
                            busy = false
                        }
                    },
                    enabled = apkReady && !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save APK to Downloads") }

                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(installer.openWearInstallerPlayStoreIntent())
                        }.onFailure {
                            // Fallback to browser Play URL
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://play.google.com/store/apps/details?id=org.freepoc.wearinstaller2".toUri()
                                )
                            )
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Get Wear Installer") }

                Text(
                    "Wear Installer steps: enable Wireless debugging on the watch, open Wear Installer, " +
                        "enter the watch IP, then pick the shared FitnessRPG Wear APK.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Direct install (only if port 5555 is open)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it.trim() },
                    label = { Text("Watch IP") },
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter(Char::isDigit).take(5) },
                    label = { Text("ADB port") },
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                diagnosis?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (busy) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Working…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                status?.let { msg ->
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && apkReady && ip.isNotBlank() && (port.toIntOrNull() != null),
                onClick = {
                    busy = true
                    status = null
                    success = false
                    scope.launch {
                        val result = installer.install(ip, port.toIntOrNull() ?: 5555)
                        success = result.isSuccess
                        status = result.fold(
                            onSuccess = { it },
                            onFailure = { it.message ?: "Install failed." }
                        )
                        if (result.isSuccess) alreadyInstalled = true
                        busy = false
                    }
                }
            ) { Text("Try direct install") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Close") }
        }
    )
}
