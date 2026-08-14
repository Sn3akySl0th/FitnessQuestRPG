package com.fitnessquest.rpg.domain.mastery

import com.fitnessquest.rpg.data.db.ExerciseCategory

/**
 * High-level canonical movement patterns that aggregate specific exercises.
 */
enum class CanonicalMovement(
    val canonicalKey: String,
    val displayName: String,
    val category: ExerciseCategory,
    val description: String
) {
    SQUAT(
        canonicalKey = "SQUAT",
        displayName = "Squat Mastery",
        category = ExerciseCategory.STRENGTH,
        description = "Knee-dominant lower body strength, quad and glute power."
    ),
    BENCH_PRESS(
        canonicalKey = "BENCH_PRESS",
        displayName = "Bench Press Mastery",
        category = ExerciseCategory.STRENGTH,
        description = "Horizontal upper body pushing, chest and triceps force."
    ),
    DEADLIFT(
        canonicalKey = "DEADLIFT",
        displayName = "Deadlift Mastery",
        category = ExerciseCategory.STRENGTH,
        description = "Hip-hinge posterior chain power, back and hamstring durability."
    ),
    OVERHEAD_PRESS(
        canonicalKey = "OVERHEAD_PRESS",
        displayName = "Overhead Press Mastery",
        category = ExerciseCategory.STRENGTH,
        description = "Vertical pushing strength, deltoids and shoulder stability."
    ),
    PULL_UP(
        canonicalKey = "PULL_UP",
        displayName = "Pull-Up & Row Mastery",
        category = ExerciseCategory.STRENGTH,
        description = "Upper body pulling strength, lats and upper back control."
    ),
    DIP_PUSHUP(
        canonicalKey = "DIP_PUSHUP",
        displayName = "Bodyweight Push Mastery",
        category = ExerciseCategory.BODYWEIGHT,
        description = "Calisthenic pushing mastery, core and tricep endurance."
    ),
    CORE_PLANK(
        canonicalKey = "CORE_PLANK",
        displayName = "Core & Stability Mastery",
        category = ExerciseCategory.BODYWEIGHT,
        description = "Isometric trunk stability, ab bracing, and spinal endurance."
    ),
    RUNNING(
        canonicalKey = "RUNNING",
        displayName = "Running Mastery",
        category = ExerciseCategory.CARDIO,
        description = "Aerobic endurance, stamina, and cardiovascular pacing."
    ),
    CYCLING(
        canonicalKey = "CYCLING",
        displayName = "Cycling Mastery",
        category = ExerciseCategory.CARDIO,
        description = "Sustained leg speed, endurance cadence, and cardiovascular output."
    ),
    WALKING_HIKING(
        canonicalKey = "WALKING_HIKING",
        displayName = "Walking & Rucking Mastery",
        category = ExerciseCategory.CARDIO,
        description = "Base conditioning, long-distance active recovery, and pack fortitude."
    ),
    FLEXIBILITY(
        canonicalKey = "FLEXIBILITY",
        displayName = "Mobility & Recovery Mastery",
        category = ExerciseCategory.FLEXIBILITY,
        description = "Full-body range of motion, muscle balance, and rejuvenation."
    ),

    // Fallbacks for custom/unknown exercises by category
    STRENGTH_GENERAL(
        canonicalKey = "STRENGTH_GENERAL",
        displayName = "General Strength Mastery",
        category = ExerciseCategory.STRENGTH,
        description = "General resistance training and strength development."
    ),
    CARDIO_GENERAL(
        canonicalKey = "CARDIO_GENERAL",
        displayName = "General Cardio Mastery",
        category = ExerciseCategory.CARDIO,
        description = "General conditioning and cardiovascular activities."
    ),
    BODYWEIGHT_GENERAL(
        canonicalKey = "BODYWEIGHT_GENERAL",
        displayName = "General Calisthenics Mastery",
        category = ExerciseCategory.BODYWEIGHT,
        description = "General gymnastic and bodyweight movement training."
    ),
    FLEXIBILITY_GENERAL(
        canonicalKey = "FLEXIBILITY_GENERAL",
        displayName = "General Flexibility Mastery",
        category = ExerciseCategory.FLEXIBILITY,
        description = "General stretching and mobility exercises."
    );

    companion object {
        fun fromKey(key: String): CanonicalMovement =
            entries.firstOrNull { it.canonicalKey.equals(key, ignoreCase = true) }
                ?: STRENGTH_GENERAL
    }
}
