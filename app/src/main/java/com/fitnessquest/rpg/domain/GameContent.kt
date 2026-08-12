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
        // ---- Warrior weapons ----
        ItemEntity(1, "Rusty Sword", "\uD83D\uDDE1\uFE0F", ItemSlot.WEAPON, 1, 40, atk = 3, description = "It's seen better days, but so have you.", classAffinity = W, style = ItemStyle.SWORD),
        ItemEntity(2, "Iron Mace", "\uD83D\uDD28", ItemSlot.WEAPON, 2, 120, atk = 7, description = "Heavy. Like leg day.", classAffinity = W, style = ItemStyle.MACE),
        ItemEntity(3, "Knight's Blade", "\u2694\uFE0F", ItemSlot.WEAPON, 3, 300, atk = 13, description = "Forged for those who never skip a session.", classAffinity = W, style = ItemStyle.SWORD),
        ItemEntity(4, "Dragonfang Greatsword", "\uD83D\uDC09", ItemSlot.WEAPON, 4, 700, atk = 22, description = "Carved from the fang of a dragon who skipped cardio.", classAffinity = W, style = ItemStyle.GREATSWORD),

        // ---- Mage weapons ----
        ItemEntity(13, "Apprentice Wand", "\uD83E\uDE84", ItemSlot.WEAPON, 1, 40, atk = 3, description = "A twig with delusions of grandeur.", classAffinity = M, style = ItemStyle.WAND),
        ItemEntity(14, "Oakheart Staff", "\uD83C\uDF3F", ItemSlot.WEAPON, 2, 120, atk = 7, description = "Still growing leaves. It refuses to stop.", classAffinity = M, style = ItemStyle.STAFF),
        ItemEntity(15, "Staff of Storms", "\u26C8\uFE0F", ItemSlot.WEAPON, 3, 300, atk = 13, description = "Crackles when you hit a new personal record.", classAffinity = M, style = ItemStyle.STAFF),
        ItemEntity(16, "Archmage's Scepter", "\uD83D\uDC51", ItemSlot.WEAPON, 4, 700, atk = 22, description = "Contains the condensed willpower of a thousand morning workouts.", classAffinity = M, style = ItemStyle.STAFF),

        // ---- Thief weapons ----
        ItemEntity(17, "Bent Shiv", "\uD83D\uDD2A", ItemSlot.WEAPON, 1, 40, atk = 3, description = "Technically a weapon. Technically.", classAffinity = T, style = ItemStyle.DAGGER),
        ItemEntity(18, "Twin Fangs", "\uD83D\uDC0D", ItemSlot.WEAPON, 2, 120, atk = 7, description = "Two daggers, because one hand should never rest.", classAffinity = T, style = ItemStyle.DAGGER),
        ItemEntity(19, "Nightpiercer", "\uD83C\uDF11", ItemSlot.WEAPON, 3, 300, atk = 13, description = "Strikes between heartbeats. Ideally between sets.", classAffinity = T, style = ItemStyle.DAGGER),
        ItemEntity(20, "Whisper of the Void", "\uD83D\uDD73\uFE0F", ItemSlot.WEAPON, 4, 700, atk = 22, description = "So sharp it cuts excuses before they're spoken.", classAffinity = T, style = ItemStyle.DAGGER),

        // ---- Ranger weapons ----
        ItemEntity(21, "Training Shortbow", "\uD83C\uDFF9", ItemSlot.WEAPON, 1, 40, atk = 3, description = "The string doubles as a resistance band.", classAffinity = R, style = ItemStyle.BOW),
        ItemEntity(22, "Hunter's Recurve", "\uD83C\uDFAF", ItemSlot.WEAPON, 2, 120, atk = 7, description = "Draw weight: your bodyweight. Keep training.", classAffinity = R, style = ItemStyle.BOW),
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

        // ---- Universal weapons ----
        ItemEntity(1091, "Training Spear", "\uD83D\uDD31", ItemSlot.WEAPON, 1, 35, atk = 2, def = 1, description = "Simple reach, steady balance, no class requirement.", style = ItemStyle.SWORD),
        ItemEntity(1092, "Balanced Shortsword", "\uD83D\uDDE1\uFE0F", ItemSlot.WEAPON, 2, 110, atk = 6, def = 1, description = "A dependable blade for any hero still finding their path.", style = ItemStyle.SWORD),
        ItemEntity(1093, "Mercenary Halberd", "\u2694\uFE0F", ItemSlot.WEAPON, 3, 280, atk = 11, def = 2, description = "Practical, durable, and happy to work for whoever trains hardest.", style = ItemStyle.GREATSWORD),
        ItemEntity(1094, "Starforged Relic Blade", "\u2728", ItemSlot.WEAPON, 4, 650, atk = 18, def = 4, hp = 12, description = "A universal legend with room for any class story.", style = ItemStyle.GREATSWORD),

        // ---- Universal starter armor ----
        ItemEntity(5, "Padded Vest", "\uD83E\uDDBA", ItemSlot.CHEST, 1, 40, def = 3, description = "Better than nothing. Barely.", style = ItemStyle.LIGHT),

        // ---- Warrior armor ----
        ItemEntity(6, "Chainmail Shirt", "\uD83D\uDD17", ItemSlot.CHEST, 2, 120, def = 7, hp = 10, description = "Surprisingly breathable.", classAffinity = W, style = ItemStyle.PLATE),
        ItemEntity(7, "Steel Plate", "\uD83D\uDEE1\uFE0F", ItemSlot.CHEST, 3, 300, def = 13, hp = 20, description = "The extra weight counts as resistance training.", classAffinity = W, style = ItemStyle.PLATE),
        ItemEntity(8, "Aegis of the Titan", "\uD83C\uDFDB\uFE0F", ItemSlot.CHEST, 4, 700, def = 20, hp = 40, description = "Worn by legends who deadlifted mountains.", classAffinity = W, style = ItemStyle.PLATE),

        // ---- Mage armor ----
        ItemEntity(25, "Apprentice Robes", "\uD83E\uDDE5", ItemSlot.CHEST, 2, 120, def = 5, hp = 15, description = "Machine washable. Mana resistant.", classAffinity = M, style = ItemStyle.ROBE),
        ItemEntity(26, "Runeweave Robe", "\u2728", ItemSlot.CHEST, 3, 300, def = 10, hp = 30, description = "The runes glow brighter after cardio.", classAffinity = M, style = ItemStyle.ROBE),
        ItemEntity(27, "Vestments of the Cosmos", "\uD83C\uDF0C", ItemSlot.CHEST, 4, 700, def = 16, hp = 52, description = "Woven from night sky and discipline.", classAffinity = M, style = ItemStyle.ROBE),

        // ---- Thief armor ----
        ItemEntity(28, "Supple Leathers", "\uD83E\uDD8E", ItemSlot.CHEST, 2, 120, def = 6, hp = 8, atk = 2, description = "Squeak-proof. Guaranteed.", classAffinity = T, style = ItemStyle.LIGHT),
        ItemEntity(29, "Shadowmesh", "\uD83C\uDF2B\uFE0F", ItemSlot.CHEST, 3, 300, def = 11, hp = 16, atk = 4, description = "Woven from shadows and skipped rest timers.", classAffinity = T, style = ItemStyle.LIGHT),
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

        // ---- Universal trinkets ----
        ItemEntity(9, "Lucky Sweatband", "\uD83C\uDFC5", ItemSlot.TRINKET, 1, 60, atk = 1, def = 1, hp = 5, description = "Absorbs sweat and bad vibes."),
        ItemEntity(10, "Amulet of Endurance", "\uD83D\uDCFF", ItemSlot.TRINKET, 2, 180, hp = 25, description = "One more rep. Always one more rep."),
        ItemEntity(11, "Ring of Fury", "\uD83D\uDC8D", ItemSlot.TRINKET, 3, 400, atk = 8, description = "Channels pre-workout energy into raw power."),
        ItemEntity(12, "Heart of the Colossus", "\u2764\uFE0F\u200D\uD83D\uDD25", ItemSlot.TRINKET, 4, 900, atk = 6, def = 6, hp = 30, description = "Beats once per rep. Never stops."),

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

        // ---- Universal starter armor pieces ----
        ItemEntity(50, "Leather Cap", "\uD83E\uDDE2", ItemSlot.HEAD, 1, 30, def = 2, hp = 3, description = "Keeps the sun out of your eyes during outdoor sets.", style = ItemStyle.LIGHT),
        ItemEntity(51, "Cloth Gloves", "\uD83E\uDDE4", ItemSlot.HANDS, 1, 25, def = 1, hp = 2, description = "Callus prevention, medieval edition.", style = ItemStyle.LIGHT),
        ItemEntity(52, "Worn Trousers", "\uD83D\uDC56", ItemSlot.LEGS, 1, 25, def = 2, hp = 2, description = "They've seen a thousand squats. They'll see a thousand more.", style = ItemStyle.LIGHT),
        ItemEntity(53, "Old Boots", "\uD83E\uDD7E", ItemSlot.FEET, 1, 25, def = 2, hp = 2, description = "Broken in. Like your excuses.", style = ItemStyle.LIGHT),

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

        // ---- Consumable supplies ----
        ItemEntity(Consumables.STREAK_FREEZE, "Streak Freeze", "\u2744\uFE0F", ItemSlot.CONSUMABLE, 1, 250,
            description = "Protects your workout streak for one missed day. Used automatically when you need it."),
        ItemEntity(Consumables.XP_ELIXIR_MINOR, "Minor XP Elixir", "\uD83E\uDDEA", ItemSlot.CONSUMABLE, 1, 120,
            description = "Drink before training: your next workout earns +${Consumables.XP_MINOR_BONUS} bonus XP."),
        ItemEntity(Consumables.XP_ELIXIR_MAJOR, "Major XP Elixir", "\u2697\uFE0F", ItemSlot.CONSUMABLE, 2, 400,
            description = "Drink before training: your next workout earns +${Consumables.XP_MAJOR_BONUS} bonus XP."),
        ItemEntity(Consumables.POTION_OF_REBIRTH, "Potion of Rebirth", "\uD83D\uDD2E", ItemSlot.CONSUMABLE, 3, 600,
            description = "Resets STR, END, AGI, and WIL to 5 and refunds every earned point to spend however you like."),

        // ---- Mid-tier gear fills ----
        ItemEntity(60, "Training Blade", "\uD83D\uDDE1\uFE0F", ItemSlot.WEAPON, 2, 90, atk = 5,
            description = "A reliable sidearm between rusty and legendary.", classAffinity = W, style = ItemStyle.SWORD),
        ItemEntity(61, "Crystal Focus", "\uD83D\uDD2E", ItemSlot.WEAPON, 2, 90, atk = 5,
            description = "Channels willpower through a cloudy gem.", classAffinity = M, style = ItemStyle.WAND),
        ItemEntity(62, "Stiletto", "\uD83D\uDDE1\uFE0F", ItemSlot.WEAPON, 2, 90, atk = 5,
            description = "Thin, quiet, and judgmental about your form.", classAffinity = T, style = ItemStyle.DAGGER),
        ItemEntity(63, "Trail Bow", "\uD83C\uDFF9", ItemSlot.WEAPON, 2, 90, atk = 5,
            description = "Perfect for warming up before the real hunt.", classAffinity = R, style = ItemStyle.BOW),
        ItemEntity(70, "Squire's Arming Sword", "\u2694\uFE0F", ItemSlot.WEAPON, 2, 90, atk = 5,
            description = "Holy enough for practice, sharp enough for pride.", classAffinity = P, style = ItemStyle.SWORD),
        ItemEntity(71, "Cinder Staff", "\uD83D\uDD25", ItemSlot.WEAPON, 2, 90, atk = 5,
            description = "A warm bone shaft that thrives on stubborn willpower.", classAffinity = N, style = ItemStyle.STAFF),
        ItemEntity(64, "Bronze Guard", "\uD83D\uDEE1\uFE0F", ItemSlot.CHEST, 2, 100, def = 5, hp = 8,
            description = "A stopgap between padded cloth and real steel.", style = ItemStyle.PLATE),
        ItemEntity(65, "Traveler's Cloak", "\uD83E\uDDE5", ItemSlot.CHEST, 3, 220, def = 8, hp = 14,
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
enum class MonsterTrait(val label: String, val emoji: String, val blurb: String) {
    ARMORED("Armored", "\uD83D\uDEE1\uFE0F", "Takes 30% less attack damage. Defense-piercing skills cut through."),
    SWIFT("Swift", "\uD83D\uDCA8", "Hard to hit and may strike twice. Speed keeps you out of reach."),
    VENOMOUS("Venomous", "\u2620\uFE0F", "Bites can poison you. Willpower shakes poison off faster."),
    REGENERATING("Regenerating", "\uD83D\uDC9A", "Recovers HP every turn. Finish it fast."),
    ENRAGED("Enraged", "\uD83D\uDE21", "Hits 40% harder once wounded. End the fight before it ends you.")
}

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
    val trait: MonsterTrait? = null
)

object MonsterCatalog {
    val all: List<Monster> = listOf(
        // Meadowlands (lv 1-3) — fair fights should last ~4-6 rounds for a same-level hero
        Monster(1, "Couch Slime", "\uD83E\uDD22", 1, 1, 100, 14, 4, 4, 18, 30, Biome.MEADOWLANDS, "A gelatinous blob that hasn't moved in weeks. Relatable, but it must be stopped."),
        Monster(2, "Gym Rat", "\uD83D\uDC00", 2, 1, 130, 18, 5, 8, 28, 48, Biome.MEADOWLANDS, "It hoards all the dumbbells and never re-racks them."),
        Monster(19, "Snooze Gremlin", "\u23F0", 2, 1, 120, 17, 4, 14, 28, 46, Biome.MEADOWLANDS, "It hits your snooze button from inside the walls. Quick little menace.", MonsterTrait.SWIFT),
        Monster(9, "Lazy Boar", "\uD83D\uDC17", 3, 1, 165, 22, 7, 6, 40, 62, Biome.MEADOWLANDS, "It naps in the sun and judges joggers. Today, it judges you."),
        Monster(20, "Junk Food Mimic", "\uD83C\uDF54", 3, 1, 150, 21, 6, 5, 38, 60, Biome.MEADOWLANDS, "Looks delicious. Bites back. Poisons your macros.", MonsterTrait.VENOMOUS),

        // Darkwood (lv 4-6)
        Monster(3, "Procrastination Imp", "\uD83D\uDC7F", 4, 2, 200, 26, 9, 11, 55, 85, Biome.DARKWOOD, "Whispers 'you can work out tomorrow' into your ear."),
        Monster(21, "Excuse Goblin", "\uD83D\uDC7A", 5, 2, 220, 28, 8, 17, 62, 95, Biome.DARKWOOD, "Darts between trees flinging excuses. Hard to pin down.", MonsterTrait.SWIFT),
        Monster(10, "Doomscroll Spider", "\uD83D\uDD77\uFE0F", 5, 2, 240, 30, 10, 13, 68, 100, Biome.DARKWOOD, "Its web is infinite. Its venom keeps you scrolling.", MonsterTrait.VENOMOUS),
        Monster(4, "Skeleton Spotter", "\uD83D\uDC80", 6, 2, 280, 34, 12, 10, 82, 120, Biome.DARKWOOD, "It has literally no muscles. It is so jealous of yours."),
        Monster(22, "Couch Potato Golem", "\uD83E\uDD54", 6, 2, 320, 30, 18, 4, 88, 125, Biome.DARKWOOD, "Centuries of sitting have compressed it into solid starch armor.", MonsterTrait.ARMORED),

        // Crystal Caves (lv 7-9)
        Monster(11, "Crystal Crab", "\uD83E\uDD80", 7, 2, 330, 36, 17, 6, 100, 140, Biome.CRYSTAL_CAVES, "Its shell is pure gem. Attacks glance right off it.", MonsterTrait.ARMORED),
        Monster(12, "Echo Bat", "\uD83E\uDD87", 8, 3, 320, 40, 11, 18, 112, 155, Biome.CRYSTAL_CAVES, "It repeats every excuse you've ever made, in your own voice.", MonsterTrait.SWIFT),
        Monster(23, "Gem Serpent", "\uD83D\uDC0D", 8, 3, 340, 39, 14, 13, 115, 158, Biome.CRYSTAL_CAVES, "Its fangs drip crystallized venom worth a fortune. Don't get bitten.", MonsterTrait.VENOMOUS),
        Monster(5, "Ogre of Excuses", "\uD83D\uDC79", 9, 3, 420, 46, 15, 7, 130, 180, Biome.CRYSTAL_CAVES, "Its club is carved from a thousand broken New Year's resolutions."),
        Monster(24, "Prism Wisp", "\uD83D\uDCA0", 9, 3, 350, 48, 10, 22, 132, 185, Biome.CRYSTAL_CAVES, "A shard of living light that refuses to hold still.", MonsterTrait.SWIFT),

        // Ember Peaks (lv 11-13)
        Monster(13, "Magma Hound", "\uD83D\uDD25", 11, 3, 500, 55, 16, 14, 168, 230, Biome.EMBER_PEAKS, "It fetches boulders. The angrier it gets, the hotter it burns.", MonsterTrait.ENRAGED),
        Monster(25, "Ash Ghoul", "\uD83E\uDDDF", 12, 3, 540, 54, 15, 11, 185, 245, Biome.EMBER_PEAKS, "Rises from every burned-out training plan. Keeps getting back up.", MonsterTrait.REGENERATING),
        Monster(6, "Cardio Wraith", "\uD83D\uDC7B", 12, 3, 560, 58, 17, 20, 188, 250, Biome.EMBER_PEAKS, "It runs eternally, fueled by the souls of abandoned treadmills.", MonsterTrait.SWIFT),
        Monster(14, "Cinder Imp", "\uD83E\uDDE8", 13, 3, 590, 62, 18, 16, 210, 275, Biome.EMBER_PEAKS, "It lights the sauna way too hot on purpose."),
        Monster(26, "Lava Turtle", "\uD83D\uDC22", 13, 3, 680, 56, 28, 4, 215, 280, Biome.EMBER_PEAKS, "Its obsidian shell shrugs off almost anything. Slow and smug about it.", MonsterTrait.ARMORED),

        // Frozen Wastes (lv 15-17)
        Monster(15, "Frost Yeti", "\u26C4", 15, 4, 720, 70, 22, 10, 260, 335, Biome.FROZEN_WASTES, "Takes ice baths recreationally. The cold knits its wounds shut.", MonsterTrait.REGENERATING),
        Monster(27, "Frostbite Wolf", "\uD83D\uDC3A", 16, 4, 740, 74, 20, 24, 280, 360, Biome.FROZEN_WASTES, "It circles twice before you can blink. Pack tactics, solo executed.", MonsterTrait.SWIFT),
        Monster(7, "Iron Golem", "\uD83E\uDD16", 16, 4, 800, 72, 28, 8, 275, 355, Biome.FROZEN_WASTES, "Assembled from every barbell plate ever left on the floor.", MonsterTrait.ARMORED),
        Monster(16, "Blizzard Elemental", "\uD83C\uDF28\uFE0F", 17, 4, 840, 80, 26, 19, 320, 400, Biome.FROZEN_WASTES, "A snowstorm with a grudge and surprisingly good footwork."),
        Monster(28, "Permafrost Lich", "\uD83E\uDDD9", 17, 4, 820, 82, 25, 13, 325, 410, Biome.FROZEN_WASTES, "An ancient coach who never let anyone skip warm-ups. Death didn't stop him.", MonsterTrait.REGENERATING),

        // Shadowfen (lv 18-20)
        Monster(17, "Bog Lurker", "\uD83D\uDC0A", 18, 4, 920, 86, 28, 12, 375, 450, Biome.SHADOWFEN, "It waits in the murk for anyone who skips leg day. Its bite festers.", MonsterTrait.VENOMOUS),
        Monster(29, "Doom Toad", "\uD83D\uDC38", 19, 4, 960, 88, 27, 11, 395, 475, Biome.SHADOWFEN, "Each croak is a prophecy of failure. Its tongue drips despair.", MonsterTrait.VENOMOUS),
        Monster(18, "Despair Shade", "\uD83E\uDEE5", 19, 4, 980, 90, 29, 17, 410, 485, Biome.SHADOWFEN, "The embodiment of 'what's the point?'. Show it the point."),
        Monster(30, "Sleep Paralysis Fiend", "\uD83D\uDE08", 20, 4, 1000, 95, 28, 25, 435, 510, Biome.SHADOWFEN, "It sits on your chest at 3 AM and dares you to make the morning workout.", MonsterTrait.SWIFT),
        Monster(8, "Burnout Dragon", "\uD83D\uDC32", 20, 4, 1100, 100, 32, 15, 480, 550, Biome.SHADOWFEN, "The final boss of every fitness journey. Its fury grows as it weakens.", MonsterTrait.ENRAGED),

        // Ambush elites (harder than biome peers; wagered rest encounters)
        Monster(101, "Restless Specter", "\uD83D\uDC7B", 5, 2, 280, 38, 12, 16, 90, 140, Biome.MEADOWLANDS, "It attacks between sets. Your rest is its feast.", MonsterTrait.SWIFT),
        Monster(102, "Plate Pirate", "\uD83E\uDDDF", 8, 3, 420, 52, 20, 10, 130, 200, Biome.DARKWOOD, "Steals your progress plates mid-rest.", MonsterTrait.ARMORED),
        Monster(103, "Interval Demon", "\uD83D\uDD25", 12, 3, 600, 68, 18, 22, 180, 280, Biome.EMBER_PEAKS, "Wants you to skip the rest and burn out.", MonsterTrait.ENRAGED),
        Monster(104, "Cooldown Debt", "\u2744\uFE0F", 16, 4, 820, 82, 26, 14, 260, 380, Biome.FROZEN_WASTES, "Compound interest on every missed rep.", MonsterTrait.REGENERATING)
    )

    fun ambushForLevel(level: Int): Monster {
        val pool = listOf(101, 102, 103, 104).mapNotNull { byId(it) }
        return pool.minByOrNull { kotlin.math.abs(it.level - (level + 3)) } ?: pool.first()
    }

    fun byId(id: Int): Monster? = all.find { it.id == id }

    fun byBiome(biome: Biome): List<Monster> =
        all.filter { it.biome == biome && it.id < 100 }
}
