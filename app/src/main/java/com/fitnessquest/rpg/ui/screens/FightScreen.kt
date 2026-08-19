package com.fitnessquest.rpg.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.domain.BattleAction
import com.fitnessquest.rpg.domain.BattleEngine
import com.fitnessquest.rpg.domain.BattleOutcome
import com.fitnessquest.rpg.domain.BattleState
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.MonsterCatalog
import com.fitnessquest.rpg.domain.RewardBatch
import com.fitnessquest.rpg.ui.appContainer
import com.fitnessquest.rpg.ui.components.ArenaFx
import com.fitnessquest.rpg.ui.components.BattleArena
import com.fitnessquest.rpg.ui.components.ConfettiOverlay
import com.fitnessquest.rpg.ui.components.RewardRevealDialog
import com.fitnessquest.rpg.ui.components.toAppearance
import com.fitnessquest.rpg.ui.components.FxKind
import com.fitnessquest.rpg.ui.components.countUp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import com.fitnessquest.rpg.ui.components.AvatarAppearance
import com.fitnessquest.rpg.ui.effects.AudioEffects
import com.fitnessquest.rpg.ui.effects.HapticEffects
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightBg
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class FightUiState(
    val battle: BattleState? = null,
    val gear: Map<ItemSlot, ItemEntity> = emptyMap(),
    val appearance: AvatarAppearance = AvatarAppearance(),
    val lootLabels: List<String> = emptyList(),
    val rewardBatch: RewardBatch? = null,
    val narration: String? = null,
    val narrationPending: Boolean = false,
    val rewardsApplied: Boolean = false,
    /** Effect currently playing in the arena. */
    val fx: ArenaFx? = null,
    /** True while a turn's animations are being sequenced; input is locked. */
    val resolving: Boolean = false,
    val ambush: Boolean = false,
)

class FightViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(FightUiState())
    val uiState: StateFlow<FightUiState> = _uiState

    private var loadedFor: Int? = null

    fun load(monsterId: Int, ambush: Boolean = false) {
        val key = (monsterId * 10) + (if (ambush) 1 else 0)
        if (loadedFor == key) return
        loadedFor = key
        viewModelScope.launch {
            val monster = MonsterCatalog.byId(monsterId) ?: return@launch
            val character = container.repository.getCharacter()
            val gear = container.repository.equippedGear(character)
            val stats = container.repository.combatStatsFor(character)
            val traits = container.repository.equippedTraits(character)
            val baseStats = if (character.characterClass == CharacterClass.DRUID) {
                container.repository.combatStatsFor(character.copy(druidForm = "HUMAN"))
            } else stats
            
            _uiState.update {
                it.copy(
                    ambush = ambush,
                    appearance = character.toAppearance(),
                    battle = BattleEngine.start(
                        playerName = character.name,
                        clazz = character.characterClass ?: CharacterClass.WARRIOR,
                        level = character.level,
                        stats = stats,
                        strength = character.strength,
                        agility = character.agility,
                        willpower = character.willpower,
                        monster = monster,
                        druidForm = character.druidForm,
                        baseStats = baseStats,
                        equippedTraits = traits,
                    ),
                    gear = gear
                )
            }
        }
    }

    private var fxId = 0L

    fun act(action: BattleAction, skillIndex: Int = 0, precisionMultiplier: Double = 1.0) {
        val ui = _uiState.value
        if (ui.resolving) return
        val current = ui.battle ?: return
        if (current.outcome != BattleOutcome.ONGOING) return
        val next = BattleEngine.takeTurn(
            state = current,
            action = action,
            skillIndex = skillIndex,
            precisionMultiplier = precisionMultiplier
        )

        if (action == BattleAction.FLEE) {
            _uiState.update { it.copy(battle = next) }
            if (next.outcome != BattleOutcome.ONGOING) settle(next)
            return
        }

        // Choreograph the turn: hero's move plays first, the monster's
        // response (and any damage to the hero) lands a beat later.
        viewModelScope.launch {
            _uiState.update { it.copy(resolving = true) }
            val playerFx = ArenaFx(
                id = ++fxId,
                kind = when (action) {
                    BattleAction.ATTACK -> FxKind.ATTACK
                    BattleAction.SKILL -> FxKind.SKILL
                    else -> FxKind.DEFEND
                },
                clazz = current.clazz,
                skillIndex = skillIndex
            )
            val mid = next.copy(playerHp = current.playerHp, outcome = BattleOutcome.ONGOING)
            _uiState.update { it.copy(battle = mid, fx = playerFx) }

            if (next.outcome == BattleOutcome.VICTORY) {
                delay(900.milliseconds) // let the finishing blow land before the overlay
                _uiState.update { it.copy(battle = next, resolving = false) }
                settle(next)
                return@launch
            }

            delay(750.milliseconds)
            val monsterFx = if (next.playerHp < current.playerHp) {
                ArenaFx(++fxId, FxKind.MONSTER_HIT, current.clazz)
            } else null
            _uiState.update {
                it.copy(battle = next, fx = monsterFx ?: it.fx, resolving = false)
            }
            if (next.outcome != BattleOutcome.ONGOING) settle(next)
        }
    }

    private fun settle(state: BattleState) {
        if (_uiState.value.rewardsApplied) return
        _uiState.update { it.copy(rewardsApplied = true) }
        viewModelScope.launch {
            val isAmbush = _uiState.value.ambush
            when (state.outcome) {
                BattleOutcome.VICTORY -> {
                    val batch = container.repository.applyVictory(
                        state.monster,
                        ambush = isAmbush
                    )
                    _uiState.update { it.copy(rewardBatch = batch) }
                    if (isAmbush) container.lastAmbushVictory = true
                }
                BattleOutcome.DEFEAT, BattleOutcome.FLED -> {
                    if (!isAmbush) container.repository.spendBattleEnergy()
                    if (isAmbush) container.lastAmbushVictory = false
                }
                BattleOutcome.ONGOING -> return@launch
            }

            if (container.gemini.isAvailable) {
                _uiState.update { it.copy(narrationPending = true) }
                val hero = container.repository.getCharacter()
                val text = container.gemini.battleNarration(
                    monsterName = state.monster.name,
                    victory = state.outcome == BattleOutcome.VICTORY,
                    playerName = state.playerName,
                    level = hero.level
                ).getOrNull()
                _uiState.update { it.copy(narration = text, narrationPending = false) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { FightViewModel(appContainer) }
        }
    }
}

@Composable
fun FightScreen(
    monsterId: Int,
    onDone: () -> Unit,
    ambush: Boolean = false,
    viewModel: FightViewModel = viewModel(factory = FightViewModel.Factory)
) {
    LaunchedEffect(monsterId, ambush) { viewModel.load(monsterId, ambush) }
    val state by viewModel.uiState.collectAsState()
    val battle = state.battle ?: return

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // The stage: hero vs monster over a biome backdrop
        BattleArena(
            battle = battle,
            gear = state.gear,
            appearance = state.appearance,
            fx = state.fx,
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${battle.monster.name}  ·  Lv ${battle.monster.level}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            battle.monster.trait?.let { trait ->
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        "${trait.emoji} ${trait.label}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Battle log
        val listState = rememberLazyListState()
        LaunchedEffect(battle.log.size) {
            if (battle.log.isNotEmpty()) listState.animateScrollToItem(battle.log.size - 1)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(battle.log) { _, line ->
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Actions
        val fighting = (battle.outcome == BattleOutcome.ONGOING) && !state.resolving
        var showMiniGame by remember { mutableStateOf(value = false) }

        if (showMiniGame) {
            PrecisionAttackDialog(
                agi = battle.agility,
                onStrike = { p ->
                    showMiniGame = false
                    val result = precisionResult(p, battle.agility)
                    if (result.multiplier > 1.0) {
                        AudioEffects.playLevelUp()
                    }
                    viewModel.act(BattleAction.ATTACK, precisionMultiplier = result.multiplier)
                }
            ) { showMiniGame = false }
        }

        // Primary Attack Button (prominent thumb-zone placement)
        Button(
            onClick = { showMiniGame = true },
            enabled = fighting,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) { 
            Text("⚔️ Attack", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) 
        }

        // Skills (Chunked for all unlocked tiers)
        val allSkills = battle.clazz.skills
        val chunkedSkills = allSkills.chunked(3)
        chunkedSkills.forEach { chunk ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chunk.forEach { skill ->
                    val index = allSkills.indexOf(skill)
                    SkillButton(
                        battle = battle,
                        index = index,
                        enabled = fighting,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.act(BattleAction.SKILL, index)
                    }
                }
                if (chunk.size < 3) {
                    repeat(3 - chunk.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Utility: Defend & Flee
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.act(BattleAction.DEFEND) },
                enabled = fighting,
                modifier = Modifier.weight(1f)
            ) { Text("🛡️ Defend") }
            OutlinedButton(
                onClick = { viewModel.act(BattleAction.FLEE) },
                enabled = fighting,
                modifier = Modifier.weight(1f)
            ) { Text("🏃 Flee") }
        }
    }

    if (battle.outcome != BattleOutcome.ONGOING) {
        if (battle.outcome == BattleOutcome.VICTORY) {
            VictoryOverlay(state = state, onDismiss = onDone)
        } else {
            BattleEndDialog(state = state, onDismiss = onDone)
        }
    }
}

/** Full-screen victory celebration with confetti and counted-up rewards. */
@Composable
private fun VictoryOverlay(state: FightUiState, onDismiss: () -> Unit) {
    val battle = state.battle ?: return
    var shown by remember { mutableStateOf(value = false) }
    LaunchedEffect(Unit) { shown = true }
    val titleScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "victoryScale"
    )

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        AudioEffects.playLevelUp()
        HapticEffects.performLevelUp(haptic, context)
                if (state.lootLabels.isNotEmpty()) {
                    delay(400)
                    AudioEffects.playLootDrop()
                }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(NightBg.copy(alpha = 0.96f))
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(battle.monster.emoji, style = MaterialTheme.typography.displayLarge)
                Text(
                    "🏆 VICTORY!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.scale(titleScale)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "The ${battle.monster.name} has fallen!",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "+${countUp(battle.monster.goldReward)} 💰   +${countUp(battle.monster.xpReward)} XP",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (state.lootLabels.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    state.lootLabels.forEach { label ->
                        Text(
                            "🎁 $label",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                when {
                    state.narration != null -> {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "“${state.narration}”",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    state.narrationPending -> {
                        Spacer(Modifier.height(12.dp))
                        CircularProgressIndicator(Modifier.width(24.dp), strokeWidth = 2.dp)
                    }
                }
                var showRewards by remember { mutableStateOf(false) }
                if (showRewards && state.rewardBatch != null) {
                    RewardRevealDialog(batch = state.rewardBatch, onDismiss = onDismiss)
                }

                Spacer(Modifier.height(24.dp))
                val isRewardLoading = state.rewardBatch == null
                Button(
                    onClick = {
                        if (state.rewardBatch != null) {
                            showRewards = true
                        } else {
                            onDismiss()
                        }
                    },
                    enabled = !isRewardLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    if (isRewardLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = NightBg,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "✨ Claim Rewards",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NightBg
                        )
                    }
                }
            }

            ConfettiOverlay(
                modifier = Modifier.fillMaxSize(),
                trigger = battle.monster.id,
                pieces = 130
            )
        }
    }
}

@Composable
private fun BattleEndDialog(state: FightUiState, onDismiss: () -> Unit) {
    val battle = state.battle ?: return
    val (title, message) = when (battle.outcome) {
        BattleOutcome.VICTORY -> "🏆 Victory!" to
            "You defeated the ${battle.monster.name}!\n+${battle.monster.goldReward} gold · +${battle.monster.xpReward} XP"
        BattleOutcome.DEFEAT -> "💀 Defeated" to
            "The ${battle.monster.name} was too strong. Train in the real world and return mightier!"
        BattleOutcome.FLED -> "🏃 Escaped" to
            "You live to fight another day. The energy was still spent."
        BattleOutcome.ONGOING -> return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message)
                if (state.lootLabels.isNotEmpty()) {
                    state.lootLabels.forEach { label ->
                        Text(
                            "🎁 $label",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                when {
                    state.narration != null -> Text(
                        "“${state.narration}”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.narrationPending -> Box(Modifier.padding(4.dp)) { CircularProgressIndicator() }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Continue") }
        }
    )
}

@Composable
private fun SkillButton(
    battle: BattleState,
    index: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val skill = battle.clazz.skills.getOrNull(index) ?: return
    val unlocked = battle.level >= skill.unlockLevel
    val cooldown = battle.skillCooldowns.getOrElse(index) { 0 }
    Button(
        onClick = onClick,
        enabled = enabled && unlocked && cooldown == 0,
        modifier = modifier.height(44.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            when {
                !unlocked -> "🔒 Lv ${skill.unlockLevel}"
                cooldown > 0 -> "${skill.emoji} (${cooldown})"
                else -> "${skill.emoji} ${skill.name}"
            },
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            maxLines = 1,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PrecisionAttackDialog(
    agi: Int,
    onStrike: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val perfectWidthFraction = perfectZoneWidth(agi)
    val perfectStart = 0.5f - perfectWidthFraction / 2f
    val perfectEnd = 0.5f + perfectWidthFraction / 2f
    var preview by remember { mutableStateOf<PrecisionResult?>(null) }
    val infiniteTransition = rememberInfiniteTransition(label = "slider")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((900 - agi * 10).coerceIn(520, 900), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("⚔️ PRECISION STRIKE!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Tap STRIKE when the indicator lands in the Gold PERFECT zone!", style = MaterialTheme.typography.bodySmall)

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.DarkGray)
                ) {
                    // Good zone
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E88E5).copy(alpha = 0.4f)))
                    
                    // Great zone
                    Box(modifier = Modifier.fillMaxWidth(0.4f).fillMaxHeight().align(Alignment.Center).background(Color(0xFF43A047).copy(alpha = 0.6f)))
                    
                    // Perfect zone (widened by AGI)
                    Box(modifier = Modifier.fillMaxWidth(perfectWidthFraction).fillMaxHeight().align(Alignment.Center).background(Color(0xFFFFD54F)))

                    // Moving slider
                    val markerWidth = 10.dp
                    Box(
                        modifier = Modifier
                            .width(markerWidth)
                            .fillMaxHeight()
                            .offset { IntOffset(x = ((maxWidth - markerWidth) * progress).roundToPx(), y = 0) }
                            .border(1.dp, Color(0xFF151515), RoundedCornerShape(6.dp))
                            .background(Color.White)
                    )
                }
                Text(
                    "🎯 Hit the center Gold bar for a Critical 1.5x Multiplier!",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Gold
                )
                preview?.let {
                    Text(
                        it.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = it.color
                    )
                }

                Button(
                    onClick = {
                        preview = precisionResult(progress, agi)
                        onStrike(progress)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("\u26A1 STRIKE!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class PrecisionResult(
    val label: String,
    val multiplier: Double,
    val color: Color
)

private fun perfectZoneWidth(agi: Int): Float =
    (0.16f + agi * 0.01f).coerceIn(0.16f, 0.36f)

private fun precisionResult(progress: Float, agi: Int): PrecisionResult {
    val perfectWidth = perfectZoneWidth(agi)
    val perfectStart = 0.5f - perfectWidth / 2f
    val perfectEnd = 0.5f + perfectWidth / 2f
    return when (progress) {
        in perfectStart..perfectEnd ->
            PrecisionResult("PERFECT! x2.0 damage", 2.0, Color(0xFFFFD54F))
        in 0.30f..0.70f ->
            PrecisionResult("GREAT! x1.5 damage", 1.5, Color(0xFF81C784))
        else ->
            PrecisionResult("Good hit", 1.0, Color(0xFF90CAF9))
    }
}
