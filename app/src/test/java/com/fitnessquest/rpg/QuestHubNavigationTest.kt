package com.fitnessquest.rpg

import com.fitnessquest.rpg.data.ai.RoutineRecommendation
import com.fitnessquest.rpg.data.db.WorkoutEntity
import com.fitnessquest.rpg.domain.Biome
import com.fitnessquest.rpg.domain.MonsterCatalog
import com.fitnessquest.rpg.ui.screens.ActiveQuestSnapshot
import com.fitnessquest.rpg.ui.screens.QuestHubAction
import com.fitnessquest.rpg.ui.screens.buildQuestHubState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuestHubNavigationTest {
    private val biome = Biome.MEADOWLANDS
    private val encounter = MonsterCatalog.regularMonstersByBiome(biome).first()

    @Test
    fun `recommended routine starts directly from the hub`() {
        val state = buildQuestHubState(
            recommendation = RoutineRecommendation(
                routine = WorkoutEntity(id = 42L, name = "Upper Body Expedition"),
                readinessPercent = 91
            ),
            activeQuest = null,
            recommendedExerciseCount = 5,
            biome = biome,
            encounter = encounter
        )

        assertEquals(QuestHubAction.BEGIN_QUEST, state.action)
        assertEquals(42L, state.workoutId)
        assertEquals("Begin Quest", state.primaryLabel)
    }

    @Test
    fun `active session always takes precedence and resumes directly`() {
        val state = buildQuestHubState(
            recommendation = RoutineRecommendation(
                routine = WorkoutEntity(id = 42L, name = "Ignored Recommendation")
            ),
            activeQuest = ActiveQuestSnapshot(
                title = "Dungeon Strength Run",
                workoutId = 7L,
                exerciseCount = 4
            ),
            recommendedExerciseCount = 5,
            biome = biome,
            encounter = encounter
        )

        assertEquals(QuestHubAction.RESUME_QUEST, state.action)
        assertEquals(7L, state.workoutId)
        assertEquals("Resume Quest", state.primaryLabel)
    }

    @Test
    fun `rest day remains advisory and opens manual training`() {
        val state = buildQuestHubState(
            recommendation = RoutineRecommendation(
                routine = null,
                isRestDay = true,
                readinessPercent = 48,
                sideQuestTitle = "Mobility patrol"
            ),
            activeQuest = null,
            recommendedExerciseCount = 0,
            biome = biome,
            encounter = encounter
        )

        assertEquals(QuestHubAction.OPEN_TRAINING, state.action)
        assertEquals("Train Anyway", state.primaryLabel)
        assertNull(state.workoutId)
    }

    @Test
    fun `missing routines explain the state and open training choices`() {
        val state = buildQuestHubState(
            recommendation = RoutineRecommendation(routine = null),
            activeQuest = null,
            recommendedExerciseCount = 0,
            biome = biome,
            encounter = encounter
        )

        assertEquals(QuestHubAction.OPEN_TRAINING, state.action)
        assertEquals("Choose Training", state.primaryLabel)
        assertNull(state.workoutId)
    }
}
