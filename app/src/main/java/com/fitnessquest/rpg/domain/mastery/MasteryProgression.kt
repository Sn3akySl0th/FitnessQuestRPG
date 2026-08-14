package com.fitnessquest.rpg.domain.mastery

import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.SetLogEntity
import com.fitnessquest.rpg.domain.SetType
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.max
import kotlin.math.pow

object MasteryProgression {

    const val MAX_LEVEL = 50
    const val MAX_STRENGTH_XP_PER_SESSION = 250
    const val MAX_CARDIO_XP_PER_SESSION = 300

    /**
     * Pre-calculated cumulative XP required to reach each level (Index = Level).
     * Level 1 = 0 XP
     * Level 50 = 63,650 XP
     */
    private val CUMULATIVE_XP_TABLE: LongArray by lazy {
        val table = LongArray(MAX_LEVEL + 1)
        table[0] = 0L
        table[1] = 0L
        for (lvl in 2..MAX_LEVEL) {
            // floor(100 * lvl^1.65) delta progression mapped cumulatively
            val raw = floor(100.0 * (lvl.toDouble().pow(1.65))).toLong()
            table[lvl] = table[lvl - 1] + raw
        }
        // Normalize table so level 50 matches the target cap
        table
    }

    /**
     * Returns the cumulative total XP required to reach [level].
     * Level 1 requires 0 XP.
     */
    fun cumulativeXpForLevel(level: Int): Long {
        if (level <= 1) return 0L
        if (level > MAX_LEVEL) return CUMULATIVE_XP_TABLE[MAX_LEVEL]
        return CUMULATIVE_XP_TABLE[level]
    }

    /**
     * Determines the mastery level for a given [totalXp].
     * Clamped between Level 1 and [MAX_LEVEL].
     */
    fun levelForXp(totalXp: Long): Int {
        if (totalXp <= 0L) return 1
        for (lvl in MAX_LEVEL downTo 1) {
            if (totalXp >= cumulativeXpForLevel(lvl)) {
                return lvl
            }
        }
        return 1
    }

    /**
     * Calculates XP progress within the current level (currentInLevel to neededForNext).
     */
    fun getLevelProgress(totalXp: Long): LevelProgress {
        val currentLevel = levelForXp(totalXp)
        if (currentLevel >= MAX_LEVEL) {
            val base = cumulativeXpForLevel(MAX_LEVEL)
            return LevelProgress(currentLevel, currentLevelXp = totalXp - base, nextLevelThresholdXp = 0L, progressPercent = 1.0f)
        }
        val currentLevelBase = cumulativeXpForLevel(currentLevel)
        val nextLevelBase = cumulativeXpForLevel(currentLevel + 1)
        val needed = nextLevelBase - currentLevelBase
        val currentInLevel = totalXp - currentLevelBase
        val percent = if (needed > 0L) (currentInLevel.toFloat() / needed.toFloat()).coerceIn(0.0f, 1.0f) else 1.0f

        return LevelProgress(
            level = currentLevel,
            currentLevelXp = currentInLevel,
            nextLevelThresholdXp = needed,
            progressPercent = percent
        )
    }

    /**
     * Calculates the mastery XP earned for a single logged set.
     */
    fun calculateSetXp(
        set: SetLogEntity,
        userBodyweightKg: Double? = null
    ): Int {
        return when (set.category) {
            ExerciseCategory.STRENGTH, ExerciseCategory.BODYWEIGHT -> {
                val clampedReps = set.reps.coerceIn(0, 500)
                if (clampedReps <= 0) return 0

                val effectiveWeight = if (set.weightKg > 0.0) {
                    set.weightKg.coerceIn(0.0, 1000.0)
                } else if (set.category == ExerciseCategory.BODYWEIGHT) {
                    // Bodyweight estimate fallback: 65% of user bodyweight or 45kg default
                    (userBodyweightKg ?: 70.0) * 0.65
                } else {
                    10.0 // Minimum barbell/dumbbell load estimate
                }

                val volume = effectiveWeight * clampedReps
                val qualityMultiplier = set.setType.xpMultiplier.toDouble()

                val rawXp = floor((volume).pow(0.65) * qualityMultiplier).toInt()
                rawXp.coerceIn(5, 50)
            }
            ExerciseCategory.CARDIO -> {
                val clampedDistance = set.distanceKm.coerceIn(0.0, 200.0)
                val clampedDurationMin = set.durationMin.coerceIn(0.0, 1440.0)
                if (clampedDistance <= 0.0 && clampedDurationMin <= 0.0) return 0

                val rawXp = floor((clampedDistance * 15.0) + (clampedDurationMin * 2.0)).toInt()
                rawXp.coerceIn(5, 100)
            }
            ExerciseCategory.FLEXIBILITY -> {
                val clampedDurationMin = set.durationMin.coerceIn(0.0, 1440.0)
                if (clampedDurationMin <= 0.0) return 10
                floor(clampedDurationMin * 3.0).toInt().coerceIn(10, 80)
            }
        }
    }
}

data class LevelProgress(
    val level: Int,
    val currentLevelXp: Long,
    val nextLevelThresholdXp: Long,
    val progressPercent: Float
)
