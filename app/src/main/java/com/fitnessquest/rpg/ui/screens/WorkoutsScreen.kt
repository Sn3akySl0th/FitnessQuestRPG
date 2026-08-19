package com.fitnessquest.rpg.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.FitQuestApp
import com.fitnessquest.rpg.data.ai.LocalAiEngine
import com.fitnessquest.rpg.data.ai.RoutineRecommendation
import com.fitnessquest.rpg.data.ai.WorkoutRecommendationEngine
import com.fitnessquest.rpg.data.db.ActiveSessionWithDetails
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.data.db.SessionEntity
import com.fitnessquest.rpg.data.db.WorkoutEntity
import com.fitnessquest.rpg.data.importexport.ImportPersistResult
import com.fitnessquest.rpg.data.importexport.ImportedWorkout
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.ClassWorkoutTemplate
import com.fitnessquest.rpg.domain.ClassWorkoutTemplates
import com.fitnessquest.rpg.domain.RewardBatch
import com.fitnessquest.rpg.notifications.WorkoutNotificationController
import com.fitnessquest.rpg.ui.appContainer
import com.fitnessquest.rpg.ui.rememberDockContentPadding
import com.fitnessquest.rpg.ui.components.*
import com.fitnessquest.rpg.ui.theme.Gold
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WorkoutsUiState(
    val workouts: List<WorkoutEntity> = emptyList(),
    val character: CharacterEntity? = null,
    val gear: Map<ItemSlot, ItemEntity> = emptyMap(),
    val template: ClassWorkoutTemplate? = null,
    val renaming: Boolean = false,
    val hevyApiKey: String = "",
    val soreMuscles: Set<String> = emptySet(),
    val wellRestedBuff: Boolean = false,
    val freshnessMap: Map<String, Int> = emptyMap(),
    val recommendation: RoutineRecommendation = RoutineRecommendation(null)
)

class WorkoutsViewModel(private val container: AppContainer) : ViewModel() {
    private val _renaming = MutableStateFlow(false)
    private val notificationController = WorkoutNotificationController(container.app)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<WorkoutsUiState> = combine(
        container.repository.workouts,
        container.repository.character,
        container.repository.sessions,
        container.prefs.soreMuscles,
        container.prefs.wellRestedBuff,
        container.prefs.hevyApiKey,
        _renaming
    ) { flows ->
        val workouts = flows[0] as List<WorkoutEntity>
        val character = flows[1] as CharacterEntity?
        val sessions = flows[2] as List<SessionEntity>
        val soreMuscles = flows[3] as Set<String>
        val wellRested = flows[4] as Boolean
        val hevyApiKey = flows[5] as String
        val renaming = flows[6] as Boolean

        val gearMap = if (character != null) container.repository.equippedGear(character) else emptyMap()
        val freshnessMap = WorkoutRecommendationEngine.calculateMuscleFreshness(sessions, soreMuscles)
        val recommendation = WorkoutRecommendationEngine.recommendNextWorkout(
            routines = workouts,
            recentSessions = sessions,
            soreMuscles = soreMuscles,
            wellRestedBuffActive = wellRested
        )

        WorkoutsUiState(
            workouts = workouts,
            character = character,
            gear = gearMap,
            template = character?.characterClass?.let { ClassWorkoutTemplates.forClass(it) },
            renaming = renaming,
            hevyApiKey = hevyApiKey,
            soreMuscles = soreMuscles,
            wellRestedBuff = wellRested,
            freshnessMap = freshnessMap,
            recommendation = recommendation
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkoutsUiState())

    fun saveHevyApiKey(key: String) {
        container.prefs.setHevyApiKey(key)
    }

    fun toggleSoreMuscle(muscle: String) {
        val current = container.prefs.soreMuscles.value.toMutableSet()
        val normalized = muscle.uppercase()
        if (current.contains(normalized)) current.remove(normalized) else current.add(normalized)
        container.prefs.setSoreMuscles(current)
    }

    fun clearSoreMuscles() {
        container.prefs.setSoreMuscles(emptySet())
    }

    fun completeSideQuest(title: String) {
        viewModelScope.launch {
            container.prefs.setWellRestedBuff(true)
            container.repository.debugGrantGold()
        }
    }

    fun triggerRandomSideQuestNotification() {
        val sideQuests = listOf(
            Pair("Pushup Blitz", "Do 10 quick pushups to earn +50 Gold & activate the 'Well Rested' Buff!"),
            Pair("Jumping Jacks Challenge", "Do 20 jumping jacks to boost your agility & activate the 'Well Rested' Buff!"),
            Pair("Mobility Stretch", "Perform 5 minutes of bodyweight stretching for +15% XP on your next workout!")
        )
        val q = sideQuests.random()
        notificationController.postSideQuestNotification(q.first, q.second)
    }

    fun createClassTemplate(onCreated: (Long) -> Unit) {
        val template = uiState.value.template ?: return
        viewModelScope.launch {
            val id = container.repository.saveWorkout(template.name, template.exercises)
            onCreated(id)
        }
    }

    fun importWorkouts(
        importedWorkouts: List<ImportedWorkout>,
        onResult: (ImportPersistResult, RewardBatch?) -> Unit
    ) {
        viewModelScope.launch {
            val result = container.repository.importExternalWorkouts(
                importedWorkouts = importedWorkouts,
                gemini = container.gemini,
                renameTemplates = true
            )
            val batch = if (result.totalAdded > 0) {
                container.repository.grantImportReward(result.totalAdded)
            } else {
                null
            }
            onResult(result, batch)
        }
    }

    fun renameWithAi(workoutId: Long) {
        if (_renaming.value) return
        viewModelScope.launch {
            _renaming.value = true
            try {
                val workout = container.repository.getWorkout(workoutId) ?: return@launch
                val exercises = container.repository.exercisesFor(workoutId)
                val exerciseNames = exercises.map { it.exerciseName }
                
                container.gemini.renameImportedRoutine(workout.name, exerciseNames).onSuccess { newName ->
                    if (newName != workout.name) {
                        container.repository.updateWorkout(workoutId, newName, exercises)
                    }
                }.onFailure {
                    val localName = LocalAiEngine.fantasyRoutineName(workout.name, exerciseNames)
                    if (localName != workout.name) {
                        container.repository.updateWorkout(workoutId, localName, exercises)
                    }
                }
            } finally {
                _renaming.value = false
            }
        }
    }

    fun renameAllWithAi() {
        if (_renaming.value) return
        viewModelScope.launch {
            _renaming.value = true
            try {
                val list = uiState.value.workouts
                for (workout in list) {
                    val name = workout.name.lowercase()
                    val isGeneric = name.contains("routine") || name.contains("trial") || 
                                    (name.contains("quest") && workout.name.split(" ").size <= 2)
                    
                    if (isGeneric) {
                        val exercises = container.repository.exercisesFor(workout.id)
                        val exerciseNames = exercises.map { it.exerciseName }
                        container.gemini.renameImportedRoutine(workout.name, exerciseNames).onSuccess { newName ->
                            if (newName != workout.name) {
                                container.repository.updateWorkout(workout.id, newName, exercises)
                            }
                        }
                        delay(500)
                    }
                }
            } finally {
                _renaming.value = false
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { WorkoutsViewModel(appContainer) }
        }
    }
}

@Composable
fun WorkoutsScreen(
    onNewWorkout: () -> Unit,
    onAiWorkout: () -> Unit,
    onExerciseLibrary: () -> Unit,
    onHistory: () -> Unit,
    onRecords: () -> Unit,
    onOpenWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    onFreestyle: () -> Unit,
    viewModel: WorkoutsViewModel = viewModel(factory = WorkoutsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val container = (context.applicationContext as FitQuestApp).container
    val activeSessionDetails by container.repository.activeSession.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    WorkoutsScreenContent(
        state = state,
        activeSessionDetails = activeSessionDetails,
        actions = WorkoutsActions(
            onNewWorkout = onNewWorkout,
            onAiWorkout = onAiWorkout,
            onExerciseLibrary = onExerciseLibrary,
            onHistory = onHistory,
            onRecords = onRecords,
            onOpenWorkout = onOpenWorkout,
            onStartWorkout = onStartWorkout,
            onFreestyle = onFreestyle,
            onSaveHevyApiKey = viewModel::saveHevyApiKey,
            onToggleSoreMuscle = viewModel::toggleSoreMuscle,
            onClearSoreMuscles = viewModel::clearSoreMuscles,
            onCompleteSideQuest = viewModel::completeSideQuest,
            onTriggerNotification = viewModel::triggerRandomSideQuestNotification,
            onCreateClassTemplate = { viewModel.createClassTemplate(onOpenWorkout) },
            onImportWorkouts = viewModel::importWorkouts,
            onRenameWithAi = viewModel::renameWithAi,
            onRenameAllWithAi = viewModel::renameAllWithAi,
            onDiscardActiveSession = {
                coroutineScope.launch {
                    container.repository.discardActiveSession()
                }
            }
        )
    )
}

data class WorkoutsActions(
    val onNewWorkout: () -> Unit = {},
    val onAiWorkout: () -> Unit = {},
    val onExerciseLibrary: () -> Unit = {},
    val onHistory: () -> Unit = {},
    val onRecords: () -> Unit = {},
    val onOpenWorkout: (Long) -> Unit = {},
    val onStartWorkout: (Long) -> Unit = {},
    val onFreestyle: () -> Unit = {},
    val onSaveHevyApiKey: (String) -> Unit = {},
    val onToggleSoreMuscle: (String) -> Unit = {},
    val onClearSoreMuscles: () -> Unit = {},
    val onCompleteSideQuest: (String) -> Unit = {},
    val onTriggerNotification: () -> Unit = {},
    val onCreateClassTemplate: () -> Unit = {},
    val onImportWorkouts: (List<ImportedWorkout>, (ImportPersistResult, RewardBatch?) -> Unit) -> Unit = { _, _ -> },
    val onRenameWithAi: (Long) -> Unit = {},
    val onRenameAllWithAi: () -> Unit = {},
    val onDiscardActiveSession: suspend () -> Unit = {}
)

@Composable
fun WorkoutsScreenContent(
    state: WorkoutsUiState,
    activeSessionDetails: ActiveSessionWithDetails?,
    actions: WorkoutsActions
) {
    val workouts = state.workouts
    val scope = rememberCoroutineScope()
    var showPlates by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showSorenessDialog by remember { mutableStateOf(false) }
    var importRewardBatch by remember { mutableStateOf<RewardBatch?>(null) }
    var importSummary by remember { mutableStateOf<String?>(null) }

    var pendingStartWorkoutId by remember { mutableStateOf<Long?>(null) }
    var showActiveSessionPrompt by remember { mutableStateOf(false) }

    fun handleStartWorkout(workoutId: Long) {
        if (activeSessionDetails != null) {
            pendingStartWorkoutId = workoutId
            showActiveSessionPrompt = true
        } else {
            actions.onStartWorkout(workoutId)
        }
    }

    if (showActiveSessionPrompt) {
        AlertDialog(
            onDismissRequest = { showActiveSessionPrompt = false },
            title = { Text("Active Quest in Progress") },
            text = { Text("You already have an active workout in progress. Would you like to resume your active quest or discard it to start a new one?") },
            confirmButton = {
                Button(
                    onClick = {
                        showActiveSessionPrompt = false
                        activeSessionDetails?.session?.let { s ->
                            actions.onStartWorkout(s.workoutId ?: -1L)
                        }
                    }
                ) {
                    Text("Resume Quest")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val nextId = pendingStartWorkoutId ?: -1L
                            scope.launch {
                                showActiveSessionPrompt = false
                                actions.onDiscardActiveSession()
                                actions.onStartWorkout(nextId)
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Discard & Start")
                    }
                    TextButton(onClick = { showActiveSessionPrompt = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showPlates) {
        PlateCalculatorDialog(onDismiss = { showPlates = false })
    }
    if (showSorenessDialog) {
        SorenessCheckInDialog(
            selectedMuscles = state.soreMuscles,
            onToggleMuscle = actions.onToggleSoreMuscle,
            onClearAll = actions.onClearSoreMuscles,
            onDismiss = { showSorenessDialog = false }
        )
    }
    if (showImportDialog) {
        WorkoutImportDialog(
            initialApiKey = state.hevyApiKey,
            onApiKeyChange = actions.onSaveHevyApiKey,
            existingWorkoutNames = workouts.map { it.name }.toSet(),
            onDismiss = { showImportDialog = false },
            onImportWorkouts = { toImport ->
                actions.onImportWorkouts(toImport) { result, batch ->
                    importSummary = "Imported ${result.templatesAdded} training quests and ${result.sessionsAdded} history sessions."
                    importRewardBatch = batch
                }
            }
        )
    }

    importRewardBatch?.let { batch ->
        RewardRevealDialog(batch = batch, onDismiss = { importRewardBatch = null })
    }

    Column(Modifier.fillMaxSize()) {
        SceneBanner(
            kind = SceneKind.TRAIN,
            title = "TRAINING GROUNDS",
            tagline = "Forge the next set"
        ) {
            IconButton(onClick = { showImportDialog = true }) {
                Icon(Icons.Filled.FileDownload, contentDescription = "Import workouts")
            }
            IconButton(onClick = { showPlates = true }) {
                Icon(Icons.Filled.FitnessCenter, contentDescription = "Plate calculator")
            }
            IconButton(onClick = actions.onHistory) {
                Icon(Icons.Filled.History, contentDescription = "History")
            }
            SettingsIconButton()
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = rememberDockContentPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                RecommendedQuestCard(
                    recommendation = state.recommendation,
                    wellRestedBuff = state.wellRestedBuff,
                    onStartWorkout = ::handleStartWorkout,
                    onCompleteSideQuest = actions.onCompleteSideQuest,
                    onOpenSorenessDialog = { showSorenessDialog = true },
                    onTriggerNotification = actions.onTriggerNotification
                )
            }

            item {
                HeroAnatomyHeatmap(
                    character = state.character,
                    freshnessMap = state.freshnessMap,
                    soreMuscles = state.soreMuscles,
                    onToggleMuscle = actions.onToggleSoreMuscle
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = actions.onNewWorkout, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("New quest", maxLines = 1)
                        }
                        Button(onClick = actions.onAiWorkout, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("AI Forge", maxLines = 1)
                        }
                    }
                    OutlinedButton(onClick = { handleStartWorkout(-1L) }, modifier = Modifier.fillMaxWidth()) {
                        Text("🏃 Freestyle session — log anything")
                    }
                    OutlinedButton(onClick = actions.onExerciseLibrary, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Exercise Library", maxLines = 1)
                    }
                    OutlinedButton(onClick = { showImportDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import Workouts", maxLines = 1)
                    }
                    
                    if (workouts.any { it.name.lowercase().contains("routine") || it.name.lowercase().contains("trial") }) {
                        OutlinedButton(
                            onClick = actions.onRenameAllWithAi,
                            enabled = !state.renaming,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            if (state.renaming) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Fantasy-fy all generic names", maxLines = 1)
                        }
                    }

                    importSummary?.let { summary ->
                        Text(summary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    
                    state.template?.let { template ->
                        SectionCard {
                            Text("${state.character?.characterClass?.emoji.orEmpty()} ${template.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(template.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(template.exercises.joinToString(" · ") { it.exerciseName }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            Button(onClick = actions.onCreateClassTemplate, modifier = Modifier.fillMaxWidth()) {
                                Text("Create class quest")
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = actions.onHistory, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.History, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("History", maxLines = 1)
                        }
                        OutlinedButton(onClick = actions.onRecords, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Records", maxLines = 1)
                        }
                    }
                }
            }

            if (workouts.isEmpty()) {
                item {
                    SectionCard {
                        Text("No training quests yet. Create a workout, let the AI forge one, or start a freestyle session.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(workouts, key = { it.id }) { workout ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clickable { actions.onOpenWorkout(workout.id) }) {
                            Text(workout.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(if (workout.aiGenerated) "✨ AI-forged" else "Tap to view or edit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { actions.onRenameWithAi(workout.id) }, enabled = !state.renaming) {
                            if (state.renaming) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Filled.AutoAwesome, contentDescription = "Rename with AI", tint = MaterialTheme.colorScheme.tertiary)
                        }
                        Button(onClick = { handleStartWorkout(workout.id) }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Text("Start")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendedQuestCard(
    recommendation: RoutineRecommendation,
    wellRestedBuff: Boolean,
    onStartWorkout: (Long) -> Unit,
    onCompleteSideQuest: (String) -> Unit,
    onOpenSorenessDialog: () -> Unit,
    onTriggerNotification: () -> Unit
) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (recommendation.isRestDay) "🛡️ Recommended Recovery" else "⚔️ Recommended Quest",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (recommendation.readinessPercent >= 85) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        "${recommendation.readinessPercent}% Primed",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                recommendation.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )

            Text(
                recommendation.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (wellRestedBuff) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("✨", fontSize = 16.sp)
                        Text(
                            "\"Well Rested\" Buff Active: +15% XP & Gold on Next Workout!",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            recommendation.cautionWarning?.let { warning ->
                Text(
                    warning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                if (recommendation.isRestDay) {
                    Button(
                        onClick = { recommendation.sideQuestTitle?.let { onCompleteSideQuest(it) } },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🛡️ Complete Side Quest (+Buff)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else if (recommendation.routine != null) {
                    val routineId = recommendation.routine.id
                    Button(
                        onClick = { onStartWorkout(routineId) },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Start Recommended Quest")
                    }
                }

                OutlinedButton(
                    onClick = onOpenSorenessDialog,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("🩹 Soreness Check-in")
                }
            }
        }
    }
}

@Composable
private fun SorenessCheckInDialog(
    selectedMuscles: Set<String>,
    onToggleMuscle: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val allMuscles = listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Quads", "Hamstrings", "Glutes", "Calves", "Abs")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🩹 Muscle Soreness Check-in") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Flag muscles that feel sore or fatigued. FitQuest will auto-adjust routine recommendations and warn against heavy movements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allMuscles.chunked(3).forEach { rowMuscles ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowMuscles.forEach { muscle ->
                                val isSelected = selectedMuscles.contains(muscle) || selectedMuscles.contains(muscle.uppercase())
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onToggleMuscle(muscle) },
                                    label = { Text(muscle) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        dismissButton = {
            if (selectedMuscles.isNotEmpty()) {
                TextButton(onClick = onClearAll) { Text("Clear All") }
            }
        }
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF12131F)
@androidx.compose.runtime.Composable
fun WorkoutsScreenPreview() {
    com.fitnessquest.rpg.ui.theme.FitQuestTheme {
        WorkoutsScreenContent(
            state = WorkoutsUiState(
                workouts = listOf(
                    WorkoutEntity(id = 1, name = "Dragon Slayer Strength", aiGenerated = true),
                    WorkoutEntity(id = 2, name = "Meadowlands Cardio", aiGenerated = false)
                ),
                character = CharacterEntity(name = "Preview Hero", characterClass = CharacterClass.WARRIOR),
                recommendation = RoutineRecommendation(routine = null, title = "Morning Drill", reason = "Time to level up!")
            ),
            activeSessionDetails = null,
            actions = WorkoutsActions()
        )
    }
}
