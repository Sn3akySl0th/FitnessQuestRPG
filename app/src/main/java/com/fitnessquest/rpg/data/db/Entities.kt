package com.fitnessquest.rpg.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.SetType

enum class ExerciseCategory(val label: String, val statLabel: String) {
    STRENGTH("Strength", "STR"),
    CARDIO("Cardio", "END"),
    BODYWEIGHT("Bodyweight", "AGI"),
    FLEXIBILITY("Flexibility", "WIL")
}

enum class ItemSlot(val label: String) {
    WEAPON("Weapon"),
    HEAD("Head"),
    CHEST("Chest"),
    HANDS("Hands"),
    LEGS("Legs"),
    FEET("Feet"),
    TRINKET("Trinket"),
    /** Not equippable: potions and supplies bought repeatedly and consumed. */
    CONSUMABLE("Consumable"),
    /** Socketable into gear instances. Stackable by quantity. */
    RUNE("Rune"),
    /** Crafting reagents / sell fodder. Stackable. */
    MATERIAL("Material"),
    /** Opens into multiple loot rolls. Stackable. */
    LOOT_CHEST("Chest")
}

/** Slots that can be worn on the body. */
val EquippableSlots = listOf(
    ItemSlot.WEAPON, ItemSlot.HEAD, ItemSlot.CHEST, ItemSlot.HANDS,
    ItemSlot.LEGS, ItemSlot.FEET, ItemSlot.TRINKET
)

/** Slots tracked by quantity on the catalog row. */
val StackableSlots = setOf(ItemSlot.CONSUMABLE, ItemSlot.RUNE, ItemSlot.MATERIAL, ItemSlot.LOOT_CHEST)

fun ItemSlot.isEquippable(): Boolean = this in EquippableSlots
fun ItemSlot.isStackable(): Boolean = this in StackableSlots

/** The five armor slots that count toward a class set bonus. */
val ArmorSlots = setOf(ItemSlot.HEAD, ItemSlot.CHEST, ItemSlot.HANDS, ItemSlot.LEGS, ItemSlot.FEET)

fun itemBonusText(item: ItemEntity): String = buildList {
    if (item.atk > 0) add("+${item.atk} ATK")
    if (item.def > 0) add("+${item.def} DEF")
    if (item.hp > 0) add("+${item.hp} HP")
}.joinToString(" ")

@Entity(tableName = "character")
data class CharacterEntity(
    @PrimaryKey val id: Long = 1L,
    val name: String = "Hero",
    /** Null until the player picks a class on first launch. */
    val characterClass: CharacterClass? = null,
    val level: Int = 1,
    val xp: Int = 0,
    val gold: Int = 50,
    val energy: Int = 30,
    val strength: Int = 1,
    val endurance: Int = 1,
    val agility: Int = 1,
    val willpower: Int = 1,
    val strProgress: Int = 0,
    val endProgress: Int = 0,
    val agiProgress: Int = 0,
    val wilProgress: Int = 0,
    val weaponId: Long? = null,
    val headId: Long? = null,
    val chestId: Long? = null,
    val handsId: Long? = null,
    val legsId: Long? = null,
    val feetId: Long? = null,
    val trinketId: Long? = null,
    val battlesWon: Int = 0,
    val sessionsCompleted: Int = 0,
    /** Consecutive days with at least one completed workout. */
    val streak: Int = 0,
    /** Epoch day (LocalDate.toEpochDay) of the most recent workout. 0 = never. */
    val lastWorkoutDay: Long = 0,
    /** Bonus XP from consumed elixirs, applied to the next completed session. */
    val pendingXpBoost: Int = 0,
    /** Unspent attribute points from initial creation or Rebirth. */
    val freeStatPoints: Int = 16,
    /** Biome enum name the hero is currently in. */
    val currentBiome: String = "MEADOWLANDS",
    /** Biome enum name being traveled to, if any. */
    val travelTarget: String? = null,
    /** Cardio kilometers logged toward the travel target. */
    val travelProgress: Double = 0.0,
    /** Accumulated un-battled steps from passive walking. */
    val idleSteps: Int = 0,
    /** Total monsters defeated in background idle battles (waiting to be claimed). */
    val idleKills: Int = 0,
    /** Total gold accumulated in background idle battles. */
    val idleGold: Int = 0,
    /** Total XP accumulated in background idle battles. */
    val idleXp: Int = 0,
    /** Serialized pending idle loot grants that should be applied when idle rewards are claimed. */
    val idlePendingLoot: String = "",
    val skinColor: Long = 0xFFE3B187,
    val hairColor: Long = 0xFF6B4A32,
    val underwearColor: Long = 0xFF4E4656,
    val eyeColor: Long = 0xFF2A2233,
    val hairStyle: String = "short",
    val gender: String = "male",
    val braColor: Long = 0xFF4E4656,
    /** Stored [CharacterRace] name; default HUMAN. */
    val race: String = "HUMAN",
    /** Persistent battle HP (null = full max HP). */
    val currentHp: Int? = null,
    /** Druid shapeshifting form ("HUMAN", "BEAR", "PANTHER"). */
    val druidForm: String = "HUMAN",
    /** Epoch day of the last wellness/meditation energy claim. */
    val lastWellnessDay: Long = 0,
    /** Epoch millis of the last time energy was recouped or modified. */
    val lastEnergyUpdate: Long = System.currentTimeMillis(),
    /** Current joined party ID from Firestore, if any. */
    val partyId: String? = null,
    /** Current joined guild ID from Firestore, if any. */
    val guildId: String? = null,

    // ---- Profile & Progress (migrated from SharedPreferences) ----
    val bodyWeightKg: Double? = null,
    val heightM: Double? = null,
    val dateOfBirthEpoch: Long? = null,
    val trainingEquipment: String = "", // Comma-separated enum names
    val trainingDaysPerWeek: Int = 3,
    val trainingSplit: String = "FULL_BODY",
    val trainingLevel: String = "BEGINNER",
    val fitnessGoal: String = "BUILD_MUSCLE",
    val muscleFocus: String = "BALANCED",
    val cardioPlacement: String = "NONE",
    val workoutDuration: String = "STANDARD",
    val effortMethod: String = "OFF",
    val plateBarKg: Double? = null,
    val claimedTrophies: String = "", // Comma-separated IDs
    val bountyDay: Long = -1,
    val bountyBattlesStart: Int = 0,
    val waterGlasses: Int = 0,
    val stretchDone: Boolean = false,
    val claimedBounties: String = "", // Comma-separated IDs
    val campaignWeek: Long = -1,
    val campaignBattlesStart: Int = 0,
    val claimedCampaigns: String = "", // Comma-separated IDs
    val encounterClaimedThisTravel: Boolean = false,
    val pendingEncounterStrBoost: Boolean = false,
    val firstWorkoutDone: Boolean = false
) {
    fun equippedIds(): Map<ItemSlot, Long?> = mapOf(
        ItemSlot.WEAPON to weaponId,
        ItemSlot.HEAD to headId,
        ItemSlot.CHEST to chestId,
        ItemSlot.HANDS to handsId,
        ItemSlot.LEGS to legsId,
        ItemSlot.FEET to feetId,
        ItemSlot.TRINKET to trinketId
    )

    /** Returns a copy of this character with a new job's stats but the same identity and global progress. */
    fun withJobProgress(p: ClassProgressEntity) = copy(
        characterClass = p.clazz,
        level = p.level,
        xp = p.xp,
        strength = p.strength,
        endurance = p.endurance,
        agility = p.agility,
        willpower = p.willpower,
        strProgress = p.strProgress,
        endProgress = p.endProgress,
        agiProgress = p.agiProgress,
        wilProgress = p.wilProgress,
        weaponId = p.weaponId,
        headId = p.headId,
        chestId = p.chestId,
        handsId = p.handsId,
        legsId = p.legsId,
        feetId = p.feetId,
        trinketId = p.trinketId,
        freeStatPoints = p.freeStatPoints,
        druidForm = "HUMAN"
    )
}

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val aiGenerated: Boolean = false
)

@Entity(tableName = "workout_exercises")
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseName: String,
    val category: ExerciseCategory,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetWeightKg: Double? = null,
    val sortOrder: Int = 0
)


@Entity(
    tableName = "sessions",
    indices = [Index(value = ["completionToken"], unique = true)]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startedAt: Long,
    val endedAt: Long,
    val xpEarned: Int,
    val goldEarned: Int,
    val energyEarned: Int,
    val setCount: Int,
    val completionToken: String? = null
)

@Entity(tableName = "set_logs")
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseName: String,
    val category: ExerciseCategory,
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val durationMin: Double = 0.0,
    val distanceKm: Double = 0.0,
    val xp: Int = 0,
    /** Reps in reserve reported for this set (RPE stores as 10 - RPE). Null = not tracked. */
    val rir: Int? = null,
    /** Optional average heart rate (BPM) captured from Wear during the set. */
    val avgHr: Int? = null,
    /** Optional peak heart rate (BPM) during the set window. */
    val maxHr: Int? = null,
    /** Cardio machine speed in km/h (0 = not logged). */
    val speedKmh: Double = 0.0,
    /** Treadmill / machine incline percent (0 = flat or not logged). */
    val inclinePercent: Double = 0.0,
    /** Optional machine program name (Hill, Intervals, Manual, …). */
    val cardioProgram: String = "",
    /** Advanced set type (Normal, Drop Set, Warm-up, ...). Defaults to NORMAL. */
    val setType: SetType = SetType.NORMAL
)

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val emoji: String,
    val slot: ItemSlot,
    val tier: Int,
    val price: Int,
    val atk: Int = 0,
    val def: Int = 0,
    val hp: Int = 0,
    val description: String = "",
    /**
     * Legacy ownership flag. For stackables, set when quantity > 0.
     * For equippable templates, ownership lives on [GearInstanceEntity] instead;
     * this field is only kept so catalog refresh migrations stay simple.
     */
    val owned: Boolean = false,
    /** Null = usable by any class; otherwise only this class can equip it. */
    val classAffinity: CharacterClass? = null,
    /** Visual style key used by the avatar renderer (see ItemStyle). */
    val style: String = "",
    /** How many the player holds. Meaningful for stackable slots. */
    val quantity: Int = 0
)

/**
 * A unique owned copy of a catalog gear piece. Multiple instances can share
 * the same [catalogId] (two Iron Maces with different runes).
 */
@Entity(tableName = "gear_instances")
data class GearInstanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val catalogId: Long,
    val atk: Int = 0,
    val def: Int = 0,
    val hp: Int = 0,
    /** Catalog rune id in socket 1, if any. */
    val rune1Id: Long? = null,
    /** Catalog rune id in socket 2, if any. */
    val rune2Id: Long? = null,
    /** Forge upgrade level applied to this owned copy. */
    val upgradeLevel: Int = 0,
    /** Future-ready rarity key for loot, visuals, and salvage value. */
    val rarity: String = "COMMON",
    /** Comma-separated trait ids until the trait registry gets its own table. */
    val traitIds: String = "",
    /** Biome enum name where this item originated, if known. */
    val originBiome: String? = null
) {
    fun runeIds(): List<Long> = listOfNotNull(rune1Id, rune2Id)
}

@Entity(tableName = "biome_progress")
data class BiomeProgressEntity(
    @PrimaryKey val biomeName: String,
    val layer: Int = 1,
    val bossUnlocked: Boolean = false,
    val bossDefeated: Boolean = false,
    val progressPoints: Int = 0,
    val firstClearRewardClaimed: Boolean = false
)

/**
 * Stores progress (level, attributes, gear) for a specific class.
 * This allows the FFXI-style "Job" system where switching classes preserves your progress in each.
 */
@Entity(tableName = "class_progress", primaryKeys = ["characterId", "clazz"])
data class ClassProgressEntity(
    val characterId: Long,
    val clazz: CharacterClass,
    val level: Int = 1,
    val xp: Int = 0,
    val strength: Int = 1,
    val endurance: Int = 1,
    val agility: Int = 1,
    val willpower: Int = 1,
    val strProgress: Int = 0,
    val endProgress: Int = 0,
    val agiProgress: Int = 0,
    val wilProgress: Int = 0,
    val weaponId: Long? = null,
    val headId: Long? = null,
    val chestId: Long? = null,
    val handsId: Long? = null,
    val legsId: Long? = null,
    val feetId: Long? = null,
    val trinketId: Long? = null,
    val freeStatPoints: Int = 0
) {
    fun equippedIds(): Map<ItemSlot, Long?> = mapOf(
        ItemSlot.WEAPON to weaponId,
        ItemSlot.HEAD to headId,
        ItemSlot.CHEST to chestId,
        ItemSlot.HANDS to handsId,
        ItemSlot.LEGS to legsId,
        ItemSlot.FEET to feetId,
        ItemSlot.TRINKET to trinketId
    )
}

@Entity(tableName = "body_metrics")
data class BodyMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val weightKg: Double,
    val heightM: Double? = null,
    val note: String = ""
)
