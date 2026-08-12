package com.fitnessquest.rpg.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.WorkoutEntity
import com.fitnessquest.rpg.data.db.WorkoutExerciseEntity
import com.fitnessquest.rpg.ui.appContainer
import com.fitnessquest.rpg.ui.theme.NightBg
import com.fitnessquest.rpg.ui.components.ExerciseDetailDialog
import com.fitnessquest.rpg.ui.components.SectionCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutDetailUi(
    val workout: WorkoutEntity? = null,
    val exercises: List<WorkoutExerciseEntity> = emptyList(),
    val loading: Boolean = true,
    val inParty: Boolean = false,
    val partyShareNote: String? = null,
    val partyShareError: String? = null,
    val partySharing: Boolean = false,
)

class WorkoutDetailViewModel(private val container: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(WorkoutDetailUi())
    val ui: StateFlow<WorkoutDetailUi> = _ui

    fun load(workoutId: Long) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true) }
            val workout = container.repository.getWorkout(workoutId)
            val exercises = if (workout != null) container.repository.exercisesFor(workoutId) else emptyList()
            _ui.update {
                it.copy(
                    workout = workout,
                    exercises = exercises,
                    loading = false,
                    inParty = container.party.state.value.inParty
                )
            }
        }
        viewModelScope.launch {
            container.party.state.collect { party ->
                _ui.update { it.copy(inParty = party.inParty) }
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = _ui.value.workout?.id ?: return
        viewModelScope.launch {
            container.repository.deleteWorkout(id)
            onDeleted()
        }
    }

    fun shareViaSystem(onReady: (String) -> Unit) {
        val id = _ui.value.workout?.id ?: return
        viewModelScope.launch {
            container.repository.workoutShareText(id)?.let(onReady)
        }
    }

    fun shareToParty() {
        val id = _ui.value.workout?.id ?: return
        _ui.update { it.copy(partySharing = true, partyShareNote = null, partyShareError = null) }
        viewModelScope.launch {
            container.party.shareWorkout(id)
                .onSuccess {
                    _ui.update {
                        it.copy(partySharing = false, partyShareNote = "Shared with your party!")
                    }
                }
                .onFailure { e ->
                    _ui.update {
                        it.copy(partySharing = false, partyShareError = e.message ?: "Share failed")
                    }
                }
        }
    }

    fun dismissPartyNote() = _ui.update { it.copy(partyShareNote = null, partyShareError = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer { WorkoutDetailViewModel(appContainer) }
        }
    }
}

@Composable
fun WorkoutDetailScreen(
    workoutId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onStart: (Long) -> Unit,
    viewModel: WorkoutDetailViewModel = viewModel(factory = WorkoutDetailViewModel.Factory)
) {
    val state by viewModel.ui.collectAsState()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(value = false) }
    var shareMenu by remember { mutableStateOf(false) }
    var detailFor by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(workoutId) { viewModel.load(workoutId) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, workoutId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load(workoutId)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    detailFor?.let { name ->
        ExerciseDetailDialog(name = name) { detailFor = null }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete quest?") },
            text = { Text("This removes \"${state.workout?.name}\" from your training grounds. History is kept.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete(onDeleted = onBack)
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

    if (shareMenu) {
        AlertDialog(
            onDismissRequest = { shareMenu = false },
            title = { Text("Share quest") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Send this workout as text, or post it to your party feed.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        shareMenu = false
                        viewModel.shareViaSystem { text ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, state.workout?.name ?: "FitQuest workout")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share quest"))
                        }
                    }
                ) { Text("Share as text") }
            },
            dismissButton = {
                Row {
                    if (state.inParty) {
                        TextButton(
                            onClick = {
                                shareMenu = false
                                viewModel.shareToParty()
                            }
                        ) { Text("Share to party") }
                    }
                    TextButton(onClick = { shareMenu = false }) { Text("Cancel") }
                }
            }
        )
    }

    state.partyShareNote?.let { note ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPartyNote,
            title = { Text("Shared") },
            text = { Text(note) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissPartyNote) { Text("OK") }
            }
        )
    }
    state.partyShareError?.let { err ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPartyNote,
            title = { Text("Couldn't share") },
            text = { Text(err) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissPartyNote) { Text("OK") }
            }
        )
    }

    Column(Modifier.fillMaxSize()) {
        Surface(
            color = NightBg,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        state.workout?.name ?: "Quest",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.workout?.aiGenerated == true) {
                        Text(
                            "✦ AI-forged",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                IconButton(onClick = { shareMenu = true }, enabled = !state.loading && state.exercises.isNotEmpty()) {
                    Icon(Icons.Filled.Share, contentDescription = "Share")
                }
                IconButton(onClick = { onEdit(workoutId) }, enabled = state.workout != null) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { confirmDelete = true }, enabled = state.workout != null) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.loading) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator() }
                }
            } else if (state.workout == null) {
                item {
                    SectionCard {
                        Text("This quest was not found.", color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = onBack) { Text("Back") }
                    }
                }
            } else {
                item {
                    Text(
                        "${state.exercises.size} exercise${if (state.exercises.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                itemsIndexed(state.exercises, key = { _, e -> e.id }) { index, exercise ->
                    SectionCard {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { detailFor = exercise.exerciseName },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${index + 1}.",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                            )
                            Column(
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    exercise.exerciseName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${exercise.category.label} · builds ${exercise.category.statLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val unit = if (
                                (exercise.category == ExerciseCategory.CARDIO) ||
                                (exercise.category == ExerciseCategory.FLEXIBILITY)
                            ) "min" else "reps"
                            Text(
                                "${exercise.targetSets} × ${exercise.targetReps} $unit",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = { onStart(workoutId) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.exercises.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text("Start quest")
                    }
                }
                item {
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text("Back to Training Grounds")
                    }
                }
            }
        }
    }
}
