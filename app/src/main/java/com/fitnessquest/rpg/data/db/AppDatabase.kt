package com.fitnessquest.rpg.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.SetType

class Converters {
    @TypeConverter
    fun categoryToString(value: ExerciseCategory): String = value.name

    @TypeConverter
    fun stringToCategory(value: String): ExerciseCategory = ExerciseCategory.valueOf(value)

    @TypeConverter
    fun slotToString(value: ItemSlot): String = value.name

    @TypeConverter
    fun stringToSlot(value: String): ItemSlot = ItemSlot.valueOf(value)

    @TypeConverter
    fun classToString(value: CharacterClass?): String? = value?.name

    @TypeConverter
    fun stringToClass(value: String?): CharacterClass? = value?.let { CharacterClass.valueOf(it) }

    @TypeConverter
    fun setTypeToString(value: SetType): String = value.name

    @TypeConverter
    fun stringToSetType(value: String): SetType = SetType.valueOf(value)
}

@Database(
    entities = [
        CharacterEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        SessionEntity::class,
        SetLogEntity::class,
        ItemEntity::class,
        GearInstanceEntity::class,
        BiomeProgressEntity::class,
        ClassProgressEntity::class,
        BodyMetricEntity::class,
        ActiveSessionEntity::class,
        ActiveExerciseEntity::class,
        ActiveSetLogEntity::class,
        PendingSyncEntity::class,
        MovementMasteryEntity::class
    ],
    version = 26,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun sessionDao(): SessionDao
    abstract fun itemDao(): ItemDao
    abstract fun gearInstanceDao(): GearInstanceDao
    abstract fun biomeProgressDao(): BiomeProgressDao
    abstract fun classProgressDao(): ClassProgressDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun activeSessionDao(): ActiveSessionDao
    abstract fun movementMasteryDao(): MovementMasteryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("character", "characterClass", "TEXT")
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("items", "classAffinity", "TEXT")
                db.addColumnIfNotExists("items", "style", "TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Splits the single armor slot into head/chest/hands/legs/feet and adds biome travel. */
        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE character_new (
                        id INTEGER NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        characterClass TEXT,
                        level INTEGER NOT NULL, xp INTEGER NOT NULL,
                        gold INTEGER NOT NULL, energy INTEGER NOT NULL,
                        strength INTEGER NOT NULL, endurance INTEGER NOT NULL,
                        agility INTEGER NOT NULL, willpower INTEGER NOT NULL,
                        strProgress INTEGER NOT NULL, endProgress INTEGER NOT NULL,
                        agiProgress INTEGER NOT NULL, wilProgress INTEGER NOT NULL,
                        weaponId INTEGER, headId INTEGER, chestId INTEGER,
                        handsId INTEGER, legsId INTEGER, feetId INTEGER, trinketId INTEGER,
                        battlesWon INTEGER NOT NULL, sessionsCompleted INTEGER NOT NULL,
                        currentBiome TEXT NOT NULL DEFAULT 'MEADOWLANDS',
                        travelTarget TEXT,
                        travelProgress REAL NOT NULL DEFAULT 0
                    )"""
                )
                db.execSQL(
                    """INSERT INTO character_new (
                        id, name, characterClass, level, xp, gold, energy,
                        strength, endurance, agility, willpower,
                        strProgress, endProgress, agiProgress, wilProgress,
                        weaponId, chestId, trinketId, battlesWon, sessionsCompleted
                    ) SELECT
                        id, name, characterClass, level, xp, gold, energy,
                        strength, endurance, agility, willpower,
                        strProgress, endProgress, agiProgress, wilProgress,
                        weaponId, armorId, trinketId, battlesWon, sessionsCompleted
                    FROM character"""
                )
                db.execSQL("DROP TABLE character")
                db.execSQL("ALTER TABLE character_new RENAME TO character")
                db.execSQL("UPDATE items SET slot = 'CHEST' WHERE slot = 'ARMOR'")
            }
        }

        /** Adds workout streaks, consumable supplies, and stat respec support. */
        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("character", "streak", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("character", "lastWorkoutDay", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("character", "pendingXpBoost", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("character", "freeStatPoints", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("items", "quantity", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Adds per-set effort tracking (RIR/RPE). */
        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("set_logs", "rir", "INTEGER")
            }
        }

        /**
         * Gear instances: owned equippable items become unique copies.
         * Character equip slot IDs are remapped from catalog id → instance id.
         */
        internal val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS gear_instances (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        catalogId INTEGER NOT NULL,
                        atk INTEGER NOT NULL,
                        def INTEGER NOT NULL,
                        hp INTEGER NOT NULL,
                        rune1Id INTEGER,
                        rune2Id INTEGER
                    )"""
                )
                // One instance per previously owned equippable catalog item.
                db.execSQL(
                    """INSERT INTO gear_instances (catalogId, atk, def, hp, rune1Id, rune2Id)
                       SELECT id, atk, def, hp, NULL, NULL FROM items
                       WHERE owned = 1 AND slot IN (
                         'WEAPON','HEAD','CHEST','HANDS','LEGS','FEET','TRINKET'
                       )"""
                )
                // Remap each equip slot from catalog id to the matching instance id.
                for (col in listOf("weaponId", "headId", "chestId", "handsId", "legsId", "feetId", "trinketId")) {
                    db.execSQL(
                        """UPDATE character SET $col = (
                            SELECT gi.id FROM gear_instances gi
                            WHERE gi.catalogId = character.$col LIMIT 1
                        ) WHERE $col IS NOT NULL"""
                    )
                }
            }
        }

        internal val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("set_logs", "avgHr", "INTEGER")
                db.addColumnIfNotExists("set_logs", "maxHr", "INTEGER")
            }
        }

        internal val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("character", "idleSteps", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("character", "idleKills", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("character", "idleGold", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("character", "idleXp", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        internal val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("character", "skinColor", "INTEGER NOT NULL DEFAULT 4293108103")
                db.addColumnIfNotExists("character", "hairColor", "INTEGER NOT NULL DEFAULT 4285229618")
                db.addColumnIfNotExists("character", "underwearColor", "INTEGER NOT NULL DEFAULT 4283319894")
                db.addColumnIfNotExists("character", "eyeColor", "INTEGER NOT NULL DEFAULT 4280951347")
                db.addColumnIfNotExists("character", "hairStyle", "TEXT NOT NULL DEFAULT 'short'")
            }
        }

        internal val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("character", "gender", "TEXT NOT NULL DEFAULT 'male'")
                db.addColumnIfNotExists("character", "braColor", "INTEGER NOT NULL DEFAULT 4283319894")
            }
        }

        /** Cardio machine intensity: speed, incline, program. */
        internal val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("set_logs", "speedKmh", "REAL NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("set_logs", "inclinePercent", "REAL NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("set_logs", "cardioProgram", "TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Fantasy race for avatar cosmetics. */
        internal val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("character", "race", "TEXT NOT NULL DEFAULT 'HUMAN'")
            }
        }

        /** Druid shapeshifting, persistent HP, and wellness tracking. */
        internal val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("character", "currentHp", "INTEGER")
                db.addColumnIfNotExists("character", "druidForm", "TEXT NOT NULL DEFAULT 'HUMAN'")
                db.addColumnIfNotExists("character", "lastWellnessDay", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Passive energy recoup tracking. */
        internal val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE character ADD COLUMN lastEnergyUpdate INTEGER NOT NULL DEFAULT 0")
                // Seed with current time so players start recouping from now.
                val now = System.currentTimeMillis()
                db.execSQL("UPDATE character SET lastEnergyUpdate = $now")
            }
        }

        /** Sync party and guild IDs to cloud. */
        internal val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("character", "partyId", "TEXT")
                db.addColumnIfNotExists("character", "guildId", "TEXT")
            }
        }

        /** Migrate progress and profile fields from SharedPreferences to DB. */
        internal val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("character", "bodyWeightKg", "REAL")
                db.addColumnIfNotExists("character", "heightM", "REAL")
                db.addColumnIfNotExists("character", "dateOfBirthEpoch", "INTEGER")
                db.addColumnIfNotExists("character", "trainingEquipment", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfNotExists("character", "trainingDaysPerWeek", "INTEGER NOT NULL DEFAULT 3")
                db.addColumnIfNotExists("character", "trainingSplit", "TEXT NOT NULL DEFAULT 'FULL_BODY'")
                db.addColumnIfNotExists("character", "trainingLevel", "TEXT NOT NULL DEFAULT 'BEGINNER'")
                db.addColumnIfNotExists("character", "fitnessGoal", "TEXT NOT NULL DEFAULT 'BUILD_MUSCLE'")
                db.addColumnIfNotExists("character", "muscleFocus", "TEXT NOT NULL DEFAULT 'BALANCED'")
                db.addColumnIfNotExists("character", "cardioPlacement", "TEXT NOT NULL DEFAULT 'NONE'")
                db.addColumnIfNotExists("character", "workoutDuration", "TEXT NOT NULL DEFAULT 'STANDARD'")
                db.addColumnIfNotExists("character", "effortMethod", "TEXT NOT NULL DEFAULT 'OFF'")
                db.addColumnIfNotExists("character", "plateBarKg", "REAL")
                db.addColumnIfNotExists("character", "claimedTrophies", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfNotExists("character", "bountyDay", "INTEGER NOT NULL DEFAULT -1")
                db.addColumnIfNotExists("character", "bountyBattlesStart", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("character", "waterGlasses", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("character", "stretchDone", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("character", "claimedBounties", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfNotExists("character", "campaignWeek", "INTEGER NOT NULL DEFAULT -1")
                db.addColumnIfNotExists("character", "campaignBattlesStart", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("character", "claimedCampaigns", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfNotExists("character", "encounterClaimedThisTravel", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("character", "pendingEncounterStrBoost", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("character", "firstWorkoutDone", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** FFXI-style Job system: per-class progress. */
        internal val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE class_progress (
                        characterId INTEGER NOT NULL,
                        clazz TEXT NOT NULL,
                        level INTEGER NOT NULL DEFAULT 1,
                        xp INTEGER NOT NULL DEFAULT 0,
                        strength INTEGER NOT NULL DEFAULT 1,
                        endurance INTEGER NOT NULL DEFAULT 1,
                        agility INTEGER NOT NULL DEFAULT 1,
                        willpower INTEGER NOT NULL DEFAULT 1,
                        strProgress INTEGER NOT NULL DEFAULT 0,
                        endProgress INTEGER NOT NULL DEFAULT 0,
                        agiProgress INTEGER NOT NULL DEFAULT 0,
                        wilProgress INTEGER NOT NULL DEFAULT 0,
                        weaponId INTEGER, headId INTEGER, chestId INTEGER,
                        handsId INTEGER, legsId INTEGER, feetId INTEGER, trinketId INTEGER,
                        freeStatPoints INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(characterId, clazz)
                    )"""
                )
                // Seed the current class progress from character table if a class is chosen.
                db.execSQL(
                    """INSERT INTO class_progress (
                        characterId, clazz, level, xp, strength, endurance, agility, willpower,
                        strProgress, endProgress, agiProgress, wilProgress,
                        weaponId, headId, chestId, handsId, legsId, feetId, trinketId, freeStatPoints
                    ) SELECT
                        id, characterClass, level, xp, strength, endurance, agility, willpower,
                        strProgress, endProgress, agiProgress, wilProgress,
                        weaponId, headId, chestId, handsId, legsId, feetId, trinketId, freeStatPoints
                    FROM character WHERE characterClass IS NOT NULL"""
                )
            }
        }

        /** Progression foundation: biome boss state and forge-ready gear metadata. */
        internal val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("gear_instances", "upgradeLevel", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("gear_instances", "rarity", "TEXT NOT NULL DEFAULT 'COMMON'")
                db.addColumnIfNotExists("gear_instances", "traitIds", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfNotExists("gear_instances", "originBiome", "TEXT")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS biome_progress (
                        biomeName TEXT NOT NULL PRIMARY KEY,
                        layer INTEGER NOT NULL DEFAULT 1,
                        bossUnlocked INTEGER NOT NULL DEFAULT 0,
                        bossDefeated INTEGER NOT NULL DEFAULT 0,
                        progressPoints INTEGER NOT NULL DEFAULT 0,
                        firstClearRewardClaimed INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                listOf("MEADOWLANDS", "DARKWOOD", "CRYSTAL_CAVES", "EMBER_PEAKS", "FROZEN_WASTES", "SHADOWFEN").forEach { biome ->
                    db.execSQL(
                        """INSERT OR IGNORE INTO biome_progress (
                            biomeName, layer, bossUnlocked, bossDefeated, progressPoints, firstClearRewardClaimed
                        ) VALUES ('$biome', 1, 0, 0, 0, 0)"""
                    )
                }
            }
        }

        /** Pending idle loot is claimed through reward reveal instead of silently applied. */
        internal val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("character", "idlePendingLoot", "TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Daily weight history tracking. */
        internal val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS body_metrics (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        weightKg REAL NOT NULL,
                        heightM REAL,
                        note TEXT NOT NULL DEFAULT ''
                    )"""
                )
            }
        }

        internal val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("workout_exercises", "targetWeightKg", "REAL")
                db.addColumnIfNotExists("set_logs", "setType", "TEXT NOT NULL DEFAULT 'NORMAL'")
            }
        }

        internal val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // "Repair" migration in case 21->22 was botched or skipped
                db.addColumnIfNotExists("workout_exercises", "targetWeightKg", "REAL")
                db.addColumnIfNotExists("set_logs", "setType", "TEXT NOT NULL DEFAULT 'NORMAL'")
            }
        }

        internal val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // "Repair" migration in case 21->22/22->23 was botched or skipped
                db.addColumnIfNotExists("workout_exercises", "targetWeightKg", "REAL")
                db.addColumnIfNotExists("set_logs", "setType", "TEXT NOT NULL DEFAULT 'NORMAL'")
                db.addColumnIfNotExists("sessions", "completionToken", "TEXT")
                db.addColumnIfNotExists("sessions", "completionReceiptJson", "TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sessions_completion_token ON sessions(completionToken)")
            }
        }

        internal val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS active_sessions (
                        id INTEGER PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        workoutId INTEGER,
                        startedAt INTEGER NOT NULL,
                        pausedAt INTEGER,
                        accumulatedPausedMs INTEGER NOT NULL DEFAULT 0,
                        restEndsAt INTEGER,
                        restDurationSec INTEGER NOT NULL DEFAULT 90,
                        heatStreak INTEGER NOT NULL DEFAULT 0,
                        lastLogAt INTEGER NOT NULL DEFAULT 0,
                        currentExerciseIndex INTEGER NOT NULL DEFAULT 0,
                        ambushOfferedThisSession INTEGER NOT NULL DEFAULT 0,
                        ambushXpMult REAL NOT NULL DEFAULT 1.0,
                        momentSpoilsUsed INTEGER NOT NULL DEFAULT 0,
                        status TEXT NOT NULL DEFAULT 'ACTIVE',
                        completionToken TEXT
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS active_session_exercises (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        activeSessionId INTEGER NOT NULL DEFAULT 1,
                        exerciseName TEXT NOT NULL,
                        category TEXT NOT NULL,
                        targetSets INTEGER NOT NULL DEFAULT 3,
                        targetReps INTEGER NOT NULL DEFAULT 10,
                        targetWeightKg REAL,
                        trackingType TEXT NOT NULL DEFAULT 'WEIGHT_REPS',
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        notes TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(activeSessionId) REFERENCES active_sessions(id) ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_active_session_exercises_activeSessionId ON active_session_exercises(activeSessionId)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS active_session_set_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        activeSessionId INTEGER NOT NULL DEFAULT 1,
                        exerciseId INTEGER NOT NULL,
                        exerciseName TEXT NOT NULL,
                        category TEXT NOT NULL,
                        weightKg REAL NOT NULL DEFAULT 0.0,
                        reps INTEGER NOT NULL DEFAULT 0,
                        durationMin REAL NOT NULL DEFAULT 0.0,
                        distanceKm REAL NOT NULL DEFAULT 0.0,
                        xp INTEGER NOT NULL DEFAULT 0,
                        rir INTEGER,
                        avgHr INTEGER,
                        maxHr INTEGER,
                        speedKmh REAL NOT NULL DEFAULT 0.0,
                        inclinePercent REAL NOT NULL DEFAULT 0.0,
                        cardioProgram TEXT NOT NULL DEFAULT '',
                        setType TEXT NOT NULL DEFAULT 'NORMAL',
                        loggedAt INTEGER NOT NULL,
                        FOREIGN KEY(activeSessionId) REFERENCES active_sessions(id) ON DELETE CASCADE,
                        FOREIGN KEY(exerciseId) REFERENCES active_session_exercises(id) ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_active_session_set_logs_activeSessionId ON active_session_set_logs(activeSessionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_active_session_set_logs_exerciseId ON active_session_set_logs(exerciseId)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS pending_sync_outbox (
                        eventId TEXT PRIMARY KEY NOT NULL,
                        type TEXT NOT NULL,
                        payloadJson TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        retryCount INTEGER NOT NULL DEFAULT 0
                    )"""
                )

                db.addColumnIfNotExists("sessions", "completionToken", "TEXT")
                db.addColumnIfNotExists("sessions", "completionReceiptJson", "TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sessions_completion_token ON sessions(completionToken)")
            }
        }

        internal val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `movement_mastery` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `characterId` INTEGER NOT NULL,
                        `canonicalKey` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `level` INTEGER NOT NULL DEFAULT 1,
                        `currentXp` INTEGER NOT NULL DEFAULT 0,
                        `lifetimeVolumeKg` REAL NOT NULL DEFAULT 0.0,
                        `lifetimeReps` INTEGER NOT NULL DEFAULT 0,
                        `lifetimeDistanceKm` REAL NOT NULL DEFAULT 0.0,
                        `lifetimeDurationSec` INTEGER NOT NULL DEFAULT 0,
                        `totalSessionsLogged` INTEGER NOT NULL DEFAULT 0,
                        `highest1RmKg` REAL NOT NULL DEFAULT 0.0,
                        `highestWeightKg` REAL NOT NULL DEFAULT 0.0,
                        `bestDistanceKm` REAL NOT NULL DEFAULT 0.0,
                        `bestPaceSecPerKm` INTEGER NOT NULL DEFAULT 0,
                        `lastTrainedEpochMs` INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_movement_mastery_characterId_canonicalKey` ON `movement_mastery` (`characterId`, `canonicalKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_movement_mastery_category` ON `movement_mastery` (`category`)")
            }
        }

        internal val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
            MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15,
            MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20,
            MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25,
            MIGRATION_25_26
        )

        private fun SupportSQLiteDatabase.addColumnIfNotExists(table: String, column: String, definition: String) {
            if (!columnExists(this, table, column)) {
                this.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
            }
        }

        private fun columnExists(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
            val cursor = db.query("PRAGMA table_info($table)")
            cursor.use {
                val nameIdx = it.getColumnIndex("name")
                if (nameIdx == -1) return false
                while (it.moveToNext()) {
                    if (it.getString(nameIdx) == column) return true
                }
            }
            return false
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitquest.db"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build().also { instance = it }
            }
    }
}
