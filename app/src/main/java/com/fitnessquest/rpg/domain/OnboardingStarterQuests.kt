package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.WorkoutExerciseEntity

/**
 * Builds a short **practice** onboarding workout — 2 exercises, 1 set each —
 * so new players can try the session UI with fake numbers. Nothing is meant to count.
 */
object OnboardingStarterQuests {

    data class Plan(
        val workoutName: String,
        val exercises: List<WorkoutExerciseEntity>,
        val summaryLines: List<String>
    )

    fun build(
        profile: TrainingProfile,
        goal: FitnessGoal,
        duration: WorkoutDurationMins,
        focus: MuscleFocus,
        cardio: CardioPlacement
    ): Plan {
        // Duration / cardio from the questionnaire still shape the *real* program later;
        // the demo stays tiny so onboarding stays snappy.
        val picks = strengthExercises(profile, focus, goal).take(2).toMutableList()
        if (picks.isEmpty()) {
            picks += "Push-ups" to ExerciseCategory.BODYWEIGHT
            picks += "Bodyweight Squats" to ExerciseCategory.BODYWEIGHT
        }
        // Optional tiny cardio peek when they asked for it — still only 2 moves total.
        if (cardio != CardioPlacement.NONE && picks.size >= 2) {
            picks[1] = cardioName(profile) to ExerciseCategory.CARDIO
        }

        val targetReps = when (goal) {
            FitnessGoal.GAIN_STRENGTH -> 5
            FitnessGoal.BUILD_MUSCLE -> 8
            FitnessGoal.FAT_LOSS -> 10
        }

        val exercises = picks.mapIndexed { index, (name, category) ->
            WorkoutExerciseEntity(
                workoutId = 0,
                exerciseName = name,
                category = category,
                targetSets = 1,
                targetReps = if (category == ExerciseCategory.CARDIO) 5 else targetReps,
                sortOrder = index
            )
        }

        return Plan(
            workoutName = "Demo Quest",
            exercises = exercises,
            summaryLines = listOf(
                "Does not count toward your data",
                "Feel free to log, swap, and experiment",
                "Nothing is saved — Training Grounds is for real workouts",
                "2 moves · 1 set each · ${goal.label} · ${profile.level.label}"
            )
        )
    }

    private fun cardioName(profile: TrainingProfile): String =
        if (Equipment.CARDIO_GEAR in profile.equipment) "Running" else "Jump Rope"

    private fun strengthExercises(
        profile: TrainingProfile,
        focus: MuscleFocus,
        goal: FitnessGoal
    ): List<Pair<String, ExerciseCategory>> {
        val hasBarbell = Equipment.BARBELL in profile.equipment
        val hasDb = Equipment.DUMBBELLS in profile.equipment
        val hasMachines = Equipment.MACHINES in profile.equipment
        val gym = hasBarbell || hasDb || hasMachines

        val pool = mutableListOf<Pair<String, ExerciseCategory>>()
        if (gym) {
            if (hasBarbell) {
                pool += "Squat" to ExerciseCategory.STRENGTH
                pool += "Bench Press" to ExerciseCategory.STRENGTH
                pool += "Barbell Row" to ExerciseCategory.STRENGTH
            }
            if (hasDb) {
                pool += "Dumbbell Shoulder Press" to ExerciseCategory.STRENGTH
                pool += "Dumbbell Curl" to ExerciseCategory.STRENGTH
            }
            if (hasMachines) {
                pool += "Lat Pulldown" to ExerciseCategory.STRENGTH
                pool += "Leg Press" to ExerciseCategory.STRENGTH
            }
        } else {
            pool += "Push-ups" to ExerciseCategory.BODYWEIGHT
            pool += "Bodyweight Squats" to ExerciseCategory.BODYWEIGHT
            pool += "Lunges" to ExerciseCategory.BODYWEIGHT
            pool += "Plank (reps = seconds)" to ExerciseCategory.BODYWEIGHT
        }

        val prioritized = when (focus) {
            MuscleFocus.BALANCED -> pool
            MuscleFocus.CHEST -> prioritize(pool, listOf("Bench Press", "Push-ups"))
            MuscleFocus.QUADRICEPS -> prioritize(pool, listOf("Squat", "Leg Press", "Bodyweight Squats", "Lunges"))
            MuscleFocus.SHOULDERS -> prioritize(pool, listOf("Overhead Press", "Dumbbell Shoulder Press"))
            MuscleFocus.UPPER_BACK -> prioritize(pool, listOf("Barbell Row", "Lat Pulldown", "Pull-ups"))
            MuscleFocus.BICEPS -> prioritize(pool, listOf("Dumbbell Curl", "Pull-ups"))
            MuscleFocus.ABS -> prioritize(pool, listOf("Sit-ups", "Plank (reps = seconds)"))
        }

        return if (goal == FitnessGoal.FAT_LOSS && gym) {
            prioritized + ("Burpees" to ExerciseCategory.BODYWEIGHT)
        } else {
            prioritized
        }
    }

    private fun prioritize(
        pool: List<Pair<String, ExerciseCategory>>,
        preferred: List<String>
    ): List<Pair<String, ExerciseCategory>> {
        val head = preferred.mapNotNull { name -> pool.find { it.first == name } }
        val tail = pool.filter { it !in head }
        return head + tail
    }
}
