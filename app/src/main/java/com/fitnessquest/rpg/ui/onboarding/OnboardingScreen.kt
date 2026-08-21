package com.fitnessquest.rpg.ui.onboarding

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitnessquest.rpg.data.health.HeightFormat
import com.fitnessquest.rpg.domain.CardioPlacement
import com.fitnessquest.rpg.domain.Equipment
import com.fitnessquest.rpg.domain.FitnessGoal
import com.fitnessquest.rpg.domain.FitnessLevel
import com.fitnessquest.rpg.domain.MuscleFocus
import com.fitnessquest.rpg.domain.Units
import com.fitnessquest.rpg.domain.WorkoutDurationMins
import com.fitnessquest.rpg.ui.components.FantasyCard
import com.fitnessquest.rpg.ui.screens.ActiveSessionScreen
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightBg
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory),
) {
    val ui by viewModel.ui.collectAsState()

    val canGoBack = ui.step != OnboardingStep.WELCOME
    if (canGoBack) {
        BackHandler { viewModel.goBack() }
    }

    val inDemoSession = (ui.step == OnboardingStep.FIRST_WORKOUT) && ui.inSession && ui.demoExercises.isNotEmpty()

    if (inDemoSession) {
        ActiveSessionScreen(
            workoutId = ui.demoSessionKey,
            demoExercises = ui.demoExercises,
            demoTitle = ui.demoTitle,
            onDone = { viewModel.skipFirstWorkout() },
            onFinished = { viewModel.onFirstWorkoutFinished() }
        )
    } else {
        OnboardingScaffold(
            progress = viewModel.progressFraction,
            showProgress = ui.step != OnboardingStep.WELCOME,
            onBack = if (canGoBack) viewModel::goBack else null,
            scrollable = (ui.step != OnboardingStep.CLASS) && (ui.step != OnboardingStep.USERNAME)
        ) {
            when (ui.step) {
                OnboardingStep.WELCOME -> WelcomeStep(
                    onGetStarted = viewModel::startNewHero,
                    onReturning = viewModel::startReturning
                )
                OnboardingStep.INTRO -> IntroStep(onContinue = viewModel::continueFromIntro)
                OnboardingStep.UNITS -> UnitsStep(viewModel)
                OnboardingStep.HEALTH_CONNECT -> HealthConnectStep(viewModel)
                OnboardingStep.GENDER -> GenderStep(viewModel)
                OnboardingStep.DOB -> DobStep(viewModel)
                OnboardingStep.HEIGHT -> HeightStep(viewModel)
                OnboardingStep.WEIGHT -> WeightStep(viewModel)
                OnboardingStep.GOAL -> ChoiceStep(
                    title = "What is your top goal?",
                    options = FitnessGoal.entries.map { it.label to it.blurb },
                    onPick = { viewModel.setGoal(FitnessGoal.entries[it]) }
                )
                OnboardingStep.EXPERIENCE -> ChoiceStep(
                    title = "How much training experience do you have?",
                    options = FitnessLevel.entries.map {
                        it.label to when (it) {
                            FitnessLevel.BEGINNER -> "0–1 year"
                            FitnessLevel.INTERMEDIATE -> "1–3 years"
                            FitnessLevel.ADVANCED -> "3+ years"
                        }
                    },
                    onPick = { i -> viewModel.setExperience(FitnessLevel.entries[i]) }
                )
                OnboardingStep.FREQUENCY -> ChoiceStep(
                    title = "How often do you want to work out?",
                    options = (1..6).map { d ->
                        "$d per week" to if (d == 3) "Recommended" else ""
                    },
                    onPick = { i -> viewModel.setFrequency(i + 1) }
                )
                OnboardingStep.CARDIO -> ChoiceStep(
                    title = "Do you want to add cardio to your workouts?",
                    options = CardioPlacement.entries.map { it.label to "" },
                    onPick = { i -> viewModel.setCardio(CardioPlacement.entries[i]) }
                )
                OnboardingStep.DURATION -> ChoiceStep(
                    title = "How long would you like your workouts to be?",
                    options = WorkoutDurationMins.entries.map { it.label to "" },
                    onPick = { i -> viewModel.setDuration(WorkoutDurationMins.entries[i]) }
                )
                OnboardingStep.FOCUS -> ChoiceStep(
                    title = "Which muscle group do you want to focus on?",
                    options = MuscleFocus.entries.map { it.label to "" },
                    subtitle = "Your program will be well-rounded, but you can highlight one group.",
                    onPick = { i -> viewModel.setFocus(MuscleFocus.entries[i]) }
                )
                OnboardingStep.GYM -> GymStep(viewModel)
                OnboardingStep.EQUIPMENT -> EquipmentStep(viewModel)
                OnboardingStep.TERMS -> TermsStep(
                    busy = ui.busy,
                    onAccept = viewModel::acceptTerms
                )
                OnboardingStep.PROGRAM -> ProgramStep(
                    summary = ui.programSummary,
                    busy = ui.busy,
                    onStart = viewModel::startFirstWorkout
                )
                OnboardingStep.FIRST_WORKOUT -> {
                    // This state handles the case where they are on the step but not yet in the session UI.
                    ProgramStep(
                        summary = ui.programSummary,
                        busy = ui.busy,
                        onStart = viewModel::startFirstWorkout
                    )
                }
                OnboardingStep.REWARDS -> RewardsStep(onContinue = viewModel::continueAfterRewards)
                OnboardingStep.NOTIFICATIONS -> NotificationsStep(viewModel)
                OnboardingStep.STEPS -> StepsPermissionStep(viewModel)
                OnboardingStep.WEAR -> WearStep(viewModel)
                OnboardingStep.CLASS -> {
                    val premium by viewModel.isPremium.collectAsState()
                    ClassPickerStep(isPremium = premium, onPick = viewModel::chooseClass)
                }
                OnboardingStep.USERNAME -> {
                    UsernameSetupStep(
                        busy = ui.usernameBusy,
                        error = ui.usernameError,
                        onClearError = viewModel::clearUsernameError,
                        onClaim = viewModel::claimUsername
                    )
                }
                OnboardingStep.ACCOUNT_PITCH -> AccountPitchStep(
                    onContinue = viewModel::continueAccountPitch
                )
                OnboardingStep.ACCOUNT, OnboardingStep.RETURNING_SIGN_IN -> AccountStep(
                    viewModel = viewModel,
                    returning = ui.step == OnboardingStep.RETURNING_SIGN_IN
                )
            }
        }
    }
}

@Composable
private fun OnboardingScaffold(
    progress: Float,
    showProgress: Boolean,
    onBack: (() -> Unit)?,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(NightBg)
    ) {
        if (showProgress) {
            OnboardingProgressBar(progress)
        }
        val modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
        
        if (scrollable) {
            Column(
                modifier.verticalScroll(rememberScrollState()),
                content = content
            )
        } else {
            Column(modifier, content = content)
        }

        if (onBack != null) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.padding(12.dp)
            ) { Text("< Back", color = Color.White.copy(alpha = 0.6f)) }
        } else {
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun OnboardingProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "spark")
    val sparkPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val sparkSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Box(modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
        // Track
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        )
        // Bar
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            val w = size.width * progress
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(Color(0xFFE3B54B), Gold)),
                size = size.copy(width = w),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // Spark at the tip
            val tipX = w
            val tipY = size.height / 2
            val sparkSize = 10.dp.toPx()
            
            // Soft glow behind the spark
            drawCircle(
                color = Gold.copy(alpha = 0.28f * sparkPulse),
                radius = sparkSize * 1.8f,
                center = Offset(tipX, tipY)
            )
            rotate(sparkSpin, pivot = Offset(tipX, tipY)) {
                val path = Path()
                path.moveTo(tipX, tipY - sparkSize)
                path.quadraticTo(tipX, tipY, tipX + sparkSize, tipY)
                path.quadraticTo(tipX, tipY, tipX, tipY + sparkSize)
                path.quadraticTo(tipX, tipY, tipX - sparkSize, tipY)
                path.quadraticTo(tipX, tipY, tipX, tipY - sparkSize)
                drawPath(path, color = Gold.copy(alpha = 0.95f))
            }
            // Tiny white core
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = sparkSize * 0.28f,
                center = Offset(tipX, tipY)
            )
        }
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String? = null) {
    Column(Modifier.padding(vertical = 24.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OptionCard(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ChoiceStep(
    title: String,
    options: List<Pair<String, String>>,
    subtitle: String? = null,
    onPick: (Int) -> Unit
) {
    StepHeader(title, subtitle)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { i, (label, blurb) ->
            OptionCard(title = label, subtitle = blurb.takeIf { it.isNotBlank() }, selected = false, onClick = { onPick(i) })
        }
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        border = if (selected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White
        )
    }
}

@Composable
private fun WelcomeStep(onGetStarted: () -> Unit, onReturning: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Fitness Quest RPG", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Train harder. Level up in game and in real life.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        Button(onClick = onGetStarted, modifier = Modifier.fillMaxWidth()) {
            Text("Get started")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onReturning, modifier = Modifier.fillMaxWidth()) {
            Text("I already have an account")
        }
    }
}

@Composable
private fun IntroStep(onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "A few quick questions, then a short demo workout.",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "The demo does not count toward your data — feel free to try logging and swapping exercises. Real workouts live in Training Grounds after setup.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
    }
}

@Composable
private fun UnitsStep(viewModel: OnboardingViewModel) {
    val imperial by viewModel.imperial.collectAsState()
    StepHeader(
        "Units of measurement",
        "We'll use these to track your lifts and travel distance."
    )
    OptionCard(
        title = if (imperial) "Imperial (lb / mi / in)" else "Metric (kg / km / cm)",
        selected = true,
        onClick = {}
    )
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { viewModel.setImperial(false) },
            modifier = Modifier.weight(1f)
        ) { Text("Metric") }
        OutlinedButton(
            onClick = { viewModel.setImperial(true) },
            modifier = Modifier.weight(1f)
        ) { Text("Imperial") }
    }
    Spacer(Modifier.height(24.dp))
    Button(onClick = viewModel::continueUnits, modifier = Modifier.fillMaxWidth()) {
        Text("Continue")
    }
}

@Composable
private fun HealthConnectStep(viewModel: OnboardingViewModel) {
    val state by viewModel.ui.collectAsState()
    val hc = viewModel.healthConnect
    val scope = rememberCoroutineScope()
    var statusMsg by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        scope.launch {
            if (hc.hasCoreReadPermissions()) {
                viewModel.applyHealthConnectAndContinue()
            } else {
                statusMsg = "Permissions not granted — you can enter body stats manually."
            }
        }
    }

    StepHeader(
        "Sync with Health Connect",
        "Auto-fill height and weight, get better calorie context and HR zones, and keep workouts in sync with your other health apps."
    )
    statusMsg?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
    }
    Button(
        onClick = {
            scope.launch {
                if (hc.hasCoreReadPermissions()) {
                    viewModel.applyHealthConnectAndContinue()
                } else {
                    permissionLauncher.launch(hc.permissions.toTypedArray())
                }
            }
        },
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        else Text("Sync with Health Connect")
    }
    Spacer(Modifier.height(10.dp))
    OutlinedButton(onClick = viewModel::skipHealthConnect, modifier = Modifier.fillMaxWidth()) {
        Text("Enter manually")
    }
}

@Composable
private fun GenderStep(viewModel: OnboardingViewModel) {
    val ui by viewModel.ui.collectAsState() // We might need to observe character instead
    StepHeader("Biological sex", "Used for heart rate and calorie estimation.")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("male", "female").forEach { g ->
            OptionCard(
                title = g.replaceFirstChar { it.uppercase() },
                selected = false, // character.gender == g in a real app
                onClick = { viewModel.setGender(g) }
            )
        }
    }
}

@Composable
private fun DobStep(viewModel: OnboardingViewModel) {
    StepHeader("When were you born?", "Used to estimate your maximum heart rate.")
    // Simplified picker for onboarding; user can refine in profile
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var selectedYear by remember { mutableStateOf(1990) }
        Text("Select birth year", style = MaterialTheme.typography.labelMedium)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                selectedYear.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = Gold
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { selectedYear -= 1 }, modifier = Modifier.weight(1f)) { Text("-") }
            OutlinedButton(onClick = { selectedYear += 1 }, modifier = Modifier.weight(1f)) { Text("+") }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val cal = Calendar.getInstance()
                cal.set(selectedYear, 0, 1)
                viewModel.setDob(cal.timeInMillis)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue") }
        OutlinedButton(onClick = viewModel::skipDob, modifier = Modifier.fillMaxWidth()) {
            Text("Skip")
        }
    }
}

@Composable
private fun HeightStep(viewModel: OnboardingViewModel) {
    val imperial by viewModel.imperial.collectAsState()
    StepHeader("What is your height?", "Used for travel calculations and avatar scaling.")
    var valA by remember { mutableStateOf("5") }
    var valB by remember { mutableStateOf("10") }
    var cm by remember { mutableStateOf("178") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (imperial) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeightField(valA, { valA = it }, "ft", Modifier.weight(1f))
                HeightField(valB, { valB = it }, "in", Modifier.weight(1f))
            }
        } else {
            HeightField(cm, { cm = it }, "cm", Modifier.fillMaxWidth())
        }
        Button(
            onClick = {
                val m = if (imperial) {
                    HeightFormat.feetInchesToMeters(valA.toIntOrNull() ?: 5, valB.toDoubleOrNull() ?: 0.0)
                } else {
                    (cm.toDoubleOrNull() ?: 178.0) / 100.0
                }
                viewModel.setHeightMeters(m)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue") }
        OutlinedButton(onClick = viewModel::skipHeight, modifier = Modifier.fillMaxWidth()) {
            Text("Skip")
        }
    }
}

@Composable
private fun HeightField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 3) onChange(it.filter { c -> c.isDigit() }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
private fun WeightStep(viewModel: OnboardingViewModel) {
    val imperial by viewModel.imperial.collectAsState()
    StepHeader("What is your weight?", "Used for bodyweight exercise load and calories.")
    var weight by remember { mutableStateOf(if (imperial) "180" else "82") }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text(Units.label(imperial)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { viewModel.setWeightDisplay(weight.toDoubleOrNull() ?: 80.0) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue") }
        OutlinedButton(onClick = viewModel::skipWeight, modifier = Modifier.fillMaxWidth()) {
            Text("Skip")
        }
    }
}

@Composable
private fun GymStep(viewModel: OnboardingViewModel) {
    StepHeader("Where do you train?", "We'll suggest exercises based on your equipment.")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OptionCard(
            "Commercial Gym", "Full access to machines, cables, and racks.", false,
            onClick = { viewModel.setGymPreset(EquipmentPool.COMMERCIAL) }
        )
        OptionCard(
            "Home Gym", "Barbell, dumbbells, and a bench.", false,
            onClick = { viewModel.setGymPreset(EquipmentPool.HOME_BASIC) }
        )
        OptionCard(
            "Bodyweight Only", "No equipment required. Just you.", false,
            onClick = { viewModel.setGymPreset(emptySet()) }
        )
    }
}

@Composable
private fun EquipmentStep(viewModel: OnboardingViewModel) {
    val profile by viewModel.profile.collectAsState()
    StepHeader("Refine your gear", "Toggle specific items you have access to.")
    val items = Equipment.entries.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { eq ->
                    ChoiceChip(
                        label = eq.label,
                        selected = eq in profile.equipment,
                        onClick = { viewModel.toggleEquipment(eq) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = viewModel::continueEquipment, modifier = Modifier.fillMaxWidth()) {
            Text("Looks good")
        }
    }
}

@Composable
private fun TermsStep(busy: Boolean, onAccept: () -> Unit) {
    StepHeader("The Hero's Oath", "Before we begin, a few necessary scrolls.")
    FantasyCard {
        Text(
            "I understand that Fitness Quest RPG is a tool to assist my training and not a medical provider. I will listen to my body, use proper form, and consult a professional if I am new to exercise or have health concerns.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = onAccept,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        else Text("I accept the oath")
    }
}

@Composable
private fun ProgramStep(summary: List<String>, busy: Boolean, onStart: () -> Unit) {
    StepHeader("Your starting quest", "Based on your goals, the forge has prepared a trial.")
    FantasyCard {
        summary.forEach { line ->
            Text("\u2022 $line", style = MaterialTheme.typography.bodyMedium)
        }
        if (summary.isEmpty()) {
            Text("Preparing your program...", style = MaterialTheme.typography.bodyMedium)
        }
    }
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = onStart,
        enabled = !busy && summary.isNotEmpty(),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        else Text("Start demo workout")
    }
}

@Composable
private fun RewardsStep(onContinue: () -> Unit) {
    StepHeader("Trial complete!", "You've earned your first spoils. Training fuels your legend.")
    FantasyCard {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("\uD83C\uDFC6", fontSize = 48.sp)
            Text("Founding Hero Bonus", fontWeight = FontWeight.Bold)
            Text("+100 XP \u00B7 +250 Gold", color = Gold)
        }
    }
    Spacer(Modifier.height(24.dp))
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
        Text("Claim and continue")
    }
}

@Composable
private fun NotificationsStep(viewModel: OnboardingViewModel) {
    StepHeader("Stay on the path", "Allow notifications to get rest timer alerts and workout reminders.")
    // In a real app, this would use the permission requester. 
    // For the demo, we just continue.
    Button(onClick = viewModel::continueAfterNotifications, modifier = Modifier.fillMaxWidth()) {
        Text("Allow notifications")
    }
    Spacer(Modifier.height(10.dp))
    OutlinedButton(onClick = viewModel::skipNotifications, modifier = Modifier.fillMaxWidth()) {
        Text("Not now")
    }
}

@Composable
private fun StepsPermissionStep(viewModel: OnboardingViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> viewModel.continueAfterSteps() }

    StepHeader("Passive Travel", "Allow activity access to let your real-world steps carry your hero between biomes.")
    Button(
        onClick = {
            val granted = if (Build.VERSION.SDK_INT >= 29) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            if (granted) viewModel.continueAfterSteps()
            else {
                if (Build.VERSION.SDK_INT >= 29) {
                    launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                } else {
                    viewModel.continueAfterSteps()
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Allow steps access") }
    Spacer(Modifier.height(10.dp))
    OutlinedButton(onClick = viewModel::skipSteps, modifier = Modifier.fillMaxWidth()) {
        Text("Not now")
    }
}

@Composable
private fun WearStep(viewModel: OnboardingViewModel) {
    val presence by viewModel.wear.collectAsState()
    StepHeader("Heart rate & Watch", "The best way to play. Get live HR, rest timers, and set logging on your wrist.")
    
    FantasyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (presence.watchLinked) "✅" else "⌚", fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (presence.watchLinked) "Watch linked!" else "No watch found",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Open Fitness Quest RPG on your Wear OS watch to sync.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(Modifier.height(24.dp))
    Button(onClick = viewModel::continueAfterWear, modifier = Modifier.fillMaxWidth()) {
        Text("Continue")
    }
    TextButton(onClick = viewModel::refreshWear, modifier = Modifier.fillMaxWidth()) {
        Text("Refresh connection")
    }
}

@Composable
private fun AccountPitchStep(onContinue: () -> Unit) {
    StepHeader("Secure your legend", "Link an account to sync your progress across devices and never lose your data.")
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
        Text("Continue to account setup")
    }
}

@Composable
private fun AccountStep(viewModel: OnboardingViewModel, returning: Boolean) {
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsState()
    StepHeader(
        if (returning) "Welcome back" else "Create your account",
        "Join thousands of heroes on the same quest."
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = {
                val act = context as? Activity
                if (act != null) viewModel.signInWithGoogle(act)
            },
            enabled = !ui.busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign in with Google")
        }
        
        var showEmail by remember { mutableStateOf(false) }
        if (!showEmail) {
            OutlinedButton(onClick = { showEmail = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Use email address")
            }
        } else {
            var email by remember { mutableStateOf("") }
            var pass by remember { mutableStateOf("") }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (returning) viewModel.signInWithEmail(email, pass)
                    else viewModel.signUpWithEmail(email, pass)
                },
                enabled = !ui.busy && email.isNotBlank() && pass.length >= 6,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (returning) "Sign In" else "Create Account")
            }
        }

        if (ui.error != null) {
            Text(ui.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        
        if (!returning) {
            TextButton(onClick = viewModel::completeOnboarding, modifier = Modifier.fillMaxWidth()) {
                Text("Play as Guest", color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

// ---- Data Models for logic ----

object EquipmentPool {
    val HOME_BASIC = setOf(Equipment.DUMBBELLS, Equipment.BARBELL) // Adjusted to use available domain Equipment
    val COMMERCIAL = Equipment.entries.toSet()
}
