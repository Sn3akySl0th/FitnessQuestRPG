package com.fitnessquest.rpg.domain.mastery

import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.SetLogEntity
import com.fitnessquest.rpg.domain.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MasteryProgressionTest {

    @Test
    fun `cumulativeXpForLevel locks in level 50 cap`() {
        assertEquals(0L, MasteryProgression.cumulativeXpForLevel(1))
        assertTrue(MasteryProgression.cumulativeXpForLevel(2) > 0L)
        assertTrue(MasteryProgression.cumulativeXpForLevel(50) > MasteryProgression.cumulativeXpForLevel(49))
        assertEquals(MasteryProgression.cumulativeXpForLevel(50), MasteryProgression.cumulativeXpForLevel(51))
    }

    @Test
    fun `levelForXp clamps between 1 and 50`() {
        assertEquals(1, MasteryProgression.levelForXp(0L))
        assertEquals(1, MasteryProgression.levelForXp(-100L))
        assertEquals(1, MasteryProgression.levelForXp(10L))
        
        val xpForLevel10 = MasteryProgression.cumulativeXpForLevel(10)
        assertEquals(10, MasteryProgression.levelForXp(xpForLevel10))
        assertEquals(10, MasteryProgression.levelForXp(xpForLevel10 + 50L))
        
        val xpForLevel50 = MasteryProgression.cumulativeXpForLevel(50)
        assertEquals(50, MasteryProgression.levelForXp(xpForLevel50))
        assertEquals(50, MasteryProgression.levelForXp(xpForLevel50 + 999_999L))
    }

    @Test
    fun `getLevelProgress returns valid bounds and percentage`() {
        val progress0 = MasteryProgression.getLevelProgress(0L)
        assertEquals(1, progress0.level)
        assertEquals(0L, progress0.currentLevelXp)
        assertTrue(progress0.nextLevelThresholdXp > 0L)
        assertEquals(0.0f, progress0.progressPercent, 0.001f)

        val maxProgress = MasteryProgression.getLevelProgress(MasteryProgression.cumulativeXpForLevel(50))
        assertEquals(50, maxProgress.level)
        assertEquals(1.0f, maxProgress.progressPercent, 0.001f)
    }

    @Test
    fun `calculateSetXp handles strength sets with quality multipliers`() {
        val normalSet = SetLogEntity(
            sessionId = 1L,
            exerciseName = "Barbell Squat",
            category = ExerciseCategory.STRENGTH,
            weightKg = 100.0,
            reps = 10,
            setType = SetType.NORMAL
        )
        val warmupSet = normalSet.copy(setType = SetType.WARM_UP)
        val dropSet = normalSet.copy(setType = SetType.DROP_SET)

        val normalXp = MasteryProgression.calculateSetXp(normalSet)
        val warmupXp = MasteryProgression.calculateSetXp(warmupSet)
        val dropSetXp = MasteryProgression.calculateSetXp(dropSet)

        assertTrue(normalXp in 5..50)
        assertTrue(warmupXp < normalXp)
        assertTrue(dropSetXp >= normalXp)
    }

    @Test
    fun `calculateSetXp handles bodyweight fallbacks when weight is zero`() {
        val pushupSet = SetLogEntity(
            sessionId = 1L,
            exerciseName = "Push-Up",
            category = ExerciseCategory.BODYWEIGHT,
            weightKg = 0.0,
            reps = 20,
            setType = SetType.NORMAL
        )
        val xpWithBodyweight = MasteryProgression.calculateSetXp(pushupSet, userBodyweightKg = 80.0)
        assertTrue(xpWithBodyweight in 5..50)
    }

    @Test
    fun `calculateSetXp handles cardio distance and duration`() {
        val runningSet = SetLogEntity(
            sessionId = 1L,
            exerciseName = "Outdoor Running",
            category = ExerciseCategory.CARDIO,
            distanceKm = 5.0,
            durationMin = 25.0
        )
        val xp = MasteryProgression.calculateSetXp(runningSet)
        // 5 * 15 + 25 * 2 = 75 + 50 = 125 -> capped at 100
        assertEquals(100, xp)

        val shortWalk = SetLogEntity(
            sessionId = 1L,
            exerciseName = "Walking",
            category = ExerciseCategory.CARDIO,
            distanceKm = 1.0,
            durationMin = 10.0
        )
        val shortXp = MasteryProgression.calculateSetXp(shortWalk)
        // 1 * 15 + 10 * 2 = 35
        assertEquals(35, shortXp)
    }

    @Test
    fun `calculateSetXp clamps extreme inputs safely`() {
        val crazySet = SetLogEntity(
            sessionId = 1L,
            exerciseName = "Crazy Squat",
            category = ExerciseCategory.STRENGTH,
            weightKg = 999_999.0,
            reps = 99_999
        )
        val clampedXp = MasteryProgression.calculateSetXp(crazySet)
        assertEquals(50, clampedXp)
    }
}
