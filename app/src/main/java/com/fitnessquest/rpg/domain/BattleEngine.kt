package com.fitnessquest.rpg.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

enum class BattleAction { ATTACK, SKILL, DEFEND, FLEE }

enum class BattleOutcome { ONGOING, VICTORY, DEFEAT, FLED }

data class BattleState(
    val playerName: String,
    val clazz: CharacterClass,
    val level: Int,
    val monster: Monster,
    val playerStats: CombatStats,
    val playerHp: Int,
    val monsterHp: Int,
    /** Cooldown turns remaining, parallel to [CharacterClass.skills]. */
    val skillCooldowns: List<Int> = listOf(0, 0, 0),
    val playerDefending: Boolean = false,
    val strength: Int = 0,
    val agility: Int = 0,
    val willpower: Int = 0,
    // Status effects. Counters tick down at the end of each full round.
    val atkBuffTurns: Int = 0,       // War Cry: player deals +40%
    val monsterChillTurns: Int = 0,  // Frost Lance: monster deals -35%
    val monsterStunTurns: Int = 0,   // Titan Smash: monster skips turns
    val monsterBleedTurns: Int = 0,  // Rain of Arrows: monster bleeds
    val monsterBleedDmg: Int = 0,
    val playerPoisonTurns: Int = 0,  // Venomous monsters poison the player
    val playerVanished: Boolean = false, // Smoke Bomb: next monster attack misses
    val log: List<String> = emptyList(),
    val outcome: BattleOutcome = BattleOutcome.ONGOING,
    val droppedItemId: Long? = null,
    val druidForm: String = "HUMAN",
    val baseStats: CombatStats? = null
) {
    /** Skills the player has unlocked at their current level. */
    val unlockedSkills: List<ClassSkill> get() = clazz.skillsUnlockedAt(level)
}

/**
 * Pure turn-based battle logic. Each [takeTurn] call resolves the player's action
 * and, if the battle continues, the monster's response.
 *
 * Stats shape every exchange:
 * - ATK/DEF drive damage on both sides.
 * - SPD gives dodge chances, flee odds, and decides who lands the first blow.
 * - WIL powers Defend healing, mage skills, and shakes off poison faster.
 * - Crit chance (AGI-driven) triggers 1.6x hits.
 *
 * Monster traits (armored, swift, venomous, regenerating, enraged) bend the
 * rules further, and class skills unlocked by leveling bend them back.
 */
object BattleEngine {

    fun start(
        playerName: String,
        clazz: CharacterClass,
        level: Int,
        stats: CombatStats,
        strength: Int,
        agility: Int,
        willpower: Int,
        monster: Monster,
        druidForm: String = "HUMAN",
        baseStats: CombatStats? = null,
        rng: Random = Random.Default
    ): BattleState {
        var s = BattleState(
            playerName = playerName,
            clazz = clazz,
            level = level,
            monster = monster,
            playerStats = stats,
            playerHp = stats.maxHp,
            monsterHp = monster.hp,
            strength = strength,
            agility = agility,
            willpower = willpower,
            druidForm = druidForm,
            baseStats = baseStats,
            log = buildList {
                add("A wild ${monster.name} ${monster.emoji} appears!")
                monster.trait?.let { add("${it.emoji} It is ${it.label.uppercase()}: ${it.blurb}") }
            }
        )
        // Speed decides who strikes first: slower heroes eat an opening hit.
        if (monster.spd > stats.spd) {
            val (dmg, _) = rollDamage(monster.atk * 0.75, stats.def.toDouble(), 0, rng)
            s = s.copy(
                playerHp = max(1, s.playerHp - dmg),
                log = s.log + "The ${monster.name} is faster than you \u2014 it strikes first for $dmg damage!"
            )
        }
        return s
    }

    fun takeTurn(
        state: BattleState,
        action: BattleAction,
        skillIndex: Int = 0,
        precisionMultiplier: Double = 1.0,
        rng: Random = Random.Default
    ): BattleState {
        if (state.outcome != BattleOutcome.ONGOING) return state
        var s = state.copy(playerDefending = false)
        val lines = mutableListOf<String>()

        when (action) {
            BattleAction.ATTACK -> s = playerAttack(s, lines, rng, precisionMultiplier)
            BattleAction.SKILL -> s = playerSkill(s, skillIndex, lines, rng)
            BattleAction.DEFEND -> {
                val heal = max(2, (s.willpower * 0.8).roundToInt())
                s = s.copy(
                    playerDefending = true,
                    playerHp = minOf(s.playerStats.maxHp, s.playerHp + heal)
                )
                lines += "You brace yourself and recover $heal HP."
            }
            BattleAction.FLEE -> {
                val chance = 50 + ((s.playerStats.spd - s.monster.spd) * 2)
                if (rng.nextInt(100) < chance.coerceIn(20, 90)) {
                    lines += "You fled the battle. Live to lift another day."
                    return s.copy(log = s.log + lines, outcome = BattleOutcome.FLED)
                } else {
                    lines += "You couldn't escape!"
                }
            }
        }

        // Bleed ticks after the player acts, so a finishing volley still counts.
        if (s.monsterBleedTurns > 0 && s.monsterHp > 0) {
            s = s.copy(monsterHp = max(0, s.monsterHp - s.monsterBleedDmg))
            lines += "The ${s.monster.name} bleeds for ${s.monsterBleedDmg} damage."
        }

        if (s.monsterHp <= 0) {
            lines += "The ${s.monster.name} is defeated! +${s.monster.goldReward} gold, +${s.monster.xpReward} XP!"
            return s.copy(log = s.log + lines, outcome = BattleOutcome.VICTORY)
        }

        // Regenerating monsters knit their wounds before acting.
        if (s.monster.trait == MonsterTrait.REGENERATING) {
            val regen = max(2, s.monster.hp / 25)
            if (s.monsterHp < s.monster.hp) {
                s = s.copy(monsterHp = minOf(s.monster.hp, s.monsterHp + regen))
                lines += "\uD83D\uDC9A The ${s.monster.name}'s wounds knit closed (+$regen HP)."
            }
        }

        // Monster's turn (unless stunned)
        if (s.monsterStunTurns > 0) {
            lines += "\uD83D\uDCAB The ${s.monster.name} is stunned and can't move!"
        } else {
            s = monsterAttack(s, lines, rng)
            if (s.playerHp <= 0) {
                lines += "You have been defeated... but every hero rises again. Train and return!"
                return s.copy(log = s.log + lines, outcome = BattleOutcome.DEFEAT)
            }
            // Swift monsters may lunge again if they outpace you.
            if (s.monster.trait == MonsterTrait.SWIFT &&
                s.monster.spd > s.playerStats.spd && rng.nextInt(100) < 30
            ) {
                lines += "\uD83D\uDCA8 The ${s.monster.name} moves in a blur \u2014 it strikes again!"
                s = monsterAttack(s, lines, rng)
                if (s.playerHp <= 0) {
                    lines += "You have been defeated... but every hero rises again. Train and return!"
                    return s.copy(log = s.log + lines, outcome = BattleOutcome.DEFEAT)
                }
            }
        }

        // Dragoon Wyvern Regen
        if (s.clazz == CharacterClass.DRAGOON && s.druidForm == "WYVERN") {
            val heal = (s.playerStats.maxHp * 0.05).roundToInt()
            s = s.copy(playerHp = min(s.playerStats.maxHp, s.playerHp + heal))
            lines += "\uD83E\uDDBA Your Wyvern heals you for $heal HP."
        }

        // Poison ticks at the end of the round; Willpower resists the worst of it.
        if (s.playerPoisonTurns > 0) {
            val poison = max(3, s.monster.tier * 6 + s.monster.level / 2 - s.willpower / 3)
            s = s.copy(playerHp = max(0, s.playerHp - poison))
            lines += "\u2620\uFE0F Poison courses through you for $poison damage."
            if (s.playerHp <= 0) {
                lines += "The poison overwhelms you... train and return stronger!"
                return s.copy(log = s.log + lines, outcome = BattleOutcome.DEFEAT)
            }
        }

        // Tick down cooldowns and statuses for the next round.
        return s.copy(
            log = s.log + lines,
            skillCooldowns = s.skillCooldowns.map { max(0, it - 1) },
            atkBuffTurns = max(0, s.atkBuffTurns - 1),
            monsterChillTurns = max(0, s.monsterChillTurns - 1),
            monsterStunTurns = max(0, s.monsterStunTurns - 1),
            monsterBleedTurns = max(0, s.monsterBleedTurns - 1),
            playerPoisonTurns = max(0, s.playerPoisonTurns - 1)
        )
    }

    // ---- Player offense ----

    private fun playerAttack(
        state: BattleState,
        lines: MutableList<String>,
        rng: Random,
        precisionMultiplier: Double = 1.0
    ): BattleState {
        var s = state
        // Swift monsters are slippery; your own speed pins them down.
        val dodge = monsterDodgeChance(s)
        if (rng.nextInt(100) < dodge) {
            lines += "The ${s.monster.name} darts aside \u2014 your attack misses!"
            return s
        }
        val (dmg, crit) = rollDamage(
            atk = s.playerStats.atk * buffMult(s) * precisionMultiplier.coerceIn(1.0, 2.0),
            def = s.monster.def.toDouble(),
            critPercent = s.playerStats.critPercent,
            rng = rng,
            armored = s.monster.trait == MonsterTrait.ARMORED
        )
        s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
        lines += when {
            precisionMultiplier >= 1.95 -> "PERFECT STRIKE! You hit the ${s.monster.name} for $dmg damage!"
            precisionMultiplier >= 1.45 -> "Great strike! You hit the ${s.monster.name} for $dmg damage."
            crit -> "CRITICAL HIT! You strike the ${s.monster.name} for $dmg damage!"
            s.monster.trait == MonsterTrait.ARMORED -> "Your attack glances off the armor \u2014 $dmg damage."
            else -> "You attack the ${s.monster.name} for $dmg damage."
        }
        return s
    }

    private fun playerSkill(
        state: BattleState,
        index: Int,
        lines: MutableList<String>,
        rng: Random
    ): BattleState {
        var s = state
        val skill = s.unlockedSkills.getOrNull(index) ?: return s
        if (s.skillCooldowns[index] > 0) {
            lines += "${skill.name} is still recharging (${s.skillCooldowns[index]} turns left)."
            return s
        }

        val atk = s.playerStats.atk * buffMult(s)
        val def = s.monster.def.toDouble()
        val armored = s.monster.trait == MonsterTrait.ARMORED

        fun hit(raw: Double, defFactor: Double, crit: Int, piercing: Boolean = false): Int {
            val (dmg, _) = rollDamage(raw, def * defFactor, crit, rng, armored = armored && !piercing)
            return dmg
        }

        when (s.clazz) {
            CharacterClass.WARRIOR -> when (index) {
                0 -> {
                    val dmg = hit(atk * 2.0, 0.6, 15)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} BERSERKER SLASH! A devastating blow for $dmg damage!"
                }
                1 -> {
                    s = s.copy(atkBuffTurns = 4) // ticks once this round, lasts 3 more
                    lines += "${skill.emoji} WAR CRY! Your muscles surge \u2014 +40% attack for 3 turns!"
                }
                else -> {
                    val dmg = hit(atk * 3.0, 0.6, 20)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg), monsterStunTurns = 2)
                    lines += "${skill.emoji} TITAN SMASH! $dmg damage \u2014 the ${s.monster.name} reels, stunned!"
                }
            }
            CharacterClass.MAGE -> when (index) {
                0 -> {
                    val dmg = hit(atk * 0.9 + s.willpower * 2.6, 0.2, 10, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} ARCANE BLAST! Raw magic melts through for $dmg damage!"
                }
                1 -> {
                    val dmg = hit(atk * 1.2 + s.willpower * 1.5, 0.3, 10, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg), monsterChillTurns = 3)
                    lines += "${skill.emoji} FROST LANCE! $dmg damage \u2014 the ${s.monster.name} is chilled, its attacks weakened!"
                }
                else -> {
                    val dmg = hit(atk * 1.5 + s.willpower * 4.0, 0.0, 15, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} METEOR! The sky falls on the ${s.monster.name} for $dmg damage!"
                }
            }
            CharacterClass.THIEF -> when (index) {
                0 -> {
                    val dmg = hit(atk * 1.5 + s.agility, 0.6, 100)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} SHADOW STRIKE! A guaranteed critical for $dmg damage!"
                }
                1 -> {
                    val dmg = hit(atk * 1.2 + s.agility, 0.6, s.playerStats.critPercent)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg), playerVanished = true)
                    lines += "${skill.emoji} SMOKE BOMB! $dmg damage \u2014 you vanish into the haze!"
                }
                else -> {
                    val executing = s.monsterHp < s.monster.hp * 0.3
                    val mult = if (executing) 3.2 else 2.0
                    val dmg = hit(atk * mult + s.agility * 1.5, 0.6, 100)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += if (executing) {
                        "${skill.emoji} ASSASSINATE! You go for the kill \u2014 $dmg damage!"
                    } else {
                        "${skill.emoji} ASSASSINATE! A lethal critical for $dmg damage!"
                    }
                }
            }
            CharacterClass.RANGER -> when (index) {
                0 -> {
                    val dmg = hit(atk * 1.7 + s.agility * 0.5, 0.0, 15, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} PIERCING ARROW! It punches clean through for $dmg damage!"
                }
                1 -> {
                    val d1 = hit(atk * 1.1, 0.6, s.playerStats.critPercent)
                    val d2 = hit(atk * 1.1, 0.6, s.playerStats.critPercent)
                    s = s.copy(monsterHp = max(0, s.monsterHp - d1 - d2))
                    lines += "${skill.emoji} TWIN SHOT! Two arrows strike for $d1 and $d2 damage!"
                }
                else -> {
                    val dmg = hit(atk * 1.8 + s.agility * 0.5, 0.3, 15, piercing = true)
                    val bleed = max(3, (s.playerStats.atk * 0.25).roundToInt())
                    s = s.copy(
                        monsterHp = max(0, s.monsterHp - dmg),
                        monsterBleedTurns = 3,
                        monsterBleedDmg = bleed
                    )
                    lines += "${skill.emoji} RAIN OF ARROWS! $dmg damage \u2014 the ${s.monster.name} is left bleeding!"
                }
            }
            CharacterClass.PALADIN -> when (index) {
                0 -> {
                    val dmg = hit(atk * 1.4 + s.willpower * 0.8, 0.5, 12)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} HOLY STRIKE! Radiance crashes for $dmg damage!"
                }
                1 -> {
                    s = s.copy(playerDefending = true, atkBuffTurns = max(s.atkBuffTurns, 2))
                    lines += "${skill.emoji} DIVINE SHIELD! A holy ward blunts the next blow!"
                }
                else -> {
                    val dmg = hit(atk * 2.2 + s.willpower, 0.2, 15, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg), monsterStunTurns = 2)
                    lines += "${skill.emoji} JUDGMENT! $dmg piercing damage \u2014 the ${s.monster.name} is stunned!"
                }
            }
            CharacterClass.NECROMANCER -> when (index) {
                0 -> {
                    val dmg = hit(atk * 1.1 + s.willpower * 2.2, 0.25, 10, piercing = true)
                    val heal = max(1, (dmg * 0.35).roundToInt())
                    s = s.copy(
                        monsterHp = max(0, s.monsterHp - dmg),
                        playerHp = min(s.playerStats.maxHp, s.playerHp + heal)
                    )
                    lines += "${skill.emoji} DRAIN LIFE! $dmg damage siphoned \u2014 you recover $heal HP!"
                }
                1 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        druidForm = "SKELETON",
                        playerStats = base.copy(
                            def = base.def + 15
                        )
                    )
                    lines += "${skill.emoji} RAISE SKELETON! A skeleton warrior rises from the earth to shield you! Defense +15."
                    val dmg = hit(atk * 1.5, 0.5, 10)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "The skeleton strikes the ${s.monster.name} for $dmg damage!"
                }
                else -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        druidForm = "ARMY",
                        playerStats = base.copy(
                            atk = (base.atk * 1.25).roundToInt(),
                            def = base.def + 20
                        )
                    )
                    lines += "${skill.emoji} ARMY OF THE DEAD! A legion of skeletons and zombies swarms the battlefield!"
                    val dmg = hit(atk * 2.2 + s.willpower * 1.5, 0.1, 15, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "The horde tramples the ${s.monster.name} for $dmg piercing damage!"
                }
            }
            CharacterClass.DRUID -> when (index) {
                0 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        druidForm = "BEAR",
                        playerStats = base.copy(
                            maxHp = base.maxHp * 2,
                            def = base.def + 10,
                            atk = (base.atk * 0.5).roundToInt()
                        ),
                        playerHp = s.playerHp + base.maxHp
                    )
                    lines += "${skill.emoji} BEAR FORM! You grow thick fur and mighty claws \u2014 you are now a Dire Bear!"
                    val currentAtk = s.playerStats.atk * buffMult(s)
                    val dmg = hit(currentAtk * 1.5, 0.4, 10)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "You swipe at the ${s.monster.name} for $dmg damage!"
                }
                1 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        druidForm = "PANTHER",
                        playerStats = base.copy(
                            critPercent = min(base.critPercent + 50, 80),
                            spd = base.spd + 20,
                            maxHp = (base.maxHp * 0.7).roundToInt()
                        ),
                        playerHp = min(s.playerHp, (base.maxHp * 0.7).roundToInt())
                    )
                    lines += "${skill.emoji} PANTHER FORM! You vanish into a blur of speed \u2014 you are now a Dire Panther!"
                    val currentAtk = s.playerStats.atk * buffMult(s)
                    val dmg = hit(currentAtk * 1.2, 0.6, 50)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "You pounce on the ${s.monster.name} for $dmg damage!"
                }
                else -> {
                    val dmg = hit(atk * 2.0 + s.willpower * 1.5, 0.3, 15)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} WRATH OF NATURE! Roots crush the ${s.monster.name} for $dmg damage!"
                }
            }
            CharacterClass.SUMMONER -> when (index) {
                0 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        druidForm = "IFRIT",
                        playerStats = base.copy(
                            atk = (base.atk * 1.5).roundToInt(),
                            def = (base.def * 0.8).roundToInt()
                        )
                    )
                    lines += "${skill.emoji} SUMMON IFRIT! A primal of fire appears! Your attack power is scorched to +50%!"
                }
                1 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        druidForm = "SHIVA",
                        playerStats = base.copy(
                            spd = base.spd + 40,
                            critPercent = min(base.critPercent + 20, 70)
                        )
                    )
                    lines += "${skill.emoji} SUMMON SHIVA! The lady of frost appears! You feel a glacial chill +40 SPD!"
                }
                else -> {
                    val dmg = hit(atk * 2.5 + s.willpower * 2.0, 0.5, 25, true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} ASTRAL FLOW! Your primal unleashes their ultimate attack for $dmg damage!"
                }
            }
            CharacterClass.DRAGOON -> when (index) {
                0 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        druidForm = "WYVERN",
                        playerStats = base.copy(
                            def = base.def + 15
                        )
                    )
                    lines += "${skill.emoji} CALL WYVERN! A loyal wyvern takes flight beside you. Defense +15 and constant healing!"
                }
                1 -> {
                    val dmg = hit(atk * 2.5, 0.5, 20, true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} JUMP! You leap high into the clouds and crash down for $dmg damage!"
                }
                else -> {
                    val dmg = hit(atk * 3.5, 0.4, 40)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} DRAGON DIVE! A legendary strike from the heavens incinerates the ${s.monster.name} for $dmg!"
                }
            }
            else -> {
                val dmg = hit(atk * 1.8 + s.willpower, 0.4, 15)
                s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                lines += "${skill.emoji} ${skill.name}! Unleashed power dealing $dmg damage!"
            }
        }

        return s.copy(
            skillCooldowns = s.skillCooldowns.mapIndexed { i, cd ->
                if (i == index) skill.cooldown + 1 else cd // +1 because it ticks this round
            }
        )
    }

    // ---- Monster offense ----

    private fun monsterAttack(state: BattleState, lines: MutableList<String>, rng: Random): BattleState {
        var s = state
        if (s.playerVanished) {
            lines += "The ${s.monster.name} lashes out at empty smoke \u2014 you're untouchable!"
            return s.copy(playerVanished = false)
        }
        // Your speed lets you slip attacks entirely.
        val dodge = playerDodgeChance(s)
        if (rng.nextInt(100) < dodge) {
            lines += "\uD83D\uDCA8 You sidestep the ${s.monster.name}'s attack!"
            return s
        }

        var monsterAtk = s.monster.atk.toDouble()
        var enraged = false
        if (s.monster.trait == MonsterTrait.ENRAGED && s.monsterHp < s.monster.hp * 0.4) {
            monsterAtk *= 1.4
            enraged = true
        }
        if (s.monsterChillTurns > 0) monsterAtk *= 0.65

        val (raw, crit) = rollDamage(monsterAtk, s.playerStats.def.toDouble(), 10, rng)
        val dmg = if (s.playerDefending) max(1, (raw * 0.45).roundToInt()) else raw
        s = s.copy(playerHp = max(0, s.playerHp - dmg))
        lines += when {
            s.playerDefending -> "The ${s.monster.name} attacks, but your guard absorbs it. $dmg damage."
            enraged -> "\uD83D\uDE21 The ${s.monster.name} attacks in a frenzy! $dmg damage!"
            crit -> "The ${s.monster.name} lands a brutal blow! $dmg damage!"
            s.monsterChillTurns > 0 -> "The chilled ${s.monster.name} strikes sluggishly for $dmg damage."
            else -> "The ${s.monster.name} hits you for $dmg damage."
        }

        // Venomous bites can poison; Willpower shortens the suffering.
        if (s.monster.trait == MonsterTrait.VENOMOUS && s.playerPoisonTurns == 0 && rng.nextInt(100) < 40) {
            val turns = max(2, 4 - s.willpower / 12)
            s = s.copy(playerPoisonTurns = turns)
            lines += "\u2620\uFE0F Venom seeps into your veins \u2014 poisoned for $turns turns!"
        }
        return s
    }

    // ---- Shared math ----

    private fun buffMult(s: BattleState): Double = if (s.atkBuffTurns > 0) 1.4 else 1.0

    /** Chance for the monster to dodge your attacks: its speed edge, plus slipperiness if swift. */
    private fun monsterDodgeChance(s: BattleState): Int {
        val base = ((s.monster.spd - s.playerStats.spd) * 1.5).roundToInt().coerceIn(0, 25)
        val swift = if (s.monster.trait == MonsterTrait.SWIFT) 8 else 0
        return base + swift
    }

    /** Chance for you to dodge the monster: your speed edge over it. */
    private fun playerDodgeChance(s: BattleState): Int =
        ((s.playerStats.spd - s.monster.spd) * 1.5).roundToInt().coerceIn(0, 30)

    /**
     * Soft mitigation: DEF reduces damage but never floors equal-level fights
     * to chip damage. Roughly 40 DEF ≈ 50% reduction, capped at 75%.
     */
    private fun rollDamage(
        atk: Double,
        def: Double,
        critPercent: Int,
        rng: Random,
        armored: Boolean = false
    ): Pair<Int, Boolean> {
        val variance = 0.85 + rng.nextDouble() * 0.3
        val crit = rng.nextInt(100) < critPercent
        val critMult = if (crit) 1.6 else 1.0
        val armorMult = if (armored) 0.7 else 1.0
        val softDef = def.coerceAtLeast(0.0)
        val mitigation = (softDef / (softDef + 40.0)).coerceIn(0.0, 0.75)
        val dmg = max(1, (atk * (1.0 - mitigation) * variance * critMult * armorMult).roundToInt())
        return dmg to crit
    }
}
