package com.fitnessquest.rpg.ui.components

import androidx.compose.foundation.layout.Arrangement
import com.fitnessquest.rpg.domain.Units
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
fun ActiveQuestBanner(
    title: String,
    startedAt: Long,
    pausedAt: Long? = null,
    accumulatedPausedMs: Long = 0L,
    setCount: Int,
    provisionalXp: Int,
    provisionalVolumeKg: Double = 0.0,
    provisionalDistanceKm: Double = 0.0,
    imperial: Boolean = false,
    onResume: () -> Unit,
    onDiscard: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var now by remember(startedAt, pausedAt) { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(startedAt, pausedAt) {
        while (pausedAt == null) {
            now = System.currentTimeMillis()
            delay(1.seconds)
        }
    }

    val elapsedMs = if (pausedAt != null) {
        (pausedAt - startedAt - accumulatedPausedMs).coerceAtLeast(0L)
    } else {
        (now - startedAt - accumulatedPausedMs).coerceAtLeast(0L)
    }

    val elapsedSec = (elapsedMs / 1000L).toInt()
    val minutes = elapsedSec / 60
    val seconds = elapsedSec % 60
    val timeFormatted = "%d:%02d".format(minutes, seconds)

    var showDiscardConfirm by remember { mutableStateOf(false) }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard Workout?") },
            text = { Text("Are you sure you want to abandon this quest? All logged sets and provisional rewards will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            showDiscardConfirm = false
                            onDiscard()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Keep Workout")
                }
            }
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 8.dp,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    
                    Text("•", style = MaterialTheme.typography.labelSmall)
                    
                    Text(
                        text = "$setCount sets",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )

                    if (provisionalVolumeKg > 0) {
                        Text("•", style = MaterialTheme.typography.labelSmall)
                        val displayVolume = Units.toDisplay(provisionalVolumeKg, imperial)
                        Text(
                            text = "${displayVolume.toInt()}${Units.label(imperial)}",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }

                    if (provisionalDistanceKm > 0) {
                        Text("•", style = MaterialTheme.typography.labelSmall)
                        val displayDist = Units.kmToDisplay(provisionalDistanceKm, imperial)
                        Text(
                            text = "%.1f%s".format(displayDist, Units.distLabel(imperial)),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }

                    if (provisionalXp > 0) {
                        Spacer(Modifier.width(2.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "+$provisionalXp",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            IconButton(onClick = { showDiscardConfirm = true }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Discard",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }

            Button(
                onClick = onResume,
                modifier = Modifier.height(40.dp).padding(end = 8.dp),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Resume", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
