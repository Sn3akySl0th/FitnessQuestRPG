package com.fitnessquest.rpg.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitnessquest.rpg.data.ai.RoutineRecommendation
import com.fitnessquest.rpg.domain.Biome
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.CharacterRace
import com.fitnessquest.rpg.domain.GearRarity
import com.fitnessquest.rpg.domain.GameMath
import com.fitnessquest.rpg.domain.Monster
import com.fitnessquest.rpg.ui.components.AvatarExpression
import com.fitnessquest.rpg.ui.components.DailyBountyCard
import com.fitnessquest.rpg.ui.components.EmbersOverlay
import com.fitnessquest.rpg.ui.components.HeroPaperDoll
import com.fitnessquest.rpg.ui.components.ManualStepEntryDialog
import com.fitnessquest.rpg.ui.components.PedometerScanDialog
import com.fitnessquest.rpg.ui.components.SettingsIconButton
import com.fitnessquest.rpg.ui.components.WeightLogDialog
import com.fitnessquest.rpg.ui.components.toAppearance
import com.fitnessquest.rpg.ui.rememberDockContentPadding
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightBg

enum class QuestHubAction {
    RESUME_QUEST,
    BEGIN_QUEST,
    OPEN_TRAINING
}

data class ActiveQuestSnapshot(
    val title: String,
    val workoutId: Long?,
    val exerciseCount: Int
)

data class QuestHubState(
    val eyebrow: String,
    val title: String,
    val detail: String,
    val readinessPercent: Int?,
    val caution: String?,
    val worldLine: String,
    val outcome: String,
    val primaryLabel: String,
    val action: QuestHubAction,
    val workoutId: Long? = null
)

internal fun buildQuestHubState(
    recommendation: RoutineRecommendation,
    activeQuest: ActiveQuestSnapshot?,
    recommendedExerciseCount: Int,
    biome: Biome,
    encounter: Monster
): QuestHubState {
    val worldLine = "${biome.emoji} ${biome.label}  •  ${encounter.emoji} ${encounter.name}"

    if (activeQuest != null) {
        val exerciseText = activeQuest.exerciseCount.takeIf { it > 0 }
            ?.let { "$it exercise${if (it == 1) "" else "s"}" }
            ?: "Quest in progress"
        return QuestHubState(
            eyebrow = "ACTIVE WORKOUT QUEST",
            title = activeQuest.title,
            detail = exerciseText,
            readinessPercent = null,
            caution = null,
            worldLine = worldLine,
            outcome = "Finish the workout to earn XP + Gold and power your adventure.",
            primaryLabel = "Resume Quest",
            action = QuestHubAction.RESUME_QUEST,
            workoutId = activeQuest.workoutId ?: -1L
        )
    }

    if (recommendation.isRestDay) {
        return QuestHubState(
            eyebrow = "TODAY'S QUEST",
            title = "Recovery Day",
            detail = recommendation.sideQuestTitle ?: "Rest and rebuild for the next encounter.",
            readinessPercent = recommendation.readinessPercent,
            caution = "Recovery is recommended, but you can still choose any workout.",
            worldLine = worldLine,
            outcome = "Recover now to return stronger against ${encounter.name}.",
            primaryLabel = "Train Anyway",
            action = QuestHubAction.OPEN_TRAINING
        )
    }

    val routine = recommendation.routine
    if (routine == null) {
        return QuestHubState(
            eyebrow = "YOUR NEXT QUEST",
            title = "Choose Your First Workout Quest",
            detail = "Create a routine or launch a freestyle workout from Training.",
            readinessPercent = recommendation.readinessPercent,
            caution = null,
            worldLine = worldLine,
            outcome = "Your first completed workout will earn XP + Gold and advance the adventure.",
            primaryLabel = "Choose Training",
            action = QuestHubAction.OPEN_TRAINING
        )
    }

    val count = recommendedExerciseCount.coerceAtLeast(0)
    val estimatedMinutes = (count * 8).coerceAtLeast(10)
    val countText = if (count == 0) "Routine ready" else "$count exercise${if (count == 1) "" else "s"} • about $estimatedMinutes min"
    return QuestHubState(
        eyebrow = "YOUR NEXT QUEST",
        title = routine.name,
        detail = countText,
        readinessPercent = recommendation.readinessPercent,
        caution = recommendation.cautionWarning,
        worldLine = worldLine,
        outcome = "Earn workout XP + Gold and prepare to face ${encounter.name}.",
        primaryLabel = "Begin Quest",
        action = QuestHubAction.BEGIN_QUEST,
        workoutId = routine.id
    )
}

@Composable
fun QuestHubScreen(
    viewModel: HeroViewModel = viewModel(factory = HeroViewModel.Factory),
    onOpenHero: () -> Unit = {},
    onOpenSaga: () -> Unit = {},
    onOpenTraining: () -> Unit = {},
    onOpenBattle: () -> Unit = {},
    onStartWorkout: (Long) -> Unit = {}
) {
    val screenState by viewModel.uiState.collectAsState()
    val bounties by viewModel.bounties.collectAsState()
    val bountyResetLabel by viewModel.bountyResetLabel.collectAsState()
    val bountyResetRemainingMs by viewModel.bountyResetRemainingMs.collectAsState()
    val imperial by viewModel.imperial.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val stepsToday by viewModel.stepsToday.collectAsState()
    var showWeightDialog by remember { mutableStateOf(false) }
    var showManualStepsDialog by remember { mutableStateOf(false) }
    var showPedometerScanDialog by remember { mutableStateOf(false) }
    var showJobSwitcher by remember { mutableStateOf(false) }
    val character = screenState.character
    val heroClass = character?.characterClass

    if (character == null || heroClass == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (showWeightDialog) {
        WeightLogDialog(
            currentWeightKg = character.bodyWeightKg ?: 75.0,
            imperial = imperial,
            onDismiss = { showWeightDialog = false },
            onSave = { weight ->
                viewModel.logWeight(weight)
                showWeightDialog = false
            }
        )
    }

    if (showManualStepsDialog) {
        ManualStepEntryDialog(
            currentStepsToday = stepsToday,
            onDismiss = { showManualStepsDialog = false },
            onConfirm = { steps ->
                viewModel.recordManualSteps(steps)
                showManualStepsDialog = false
            },
            onLaunchScan = {
                showManualStepsDialog = false
                showPedometerScanDialog = true
            }
        )
    }

    if (showPedometerScanDialog) {
        PedometerScanDialog(
            currentStepsToday = stepsToday,
            onDismiss = { showPedometerScanDialog = false },
            onConfirm = { steps ->
                viewModel.recordManualSteps(steps)
                showPedometerScanDialog = false
            }
        )
    }

    if (showJobSwitcher) {
        JobSwitchSheet(
            state = screenState,
            isPremium = isPremium,
            onDismiss = { showJobSwitcher = false },
            onSwitchJob = { selectedClass ->
                viewModel.switchJob(selectedClass)
                showJobSwitcher = false
            }
        )
    }

    EmbersOverlay(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = rememberDockContentPadding(
                horizontal = 0.dp,
                top = 0.dp,
                extraBottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                CompactQuestHeroHeader(
                    state = screenState,
                    onOpenHero = onOpenHero,
                    onOpenJobSwitcher = { showJobSwitcher = true }
                )
            }

            item {
                screenState.questHub?.let { quest ->
                    QuestHubCard(
                        state = quest,
                        onPrimaryAction = { selected ->
                            when (selected.action) {
                                QuestHubAction.RESUME_QUEST,
                                QuestHubAction.BEGIN_QUEST -> onStartWorkout(selected.workoutId ?: -1L)
                                QuestHubAction.OPEN_TRAINING -> onOpenTraining()
                            }
                        }
                    )
                }
            }

            item {
                DailyBountyCard(
                    bounties = bounties,
                    resetLabel = bountyResetLabel,
                    resetRemainingMs = bountyResetRemainingMs,
                    onClaim = viewModel::claimBounty,
                    onLogProgress = { bounty ->
                        when (bounty.id) {
                            "b_weight" -> showWeightDialog = true
                            "b_steps" -> showManualStepsDialog = true
                            else -> viewModel.logBountyProgress(bounty)
                        }
                    },
                    onOpenBattle = onOpenBattle,
                    onOpenSaga = onOpenSaga,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactQuestHeroHeader(
    state: HeroUiState,
    onOpenHero: () -> Unit,
    onOpenJobSwitcher: () -> Unit
) {
    val character = state.character ?: return
    val heroClass = character.characterClass ?: return
    val activeJobLevel = state.allClassProgress
        .firstOrNull { it.clazz == heroClass }
        ?.level
        ?: character.level
    val headerHeight = if (LocalDensity.current.fontScale >= 1.2f) 236.dp else 216.dp
    val biome = Biome.fromName(character.currentBiome)
    val colors = listOf(Color(biome.colorA), Color(biome.colorB))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .background(Brush.horizontalGradient(colors))
            .statusBarsPadding()
            .clickable(onClick = onOpenHero)
            .semantics { contentDescription = "Open Hero details" }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.43f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    HeroPaperDoll(
                        clazz = heroClass,
                        gear = state.gear,
                        appearance = character.toAppearance(),
                        highestRarity = GearRarity.COMMON,
                        expression = AvatarExpression.CALM,
                        modifier = Modifier.fillMaxSize(),
                        onAvatarClick = onOpenHero
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(0.57f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "QUEST HUB",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = CharacterRace.fromStored(character.race).label,
                        style = MaterialTheme.typography.bodySmall,
                        color = Gold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${biome.emoji} ${biome.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f),
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactHeaderToken(emoji = "💰", text = character.gold.toString())
                        CompactHeaderToken(emoji = "⚡", text = "${character.energy}/${GameMath.MAX_ENERGY}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("View Hero", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Surface(
                            onClick = onOpenJobSwitcher,
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF171C2B).copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, Gold.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = "${heroClass.emoji} Lv $activeJobLevel  ▾",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.25f))
        ) {
            SettingsIconButton()
        }
    }
}

@Composable
private fun CompactHeaderToken(emoji: String, text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF171C2B).copy(alpha = 0.82f),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f))
    ) {
        Text(
            text = "$emoji $text",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobSwitchSheet(
    state: HeroUiState,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onSwitchJob: (CharacterClass) -> Unit
) {
    val character = state.character ?: return
    val activeClass = character.characterClass ?: return
    val jobs = CharacterClass.entries.sortedWith(
        compareByDescending<CharacterClass> { it == activeClass }
            .thenByDescending { candidate ->
                state.allClassProgress.firstOrNull { it.clazz == candidate }?.level ?: 1
            }
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Switch Job", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(
                "Each job retains its own level and equipped gear.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 440.dp),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(jobs, key = { it.name }) { job ->
                val level = state.allClassProgress.firstOrNull { it.clazz == job }?.level ?: 1
                val isActive = job == activeClass
                val isLocked = job.requiresPremium && !isPremium
                Surface(
                    onClick = { if (!isLocked) onSwitchJob(job) },
                    enabled = !isActive && !isLocked,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isActive) Gold.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = when {
                        isActive -> BorderStroke(1.dp, Gold)
                        isLocked -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        else -> null
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(job.emoji, style = MaterialTheme.typography.headlineSmall)
                        Column(Modifier.weight(1f)) {
                            Text(
                                job.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                when {
                                    isActive -> "Active • Lv $level"
                                    isLocked -> "✦ Premium"
                                    else -> "Lv $level"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isActive) Gold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestHubCard(
    state: QuestHubState,
    onPrimaryAction: (QuestHubState) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF171C2B).copy(alpha = 0.96f),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.eyebrow,
                    style = MaterialTheme.typography.labelSmall,
                    color = Gold,
                    fontWeight = FontWeight.Black
                )
                state.readinessPercent?.let { readiness ->
                    Text(
                        text = "$readiness% ready",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (readiness >= 75) Color(0xFF6FE39A) else Color(0xFFFFC857),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = state.worldLine,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(state.detail, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f))
            Text(state.outcome, style = MaterialTheme.typography.bodySmall, color = Gold.copy(alpha = 0.9f))
            state.caution?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFC857))
            }

            Button(
                onClick = { onPrimaryAction(state) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = state.primaryLabel },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(state.primaryLabel, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Preview(name = "Quest Hub compact", widthDp = 320, heightDp = 640, showBackground = true)
@Preview(name = "Quest Hub 1.3x font", widthDp = 320, heightDp = 640, fontScale = 1.3f, showBackground = true)
@Composable
private fun QuestHubCardPreview() {
    QuestHubCard(
        state = QuestHubState(
            eyebrow = "YOUR NEXT QUEST",
            title = "Upper Body Expedition",
            detail = "5 exercises • about 40 min",
            readinessPercent = 82,
            caution = "Soreness reported. Adjust load as needed.",
            worldLine = "🌾 Meadowlands  •  🤢 Couch Slime",
            outcome = "Earn workout XP + Gold and prepare to face Couch Slime.",
            primaryLabel = "Begin Quest",
            action = QuestHubAction.BEGIN_QUEST,
            workoutId = 42L
        ),
        onPrimaryAction = {}
    )
}
