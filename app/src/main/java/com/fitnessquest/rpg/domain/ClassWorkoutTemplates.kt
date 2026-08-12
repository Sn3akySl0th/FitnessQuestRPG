package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.WorkoutExerciseEntity

data class ClassWorkoutTemplate(
    val name: String,
    val tagline: String,
    val exercises: List<WorkoutExerciseEntity>
)

object ClassWorkoutTemplates {
    fun forClass(cls: CharacterClass): ClassWorkoutTemplate = when (cls) {
        CharacterClass.WARRIOR -> template(
            name = "Warrior Iron Circuit",
            tagline = "Strength volume for a frontline bruiser.",
            moves = listOf(
                "Squat" to ExerciseCategory.STRENGTH,
                "Bench Press" to ExerciseCategory.STRENGTH,
                "Barbell Row" to ExerciseCategory.STRENGTH,
                "Plank" to ExerciseCategory.FLEXIBILITY
            )
        )
        CharacterClass.MAGE -> template(
            name = "Mage Focus Flow",
            tagline = "Willpower, posture, and consistency work.",
            moves = listOf(
                "Dumbbell Shoulder Press" to ExerciseCategory.STRENGTH,
                "Lat Pulldown" to ExerciseCategory.STRENGTH,
                "Yoga" to ExerciseCategory.FLEXIBILITY,
                "Walking" to ExerciseCategory.CARDIO
            )
        )
        CharacterClass.THIEF -> template(
            name = "Thief Shadow Sets",
            tagline = "Agility, bodyweight control, and fast finishers.",
            moves = listOf(
                "Push-ups" to ExerciseCategory.BODYWEIGHT,
                "Lunges" to ExerciseCategory.BODYWEIGHT,
                "Burpees" to ExerciseCategory.BODYWEIGHT,
                "Jump Rope" to ExerciseCategory.CARDIO
            )
        )
        CharacterClass.RANGER -> template(
            name = "Ranger Trail Hunt",
            tagline = "Cardio plus athletic strength for long journeys.",
            moves = listOf(
                "Running" to ExerciseCategory.CARDIO,
                "Pull-ups" to ExerciseCategory.BODYWEIGHT,
                "Bodyweight Squats" to ExerciseCategory.BODYWEIGHT,
                "Plank" to ExerciseCategory.FLEXIBILITY
            )
        )
        CharacterClass.PALADIN -> template(
            name = "Paladin Dawn Guard",
            tagline = "Balanced strength and endurance with a sturdy core.",
            moves = listOf(
                "Leg Press" to ExerciseCategory.STRENGTH,
                "Chest Press" to ExerciseCategory.STRENGTH,
                "Walking" to ExerciseCategory.CARDIO,
                "Plank" to ExerciseCategory.FLEXIBILITY
            )
        )
        CharacterClass.NECROMANCER -> template(
            name = "Necromancer Grave Tempo",
            tagline = "Slow control, posterior chain, and recovery discipline.",
            moves = listOf(
                "Deadlift" to ExerciseCategory.STRENGTH,
                "Seated Row" to ExerciseCategory.STRENGTH,
                "Yoga" to ExerciseCategory.FLEXIBILITY,
                "Plank" to ExerciseCategory.FLEXIBILITY
            )
        )
        else -> template(
            name = "${cls.label} Training",
            tagline = "Balanced routine tailored to ${cls.label}.",
            moves = listOf(
                "Squat" to ExerciseCategory.STRENGTH,
                "Push-ups" to ExerciseCategory.BODYWEIGHT,
                "Running" to ExerciseCategory.CARDIO,
                "Yoga" to ExerciseCategory.FLEXIBILITY
            )
        )
    }

    private fun template(
        name: String,
        tagline: String,
        moves: List<Pair<String, ExerciseCategory>>
    ): ClassWorkoutTemplate = ClassWorkoutTemplate(
        name = name,
        tagline = tagline,
        exercises = moves.mapIndexed { index, (exercise, category) ->
            WorkoutExerciseEntity(
                workoutId = 0,
                exerciseName = exercise,
                category = category,
                targetSets = if (category == ExerciseCategory.CARDIO) 1 else 3,
                targetReps = when (category) {
                    ExerciseCategory.STRENGTH -> 8
                    ExerciseCategory.CARDIO -> 12
                    ExerciseCategory.BODYWEIGHT -> 12
                    ExerciseCategory.FLEXIBILITY -> 5
                },
                sortOrder = index
            )
        }
    )
}
