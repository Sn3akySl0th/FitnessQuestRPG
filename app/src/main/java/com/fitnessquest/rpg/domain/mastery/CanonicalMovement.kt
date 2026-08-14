package com.fitnessquest.rpg.domain.mastery

import com.fitnessquest.rpg.data.db.ExerciseCategory

/**
 * High-level canonical movement patterns that aggregate specific exercises.
 */
enum class CanonicalMovement(
    val canonicalKey: String,
    val displayName: String,
    val category: ExerciseCategory,
    val description: String,
    val icon: String = "🥋"
) {
    SQUAT(
        canonicalKey = "SQUAT",
        displayName = "Squat Mastery",
        category = ExerciseCategory.STRENGTH,
        description = "Knee-dominant lower body strength, quad and glute power.",
        icon = "🦵"
    ),
    BENCH_PRESS(
        canonicalKey = "BENCH_PRESS",
        displayName = "Bench Press Mastery",
        category = ExerciseCategory.STRENGTH,
        description = "Horizontal upper body pushing, chest and triceps force.",
        icon = "💥"
    ),
    DEADLIFT(
        canonicalKey = "DEADLIFT",
        displayName = "Deadlift Mastery",
        category = ExerciseCategory.STRENGTH,
        description = "Hip-hinge posterior chain power, back and hamstring durability.",
        icon = "⛓️"
    ),
    OVERHEAD_PRESS(
        canonicalKey = "OVERHEAD_PRESS",
        displayName = "Overhead Press Mastery",
        category = ExerciseCategory.STRENGTH,
        description = "Vertical pushing strength, deltoids and shoulder stability.",
        icon = "⚔️"
    ),
    PULL_UP(
        canonicalKey = "PULL_UP",
        displayName = "Pull-Up & Row Mastery",
        category = ExerciseCategory.STRENGTH,
        description = "Vertical and horizontal pulling power, upper back and lats.",
        icon = "🦅"
    ),
    DIP_PUSHUP(
        canonicalKey = "DIP_PUSHUP",
        displayName = "Push-Up & Dip Mastery",
        category = ExerciseCategory.BODYWEIGHT,
        description = "Bodyweight pushing capacity, pectorals and triceps endurance.",
        icon = "🥊"
    ),
    CORE_PLANK(
        canonicalKey = "CORE_PLANK",
        displayName = "Core & Plank Mastery",
        category = ExerciseCategory.BODYWEIGHT,
        description = "Trunk stability, anti-extension, and isometric abdominal endurance.",
        icon = "🧱"
    ),
    RUNNING(
        canonicalKey = "RUNNING",
        displayName = "Running Mastery",
        category = ExerciseCategory.CARDIO,
        description = "High-impact aerobic capacity, sprint speed, and pacing control.",
        icon = "👟"
    ),
    CYCLING(
        canonicalKey = "CYCLING",
        displayName = "Cycling Mastery",
        category = ExerciseCategory.CARDIO,
        description = "Low-impact continuous output, cadence endurance, and leg stamina.",
        icon = "🚴"
    ),
    WALKING_HIKING(
        canonicalKey = "WALKING_HIKING",
        displayName = "Walking & Hiking Mastery",
        category = ExerciseCategory.CARDIO,
        description = "Zone-1 aerobic volume, elevation gain, and daily movement base.",
        icon = "🎒"
    ),
    FLEXIBILITY(
        canonicalKey = "FLEXIBILITY",
        displayName = "Flexibility & Yoga Mastery",
        category = ExerciseCategory.FLEXIBILITY,
        description = "Joint range of motion, muscle recovery, and mobility flows.",
        icon = "🧘"
    ),

    // Fallbacks for custom/unknown exercises by category
    STRENGTH_GENERAL(
        canonicalKey = "STRENGTH_GENERAL",
        displayName = "General Strength Mastery",
        category = ExerciseCategory.STRENGTH,
        description = "General resistance training and strength development.",
        icon = "🏋️"
    ),
    CARDIO_GENERAL(
        canonicalKey = "CARDIO_GENERAL",
        displayName = "General Cardio Mastery",
        category = ExerciseCategory.CARDIO,
        description = "General conditioning and cardiovascular activities.",
        icon = "🏃"
    ),
    BODYWEIGHT_GENERAL(
        canonicalKey = "BODYWEIGHT_GENERAL",
        displayName = "General Calisthenics Mastery",
        category = ExerciseCategory.BODYWEIGHT,
        description = "General gymnastic and bodyweight movement training.",
        icon = "🤸"
    ),
    FLEXIBILITY_GENERAL(
        canonicalKey = "FLEXIBILITY_GENERAL",
        displayName = "General Flexibility Mastery",
        category = ExerciseCategory.FLEXIBILITY,
        description = "General stretching and mobility exercises.",
        icon = "🌿"
    );

    companion object {
        fun fromKey(key: String): CanonicalMovement =
            entries.firstOrNull { it.canonicalKey.equals(key, ignoreCase = true) }
                ?: STRENGTH_GENERAL
    }
}
