package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.ArmorSlots
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.SetLogEntity
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class StatGains(
    val strength: Int = 0,
    val endurance: Int = 0,
    val agility: Int = 0,
    val willpower: Int = 0,
) {
    val any: Boolean get() = (strength + endurance + agility + willpower) > 0
}

data class MasteryStatBonus(
    val flatAtk: Int = 0,
    val flatDef: Int = 0,
    val flatMaxHp: Int = 0,
    val flatSpd: Int = 0,
    val flatCritPercent: Int = 0,
    val mitigationPercent: Float = 0f,
    val siphonBonusPercent: Float = 0f,
    val xpMultiplierBonus: Float = 0f,
    val flatMaxEnergy: Int = 0
) {
    companion object { val NONE = MasteryStatBonus() }
}

data class MasteryPerkUnlock(
    val canonicalKey: String,
    val perkName: String,
    val perkDescription: String
)

data class SessionResult(
    val xp: Int,
    val gold: Int,
    val energy: Int,
    val levelsGained: Int,
    val statGains: StatGains,
    val updatedCharacter: CharacterEntity,
    /** Total weight lifted (kg) across all strength sets. */
    val volumeKg: Double = 0.0,
    /** Total active duration in milliseconds. */
    val durationMs: Long = 0,
    /** Unique muscle groups trained this session (e.g. "chest", "quads"). */
    val musclesWorked: Set<String> = emptySet(),
    /** Workouts completed so far this ISO week. */
    val weeklyWorkoutsDone: Int = 1,
    /** Weekly workout goal from player's profile. */
    val weeklyWorkoutsGoal: Int = 3,
    /** Cardio kilometers that counted toward biome travel this session. */
    val travelKm: Double = 0.0,
    /** Label of the biome reached this session, if travel completed. */
    val arrivedAt: String? = null,
    /** Day streak after this session. */
    val streak: Int = 0,
    /** True when Streak Freezes were consumed to keep the streak alive. */
    val streakSaved: Boolean = false,
    /** Bonus XP from a consumed elixir that was included in [xp]. */
    val xpBoostApplied: Int = 0,
    /** Personal records set this session. */
    val prs: List<SessionPr> = emptyList(),
    /** Unified rewards for the reveal flow. */
    val rewardBatch: RewardBatch? = null,
    /** End-of-workout loot grants (labels for UI). */
    val lootLabels: List<String> = emptyList(),
    /** Unlocked Movement Mastery perks earned in this session. */
    val masteryPerkUnlocks: List<MasteryPerkUnlock> = emptyList()
)

/** A personal record set during a session. */
data class SessionPr(
    val exerciseName: String,
    val kind: PrKind,
    val value: Double, // Weight in kg or Volume in kg
    val reps: Int = 0, // Relevant for weight PRs
    val isNew: Boolean = true
)

enum class PrKind { 
    WEIGHT, VOLUME, ONE_RM, 
    DISTANCE, PACE, TIME, REPS, 
    SPEED, INCLINE 
}

/** Recommended weight/reps for the next set, from effort autoregulation. */
data class SetSuggestion(
    val weightKg: Double,
    val reps: Int,
    val note: String
)

data class CombatStats(
    val maxHp: Int,
    val atk: Int,
    val def: Int,
    val spd: Int,
    val critPercent: Int,
    /** Heal on victory when a Siphon rune is socketed. */
    val siphonHeal: Int = 0,
    /** Percentage physical damage mitigation bonus (e.g. 0.05 = 5%). */
    val mitigationPercent: Float = 0f
)

object GameMath {

    fun calculate1RM(weight: Double, reps: Int): Double =
        if (reps <= 0) 0.0 else weight / (1.0278 - (0.0278 * reps))

    const val BATTLE_ENERGY_COST = 10
    const val MAX_ENERGY = 100

    /** Milliseconds of rest needed to recoup 1 unit of energy. (12 minutes per point = 5 per hour). */
    const val ENERGY_TICK_MILLIS = 12 * 60 * 1000L

    /** Steps needed to advance biome travel by one kilometer. */
    const val STEPS_PER_KM = 1300

    /** Bonus XP awarded for each personal record set in a session. */
    const val PR_BONUS_XP = 25

    /**
     * Autoregulation: suggests the next set from the effort of the last one.
     * Effort is reps-in-reserve (RIR); the 1-3 range is the productive sweet
     * spot, so easier sets push weight (or reps, for bodyweight) up and an
     * all-out set backs off.
     */
    fun suggestNextSet(weightKg: Double, reps: Int, rir: Int): SetSuggestion {
        val weighted = weightKg > 0
        return when {
            rir >= 4 -> if (weighted) {
                SetSuggestion(weightKg * 1.075, reps, "Plenty left in the tank \u2014 add weight")
            } else {
                SetSuggestion(0.0, reps + 3, "Plenty left in the tank \u2014 add reps")
            }
            rir == 3 -> if (weighted) {
                SetSuggestion(weightKg * 1.05, reps, "Felt easy \u2014 nudge the weight up")
            } else {
                SetSuggestion(0.0, reps + 2, "Felt easy \u2014 add a couple reps")
            }
            rir >= 1 -> SetSuggestion(weightKg, reps, "Right in the zone \u2014 keep it here")
            else -> if (weighted) {
                SetSuggestion(weightKg * 0.95, reps, "That was your limit \u2014 back off a touch")
            } else {
                SetSuggestion(0.0, (reps - 2).coerceAtLeast(1), "That was your limit \u2014 drop a couple reps")
            }
        }
    }

    fun xpToNextLevel(level: Int): Int = (100.0 * level.toDouble().pow(1.3)).roundToInt()

    /** XP awarded for a single logged set. Volume-based: heavier and longer earn more. */
    fun xpForSet(log: SetLogEntity): Int {
        val base = when (log.category) {
            ExerciseCategory.STRENGTH -> (log.weightKg * log.reps / 10.0).roundToInt()
            ExerciseCategory.CARDIO -> {
                val b = log.durationMin * 2 + log.distanceKm * 8
                // Mild intensity bonus from machine settings (optional fields).
                val speedBonus = (log.speedKmh / 12.0).coerceIn(0.0, 1.5) * log.durationMin
                val inclineBonus = (log.inclinePercent / 10.0).coerceIn(0.0, 1.5) * log.durationMin
                (b + speedBonus + inclineBonus).roundToInt()
            }
            ExerciseCategory.BODYWEIGHT -> {
                if (log.reps <= 0 && log.durationMin > 0) {
                    (log.durationMin * 2).roundToInt()
                } else {
                    val loadFactor = if (log.weightKg > 0) 1.0 + (log.weightKg / 200.0).coerceIn(0.0, 0.5) else 1.0
                    (log.reps * 0.8 * loadFactor).roundToInt()
                }
            }
            ExerciseCategory.FLEXIBILITY -> (log.durationMin * 2).roundToInt()
        }
        return max(1, (base * log.setType.xpMultiplier).roundToInt())
    }


    /** XP needed within a stat's category before that stat ticks up by one. */
    fun statThreshold(currentStat: Int): Int = 40 + currentStat * 12

    /** Seconds past the rest window a set may land and still keep the heat streak. */
    const val HEAT_GRACE_SEC = 90

    /** Calculates XP Heat Multiplier based on focused sets completed within rest intervals. */
    fun calculateHeatMultiplier(consecutiveFocusedSets: Int): Float = when {
        consecutiveFocusedSets >= 6 -> 1.25f
        consecutiveFocusedSets >= 3 -> 1.10f
        else -> 1.0f
    }

    fun heatLabel(consecutiveFocusedSets: Int): String = when {
        consecutiveFocusedSets >= 6 -> "🔥🔥 ON FIRE! (+25% XP)"
        consecutiveFocusedSets >= 3 -> "🔥 WARMING UP (+10% XP)"
        else -> "Workout Momentum"
    }

    /** Returns true if 24-60 hours have elapsed since last workout (healthy recovery rest day). */
    fun isWellRested(lastWorkoutEpochDay: Long): Boolean {
        if (lastWorkoutEpochDay == 0L) return false
        val daysElapsed = LocalDate.now().toEpochDay() - lastWorkoutEpochDay
        return daysElapsed in 1..2
    }

    /**
     * Applies a finished workout to the character: XP, gold, energy, per-stat progress,
     * stat increases, and level-ups.
     */
    fun applySession(
        character: CharacterEntity,
        logs: List<SetLogEntity>,
        durationMs: Long,
        musclesWorked: Set<String>,
        weeklyWorkoutsDone: Int,
        weeklyWorkoutsGoal: Int,
        bonusXp: Int = 0,
        isWellRested: Boolean = false,
        maxEnergy: Int = MAX_ENERGY,
        masteryBonus: MasteryStatBonus = MasteryStatBonus.NONE
    ): SessionResult {
        var totalXp = logs.sumOf { it.xp } + bonusXp
        if (masteryBonus.xpMultiplierBonus > 0f) {
            totalXp = (totalXp * (1f + masteryBonus.xpMultiplierBonus)).toInt()
        }
        val volumeKg = logs.asSequence().filter { it.category == ExerciseCategory.STRENGTH }.sumOf { it.weightKg * it.reps }
        
        var gold = max(1, totalXp / 2)
        
        if (isWellRested) {
            totalXp = (totalXp * 1.15f).toInt()
            gold = (gold * 1.15f).toInt()
        }
        
        val energyGain = min(20 + totalXp / 25, 60)
        val now = System.currentTimeMillis()

        var str = character.strength
        var end = character.endurance
        var agi = character.agility
        var wil = character.willpower
        var strP = character.strProgress + logs.asSequence().filter { it.category == ExerciseCategory.STRENGTH }.sumOf { it.xp }
        var endP = character.endProgress + logs.asSequence().filter { it.category == ExerciseCategory.CARDIO }.sumOf { it.xp }
        var agiP = character.agiProgress + logs.asSequence().filter { it.category == ExerciseCategory.BODYWEIGHT }.sumOf { it.xp }
        var wilP = character.wilProgress + logs.asSequence().filter { it.category == ExerciseCategory.FLEXIBILITY }.sumOf { it.xp }

        var strGain = 0
        var endGain = 0
        var agiGain = 0
        var wilGain = 0
        while (strP >= statThreshold(str)) { strP -= statThreshold(str); str++; strGain++ }
        while (endP >= statThreshold(end)) { endP -= statThreshold(end); end++; endGain++ }
        while (agiP >= statThreshold(agi)) { agiP -= statThreshold(agi); agi++; agiGain++ }
        while (wilP >= statThreshold(wil)) { wilP -= statThreshold(wil); wil++; wilGain++ }

        var level = character.level
        var xp = character.xp + totalXp
        var levelsGained = 0
        while (xp >= xpToNextLevel(level)) {
            xp -= xpToNextLevel(level)
            level++
            levelsGained++
        }

        val updated = character.copy(
            level = level,
            xp = xp,
            gold = character.gold + gold,
            energy = min(character.energy + energyGain, maxEnergy),
            lastEnergyUpdate = now,
            strength = str, endurance = end, agility = agi, willpower = wil,
            strProgress = strP, endProgress = endP, agiProgress = agiP, wilProgress = wilP,
            sessionsCompleted = character.sessionsCompleted + 1
        )

        return SessionResult(
            xp = totalXp,
            gold = gold,
            energy = energyGain,
            levelsGained = levelsGained,
            statGains = StatGains(strGain, endGain, agiGain, wilGain),
            updatedCharacter = updated,
            volumeKg = volumeKg,
            durationMs = durationMs,
            musclesWorked = musclesWorked,
            weeklyWorkoutsDone = weeklyWorkoutsDone,
            weeklyWorkoutsGoal = weeklyWorkoutsGoal,
            xpBoostApplied = bonusXp
        )
    }

    /** Applies battle rewards (and XP-driven level ups) after a victory. */
    fun applyLevelUps(character: CharacterEntity): CharacterEntity {
        var level = character.level
        var xp = character.xp
        while (xp >= xpToNextLevel(level)) {
            xp -= xpToNextLevel(level)
            level++
        }
        return character.copy(level = level, xp = xp)
    }

    fun applyBattleRewards(character: CharacterEntity, monster: Monster): CharacterEntity {
        val now = System.currentTimeMillis()
        val next = character.copy(
            xp = character.xp + monster.xpReward,
            gold = character.gold + monster.goldReward,
            energy = max(0, character.energy - BATTLE_ENERGY_COST),
            lastEnergyUpdate = now,
            battlesWon = character.battlesWon + 1
        )
        return applyLevelUps(next)
    }

    /** Number of equipped armor pieces that match the hero's class (max 5). */
    fun setPieceCount(character: CharacterEntity, equipped: List<ItemEntity>): Int {
        val cls = character.characterClass ?: return 0
        return equipped.count { it.slot in ArmorSlots && it.classAffinity == cls }
    }

    fun hasFullSetBonus(character: CharacterEntity, equipped: List<ItemEntity>): Boolean =
        setPieceCount(character, equipped) >= ArmorSlots.size

    fun combatStats(
        character: CharacterEntity,
        equipped: List<ItemEntity>,
        runeSpd: Int = 0,
        runeCrit: Int = 0,
        siphonHeal: Int = 0,
        masteryBonus: MasteryStatBonus = MasteryStatBonus.NONE
    ): CombatStats {
        val cls = character.characterClass ?: CharacterClass.WARRIOR
        val baseHp = 40 + character.endurance * 8 + character.level * 5 + equipped.sumOf { it.hp }
        val itemAtk = equipped.sumOf { it.atk }

        // Each class draws attack power from different attributes.
        var atk = 5 + itemAtk + when (cls) {
            CharacterClass.WARRIOR -> (character.strength * 2.4).roundToInt()
            CharacterClass.MAGE -> character.strength + (character.willpower * 1.5).roundToInt()
            CharacterClass.THIEF -> (character.strength * 1.5 + character.agility * 0.7).roundToInt()
            CharacterClass.RANGER -> (character.strength * 1.7 + character.agility * 0.5).roundToInt()
            CharacterClass.PALADIN ->
                (character.strength * 1.8 + character.willpower * 0.8).roundToInt()
            CharacterClass.NECROMANCER ->
                character.strength + (character.willpower * 1.6).roundToInt()
            CharacterClass.WHITE_MAGE ->
                (character.willpower * 1.8).roundToInt()
            CharacterClass.MONK ->
                (character.agility * 1.5 + character.willpower * 0.9).roundToInt()
            CharacterClass.DRUID ->
                (character.willpower * 1.2 + character.endurance * 0.8).roundToInt()
            CharacterClass.BERSERKER ->
                (character.strength * 2.2).roundToInt()
            CharacterClass.BARD ->
                (character.agility * 1.1 + character.willpower * 1.1).roundToInt()
            CharacterClass.SUMMONER ->
                (character.willpower * 1.7).roundToInt()
            CharacterClass.DRAGOON ->
                (character.strength * 1.6 + character.agility * 0.8).roundToInt()
        }
        
        // Form modifiers (Druid/Summoner/Dragoon)
        if (cls == CharacterClass.DRUID) {
            when (character.druidForm) {
                "BEAR" -> atk = (atk * 0.5).roundToInt()
                "PANTHER" -> atk = (atk * 1.1).roundToInt()
            }
        } else if (cls == CharacterClass.SUMMONER) {
            when (character.druidForm) {
                "IFRIT" -> atk = (atk * 1.5).roundToInt()
                "SHIVA" -> atk = (atk * 1.1).roundToInt()
            }
        }

        var maxHp = when (cls) {
            CharacterClass.WARRIOR -> (baseHp * 1.1).roundToInt()
            CharacterClass.PALADIN -> (baseHp * 1.08).roundToInt()
            CharacterClass.DRUID -> when (character.druidForm) {
                "BEAR" -> (baseHp * 2.0).roundToInt()
                "PANTHER" -> (baseHp * 0.7).roundToInt() // Panther penalty
                else -> baseHp
            }
            else -> baseHp
        }
        var def = 2 + character.agility + equipped.sumOf { it.def } 
        if (cls == CharacterClass.DRUID && character.druidForm == "BEAR") def += 10
        if (cls == CharacterClass.SUMMONER && character.druidForm == "IFRIT") def = (def * 0.8).roundToInt()
        if (cls == CharacterClass.DRAGOON && character.druidForm == "WYVERN") def += 15

        // Full class armor set: +10% ATK and HP, +3 DEF.
        if (hasFullSetBonus(character, equipped)) {
            atk = (atk * 1.1).roundToInt()
            maxHp = (maxHp * 1.1).roundToInt()
            def += 3
        }

        // Mastery stat bonuses
        atk += masteryBonus.flatAtk
        def += masteryBonus.flatDef
        maxHp += masteryBonus.flatMaxHp

        val baseCrit = when (cls) {
            CharacterClass.THIEF -> min(10 + character.agility, 50)
            CharacterClass.DRUID -> if (character.druidForm == "PANTHER") min(35 + character.agility, 70) else min(5 + character.agility / 2, 35)
            CharacterClass.MONK -> min(15 + character.agility, 55)
            CharacterClass.SUMMONER -> if (character.druidForm == "SHIVA") min(25 + character.agility, 60) else min(5 + character.agility / 2, 35)
            else -> min(5 + character.agility / 2, 35)
        }

        return CombatStats(
            maxHp = maxHp,
            atk = atk,
            def = def,
            spd = (character.agility * 2 + character.level + runeSpd + masteryBonus.flatSpd + (if (cls == CharacterClass.DRUID && character.druidForm == "PANTHER") 20 else 0) + (if (cls == CharacterClass.SUMMONER && character.druidForm == "SHIVA") 40 else 0)),
            critPercent = min(baseCrit + runeCrit + masteryBonus.flatCritPercent, 70),
            siphonHeal = if (masteryBonus.siphonBonusPercent > 0f) (siphonHeal * (1f + masteryBonus.siphonBonusPercent)).roundToInt() else siphonHeal,
            mitigationPercent = masteryBonus.mitigationPercent
        )
    }

    /** One-time attribute bonus applied when a class is first chosen. */
    fun applyClassBonus(character: CharacterEntity, cls: CharacterClass): CharacterEntity = when (cls) {
        CharacterClass.WARRIOR -> character.copy(strength = character.strength + 3)
        CharacterClass.MAGE -> character.copy(willpower = character.willpower + 3)
        CharacterClass.THIEF -> character.copy(agility = character.agility + 3)
        CharacterClass.RANGER -> character.copy(endurance = character.endurance + 3)
        CharacterClass.PALADIN -> character.copy(
            strength = character.strength + 2,
            willpower = character.willpower + 2
        )
        CharacterClass.NECROMANCER -> character.copy(willpower = character.willpower + 3)
        CharacterClass.WHITE_MAGE -> character.copy(willpower = character.willpower + 3)
        CharacterClass.MONK -> character.copy(agility = character.agility + 2, willpower = character.willpower + 2)
        CharacterClass.DRUID -> character.copy(endurance = character.endurance + 2, agility = character.agility + 2)
        CharacterClass.BERSERKER -> character.copy(strength = character.strength + 3)
        CharacterClass.BARD -> character.copy(agility = character.agility + 2, willpower = character.willpower + 2)
        CharacterClass.SUMMONER -> character.copy(willpower = character.willpower + 3)
        CharacterClass.DRAGOON -> character.copy(strength = character.strength + 2, agility = character.agility + 1)
    }

    /** Passive energy recoup logic for "Resting gives energy". */
    fun recoupEnergy(
        character: CharacterEntity,
        now: Long = System.currentTimeMillis(),
        maxEnergy: Int = MAX_ENERGY
    ): CharacterEntity {
        if (character.energy >= maxEnergy) return character.copy(lastEnergyUpdate = now)
        val elapsed = now - character.lastEnergyUpdate
        if (elapsed < ENERGY_TICK_MILLIS) return character
        
        val points = (elapsed / ENERGY_TICK_MILLIS).toInt()
        val newEnergy = min(maxEnergy, character.energy + points)
        // Preserve fractional progress towards the next point.
        val remainder = elapsed % ENERGY_TICK_MILLIS
        return character.copy(
            energy = newEnergy,
            lastEnergyUpdate = now - remainder
        )
    }
}
