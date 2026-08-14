package com.fitnessquest.rpg.data.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration & restored database regression test matrix for AppDatabase (Version 25).
 *
 * HISTORICAL SCHEMA NOTE:
 * Historical Room schema export JSON files (1.json through 24.json) are not tracked in Git history
 * (Room schema export tracking begins at version 25.json). Therefore, starting checkpoint schemas
 * V1 through V24 are inferred and synthetically constructed by working backward from current V25
 * entity definitions in [Entities.kt] and subtracting columns introduced by production migrations.
 * While this matrix rigorously verifies migration chaining, SQL DDL execution, and compatibility
 * with Room's final expected V25 schema, it is synthetic rather than testing historical Room binary exports.
 *
 * STORAGE CONTEXT NOTE:
 * Device-Protected Storage ([Context.createDeviceProtectedStorageContext]) is used for synthetic
 * test database creation to guarantee test isolation and prevent storage lock / Direct Boot failures
 * (`IllegalStateException: SharedPreferences in credential encrypted storage...`) when instrumented tests
 * execute under [AndroidJUnitRunner].
 *
 * Execution commands:
 * JVM unit tests (ActiveSessionTest):
 *   .\gradlew.bat :app:testDebugUnitTest --tests "com.fitnessquest.rpg.ActiveSessionTest" --no-configuration-cache
 * Instrumented migration matrix tests (AppDatabaseMigrationTest & AppDatabasePersistenceTest):
 *   .\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.fitnessquest.rpg.data.db.AppDatabaseMigrationTest,com.fitnessquest.rpg.data.db.AppDatabasePersistenceTest" --no-configuration-cache
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dpContext = context.createDeviceProtectedStorageContext()
    private val databaseNames = mutableSetOf<String>()

    @After
    fun cleanUp() {
        databaseNames.forEach { name ->
            runCatching { dpContext.deleteDatabase(name) }
        }
    }

    /**
     * Test clean database creation at the current schema (Version 26).
     * Validates that all 15 entities and expected indexes are created without error.
     */
    @Test
    fun cleanDatabaseCreationAtVersion26_validatesSchemaAndEntities() {
        val name = "clean-v26-${System.nanoTime()}"
        databaseNames += name
        val db = buildDatabase(name)
        val sql = db.openHelper.writableDatabase
        assertVersion26TablesExist(sql, startVersion = 26)
        db.close()
    }

    /**
     * Test every migration path that the app officially supports from V1 through V25 to V26.
     * Starts at inferred V1 schema derived by working backward from V26 entities and migrations.
     * Sequentially applies production migrations to build checkpoints V1..V25.
     * Collects failures across all starting versions so a failure in V1 does not block testing V2..V25.
     */
    @Test
    fun migrateAllSupportedVersions_V1ThroughV25_toVersion26() {
        val failures = mutableListOf<String>()

        for (startVersion in 1..25) {
            val name = "migration-v${startVersion}-to-v26-${System.nanoTime()}"
            databaseNames += name

            try {
                // Construct synthetic legacy database checkpoint at startVersion
                val legacyDb = openRawCheckpointDatabase(name, startVersion)
                seedLegacyDataForVersion(legacyDb, startVersion)
                legacyDb.close()

                // Open with Room using full ALL_MIGRATIONS array
                withMigratedDatabase(name) { migratedDb ->
                    val sql = migratedDb.openHelper.writableDatabase
                    assertVersion26TablesExist(sql, startVersion)
                    verifySeededDataSurvived(sql, startVersion)
                }
            } catch (e: Throwable) {
                failures.add("Version $startVersion migration failure: ${e.message}\n${e.stackTraceToString()}")
            }
        }

        if (failures.isNotEmpty()) {
            fail("Migration test matrix failed for ${failures.size} version(s):\n" + failures.joinToString("\n---\n"))
        }
    }

    /**
     * Isolated malformed test: V23 database missing only workout_exercises.targetWeightKg.
     * Verifies that the V23_24 / V24_25 repair migrations add targetWeightKg without breaking existing exercises.
     */
    @Test
    fun restoredMalformedV23_missingTargetWeightKg_repairsAndPreservesWorkouts() {
        val name = "malformed-v23-weight-${System.nanoTime()}"
        databaseNames += name

        val legacy = openRawCheckpointDatabase(name, targetVersion = 23)
        seedLegacyDataForVersion(legacy, version = 23)
        rebuildWorkoutExercisesWithoutTargetWeight(legacy)
        legacy.execSQL(
            "INSERT INTO workout_exercises (id, workoutId, exerciseName, category, targetSets, targetReps, sortOrder) " +
                "VALUES (999, 10, 'Malformed Bench', 'STRENGTH', 3, 10, 2)"
        )
        legacy.close()

        withMigratedDatabase(name) { migrated ->
            val sql = migrated.openHelper.writableDatabase
            assertTrue(
                "[Restored V23 Malformed] workout_exercises.targetWeightKg missing after repair migration",
                columnExists(sql, "workout_exercises", "targetWeightKg")
            )
            assertVersion26TablesExist(sql, startVersion = 23)
            sql.query("SELECT exerciseName FROM workout_exercises WHERE id = 999").use { cursor ->
                assertTrue("[Restored V23 Malformed] workout_exercises row missing after repair", cursor.moveToFirst())
                assertEquals("Malformed Bench", cursor.getString(0))
            }
        }
    }

    /**
     * Isolated malformed test: V23 database missing only set_logs.setType.
     * Verifies that the repair migration adds setType with default 'NORMAL'.
     */
    @Test
    fun restoredMalformedV23_missingSetType_repairsAndPreservesSetLogs() {
        val name = "malformed-v23-settype-${System.nanoTime()}"
        databaseNames += name

        val legacy = openRawCheckpointDatabase(name, targetVersion = 23)
        seedLegacyDataForVersion(legacy, version = 23)
        rebuildSetLogsWithoutSetType(legacy)
        legacy.execSQL(
            "INSERT INTO set_logs (id, sessionId, exerciseName, category, weightKg, reps, durationMin, distanceKm, xp, rir, avgHr, maxHr, speedKmh, inclinePercent, cardioProgram) " +
                "VALUES (999, 50, 'Legacy Press', 'STRENGTH', 60.0, 10, 0.0, 0.0, 30, 2, 110, 130, 0.0, 0.0, '')"
        )
        legacy.close()

        withMigratedDatabase(name) { migrated ->
            val sql = migrated.openHelper.writableDatabase
            assertTrue(
                "[Restored V23 Malformed] set_logs.setType missing after repair migration",
                columnExists(sql, "set_logs", "setType")
            )
            assertVersion26TablesExist(sql, startVersion = 23)
            sql.query("SELECT exerciseName, setType FROM set_logs WHERE id = 999").use { cursor ->
                assertTrue("[Restored V23 Malformed] set_logs row missing after repair", cursor.moveToFirst())
                assertEquals("Legacy Press", cursor.getString(0))
                assertEquals("NORMAL", cursor.getString(1))
            }
        }
    }

    /**
     * Isolated malformed test: V24 database missing only sessions.completionToken / completionReceiptJson.
     * Verifies completion columns and unique index are safely added without deleting session records.
     */
    @Test
    fun restoredMalformedV24_missingCompletionColumns_repairsAndPreservesSessions() {
        val name = "malformed-v24-completion-${System.nanoTime()}"
        databaseNames += name

        val legacy = openRawCheckpointDatabase(name, targetVersion = 24)
        seedLegacyDataForVersion(legacy, version = 24)
        rebuildSessionsWithoutCompletion(legacy)
        legacy.execSQL(
            "INSERT INTO sessions (id, name, startedAt, endedAt, xpEarned, goldEarned, energyEarned, setCount) " +
                "VALUES (999, 'Restored V24 Session', 1000, 2000, 80, 20, 5, 2)"
        )
        legacy.close()

        withMigratedDatabase(name) { migrated ->
            val sql = migrated.openHelper.writableDatabase
            assertTrue(
                "[Restored V24 Malformed] sessions.completionToken missing after repair migration",
                columnExists(sql, "sessions", "completionToken")
            )
            assertTrue(
                "[Restored V24 Malformed] sessions.completionReceiptJson missing after repair migration",
                columnExists(sql, "sessions", "completionReceiptJson")
            )
            assertTrue(
                "[Restored V24 Malformed] index_sessions_completion_token missing after repair migration",
                indexExists(sql, "index_sessions_completion_token")
            )
            assertVersion26TablesExist(sql, startVersion = 24)
            sql.query("SELECT name FROM sessions WHERE id = 999").use { cursor ->
                assertTrue("[Restored V24 Malformed] sessions row missing after repair", cursor.moveToFirst())
                assertEquals("Restored V24 Session", cursor.getString(0))
            }
        }
    }

    /**
     * Test that an existing completion token and receipt in V24 are preserved during V24->V25 migration.
     */
    @Test
    fun migrate24To25_preservesExistingCompletionReceipt() {
        val name = "migration-24-receipt-${System.nanoTime()}"
        databaseNames += name

        val legacy = openRawCheckpointDatabase(name, targetVersion = 24)
        val receipt = "{\"xp\":75,\"gold\":20}"
        legacy.execSQL(
            "INSERT INTO sessions (id, name, startedAt, endedAt, xpEarned, goldEarned, energyEarned, setCount, completionToken, completionReceiptJson) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(3, "Repaired V24", 500, 600, 75, 20, 5, 3, "token-v24", receipt)
        )
        legacy.close()

        withMigratedDatabase(name) { migrated ->
            val sql = migrated.openHelper.writableDatabase
            sql.query("SELECT completionToken, completionReceiptJson FROM sessions WHERE id = 3").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("token-v24", cursor.getString(0))
                assertEquals(receipt, cursor.getString(1))
            }
            assertVersion26TablesExist(sql, startVersion = 24)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Helper Methods & Checkpoint Database Builders
    // ---------------------------------------------------------------------------------------------

    private fun ensureDatabaseDir() {
        dpContext.openOrCreateDatabase("init_test_dir.db", Context.MODE_PRIVATE, null).close()
        dpContext.deleteDatabase("init_test_dir.db")
    }

    private fun openRawDatabase(name: String, version: Int): SupportSQLiteDatabase {
        ensureDatabaseDir()
        val configuration = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(dpContext)
            .name(name)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        return androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
            .create(configuration)
            .writableDatabase
    }

    private fun openRawCheckpointDatabase(name: String, targetVersion: Int): SupportSQLiteDatabase {
        val rawDb = openRawDatabase(name, version = 1)
        createInferredVersion1Schema(rawDb)
        rawDb.version = 1

        for (v in 1 until targetVersion) {
            val migration = AppDatabase.ALL_MIGRATIONS[v - 1]
            migration.migrate(rawDb)
            rawDb.version = v + 1
        }
        return rawDb
    }

    /**
     * Derived inferred V1 schema by working backward from current V25 entity definitions and production migrations:
     * - character: id, name, level, xp, gold, energy, strength, endurance, agility, willpower, strProgress, endProgress, agiProgress, wilProgress, weaponId, armorId, trinketId, battlesWon, sessionsCompleted
     * - workouts: id, name, createdAt, aiGenerated (persistent columns no later migration adds)
     * - workout_exercises: id, workoutId, exerciseName, category, targetSets, targetReps, sortOrder
     * - sessions: id, name, startedAt, endedAt, xpEarned, goldEarned, energyEarned, setCount
     * - set_logs: id, sessionId, exerciseName, category, weightKg, reps, durationMin, distanceKm, xp
     * - items: id, name, emoji, slot, tier, price, atk, def, hp, description, owned
     */
    private fun createInferredVersion1Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS character (
                id INTEGER NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                level INTEGER NOT NULL, xp INTEGER NOT NULL,
                gold INTEGER NOT NULL, energy INTEGER NOT NULL,
                strength INTEGER NOT NULL, endurance INTEGER NOT NULL,
                agility INTEGER NOT NULL, willpower INTEGER NOT NULL,
                strProgress INTEGER NOT NULL, endProgress INTEGER NOT NULL,
                agiProgress INTEGER NOT NULL, wilProgress INTEGER NOT NULL,
                weaponId INTEGER, armorId INTEGER, trinketId INTEGER,
                battlesWon INTEGER NOT NULL, sessionsCompleted INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS workouts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                aiGenerated INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS workout_exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                workoutId INTEGER NOT NULL,
                exerciseName TEXT NOT NULL,
                category TEXT NOT NULL,
                targetSets INTEGER NOT NULL,
                targetReps INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                startedAt INTEGER NOT NULL,
                endedAt INTEGER NOT NULL,
                xpEarned INTEGER NOT NULL,
                goldEarned INTEGER NOT NULL,
                energyEarned INTEGER NOT NULL,
                setCount INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS set_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sessionId INTEGER NOT NULL,
                exerciseName TEXT NOT NULL,
                category TEXT NOT NULL,
                weightKg REAL NOT NULL,
                reps INTEGER NOT NULL,
                durationMin REAL NOT NULL,
                distanceKm REAL NOT NULL,
                xp INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                emoji TEXT NOT NULL,
                slot TEXT NOT NULL,
                tier INTEGER NOT NULL,
                price INTEGER NOT NULL,
                atk INTEGER NOT NULL,
                def INTEGER NOT NULL,
                hp INTEGER NOT NULL,
                description TEXT NOT NULL,
                owned INTEGER NOT NULL
            )"""
        )
    }

    private fun seedLegacyDataForVersion(db: SupportSQLiteDatabase, version: Int) {
        db.execSQL(
            "INSERT INTO character (id, name, level, xp, gold, energy, strength, endurance, agility, willpower, strProgress, endProgress, agiProgress, wilProgress, battlesWon, sessionsCompleted) " +
                "VALUES (1, 'Hero V$version', 5, 500, 100, 20, 10, 10, 10, 10, 0, 0, 0, 0, 2, 1)"
        )
        db.execSQL("INSERT INTO workouts (id, name, createdAt, aiGenerated) VALUES (10, 'Workout V$version', 1000, 0)")
        db.execSQL(
            "INSERT INTO workout_exercises (id, workoutId, exerciseName, category, targetSets, targetReps, sortOrder) " +
                "VALUES (100, 10, 'Squat', 'STRENGTH', 3, 10, 1)"
        )
        db.execSQL(
            "INSERT INTO sessions (id, name, startedAt, endedAt, xpEarned, goldEarned, energyEarned, setCount) " +
                "VALUES (50, 'Session V$version', 1000, 2000, 50, 10, 2, 1)"
        )
        db.execSQL(
            "INSERT INTO set_logs (id, sessionId, exerciseName, category, weightKg, reps, durationMin, distanceKm, xp) " +
                "VALUES (500, 50, 'Squat', 'STRENGTH', 100.0, 10, 0.0, 0.0, 50)"
        )
        db.execSQL("INSERT INTO items (id, name, emoji, slot, tier, price, atk, def, hp, description, owned) VALUES (1, 'Iron Sword', '⚔️', 'WEAPON', 1, 50, 5, 0, 0, 'Basic sword', 1)")

        if (version >= 7) {
            db.execSQL("INSERT INTO gear_instances (id, catalogId, atk, def, hp) VALUES (1, 1, 5, 0, 0)")
        }
        if (version >= 18) {
            db.execSQL(
                "INSERT INTO class_progress (characterId, clazz, level, xp, strength, endurance, agility, willpower, strProgress, endProgress, agiProgress, wilProgress, freeStatPoints) " +
                    "VALUES (1, 'WARRIOR', 5, 500, 10, 10, 10, 10, 0, 0, 0, 0, 0)"
            )
        }
        if (version >= 21) {
            db.execSQL("INSERT INTO body_metrics (id, timestamp, weightKg, note) VALUES (1, 100000, 80.0, 'Baseline')")
        }
    }

    private fun verifySeededDataSurvived(db: SupportSQLiteDatabase, startVersion: Int) {
        db.query("SELECT name FROM character WHERE id = 1").use { cursor ->
            assertTrue("[Migration V$startVersion->V25] character data missing", cursor.moveToFirst())
            assertEquals("Hero V$startVersion", cursor.getString(0))
        }
        db.query("SELECT name FROM workouts WHERE id = 10").use { cursor ->
            assertTrue("[Migration V$startVersion->V25] workouts data missing", cursor.moveToFirst())
            assertEquals("Workout V$startVersion", cursor.getString(0))
        }
        db.query("SELECT exerciseName FROM workout_exercises WHERE id = 100").use { cursor ->
            assertTrue("[Migration V$startVersion->V25] workout_exercises data missing", cursor.moveToFirst())
            assertEquals("Squat", cursor.getString(0))
        }
        db.query("SELECT name FROM sessions WHERE id = 50").use { cursor ->
            assertTrue("[Migration V$startVersion->V25] sessions data missing", cursor.moveToFirst())
            assertEquals("Session V$startVersion", cursor.getString(0))
        }
        db.query("SELECT exerciseName FROM set_logs WHERE id = 500").use { cursor ->
            assertTrue("[Migration V$startVersion->V25] set_logs data missing", cursor.moveToFirst())
            assertEquals("Squat", cursor.getString(0))
        }
        db.query("SELECT name FROM items WHERE id = 1").use { cursor ->
            assertTrue("[Migration V$startVersion->V25] items data missing", cursor.moveToFirst())
            assertEquals("Iron Sword", cursor.getString(0))
        }

        if (startVersion >= 7) {
            db.query("SELECT catalogId FROM gear_instances WHERE id = 1").use { cursor ->
                assertTrue("[Migration V$startVersion->V25] gear_instances data missing", cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }
        if (startVersion >= 18) {
            db.query("SELECT clazz FROM class_progress WHERE characterId = 1").use { cursor ->
                assertTrue("[Migration V$startVersion->V25] class_progress data missing", cursor.moveToFirst())
                assertEquals("WARRIOR", cursor.getString(0))
            }
        }
        if (startVersion >= 21) {
            db.query("SELECT weightKg FROM body_metrics WHERE id = 1").use { cursor ->
                assertTrue("[Migration V$startVersion->V25] body_metrics data missing", cursor.moveToFirst())
                assertEquals(80.0, cursor.getDouble(0), 0.001)
            }
        }
    }

    private fun rebuildSessionsWithoutCompletion(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS index_sessions_completion_token")
        db.execSQL("ALTER TABLE sessions RENAME TO sessions_current")
        db.execSQL(
            """CREATE TABLE sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                startedAt INTEGER NOT NULL,
                endedAt INTEGER NOT NULL,
                xpEarned INTEGER NOT NULL,
                goldEarned INTEGER NOT NULL,
                energyEarned INTEGER NOT NULL,
                setCount INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            "INSERT INTO sessions (id, name, startedAt, endedAt, xpEarned, goldEarned, energyEarned, setCount) " +
                "SELECT id, name, startedAt, endedAt, xpEarned, goldEarned, energyEarned, setCount " +
                "FROM sessions_current"
        )
        db.execSQL("DROP TABLE sessions_current")
    }

    private fun rebuildWorkoutExercisesWithoutTargetWeight(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workout_exercises RENAME TO workout_exercises_current")
        db.execSQL(
            """CREATE TABLE workout_exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                workoutId INTEGER NOT NULL,
                exerciseName TEXT NOT NULL,
                category TEXT NOT NULL,
                targetSets INTEGER NOT NULL,
                targetReps INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            "INSERT INTO workout_exercises (id, workoutId, exerciseName, category, targetSets, targetReps, sortOrder) " +
                "SELECT id, workoutId, exerciseName, category, targetSets, targetReps, sortOrder " +
                "FROM workout_exercises_current"
        )
        db.execSQL("DROP TABLE workout_exercises_current")
    }

    private fun rebuildSetLogsWithoutSetType(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE set_logs RENAME TO set_logs_current")
        db.execSQL(
            """CREATE TABLE set_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sessionId INTEGER NOT NULL,
                exerciseName TEXT NOT NULL,
                category TEXT NOT NULL,
                weightKg REAL NOT NULL,
                reps INTEGER NOT NULL,
                durationMin REAL NOT NULL,
                distanceKm REAL NOT NULL,
                xp INTEGER NOT NULL,
                rir INTEGER,
                avgHr INTEGER,
                maxHr INTEGER,
                speedKmh REAL NOT NULL,
                inclinePercent REAL NOT NULL,
                cardioProgram TEXT NOT NULL
            )"""
        )
        db.execSQL(
            "INSERT INTO set_logs (id, sessionId, exerciseName, category, weightKg, reps, durationMin, " +
                "distanceKm, xp, rir, avgHr, maxHr, speedKmh, inclinePercent, cardioProgram) " +
                "SELECT id, sessionId, exerciseName, category, weightKg, reps, durationMin, distanceKm, " +
                "xp, rir, avgHr, maxHr, speedKmh, inclinePercent, cardioProgram FROM set_logs_current"
        )
        db.execSQL("DROP TABLE set_logs_current")
    }

    private fun buildDatabase(name: String): AppDatabase {
        ensureDatabaseDir()
        return Room.databaseBuilder(dpContext, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
    }

    private fun openMigratedDatabase(name: String): AppDatabase =
        buildDatabase(name).also { it.openHelper.writableDatabase }

    private inline fun withMigratedDatabase(name: String, block: (AppDatabase) -> Unit) {
        val database = openMigratedDatabase(name)
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    private fun columnExists(db: SupportSQLiteDatabase, table: String, column: String): Boolean =
        db.query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            generateSequence { if (cursor.moveToNext()) cursor else null }
                .any { it.getString(nameIndex) == column }
        }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean =
        db.query("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(table)).use {
            it.moveToFirst()
        }

    private fun indexExists(db: SupportSQLiteDatabase, index: String): Boolean =
        db.query("SELECT 1 FROM sqlite_master WHERE type = 'index' AND name = ?", arrayOf(index)).use {
            it.moveToFirst()
        }

    private fun assertVersion26TablesExist(db: SupportSQLiteDatabase, startVersion: Int) {
        val requiredTables = listOf(
            "character", "workouts", "workout_exercises", "sessions", "set_logs",
            "items", "gear_instances", "biome_progress", "class_progress", "body_metrics",
            "active_sessions", "active_session_exercises", "active_session_set_logs", "pending_sync_outbox",
            "movement_mastery"
        )
        for (table in requiredTables) {
            assertTrue(
                "[Migration V$startVersion->V26] Expected V26 table '$table' does not exist",
                tableExists(db, table)
            )
        }
        assertTrue(
            "[Migration V$startVersion->V26] Expected unique index 'index_movement_mastery_characterId_canonicalKey' missing",
            indexExists(db, "index_movement_mastery_characterId_canonicalKey")
        )
        assertTrue(
            "[Migration V$startVersion->V26] Expected index 'index_movement_mastery_category' missing",
            indexExists(db, "index_movement_mastery_category")
        )
        assertFalse(
            "[Migration V$startVersion->V26] Unexpected temp table 'sessions_current' was left in database",
            tableExists(db, "sessions_current")
        )
    }
}
