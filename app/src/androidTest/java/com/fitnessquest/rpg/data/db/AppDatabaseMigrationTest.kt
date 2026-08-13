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
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseNames = mutableSetOf<String>()

    @After
    fun cleanUp() {
        databaseNames.forEach(context::deleteDatabase)
    }

    @Test
    fun migrate23To25_repairsColumnsAndPreservesHistory() {
        val name = "migration-23-${System.nanoTime()}"
        val legacy = prepareLegacyDatabase(name, version = 23, keepCompletionColumns = false)
        legacy.execSQL(
            "INSERT INTO sessions " +
                "(id, name, startedAt, endedAt, xpEarned, goldEarned, energyEarned, setCount) " +
                "VALUES (1, 'Legacy Session', 100, 200, 45, 12, 3, 1)"
        )
        legacy.execSQL(
            "INSERT INTO workout_exercises " +
                "(id, workoutId, exerciseName, category, targetSets, targetReps, sortOrder) " +
                "VALUES (1, 99, 'Legacy Squat', 'STRENGTH', 3, 8, 0)"
        )
        legacy.execSQL(
            "INSERT INTO set_logs " +
                "(id, sessionId, exerciseName, category, weightKg, reps, durationMin, distanceKm, xp, " +
                "rir, avgHr, maxHr, speedKmh, inclinePercent, cardioProgram) " +
                "VALUES (1, 1, 'Legacy Squat', 'STRENGTH', 100.0, 8, 0.0, 0.0, 45, " +
                "2, 120, 150, 0.0, 0.0, '')"
        )
        legacy.close()

        withMigratedDatabase(name) { migrated ->
            val sql = migrated.openHelper.writableDatabase
            assertTrue(columnExists(sql, "workout_exercises", "targetWeightKg"))
            assertTrue(columnExists(sql, "set_logs", "setType"))
            assertTrue(columnExists(sql, "sessions", "completionToken"))
            assertTrue(columnExists(sql, "sessions", "completionReceiptJson"))
            assertTrue(indexExists(sql, "index_sessions_completion_token"))
            assertVersion25TablesExist(sql)

            sql.query("SELECT name, xpEarned FROM sessions WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Legacy Session", cursor.getString(0))
                assertEquals(45, cursor.getInt(1))
            }
            sql.query("SELECT setType FROM set_logs WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("NORMAL", cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate24To25_addsMissingCompletionColumns() {
        val name = "migration-24-missing-${System.nanoTime()}"
        val legacy = prepareLegacyDatabase(name, version = 24, keepCompletionColumns = false)
        legacy.execSQL(
            "INSERT INTO sessions " +
                "(id, name, startedAt, endedAt, xpEarned, goldEarned, energyEarned, setCount) " +
                "VALUES (2, 'Released V24', 300, 400, 60, 15, 4, 2)"
        )
        legacy.close()

        withMigratedDatabase(name) { migrated ->
            val sql = migrated.openHelper.writableDatabase
            assertTrue(columnExists(sql, "sessions", "completionToken"))
            assertTrue(columnExists(sql, "sessions", "completionReceiptJson"))
            assertTrue(indexExists(sql, "index_sessions_completion_token"))
            assertVersion25TablesExist(sql)
            sql.query("SELECT name FROM sessions WHERE id = 2").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Released V24", cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate24To25_preservesExistingCompletionReceipt() {
        val name = "migration-24-repaired-${System.nanoTime()}"
        val legacy = prepareLegacyDatabase(name, version = 24, keepCompletionColumns = true)
        val receipt = "{\"xp\":75,\"gold\":20}"
        legacy.execSQL(
            "INSERT INTO sessions " +
                "(id, name, startedAt, endedAt, xpEarned, goldEarned, energyEarned, setCount, " +
                "completionToken, completionReceiptJson) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(3, "Repaired V24", 500, 600, 75, 20, 5, 3, "token-v24", receipt)
        )
        legacy.close()

        withMigratedDatabase(name) { migrated ->
            val sql = migrated.openHelper.writableDatabase
            sql.query(
                "SELECT completionToken, completionReceiptJson FROM sessions WHERE id = 3"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("token-v24", cursor.getString(0))
                assertEquals(receipt, cursor.getString(1))
            }
            assertVersion25TablesExist(sql)
        }
    }

    private fun prepareLegacyDatabase(
        name: String,
        version: Int,
        keepCompletionColumns: Boolean
    ): SupportSQLiteDatabase {
        databaseNames += name
        val room = buildDatabase(name)
        val sql = room.openHelper.writableDatabase
        sql.execSQL("PRAGMA foreign_keys = OFF")
        sql.execSQL("DROP TABLE IF EXISTS active_session_set_logs")
        sql.execSQL("DROP TABLE IF EXISTS active_session_exercises")
        sql.execSQL("DROP TABLE IF EXISTS active_sessions")
        sql.execSQL("DROP TABLE IF EXISTS pending_sync_outbox")

        if (!keepCompletionColumns) rebuildSessionsWithoutCompletion(sql)
        if (version == 23) {
            rebuildWorkoutExercisesWithoutTargetWeight(sql)
            rebuildSetLogsWithoutSetType(sql)
        }

        sql.version = version
        room.close()
        return openRawDatabase(name, version)
    }

    private fun openRawDatabase(name: String, version: Int): SupportSQLiteDatabase {
        val configuration = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
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

    private fun buildDatabase(name: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_23_24, AppDatabase.MIGRATION_24_25)
            .allowMainThreadQueries()
            .build()

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

    private fun assertVersion25TablesExist(db: SupportSQLiteDatabase) {
        assertTrue(tableExists(db, "active_sessions"))
        assertTrue(tableExists(db, "active_session_exercises"))
        assertTrue(tableExists(db, "active_session_set_logs"))
        assertTrue(tableExists(db, "pending_sync_outbox"))
        assertFalse(tableExists(db, "sessions_current"))
    }
}
