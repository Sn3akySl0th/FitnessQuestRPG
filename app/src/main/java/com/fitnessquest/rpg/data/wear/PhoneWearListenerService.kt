package com.fitnessquest.rpg.data.wear

import com.fitnessquest.rpg.FitQuestApp
import com.fitnessquest.rpg.data.ai.WorkoutRecommendationEngine
import com.fitnessquest.rpg.data.db.WorkoutExerciseEntity
import com.fitnessquest.rpg.data.export.AvatarExporter
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.ClassWorkoutTemplates
import com.fitnessquest.rpg.domain.ExerciseTracking
import com.fitnessquest.rpg.domain.ExerciseTrackingType
import com.fitnessquest.rpg.domain.Units
import com.fitnessquest.rpg.ui.screens.SessionExercise
import com.fitnessquest.shared.wear.WearExerciseState
import com.fitnessquest.shared.wear.WearPaths
import com.fitnessquest.shared.wear.WearRoutineSummary
import com.fitnessquest.shared.wear.WearSessionState
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/**
 * Ensures wear messages can wake the phone process; answers HELLO, provides routines,
 * and allows starting a workout directly from the watch.
 */
class PhoneWearListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val container get() = (applicationContext as? FitQuestApp)?.container

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val appContainer = container ?: return super.onMessageReceived(messageEvent)

        when (messageEvent.path) {
            WearPaths.HELLO -> {
                scope.launch {
                    runCatching {
                        Wearable.getMessageClient(this@PhoneWearListenerService)
                            .sendMessage(messageEvent.sourceNodeId, WearPaths.HELLO_ACK, ByteArray(0))
                            .await()
                    }
                    sendRoutinesList(messageEvent.sourceNodeId)
                    syncAvatar(appContainer)
                }
            }
            WearPaths.ROUTINES_LIST -> {
                scope.launch {
                    sendRoutinesList(messageEvent.sourceNodeId)
                }
            }
            WearPaths.START_WORKOUT -> {
                scope.launch {
                    startWorkoutFromWear(appContainer, messageEvent.sourceNodeId, messageEvent.data)
                }
            }
            WearPaths.DAILY_STEPS_SYNC -> {
                scope.launch {
                    com.fitnessquest.shared.wear.WearDailyStepsSync.fromJson(messageEvent.data)?.let { payload ->
                        appContainer.steps.syncWearOsSteps(payload.epochDay, payload.steps)
                    }
                }
            }
        }
        super.onMessageReceived(messageEvent)
    }

    private suspend fun syncAvatar(appContainer: com.fitnessquest.rpg.AppContainer) {
        runCatching {
            val char = appContainer.repository.character.firstOrNull() ?: return
            val gear = appContainer.repository.equippedGear(char)
            AvatarExporter.syncAvatarToWear(
                context = this@PhoneWearListenerService,
                character = char,
                cls = char.characterClass ?: CharacterClass.WARRIOR,
                gear = gear
            )
        }
    }

    private suspend fun sendRoutinesList(targetNodeId: String) {
        val appContainer = container ?: return
        runCatching {
            val workouts = appContainer.repository.workouts.firstOrNull() ?: emptyList()
            val sessions = appContainer.repository.sessions.firstOrNull() ?: emptyList()
            val sore = appContainer.prefs.soreMuscles.firstOrNull() ?: emptySet()
            val wellRested = appContainer.prefs.wellRestedBuff.firstOrNull() ?: false

            val recommendation = WorkoutRecommendationEngine.recommendNextWorkout(
                routines = workouts,
                recentSessions = sessions,
                soreMuscles = sore,
                wellRestedBuffActive = wellRested
            )

            val recRoutine = recommendation.routine

            val list = mutableListOf<WearRoutineSummary>()
            if (recRoutine != null) {
                val exCount = appContainer.repository.exercisesFor(recRoutine.id).size
                list.add(
                    WearRoutineSummary(
                        id = recRoutine.id,
                        name = "🔥 ${recRoutine.name} (Rec.)",
                        exerciseCount = exCount,
                        isRecommended = true
                    )
                )
            }

            workouts.forEach { w ->
                if (w.id != recRoutine?.id) {
                    val exCount = appContainer.repository.exercisesFor(w.id).size
                    list.add(
                        WearRoutineSummary(
                            id = w.id,
                            name = w.name,
                            exerciseCount = exCount,
                            isRecommended = false
                        )
                    )
                }
            }

            if (list.isEmpty()) {
                val char = appContainer.repository.character.firstOrNull()
                val cls = char?.characterClass ?: CharacterClass.WARRIOR
                val tpl = ClassWorkoutTemplates.forClass(cls)
                list.add(
                    WearRoutineSummary(
                        id = -1L,
                        name = "⚔️ ${tpl.name}",
                        exerciseCount = tpl.exercises.size,
                        isRecommended = true
                    )
                )
            }

            val bytes = WearRoutineSummary.listToJson(list)
            Wearable.getMessageClient(this)
                .sendMessage(targetNodeId, WearPaths.ROUTINES_LIST, bytes)
                .await()
        }
    }

    private suspend fun startWorkoutFromWear(
        appContainer: com.fitnessquest.rpg.AppContainer,
        sourceNodeId: String,
        data: ByteArray
    ) {
        runCatching {
            val req = if (data.isNotEmpty()) JSONObject(String(data, Charsets.UTF_8)) else JSONObject()
            val routineId = req.optLong("routineId", -1L)

            val workouts = appContainer.repository.workouts.firstOrNull() ?: emptyList()
            val selectedWorkout = if (routineId > 0) workouts.find { it.id == routineId } else null

            val sessions = appContainer.repository.sessions.firstOrNull() ?: emptyList()
            val sore = appContainer.prefs.soreMuscles.firstOrNull() ?: emptySet()
            val wellRested = appContainer.prefs.wellRestedBuff.firstOrNull() ?: false
            val recRoutine = WorkoutRecommendationEngine.recommendNextWorkout(workouts, sessions, sore, wellRested).routine

            val (targetWorkoutId, targetWorkoutName) = when {
                selectedWorkout != null -> selectedWorkout.id to selectedWorkout.name
                recRoutine != null -> recRoutine.id to recRoutine.name
                workouts.isNotEmpty() -> workouts.first().id to workouts.first().name
                else -> null to null
            }

            val workoutExercises: List<WorkoutExerciseEntity> = if (targetWorkoutId != null) {
                appContainer.repository.exercisesFor(targetWorkoutId)
            } else {
                val char = appContainer.repository.character.firstOrNull()
                val cls = char?.characterClass ?: CharacterClass.WARRIOR
                ClassWorkoutTemplates.forClass(cls).exercises
            }

            val title = targetWorkoutName ?: "Hero's Training Quest"
            val sessionExercises = workoutExercises.map { e ->
                SessionExercise(
                    name = e.exerciseName,
                    category = e.category,
                    targetSets = e.targetSets,
                    targetReps = e.targetReps,
                    targetWeightKg = e.targetWeightKg,
                    trackingType = ExerciseTracking.resolve(name = e.exerciseName, category = e.category)
                )
            }

            // Start session in repository
            appContainer.repository.startActiveSession(
                title = title,
                workoutId = targetWorkoutId,
                exercises = sessionExercises
            )

            // Convert to WearExerciseState
            val imperial = appContainer.prefs.imperial.firstOrNull() ?: true
            val wearExercises = sessionExercises.map { ex ->
                val prev = appContainer.repository.getPreviousPerformance(ex.name)
                val prevWeightKg = prev.firstOrNull()?.weightKg ?: ex.targetWeightKg ?: 0.0
                val prevWeightDisp = if (prevWeightKg > 0) Units.toDisplay(prevWeightKg, imperial) else 0.0
                WearExerciseState(
                    name = ex.name,
                    category = ex.category.name,
                    targetSets = ex.targetSets,
                    targetReps = ex.targetReps,
                    loggedSets = 0,
                    trackingType = ex.trackingType.name,
                    lastWeightDisplay = prevWeightDisp,
                    lastReps = ex.targetReps,
                    suggestedWeightDisplay = if (prevWeightDisp > 0) prevWeightDisp else null,
                    suggestedReps = ex.targetReps,
                    supersetId = ex.supersetId
                )
            }

            val sessionState = WearSessionState(
                active = true,
                title = title,
                imperial = imperial,
                heatStreak = appContainer.repository.character.firstOrNull()?.let { appContainer.repository.equippedGear(it) }?.size ?: 0,
                totalSets = 0,
                totalXp = 0,
                restEndsAt = null,
                restDurationSec = 60,
                currentIndex = 0,
                exercises = wearExercises,
                watchLinked = true
            )

            Wearable.getMessageClient(this)
                .sendMessage(sourceNodeId, WearPaths.SESSION_STATE, sessionState.toJson())
                .await()
        }
    }
}
