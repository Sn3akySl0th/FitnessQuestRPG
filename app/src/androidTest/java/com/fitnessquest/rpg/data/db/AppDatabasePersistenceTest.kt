package com.fitnessquest.rpg.data.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitnessquest.rpg.domain.SetType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabasePersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseNames = mutableSetOf<String>()

    @After
    fun cleanUp() {
        databaseNames.forEach(context::deleteDatabase)
    }

    @Test
    fun activeWorkout_survivesCloseAndReopen_thenCascadesWithoutDeletingHistoryOrOutbox() = runBlocking {
        val name = "active-persistence-${System.nanoTime()}"
        databaseNames += name
        var db = buildDatabase(name)
        val dao = db.activeSessionDao()
        val startedAt = 1_700_000_000_000L
        val completionToken = "active-token-123"
        dao.upsertActiveSession(
            ActiveSessionEntity(
                title = "Persistent Quest",
                startedAt = startedAt,
                pausedAt = startedAt + 5_000,
                accumulatedPausedMs = 2_000,
                restEndsAt = startedAt + 90_000,
                currentExerciseIndex = 1,
                completionToken = completionToken
            )
        )
        val exerciseIds = dao.insertActiveExercises(
            listOf(
                ActiveExerciseEntity(
                    exerciseName = "Bench Press",
                    category = ExerciseCategory.STRENGTH,
                    targetSets = 4,
                    targetReps = 8,
                    targetWeightKg = 82.5,
                    sortOrder = 1,
                    notes = "Pause at the chest"
                ),
                ActiveExerciseEntity(
                    exerciseName = "Warmup Run",
                    category = ExerciseCategory.CARDIO,
                    trackingType = "DISTANCE_TIME",
                    sortOrder = 0
                )
            )
        )
        dao.insertActiveSetLog(
            ActiveSetLogEntity(
                exerciseId = exerciseIds.first(),
                exerciseName = "Bench Press",
                category = ExerciseCategory.STRENGTH,
                weightKg = 82.5,
                reps = 8,
                rir = 2,
                avgHr = 132,
                maxHr = 151,
                setType = SetType.WARM_UP,
                loggedAt = startedAt + 60_000
            )
        )
        dao.insertOutboxEvent(
            PendingSyncEntity(
                eventId = "social-event-1",
                type = "PARTY_XP",
                payloadJson = "{\"xp\":50}"
            )
        )
        db.sessionDao().insertSession(
            SessionEntity(
                id = 50,
                name = "Completed History",
                startedAt = startedAt - 100_000,
                endedAt = startedAt - 50_000,
                xpEarned = 40,
                goldEarned = 10,
                energyEarned = 2,
                setCount = 1
            )
        )
        db.close()

        db = buildDatabase(name)
        val reopenedDao = db.activeSessionDao()
        val restored = reopenedDao.getActiveSessionWithDetails()
        assertNotNull(restored)
        assertEquals(startedAt, restored?.session?.startedAt)
        assertEquals(completionToken, restored?.session?.completionToken)
        assertEquals(1, restored?.session?.currentExerciseIndex)
        assertEquals(listOf("Warmup Run", "Bench Press"), restored?.sortedExercises?.map { it.exercise.exerciseName })

        val bench = restored?.sortedExercises?.last()
        assertEquals(82.5, bench?.exercise?.targetWeightKg ?: 0.0, 0.0)
        assertEquals("Pause at the chest", bench?.exercise?.notes)
        assertEquals(1, bench?.sets?.size)
        assertEquals(SetType.WARM_UP, bench?.sets?.single()?.setType)
        assertEquals(132, bench?.sets?.single()?.avgHr)

        reopenedDao.deleteActiveSession()
        assertNull(reopenedDao.getActiveSessionWithDetails())
        assertTrue(reopenedDao.getActiveExercises().isEmpty())
        db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM active_session_set_logs").use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
        assertEquals(1, reopenedDao.getPendingOutboxEvents().size)
        assertEquals("Completed History", db.sessionDao().getAllSessions().single().name)
        db.close()
    }

    @Test
    fun completionToken_uniqueIndexRejectsDuplicatesButAllowsLegacyNulls() = runBlocking {
        val name = "completion-token-${System.nanoTime()}"
        databaseNames += name
        val db = buildDatabase(name)
        val dao = db.sessionDao()
        dao.insertSession(session(id = 1, token = "same-token"))

        try {
            dao.insertSession(session(id = 2, token = "same-token"))
            fail("Expected the unique completion-token index to reject the duplicate")
        } catch (_: SQLiteConstraintException) {
            // Expected.
        }

        dao.insertSession(session(id = 3, token = null))
        dao.insertSession(session(id = 4, token = null))
        assertEquals(3, dao.getAllSessions().size)
        db.close()
    }

    private fun session(id: Long, token: String?): SessionEntity = SessionEntity(
        id = id,
        name = "Session $id",
        startedAt = id * 100,
        endedAt = id * 100 + 50,
        xpEarned = 10,
        goldEarned = 2,
        energyEarned = 1,
        setCount = 1,
        completionToken = token
    )

    private fun buildDatabase(name: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_23_24, AppDatabase.MIGRATION_24_25)
            .allowMainThreadQueries()
            .build()
}
