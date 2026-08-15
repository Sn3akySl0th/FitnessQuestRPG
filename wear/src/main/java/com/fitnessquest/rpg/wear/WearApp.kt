package com.fitnessquest.rpg.wear

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
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
                            text = fb.message,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.body1
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onDismissFeedback) {
                            Text("OK")
                        }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val color = if (phoneConnected) MaterialTheme.colors.primary else Color.Gray
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "FitQuest",
            style = MaterialTheme.typography.title1,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = statusText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption2
        )
        if (!phoneConnected) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetryLink) {
                Text("Retry Link")
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
    val listState = rememberScalingLazyListState()
    val session = state.session
    val idx = state.selectedIndex
    val ex = session.exercises.getOrNull(idx)
    val count = session.exercises.size
    val tracking = ex?.trackingType?.uppercase() ?: "WEIGHT_REPS"
    val unit = if (session.imperial) "lb" else "kg"
    val distUnit = if (session.imperial) "mi" else "km"
    val weightStep = if (session.imperial) 5.0 else 2.5

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: Heart Rate and Timer
        item {
            Row(
                modifier = Modifier.fillMaxWidth(0.92f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val m = state.metrics
                val color = wearZoneColor(m.zone)
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WearPulsingHeart(bpm = m.bpm, color = color, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = m.bpm?.toString() ?: "--",
                            style = MaterialTheme.typography.title3,
                            color = color,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(m.zone ?: "Zone", style = MaterialTheme.typography.caption2, color = color)
                }

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
                        text = "${idx + 1} / $count",
                        style = MaterialTheme.typography.caption2,
                        modifier = Modifier.padding(horizontal = 8.dp)
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
                    modifier = Modifier.fillMaxWidth(0.95f)
                )
            }

            item {
                Text(
                    text = "Set ${ex.loggedSets + 1} \u2022 Goal ${ex.targetReps}",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.8f)
                )
            }

            // High-tactility steppers
            when (tracking) {
                "CARDIO_MACHINE", "DISTANCE_TIME" -> {
                    item {
                        BigStepper(
                            value = "${fmt(state.durationMin)} min",
                            onMinus = { onAdjustDuration(-0.5) },
                            onPlus = { onAdjustDuration(0.5) }
                        )
                    }
                    item {
                        BigStepper(
                            value = "${fmt(state.distanceDisplay)} $distUnit",
                            onMinus = { onAdjustDistance(-0.1) },
                            onPlus = { onAdjustDistance(0.1) }
                        )
                    }
                }
                "DISTANCE_ONLY" -> {
                    item {
                        BigStepper(
                            value = "${fmt(state.distanceDisplay)} $distUnit",
                            onMinus = { onAdjustDistance(-0.1) },
                            onPlus = { onAdjustDistance(0.1) }
                        )
                    }
                }
                "TIME_ONLY" -> {
                    item {
                        BigStepper(
                            value = "${fmt(state.durationMin)} min",
                            onMinus = { onAdjustDuration(-0.5) },
                            onPlus = { onAdjustDuration(0.5) }
                        )
                    }
                }
                "BODYWEIGHT_REPS", "REPS_ONLY" -> {
                    item {
                        BigStepper(
                            value = "${state.reps} reps",
                            onMinus = { onAdjustReps(-1) },
                            onPlus = { onAdjustReps(1) }
                        )
                    }
                }
                else -> {
                    item {
                        BigStepper(
                            value = "${fmt(state.weightDisplay)} $unit",
                            onMinus = { onAdjustWeight(-weightStep) },
                            onPlus = { onAdjustWeight(weightStep) }
                        )
                    }
                    item {
                        BigStepper(
                            value = "${state.reps} reps",
                            onMinus = { onAdjustReps(-1) },
                            onPlus = { onAdjustReps(1) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            item {
                Button(
                    onClick = onLogSet,
                    modifier = Modifier.fillMaxWidth(0.95f).height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Text("LOG SET", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }

            if (state.lastLogFlash != null) {
                item {
                    Text(
                        state.lastLogFlash,
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BigStepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onMinus,
            modifier = Modifier.size(44.dp),
            colors = ButtonDefaults.secondaryButtonColors()
        ) { Icon(Icons.Default.Remove, contentDescription = "Decrease") }
        
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colors.surface)
                .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onPlus,
            modifier = Modifier.size(44.dp),
            colors = ButtonDefaults.secondaryButtonColors()
        ) { Icon(Icons.Default.Add, contentDescription = "Increase") }
    }
}

@Composable
private fun WearPulsingHeart(bpm: Int?, color: Color, modifier: Modifier = Modifier) {
    val beatMs = bpm?.let { (60_000 / it.coerceIn(45, 200)).coerceIn(300, 1_300) } ?: 1000
    val transition = rememberInfiniteTransition(label = "heartPulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (beatMs / 2).coerceAtLeast(150)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    )
    Text(
        "\u2665",
        color = color,
        fontSize = 20.sp,
        fontWeight = FontWeight.Black,
        modifier = modifier.scale(scale),
        textAlign = TextAlign.Center
    )
}

private fun wearZoneColor(zone: String?): Color = when (zone?.lowercase()) {
    "warmup" -> Color(0xFF7BB4E3)
    "easy" -> Color(0xFF6BC96B)
    "work" -> Color(0xFFF0A830)
    "high" -> Color(0xFFE35B5B)
    else -> Color(0xFF9C7BE3)
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
    
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("REST", style = MaterialTheme.typography.caption1, fontWeight = FontWeight.Bold)
            Text(
                "%d:%02d".format(remaining / 60, remaining % 60),
                style = MaterialTheme.typography.display1,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colors.primary
            )
            
            Spacer(Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                val color = wearZoneColor(metrics.zone)
                WearPulsingHeart(bpm = metrics.bpm, color = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${metrics.bpm ?: "--"} bpm",
                    style = MaterialTheme.typography.title3,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onExtend,
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
                ) { Text("+30s", fontSize = 12.sp) }
                
                Button(
                    onClick = onSkip,
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
                ) { Text("Skip", fontSize = 12.sp) }
            }
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
