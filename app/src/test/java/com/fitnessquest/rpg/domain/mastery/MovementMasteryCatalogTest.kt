package com.fitnessquest.rpg.domain.mastery

import com.fitnessquest.rpg.data.db.ExerciseCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class MovementMasteryCatalogTest {

    @Test
    fun `resolves canonical lifts accurately`() {
        assertEquals(CanonicalMovement.SQUAT, MovementMasteryCatalog.resolve("Barbell Back Squat", ExerciseCategory.STRENGTH))
        assertEquals(CanonicalMovement.SQUAT, MovementMasteryCatalog.resolve("Leg Press (45 Degree)", ExerciseCategory.STRENGTH))
        assertEquals(CanonicalMovement.BENCH_PRESS, MovementMasteryCatalog.resolve("Incline Dumbbell Press", ExerciseCategory.STRENGTH))
        assertEquals(CanonicalMovement.DEADLIFT, MovementMasteryCatalog.resolve("Romanian Deadlift (Barbell)", ExerciseCategory.STRENGTH))
        assertEquals(CanonicalMovement.OVERHEAD_PRESS, MovementMasteryCatalog.resolve("Seated OHP", ExerciseCategory.STRENGTH))
        assertEquals(CanonicalMovement.PULL_UP, MovementMasteryCatalog.resolve("Lat Pulldown (Cable)", ExerciseCategory.STRENGTH))
        assertEquals(CanonicalMovement.DIP_PUSHUP, MovementMasteryCatalog.resolve("Weighted Dip", ExerciseCategory.BODYWEIGHT))
        assertEquals(CanonicalMovement.CORE_PLANK, MovementMasteryCatalog.resolve("Ab Wheel Rollout", ExerciseCategory.BODYWEIGHT))
        assertEquals(CanonicalMovement.RUNNING, MovementMasteryCatalog.resolve("Treadmill Run", ExerciseCategory.CARDIO))
        assertEquals(CanonicalMovement.CYCLING, MovementMasteryCatalog.resolve("Stationary Bike", ExerciseCategory.CARDIO))
        assertEquals(CanonicalMovement.WALKING_HIKING, MovementMasteryCatalog.resolve("Outdoor Walking", ExerciseCategory.CARDIO))
        assertEquals(CanonicalMovement.FLEXIBILITY, MovementMasteryCatalog.resolve("Morning Yoga Flow", ExerciseCategory.FLEXIBILITY))
    }

    @Test
    fun `falls back gracefully for custom and unknown exercises`() {
        assertEquals(CanonicalMovement.STRENGTH_GENERAL, MovementMasteryCatalog.resolve("Alien Gravity Lift", ExerciseCategory.STRENGTH))
        assertEquals(CanonicalMovement.CARDIO_GENERAL, MovementMasteryCatalog.resolve("Space Shuttle Jump", ExerciseCategory.CARDIO))
        assertEquals(CanonicalMovement.BODYWEIGHT_GENERAL, MovementMasteryCatalog.resolve("Ninja Leap", ExerciseCategory.BODYWEIGHT))
        assertEquals(CanonicalMovement.FLEXIBILITY_GENERAL, MovementMasteryCatalog.resolve("Dragon Chi Breathing", ExerciseCategory.FLEXIBILITY))
    }
}
