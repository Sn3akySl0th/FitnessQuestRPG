package com.fitnessquest.rpg.wear

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
    onToggleCardioTimer: () -> Unit,
    onResetCardioTimer: () -> Unit,
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
            // Ambient Hero Avatar Backdrop
            state.avatarBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Avatar Backdrop",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.20f),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )
            }

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
                        onToggleCardioTimer = onToggleCardioTimer,
                        onResetCardioTimer = onResetCardioTimer,
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
                            .padding(horizontal = 16.dp, vertical = 20.dp), // W-7 safe padding for round screens
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = fb.message,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.body1
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onDismissFeedback,
                            modifier = Modifier.size(width = 80.dp, height = 36.dp)
                        ) {
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
    onToggleCardioTimer: () -> Unit,
    onResetCardioTimer: () -> Unit,
    onRequestHrPermission: () -> Unit
) {
    val session = state.session
    val idx = state.selectedIndex
    val ex = session.exercises.getOrNull(idx)
    val count = session.exercises.size
    val tracking = ex?.trackingType?.uppercase() ?: "WEIGHT_REPS"
    val unit = if (session.imperial) "lb" else "kg"
    val distUnit = if (session.imperial) "mi" else "km"
    val weightStep = if (session.imperial) 5.0 else 2.5
    val isCardio = tracking in setOf("CARDIO_MACHINE", "DISTANCE_TIME", "TIME_ONLY")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (ex != null) {
            val isComplete = ex.loggedSets >= ex.targetSets && ex.targetSets > 0
            val m = state.metrics
            val color = wearZoneColor(m.zone)

            // 1. Top Section: Exercise Name + Subtitle with Set #, HR, and Calories (Hevy-style)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text(
                    text = ex.name,
                    style = MaterialTheme.typography.title3,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.fillMaxWidth(0.90f)
                )

                Spacer(Modifier.height(2.dp))

                // Subtitle: "Set 1/4 • ❤️ 102 • 🔥 24 kcal" (or "✓ Done (4) • ❤️ 102")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isComplete) {
                        Text(
                            text = "✓ Done (${ex.loggedSets})",
                            style = MaterialTheme.typography.caption2,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Set ${ex.loggedSets + 1}/${ex.targetSets}",
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    // Heart Rate
                    WearPulsingHeart(bpm = m.bpm, color = color, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "${m.bpm ?: "--"}",
                        style = MaterialTheme.typography.caption2,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.width(6.dp))

                    // Streak or Calories
                    if (session.heatStreak > 0) {
                        Text(
                            text = "🔥×${session.heatStreak}",
                            style = MaterialTheme.typography.caption2,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                    }

                    Text(
                        text = "${m.caloriesKcal?.toInt() ?: 0} kcal",
                        style = MaterialTheme.typography.caption2,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 2. Middle Section: Dual Input Pods (Side-by-Side)
            if (isCardio) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colors.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Stopwatch Time Display & Toggle
                    val totalSecs = state.cardioTimerSeconds
                    val timeStr = "%02d:%02d".format(totalSecs / 60, totalSecs % 60)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (totalSecs > 0 || state.cardioTimerActive) timeStr else "${fmt(state.durationMin)} min",
                            style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold,
                            color = if (state.cardioTimerActive) Color(0xFF4CAF50) else MaterialTheme.colors.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = onToggleCardioTimer,
                                modifier = Modifier.size(28.dp),
                                colors = ButtonDefaults.secondaryButtonColors()
                            ) {
                                Icon(
                                    imageVector = if (state.cardioTimerActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (state.cardioTimerActive) "Pause" else "Start",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            if (totalSecs > 0) {
                                Button(
                                    onClick = onResetCardioTimer,
                                    modifier = Modifier.size(28.dp),
                                    colors = ButtonDefaults.secondaryButtonColors()
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Reset",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Distance Stepper
                    if (tracking in setOf("CARDIO_MACHINE", "DISTANCE_TIME")) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${fmt(state.distanceDisplay)} $distUnit",
                                style = MaterialTheme.typography.caption1,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { onAdjustDistance(if (session.imperial) -0.1 else -0.2) },
                                    modifier = Modifier.size(28.dp),
                                    colors = ButtonDefaults.secondaryButtonColors()
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "-", modifier = Modifier.size(14.dp))
                                }
                                Button(
                                    onClick = { onAdjustDistance(if (session.imperial) 0.1 else 0.2) },
                                    modifier = Modifier.size(28.dp),
                                    colors = ButtonDefaults.secondaryButtonColors()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "+", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                // Strength Pods: Weight (Left) & Reps (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(0.96f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Pod: Weight
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colors.surface.copy(alpha = 0.85f))
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${fmt(state.weightDisplay)} $unit",
                            style = MaterialTheme.typography.caption1,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { onAdjustWeight(-weightStep) },
                                modifier = Modifier.size(26.dp),
                                colors = ButtonDefaults.secondaryButtonColors()
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-", modifier = Modifier.size(12.dp))
                            }
                            Button(
                                onClick = { onAdjustWeight(weightStep) },
                                modifier = Modifier.size(26.dp),
                                colors = ButtonDefaults.secondaryButtonColors()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "+", modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    // Right Pod: Reps
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colors.surface.copy(alpha = 0.85f))
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${state.reps} reps",
                            style = MaterialTheme.typography.caption1,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { onAdjustReps(-1) },
                                modifier = Modifier.size(26.dp),
                                colors = ButtonDefaults.secondaryButtonColors()
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-", modifier = Modifier.size(12.dp))
                            }
                            Button(
                                onClick = { onAdjustReps(1) },
                                modifier = Modifier.size(26.dp),
                                colors = ButtonDefaults.secondaryButtonColors()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "+", modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            // 3. Previous Performance / Goal hint
            val prevText = if (isCardio) {
                "Goal: ${fmt(state.durationMin)} min"
            } else if (ex.lastWeightDisplay > 0) {
                "prev. ${fmt(ex.lastWeightDisplay)} $unit × ${ex.targetReps}"
            } else {
                "Goal: ${ex.targetReps} reps"
            }
            Text(
                text = prevText,
                style = MaterialTheme.typography.caption3,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )

            // 4. Bottom Action Bar (Hevy-style: [<] [ LOG ] [>])
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    enabled = idx > 0,
                    onClick = { onSelectExercise(idx - 1) },
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Exercise",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Button(
                    onClick = onLogSet,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(19.dp),
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Log",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "LOG",
                            style = MaterialTheme.typography.button,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Button(
                    enabled = idx < count - 1,
                    onClick = { onSelectExercise(idx + 1) },
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Exercise",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
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
    
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Text("REST", style = MaterialTheme.typography.caption1, fontWeight = FontWeight.Bold)
            Text(
                "%d:%02d".format(remaining / 60, remaining % 60),
                style = MaterialTheme.typography.display1,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colors.primary
            )
            
            Spacer(Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                val color = wearZoneColor(metrics.zone)
                WearPulsingHeart(bpm = metrics.bpm, color = color, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${metrics.bpm ?: "--"} bpm",
                    style = MaterialTheme.typography.title3,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }

            // W-3: Rest text ellipsis and center alignment
            metrics.zone?.let {
                Text(
                    text = "Recovering in $it Zone",
                    style = MaterialTheme.typography.caption2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onExtend,
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier.size(width = 60.dp, height = 34.dp)
                ) { Text("+30s", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                
                Button(
                    onClick = onSkip,
                    modifier = Modifier.size(width = 60.dp, height = 34.dp)
                ) { Text("Skip", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
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
        "♥",
        color = color,
        fontSize = 16.sp,
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

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

private fun fmt(value: Double): String =
    if (value % 1.0 < 0.05) value.toInt().toString() else "%.1f".format(value)
