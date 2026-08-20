package com.fitnessquest.rpg.ui.screens

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.data.ai.CoachAdvice
import com.fitnessquest.rpg.data.ai.LocalAiEngine
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.SetLogEntity
import com.fitnessquest.rpg.data.health.HeightFormat
import com.fitnessquest.rpg.data.media.MediaState
import com.fitnessquest.rpg.data.sync.OutboxWorker
import com.fitnessquest.rpg.data.wear.WearSessionBridge
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.EffortMethod
import com.fitnessquest.rpg.domain.ExerciseCategories
import com.fitnessquest.rpg.domain.ExerciseTracking
import com.fitnessquest.rpg.domain.ExerciseTrackingType
import com.fitnessquest.rpg.domain.GameMath
import com.fitnessquest.rpg.domain.MomentTrigger
import com.fitnessquest.rpg.domain.MonsterCatalog
import com.fitnessquest.rpg.data.db.ActiveSetLogEntity
import com.fitnessquest.rpg.domain.PrKind
import com.fitnessquest.rpg.domain.RewardBatch
import com.fitnessquest.rpg.domain.SessionPr
import com.fitnessquest.rpg.domain.SessionResult
import com.fitnessquest.rpg.domain.SetType
import com.fitnessquest.rpg.data.party.PartyBoss
import com.fitnessquest.rpg.data.party.PartyPulse
import com.fitnessquest.rpg.domain.StatGains
import com.fitnessquest.rpg.domain.Units
import com.fitnessquest.rpg.notifications.WorkoutNotificationController
import com.fitnessquest.rpg.ui.appContainer
import com.fitnessquest.rpg.ui.components.AvatarDetail
import com.fitnessquest.rpg.ui.components.AvatarExpression
import com.fitnessquest.rpg.ui.components.CharacterAvatar
import com.fitnessquest.rpg.ui.components.ConfettiOverlay
import com.fitnessquest.rpg.ui.components.ExerciseDetailDialog
import com.fitnessquest.rpg.ui.components.ExercisePickerDialog
import com.fitnessquest.rpg.ui.components.FloatingBurst
import com.fitnessquest.rpg.ui.components.GhostBarMeter
import com.fitnessquest.rpg.ui.components.FloatingTextBurst
import com.fitnessquest.rpg.ui.components.LevelUpModal
import com.fitnessquest.rpg.ui.components.PlateCalculatorDialog
import com.fitnessquest.rpg.ui.components.RewardRevealDialog
import com.fitnessquest.rpg.ui.components.SectionCard
import com.fitnessquest.rpg.ui.effects.AudioEffects
import com.fitnessquest.rpg.ui.effects.HapticEffects
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightBg
import com.fitnessquest.shared.wear.WearExerciseState
import com.fitnessquest.shared.wear.WearFeedbackKind
import com.fitnessquest.shared.wear.WearRestAction
import com.fitnessquest.shared.wear.WearSessionState
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

data class SessionExercise(
    val dbId: Long = 0L,
    val name: String,
    val category: ExerciseCategory,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetWeightKg: Double? = null,
    val trackingType: ExerciseTrackingType = ExerciseTracking.resolve(name, category),
    val loggedSets: List<SetLogEntity> = emptyList(),
    val suggestionReason: String? = null,
    val supersetId: String? = null
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
    val isDemo: Boolean = false
)

data class WorkoutSummaryItem(
    val name: String,
    val isIncreasedWeight: Boolean,
    val isIncreasedVolume: Boolean,
    val isIncreased1RM: Boolean,
    val prs: Set<PrKind>,
    val iconUrl: String? = null,
    val muscles: Set<String> = emptySet()
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
    val restEndsAt: Long? = null,
    val restPausedRemainingMs: Long? = null,
    val restDurationSec: Int = 90,
    val hasAi: Boolean = false,
    val coach: CoachAdvice? = null,
    val coachLoading: Boolean = false,
    val coachError: String? = null,
    val finishError: String? = null,
    val aiSwapIndex: Int? = null,
    val swapNote: String? = null,
    val heatStreak: Int = 0,
    val lastLogAt: Long = 0,
    val momentSpoil: MomentSpoil? = null,
    val momentSpoilsUsed: Int = 0,
    val ambushOffer: AmbushOffer? = null,
    val ambushOfferedThisSession: Boolean = false,
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
    val isDemo: Boolean = false,
    val media: MediaState = MediaState(),
    val originalWorkoutId: Long? = null,
    val partyBoss: PartyBoss? = null,
    val partyPulses: List<PartyPulse> = emptyList(),
    val hasPrAchievement: Boolean = false,
    val characterClass: CharacterClass = CharacterClass.WARRIOR
) {
    val totalSets: Int get() = exercises.sumOf { it.loggedSets.size }
    val totalXp: Int get() = exercises.sumOf { ex -> ex.loggedSets.sumOf { it.xp } }
    val totalVolumeKg: Double get() = exercises.sumOf { ex -> ex.loggedSets.sumOf { it.weightKg * it.reps } }
}

class ActiveSessionViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveSessionUiState(hasAi = container.gemini.isAvailable))
    val uiState: StateFlow<ActiveSessionUiState> = _uiState

    val imperial: StateFlow<Boolean> = container.prefs.imperial
    val effortMethod: StateFlow<EffortMethod> = container.prefs.effortMethod
    val showCardioIntensity: StateFlow<Boolean> = container.prefs.showCardioIntensity
    val customCardioPrograms: StateFlow<Set<String>> = container.prefs.customCardioPrograms

    fun setShowCardioIntensity(value: Boolean) = container.prefs.setShowCardioIntensity(value)
    fun addCustomCardioProgram(program: String) = container.prefs.addCustomCardioProgram(program)

    private val wearBridge = WearSessionBridge(container.app)
    private val workoutNotification = WorkoutNotificationController(container.app)
    private val coachedExercises = mutableSetOf<String>()

    private var startedAt: Long = System.currentTimeMillis()
    private val _sessionDurationMs = MutableStateFlow(0L)
    val sessionDurationMs: StateFlow<Long> = _sessionDurationMs

    private var activeCompletionToken: String? = null
    private var loadedFor: Long? = null
    private var demoMode: Boolean = false
    private var isFinishing = false

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

        container.repository.character.onEach { char ->
            _uiState.update { it.copy(characterClass = char.characterClass ?: CharacterClass.WARRIOR) }
        }.launchIn(viewModelScope)

        container.repository.activeSession.onEach { details ->
            if (demoMode || details == null) return@onEach
            val session = details.session
            startedAt = session.startedAt
            activeCompletionToken = session.completionToken ?: activeCompletionToken
            val exercises = details.sortedExercises.map { exWithSets ->
                val ex = exWithSets.exercise
                val trackingType = try {
                    ExerciseTrackingType.valueOf(ex.trackingType)
                } catch (e: Exception) {
                    ExerciseTrackingType.WEIGHT_REPS
                }
                SessionExercise(
                    dbId = ex.id,
                    name = ex.exerciseName,
                    category = ex.category,
                    targetSets = ex.targetSets,
                    targetReps = ex.targetReps,
                    targetWeightKg = ex.targetWeightKg,
                    trackingType = trackingType,
                    suggestionReason = ex.suggestionReason,
                    supersetId = ex.supersetId,
                    loggedSets = exWithSets.sets.map { s ->
                        SetLogEntity(
                            id = s.id,
                            sessionId = 0,
                            exerciseName = s.exerciseName,
                            category = s.category,
                            weightKg = s.weightKg,
                            reps = s.reps,
                            durationMin = s.durationMin,
                            distanceKm = s.distanceKm,
                            xp = s.xp,
                            rir = s.rir,
                            avgHr = s.avgHr,
                            maxHr = s.maxHr,
                            speedKmh = s.speedKmh,
                            inclinePercent = s.inclinePercent,
                            cardioProgram = s.cardioProgram,
                            setType = s.setType
                        )
                    }
                )
            }
            _uiState.update { s ->
                s.copy(
                    loading = false,
                    title = session.title,
                    exercises = exercises,
                    restEndsAt = session.restEndsAt,
                    restDurationSec = session.restDurationSec,
                    heatStreak = session.heatStreak,
                    lastLogAt = session.lastLogAt,
                    currentExerciseIndex = session.currentExerciseIndex,
                    ambushOfferedThisSession = session.ambushOfferedThisSession,
                    ambushXpMult = session.ambushXpMult,
                    momentSpoilsUsed = session.momentSpoilsUsed,
                    originalWorkoutId = session.workoutId
                )
            }
        }.launchIn(viewModelScope)

        _uiState
            .map { s -> 
                if (s.loading || s.finish != null || demoMode) return@map null
                
                val effects = mutableListOf<String>()
                if (s.hasPrAchievement) effects += "\u26A1" // CRIT
                if (s.heatStreak >= 3) effects += "\uD83E\uDE78" // BLEED
                if ((s.heartRateBpm ?: 0) > s.maxHeartRate * 0.9) effects += "\uD83D\uDD25" // BURN
                
                s.totalXp to effects
            }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { (xp, effects) ->
                container.party.sendActivePulse(xp, effects)
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            while (isActive) {
                if (!demoMode && _uiState.value.finish == null) {
                    _sessionDurationMs.value = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
                }
                delay(1000L)
            }
        }

        container.party.state.onEach { party ->
            _uiState.update { it.copy(partyBoss = party.boss, partyPulses = party.activePulses) }
        }.launchIn(viewModelScope)

        _uiState
            .map { s -> listOf(s.loading, s.exercises, s.restEndsAt) }
            .distinctUntilChanged()
            .onEach { publishWearState() }
            .launchIn(viewModelScope)
    }

    fun toggleMusic() = container.music.togglePlayPause()
    fun skipMusic() = container.music.skipNext()
    fun prevMusic() = container.music.skipPrevious()

    fun setManualDuration(minutes: Int) {
        val newStartedAt = System.currentTimeMillis() - (minutes * 60 * 1000L)
        startedAt = newStartedAt
        _sessionDurationMs.value = (minutes * 60 * 1000L)
        if (!demoMode) {
            viewModelScope.launch {
                container.repository.updateActiveSessionStartTime(newStartedAt)
            }
        }
    }

    fun load(workoutId: Long) {
        if (loadedFor == workoutId) return
        loadedFor = workoutId
        demoMode = false
        viewModelScope.launch {
            val existing = container.repository.getActiveSessionWithDetails()
            if (existing != null) {
                // Defensive check: if existing session doesn't match workoutId, it might be a race condition.
                // We return if workoutId matches or if it's a freestyle session and existing is also freestyle.
                val isFreestyle = workoutId < 0
                val existingIsFreestyle = existing.session.workoutId == null
                
                if ((isFreestyle && existingIsFreestyle) || (existing.session.workoutId == workoutId)) {
                    publishWearState()
                    return@launch
                }
                // Otherwise, the existing session is stale/wrong; we should let the new one overwrite it
                // (Repository.startActiveSession already does a wipe).
            }
            val title: String
            val exercises: List<SessionExercise>
            if (workoutId < 0) {
                title = "Freestyle Session"
                exercises = emptyList()
            } else {
                val workout = container.repository.getWorkout(workoutId)
                val exEntities = container.repository.exercisesFor(workoutId)
                title = workout?.name ?: "Session"
                exercises = exEntities.map { e ->
                    val tracking = resolveExerciseTracking(e.exerciseName, e.category)
                    SessionExercise(
                        name = e.exerciseName,
                        category = tracking.first,
                        targetSets = e.targetSets,
                        targetReps = e.targetReps,
                        targetWeightKg = e.targetWeightKg,
                        trackingType = tracking.second,
                        supersetId = e.supersetId
                    )
                }
            }
            container.repository.startActiveSession(title, if (workoutId > 0) workoutId else null, exercises)
            publishWearState()
        }
    }

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
                isDemo = true
            )
        }
        publishWearState()
    }

    fun addExercise(name: String, category: ExerciseCategory) {
        viewModelScope.launch {
            val resolved = resolveExerciseTracking(name, category)
            if (demoMode) {
                _uiState.update { s ->
                    s.copy(exercises = s.exercises + SessionExercise(name = name, category = resolved.first, trackingType = resolved.second))
                }
            } else {
                container.repository.addActiveExercise(name, resolved.first, resolved.second.name)
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
        val s = _uiState.value
        val ex = s.exercises.getOrNull(index) ?: return
        if (s.exercises.size <= 1) return
        if (demoMode) {
            _uiState.update { state ->
                val updated = state.exercises.filterIndexed { i, _ -> i != index }
                val newIndex = state.currentExerciseIndex.coerceAtMost(updated.size - 1)
                state.copy(exercises = updated, currentExerciseIndex = newIndex)
            }
        } else {
            viewModelScope.launch {
                container.repository.removeActiveExercise(ex.dbId)
            }
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
        val priorEx = prior.exercises.getOrNull(safeIndex) ?: return
        val effectiveWeight = when (priorEx.category) {
            ExerciseCategory.BODYWEIGHT -> if (weightKg <= 0) container.prefs.bodyWeightKg() ?: 0.0 else weightKg
            else -> weightKg
        }

        val now = System.currentTimeMillis()
        val windowMs = (prior.restDurationSec + GameMath.HEAT_GRACE_SEC) * 1000L
        val heatStreak = if (prior.lastLogAt > 0 && now - prior.lastLogAt <= windowMs) prior.heatStreak + 1 else 1
        val multiplier = GameMath.calculateHeatMultiplier(heatStreak) * prior.ambushXpMult
        val log = SetLogEntity(
            sessionId = 0,
            exerciseName = priorEx.name,
            category = priorEx.category,
            weightKg = effectiveWeight,
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
        val setXp = (GameMath.xpForSet(log, isSuperset = priorEx.supersetId != null) * multiplier).toInt()

        if (demoMode) {
            _uiState.update { s ->
                val withXp = log.copy(xp = setXp)
                s.copy(
                    exercises = s.exercises.toMutableList().also {
                        it[safeIndex] = priorEx.copy(loggedSets = priorEx.loggedSets + withXp)
                    },
                    restEndsAt = if (s.restDurationSec > 0) now + s.restDurationSec * 1000L else null,
                    heatStreak = heatStreak,
                    lastLogAt = now,
                    currentExerciseIndex = safeIndex
                )
            }
        } else {
            viewModelScope.launch {
                container.repository.logActiveSet(
                    exerciseId = priorEx.dbId,
                    exerciseName = priorEx.name,
                    category = priorEx.category,
                    weightKg = effectiveWeight,
                    reps = reps,
                    durationMin = durationMin,
                    distanceKm = distanceKm,
                    xp = setXp,
                    rir = rir,
                    avgHr = hrAvg,
                    maxHr = hrMax,
                    speedKmh = speedKmh,
                    inclinePercent = inclinePercent,
                    cardioProgram = cardioProgram.trim(),
                    setType = setType,
                    heatStreak = heatStreak,
                    restDurationSec = prior.restDurationSec
                )
                // Trigger auto-coach after DB write to ensure uiState picks up the new set
                maybeAutoCoach(index)
            }
        }

        if (demoMode) maybeAutoCoach(index)
        
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

    private val historyWeightCache = mutableMapOf<String, Double>()

    suspend fun getPreviousPerformance(exerciseName: String): List<SetLogEntity> {
        if (demoMode) return emptyList()
        val history = container.repository.getPreviousPerformance(exerciseName)
        history.firstOrNull()?.weightKg?.let { w ->
            if (w > 0 && historyWeightCache[exerciseName] != w) {
                historyWeightCache[exerciseName] = w
                publishWearState()
            }
        }
        return history
    }

    fun updateLoggedSet(
        exerciseIndex: Int,
        setIndex: Int,
        updated: SetLogEntity
    ) {
        val s = _uiState.value
        val ex = s.exercises.getOrNull(exerciseIndex) ?: return
        val currentSet = ex.loggedSets.getOrNull(setIndex) ?: return

        val newXp = GameMath.xpForSet(updated)
        val withXp = updated.copy(xp = newXp)

        if (demoMode) {
            _uiState.update { state ->
                val newExercises = state.exercises.toMutableList()
                val newSets = ex.loggedSets.toMutableList()
                newSets[setIndex] = withXp
                newExercises[exerciseIndex] = ex.copy(loggedSets = newSets)
                state.copy(exercises = newExercises)
            }
        } else {
            viewModelScope.launch {
                if (currentSet.id > 0) {
                    container.repository.updateActiveSetLog(
                        ActiveSetLogEntity(
                            id = currentSet.id,
                            activeSessionId = 1L,
                            exerciseId = ex.dbId,
                            exerciseName = ex.name,
                            category = ex.category,
                            weightKg = withXp.weightKg,
                            reps = withXp.reps,
                            durationMin = withXp.durationMin,
                            distanceKm = withXp.distanceKm,
                            xp = withXp.xp,
                            rir = withXp.rir,
                            avgHr = withXp.avgHr,
                            maxHr = withXp.maxHr,
                            speedKmh = withXp.speedKmh,
                            inclinePercent = withXp.inclinePercent,
                            cardioProgram = withXp.cardioProgram,
                            setType = withXp.setType,
                            loggedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        publishWearState()
    }

    fun deleteLoggedSet(
        exerciseIndex: Int,
        setIndex: Int
    ) {
        val s = _uiState.value
        val ex = s.exercises.getOrNull(exerciseIndex) ?: return
        val currentSet = ex.loggedSets.getOrNull(setIndex) ?: return

        if (demoMode) {
            _uiState.update { state ->
                val newExercises = state.exercises.toMutableList()
                val newSets = ex.loggedSets.filterIndexed { i, _ -> i != setIndex }
                newExercises[exerciseIndex] = ex.copy(loggedSets = newSets)
                state.copy(exercises = newExercises)
            }
        } else {
            viewModelScope.launch {
                if (currentSet.id > 0) {
                    container.repository.deleteActiveSetLog(currentSet.id)
                }
            }
        }
        publishWearState()
    }

    fun linkSupersetWithNext(index: Int) {
        val s = _uiState.value
        val ex1 = s.exercises.getOrNull(index) ?: return
        val ex2 = s.exercises.getOrNull(index + 1) ?: return
        val existingGroups = s.exercises.mapNotNull { it.supersetId }.distinct()
        val newGroupId = (1..26).map { "SS$it" }.firstOrNull { it !in existingGroups } ?: "SS1"

        if (demoMode) {
            _uiState.update { state ->
                val updated = state.exercises.toMutableList()
                updated[index] = ex1.copy(supersetId = newGroupId)
                updated[index + 1] = ex2.copy(supersetId = newGroupId)
                state.copy(exercises = updated)
            }
        } else {
            viewModelScope.launch {
                if (ex1.dbId > 0) container.repository.updateActiveExerciseSuperset(ex1.dbId, newGroupId)
                if (ex2.dbId > 0) container.repository.updateActiveExerciseSuperset(ex2.dbId, newGroupId)
            }
        }
        publishWearState()
    }

    fun unlinkSuperset(index: Int) {
        val s = _uiState.value
        val ex = s.exercises.getOrNull(index) ?: return
        if (demoMode) {
            _uiState.update { state ->
                val updated = state.exercises.toMutableList()
                updated[index] = ex.copy(supersetId = null)
                state.copy(exercises = updated)
            }
        } else {
            viewModelScope.launch {
                if (ex.dbId > 0) container.repository.updateActiveExerciseSuperset(ex.dbId, null)
            }
        }
        publishWearState()
    }

    private fun publishWearState() {
        val s = _uiState.value
        val imperial = container.prefs.imperial.value
        val exercises = s.exercises.map { ex ->
            val last = ex.loggedSets.lastOrNull()
            
            // Calculate suggestion for the NEXT set of this exercise
            val suggestion = if (ex.loggedSets.isNotEmpty()) {
                GameMath.suggestNextSet(last?.weightKg ?: 0.0, last?.reps ?: 0, last?.rir ?: 5)
            } else null
            
            val historicalWeight = if (ex.loggedSets.isEmpty()) {
                historyWeightCache[ex.name]
            } else null
            
            val suggestedWeight = suggestion?.weightKg ?: ex.targetWeightKg ?: historicalWeight
            val suggestedReps = suggestion?.reps ?: ex.targetReps
            val reason = ex.suggestionReason ?: suggestion?.note

            WearExerciseState(
                name = ex.name,
                category = ex.category.name,
                targetSets = ex.targetSets,
                targetReps = ex.targetReps,
                loggedSets = ex.loggedSets.size,
                trackingType = ex.trackingType.name,
                lastWeightDisplay = last?.let { Units.toDisplay(it.weightKg, imperial) }
                    ?: historicalWeight?.let { Units.toDisplay(it, imperial) } ?: 0.0,
                lastReps = last?.reps ?: 0,
                suggestedWeightDisplay = suggestedWeight?.takeIf { it > 0 }?.let { Units.toDisplay(it, imperial) },
                suggestedReps = suggestedReps.takeIf { it > 0 },
                suggestionReason = reason,
                supersetId = ex.supersetId
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
                heavy && Random.nextInt(100) < 25 -> MomentTrigger.HEAVY_LIFT
                milestone && Random.nextInt(100) < 15 -> MomentTrigger.MILESTONE
                else -> null
            } ?: return@launch

            val loot = container.repository.grantMomentLoot(trigger)
            if (loot.isEmpty) return@launch
            val labels = loot.labels()
            _uiState.update {
                it.copy(
                    momentSpoil = MomentSpoil(labels = labels, storyPending = it.hasAi, isPr = isPr),
                    momentSpoilsUsed = it.momentSpoilsUsed + 1,
                    hasPrAchievement = it.hasPrAchievement || isPr
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
        if (Random.nextInt(100) >= 15) return
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
            wearBridge.pushFeedback(WearFeedbackKind.AMBUSH, "Ambush! ${monster.name} stalks your rest.")
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

    fun consumeAmbushResultIfNeeded() {
        val result = container.lastAmbushVictory ?: return
        if (_uiState.value.restPausedRemainingMs == null) {
            container.lastAmbushVictory = null
            return
        }
        container.lastAmbushVictory = null
        resumeAfterAmbush(result)
    }

    private fun maybeAutoCoach(index: Int) {
        val s = _uiState.value
        if (s.coachLoading) return
        val ex = s.exercises.getOrNull(index) ?: return
        
        // Auto-coach when an exercise is fully completed (logged all sets)
        if (ex.loggedSets.size < ex.targetSets) return
        
        // Use a unique key to prevent spamming the same exercise completion coaching
        val coachingKey = "completed_${ex.name}_${ex.loggedSets.size}"
        if (!coachedExercises.add(coachingKey)) return
        
        askCoach()
    }

    fun askCoach(manual: Boolean = false, simulateHighPerf: Boolean = false) {
        val s = _uiState.value
        if (s.coachLoading) return
        if (!manual && s.totalSets == 0) return
        
        _uiState.update { it.copy(coachLoading = true, coach = null, coachError = null) }

        viewModelScope.launch {
            try {
                // If Gemini is not fully set up (no key and no local model), skip directly to Squire.
                // If there's a local model but no key, container.gemini.isAvailable is true, but cloud will fail and it will retry local.
                if (!container.gemini.isAvailable) {
                    val advice = LocalAiEngine.coachHeuristic(s.exercises, container.prefs.imperial.value)
                    _uiState.update { it.copy(coachLoading = false, coach = advice) }
                    return@launch
                }

                val performed = if (simulateHighPerf) {
                    s.exercises.firstOrNull()?.let { ex ->
                        "- ${ex.name}: ${ex.targetWeightKg ?: 100.0}kg x ${ex.targetReps + 5} @0 RIR (Exceeded target!)"
                    } ?: "Squat: 100kg x 20 @0 RIR"
                } else {
                    s.exercises.filter { it.loggedSets.isNotEmpty() }.joinToString("\n") { ex ->
                        val sets = ex.loggedSets.joinToString(", ") { set ->
                            val effort = set.rir?.let { " @$it RIR" }.orEmpty()
                            "${set.weightKg}kg x ${set.reps}$effort"
                        }
                        "- ${ex.name}: $sets"
                    }
                }
                
                val remaining = s.exercises.filter { it.loggedSets.isEmpty() }
                    .joinToString("\n") { "- ${it.name} (${it.targetSets}x${it.targetReps})" }
                    .ifBlank { "(none)" }
                
                val hrContext = s.heartRateBpm?.let { "\nHR: $it bpm" }.orEmpty()
                val history = container.repository.recentWorkoutsSummary(limit = 5)

                val contextPrefix = if (simulateHighPerf) "CRITICAL: The player just SMASHED their targets. You MUST increase intensity significantly for remaining exercises.\n" else ""

                container.gemini.coachSession(performed, remaining, container.prefs.profile.value, contextPrefix + history + hrContext, container.prefs.imperial.value)
                    .onSuccess { advice ->
                        val valid = advice.changes.filter { change ->
                            _uiState.value.exercises.any { it.loggedSets.isEmpty() && it.name.equals(change.exercise, true) }
                        }
                        _uiState.update { it.copy(coachLoading = false, coach = advice.copy(changes = valid)) }
                    }
                    .onFailure { e ->
                        // Fallback to Squire if cloud AI (or local retry) is unavailable or failing
                        val advice = LocalAiEngine.coachHeuristic(s.exercises, container.prefs.imperial.value)
                        _uiState.update { it.copy(coachLoading = false, coach = advice) }
                    }
            } catch (e: Exception) {
                // Last ditch fallback
                val advice = LocalAiEngine.coachHeuristic(s.exercises, container.prefs.imperial.value)
                _uiState.update { it.copy(coachLoading = false, coach = advice) }
            }
        }
    }

    fun dismissCoachError() = _uiState.update { it.copy(coachError = null) }
    fun dismissFinishError() = _uiState.update { it.copy(finishError = null) }

    fun applyCoachChanges() {
        val advice = _uiState.value.coach ?: return
        viewModelScope.launch {
            var list = _uiState.value.exercises
            for (change in advice.changes) {
                val idx = list.indexOfFirst { it.loggedSets.isEmpty() && it.name.equals(change.exercise, true) }
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
                            targetWeightKg = change.weightKg ?: ex.targetWeightKg,
                            trackingType = tracking.second,
                            suggestionReason = change.reason
                        )
                    }
                    if (!demoMode) {
                        container.repository.updateActiveExerciseTargets(
                            exerciseId = ex.dbId,
                            sets = change.sets ?: ex.targetSets,
                            reps = change.reps ?: ex.targetReps,
                            weightKg = change.weightKg ?: ex.targetWeightKg,
                            reason = change.reason
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
        val s = _uiState.value
        val target = index + delta
        if (target !in s.exercises.indices) return
        val reordered = s.exercises.toMutableList().also { list ->
            val tmp = list[index]
            list[index] = list[target]
            list[target] = tmp
        }
        if (demoMode) {
            _uiState.update { it.copy(exercises = reordered) }
        } else {
            viewModelScope.launch {
                container.repository.reorderActiveExercises(reordered.map { it.dbId })
            }
        }
    }

    fun swapExercise(index: Int, name: String, category: ExerciseCategory, permanent: Boolean = false) {
        viewModelScope.launch {
            val resolved = resolveExerciseTracking(name, category)
            val ex = _uiState.value.exercises.getOrNull(index) ?: return@launch
            val workoutId = if (permanent) _uiState.value.originalWorkoutId else null
            if (demoMode) {
                _uiState.update { s ->
                    s.copy(exercises = s.exercises.toMutableList().also {
                        it[index] = ex.copy(name = name, category = resolved.first, trackingType = resolved.second)
                    })
                }
            } else {
                container.repository.swapActiveExercise(ex.dbId, name, resolved.first, resolved.second.name, workoutId)
            }
            publishWearState()
        }
    }

    fun aiSwap(index: Int, permanent: Boolean = false) {
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
                val workoutId = if (permanent) _uiState.value.originalWorkoutId else null
                if (demoMode) {
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
                } else {
                    container.repository.swapActiveExercise(ex.dbId, swap.name, tracking.first, tracking.second.name, workoutId)
                    _uiState.update { it.copy(aiSwapIndex = null, swapNote = "Swapped ${ex.name} \u2192 ${swap.name}. ${swap.reason}") }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(aiSwapIndex = null, swapNote = "Swap failed: ${e.message}") }
            }
        }
    }

    fun skipRest() {
        val s = _uiState.value
        if (demoMode) {
            _uiState.update { it.copy(restEndsAt = null) }
        } else {
            viewModelScope.launch { container.repository.updateActiveRestTimer(null, s.restDurationSec) }
        }
        publishWearState()
    }

    fun extendRest(seconds: Int) {
        val now = System.currentTimeMillis()
        val s = _uiState.value
        val baseTime = s.restEndsAt?.takeIf { it > now } ?: now
        val newEndsAt = baseTime + seconds * 1000L
        if (demoMode) {
            _uiState.update { it.copy(restEndsAt = newEndsAt) }
        } else {
            viewModelScope.launch { container.repository.updateActiveRestTimer(newEndsAt, s.restDurationSec) }
        }
        publishWearState()
    }

    fun setRestDuration(seconds: Int) {
        val s = _uiState.value
        if (demoMode) {
            _uiState.update { it.copy(restDurationSec = seconds) }
        } else {
            viewModelScope.launch { container.repository.updateActiveRestTimer(s.restEndsAt, seconds) }
        }
        publishWearState()
    }

    fun removeLastSet(index: Int) {
        val ex = _uiState.value.exercises.getOrNull(index) ?: return
        if (ex.loggedSets.isEmpty()) return
        if (demoMode) {
            _uiState.update { s ->
                s.copy(exercises = s.exercises.toMutableList().also { it[index] = ex.copy(loggedSets = ex.loggedSets.dropLast(1)) })
            }
        } else {
            viewModelScope.launch { container.repository.removeLastActiveSet(ex.dbId) }
        }
        publishWearState()
    }

    suspend fun abandon() {
        if (!demoMode) {
            container.repository.discardActiveSession()
            container.party.clearActivePulse()
        }
        wearBridge.unbind()
        workoutNotification.cancel()
    }

    fun finish() {
        if (isFinishing) return
        val state = _uiState.value
        val logs = state.exercises.flatMap { it.loggedSets }
        if (logs.isEmpty()) {
            _uiState.update { it.copy(finishError = "Log at least one set before finishing the quest.") }
            return
        }
        isFinishing = true
        _uiState.update { it.copy(finishError = null) }
        viewModelScope.launch {
            try {
                if (demoMode) {
                    val character = container.repository.getCharacter()
                    val withXp = logs.map { it.copy(xp = if (it.xp > 0) it.xp else GameMath.xpForSet(it)) }
                    val preview = GameMath.applySession(character, withXp, System.currentTimeMillis() - startedAt, emptySet(), 1, 3)
                    _uiState.update { it.copy(finish = SessionFinish(result = preview.copy(updatedCharacter = character), isDemo = true)) }
                    wearBridge.unbind()
                    workoutNotification.cancel()
                    return@launch
                }

                val strMult = if (container.prefs.consumeEncounterStrBoost()) 1.15f else 1f
                val token = activeCompletionToken ?: "session_${startedAt}_${logs.size}"
                val result = container.repository.completeSession(
                    name = state.title,
                    startedAt = startedAt,
                    logs = logs,
                    strengthXpMultiplier = strMult,
                    completionToken = token,
                    userId = container.auth.currentUid(),
                    caloriesKcal = state.wearCaloriesKcal?.toInt(),
                    avgHr = state.heartRateBpm, // Better than nothing if set-level HR missing
                    maxHr = state.maxHeartRate, // Placeholder or from peak HR tracking if we had it
                    steps = state.wearSteps,
                    distanceMeters = state.wearDistanceMeters,
                    activeDurationMs = state.wearActiveDurationMs
                )
                container.party.clearActivePulse()
                OutboxWorker.enqueue(container.app)

                val summaryItems = state.exercises.map { ex ->
                    val exPrs = result.prs.filter { it.exerciseName == ex.name }
                    val info = container.exerciseInfo.find(ex.name)
                    WorkoutSummaryItem(
                        name = ex.name,
                        isIncreasedWeight = exPrs.any { it.kind == PrKind.WEIGHT },
                        isIncreasedVolume = exPrs.any { it.kind == PrKind.VOLUME },
                        isIncreased1RM = exPrs.any { it.kind == PrKind.ONE_RM },
                        prs = exPrs.filter { it.isNew }.map { it.kind }.toSet(),
                        iconUrl = info?.imageUrls?.firstOrNull(),
                        muscles = info?.primaryMuscles?.toSet() ?: emptySet()
                    )
                }

                val wantPraise = container.gemini.isAvailable
                _uiState.update {
                    it.copy(finish = SessionFinish(result, praisePending = wantPraise, caloriesKcal = state.wearCaloriesKcal?.toInt(), activeDurationMs = state.wearActiveDurationMs, zoneWorkSec = state.wearZoneWorkSec, zoneHighSec = state.wearZoneHighSec, steps = state.wearSteps, distanceMeters = state.wearDistanceMeters, summaryItems = summaryItems))
                }
                wearBridge.unbind()
                workoutNotification.cancel()

                if (wantPraise) {
                    val summary = state.exercises.filter { it.loggedSets.isNotEmpty() }.joinToString(", ") { "${it.name} (${it.loggedSets.size} sets)" }
                    val praise = container.gemini.sessionPraise(summary, result.updatedCharacter.name).getOrNull()
                    _uiState.update { s -> s.finish?.let { f -> s.copy(finish = f.copy(praise = praise, praisePending = false)) } ?: s }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(finishError = error.message?.takeIf { msg -> msg.isNotBlank() } ?: "Workout couldn't be saved.") }
            } finally {
                isFinishing = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (!demoMode) {
            viewModelScope.launch { container.party.clearActivePulse() }
        }
        wearBridge.unbind()
        workoutNotification.cancel()
        container.music.stop()
    }

    companion object {
        val Factory = viewModelFactory { initializer { ActiveSessionViewModel(appContainer) } }
    }
}

@Composable
fun ActiveSessionScreen(
    workoutId: Long,
    onDone: () -> Unit,
    onAmbushFight: (monsterId: Int) -> Unit = {},
    onFinished: (() -> Unit)? = null,
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
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.consumeAmbushResultIfNeeded()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val state by viewModel.uiState.collectAsState()
    val imperial by viewModel.imperial.collectAsState()
    val effortMethod by viewModel.effortMethod.collectAsState()
    val sessionDurationMs by viewModel.sessionDurationMs.collectAsState()
    val customCardioPrograms by viewModel.customCardioPrograms.collectAsState()

    ActiveSessionScreenContent(
        state = state,
        imperial = imperial,
        effortMethod = effortMethod,
        sessionDurationMs = sessionDurationMs,
        customCardioPrograms = customCardioPrograms,
        onDone = onDone,
        onAmbushFight = onAmbushFight,
        onFinished = onFinished ?: onDone,
        actions = ActiveSessionActions(
            onToggleMusic = viewModel::toggleMusic,
            onSkipMusic = viewModel::skipMusic,
            onPrevMusic = viewModel::prevMusic,
            onSetManualDuration = viewModel::setManualDuration,
            onAddExercise = viewModel::addExercise,
            onRemoveExercise = viewModel::removeExercise,
            onMoveExercise = viewModel::moveExercise,
            onSwapExercise = viewModel::swapExercise,
            onAiSwap = viewModel::aiSwap,
            onLogSet = viewModel::logSet,
            onRemoveLastSet = viewModel::removeLastSet,
            onSkipRest = viewModel::skipRest,
            onExtendRest = viewModel::extendRest,
            onSetRestDuration = viewModel::setRestDuration,
            onAbandon = viewModel::abandon,
            onFinish = viewModel::finish,
            onAskCoach = viewModel::askCoach,
            onApplyCoachChanges = viewModel::applyCoachChanges,
            onDismissCoach = viewModel::dismissCoach,
            onDismissSwapNote = viewModel::dismissSwapNote,
            onDismissWearBanner = viewModel::dismissWearBanner,
            onDismissFinishError = viewModel::dismissFinishError,
            onDismissCoachError = viewModel::dismissCoachError,
            onDismissMomentSpoil = viewModel::dismissMomentSpoil,
            onDeclineAmbush = viewModel::declineAmbush,
            onAcceptAmbush = viewModel::acceptAmbush,
            onAddCustomCardioProgram = viewModel::addCustomCardioProgram,
            onUpdateLoggedSet = viewModel::updateLoggedSet,
            onDeleteLoggedSet = viewModel::deleteLoggedSet,
            onLinkSuperset = viewModel::linkSupersetWithNext,
            onUnlinkSuperset = viewModel::unlinkSuperset,
            getPreviousPerformance = viewModel::getPreviousPerformance,
            bodyWeightKgOrNull = viewModel::bodyWeightKgOrNull
        )
    )
}

data class ActiveSessionActions(
    val onToggleMusic: () -> Unit = {},
    val onSkipMusic: () -> Unit = {},
    val onPrevMusic: () -> Unit = {},
    val onSetManualDuration: (Int) -> Unit = {},
    val onAddExercise: (String, ExerciseCategory) -> Unit = { _, _ -> },
    val onRemoveExercise: (Int) -> Unit = {},
    val onMoveExercise: (Int, Int) -> Unit = { _, _ -> },
    val onSwapExercise: (Int, String, ExerciseCategory, Boolean) -> Unit = { _, _, _, _ -> },
    val onAiSwap: (Int, Boolean) -> Unit = { _, _ -> },
    val onLogSet: (Int, Double, Int, Double, Double, Int?, Int?, Int?, Double, Double, String, SetType, Boolean) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    val onRemoveLastSet: (Int) -> Unit = {},
    val onUpdateLoggedSet: (Int, Int, SetLogEntity) -> Unit = { _, _, _ -> },
    val onDeleteLoggedSet: (Int, Int) -> Unit = { _, _ -> },
    val onLinkSuperset: (Int) -> Unit = {},
    val onUnlinkSuperset: (Int) -> Unit = {},
    val onSkipRest: () -> Unit = {},
    val onExtendRest: (Int) -> Unit = {},
    val onSetRestDuration: (Int) -> Unit = {},
    val onAbandon: suspend () -> Unit = {},
    val onFinish: () -> Unit = {},
    val onAskCoach: (Boolean, Boolean) -> Unit = { _, _ -> },
    val onApplyCoachChanges: () -> Unit = {},
    val onDismissCoach: () -> Unit = {},
    val onDismissSwapNote: () -> Unit = {},
    val onDismissWearBanner: () -> Unit = {},
    val onDismissFinishError: () -> Unit = {},
    val onDismissCoachError: () -> Unit = {},
    val onDismissMomentSpoil: () -> Unit = {},
    val onDeclineAmbush: () -> Unit = {},
    val onAcceptAmbush: ((Int) -> Unit) -> Unit = {},
    val onAddCustomCardioProgram: (String) -> Unit = {},
    val getPreviousPerformance: suspend (String) -> List<SetLogEntity> = { emptyList() },
    val bodyWeightKgOrNull: () -> Double? = { null }
)

@Composable
fun ActiveSessionScreenContent(
    state: ActiveSessionUiState,
    imperial: Boolean,
    effortMethod: EffortMethod,
    sessionDurationMs: Long,
    customCardioPrograms: Set<String>,
    onDone: () -> Unit,
    onAmbushFight: (Int) -> Unit,
    onFinished: () -> Unit,
    actions: ActiveSessionActions
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showPicker by remember { mutableStateOf(false) }
    var swapFor by remember { mutableStateOf<Int?>(null) }
    var showPlates by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var expandedIndex by remember { mutableIntStateOf(-1) }

    // Sync expandedIndex with currentExerciseIndex when it changes
    LaunchedEffect(state.currentExerciseIndex) {
        expandedIndex = state.currentExerciseIndex
    }

    data class PendingSwap(val index: Int, val name: String, val category: ExerciseCategory, val isAi: Boolean = false)
    var pendingSwap by remember { mutableStateOf<PendingSwap?>(null) }

    if (pendingSwap != null && state.originalWorkoutId != null && state.originalWorkoutId > 0) {
        AlertDialog(
            onDismissRequest = { pendingSwap = null },
            title = { Text("Update Routine?") },
            text = { Text("Would you like to make this exercise swap permanent in your routine template, or just for this session?") },
            confirmButton = {
                TextButton(onClick = {
                    val p = pendingSwap!!
                    if (p.isAi) actions.onAiSwap(p.index, true) else actions.onSwapExercise(p.index, p.name, p.category, true)
                    pendingSwap = null
                }) { Text("Make Permanent") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val p = pendingSwap!!
                    if (p.isAi) actions.onAiSwap(p.index, false) else actions.onSwapExercise(p.index, p.name, p.category, false)
                    pendingSwap = null
                }) { Text("Session Only") }
            }
        )
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard Quest?") },
            text = { Text("This will abandon your current progress. All sets logged in this session will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            actions.onAbandon()
                            showDiscardConfirm = false
                            onDone()
                        }
                    }
                ) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text("Cancel") }
            }
        )
    }

    state.coach?.let { coach ->
        AlertDialog(
            onDismissRequest = actions.onDismissCoach,
            title = { Text("\uD83E\uDDD9 Coach's Counsel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(coach.message, style = MaterialTheme.typography.bodyMedium)
                    coach.changes.forEach { change ->
                        val action = if (change.replaceWith != null) {
                            "${change.exercise} \u2192 ${change.replaceWith}"
                        } else {
                            val weightPart = if (change.weightKg != null) {
                                " @ ${Units.toDisplay(change.weightKg, imperial).let(HeightFormat::trimNum)} ${Units.label(imperial)}"
                            } else ""
                            "${change.exercise}: ${change.sets ?: "?"} \u00D7 ${change.reps ?: "?"}$weightPart"
                        }
                        Column {
                            Text(action, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            if (change.reason.isNotBlank()) Text(change.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                if (coach.changes.isNotEmpty()) {
                    Button(onClick = actions.onApplyCoachChanges) { Text("Apply Changes") }
                } else {
                    Button(onClick = actions.onDismissCoach) { Text("Got it") }
                }
            },
            dismissButton = {
                if (coach.changes.isNotEmpty()) {
                    TextButton(onClick = actions.onDismissCoach) { Text("Keep Plan") }
                }
            }
        )
    }

    LaunchedEffect(state.currentExerciseIndex) {
        if (state.exercises.isNotEmpty()) {
            listState.animateScrollToItem(state.currentExerciseIndex + 2)
        }
    }

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

    Column(Modifier.fillMaxSize().imePadding()) {
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
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = { showPlates = true }) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = "Plates")
                    }
                    IconButton(onClick = { showDiscardConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Discard", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    }
                    Button(
                        onClick = { actions.onFinish() },
                        enabled = state.totalSets > 0,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(if (state.isDemo) "Done" else "Finish")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusColor = if (state.watchLinked) Color(0xFF4CAF50) else Color.Gray
                    Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                    Text(
                        text = if (state.watchLinked) "Watch: Live" else "Watch: Off",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    if (state.heartRateBpm != null) {
                        val zoneColor = when (state.hrZoneLabel?.lowercase()) {
                            "warmup" -> Color(0xFF7BB4E3)
                            "easy" -> Color(0xFF4CAF50)
                            "work" -> Color(0xFFFBC02D)
                            "high" -> Color(0xFFF57C00)
                            else -> Color(0xFFD32F2F)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(zoneColor))
                            Text("${state.heartRateBpm}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("bpm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val kcal = state.wearCaloriesKcal ?: (state.totalXp.toDouble() * 0.4).takeIf { it > 0 }
                    if (kcal != null) {
                        Spacer(Modifier.width(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(12.dp))
                            Text("${kcal.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (state.media.packageName != null) {
                    MediaControllerCard(
                        state = state.media,
                        onToggle = actions.onToggleMusic,
                        onNext = actions.onSkipMusic,
                        onPrev = actions.onPrevMusic
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    var showDurationEdit by remember { mutableStateOf(false) }
                    if (showDurationEdit) {
                        DurationEditDialog(
                            initialMinutes = (sessionDurationMs / 60000L).toInt(),
                            onDismiss = { showDurationEdit = false },
                            onConfirm = { mins ->
                                actions.onSetManualDuration(mins)
                                showDurationEdit = false
                            }
                        )
                    }

                    MetricItem(
                        label = "Time", 
                        value = formatDuration(sessionDurationMs),
                        modifier = Modifier.clickable { showDurationEdit = true }
                    )
                    MetricItem("Volume", "${Units.toDisplay(state.totalVolumeKg, imperial).toInt()} ${Units.label(imperial)}")
                    MetricItem("Sets", state.totalSets.toString())
                    MetricItem("XP", "+${state.totalXp}")
                }

                state.partyBoss?.let { boss ->
                    val activeDamage = state.partyPulses.sumOf { it.xp }.toLong()
                    val ghostHp = (boss.hp - activeDamage).coerceAtLeast(0L)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        GhostBarMeter(
                            label = "${boss.emoji} Party Raid: ${boss.name}",
                            valueText = if (activeDamage > 0) "${ghostHp} / ${boss.hp} HP" else "${boss.hp} / ${boss.maxHp}",
                            actualProgress = (boss.hp.toFloat() / boss.maxHp).coerceIn(0f, 1f),
                            ghostProgress = (ghostHp.toFloat() / boss.maxHp).coerceIn(0f, 1f),
                            color = MaterialTheme.colorScheme.error,
                            ghostColor = Color.White.copy(alpha = 0.25f)
                        )
                        if (state.partyPulses.isNotEmpty()) {
                            val pulseEffects = state.partyPulses.flatMap { it.effects }.distinct().joinToString(" ")
                            Text(
                                text = buildString {
                                    append("\u2694\uFE0F ${state.partyPulses.size} hero${if (state.partyPulses.size > 1) "es" else ""} striking!")
                                    if (pulseEffects.isNotEmpty()) append(" Status: $pulseEffects")
                                    append(" Pending: $activeDamage XP")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.wearBanner?.let { banner ->
                    item {
                        SectionCard {
                            Text(banner, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                            TextButton(onClick = actions.onDismissWearBanner) { Text("Got it") }
                        }
                    }
                }

                if (state.coachLoading) {
                    item {
                        SectionCard {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("\uD83E\uDDD9 Coaching...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                state.coachError?.let { error ->
                    item {
                        SectionCard {
                            Text("\uD83E\uDDD9 $error", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { actions.onAskCoach(false, false) }) { Text("Try again") }
                                TextButton(onClick = actions.onDismissCoachError) { Text("Dismiss") }
                            }
                        }
                    }
                }

                state.finishError?.let { error ->
                    item {
                        SectionCard {
                            Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = actions.onFinish) { Text("Try again") }
                                TextButton(onClick = actions.onDismissFinishError) { Text("Dismiss") }
                            }
                        }
                    }
                }

                state.swapNote?.let { note ->
                    item {
                        SectionCard {
                            Text("\u2728 $note", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                            TextButton(onClick = actions.onDismissSwapNote) { Text("Got it") }
                        }
                    }
                }

                itemsIndexed(state.exercises, key = { _, ex -> ex.name }) { index, exercise ->
                    val canLink = index < state.exercises.lastIndex && exercise.supersetId == null && state.exercises[index + 1].supersetId == null
                    ExerciseLogCard(
                        exercise = exercise,
                        imperial = imperial,
                        effortMethod = effortMethod,
                        bodyWeightKg = actions.bodyWeightKgOrNull(),
                        showCardioIntensity = false,
                        onToggleCardioIntensity = {},
                        aiSwapping = state.aiSwapIndex == index,
                        minimized = index != expandedIndex,
                        onHeaderClick = {
                            expandedIndex = if (expandedIndex == index) -1 else index
                        },
                        canMoveUp = index > 0,
                        canMoveDown = index < state.exercises.lastIndex,
                        canRemove = state.exercises.size > 1,
                        canLinkSuperset = canLink,
                        onLinkSuperset = { actions.onLinkSuperset(index) },
                        onUnlinkSuperset = { actions.onUnlinkSuperset(index) },
                        onMove = { d -> actions.onMoveExercise(index, d) },
                        onSwap = { swapFor = index },
                        onRemove = { actions.onRemoveExercise(index) },
                        onLogSet = { w, r, dur, dist, rir, speed, incline, program, st ->
                            actions.onLogSet(index, w, r, dur, dist, rir, null, null, speed, incline, program, st, false)
                            // Auto-focus next exercise in the superset
                            val groupId = exercise.supersetId
                            if (groupId != null) {
                                val matchIndices = state.exercises.mapIndexedNotNull { i, e -> if (e.supersetId == groupId) i else null }
                                if (matchIndices.size > 1) {
                                    val currPos = matchIndices.indexOf(index)
                                    val nextPos = (currPos + 1) % matchIndices.size
                                    expandedIndex = matchIndices[nextPos]
                                }
                            }
                        },
                        onUndo = { actions.onRemoveLastSet(index) },
                        onUpdateSet = { sIdx, updated -> actions.onUpdateLoggedSet(index, sIdx, updated) },
                        onDeleteSet = { sIdx -> actions.onDeleteLoggedSet(index, sIdx) },
                        getPreviousPerformance = actions.getPreviousPerformance,
                        customPrograms = customCardioPrograms,
                        onAddCustomProgram = actions.onAddCustomCardioProgram
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
            }

            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                FloatingActionButton(
                    onClick = { actions.onAskCoach(true, false) },
                    containerColor = if (state.coachLoading) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (state.coachLoading) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.combinedClickable(
                        enabled = !state.coachLoading,
                        onClick = { actions.onAskCoach(true, false) },
                        onLongClick = { actions.onAskCoach(true, true) }
                    )
                ) {
                    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (state.coachLoading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = if (state.hasAi) {
                                if (state.coachLoading) "Consulting..." else "\uD83E\uDDD9 Coach"
                            } else {
                                if (state.coachLoading) "Scouting..." else "\uD83D\uDDE1\uFE0F Squire"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
        state.restEndsAt?.let { endsAt ->
            RestTimerBar(
                endsAt = endsAt,
                onExtend = { actions.onExtendRest(15) },
                onSkip = actions.onSkipRest,
                onFinish = actions.onSkipRest
            )
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            onDismiss = { showPicker = false },
            onPick = { n, c -> actions.onAddExercise(n, c) }
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
                                if (state.originalWorkoutId != null && state.originalWorkoutId > 0) {
                                    pendingSwap = PendingSwap(index, "", exercise.category, true)
                                } else {
                                    actions.onAiSwap(index, false)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("\u2728 Let the AI pick")
                        }
                    }
                } else {
                    null
                },
                onDismiss = { swapFor = null },
                onPick = { n, c ->
                    swapFor = null
                    if (state.originalWorkoutId != null && state.originalWorkoutId > 0) {
                        pendingSwap = PendingSwap(index, n, c, false)
                    } else {
                        actions.onSwapExercise(index, n, c, false)
                    }
                }
            )
        }
    }

    state.momentSpoil?.let { spoil ->
        AlertDialog(
            onDismissRequest = actions.onDismissMomentSpoil,
            title = { Text(if (spoil.isPr) "\uD83C\uDFC6 Record!" else "\uD83C\uDF81 Spoils!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    spoil.labels.forEach { Text("\u2022 $it", fontWeight = FontWeight.SemiBold) }
                    if (spoil.storyPending) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    } else {
                        spoil.story?.let { Text("\u201C$it\u201D") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = actions.onDismissMomentSpoil) { Text("Claim") }
            }
        )
    }

    state.ambushOffer?.let { offer ->
        AlertDialog(
            onDismissRequest = actions.onDeclineAmbush,
            title = { Text("${offer.monsterEmoji} Ambush!") },
            text = { Text("A ${offer.monsterName} stalks your rest.") },
            confirmButton = {
                TextButton(onClick = { actions.onAcceptAmbush(onAmbushFight) }) { Text("Fight") }
            },
            dismissButton = {
                TextButton(onClick = actions.onDeclineAmbush) { Text("Keep resting") }
            }
        )
    }

    state.finish?.let { finish ->
        var showLevelUp by remember(finish) { mutableStateOf(finish.result.levelsGained > 0) }
        if (showLevelUp) {
            val newLevel = finish.result.updatedCharacter.level
            val cls = finish.result.updatedCharacter.characterClass ?: CharacterClass.WARRIOR
            LevelUpModal(
                newLevel = newLevel,
                clazz = cls,
                statGains = finish.result.statGains,
                unlockedSkills = cls.skills.filter { it.unlockLevel in (newLevel - finish.result.levelsGained + 1)..newLevel },
                onDismiss = { showLevelUp = false }
            )
        } else {
            SessionResultDialog(finish = finish, imperial = imperial, onDismiss = { onFinished() })
        }
    }
}

@Composable
private fun DurationEditDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var minutes by remember { mutableStateOf(initialMinutes.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Duration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Set workout duration in minutes:")
                CompactNumberField(
                    value = minutes,
                    onValueChange = { minutes = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    minutes.toIntOrNull()?.let { onConfirm(it) }
                }
            ) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun MetricItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RestTimerBar(endsAt: Long, onExtend: () -> Unit, onSkip: () -> Unit, onFinish: () -> Unit) {
    var now by remember(endsAt) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAt) { while (System.currentTimeMillis() < endsAt) { now = System.currentTimeMillis(); delay(200.milliseconds) }; now = System.currentTimeMillis(); AudioEffects.playRestDone(); onFinish() }
    val rem = ((endsAt - now) / 1000L).toInt().coerceAtLeast(0)
    Surface(color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Filled.Timer, null)
            Text(text = if (rem > 0) "Rest: %d:%02d".format(rem / 60, rem % 60) else "Ready!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onExtend) { Text("+15s", fontWeight = FontWeight.Bold) }
            Button(onClick = onSkip, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer, contentColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 16.dp)) { Text("Skip") }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

@Composable
fun EditSetDialog(
    set: SetLogEntity,
    exercise: SessionExercise,
    imperial: Boolean,
    effortMethod: EffortMethod,
    customPrograms: Set<String> = emptySet(),
    onAddCustomProgram: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (SetLogEntity) -> Unit,
    onDelete: () -> Unit
) {
    val isTimed = exercise.trackingType in setOf(ExerciseTrackingType.TIME_ONLY, ExerciseTrackingType.DISTANCE_TIME, ExerciseTrackingType.CARDIO_MACHINE)
    val isCardio = exercise.trackingType == ExerciseTrackingType.CARDIO_MACHINE || exercise.trackingType == ExerciseTrackingType.DISTANCE_TIME

    val initialWeightDisplay = if (set.weightKg > 0) Units.toDisplay(set.weightKg, imperial).let(HeightFormat::trimNum) else ""
    var weight by remember { mutableStateOf(initialWeightDisplay) }
    var reps by remember { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }
    
    val totalSecs = (set.durationMin * 60.0).roundToInt()
    var durationMin by remember { mutableStateOf(if (set.durationMin > 0) (totalSecs / 60).toString() else "") }
    var durationSec by remember { mutableStateOf(if (totalSecs % 60 > 0) (totalSecs % 60).toString() else "") }
    
    val initialDist = if (set.distanceKm > 0) Units.trimmed(Units.kmToDisplay(set.distanceKm, imperial)) else ""
    var distance by remember { mutableStateOf(initialDist) }
    var speed by remember { mutableStateOf(if (set.speedKmh > 0) Units.trimmed(Units.speedToDisplay(set.speedKmh, imperial)) else "") }
    var incline by remember { mutableStateOf(if (set.inclinePercent > 0) set.inclinePercent.toString() else "") }
    var program by remember { mutableStateOf(set.cardioProgram) }
    var effort by remember { mutableStateOf(set.rir) }
    var setType by remember { mutableStateOf(set.setType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Set • ${exercise.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Set Type Selector Dropdown
                DropdownField(
                    label = "Type",
                    value = "${setType.shortLabel} (${setType.label})",
                    modifier = Modifier.fillMaxWidth()
                ) { onDismiss ->
                    SetType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(type.shortLabel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(type.label)
                                }
                            },
                            onClick = {
                                setType = type
                                onDismiss()
                            }
                        )
                    }
                }

                if (isCardio) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = distance,
                            onValueChange = { distance = it },
                            label = { Text("Dist (${Units.distLabel(imperial)})") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = durationMin,
                            onValueChange = { durationMin = it },
                            label = { Text("Min") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = durationSec,
                            onValueChange = { durationSec = it },
                            label = { Text("Sec") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        DropdownField(
                            label = "Incline %",
                            value = if (incline.isNotBlank()) "$incline%" else "0%",
                            modifier = Modifier.weight(1f)
                        ) { onDismiss ->
                            listOf("0", "0.5", "1.0", "1.5", "2.0", "3.0", "4.0", "5.0", "6.0", "7.5", "10.0", "12.0", "15.0").forEach { v ->
                                DropdownMenuItem(
                                    text = { Text("$v%") },
                                    onClick = {
                                        incline = v
                                        onDismiss()
                                    }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = speed,
                            onValueChange = { speed = it },
                            label = { Text("Speed") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    var showAddProgramInEdit by remember { mutableStateOf(false) }
                    if (showAddProgramInEdit) {
                        var newProgName by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { showAddProgramInEdit = false },
                            title = { Text("Add Custom Program") },
                            text = {
                                OutlinedTextField(
                                    value = newProgName,
                                    onValueChange = { newProgName = it },
                                    label = { Text("Program Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    if (newProgName.isNotBlank()) {
                                        onAddCustomProgram(newProgName)
                                        program = newProgName
                                    }
                                    showAddProgramInEdit = false
                                }) { Text("Add") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddProgramInEdit = false }) { Text("Cancel") }
                            }
                        )
                    }
                    DropdownField(
                        label = "Program",
                        value = program.ifEmpty { "Manual" },
                        modifier = Modifier.fillMaxWidth()
                    ) { onDismiss ->
                        val allPrograms = (listOf("Manual") + customPrograms.toList()).distinct()
                        allPrograms.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = {
                                    program = if (p == "Manual") "" else p
                                    onDismiss()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("+ Add Custom...") },
                            onClick = {
                                showAddProgramInEdit = true
                                onDismiss()
                            }
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = if (isTimed) durationMin else weight,
                            onValueChange = { if (isTimed) durationMin = it else weight = it },
                            label = { Text(if (isTimed) "Min" else "Weight (${Units.label(imperial)})") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = if (isTimed) durationSec else reps,
                            onValueChange = { if (isTimed) durationSec = it else reps = it },
                            label = { Text(if (isTimed) "Sec" else "Reps") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    // Effort (RIR / RPE) Dropdown
                    if (effortMethod != EffortMethod.OFF) {
                        DropdownField(
                            label = effortMethod.label,
                            value = effort?.let { effortMethod.display(it) } ?: "—",
                            modifier = Modifier.fillMaxWidth()
                        ) { onDismiss ->
                            val ops = if (effortMethod == EffortMethod.RPE) {
                                listOf(null to "—", 4 to "6", 3 to "7", 2 to "8", 1 to "9", 0 to "10")
                            } else {
                                listOf(null to "—", 0 to "0", 1 to "1", 2 to "2", 3 to "3", 4 to "4", 5 to "5+")
                            }
                            ops.forEach { (v, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        effort = v
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val totalMins = (durationMin.toDoubleOrNull() ?: 0.0) + (durationSec.toDoubleOrNull() ?: 0.0) / 60.0
                    val updated = set.copy(
                        weightKg = if (isTimed) 0.0 else Units.toKg(weight.toDoubleOrNull() ?: 0.0, imperial),
                        reps = if (isTimed) 0 else (reps.toIntOrNull() ?: 0),
                        durationMin = if (isTimed || isCardio) totalMins else 0.0,
                        distanceKm = if (isCardio) Units.toKm(distance.toDoubleOrNull() ?: 0.0, imperial) else 0.0,
                        rir = effort,
                        speedKmh = if (isCardio) Units.toSpeedKmh(speed.toDoubleOrNull() ?: 0.0, imperial) else 0.0,
                        inclinePercent = if (isCardio) (incline.toDoubleOrNull() ?: 0.0) else 0.0,
                        cardioProgram = program.trim(),
                        setType = setType
                    )
                    onSave(updated)
                    onDismiss()
                }
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDelete(); onDismiss() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun DropdownField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(onDismiss: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(vertical = 10.dp, horizontal = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                content { expanded = false }
            }
        }
    }
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
    minimized: Boolean = false,
    onHeaderClick: () -> Unit = {},
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean = true,
    canLinkSuperset: Boolean = false,
    onLinkSuperset: () -> Unit = {},
    onUnlinkSuperset: () -> Unit = {},
    onMove: (delta: Int) -> Unit,
    onSwap: () -> Unit,
    onRemove: () -> Unit = {},
    onLogSet: (weightKg: Double, reps: Int, durationMin: Double, distanceKm: Double, rir: Int?, speedKmh: Double, inclinePercent: Double, cardioProgram: String, setType: SetType) -> Unit,
    onUndo: () -> Unit,
    onUpdateSet: (setIndex: Int, updated: SetLogEntity) -> Unit = { _, _ -> },
    onDeleteSet: (setIndex: Int) -> Unit = {},
    getPreviousPerformance: suspend (String) -> List<SetLogEntity>,
    customPrograms: Set<String> = emptySet(),
    onAddCustomProgram: (String) -> Unit = {}
) {
    var editingSetIndex by remember { mutableStateOf<Int?>(null) }

    editingSetIndex?.let { sIdx ->
        val set = exercise.loggedSets.getOrNull(sIdx)
        if (set != null) {
            EditSetDialog(
                set = set,
                exercise = exercise,
                imperial = imperial,
                effortMethod = effortMethod,
                customPrograms = customPrograms,
                onAddCustomProgram = onAddCustomProgram,
                onDismiss = { editingSetIndex = null },
                onSave = { updated ->
                    onUpdateSet(sIdx, updated)
                    editingSetIndex = null
                },
                onDelete = {
                    onDeleteSet(sIdx)
                    editingSetIndex = null
                }
            )
        }
    }

    var weight by rememberSaveable(exercise.name) { mutableStateOf(exercise.targetWeightKg?.let { Units.toDisplay(it, imperial).let(HeightFormat::trimNum) } ?: "") }
    var reps by rememberSaveable(exercise.name) { mutableStateOf(exercise.targetReps.toString()) }
    var durationMin by rememberSaveable(exercise.name) { mutableStateOf("") }
    var durationSec by rememberSaveable(exercise.name) { mutableStateOf("") }
    var distance by rememberSaveable(exercise.name) { mutableStateOf("") }
    var speed by rememberSaveable(exercise.name) { mutableStateOf("") }
    var incline by rememberSaveable(exercise.name) { mutableStateOf("") }
    var program by rememberSaveable(exercise.name) { mutableStateOf("") }
    var effort by rememberSaveable(exercise.name) { mutableStateOf<Int?>(null) }
    var setType by rememberSaveable(exercise.name) { mutableStateOf(SetType.NORMAL) }
    var previousSets by remember(exercise.name) { mutableStateOf<List<SetLogEntity>>(emptyList()) }
    LaunchedEffect(exercise.name) { previousSets = getPreviousPerformance(exercise.name) }

    // Prefill logic for the weight and reps fields
    LaunchedEffect(exercise.targetWeightKg, exercise.targetReps, exercise.suggestionReason, previousSets, imperial) {
        val targetWeightNum = exercise.targetWeightKg?.let { Units.toDisplay(it, imperial) } ?: 0.0
        val historyWeightNum = previousSets.firstOrNull()?.weightKg?.let { Units.toDisplay(it, imperial) } ?: 0.0
        
        // Priority 1: AI Coach/Target weight
        if (targetWeightNum > 0.0) {
            val display = Units.toDisplay(exercise.targetWeightKg!!, imperial).let(HeightFormat::trimNum)
            if (weight != display) {
                weight = display
            }
        } 
        // Priority 2: History (if weight field is empty and no target exists)
        else if (weight.isBlank() && historyWeightNum > 0.0) {
            weight = historyWeightNum.let(HeightFormat::trimNum)
        }
        
        // Reps prefilling
        if (reps.isBlank() || reps == "0" || reps.toIntOrNull() != exercise.targetReps) {
            reps = exercise.targetReps.toString()
        }
    }

    var showAdvanced by rememberSaveable(exercise.name) { mutableStateOf(false) }
    var showAddProgramDialog by remember { mutableStateOf(false) }

    if (showAddProgramDialog) {
        var newProgramName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddProgramDialog = false },
            title = { Text("Add Custom Program") },
            text = {
                OutlinedTextField(
                    value = newProgramName,
                    onValueChange = { newProgramName = it },
                    label = { Text("Program Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newProgramName.isNotBlank()) {
                        onAddCustomProgram(newProgramName)
                        program = newProgramName
                    }
                    showAddProgramDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddProgramDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Stopwatch state
    var timerActive by rememberSaveable(exercise.name) { mutableStateOf(false) }
    var timerSeconds by rememberSaveable(exercise.name) { mutableIntStateOf(0) }
    LaunchedEffect(timerActive) {
        if (timerActive) {
            while (isActive) {
                delay(1000L)
                timerSeconds++
            }
        }
    }

    val isTimed = exercise.trackingType in setOf(ExerciseTrackingType.TIME_ONLY, ExerciseTrackingType.DISTANCE_TIME, ExerciseTrackingType.CARDIO_MACHINE)
    val isCardio = exercise.trackingType == ExerciseTrackingType.CARDIO_MACHINE
    val hasDistance = exercise.trackingType in setOf(ExerciseTrackingType.DISTANCE_TIME, ExerciseTrackingType.CARDIO_MACHINE)

    // Auto-calculate speed
    LaunchedEffect(distance, durationMin, durationSec) {
        if (hasDistance) {
            val dist = distance.toDoubleOrNull() ?: 0.0
            val mins = (durationMin.toDoubleOrNull() ?: 0.0) + (durationSec.toDoubleOrNull() ?: 0.0) / 60.0
            if (dist > 0 && mins > 0) {
                val calculatedSpeed = dist / (mins / 60.0)
                speed = "%.1f".format(calculatedSpeed)
            }
        }
    }

    val effortApplies = effortMethod != EffortMethod.OFF && exercise.trackingType in setOf(ExerciseTrackingType.WEIGHT_REPS, ExerciseTrackingType.BODYWEIGHT_REPS, ExerciseTrackingType.ASSISTED_REPS)
    val lastSet = exercise.loggedSets.lastOrNull()
    val suggestion = if (effortApplies && lastSet?.rir != null) GameMath.suggestNextSet(lastSet.weightKg, lastSet.reps, lastSet.rir) else null
    val suggestedWeightText = suggestion?.weightKg?.takeIf { it > 0 }?.let { val display = (Units.toDisplay(Units.roundToPlate(it, imperial), imperial) * 10.0).roundToInt() / 10.0; if (display % 1.0 == 0.0) display.toInt().toString() else display.toString() }

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var burstVisible by remember(exercise.name) { mutableStateOf(false) }
    var burstXp by remember(exercise.name) { mutableIntStateOf(0) }
    var seenSets by rememberSaveable(exercise.name) { mutableIntStateOf(exercise.loggedSets.size) }
    LaunchedEffect(exercise.loggedSets.size) {
        if (exercise.loggedSets.size > seenSets) {
            burstXp = exercise.loggedSets.last().xp
            burstVisible = true
            seenSets = exercise.loggedSets.size
            AudioEffects.playSetLogged(); HapticEffects.performSetLogged(haptic, context)
            if (suggestion != null) { suggestedWeightText?.let { weight = it }; reps = suggestion.reps.toString() }
            delay(1200.milliseconds); burstVisible = false
        } else seenSets = exercise.loggedSets.size
    }

    var showDetail by remember(exercise.name) { mutableStateOf(false) }
    if (showDetail) ExerciseDetailDialog(name = exercise.name, onDismiss = { showDetail = false })
    var showPlates by remember(exercise.name) { mutableStateOf(false) }
    if (showPlates) PlateCalculatorDialog(onDismiss = { showPlates = false }, onUseWeight = { d -> val r = (d * 10.0).roundToInt() / 10.0; weight = if (r % 1.0 == 0.0) r.toInt().toString() else r.toString() })

    Box(contentAlignment = Alignment.Center) {
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable { if (minimized) onHeaderClick() else showDetail = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (exercise.supersetId != null) {
                                Surface(
                                    color = Gold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "⚡ ${exercise.supersetId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = Gold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${exercise.category.label} \u2022 ${targetSummary(exercise)} \u2022 ${exercise.category.statLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (minimized) {
                        IconButton(onClick = onHeaderClick, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, "Expand")
                        }
                    }
                    if (!minimized) {
                        IconButton(onClick = { onMove(-1) }, enabled = canMoveUp, modifier = Modifier.size(32.dp)) { 
                            Icon(Icons.Default.KeyboardArrowUp, "Move Up", modifier = Modifier.size(20.dp)) 
                        }
                        IconButton(onClick = { onMove(1) }, enabled = canMoveDown, modifier = Modifier.size(32.dp)) { 
                            Icon(Icons.Default.KeyboardArrowDown, "Move Down", modifier = Modifier.size(20.dp)) 
                        }
                    }
                    if (exercise.supersetId != null) {
                        IconButton(onClick = onUnlinkSuperset, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.LinkOff, "Unlink Superset", modifier = Modifier.size(18.dp), tint = Gold)
                        }
                    } else if (canLinkSuperset) {
                        IconButton(onClick = onLinkSuperset, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Link, "Link with below into Superset", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (isTimed) {
                        IconButton(onClick = { 
                            if (timerActive) {
                                durationMin = (timerSeconds / 60).toString()
                                durationSec = (timerSeconds % 60).toString()
                                timerActive = false
                            } else {
                                timerSeconds = 0
                                timerActive = true
                            }
                        }) {
                            Icon(
                                if (timerActive) Icons.Default.Pause else Icons.Default.Timer,
                                contentDescription = "Timer",
                                tint = if (timerActive) Gold else MaterialTheme.colorScheme.primary
                            )
                        }
                        if (timerActive || timerSeconds > 0) {
                            Text(
                                "%d:%02d".format(timerSeconds / 60, timerSeconds % 60),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Gold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                    if (exercise.trackingType == ExerciseTrackingType.WEIGHT_REPS) IconButton(onClick = { showPlates = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.FitnessCenter, "Plates", modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = onSwap, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Bolt, "Swap", modifier = Modifier.size(18.dp)) }
                    if (isCardio) {
                        IconButton(onClick = { showAdvanced = !showAdvanced }, modifier = Modifier.size(32.dp)) {
                            Icon(if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.Tune, "Advanced", modifier = Modifier.size(18.dp), tint = if (showAdvanced) Gold else MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = onRemove, enabled = canRemove, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Delete, "Remove", modifier = Modifier.size(18.dp), tint = if (canRemove) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline) }
                }
                if (isCardio && exercise.loggedSets.isNotEmpty()) {
                    val last = exercise.loggedSets.last()
                    if (last.inclinePercent > 0 || last.cardioProgram.isNotBlank()) {
                        Text(
                            text = buildString {
                                if (last.inclinePercent > 0) append("Last Incline: ${trim(last.inclinePercent)}%")
                                if (last.cardioProgram.isNotBlank()) {
                                    if (isNotEmpty()) append(" • ")
                                    append("Program: ${last.cardioProgram}")
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                        )
                    }
                }

                if (!minimized) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Type", Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        Text("Set", Modifier.width(24.dp), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        Text("Previous", Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        if (isCardio) {
                            Text("Dist", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                            Text("Time", Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        } else {
                            val c1 = if (isTimed) "Min" else "Weight"
                            val c2 = if (isTimed) "Sec" else "Reps"
                            Text(c1, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                            Text(c2, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                            Text(effortMethod.label, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.width(36.dp))
                    }
                }

                exercise.loggedSets.forEachIndexed { i, set ->
                    if (minimized && i < exercise.loggedSets.size - 1) return@forEachIndexed
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { editingSetIndex = i }
                            .alpha(if (minimized) 1.0f else 0.85f)
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(set.setType.shortLabel, Modifier.width(32.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        Text("${i + 1}", Modifier.width(24.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        val p = previousSets.getOrNull(i); Text(if (p != null) setSummary(p, imperial) else "—", Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        
                        if (isCardio) {
                            Text(Units.trimmed(Units.kmToDisplay(set.distanceKm, imperial)), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                            Text(Units.formatTimeMinutes(set.durationMin), Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        } else {
                            val totalSecs = Math.round(set.durationMin * 60.0).toInt()
                            Text(if (isTimed) (totalSecs / 60).toString() else Units.formatWeight(set.weightKg, imperial), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                            Text(if (isTimed) (totalSecs % 60).toString() else set.reps.toString(), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                            Text(set.rir?.let { effortMethod.display(it) } ?: "—", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                        Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(36.dp).padding(8.dp))
                    }
                }
                if (!minimized && exercise.loggedSets.size < 20) {
                    val isLogEnabled = if (isCardio) {
                        distance.isNotEmpty() && (durationMin.isNotEmpty() || durationSec.isNotEmpty())
                    } else if (exercise.trackingType == ExerciseTrackingType.WEIGHT_REPS) {
                        weight.isNotEmpty() && reps.isNotEmpty()
                    } else {
                        reps.isNotEmpty()
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isCardio) {
                            // Cardio Row: Set Type, Set Number, Prev Performance, Distance, Duration (Min : Sec)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.width(32.dp)) {
                                    var exp by remember { mutableStateOf(false) }
                                    Text(
                                        setType.shortLabel,
                                        Modifier.clickable { exp = true }.padding(4.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    )
                                    DropdownMenu(exp, { exp = false }) {
                                        SetType.entries.forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text("${type.shortLabel} (${type.label})") },
                                                onClick = { setType = type; exp = false }
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "${exercise.loggedSets.size + 1}",
                                    Modifier.width(24.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                val p = previousSets.getOrNull(exercise.loggedSets.size)
                                Text(
                                    if (p != null) setSummary(p, imperial) else "—",
                                    Modifier.weight(1.2f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                                CompactNumberField(distance, { distance = it }, Modifier.weight(1.2f))
                                Row(
                                    Modifier.weight(1.8f),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CompactNumberField(durationMin, { durationMin = it }, Modifier.weight(1f))
                                    Text(":", style = MaterialTheme.typography.bodyMedium)
                                    CompactNumberField(durationSec, { durationSec = it }, Modifier.weight(1f))
                                }
                            }
                        } else {
                            // Strength / Bodyweight / Timed Row
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.width(32.dp)) {
                                    var exp by remember { mutableStateOf(false) }
                                    Text(
                                        setType.shortLabel,
                                        Modifier.clickable { exp = true }.padding(4.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    )
                                    DropdownMenu(exp, { exp = false }) {
                                        SetType.entries.forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text("${type.shortLabel} (${type.label})") },
                                                onClick = { setType = type; exp = false }
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "${exercise.loggedSets.size + 1}",
                                    Modifier.width(24.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                val p = previousSets.getOrNull(exercise.loggedSets.size)
                                Text(
                                    if (p != null) setSummary(p, imperial) else "—",
                                    Modifier.weight(1.2f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                                CompactNumberField(if (isTimed) durationMin else weight, { if (isTimed) durationMin = it else weight = it }, Modifier.weight(1f))
                                CompactNumberField(if (isTimed) durationSec else reps, { if (isTimed) durationSec = it else reps = it }, Modifier.weight(1f))
                                Box(Modifier.weight(1f)) {
                                    var exp by remember { mutableStateOf(false) }
                                    Text(
                                        effort?.let { effortMethod.display(it) } ?: "—",
                                        Modifier.fillMaxWidth().clickable { exp = true }.padding(4.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = if (effort == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                                    )
                                    DropdownMenu(exp, { exp = false }) {
                                        val ops = if (effortMethod == EffortMethod.RPE) listOf(null to "—", 4 to "6", 3 to "7", 2 to "8", 1 to "9", 0 to "10") else listOf(null to "—", 0 to "0", 1 to "1", 2 to "2", 3 to "3", 4 to "4", 5 to "5+")
                                        ops.forEach { (v, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { effort = v; exp = false }) }
                                    }
                                }
                            }
                        }

                        if (isCardio && showAdvanced) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Incline Dropdown
                                LabeledBox(
                                    label = "Incl %",
                                    value = incline.ifEmpty { "0" },
                                    modifier = Modifier.weight(1f),
                                    onClick = { }
                                ) { 
                                    var exp by remember { mutableStateOf(false) }
                                    Box(Modifier.fillMaxSize().clickable { exp = true })
                                    DropdownMenu(exp, { exp = false }) {
                                        listOf("0", "0.5", "1.0", "1.5", "2.0", "3.0", "4.0", "5.0", "6.0", "7.5", "10.0", "12.0", "15.0").forEach { v ->
                                            DropdownMenuItem(text = { Text("$v%") }, onClick = { incline = v; exp = false })
                                        }
                                    }
                                }

                                LabeledTextField(
                                    label = "Avg Spd",
                                    value = speed,
                                    onValueChange = { speed = it },
                                    modifier = Modifier.weight(1f)
                                )

                                // Program Dropdown
                                LabeledBox(
                                    label = "Program",
                                    value = program.ifEmpty { "Manual" },
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    var exp by remember { mutableStateOf(false) }
                                    Box(Modifier.fillMaxSize().clickable { exp = true })
                                    DropdownMenu(exp, { exp = false }) {
                                        customPrograms.sorted().forEach { p ->
                                            DropdownMenuItem(text = { Text(p) }, onClick = { program = p; exp = false })
                                        }
                                        DropdownMenuItem(text = { Text("+ Add Custom...") }, onClick = { showAddProgramDialog = true; exp = false })
                                    }
                                }
                            }
                        }

                        // Prominent 48dp Minimum Log Set Button
                        Button(
                            onClick = {
                                val totalMins = (durationMin.toDoubleOrNull() ?: 0.0) + (durationSec.toDoubleOrNull() ?: 0.0) / 60.0
                                onLogSet(
                                    Units.toKg(weight.toDoubleOrNull() ?: 0.0, imperial),
                                    reps.toIntOrNull() ?: 0,
                                    totalMins,
                                    Units.toKm(distance.toDoubleOrNull() ?: 0.0, imperial),
                                    effort,
                                    Units.toSpeedKmh(speed.toDoubleOrNull() ?: 0.0, imperial),
                                    incline.toDoubleOrNull() ?: 0.0,
                                    program,
                                    setType
                                )
                                effort = null; setType = SetType.NORMAL; durationMin = ""; durationSec = ""; distance = ""; speed = ""; incline = ""; program = ""; timerSeconds = 0; timerActive = false
                            },
                            enabled = isLogEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .semantics { contentDescription = "Log set for ${exercise.name}" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Log Set", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                val aiReason = exercise.suggestionReason
                if (!minimized && (suggestion != null || aiReason != null)) {
                    val nextText = if (suggestion != null) {
                        val t = if (suggestedWeightText != null) "$suggestedWeightText ${Units.label(imperial)} \u00D7 ${suggestion.reps}" else "${suggestion.reps} reps"
                        "next: $t"
                    } else {
                        val weightPart = exercise.targetWeightKg?.let { " @ ${Units.toDisplay(it, imperial).let(HeightFormat::trimNum)} ${Units.label(imperial)}" }.orEmpty()
                        "next: ${exercise.targetReps} reps$weightPart"
                    }
                    val note = aiReason ?: suggestion?.note ?: "Ready for the next set"
                    Text("\uD83D\uDCA1 $note \u00B7 $nextText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
                }
                if (exercise.loggedSets.isNotEmpty() && !minimized) TextButton(onClick = onUndo, Modifier.align(Alignment.Start)) { Text("Undo Last Set", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
            }
        }
        if (burstVisible) Box(Modifier.align(Alignment.Center)) { FloatingTextBurst(FloatingBurst(text = "+$burstXp XP", subtext = "${exercise.category.statLabel} GAIN!", color = Gold), { }) }
    }
}

@Composable
private fun LabeledTextField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(vertical = 8.dp, horizontal = 6.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun LabeledBox(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}, content: @Composable BoxScope.() -> Unit = {}) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
        Box(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).clickable { onClick() }.padding(vertical = 8.dp, horizontal = 6.dp)
        ) {
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            content()
        }
    }
}

@Composable
private fun CompactNumberField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
    )
}

@Composable
private fun setSummary(set: SetLogEntity, imperial: Boolean): String {
    val typePrefix = if (set.setType != SetType.NORMAL) "[${set.setType.shortLabel}] " else ""
    val body = when (set.category) {
        ExerciseCategory.STRENGTH -> "${Units.formatWeight(set.weightKg, imperial)} \u00D7 ${set.reps}"
        ExerciseCategory.CARDIO -> buildString {
            if (set.durationMin > 0) append(Units.formatTimeMinutes(set.durationMin))
            if (set.distanceKm > 0) { if (isNotEmpty()) append(" \u00B7 "); append(Units.formatDistance(set.distanceKm, imperial)) }
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
    ExerciseTrackingType.CARDIO_MACHINE, ExerciseTrackingType.DISTANCE_TIME -> "Tgt ${exercise.targetReps}m"
    ExerciseTrackingType.DISTANCE_ONLY -> "Tgt Dist"
    ExerciseTrackingType.TIME_ONLY -> "Tgt ${exercise.targetReps}m"
    else -> "Tgt ${exercise.targetSets}\u00D7${exercise.targetReps}"
}

private fun trim(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else "%.1f".format(d)

@Composable
private fun SessionResultDialog(finish: SessionFinish, imperial: Boolean, onDismiss: () -> Unit) {
    val r = finish.result
    var showRewards by remember { mutableStateOf(false) }
    val durationText = formatDuration(r.durationMs)
    val volumeText = "${Units.toDisplay(r.volumeKg, imperial).roundToInt()} ${Units.label(imperial)}"
    val prCount = r.prs.count { it.isNew }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = NightBg) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Spacer(Modifier.height(24.dp))
                Text(text = if (finish.isDemo) "Demo Complete!" else "Well Done!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
                Text(text = "You completed ${r.weeklyWorkoutsDone} of ${r.weeklyWorkoutsGoal} workouts this week.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                    CharacterAvatar(clazz = r.updatedCharacter.characterClass ?: CharacterClass.WARRIOR, modifier = Modifier.size(200.dp), highlightMuscles = r.musclesWorked, expression = AvatarExpression.VICTORIOUS)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryStatCard("Duration", durationText, Modifier.weight(1f))
                    SummaryStatCard("Volume", volumeText, Modifier.weight(1f))
                    SummaryStatCard("PRs", "\uD83C\uDFC5 $prCount", Modifier.weight(1f))
                }
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Workout Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    finish.summaryItems.forEach { SummaryItem(it, r.updatedCharacter.characterClass ?: CharacterClass.WARRIOR) }
                }
                if (r.prs.isNotEmpty()) PRSection(r.prs, imperial)
                if (finish.praise != null) Text("\u201C${finish.praise}\u201D", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.weight(1f))
                Button(onClick = { if (r.rewardBatch != null) showRewards = true else onDismiss() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp), colors = if (r.rewardBatch != null) ButtonDefaults.buttonColors(containerColor = Gold) else ButtonDefaults.buttonColors()) {
                    Text(if (r.rewardBatch != null) "Claim Rewards" else "Continue", style = MaterialTheme.typography.titleMedium, color = if (r.rewardBatch != null) NightBg else Color.White)
                }
                Spacer(Modifier.height(16.dp))
            }
            if (showRewards && r.rewardBatch != null) RewardRevealDialog(batch = r.rewardBatch, onDismiss = onDismiss)
            ConfettiOverlay(modifier = Modifier.fillMaxSize(), trigger = r.xp, pieces = if (r.levelsGained > 0) 150 else 80)
        }
    }
}

@Composable
private fun SummaryStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.08f)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun PRSection(prs: List<SessionPr>, imperial: Boolean) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Personal Records", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.6f))
        prs.filter { it.isNew }.groupBy { it.exerciseName }.forEach { (name, exPrs) ->
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("\uD83C\uDFC5", fontSize = 24.sp); Spacer(Modifier.width(12.dp))
                Column {
                    Text(name, fontWeight = FontWeight.Bold, color = Color.White)
                    exPrs.forEach { pr ->
                        val label = when (pr.kind) {
                            PrKind.WEIGHT -> "Weight"; PrKind.VOLUME -> "Volume"; PrKind.ONE_RM -> "1RM"; PrKind.DISTANCE -> "Distance"; PrKind.PACE -> "Pace"; PrKind.TIME -> "Duration"; PrKind.REPS -> "Reps"; PrKind.SPEED -> "Speed"; PrKind.INCLINE -> "Incline"
                        }
                        val valText = when (pr.kind) {
                            PrKind.WEIGHT -> "${Units.trimmed(Units.toDisplay(pr.value, imperial))} ${Units.label(imperial)} \u00D7 ${pr.reps}"; PrKind.DISTANCE -> Units.formatDistance(pr.value, imperial); PrKind.PACE -> Units.formatPace(pr.value, imperial); PrKind.TIME -> Units.formatTimeMinutes(pr.value); PrKind.REPS -> "${pr.value.toInt()} reps"; PrKind.SPEED -> Units.formatSpeed(pr.value, imperial); PrKind.INCLINE -> "${Units.trimmed(pr.value)}%"; else -> "${Units.trimmed(Units.toDisplay(pr.value, imperial))} ${Units.label(imperial)}"
                        }
                        Text("$label \u2014 $valText", style = MaterialTheme.typography.bodySmall, color = Gold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(item: WorkoutSummaryItem, clazz: CharacterClass) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            if (item.muscles.isNotEmpty()) {
                // Show naked avatar with highlights
                CharacterAvatar(
                    clazz = clazz,
                    modifier = Modifier.size(40.dp),
                    gear = emptyMap(),
                    highlightMuscles = item.muscles,
                    expression = AvatarExpression.CALM,
                    detail = AvatarDetail.COMPACT
                )
            } else if (item.iconUrl != null) {
                AsyncImage(model = item.iconUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text("\uD83C\uDFCB\uFE0F", fontSize = 20.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, fontWeight = FontWeight.Bold, color = Color.White)
            if (item.isIncreasedWeight || item.isIncreasedVolume || item.isIncreased1RM) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.North, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = if (item.isIncreasedWeight) "Increased Weight" else if (item.isIncreasedVolume) "Increased Volume" else "Increased 1RM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
            }
        }
    }
}

@Composable
private fun MediaControllerCard(state: MediaState, onToggle: () -> Unit, onNext: () -> Unit, onPrev: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Bolt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = state.title ?: "No Track", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = state.artist ?: "Unknown Artist", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row {
                IconButton(onClick = onPrev) { Icon(Icons.Default.SkipPrevious, "Prev") }
                IconButton(onClick = onToggle) { Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause") }
                IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Next") }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF12131F)
@androidx.compose.runtime.Composable
fun ActiveSessionScreenPreview() {
    com.fitnessquest.rpg.ui.theme.FitQuestTheme {
        ActiveSessionScreenContent(
            state = ActiveSessionUiState(
                title = "Preview Quest",
                exercises = listOf(
                    SessionExercise(name = "Pushups", category = ExerciseCategory.BODYWEIGHT),
                    SessionExercise(name = "Squats", category = ExerciseCategory.STRENGTH),
                    SessionExercise(name = "Treadmill Run", category = ExerciseCategory.CARDIO, trackingType = ExerciseTrackingType.CARDIO_MACHINE)
                ),
                loading = false
            ),
            imperial = true,
            effortMethod = EffortMethod.RIR,
            sessionDurationMs = 1200000,
            customCardioPrograms = emptySet(),
            onDone = {},
            onAmbushFight = {},
            onFinished = {},
            actions = ActiveSessionActions()
        )
    }
}
