package com.fitnessquest.rpg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fitnessquest.rpg.FitQuestApp
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.exercises.ExerciseInfo
import com.fitnessquest.rpg.domain.EquipmentCatalog
import com.fitnessquest.rpg.domain.EquipmentFamily
import com.fitnessquest.rpg.domain.EquipmentStation
import com.fitnessquest.rpg.domain.ExerciseCategories
import com.fitnessquest.rpg.domain.ExerciseTrackingType
import kotlinx.coroutines.launch

private enum class PickerMode { Category, Equipment }

/**
 * Dialog for picking an exercise from the catalog (by search, category, or equipment station)
 * or typing a custom one.
 *
 * @param title dialog title when browsing the root
 * @param header optional content above the browse controls (e.g. AI swap button)
 */
@Composable
fun ExercisePickerDialog(
    onDismiss: () -> Unit,
    onPick: (name: String, category: ExerciseCategory) -> Unit,
    title: String = "Add exercise",
    header: (@Composable () -> Unit)? = null
) {
    var mode by remember { mutableStateOf(PickerMode.Equipment) }
    var category by remember { mutableStateOf(ExerciseCategory.STRENGTH) }
    var family by remember { mutableStateOf<EquipmentFamily?>(null) }
    var station by remember { mutableStateOf<EquipmentStation?>(null) }
    var query by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var detailFor by remember { mutableStateOf<String?>(null) }
    var showSuggest by remember { mutableStateOf(false) }

    detailFor?.let { name ->
        ExerciseDetailDialog(name = name, onDismiss = { detailFor = null })
    }
    if (showSuggest) {
        CatalogSuggestionDialog(onDismiss = { showSuggest = false })
    }

    val searching = query.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (station != null && !searching) {
                    IconButton(onClick = { station = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    when {
                        searching -> "Search results"
                        station != null -> station!!.label
                        mode == PickerMode.Equipment -> "Choose equipment"
                        else -> title
                    }
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                header?.invoke()

                TextButton(
                    onClick = { showSuggest = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Suggest machine/exercise")
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("Search exercises") },
                    placeholder = { Text("e.g. incline press, treadmill") },
                    modifier = Modifier.fillMaxWidth()
                )

                when {
                    searching -> {
                        SearchExerciseList(
                            query = query.trim(),
                            onPick = { name, cat ->
                                onPick(name, cat)
                                onDismiss()
                            },
                            onInfo = { detailFor = it }
                        )
                    }
                    station != null -> {
                        StationExerciseList(
                            station = station!!,
                            onPick = { name, cat ->
                                onPick(name, cat)
                                onDismiss()
                            },
                            onInfo = { detailFor = it }
                        )
                    }
                    else -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = mode == PickerMode.Equipment,
                                onClick = { mode = PickerMode.Equipment },
                                label = { Text("Equipment") }
                            )
                            FilterChip(
                                selected = mode == PickerMode.Category,
                                onClick = { mode = PickerMode.Category },
                                label = { Text("Category") }
                            )
                        }
                        when (mode) {
                            PickerMode.Equipment -> {
                                EquipmentBrowser(
                                    selectedFamily = family,
                                    onFamily = { family = it },
                                    onStation = { station = it }
                                )
                            }
                            PickerMode.Category -> {
                                CategoryBrowser(
                                    category = category,
                                    onCategory = { category = it },
                                    onPick = { name, cat ->
                                        onPick(name, cat)
                                        onDismiss()
                                    },
                                    onInfo = { detailFor = it }
                                )
                            }
                        }
                    }
                }

                if (station == null || searching) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        singleLine = true,
                        label = { Text("Or type a custom exercise") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = customName.isNotBlank(),
                onClick = {
                    onPick(customName.trim(), category)
                    onDismiss()
                }
            ) { Text("Add custom") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SearchExerciseList(
    query: String,
    onPick: (String, ExerciseCategory) -> Unit,
    onInfo: (String) -> Unit
) {
    val container = (LocalContext.current.applicationContext as FitQuestApp).container
    val guides by produceState<List<ExerciseInfo>>(initialValue = emptyList(), query) {
        val all = container.exerciseInfo.allGuides()
        val q = query.lowercase()
        val tokens = q.split(' ').filter { it.isNotBlank() }
        value = all
            .asSequence()
            .map { guide ->
                val name = guide.name.lowercase()
                val equipment = guide.equipment.lowercase()
                val muscles = (guide.primaryMuscles + guide.secondaryMuscles).joinToString(" ").lowercase()
                val category = categoryForGuide(guide).label.lowercase()
                val stations = EquipmentCatalog.stationsFor(guide.name, guide.equipment)
                    .joinToString(" ") { "${it.label} ${it.subtitle}" }
                    .lowercase()
                val score = when {
                    name == q -> 300
                    name.startsWith(q) -> 200
                    name.contains(q) -> 100
                    equipment.contains(q) -> 90
                    stations.contains(q) -> 85
                    muscles.contains(q) -> 75
                    category.contains(q) -> 60
                    tokens.isNotEmpty() && tokens.all {
                        name.contains(it) ||
                            equipment.contains(it) ||
                            stations.contains(it) ||
                            muscles.contains(it) ||
                            category.contains(it)
                    } -> 50
                    else -> 0
                }
                guide to score
            }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<ExerciseInfo, Int>> { it.second }.thenBy { it.first.name.lowercase() })
            .map { it.first }
            .take(80)
            .toList()
    }
    Text(
        if (guides.isEmpty()) "No matches for \"$query\"" else "${guides.size} matches",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    LazyColumn(modifier = Modifier.height(280.dp)) {
        items(guides, key = { it.name }) { guide ->
            ExercisePickerRow(
                name = guide.name,
                subtitle = buildString {
                    append(guide.primaryMuscles.joinToString(" · "))
                    if (guide.equipment.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(guide.equipment.replaceFirstChar { it.uppercase() })
                    }
                }.ifBlank { null },
                thumbUrl = guide.imageUrls.firstOrNull(),
                onPick = { onPick(guide.name, categoryForGuide(guide)) },
                onInfo = { onInfo(guide.name) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun EquipmentBrowser(
    selectedFamily: EquipmentFamily?,
    onFamily: (EquipmentFamily?) -> Unit,
    onStation: (EquipmentStation) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        FilterChip(
            selected = selectedFamily == null,
            onClick = { onFamily(null) },
            label = { Text("All") }
        )
        EquipmentFamily.entries.forEach { f ->
            FilterChip(
                selected = selectedFamily == f,
                onClick = { onFamily(f) },
                label = { Text(f.label) }
            )
        }
    }
    val stations = remember(selectedFamily) {
        emptyList<EquipmentStation>()
    }
    val container = (LocalContext.current.applicationContext as FitQuestApp).container
    val dynamicStations by produceState<List<EquipmentStation>>(initialValue = stations, selectedFamily) {
        value = container.exerciseInfo.allStations(selectedFamily)
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height(420.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(dynamicStations, key = { it.id }) { item ->
            EquipmentStationCard(station = item, onClick = { onStation(item) })
        }
    }
}

@Composable
private fun EquipmentStationCard(
    station: EquipmentStation,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageModel = remember(station.id) {
        station.remoteImageUrl ?: runCatching {
            context.assets.open("equipment/${station.id}.png").use { it.readBytes() }
        }.getOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(Color(0xFF0E0F18)),
            contentAlignment = Alignment.Center
        ) {
            if (imageModel != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = station.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                )
            } else {
                Text(
                    equipmentEmoji(station.family),
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            station.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        if (station.subtitle.isNotBlank()) {
            Text(
                station.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}

@Composable
private fun StationExerciseList(
    station: EquipmentStation,
    onPick: (String, ExerciseCategory) -> Unit,
    onInfo: (String) -> Unit
) {
    val container = (LocalContext.current.applicationContext as FitQuestApp).container
    val guides by produceState<List<ExerciseInfo>>(initialValue = emptyList(), station.id) {
        value = container.exerciseInfo.forStation(station)
    }
    Text(
        "${guides.size} exercises · ${station.family.label}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    LazyColumn(modifier = Modifier.height(280.dp)) {
        if (guides.isEmpty()) {
            item {
                Text(
                    "No matching exercises yet. Add a custom name below, or pick another station.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(guides, key = { it.name }) { guide ->
                ExercisePickerRow(
                    name = guide.name,
                    subtitle = buildString {
                        append(guide.primaryMuscles.joinToString(" · "))
                        if (guide.equipment.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(guide.equipment.replaceFirstChar { it.uppercase() })
                        }
                    }.ifBlank { null },
                    thumbUrl = guide.imageUrls.firstOrNull(),
                    onPick = {
                        onPick(guide.name, categoryForGuide(guide))
                    },
                    onInfo = { onInfo(guide.name) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun CategoryBrowser(
    category: ExerciseCategory,
    onCategory: (ExerciseCategory) -> Unit,
    onPick: (String, ExerciseCategory) -> Unit,
    onInfo: (String) -> Unit
) {
    val container = (LocalContext.current.applicationContext as FitQuestApp).container
    val guides by produceState<List<ExerciseInfo>>(initialValue = emptyList(), category) {
        value = container.exerciseInfo.allGuides()
            .filter { categoryForGuide(it) == category }
            .sortedBy { it.name.lowercase() }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ExerciseCategory.entries.forEach { c ->
            FilterChip(
                selected = category == c,
                onClick = { onCategory(c) },
                label = { Text(c.statLabel) }
            )
        }
    }
    Text(
        "${category.label} · ${guides.size} exercises",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
    LazyColumn(modifier = Modifier.height(240.dp)) {
        items(guides, key = { it.name }) { guide ->
            ExercisePickerRow(
                name = guide.name,
                subtitle = buildString {
                    append(guide.primaryMuscles.joinToString(" · "))
                    if (guide.equipment.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(guide.equipment.replaceFirstChar { it.uppercase() })
                    }
                }.ifBlank { null },
                thumbUrl = guide.imageUrls.firstOrNull(),
                onPick = { onPick(guide.name, category) },
                onInfo = { onInfo(guide.name) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun ExercisePickerRow(
    name: String,
    onPick: () -> Unit,
    onInfo: () -> Unit,
    subtitle: String? = null,
    thumbUrl: String? = null
) {
    val container = (LocalContext.current.applicationContext as FitQuestApp).container
    val info by produceState<ExerciseInfo?>(initialValue = null, name, thumbUrl) {
        value = container.exerciseInfo.find(name)
    }
    val character by container.repository.character.collectAsState(initial = null)
    
    val muscles = subtitle ?: info?.primaryMuscles?.joinToString(" · ").orEmpty()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick)
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2C2C2C)),
            contentAlignment = Alignment.Center
        ) {
            val c = character
            if (info != null && c != null) {
                val targets = (info!!.primaryMuscles + info!!.secondaryMuscles).toSet()
                val backMuscles = listOf(
                    "back", "lats", "traps", "lower back", "middle back",
                    "hamstrings", "glutes", "triceps", "calves"
                )
                val isBack = targets.any { backMuscles.contains(it.lowercase()) }

                val lowerBodyMuscles = listOf(
                    "quads", "quadriceps", "hamstrings", "glutes", "calves", "legs",
                    "adductors", "abductors"
                )
                val upperBodyMuscles = listOf(
                    "chest", "pecs", "abs", "abdominals", "core", "obliques",
                    "back", "lats", "traps", "lower back", "middle back",
                    "shoulders", "delts", "biceps", "triceps", "forearms", "neck"
                )
                val targetsLower = targets.any { lowerBodyMuscles.contains(it.lowercase()) }
                val targetsUpper = targets.any { upperBodyMuscles.contains(it.lowercase()) }
                val focus = if (targetsLower && !targetsUpper) AvatarFocus.LOWER_BODY else AvatarFocus.FULL_BODY
                
                CharacterAvatar(
                    clazz = c.characterClass ?: com.fitnessquest.rpg.domain.CharacterClass.WARRIOR,
                    gear = emptyMap(),
                    appearance = c.toAppearance(),
                    highlightMuscles = targets,
                    facingBack = isBack,
                    focus = focus,
                    detail = AvatarDetail.COMPACT,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("\uD83C\uDFCB\uFE0F")
            }
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            if (muscles.isNotBlank()) {
                Text(
                    muscles,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onInfo) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = "Exercise details",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun categoryForGuide(guide: ExerciseInfo): ExerciseCategory =
    ExerciseCategories.infer(guide.name, guide.equipment, guide.dbCategory)

private fun equipmentEmoji(family: EquipmentFamily): String = when (family) {
    EquipmentFamily.FREE_WEIGHTS -> "\uD83C\uDFCB\uFE0F"
    EquipmentFamily.MACHINES -> "\uD83C\uDFD7\uFE0F"
    EquipmentFamily.CABLES -> "\uD83D\uDD17"
    EquipmentFamily.CARDIO -> "\uD83C\uDFC3"
    EquipmentFamily.BODYWEIGHT -> "\uD83E\uDD38"
    EquipmentFamily.ACCESSORIES -> "\uD83E\uDDF5"
}

@Composable
private fun CatalogSuggestionDialog(onDismiss: () -> Unit) {
    val container = (LocalContext.current.applicationContext as FitQuestApp).container
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf("exercise") }
    var name by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("machine") }
    var muscles by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var trackingType by remember { mutableStateOf(ExerciseTrackingType.WEIGHT_REPS) }
    var family by remember { mutableStateOf(EquipmentFamily.MACHINES) }
    var subtitle by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suggest catalog item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = type == "exercise",
                        onClick = { type = "exercise" },
                        label = { Text("Exercise") }
                    )
                    FilterChip(
                        selected = type == "machine",
                        onClick = { type = "machine" },
                        label = { Text("Machine") }
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(if (type == "machine") "Machine name" else "Exercise name") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (type == "exercise") {
                    OutlinedTextField(
                        value = equipment,
                        onValueChange = { equipment = it },
                        singleLine = true,
                        label = { Text("Equipment") },
                        placeholder = { Text("machine, cable, dumbbell") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = muscles,
                        onValueChange = { muscles = it },
                        singleLine = true,
                        label = { Text("Primary muscles") },
                        placeholder = { Text("adductors, glutes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        minLines = 3,
                        label = { Text("Steps or notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        ExerciseTrackingType.entries.forEach { option ->
                            FilterChip(
                                selected = trackingType == option,
                                onClick = { trackingType = option },
                                label = { Text(option.label, maxLines = 1) }
                            )
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        EquipmentFamily.entries.forEach { f ->
                            FilterChip(
                                selected = family == f,
                                onClick = { family = f },
                                label = { Text(f.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        singleLine = true,
                        label = { Text("Short subtitle") },
                        placeholder = { Text("Inner-thigh adductors") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = matches,
                        onValueChange = { matches = it },
                        singleLine = true,
                        label = { Text("Exercise name matches") },
                        placeholder = { Text("hip adduction, thigh adductor") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                message?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && !busy,
                onClick = {
                    busy = true
                    message = null
                    scope.launch {
                        val result = if (type == "exercise") {
                            container.exerciseInfo.submitExerciseSuggestion(
                                name = name,
                                category = "strength",
                                equipment = equipment,
                                primaryMuscles = muscles,
                                instructions = instructions,
                                trackingType = trackingType.name
                            )
                        } else {
                            container.exerciseInfo.submitMachineSuggestion(
                                label = name,
                                family = family,
                                subtitle = subtitle,
                                matches = matches
                            )
                        }
                        busy = false
                        result
                            .onSuccess { message = "Submitted for review. Approved items appear for everyone." }
                            .onFailure { message = it.message ?: "Could not submit suggestion." }
                    }
                }
            ) { Text(if (busy) "Submitting..." else "Submit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
