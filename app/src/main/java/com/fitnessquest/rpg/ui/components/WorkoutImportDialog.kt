package com.fitnessquest.rpg.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fitnessquest.rpg.data.importexport.ImportedWorkout
import com.fitnessquest.rpg.data.importexport.ImportedWorkoutKind
import com.fitnessquest.rpg.data.importexport.WorkoutImportService
import com.fitnessquest.rpg.ui.appContainer
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightBg
import kotlinx.coroutines.launch

@Composable
fun WorkoutImportDialog(
    initialApiKey: String = "",
    onApiKeyChange: (String) -> Unit = {},
    existingWorkoutNames: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onImportWorkouts: (List<ImportedWorkout>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val normalizedExisting = remember(existingWorkoutNames) { existingWorkoutNames.map { it.trim().lowercase() }.toSet() }

    var activeTab by remember { mutableIntStateOf(0) }
    var apiKey by remember(initialApiKey) { mutableStateOf(initialApiKey) }


    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var parsedWorkouts by remember { mutableStateOf<List<ImportedWorkout>>(emptyList()) }
    var selectedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var diagnosticReport by remember { mutableStateOf<String?>(null) }

    // Android SAF CSV File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            loading = true
            errorMessage = null
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val result = WorkoutImportService.parseCsvStream(inputStream)
                        result.fold(
                            onSuccess = { workouts ->
                                parsedWorkouts = workouts
                                selectedIndices = workouts.indices.filter { idx ->
                                    workouts[idx].title.trim().lowercase() !in normalizedExisting
                                }.toSet()
                                loading = false
                            },
                            onFailure = { err ->
                                errorMessage = err.message ?: "Failed to parse CSV file."
                                loading = false
                            }
                        )
                    } else {
                        errorMessage = "Could not open selected CSV file."
                        loading = false
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Error reading CSV file."
                    loading = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(20.dp))
                .background(NightBg)
                .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Import Workouts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Gold
                )

                SecondaryTabRow(selectedTabIndex = activeTab) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = {
                            activeTab = 0
                            errorMessage = null
                        },
                        text = { Text("📁 CSV File") }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = {
                            activeTab = 1
                            errorMessage = null
                        },
                        text = { Text("🔑 Hevy API") }
                    )
                }

                if (activeTab == 0) {
                    // CSV Import Tab
                    SectionCard {
                        Text(
                            "Export your workouts as a CSV from Hevy, Strong, or any standard app, then select the CSV file here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold)
                        ) {
                            Icon(Icons.Filled.FolderZip, contentDescription = null, tint = NightBg)
                            Spacer(Modifier.width(8.dp))
                            Text("Choose CSV File", color = NightBg, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val container = (context.applicationContext as com.fitnessquest.rpg.FitQuestApp).container
                                    val deleted = container.repository.cleanUpFragmentedSingleExerciseTemplates()
                                    errorMessage = "Cleaned up $deleted fragmented CSV routine templates!"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🧹 Clean Up Fragmented CSV Routines", fontSize = 12.sp)
                        }
                    }
                } else {
                    // Hevy API Tab
                    SectionCard {
                        Text(
                            "Enter your Hevy API Key to pull routines into Training as Fitness Quest RPG quest names and completed workouts into History.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { 
                                apiKey = it
                                onApiKeyChange(it)
                            },
                            label = { Text("Hevy API Key") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    loading = true
                                    errorMessage = null
                                    scope.launch {
                                        WorkoutImportService.fetchHevyRoutines(apiKey).fold(
                                            onSuccess = { routines ->
                                                parsedWorkouts = routines
                                                selectedIndices = routines.indices.toSet()
                                                loading = false
                                            },
                                            onFailure = { err ->
                                                errorMessage = err.message ?: "Failed to fetch Hevy routines."
                                                loading = false
                                            }
                                        )
                                    }
                                },
                                enabled = apiKey.isNotBlank() && !loading,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Gold)
                            ) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = NightBg)
                                Spacer(Modifier.width(6.dp))
                                Text("Fetch Routines", color = NightBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    loading = true
                                    scope.launch {
                                        WorkoutImportService.inspectHevyApi(apiKey).fold(
                                            onSuccess = { report ->
                                                diagnosticReport = report
                                                loading = false
                                            },
                                            onFailure = { err ->
                                                errorMessage = err.message ?: "Diagnostic failed."
                                                loading = false
                                            }
                                        )
                                    }
                                },
                                enabled = apiKey.isNotBlank() && !loading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.BugReport, contentDescription = null, tint = Gold)
                                Spacer(Modifier.width(6.dp))
                                Text("Test API Key", color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                errorMessage?.let { err ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF7F1D1D).copy(alpha = 0.4f))
                            .border(1.dp, Color(0xFFF87171), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(err, color = Color(0xFFF87171), style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Gold)
                    }
                } else if (parsedWorkouts.isNotEmpty()) {
                    Text(
                        "Found ${parsedWorkouts.size} Items to Import:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(parsedWorkouts) { index, workout ->
                            val isSelected = index in selectedIndices
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable {
                                        selectedIndices = if (isSelected) {
                                            selectedIndices - index
                                        } else {
                                            selectedIndices + index
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedIndices = if (checked) selectedIndices + index else selectedIndices - index
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = NightBg)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val isHistory = workout.kind == ImportedWorkoutKind.HISTORY
                                    val isDuplicate = !isHistory && workout.title.trim().lowercase() in normalizedExisting
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            workout.title,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (isDuplicate) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "(Already Saved)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Gold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        "${if (isHistory) "History session" else "Training quest"} - ${workout.exercises.size} exercises (${workout.exercises.take(3).joinToString { it.name }})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    if (parsedWorkouts.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val toImport = selectedIndices.map { parsedWorkouts[it] }
                                onImportWorkouts(toImport)
                                onDismiss()
                            },
                            enabled = selectedIndices.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold)
                        ) {
                            Text("Import (${selectedIndices.size})", color = NightBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    val activeReport = diagnosticReport
    if (activeReport != null) {
        AlertDialog(
            onDismissRequest = { diagnosticReport = null },
            title = { Text("🔍 Hevy API Diagnostic Report", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        activeReport,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { diagnosticReport = null }) {
                    Text("Close", color = Gold)
                }
            }
        )
    }
}
