package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessquest.rpg.data.db.BiomeProgressEntity
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.domain.Biome
import com.fitnessquest.rpg.domain.GameMath
import com.fitnessquest.rpg.domain.Units
import com.fitnessquest.rpg.domain.ProgressionRules
import com.fitnessquest.rpg.ui.effects.AudioEffects
import com.fitnessquest.rpg.ui.effects.HapticEffects
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.HealthRed
import com.fitnessquest.rpg.ui.theme.MysticPurple

/**
 * Interactive Biome Cartography Map.
 * Renders a winding path connecting the 6 biomes with unlock nodes,
 * travel progress bars, hero location markers, and a one-shot lore encounter.
 */
@Composable
fun InteractiveWorldMap(
    character: CharacterEntity,
    imperial: Boolean,
    encounterClaimed: Boolean,
    biomeRequirement: ProgressionRules.BiomeRequirement,
    allProgress: List<BiomeProgressEntity>,
    onStartTravel: (Biome) -> Unit,
    onCancelTravel: () -> Unit,
    onClaimEncounter: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val currentBiome = Biome.fromName(character.currentBiome)
    val travelTarget = character.travelTarget?.let { Biome.fromName(it) }
    var selectedBiome by remember { mutableStateOf<Biome?>(null) }
    var showLoreDialog by remember { mutableStateOf(value = false) }

    val biomes = Biome.entries

    val infiniteTransition = rememberInfiniteTransition(label = "heroMarker")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🗺️ WORLD MAP OF FITQUEST",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Gold
                )
                Text(
                    text = "Current: ${currentBiome.emoji} ${currentBiome.label}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Progression Status Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (biomeRequirement) {
                        is ProgressionRules.BiomeRequirement.Locked -> {
                            Text("🔒", fontSize = 24.sp)
                            Column {
                                Text(
                                    biomeRequirement.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (biomeRequirement.pointsNeeded > 0) {
                                    Text(
                                        "Earn ${biomeRequirement.pointsNeeded} more points to unlock the boss.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Gold
                                    )
                                } else {
                                    Text(
                                        "The boss is ready! Defeat it to unlock travel.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HealthRed
                                    )
                                }
                            }
                        }
                        is ProgressionRules.BiomeRequirement.Unlocked -> {
                            Text("✨", fontSize = 24.sp)
                            Column {
                                Text(
                                    "Path Unlocked!",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6BC96B)
                                )
                                Text(
                                    "You can now travel to ${biomeRequirement.nextBiome.label}.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                        ProgressionRules.BiomeRequirement.MaxBiome -> {
                            Text("🏆", fontSize = 24.sp)
                            Column {
                                Text(
                                    "World Conquered!",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Gold
                                )
                                Text(
                                    "All biomes cleared. You've reached the final frontier!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            if (travelTarget != null) {
                val needed = travelTarget.travelKm
                val done = character.travelProgress
                val frac = (done / needed.coerceAtLeast(0.1)).coerceIn(0.0, 1.0).toFloat()
                val progressLabel = "${Units.formatDistance(done, imperial)} / ${Units.formatDistance(needed, imperial)}"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MysticPurple.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "🏃 Traveling to ${travelTarget.emoji} ${travelTarget.label}...",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Gold
                        )
                        Text(
                            progressLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                    BarMeter(
                        label = Units.distLabel(imperial).uppercase(),
                        valueText = progressLabel,
                        progress = frac,
                        color = Gold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!encounterClaimed) {
                            TextButton(
                                onClick = {
                                    showLoreDialog = true
                                    AudioEffects.playLootDrop()
                                    HapticEffects.performLevelUp(haptic, context)
                                }
                            ) {
                                Text("✨ Lore Event", style = MaterialTheme.typography.labelSmall, color = Gold)
                            }
                        } else {
                            Text(
                                "Lore claimed — bonus waits for your next strength workout",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        TextButton(onClick = onCancelTravel) {
                            Text("Cancel Travel", style = MaterialTheme.typography.labelSmall, color = HealthRed)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                biomes.forEachIndexed { index, b ->
                    val isUnlocked = (character.level >= b.levelRequired) && ProgressionRules.canEnterBiome(b, allProgress)
                    val isCurrent = b == currentBiome
                    val isTarget = b == travelTarget

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            selectedBiome = b
                            AudioEffects.playSetLogged()
                            HapticEffects.performSetLogged(haptic, context)
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val borderBrush = when {
                                isCurrent -> Brush.sweepGradient(listOf(Gold, Color.White, Gold))
                                isTarget -> Brush.sweepGradient(listOf(Color(0xFF6BC96B), Color.White))
                                isUnlocked -> Brush.sweepGradient(listOf(Color(b.colorA), Color(b.colorB)))
                                else -> Brush.sweepGradient(listOf(Color.Gray, Color.DarkGray))
                            }

                            Surface(
                                modifier = Modifier
                                    .size(68.dp)
                                    .scale(if (isCurrent) pulseScale else 1.0f),
                                shape = CircleShape,
                                color = if (isUnlocked) Color(b.colorB).copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.6f),
                                border = BorderStroke(2.5.dp, borderBrush)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (isUnlocked) b.emoji else "🔒",
                                        fontSize = 28.sp
                                    )
                                }
                            }

                            if (isCurrent) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp),
                                    shape = CircleShape,
                                    color = Gold
                                ) {
                                    Text(
                                        "HERO",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = b.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isCurrent || isTarget) FontWeight.Black else FontWeight.Bold
                            ),
                            color = if (isUnlocked) Color.White else Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (isUnlocked) "Lv ${b.levelRequired}+" else "Req Lv ${b.levelRequired}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUnlocked) Gold else Color.Red.copy(alpha = 0.8f)
                        )
                    }

                    if (index < biomes.lastIndex) {
                        Text(
                            "➔",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }

    selectedBiome?.let { b ->
        val levelMet = character.level >= b.levelRequired
        val canEnter = ProgressionRules.canEnterBiome(b, allProgress)
        val isCurrent = b == currentBiome
        val isTarget = b == travelTarget
        val distanceText = Units.formatDistance(b.travelKm, imperial)

        AlertDialog(
            onDismissRequest = { selectedBiome = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(b.emoji, fontSize = 28.sp)
                    Text(b.label, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(b.blurb, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Level Required: Lv ${b.levelRequired}", color = Gold, fontWeight = FontWeight.Bold)
                    Text("Travel Goal: $distanceText", color = Color.White.copy(alpha = 0.8f))
                    Text(
                        "(Achieved by Cardio or Steps: ${GameMath.STEPS_PER_KM} steps = 1 km)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                if (!isCurrent && !isTarget) {
                    if (canEnter && levelMet) {
                        Button(
                            onClick = {
                                selectedBiome = null
                                onStartTravel(b)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)
                        ) {
                            Text("START TRAVEL ($distanceText)", fontWeight = FontWeight.Bold)
                        }
                    } else if (levelMet) {
                        // Level unlocked but boss gated
                        val prevBiome = Biome.entries.getOrNull(b.ordinal - 1)
                        Text(
                            "⚔️ Defeat the ${prevBiome?.label ?: "previous"} boss to unlock this path.",
                            style = MaterialTheme.typography.bodySmall,
                            color = HealthRed,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        Text(
                            "🔒 Level ${b.levelRequired} required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBiome = null }) { Text("Close") }
            }
        )
    }

    if (showLoreDialog) {
        AlertDialog(
            onDismissRequest = { showLoreDialog = false },
            title = { Text("📜 LORE ENCOUNTER", color = Gold, fontWeight = FontWeight.Black) },
            text = {
                Text(
                    "Ancient Obelisk Discovered!\n\n" +
                        "While trekking toward ${travelTarget?.label ?: "your destination"}, " +
                        "you uncovered an ancient rune stone. Your next workout that includes " +
                        "strength sets earns +15% Strength progress.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLoreDialog = false
                        onClaimEncounter()
                    }
                ) { Text("CLAIM ENCOUNTER BONUS") }
            },
            dismissButton = {
                TextButton(onClick = { showLoreDialog = false }) { Text("Later") }
            }
        )
    }
}
