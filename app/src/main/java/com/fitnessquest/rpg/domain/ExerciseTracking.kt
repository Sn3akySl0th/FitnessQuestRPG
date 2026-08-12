package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.ExerciseCategory

enum class ExerciseTrackingType(val label: String) {
    WEIGHT_REPS("Weight + reps"),
    REPS_ONLY("Reps only"),
    TIME_ONLY("Time only"),
    DISTANCE_TIME("Distance + time"),
    DISTANCE_ONLY("Distance only"),
    ASSISTED_REPS("Assisted reps"),
    BODYWEIGHT_REPS("Bodyweight reps"),
    CARDIO_MACHINE("Cardio machine")
}

object ExerciseTracking {
    fun resolve(
        name: String,
        category: ExerciseCategory,
        equipment: String = "",
        dbCategory: String? = null,
        primaryMuscles: List<String> = emptyList(),
        explicitTrackingType: String? = null
    ): ExerciseTrackingType {
        explicitTrackingType
            ?.trim()
            ?.uppercase()
            ?.let { raw -> runCatching { ExerciseTrackingType.valueOf(raw) }.getOrNull() }
            ?.let { return it }

        val n = name.lowercase()
        val eq = equipment.lowercase()
        val db = dbCategory?.lowercase().orEmpty()
        val muscles = primaryMuscles.joinToString(" ").lowercase()

        if (isFlexibility(n, eq, db)) return ExerciseTrackingType.TIME_ONLY
        if (isDistanceOnly(n, eq)) return ExerciseTrackingType.DISTANCE_ONLY
        if (isCardioMachine(n, eq)) return ExerciseTrackingType.CARDIO_MACHINE
        if (category == ExerciseCategory.CARDIO || db == "cardio") return ExerciseTrackingType.DISTANCE_TIME
        if (isAssisted(n, eq)) return ExerciseTrackingType.ASSISTED_REPS
        if (isTimedStrength(n, eq, muscles)) return ExerciseTrackingType.TIME_ONLY
        if (category == ExerciseCategory.BODYWEIGHT || isBodyweight(n, eq)) return ExerciseTrackingType.BODYWEIGHT_REPS
        if (isRepsOnly(n, eq)) return ExerciseTrackingType.REPS_ONLY
        return ExerciseTrackingType.WEIGHT_REPS
    }

    private fun isFlexibility(name: String, equipment: String, dbCategory: String): Boolean =
        dbCategory == "stretching" ||
            equipment.contains("foam") ||
            name.contains("stretch") ||
            name.contains("yoga") ||
            name.contains("mobility") ||
            name.contains("pilates") ||
            name.contains("foam roll")

    private fun isDistanceOnly(name: String, equipment: String): Boolean =
        name.contains("distance only") || equipment.contains("gps")

    private fun isCardioMachine(name: String, equipment: String): Boolean {
        val cardioMachineNames = listOf(
            "treadmill", "elliptical", "stair", "stepmill", "stairmaster",
            "rower", "rowing machine", "stationary bike", "recumbent bike",
            "air bike", "assault bike", "ski erg", "skierg"
        )
        return cardioMachineNames.any { name.contains(it) || equipment.contains(it) }
    }

    private fun isAssisted(name: String, equipment: String): Boolean =
        name.contains("assisted") ||
            equipment.contains("assisted") ||
            name.contains("gravitron")

    private fun isTimedStrength(name: String, equipment: String, muscles: String): Boolean =
        name.contains("plank") ||
            name.contains("hold") ||
            name.contains("isometric") ||
            name.contains("dead hang") ||
            name.contains("wall sit") ||
            (equipment.contains("mat") && muscles.contains("core") && name.contains("pose"))

    private fun isBodyweight(name: String, equipment: String): Boolean =
        equipment == "body only" ||
            equipment == "none" ||
            name.contains("push-up") ||
            name.contains("pushup") ||
            name.contains("pull-up") ||
            name.contains("pullup") ||
            name.contains("chin-up") ||
            name.contains("chinup") ||
            name.contains("burpee") ||
            name.contains("sit-up") ||
            name.contains("mountain climber")

    private fun isRepsOnly(name: String, equipment: String): Boolean =
        equipment.contains("body") ||
            name.contains("crunch") && !equipment.contains("machine") ||
            name.contains("calisthenic")
}
