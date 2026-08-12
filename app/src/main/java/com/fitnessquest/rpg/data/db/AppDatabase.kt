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
        BodyMetricEntity::class
    ],
    version = 24,
    exportSchema = false
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

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE character ADD COLUMN characterClass TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN classAffinity TEXT")
                db.execSQL("ALTER TABLE items ADD COLUMN style TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Splits the single armor slot into head/chest/hands/legs/feet and adds biome travel. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
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
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE character ADD COLUMN streak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE character ADD COLUMN lastWorkoutDay INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE character ADD COLUMN pendingXpBoost INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE character ADD COLUMN freeStatPoints INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE items ADD COLUMN quantity INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Adds per-set effort tracking (RIR/RPE). */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE set_logs ADD COLUMN rir INTEGER")
            }
        }

        /**
         * Gear instances: owned equippable items become unique copies.
         * Character equip slot IDs are remapped from catalog id → instance id.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
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

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE set_logs ADD COLUMN avgHr INTEGER")
                db.execSQL("ALTER TABLE set_logs ADD COLUMN maxHr INTEGER")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE character ADD COLUMN idleSteps INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE character ADD COLUMN idleKills INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE character ADD COLUMN idleGold INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE character ADD COLUMN idleXp INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE character ADD COLUMN skinColor INTEGER NOT NULL DEFAULT 4293108103")
                db.execSQL("ALTER TABLE character ADD COLUMN hairColor INTEGER NOT NULL DEFAULT 4285229618")
                db.execSQL("ALTER TABLE character ADD COLUMN underwearColor INTEGER NOT NULL DEFAULT 4283319894")
                db.execSQL("ALTER TABLE character ADD COLUMN eyeColor INTEGER NOT NULL DEFAULT 4280951347")
                db.execSQL("ALTER TABLE character ADD COLUMN hairStyle TEXT NOT NULL DEFAULT 'short'")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE character ADD COLUMN gender TEXT NOT NULL DEFAULT 'male'")
                db.execSQL("ALTER TABLE character ADD COLUMN braColor INTEGER NOT NULL DEFAULT 4283319894")
            }
        }

        /** Cardio machine intensity: speed, incline, program. */
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE set_logs ADD COLUMN speedKmh REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE set_logs ADD COLUMN inclinePercent REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE set_logs ADD COLUMN cardioProgram TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Fantasy race for avatar cosmetics. */
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE character ADD COLUMN race TEXT NOT NULL DEFAULT 'HUMAN'")
            }
        }

        /** Druid shapeshifting, persistent HP, and wellness tracking. */
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE character ADD COLUMN currentHp INTEGER")
                db.execSQL("ALTER TABLE character ADD COLUMN druidForm TEXT NOT NULL DEFAULT 'HUMAN'")
                db.execSQL("ALTER TABLE character ADD COLUMN lastWellnessDay INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Passive energy recoup tracking. */
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE character ADD COLUMN lastEnergyUpdate INTEGER NOT NULL DEFAULT 0")
                // Seed with current time so players start recouping from now.
                val now = System.currentTimeMillis()
                db.execSQL("UPDATE character SET lastEnergyUpdate = $now")
            }
        }

        /** Sync party and guild IDs to cloud. */
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE character ADD COLUMN partyId TEXT")
                db.execSQL("ALTER TABLE character ADD COLUMN guildId TEXT")
            }
        }

        /** Migrate progress and profile fields from SharedPreferences to DB. */
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE character ADD COLUMN bodyWeightKg REAL")
                db.execSQL("ALTER TABLE character ADD COLUMN heightM REAL")
                db.execSQL("ALTER TABLE character ADD COLUMN dateOfBirthEpoch INTEGER")
                db.execSQL("ALTER TABLE character ADD COLUMN trainingEquipment TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE character ADD COLUMN trainingDaysPerWeek INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE character ADD COLUMN trainingSplit TEXT NOT NULL DEFAULT 'FULL_BODY'")
                db.execSQL("ALTER TABLE character ADD COLUMN trainingLevel TEXT NOT NULL DEFAULT 'BEGINNER'")
                db.execSQL("ALTER TABLE character ADD COLUMN fitnessGoal TEXT NOT NULL DEFAULT 'BUILD_MUSCLE'")
                db.execSQL("ALTER TABLE character ADD COLUMN muscleFocus TEXT NOT NULL DEFAULT 'BALANCED'")
                db.execSQL("ALTER TABLE character ADD COLUMN cardioPlacement TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE character ADD COLUMN workoutDuration TEXT NOT NULL DEFAULT 'STANDARD'")
                db.execSQL("ALTER TABLE character ADD COLUMN effortMethod TEXT NOT NULL DEFAULT 'OFF'")
                db.execSQL("ALTER TABLE character ADD COLUMN plateBarKg REAL")
                db.execSQL("ALTER TABLE character ADD COLUMN claimedTrophies TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE character ADD COLUMN bountyDay INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE character ADD COLUMN bountyBattlesStart INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE character ADD COLUMN waterGlasses INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE character ADD COLUMN stretchDone INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE character ADD COLUMN claimedBounties TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE character ADD COLUMN campaignWeek INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE character ADD COLUMN campaignBattlesStart INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE character ADD COLUMN claimedCampaigns TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE character ADD COLUMN encounterClaimedThisTravel INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE character ADD COLUMN pendingEncounterStrBoost INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE character ADD COLUMN firstWorkoutDone INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** FFXI-style Job system: per-class progress. */
        private val MIGRATION_17_18 = object : Migration(17, 18) {
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
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE gear_instances ADD COLUMN upgradeLevel INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE gear_instances ADD COLUMN rarity TEXT NOT NULL DEFAULT 'COMMON'")
                db.execSQL("ALTER TABLE gear_instances ADD COLUMN traitIds TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE gear_instances ADD COLUMN originBiome TEXT")
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
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE character ADD COLUMN idlePendingLoot TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Daily weight history tracking. */
        private val MIGRATION_20_21 = object : Migration(20, 21) {
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

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN targetWeightKg REAL")
                db.execSQL("ALTER TABLE set_logs ADD COLUMN setType TEXT NOT NULL DEFAULT 'NORMAL'")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // "Repair" migration in case 21->22 was botched or skipped
                runCatching { db.execSQL("ALTER TABLE workout_exercises ADD COLUMN targetWeightKg REAL") }
                runCatching { db.execSQL("ALTER TABLE set_logs ADD COLUMN setType TEXT NOT NULL DEFAULT 'NORMAL'") }
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safe repair: check if columns exist before adding them.
                // This handles cases where Android Auto-Restore brings back a database
                // that Room thinks is V23 but is missing V22 columns.
                
                if (!columnExists(db, "workout_exercises", "targetWeightKg")) {
                    db.execSQL("ALTER TABLE workout_exercises ADD COLUMN targetWeightKg REAL")
                }
                if (!columnExists(db, "set_logs", "setType")) {
                    db.execSQL("ALTER TABLE set_logs ADD COLUMN setType TEXT NOT NULL DEFAULT 'NORMAL'")
                }
            }

            private fun columnExists(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
                val cursor = db.query("PRAGMA table_info($table)")
                cursor.use {
                    val nameIdx = it.getColumnIndex("name")
                    while (it.moveToNext()) {
                        if (it.getString(nameIdx) == column) return true
                    }
                }
                return false
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitquest.db"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                        MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                        MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15,
                        MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20,
                        MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24
                    )
                    .build().also { instance = it }
            }
    }
}
