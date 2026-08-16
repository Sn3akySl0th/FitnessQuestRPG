package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.ExerciseCategory

/**
 * Maps free-exercise-db metadata / exercise names onto FitQuest's
 * [ExerciseCategory] (which drives logging UI and STR/END/… XP).
 *
 * Cardio machines in the DB often have `equipment: "machine"` and names like
 * "Walking, Treadmill" — without this, they were misclassified as STRENGTH.
 */
object ExerciseCategories {

    fun fromDbCategory(raw: String?): ExerciseCategory? = when (raw?.trim()?.lowercase()) {
        "cardio" -> ExerciseCategory.CARDIO
        "stretching" -> ExerciseCategory.FLEXIBILITY
        "strength", "powerlifting", "olympic weightlifting", "strongman" ->
            ExerciseCategory.STRENGTH
        "plyometrics" -> ExerciseCategory.BODYWEIGHT
        else -> null
    }

    /**
     * Infer category from name + optional DB fields.
     * Prefer the DB's own category when present.
     */
    fun infer(
        name: String,
        equipment: String = "",
        dbCategory: String? = null
    ): ExerciseCategory {
        fromDbCategory(dbCategory)?.let { return it }

        val n = name.lowercase()
        val eq = equipment.trim().lowercase()

        if (looksLikeCardio(n, eq)) return ExerciseCategory.CARDIO
        if (looksLikeFlexibility(n, eq)) return ExerciseCategory.FLEXIBILITY
        if (looksLikeBodyweight(n, eq)) return ExerciseCategory.BODYWEIGHT
        return ExerciseCategory.STRENGTH
    }

    /**
     * Correct a category already stored on a workout / session.
     * Always upgrades mis-tagged cardio machines to CARDIO so logging uses
     * duration/distance and XP goes to END.
     */
    fun resolveStored(name: String, stored: ExerciseCategory): ExerciseCategory {
        val inferred = infer(name)
        return when {
            inferred == ExerciseCategory.CARDIO -> ExerciseCategory.CARDIO
            inferred == ExerciseCategory.FLEXIBILITY &&
                stored == ExerciseCategory.STRENGTH -> ExerciseCategory.FLEXIBILITY
            else -> stored
        }
    }

    /**
     * AI / free-text coercion: honor an explicit enum when sensible, but
     * override when the name is clearly cardio (or flexibility mislabeled as strength).
     */
    fun coerce(rawCategory: String?, exerciseName: String): ExerciseCategory {
        val parsed = rawCategory?.trim()?.uppercase()?.let {
            runCatching { ExerciseCategory.valueOf(it) }.getOrNull()
        }
        val inferred = infer(exerciseName)
        if (inferred == ExerciseCategory.CARDIO) return ExerciseCategory.CARDIO
        if (inferred == ExerciseCategory.FLEXIBILITY &&
            (parsed == null || parsed == ExerciseCategory.STRENGTH)
        ) {
            return ExerciseCategory.FLEXIBILITY
        }
        return parsed ?: inferred
    }

    private fun looksLikeCardio(name: String, equipment: String): Boolean {
        val strengthCues = listOf(
            "iso-lateral", "isolateral", "seated row", "cable row", "chest supported",
            "t-bar", "t bar", "smith", "lever row", "dumbbell row", "barbell row",
            "machine row", "lat pulldown", "pulldown", "bench",
            "press", "curl", "extension", "squat", "deadlift", "shrug",
            "fly", "flye", "raise", "lunge"
        ).any { name.contains(it) }

        if (strengthCues) return false

        val machineCue = listOf(
            "treadmill", "elliptical", "stairmaster", "step mill", "stepmill",
            "recumbent bike", "bicycling", "rope jumping", "jump rope",
            "prowler", "trail running", "skating", "stair", "cycling",
            "swimming", "swim", "hiking", "hike", "air bike", "assault bike",
            "ski erg", "skierg"
        ).any { name.contains(it) }

        val rowErg = (name == "rowing" || 
            name.contains("rowing machine") || 
            name.contains("rower") || 
            name.contains("row erg") || 
            name.contains("indoor rower") || 
            name.contains("water rower") ||
            (name.contains("row") && name.contains("erg"))) && !name.contains("machine row")

        val bike = name.contains("bike") && !name.contains("ab ")

        // Gait cardio (walk / jog / run) — exclude loaded carries & lunges.
        val strengthGait = listOf(
            "farmer", "lunge", "dumbbell", "barbell", "kettle", "sled",
            "rack", "plate", "trap bar", "yoke"
        ).any { name.contains(it) }
        val gait = !strengthGait && (
            name.contains("jog") ||
                name.contains("running") ||
                name == "run" ||
                name.startsWith("run ") ||
                name.contains("walking") ||
                name.startsWith("walk") ||
                name.contains(" walk") ||
                name.contains("treadmill")
            )

        if (machineCue || rowErg || bike || gait) return true

        if (EquipmentCatalog.all.any {
                it.family == EquipmentFamily.CARDIO &&
                    EquipmentCatalog.matches(it, name, equipment)
            }
        ) {
            return true
        }

        return equipment.contains("treadmill") ||
            equipment.contains("elliptical") ||
            equipment == "bike" ||
            equipment.contains("cardio")
    }

    private fun looksLikeFlexibility(name: String, equipment: String): Boolean =
        equipment.contains("foam") ||
            name.contains("stretch") ||
            name.contains("yoga") ||
            name.contains("mobility") ||
            name.contains("pilates") ||
            name.contains("foam roll")

    private fun looksLikeBodyweight(name: String, equipment: String): Boolean =
        equipment == "body only" ||
            (equipment.contains("body") && !equipment.contains("barbell")) ||
            name.contains("push-up") || name.contains("pushup") ||
            name.contains("pull-up") || name.contains("pullup") ||
            name.contains("chin-up") || name.contains("chinup") ||
            name.contains("plank") ||
            name.contains("bodyweight") ||
            name.contains("burpee")
}
