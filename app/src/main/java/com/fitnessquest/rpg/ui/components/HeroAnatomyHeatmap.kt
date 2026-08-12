package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.Parchment

enum class AnatomyView { FRONT, BACK }

data class MuscleRegion(
    val id: String,
    val displayName: String,
    val view: AnatomyView,
    val icon: String = "💪"
)

val FrontMuscles = listOf(
    MuscleRegion("CHEST", "Chest", AnatomyView.FRONT, "🛡️"),
    MuscleRegion("SHOULDERS", "Shoulders", AnatomyView.FRONT, "⚔️"),
    MuscleRegion("BICEPS", "Biceps", AnatomyView.FRONT, "💪"),
    MuscleRegion("ABS", "Abs / Core", AnatomyView.FRONT, "⚡"),
    MuscleRegion("QUADS", "Quads", AnatomyView.FRONT, "🦵"),
    MuscleRegion("FOREARMS", "Forearms", AnatomyView.FRONT, "🧤")
)

val BackMuscles = listOf(
    MuscleRegion("LATS", "Lats / Back", AnatomyView.BACK, "🏹"),
    MuscleRegion("TRAPS", "Traps", AnatomyView.BACK, "🧛"),
    MuscleRegion("TRICEPS", "Triceps", AnatomyView.BACK, "🗡️"),

    MuscleRegion("GLUTES", "Glutes", AnatomyView.BACK, "🔥"),
    MuscleRegion("HAMSTRINGS", "Hamstrings", AnatomyView.BACK, "🦵"),
    MuscleRegion("CALVES", "Calves", AnatomyView.BACK, "⚡"),
    MuscleRegion("LOWER_BACK", "Lower Back", AnatomyView.BACK, "🛡️")
)


@Composable
fun HeroAnatomyHeatmap(
    character: CharacterEntity?,
    freshnessMap: Map<String, Int>,

    soreMuscles: Set<String>,
    onToggleMuscle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeView by remember { mutableStateOf(AnatomyView.FRONT) }
    var selectedMuscle by remember { mutableStateOf<MuscleRegion?>(null) }

    val currentMuscles = if (activeView == AnatomyView.FRONT) FrontMuscles else BackMuscles

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1E2A),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🩸 HERO MUSCLE RECOVERY & HEATMAP", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Gold)
                }

                // Front / Back Toggle Switch
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF12121C),
                    border = BorderStroke(1.dp, Parchment.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                activeView = if (activeView == AnatomyView.FRONT) AnatomyView.BACK else AnatomyView.FRONT
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = "Flip view", tint = Gold, modifier = Modifier.size(14.dp))
                        Text(
                            if (activeView == AnatomyView.FRONT) "FRONT VIEW" else "BACK VIEW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Gold
                        )
                    }
                }
            }

            // Subtitle Guidance
            Text(
                "Tap any muscle region to toggle soreness or view recovery status. Colors indicate muscle readiness.",
                style = MaterialTheme.typography.bodySmall,
                color = Parchment.copy(alpha = 0.7f),
                fontSize = 11.sp
            )

            // Anatomy Figure Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                // Background Hero Avatar (No armor, focusing on muscle regions)
                character?.let { hero ->
                    CharacterAvatar(
                        clazz = hero.characterClass ?: CharacterClass.WARRIOR,
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        gear = emptyMap(), // Remove armor to show muscles
                        appearance = hero.toAppearance(),
                        facingBack = activeView == AnatomyView.BACK,
                        expression = AvatarExpression.BATTLE_READY,
                        detail = AvatarDetail.FULL,
                        highlightMuscles = soreMuscles,
                        focus = AvatarFocus.FULL_BODY
                    )
                }

                // Legend at top left
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    LegendChip("Primed", Color(0xFF4CAF50))
                    LegendChip("Rebuilding", Color(0xFFFFB300))
                    LegendChip("Sore", Color(0xFFE53935))
                }
            }

            // Muscle Region Selection Grid (Floating style below the box)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                currentMuscles.chunked(3).forEach { rowMuscles ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowMuscles.forEach { region ->
                            val isSore = soreMuscles.contains(region.id) || soreMuscles.contains(region.displayName.uppercase())
                            val freshness = freshnessMap[region.displayName] ?: freshnessMap[region.id] ?: 95
                            val chipColor = when {
                                isSore -> Color(0xFFE53935)
                                freshness >= 85 -> Color(0xFF4CAF50)
                                freshness >= 45 -> Color(0xFFFFB300)
                                else -> Color(0xFFE53935)
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = chipColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, chipColor.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedMuscle = region
                                        onToggleMuscle(region.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(region.icon, fontSize = 13.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        region.displayName.substringBefore(" /"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Parchment,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    if (isSore) {
                                        Text("🔥", fontSize = 10.sp)
                                    } else {
                                        Text("$freshness%", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = chipColor)
                                    }
                                }
                            }
                        }
                        if (rowMuscles.size < 3) {
                            Spacer(Modifier.weight((3 - rowMuscles.size).toFloat()))
                        }
                    }
                }
            }

            // Reserved space for Tooltip to prevent shifting
            Box(Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
                selectedMuscle?.let { region ->
                    val isSore = soreMuscles.contains(region.id) || soreMuscles.contains(region.displayName.uppercase())
                    val freshness = freshnessMap[region.displayName] ?: freshnessMap[region.id] ?: 95
                    val statusText = when {
                        isSore -> "🔴 Flagged Sore — Exercise loads will auto-adjust for safety."
                        freshness >= 85 -> "🟢 100% Primed & Recovered — Maximum growth potential!"
                        else -> "🟡 Rebuilding (${freshness}% rested) — Train lightly or allow rest."
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF14141E),
                        border = BorderStroke(1.dp, Gold.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(region.icon, fontSize = 16.sp)
                            Column {
                                Text("${region.displayName} Status", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Gold)
                                Text(statusText, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = Parchment.copy(alpha = 0.9f))
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun LegendChip(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Parchment.copy(alpha = 0.8f))
    }
}
