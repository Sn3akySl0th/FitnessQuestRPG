package com.fitnessquest.rpg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fitnessquest.rpg.domain.GameMath
import com.fitnessquest.rpg.ui.theme.*

/**
 * Dialog allowing players to record steps from external step counters,
 * clip-on pedometers, desk walking pads, or third-party smartwatch apps.
 */
@Composable
fun ManualStepEntryDialog(
    currentStepsToday: Int,
    onDismiss: () -> Unit,
    onConfirm: (totalSteps: Int) -> Unit,
    onLaunchScan: () -> Unit
) {
    var inputVal by remember { mutableStateOf(if (currentStepsToday > 0) currentStepsToday.toString() else "") }
    val enteredSteps = inputVal.toIntOrNull() ?: 0
    val deltaSteps = (enteredSteps - currentStepsToday).coerceAtLeast(0)
    val kmProgress = deltaSteps.toDouble() / GameMath.STEPS_PER_KM
    val monsterBattles = deltaSteps / 500

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NightSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, GoldDim.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "👟 Scout Journal",
                    style = MaterialTheme.typography.titleLarge,
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Record steps from clip-on pedometers, smart watches, or treadmills.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Parchment.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Current vs New Total Display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NightSurfaceHigh, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Current Credited", fontSize = 11.sp, color = Parchment.copy(alpha = 0.6f))
                        Text(
                            text = "%,d".format(currentStepsToday),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Parchment
                        )
                    }
                    if (deltaSteps > 0) {
                        Text(
                            text = "+%,d".format(deltaSteps),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = StaminaGreen
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputVal,
                    onValueChange = { inputVal = it.filter { ch -> ch.isDigit() }.take(6) },
                    label = { Text("Total Daily Steps") },
                    placeholder = { Text("e.g. 7500") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = NightSurfaceHigh,
                        focusedTextColor = Parchment,
                        unfocusedTextColor = Parchment
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Increment Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(1000, 3000, 5000, 10000).forEach { inc ->
                        SuggestionChip(
                            onClick = {
                                val base = inputVal.toIntOrNull() ?: currentStepsToday
                                inputVal = (base + inc).coerceAtMost(50000).toString()
                            },
                            label = { Text("+${inc / 1000}k", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Expedition Rewards Preview
                if (deltaSteps > 0) {
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(listOf(Color(0xFF1B2A1E), Color(0xFF131F16))),
                                RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, StaminaGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "⚔️ Expedition Rewards Preview",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StaminaGreen
                        )
                        Text(
                            text = "• +%.2f km Biome Travel".format(kmProgress),
                            fontSize = 11.sp,
                            color = Parchment
                        )
                        if (monsterBattles > 0) {
                            Text(
                                text = "• $monsterBattles Idle Monster Battles (Gold & Loot)",
                                fontSize = 11.sp,
                                color = Parchment
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onLaunchScan,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ArcaneBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📸 Scan", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (enteredSteps > currentStepsToday) {
                                onConfirm(enteredSteps)
                            }
                            onDismiss()
                        },
                        enabled = enteredSteps > currentStepsToday,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = NightBg
                        ),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Record Steps", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
