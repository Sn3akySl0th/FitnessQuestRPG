package com.fitnessquest.rpg.data.importexport

import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.domain.ExerciseCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutImportServiceTest {

    @Test
    fun isoLateralRowMachine_infersAsStrength_notCardio() {
        val category = ExerciseCategories.infer("Iso-Lateral Row (Machine)")
        assertEquals(
            "Iso-Lateral Row (Machine) must be classified as STRENGTH to avoid duration mapping",
            ExerciseCategory.STRENGTH,
            category
        )
    }

    @Test
    fun strengthRowVariations_inferAsStrength() {
        val variations = listOf(
            "Chest Supported Row (Machine)",
            "Seated Cable Row",
            "T-Bar Row",
            "Lever Row (Machine)",
            "Smith Machine Row",
            "Dumbbell Incline Row",
            "Machine Row"
        )
        for (name in variations) {
            val category = ExerciseCategories.infer(name)
            assertEquals("$name should be STRENGTH", ExerciseCategory.STRENGTH, category)
        }
    }

    @Test
    fun pureCardioExercises_inferAsCardio() {
        val cardioList = listOf(
            "Treadmill Walking",
            "Indoor Rowing",
            "Rowing Machine",
            "Row Erg",
            "Elliptical Trainer",
            "Stationary Bike",
            "Outdoor Running"
        )
        for (name in cardioList) {
            val category = ExerciseCategories.infer(name)
            assertEquals("$name should be CARDIO", ExerciseCategory.CARDIO, category)
        }
    }

    @Test
    fun toSessionEntity_generatesStableHevyCompletionToken() {
        val imported = ImportedWorkout(
            title = "Upper 1",
            exercises = emptyList(),
            kind = ImportedWorkoutKind.HISTORY,
            startedAt = 1723456789000L,
            endedAt = 1723460389000L,
            externalId = "hevy_workout_abc_123"
        )
        val entity = WorkoutImportService.toSessionEntity(imported)
        assertEquals("hevy_api_hevy_workout_abc_123", entity.completionToken)
    }

    @Test
    fun toSessionEntity_withoutExternalId_generatesDeterministicToken() {
        val imported = ImportedWorkout(
            title = "Leg Day Blitz",
            exercises = emptyList(),
            kind = ImportedWorkoutKind.HISTORY,
            startedAt = 1723456789000L,
            endedAt = 1723460389000L
        )
        val entity1 = WorkoutImportService.toSessionEntity(imported)
        val entity2 = WorkoutImportService.toSessionEntity(imported)
        assertNotNull(entity1.completionToken)
        assertEquals(entity1.completionToken, entity2.completionToken)
    }
}
