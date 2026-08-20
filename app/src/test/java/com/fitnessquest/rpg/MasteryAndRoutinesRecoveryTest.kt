package com.fitnessquest.rpg

import android.database.Cursor
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fitnessquest.rpg.data.GameRepository
import com.fitnessquest.rpg.data.db.ActiveSessionDao
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
import com.fitnessquest.rpg.data.db.SessionDao
import com.fitnessquest.rpg.data.db.SessionEntity
import com.fitnessquest.rpg.data.db.SetLogEntity
import com.fitnessquest.rpg.data.db.WorkoutDao
import com.fitnessquest.rpg.data.db.WorkoutEntity
import com.fitnessquest.rpg.data.db.WorkoutExerciseEntity
import com.fitnessquest.rpg.domain.SetType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

private inline fun <reified T> createDummyDao(): T {
    return Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
    ) { _, method, _ ->
        val returnType = method.returnType
        if (returnType == java.util.List::class.java || returnType.name.contains("List")) {
            emptyList<Any>()
        } else if (returnType == Boolean::class.javaPrimitiveType) {
            false
        } else if (returnType == Int::class.javaPrimitiveType) {
            0
        } else if (returnType == Long::class.javaPrimitiveType) {
            0L
        } else {
            null
        }
    } as T
}

class MasteryAndRoutinesRecoveryTest {

    private lateinit var db: FakeDatabase
    private lateinit var repository: GameRepository

    private class FakeDatabase : AppDatabase() {
        var character: CharacterEntity = CharacterEntity(id = 1L, name = "Hero", bodyWeightKg = 80.0)
        val sessions = mutableListOf<SessionEntity>()
        val setLogs = mutableListOf<SetLogEntity>()
        val masteries = mutableMapOf<String, MovementMasteryEntity>()
        val workouts = mutableMapOf<Long, WorkoutEntity>()
        val workoutExercises = mutableMapOf<Long, MutableList<WorkoutExerciseEntity>>()
        private var nextWorkoutId = 1L

        val characterDaoProxy = Proxy.newProxyInstance(
            CharacterDao::class.java.classLoader,
            arrayOf(CharacterDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "get" -> character
                "upsert", "update" -> {
                    character = args[0] as CharacterEntity
                    null
                }
                else -> null
            }
        } as CharacterDao

        val sessionDaoProxy = Proxy.newProxyInstance(
            SessionDao::class.java.classLoader,
            arrayOf(SessionDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getAllSessions" -> sessions.toList()
                "getAllSetLogs" -> setLogs.toList()
                "insertSession" -> {
                    val s = args[0] as SessionEntity
                    sessions.add(s)
                    s.id
                }
                "insertSetLogs" -> {
                    @Suppress("UNCHECKED_CAST")
                    val logs = args[0] as List<SetLogEntity>
                    setLogs.addAll(logs)
                    null
                }
                "setLogsFor" -> {
                    val sId = args[0] as Long
                    setLogs.filter { it.sessionId == sId }
                }
                else -> null
            }
        } as SessionDao

        val movementMasteryDaoProxy = Proxy.newProxyInstance(
            MovementMasteryDao::class.java.classLoader,
            arrayOf(MovementMasteryDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getAll" -> masteries.values.toList()
                "getByCanonicalKey" -> masteries[args[0] as String]
                "upsertAll" -> {
                    @Suppress("UNCHECKED_CAST")
                    val list = args[0] as List<MovementMasteryEntity>
                    list.forEach { masteries[it.canonicalKey] = it }
                    null
                }
                "upsert" -> {
                    val m = args[0] as MovementMasteryEntity
                    masteries[m.canonicalKey] = m
                    null
                }
                else -> null
            }
        } as MovementMasteryDao

        val workoutDaoProxy = Proxy.newProxyInstance(
            WorkoutDao::class.java.classLoader,
            arrayOf(WorkoutDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getAllWorkouts" -> workouts.values.toList()
                "getWorkout", "get" -> workouts[args[0] as Long]
                "exercisesFor" -> workoutExercises[args[0] as Long] ?: emptyList<WorkoutExerciseEntity>()
                "saveWorkout" -> {
                    val w = args[0] as WorkoutEntity
                    @Suppress("UNCHECKED_CAST")
                    val exList = args[1] as List<WorkoutExerciseEntity>
                    val id = nextWorkoutId++
                    val saved = w.copy(id = id)
                    workouts[id] = saved
                    workoutExercises[id] = exList.mapIndexed { i, e -> e.copy(id = 0, workoutId = id, sortOrder = i) }.toMutableList()
                    id
                }
                "insertWorkout" -> {
                    val w = args[0] as WorkoutEntity
                    val id = nextWorkoutId++
                    val saved = w.copy(id = id)
                    workouts[id] = saved
                    id
                }
                "insertExercises" -> {
                    @Suppress("UNCHECKED_CAST")
                    val exList = args[0] as List<WorkoutExerciseEntity>
                    if (exList.isNotEmpty()) {
                        val wId = exList.first().workoutId
                        workoutExercises.getOrPut(wId) { mutableListOf() }.addAll(exList)
                    }
                    null
                }
                "deleteWorkoutFully" -> {
                    val id = args[0] as Long
                    workouts.remove(id)
                    workoutExercises.remove(id)
                    null
                }
                "deleteWorkoutsFully" -> {
                    @Suppress("UNCHECKED_CAST")
                    val ids = args[0] as List<Long>
                    ids.forEach { id ->
                        workouts.remove(id)
                        workoutExercises.remove(id)
                    }
                    null
                }
                else -> null
            }
        } as WorkoutDao

        override fun characterDao(): CharacterDao = characterDaoProxy
        override fun sessionDao(): SessionDao = sessionDaoProxy
        override fun movementMasteryDao(): MovementMasteryDao = movementMasteryDaoProxy
        override fun workoutDao(): WorkoutDao = workoutDaoProxy

        override fun biomeProgressDao(): BiomeProgressDao = createDummyDao()
        override fun itemDao(): ItemDao = createDummyDao()
        override fun gearInstanceDao(): GearInstanceDao = createDummyDao()
        override fun classProgressDao(): ClassProgressDao = createDummyDao()
        override fun bodyMetricDao(): BodyMetricDao = createDummyDao()
        override fun activeSessionDao(): ActiveSessionDao = createDummyDao()

        override fun createInvalidationTracker(): InvalidationTracker {
            return Proxy.newProxyInstance(
                InvalidationTracker::class.java.classLoader,
                arrayOf(InvalidationTracker::class.java),
            ) { _, _, _ -> null } as InvalidationTracker
        }

        override fun clearAllTables() {}
    }

    @Before
    fun setUp() {
        db = FakeDatabase()
        repository = GameRepository(db)
    }

    @Test
    fun all_migrations_are_contiguous_and_complete() {
        val migrations = AppDatabase.ALL_MIGRATIONS
        assertEquals("Should have 30 migrations from v1 to v31", 30, migrations.size)

        for (i in 1..30) {
            val migration = migrations.find { it.startVersion == i && it.endVersion == i + 1 }
            assertNotNull("Missing migration from v$i to v${i + 1}", migration)
        }
    }

    @Test
    fun selfHealDatabaseSchema_dynamically_repairs_missing_columns() {
        val executedSqls = mutableListOf<String>()
        val existingColumns = mutableMapOf<String, MutableSet<String>>(
            "character" to mutableSetOf("id", "name", "level", "xp"),
            "set_logs" to mutableSetOf("id", "sessionId", "exerciseName", "category", "weightKg", "reps", "xp"),
            "biome_progress" to mutableSetOf("biomeName", "layer"),
            "gear_instances" to mutableSetOf("id", "catalogId", "atk", "def", "hp"),
            "workout_exercises" to mutableSetOf("id", "workoutId", "exerciseName", "category", "targetSets", "targetReps")
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
                            else -> null
                        }
                    }
                }
                else -> null
            }
        } as SupportSQLiteDatabase

        // Execute self-healing on outdated schema
        AppDatabase.selfHealDatabaseSchema(dbProxy)

        // Verify character table auto-repaired
        assertTrue(existingColumns["character"]?.contains("dailyGambleCount") == true)
        assertTrue(existingColumns["character"]?.contains("lastGambleResetEpochMs") == true)
        assertTrue(existingColumns["character"]?.contains("glamourHeadId") == true)
        assertTrue(existingColumns["character"]?.contains("glamourWeaponId") == true)

        // Verify gear_instances auto-repaired
        assertTrue(existingColumns["gear_instances"]?.contains("upgradeLevel") == true)
        assertTrue(existingColumns["gear_instances"]?.contains("rarity") == true)

        // Verify set_logs auto-repaired
        assertTrue(existingColumns["set_logs"]?.contains("setType") == true)
        assertTrue(existingColumns["set_logs"]?.contains("speedKmh") == true)
    }

    @Test
    fun migration_30_to_31_adds_daily_gamble_columns() {
        val executedSqls = mutableListOf<String>()
        val existingColumns = mutableMapOf<String, MutableSet<String>>()
        existingColumns["character"] = mutableSetOf("id", "name", "level", "xp")

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
                            else -> null
                        }
                    }
                }
                else -> null
            }
        } as SupportSQLiteDatabase

        // Execute MIGRATION_30_31
        AppDatabase.MIGRATION_30_31.migrate(dbProxy)

        assertTrue(existingColumns["character"]?.contains("dailyGambleCount") == true)
        assertTrue(existingColumns["character"]?.contains("lastGambleResetEpochMs") == true)
    }

    @Test
    fun recalculateMasteriesFromHistory_reconstructsAllMasteriesAccurately() = runBlocking {
        // Insert historical sessions and sets
        val session1 = SessionEntity(
            id = 1L,
            name = "Leg Day",
            startedAt = 1000L,
            endedAt = 2000L,
            xpEarned = 150,
            goldEarned = 20,
            energyEarned = 5,
            setCount = 2
        )
        db.sessionDao().insertSession(session1)

        val set1 = SetLogEntity(
            sessionId = 1L,
            exerciseName = "Barbell Squat",
            category = ExerciseCategory.STRENGTH,
            weightKg = 100.0,
            reps = 10,
            xp = 50,
            setType = SetType.NORMAL
        )
        val set2 = SetLogEntity(
            sessionId = 1L,
            exerciseName = "Barbell Squat",
            category = ExerciseCategory.STRENGTH,
            weightKg = 120.0,
            reps = 8,
            xp = 60,
            setType = SetType.NORMAL
        )
        db.sessionDao().insertSetLogs(listOf(set1, set2))

        val session2 = SessionEntity(
            id = 2L,
            name = "Cardio Run",
            startedAt = 3000L,
            endedAt = 4000L,
            xpEarned = 200,
            goldEarned = 30,
            energyEarned = 10,
            setCount = 1
        )
        db.sessionDao().insertSession(session2)

        val set3 = SetLogEntity(
            sessionId = 2L,
            exerciseName = "Outdoor Running",
            category = ExerciseCategory.CARDIO,
            weightKg = 0.0,
            reps = 0,
            durationMin = 30.0,
            distanceKm = 5.0,
            xp = 100,
            setType = SetType.NORMAL
        )
        db.sessionDao().insertSetLogs(listOf(set3))

        // Ensure mastery table starts empty (simulating fresh install / data recovery)
        val initialMasteries = db.movementMasteryDao().getAll()
        assertTrue(initialMasteries.isEmpty())

        // Run recovery recalculation
        val count = repository.recalculateAllMovementMasteriesFromHistory()
        assertEquals(2, count)

        val masteries = db.movementMasteryDao().getAll()
        assertEquals(2, masteries.size)

        val squatMastery = masteries.find { it.displayName.contains("Squat", ignoreCase = true) }
        assertNotNull(squatMastery)
        assertEquals(120.0, squatMastery!!.highestWeightKg, 0.01)
        assertTrue(squatMastery.lifetimeVolumeKg > 0.0)
        assertEquals(18, squatMastery.lifetimeReps)
        assertEquals(1, squatMastery.totalSessionsLogged)

        val runningMastery = masteries.find { it.displayName.contains("Running", ignoreCase = true) }
        assertNotNull(runningMastery)
        assertEquals(5.0, runningMastery!!.lifetimeDistanceKm, 0.01)
        assertEquals(1800L, runningMastery.lifetimeDurationSec)
    }

    @Test
    fun cleanDuplicateWorkouts_and_bulkDeleteWorkouts_workCorrectly() = runBlocking {
        // Create duplicate workouts
        val exercises = listOf(
            WorkoutExerciseEntity(workoutId = 0, exerciseName = "Bench Press", category = ExerciseCategory.STRENGTH, targetSets = 3, targetReps = 10, sortOrder = 0)
        )
        val w1 = repository.saveWorkout("Chest Day", exercises)
        val w2 = repository.saveWorkout("Chest Day (1)", exercises)
        val w3 = repository.saveWorkout("Chest Day (2)", exercises)

        val allWorkoutsBefore = db.workoutDao().getAllWorkouts()
        assertEquals(3, allWorkoutsBefore.size)

        // Clean duplicates
        val cleaned = repository.cleanDuplicateWorkouts()
        assertEquals(2, cleaned)

        val allWorkoutsAfter = db.workoutDao().getAllWorkouts()
        assertEquals(1, allWorkoutsAfter.size)
        assertEquals(w1, allWorkoutsAfter.first().id)

        // Test bulk delete
        val w4 = repository.saveWorkout("Leg Day", exercises)
        assertEquals(2, db.workoutDao().getAllWorkouts().size)

        repository.deleteWorkoutsBulk(listOf(w1, w4))
        assertTrue(db.workoutDao().getAllWorkouts().isEmpty())
    }
}
