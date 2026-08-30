package com.fitnessquest.rpg.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitnessquest.rpg.domain.Biome
import com.fitnessquest.rpg.domain.LootDropLine
import com.fitnessquest.rpg.domain.LootIntel
import com.fitnessquest.rpg.domain.LootSourceGuide
import com.fitnessquest.rpg.domain.Monster
import com.fitnessquest.rpg.domain.MonsterLootProfile
import com.fitnessquest.rpg.ui.theme.Gold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonsterLootGuideSheet(
    monster: Monster,
    biome: Biome,
    onDismiss: () -> Unit,
) {
    val profile = remember(monster.id, biome) { LootIntel.monsterProfile(monster, biome) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LootProfileContent(
            title = "${monster.emoji} ${monster.name}",
            subtitle = "${biome.label} · Level ${monster.level} · ${profile.summary}",
            dropLines = profile.dropLines,
            footer = "Gear rolls use your class templates with random affixes and stats.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiomeLootGuideSheet(
    biome: Biome,
    onDismiss: () -> Unit,
) {
    val profile = remember(biome) { LootIntel.biomeProfile(biome) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LootProfileContent(
            title = "${biome.emoji} ${biome.label}",
            subtitle = profile.biome.blurb,
            dropLines = profile.dropLines,
            extra = {
                Text(
                    "Signature relic: ${profile.bossRelicName}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                )
                Text(
                    "Example sets at T${profile.bossGearTier}: ${profile.setExamples.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                )
                Text(
                    "Monsters: ${profile.regularMonsters.joinToString { it.name }} · Boss: ${profile.boss.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LootAtlasSheet(
    currentBiome: Biome,
    onDismiss: () -> Unit,
    onSelectBiome: (Biome) -> Unit = {},
) {
    val profiles = remember { LootIntel.allBiomeProfiles() }
    var expandedBiome by remember { mutableStateOf<Biome?>(currentBiome) }
    var setQuery by remember { mutableStateOf("") }
    val setResults = remember(setQuery) { LootIntel.searchSetProfiles(setQuery) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Loot Atlas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            Text(
                "Where to farm gear, relics, and crafting mats across FitQuest.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )

            OutlinedTextField(
                value = setQuery,
                onValueChange = { setQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search armor sets (e.g. Nightveil)") },
                singleLine = true,
                colors = TextFieldDefaults.colors(),
            )
            if (setQuery.isNotBlank()) {
                setResults.take(6).forEach { profile ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            profile.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Gold,
                        )
                        profile.classAffinity?.let {
                            Text("Class affinity: $it", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
                        }
                        Text(profile.farmHint, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                        Text(profile.bonusSummary, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }

            profiles.forEach { profile ->
                val expanded = expandedBiome == profile.biome
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedBiome = if (expanded) null else profile.biome
                            onSelectBiome(profile.biome)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (profile.biome == currentBiome) Gold.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(
                        1.dp,
                        if (profile.biome == currentBiome) Gold.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${profile.biome.emoji} ${profile.biome.label}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "T${profile.bossGearTier} cap",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (expanded) {
                            profile.dropLines.forEach { DropLineRow(it) }
                            Text(
                                "Relic: ${profile.bossRelicName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        } else {
                            Text(
                                "Boss: ${profile.boss.name} · ${profile.bossRelicName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.65f),
                            )
                        }
                    }
                }
            }

            Text(
                "Other sources",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Gold,
            )
            LootIntel.otherSources().forEach { guide ->
                OtherSourceRow(guide)
            }
        }
    }
}

@Composable
fun LootInfoIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = contentDescription,
            tint = Gold.copy(alpha = 0.85f),
        )
    }
}

@Composable
fun LootAtlasCard(
    currentBiome: Biome,
    onOpenAtlas: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = remember(currentBiome) { LootIntel.biomeProfile(currentBiome) }
    FantasyCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenAtlas),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Loot Atlas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Gold,
                )
                Text(
                    "See what ${currentBiome.label} battles and bosses can drop.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                )
                Text(
                    "Now: T${profile.bossGearTier} gear · ${profile.bossRelicName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
            Text("🗺️", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun LootProfileContent(
    title: String,
    subtitle: String,
    dropLines: List<LootDropLine>,
    footer: String? = null,
    extra: @Composable (() -> Unit)? = null,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
        }
        if (extra != null) {
            item { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { extra() } }
        }
        items(dropLines) { line -> DropLineRow(line) }
        if (footer != null) {
            item {
                Text(
                    footer,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun DropLineRow(line: LootDropLine) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(line.emoji, style = MaterialTheme.typography.titleMedium)
        Column {
            Text(
                line.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                line.detail,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun OtherSourceRow(guide: LootSourceGuide) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "${guide.emoji} ${guide.title}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            guide.summary,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f),
        )
        guide.bullets.forEach { bullet ->
            Text(
                "• $bullet",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
            )
        }
    }
}
