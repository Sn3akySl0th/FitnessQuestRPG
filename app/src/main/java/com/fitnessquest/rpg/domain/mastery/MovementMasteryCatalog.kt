package com.fitnessquest.rpg.domain.mastery

import com.fitnessquest.rpg.data.db.ExerciseCategory

/**
 * Resolves exercise names (e.g. "Barbell Back Squat", "Front Squat", "Leg Press") to their
 * high-level CanonicalMovement pattern, with resilient fallbacks for custom or unknown exercises.
 */
object MovementMasteryCatalog {

    /**
     * Resolves an exercise name and category to its canonical movement.
     */
    fun resolve(exerciseName: String, category: ExerciseCategory): CanonicalMovement {
        val normalized = exerciseName.trim().lowercase()

        // 1. Specific string matching
        when {
            // Squats & Knee-dominant
            normalized.contains("squat") ||
                normalized.contains("leg press") ||
                normalized.contains("hack squat") ||
                normalized.contains("lunge") ||
                normalized.contains("split squat") ||
                normalized.contains("step up") ||
                normalized.contains("leg extension") -> return CanonicalMovement.SQUAT

            // Bench Press & Horizontal Chest Push
            normalized.contains("bench press") ||
                normalized.contains("chest press") ||
                normalized.contains("dumbbell press") ||
                normalized.contains("db press") ||
                normalized.contains("floor press") ||
                normalized.contains("pec fly") ||
                normalized.contains("cable fly") ||
                normalized.contains("chest fly") -> return CanonicalMovement.BENCH_PRESS

            // Deadlift & Hip Hinge
            normalized.contains("deadlift") ||
                normalized.contains("rdl") ||
                normalized.contains("good morning") ||
                normalized.contains("hip thrust") ||
                normalized.contains("glute bridge") ||
                normalized.contains("back extension") ||
                normalized.contains("hyperextension") ||
                normalized.contains("leg curl") ||
                normalized.contains("hamstring curl") -> return CanonicalMovement.DEADLIFT

            // Overhead Press & Shoulders
            normalized.contains("overhead press") ||
                normalized.contains("shoulder press") ||
                normalized.contains("military press") ||
                normalized.contains("ohp") ||
                normalized.contains("arnold press") ||
                normalized.contains("push press") ||
                normalized.contains("lateral raise") ||
                normalized.contains("front raise") ||
                normalized.contains("face pull") ||
                normalized.contains("upright row") -> return CanonicalMovement.OVERHEAD_PRESS

            // Pull-Ups & Rows & Vertical/Horizontal Pull
            normalized.contains("pull up") ||
                normalized.contains("pull-up") ||
                normalized.contains("pullup") ||
                normalized.contains("chin up") ||
                normalized.contains("chin-up") ||
                normalized.contains("chinup") ||
                normalized.contains("lat pulldown") ||
                normalized.contains("pulldown") ||
                normalized.contains("row") ||
                normalized.contains("shrug") -> return CanonicalMovement.PULL_UP

            // Dips & Pushups (Bodyweight / Calisthenics Push)
            normalized.contains("push up") ||
                normalized.contains("push-up") ||
                normalized.contains("pushup") ||
                normalized.contains("dip") ||
                normalized.contains("muscle up") ||
                normalized.contains("muscle-up") ||
                normalized.contains("handstand") -> return CanonicalMovement.DIP_PUSHUP

            // Core & Plank
            normalized.contains("plank") ||
                normalized.contains("crunch") ||
                normalized.contains("sit up") ||
                normalized.contains("sit-up") ||
                normalized.contains("situp") ||
                normalized.contains("leg raise") ||
                normalized.contains("ab wheel") ||
                normalized.contains("rollout") ||
                normalized.contains("russian twist") ||
                normalized.contains("hollow body") ||
                normalized.contains("vacuum") ||
                normalized.contains("dead bug") -> return CanonicalMovement.CORE_PLANK

            // Running
            normalized.contains("run") ||
                normalized.contains("jog") ||
                normalized.contains("sprint") ||
                normalized.contains("treadmill") -> return CanonicalMovement.RUNNING

            // Cycling
            normalized.contains("cycle") ||
                normalized.contains("cycling") ||
                normalized.contains("bike") ||
                normalized.contains("biking") ||
                normalized.contains("spin") ||
                normalized.contains("assault bike") ||
                normalized.contains("airdyne") -> return CanonicalMovement.CYCLING

            // Walking & Hiking
            normalized.contains("walk") ||
                normalized.contains("walking") ||
                normalized.contains("hike") ||
                normalized.contains("hiking") ||
                normalized.contains("ruck") ||
                normalized.contains("rucking") ||
                normalized.contains("stair") ||
                normalized.contains("elliptical") ||
                normalized.contains("rowing machine") ||
                normalized.contains("erg") -> return CanonicalMovement.WALKING_HIKING

            // Flexibility & Recovery
            normalized.contains("yoga") ||
                normalized.contains("stretch") ||
                normalized.contains("stretching") ||
                normalized.contains("mobility") ||
                normalized.contains("foam roll") -> return CanonicalMovement.FLEXIBILITY
        }

        // 2. Category-based fallback for unknown or custom exercises
        return when (category) {
            ExerciseCategory.STRENGTH -> CanonicalMovement.STRENGTH_GENERAL
            ExerciseCategory.CARDIO -> CanonicalMovement.CARDIO_GENERAL
            ExerciseCategory.BODYWEIGHT -> CanonicalMovement.BODYWEIGHT_GENERAL
            ExerciseCategory.FLEXIBILITY -> CanonicalMovement.FLEXIBILITY_GENERAL
        }
    }
}
