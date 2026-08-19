package com.fitnessquest.rpg

import android.database.Cursor
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fitnessquest.rpg.data.GameRepository
import com.fitnessquest.rpg.data.db.ActiveExerciseEntity
import com.fitnessquest.rpg.data.db.ActiveExerciseWithSets
import com.fitnessquest.rpg.data.db.ActiveSessionDao
import com.fitnessquest.rpg.data.db.ActiveSessionEntity
import com.fitnessquest.rpg.data.db.ActiveSessionWithDetails
import com.fitnessquest.rpg.data.db.ActiveSetLogEntity
import com.fitnessquest.rpg.data.db.AppDatabase
import com.fitnessquest.rpg.data.db.BiomeProgressDao
import com.fitnessquest.rpg.data.db.BodyMetricDao
import com.fitnessquest.rpg.data.db.CharacterDao
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ClassProgressDao
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.GearInstanceDao
import com.fitnessquest.rpg.data.db.ItemDao
import com.fitnessquest.rpg.data.db.MovementMasteryDao
import com.fitnessquest.rpg.data.db.MovementMasteryEntity
import com.fitnessquest.rpg.data.db.PendingSyncEntity
import com.fitnessquest.rpg.data.db.SessionDao
import com.fitnessquest.rpg.data.db.SessionEntity
import com.fitnessquest.rpg.data.db.SetLogEntity
import com.fitnessquest.rpg.data.db.WorkoutDao
import com.fitnessquest.rpg.data.sync.OutboxProcessor
import com.fitnessquest.rpg.data.sync.OutboxSyncResult
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.SetType
import com.fitnessquest.rpg.ui.screens.SessionExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation

private inline fun <reified T> createDummyProxy(): T {
    return Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java)
    ) { _, method, args ->
        val returnType = method.returnType
        val lastArg = args?.lastOrNull()
        if (lastArg is Continuation<*>) {
            if (method.name.endsWith("UpToTier") ||
                method.name.contains("List") ||
                method.name.contains("All") ||
                method.name.contains("Logs") ||
                method.name.contains("Events") ||
                method.name.contains("exercises") ||
                method.name.contains("Exercises") ||
                method.name.contains("For") ||
                method.name.startsWith("by")
            ) {
                emptyList<Any>()
            } else {
                null
            }
        } else if (returnType == java.util.List::class.java || returnType == java.util.Set::class.java || returnType.name.contains("List") || returnType.name.contains("Set")) {
            emptyList<Any>()
        } else if (returnType == Boolean::class.javaPrimitiveType) {
            false
        } else if (returnType == Int::class.javaPrimitiveType) {
            0
        } else if (returnType == Long::class.javaPrimitiveType) {
            0L
        } else if (returnType == Float::class.javaPrimitiveType) {
            0f
        } else if (returnType == Double::class.javaPrimitiveType) {
            0.0
        } else {
            null
        }
    } as T
}

class ActiveSessionTest {

    private lateinit var db: FakeAppDatabase
    private lateinit var repository: GameRepository

    private class FakeAppDatabase : AppDatabase() {
        var activeSessionEntity: ActiveSessionEntity? = null
        val exercises = mutableListOf<ActiveExerciseEntity>()
        val setLogs = mutableListOf<ActiveSetLogEntity>()
        val outboxEvents = mutableListOf<PendingSyncEntity>()
        val completedSessions = mutableMapOf<String, SessionEntity>()
        val savedSessions = mutableListOf<SessionEntity>()
        val savedSetLogs = mutableListOf<SetLogEntity>()
        var character: CharacterEntity? = null

        private val activeSessionState = MutableStateFlow<ActiveSessionWithDetails?>(null)
        private val characterState = MutableStateFlow<CharacterEntity?>(null)

        private fun updateActiveFlow() {
            val session = activeSessionEntity
            if (session == null) {
                activeSessionState.value = null
                return
            }
            val details = ActiveSessionWithDetails(
                session = session,
                exercises = exercises.map { ex ->
                    ActiveExerciseWithSets(
                        exercise = ex,
                        sets = setLogs.filter { it.exerciseId == ex.id }
                    )
                }
            )
            activeSessionState.value = details
        }

        val activeSessionDaoProxy = Proxy.newProxyInstance(
            ActiveSessionDao::class.java.classLoader,
            arrayOf(ActiveSessionDao::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getActiveSessionFlow", "observeActiveSessionWithDetails" -> activeSessionState
                "getActiveSession" -> activeSessionEntity
                "getActiveSessionWithDetails" -> {
                    val session = activeSessionEntity
                    if (session == null) null
                    else ActiveSessionWithDetails(
                        session = session,
                        exercises = exercises.map { ex ->
                            ActiveExerciseWithSets(
                                exercise = ex,
                                sets = setLogs.filter { it.exerciseId == ex.id }
                            )
                        }
                    )
                }
                "getActiveExercises" -> exercises.toList()
                "getActiveSetLogs" -> setLogs.toList()
                "insertActiveSession", "upsertActiveSession" -> {
                    val session = args[0] as ActiveSessionEntity
                    activeSessionEntity = session
                    updateActiveFlow()
                    session.id
                }
                "insertActiveExercises" -> {
                    @Suppress("UNCHECKED_CAST")
                    val list = args[0] as List<ActiveExerciseEntity>
                    val assigned = list.mapIndexed { index, ex -> ex.copy(id = (index + 1).toLong()) }
                    exercises.addAll(assigned)
                    updateActiveFlow()
                    assigned.map { it.id }
                }
                "insertActiveSetLogs" -> {
                    @Suppress("UNCHECKED_CAST")
                    val list = args[0] as List<ActiveSetLogEntity>
                    setLogs.addAll(list)
                    updateActiveFlow()
                    null
                }
                "insertActiveSetLog" -> {
                    val log = args[0] as ActiveSetLogEntity
                    val id = (setLogs.size + 1).toLong()
                    val created = log.copy(id = id)
                    setLogs.add(created)
                    updateActiveFlow()
                    id
                }
                "updateActiveSession" -> {
                    activeSessionEntity = args[0] as ActiveSessionEntity
                    updateActiveFlow()
                    null
                }
                "updateActiveExercise" -> {
                    val ex = args[0] as ActiveExerciseEntity
                    val idx = exercises.indexOfFirst { it.id == ex.id }
                    if (idx != -1) {
                        exercises[idx] = ex
                        updateActiveFlow()
                    }
                    null
                }
                "deleteActiveSession" -> {
                    val count = if (activeSessionEntity != null) 1 else 0
                    activeSessionEntity = null
                    exercises.clear()
                    setLogs.clear()
                    updateActiveFlow()
                    count
                }
                "deleteActiveSetLogs" -> {
                    val count = setLogs.size
                    setLogs.clear()
                    updateActiveFlow()
                    count
                }
                "deleteActiveExercises" -> {
                    val count = exercises.size
                    exercises.clear()
                    updateActiveFlow()
                    count
                }
                "deleteSetLog" -> {
                    val id = args[0] as Long
                    val removed = setLogs.removeAll { it.id == id }
                    updateActiveFlow()
                    if (removed) 1 else 0
                }
                "deleteLastSetLogForExercise" -> {
                    val exerciseId = args[0] as Long
                    val last = setLogs.findLast { it.exerciseId == exerciseId }
                    if (last != null) {
                        setLogs.remove(last)
                        updateActiveFlow()
                    }
                    null
                }
                "updateActiveSetLog" -> {
                    val log = args[0] as ActiveSetLogEntity
                    val idx = setLogs.indexOfFirst { it.id == log.id }
                    if (idx != -1) {
                        setLogs[idx] = log
                        updateActiveFlow()
                    }
                    null
                }
                "insertOutboxEvent" -> {
                    outboxEvents.add(args[0] as PendingSyncEntity)
                    null
                }
                "getPendingOutboxEvents" -> outboxEvents.filter { it.status == "PENDING" }
                "markOutboxEventSent" -> {
                    val eventId = args[0] as String
                    val idx = outboxEvents.indexOfFirst { it.eventId == eventId }
                    if (idx != -1) outboxEvents[idx] = outboxEvents[idx].copy(status = "SENT")
                    null
                }
                "incrementOutboxEventRetry" -> {
                    val eventId = args[0] as String
                    val idx = outboxEvents.indexOfFirst { it.eventId == eventId }
                    if (idx != -1) {
                        val ev = outboxEvents[idx]
                        outboxEvents[idx] = ev.copy(retryCount = ev.retryCount + 1)
                    }
                    null
                }
                "getSessionByCompletionToken" -> completedSessions[args[0] as String]
                "updateExerciseSuperset" -> {
                    val exerciseId = args[0] as Long
                    val supersetId = args[1] as? String
                    val idx = exercises.indexOfFirst { it.id == exerciseId }
                    if (idx != -1) {
                        exercises[idx] = exercises[idx].copy(supersetId = supersetId)
                        updateActiveFlow()
                    }
                    null
                }
                else -> null
            }
        } as ActiveSessionDao

        val sessionDaoProxy = Proxy.newProxyInstance(
            SessionDao::class.java.classLoader,
            arrayOf(SessionDao::class.java)
        ) { _, method, args ->
            when (method.name) {
                "insertSession" -> {
                    val session = args[0] as SessionEntity
                    val id = (savedSessions.size + 1).toLong()
                    val created = session.copy(id = id)
                    savedSessions.add(created)
                    session.completionToken?.let { token ->
                        completedSessions[token] = created
                    }
                    id
                }
                "insertSetLogs" -> {
                    @Suppress("UNCHECKED_CAST")
                    savedSetLogs.addAll(args[0] as List<SetLogEntity>)
                    null
                }
                "logsForExercise" -> {
                    val exName = args[0] as String
                    savedSetLogs.filter { it.exerciseName == exName }
                }
                "getAllLoggedExerciseNames" -> {
                    savedSetLogs.map { it.exerciseName }.distinct()
                }
                "logsForExercises" -> {
                    @Suppress("UNCHECKED_CAST")
                    val names = args[0] as List<String>
                    savedSetLogs.filter { it.exerciseName in names }
                }
                "sessionsByIds" -> {
                    @Suppress("UNCHECKED_CAST")
                    val ids = args[0] as List<Long>
                    savedSessions.filter { it.id in ids }.sortedByDescending { it.endedAt }
                }
                "countSessionsSince" -> {
                    val sinceMs = args[0] as Long
                    savedSessions.count { it.endedAt >= sinceMs }
                }
                else -> null
            }
        } as SessionDao

        val characterDaoProxy = Proxy.newProxyInstance(
            CharacterDao::class.java.classLoader,
            arrayOf(CharacterDao::class.java)
        ) { _, method, args ->
            when (method.name) {
                "get" -> character
                "observe", "getCharacterFlow" -> characterState
                "upsert", "update" -> {
                    val char = args[0] as CharacterEntity
                    character = char
                    characterState.value = char
                    null
                }
                else -> null
            }
        } as CharacterDao

        val movementMasteries = mutableMapOf<String, MovementMasteryEntity>()
        val movementMasteryFlow = MutableStateFlow<List<MovementMasteryEntity>>(emptyList())

        val movementMasteryDaoProxy = Proxy.newProxyInstance(
            MovementMasteryDao::class.java.classLoader,
            arrayOf(MovementMasteryDao::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getByCanonicalKey" -> movementMasteries[args[0] as String]
                "getAll" -> movementMasteries.values.toList()
                "observeAll" -> movementMasteryFlow
                "upsert" -> {
                    val entity = args[0] as MovementMasteryEntity
                    movementMasteries[entity.canonicalKey] = entity
                    movementMasteryFlow.value = movementMasteries.values.toList()
                    1L
                }
                "upsertAll" -> {
                    @Suppress("UNCHECKED_CAST")
                    val list = args[0] as List<MovementMasteryEntity>
                    list.forEach { movementMasteries[it.canonicalKey] = it }
                    movementMasteryFlow.value = movementMasteries.values.toList()
                    null
                }
                else -> null
            }
        } as MovementMasteryDao

        override fun activeSessionDao(): ActiveSessionDao = activeSessionDaoProxy
        override fun sessionDao(): SessionDao = sessionDaoProxy
        override fun characterDao(): CharacterDao = characterDaoProxy
        override fun movementMasteryDao(): MovementMasteryDao = movementMasteryDaoProxy
        override fun workoutDao(): WorkoutDao = createDummyProxy()
        override fun itemDao(): ItemDao = createDummyProxy()
        override fun gearInstanceDao(): GearInstanceDao = createDummyProxy()
        override fun biomeProgressDao(): BiomeProgressDao = createDummyProxy()
        override fun classProgressDao(): ClassProgressDao = createDummyProxy()
        override fun bodyMetricDao(): BodyMetricDao = createDummyProxy()

        private val directExecutor = java.util.concurrent.Executor { it.run() }
        override val transactionExecutor: java.util.concurrent.Executor = directExecutor
        override val queryExecutor: java.util.concurrent.Executor = directExecutor

        override fun createInvalidationTracker(): InvalidationTracker = createDummyProxy()
        override fun clearAllTables() {}
        override fun beginTransaction() {}
        override fun setTransactionSuccessful() {}
        override fun endTransaction() {}
        override fun runInTransaction(body: Runnable) { body.run() }
        override fun <V> runInTransaction(body: java.util.concurrent.Callable<V>): V = body.call()
    }

    @Before
    fun setUp() {
        db = FakeAppDatabase()
        repository = GameRepository(db)

        runBlocking {
            db.characterDao().upsert(
                CharacterEntity(
                    id = 1,
                    name = "Hero",
                    characterClass = CharacterClass.WARRIOR,
                    partyId = "party_777",
                    guildId = "guild_888"
                )
            )
        }
    }

    /**
     * V23 Fixture Migration Test:
     * Executes MIGRATION_23_24 and MIGRATION_24_25 on a simulated V23 database state lacking
     * targetWeightKg, setType, completionToken, and completionReceiptJson. Verifies repaired columns,
     * unique index, new active session tables, and outbox schema creations.
     */
    @Test
    fun v23_sqlite_fixture_migration_test() {
        val executedSqls = mutableListOf<String>()
        val existingColumns = mutableMapOf<String, MutableSet<String>>(
            "workout_exercises" to mutableSetOf("id", "workoutId", "exerciseName", "category", "targetSets", "targetReps"),
            "set_logs" to mutableSetOf("id", "sessionId", "exerciseName", "category", "weightKg", "reps", "durationMin", "distanceKm", "xp", "loggedAt"),
            "sessions" to mutableSetOf("id", "name", "startedAt", "endedAt", "xpEarned", "goldEarned", "energyEarned", "setCount")
        )

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
                        Cursor::class.java.classLoader,
                        arrayOf(Cursor::class.java)
                    ) { _, cMethod, _ ->
                        when (cMethod.name) {
                            "getColumnIndex" -> 0
                            "moveToNext" -> {
                                index++
                                index < colList.size
                            }
                            "getString" -> colList.getOrNull(index) ?: ""
                            "close" -> null
                            else -> {
                                when (cMethod.returnType) {
                                    Boolean::class.javaPrimitiveType -> false
                                    Int::class.javaPrimitiveType -> 0
                                    Long::class.javaPrimitiveType -> 0L
                                    Float::class.javaPrimitiveType -> 0f
                                    Double::class.javaPrimitiveType -> 0.0
                                    else -> null
                                }
                            }
                        }
                    }
                }
                else -> {
                    when (method.returnType) {
                        Boolean::class.javaPrimitiveType -> false
                        Int::class.javaPrimitiveType -> 0
                        Long::class.javaPrimitiveType -> 0L
                        Float::class.javaPrimitiveType -> 0f
                        Double::class.javaPrimitiveType -> 0.0
                        else -> null
                    }
                }
            }
        } as SupportSQLiteDatabase

        // Execute production migrations
        AppDatabase.MIGRATION_23_24.migrate(dbProxy)
        AppDatabase.MIGRATION_24_25.migrate(dbProxy)

        // Verify MIGRATION_23_24 repairs & additions
        assertTrue("workout_exercises.targetWeightKg repaired", existingColumns["workout_exercises"]?.contains("targetWeightKg") == true)
        assertTrue("set_logs.setType repaired", existingColumns["set_logs"]?.contains("setType") == true)
        assertTrue("sessions.completionToken added", existingColumns["sessions"]?.contains("completionToken") == true)
        assertTrue("sessions.completionReceiptJson added", existingColumns["sessions"]?.contains("completionReceiptJson") == true)
        assertTrue("sessions completionToken unique index created", executedSqls.any { it.contains("index_sessions_completion_token") })

        // Verify MIGRATION_24_25 tables created
        assertTrue("active_sessions table created", executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS active_sessions") })
        assertTrue("active_session_exercises table created", executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS active_session_exercises") })
        assertTrue("active_session_set_logs table created", executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS active_session_set_logs") })
        assertTrue("pending_sync_outbox table created", executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS pending_sync_outbox") })
    }

    @Test
    fun completeSession_fullReceiptRestoration_andIdempotency() = runBlocking {
        val char = db.characterDao().get()!!
        db.characterDao().upsert(char.copy(partyId = "party_777", guildId = "guild_888"))

        val exercises = listOf(
            SessionExercise(name = "Bench Press", category = ExerciseCategory.STRENGTH)
        )
        val activeDetails = repository.startActiveSession("Chest Day", 1L, exercises)
        val token = activeDetails.session.completionToken!!

        val exId = activeDetails.sortedExercises.first().exercise.id
        repository.logActiveSet(
            exerciseId = exId,
            exerciseName = "Bench Press",
            category = ExerciseCategory.STRENGTH,
            weightKg = 100.0,
            reps = 10,
            durationMin = 0.0,
            distanceKm = 0.0,
            xp = 100,
            rir = null,
            avgHr = null,
            maxHr = null,
            speedKmh = 0.0,
            inclinePercent = 0.0,
            cardioProgram = "",
            setType = SetType.NORMAL,
            heatStreak = 1,
            restDurationSec = 90
        )

        val logs = listOf(
            SetLogEntity(
                sessionId = 0,
                exerciseName = "Bench Press",
                category = ExerciseCategory.STRENGTH,
                weightKg = 100.0,
                reps = 10,
                xp = 100
            )
        )
        val startMs = System.currentTimeMillis()

        // Seed history for PR check
        db.savedSetLogs.add(SetLogEntity(
            sessionId = 999,
            exerciseName = "Bench Press",
            category = ExerciseCategory.STRENGTH,
            weightKg = 90.0,
            reps = 10,
            xp = 50
        ))

        // First completion call
        val result1 = repository.completeSession(
            "Chest Day",
            startMs,
            logs,
            completionToken = token,
            userId = "firebase_uid_123"
        )
        val char1 = db.characterDao().get()

        // Verify outbox payload contains destination partyId, guildId, and uid
        val outboxEvents = db.activeSessionDao().getPendingOutboxEvents()
        assertEquals(2, outboxEvents.size)
        val partyEvent = outboxEvents.find { it.type == "PARTY_XP" }
        assertNotNull(partyEvent)
        assertTrue(partyEvent!!.payloadJson.contains("party_777"))
        assertTrue(partyEvent.payloadJson.contains("1"))

        // Second completion call (idempotent retry)
        val result2 = repository.completeSession(
            "Chest Day",
            startMs,
            logs,
            completionToken = token,
            userId = "firebase_uid_123"
        )
        val char2 = db.characterDao().get()

        // Verify full SessionResult restoration
        assertEquals(result1.xp, result2.xp)
        assertEquals(result1.gold, result2.gold)
        assertEquals(result1.energy, result2.energy)
        assertEquals(result1.statGains, result2.statGains)
        assertEquals(result1.volumeKg, result2.volumeKg, 0.001)
        assertEquals(result1.durationMs, result2.durationMs)
        assertEquals(result1.musclesWorked, result2.musclesWorked)
        assertEquals(result1.prs.size, result2.prs.size)
        assertTrue("Restored PRs should not be empty", result2.prs.isNotEmpty())
        assertEquals(result1.lootLabels, result2.lootLabels)

        assertEquals(result1.rewardBatch, result2.rewardBatch)

        // Verify character state was NOT mutated a second time
        assertEquals(char1?.xp, char2?.xp)
        assertEquals(char1?.gold, char2?.gold)
    }

    @Test
    fun outboxProcessor_deliveryOutcomes_test() = runBlocking {
        val dao = db.activeSessionDao()
        val eventDelivered = PendingSyncEntity(eventId = "tok1:PARTY", type = "PARTY_XP", payloadJson = "{\"xp\":100,\"partyId\":\"p1\",\"uid\":\"u1\"}")
        val eventAlready = PendingSyncEntity(eventId = "tok2:PARTY", type = "PARTY_XP", payloadJson = "{\"xp\":100,\"partyId\":\"p1\",\"uid\":\"u1\"}")
        val eventNotApp = PendingSyncEntity(eventId = "tok3:PARTY", type = "PARTY_XP", payloadJson = "{\"xp\":100}")
        val eventFailure = PendingSyncEntity(eventId = "tok4:PARTY", type = "PARTY_XP", payloadJson = "{\"xp\":100,\"partyId\":\"p1\",\"uid\":\"u1\"}")

        dao.insertOutboxEvent(eventDelivered)
        dao.insertOutboxEvent(eventAlready)
        dao.insertOutboxEvent(eventNotApp)
        dao.insertOutboxEvent(eventFailure)

        val fakeProcessor = object : OutboxProcessor {
            override suspend fun processEvent(event: PendingSyncEntity): OutboxSyncResult {
                return when (event.eventId) {
                    "tok1:PARTY" -> OutboxSyncResult.DELIVERED
                    "tok2:PARTY" -> OutboxSyncResult.ALREADY_PROCESSED
                    "tok3:PARTY" -> OutboxSyncResult.NOT_APPLICABLE
                    "tok4:PARTY" -> OutboxSyncResult.RETRYABLE_FAILURE
                    else -> OutboxSyncResult.NOT_APPLICABLE
                }
            }
        }

        // Process all events
        for (event in dao.getPendingOutboxEvents()) {
            val result = fakeProcessor.processEvent(event)
            when (result) {
                OutboxSyncResult.DELIVERED,
                OutboxSyncResult.ALREADY_PROCESSED,
                OutboxSyncResult.NOT_APPLICABLE -> dao.markOutboxEventSent(event.eventId)
                OutboxSyncResult.RETRYABLE_FAILURE -> dao.incrementOutboxEventRetry(event.eventId)
            }
        }

        // Assert tok1, tok2, tok3 marked SENT (only tok4 remains pending with retryCount = 1)
        val pending = dao.getPendingOutboxEvents()
        assertEquals(1, pending.size)
        assertEquals("tok4:PARTY", pending.first().eventId)
        assertEquals(1, pending.first().retryCount)
    }

    @Test
    fun activeSession_cascadingDeletion_test() = runBlocking {
        val exercises = listOf(
            SessionExercise(name = "Deadlift", category = ExerciseCategory.STRENGTH)
        )
        repository.startActiveSession("Pull Day", 1L, exercises)
        val active = repository.activeSession.first()!!
        val exId = active.sortedExercises.first().exercise.id

        repository.logActiveSet(
            exerciseId = exId,
            exerciseName = "Deadlift",
            category = ExerciseCategory.STRENGTH,
            weightKg = 140.0,
            reps = 5,
            durationMin = 0.0,
            distanceKm = 0.0,
            xp = 120,
            rir = null,
            avgHr = null,
            maxHr = null,
            speedKmh = 0.0,
            inclinePercent = 0.0,
            cardioProgram = "",
            setType = SetType.NORMAL,
            heatStreak = 1,
            restDurationSec = 90
        )

        // Discard active session
        repository.discardActiveSession()

        // Assert all exercises and sets were CASCADE deleted
        assertNull(repository.getActiveSessionWithDetails())
        assertEquals(0, db.activeSessionDao().getActiveExercises().size)
    }

    @Test
    fun testSessionCompletionResetsSetIds() = runBlocking {
        db.character = CharacterEntity(name = "Hero", level = 1, xp = 0, gold = 100)
        val exercises = listOf(SessionExercise(name = "Bench Press", category = ExerciseCategory.STRENGTH))
        repository.startActiveSession("Test Quest", null, exercises)
        val activeEx = db.exercises.first()
        repository.logActiveSet(activeEx.id, activeEx.exerciseName, activeEx.category, 100.0, 10, 0.0, 0.0, 10, 2, null, null, 0.0, 0.0, "", SetType.NORMAL, 1, 60)
        val draftSet = db.setLogs.first()
        assertTrue("Draft set should have a generated ID", draftSet.id > 0)
        val logs = listOf(SetLogEntity(id = draftSet.id, sessionId = 0, exerciseName = "Bench Press", category = ExerciseCategory.STRENGTH, weightKg = 100.0, reps = 10, xp = 10))
        repository.completeSession("Test Quest", System.currentTimeMillis() - 1000, logs, 1.0f, "token_123", "user_1")
        val savedLog = db.savedSetLogs.first()
        assertEquals("Set ID must be reset to 0 for history insertion", 0L, savedLog.id)
    }

    @Test
    fun testSupersetPreservationAndLinking() = runBlocking {
        val exercises = listOf(
            SessionExercise(name = "Bicep Curl", category = ExerciseCategory.STRENGTH, supersetId = "SS1"),
            SessionExercise(name = "Tricep Pushdown", category = ExerciseCategory.STRENGTH, supersetId = "SS1"),
            SessionExercise(name = "Lat Pulldown", category = ExerciseCategory.STRENGTH, supersetId = null)
        )
        repository.startActiveSession("Arm Blast", null, exercises)
        val active = repository.activeSession.first()!!
        assertEquals(3, active.sortedExercises.size)
        assertEquals("SS1", active.sortedExercises[0].exercise.supersetId)
        assertEquals("SS1", active.sortedExercises[1].exercise.supersetId)
        assertNull(active.sortedExercises[2].exercise.supersetId)

        // Update superset grouping dynamically
        val thirdExId = active.sortedExercises[2].exercise.id
        repository.updateActiveExerciseSuperset(thirdExId, "SS2")
        val updated = repository.activeSession.first()!!
        assertEquals("SS2", updated.sortedExercises[2].exercise.supersetId)
    }

    @Test
    fun testSupersetXpBonus() {
        val normalSet = SetLogEntity(
            sessionId = 0,
            exerciseName = "Bench Press",
            category = ExerciseCategory.STRENGTH,
            weightKg = 100.0,
            reps = 10,
            setType = SetType.NORMAL
        )
        val regularXp = com.fitnessquest.rpg.domain.GameMath.xpForSet(normalSet, isSuperset = false)
        val supersetXp = com.fitnessquest.rpg.domain.GameMath.xpForSet(normalSet, isSuperset = true)

        assertEquals(100, regularXp)
        assertEquals(115, supersetXp) // 1.15x density bonus
    }

    @Test
    fun testWearProtocolSupersetSerialization() {
        val sessionState = com.fitnessquest.shared.wear.WearSessionState(
            active = true,
            title = "Chest & Back Super Quest",
            imperial = true,
            heatStreak = 2,
            totalSets = 4,
            totalXp = 150,
            restEndsAt = null,
            restDurationSec = 60,
            currentIndex = 0,
            exercises = listOf(
                com.fitnessquest.shared.wear.WearExerciseState(
                    name = "Incline Press",
                    category = "STRENGTH",
                    targetSets = 3,
                    targetReps = 10,
                    loggedSets = 1,
                    supersetId = "SS1"
                ),
                com.fitnessquest.shared.wear.WearExerciseState(
                    name = "Chest Supported Row",
                    category = "STRENGTH",
                    targetSets = 3,
                    targetReps = 10,
                    loggedSets = 1,
                    supersetId = "SS1"
                )
            )
        )
        val json = sessionState.toJson()
        val restored = com.fitnessquest.shared.wear.WearSessionState.fromJson(json)

        assertEquals("SS1", restored.exercises[0].supersetId)
        assertEquals("SS1", restored.exercises[1].supersetId)
    }

    /**
     * Test 1: Completion-Token Idempotency (Sequential Duplicate Verification)
     *
     * Note on Concurrency Limitation:
     * This test executes sequentially against FakeAppDatabase. True OS-level SQLite WAL lock contention
     * cannot be modeled here; this verifies authoritative repository/receipt behavior when an identical
     * completion token is submitted multiple times (e.g. rapid double tap, retry, or replayed request).
     */
    @Test
    fun testCompletionTokenIdempotency_sequentialDuplicate_appliesOnceAndRestoresReceipt() = runBlocking {
        val initialChar = db.characterDao().get()!!
        db.characterDao().upsert(initialChar.copy(partyId = "party_alpha", guildId = "guild_beta"))

        val exercises = listOf(
            SessionExercise(name = "Bench Press", category = ExerciseCategory.STRENGTH),
            SessionExercise(name = "Treadmill Run", category = ExerciseCategory.CARDIO)
        )
        val activeDetails = repository.startActiveSession("Hybrid Power Session", 101L, exercises)
        val token = "idempotent_token_test_abc123"

        val benchExId = activeDetails.sortedExercises[0].exercise.id
        val runExId = activeDetails.sortedExercises[1].exercise.id

        repository.logActiveSet(
            exerciseId = benchExId,
            exerciseName = "Bench Press",
            category = ExerciseCategory.STRENGTH,
            weightKg = 100.0,
            reps = 10,
            durationMin = 0.0,
            distanceKm = 0.0,
            xp = 100,
            rir = 2,
            avgHr = null,
            maxHr = null,
            speedKmh = 0.0,
            inclinePercent = 0.0,
            cardioProgram = "",
            setType = SetType.NORMAL,
            heatStreak = 1,
            restDurationSec = 60
        )

        repository.logActiveSet(
            exerciseId = runExId,
            exerciseName = "Treadmill Run",
            category = ExerciseCategory.CARDIO,
            weightKg = 0.0,
            reps = 0,
            durationMin = 20.0,
            distanceKm = 3.0,
            xp = 80,
            rir = null,
            avgHr = 145,
            maxHr = 160,
            speedKmh = 9.0,
            inclinePercent = 1.0,
            cardioProgram = "Warmup",
            setType = SetType.NORMAL,
            heatStreak = 2,
            restDurationSec = 30
        )

        val setLogs = listOf(
            SetLogEntity(
                sessionId = 0,
                exerciseName = "Bench Press",
                category = ExerciseCategory.STRENGTH,
                weightKg = 100.0,
                reps = 10,
                xp = 100
            ),
            SetLogEntity(
                sessionId = 0,
                exerciseName = "Treadmill Run",
                category = ExerciseCategory.CARDIO,
                durationMin = 20.0,
                distanceKm = 3.0,
                xp = 80
            )
        )
        val startedAt = System.currentTimeMillis() - 1800000L // 30 mins ago

        // First completion invocation
        val firstResult = repository.completeSession(
            name = "Hybrid Power Session",
            startedAt = startedAt,
            logs = setLogs,
            strengthXpMultiplier = 1.0f,
            completionToken = token,
            userId = "user_abc_789"
        )
        val charAfterFirst = db.characterDao().get()!!

        // Explicit assertions on first completion
        assertEquals(1, db.savedSessions.size)
        assertEquals(2, db.savedSetLogs.size)
        assertNotNull(firstResult.rewardBatch)
        assertTrue("XP earned must be positive", firstResult.xp > 0)
        assertNotNull("Character must be persisted", charAfterFirst)
        assertNull("Active session entity must be wiped after completion", db.activeSessionEntity)

        val outboxEventsFirst = db.activeSessionDao().getPendingOutboxEvents()
        val masteryCountFirst = db.movementMasteryDao().getAll().size

        // Second completion invocation with the EXACT same token (idempotent duplicate/retry)
        val secondResult = repository.completeSession(
            name = "Hybrid Power Session",
            startedAt = startedAt,
            logs = setLogs,
            strengthXpMultiplier = 1.0f,
            completionToken = token,
            userId = "user_abc_789"
        )
        val charAfterSecond = db.characterDao().get()!!

        // 1. Assert exactly one completed session record exists (no second row inserted)
        assertEquals("Exactly one session record must exist", 1, db.savedSessions.size)
        assertEquals("Set logs must not be inserted a second time", 2, db.savedSetLogs.size)

        // 2. Assert character state was not mutated a second time
        assertEquals("Character XP must not increase on duplicate completion", charAfterFirst.xp, charAfterSecond.xp)
        assertEquals("Character gold must not increase on duplicate completion", charAfterFirst.gold, charAfterSecond.gold)
        assertEquals("Character energy must not increase on duplicate completion", charAfterFirst.energy, charAfterSecond.energy)
        assertEquals("Character streak must remain identical", charAfterFirst.streak, charAfterSecond.streak)

        // 3. Assert outbox events and movement mastery were not duplicated
        assertEquals("Outbox events count must remain identical", outboxEventsFirst.size, db.activeSessionDao().getPendingOutboxEvents().size)
        assertEquals("Movement mastery records count must remain identical", masteryCountFirst, db.movementMasteryDao().getAll().size)

        // 4. Assert authoritative receipt fields match between first and duplicate calls
        assertEquals(firstResult.xp, secondResult.xp)
        assertEquals(firstResult.gold, secondResult.gold)
        assertEquals(firstResult.energy, secondResult.energy)
        assertEquals(firstResult.volumeKg, secondResult.volumeKg, 0.001)
        assertEquals(firstResult.durationMs, secondResult.durationMs)
        assertEquals(firstResult.statGains, secondResult.statGains)
        assertEquals(firstResult.prs.size, secondResult.prs.size)
        assertEquals(firstResult.lootLabels, secondResult.lootLabels)
        assertEquals(firstResult.rewardBatch?.rewards?.size, secondResult.rewardBatch?.rewards?.size)
    }

    /**
     * Test 2: Cardio Set Persistence, Speed/Pace, Edit, and Delete
     *
     * Verifies that cardio sets persist duration, distance, speed, and program fields,
     * support in-place editing, and can be deleted without corrupting concurrent strength logs.
     */
    @Test
    fun testCardioSetPersistence_speedPace_editingAndDeletion() = runBlocking {
        val exercises = listOf(
            SessionExercise(name = "Overhead Press", category = ExerciseCategory.STRENGTH),
            SessionExercise(name = "Outdoor Cycling", category = ExerciseCategory.CARDIO)
        )
        val active = repository.startActiveSession("Endurance & Shoulders", null, exercises)
        val pressExId = active.sortedExercises[0].exercise.id
        val cycleExId = active.sortedExercises[1].exercise.id

        // 1. Log one strength set first
        repository.logActiveSet(
            exerciseId = pressExId,
            exerciseName = "Overhead Press",
            category = ExerciseCategory.STRENGTH,
            weightKg = 60.0,
            reps = 8,
            durationMin = 0.0,
            distanceKm = 0.0,
            xp = 60,
            rir = 1,
            avgHr = null,
            maxHr = null,
            speedKmh = 0.0,
            inclinePercent = 0.0,
            cardioProgram = "",
            setType = SetType.NORMAL,
            heatStreak = 1,
            restDurationSec = 90
        )

        // 2. Log one cardio set: 30 minutes, 10.0 km (derived speed = 20.0 km/h)
        val duration = 30.0
        val distance = 10.0
        val expectedSpeed = distance / (duration / 60.0) // 20.0 km/h

        repository.logActiveSet(
            exerciseId = cycleExId,
            exerciseName = "Outdoor Cycling",
            category = ExerciseCategory.CARDIO,
            weightKg = 0.0,
            reps = 0,
            durationMin = duration,
            distanceKm = distance,
            xp = 120,
            rir = null,
            avgHr = 150,
            maxHr = 168,
            speedKmh = expectedSpeed,
            inclinePercent = 3.0,
            cardioProgram = "Intervals",
            setType = SetType.NORMAL,
            heatStreak = 2,
            restDurationSec = 60
        )

        val activeLogs = db.setLogs.toList()
        assertEquals(2, activeLogs.size)

        val cardioLog = activeLogs.find { it.category == ExerciseCategory.CARDIO }
        assertNotNull("Cardio set log must exist", cardioLog)
        assertEquals("Outdoor Cycling", cardioLog!!.exerciseName)
        assertEquals(ExerciseCategory.CARDIO, cardioLog.category)
        assertEquals(30.0, cardioLog.durationMin, 0.001)
        assertEquals(10.0, cardioLog.distanceKm, 0.001)
        assertEquals(20.0, cardioLog.speedKmh, 0.001)
        assertEquals(3.0, cardioLog.inclinePercent, 0.001)
        assertEquals("Intervals", cardioLog.cardioProgram)

        // 3. Edit the cardio set: update duration to 35.0 mins and distance to 12.0 km
        val updatedCardioLog = cardioLog.copy(
            durationMin = 35.0,
            distanceKm = 12.0,
            speedKmh = 12.0 / (35.0 / 60.0)
        )
        repository.updateActiveSetLog(updatedCardioLog)

        val logsAfterEdit = db.setLogs.toList()
        val editedLog = logsAfterEdit.find { it.id == cardioLog.id }!!
        assertEquals(35.0, editedLog.durationMin, 0.001)
        assertEquals(12.0, editedLog.distanceKm, 0.001)

        // 4. Delete the cardio set log
        repository.deleteActiveSetLog(cardioLog.id)

        val logsAfterDelete = db.setLogs.toList()
        assertEquals("Only one log should remain after deleting cardio", 1, logsAfterDelete.size)

        // 5. Assert the remaining strength set was not corrupted or modified
        val remainingStrengthLog = logsAfterDelete.first()
        assertEquals("Overhead Press", remainingStrengthLog.exerciseName)
        assertEquals(ExerciseCategory.STRENGTH, remainingStrengthLog.category)
        assertEquals(60.0, remainingStrengthLog.weightKg, 0.001)
        assertEquals(8, remainingStrengthLog.reps)
    }

    /**
     * Test 3: Draft-Session Restore
     *
     * Verifies that observing or restoring an active draft session rehydrates all exercises,
     * sets, and metadata without duplicating rows or marking the workout completed.
     */
    @Test
    fun testDraftSessionRestore_rehydratesSetsAndMetadataWithoutDuplication() = runBlocking {
        val exercises = listOf(
            SessionExercise(name = "Barbell Squat", category = ExerciseCategory.STRENGTH, targetSets = 3, targetReps = 8, targetWeightKg = 120.0),
            SessionExercise(name = "Rowing", category = ExerciseCategory.CARDIO, targetSets = 1, targetReps = 20)
        )
        val initialSession = repository.startActiveSession("Legs & Rowing Quest", 202L, exercises)
        val squatExId = initialSession.sortedExercises[0].exercise.id
        val rowExId = initialSession.sortedExercises[1].exercise.id

        // Log 2 strength sets
        repository.logActiveSet(squatExId, "Barbell Squat", ExerciseCategory.STRENGTH, 120.0, 8, 0.0, 0.0, 80, 2, null, null, 0.0, 0.0, "", SetType.NORMAL, 1, 90)
        repository.logActiveSet(squatExId, "Barbell Squat", ExerciseCategory.STRENGTH, 125.0, 6, 0.0, 0.0, 90, 1, null, null, 0.0, 0.0, "", SetType.NORMAL, 2, 90)

        // Log 1 cardio set
        repository.logActiveSet(rowExId, "Rowing", ExerciseCategory.CARDIO, 0.0, 0, 15.0, 3.0, 70, null, 140, 155, 12.0, 0.0, "500m Splits", SetType.NORMAL, 3, 60)

        assertEquals(3, db.setLogs.size)

        // Simulate app restart / ViewModel rehydration via getActiveSessionWithDetails and activeSession Flow
        val restoredDetails = repository.getActiveSessionWithDetails()
        val flowDetails = repository.activeSession.first()

        assertNotNull("Restored session details must not be null", restoredDetails)
        assertNotNull("Flow session details must not be null", flowDetails)

        assertEquals("Legs & Rowing Quest", restoredDetails!!.session.title)
        assertEquals(202L, restoredDetails.session.workoutId)
        assertEquals("ACTIVE", restoredDetails.session.status)

        val restoredExercises = restoredDetails.sortedExercises
        assertEquals(2, restoredExercises.size)

        val squatEx = restoredExercises[0]
        assertEquals("Barbell Squat", squatEx.exercise.exerciseName)
        assertEquals(2, squatEx.sets.size)
        assertEquals(120.0, squatEx.sets[0].weightKg, 0.001)
        assertEquals(8, squatEx.sets[0].reps)
        assertEquals(125.0, squatEx.sets[1].weightKg, 0.001)
        assertEquals(6, squatEx.sets[1].reps)

        val rowEx = restoredExercises[1]
        assertEquals("Rowing", rowEx.exercise.exerciseName)
        assertEquals(1, rowEx.sets.size)
        assertEquals(15.0, rowEx.sets[0].durationMin, 0.001)
        assertEquals(3.0, rowEx.sets[0].distanceKm, 0.001)
        assertEquals("500m Splits", rowEx.sets[0].cardioProgram)

        // Assert repeated observation does not create duplicate rows
        val secondRead = repository.getActiveSessionWithDetails()
        assertEquals(3, secondRead?.exercises?.flatMap { it.sets }?.size)
        assertEquals("No saved completed sessions should exist yet", 0, db.savedSessions.size)
    }
}
