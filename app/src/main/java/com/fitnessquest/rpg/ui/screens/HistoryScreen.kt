package com.fitnessquest.rpg.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.SessionEntity
import com.fitnessquest.rpg.data.db.SetLogEntity
import com.fitnessquest.rpg.domain.PrKind
import com.fitnessquest.rpg.domain.SetType
import com.fitnessquest.rpg.domain.Units
import com.fitnessquest.rpg.ui.appContainer
import com.fitnessquest.rpg.ui.rememberDockContentPadding
import com.fitnessquest.rpg.ui.components.ExerciseDetailDialog
import com.fitnessquest.rpg.ui.components.SectionCard
import com.fitnessquest.rpg.ui.theme.NightBg
import com.fitnessquest.rpg.ui.theme.StatAgi
import com.fitnessquest.rpg.ui.theme.StatEnd
import com.fitnessquest.rpg.ui.theme.StatStr
import com.fitnessquest.rpg.ui.theme.StatWil
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/** A personal record: the best performance ever logged for an exercise. */
data class PersonalRecord(
    val exerciseName: String,
    val kind: PrKind,
    val value: Double,
    val reps: Int,
    val date: Long,
    val sessionId: Long,
)

data class LogUiState(
    val sessions: List<SessionEntity> = emptyList(),
    val logsBySession: Map<Long, List<SetLogEntity>> = emptyMap()
) {
    val totalVolumeKg: Double
        get() = logsBySession.values.asSequence().flatten().sumOf { it.weightKg * it.reps }

    /** Best performance per exercise/metric, most recent first. */
    val records: List<PersonalRecord>
        get() {
            val endedAt = sessions.associateBy({ it.id }, { it.endedAt })
            val allLogs = logsBySession.values.flatten()
            val results = mutableListOf<PersonalRecord>()
            
            allLogs.groupBy { it.exerciseName }.forEach { (name, sets) ->
                // 1. Weight Record
                sets.filter { it.weightKg > 0 }.maxByOrNull { it.weightKg }?.let { best ->
                    results += PersonalRecord(name, PrKind.WEIGHT, best.weightKg, best.reps, endedAt[best.sessionId] ?: 0L, best.sessionId)
                }
                // 2. Distance Record
                sets.filter { it.distanceKm > 0 }.maxByOrNull { it.distanceKm }?.let { best ->
                    results += PersonalRecord(name, PrKind.DISTANCE, best.distanceKm, 0, endedAt[best.sessionId] ?: 0L, best.sessionId)
                }
                // 3. Pace Record
                sets.filter { it.distanceKm > 0 && it.durationMin > 0 }.minByOrNull { it.durationMin / it.distanceKm }?.let { best ->
                    results += PersonalRecord(name, PrKind.PACE, best.durationMin / best.distanceKm, 0, endedAt[best.sessionId] ?: 0L, best.sessionId)
                }
                // 4. Reps Record (Bodyweight)
                sets.filter { it.weightKg <= 0 && it.reps > 0 }.maxByOrNull { it.reps }?.let { best ->
                    results += PersonalRecord(name, PrKind.REPS, best.reps.toDouble(), 0, endedAt[best.sessionId] ?: 0L, best.sessionId)
                }
                // 5. Duration Record
                sets.filter { it.durationMin > 0 }.maxByOrNull { it.durationMin }?.let { best ->
                    results += PersonalRecord(name, PrKind.TIME, best.durationMin, 0, endedAt[best.sessionId] ?: 0L, best.sessionId)
                }
                // 6. Speed Record
                sets.filter { it.speedKmh > 0 }.maxByOrNull { it.speedKmh }?.let { best ->
                    results += PersonalRecord(name, PrKind.SPEED, best.speedKmh, 0, endedAt[best.sessionId] ?: 0L, best.sessionId)
                }
                // 7. Incline Record
                sets.filter { it.inclinePercent > 0 }.maxByOrNull { it.inclinePercent }?.let { best ->
                    results += PersonalRecord(name, PrKind.INCLINE, best.inclinePercent, 0, endedAt[best.sessionId] ?: 0L, best.sessionId)
                }
            }
            return results.sortedByDescending { it.date }
        }

    /** XP earned per day (last 7 days) split by exercise category, oldest first. */
    val weekXp: List<Pair<LocalDate, Map<ExerciseCategory, Int>>>
        get() {
            val zone = ZoneId.systemDefault()
            val dayOf = { millis: Long -> Instant.ofEpochMilli(millis).atZone(zone).toLocalDate() }
            val sessionDay = sessions.associate { it.id to dayOf(it.endedAt) }
            val logsByDay = logsBySession.entries
                .mapNotNull { (id, logs) -> sessionDay[id]?.let { day -> day to logs } }
                .groupBy({ it.first }) { it.second }
            val today = LocalDate.now()
            return (6 downTo 0).map { back ->
                val day = today.minusDays(back.toLong())
                val logs = logsByDay[day].orEmpty().flatten()
                day to logs.groupBy { it.category }.mapValues { (_, l) -> l.sumOf { it.xp } }
            }
        }
}

class HistoryViewModel(private val container: AppContainer) : ViewModel() {

    val uiState: StateFlow<LogUiState> = combine(
        container.repository.sessions,
        container.repository.allSetLogs
    ) { sessions, logs ->
        LogUiState(sessions = sessions, logsBySession = logs.groupBy { it.sessionId })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LogUiState())

    val imperial: StateFlow<Boolean> = container.prefs.imperial

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            container.repository.deleteSession(sessionId, container.auth.currentUid())
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HistoryViewModel(appContainer) }
        }
    }
}

private val categoryColors = mapOf(
    ExerciseCategory.STRENGTH to StatStr,
    ExerciseCategory.CARDIO to StatEnd,
    ExerciseCategory.BODYWEIGHT to StatAgi,
    ExerciseCategory.FLEXIBILITY to StatWil
)

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenSession: (Long) -> Unit = {},
    initialTab: Int = 0,
    title: String = "Chronicle",
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    val imperial by viewModel.imperial.collectAsState()
    var tab by remember(initialTab) { mutableIntStateOf(initialTab) }

    var sessionToDelete by remember { mutableStateOf<SessionEntity?>(null) }
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Workout?") },
            text = { Text("This will remove '${session.name}' from your history and revert the XP, Gold, and Energy you earned from it. Your level might decrease.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(session.id)
                        sessionToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("Cancel") }
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = rememberDockContentPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("History") }
                    SegmentedButton(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Progress") }
                }
            }

            if (state.sessions.isEmpty()) {
                item {
                    SectionCard {
                        Text(
                            "The chronicle is empty. Complete a workout to write your first entry.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (tab == 0) {
                historyItems(state, imperial, onOpenSession = onOpenSession, onDelete = { sessionToDelete = it })
            } else {
                progressItems(state, imperial, onOpenSession = onOpenSession)
            }
        }
    }
}

// ---- History tab ----

private fun LazyListScope.historyItems(
    state: LogUiState,
    imperial: Boolean,
    onOpenSession: (Long) -> Unit,
    onDelete: (SessionEntity) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val byDay = state.sessions.groupBy {
        Instant.ofEpochMilli(it.endedAt).atZone(zone).toLocalDate()
    }.entries.sortedByDescending { it.key }

    byDay.forEach { (day, daySessions) ->
        item(key = "day-$day") {
            val fmt = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    fmt.format(Date(daySessions.first().endedAt)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${daySessions.sumOf { it.setCount }} sets \u00B7 +${daySessions.sumOf { it.xpEarned }} XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(daySessions, key = { it.id }) { session ->
            SessionLogCard(
                session = session,
                logs = state.logsBySession[session.id].orEmpty(),
                imperial = imperial,
                onClick = { onOpenSession(session.id) },
                onDelete = { onDelete(session) }
            )
        }
    }
}

@Composable
private fun SessionLogCard(
    session: SessionEntity,
    logs: List<SetLogEntity>,
    imperial: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(value = false) }
    var detailFor by remember { mutableStateOf<String?>(null) }
    detailFor?.let { name ->
        ExerciseDetailDialog(name = name, onDismiss = { detailFor = null })
    }
    SectionCard {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(session.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val fmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                Text(
                    "${fmt.format(Date(session.endedAt))} \u00B7 ${session.setCount} sets \u00B7 ${durationText(session)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "+${session.xpEarned} XP",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    if (expanded) "Hide sets \u25B2" else "Show sets \u25BC",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        if (expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                logs.groupBy { it.exerciseName }.forEach { (exercise, sets) ->
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { detailFor = exercise }
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(
                                        categoryColors[sets.first().category] ?: Color.Gray,
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                exercise,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        sets.forEachIndexed { i, set ->
                            Row(Modifier.padding(start = 14.dp)) {
                                Text(
                                    "Set ${i + 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(48.dp)
                                )
                                Text(
                                    setSummary(set, imperial),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "+${set.xp} XP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---- Progress tab ----

private fun LazyListScope.progressItems(state: LogUiState, imperial: Boolean, onOpenSession: (Long) -> Unit) {
    item(key = "stats") {
        SectionCard(title = "Stats Overview") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                OverviewStat("\uD83C\uDFCB\uFE0F", state.sessions.size.toString(), "Workouts")
                OverviewStat(
                    "\uD83D\uDCE6",
                    volumeText(Units.toDisplay(state.totalVolumeKg, imperial)),
                    "Volume (${Units.label(imperial)})"
                )
                OverviewStat("\uD83C\uDFC6", "${state.records.size}", "Records")
            }
        }
    }

    item(key = "chart") {
        SectionCard(title = "XP This Week") {
            WeeklyXpChart(state.weekXp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categoryColors.forEach { (category, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(color, CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text(category.statLabel, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    item(key = "prs-title") {
        Text(
            "\uD83C\uDFC6 Personal Records",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
    if (state.records.isEmpty()) {
        item(key = "prs-empty") {
            SectionCard {
                Text(
                    "No records yet. Log weighted sets and your heaviest lifts will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        items(state.records, key = { "pr-${it.exerciseName}-${it.kind.name}" }) { pr ->
            var showDetail by remember { mutableStateOf(false) }
            if (showDetail) {
                ExerciseDetailDialog(name = pr.exerciseName, onDismiss = { showDetail = false })
            }
            SectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onOpenSession(pr.sessionId) }
                ) {
                    Text("\uD83C\uDFC6", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pr.exerciseName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        val prLabel = when (pr.kind) {
                            PrKind.WEIGHT -> "Best Weight"
                            PrKind.DISTANCE -> "Longest Distance"
                            PrKind.PACE -> "Best Pace"
                            PrKind.TIME -> "Longest Duration"
                            PrKind.REPS -> "Max Reps"
                            PrKind.SPEED -> "Max Speed"
                            PrKind.INCLINE -> "Max Incline"
                            else -> "New Record"
                        }
                        val prValue = when (pr.kind) {
                            PrKind.WEIGHT -> "${Units.formatWeight(pr.value, imperial)} \u00D7 ${pr.reps}"
                            PrKind.DISTANCE -> Units.formatDistance(pr.value, imperial)
                            PrKind.PACE -> Units.formatPace(pr.value, imperial)
                            PrKind.TIME -> Units.formatTimeMinutes(pr.value)
                            PrKind.REPS -> "${pr.value.toInt()} reps"
                            PrKind.SPEED -> Units.formatSpeed(pr.value, imperial)
                            PrKind.INCLINE -> "${Units.trimmed(pr.value)}%"
                            else -> pr.value.toString()
                        }
                        Text(
                            "$prLabel: $prValue",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    val fmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                    Text(
                        fmt.format(Date(pr.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
    }
}

/** Stacked bar chart of XP per day, colored by exercise category. */
@Composable
private fun WeeklyXpChart(week: List<Pair<LocalDate, Map<ExerciseCategory, Int>>>) {
    val maxTotal = week.maxOf { (_, xp) -> xp.values.sum() }.coerceAtLeast(1)
    Row(
        Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        week.forEach { (day, xpByCategory) ->
            val total = xpByCategory.values.sum()
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (total > 0) {
                    Text(
                        "$total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height((110 * total / maxTotal).coerceAtLeast(6).dp)
                    ) {
                        categoryColors.forEach { (category, color) ->
                            val xp = xpByCategory[category] ?: 0
                            if (xp > 0) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(xp.toFloat())
                                        .background(color, RoundedCornerShape(2.dp))
                                    )
                            }
                        }
                    }
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
                Text(
                    day.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---- Shared helpers ----

private fun durationText(session: SessionEntity): String {
    val minutes = ((session.endedAt - session.startedAt) / 60000L).coerceAtLeast(1)
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
}

private fun setSummary(set: SetLogEntity, imperial: Boolean): String {
    val typePrefix = if (set.setType != SetType.NORMAL) "[${set.setType.shortLabel}] " else ""
    val body = when (set.category) {
        ExerciseCategory.STRENGTH -> "${Units.formatWeight(set.weightKg, imperial)} \u00D7 ${set.reps}"
        ExerciseCategory.CARDIO -> buildString {
            append(Units.formatTimeMinutes(set.durationMin))
            if (set.distanceKm > 0) append(" \u00B7 ${Units.formatDistance(set.distanceKm, imperial)}")
            if (set.speedKmh > 0) append(" \u00B7 ${Units.formatSpeed(set.speedKmh, imperial)}")
            if (set.inclinePercent > 0) append(" \u00B7 ${trimDouble(set.inclinePercent)}%")
            if (set.cardioProgram.isNotBlank()) append(" \u00B7 ${set.cardioProgram}")
        }
        ExerciseCategory.BODYWEIGHT -> "${set.reps} reps"
        ExerciseCategory.FLEXIBILITY -> Units.formatTimeMinutes(set.durationMin)
    }
    return typePrefix + body
}


private fun volumeText(kg: Double): String = when {
    kg >= 1_000_000 -> "%.1fM".format(kg / 1_000_000)
    kg >= 1_000 -> "%.1fk".format(kg / 1_000)
    else -> trimDouble(kg)
}

private fun trimDouble(d: Double): String =
    if (d % 1.0 == 0.0) d.toInt().toString() else "%.1f".format(d)
