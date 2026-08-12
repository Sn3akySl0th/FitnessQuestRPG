package com.fitnessquest.rpg.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitnessquest.rpg.FitQuestApp
import com.fitnessquest.rpg.domain.PlateMath
import com.fitnessquest.rpg.domain.Units

private enum class PlateCalcMode { LOAD, TOTAL }

/**
 * Two-mode plate calculator: target → plates per side, or plate counts → total weight.
 * [onUseWeight] receives the total in **display units** (lb or kg).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlateCalculatorDialog(
    onDismiss: () -> Unit,
    onUseWeight: ((Double) -> Unit)? = null,
) {
    val container = (LocalContext.current.applicationContext as FitQuestApp).container
    val imperial by container.prefs.imperial.collectAsState()
    val savedBarKg by container.prefs.plateBarKg.collectAsState()
    val unit = Units.label(imperial)
    val dens = remember(imperial) { PlateMath.denominations(imperial) }
    val presets = remember(imperial) { PlateMath.barPresets(imperial) }

    val initialBar = remember(imperial, savedBarKg) {
        savedBarKg?.let { Units.toDisplay(it, imperial) }
            ?: PlateMath.defaultBar(imperial)
    }

    var mode by remember { mutableStateOf(PlateCalcMode.LOAD) }
    var targetText by remember { mutableStateOf("") }
    var barText by remember(initialBar) {
        mutableStateOf(formatInput(initialBar))
    }
    var plateCounts by remember(imperial) {
        mutableStateOf(dens.associateWith { 0 })
    }

    val bar = barText.toDoubleOrNull() ?: 0.0
    val target = targetText.toDoubleOrNull() ?: 0.0

    val loadResult = remember(target, bar, imperial) {
        if (target > 0) PlateMath.platesForTarget(target, bar, imperial) else null
    }
    val totalFromPlates = remember(bar, plateCounts, imperial) {
        PlateMath.totalWeight(bar, plateCounts, imperial)
    }
    val resultTotal = when (mode) {
        PlateCalcMode.LOAD -> loadResult?.loadedTotal
        PlateCalcMode.TOTAL -> totalFromPlates
    }

    fun persistBar() {
        val display = barText.toDoubleOrNull() ?: return
        container.prefs.setPlateBarKg(Units.toKg(display, imperial))
    }

    AlertDialog(
        onDismissRequest = {
            persistBar()
            onDismiss()
        },
        title = { Text("Plate calculator") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == PlateCalcMode.LOAD,
                        onClick = { mode = PlateCalcMode.LOAD },
                        label = { Text("Load") }
                    )
                    FilterChip(
                        selected = mode == PlateCalcMode.TOTAL,
                        onClick = { mode = PlateCalcMode.TOTAL },
                        label = { Text("Total") }
                    )
                }

                Text(
                    if (mode == PlateCalcMode.LOAD) {
                        "Enter a target weight to see plates per side."
                    } else {
                        "Add plates per side to see the total on the bar."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = barText,
                    onValueChange = { barText = it.filterInput() },
                    label = { Text("Bar / starting weight ($unit)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { preset ->
                        val label = if (preset == 0.0) "Machine" else PlateMath.formatPlate(preset)
                        FilterChip(
                            selected = barText.toDoubleOrNull() == preset,
                            onClick = { barText = formatInput(preset) },
                            label = { Text(label) }
                        )
                    }
                }

                if (mode == PlateCalcMode.LOAD) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filterInput() },
                        label = { Text("Target ($unit)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    loadResult?.let { result ->
                        ResultBlock(
                            summary = result.summary,
                            total = result.loadedTotal,
                            unit = unit,
                            remainder = result.remainder,
                            target = target
                        )
                    }
                } else {
                    dens.forEach { plate ->
                        val count = plateCounts[plate] ?: 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${PlateMath.formatPlate(plate)} $unit",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (count > 0) {
                                            plateCounts += (plate to (count - 1))
                                        }
                                    },
                                    enabled = count > 0
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Fewer")
                                }
                                Text(
                                    count.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = {
                                        plateCounts = plateCounts + (plate to count + 1)
                                    }
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "More")
                                }
                            }
                        }
                    }
                    ResultBlock(
                        summary = plateCounts
                            .filter { it.value > 0 }
                            .entries
                            .asSequence()
                .sortedByDescending { it.key }
                            .joinToString(", ") {
                                "${it.value}\u00D7${PlateMath.formatPlate(it.key)}"
                            }
                            .ifEmpty { "Bar only" }
                            .let { if (it == "Bar only") it else "$it per side" },
                        total = totalFromPlates,
                        unit = unit,
                        remainder = 0.0,
                        target = 0.0
                    )
                }
            }
        },
        confirmButton = {
            if (onUseWeight != null && resultTotal != null && resultTotal > 0) {
                TextButton(
                    onClick = {
                        persistBar()
                        onUseWeight(resultTotal)
                        onDismiss()
                    }
                ) { Text("Use weight") }
            }
            TextButton(
                onClick = {
                    persistBar()
                    onDismiss()
                }
            ) { Text("Done") }
        }
    )
}

@Composable
private fun ResultBlock(
    summary: String,
    total: Double,
    unit: String,
    remainder: Double,
    target: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(summary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Total: ${formatInput(total)} $unit",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        if (remainder > 0.01 && target > 0) {
            Text(
                "Closest load — ${formatInput(remainder)} $unit short of target",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun String.filterInput(): String =
    filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')

private fun formatInput(value: Double): String =
    if (value % 1.0 in 0.0..0.001 || value % 1.0 in 0.999..1.0) {
        value.toInt().toString()
    } else {
        "%.2f".format(value).trimEnd('0').trimEnd('.')
    }
