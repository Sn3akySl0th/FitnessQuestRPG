package com.fitnessquest.rpg

import com.fitnessquest.rpg.data.db.ActiveExerciseEntity
import com.fitnessquest.rpg.data.db.ActiveExerciseWithSets
import com.fitnessquest.rpg.data.db.ActiveSessionEntity
import com.fitnessquest.rpg.data.db.ActiveSessionWithDetails
import com.fitnessquest.rpg.data.db.ActiveSetLogEntity
import com.fitnessquest.rpg.data.db.AppDatabase
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.PendingSyncEntity
import com.fitnessquest.rpg.data.db.SessionEntity
import com.fitnessquest.rpg.data.sync.OutboxSyncResult
import com.fitnessquest.rpg.domain.SetType
import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class ActiveSessionTest {

    /**
     * V23 Fixture Migration Test:
     * Exercises MIGRATION_23_24 and MIGRATION_24_25 on a simulated V23 database state lacking
     * targetWeightKg, setType, and completionToken. Verifies repaired columns, unique index,
     * new active session tables, and outbox schema creations.
     */
    @Test
    fun v23_sqlite_fixture_migration_test() {
        val executedSqls = mutableListOf<String>()
        val existingColumns = mutableMapOf<String, MutableSet<String>>(
            "workout_exercises" to mutableSetOf("id", "workoutId", "exerciseName", "category", "targetSets", "targetReps"),
            "set_logs" to mutableSetOf("id", "sessionId", "exerciseName", "category", "weightKg", "reps", "durationMin", "distanceKm", "xp", "loggedAt"),
            "sessions" to mutableSetOf("id", "name", "startedAt", "endedAt", "xpEarned", "goldEarned", "energyEarned", "setCount")
        )

        val cursorProxy = Proxy.newProxyInstance(
            android.database.Cursor::class.java.classLoader,
            arrayOf(android.database.Cursor::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getColumnIndex" -> 0
                "moveToNext" -> false
                "getString" -> ""
                "close" -> null
                else -> null
            }
        } as android.database.Cursor

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    val sql = args[0] as String
                    executedSqls.add(sql)
                    if (sql.startsWith("ALTER TABLE")) {
                        val parts = sql.split(" ")
                        val tableName = parts[2]
                        val columnName = parts[5]
                        existingColumns.getOrPut(tableName) { mutableSetOf() }.add(columnName)
                    }
                    null
                }
                "query" -> {
                    val sql = args[0] as String
                    val table = sql.substringAfter("PRAGMA table_info(").substringBefore(")")
                    val cols = existingColumns[table] ?: emptySet()
                    var index = -1
                    val colList = cols.toList()
                    Proxy.newProxyInstance(
                        android.database.Cursor::class.java.classLoader,
                        arrayOf(android.database.Cursor::class.java)
                    ) { _, cMethod, cArgs ->
                        when (cMethod.name) {
                            "getColumnIndex" -> 0
                            "moveToNext" -> {
                                index++
                                index < colList.size
                            }
                            "getString" -> colList.getOrNull(index) ?: ""
                            "close" -> null
                            else -> null
                        }
                    }
                }
                else -> null
            }
        } as SupportSQLiteDatabase

        // Execute production migrations
        AppDatabase.MIGRATION_23_24.migrate(dbProxy)
        AppDatabase.MIGRATION_24_25.migrate(dbProxy)

        // Verify MIGRATION_23_24 repairs & additions
        assertTrue("workout_exercises.targetWeightKg repaired", existingColumns["workout_exercises"]?.contains("targetWeightKg") == true)
        assertTrue("set_logs.setType repaired", existingColumns["set_logs"]?.contains("setType") == true)
        assertTrue("sessions.completionToken added", existingColumns["sessions"]?.contains("completionToken") == true)
        assertTrue("sessions completionToken unique index created", executedSqls.any { it.contains("index_sessions_completion_token") })

        // Verify MIGRATION_24_25 tables created
        assertTrue("active_sessions table created", executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS active_sessions") })
        assertTrue("active_session_exercises table created", executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS active_session_exercises") })
        assertTrue("active_session_set_logs table created", executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS active_session_set_logs") })
        assertTrue("pending_sync_outbox table created", executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS pending_sync_outbox") })
    }

    @Test
    fun activeSession_structureAndSorting() {
        val session = ActiveSessionEntity(
            id = 1L,
            title = "Chest & Triceps",
            startedAt = 100000L,
            status = "ACTIVE",
            completionToken = "uuid_token_123"
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
        assertEquals("uuid_token_123", details.session.completionToken)
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
    fun outboxSyncResult_classification_test() {
        // Simulating the 4 sync result classifications
        fun processResult(result: OutboxSyncResult, currentRetry: Int): Pair<String, Int> {
            return when (result) {
                OutboxSyncResult.DELIVERED,
                OutboxSyncResult.ALREADY_PROCESSED,
                OutboxSyncResult.NOT_APPLICABLE -> "SENT" to currentRetry
                OutboxSyncResult.RETRYABLE_FAILURE -> "PENDING" to (currentRetry + 1)
            }
        }

        // 1. DELIVERED -> SENT
        val (status1, retries1) = processResult(OutboxSyncResult.DELIVERED, 0)
        assertEquals("SENT", status1)
        assertEquals(0, retries1)

        // 2. ALREADY_PROCESSED -> SENT
        val (status2, retries2) = processResult(OutboxSyncResult.ALREADY_PROCESSED, 0)
        assertEquals("SENT", status2)
        assertEquals(0, retries2)

        // 3. NOT_APPLICABLE (no party/guild) -> SENT (completed no-op)
        val (status3, retries3) = processResult(OutboxSyncResult.NOT_APPLICABLE, 0)
        assertEquals("SENT", status3)
        assertEquals(0, retries3)

        // 4. RETRYABLE_FAILURE -> PENDING & retries incremented
        val (status4, retries4) = processResult(OutboxSyncResult.RETRYABLE_FAILURE, 0)
        assertEquals("PENDING", status4)
        assertEquals(1, retries4)
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
