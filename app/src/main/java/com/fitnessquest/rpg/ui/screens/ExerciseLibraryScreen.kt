package com.fitnessquest.rpg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fitnessquest.rpg.FitQuestApp
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.SetLogEntity
import com.fitnessquest.rpg.data.exercises.ExerciseInfo
import com.fitnessquest.rpg.domain.Equipment
import com.fitnessquest.rpg.domain.EquipmentCatalog
import com.fitnessquest.rpg.domain.EquipmentFamily
import com.fitnessquest.rpg.domain.EquipmentStation
import com.fitnessquest.rpg.domain.ExerciseCategories
import com.fitnessquest.rpg.domain.ExerciseTracking
import com.fitnessquest.rpg.domain.TrainingProfile
import com.fitnessquest.rpg.ui.components.ExerciseDetailDialog
import com.fitnessquest.rpg.ui.components.SceneBanner
import com.fitnessquest.rpg.ui.components.SceneKind
import com.fitnessquest.rpg.ui.components.SectionCard
import com.fitnessquest.rpg.ui.rememberDockContentPadding

private enum class ExerciseLibraryTab(val label: String) {
    EXERCISES("Exercises"),
    MACHINES("Machines"),
    RECENT("Recent")
}

@Composable
fun ExerciseLibraryScreen(onBack: () -> Unit) {
    val container = (LocalContext.current.applicationContext as FitQuestApp).container
    val profile by container.prefs.profile.collectAsState()
    val logs by container.repository.allSetLogs.collectAsState(initial = emptyList())
    var tab by remember { mutableStateOf(ExerciseLibraryTab.EXERCISES) }
    var query by remember { mutableStateOf("") }
    var myGymOnly by remember { mutableStateOf(false) }
    var family by remember { mutableStateOf<EquipmentFamily?>(null) }
    var detailFor by remember { mutableStateOf<String?>(null) }

    detailFor?.let { ExerciseDetailDialog(name = it, onDismiss = { detailFor = null }) }

    Column(Modifier.fillMaxSize()) {
        SceneBanner(
            kind = SceneKind.TRAIN,
            title = "EXERCISE LIBRARY",
            tagline = "Find movements, machines, and history"
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = rememberDockContentPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        label = { Text("Search exercises, muscles, or machines") },
                        placeholder = { Text("bench, adductors, treadmill") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        ExerciseLibraryTab.entries.forEach { item ->
                            FilterChip(
                                selected = tab == item,
                                onClick = { tab = item },
                                label = { Text(item.label, maxLines = 1) }
                            )
                        }
                        FilterChip(
                            selected = myGymOnly,
                            onClick = { myGymOnly = !myGymOnly },
                            label = { Text("My Gym", maxLines = 1) }
                        )
                    }
                    MyGymSummary(profile)
                }
            }

            when (tab) {
                ExerciseLibraryTab.EXERCISES -> {
                    item {
                        ExerciseResultsHeader(
                            query = query,
                            profile = profile,
                            myGymOnly = myGymOnly,
                            onDetail = { detailFor = it }
                        )
                    }
                }
                ExerciseLibraryTab.MACHINES -> {
                    item {
                        MachineFilters(selectedFamily = family, onSelect = { family = it })
                    }
                    item {
                        MachineResults(
                            query = query,
                            selectedFamily = family,
                            profile = profile,
                            myGymOnly = myGymOnly,
                            onExerciseDetail = { detailFor = it }
                        )
                    }
                }
                ExerciseLibraryTab.RECENT -> {
                    item {
                        RecentExercises(
                            logs = logs,
                            query = query,
                            onDetail = { detailFor = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseResultsHeader(
    query: String,
    profile: TrainingProfile,
    myGymOnly: Boolean,
    onDetail: (String) -> Unit
) {
    val container = (LocalContext.current.applicationContext as FitQuestApp).container
    val guides by produceState<List<ExerciseInfo>?>(initialValue = null, query, profile, myGymOnly) {
        val all = container.exerciseInfo.allGuides()
        value = all
            .asSequence()
            .filter { !myGymOnly || exerciseAvailableForProfile(it, profile) }
            .map { it to exerciseSearchScore(it, query) }
            .filter { query.isBlank() || it.second > 0 }
            .sortedWith(
                compareByDescending<Pair<ExerciseInfo, Int>> { it.second }
                    .thenBy { it.first.name.lowercase() }
            )
            .map { it.first }
            .take(160)
            .toList()
    }
    val items = guides
    SectionCard {
        if (items == null) {
            LoadingRow("Loading exercises")
            return@SectionCard
        }
        Text(
            "${items.size} exercise${if (items.size == 1) "" else "s"}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Tap a row for the guide, targets, equipment, and your history.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (items.isEmpty()) {
            EmptyLibraryMessage()
        } else {
            items.forEach { guide ->
                ExerciseLibraryRow(guide = guide, onDetail = { onDetail(guide.name) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun MachineResults(
    query: String,
    selectedFamily: EquipmentFamily?,
    profile: TrainingProfile,
    myGymOnly: Boolean,
    onExerciseDetail: (String) -> Unit
) {
    val container = (LocalContext.current.applicationContext as FitQuestApp).container
    val stations by produceState<List<EquipmentStation>?>(initialValue = null, query, selectedFamily, profile, myGymOnly) {
        value = container.exerciseInfo.allStations(selectedFamily)
            .asSequence()
            .filter { !myGymOnly || stationAvailableForProfile(it, profile) }
            .map { it to stationSearchScore(it, query) }
            .filter { query.isBlank() || it.second > 0 }
            .sortedWith(
                compareByDescending<Pair<EquipmentStation, Int>> { it.second }
                    .thenBy { it.first.family.ordinal }
                    .thenBy { it.first.label.lowercase() }
            )
            .map { it.first }
            .toList()
    }

    val items = stations
    SectionCard {
        if (items == null) {
            LoadingRow("Loading machines")
            return@SectionCard
        }
        Text(
            "${items.size} station${if (items.size == 1) "" else "s"}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (items.isEmpty()) {
            EmptyLibraryMessage()
        } else {
            items.forEach { station ->
                MachineLibraryCard(station = station, onExerciseDetail = onExerciseDetail)
            }
        }
    }
}

@Composable
private fun MachineLibraryCard(
    station: EquipmentStation,
    onExerciseDetail: (String) -> Unit
) {
    val container = (LocalContext.current.applicationContext as FitQuestApp).container
    val context = LocalContext.current
    val exercises by produceState<List<ExerciseInfo>?>(initialValue = null, station.id) {
        value = container.exerciseInfo.forStation(station).take(8)
    }
    val imageModel = remember(station.id, station.remoteImageUrl) {
        station.remoteImageUrl ?: runCatching {
            context.assets.open("equipment/${station.id}.png").use { it.readBytes() }
        }.getOrNull()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 86.dp, height = 72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0E0F18)),
                contentAlignment = Alignment.Center
            ) {
                if (imageModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(imageModel).crossfade(true).build(),
                        contentDescription = station.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                } else {
                    Text(station.family.label.take(1), style = MaterialTheme.typography.headlineMedium)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    station.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOf(station.family.label, station.subtitle)
                        .filter { it.isNotBlank() }
                        .joinToString(" - "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        val list = exercises
        when {
            list == null -> LoadingRow("Finding exercises")
            list.isEmpty() -> Text(
                "No mapped exercises yet. Use Suggest machine/exercise from the workout picker to add one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> {
                Text(
                    "${list.size} available exercise${if (list.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                list.forEach { guide ->
                    ExerciseLibraryRow(guide = guide, compact = true, onDetail = { onExerciseDetail(guide.name) })
                }
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun RecentExercises(
    logs: List<SetLogEntity>,
    query: String,
    onDetail: (String) -> Unit
) {
    val recent = remember(logs, query) {
        logs
            .asReversed()
            .distinctBy { it.exerciseName.lowercase() }
            .filter { query.isBlank() || it.exerciseName.contains(query, ignoreCase = true) }
            .take(60)
    }
    SectionCard {
        Text("Recently logged", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (recent.isEmpty()) {
            Text(
                "Recent exercises will appear here after you complete workouts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            recent.forEach { log ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDetail(log.exerciseName) }
                        .padding(vertical = 8.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(log.exerciseName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${log.category.label} - last set ${recentSetSummary(log)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(Icons.Outlined.Info, contentDescription = "Exercise details", tint = MaterialTheme.colorScheme.primary)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ExerciseLibraryRow(
    guide: ExerciseInfo,
    compact: Boolean = false,
    onDetail: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDetail)
            .padding(vertical = if (compact) 5.dp else 8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                guide.name,
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                exerciseSubtitle(guide),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDetail) {
            Icon(Icons.Outlined.Info, contentDescription = "Exercise details", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun MachineFilters(
    selectedFamily: EquipmentFamily?,
    onSelect: (EquipmentFamily?) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        FilterChip(selected = selectedFamily == null, onClick = { onSelect(null) }, label = { Text("All") })
        EquipmentFamily.entries.forEach { family ->
            FilterChip(
                selected = selectedFamily == family,
                onClick = { onSelect(family) },
                label = { Text(family.label, maxLines = 1) }
            )
        }
    }
}

@Composable
private fun MyGymSummary(profile: TrainingProfile) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        profile.equipment.sortedBy { it.ordinal }.forEach { equipment ->
            AssistChip(onClick = {}, label = { Text(equipment.label, maxLines = 1) })
        }
    }
}

@Composable
private fun LoadingRow(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyLibraryMessage() {
    Text(
        "No matches. Try a broader search or turn off My Gym.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun exerciseSubtitle(guide: ExerciseInfo): String {
    val muscles = (guide.primaryMuscles + guide.secondaryMuscles)
        .distinct()
        .take(4)
        .joinToString(", ")
    val pieces = listOf(
        categoryForGuide(guide).label,
        ExerciseTracking.resolve(
            name = guide.name,
            category = categoryForGuide(guide),
            equipment = guide.equipment,
            dbCategory = guide.dbCategory,
            primaryMuscles = guide.primaryMuscles,
            explicitTrackingType = guide.trackingType
        ).label,
        guide.equipment.replaceFirstChar { it.uppercase() },
        muscles
    ).filter { it.isNotBlank() }
    return pieces.joinToString(" - ")
}

private fun exerciseSearchScore(guide: ExerciseInfo, query: String): Int {
    if (query.isBlank()) return 1
    val q = query.trim().lowercase()
    val tokens = q.split(' ', ',', '/', '-').filter { it.isNotBlank() }
    val name = guide.name.lowercase()
    val equipment = guide.equipment.lowercase()
    val muscles = (guide.primaryMuscles + guide.secondaryMuscles).joinToString(" ").lowercase()
    val category = categoryForGuide(guide).label.lowercase()
    return when {
        name == q -> 500
        name.startsWith(q) -> 350
        name.contains(q) -> 250
        equipment.contains(q) -> 180
        muscles.contains(q) -> 160
        category.contains(q) -> 100
        tokens.isNotEmpty() && tokens.all { token ->
            name.contains(token) || equipment.contains(token) || muscles.contains(token) || category.contains(token)
        } -> 80
        else -> 0
    }
}

private fun stationSearchScore(station: EquipmentStation, query: String): Int {
    if (query.isBlank()) return 1
    val q = query.trim().lowercase()
    val haystack = buildString {
        append(station.label.lowercase()).append(' ')
        append(station.subtitle.lowercase()).append(' ')
        append(station.family.label.lowercase()).append(' ')
        append(station.dbTags.joinToString(" ")).append(' ')
        append(station.nameContains.joinToString(" "))
    }
    return when {
        station.label.lowercase() == q -> 500
        station.label.lowercase().startsWith(q) -> 350
        haystack.contains(q) -> 200
        q.split(' ').filter { it.isNotBlank() }.all { haystack.contains(it) } -> 80
        else -> 0
    }
}

private fun exerciseAvailableForProfile(guide: ExerciseInfo, profile: TrainingProfile): Boolean {
    val stations = EquipmentCatalog.stationsFor(guide.name, guide.equipment)
    if (stations.any { stationAvailableForProfile(it, profile) }) return true
    val equipment = guide.equipment.lowercase()
    return profile.equipment.any { owned ->
        when (owned) {
            Equipment.BODYWEIGHT -> equipment.contains("body") || equipment == "none"
            Equipment.DUMBBELLS -> equipment.contains("dumbbell")
            Equipment.BARBELL -> equipment.contains("barbell") || equipment.contains("e-z")
            Equipment.KETTLEBELL -> equipment.contains("kettlebell")
            Equipment.MACHINES -> equipment.contains("machine")
            Equipment.BANDS -> equipment.contains("band")
            Equipment.CARDIO_GEAR -> guide.name.contains("running", ignoreCase = true) ||
                guide.name.contains("walking", ignoreCase = true) ||
                guide.name.contains("cycling", ignoreCase = true) ||
                guide.name.contains("rowing", ignoreCase = true) ||
                equipment.contains("machine")
        }
    }
}

private fun stationAvailableForProfile(station: EquipmentStation, profile: TrainingProfile): Boolean =
    profile.equipment.any { owned ->
        when (owned) {
            Equipment.BODYWEIGHT -> station.family == EquipmentFamily.BODYWEIGHT
            Equipment.DUMBBELLS -> station.id.contains("dumbbell")
            Equipment.BARBELL -> station.id.contains("barbell") || station.id.contains("ez_bar") || station.id.contains("bench")
            Equipment.KETTLEBELL -> station.id.contains("kettlebell")
            Equipment.MACHINES -> station.family == EquipmentFamily.MACHINES || station.family == EquipmentFamily.CABLES
            Equipment.BANDS -> station.id.contains("band")
            Equipment.CARDIO_GEAR -> station.family == EquipmentFamily.CARDIO
        }
    }

private fun categoryForGuide(guide: ExerciseInfo): ExerciseCategory =
    ExerciseCategories.infer(guide.name, guide.equipment, guide.dbCategory)

private fun recentSetSummary(log: SetLogEntity): String = when {
    log.weightKg > 0 && log.reps > 0 -> "${trim(log.weightKg)} kg x ${log.reps}"
    log.distanceKm > 0.0 -> "${trim(log.distanceKm)} km"
    log.durationMin > 0.0 -> "${trim(log.durationMin)} min"
    log.reps > 0 -> "${log.reps} reps"
    else -> "${log.xp} XP"
}

private fun trim(value: Double): String =
    if (value % 1.0 < 0.05 || value % 1.0 > 0.95) value.toInt().toString() else "%.1f".format(value)
