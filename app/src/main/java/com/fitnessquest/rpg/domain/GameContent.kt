package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot

/** A suggested exercise shown in the picker. Users can also type custom names. */
data class CatalogExercise(val name: String, val category: ExerciseCategory)

object ExerciseCatalog {
    val all: List<CatalogExercise> = listOf(
        // Strength (weights)
        CatalogExercise("Bench Press", ExerciseCategory.STRENGTH),
        CatalogExercise("Squat", ExerciseCategory.STRENGTH),
        CatalogExercise("Deadlift", ExerciseCategory.STRENGTH),
        CatalogExercise("Overhead Press", ExerciseCategory.STRENGTH),
        CatalogExercise("Barbell Row", ExerciseCategory.STRENGTH),
        CatalogExercise("Dumbbell Curl", ExerciseCategory.STRENGTH),
        CatalogExercise("Lat Pulldown", ExerciseCategory.STRENGTH),
        CatalogExercise("Leg Press", ExerciseCategory.STRENGTH),
        CatalogExercise("Dumbbell Shoulder Press", ExerciseCategory.STRENGTH),
        CatalogExercise("Tricep Pushdown", ExerciseCategory.STRENGTH),
        // Cardio
        CatalogExercise("Running", ExerciseCategory.CARDIO),
        CatalogExercise("Cycling", ExerciseCategory.CARDIO),
        CatalogExercise("Rowing Machine", ExerciseCategory.CARDIO),
        CatalogExercise("Jump Rope", ExerciseCategory.CARDIO),
        CatalogExercise("Swimming", ExerciseCategory.CARDIO),
        CatalogExercise("Stair Climber", ExerciseCategory.CARDIO),
        CatalogExercise("Elliptical", ExerciseCategory.CARDIO),
        CatalogExercise("Walking (brisk)", ExerciseCategory.CARDIO),
        // Bodyweight
        CatalogExercise("Push-ups", ExerciseCategory.BODYWEIGHT),
        CatalogExercise("Pull-ups", ExerciseCategory.BODYWEIGHT),
        CatalogExercise("Dips", ExerciseCategory.BODYWEIGHT),
        CatalogExercise("Bodyweight Squats", ExerciseCategory.BODYWEIGHT),
        CatalogExercise("Lunges", ExerciseCategory.BODYWEIGHT),
        CatalogExercise("Plank (reps = seconds)", ExerciseCategory.BODYWEIGHT),
        CatalogExercise("Burpees", ExerciseCategory.BODYWEIGHT),
        CatalogExercise("Sit-ups", ExerciseCategory.BODYWEIGHT),
        CatalogExercise("Mountain Climbers", ExerciseCategory.BODYWEIGHT),
        // Flexibility
        CatalogExercise("Yoga Flow", ExerciseCategory.FLEXIBILITY),
        CatalogExercise("Static Stretching", ExerciseCategory.FLEXIBILITY),
        CatalogExercise("Foam Rolling", ExerciseCategory.FLEXIBILITY),
        CatalogExercise("Mobility Drills", ExerciseCategory.FLEXIBILITY),
        CatalogExercise("Pilates", ExerciseCategory.FLEXIBILITY)
    )

    fun byCategory(category: ExerciseCategory): List<CatalogExercise> =
        all.filter { it.category == category }
}

/** Visual style keys understood by the avatar renderer. */
object ItemStyle {
    const val SWORD = "SWORD"
    const val GREATSWORD = "GREATSWORD"
    const val MACE = "MACE"
    const val WAND = "WAND"
    const val STAFF = "STAFF"
    const val DAGGER = "DAGGER"
    const val BOW = "BOW"
    const val PLATE = "PLATE"
    const val ROBE = "ROBE"
    const val LIGHT = "LIGHT"
}

/** Well-known consumable item ids (900 range, clear of gear ids). */
object Consumables {
    const val STREAK_FREEZE = 901L
    const val XP_ELIXIR_MINOR = 902L
    const val XP_ELIXIR_MAJOR = 903L
    const val POTION_OF_REBIRTH = 904L
    const val HEALTH_POTION = 905L
    const val LIFE_POTION = 906L
    const val CLEANSING_SALVE = 907L
    const val BATTLE_ELIXIR = 908L
    const val IRONHIDE_SALVE = 909L

    const val XP_MINOR_BONUS = 50
    const val XP_MAJOR_BONUS = 200
    const val HEALTH_POTION_HEAL = 50
}

object ItemCatalog {
    private val W = CharacterClass.WARRIOR
    private val M = CharacterClass.MAGE
    private val T = CharacterClass.THIEF
    private val R = CharacterClass.RANGER
    private val P = CharacterClass.PALADIN
    private val N = CharacterClass.NECROMANCER
    private val WM = CharacterClass.WHITE_MAGE
    private val MK = CharacterClass.MONK
    private val DR = CharacterClass.DRUID
    private val BS = CharacterClass.BERSERKER
    private val BD = CharacterClass.BARD
    private val SM = CharacterClass.SUMMONER
    private val DG = CharacterClass.DRAGOON

    val all: List<ItemEntity> = listOf(
        // ---- Basic & Starter weapons (Universal, no class affinity) ----
        ItemEntity(1, "Rusty Sword", "\uD83D\uDDE1\uFE0F", ItemSlot.WEAPON, 1, 40, atk = 3, description = "It's seen better days, but so have you.", style = ItemStyle.SWORD),
        ItemEntity(2, "Iron Mace", "\uD83D\uDD28", ItemSlot.WEAPON, 2, 120, atk = 7, description = "Heavy. Like leg day.", style = ItemStyle.MACE),
        ItemEntity(3, "Knight's Blade", "\u2694\uFE0F", ItemSlot.WEAPON, 3, 300, atk = 13, description = "Forged for those who never skip a session.", classAffinity = W, style = ItemStyle.SWORD),
        ItemEntity(4, "Dragonfang Greatsword", "\uD83D\uDC09", ItemSlot.WEAPON, 4, 700, atk = 22, description = "Carved from the fang of a dragon who skipped cardio.", classAffinity = W, style = ItemStyle.GREATSWORD),

        // ---- Mage & Arcane weapons ----
        ItemEntity(13, "Apprentice Wand", "\uD83E\uDE84", ItemSlot.WEAPON, 1, 40, atk = 3, description = "A simple focus wand suitable for any aspiring spellcaster.", style = ItemStyle.WAND),
        ItemEntity(14, "Oakheart Staff", "\uD83C\uDF3F", ItemSlot.WEAPON, 2, 120, atk = 7, description = "Still growing leaves. It refuses to stop.", style = ItemStyle.STAFF),
        ItemEntity(15, "Staff of Storms", "\u26C8\uFE0F", ItemSlot.WEAPON, 3, 300, atk = 13, description = "Crackles when you hit a new personal record.", classAffinity = M, style = ItemStyle.STAFF),
        ItemEntity(16, "Archmage's Scepter", "\uD83D\uDC51", ItemSlot.WEAPON, 4, 700, atk = 22, description = "Contains the condensed willpower of a thousand morning workouts.", classAffinity = M, style = ItemStyle.STAFF),

        // ---- Thief & Agility weapons ----
        ItemEntity(17, "Bent Shiv", "\uD83D\uDD2A", ItemSlot.WEAPON, 1, 40, atk = 3, description = "Technically a weapon. Technically.", style = ItemStyle.DAGGER),
        ItemEntity(18, "Twin Fangs", "\uD83D\uDC0D", ItemSlot.WEAPON, 2, 120, atk = 7, description = "Two daggers, because one hand should never rest.", style = ItemStyle.DAGGER),
        ItemEntity(19, "Nightpiercer", "\uD83C\uDF11", ItemSlot.WEAPON, 3, 300, atk = 13, description = "Strikes between heartbeats. Ideally between sets.", classAffinity = T, style = ItemStyle.DAGGER),
        ItemEntity(20, "Whisper of the Void", "\uD83D\uDD73\uFE0F", ItemSlot.WEAPON, 4, 700, atk = 22, description = "So sharp it cuts excuses before they're spoken.", classAffinity = T, style = ItemStyle.DAGGER),

        // ---- Ranger & Marksmanship weapons ----
        ItemEntity(21, "Training Shortbow", "\uD83C\uDFF9", ItemSlot.WEAPON, 1, 40, atk = 3, description = "The string doubles as a resistance band.", style = ItemStyle.BOW),
        ItemEntity(22, "Hunter's Recurve", "\uD83C\uDFAF", ItemSlot.WEAPON, 2, 120, atk = 7, description = "Draw weight: your bodyweight. Keep training.", style = ItemStyle.BOW),
        ItemEntity(23, "Hawkeye Longbow", "\uD83E\uDD85", ItemSlot.WEAPON, 3, 300, atk = 13, description = "Its arrows have never met a rest day.", classAffinity = R, style = ItemStyle.BOW),
        ItemEntity(24, "Worldroot Warbow", "\uD83C\uDF32", ItemSlot.WEAPON, 4, 700, atk = 22, description = "Cut from the oldest tree in the oldest forest. It approves of your squat depth.", classAffinity = R, style = ItemStyle.BOW),

        // ---- Necromancer weapons (skull-topped bone staves) ----
        ItemEntity(46, "Bone Staff", "\uD83E\uDDB4", ItemSlot.WEAPON, 1, 40, atk = 3, description = "Still has a little marrow. Motivational.", classAffinity = N, style = ItemStyle.STAFF),
        ItemEntity(47, "Graveoak Staff", "\uD83C\uDF32", ItemSlot.WEAPON, 2, 120, atk = 7, description = "Grown in soil fertilized by missed rest days.", classAffinity = N, style = ItemStyle.STAFF),
        ItemEntity(48, "Soulflayer", "\uD83D\uDD2E", ItemSlot.WEAPON, 3, 300, atk = 13, description = "Drinks fatigue and returns it as willpower. The skull watches.", classAffinity = N, style = ItemStyle.STAFF),
        ItemEntity(49, "Eclipse Phylactery", "\uD83C\uDF11", ItemSlot.WEAPON, 4, 700, atk = 22, description = "A pocket afterlife capped with a screaming skull. Keep it charged with hard sets.", classAffinity = N, style = ItemStyle.STAFF),

        // ---- Paladin weapons ----
        ItemEntity(1001, "Novice Oathblade", "\u2694\uFE0F", ItemSlot.WEAPON, 1, 40, atk = 3, def = 1, description = "A humble blade that gets brighter when promises turn into reps.", classAffinity = P, style = ItemStyle.SWORD),
        ItemEntity(1002, "Tempered Oathblade", "\uD83D\uDEE1\uFE0F", ItemSlot.WEAPON, 2, 120, atk = 7, def = 2, description = "Balanced for protection, progress, and dramatic post-workout posing.", classAffinity = P, style = ItemStyle.SWORD),
        ItemEntity(1003, "Radiant Crusader Blade", "\u2728", ItemSlot.WEAPON, 3, 300, atk = 13, def = 3, description = "Holy steel that hums whenever your form stays honest.", classAffinity = P, style = ItemStyle.SWORD),
        ItemEntity(1004, "Dawnkeeper", "\u2600\uFE0F", ItemSlot.WEAPON, 4, 700, atk = 22, def = 5, hp = 10, description = "A sunrise made sharp enough to cut through fatigue.", classAffinity = P, style = ItemStyle.GREATSWORD),

        // ---- White Mage weapons ----
        ItemEntity(1011, "Candlelit Wand", "\uD83D\uDD6F\uFE0F", ItemSlot.WEAPON, 1, 40, atk = 2, def = 1, hp = 5, description = "A small focus for steady breathing and stubborn recovery.", classAffinity = WM, style = ItemStyle.WAND),
        ItemEntity(1012, "Sanctuary Rod", "\uD83D\uDC8E", ItemSlot.WEAPON, 2, 120, atk = 6, def = 2, hp = 10, description = "Turns careful consistency into a quiet kind of power.", classAffinity = WM, style = ItemStyle.STAFF),
        ItemEntity(1013, "Mercybrand Staff", "\u2728", ItemSlot.WEAPON, 3, 300, atk = 11, def = 4, hp = 18, description = "A healer's staff with enough bite to end the fight kindly.", classAffinity = WM, style = ItemStyle.STAFF),
        ItemEntity(1014, "Seraphic Scepter", "\uD83D\uDD4A\uFE0F", ItemSlot.WEAPON, 4, 700, atk = 18, def = 7, hp = 32, description = "It carries the calm of a full rest day and the force of showing up anyway.", classAffinity = WM, style = ItemStyle.STAFF),

        // ---- Monk weapons ----
        ItemEntity(1021, "Novice Handwraps", "\uD83E\uDD4A", ItemSlot.WEAPON, 1, 40, atk = 3, hp = 3, description = "Cloth, focus, and a complete lack of excuses.", classAffinity = MK, style = ItemStyle.MACE),
        ItemEntity(1022, "Iron Palm Bands", "\uD83D\uDCAA", ItemSlot.WEAPON, 2, 120, atk = 7, def = 1, hp = 6, description = "Weighted wraps for turning tempo work into thunder.", classAffinity = MK, style = ItemStyle.MACE),
        ItemEntity(1023, "Stormstep Tonfa", "\u26A1", ItemSlot.WEAPON, 3, 300, atk = 13, def = 2, hp = 10, description = "Fast enough to make cooldowns feel personal.", classAffinity = MK, style = ItemStyle.MACE),
        ItemEntity(1024, "Dragon Palm Relics", "\uD83D\uDC09", ItemSlot.WEAPON, 4, 700, atk = 22, def = 3, hp = 18, description = "Ancient striking relics tuned to breath, balance, and brutal intervals.", classAffinity = MK, style = ItemStyle.MACE),

        // ---- Druid weapons ----
        ItemEntity(1031, "Sproutwood Cane", "\uD83C\uDF31", ItemSlot.WEAPON, 1, 40, atk = 3, hp = 4, description = "Still growing. It appreciates hydration.", classAffinity = DR, style = ItemStyle.STAFF),
        ItemEntity(1032, "Briarroot Staff", "\uD83C\uDF3F", ItemSlot.WEAPON, 2, 120, atk = 7, def = 1, hp = 8, description = "Roots deeper whenever your weekly streak holds.", classAffinity = DR, style = ItemStyle.STAFF),
        ItemEntity(1033, "Moonwell Crook", "\uD83C\uDF19", ItemSlot.WEAPON, 3, 300, atk = 13, def = 2, hp = 16, description = "Draws strength from cycles: train, recover, return sharper.", classAffinity = DR, style = ItemStyle.STAFF),
        ItemEntity(1034, "Worldroot Elderstaff", "\uD83C\uDF32", ItemSlot.WEAPON, 4, 700, atk = 22, def = 4, hp = 28, description = "The forest keeps count of your reps. It is impressed.", classAffinity = DR, style = ItemStyle.STAFF),

        // ---- Berserker weapons ----
        ItemEntity(1041, "Chipped War Axe", "\uD83E\uDE93", ItemSlot.WEAPON, 1, 40, atk = 4, hp = 2, description = "Not subtle. Neither was that last set.", classAffinity = BS, style = ItemStyle.GREATSWORD),
        ItemEntity(1042, "Ragecleaver", "\uD83D\uDD25", ItemSlot.WEAPON, 2, 120, atk = 8, hp = 5, description = "Converts bad moods into excellent training volume.", classAffinity = BS, style = ItemStyle.GREATSWORD),
        ItemEntity(1043, "Blood-Iron Maul", "\uD83D\uDD28", ItemSlot.WEAPON, 3, 300, atk = 15, hp = 8, description = "Every swing sounds like plates returning to the rack.", classAffinity = BS, style = ItemStyle.MACE),
        ItemEntity(1044, "Riftbreaker Greataxe", "\u26A1", ItemSlot.WEAPON, 4, 700, atk = 25, hp = 14, description = "A reckless masterpiece for heroes who weaponize intensity.", classAffinity = BS, style = ItemStyle.GREATSWORD),

        // ---- Bard weapons ----
        ItemEntity(1051, "Practice Lute", "\uD83C\uDFB5", ItemSlot.WEAPON, 1, 40, atk = 2, def = 1, hp = 4, description = "Three strings, two chords, one surprisingly motivational solo.", classAffinity = BD, style = ItemStyle.WAND),
        ItemEntity(1052, "Resonant Baton", "\uD83C\uDFBC", ItemSlot.WEAPON, 2, 120, atk = 6, def = 2, hp = 8, description = "Keeps rhythm through reps, rounds, and dramatic entrances.", classAffinity = BD, style = ItemStyle.WAND),
        ItemEntity(1053, "Virtuoso's Rapier", "\uD83D\uDDE1\uFE0F", ItemSlot.WEAPON, 3, 300, atk = 12, def = 3, hp = 12, description = "Sharp phrasing. Sharper point.", classAffinity = BD, style = ItemStyle.SWORD),
        ItemEntity(1054, "Finale Harpblade", "\uD83C\uDFB6", ItemSlot.WEAPON, 4, 700, atk = 20, def = 5, hp = 22, description = "Ends battles on a high note and workouts on a better one.", classAffinity = BD, style = ItemStyle.SWORD),

        // ---- Summoner weapons ----
        ItemEntity(1071, "Novice Horn", "\uD83E\uDDA4", ItemSlot.WEAPON, 1, 40, atk = 3, hp = 2, description = "A small horn used to call minor spirits. It hums when you focus.", classAffinity = SM, style = ItemStyle.WAND),
        ItemEntity(1072, "Primal Flute", "\uD83C\uDFBC", ItemSlot.WEAPON, 2, 120, atk = 7, def = 1, hp = 6, description = "The notes linger in the air, weaving the fabric of the elements.", classAffinity = SM, style = ItemStyle.WAND),
        ItemEntity(1073, "Caller's Staff", "\uD83E\uDE84", ItemSlot.WEAPON, 3, 300, atk = 13, def = 2, hp = 14, description = "A heavy staff topped with a focusing gem that glows with inner light.", classAffinity = SM, style = ItemStyle.STAFF),
        ItemEntity(1074, "Apocalypse Horn", "\uD83C\uDF0C", ItemSlot.WEAPON, 4, 700, atk = 22, def = 4, hp = 26, description = "Its sound can be heard across biomes, summoning the ancient elders.", classAffinity = SM, style = ItemStyle.STAFF),

        // ---- Dragoon weapons ----
        ItemEntity(1081, "Worn Harpoon", "\uD83D\uDDE1\uFE0F", ItemSlot.WEAPON, 1, 40, atk = 3, def = 1, description = "A reliable tool for hunters of the deep sky.", classAffinity = DG, style = ItemStyle.SWORD),
        ItemEntity(1082, "Iron Lance", "\u2694\uFE0F", ItemSlot.WEAPON, 2, 120, atk = 7, def = 2, description = "Balanced for gravity-defying leaps and precise landings.", classAffinity = DG, style = ItemStyle.SWORD),
        ItemEntity(1083, "Wyrmslayer Spear", "\u26A1", ItemSlot.WEAPON, 3, 300, atk = 13, def = 3, hp = 10, description = "Tipped with dragon-bone. It thirsts for the clouds.", classAffinity = DG, style = ItemStyle.GREATSWORD),
        ItemEntity(1084, "Gae Bolg", "\uD83D\uDD25", ItemSlot.WEAPON, 4, 700, atk = 24, def = 5, hp = 20, description = "The legendary spear of the sky kings. It feels lighter the higher you jump.", classAffinity = DG, style = ItemStyle.GREATSWORD),

        // ---- Universal weapons (Equippable by all classes, no special restrictions) ----
        ItemEntity(1091, "Training Spear", "\uD83D\uDD31", ItemSlot.WEAPON, 1, 35, atk = 2, def = 1, description = "Simple reach, steady balance, no class requirement.", style = ItemStyle.SWORD),
        ItemEntity(1092, "Balanced Shortsword", "\uD83D\uDDE1\uFE0F", ItemSlot.WEAPON, 2, 110, atk = 6, def = 1, description = "A dependable blade for any hero still finding their path.", style = ItemStyle.SWORD),
        ItemEntity(1093, "Mercenary Halberd", "\u2694\uFE0F", ItemSlot.WEAPON, 3, 280, atk = 11, def = 2, description = "Practical, durable, and happy to work for whoever trains hardest.", style = ItemStyle.GREATSWORD),
        ItemEntity(1094, "Starforged Relic Blade", "\u2728", ItemSlot.WEAPON, 4, 650, atk = 18, def = 4, hp = 12, description = "A universal legend with room for any class story.", style = ItemStyle.GREATSWORD),
        ItemEntity(1171, "Traveler's Staff", "🪵", ItemSlot.WEAPON, 1, 40, atk = 3, description = "A sturdy hardwood walking staff suitable for any traveler, hiker, or caster.", style = ItemStyle.STAFF),
        ItemEntity(1172, "Novice Bludgeon", "🏏", ItemSlot.WEAPON, 1, 40, atk = 3, description = "A simple weighted club with no class restrictions.", style = ItemStyle.MACE),
        ItemEntity(1173, "Focusing Wand", "🪄", ItemSlot.WEAPON, 2, 120, atk = 7, description = "A polished glass-tipped wand for versatile spellcraft.", style = ItemStyle.WAND),
        ItemEntity(1174, "Runed Quarterstaff", "🔮", ItemSlot.WEAPON, 3, 300, atk = 13, description = "Carved with sturdy balance rings for casting or striking.", style = ItemStyle.STAFF),
        ItemEntity(1175, "Composite Longbow", "🏹", ItemSlot.WEAPON, 3, 300, atk = 13, description = "A flexible, reliable bow crafted for any archer.", style = ItemStyle.BOW),
        ItemEntity(1176, "Steel Morningstar", "🔨", ItemSlot.WEAPON, 3, 300, atk = 13, description = "A spiked steel mace with universal balance.", style = ItemStyle.MACE),
        ItemEntity(1177, "Gilded Wand", "✨", ItemSlot.WEAPON, 3, 300, atk = 13, description = "Inlaid with conductive gold filigree for clean mana channeling.", style = ItemStyle.WAND),
        ItemEntity(1178, "Duelist's Rapier", "🗡️", ItemSlot.WEAPON, 3, 300, atk = 13, description = "A sharp, balanced thrusting sword for any disciplined hero.", style = ItemStyle.SWORD),
        ItemEntity(1179, "Woodcutter's Axe", "🪓", ItemSlot.WEAPON, 1, 40, atk = 4, description = "A simple two-handed axe that delivers straightforward power.", style = ItemStyle.GREATSWORD),

        // ---- Universal starter & standard armor (Cloth / Leather / Iron) ----
        ItemEntity(5, "Padded Vest", "\uD83E\uDDBA", ItemSlot.CHEST, 1, 40, def = 3, description = "Better than nothing. Barely.", style = ItemStyle.LIGHT),
        ItemEntity(1181, "Simple Robes", "👘", ItemSlot.CHEST, 1, 40, def = 2, hp = 6, description = "Lightweight linen robes for unrestricted movement and casting.", style = ItemStyle.ROBE),
        ItemEntity(1187, "Ringmail Vest", "🛡️", ItemSlot.CHEST, 1, 45, def = 4, hp = 4, description = "Interlocking iron rings over padded cloth for solid basic defense.", style = ItemStyle.PLATE),

        // ---- Standard Iron & Plate armor (Universal) ----
        ItemEntity(6, "Chainmail Shirt", "\uD83D\uDD17", ItemSlot.CHEST, 2, 120, def = 7, hp = 10, description = "Surprisingly breathable iron mail for any front-line adventurer.", style = ItemStyle.PLATE),
        ItemEntity(7, "Steel Plate", "\uD83D\uDEE1\uFE0F", ItemSlot.CHEST, 3, 300, def = 13, hp = 20, description = "The extra weight counts as resistance training.", style = ItemStyle.PLATE),
        ItemEntity(1192, "Reinforced Cuirass", "🛡️", ItemSlot.CHEST, 3, 310, def = 13, hp = 22, description = "Solid curved iron plates protecting the vital chest and back.", style = ItemStyle.PLATE),
        ItemEntity(8, "Aegis of the Titan", "\uD83C\uDFDB\uFE0F", ItemSlot.CHEST, 4, 700, def = 20, hp = 40, description = "Worn by legends who deadlifted mountains.", classAffinity = W, style = ItemStyle.PLATE),

        // ---- Standard Cloth & Robe armor (Universal) ----
        ItemEntity(25, "Apprentice Robes", "\uD83E\uDDE5", ItemSlot.CHEST, 2, 120, def = 5, hp = 15, description = "Machine washable. Mana resistant. Open to any hero.", style = ItemStyle.ROBE),
        ItemEntity(26, "Runeweave Robe", "\u2728", ItemSlot.CHEST, 3, 300, def = 10, hp = 30, description = "The runes glow brighter after cardio.", style = ItemStyle.ROBE),
        ItemEntity(1184, "Weaver's Garb", "🥋", ItemSlot.CHEST, 3, 300, def = 9, hp = 30, description = "Dense, breathable woven tunic for long journeys and high mobility.", style = ItemStyle.ROBE),
        ItemEntity(27, "Vestments of the Cosmos", "\uD83C\uDF0C", ItemSlot.CHEST, 4, 700, def = 16, hp = 52, description = "Woven from night sky and discipline.", classAffinity = M, style = ItemStyle.ROBE),

        // ---- Standard Leather & Light armor (Universal) ----
        ItemEntity(28, "Supple Leathers", "\uD83E\uDD8E", ItemSlot.CHEST, 2, 120, def = 6, hp = 8, atk = 2, description = "Squeak-proof. Guaranteed.", style = ItemStyle.LIGHT),
        ItemEntity(29, "Shadowmesh", "\uD83C\uDF2B\uFE0F", ItemSlot.CHEST, 3, 300, def = 11, hp = 16, atk = 4, description = "Woven from shadows and skipped rest timers.", style = ItemStyle.LIGHT),
        ItemEntity(1185, "Studded Leather Armor", "🦺", ItemSlot.CHEST, 3, 300, def = 11, hp = 20, atk = 2, description = "Hardened leather reinforced with iron rivets for all-around protection.", style = ItemStyle.LIGHT),
        ItemEntity(30, "Nightveil Shroud", "\uD83C\uDF18", ItemSlot.CHEST, 4, 700, def = 17, hp = 28, atk = 7, description = "Even mirrors lose track of you.", classAffinity = T, style = ItemStyle.LIGHT),

        // ---- Ranger armor ----
        ItemEntity(31, "Scout's Jerkin", "\uD83C\uDF42", ItemSlot.CHEST, 2, 120, def = 6, hp = 12, description = "Smells like pine and progress.", classAffinity = R, style = ItemStyle.LIGHT),
        ItemEntity(32, "Wildstalker Cloak", "\uD83C\uDF43", ItemSlot.CHEST, 3, 300, def = 12, hp = 22, description = "Blends into any forest. And most gyms.", classAffinity = R, style = ItemStyle.LIGHT),
        ItemEntity(33, "Mantle of the Wilds", "\uD83E\uDD8C", ItemSlot.CHEST, 4, 700, def = 18, hp = 38, description = "The wilderness itself vouches for your consistency.", classAffinity = R, style = ItemStyle.LIGHT),

        // ---- Paladin armor ----
        ItemEntity(42, "Acolyte's Cuirass", "\uD83D\uDEE1\uFE0F", ItemSlot.CHEST, 2, 120, def = 7, hp = 12, description = "Polished enough to shame your form in the mirror.", classAffinity = P, style = ItemStyle.PLATE),
        ItemEntity(43, "Radiant Plate", "\u2728", ItemSlot.CHEST, 3, 300, def = 13, hp = 24, description = "Glows after clean reps. Dims after ego lifting.", classAffinity = P, style = ItemStyle.PLATE),
        ItemEntity(44, "Aegis of the Dawn", "\u2600\uFE0F", ItemSlot.CHEST, 4, 700, def = 19, hp = 44, description = "Holy steel that treats missed workouts as heresy.", classAffinity = P, style = ItemStyle.PLATE),

        // ---- Necromancer armor ----
        ItemEntity(66, "Boneweave Vestments", "\uD83E\uDDB4", ItemSlot.CHEST, 2, 120, def = 5, hp = 16, description = "Surprisingly flexible. The previous owner agrees.", classAffinity = N, style = ItemStyle.ROBE),
        ItemEntity(67, "Cryptweave Mantle", "\uD83C\uDF11", ItemSlot.CHEST, 3, 300, def = 10, hp = 32, description = "Whispers encouragement. Or threats. Hard to tell.", classAffinity = N, style = ItemStyle.ROBE),
        ItemEntity(68, "Shroud of the Last Breath", "\uD83D\uDC80", ItemSlot.CHEST, 4, 700, def = 15, hp = 54, description = "Woven from expired memberships and unfinished programs.", classAffinity = N, style = ItemStyle.ROBE),

        // ---- Universal trinkets & Back pieces ----
        ItemEntity(9, "Lucky Sweatband", "\uD83C\uDFC5", ItemSlot.TRINKET, 1, 60, atk = 1, def = 1, hp = 5, description = "Absorbs sweat and bad vibes."),
        ItemEntity(1195, "Novice Cape", "🧣", ItemSlot.TRINKET, 1, 50, def = 2, hp = 5, description = "A crimson adventurer cape that billows in the wind.", style = "cape"),
        ItemEntity(1196, "Hunter's Quiver", "🏹", ItemSlot.TRINKET, 1, 50, atk = 2, hp = 3, description = "A leather arrow quiver worn strapped across the back.", style = "quiver"),
        ItemEntity(10, "Amulet of Endurance", "\uD83D\uDCFF", ItemSlot.TRINKET, 2, 180, hp = 25, description = "One more rep. Always one more rep."),
        ItemEntity(1197, "Iron Back Shield", "🛡️", ItemSlot.TRINKET, 2, 150, def = 8, hp = 15, description = "A sturdy strapped heater shield carried on the back between battles.", style = "shield"),
        ItemEntity(11, "Ring of Fury", "\uD83D\uDC8D", ItemSlot.TRINKET, 3, 400, atk = 8, description = "Channels pre-workout energy into raw power."),
        ItemEntity(12, "Heart of the Colossus", "\u2764\uFE0F\u200D\uD83D\uDD25", ItemSlot.TRINKET, 4, 900, atk = 6, def = 6, hp = 30, description = "Beats once per rep. Never stops."),
        ItemEntity(1198, "Celestial Wings", "🪽", ItemSlot.TRINKET, 4, 850, atk = 8, def = 8, hp = 30, description = "Luminous wings that unfurl behind disciplined heroes.", style = "wings"),

        // ---- Class trinkets ----
        ItemEntity(34, "Grimoire of Gains", "\uD83D\uDCD6", ItemSlot.TRINKET, 3, 400, atk = 5, hp = 15, description = "Every page is a workout log from a legendary mage.", classAffinity = M),
        ItemEntity(35, "Loaded Dice", "\uD83C\uDFB2", ItemSlot.TRINKET, 3, 400, atk = 6, def = 3, description = "Fortune favors the flexible.", classAffinity = T),
        ItemEntity(36, "Falcon Feather", "\uD83E\uDEB6", ItemSlot.TRINKET, 3, 400, atk = 4, def = 5, description = "Plucked mid-flight. The falcon respected the hustle.", classAffinity = R),
        ItemEntity(37, "Champion's Girdle", "\uD83E\uDD4B", ItemSlot.TRINKET, 3, 400, def = 4, hp = 20, description = "A weight belt worn by a hundred arena champions.", classAffinity = W),
        ItemEntity(45, "Relic of the Oath", "\uD83D\uDD4A\uFE0F", ItemSlot.TRINKET, 3, 400, def = 5, hp = 18, description = "A sealed vow: finish the workout or face judgment.", classAffinity = P),
        ItemEntity(69, "Phylactery Charm", "\uD83D\uDC80", ItemSlot.TRINKET, 3, 400, atk = 5, hp = 16, description = "Stores a spare soul — and leftover progressive overload.", classAffinity = N),
        ItemEntity(1061, "Sanctuary Censer", "\uD83D\uDD6F\uFE0F", ItemSlot.TRINKET, 3, 400, def = 5, hp = 22, description = "A warm charm for healers who know recovery is also a skill.", classAffinity = WM),
        ItemEntity(1062, "Temple Beads", "\uD83D\uDCFF", ItemSlot.TRINKET, 3, 400, atk = 4, def = 4, hp = 12, description = "Counts breaths, reps, and the distance between impulse and action.", classAffinity = MK),
        ItemEntity(1063, "Moonseed Talisman", "\uD83C\uDF19", ItemSlot.TRINKET, 3, 400, def = 4, hp = 24, description = "A living seed that roots deeper after every completed session.", classAffinity = DR),
        ItemEntity(1064, "Blood-Iron Torque", "\uD83D\uDD25", ItemSlot.TRINKET, 3, 400, atk = 7, hp = 10, description = "A brutal necklace for controlled chaos and heavy lifts.", classAffinity = BS),
        ItemEntity(1065, "Virtuoso's Pick", "\uD83C\uDFB6", ItemSlot.TRINKET, 3, 400, atk = 5, def = 3, hp = 12, description = "Keeps the party moving when motivation misses the beat.", classAffinity = BD),
        ItemEntity(1071, "Novice Horn", "\uD83E\uDDA4", ItemSlot.WEAPON, 1, 40, atk = 3, hp = 2, description = "A small horn used to call minor spirits. It hums when you focus.", classAffinity = SM, style = ItemStyle.WAND),
        ItemEntity(1072, "Primal Flute", "\uD83C\uDFBC", ItemSlot.WEAPON, 2, 120, atk = 7, def = 1, hp = 6, description = "The notes linger in the air, weaving the fabric of the elements.", classAffinity = SM, style = ItemStyle.WAND),
        ItemEntity(1073, "Caller's Staff", "\uD83E\uDE84", ItemSlot.WEAPON, 3, 300, atk = 13, def = 2, hp = 14, description = "A heavy staff topped with a focusing gem that glows with inner light.", classAffinity = SM, style = ItemStyle.STAFF),
        ItemEntity(1074, "Apocalypse Horn", "\uD83C\uDF0C", ItemSlot.WEAPON, 4, 700, atk = 22, def = 4, hp = 26, description = "Its sound can be heard across biomes, summoning the ancient elders.", classAffinity = SM, style = ItemStyle.STAFF),
        ItemEntity(1081, "Worn Harpoon", "\uD83D\uDDE1\uFE0F", ItemSlot.WEAPON, 1, 40, atk = 3, def = 1, description = "A reliable tool for hunters of the deep sky.", classAffinity = DG, style = ItemStyle.SWORD),
        ItemEntity(1082, "Iron Lance", "\u2694\uFE0F", ItemSlot.WEAPON, 2, 120, atk = 7, def = 2, description = "Balanced for gravity-defying leaps and precise landings.", classAffinity = DG, style = ItemStyle.SWORD),
        ItemEntity(1083, "Wyrmslayer Spear", "\u26A1", ItemSlot.WEAPON, 3, 300, atk = 13, def = 3, hp = 10, description = "Tipped with dragon-bone. It thirsts for the clouds.", classAffinity = DG, style = ItemStyle.GREATSWORD),
        ItemEntity(1084, "Gae Bolg", "\uD83D\uDD25", ItemSlot.WEAPON, 4, 700, atk = 24, def = 5, hp = 20, description = "The legendary spear of the sky kings. It feels lighter the higher you jump.", classAffinity = DG, style = ItemStyle.GREATSWORD),

        // ---- Universal starter armor pieces (Cloth / Leather / Iron) ----
        ItemEntity(50, "Leather Cap", "\uD83E\uDDE2", ItemSlot.HEAD, 1, 30, def = 2, hp = 3, description = "Keeps the sun out of your eyes during outdoor sets.", style = ItemStyle.LIGHT),
        ItemEntity(1182, "Cloth Cowl", "🧣", ItemSlot.HEAD, 1, 30, def = 1, hp = 4, description = "A simple fabric hood keeping the chill away.", style = ItemStyle.ROBE),
        ItemEntity(1188, "Iron Kettle Helm", "🪖", ItemSlot.HEAD, 1, 35, def = 3, hp = 4, description = "A domed iron helmet with a protective brim.", style = ItemStyle.PLATE),
        ItemEntity(51, "Cloth Gloves", "\uD83E\uDDE4", ItemSlot.HANDS, 1, 25, def = 1, hp = 2, description = "Callus prevention, medieval edition.", style = ItemStyle.LIGHT),
        ItemEntity(1186, "Leather Bracers", "🧤", ItemSlot.HANDS, 1, 25, def = 2, hp = 2, description = "Supple wrist guards for tool and weapon handling.", style = ItemStyle.LIGHT),
        ItemEntity(1189, "Iron Vambraces", "🦾", ItemSlot.HANDS, 1, 30, def = 2, hp = 3, description = "Solid iron forearm guards.", style = ItemStyle.PLATE),
        ItemEntity(52, "Worn Trousers", "\uD83D\uDC56", ItemSlot.LEGS, 1, 25, def = 2, hp = 2, description = "They've seen a thousand squats. They'll see a thousand more.", style = ItemStyle.LIGHT),
        ItemEntity(1190, "Linen Pants", "👖", ItemSlot.LEGS, 1, 25, def = 1, hp = 3, description = "Breathable lightweight pants allowing full freedom of movement.", style = ItemStyle.ROBE),
        ItemEntity(1191, "Iron Greaves", "🦿", ItemSlot.LEGS, 1, 30, def = 3, hp = 3, description = "Iron shin guards over quilted padding.", style = ItemStyle.PLATE),
        ItemEntity(53, "Old Boots", "\uD83E\uDD7E", ItemSlot.FEET, 1, 25, def = 2, hp = 2, description = "Broken in. Like your excuses.", style = ItemStyle.LIGHT),
        ItemEntity(1183, "Cloth Slippers", "🥿", ItemSlot.FEET, 1, 25, def = 1, hp = 3, description = "Soft, silent slippers tailored from sturdy cotton.", style = ItemStyle.ROBE),
        ItemEntity(1193, "Iron Sabatons", "🥾", ItemSlot.FEET, 1, 30, def = 3, hp = 3, description = "Reinforced steel-toed boots.", style = ItemStyle.PLATE),

        // ---- Universal adventurer gear ----
        ItemEntity(1101, "Adventurer's Hood", "\uD83E\uDDE2", ItemSlot.HEAD, 2, 95, def = 4, hp = 6, description = "A reliable hood for heroes between identities.", style = ItemStyle.LIGHT),
        ItemEntity(1102, "Adventurer's Vest", "\uD83E\uDDBA", ItemSlot.CHEST, 2, 130, def = 7, hp = 10, description = "Practical armor with enough pockets for snacks and loot.", style = ItemStyle.LIGHT),
        ItemEntity(1103, "Adventurer's Grips", "\uD83E\uDDE4", ItemSlot.HANDS, 2, 90, def = 3, hp = 5, description = "Made for lifting, looting, and holding the line.", style = ItemStyle.LIGHT),
        ItemEntity(1104, "Adventurer's Legguards", "\uD83D\uDC56", ItemSlot.LEGS, 2, 95, def = 5, hp = 7, description = "Flexible enough for lunges, sturdy enough for trouble.", style = ItemStyle.LIGHT),
        ItemEntity(1105, "Adventurer's Boots", "\uD83E\uDD7E", ItemSlot.FEET, 2, 90, def = 4, hp = 6, description = "Every journey starts with footwear that can survive stairs.", style = ItemStyle.LIGHT),
        ItemEntity(1111, "Veteran's Coif", "\uD83D\uDEE1\uFE0F", ItemSlot.HEAD, 3, 220, def = 7, hp = 10, description = "Battle-tested, sweat-tested, and still presentable.", style = ItemStyle.PLATE),
        ItemEntity(1112, "Veteran's Harness", "\uD83E\uDDBA", ItemSlot.CHEST, 3, 320, def = 12, hp = 20, description = "Universal kit for anyone who has earned a harder road.", style = ItemStyle.PLATE),
        ItemEntity(1113, "Veteran's Gauntlets", "\uD83E\uDDE4", ItemSlot.HANDS, 3, 210, def = 6, hp = 9, description = "Firm grip, steady wrists, fewer regrettable choices.", style = ItemStyle.PLATE),
        ItemEntity(1114, "Veteran's Greaves", "\uD83E\uDDBF", ItemSlot.LEGS, 3, 230, def = 8, hp = 12, description = "For the heroic art of not skipping leg day.", style = ItemStyle.PLATE),
        ItemEntity(1115, "Veteran's Treads", "\uD83E\uDD7E", ItemSlot.FEET, 3, 210, def = 6, hp = 10, description = "Heavy enough to feel earned, light enough to keep moving.", style = ItemStyle.PLATE),
        ItemEntity(1121, "Mythic Wayfarer Crown", "\uD83D\uDC51", ItemSlot.HEAD, 4, 520, def = 10, hp = 18, description = "A crown for heroes who refuse to be boxed into one legend.", style = ItemStyle.LIGHT),
        ItemEntity(1122, "Mythic Wayfarer Mantle", "\uD83E\uDDE5", ItemSlot.CHEST, 4, 760, def = 17, hp = 36, description = "Adaptive armor that looks at class restrictions and politely ignores them.", style = ItemStyle.LIGHT),
        ItemEntity(1123, "Mythic Wayfarer Gloves", "\u2728", ItemSlot.HANDS, 4, 500, def = 9, hp = 16, description = "Fit for spellwork, swordwork, and re-racking your weights.", style = ItemStyle.LIGHT),
        ItemEntity(1124, "Mythic Wayfarer Greaves", "\uD83D\uDC56", ItemSlot.LEGS, 4, 540, def = 12, hp = 22, description = "Leg armor for long roads and longer progression arcs.", style = ItemStyle.LIGHT),
        ItemEntity(1125, "Mythic Wayfarer Boots", "\uD83E\uDD7E", ItemSlot.FEET, 4, 500, def = 10, hp = 18, description = "They make every biome feel one step closer.", style = ItemStyle.LIGHT),

        // ---- Consumable supplies & In-Combat Tactical Items ----
        ItemEntity(Consumables.STREAK_FREEZE, "Streak Freeze", "\u2744\uFE0F", ItemSlot.CONSUMABLE, 1, 250,
            description = "Protects your workout streak for one missed day. Used automatically when you need it."),
        ItemEntity(Consumables.XP_ELIXIR_MINOR, "Minor XP Elixir", "\uD83E\uDDEA", ItemSlot.CONSUMABLE, 1, 120,
            description = "Drink before training: your next workout earns +${Consumables.XP_MINOR_BONUS} bonus XP."),
        ItemEntity(Consumables.XP_ELIXIR_MAJOR, "Major XP Elixir", "\u2697\uFE0F", ItemSlot.CONSUMABLE, 2, 400,
            description = "Drink before training: your next workout earns +${Consumables.XP_MAJOR_BONUS} bonus XP."),
        ItemEntity(Consumables.POTION_OF_REBIRTH, "Potion of Rebirth", "\uD83D\uDD2E", ItemSlot.CONSUMABLE, 3, 600,
            description = "Resets STR, END, AGI, and WIL to 5 and refunds every earned point to spend however you like."),
        ItemEntity(Consumables.HEALTH_POTION, "Health Potion", "🧪", ItemSlot.CONSUMABLE, 1, 60,
            description = "Use in combat to restore 35% of your Max HP immediately."),
        ItemEntity(Consumables.CLEANSING_SALVE, "Cleansing Salve", "🌿", ItemSlot.CONSUMABLE, 2, 90,
            description = "Use in combat to instantly cleanse poisons, bleeds, and status debuffs."),
        ItemEntity(Consumables.BATTLE_ELIXIR, "Battle Elixir", "⚔️", ItemSlot.CONSUMABLE, 2, 110,
            description = "Drink in combat: boosts Attack by +35% for 3 turns."),
        ItemEntity(Consumables.IRONHIDE_SALVE, "Ironhide Salve", "🛡️", ItemSlot.CONSUMABLE, 2, 110,
            description = "Apply in combat: strengthens Defense by +30 for 3 turns."),

        // ---- Unique Boss Signature Relics (Trophies) & Beta Exclusives ----
        ItemEntity(1205, "Pioneer's Amulet", "🌟", ItemSlot.TRINKET, 3, 1000, atk = 12, def = 12, hp = 30,
            description = "Exclusive relic awarded to Closed Beta pioneers. Emits an aura of ancient determination.", style = ItemStyle.LIGHT),
        ItemEntity(1201, "Boar's Slumber Girdle", "🐗", ItemSlot.TRINKET, 2, 350, def = 6, hp = 25,
            description = "Trophy of the Lazy Boar. Embraces restorative rest: catching breath on Defend restores +15 bonus HP."),
        ItemEntity(1202, "Spotter's Ribcage Aegis", "💀", ItemSlot.TRINKET, 3, 500, atk = 4, def = 10, hp = 30,
            description = "Trophy of the Skeleton Spotter. Warded bone shards blunt incoming blows and reflect damage."),
        ItemEntity(1203, "Ogre's Resolution Smasher", "👹", ItemSlot.TRINKET, 3, 550, atk = 12, def = 6, hp = 20,
            description = "Trophy of the Ogre of Excuses. Heavy attacks shatter enemy posture with high Stagger chance."),
        ItemEntity(1204, "Wraith's Treadmill Cinders", "👻", ItemSlot.TRINKET, 4, 850, atk = 10, def = 8, hp = 25,
            description = "Trophy of the Cardio Wraith. Imbued with infinite aerobic cadence: grants +25 Speed in combat."),
        ItemEntity(1205, "Golem's Unyielding Core", "🤖", ItemSlot.TRINKET, 4, 900, def = 16, hp = 45,
            description = "Trophy of the Iron Golem. Barbell-forged iron plates grant +10% physical damage mitigation."),
        ItemEntity(1206, "Burnout Dragon's Flameheart", "🐲", ItemSlot.TRINKET, 4, 1100, atk = 16, def = 10, hp = 40,
            description = "Trophy of the Burnout Dragon. Converts intensity to fire: Critical strikes ignite enemies for 3 turns."),

        // ---- Extra diverse weapons & gear progression ----
        ItemEntity(1131, "Swiftwind Scythe", "🌾", ItemSlot.WEAPON, 2, 115, atk = 7, def = 1, description = "Sweeps through training volume with effortless cadence.", style = ItemStyle.SWORD),
        ItemEntity(1132, "Thunderstrike Greatclub", "🪵", ItemSlot.WEAPON, 3, 310, atk = 14, hp = 10, description = "When subtlety fails, heavy sets succeed.", style = ItemStyle.MACE),
        ItemEntity(1133, "Aegis-Breaker Pike", "🔱", ItemSlot.WEAPON, 3, 290, atk = 12, def = 3, description = "Finds weak points in enemy form and posture.", style = ItemStyle.GREATSWORD),
        ItemEntity(1134, "Solar Flare Warblade", "⚔️", ItemSlot.WEAPON, 4, 720, atk = 23, def = 4, hp = 15, description = "Shines with the radiant energy of an unbroken streak.", style = ItemStyle.GREATSWORD),
        ItemEntity(1135, "Voidcaller Grimstaff", "🔮", ItemSlot.WEAPON, 4, 710, atk = 21, def = 6, hp = 24, description = "A mysterious conduit for absolute mental fortitude.", style = ItemStyle.STAFF),

        // ---- Additional diverse trinkets ----
        ItemEntity(1141, "Iron Will Locket", "📿", ItemSlot.TRINKET, 1, 55, def = 2, hp = 8, description = "Holds a reminder of why you started."),
        ItemEntity(1142, "Band of the Swiftfoot", "💍", ItemSlot.TRINKET, 2, 175, atk = 4, def = 2, hp = 10, description = "Light as morning mist. Enhances agility and speed."),
        ItemEntity(1143, "Titan's Power Belt", "🥋", ItemSlot.TRINKET, 2, 190, atk = 6, hp = 15, description = "Braces your core for heavy lifts and tough rounds."),
        ItemEntity(1144, "Phoenix Ember Brooch", "🔥", ItemSlot.TRINKET, 3, 420, atk = 8, def = 4, hp = 16, description = "Rises stronger from every failed attempt."),
        ItemEntity(1145, "Relic of Unbroken Focus", "💎", ItemSlot.TRINKET, 4, 920, atk = 10, def = 8, hp = 35, description = "The crown jewel of lifelong discipline."),

        // ---- Additional armor options ----
        ItemEntity(1151, "Gladiator's Visor", "🪖", ItemSlot.HEAD, 2, 90, def = 4, hp = 8, description = "Built for arena sand and barbell racks.", style = ItemStyle.PLATE),
        ItemEntity(1152, "Shadowdancer Bracers", "🧤", ItemSlot.HANDS, 2, 85, atk = 2, def = 3, hp = 4, description = "Form-fitting grips for rapid strikes.", style = ItemStyle.LIGHT),
        ItemEntity(1153, "Ironstride Greaves", "🦿", ItemSlot.LEGS, 2, 100, def = 6, hp = 8, description = "Sturdy plated protection for heavy leg training.", style = ItemStyle.PLATE),
        ItemEntity(1154, "Zephyr Treads", "👟", ItemSlot.FEET, 2, 85, def = 3, hp = 8, description = "Cushioned soles that make cardio feel weightless.", style = ItemStyle.LIGHT),
        ItemEntity(1161, "Drakescale Helm", "🐲", ItemSlot.HEAD, 3, 230, def = 8, hp = 14, description = "Forged from the shed scales of mountain wyverns.", style = ItemStyle.PLATE),
        ItemEntity(1162, "Astral Silk Robes", "👘", ItemSlot.CHEST, 3, 310, def = 11, hp = 26, description = "Imbued with the quiet stillness of deep meditation.", style = ItemStyle.ROBE),
        ItemEntity(1163, "Colossus Pauldrons", "🛡️", ItemSlot.CHEST, 4, 750, def = 19, hp = 42, description = "Shoulder armor wide enough to shoulder entire campaigns.", style = ItemStyle.PLATE),

        // ---- Mid-tier gear fills ----
        ItemEntity(60, "Training Blade", "🗡️", ItemSlot.WEAPON, 2, 90, atk = 5,
            description = "A reliable sidearm between rusty and legendary.", classAffinity = W, style = ItemStyle.SWORD),
        ItemEntity(61, "Crystal Focus", "🔮", ItemSlot.WEAPON, 2, 90, atk = 5,
            description = "Channels willpower through a cloudy gem.", classAffinity = M, style = ItemStyle.WAND),
        ItemEntity(62, "Stiletto", "🗡️", ItemSlot.WEAPON, 2, 90, atk = 5,
            description = "Thin, quiet, and judgmental about your form.", classAffinity = T, style = ItemStyle.DAGGER),
        ItemEntity(63, "Trail Bow", "🏹", ItemSlot.WEAPON, 2, 90, atk = 5,
            description = "Perfect for warming up before the real hunt.", classAffinity = R, style = ItemStyle.BOW),
        ItemEntity(70, "Squire's Arming Sword", "⚔️", ItemSlot.WEAPON, 2, 90, atk = 5,
            description = "Holy enough for practice, sharp enough for pride.", classAffinity = P, style = ItemStyle.SWORD),
        ItemEntity(71, "Cinder Staff", "🔥", ItemSlot.WEAPON, 2, 90, atk = 5,
            description = "A warm bone shaft that thrives on stubborn willpower.", classAffinity = N, style = ItemStyle.STAFF),
        ItemEntity(64, "Bronze Guard", "🛡️", ItemSlot.CHEST, 2, 100, def = 5, hp = 8,
            description = "A stopgap between padded cloth and real steel.", style = ItemStyle.PLATE),
        ItemEntity(65, "Traveler's Cloak", "🧥", ItemSlot.CHEST, 3, 220, def = 8, hp = 14,
            description = "Keeps the rain off and the XP on.", style = ItemStyle.LIGHT),

        // ---- Runes (800s) ----
        ItemEntity(Runes.POWER, "Rune of Power", "\uD83D\uDCAA", ItemSlot.RUNE, 1, 80, atk = 3,
            description = "Socket into gear: +3 ATK."),
        ItemEntity(Runes.WARD, "Rune of Ward", "\uD83D\uDEE1\uFE0F", ItemSlot.RUNE, 1, 80, def = 3,
            description = "Socket into gear: +3 DEF."),
        ItemEntity(Runes.VITAL, "Rune of Vitality", "\uD83D\uDC9A", ItemSlot.RUNE, 2, 120, hp = 12,
            description = "Socket into gear: +12 HP."),
        ItemEntity(Runes.SWIFT, "Rune of Swiftness", "\uD83D\uDCA8", ItemSlot.RUNE, 2, 140,
            description = "Socket into gear: +4 SPD in combat."),
        ItemEntity(Runes.FURY, "Rune of Fury", "\uD83D\uDD25", ItemSlot.RUNE, 3, 200,
            description = "Socket into gear: +5% crit chance."),
        ItemEntity(Runes.SIPHON, "Rune of Siphon", "\uD83E\uDE78", ItemSlot.RUNE, 3, 220,
            description = "Socket into gear: heal a little when you defeat an enemy."),

        // ---- Materials (700s) ----
        ItemEntity(Materials.SCRAP_IRON, "Scrap Iron", "\u2699\uFE0F", ItemSlot.MATERIAL, 1, 15,
            description = "Twisted metal from fallen foes. Sells for a few coins."),
        ItemEntity(Materials.ARCANE_DUST, "Arcane Dust", "\u2728", ItemSlot.MATERIAL, 2, 25,
            description = "Glitters when you hit a personal record."),
        ItemEntity(Materials.BEAST_HIDE, "Beast Hide", "\uD83E\uDDE4", ItemSlot.MATERIAL, 2, 22,
            description = "Tough leather from meadow and marsh alike."),
        ItemEntity(Materials.EMBER_COAL, "Ember Coal", "\uD83D\uDD25", ItemSlot.MATERIAL, 3, 35,
            description = "Still warm. Smells like a PR attempt."),

        // ---- Loot chests (750s) ----
        ItemEntity(LootChests.WOODEN, "Wooden Chest", "\uD83D\uDCE6", ItemSlot.LOOT_CHEST, 1, 100,
            description = "A humble box of spoils. Open it from your inventory."),
        ItemEntity(LootChests.BIOME, "Biome Cache", "\uD83C\uDF81", ItemSlot.LOOT_CHEST, 2, 200,
            description = "Packed by the land itself. Open for 2-4 rewards."),
        ItemEntity(LootChests.WAR_CACHE, "War Cache", "\u2694\uFE0F", ItemSlot.LOOT_CHEST, 3, 350,
            description = "Ambush spoils. Expect something fierce inside.")
    ) + setPieces()

    /**
     * Generates the class armor sets: head/hands/legs/feet at tiers 2-4 for each class.
     * IDs are deterministic (100 + class*60 + tier*10 + slot) so the catalog can
     * safely sync with the database across app updates.
     */
    private fun setPieces(): List<ItemEntity> {
        data class Theme(
            val cls: CharacterClass,
            val setNames: Map<Int, String>,
            val pieces: Map<ItemSlot, String>,
            val style: String
        )

        val themes = listOf(
            Theme(W, mapOf(2 to "Ironbound", 3 to "Steelwrought", 4 to "Titanforged"),
                mapOf(ItemSlot.HEAD to "Helm", ItemSlot.CHEST to "Cuirass", ItemSlot.HANDS to "Gauntlets", ItemSlot.LEGS to "Greaves", ItemSlot.FEET to "Sabatons"),
                ItemStyle.PLATE),
            Theme(M, mapOf(2 to "Apprentice", 3 to "Runebound", 4 to "Cosmic"),
                mapOf(ItemSlot.HEAD to "Hat", ItemSlot.CHEST to "Robe", ItemSlot.HANDS to "Gloves", ItemSlot.LEGS to "Leggings", ItemSlot.FEET to "Slippers"),
                ItemStyle.ROBE),
            Theme(T, mapOf(2 to "Prowler's", 3 to "Shadowstep", 4 to "Nightveil"),
                mapOf(ItemSlot.HEAD to "Cowl", ItemSlot.CHEST to "Vest", ItemSlot.HANDS to "Grips", ItemSlot.LEGS to "Breeches", ItemSlot.FEET to "Softsoles"),
                ItemStyle.LIGHT),
            Theme(R, mapOf(2 to "Scout's", 3 to "Wildstalker", 4 to "Worldroot"),
                mapOf(ItemSlot.HEAD to "Hood", ItemSlot.CHEST to "Tunic", ItemSlot.HANDS to "Bracers", ItemSlot.LEGS to "Trousers", ItemSlot.FEET to "Striders"),
                ItemStyle.LIGHT),
            Theme(P, mapOf(2 to "Oathbound", 3 to "Radiant", 4 to "Dawnforged"),
                mapOf(ItemSlot.HEAD to "Helm", ItemSlot.CHEST to "Cuirass", ItemSlot.HANDS to "Gauntlets", ItemSlot.LEGS to "Greaves", ItemSlot.FEET to "Sabatons"),
                ItemStyle.PLATE),
            Theme(N, mapOf(2 to "Bonebound", 3 to "Cryptweave", 4 to "Eclipse"),
                mapOf(ItemSlot.HEAD to "Hood", ItemSlot.CHEST to "Vestments", ItemSlot.HANDS to "Gloves", ItemSlot.LEGS to "Wraps", ItemSlot.FEET to "Slippers"),
                ItemStyle.ROBE),
            Theme(WM, mapOf(2 to "Vestments of Light", 3 to "Sanctified", 4 to "Celestial"),
                mapOf(ItemSlot.HEAD to "Miter", ItemSlot.CHEST to "Robes", ItemSlot.HANDS to "Gloves", ItemSlot.LEGS to "Vestments", ItemSlot.FEET to "Slippers"),
                ItemStyle.ROBE),
            Theme(MK, mapOf(2 to "Bandit", 3 to "Temple", 4 to "Dragon-Fist"),
                mapOf(ItemSlot.HEAD to "Headband", ItemSlot.CHEST to "Garb", ItemSlot.HANDS to "Wraps", ItemSlot.LEGS to "Pants", ItemSlot.FEET to "Sandals"),
                ItemStyle.LIGHT),
            Theme(DR, mapOf(2 to "Wildwood", 3 to "Sylvan", 4 to "Archdruid"),
                mapOf(ItemSlot.HEAD to "Cowl", ItemSlot.CHEST to "Raiment", ItemSlot.HANDS to "Bracers", ItemSlot.LEGS to "Kilt", ItemSlot.FEET to "Striders"),
                ItemStyle.LIGHT),
            Theme(BS, mapOf(2 to "Bloodbound", 3 to "Warlord", 4 to "Juggernaut"),
                mapOf(ItemSlot.HEAD to "Helm", ItemSlot.CHEST to "Harness", ItemSlot.HANDS to "Gauntlets", ItemSlot.LEGS to "Greaves", ItemSlot.FEET to "Sabatons"),
                ItemStyle.PLATE),
            Theme(BD, mapOf(2 to "Minstrel", 3 to "Virtuoso", 4 to "Harmonic"),
                mapOf(ItemSlot.HEAD to "Cap", ItemSlot.CHEST to "Silks", ItemSlot.HANDS to "Gloves", ItemSlot.LEGS to "Trousers", ItemSlot.FEET to "Boots"),
                ItemStyle.ROBE),
            Theme(SM, mapOf(2 to "Evoker's", 3 to "Summoner's", 4 to "Caller's"),
                mapOf(ItemSlot.HEAD to "Horn", ItemSlot.CHEST to "Doublet", ItemSlot.HANDS to "Cuffs", ItemSlot.LEGS to "Spats", ItemSlot.FEET to "Pigaches"),
                ItemStyle.ROBE),
            Theme(DG, mapOf(2 to "Wyrmkin", 3 to "Dragonbane", 4 to "Dragonlord"),
                mapOf(ItemSlot.HEAD to "Armet", ItemSlot.CHEST to "Mail", ItemSlot.HANDS to "Finger-gauntlets", ItemSlot.LEGS to "Breeches", ItemSlot.FEET to "Sabatons"),
                ItemStyle.PLATE)
        )

        val slots = listOf(ItemSlot.HEAD, ItemSlot.CHEST, ItemSlot.HANDS, ItemSlot.LEGS, ItemSlot.FEET)
        val emojis = mapOf(
            ItemSlot.HEAD to "\uD83E\uDE96", ItemSlot.CHEST to "\uD83D\uDC55", ItemSlot.HANDS to "\uD83E\uDDE4",
            ItemSlot.LEGS to "\uD83D\uDC56", ItemSlot.FEET to "\uD83E\uDD7E"
        )
        val prices = mapOf(
            ItemSlot.HEAD to mapOf(2 to 70, 3 to 180, 4 to 420),
            ItemSlot.CHEST to mapOf(2 to 80, 3 to 200, 4 to 450),
            ItemSlot.HANDS to mapOf(2 to 50, 3 to 130, 4 to 300),
            ItemSlot.LEGS to mapOf(2 to 60, 3 to 150, 4 to 350),
            ItemSlot.FEET to mapOf(2 to 50, 3 to 130, 4 to 300)
        )
        val defs = mapOf(
            ItemSlot.HEAD to mapOf(2 to 4, 3 to 8, 4 to 12),
            ItemSlot.CHEST to mapOf(2 to 5, 3 to 10, 4 to 15),
            ItemSlot.HANDS to mapOf(2 to 3, 3 to 6, 4 to 9),
            ItemSlot.LEGS to mapOf(2 to 4, 3 to 7, 4 to 11),
            ItemSlot.FEET to mapOf(2 to 3, 3 to 6, 4 to 9)
        )
        val hps = mapOf(
            ItemSlot.HEAD to mapOf(2 to 6, 3 to 12, 4 to 20),
            ItemSlot.CHEST to mapOf(2 to 8, 3 to 16, 4 to 25),
            ItemSlot.HANDS to mapOf(2 to 4, 3 to 8, 4 to 14),
            ItemSlot.LEGS to mapOf(2 to 5, 3 to 10, 4 to 18),
            ItemSlot.FEET to mapOf(2 to 4, 3 to 8, 4 to 14)
        )

        return themes.flatMapIndexed { clsIdx, theme ->
            (2..4).flatMap { tier ->
                slots.mapIndexed { slotIdx, slot ->
                    ItemEntity(
                        id = 2000L + clsIdx * 60 + tier * 10 + slotIdx,
                        name = "${theme.setNames.getValue(tier)} ${theme.pieces.getValue(slot)}",
                        emoji = emojis.getValue(slot),
                        slot = slot,
                        tier = tier,
                        price = prices.getValue(slot).getValue(tier),
                        def = defs.getValue(slot).getValue(tier),
                        hp = hps.getValue(slot).getValue(tier),
                        description = "Part of the ${theme.setNames.getValue(tier)} set. " +
                            "Wear all 5 ${theme.cls.label} armor pieces for a set bonus.",
                        classAffinity = theme.cls,
                        style = theme.style
                    )
                }
            }
        }
    }
}

/**
 * Combat quirks that make monsters fight differently. Each one rewards a
 * different player stat or tactic, so builds and skill choices matter.
 */
/**
 * Combat quirks that make monsters fight differently. Each one rewards a
 * different player stat or tactic, so builds and skill choices matter.
 */
enum class MonsterTrait(val label: String, val emoji: String, val blurb: String) {
    ARMORED("Armored", "\uD83D\uDEE1\uFE0F", "Takes 30% less attack damage. Defense-piercing skills cut through."),
    SWIFT("Swift", "\uD83D\uDCA8", "Hard to hit and may strike twice. Speed keeps you out of reach."),
    VENOMOUS("Venomous", "\u2620\uFE0F", "Bites can poison you. Willpower shakes poison off faster."),
    REGENERATING("Regenerating", "\uD83D\uDC9A", "Recovers HP every turn. Finish it fast."),
    ENRAGED("Enraged", "\uD83D\uDE21", "Hits 40% harder once wounded. End the fight before it ends you.")
}

/**
 * Monster variant tier: roaming monsters can roll as Elite or Epic champions with boosted stats and rewards.
 */
enum class MonsterVariant(
    val label: String,
    val badge: String,
    val hpMult: Double,
    val atkMult: Double,
    val rewardMult: Double
) {
    NORMAL("Normal", "", 1.0, 1.0, 1.0),
    ELITE("Elite", "⭐ ELITE", 1.35, 1.20, 2.0),
    EPIC("Epic", "👑 EPIC", 1.70, 1.40, 3.5)
}

/**
 * Telegraphed super-attack that enemies charge up for 1 turn, giving the player tactical warning.
 */
data class TelegraphedMove(
    val name: String,
    val emoji: String,
    val description: String,
    val turnsUntilStrike: Int = 1,
    val damageMultiplier: Double = 2.2,
    val armorPiercing: Boolean = false,
    val appliesStun: Boolean = false
)

data class Monster(
    val id: Int,
    val name: String,
    val emoji: String,
    val level: Int,
    val tier: Int,
    val hp: Int,
    val atk: Int,
    val def: Int,
    val spd: Int,
    val goldReward: Int,
    val xpReward: Int,
    val biome: Biome,
    val description: String,
    val trait: MonsterTrait? = null,
    val secondaryTrait: MonsterTrait? = null,
    val bossOf: Biome? = null,
    val variant: MonsterVariant = MonsterVariant.NORMAL,
    val telegraphedMove: TelegraphedMove? = null
) {
    val isBoss: Boolean get() = bossOf != null

    fun withVariant(v: MonsterVariant, extraTrait: MonsterTrait? = null): Monster {
        if (v == MonsterVariant.NORMAL) return this
        val newHp = (hp * v.hpMult).toInt()
        val newAtk = (atk * v.atkMult).toInt()
        val newGold = (goldReward * v.rewardMult).toInt()
        val newXp = (xpReward * v.rewardMult).toInt()
        val telegraphed = if (v == MonsterVariant.EPIC && telegraphedMove == null) {
            TelegraphedMove(
                name = "Primal Overload",
                emoji = "⚡",
                description = "Gathers terrifying energy to strike for massive damage! DEFEND or STUN!",
                damageMultiplier = 2.2
            )
        } else telegraphedMove

        return copy(
            name = "${v.badge} $name",
            hp = newHp,
            atk = newAtk,
            goldReward = newGold,
            xpReward = newXp,
            variant = v,
            secondaryTrait = extraTrait ?: secondaryTrait,
            telegraphedMove = telegraphed
        )
    }
}

object MonsterCatalog {
    val all: List<Monster> = listOf(
        // Meadowlands (lv 1-3)
        Monster(1, "Couch Slime", "🤢", 1, 1, 140, 24, 6, 5, 20, 35, Biome.MEADOWLANDS, "A gelatinous blob that hasn't moved in weeks. Relatable, but it must be stopped."),
        Monster(2, "Gym Rat", "🐀", 2, 1, 160, 28, 8, 9, 30, 50, Biome.MEADOWLANDS, "It hoards all the dumbbells and never re-racks them."),
        Monster(19, "Snooze Gremlin", "⏰", 2, 1, 150, 26, 7, 16, 30, 48, Biome.MEADOWLANDS, "It hits your snooze button from inside the walls. Quick little menace.", MonsterTrait.SWIFT),
        Monster(20, "Junk Food Mimic", "🍔", 3, 1, 190, 30, 9, 7, 40, 65, Biome.MEADOWLANDS, "Looks delicious. Bites back. Poisons your macros.", MonsterTrait.VENOMOUS),
        Monster(31, "Sugar Crash Specter", "🍩", 3, 1, 175, 32, 8, 12, 42, 68, Biome.MEADOWLANDS, "Manifests when energy crashes. Strikes with sudden furious spikes.", MonsterTrait.ENRAGED),
        Monster(32, "Skipping Rope Viper", "🪢", 3, 1, 165, 28, 8, 18, 38, 62, Biome.MEADOWLANDS, "Tangles up your footwork and strikes in rapid succession.", MonsterTrait.SWIFT),
        Monster(9, "Lazy Boar", "🐗", 3, 1, 320, 38, 14, 8, 55, 85, Biome.MEADOWLANDS, "It naps in the sun and judges joggers. Today, it judges you.",
            trait = MonsterTrait.REGENERATING,
            bossOf = Biome.MEADOWLANDS,
            telegraphedMove = TelegraphedMove("Mudslide Slam", "🐗", "Braces its massive hooves to charge down the meadow! DEFEND or STUN!", damageMultiplier = 2.0)
        ),

        // Darkwood (lv 4-6)
        Monster(3, "Procrastination Imp", "👿", 4, 2, 280, 40, 12, 12, 60, 90, Biome.DARKWOOD, "Whispers 'you can work out tomorrow' into your ear."),
        Monster(21, "Excuse Goblin", "👺", 5, 2, 310, 44, 12, 19, 68, 105, Biome.DARKWOOD, "Darts between trees flinging excuses. Hard to pin down.", MonsterTrait.SWIFT),
        Monster(10, "Doomscroll Spider", "🕷️", 5, 2, 340, 48, 14, 14, 75, 115, Biome.DARKWOOD, "Its web is infinite. Its venom keeps you scrolling.", MonsterTrait.VENOMOUS),
        Monster(22, "Couch Potato Golem", "🥔", 6, 2, 450, 46, 26, 6, 95, 135, Biome.DARKWOOD, "Centuries of sitting have compressed it into solid starch armor.", MonsterTrait.ARMORED),
        Monster(33, "Form Police Gargoyle", "🗿", 6, 2, 390, 50, 22, 10, 92, 130, Biome.DARKWOOD, "Stony critic that punishes every slip in posture.", MonsterTrait.ARMORED),
        Monster(34, "Ego-Lift Minotaur", "🐂", 6, 2, 460, 54, 16, 13, 98, 140, Biome.DARKWOOD, "Stacks way too much weight and enters a berserk frenzy when challenged.", MonsterTrait.ENRAGED),
        Monster(4, "Skeleton Spotter", "💀", 6, 2, 620, 56, 20, 14, 110, 160, Biome.DARKWOOD, "It has literally no muscles. It is so jealous of yours.",
            trait = MonsterTrait.ARMORED,
            bossOf = Biome.DARKWOOD,
            telegraphedMove = TelegraphedMove("Barbell Guillotine", "💀", "Raises a colossal weighted barbell overhead! DEFEND or STUN!", damageMultiplier = 2.2, armorPiercing = true)
        ),

        // Crystal Caves (lv 7-9)
        Monster(11, "Crystal Crab", "🦀", 7, 2, 490, 56, 28, 8, 110, 150, Biome.CRYSTAL_CAVES, "Its shell is pure gem. Attacks glance right off it.", MonsterTrait.ARMORED),
        Monster(12, "Echo Bat", "🦇", 8, 3, 460, 62, 16, 22, 120, 165, Biome.CRYSTAL_CAVES, "It repeats every excuse you've ever made, in your own voice.", MonsterTrait.SWIFT),
        Monster(23, "Gem Serpent", "🐍", 8, 3, 510, 62, 20, 16, 125, 170, Biome.CRYSTAL_CAVES, "Its fangs drip crystallized venom worth a fortune. Don't get bitten.", MonsterTrait.VENOMOUS),
        Monster(24, "Prism Wisp", "🗯️", 9, 3, 500, 70, 15, 26, 140, 195, Biome.CRYSTAL_CAVES, "A shard of living light that refuses to hold still.", MonsterTrait.SWIFT),
        Monster(35, "Cramp Basilisk", "🦎", 9, 3, 540, 66, 22, 14, 145, 200, Biome.CRYSTAL_CAVES, "Its gaze locks muscles into sudden agonizing spasms.", MonsterTrait.VENOMOUS),
        Monster(36, "Resonant Stalagmite", "💎", 9, 3, 580, 60, 32, 7, 142, 190, Biome.CRYSTAL_CAVES, "Living cavern crystal that vibrates and deflects blunt force.", MonsterTrait.ARMORED),
        Monster(5, "Ogre of Excuses", "👹", 9, 3, 880, 76, 25, 10, 175, 240, Biome.CRYSTAL_CAVES, "Its club is carved from a thousand broken New Year's resolutions.",
            trait = MonsterTrait.ENRAGED,
            bossOf = Biome.CRYSTAL_CAVES,
            telegraphedMove = TelegraphedMove("Resolution Crusher", "👹", "Winds up a catastrophic sweep with its colossal club! DEFEND or STUN!", damageMultiplier = 2.3)
        ),

        // Ember Peaks (lv 11-13)
        Monster(13, "Magma Hound", "🔥", 11, 3, 750, 86, 24, 16, 180, 250, Biome.EMBER_PEAKS, "It fetches boulders. The angrier it gets, the hotter it burns.", MonsterTrait.ENRAGED),
        Monster(25, "Ash Ghoul", "🧟", 12, 3, 790, 84, 22, 13, 195, 260, Biome.EMBER_PEAKS, "Rises from every burned-out training plan. Keeps getting back up.", MonsterTrait.REGENERATING),
        Monster(14, "Cinder Imp", "🧨", 13, 3, 850, 94, 25, 18, 220, 290, Biome.EMBER_PEAKS, "It lights the sauna way too hot on purpose.", MonsterTrait.SWIFT),
        Monster(26, "Lava Turtle", "🐢", 13, 3, 1020, 86, 42, 6, 230, 300, Biome.EMBER_PEAKS, "Its obsidian shell shrugs off almost anything. Slow and smug about it.", MonsterTrait.ARMORED),
        Monster(37, "Sauna Pyromancer", "🧙‍♂️", 12, 3, 820, 96, 20, 17, 210, 275, Biome.EMBER_PEAKS, "Pours molten heat over the arena, testing your thermoregulation.", MonsterTrait.ENRAGED),
        Monster(38, "Overload Chimera", "🦁", 13, 3, 940, 92, 30, 15, 225, 295, Biome.EMBER_PEAKS, "Amalgamation of three conflicting training programs in eternal burning fury.", MonsterTrait.REGENERATING),
        Monster(6, "Cardio Wraith", "👻", 12, 3, 1150, 102, 30, 24, 250, 330, Biome.EMBER_PEAKS, "It runs eternally, fueled by the souls of abandoned treadmills.",
            trait = MonsterTrait.SWIFT,
            bossOf = Biome.EMBER_PEAKS,
            telegraphedMove = TelegraphedMove("Treadmill Blitz", "👻", "Revs into Mach 3 sprint for a blinding supersonic lunge! DEFEND or STUN!", damageMultiplier = 2.4)
        ),

        // Frozen Wastes (lv 15-17)
        Monster(15, "Frost Yeti", "⛄", 15, 4, 1120, 108, 32, 12, 280, 360, Biome.FROZEN_WASTES, "Takes ice baths recreationally. The cold knits its wounds shut.", MonsterTrait.REGENERATING),
        Monster(27, "Frostbite Wolf", "🐺", 16, 4, 1150, 112, 30, 28, 300, 385, Biome.FROZEN_WASTES, "It circles twice before you can blink. Pack tactics, solo executed.", MonsterTrait.SWIFT),
        Monster(16, "Blizzard Elemental", "🌨️", 17, 4, 1240, 118, 36, 22, 340, 425, Biome.FROZEN_WASTES, "A snowstorm with a grudge and surprisingly good footwork.", MonsterTrait.SWIFT),
        Monster(28, "Permafrost Lich", "🧙", 17, 4, 1220, 122, 36, 16, 350, 440, Biome.FROZEN_WASTES, "An ancient coach who never let anyone skip warm-ups. Death didn't stop him.", MonsterTrait.REGENERATING),
        Monster(39, "Cryo-Stasis Golem", "🧊", 16, 4, 1400, 110, 46, 8, 335, 420, Biome.FROZEN_WASTES, "Cooled to absolute zero. Attacks glance off its glacial armor.", MonsterTrait.ARMORED),
        Monster(40, "Rest-Day Siren", "🧜‍♀️", 17, 4, 1180, 126, 28, 20, 360, 450, Biome.FROZEN_WASTES, "Sings a hypnotic melody tempting you to cancel your morning workout.", MonsterTrait.VENOMOUS),
        Monster(7, "Iron Golem", "🤖", 16, 4, 1650, 124, 48, 10, 380, 480, Biome.FROZEN_WASTES, "Assembled from every barbell plate ever left on the floor.",
            trait = MonsterTrait.ARMORED,
            bossOf = Biome.FROZEN_WASTES,
            telegraphedMove = TelegraphedMove("Colossus Quake", "🤖", "Slams 1,000kg iron fists into the glacial earth! DEFEND or STUN!", damageMultiplier = 2.5)
        ),

        // Shadowfen (lv 18-20)
        Monster(17, "Bog Lurker", "🐊", 18, 4, 1380, 132, 40, 15, 400, 480, Biome.SHADOWFEN, "It waits in the murk for anyone who skips leg day. Its bite festers.", MonsterTrait.VENOMOUS),
        Monster(29, "Doom Toad", "🐸", 19, 4, 1420, 136, 38, 14, 420, 505, Biome.SHADOWFEN, "Each croak is a prophecy of failure. Its tongue drips despair.", MonsterTrait.VENOMOUS),
        Monster(18, "Despair Shade", "🫥", 19, 4, 1480, 140, 40, 20, 440, 520, Biome.SHADOWFEN, "The embodiment of 'what's the point?'. Show it the point.", MonsterTrait.ENRAGED),
        Monster(30, "Sleep Paralysis Fiend", "😈", 20, 4, 1520, 146, 40, 28, 465, 545, Biome.SHADOWFEN, "It sits on your chest at 3 AM and dares you to make the morning workout.", MonsterTrait.SWIFT),
        Monster(41, "DOMS Behemoth", "🦧", 20, 4, 1700, 142, 48, 11, 480, 560, Biome.SHADOWFEN, "Delayed Onset Muscle Soreness made flesh. Heavy, bone-cracking strikes.", MonsterTrait.ARMORED),
        Monster(42, "Void Leech Queen", "🪱", 20, 4, 1560, 148, 36, 18, 490, 570, Biome.SHADOWFEN, "Drains momentum and will. Recovers rapidly from damage.", MonsterTrait.REGENERATING),
        Monster(8, "Burnout Dragon", "🐲", 20, 4, 2250, 165, 50, 18, 620, 750, Biome.SHADOWFEN, "The final boss of every fitness journey. Its fury grows as it weakens.",
            trait = MonsterTrait.ENRAGED,
            bossOf = Biome.SHADOWFEN,
            telegraphedMove = TelegraphedMove("Cataclysmic Burnout Breath", "🐲", "Inhales apocalyptic hellfire to annihilate everything! DEFEND or STUN!", damageMultiplier = 2.8, armorPiercing = true)
        ),

        // Ambush elites (harder than biome peers; wagered rest encounters)
        Monster(101, "Restless Specter", "👻", 5, 2, 420, 52, 18, 18, 120, 175, Biome.MEADOWLANDS, "It attacks between sets. Your rest is its feast.", MonsterTrait.SWIFT),
        Monster(102, "Plate Pirate", "🧟", 8, 3, 620, 72, 28, 12, 165, 250, Biome.DARKWOOD, "Steals your progress plates mid-rest.", MonsterTrait.ARMORED),
        Monster(103, "Interval Demon", "🔥", 12, 3, 880, 98, 26, 24, 230, 350, Biome.EMBER_PEAKS, "Wants you to skip the rest and burn out.", MonsterTrait.ENRAGED),
        Monster(104, "Cooldown Debt", "❄️", 16, 4, 1200, 120, 36, 16, 330, 480, Biome.FROZEN_WASTES, "Compound interest on every missed rep.", MonsterTrait.REGENERATING),
    )

    fun rollVariant(monster: Monster, rng: kotlin.random.Random = kotlin.random.Random.Default): Monster {
        if (monster.isBoss || monster.id >= 100) return monster
        val roll = rng.nextInt(100)
        return when {
            roll < 5 -> {
                val extraTrait = MonsterTrait.entries.filter { it != monster.trait }.randomOrNull(rng)
                monster.withVariant(MonsterVariant.EPIC, extraTrait)
            }
            roll < 25 -> {
                val extraTrait = MonsterTrait.entries.filter { it != monster.trait }.randomOrNull(rng)
                monster.withVariant(MonsterVariant.ELITE, extraTrait)
            }
            else -> monster
        }
    }

    fun ambushForLevel(level: Int): Monster {
        val pool = listOf(101, 102, 103, 104).mapNotNull { byId(it) }
        return pool.minByOrNull { kotlin.math.abs(it.level - (level + 3)) } ?: pool.first()
    }

    fun byId(id: Int): Monster? = all.find { it.id == id }

    fun byBiome(biome: Biome): List<Monster> =
        all.filter { it.biome == biome && it.id < 100 }

    fun bossForBiome(biome: Biome): Monster =
        all.first { it.bossOf == biome }

    fun isBoss(monster: Monster): Boolean = monster.isBoss

    fun regularMonstersByBiome(biome: Biome): List<Monster> =
        all.filter { it.biome == biome && it.id < 100 && !it.isBoss }
}
