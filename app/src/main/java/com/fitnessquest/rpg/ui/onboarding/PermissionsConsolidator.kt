package com.fitnessquest.rpg.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.fitnessquest.rpg.FitQuestApp
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightBg
import kotlinx.coroutines.launch

@Composable
fun PermissionsConsolidator(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val container = (context.applicationContext as FitQuestApp).container
    val scope = rememberCoroutineScope()
    
    var activityGranted by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= 29) checkPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
            else true
        )
    }

    var notificationGranted by remember { 
        mutableStateOf(if (Build.VERSION.SDK_INT >= 33) checkPermission(context, Manifest.permission.POST_NOTIFICATIONS) else true) 
    }
    var healthGranted by remember { mutableStateOf(false) }
    var checkingHealth by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        healthGranted = container.healthConnect.hasCoreReadPermissions()
        checkingHealth = false
    }

    val activityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        activityGranted = granted
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationGranted = granted
    }
    val healthLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        scope.launch { healthGranted = container.healthConnect.hasCoreReadPermissions() }
    }

    // If all are granted, we can auto-dismiss or show a "Done" button.
    val allDone = activityGranted && notificationGranted && (healthGranted || container.healthConnect.sdkStatus() != 1)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NightBg.copy(alpha = 0.95f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.Security,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                "Hero's Permissions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            
            Text(
                "You've returned to the realm! Enable these essential powers to ensure your journey is tracked correctly.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            PermissionRow(
                title = "Passive Travel",
                description = "Uses steps to move your hero across biomes while the app is closed.",
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                granted = activityGranted,

                onEnable = {
                    if (Build.VERSION.SDK_INT >= 29) {
                        activityLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    } else {
                        activityGranted = true
                    }
                }
            )


            if (Build.VERSION.SDK_INT >= 33) {
                PermissionRow(
                    title = "Battle Alerts",
                    description = "Notifies you when rest timers end or when raid bosses are active.",
                    icon = Icons.Filled.Notifications,
                    granted = notificationGranted,
                    onEnable = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
            }

            if (!checkingHealth) {
                PermissionRow(
                    title = "Health Sanctuary",
                    description = "Syncs height, weight, and heart rate for accurate RPG stats.",
                    icon = Icons.Filled.Favorite,
                    granted = healthGranted,
                    onEnable = {
                        scope.launch {
                            if (!container.healthConnect.hasCoreReadPermissions()) {
                                healthLauncher.launch(container.healthConnect.permissions.toTypedArray())
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = if (allDone) Gold else MaterialTheme.colorScheme.primary)
            ) {
                Text(if (allDone) "Enter Training Grounds" else "I'll do this later", color = if (allDone) NightBg else Color.White)
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    icon: ImageVector,
    granted: Boolean,
    onEnable: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (granted) Color(0xFF4CAF50).copy(alpha = 0.1f) else Gold.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (granted) Color(0xFF4CAF50) else Gold,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (granted) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Granted", tint = Color(0xFF4CAF50))
        } else {
            TextButton(
                onClick = onEnable,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Enable", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun checkPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
