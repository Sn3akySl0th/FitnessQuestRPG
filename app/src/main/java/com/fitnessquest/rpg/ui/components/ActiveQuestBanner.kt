package com.fitnessquest.rpg.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun ActiveQuestBanner(
    title: String,
    startedAt: Long,
    pausedAt: Long? = null,
    accumulatedPausedMs: Long = 0L,
    setCount: Int,
    provisionalXp: Int,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                        showDiscardConfirm = false
                        onDiscard()
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
        tonalElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "$setCount sets",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (provisionalXp > 0) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "+$provisionalXp XP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { showDiscardConfirm = true },
                modifier = Modifier.height(36.dp)
            ) {
                Text("Discard", style = MaterialTheme.typography.labelMedium)
            }

            Button(
                onClick = onResume,
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.height(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Resume", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
