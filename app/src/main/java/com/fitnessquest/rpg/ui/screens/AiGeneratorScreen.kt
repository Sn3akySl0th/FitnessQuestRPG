package com.fitnessquest.rpg.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.data.ai.AiExercise
import com.fitnessquest.rpg.data.ai.AiWorkout
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.WorkoutExerciseEntity
import com.fitnessquest.rpg.domain.Equipment
import com.fitnessquest.rpg.domain.FitnessLevel
import com.fitnessquest.rpg.domain.Split
import com.fitnessquest.rpg.domain.TrainingProfile
import com.fitnessquest.rpg.ui.appContainer
import com.fitnessquest.rpg.ui.components.SectionCard
import com.fitnessquest.rpg.ui.theme.Gold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AiForgeMode { Describe, Paste, Photo }

data class AiUiState(
    val hasKey: Boolean = true,
    val localReady: Boolean = false,
    val mode: AiForgeMode = AiForgeMode.Describe,
    val generating: Boolean = false,
    val results: List<AiWorkout> = emptyList(),
    val error: String? = null,
    val saved: Boolean = false,
    val saving: Boolean = false
)


class AiGeneratorViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AiUiState(
            hasKey = container.gemini.isAvailable,
            localReady = container.gemini.isLocalModelReady()
        )
    )
    val uiState: StateFlow<AiUiState> = _uiState

    val profile: StateFlow<TrainingProfile> = container.prefs.profile

    fun updateProfile(update: (TrainingProfile) -> TrainingProfile) {
        container.prefs.saveProfile(update(container.prefs.profile.value))
    }

    fun setMode(mode: AiForgeMode) {
        _uiState.update { it.copy(mode = mode, error = null) }
    }

    fun saveKey(key: String) {
        container.gemini.apiKey = key
        _uiState.update { it.copy(hasKey = container.gemini.isAvailable) }
    }

    fun generate(request: String) {
        if (request.isBlank()) return
        _uiState.update { it.copy(generating = true, error = null, results = emptyList()) }
        viewModelScope.launch {
            val history = container.repository.recentWorkoutsSummary()
            container.gemini.generateWorkout(request, container.prefs.profile.value, history)
                .onSuccess { workouts ->
                    _uiState.update { it.copy(generating = false, results = workouts) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(generating = false, error = e.message ?: "Generation failed") }
                }
        }
    }

    fun importFromText(raw: String) {
        if (raw.isBlank()) return
        _uiState.update { it.copy(generating = true, error = null, results = emptyList()) }
        viewModelScope.launch {
            val history = container.repository.recentWorkoutsSummary()
            container.gemini.importWorkoutFromText(raw, container.prefs.profile.value, history)
                .onSuccess { workouts ->
                    _uiState.update { it.copy(generating = false, results = workouts) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(generating = false, error = e.message ?: "Import failed") }
                }
        }
    }

    fun importFromImage(bytes: ByteArray, mimeType: String, hint: String?) {
        if (bytes.isEmpty()) return
        _uiState.update { it.copy(generating = true, error = null, results = emptyList()) }
        viewModelScope.launch {
            val history = container.repository.recentWorkoutsSummary()
            container.gemini.importWorkoutFromImage(
                imageBytes = bytes,
                mimeType = mimeType,
                optionalHint = hint,
                profile = container.prefs.profile.value,
                history = history
            )
                .onSuccess { workouts ->
                    _uiState.update { it.copy(generating = false, results = workouts) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(generating = false, error = e.message ?: "Import failed") }
                }
        }
    }

    fun updateResult(index: Int, workout: AiWorkout) {
        _uiState.update { s ->
            val updated = s.results.toMutableList()
            if (index in updated.indices) {
                updated[index] = workout
                s.copy(results = updated)
            } else s
        }
    }

    fun removeResult(index: Int) {
        _uiState.update { s ->
            val updated = s.results.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
                s.copy(results = updated)
            } else s
        }
    }

    fun saveWorkout(index: Int, onSaved: () -> Unit) {
        if (_uiState.value.saving) return
        val workout = _uiState.value.results.getOrNull(index) ?: return
        _uiState.update { it.copy(saving = true) }
        
        viewModelScope.launch {
            try {
                container.repository.saveWorkout(
                    name = workout.name,
                    exercises = workout.exercises.map {
                        WorkoutExerciseEntity(
                            workoutId = 0,
                            exerciseName = it.name,
                            category = it.category,
                            targetSets = it.sets,
                            targetReps = it.reps,
                            targetWeightKg = it.weightKg
                        )
                    },
                    aiGenerated = true
                )
                // Remove from local results list so we don't save it twice
                _uiState.update { s ->
                    val updated = s.results.toMutableList()
                    if (index in updated.indices) {
                        updated.removeAt(index)
                    }
                    s.copy(results = updated, saving = false)
                }
                
                if (_uiState.value.results.isEmpty()) {
                    _uiState.update { it.copy(saved = true) }
                    onSaved()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(saving = false, error = "Save failed: ${e.message}") }
            }
        }
    }


    fun saveAll(onSaved: () -> Unit) {
        if (_uiState.value.saving) return
        val workouts = _uiState.value.results.toList()
        if (workouts.isEmpty()) return
        _uiState.update { it.copy(saving = true) }

        viewModelScope.launch {
            try {
                workouts.forEach { workout ->
                    container.repository.saveWorkout(
                        name = workout.name,
                        exercises = workout.exercises.map {
                            WorkoutExerciseEntity(
                                workoutId = 0,
                                exerciseName = it.name,
                                category = it.category,
                                targetSets = it.sets,
                                targetReps = it.reps,
                                targetWeightKg = it.weightKg
                            )
                        },
                        aiGenerated = true
                    )
                }
                _uiState.update { it.copy(results = emptyList(), saved = true) }
                onSaved()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Save failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(saving = false) }
            }
        }
    }



    companion object {
        val Factory = viewModelFactory {
            initializer { AiGeneratorViewModel(appContainer) }
        }
    }
}

@Composable
fun AiGeneratorScreen(
    onDone: () -> Unit,
    viewModel: AiGeneratorViewModel = viewModel(factory = AiGeneratorViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val context = LocalContext.current
    var request by remember { mutableStateOf("") }
    var pasteText by remember { mutableStateOf("") }
    var photoHint by remember { mutableStateOf("") }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var photoMime by remember { mutableStateOf("image/jpeg") }
    var keyInput by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                photoBytes = bytes
                photoMime = mime
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("\u2728 The AI Forge", style = MaterialTheme.typography.headlineMedium)
                Text(
                    when (state.mode) {
                        AiForgeMode.Describe ->
                            "Describe the training quest you seek, and the Forge shall craft it."
                        AiForgeMode.Paste ->
                            "Paste a workout from Strong, Hevy, Apple Fitness, or your notes."
                        AiForgeMode.Photo ->
                            "Choose a screenshot of a workout and the Forge will read it."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            TrainingProfileCard(
                profile = profile,
                onUpdate = viewModel::updateProfile
            )
        }

        if (!state.hasKey && !state.localReady) {
            item {
                SectionCard(title = "AI Engine Required") {
                    Text(
                        "The Forge can be powered by Google Gemini (online) or a Local AI model (offline).\n\n" +
                            "To proceed, either enter a Gemini API key or download a model in Settings.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("API key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { viewModel.saveKey(keyInput) },
                        enabled = keyInput.isNotBlank()
                    ) { Text("Save key") }
                    
                    TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                        Text("Go to Settings to download local model")
                    }
                }
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiForgeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.mode == mode,
                            onClick = { viewModel.setMode(mode) },
                            label = { Text(mode.name) }
                        )
                    }
                }
            }

            when (state.mode) {
                AiForgeMode.Describe -> {
                    item {
                        OutlinedTextField(
                            value = request,
                            onValueChange = { request = it },
                            label = { Text("e.g. \"45 min upper body\"") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        ForgeActionButton(
                            label = "Forge my workout",
                            busyLabel = "Forging...",
                            enabled = request.isNotBlank() && !state.generating,
                            generating = state.generating,
                            onClick = { viewModel.generate(request) }
                        )
                    }
                }
                AiForgeMode.Paste -> {
                    item {
                        OutlinedTextField(
                            value = pasteText,
                            onValueChange = { pasteText = it },
                            label = { Text("Paste workout text here") },
                            minLines = 8,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        ForgeActionButton(
                            label = "Import with AI",
                            busyLabel = "Importing...",
                            enabled = pasteText.isNotBlank() && !state.generating,
                            generating = state.generating,
                            onClick = { viewModel.importFromText(pasteText) }
                        )
                    }
                }
                AiForgeMode.Photo -> {
                    item {
                        val bytes = photoBytes
                        if (bytes != null) {
                            val bitmap = remember(bytes) {
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Selected workout screenshot",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        } else {
                            Text(
                                "No screenshot selected yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (photoBytes == null) "Choose screenshot" else "Choose different photo") }
                    }
                    item {
                        OutlinedTextField(
                            value = photoHint,
                            onValueChange = { photoHint = it },
                            label = { Text("Optional hint (e.g. \"leg day from Hevy\")") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        ForgeActionButton(
                            label = "Import with AI",
                            busyLabel = "Importing...",
                            enabled = (photoBytes != null) && !state.generating,
                            generating = state.generating,
                            onClick = {
                                photoBytes?.let { bytes ->
                                    viewModel.importFromImage(
                                        bytes = bytes,
                                        mimeType = photoMime,
                                        hint = photoHint.takeIf { it.isNotBlank() }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        state.error?.let { error ->
            item {
                SectionCard {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        state.results.forEachIndexed { index, workout ->
            item(key = "res_$index") {
                EditableWorkoutCard(
                    workout = workout,
                    saving = state.saving,
                    onUpdate = { viewModel.updateResult(index, it) },
                    onRemove = { viewModel.removeResult(index) },
                    onSave = { viewModel.saveWorkout(index, onDone) }
                )
            }
        }

        if (state.results.size > 1) {
            item {
                Button(
                    onClick = { viewModel.saveAll(onDone) },
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save all ${state.results.size} quests") }
            }
        }


        item {
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    }
}

@Composable
private fun EditableWorkoutCard(
    workout: AiWorkout,
    saving: Boolean,
    onUpdate: (AiWorkout) -> Unit,
    onRemove: () -> Unit,
    onSave: () -> Unit
) {
    SectionCard {
        var editing by remember { mutableStateOf(false) }
        var name by remember(workout.name) { mutableStateOf(workout.name) }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (editing) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Quest Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(workout.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    workout.description?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            IconButton(onClick = { 
                if (editing) onUpdate(workout.copy(name = name))
                editing = !editing 
            }, enabled = !saving) {
                Icon(if (editing) Icons.Filled.Check else Icons.Filled.Edit, contentDescription = null)
            }
            IconButton(onClick = onRemove, enabled = !saving) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.height(8.dp))

        workout.exercises.forEachIndexed { i, ex ->
            EditableExerciseRow(
                exercise = ex,
                onUpdate = { updated ->
                    val newList = workout.exercises.toMutableList()
                    newList[i] = updated
                    onUpdate(workout.copy(exercises = newList))
                }
            )
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onSave,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth()
        ) { 
            if (saving) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
            }
            Text("Save quest") 
        }
    }
}


@Composable
private fun EditableExerciseRow(
    exercise: AiExercise,
    onUpdate: (AiExercise) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${exercise.sets} \u00D7 ${exercise.reps} ${if (exercise.category == ExerciseCategory.CARDIO) "min" else "reps"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (exercise.weightKg != null) {
                Text(
                    "${exercise.weightKg} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = Gold
                )
            }
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
        
        if (expanded) {
            Column(
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (exercise.suggestionReason != null) {
                    Text(
                        "\uD83D\uDCA1 ${exercise.suggestionReason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(6.dp)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallNumberField(
                        value = exercise.sets.toString(),
                        onValueChange = { it.toIntOrNull()?.let { s -> onUpdate(exercise.copy(sets = s)) } },
                        label = "Sets",
                        modifier = Modifier.weight(1f)
                    )
                    SmallNumberField(
                        value = exercise.reps.toString(),
                        onValueChange = { it.toIntOrNull()?.let { r -> onUpdate(exercise.copy(reps = r)) } },
                        label = if (exercise.category == ExerciseCategory.CARDIO) "Min" else "Reps",
                        modifier = Modifier.weight(1f)
                    )
                    SmallNumberField(
                        value = exercise.weightKg?.toString() ?: "",
                        onValueChange = { onUpdate(exercise.copy(weightKg = it.toDoubleOrNull())) },
                        label = "Kg",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 10.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.height(48.dp),
        textStyle = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun ForgeActionButton(
    label: String,
    busyLabel: String,
    enabled: Boolean,
    generating: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (generating) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(18.dp),
                strokeWidth = 2.dp
            )
            Text(busyLabel)
        } else {
            Text(label)
        }
    }
}

/**
 * Persistent training profile: gear, schedule, split, and experience level.
 * Collapsed by default to a one-line summary; every generated workout uses it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrainingProfileCard(
    profile: TrainingProfile,
    onUpdate: ((TrainingProfile) -> TrainingProfile) -> Unit
) {
    var expanded by remember { mutableStateOf(value = false) }
    SectionCard {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "\uD83C\uDFAF Training Profile",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${profile.level.label} \u00B7 ${profile.daysPerWeek}x/week \u00B7 ${profile.split.label} \u00B7 " +
                        "${profile.equipment.size} gear type${if (profile.equipment.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                if (expanded) "\u25B2" else "\u25BC",
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (expanded) {
            Text("My equipment", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Equipment.entries.forEach { gear ->
                    FilterChip(
                        selected = gear in profile.equipment,
                        onClick = {
                            onUpdate { p ->
                                val updated = if (gear in p.equipment) p.equipment - gear else p.equipment + gear
                                // Never allow an empty arsenal.
                                p.copy(equipment = updated.ifEmpty { setOf(Equipment.BODYWEIGHT) })
                            }
                        },
                        label = { Text(gear.label) }
                    )
                }
            }

            Text("Days per week", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                (1..7).forEach { days ->
                    FilterChip(
                        selected = profile.daysPerWeek == days,
                        onClick = { onUpdate { p -> p.copy(daysPerWeek = days) } },
                        label = { Text(days.toString()) }
                    )
                }
            }

            Text("Workout split", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Split.entries.forEach { split ->
                    FilterChip(
                        selected = profile.split == split,
                        onClick = { onUpdate { p -> p.copy(split = split) } },
                        label = { Text(split.label) }
                    )
                }
            }
            if (profile.recommendedSplit != profile.split) {
                Text(
                    "Recommended for ${profile.daysPerWeek} days/week: ${profile.recommendedSplit.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Text("Fitness level", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FitnessLevel.entries.forEach { level ->
                    FilterChip(
                        selected = profile.level == level,
                        onClick = { onUpdate { p -> p.copy(level = level) } },
                        label = { Text(level.label) }
                    )
                }
            }
        }
    }
}
