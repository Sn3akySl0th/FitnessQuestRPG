package com.fitnessquest.rpg.domain

/** Training gear the player has access to. */
enum class Equipment(val label: String) {
    BODYWEIGHT("Bodyweight"),
    DUMBBELLS("Dumbbells"),
    BARBELL("Barbell"),
    KETTLEBELL("Kettlebell"),
    MACHINES("Gym machines"),
    BANDS("Resistance bands"),
    CARDIO_GEAR("Treadmill / bike")
}

enum class Split(val label: String) {
    FULL_BODY("Full-body"),
    UPPER_LOWER("Upper / Lower"),
    PUSH_PULL_LEGS("Push / Pull / Legs")
}

enum class FitnessLevel(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced")
}

/** Top fitness goal captured during Play onboarding. */
enum class FitnessGoal(val label: String, val blurb: String) {
    BUILD_MUSCLE("Build muscle", "Add size with progressive overload"),
    GAIN_STRENGTH("Gain strength", "Get stronger on big lifts"),
    FAT_LOSS("Fat loss", "Train hard and stay consistent")
}

/** Optional muscle emphasis for the starter quest. */
enum class MuscleFocus(val label: String) {
    BALANCED("Balanced program"),
    ABS("Abs"),
    BICEPS("Biceps"),
    SHOULDERS("Shoulders"),
    UPPER_BACK("Upper Back"),
    QUADRICEPS("Quadriceps"),
    CHEST("Chest")
}

/** Where cardio sits in the starter session. */
enum class CardioPlacement(val label: String) {
    NONE("No cardio"),
    BEGINNING("Yes, at the beginning"),
    END("Yes, at the end")
}

/** Target length for the first quest. */
enum class WorkoutDurationMins(val minutes: Int, val label: String) {
    SHORT(30, "30 min"),
    STANDARD(40, "40 min Recommended"),
    LONG(60, "60 min")
}

/**
 * How the player rates set difficulty. Both scales measure the same thing:
 * RIR counts reps left in the tank, RPE rates exertion out of 10 (RPE = 10 - RIR).
 * Effort is always stored as RIR; the method only changes what the UI shows.
 */
enum class EffortMethod(val label: String, val description: String) {
    OFF("Off", "No effort tracking"),
    RIR("RIR", "Reps In Reserve: how many more reps you could have done (0 = none left)"),
    RPE("RPE", "Rate of Perceived Exertion: how hard the set felt out of 10 (10 = max effort)");

    /** Display value for a stored RIR under this method. */
    fun display(rir: Int): String = when (this) {
        RPE -> "RPE ${10 - rir}"
        else -> "RIR ${if (rir >= 5) "5+" else rir.toString()}"
    }
}

/**
 * The player's persistent training preferences. Fed into every AI workout
 * generation so results match their gear, schedule, and experience.
 */
data class TrainingProfile(
    val equipment: Set<Equipment> = setOf(Equipment.BODYWEIGHT),
    val daysPerWeek: Int = 3,
    val split: Split = Split.FULL_BODY,
    val level: FitnessLevel = FitnessLevel.BEGINNER
) {
    val recommendedSplit: Split
        get() = when {
            daysPerWeek >= 5 -> Split.PUSH_PULL_LEGS
            daysPerWeek >= 4 -> Split.UPPER_LOWER
            else -> Split.FULL_BODY
        }

    /** Compact description used inside the Gemini prompt. */
    fun promptSummary(): String = buildString {
        appendLine("- Available equipment: ${equipment.joinToString { it.label }}")
        appendLine("- Fitness level: ${level.label}")
        append("- Trains ${daysPerWeek}x per week following a ${split.label} split.")
    }
}

/** Unit conversion helpers. Weights are always stored in kg, distances in km. */
object Units {
    private const val LB_PER_KG = 2.2046226
    private const val MI_PER_KM = 0.62137119

    fun toDisplay(kg: Double, imperial: Boolean): Double = if (imperial) kg * LB_PER_KG else kg

    fun toKg(display: Double, imperial: Boolean): Double = if (imperial) display / LB_PER_KG else display

    fun label(imperial: Boolean): String = if (imperial) "lb" else "kg"

    fun formatWeight(kg: Double, imperial: Boolean): String =
        "${trimmed(toDisplay(kg, imperial))} ${label(imperial)}"

    fun kmToDisplay(km: Double, imperial: Boolean): Double = if (imperial) km * MI_PER_KM else km

    fun toKm(display: Double, imperial: Boolean): Double = if (imperial) display / MI_PER_KM else display

    fun distLabel(imperial: Boolean): String = if (imperial) "mi" else "km"

    fun formatDistance(km: Double, imperial: Boolean): String =
        "${trimmed(kmToDisplay(km, imperial))} ${distLabel(imperial)}"

    fun speedLabel(imperial: Boolean): String = if (imperial) "mph" else "km/h"

    fun speedToDisplay(kmh: Double, imperial: Boolean): Double =
        if (imperial) kmh * MI_PER_KM else kmh

    fun toSpeedKmh(display: Double, imperial: Boolean): Double =
        if (imperial) display / MI_PER_KM else display

    fun formatSpeed(kmh: Double, imperial: Boolean): String =
        "${trimmed(speedToDisplay(kmh, imperial))} ${speedLabel(imperial)}"

    fun formatPace(minsPerKm: Double, imperial: Boolean): String {
        val displayPace = if (imperial) minsPerKm / 0.62137119 else minsPerKm
        val mins = displayPace.toInt()
        val secs = ((displayPace - mins) * 60).toInt()
        return "%d:%02d /%s".format(mins, secs, distLabel(imperial))
    }

    fun formatTimeMinutes(mins: Double): String {
        val totalSeconds = (mins * 60).toInt()
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return if (s == 0) "${m}m" else "${m}m ${s}s"
    }

    /** Rounds a kg weight to the nearest loadable increment (2.5 kg or 5 lb). */
    fun roundToPlate(kg: Double, imperial: Boolean): Double =
        if (imperial) {
            Math.round(kg * LB_PER_KG / 5.0) * 5.0 / LB_PER_KG
        } else {
            Math.round(kg / 2.5) * 2.5
        }

    fun trimmed(value: Double): String =
        if (value % 1.0 < 0.05 || value % 1.0 > 0.95) {
            Math.round(value).toString()
        } else {
            "%.1f".format(value)
        }
}
