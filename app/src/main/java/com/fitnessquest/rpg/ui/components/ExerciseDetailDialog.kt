package com.fitnessquest.rpg.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.fitnessquest.rpg.FitQuestApp
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.SessionEntity
import com.fitnessquest.rpg.data.db.SetLogEntity
import com.fitnessquest.rpg.data.exercises.ExerciseInfo
import com.fitnessquest.rpg.domain.*
import com.fitnessquest.rpg.domain.mastery.MasteryProgression
import com.fitnessquest.rpg.domain.mastery.MovementMasteryCatalog
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightBg
import com.fitnessquest.rpg.ui.theme.Parchment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class ChartMetric(val label: String) {
    WEIGHT("Weight"),
    ONE_RM("1RM"),
    VOLUME("Volume"),
    REPS("Reps"),
    DISTANCE("Distance"),
    TIME("Time"),
    PACE("Pace"),
    SPEED("Speed"),
    INCLINE("Incline")
}

enum class TimeFrame(val label: String, val days: Int?) {
    WEEK("Week", 7),
    MONTH("Month", 30),
    THREE_M("3M", 90),
    YEAR("Year", 365),
    ALL("All", null)
}

/**
 * Enhanced details for any exercise: Summary, History, and How-to.
 * Redesigned for "Training Mastery" with immersive art and deep progress insights.
 */
@Composable
fun ExerciseDetailDialog(
    name: String,
    onAddToWorkout: (() -> Unit)? = null,
    onSwap: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as FitQuestApp).container
    val imperial by container.prefs.imperial.collectAsState()
    val character by container.repository.character.collectAsState(initial = null)

    var loaded by remember(name) { mutableStateOf(false) }
    var info by remember(name) { mutableStateOf<ExerciseInfo?>(null) }
    var history by remember(name) {
        mutableStateOf<List<Pair<SessionEntity, List<SetLogEntity>>>>(emptyList())
    }
    
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(name) {
        info = container.exerciseInfo.find(name)
        history = container.repository.exerciseHistory(name)
        loaded = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NightBg
        ) {
            Column(Modifier.fillMaxSize()) {
                // Immersive Sticky Header
                ExerciseDetailHeader(
                    name = name,
                    info = info,
                    onDismiss = onDismiss,
                    onAddToWorkout = onAddToWorkout,
                    onSwap = onSwap
                )

                if (!loaded) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Gold)
                    }
                } else {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Gold,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Gold
                            )
                        },
                        divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) }
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Default.BarChart, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Mastery")
                            }
                        }
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Default.History, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("History")
                            }
                        }
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Default.Info, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("How To")
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (selectedTab) {
                            0 -> SummaryTab(name, info, character, history, imperial)
                            1 -> HistoryTab(history, imperial)
                            2 -> HowToTab(info)
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetailHeader(
    name: String,
    info: ExerciseInfo?,
    onDismiss: () -> Unit,
    onAddToWorkout: (() -> Unit)?,
    onSwap: (() -> Unit)?
) {
    var isFavorite by remember { mutableStateOf(false) }

    Surface(
        color = NightBg.copy(alpha = 0.98f),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = { isFavorite = !isFavorite }) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "Favorite",
                        tint = if (isFavorite) Color.Red else Color.White
                    )
                }
            }
            
            // Quick Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onAddToWorkout != null) {
                    ActionChip("Add to Workout", Icons.Default.Add, onAddToWorkout)
                }
                if (onSwap != null) {
                    ActionChip("Swap Exercise", Icons.Default.SwapHoriz, onSwap)
                }
                
                info?.let {
                    val category = ExerciseCategories.infer(it.name, it.equipment, it.dbCategory)
                    TypeChip(category.label, Gold)
                    if (it.equipment.isNotBlank()) {
                        TypeChip(it.equipment, Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TypeChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            label.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SummaryTab(
    name: String,
    guide: ExerciseInfo?,
    p: CharacterEntity?,
    history: List<Pair<SessionEntity, List<SetLogEntity>>>,
    imperial: Boolean
) {
    val category = guide?.let { ExerciseCategories.infer(it.name, it.equipment, it.dbCategory) } ?: ExerciseCategory.STRENGTH
    val trackingType = ExerciseTracking.resolve(
        name = guide?.name ?: "",
        category = category,
        equipment = guide?.equipment ?: "",
        dbCategory = guide?.dbCategory,
        primaryMuscles = guide?.primaryMuscles ?: emptyList(),
        explicitTrackingType = guide?.trackingType
    )

    val availableMetrics = remember(trackingType) {
        getAvailableMetrics(trackingType)
    }
    var chartMetric by remember(availableMetrics) {
        mutableStateOf(availableMetrics.firstOrNull() ?: ChartMetric.WEIGHT)
    }
    var timeFrame by remember { mutableStateOf(TimeFrame.ALL) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mastery Header & Art
        MasteryHeader(name, guide, history)

        // Progress Section
        if (history.isNotEmpty()) {
            FantasyCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Mastery Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                    
                    // Timeframe Selectors
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TimeFrame.entries.forEach { tf ->
                            TimeFrameChip(tf.label, timeFrame == tf) { timeFrame = tf }
                        }
                    }
                }

                // Metric Selectors
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableMetrics.forEach { metric ->
                        MetricChip(metric.label, chartMetric == metric) { chartMetric = metric }
                    }
                }

                val locale = Locale.current
                val fmt = remember(locale) { SimpleDateFormat("MMM d", java.util.Locale.getDefault()) }
                val now = System.currentTimeMillis()
                val filteredHistory = history.filter { (session, _) ->
                    timeFrame.days?.let { days ->
                        session.endedAt >= now - (days.toLong() * 24 * 60 * 60 * 1000)
                    } ?: true
                }.reversed()

                val chartPoints = filteredHistory.mapIndexed { index, (session, sets) ->
                    val value = when (chartMetric) {
                        ChartMetric.WEIGHT -> Units.toDisplay(sets.maxOfOrNull { it.weightKg } ?: 0.0, imperial).toFloat()
                        ChartMetric.ONE_RM -> Units.toDisplay(sets.maxOfOrNull { GameMath.calculate1RM(it.weightKg, it.reps) } ?: 0.0, imperial).toFloat()
                        ChartMetric.VOLUME -> Units.toDisplay(sets.sumOf { it.weightKg * it.reps }, imperial).toFloat()
                        ChartMetric.REPS -> sets.sumOf { it.reps }.toFloat()
                        ChartMetric.DISTANCE -> Units.kmToDisplay(sets.sumOf { it.distanceKm }, imperial).toFloat()
                        ChartMetric.TIME -> sets.sumOf { it.durationMin }.toFloat()
                        ChartMetric.PACE -> {
                            val totalDist = sets.sumOf { it.distanceKm }
                            val totalTime = sets.sumOf { it.durationMin }
                            if (totalDist > 0) {
                                val minsPerKm = totalTime / totalDist
                                (if (imperial) minsPerKm / 0.62137119 else minsPerKm).toFloat()
                            } else 0f
                        }
                        ChartMetric.SPEED -> sets.maxOfOrNull { it.speedKmh }?.let { Units.speedToDisplay(it, imperial) }?.toFloat() ?: 0f
                        ChartMetric.INCLINE -> sets.maxOfOrNull { it.inclinePercent }?.toFloat() ?: 0f
                    }
                    ChartPoint(index.toFloat(), value, date = fmt.format(Date(session.endedAt)))
                }

                SimpleLineChart(
                    points = chartPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    lineColor = Gold,
                    unitLabel = getMetricUnit(chartMetric, imperial)
                )
                
                // Progress Insights
                ProgressInsights(history, imperial, trackingType)
            }
            
            // High-level Records Card
            MasteryRecordsCard(history, imperial, trackingType)
        } else {
            // Empty state for graph
            SectionCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.BarChart, null, Modifier.size(48.dp), tint = Color.White.copy(alpha = 0.1f))
                    Spacer(Modifier.height(12.dp))
                    Text("No training data yet", color = Color.White.copy(alpha = 0.4f))
                    Text("Complete a quest with this exercise to see your progress.", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color.White.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Avatar Muscles View
        if (p != null && guide != null) {
            MasteryAvatarView(p, guide)
        }
    }
}

@Composable
private fun MasteryHeader(name: String, guide: ExerciseInfo?, history: List<Pair<SessionEntity, List<SetLogEntity>>>) {
    val container = (LocalContext.current.applicationContext as FitQuestApp).container
    val masteryList by container.repository.observeMovementMastery().collectAsState(initial = null)
    val category = guide?.let { ExerciseCategories.infer(it.name, it.equipment, it.dbCategory) } ?: ExerciseCategory.STRENGTH
    val canonical = remember(name, guide) { MovementMasteryCatalog.resolve(name, category) }
    val masteryEntity = masteryList?.find { it.canonicalKey == canonical.name }

    when {
        masteryList == null -> {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Gold, strokeWidth = 2.dp)
            }
        }
        masteryEntity == null -> {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Start training to unlock mastery",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> {
            val totalXp = masteryEntity.currentXp
            val masteryLevel = masteryEntity.level
            val currentLevelBase = MasteryProgression.cumulativeXpForLevel(masteryLevel)
            val nextLevelBase = MasteryProgression.cumulativeXpForLevel(masteryLevel + 1)
            val neededForLevel = (nextLevelBase - currentLevelBase).coerceAtLeast(1L)
            val progress = if (masteryLevel >= MasteryProgression.MAX_LEVEL) 1f
                else ((totalXp - currentLevelBase).toFloat() / neededForLevel.toFloat()).coerceIn(0f, 1f)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Immersive Art
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Gold.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    MasteryArt(category, guide?.equipment.orEmpty())
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        "${canonical.displayName} Mastery Lv $masteryLevel",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Gold
                    )
                    Text(
                        "${totalXp} Total XP earned",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    // Mastery XP Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = Gold,
                            trackColor = Color.White.copy(alpha = 0.1f),
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lvl $masteryLevel", style = MaterialTheme.typography.labelSmall, color = Gold)
                            Text(if (masteryLevel >= MasteryProgression.MAX_LEVEL) "MAX" else "Lvl ${masteryLevel + 1}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MasteryArt(category: ExerciseCategory, equipment: String) {
    Canvas(Modifier.size(60.dp)) {
        val u = size.width / 100f
        val color = Gold
        val secondary = Parchment.copy(alpha = 0.5f)
        
        when (category) {
            ExerciseCategory.STRENGTH -> {
                if (equipment.contains("barbell") || equipment.contains("trap bar")) {
                    drawLine(secondary, Offset(10f * u, 50f * u), Offset(90f * u, 50f * u), strokeWidth = 4f * u, cap = StrokeCap.Round)
                    drawRect(color, Offset(10f * u, 35f * u), Size(15f * u, 30f * u))
                    drawRect(color, Offset(75f * u, 35f * u), Size(15f * u, 30f * u))
                    drawCircle(color, center = Offset(50f * u, 50f * u), radius = 6f * u, style = Stroke(2f * u))
                } else {
                    drawLine(secondary, Offset(20f * u, 50f * u), Offset(80f * u, 50f * u), strokeWidth = 8f * u, cap = StrokeCap.Round)
                    drawCircle(color, center = Offset(20f * u, 50f * u), radius = 15f * u)
                    drawCircle(color, center = Offset(80f * u, 50f * u), radius = 15f * u)
                }
            }
            ExerciseCategory.CARDIO -> {
                val path = Path().apply {
                    moveTo(50f * u, 30f * u)
                    quadraticTo(20f * u, 10f * u, 20f * u, 50f * u)
                    quadraticTo(20f * u, 80f * u, 50f * u, 95f * u)
                    quadraticTo(80f * u, 80f * u, 80f * u, 50f * u)
                    quadraticTo(80f * u, 10f * u, 50f * u, 30f * u)
                }
                drawPath(path, color)
                drawLine(secondary, Offset(50f * u, 95f * u), Offset(10f * u, 20f * u), strokeWidth = 2f * u)
                drawLine(secondary, Offset(50f * u, 95f * u), Offset(90f * u, 20f * u), strokeWidth = 2f * u)
            }
            ExerciseCategory.BODYWEIGHT -> {
                drawCircle(color, center = Offset(50f * u, 30f * u), radius = 12f * u)
                drawLine(color, Offset(50f * u, 42f * u), Offset(50f * u, 75f * u), strokeWidth = 8f * u, cap = StrokeCap.Round)
                drawLine(color, Offset(50f * u, 50f * u), Offset(25f * u, 65f * u), strokeWidth = 6f * u, cap = StrokeCap.Round)
                drawLine(color, Offset(50f * u, 50f * u), Offset(75f * u, 65f * u), strokeWidth = 6f * u, cap = StrokeCap.Round)
                drawLine(color, Offset(50f * u, 75f * u), Offset(35f * u, 95f * u), strokeWidth = 6f * u, cap = StrokeCap.Round)
                drawLine(color, Offset(50f * u, 75f * u), Offset(65f * u, 95f * u), strokeWidth = 6f * u, cap = StrokeCap.Round)
            }
            ExerciseCategory.FLEXIBILITY -> {
                drawCircle(color, center = Offset(50f * u, 50f * u), radius = 25f * u, style = Stroke(2f * u))
                val reed = Path().apply {
                    moveTo(50f * u, 90f * u)
                    cubicTo(40f * u, 70f * u, 60f * u, 40f * u, 50f * u, 10f * u)
                }
                drawPath(reed, color, style = Stroke(width = 4f * u, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun ProgressInsights(
    history: List<Pair<SessionEntity, List<SetLogEntity>>>,
    imperial: Boolean,
    trackingType: ExerciseTrackingType
) {
    if (history.isEmpty()) return
    
    val lastMonthSets = history.filter { (s, _) -> 
        s.endedAt >= System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000 
    }.flatMap { it.second }
    
    val locale = Locale.current
    val fmt = remember(locale) { SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()) }
    val lastTrained = fmt.format(Date(history.first().first.endedAt))
    
    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InsightRow("Last trained", lastTrained)
        InsightRow("Total sessions", history.size.toString())
        
        when (trackingType) {
            ExerciseTrackingType.DISTANCE_TIME, ExerciseTrackingType.DISTANCE_ONLY, ExerciseTrackingType.CARDIO_MACHINE -> {
                val lastMonthDist = lastMonthSets.sumOf { it.distanceKm }
                if (lastMonthDist > 0) {
                    InsightRow("30d Distance", Units.formatDistance(lastMonthDist, imperial))
                }
                
                // Show most used intensity/program if applicable
                if (trackingType == ExerciseTrackingType.CARDIO_MACHINE) {
                    val recentSets = history.flatMap { it.second }.take(20)
                    val topProgram = recentSets.filter { it.cardioProgram.isNotBlank() }
                        .groupBy { it.cardioProgram }
                        .maxByOrNull { it.value.size }?.key
                    val avgSpeed = recentSets.filter { it.speedKmh > 0 }.map { it.speedKmh }.average().takeIf { !it.isNaN() }
                    val avgIncline = recentSets.filter { it.inclinePercent > 0 }.map { it.inclinePercent }.average().takeIf { !it.isNaN() }
                    
                    if (topProgram != null) InsightRow("Fav Program", topProgram)
                    if (avgSpeed != null) InsightRow("Avg Speed", Units.formatSpeed(avgSpeed, imperial))
                    if (avgIncline != null) InsightRow("Avg Incline", "%.1f%%".format(avgIncline))
                }
            }
            else -> {
                val lastMonthVolume = lastMonthSets.sumOf { it.weightKg * it.reps }
                if (lastMonthVolume > 0) {
                    InsightRow("30d Volume", Units.formatWeight(lastMonthVolume, imperial))
                }
            }
        }
    }
}

@Composable
private fun InsightRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MasteryRecordsCard(
    history: List<Pair<SessionEntity, List<SetLogEntity>>>,
    imperial: Boolean,
    trackingType: ExerciseTrackingType
) {
    FantasyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("\uD83C\uDFC5", fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text("Mastery Records", fontWeight = FontWeight.Black, color = Gold, style = MaterialTheme.typography.titleMedium)
        }
        
        when (trackingType) {
            ExerciseTrackingType.DISTANCE_TIME, ExerciseTrackingType.DISTANCE_ONLY, ExerciseTrackingType.CARDIO_MACHINE -> {
                val bestDist = history.map { it.second.sumOf { s -> s.distanceKm } }.maxOrNull() ?: 0.0
                val bestTime = history.map { it.second.sumOf { s -> s.durationMin } }.maxOrNull() ?: 0.0
                val bestPace = history.flatMap { it.second }.filter { s -> s.distanceKm > 0 }.minOfOrNull { it.durationMin / it.distanceKm }
                val bestSpeed = history.flatMap { it.second }.maxOfOrNull { it.speedKmh } ?: 0.0
                val bestIncline = history.flatMap { it.second }.maxOfOrNull { it.inclinePercent } ?: 0.0
                
                RecordRow("Longest Distance", Units.formatDistance(bestDist, imperial))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                RecordRow("Longest Duration", Units.formatTimeMinutes(bestTime))
                if (bestPace != null) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    RecordRow("Best Pace", Units.formatPace(bestPace, imperial))
                }
                if (bestSpeed > 0.0) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    RecordRow("Max Speed", Units.formatSpeed(bestSpeed, imperial))
                }
                if (bestIncline > 0.0) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    RecordRow("Max Incline", "${Units.trimmed(bestIncline)}%")
                }
            }
            ExerciseTrackingType.REPS_ONLY, ExerciseTrackingType.BODYWEIGHT_REPS -> {
                val maxReps = history.flatMap { it.second }.maxOfOrNull { it.reps } ?: 0
                val totalReps = history.sumOf { it.second.sumOf { s -> s.reps } }
                RecordRow("Max Reps (Single Set)", maxReps.toString())
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                RecordRow("Lifetime Reps", totalReps.toString())
            }
            ExerciseTrackingType.TIME_ONLY -> {
                val maxTime = history.flatMap { it.second }.maxOfOrNull { it.durationMin } ?: 0.0
                val totalTime = history.sumOf { it.second.sumOf { s -> s.durationMin } }
                RecordRow("Longest Set", Units.formatTimeMinutes(maxTime))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                RecordRow("Lifetime Time", Units.formatTimeMinutes(totalTime))
            }
            else -> {
                val bestWeight = history.flatMap { it.second }.maxOfOrNull { it.weightKg } ?: 0.0
                val best1RM = history.flatMap { it.second }.maxOfOrNull { GameMath.calculate1RM(it.weightKg, it.reps) } ?: 0.0
                val bestVolume = history.map { it.second.sumOf { s -> s.weightKg * s.reps } }.maxOrNull() ?: 0.0

                RecordRow("Heaviest Weight", Units.formatWeight(bestWeight, imperial))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                RecordRow("Best 1RM (Est.)", Units.formatWeight(best1RM, imperial))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                RecordRow("Max Volume", Units.formatWeight(bestVolume, imperial))
            }
        }
    }
}

@Composable
private fun MasteryAvatarView(p: CharacterEntity, guide: ExerciseInfo) {
    val allTargets = (guide.primaryMuscles + guide.secondaryMuscles).map { it.lowercase().trim() }.toSet()
    
    val frontMuscles = setOf("abs", "abdominals", "core", "chest", "pecs", "pectorals", "quadriceps", "quads", "biceps", "forearms", "shoulders", "delts")
    val backMuscles = setOf("back", "middle back", "mid back", "lats", "lower back", "erectors", "traps", "trapezius", "triceps", "glutes", "hamstrings", "calves", "shoulders", "delts")
    
    val worksFront = allTargets.any { it in frontMuscles || it == "arms" || it == "legs" }
    val worksBack = allTargets.any { it in backMuscles || it == "arms" || it == "legs" }

    val isLowerBody = allTargets.all { it in setOf("quadriceps", "quads", "glutes", "hamstrings", "calves", "legs", "adductors", "abductors") }
    val isUpperBody = allTargets.all { it in setOf("chest", "pecs", "pectorals", "shoulders", "delts", "triceps", "biceps", "forearms", "back", "lats", "traps") }
    
    val focus = when {
        isLowerBody -> AvatarFocus.LOWER_BODY
        isUpperBody -> AvatarFocus.PORTRAIT
        else -> AvatarFocus.FULL_BODY
    }

    FantasyCard {
        Text("Muscles Targeted", style = MaterialTheme.typography.titleSmall, color = Gold, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.02f)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val avatarSize = if (worksFront && worksBack) 140.dp else 240.dp
            
            if (worksFront || !worksBack) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CharacterAvatar(
                        clazz = p.characterClass ?: CharacterClass.WARRIOR,
                        highlightMuscles = allTargets,
                        modifier = Modifier.size(avatarSize),
                        expression = AvatarExpression.BATTLE_READY,
                        facingBack = false,
                        focus = focus
                    )
                    Text("FRONT", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f))
                }
            }
            
            if (worksFront && worksBack) {
                Spacer(Modifier.width(16.dp))
            }
            
            if (worksBack) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CharacterAvatar(
                        clazz = p.characterClass ?: CharacterClass.WARRIOR,
                        highlightMuscles = allTargets,
                        modifier = Modifier.size(avatarSize),
                        expression = AvatarExpression.BATTLE_READY,
                        facingBack = true,
                        focus = focus
                    )
                    Text("BACK", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f))
                }
            }
        }
        
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            guide.primaryMuscles.forEach { muscle -> MuscleTag(muscle, isPrimary = true) }
            guide.secondaryMuscles.forEach { muscle -> MuscleTag(muscle, isPrimary = false) }
        }
    }
}

@Composable
private fun MuscleTag(name: String, isPrimary: Boolean) {
    Surface(
        color = if (isPrimary) Gold.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, if (isPrimary) Gold.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
    ) {
        Text(
            name.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = if (isPrimary) Gold else Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TimeFrameChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) Gold.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.height(24.dp)
    ) {
        Box(Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Gold else Color.White.copy(alpha = 0.4f),
                fontWeight = if (selected) FontWeight.Black else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun MetricChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) Gold else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.height(28.dp)
    ) {
        Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.Black else Color.White.copy(alpha = 0.7f),
                fontWeight = if (selected) FontWeight.Black else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun RecordRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun HistoryTab(
    history: List<Pair<SessionEntity, List<SetLogEntity>>>,
    imperial: Boolean
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (history.isEmpty()) {
            Text(
                "No training history yet. Start your journey!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp)
            )
        } else {
            val locale = Locale.current
            val fmt = remember(locale) { SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()) }
            history.forEach { (session, sets) ->
                val sessionVolume = sets.sumOf { it.weightKg * it.reps }
                val bestSet = sets.maxByOrNull { it.weightKg }
                val est1RM = sets.maxOfOrNull { GameMath.calculate1RM(it.weightKg, it.reps) } ?: 0.0
                
                FantasyCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                fmt.format(Date(session.endedAt)),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                Units.formatWeight(sessionVolume, imperial) + " Vol",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold
                            )
                        }
                        
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Best Set", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                                Text(bestSet?.let { historySetSummary(it, imperial) } ?: "N/A", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Est. 1RM", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                                Text(Units.formatWeight(est1RM, imperial), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        
                        sets.forEachIndexed { i, set ->
                            Text(
                                "Set ${i + 1}: ${historySetSummary(set, imperial)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HowToTab(guide: ExerciseInfo?) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (guide == null || guide.instructions.isEmpty()) {
            Text("No instructions available.", color = Color.White.copy(alpha = 0.5f))
        } else {
            FantasyCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, Modifier.size(20.dp), tint = Gold)
                    Spacer(Modifier.width(8.dp))
                    Text("Mastery Cues", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Gold)
                }
                Text(
                    "Mastering the ${guide.name} improves your ${guide.primaryMuscles.firstOrNull() ?: "core"} strength.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            guide.instructions.forEachIndexed { i, step ->
                val isWarning = step.contains("avoid", ignoreCase = true) || 
                                step.contains("don't", ignoreCase = true) ||
                                step.contains("caution", ignoreCase = true)

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = if (isWarning) Color(0xFFE57373) else Gold,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isWarning) {
                                Icon(Icons.Default.Warning, null, Modifier.size(14.dp), tint = Color.Black)
                            } else {
                                Text(
                                    text = (i + 1).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                    Column {
                        Text(
                            text = if (isWarning) "CRITICAL NOTE" else "STEP ${i + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isWarning) Color(0xFFE57373) else Gold,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

private fun historySetSummary(set: SetLogEntity, imperial: Boolean): String = when (set.category) {
    ExerciseCategory.STRENGTH -> "${Units.formatWeight(set.weightKg, imperial)} x ${set.reps}"
    ExerciseCategory.CARDIO -> buildString {
        if (set.durationMin > 0) append(Units.formatTimeMinutes(set.durationMin))
        if (set.distanceKm > 0) {
            if (isNotEmpty()) append(" \u00B7 ")
            append(Units.formatDistance(set.distanceKm, imperial))
        }
        if (set.speedKmh > 0) {
            if (isNotEmpty()) append(" \u00B7 ")
            append(Units.formatSpeed(set.speedKmh, imperial))
        }
        if (set.inclinePercent > 0) {
            if (isNotEmpty()) append(" \u00B7 ")
            append("${Units.trimmed(set.inclinePercent)}%")
        }
        if (set.cardioProgram.isNotBlank()) {
            if (isNotEmpty()) append(" \u00B7 ")
            append(set.cardioProgram)
        }
    }
    ExerciseCategory.BODYWEIGHT -> if (set.reps > 0) "${set.reps} reps" else Units.formatTimeMinutes(set.durationMin)
    ExerciseCategory.FLEXIBILITY -> Units.formatTimeMinutes(set.durationMin)
    else -> "${set.reps} reps"
}

private fun getAvailableMetrics(type: ExerciseTrackingType): List<ChartMetric> = when (type) {
    ExerciseTrackingType.WEIGHT_REPS, ExerciseTrackingType.ASSISTED_REPS -> 
        listOf(ChartMetric.WEIGHT, ChartMetric.ONE_RM, ChartMetric.VOLUME, ChartMetric.REPS)
    ExerciseTrackingType.REPS_ONLY, ExerciseTrackingType.BODYWEIGHT_REPS -> 
        listOf(ChartMetric.REPS)
    ExerciseTrackingType.TIME_ONLY -> 
        listOf(ChartMetric.TIME)
    ExerciseTrackingType.DISTANCE_TIME -> 
        listOf(ChartMetric.DISTANCE, ChartMetric.TIME, ChartMetric.PACE)
    ExerciseTrackingType.DISTANCE_ONLY -> 
        listOf(ChartMetric.DISTANCE)
    ExerciseTrackingType.CARDIO_MACHINE -> 
        listOf(ChartMetric.DISTANCE, ChartMetric.TIME, ChartMetric.PACE, ChartMetric.SPEED, ChartMetric.INCLINE)
}

private fun getMetricUnit(metric: ChartMetric, imperial: Boolean): String = when (metric) {
    ChartMetric.WEIGHT, ChartMetric.ONE_RM, ChartMetric.VOLUME -> Units.label(imperial)
    ChartMetric.REPS -> "reps"
    ChartMetric.DISTANCE -> Units.distLabel(imperial)
    ChartMetric.TIME -> "m"
    ChartMetric.PACE -> Units.distLabel(imperial).let { if (it == "mi") "/mi" else "/km" } 
    ChartMetric.SPEED -> Units.speedLabel(imperial)
    ChartMetric.INCLINE -> "%"
}

