package com.fitnessquest.rpg.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fitnessquest.rpg.domain.Units
import com.fitnessquest.rpg.ui.theme.Gold

@Composable
fun WeightLogDialog(
    currentWeightKg: Double,
    imperial: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var weightText by remember { 
        mutableStateOf(Units.toDisplay(currentWeightKg, imperial).let { 
            if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it) 
        }) 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚖️ Daily Weight In",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Logging your weight daily keeps your physical stats in sync and earns you rewards!",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text(if (imperial) "Weight (lb)" else "Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    weightText.toDoubleOrNull()?.let { display ->
                        onSave(Units.toKg(display, imperial))
                    }
                },
                enabled = weightText.toDoubleOrNull() != null
            ) {
                Text("Save & Earn Rewards")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
