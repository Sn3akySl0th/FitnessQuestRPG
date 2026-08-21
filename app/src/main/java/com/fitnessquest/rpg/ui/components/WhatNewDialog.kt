package com.fitnessquest.rpg.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitnessquest.rpg.data.WhatNewContent

@Composable
fun WhatNewDialog(onDismiss: () -> Unit) {
    val latest = WhatNewContent.getLatest() ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "What's New in Fitness Quest RPG ${latest.version}",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                latest.changes.forEach { change ->
                    Text(
                        text = "\u2022 $change",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Let's Go!")
            }
        }
    )
}
