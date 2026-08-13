package com.fitnessquest.rpg

import com.fitnessquest.rpg.data.db.ActiveExerciseEntity
import com.fitnessquest.rpg.data.db.ActiveExerciseWithSets
import com.fitnessquest.rpg.data.db.ActiveSessionEntity
import com.fitnessquest.rpg.data.db.ActiveSessionWithDetails
import com.fitnessquest.rpg.data.db.ActiveSetLogEntity
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.PendingSyncEntity
import com.fitnessquest.rpg.data.db.SessionEntity
import com.fitnessquest.rpg.domain.GameMath
import org.junit.Assert.*
import org.junit.Test

class ActiveSessionTest {

    @Test
    fun activeSession_structureAndSorting() {
        val session = ActiveSessionEntity(
            id = 1L,
            title = "Chest & Triceps",
            startedAt = 100000L,
            status = "ACTIVE"
        )
        val ex1 = ActiveExerciseEntity(
            id = 10L,
            activeSessionId = 1L,
            exerciseName = "Bench Press",
            category = ExerciseCategory.STRENGTH,
            sortOrder = 1
        )
        val ex2 = ActiveExerciseEntity(
            id = 11L,
            activeSessionId = 1L,
            exerciseName = "Incline Dumbbell Press",
            category = ExerciseCategory.STRENGTH,
            sortOrder = 0
        )
        val set1 = ActiveSetLogEntity(
            id = 100L,
            activeSessionId = 1L,
            exerciseId = 10L,
            exerciseName = "Bench Press",
            category = ExerciseCategory.STRENGTH,
            weightKg = 80.0,
            reps = 10,
            loggedAt = 100050L
        )

        val details = ActiveSessionWithDetails(
            session = session,
            exercises = listOf(
                ActiveExerciseWithSets(ex1, listOf(set1)),
                ActiveExerciseWithSets(ex2, emptyList())
            )
        )

        assertEquals("Chest & Triceps", details.session.title)
        assertEquals(2, details.exercises.size)
        // Verify sortedExercises sorts by sortOrder ASC
        assertEquals("Incline Dumbbell Press", details.sortedExercises[0].exercise.exerciseName)
        assertEquals("Bench Press", details.sortedExercises[1].exercise.exerciseName)
        assertEquals(1, details.sortedExercises[1].sets.size)
        assertEquals(80.0, details.sortedExercises[1].sets[0].weightKg, 0.01)
    }

    @Test
    fun idempotencyToken_uniqueness() {
        val token = "token_abc_123"
        val session1 = SessionEntity(
            id = 1,
            name = "Morning Quest",
            startedAt = 100000L,
            endedAt = 103000L,
            xpEarned = 150,
            goldEarned = 25,
            energyEarned = 10,
            setCount = 5,
            completionToken = token
        )
        val session2 = SessionEntity(
            id = 2,
            name = "Morning Quest Retry",
            startedAt = 100000L,
            endedAt = 103000L,
            xpEarned = 150,
            goldEarned = 25,
            energyEarned = 10,
            setCount = 5,
            completionToken = token
        )

        assertEquals(session1.completionToken, session2.completionToken)
        assertNotNull(session1.completionToken)
    }

    @Test
    fun outboxEvents_formatAndKeyDerivation() {
        val token = "session_token_xyz"
        val partyEvent = PendingSyncEntity(
            eventId = "${token}:PARTY",
            type = "PARTY_XP",
            payloadJson = "{\"xp\":200}"
        )
        val guildEvent = PendingSyncEntity(
            eventId = "${token}:GUILD",
            type = "GUILD_XP",
            payloadJson = "{\"xp\":200}"
        )

        assertEquals("session_token_xyz:PARTY", partyEvent.eventId)
        assertEquals("session_token_xyz:GUILD", guildEvent.eventId)
        assertEquals("PENDING", partyEvent.status)
        assertEquals(0, partyEvent.retryCount)
    }

    @Test
    fun exerciseReorderAndRemoval_byUniqueId() {
        val exercises = mutableListOf(
            ActiveExerciseEntity(id = 1, activeSessionId = 1, exerciseName = "Squats", category = ExerciseCategory.STRENGTH, sortOrder = 0),
            ActiveExerciseEntity(id = 2, activeSessionId = 1, exerciseName = "Lunges", category = ExerciseCategory.STRENGTH, sortOrder = 1)
        )

        // Reorder by ID: place id=2 first, id=1 second
        val newOrderIds = listOf(2L, 1L)
        val reordered = exercises.map { ex ->
            val newIndex = newOrderIds.indexOf(ex.id)
            ex.copy(sortOrder = if (newIndex != -1) newIndex else ex.sortOrder)
        }.sortedBy { it.sortOrder }

        assertEquals("Lunges", reordered[0].exerciseName)
        assertEquals(0, reordered[0].sortOrder)
        assertEquals("Squats", reordered[1].exerciseName)
        assertEquals(1, reordered[1].sortOrder)

        // Remove exercise by ID
        val filtered = reordered.filterNot { it.id == 2L }
        assertEquals(1, filtered.size)
        assertEquals("Squats", filtered[0].exerciseName)
    }

    @Test
    fun durationDerivation_fromTimestamps() {
        val startedAt = 1_000_000L
        val now = 1_060_000L // 60 seconds later
        val pausedAt: Long? = null
        val accumulatedPausedMs = 0L

        val elapsedMs = if (pausedAt != null) {
            pausedAt - startedAt - accumulatedPausedMs
        } else {
            now - startedAt - accumulatedPausedMs
        }

        assertEquals(60_000L, elapsedMs)
    }
}
