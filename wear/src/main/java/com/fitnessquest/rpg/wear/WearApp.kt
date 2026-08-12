package com.fitnessquest.rpg.wear

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Dialog
import com.fitnessquest.shared.wear.WearLiveMetrics
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun WearApp(
    state: WearUiState,
    onSelectExercise: (Int) -> Unit,
    onLogSet: () -> Unit,
    onSkipRest: () -> Unit,
    onExtendRest: () -> Unit,
    onDismissFeedback: () -> Unit,
    onAdjustWeight: (Double) -> Unit,
    onAdjustReps: (Int) -> Unit,
    onAdjustDuration: (Double) -> Unit,
    onAdjustDistance: (Double) -> Unit,
    onRetryLink: () -> Unit,
    onRequestHrPermission: () -> Unit
) {
    FitnessRpgWearTheme {
        val bg = MaterialTheme.colors.background
        Box(
            Modifier
                .fillMaxSize()
                .background(bg)
        ) {
            val session = state.session
            if (!session.active) {
                IdleScreen(
                    phoneConnected = state.phoneConnected,
                    statusText = state.linkStatus,
                    onRetryLink = onRetryLink
                )
            } else {
                val resting = session.restEndsAt != null &&
                    (session.restEndsAt ?: 0) > System.currentTimeMillis()
                if (resting) {
                    RestScreen(
                        endsAt = session.restEndsAt!!,
                        metrics = state.metrics,
                        hrStatus = state.hrStatus,
                        onSkip = onSkipRest,
                        onExtend = onExtendRest,
                        onRequestHrPermission = onRequestHrPermission
                    )
                } else {
                    SessionScreen(
                        state = state,
                        onSelectExercise = onSelectExercise,
                        onLogSet = onLogSet,
                        onAdjustWeight = onAdjustWeight,
                        onAdjustReps = onAdjustReps,
                        onAdjustDuration = onAdjustDuration,
                        onAdjustDistance = onAdjustDistance,
                        onRequestHrPermission = onRequestHrPermission
                    )
                }
            }

            state.feedback?.let { fb ->
                Dialog(showDialog = true, onDismissRequest = onDismissFeedback) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colors.surface)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            fb.message,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.onSurface,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onDismissFeedback) { Text("OK") }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleScreen(
    phoneConnected: Boolean,
    statusText: String,
    onRetryLink: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        TimeText()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "FitnessRPG",
                style = MaterialTheme.typography.title2,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (phoneConnected) "Watch linked"
                else "Waiting for phone",
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.SemiBold,
                color = if (phoneConnected) Color(0xFF6BC96B) else MaterialTheme.colors.primary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(6.dp))
            Text(
                statusText.ifBlank {
                    if (phoneConnected) "Start a quest on your phone"
                    else "Open FitnessRPG on your phone"
                },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onBackground,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onRetryLink, modifier = Modifier.fillMaxWidth(0.78f)) {
                Text(if (phoneConnected) "Refresh" else "Find phone")
            }
        }
    }
}

@Composable
private fun SessionScreen(
    state: WearUiState,
    onSelectExercise: (Int) -> Unit,
    onLogSet: () -> Unit,
    onAdjustWeight: (Double) -> Unit,
    onAdjustReps: (Int) -> Unit,
    onAdjustDuration: (Double) -> Unit,
    onAdjustDistance: (Double) -> Unit,
    onRequestHrPermission: () -> Unit
) {
    val session = state.session
    val unit = if (session.imperial) "lb" else "kg"
    val distUnit = if (session.imperial) "mi" else "km"
    val weightStep = if (session.imperial) 5.0 else 2.5
    val count = session.exercises.size
    val idx = state.selectedIndex.coerceIn(0, (count - 1).coerceAtLeast(0))
    val ex = session.exercises.getOrNull(idx)
    val category = ex?.category?.uppercase().orEmpty()
    val tracking = ex?.trackingType?.uppercase()?.takeIf { it.isNotBlank() } ?: when (category) {
        "CARDIO" -> "DISTANCE_TIME"
        "FLEXIBILITY" -> "TIME_ONLY"
        "BODYWEIGHT" -> "BODYWEIGHT_REPS"
        else -> "WEIGHT_REPS"
    }
    val m = state.metrics

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 20.dp)
    ) {
        item { TimeText() }
        
        // Compact Status & Metrics at the top
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onRequestHrPermission() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Focused Heart Rate
                Column(horizontalAlignment = Alignment.Start) {
                    val bpm = m.bpm
                    val color = wearZoneColor(m.zone)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WearPulsingHeart(bpm, color, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = bpm?.toString() ?: "--",
                            style = MaterialTheme.typography.title3,
                            color = color,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(m.zone ?: "Zone", style = MaterialTheme.typography.caption2, color = color)
                }

                // Focused Calories / Activity
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = m.caloriesKcal?.let { "${it.toInt()} kcal" } ?: "0 kcal",
                        style = MaterialTheme.typography.title3,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Black
                    )
                    Text(formatDuration(m.activeDurationMs ?: 0L), style = MaterialTheme.typography.caption2)
                }
            }
        }

        if (ex != null) {
            // Exercise Title & Navigation
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        enabled = idx > 0,
                        onClick = { onSelectExercise(idx - 1) },
                        modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                    
                    Text(
                        text = "${idx + 1} of $count",
                        style = MaterialTheme.typography.caption2,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Button(
                        enabled = idx < count - 1,
                        onClick = { onSelectExercise(idx + 1) },
                        modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) }
                }
            }

            item {
                Text(
                    text = ex.name,
                    style = MaterialTheme.typography.title2,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(0.92f)
                )
            }

            item {
                Text(
                    text = "Set ${ex.loggedSets + 1} \u2022 Goal ${ex.targetReps}",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.8f)
                )
            }

            // Controls Section
            when (tracking) {
                "CARDIO_MACHINE", "DISTANCE_TIME" -> {
                    item {
                        StepperRow(
                            label = "${fmt(state.durationMin)} min",
                            onMinus = { onAdjustDuration(-0.5) },
                            onPlus = { onAdjustDuration(0.5) }
                        )
                    }
                    item {
                        StepperRow(
                            label = "${fmt(state.distanceDisplay)} $distUnit",
                            onMinus = { onAdjustDistance(-0.1) },
                            onPlus = { onAdjustDistance(0.1) }
                        )
                    }
                }
                "DISTANCE_ONLY" -> {
                    item {
                        StepperRow(
                            label = "${fmt(state.distanceDisplay)} $distUnit",
                            onMinus = { onAdjustDistance(-0.1) },
                            onPlus = { onAdjustDistance(0.1) }
                        )
                    }
                }
                "TIME_ONLY" -> {
                    item {
                        StepperRow(
                            label = "${fmt(state.durationMin)} min",
                            onMinus = { onAdjustDuration(-0.5) },
                            onPlus = { onAdjustDuration(0.5) }
                        )
                    }
                }
                "BODYWEIGHT_REPS", "REPS_ONLY" -> {
                    item {
                        StepperRow(
                            label = "${state.reps} reps",
                            onMinus = { onAdjustReps(-1) },
                            onPlus = { onAdjustReps(1) }
                        )
                    }
                }
                else -> {
                    item {
                        StepperRow(
                            label = "${fmt(state.weightDisplay)} $unit",
                            onMinus = { onAdjustWeight(-weightStep) },
                            onPlus = { onAdjustWeight(weightStep) }
                        )
                    }
                    item {
                        StepperRow(
                            label = "${state.reps} reps",
                            onMinus = { onAdjustReps(-1) },
                            onPlus = { onAdjustReps(1) }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
            }

            item {
                Button(
                    onClick = onLogSet,
                    modifier = Modifier.fillMaxWidth(0.9f).height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Text("LOG SET", fontWeight = FontWeight.Black)
                }
            }

            if (state.lastLogFlash != null) {
                item {
                    Text(
                        state.lastLogFlash,
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item {
                Text(
                    "No exercises in this quest",
                    color = MaterialTheme.colors.onBackground,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HeartRatePill(
    bpm: Int?,
    zone: String?,
    fallback: String,
    onClick: () -> Unit
) {
    val color = wearZoneColor(zone)
    Button(
        onClick = onClick,
        colors = ButtonDefaults.secondaryButtonColors(),
        modifier = Modifier.fillMaxWidth(0.88f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WearPulsingHeart(
                bpm = bpm,
                color = color,
                modifier = Modifier.size(34.dp)
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(
                    bpm?.let { "$it bpm" } ?: fallback,
                    style = MaterialTheme.typography.caption1,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    zone?.let { "$it zone" } ?: "Tap for HR",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WearPulsingHeart(bpm: Int?, color: Color, modifier: Modifier = Modifier) {
    val beatMs = bpm?.let { (60_000 / it.coerceIn(45, 190)).coerceIn(320, 1_300) } ?: 900
    val transition = rememberInfiniteTransition(label = "wearHeartPulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (bpm == null) 1.04f else 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (beatMs / 2).coerceAtLeast(160)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wearHeartPulseScale"
    )
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "\u2665",
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.scale(scale)
        )
    }
}

private fun wearZoneColor(zone: String?): Color = when (zone?.lowercase()) {
    "warmup" -> Color(0xFF7BB4E3)
    "easy" -> Color(0xFF6BC96B)
    "work" -> Color(0xFFF0A830)
    "high" -> Color(0xFFE35B5B)
    else -> Color(0xFF9C7BE3)
}

@Composable
private fun StepperRow(
    label: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colors.surface)
            .border(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.28f), RoundedCornerShape(22.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onMinus, modifier = Modifier.size(36.dp)) { Text("-") }
        Text(
            label,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Button(onClick = onPlus, modifier = Modifier.size(36.dp)) { Text("+") }
    }
}

@Composable
private fun RestScreen(
    endsAt: Long,
    metrics: WearLiveMetrics,
    hrStatus: String,
    onSkip: () -> Unit,
    onExtend: () -> Unit,
    onRequestHrPermission: () -> Unit
) {
    var remaining by remember(endsAt) {
        mutableLongStateOf(((endsAt - System.currentTimeMillis()) / 1000L).coerceAtLeast(0))
    }
    LaunchedEffect(endsAt) {
        while (true) {
            remaining = ((endsAt - System.currentTimeMillis()) / 1000L).coerceAtLeast(0)
            if (remaining <= 0) break
            delay(250.milliseconds)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Rest", style = MaterialTheme.typography.title2, color = MaterialTheme.colors.onBackground, maxLines = 1)
        Text(
            "%d:%02d".format(remaining / 60, remaining % 60),
            style = MaterialTheme.typography.display1,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )
        HeartRatePill(
            bpm = metrics.restHrCurrent ?: metrics.bpm,
            zone = metrics.zone,
            fallback = hrStatus,
            onClick = onRequestHrPermission
        )
        val start = metrics.restHrStart
        val drop = metrics.restHrDrop
        if (start != null && drop != null) {
            Text(
                "Recovery $start → ${metrics.restHrCurrent ?: "—"} (−$drop)",
                style = MaterialTheme.typography.caption1,
                color = if (metrics.restHrGoalMet) Color(0xFF6BC96B) else MaterialTheme.colors.onBackground,
                textAlign = TextAlign.Center
            )
        }
        metrics.restHrGoalBpm?.let { goal ->
            Text(
                if (metrics.restHrGoalMet) "Ready (≤$goal)" else "Goal ♥ ≤ $goal",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.secondary
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onExtend) { Text("+30s") }
            Button(onClick = onSkip) { Text("Skip") }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

private fun fmt(value: Double): String =
    if (value % 1.0 < 0.05) value.toInt().toString() else "%.1f".format(value)
