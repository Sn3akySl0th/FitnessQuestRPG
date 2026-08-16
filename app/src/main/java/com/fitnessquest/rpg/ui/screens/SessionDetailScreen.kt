package com.fitnessquest.rpg.ui.screens

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.SessionEntity
import com.fitnessquest.rpg.data.db.SessionReceiptCodec
import com.fitnessquest.rpg.data.db.SetLogEntity
import com.fitnessquest.rpg.domain.*
import com.fitnessquest.rpg.ui.appContainer
import com.fitnessquest.rpg.ui.components.AvatarDetail
import com.fitnessquest.rpg.ui.components.CharacterAvatar
import com.fitnessquest.rpg.ui.components.ExerciseDetailDialog
import com.fitnessquest.rpg.ui.components.SectionCard
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightBg
import com.fitnessquest.rpg.ui.theme.StatEnd
import com.fitnessquest.rpg.ui.theme.StatStr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class SessionDetailUi(
    val session: SessionEntity? = null,
    val logs: List<SetLogEntity> = emptyList(),
    val result: SessionResult? = null,
    val loading: Boolean = true
)

class SessionDetailViewModel(private val container: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(SessionDetailUi())
    val ui: StateFlow<SessionDetailUi> = _ui

    val imperial: StateFlow<Boolean> = container.prefs.imperial

    fun load(sessionId: Long) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true) }
            val pair = container.repository.getSessionWithLogs(sessionId)
            val char = container.repository.getCharacter()
            
            // Try to deserialize the result from the receipt
            val result = pair?.first?.completionReceiptJson?.let { 
                SessionReceiptCodec.deserialize(it, char)
            }

            _ui.update {
                it.copy(
                    session = pair?.first,
                    logs = pair?.second ?: emptyList(),
                    result = result,
                    loading = false
                )
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = _ui.value.session?.id ?: return
        viewModelScope.launch {
            container.repository.deleteSession(id, container.auth.currentUid())
            onDeleted()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SessionDetailViewModel(appContainer) }
        }
    }
}

@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = viewModel(factory = SessionDetailViewModel.Factory)
) {
    val state by viewModel.ui.collectAsState()
    val imperial by viewModel.imperial.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }
    var detailFor by remember { mutableStateOf<String?>(null) }
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]

    LaunchedEffect(sessionId) { viewModel.load(sessionId) }

    detailFor?.let { name ->
        ExerciseDetailDialog(name = name) { detailFor = null }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete from Chronicle?") },
            text = { Text("This will remove this workout and revert all XP/Gold earned. Your level may decrease.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

    Column(Modifier.fillMaxSize().background(NightBg)) {
        // Custom Top Bar
        Surface(color = NightBg, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(state.session?.name ?: "Workout Detail", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        state.session?.let { s ->
                            val fmt = remember(locale) { SimpleDateFormat("EEEE, MMM d, yyyy \u00B7 h:mm a", locale) }
                            Text(fmt.format(Date(s.endedAt)), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                        }
                        if (state.result?.avgHr != null) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.Watch, null, tint = StatEnd, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Wear OS", style = MaterialTheme.typography.labelSmall, color = StatEnd)
                        }
                    }
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Gold)
            }
        } else if (state.session == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Workout not found", color = Color.White)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Stats
                item {
                    val res = state.result
                    val vol = res?.volumeKg ?: state.logs.filter { it.category == ExerciseCategory.STRENGTH }.sumOf { it.weightKg * it.reps }
                    val dur = state.session!!.endedAt - state.session!!.startedAt
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        SummaryStatItem("Time", formatDuration(dur), Gold)
                        SummaryStatItem("Volume", "${Units.toDisplay(vol, imperial).roundToInt()} ${Units.label(imperial)}", Gold)
                        SummaryStatItem("Sets", state.session!!.setCount.toString(), Gold)
                    }
                }

                // XP / Gold / Calories
                item {
                    val res = state.result
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        SummaryStatItem("XP", "+${state.session!!.xpEarned}", MaterialTheme.colorScheme.primary)
                        SummaryStatItem("Gold", "+${state.session!!.goldEarned}g", Gold)
                        if (res?.caloriesKcal != null) {
                            SummaryStatItem("Calories", "${res.caloriesKcal} kcal", Color(0xFFFF9800))
                        }
                    }
                }

                // Muscle Split (If available)
                state.result?.musclesWorked?.let { muscles ->
                    if (muscles.isNotEmpty()) {
                        item {
                            SectionCard(title = "Muscle Split") {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                                        state.result?.updatedCharacter?.let { hero ->
                                            CharacterAvatar(
                                                clazz = hero.characterClass ?: CharacterClass.WARRIOR,
                                                modifier = Modifier.size(70.dp),
                                                gear = emptyMap(),
                                                highlightMuscles = muscles,
                                                detail = AvatarDetail.COMPACT
                                            )
                                        }
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        muscles.take(5).forEach { muscle ->
                                            Text("\u2022 ${muscle.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Heart Rate (Avg/Max if available)
                state.result?.let { res ->
                    if (res.avgHr != null || res.maxHr != null) {
                        item {
                            SectionCard(title = "Heart Rate") {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    if (res.avgHr != null) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Average", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                            Text("${res.avgHr} bpm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatEnd)
                                        }
                                    }
                                    if (res.maxHr != null) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Peak", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                            Text("${res.maxHr} bpm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE35B5B))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Personal Records Set
                state.result?.prs?.filter { it.isNew }?.let { newPrs ->
                    if (newPrs.isNotEmpty()) {
                        item {
                            SectionCard(title = "\uD83C\uDFC6 Personal Records Set") {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    newPrs.forEach { pr ->
                                        val label = when (pr.kind) {
                                            PrKind.WEIGHT -> "Weight"; PrKind.VOLUME -> "Volume"; PrKind.ONE_RM -> "1RM"; PrKind.DISTANCE -> "Distance"; PrKind.PACE -> "Pace"; PrKind.TIME -> "Duration"; PrKind.REPS -> "Reps"; PrKind.SPEED -> "Speed"; PrKind.INCLINE -> "Incline"
                                        }
                                        val valText = when (pr.kind) {
                                            PrKind.WEIGHT -> "${Units.trimmed(Units.toDisplay(pr.value, imperial))} ${Units.label(imperial)} \u00D7 ${pr.reps}"; PrKind.DISTANCE -> Units.formatDistance(pr.value, imperial); PrKind.PACE -> Units.formatPace(pr.value, imperial); PrKind.TIME -> Units.formatTimeMinutes(pr.value); PrKind.REPS -> "${pr.value.toInt()} reps"; PrKind.SPEED -> Units.formatSpeed(pr.value, imperial); PrKind.INCLINE -> "${Units.trimmed(pr.value)}%"; else -> "${Units.trimmed(Units.toDisplay(pr.value, imperial))} ${Units.label(imperial)}"
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(6.dp).clip(CircleShape).background(Gold))
                                            Spacer(Modifier.width(8.dp))
                                            Text("${pr.exerciseName}: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("$label $valText", style = MaterialTheme.typography.bodySmall, color = Gold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Exercise List
                itemsIndexed(state.logs.groupBy { it.exerciseName }.toList()) { _, (name, sets) ->
                    ExerciseDetailCard(name, sets, imperial) { detailFor = name }
                }
                
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SummaryStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
    }
}

@Composable
private fun ExerciseDetailCard(name: String, sets: List<SetLogEntity>, imperial: Boolean, onTitleClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onTitleClick() }.padding(bottom = 8.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (sets.first().category == ExerciseCategory.STRENGTH) StatStr else Gold))
            Spacer(Modifier.width(8.dp))
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Gold)
        }
        
        // Table Header
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SET", Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f), textAlign = TextAlign.Center)
            Text("PERFORMANCE", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
            Text("XP", Modifier.width(50.dp), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f), textAlign = TextAlign.End)
        }

        sets.forEachIndexed { i, set ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", Modifier.width(32.dp), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                
                val perfText = when (set.category) {
                    ExerciseCategory.STRENGTH -> "${Units.formatWeight(set.weightKg, imperial)} \u00D7 ${set.reps}${set.rir?.let { " @ RPE ${10-it}" }.orEmpty()}"
                    ExerciseCategory.CARDIO -> buildString {
                        append(Units.formatTimeMinutes(set.durationMin))
                        if (set.distanceKm > 0) append(" \u00B7 ${Units.formatDistance(set.distanceKm, imperial)}")
                        if (set.inclinePercent > 0) append(" \u00B7 ${trimDouble(set.inclinePercent)}%")
                    }
                    else -> "${set.reps} reps"
                }
                
                Text(perfText, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Color.White)
                
                Text("+${set.xp}", Modifier.width(50.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider(Modifier.padding(top = 12.dp), color = Color.White.copy(alpha = 0.1f))
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    return if (h > 0) "%dh %dm".format(h, m) else "%dm".format(m)
}

private fun trimDouble(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else "%.1f".format(d)
