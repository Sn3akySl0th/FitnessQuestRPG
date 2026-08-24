package com.fitnessquest.rpg.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import com.fitnessquest.rpg.util.ParentalManagementHelper
import kotlinx.coroutines.launch

@Composable
fun PermissionsConsolidator(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val container = (context.applicationContext as FitQuestApp).container
    val scope = rememberCoroutineScope()

    val isManagedDevice = remember { ParentalManagementHelper.isDeviceManagedOrRestricted(context) }
    
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

    val hcStatus = remember { container.healthConnect.sdkStatus() }
    val isHealthUnavailable = hcStatus != 1 // 1 = SDK_AVAILABLE

    // If all are granted or unavailable by policy, we can auto-dismiss or show a "Done" button.
    val allDone = activityGranted && notificationGranted && (healthGranted || isHealthUnavailable)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NightBg.copy(alpha = 0.95f))
            .padding(20.dp),
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
                modifier = Modifier.size(44.dp)
            )
            
            Text(
                "Hero's Permissions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            // Parental Controls / Supervised Device Notice Card
            if (isManagedDevice || isHealthUnavailable) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🛡️", fontSize = 20.sp)
                            Text(
                                "Parental Controls / Supervised Device",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Gold
                            )
                        }
                        Text(
                            text = "We detected this device is under parental supervision or managed profile (such as Google Family Link).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Google Health Connect is disabled by policy on supervised accounts.\n" +
                                   "• Physical Activity & Notifications can be allowed by a parent in the Google Family Link app under FitQuest permissions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "✨ You can continue right now — FitQuest will use built-in step sensors and manual tracking!",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Gold
                        )
                    }
                }
            } else {
                Text(
                    "You've returned to the realm! Enable these essential powers to ensure your journey is tracked correctly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

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

            if (!checkingHealth && !isHealthUnavailable) {
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

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (allDone || isManagedDevice) Gold else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (allDone) "Enter Training Grounds"
                           else if (isManagedDevice) "Continue with Built-in Sensors"
                           else "I'll do this later",
                    color = if (allDone || isManagedDevice) NightBg else Color.White,
                    fontWeight = FontWeight.Bold
                )
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
    return runCatching {
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)
}
