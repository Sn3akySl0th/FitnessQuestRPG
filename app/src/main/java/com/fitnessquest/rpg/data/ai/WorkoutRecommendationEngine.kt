package com.fitnessquest.rpg.data.ai

import com.fitnessquest.rpg.data.db.SessionEntity
import com.fitnessquest.rpg.data.db.WorkoutEntity
import java.util.concurrent.TimeUnit

data class RoutineRecommendation(
    val routine: WorkoutEntity?,
    val isRestDay: Boolean = false,
    val readinessPercent: Int = 100,
    val title: String = "",
    val reason: String = "",
    val bonusXpPercent: Int = 0,
    val cautionWarning: String? = null,
    val sideQuestTitle: String? = null,
    val sideQuestReps: String? = null
)

object WorkoutRecommendationEngine {

    private const val FULL_RECOVERY_HOURS = 48.0

    fun calculateMuscleFreshness(
        recentSessions: List<SessionEntity>,
        soreMuscles: Set<String> = emptySet(),
        nowMillis: Long = System.currentTimeMillis()
    ): Map<String, Int> {
        val lastTrainedMap = mutableMapOf<String, Long>()

        for (session in recentSessions) {
            val sessionTime = session.startedAt
            val inferredMuscles = inferMusclesFromText(session.name.lowercase())
            for (m in inferredMuscles) {
                val existing = lastTrainedMap[m] ?: 0L
                if (sessionTime > existing) {
                    lastTrainedMap[m] = sessionTime
                }
            }
        }

        val allMuscles = listOf(
            "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Quads", 
            "Hamstrings", "Glutes", "Calves", "Abs", "Traps", "Lats", 
            "Lower Back", "Forearms"
        )
        val result = mutableMapOf<String, Int>()

        for (m in allMuscles) {
            val lastTrained = lastTrainedMap[m]
            val baseFreshness = if (lastTrained == null) {
                100
            } else {
                val diffHours = (nowMillis - lastTrained).toDouble() / (1000.0 * 60.0 * 60.0)
                ((diffHours / FULL_RECOVERY_HOURS) * 100.0).toInt().coerceIn(15, 100)
            }

            val isSore = soreMuscles.contains(m.uppercase()) || 
                         soreMuscles.contains(m) || 
                         soreMuscles.contains(m.uppercase().replace(" ", "_"))
            val finalFreshness = if (isSore) {
                (baseFreshness - 30).coerceAtLeast(15)
            } else {
                baseFreshness
            }
            result[m] = finalFreshness
        }

        return result
    }

    fun recommendNextWorkout(
        routines: List<WorkoutEntity>,
        recentSessions: List<SessionEntity>,
        soreMuscles: Set<String> = emptySet(),
        wellRestedBuffActive: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): RoutineRecommendation {
        if (routines.isEmpty()) {
            return RoutineRecommendation(
                routine = null,
                isRestDay = false,
                readinessPercent = 100,
                title = "Create Your First Quest Routine",
                reason = "Build a custom routine or start a quick quest to earn XP & Gold!",
                bonusXpPercent = if (wellRestedBuffActive) 15 else 0
            )
        }

        val lastSession = recentSessions.firstOrNull()
        val hoursSinceLastSession = lastSession?.let {
            TimeUnit.MILLISECONDS.toHours(nowMillis - it.startedAt)
        } ?: 999L

        val freshnessMap = calculateMuscleFreshness(recentSessions, soreMuscles, nowMillis)

        var bestRoutine: WorkoutEntity? = null
        var maxScore = -9999

        for (routine in routines) {
            val routineMuscles = inferMusclesFromText(routine.name.lowercase())
            val avgFreshness = if (routineMuscles.isNotEmpty()) {
                routineMuscles.map { freshnessMap[it] ?: 100 }.average().toInt()
            } else 90

            val hasSoreConflict = routineMuscles.any { 
                soreMuscles.contains(it) || 
                soreMuscles.contains(it.uppercase()) ||
                soreMuscles.contains(it.uppercase().replace(" ", "_"))
            }

            var score = avgFreshness
            if (hasSoreConflict) score -= 40
            if (lastSession != null && lastSession.name.equals(routine.name, ignoreCase = true) && hoursSinceLastSession < 24) {
                score -= 50
            }

            if (score > maxScore) {
                maxScore = score
                bestRoutine = routine
            }
        }

        val selectedRoutine = bestRoutine ?: routines.first()

        val overallFreshness = freshnessMap.values.average().toInt()
        val isRestDayIndicated = (hoursSinceLastSession < 18 && overallFreshness < 65)

        if (isRestDayIndicated) {
            val sideQuests = listOf(
                Pair("10-Min Full Body Mobility & Stretch", "10 Mins"),
                Pair("5,000 Step Recovery Walk", "5k Steps"),
                Pair("10 Quick Bodyweight Pushups", "10 Reps"),
                Pair("20 Jumping Jacks Blitz", "20 Reps")
            )
            val randomQuest = sideQuests.random()
            return RoutineRecommendation(
                routine = null,
                isRestDay = true,
                readinessPercent = overallFreshness,
                title = "🛡️ Active Recovery Rest Day",
                reason = "Your muscles are actively rebuilding (Overall Freshness: $overallFreshness%). Complete a light side quest for a boost!",
                bonusXpPercent = 0,
                sideQuestTitle = randomQuest.first,
                sideQuestReps = randomQuest.second
            )
        }

        val recMuscles = inferMusclesFromText(selectedRoutine.name.lowercase())
        val recFreshness = if (recMuscles.isNotEmpty()) {
            recMuscles.map { freshnessMap[it] ?: 100 }.average().toInt().coerceIn(50, 100)
        } else 90

        val soreWarning = if (recMuscles.any { 
            soreMuscles.contains(it) || 
            soreMuscles.contains(it.uppercase()) ||
            soreMuscles.contains(it.uppercase().replace(" ", "_"))
        }) {
            "⚠️ Soreness reported in target muscles. Adjust load as needed."
        } else null

        val bonusXp = if (wellRestedBuffActive) 15 else if (recFreshness >= 90) 10 else 0
        val reasonText = when {
            lastSession == null -> "First quest! Target muscles are 100% primed and ready for action."
            recFreshness >= 90 -> "Target muscles (${recMuscles.joinToString()}) are 100% Primed & Recovered!"
            else -> "Optimal rotation sequence following your previous session (${lastSession.name})."
        }

        return RoutineRecommendation(
            routine = selectedRoutine,
            isRestDay = false,
            readinessPercent = recFreshness,
            title = selectedRoutine.name,
            reason = reasonText,
            bonusXpPercent = bonusXp,
            cautionWarning = soreWarning
        )
    }

    private fun inferMusclesFromText(text: String): List<String> = when {
        "upper" in text -> listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Traps", "Forearms")
        "lower" in text || "leg" in text -> listOf("Quads", "Hamstrings", "Glutes", "Calves")
        "push" in text -> listOf("Chest", "Shoulders", "Triceps")
        "pull" in text -> listOf("Back", "Lats", "Biceps", "Traps", "Forearms")
        "chest" in text -> listOf("Chest", "Triceps", "Shoulders")
        "back" in text -> listOf("Back", "Lats", "Lower Back", "Traps", "Biceps")
        "shoulder" in text || "delt" in text -> listOf("Shoulders", "Triceps", "Traps")
        "arm" in text -> listOf("Biceps", "Triceps", "Forearms")
        "core" in text || "abs" in text -> listOf("Abs", "Lower Back")
        else -> listOf("Chest", "Back", "Quads", "Hamstrings")
    }
}
