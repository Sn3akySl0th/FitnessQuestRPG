package com.fitnessquest.rpg.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.WorkoutExerciseEntity
import com.fitnessquest.rpg.ui.appContainer
import com.fitnessquest.rpg.ui.components.ExerciseDetailDialog
import com.fitnessquest.rpg.ui.components.ExercisePickerDialog
import com.fitnessquest.rpg.ui.components.SectionCard
import kotlinx.coroutines.launch

data class DraftExercise(
    val name: String,
    val category: ExerciseCategory,
    val sets: Int = 3,
    val reps: Int = 10,
    val weightKg: Double? = null,
)

class WorkoutEditorViewModel(private val container: AppContainer) : ViewModel() {
    fun save(name: String, drafts: List<DraftExercise>, workoutId: Long, onSaved: () -> Unit) {
        viewModelScope.launch {
            val exercises = drafts.map {
                WorkoutExerciseEntity(
                    workoutId = 0,
                    exerciseName = it.name,
                    category = it.category,
                    targetSets = it.sets,
                    targetReps = it.reps,
                    targetWeightKg = it.weightKg
                )
            }

            val title = name.trim().ifEmpty { "Unnamed Quest" }
            if (workoutId > 0) {
                container.repository.updateWorkout(workoutId, title, exercises)
            } else {
                container.repository.saveWorkout(title, exercises)
            }
            onSaved()
        }
    }

    suspend fun loadDraft(workoutId: Long): Pair<String, List<DraftExercise>>? {
        val workout = container.repository.getWorkout(workoutId) ?: return null
        val exercises = container.repository.exercisesFor(workoutId)
        return workout.name to exercises.map {
            DraftExercise(
                name = it.exerciseName,
                category = it.category,
                sets = it.targetSets,
                reps = it.targetReps,
                weightKg = it.targetWeightKg
            )
        }
    }


    companion object {
        val Factory = viewModelFactory {
            initializer { WorkoutEditorViewModel(appContainer) }
        }
    }
}

@Composable
fun WorkoutEditorScreen(
    workoutId: Long = -1L,
    onDone: () -> Unit,
    viewModel: WorkoutEditorViewModel = viewModel(factory = WorkoutEditorViewModel.Factory)
) {
    var name by remember { mutableStateOf("") }
    val drafts = remember { mutableListOf<DraftExercise>().toMutableStateList() }
    var showPicker by remember { mutableStateOf(value = false) }
    var loading by remember { mutableStateOf(workoutId > 0) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(workoutId) {
        if (workoutId <= 0) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        val loaded = viewModel.loadDraft(workoutId)
        if (loaded == null) {
            loadFailed = true
        } else {
            name = loaded.first
            drafts.clear()
            drafts.addAll(loaded.second)
        }
        loading = false
    }

    if (loading) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) { CircularProgressIndicator() }
        return
    }

    if (loadFailed) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Could not load this quest.", color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onDone) { Text("Back") }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                if (workoutId > 0) "Edit quest" else "Forge a Workout",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Workout name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        itemsIndexed(drafts) { index, draft ->
            var showDetail by remember { mutableStateOf(false) }
            if (showDetail) {
                ExerciseDetailDialog(name = draft.name) { showDetail = false }
            }
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable { showDetail = true }
                    ) {
                        Text(draft.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${draft.category.label} → ${draft.category.statLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { drafts.removeAt(index) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.sets.toString(),
                        onValueChange = { v ->
                            drafts[index] = draft.copy(sets = v.toIntOrNull()?.coerceIn(1, 20) ?: draft.sets)
                        },
                        label = { Text("Sets") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp)
                    )
                    OutlinedTextField(
                        value = draft.reps.toString(),
                        onValueChange = { v ->
                            drafts[index] = draft.copy(reps = v.toIntOrNull()?.coerceIn(1, 500) ?: draft.reps)
                        },
                        label = {
                            Text(
                                if ((draft.category == ExerciseCategory.CARDIO) ||
                                    (draft.category == ExerciseCategory.FLEXIBILITY)
                                ) "Minutes" else "Reps"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp)
                    )
                    OutlinedTextField(
                        value = draft.weightKg?.toString() ?: "",
                        onValueChange = { v ->
                            drafts[index] = draft.copy(weightKg = v.toDoubleOrNull())
                        },
                        label = { Text("Kg") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(100.dp)
                    )
                }
            }

        }

        item {
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add exercise")
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { viewModel.save(name, drafts, workoutId) { onDone() } },
                    enabled = drafts.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text(if (workoutId > 0) "Save changes" else "Save workout") }
            }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            onDismiss = { showPicker = false },
            onPick = { pickedName, category ->
                drafts.add(
                    DraftExercise(
                        name = pickedName,
                        category = category,
                        reps = if (category == ExerciseCategory.CARDIO || category == ExerciseCategory.FLEXIBILITY) 15 else 10
                    )
                )
            }
        )
    }
}
