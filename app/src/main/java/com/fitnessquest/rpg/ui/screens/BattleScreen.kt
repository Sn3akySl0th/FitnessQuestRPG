package com.fitnessquest.rpg.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.domain.Biome
import com.fitnessquest.rpg.domain.CombatStats
import com.fitnessquest.rpg.domain.GameMath
import com.fitnessquest.rpg.domain.Monster
import com.fitnessquest.rpg.domain.MonsterCatalog
import kotlinx.coroutines.flow.combine
import com.fitnessquest.rpg.domain.Units
import com.fitnessquest.rpg.ui.appContainer
import com.fitnessquest.rpg.ui.rememberDockContentPadding
import com.fitnessquest.rpg.ui.components.InteractiveWorldMap
import com.fitnessquest.rpg.ui.components.ResourceChip
import com.fitnessquest.rpg.ui.components.SceneBanner
import com.fitnessquest.rpg.ui.components.SceneKind
import com.fitnessquest.rpg.ui.components.SectionCard
import com.fitnessquest.rpg.ui.components.SettingsIconButton
import com.fitnessquest.rpg.ui.theme.Gold
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class BattleSelectViewModel(private val container: AppContainer) : ViewModel() {
    val battleState: StateFlow<BattleSelectUiState> = combine(
        container.repository.character,
        container.repository.ownedGear,
        container.repository.items,
    ) { character, owned, items ->
        val runeMap = items.asSequence().filter { it.slot == ItemSlot.RUNE }.associateBy { it.id }
        val gear = character.equippedIds().mapNotNull { (slot, id) ->
            owned.find { it.instance.id == id }?.asEquippedItem(runeMap)?.let { slot to it }
        }.toMap()
        BattleSelectUiState(
            character = character,
            combat = GameMath.combatStats(character, gear.values.toList())
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BattleSelectUiState())

    val imperial: StateFlow<Boolean> = container.prefs.imperial

    val stepsToday: StateFlow<Int> = container.steps.stepsToday
    val stepTracking: StateFlow<Boolean> = container.steps.tracking
    val stepSensorAvailable: Boolean get() = container.steps.hasSensor

    fun startStepTracking() = container.steps.start()

    fun startTravel(biome: Biome) {
        viewModelScope.launch {
            container.prefs.resetTravelEncounter()
            container.repository.startTravel(biome)
        }
    }

    fun cancelTravel() {
        viewModelScope.launch {
            container.prefs.resetTravelEncounter()
            container.repository.cancelTravel()
        }
    }

    val encounterClaimed: StateFlow<Boolean> = container.prefs.encounterClaimedThisTravel

    fun claimEncounterBonus() {
        if (battleState.value.character?.travelTarget == null) return
        container.prefs.claimTravelEncounter()
    }

    /** Triggers energy recoup logic periodically. */
    fun refreshCharacter() {
        viewModelScope.launch {
            container.repository.getCharacter()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { BattleSelectViewModel(appContainer) }
        }
    }
}

data class BattleSelectUiState(
    val character: CharacterEntity? = null,
    val combat: CombatStats? = null
)

@Composable
fun BattleScreen(
    onFight: (Int) -> Unit,
    viewModel: BattleSelectViewModel = viewModel(factory = BattleSelectViewModel.Factory)
) {
    val battleState by viewModel.battleState.collectAsState()
    val imperial by viewModel.imperial.collectAsState()
    val encounterClaimed by viewModel.encounterClaimed.collectAsState()
    val c = battleState.character ?: return
    val combat = battleState.combat
    val canFight = c.energy >= GameMath.BATTLE_ENERGY_COST
    val biome = Biome.fromName(c.currentBiome)
    val travelTarget = c.travelTarget?.let { Biome.fromName(it) }

    // Periodically refresh to recoup energy while selecting battle
    LaunchedEffect(Unit) {
        while (true) {
            delay(1.minutes)
            viewModel.refreshCharacter()
        }
    }

    Column(Modifier.fillMaxSize()) {
        SceneBanner(
            kind = SceneKind.BATTLE,
            title = "BATTLEFIELDS",
            tagline = "Spend energy. Claim spoils."
        ) {
            SettingsIconButton()
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = rememberDockContentPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InteractiveWorldMap(
                    character = c,
                    imperial = imperial,
                    encounterClaimed = encounterClaimed,
                    onStartTravel = viewModel::startTravel,
                    onCancelTravel = viewModel::cancelTravel,
                    onClaimEncounter = viewModel::claimEncounterBonus
                )
            }


        if (viewModel.stepSensorAvailable) {
            item {
                val stepsToday by viewModel.stepsToday.collectAsState()
                val tracking by viewModel.stepTracking.collectAsState()
                StepsCard(
                    steps = stepsToday,
                    tracking = tracking,
                    traveling = travelTarget != null,
                    imperial = imperial,
                    onEnabled = viewModel::startStepTracking
                )
            }
        }


        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Local Monsters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResourceChip("\u26A1", "${c.energy} energy")
                    ResourceChip("\u2694\uFE0F", "costs ${GameMath.BATTLE_ENERGY_COST} per battle")
                }
                if (c.energy < GameMath.MAX_ENERGY) {
                    Text(
                        "✨ Resting... +1⚡ every 12m",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!canFight) {
                    Text(
                        "Not enough energy! Complete a workout to recharge \u2014 " +
                            "real training fuels your battles.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        items(MonsterCatalog.byBiome(biome), key = { it.id }) { monster ->
            MonsterCard(
                monster = monster,
                playerLevel = c.level,
                combat = combat,
                enabled = canFight
            ) { onFight(monster.id) }
        }
        }
    }


}

/**
 * Daily step counter feeding passive travel. Shows an enable button until the
 * activity-recognition permission is granted (Android 10+).
 */
@Composable
private fun StepsCard(
    steps: Int,
    tracking: Boolean,
    traveling: Boolean,
    imperial: Boolean,
    onEnabled: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) onEnabled() }

    SectionCard {
        if (tracking) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("\uD83D\uDC5F", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "%,d steps today".format(steps),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (traveling) {
                            "Every step pushes your journey forward \u2014 " +
                                "${GameMath.STEPS_PER_KM} steps \u2248 ${Units.formatDistance(1.0, imperial)}."
                        } else {
                            "Start a journey and your steps will carry you there."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                "\uD83D\uDC5F Step-powered travel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Let your real-world steps carry your hero between biomes, " +
                    "even outside workouts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= 29) {
                        launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    } else {
                        onEnabled()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enable step tracking") }
        }
    }
}

@Composable
private fun MonsterCard(
    monster: Monster,
    playerLevel: Int,
    combat: CombatStats?,
    enabled: Boolean,
    onFight: () -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(monster.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(monster.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Level ${monster.level} \u00B7 ${difficultyLabel(monster.level, playerLevel)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = difficultyColor(monster.level, playerLevel)
                )
                monster.trait?.let { trait ->
                    Text(
                        "${trait.emoji} ${trait.label}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Button(onClick = onFight, enabled = enabled) { Text("Fight") }
        }
        Text(
            monster.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Reward: ${monster.goldReward} \uD83D\uDCB0 \u00B7 ${monster.xpReward} XP",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        val estimate = battleEstimate(monster, playerLevel, combat)
        Text(
            "Preview: ${estimate.winChance}% win chance \u00B7 ${estimate.warning} \u00B7 train ${estimate.recommendedStat}",
            style = MaterialTheme.typography.bodySmall,
            color = estimate.color()
        )
    }
}

private data class BattleEstimate(
    val winChance: Int,
    val warning: String,
    val recommendedStat: String
)

private fun battleEstimate(monster: Monster, playerLevel: Int, combat: CombatStats?): BattleEstimate {
    val power = if (combat != null) {
        (combat.maxHp * 0.42) + (combat.atk * 5.0) + (combat.def * 4.0) + (combat.spd * 1.5) + (combat.critPercent * 1.2)
    } else {
        playerLevel * 22.0
    }
    val monsterPower = monster.hp * 0.42 + monster.atk * 5.0 + monster.def * 4.0 + monster.spd * 1.5 +
        monster.level * 12.0 + (monster.trait?.let { 18.0 } ?: 0.0)
    val chance = (50 + ((power - monsterPower) / 4.0)).toInt().coerceIn(12, 92)
    val warning = when {
        monster.trait != null -> "${monster.trait.emoji} ${monster.trait.label}"
        chance >= 70 -> "favorable"
        chance >= 45 -> "even fight"
        else -> "high risk"
    }
    val recommended = when {
        combat == null -> "balanced stats"
        monster.atk > combat.def + 6 -> "DEF"
        monster.hp > combat.maxHp -> "HP/END"
        monster.spd > combat.spd + 4 -> "SPD/AGI"
        monster.def > combat.atk / 2 -> "ATK/STR"
        else -> "your main stat"
    }
    return BattleEstimate(chance, warning, recommended)
}

@Composable
private fun BattleEstimate.color() = when {
    winChance >= 70 -> MaterialTheme.colorScheme.primary
    winChance >= 45 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}

private fun difficultyLabel(monsterLevel: Int, playerLevel: Int): String = when {
    monsterLevel <= playerLevel - 3 -> "Easy prey"
    monsterLevel <= playerLevel + 1 -> "Fair fight"
    monsterLevel <= playerLevel + 4 -> "Dangerous"
    else -> "Deadly"
}

@Composable
private fun difficultyColor(monsterLevel: Int, playerLevel: Int) = when {
    monsterLevel <= playerLevel - 3 -> MaterialTheme.colorScheme.secondary
    monsterLevel <= playerLevel + 1 -> MaterialTheme.colorScheme.primary
    monsterLevel <= playerLevel + 4 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}
