package com.fitnessquest.rpg.ui.screens

import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.data.ai.CoachAdvice
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.SetLogEntity
import com.fitnessquest.rpg.data.health.HeightFormat
import com.fitnessquest.rpg.data.wear.WearSessionBridge
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.EffortMethod
import com.fitnessquest.rpg.domain.ExerciseCategories
import com.fitnessquest.rpg.domain.ExerciseTracking
import com.fitnessquest.rpg.domain.ExerciseTrackingType
import com.fitnessquest.rpg.domain.GameMath
import com.fitnessquest.rpg.domain.MomentTrigger
import com.fitnessquest.rpg.domain.MonsterCatalog
import com.fitnessquest.rpg.domain.PrKind
import com.fitnessquest.rpg.domain.RewardBatch
import com.fitnessquest.rpg.domain.SessionPr
import com.fitnessquest.rpg.domain.SessionResult
import com.fitnessquest.rpg.domain.StatGains
import com.fitnessquest.rpg.domain.Units
import com.fitnessquest.rpg.notifications.WorkoutNotificationController
import com.fitnessquest.rpg.ui.appContainer
import com.fitnessquest.rpg.ui.components.AvatarExpression
import com.fitnessquest.rpg.ui.components.CharacterAvatar
import com.fitnessquest.rpg.ui.components.ConfettiOverlay
import com.fitnessquest.rpg.ui.components.RewardRevealDialog
import com.fitnessquest.rpg.ui.components.ExerciseDetailDialog
import com.fitnessquest.rpg.ui.components.ExercisePickerDialog
import com.fitnessquest.rpg.ui.components.FloatingBurst
import com.fitnessquest.rpg.ui.components.FloatingTextBurst
import com.fitnessquest.rpg.ui.components.LevelUpModal
import com.fitnessquest.rpg.ui.components.PlateCalculatorDialog
import com.fitnessquest.rpg.ui.components.SectionCard
import com.fitnessquest.rpg.ui.components.countUp
import com.fitnessquest.rpg.ui.effects.AudioEffects
import com.fitnessquest.rpg.ui.effects.HapticEffects
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightBg
import com.fitnessquest.shared.wear.WearExerciseState
import com.fitnessquest.shared.wear.WearFeedbackKind
import com.fitnessquest.shared.wear.WearRestAction
import com.fitnessquest.shared.wear.WearSessionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import com.fitnessquest.rpg.data.media.MediaState
import com.fitnessquest.rpg.domain.SetType
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

data class SessionExercise(
    val name: String,
    val category: ExerciseCategory,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetWeightKg: Double? = null,
    val trackingType: ExerciseTrackingType = ExerciseTracking.resolve(name, category),

    val loggedSets: List<SetLogEntity> = emptyList(),
)

data class SessionFinish(
    val result: SessionResult,
    val praise: String? = null,
    val praisePending: Boolean = false,
    val caloriesKcal: Int? = null,
    val activeDurationMs: Long? = null,
    val zoneWorkSec: Int = 0,
    val zoneHighSec: Int = 0,
    val steps: Long? = null,
    val distanceMeters: Double? = null,
    val summaryItems: List<WorkoutSummaryItem> = emptyList(),
    /** Practice session — rewards are preview-only and were not applied. */
    val isDemo: Boolean = false
)

data class WorkoutSummaryItem(
    val name: String,
    val isIncreasedWeight: Boolean,
    val isIncreasedVolume: Boolean,
    val isIncreased1RM: Boolean,
    val prs: Set<PrKind>,
    val iconUrl: String? = null
)

data class MomentSpoil(
    val labels: List<String>,
    val story: String? = null,
    val storyPending: Boolean = false,
    val isPr: Boolean = false
)

data class AmbushOffer(
    val monsterId: Int,
    val monsterName: String,
    val monsterEmoji: String,
    val goldWager: Int,
    val energyWager: Int
)

data class ActiveSessionUiState(
    val title: String = "Freestyle Session",
    val exercises: List<SessionExercise> = emptyList(),
    val loading: Boolean = true,
    val finish: SessionFinish? = null,
    /** Epoch millis when the current rest period ends; null when not resting. */
    val restEndsAt: Long? = null,
    /** Remaining rest ms when paused for an ambush. */
    val restPausedRemainingMs: Long? = null,
    val restDurationSec: Int = 90,
    val hasAi: Boolean = false,
    val coach: CoachAdvice? = null,
    val coachLoading: Boolean = false,
    val coachError: String? = null,
    /** Index of the exercise currently being swapped by the AI, if any. */
    val aiSwapIndex: Int? = null,
    /** Transient confirmation after an AI swap ("Swapped X for Y: reason"). */
    val swapNote: String? = null,
    /** Consecutive sets logged within the rest window; fuels the heat XP multiplier. */
    val heatStreak: Int = 0,
    /** Epoch millis of the most recent logged set (0 = none yet). */
    val lastLogAt: Long = 0,
    val momentSpoil: MomentSpoil? = null,
    val momentSpoilsUsed: Int = 0,
    val ambushOffer: AmbushOffer? = null,
    val ambushOfferedThisSession: Boolean = false,
    /** XP multiplier from winning an ambush (1.0 = none). */
    val ambushXpMult: Float = 1f,
    val watchLinked: Boolean = false,
    val heartRateBpm: Int? = null,
    val hrZoneLabel: String? = null,
    val maxHeartRate: Int = 190,
    val wearCaloriesKcal: Double? = null,
    val wearSteps: Long? = null,
    val wearStepsPerMin: Long? = null,
    val wearDistanceMeters: Double? = null,
    val wearSpeedMps: Double? = null,
    val wearPaceSecPerKm: Double? = null,
    val wearElevationMeters: Double? = null,
    val wearFloors: Double? = null,
    val wearActiveDurationMs: Long? = null,
    val wearSessionDurationMs: Long? = null,
    val wearZoneWarmupSec: Int = 0,
    val wearZoneEasySec: Int = 0,
    val wearZoneWorkSec: Int = 0,
    val wearZoneHighSec: Int = 0,
    val wearRestHrStart: Int? = null,
    val wearRestHrDrop: Int? = null,
    val wearRestHrGoalMet: Boolean = false,
    val wearHasGps: Boolean = false,
    val wearGoalLabel: String? = null,
    val wearBanner: String? = null,
    val currentExerciseIndex: Int = 0,
    /** Onboarding practice session — nothing persists. */
    val isDemo: Boolean = false,
    val media: MediaState = MediaState()
) {
    val totalSets: Int get() = exercises.sumOf { it.loggedSets.size }
    val totalXp: Int get() = exercises.sumOf { ex -> ex.loggedSets.sumOf { it.xp } }
    val totalVolumeKg: Double get() = exercises.sumOf { ex -> ex.loggedSets.sumOf { it.weightKg * it.reps } }
}

class ActiveSessionViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveSessionUiState(hasAi = container.gemini.hasKey))
    val uiState: StateFlow<ActiveSessionUiState> = _uiState

    val imperial: StateFlow<Boolean> = container.prefs.imperial
    val effortMethod: StateFlow<EffortMethod> = container.prefs.effortMethod
    val showCardioIntensity: StateFlow<Boolean> = container.prefs.showCardioIntensity

    fun setShowCardioIntensity(value: Boolean) = container.prefs.setShowCardioIntensity(value)

    private val wearBridge = WearSessionBridge(container.app)
    private val workoutNotification = WorkoutNotificationController(container.app)

    /** Exercises the coach has already reviewed, so completion only triggers one check each. */
    private val coachedExercises = mutableSetOf<String>()

    private val startedAt = System.currentTimeMillis()
    private var loadedFor: Long? = null
    private var demoMode: Boolean = false

    init {
        wearBridge.bind()
        wearBridge.link.onEach { link ->
            val m = link.metrics
            _uiState.update {
                it.copy(
                    watchLinked = link.watchConnected,
                    heartRateBpm = link.bpm ?: m.bpm,
                    hrZoneLabel = link.zone?.label ?: m.zone,
                    maxHeartRate = container.prefs.maxHr(),
                    wearCaloriesKcal = m.caloriesKcal,
                    wearSteps = m.steps,
                    wearStepsPerMin = m.stepsPerMin,
                    wearDistanceMeters = m.distanceMeters,
                    wearSpeedMps = m.speedMps,
                    wearPaceSecPerKm = m.paceSecPerKm,
                    wearElevationMeters = m.elevationMeters,
                    wearFloors = m.floors,
                    wearActiveDurationMs = m.activeDurationMs,
                    wearSessionDurationMs = m.sessionDurationMs,
                    wearZoneWarmupSec = m.zoneWarmupSec,
                    wearZoneEasySec = m.zoneEasySec,
                    wearZoneWorkSec = m.zoneWorkSec,
                    wearZoneHighSec = m.zoneHighSec,
                    wearRestHrStart = m.restHrStart,
                    wearRestHrDrop = m.restHrDrop,
                    wearRestHrGoalMet = m.restHrGoalMet,
                    wearHasGps = (m.latitude != null) && (m.longitude != null),
                    wearGoalLabel = m.goalLabel,
                    wearBanner = link.banner
                )
            }
            updateWorkoutNotification()
            // Keep watch metrics fresh without rewriting the whole workout payload every tick.
        }.launchIn(viewModelScope)
        wearBridge.logSetCommands.onEach { cmd ->
            val imperial = container.prefs.imperial.value
            val weightKg = Units.toKg(cmd.weightDisplay, imperial)
            val distanceKm = Units.toKm(cmd.distanceDisplay, imperial)
            logSet(
                index = cmd.exerciseIndex,
                weightKg = weightKg,
                reps = cmd.reps,
                durationMin = cmd.durationMin,
                distanceKm = distanceKm,
                rir = cmd.rir,
                avgHr = cmd.avgHr ?: wearBridge.sessionHrStats().first,
                maxHr = cmd.maxHr ?: wearBridge.sessionHrStats().second,
                fromWatch = true
            )
        }.launchIn(viewModelScope)
                wearBridge.restCommands.onEach { cmd ->
            when (cmd.action) {
                WearRestAction.SKIP -> skipRest()
                WearRestAction.EXTEND -> extendRest(cmd.seconds)
                WearRestAction.SET_DURATION -> setRestDuration(cmd.seconds)
            }
        }.launchIn(viewModelScope)

        container.music.state.onEach { media ->
            _uiState.update { it.copy(media = media) }
        }.launchIn(viewModelScope)
        
        container.music.start()
    }

    fun toggleMusic() = container.music.togglePlayPause()
    fun skipMusic() = container.music.skipNext()
    fun prevMusic() = container.music.skipPrevious()

    fun load(workoutId: Long) {
        if (loadedFor == workoutId) return
        loadedFor = workoutId
        demoMode = false
        viewModelScope.launch {
            if (workoutId < 0) {
                _uiState.update { it.copy(loading = false, title = "Freestyle Session", isDemo = false) }
            } else {
                val workout = container.repository.getWorkout(workoutId)
                val exercises = container.repository.exercisesFor(workoutId)
                val resolved = exercises.map { e ->
                    val tracking = resolveExerciseTracking(e.exerciseName, e.category)
                    e to tracking
                }
                _uiState.update {
                    it.copy(
                        loading = false,
                        title = workout?.name ?: "Session",
                        isDemo = false,
                        exercises = resolved.map { (e, tracking) ->
                            SessionExercise(
                                name = e.exerciseName,
                                category = tracking.first,
                                targetSets = e.targetSets,
                                targetReps = e.targetReps,
                                targetWeightKg = e.targetWeightKg,
                                trackingType = tracking.second
                            )

                        }
                    )
                }
            }
            publishWearState()
        }
    }

    /**
     * In-memory practice session for Play onboarding. Does not load or write workouts.
     * [demoKey] should be unique per open so abandon → restart reloads cleanly.
     */
    fun loadDemo(demoKey: Long, title: String, exercises: List<SessionExercise>) {
        if (loadedFor == demoKey && demoMode) return
        loadedFor = demoKey
        demoMode = true
        _uiState.update {
            it.copy(
                loading = false,
                title = title,
                exercises = exercises,
                finish = null,
                ambushOffer = null,
                ambushOfferedThisSession = true, // suppress ambush wagers in demo
                momentSpoil = null,
                momentSpoilsUsed = 99,
                isDemo = true
            )
        }
        publishWearState()
    }

    fun addExercise(name: String, category: ExerciseCategory) {
        viewModelScope.launch {
            val resolved = resolveExerciseTracking(name, category)
            _uiState.update { s ->
                s.copy(exercises = s.exercises + SessionExercise(name, resolved.first, trackingType = resolved.second))
            }
            publishWearState()
        }
    }

    private suspend fun resolveExerciseTracking(
        name: String,
        stored: ExerciseCategory
    ): Pair<ExerciseCategory, ExerciseTrackingType> {
        val guide = container.exerciseInfo.find(name)
        return if (guide != null) {
            val category = ExerciseCategories.infer(guide.name, guide.equipment, guide.dbCategory)
            category to ExerciseTracking.resolve(
                name = guide.name,
                category = category,
                equipment = guide.equipment,
                dbCategory = guide.dbCategory,
                primaryMuscles = guide.primaryMuscles,
                explicitTrackingType = guide.trackingType
            )
        } else {
            val category = ExerciseCategories.resolveStored(name, stored)
            category to ExerciseTracking.resolve(name, category)
        }
    }

    fun removeExercise(index: Int) {
        _uiState.update { s ->
            if (index !in s.exercises.indices || s.exercises.size <= 1) return@update s
            val updated = s.exercises.filterIndexed { i, _ -> i != index }
            val newIndex = s.currentExerciseIndex.coerceAtMost(updated.size - 1)
            s.copy(
                exercises = updated,
                currentExerciseIndex = newIndex
            )
        }
        publishWearState()
    }

    fun logSet(
        index: Int,
        weightKg: Double,
        reps: Int,
        durationMin: Double,
        distanceKm: Double,
        rir: Int? = null,
        avgHr: Int? = null,
        maxHr: Int? = null,
        speedKmh: Double = 0.0,
        inclinePercent: Double = 0.0,
        cardioProgram: String = "",
        setType: SetType = SetType.NORMAL,
        fromWatch: Boolean = false
    ) {

        val hrAvg = avgHr ?: wearBridge.sessionHrStats().first
        val hrMax = maxHr ?: wearBridge.sessionHrStats().second
        val prior = _uiState.value
        val safeIndex = index.coerceIn(0, (prior.exercises.size - 1).coerceAtLeast(0))
        val priorEx = prior.exercises.getOrNull(safeIndex)
        val effectiveWeight = when (priorEx?.category) {
            ExerciseCategory.BODYWEIGHT -> if (weightKg <= 0) container.prefs.bodyWeightKg() ?: 0.0 else weightKg
            else -> weightKg
        }
        _uiState.update { s ->
            if (s.exercises.isEmpty()) return@update s
            val ex = s.exercises[safeIndex]
            val log = SetLogEntity(
                sessionId = 0,
                exerciseName = ex.name,
                category = ex.category,
                weightKg = if (ex.category == ExerciseCategory.BODYWEIGHT && weightKg <= 0) {
                    container.prefs.bodyWeightKg() ?: 0.0
                } else {
                    weightKg
                },
                reps = reps,
                durationMin = durationMin,
                distanceKm = distanceKm,
                rir = rir,
                avgHr = hrAvg,
                maxHr = hrMax,
                speedKmh = speedKmh,
                inclinePercent = inclinePercent,
                cardioProgram = cardioProgram.trim(),
                setType = setType
            )

            // Workout heat: staying on pace (rest window + time for the set itself)
            // builds a streak that multiplies set XP.
            val now = System.currentTimeMillis()
            val windowMs = (s.restDurationSec + GameMath.HEAT_GRACE_SEC) * 1000L
            val heatStreak = if (s.lastLogAt > 0 && now - s.lastLogAt <= windowMs) s.heatStreak + 1 else 1
            val multiplier = GameMath.calculateHeatMultiplier(heatStreak) * s.ambushXpMult
            val withXp = log.copy(xp = (GameMath.xpForSet(log) * multiplier).toInt())
            s.copy(
                exercises = s.exercises.toMutableList().also {
                    it[safeIndex] = ex.copy(loggedSets = ex.loggedSets + withXp)
                },
                // Logging a set kicks off the rest timer automatically (if restDurationSec > 0).
                restEndsAt = if (s.restDurationSec > 0) now + s.restDurationSec * 1000L else null,
                heatStreak = heatStreak,
                lastLogAt = now,
                currentExerciseIndex = safeIndex
            )
        }
        maybeAutoCoach(index)
        maybeMomentLoot(index, effectiveWeight, reps, rir)
        maybeAmbushOffer()
        if (fromWatch) wearBridge.noteSetLogged()
        publishWearState()
    }

    fun dismissWearBanner() {
        wearBridge.clearBanner()
        _uiState.update { it.copy(wearBanner = null) }
    }

    fun bodyWeightKgOrNull(): Double? = container.prefs.bodyWeightKg()


    private fun publishWearState() {
        val s = _uiState.value
        val imperial = container.prefs.imperial.value
        val exercises = s.exercises.map { ex ->
            val last = ex.loggedSets.lastOrNull()
            WearExerciseState(
                name = ex.name,
                category = ex.category.name,
                targetSets = ex.targetSets,
                targetReps = ex.targetReps,
                loggedSets = ex.loggedSets.size,
                trackingType = ex.trackingType.name,
                lastWeightDisplay = last?.let { Units.toDisplay(it.weightKg, imperial) } ?: 0.0,
                lastReps = last?.reps ?: 0
            )
        }
        wearBridge.publishSession(
            WearSessionState(
                active = s.finish == null && !s.loading,
                title = s.title,
                imperial = imperial,
                heatStreak = s.heatStreak,
                totalSets = s.totalSets,
                totalXp = s.totalXp,
                restEndsAt = s.restEndsAt,
                restDurationSec = s.restDurationSec,
                currentIndex = s.currentExerciseIndex.coerceIn(0, (exercises.size - 1).coerceAtLeast(0)),
                exercises = exercises,
                heartRateBpm = s.heartRateBpm,
                hrZone = s.hrZoneLabel,
                watchLinked = s.watchLinked,
                bodyWeightKg = container.prefs.bodyWeightKg(),
                ageYears = container.prefs.ageYears(),
                maxHr = container.prefs.maxHr()
            )
        )
        updateWorkoutNotification()
    }

    private fun updateWorkoutNotification() {
        workoutNotification.update(_uiState.value, startedAt)
    }

    private fun maybeMomentLoot(index: Int, weightKg: Double, reps: Int, rir: Int?) {
        if (demoMode) return
        viewModelScope.launch {
            val s = _uiState.value
            val ex = s.exercises.getOrNull(index) ?: return@launch
            val isPr = ex.category == ExerciseCategory.STRENGTH &&
                container.repository.isPersonalRecord(ex.name, weightKg)
            val heavy = ex.category == ExerciseCategory.STRENGTH &&
                ((weightKg * reps >= 800) || (rir != null && rir <= 1))
            val milestone = s.totalSets > 0 && s.totalSets % 5 == 0
            val trigger = when {
                isPr -> MomentTrigger.PERSONAL_RECORD
                heavy && kotlin.random.Random.nextInt(100) < 25 -> MomentTrigger.HEAVY_LIFT
                milestone && kotlin.random.Random.nextInt(100) < 15 -> MomentTrigger.MILESTONE
                else -> null
            } ?: return@launch

            // Cap soft moments at 2; PRs always get a roll.
            if (!isPr && s.momentSpoilsUsed >= 2) return@launch

            val loot = container.repository.grantMomentLoot(trigger)
            if (loot.isEmpty) return@launch
            val labels = loot.labels()
            _uiState.update {
                it.copy(
                    momentSpoil = MomentSpoil(labels = labels, storyPending = it.hasAi, isPr = isPr),
                    momentSpoilsUsed = it.momentSpoilsUsed + 1
                )
            }
            if (isPr) {
                wearBridge.pushFeedback(WearFeedbackKind.PR, "Personal record! The chronicle remembers.")
            }
            if (s.hasAi) {
                val hero = container.repository.getCharacter()
                val story = container.gemini.lootStory(
                    playerName = hero.name,
                    classLabel = hero.characterClass?.label ?: "Hero",
                    level = hero.level,
                    exerciseName = ex.name,
                    weightKg = weightKg,
                    reps = reps,
                    isPr = isPr,
                    lootLabels = labels
                ).getOrElse {
                    "The forge remembers this ${ex.name}. Spoils of sweat: ${labels.joinToString(", ")}."
                }
                _uiState.update { st ->
                    st.copy(momentSpoil = st.momentSpoil?.copy(story = story, storyPending = false))
                }
            } else {
                _uiState.update { st ->
                    st.copy(
                        momentSpoil = st.momentSpoil?.copy(
                            story = "Your ${ex.name} shakes the realm. Spoils: ${labels.joinToString(", ")}.",
                            storyPending = false
                        )
                    )
                }
            }
        }
    }

    private fun maybeAmbushOffer() {
        if (demoMode) return
        val s = _uiState.value
        if (s.ambushOfferedThisSession || s.restEndsAt == null) return
        if (kotlin.random.Random.nextInt(100) >= 15) return
        viewModelScope.launch {
            val hero = container.repository.getCharacter()
            val monster = MonsterCatalog.ambushForLevel(hero.level)
            val gold = 20 + hero.level * 5
            val energy = 5
            _uiState.update {
                it.copy(
                    ambushOfferedThisSession = true,
                    ambushOffer = AmbushOffer(
                        monsterId = monster.id,
                        monsterName = monster.name,
                        monsterEmoji = monster.emoji,
                        goldWager = gold,
                        energyWager = energy
                    )
                )
            }
            wearBridge.pushFeedback(
                WearFeedbackKind.AMBUSH,
                "Ambush! ${monster.name} stalks your rest — check your phone."
            )
        }
    }

    fun dismissMomentSpoil() = _uiState.update { it.copy(momentSpoil = null) }

    fun declineAmbush() = _uiState.update { it.copy(ambushOffer = null) }

    fun acceptAmbush(onNavigate: (Int) -> Unit) {
        val offer = _uiState.value.ambushOffer ?: return
        viewModelScope.launch {
            val paid = container.repository.payAmbushWager(offer.goldWager, offer.energyWager)
            if (!paid) {
                _uiState.update { it.copy(ambushOffer = null) }
                return@launch
            }
            val remaining = _uiState.value.restEndsAt?.let { (it - System.currentTimeMillis()).coerceAtLeast(0) }
            _uiState.update {
                it.copy(
                    ambushOffer = null,
                    restPausedRemainingMs = remaining,
                    restEndsAt = null
                )
            }
            onNavigate(offer.monsterId)
        }
    }

    /** Call when returning from an ambush fight. */
    fun resumeAfterAmbush(won: Boolean) {
        _uiState.update { s ->
            val remaining = s.restPausedRemainingMs ?: (s.restDurationSec * 1000L)
            s.copy(
                restPausedRemainingMs = null,
                restEndsAt = System.currentTimeMillis() + remaining,
                ambushXpMult = if (won) 1.3f else s.ambushXpMult,
                heatStreak = if (won) s.heatStreak + 2 else s.heatStreak
            )
        }
        publishWearState()
    }

    /** Consumes FightScreen ambush outcome when the session screen resumes. */
    fun consumeAmbushResultIfNeeded() {
        val result = container.lastAmbushVictory ?: return
        if (_uiState.value.restPausedRemainingMs == null) {
            container.lastAmbushVictory = null
            return
        }
        container.lastAmbushVictory = null
        resumeAfterAmbush(result)
    }

    /** When an exercise hits its target sets, ask the coach to review the rest of the plan. */
    private fun maybeAutoCoach(index: Int) {
        val s = _uiState.value
        if (!s.hasAi || s.coachLoading) return
        val ex = s.exercises.getOrNull(index) ?: return
        if (ex.loggedSets.size < ex.targetSets) return
        if (!coachedExercises.add(ex.name)) return
        // Only bother the coach if something is left to adjust.
        if (s.exercises.none { it.loggedSets.isEmpty() }) return
        askCoach()
    }

    fun askCoach() {
        val s = _uiState.value
        if (!s.hasAi || s.coachLoading || s.totalSets == 0) return
        _uiState.update { it.copy(coachLoading = true, coach = null, coachError = null) }
        val performed = s.exercises.asSequence().filter { it.loggedSets.isNotEmpty() }.joinToString("\n") { ex ->
            val sets = ex.loggedSets.joinToString(", ") { set ->
                val effort = set.rir?.let { " @$it RIR" }.orEmpty()
                when (set.category) {
                    ExerciseCategory.STRENGTH -> "${set.weightKg}kg x ${set.reps}$effort"
                    ExerciseCategory.BODYWEIGHT -> "${set.reps} reps$effort"
                    ExerciseCategory.CARDIO -> buildString {
                        append(Units.formatTimeMinutes(set.durationMin))
                        if (set.distanceKm > 0) append(" / ${set.distanceKm}km")
                        if (set.speedKmh > 0) append(" @ ${set.speedKmh} km/h")
                        if (set.inclinePercent > 0) append(" / ${set.inclinePercent}% incline")
                        if (set.cardioProgram.isNotBlank()) append(" [${set.cardioProgram}]")
                    }
                    else -> Units.formatTimeMinutes(set.durationMin)
                }
            }
            "- ${ex.name} (target ${ex.targetSets}x${ex.targetReps}): $sets"
        }
        val remaining = s.exercises.filter { it.loggedSets.isEmpty() }
            .joinToString("\n") { "- ${it.name} (${it.category.name}, planned ${it.targetSets}x${it.targetReps})" }
            .ifBlank { "(none)" }
            
        val hrContext = s.heartRateBpm?.let { 
            "\nCurrent Heart Rate: $it bpm (${s.hrZoneLabel ?: "Unknown"} zone)" 
        }.orEmpty()

        viewModelScope.launch {
            val history = container.repository.recentWorkoutsSummary(limit = 5)
            container.gemini.coachSession(performed, remaining, container.prefs.profile.value, history + hrContext)
                .onSuccess { advice ->


                    // Drop changes that reference exercises we can't find or that already started.
                    val valid = advice.changes.filter { change ->
                        _uiState.value.exercises.any {
                            it.loggedSets.isEmpty() && it.name.equals(change.exercise, ignoreCase = true)
                        }
                    }
                    _uiState.update { it.copy(coachLoading = false, coach = advice.copy(changes = valid)) }
                }
                .onFailure { e ->
                    Log.e("FitnessRPG", "Coach check-in failed", e)
                    _uiState.update {
                        it.copy(
                            coachLoading = false,
                            coachError = "The coach lost the thread: ${e.message ?: "unknown error"}"
                        )
                    }
                }
        }
    }

    fun dismissCoachError() = _uiState.update { it.copy(coachError = null) }

    fun applyCoachChanges() {
        val advice = _uiState.value.coach ?: return
        viewModelScope.launch {
            var list = _uiState.value.exercises
            for (change in advice.changes) {
                val idx = list.indexOfFirst {
                    it.loggedSets.isEmpty() && it.name.equals(change.exercise, ignoreCase = true)
                }
                if (idx >= 0) {
                    val ex = list[idx]
                    val nextName = change.replaceWith ?: ex.name
                    val nextCategory = change.replaceCategory ?: ex.category
                    val tracking = resolveExerciseTracking(nextName, nextCategory)
                    list = list.toMutableList().also {
                        it[idx] = ex.copy(
                            name = nextName,
                            category = tracking.first,
                            targetSets = change.sets ?: ex.targetSets,
                            targetReps = change.reps ?: ex.targetReps,
                            trackingType = tracking.second
                        )
                    }
                }
            }
            _uiState.update { it.copy(exercises = list, coach = null) }
            publishWearState()
        }
    }

    fun dismissCoach() = _uiState.update { it.copy(coach = null) }

    fun dismissSwapNote() = _uiState.update { it.copy(swapNote = null) }

    fun moveExercise(index: Int, delta: Int) {
        _uiState.update { s ->
            val target = index + delta
            if (target !in s.exercises.indices) return@update s
            s.copy(
                exercises = s.exercises.toMutableList().also { list ->
                    val tmp = list[index]
                    list[index] = list[target]
                    list[target] = tmp
                }
            )
        }
    }

    fun swapExercise(index: Int, name: String, category: ExerciseCategory) {
        viewModelScope.launch {
            val resolved = resolveExerciseTracking(name, category)
            _uiState.update { s ->
                val ex = s.exercises.getOrNull(index) ?: return@update s
                s.copy(exercises = s.exercises.toMutableList().also {
                    it[index] = ex.copy(name = name, category = resolved.first, trackingType = resolved.second)
                })
            }
            publishWearState()
        }
    }

    /** Lets the AI pick a replacement matching the player's equipment profile. */
    fun aiSwap(index: Int) {
        val s = _uiState.value
        val ex = s.exercises.getOrNull(index) ?: return
        if (!s.hasAi || s.aiSwapIndex != null) return
        _uiState.update { it.copy(aiSwapIndex = index, swapNote = null) }
        viewModelScope.launch {
            container.gemini.suggestSwap(
                exerciseName = ex.name,
                category = ex.category,
                targetSets = ex.targetSets,
                targetReps = ex.targetReps,
                avoid = s.exercises.map { it.name },
                profile = container.prefs.profile.value
            ).onSuccess { swap ->
                val tracking = resolveExerciseTracking(swap.name, swap.category)
                _uiState.update { state ->
                    val current = state.exercises.getOrNull(index)
                    if (current == null) state.copy(aiSwapIndex = null)
                    else state.copy(
                        aiSwapIndex = null,
                        swapNote = "Swapped ${current.name} \u2192 ${swap.name}. ${swap.reason}",
                        exercises = state.exercises.toMutableList().also {
                            it[index] = current.copy(
                                name = swap.name,
                                category = tracking.first,
                                targetSets = swap.sets,
                                targetReps = swap.reps,
                                trackingType = tracking.second
                            )
                        }
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(aiSwapIndex = null, swapNote = "Swap failed: ${e.message}")
                }
            }
        }
    }

    fun skipRest() {
        _uiState.update { it.copy(restEndsAt = null) }
        publishWearState()
    }

    fun extendRest(seconds: Int) {
        val now = System.currentTimeMillis()
        _uiState.update { s ->
            val baseTime = s.restEndsAt?.takeIf { it > now } ?: now
            s.copy(restEndsAt = baseTime + seconds * 1000L)
        }
        publishWearState()
    }

    fun setRestDuration(seconds: Int) {
        _uiState.update { it.copy(restDurationSec = seconds) }
        publishWearState()
    }

    fun removeLastSet(index: Int) {
        _uiState.update { s ->
            val ex = s.exercises[index]
            if (ex.loggedSets.isEmpty()) return@update s
            s.copy(
                exercises = s.exercises.toMutableList().also {
                    it[index] = ex.copy(loggedSets = ex.loggedSets.dropLast(1))
                }
            )
        }
        publishWearState()
    }

    fun finish() {
        val state = _uiState.value
        val logs = state.exercises.flatMap { it.loggedSets }
        if (logs.isEmpty()) return
        viewModelScope.launch {
            if (demoMode) {
                val character = container.repository.getCharacter()
                val withXp = logs.map { log ->
                    if (log.xp > 0) log else log.copy(xp = GameMath.xpForSet(log))
                }
                val preview = GameMath.applySession(
                    character = character,
                    logs = withXp,
                    durationMs = System.currentTimeMillis() - startedAt,
                    musclesWorked = emptySet(),
                    weeklyWorkoutsDone = 1,
                    weeklyWorkoutsGoal = 3
                )
                // Preview only — do not persist character, session, party, or guild.
                _uiState.update {
                    it.copy(
                        finish = SessionFinish(
                            result = preview.copy(
                                updatedCharacter = character,
                                levelsGained = 0,
                                statGains = StatGains(),
                                travelKm = 0.0,
                                arrivedAt = null,
                                streak = character.streak,
                                streakSaved = false,
                                xpBoostApplied = 0,
                                prs = emptyList(),
                                lootLabels = emptyList()
                            ),
                            isDemo = true
                        )
                    )
                }
                wearBridge.unbind()
                workoutNotification.cancel()
                return@launch
            }

            val strMult = if (container.prefs.consumeEncounterStrBoost()) 1.15f else 1f
            val result = container.repository.completeSession(
                state.title,
                startedAt,
                logs,
                strengthXpMultiplier = strMult
            )
            // Every XP point earned is dealt to the party raid boss as damage.
            container.party.reportSessionXp(result.xp)
            container.guild.reportSessionXp(result.xp)
            
            val summaryItems = state.exercises.map { ex ->
                val exPrs = result.prs.filter { it.exerciseName == ex.name }
                val icon = container.exerciseInfo.find(ex.name)?.imageUrls?.firstOrNull()
                WorkoutSummaryItem(
                    name = ex.name,
                    isIncreasedWeight = exPrs.any { it.kind == PrKind.WEIGHT },
                    isIncreasedVolume = exPrs.any { it.kind == PrKind.VOLUME },
                    isIncreased1RM = exPrs.any { it.kind == PrKind.ONE_RM },
                    prs = exPrs.filter { it.isNew }.map { it.kind }.toSet(),
                    iconUrl = icon
                )
            }

            val wantPraise = container.gemini.hasKey
            _uiState.update {
                it.copy(
                    finish = SessionFinish(
                        result = result,
                        praisePending = wantPraise,
                        caloriesKcal = state.wearCaloriesKcal?.toInt(),
                        activeDurationMs = state.wearActiveDurationMs,
                        zoneWorkSec = state.wearZoneWorkSec,
                        zoneHighSec = state.wearZoneHighSec,
                        steps = state.wearSteps,
                        distanceMeters = state.wearDistanceMeters,
                        summaryItems = summaryItems
                    )
                )
            }
            wearBridge.unbind()
            workoutNotification.cancel()

            if (wantPraise) {
                val summary = state.exercises
                    .filter { it.loggedSets.isNotEmpty() }
                    .joinToString(", ") { "${it.name} (${it.loggedSets.size} sets)" }
                val praise = container.gemini
                    .sessionPraise(summary, result.updatedCharacter.name)
                    .getOrNull()
                _uiState.update { s ->
                    s.finish?.let { f -> s.copy(finish = f.copy(praise = praise, praisePending = false)) } ?: s
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        wearBridge.unbind()
        workoutNotification.cancel()
        container.music.stop()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ActiveSessionViewModel(appContainer) }
        }
    }
}

@Composable
fun ActiveSessionScreen(
    workoutId: Long,
    onDone: () -> Unit,
    onAmbushFight: (monsterId: Int) -> Unit = {},
    /** Called when the player finishes and dismisses the result dialog (not abandon). */
    onFinished: (() -> Unit)? = null,
    /** When non-null, loads an in-memory practice session that does not count. */
    demoExercises: List<SessionExercise>? = null,
    demoTitle: String = "Demo Quest",
    viewModel: ActiveSessionViewModel = viewModel(factory = ActiveSessionViewModel.Factory)
) {
    LaunchedEffect(workoutId, demoExercises) {
        if (demoExercises != null) {
            viewModel.loadDemo(workoutId, demoTitle, demoExercises)
        } else {
            viewModel.load(workoutId)
        }
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.consumeAmbushResultIfNeeded()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val state by viewModel.uiState.collectAsState()
    val imperial by viewModel.imperial.collectAsState()
    var showPicker by remember { mutableStateOf(value = false) }
    var swapFor by remember { mutableStateOf<Int?>(null) }
    var showPlates by remember { mutableStateOf(false) }

    if (showPlates) {
        PlateCalculatorDialog(onDismiss = { showPlates = false })
    }

    val activity = LocalActivity.current
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Sticky Header: Improved with reference UI layout
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDone, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back")
                    }
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showPlates = true }) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = "Plate calculator")
                    }
                    Button(
                        onClick = { viewModel.finish() },
                        enabled = state.totalSets > 0,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(if (state.isDemo) "Done" else "Finish")
                    }
                }

                // WearOS Status Line
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val statusColor = if (state.watchLinked) Color(0xFF4CAF50) else Color.Gray
                    Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                    Text(
                        text = if (state.watchLinked) "WearOS Watch Connected" else "Watch Disconnected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(Modifier.weight(1f))
                    
                    if (state.heartRateBpm != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                            Text("${state.heartRateBpm} bpm", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    val kcal = state.wearCaloriesKcal
                    if (kcal != null) {
                        Spacer(Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                            Text("${kcal.toInt()} kcal", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Media Controller Integration
                if (state.media.packageName != null) {
                    MediaControllerCard(
                        state = state.media,
                        onToggle = viewModel::toggleMusic,
                        onNext = viewModel::skipMusic,
                        onPrev = viewModel::prevMusic
                    )
                }

                // Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem("Duration", formatWearDuration(state.wearSessionDurationMs ?: 0L))
                    MetricItem("Volume", "${state.totalVolumeKg.toInt()} ${Units.label(imperial)}")
                    MetricItem("Sets", state.totalSets.toString())
                }
            }
        }

        // Scrollable Exercise List
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.wearBanner?.let { banner ->
                item {
                    SectionCard {
                        Text(banner, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                        TextButton(onClick = viewModel::dismissWearBanner) { Text("Got it") }
                    }
                }
            }

            if (state.coachLoading) {
                item {
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("\uD83E\uDDD9 The coach studies your battle log...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            state.coach?.let { coach ->
                item { CoachCard(coach, onApply = viewModel::applyCoachChanges, onDismiss = viewModel::dismissCoach) }
            }

            state.coachError?.let { error ->
                item {
                    SectionCard {
                        Text(
                            "\uD83E\uDDD9 $error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = viewModel::askCoach) { Text("Try again") }
                            TextButton(onClick = viewModel::dismissCoachError) { Text("Dismiss") }
                        }
                    }
                }
            }

            state.swapNote?.let { note ->
                item {
                    SectionCard {
                        Text("\u2728 $note", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                        TextButton(onClick = viewModel::dismissSwapNote) { Text("Got it") }
                    }
                }
            }

            itemsIndexed(state.exercises, key = { _, ex -> ex.name }) { index, exercise ->
                val effortMethod by viewModel.effortMethod.collectAsState()
                val showCardioIntensity by viewModel.showCardioIntensity.collectAsState()
                ExerciseLogCard(
                    exercise = exercise,
                    imperial = imperial,
                    effortMethod = effortMethod,
                    bodyWeightKg = viewModel.bodyWeightKgOrNull(),
                    showCardioIntensity = showCardioIntensity,
                    onToggleCardioIntensity = viewModel::setShowCardioIntensity,
                    aiSwapping = state.aiSwapIndex == index,
                    canMoveUp = index > 0,
                    canMoveDown = index < state.exercises.lastIndex,
                    canRemove = state.exercises.size > 1,
                    onMove = { delta -> viewModel.moveExercise(index, delta) },
                    onSwap = { swapFor = index },
                    onRemove = { viewModel.removeExercise(index) },
                    onLogSet = { w, r, dur, dist, rir, speed, incline, program, st ->
                        viewModel.logSet(
                            index = index,
                            weightKg = w,
                            reps = r,
                            durationMin = dur,
                            distanceKm = dist,
                            rir = rir,
                            speedKmh = speed,
                            inclinePercent = incline,
                            cardioProgram = program,
                            setType = st
                        )
                    },

                    onUndo = { viewModel.removeLastSet(index) }
                )
            }

            item {
                OutlinedButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add Exercise")
                }
            }

            if (state.hasAi && state.totalSets > 0 && !state.coachLoading) {
                item {
                    OutlinedButton(onClick = viewModel::askCoach, modifier = Modifier.fillMaxWidth()) {
                        Text("\uD83E\uDDD9 Coach check-in")
                    }
                }
            }
        }

        // Sticky Bottom: Rest Timer Bar
        state.restEndsAt?.let { endsAt ->
            RestTimerBar(
                endsAt = endsAt,
                onExtend = { viewModel.extendRest(15) },
                onSkip = viewModel::skipRest,
                onFinish = viewModel::skipRest
            )
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            onDismiss = { showPicker = false },
            onPick = { name, category -> viewModel.addExercise(name, category) }
        )
    }

    swapFor?.let { index ->
        state.exercises.getOrNull(index)?.let { exercise ->
            ExercisePickerDialog(
                title = "Swap ${exercise.name}",
                header = if (state.hasAi) {
                    {
                        Button(
                            onClick = {
                                swapFor = null
                                viewModel.aiSwap(index)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("\u2728 Let the AI pick (uses your equipment)")
                        }
                    }
                } else {
                    null
                },
                onDismiss = { swapFor = null },
                onPick = { name, category ->
                    swapFor = null
                    viewModel.swapExercise(index, name, category)
                }
            )
        }
    }

    state.momentSpoil?.let { spoil ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMomentSpoil,
            title = {
                Text(if (spoil.isPr) "\uD83C\uDFC6 Record Spoils!" else "\uD83C\uDF81 Spoils!")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    spoil.labels.forEach { Text("\u2022 $it", fontWeight = FontWeight.SemiBold) }
                    when {
                        spoil.storyPending -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        spoil.story != null -> Text(
                            "\u201C${spoil.story}\u201D",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissMomentSpoil) { Text("Claim") }
            }
        )
    }

    state.ambushOffer?.let { offer ->
        AlertDialog(
            onDismissRequest = viewModel::declineAmbush,
            title = { Text("${offer.monsterEmoji} Ambush!") },
            text = {
                Text(
                    "A ${offer.monsterName} stalks your rest. Wager ${offer.goldWager} gold and " +
                        "${offer.energyWager} energy for a harder fight. Win for rich loot and a " +
                        "+30% XP boost on remaining sets."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.acceptAmbush(onAmbushFight) }) { Text("Fight") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::declineAmbush) { Text("Keep resting") }
            }
        )
    }

    state.finish?.let { finish ->
        // Level-up fanfare first; the quest summary takes over once it's claimed.
        // (Rendering both at once would stack the summary dialog on top.)
        var showLevelUp by remember(finish) { mutableStateOf(finish.result.levelsGained > 0) }
        if (showLevelUp) {
            val newLevel = finish.result.updatedCharacter.level
            val oldLevel = newLevel - finish.result.levelsGained
            val cls = finish.result.updatedCharacter.characterClass ?: CharacterClass.WARRIOR
            LevelUpModal(
                newLevel = newLevel,
                clazz = cls,
                statGains = finish.result.statGains,
                unlockedSkills = cls.skills.filter { it.unlockLevel in (oldLevel + 1)..newLevel },
                onDismiss = { showLevelUp = false }
            )
        } else {
            SessionResultDialog(finish = finish, imperial = imperial, onDismiss = { (onFinished ?: onDone)() })
        }
    }
}



@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Bottom sticky bar for the rest timer, accessible and clean. */
@Composable
private fun RestTimerBar(
    endsAt: Long,
    onExtend: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    var now by remember(endsAt) { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(endsAt) {
        while (System.currentTimeMillis() < endsAt) {
            now = System.currentTimeMillis()
            delay(200.milliseconds)
        }
        now = System.currentTimeMillis()
        AudioEffects.playRestDone()
        onFinish()
    }

    val remainingMs = (endsAt - now).coerceAtLeast(0L)
    val remainingSec = (remainingMs / 1000L).toInt()

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Filled.Timer, contentDescription = null)
            
            Text(
                text = if (remainingSec > 0) "Resting: %d:%02d".format(remainingSec / 60, remainingSec % 60) else "Rest Complete!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = onExtend,
                enabled = true
            ) {
                Text("+15s", fontWeight = FontWeight.Bold)
            }
            
            Button(
                onClick = onSkip,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentColor = MaterialTheme.colorScheme.primaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Skip")
            }
        }
    }
}

private fun formatWearDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}



@Composable
private fun ExerciseLogCard(
    exercise: SessionExercise,
    imperial: Boolean,
    effortMethod: EffortMethod,
    bodyWeightKg: Double? = null,
    showCardioIntensity: Boolean = false,
    onToggleCardioIntensity: (Boolean) -> Unit = {},
    aiSwapping: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean = true,
    onMove: (delta: Int) -> Unit,
    onSwap: () -> Unit,
    onRemove: () -> Unit = {},
    onLogSet: (
        weightKg: Double,
        reps: Int,
        durationMin: Double,
        distanceKm: Double,
        rir: Int?,
        speedKmh: Double,
        inclinePercent: Double,
        cardioProgram: String,
        setType: SetType
    ) -> Unit,

    onUndo: () -> Unit
) {
    var weight by rememberSaveable(exercise.name) { 
        mutableStateOf(
            exercise.targetWeightKg?.let { 
                Units.toDisplay(it, imperial).let(HeightFormat::trimNum)
            } ?: ""
        ) 
    }

    var reps by rememberSaveable(exercise.name) { mutableStateOf("") }
    var durationMin by rememberSaveable(exercise.name) { mutableStateOf("") }
    var durationSec by rememberSaveable(exercise.name) { mutableStateOf("") }
    var distance by rememberSaveable(exercise.name) { mutableStateOf("") }
    var speed by rememberSaveable(exercise.name) { mutableStateOf("") }
    var incline by rememberSaveable(exercise.name) { mutableStateOf("") }
    var program by rememberSaveable(exercise.name) { mutableStateOf("") }
    var effort by rememberSaveable(exercise.name) { mutableStateOf<Int?>(null) }
    var setType by rememberSaveable(exercise.name) { mutableStateOf(SetType.NORMAL) }

    
    var timerRunning by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            while (timerRunning) {
                delay(1000)
                timerSeconds++
                durationMin = (timerSeconds / 60).toString()
                durationSec = (timerSeconds % 60).toString()
            }
        }
    }

    val effortApplies = effortMethod != EffortMethod.OFF &&
        exercise.trackingType in setOf(
            ExerciseTrackingType.WEIGHT_REPS,
            ExerciseTrackingType.BODYWEIGHT_REPS,
            ExerciseTrackingType.ASSISTED_REPS
        )

    // Effort autoregulation: derive the next-set target from the last rated set.
    val lastSet = exercise.loggedSets.lastOrNull()
    val suggestion = if (effortApplies && lastSet?.rir != null) {
        GameMath.suggestNextSet(lastSet.weightKg, lastSet.reps, lastSet.rir)
    } else {
        null
    }
    val suggestedWeightText = suggestion?.weightKg?.takeIf { it > 0 }?.let {
        // Round to one decimal first so kg<->lb float noise doesn't show "50.0".
        val display = (Units.toDisplay(
            Units.roundToPlate(it, imperial),
            imperial
        ) * 10.0).roundToInt() / 10.0
        if (display % 1.0 == 0.0) display.toInt().toString() else display.toString()
    }

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Floating "+XP" burst that pops over the card when a set lands, plus
    // prefilling the next set's fields from the suggestion.
    var burstVisible by remember(exercise.name) { mutableStateOf(false) }
    var burstXp by remember(exercise.name) { mutableIntStateOf(0) }
    var seenSets by rememberSaveable(exercise.name) { mutableIntStateOf(exercise.loggedSets.size) }
    LaunchedEffect(exercise.loggedSets.size) {
        if (exercise.loggedSets.size > seenSets) {
            burstXp = exercise.loggedSets.last().xp
            burstVisible = true
            seenSets = exercise.loggedSets.size
            AudioEffects.playSetLogged()
            HapticEffects.performSetLogged(haptic, context)

            if (suggestion != null) {
                suggestedWeightText?.let { weight = it }
                reps = suggestion.reps.toString()
            }

            delay(1200.milliseconds)
            burstVisible = false
        } else {
            seenSets = exercise.loggedSets.size
        }
    }

    var showDetail by remember(exercise.name) { mutableStateOf(false) }
    if (showDetail) {
        ExerciseDetailDialog(name = exercise.name, onDismiss = { showDetail = false })
    }
    var showPlates by remember(exercise.name) { mutableStateOf(false) }
    if (showPlates) {
        PlateCalculatorDialog(
            onDismiss = { showPlates = false },
            onUseWeight = { display ->
                val rounded = (display * 10.0).roundToInt() / 10.0
                weight = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
            }
        )
    }

    Box {
    SectionCard {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { showDetail = true }
                ) {
                    Text(
                        exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${exercise.category.label} \u2022 ${targetSummary(exercise)} \u2022 ${exercise.category.statLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (exercise.trackingType == ExerciseTrackingType.WEIGHT_REPS) {
                    IconButton(onClick = { showPlates = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = "Plate calculator", modifier = Modifier.size(22.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (aiSwapping) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = { onMove(-1) }, enabled = canMoveUp, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
                    }
                    IconButton(onClick = { onMove(1) }, enabled = canMoveDown, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
                    }
                    TextButton(
                        onClick = onSwap,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("\u21C4 Swap", style = MaterialTheme.typography.labelLarge)
                    }
                    IconButton(onClick = onRemove, enabled = canRemove, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Remove exercise",
                            modifier = Modifier.size(20.dp),
                            tint = if (canRemove) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
        }

        exercise.loggedSets.forEachIndexed { i, set ->
            val effortTag = set.rir?.let { "  \u00B7 ${effortMethod.display(it)}" }.orEmpty()
            Text(
                "Set ${i + 1} \u2014 ${setSummary(set, imperial)}$effortTag  (+${set.xp} XP)",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (effortApplies) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                Text(
                    "${effortMethod.label}:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val options = if (effortMethod == EffortMethod.RPE) {
                    listOf(4 to "6", 3 to "7", 2 to "8", 1 to "9", 0 to "10")
                } else {
                    listOf(0 to "0", 1 to "1", 2 to "2", 3 to "3", 4 to "4", 5 to "5+")
                }
                options.forEach { (rirValue, label) ->
                    FilterChip(
                        selected = effort == rirValue,
                        onClick = { effort = if (effort == rirValue) null else rirValue },
                        label = { Text(label) },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
            }
        }

        // Set Type Selector
        if (exercise.category in setOf(ExerciseCategory.STRENGTH, ExerciseCategory.BODYWEIGHT)) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp)
                ) {
                    SetType.entries.forEach { type ->
                        FilterChip(
                            selected = setType == type,
                            onClick = { setType = type },
                            label = { Text(type.label, fontSize = 11.sp) },
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {

            when (exercise.trackingType) {
                ExerciseTrackingType.WEIGHT_REPS -> {
                    NumberField(weight, { weight = it }, Units.label(imperial), Modifier.weight(1f))
                    NumberField(reps, { reps = it }, "reps", Modifier.weight(1f))
                }
                ExerciseTrackingType.CARDIO_MACHINE,
                ExerciseTrackingType.DISTANCE_TIME -> {
                    NumberField(durationMin, { durationMin = it }, "min", Modifier.weight(1f))
                    Text(":", style = MaterialTheme.typography.titleMedium, color = Gold, modifier = Modifier.padding(horizontal = 1.dp))
                    NumberField(durationSec, { durationSec = it }, "sec", Modifier.weight(1f))
                    IconButton(
                        onClick = { 
                            timerRunning = !timerRunning
                            if (timerRunning) {
                                timerSeconds = (durationMin.toIntOrNull() ?: 0) * 60 + (durationSec.toIntOrNull() ?: 0)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (timerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Stopwatch",
                            tint = Gold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    NumberField(distance, { distance = it }, Units.distLabel(imperial), Modifier.weight(1f))
                }
                ExerciseTrackingType.DISTANCE_ONLY -> {
                    NumberField(distance, { distance = it }, Units.distLabel(imperial), Modifier.weight(1f))
                }
                ExerciseTrackingType.BODYWEIGHT_REPS -> {
                    NumberField(reps, { reps = it }, "reps", Modifier.weight(1f))
                    if (bodyWeightKg != null && bodyWeightKg > 0) {
                        Text(
                            "Load ${Units.formatWeight(bodyWeightKg, imperial)} (body)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                ExerciseTrackingType.ASSISTED_REPS -> {
                    NumberField(weight, { weight = it }, "assist ${Units.label(imperial)}", Modifier.weight(1f))
                    NumberField(reps, { reps = it }, "reps", Modifier.weight(1f))
                }
                ExerciseTrackingType.REPS_ONLY -> {
                    NumberField(reps, { reps = it }, "reps", Modifier.weight(1f))
                }
                ExerciseTrackingType.TIME_ONLY -> {
                    NumberField(durationMin, { durationMin = it }, "min", Modifier.weight(1f))
                    Text(":", style = MaterialTheme.typography.titleMedium, color = Gold, modifier = Modifier.padding(horizontal = 1.dp))
                    NumberField(durationSec, { durationSec = it }, "sec", Modifier.weight(1f))
                    IconButton(
                        onClick = { 
                            timerRunning = !timerRunning
                            if (timerRunning) {
                                timerSeconds = (durationMin.toIntOrNull() ?: 0) * 60 + (durationSec.toIntOrNull() ?: 0)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (timerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Stopwatch",
                            tint = Gold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Button(
                onClick = {
                    val totalMins = (durationMin.toDoubleOrNull() ?: 0.0) + (durationSec.toDoubleOrNull() ?: 0.0) / 60.0
                    onLogSet(
                        Units.toKg(weight.toDoubleOrNull() ?: 0.0, imperial),
                        reps.toIntOrNull() ?: 0,
                        totalMins,
                        Units.toKm(distance.toDoubleOrNull() ?: 0.0, imperial),
                        if (effortApplies) effort else null,
                        Units.toSpeedKmh(speed.toDoubleOrNull() ?: 0.0, imperial),
                        incline.toDoubleOrNull() ?: 0.0,
                        program,
                        setType
                    )
                    effort = null
                    setType = SetType.NORMAL
                    timerRunning = false

                    timerSeconds = 0
                    durationMin = ""
                    durationSec = ""
                },
                modifier = Modifier.height(48.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                enabled = when (exercise.trackingType) {
                    ExerciseTrackingType.WEIGHT_REPS ->
                        weight.toDoubleOrNull() != null && (reps.toIntOrNull() ?: 0) > 0
                    ExerciseTrackingType.CARDIO_MACHINE,
                    ExerciseTrackingType.DISTANCE_TIME ->
                        (durationMin.toDoubleOrNull() ?: 0.0) > 0.0 || (durationSec.toDoubleOrNull() ?: 0.0) > 0.0 || (distance.toDoubleOrNull() ?: 0.0) > 0.0
                    ExerciseTrackingType.DISTANCE_ONLY -> (distance.toDoubleOrNull() ?: 0.0) > 0.0
                    ExerciseTrackingType.ASSISTED_REPS ->
                        weight.toDoubleOrNull() != null && (reps.toIntOrNull() ?: 0) > 0
                    ExerciseTrackingType.BODYWEIGHT_REPS,
                    ExerciseTrackingType.REPS_ONLY -> (reps.toIntOrNull() ?: 0) > 0
                    ExerciseTrackingType.TIME_ONLY -> (durationMin.toDoubleOrNull() ?: 0.0) > 0.0 || (durationSec.toDoubleOrNull() ?: 0.0) > 0.0
                }
            ) { Text("Log") }
        }

        if (exercise.trackingType == ExerciseTrackingType.CARDIO_MACHINE) {
            TextButton(
                onClick = { onToggleCardioIntensity(!showCardioIntensity) },
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text(
                    if (showCardioIntensity) "\u2212 Less detail"
                    else "+ Intensity (speed/incline)"
                )
            }
            if (showCardioIntensity) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(speed, { speed = it }, Units.speedLabel(imperial), Modifier.weight(1f))
                    NumberField(incline, { incline = it }, "incline %", Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = program,
                    onValueChange = { program = it },
                    label = { Text("Program (optional)") },
                    placeholder = { Text("Manual, Hill, Intervals…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (suggestion != null) {
            val target = if (suggestedWeightText != null) {
                "$suggestedWeightText ${Units.label(imperial)} \u00D7 ${suggestion.reps}"
            } else {
                "${suggestion.reps} reps"
            }
            Text(
                "\uD83D\uDCA1 ${suggestion.note} \u00B7 next: $target",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (exercise.loggedSets.isNotEmpty()) {
            TextButton(onClick = onUndo) { Text("Undo last set") }
        }
    }

    if (burstVisible) {
        Box(Modifier.align(Alignment.Center)) {
            FloatingTextBurst(
                burst = FloatingBurst(
                    text = "+$burstXp XP",
                    subtext = "${exercise.category.statLabel} GAIN!",
                    color = Gold
                ),
                onFinished = { }
            )
        }
    }
    }
}

/** Coach advice card: message plus the proposed plan changes, applied all at once. */
@Composable
private fun CoachCard(coach: CoachAdvice, onApply: () -> Unit, onDismiss: () -> Unit) {
    SectionCard(title = "\uD83E\uDDD9 Coach's counsel") {
        Text(coach.message, style = MaterialTheme.typography.bodyMedium)
        coach.changes.forEach { change ->
            val action = when {
                change.replaceWith != null -> "${change.exercise} \u2192 ${change.replaceWith}"
                else -> "${change.exercise}: ${change.sets ?: "?"} \u00D7 ${change.reps ?: "?"}"
            }
            Column {
                Text(action, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                if (change.reason.isNotBlank()) {
                    Text(
                        change.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (coach.changes.isNotEmpty()) {
                Button(onClick = onApply, modifier = Modifier.weight(1f)) { Text("Apply changes") }
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text(if (coach.changes.isEmpty()) "Thanks!" else "Keep plan")
            }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onChange,
        label = {
            Text(
                text = label,
                fontSize = 9.sp,
                maxLines = 1,
                softWrap = false,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.height(48.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        )
    )
}

private fun setSummary(set: SetLogEntity, imperial: Boolean): String {
    val typePrefix = if (set.setType != SetType.NORMAL) "[${set.setType.shortLabel}] " else ""
    val body = when (set.category) {
        ExerciseCategory.STRENGTH -> "${Units.formatWeight(set.weightKg, imperial)} \u00D7 ${set.reps}"
        ExerciseCategory.CARDIO -> buildString {
            if (set.durationMin > 0) append(Units.formatTimeMinutes(set.durationMin))
            if (set.distanceKm > 0) {
                if (isNotEmpty()) append(" \u00B7 ")
                append(Units.formatDistance(set.distanceKm, imperial))
            }
            if (set.speedKmh > 0) append(" \u00B7 ${Units.formatSpeed(set.speedKmh, imperial)}")
            if (set.inclinePercent > 0) append(" \u00B7 ${trim(set.inclinePercent)}%")
            if (set.cardioProgram.isNotBlank()) append(" \u00B7 ${set.cardioProgram}")
            if (isBlank()) append("cardio")
        }
        ExerciseCategory.BODYWEIGHT -> if (set.reps > 0) "${set.reps} reps" else Units.formatTimeMinutes(set.durationMin)
        ExerciseCategory.FLEXIBILITY -> Units.formatTimeMinutes(set.durationMin)
    }
    return typePrefix + body
}


private fun targetSummary(exercise: SessionExercise): String = when (exercise.trackingType) {
    ExerciseTrackingType.CARDIO_MACHINE,
    ExerciseTrackingType.DISTANCE_TIME -> "Tgt ${exercise.targetReps}m"
    ExerciseTrackingType.DISTANCE_ONLY -> "Tgt Dist"
    ExerciseTrackingType.TIME_ONLY -> "Tgt ${exercise.targetReps}m"
    else -> "Tgt ${exercise.targetSets}\u00D7${exercise.targetReps}"
}

private fun trim(d: Double): String =
    if (d % 1.0 == 0.0) d.toInt().toString() else "%.1f".format(d)

/**
 * Full-screen celebration shown when the quest is finished.
 * Redesigned to show Duration, Volume, PRs, and Muscles Worked.
 */
@Composable
private fun SessionResultDialog(finish: SessionFinish, imperial: Boolean, onDismiss: () -> Unit) {
    val r = finish.result
    var shown by remember { mutableStateOf(false) }
    var showRewards by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    
    val durationText = formatWearDuration(r.durationMs)
    val volumeText = "${r.volumeKg.roundToInt()} ${Units.label(imperial)}"
    val prCount = r.prs.count { it.isNew }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NightBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = if (finish.isDemo) "Demo Complete!" else "Well Done!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Text(
                    text = "You completed ${r.weeklyWorkoutsDone} of ${r.weeklyWorkoutsGoal} workouts this week.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )

                // Muscle Map / Avatar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    CharacterAvatar(
                        clazz = r.updatedCharacter.characterClass ?: CharacterClass.WARRIOR,
                        modifier = Modifier.size(200.dp),
                        highlightMuscles = r.musclesWorked,
                        expression = AvatarExpression.VICTORIOUS
                    )
                }

                // Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryStatCard(
                        label = "Duration",
                        value = durationText,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        label = "Volume",
                        value = volumeText,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        label = "PRs",
                        value = "\uD83C\uDFC5 $prCount",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Workout Summary List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Workout Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    finish.summaryItems.forEach { item ->
                        SummaryItem(item)
                    }
                }
                
                // Rewards & PRs Section
                if (r.prs.isNotEmpty()) {
                    PRSection(r.prs, imperial)
                }

                if (finish.praise != null) {
                    Text(
                        "\u201C${finish.praise}\u201D",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(Modifier.weight(1f))
                
                Button(
                    onClick = {
                        if (r.rewardBatch != null) {
                            showRewards = true
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = if (r.rewardBatch != null) ButtonDefaults.buttonColors(containerColor = Gold) else ButtonDefaults.buttonColors()
                ) {
                    Text(
                        if (r.rewardBatch != null) "Claim Rewards" else "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (r.rewardBatch != null) NightBg else Color.White
                    )
                }
                
                Spacer(Modifier.height(16.dp))
            }

            if (showRewards && r.rewardBatch != null) {
                RewardRevealDialog(batch = r.rewardBatch, onDismiss = onDismiss)
            }
            
            ConfettiOverlay(
                modifier = Modifier.fillMaxSize(),
                trigger = r.xp,
                pieces = if (r.levelsGained > 0) 150 else 80
            )
        }
    }
}

@Composable
private fun SummaryStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun PRSection(prs: List<SessionPr>, imperial: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Personal Records", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.6f))
        
        prs.filter { it.isNew }.groupBy { it.exerciseName }.forEach { (name, exPrs) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("\uD83C\uDFC5", fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(name, fontWeight = FontWeight.Bold, color = Color.White)
                    exPrs.forEach { pr ->
                        val label = when (pr.kind) {
                            PrKind.WEIGHT -> "Weight"
                            PrKind.VOLUME -> "Volume"
                            PrKind.ONE_RM -> "1RM"
                            PrKind.DISTANCE -> "Distance"
                            PrKind.PACE -> "Pace"
                            PrKind.TIME -> "Duration"
                            PrKind.REPS -> "Reps"
                            PrKind.SPEED -> "Speed"
                            PrKind.INCLINE -> "Incline"
                        }
                        val valueText = when (pr.kind) {
                            PrKind.WEIGHT -> "${Units.trimmed(Units.toDisplay(pr.value, imperial))} ${Units.label(imperial)} \u00D7 ${pr.reps}"
                            PrKind.DISTANCE -> Units.formatDistance(pr.value, imperial)
                            PrKind.PACE -> Units.formatPace(pr.value, imperial)
                            PrKind.TIME -> Units.formatTimeMinutes(pr.value)
                            PrKind.REPS -> "${pr.value.toInt()} reps"
                            PrKind.SPEED -> Units.formatSpeed(pr.value, imperial)
                            PrKind.INCLINE -> "${Units.trimmed(pr.value)}%"
                            else -> "${Units.trimmed(Units.toDisplay(pr.value, imperial))} ${Units.label(imperial)}"
                        }
                        Text("$label \u2014 $valueText", style = MaterialTheme.typography.bodySmall, color = Gold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LootSection(labels: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Quest Spoils", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.6f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            labels.forEach { label ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}



@Composable
private fun SummaryItem(item: WorkoutSummaryItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (item.iconUrl != null) {
                AsyncImage(
                    model = item.iconUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("\uD83C\uDFCB\uFE0F", fontSize = 20.sp)
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(Modifier.weight(1f)) {
            Text(item.name, fontWeight = FontWeight.Bold, color = Color.White)
            if (item.isIncreasedWeight || item.isIncreasedVolume || item.isIncreased1RM) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.North,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = when {
                            item.isIncreasedWeight -> "Increased Weight"
                            item.isIncreasedVolume -> "Increased Volume"
                            else -> "Increased 1RM"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaControllerCard(
    state: MediaState,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Bolt, // Proxy for music icon
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.title ?: "No Track",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row {
                IconButton(onClick = onPrev) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause"
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next")
                }
            }
        }
    }
}
