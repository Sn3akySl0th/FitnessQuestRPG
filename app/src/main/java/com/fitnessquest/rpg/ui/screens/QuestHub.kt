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
import com.fitnessquest.rpg.ui.components.AvatarCustomizationDialog
import com.fitnessquest.rpg.ui.components.AvatarExpression
import com.fitnessquest.rpg.ui.components.BetaFeedbackDialog
import com.fitnessquest.rpg.ui.components.BetaWalkthroughCard
import com.fitnessquest.rpg.ui.components.DailyBountyCard
import com.fitnessquest.rpg.ui.components.EmbersOverlay
import com.fitnessquest.rpg.ui.components.HeroPaperDoll
import com.fitnessquest.rpg.ui.components.ManualStepEntryDialog
import com.fitnessquest.rpg.ui.components.PedometerScanDialog
import com.fitnessquest.rpg.ui.components.SettingsIconButton
import com.fitnessquest.rpg.ui.components.WeightLogDialog
import com.fitnessquest.rpg.ui.components.noteBetaFeedbackSubmitted
import com.fitnessquest.rpg.ui.components.toAppearance
import com.fitnessquest.rpg.ui.rememberDockContentPadding
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightBg

enum class QuestHubAction {
    RESUME_QUEST,
    BEGIN_QUEST,
    OPEN_TRAINING,
    CHALLENGE_BOSS
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
    val workoutId: Long? = null,
    val bossId: Int? = null
)

internal fun buildQuestHubState(
    recommendation: RoutineRecommendation,
    activeQuest: ActiveQuestSnapshot?,
    recommendedExerciseCount: Int,
    biome: Biome,
    encounter: Monster,
    bossReady: Boolean = false,
    boss: Monster? = null
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

    if (bossReady && boss != null) {
        return QuestHubState(
            eyebrow = "CHAPTER MILESTONE",
            title = "Challenge ${boss.name}",
            detail = "The path to the next biome is blocked. Conquer the boss to advance!",
            readinessPercent = recommendation.readinessPercent,
            caution = null,
            worldLine = worldLine,
            outcome = "Defeat ${boss.name} to earn Epic loot and unlock ${Biome.entries.getOrNull(biome.ordinal + 1)?.label ?: "the next zone"}.",
            primaryLabel = "Challenge Boss",
            action = QuestHubAction.CHALLENGE_BOSS,
            bossId = boss.id
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
    var showMorphSwitcher by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val character = screenState.character
    val heroClass = character?.characterClass

    if (showFeedbackDialog) {
        BetaFeedbackDialog(
            character = character,
            onDismiss = { showFeedbackDialog = false },
            onFeedbackSubmitted = {
                noteBetaFeedbackSubmitted(context)
                showFeedbackDialog = false
            }
        )
    }

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

    if (showMorphSwitcher) {
        MorphSwitchSheet(
            character = character,
            onDismiss = { showMorphSwitcher = false },
            onMorphChange = { newForm ->
                viewModel.setDruidForm(newForm)
                showMorphSwitcher = false
            }
        )
    }

    if (showAvatarDialog) {
        AvatarCustomizationDialog(
            character = character,
            isPremium = isPremium,
            onDismiss = { showAvatarDialog = false },
            onClassChange = { viewModel.switchJob(it) },
            onSave = { skinColor, hairColor, underwearColor, eyeColor, hairStyle, gender, braColor, race ->
                viewModel.updateAppearance(
                    skinColor, hairColor, underwearColor, eyeColor, hairStyle, gender, braColor, race
                )
                showAvatarDialog = false
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
                UnifiedHeroHeader(
                    state = screenState,
                    isQuestHub = true,
                    onAvatarClick = { showAvatarDialog = true },
                    onOpenJobSwitcher = { showJobSwitcher = true },
                    onOpenMorphSwitcher = { showMorphSwitcher = true },
                    onSettingsClick = {},
                    onOpenHero = onOpenHero
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
                                QuestHubAction.CHALLENGE_BOSS -> onOpenBattle()
                            }
                        }
                    )
                }
            }

            item {
                BetaWalkthroughCard(
                    character = screenState.character,
                    onOpenFeedback = { showFeedbackDialog = true },
                    onClaimPioneerReward = { viewModel.claimBetaPioneerReward() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
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
