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
    val skillCooldowns: List<Int> = List(6) { 0 },
    val playerDefending: Boolean = false,
    val strength: Int = 0,
    val agility: Int = 0,
    val willpower: Int = 0,
    // Status effects. Counters tick down at the end of each full round.
    val atkBuffTurns: Int = 0,       // War Cry / Bestial Wrath: player deals +40%
    val monsterChillTurns: Int = 0,  // Frost Lance: monster deals -35%
    val monsterStunTurns: Int = 0,   // Titan Smash / Stun skills: monster skips turns
    val monsterBleedTurns: Int = 0,  // Bleeds: monster takes damage over time
    val monsterBleedDmg: Int = 0,
    val playerPoisonTurns: Int = 0,  // Venomous monsters poison the player
    val playerVanished: Boolean = false, // Smoke Bomb: next monster attack misses
    val log: List<String> = emptyList(),
    val outcome: BattleOutcome = BattleOutcome.ONGOING,
    val droppedItemId: Long? = null,
    val druidForm: String = "HUMAN",
    val baseStats: CombatStats? = null,
    val equippedTraits: List<GearTrait> = emptyList()
) {
    /** Skills the player has unlocked at their current level. */
    val unlockedSkills: List<ClassSkill> get() = clazz.skillsUnlockedAt(level)
}

/**
 * Pure turn-based battle logic. Each [takeTurn] call resolves the player's action
 * and, if the battle continues, the monster's response.
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
        equippedTraits: List<GearTrait> = emptyList(),
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
            equippedTraits = equippedTraits,
            skillCooldowns = List(clazz.skills.size) { 0 },
            log = buildList {
                add("A wild ${monster.name} ${monster.emoji} appears!")
                monster.trait?.let { add("${it.emoji} It is ${it.label.uppercase()}: ${it.blurb}") }
            }
        )

        // Swift monsters strike first if they outspeed you.
        if (monster.spd > stats.spd) {
            val lines = mutableListOf("${monster.name} is faster and strikes first!")
            s = monsterAttack(s, lines, rng)
            s = s.copy(log = s.log + lines)
            if (s.playerHp <= 0) {
                return s.copy(
                    outcome = BattleOutcome.DEFEAT,
                    log = s.log + "You were struck down before you could act!"
                )
            }
        }
        return s
    }

    /**
     * Resolves a turn: player acts -> status ticks -> monster acts (if alive) -> end-of-round upkeep.
     */
    fun takeTurn(
        state: BattleState,
        action: BattleAction,
        skillIndex: Int = 0,
        rng: Random = Random.Default,
        precisionMultiplier: Double = 1.0
    ): BattleState {
        if (state.outcome != BattleOutcome.ONGOING) return state

        var s = state.copy(playerDefending = false)
        val lines = mutableListOf<String>()

        // 1. Player Action
        when (action) {
            BattleAction.ATTACK -> s = playerAttack(s, lines, rng, precisionMultiplier)
            BattleAction.SKILL -> s = playerSkill(s, skillIndex, lines, rng)
            BattleAction.DEFEND -> {
                s = s.copy(playerDefending = true)
                val heal = max(1, s.willpower / 4 + s.playerStats.maxHp / 20)
                val newHp = min(s.playerStats.maxHp, s.playerHp + heal)
                s = s.copy(playerHp = newHp)
                lines += "🛡️ You raise your guard, bracing for impact and catching your breath (+$heal HP)."
            }
            BattleAction.FLEE -> {
                val fleeChance = (40 + (s.playerStats.spd - s.monster.spd) * 2).coerceIn(15, 85)
                return if (rng.nextInt(100) < fleeChance) {
                    lines += "🏃 You managed to escape!"
                    s.copy(log = s.log + lines, outcome = BattleOutcome.FLED)
                } else {
                    lines += "🏃 You tried to run, but the ${s.monster.name} cut you off!"
                    monsterAttack(s, lines, rng).let { next ->
                        next.copy(log = next.log + lines)
                    }
                }
            }
        }

        // Check victory
        if (s.monsterHp <= 0) {
            lines += "🏆 The ${s.monster.name} collapses! Victory is yours!"
            return s.copy(
                log = s.log + lines,
                outcome = BattleOutcome.VICTORY
            )
        }

        // 2. Monster Bleed status
        if (s.monsterBleedTurns > 0) {
            s = s.copy(monsterHp = max(0, s.monsterHp - s.monsterBleedDmg))
            lines += "🩸 The ${s.monster.name} bleeds for ${s.monsterBleedDmg} damage."
            if (s.monsterHp <= 0) {
                lines += "🏆 The ${s.monster.name} succumbed to its wounds! Victory!"
                return s.copy(
                    log = s.log + lines,
                    outcome = BattleOutcome.VICTORY
                )
            }
        }

        // 3. Monster Regenerating status
        if (s.monster.trait == MonsterTrait.REGENERATING && s.monsterHp < s.monster.hp) {
            val regen = max(4, (s.monster.hp * 0.08).roundToInt())
            s = s.copy(monsterHp = min(s.monster.hp, s.monsterHp + regen))
            lines += "💚 The ${s.monster.name} knits its wounds shut, recovering $regen HP."
        }

        // 4. Monster Action
        if (s.monsterStunTurns > 0) {
            lines += "💫 The ${s.monster.name} is stunned and cannot act!"
        } else {
            s = monsterAttack(s, lines, rng)
            if (s.playerHp <= 0) {
                lines += "You have been defeated... train and return stronger!"
                return s.copy(log = s.log + lines, outcome = BattleOutcome.DEFEAT)
            }

            // Swift monsters may lunge again if they outpace you
            if (s.monster.trait == MonsterTrait.SWIFT &&
                s.monster.spd > s.playerStats.spd && rng.nextInt(100) < 30
            ) {
                lines += "💨 The ${s.monster.name} moves in a blur — it strikes again!"
                s = monsterAttack(s, lines, rng)
                if (s.playerHp <= 0) {
                    lines += "You have been defeated... train and return stronger!"
                    return s.copy(log = s.log + lines, outcome = BattleOutcome.DEFEAT)
                }
            }
        }

        // 5. Dragoon Wyvern Regen
        if (s.clazz == CharacterClass.DRAGOON && s.druidForm == "WYVERN") {
            val heal = max(2, (s.playerStats.maxHp * 0.02).roundToInt())
            s = s.copy(playerHp = min(s.playerStats.maxHp, s.playerHp + heal))
            lines += "🐲 Your Wyvern heals you for $heal HP."
        }

        // 6. Hunter / Ranger Companion Turn Assist
        if (s.clazz == CharacterClass.RANGER) {
            when (s.druidForm) {
                "FALCON" -> {
                    val falconDmg = max(3, (s.playerStats.atk * 0.22).roundToInt())
                    s = s.copy(monsterHp = max(0, s.monsterHp - falconDmg))
                    lines += "🦅 Your Hunting Falcon swoops down, raking the ${s.monster.name} for $falconDmg damage!"
                }
                "BEAR" -> {
                    val bearHeal = max(2, (s.playerStats.maxHp * 0.025).roundToInt())
                    s = s.copy(playerHp = min(s.playerStats.maxHp, s.playerHp + bearHeal))
                    lines += "🐻 Your Forest Bear shields you, restoring $bearHeal HP."
                }
                else -> { // "WOLF" or default
                    val biteDmg = max(3, (s.playerStats.atk * 0.25).roundToInt())
                    s = s.copy(monsterHp = max(0, s.monsterHp - biteDmg))
                    lines += "🐺 Your Dire Wolf flanks from the brush, biting the ${s.monster.name} for $biteDmg damage!"
                }
            }
            if (s.monsterHp <= 0) {
                lines += "🏆 The ${s.monster.name} falls to your companion! Victory!"
                return s.copy(
                    log = s.log + lines,
                    outcome = BattleOutcome.VICTORY
                )
            }
        }

        // 7. Druid Wild Shape Turn Passives
        if (s.clazz == CharacterClass.DRUID) {
            when (s.druidForm) {
                "BEAR" -> {
                    val bearRegen = max(3, (s.playerStats.maxHp * 0.03).roundToInt())
                    s = s.copy(playerHp = min(s.playerStats.maxHp, s.playerHp + bearRegen))
                    lines += "🐻 Ironhide vitality restores $bearRegen HP."
                }
                "PANTHER" -> {
                    if (s.monsterBleedTurns <= 0 && rng.nextInt(100) < 40) {
                        val bleedDmg = max(4, (s.playerStats.atk * 0.35).roundToInt())
                        s = s.copy(monsterBleedTurns = 2, monsterBleedDmg = bleedDmg)
                        lines += "🐆 Shadow claws leave deep lacerations! The ${s.monster.name} is bleeding."
                    }
                }
                "TREANT" -> {
                    val sporeHeal = max(2, (s.playerStats.maxHp * 0.02).roundToInt())
                    val thornDmg = max(3, (s.playerStats.def * 0.4).roundToInt())
                    s = s.copy(
                        playerHp = min(s.playerStats.maxHp, s.playerHp + sporeHeal),
                        monsterHp = max(0, s.monsterHp - thornDmg)
                    )
                    lines += "🌲 Ancient Treant spores restore $sporeHeal HP and thorns prick for $thornDmg damage!"
                }
                "MOONKIN" -> {
                    val moonfireDmg = max(4, (s.willpower * 0.8 + s.playerStats.atk * 0.3).roundToInt())
                    s = s.copy(monsterHp = max(0, s.monsterHp - moonfireDmg))
                    lines += "🦉 Celestial Moonfire burns the ${s.monster.name} for $moonfireDmg arcane damage!"
                }
                "AVATAR" -> {
                    val primalHeal = max(4, (s.playerStats.maxHp * 0.04).roundToInt())
                    val primalDmg = max(6, (s.playerStats.atk * 0.4 + s.willpower * 0.5).roundToInt())
                    s = s.copy(
                        playerHp = min(s.playerStats.maxHp, s.playerHp + primalHeal),
                        monsterHp = max(0, s.monsterHp - primalDmg)
                    )
                    lines += "🦅 Primal Avatar aura heals for $primalHeal HP and lashes out for $primalDmg nature damage!"
                }
            }
            if (s.monsterHp <= 0) {
                lines += "🏆 The ${s.monster.name} falls to nature's wrath! Victory!"
                return s.copy(
                    log = s.log + lines,
                    outcome = BattleOutcome.VICTORY
                )
            }
        }

        // Poison ticks at the end of the round
        if (s.playerPoisonTurns > 0) {
            val poison = max(3, s.monster.tier * 6 + s.monster.level / 2 - s.willpower / 3)
            s = s.copy(playerHp = max(0, s.playerHp - poison))
            lines += "☠️ Poison courses through you for $poison damage."
            if (s.playerHp <= 0) {
                lines += "The poison overwhelms you... train and return stronger!"
                return s.copy(log = s.log + lines, outcome = BattleOutcome.DEFEAT)
            }
        }

        // Tick down cooldowns and statuses for the next round
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
        val dodge = monsterDodgeChance(s)
        if (rng.nextInt(100) < dodge) {
            lines += "The ${s.monster.name} darts aside — your attack misses!"
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
            s.monster.trait == MonsterTrait.ARMORED -> "Your attack glances off the armor — $dmg damage."
            else -> "You attack the ${s.monster.name} for $dmg damage."
        }

        if (s.equippedTraits.contains(GearTrait.VAMPIRIC) && dmg > 0) {
            val leech = max(1, (dmg * 0.12).roundToInt())
            s = s.copy(playerHp = min(s.playerStats.maxHp, s.playerHp + leech))
            lines += "🩸 Vampiric: You leeched $leech HP from the strike!"
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
        val currentCd = s.skillCooldowns.getOrElse(index) { 0 }
        if (currentCd > 0) {
            lines += "${skill.name} is still recharging ($currentCd turns left)."
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
                    s = s.copy(atkBuffTurns = 4)
                    lines += "${skill.emoji} WAR CRY! Your muscles surge — +40% attack for 3 turns!"
                }
                2 -> {
                    val dmg = hit(atk * 3.0, 0.6, 20)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg), monsterStunTurns = 2)
                    lines += "${skill.emoji} TITAN SMASH! $dmg damage — the ${s.monster.name} reels, stunned!"
                }
                3 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(playerStats = base.copy(def = base.def + 35), playerDefending = true)
                    lines += "${skill.emoji} IRON BASTION! Impenetrable guard raised! Defense +35."
                }
                4 -> {
                    val dmg = hit(atk * 3.5 + s.strength * 2.0, 0.2, 25, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} COLOSSUS CLEAVE! A colossal armor-shattering sweep for $dmg damage!"
                }
                else -> {
                    val dmg = hit(atk * 4.5 + s.strength * 3.0, 0.1, 40, piercing = true)
                    s = s.copy(
                        monsterHp = max(0, s.monsterHp - dmg),
                        atkBuffTurns = 5,
                        playerHp = min(s.playerStats.maxHp, s.playerHp + (s.playerStats.maxHp * 0.3).roundToInt())
                    )
                    lines += "${skill.emoji} AVATAR OF WAR! You awaken the Titan God within for $dmg catastrophic damage!"
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
                    lines += "${skill.emoji} FROST LANCE! $dmg damage — the ${s.monster.name} is chilled!"
                }
                2 -> {
                    val dmg = hit(atk * 1.5 + s.willpower * 4.0, 0.0, 15, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} METEOR! The sky falls on the ${s.monster.name} for $dmg damage!"
                }
                3 -> {
                    val dmg = hit(atk * 1.6 + s.willpower * 3.2, 0.1, 25, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg), monsterStunTurns = 1)
                    lines += "${skill.emoji} CHAIN LIGHTNING! Electric forks incinerate for $dmg damage and shock the foe!"
                }
                4 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        playerStats = base.copy(spd = base.spd + 50),
                        skillCooldowns = List(s.skillCooldowns.size) { 0 }
                    )
                    lines += "${skill.emoji} TIME WARP! Time bends to your will! All other cooldowns reset and +50 SPD!"
                }
                else -> {
                    val dmg = hit(atk * 2.0 + s.willpower * 6.5, 0.0, 30, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} SUPERNOVA! A stellar cataclysm disintegrates the ${s.monster.name} for $dmg damage!"
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
                    lines += "${skill.emoji} SMOKE BOMB! $dmg damage — you vanish into the haze!"
                }
                2 -> {
                    val executing = s.monsterHp < s.monster.hp * 0.35
                    val mult = if (executing) 3.5 else 2.2
                    val dmg = hit(atk * mult + s.agility * 1.5, 0.6, 100)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} ASSASSINATE! Lethal strike for $dmg damage!"
                }
                3 -> {
                    val dmg = hit(atk * 1.4 + s.agility * 1.2, 0.3, 30)
                    val poison = max(4, (atk * 0.35).roundToInt())
                    s = s.copy(
                        monsterHp = max(0, s.monsterHp - dmg),
                        monsterBleedTurns = 4,
                        monsterBleedDmg = poison
                    )
                    lines += "${skill.emoji} POISONED BLADE! $dmg damage — venom inflicts $poison poison each turn!"
                }
                4 -> {
                    val d1 = hit(atk * 0.9 + s.agility * 0.5, 0.5, 100)
                    val d2 = hit(atk * 0.9 + s.agility * 0.5, 0.5, 100)
                    val d3 = hit(atk * 0.9 + s.agility * 0.5, 0.5, 100)
                    val d4 = hit(atk * 0.9 + s.agility * 0.5, 0.5, 100)
                    val total = d1 + d2 + d3 + d4
                    s = s.copy(monsterHp = max(0, s.monsterHp - total))
                    lines += "${skill.emoji} PHANTOM DANCE! 4 critical strikes land in an instant for $total damage!"
                }
                else -> {
                    val dmg = hit(atk * 5.2 + s.agility * 3.5, 0.1, 100, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg), playerVanished = true)
                    lines += "${skill.emoji} DEATH'S EMBRACE! You reap the enemy's life force for $dmg piercing damage and vanish!"
                }
            }
            CharacterClass.RANGER -> when (index) {
                0 -> {
                    val dmg = hit(atk * 1.8 + s.agility * 0.6, 0.0, 15, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} FLANKING SHOT! Arrow and companion strike in tandem for $dmg piercing damage!"
                }
                1 -> {
                    val bleed = max(3, (s.playerStats.atk * 0.3).roundToInt())
                    s = s.copy(
                        atkBuffTurns = 3,
                        monsterBleedTurns = 3,
                        monsterBleedDmg = bleed
                    )
                    lines += "${skill.emoji} PRIMAL HOWL! Companion howls! Crit boosted and ${s.monster.name} is bleeding!"
                }
                2 -> {
                    val executing = s.monsterHp < s.monster.hp * 0.4
                    val mult = if (executing) 3.6 else 2.4
                    val dmg = hit(atk * mult + s.agility * 1.2, 0.3, 30)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} BESTIAL WRATH! Coordinated frenzy deals $dmg execute damage!"
                }
                3 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        atkBuffTurns = 3,
                        playerStats = base.copy(spd = base.spd + 30, critPercent = min(80, base.critPercent + 35))
                    )
                    lines += "${skill.emoji} EAGLE EYE! Predatory focus locked! +30 SPD & massive critical boost!"
                }
                4 -> {
                    val heal = (s.playerStats.maxHp * 0.25).roundToInt()
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        playerHp = min(s.playerStats.maxHp, s.playerHp + heal),
                        playerStats = base.copy(def = base.def + 25)
                    )
                    lines += "${skill.emoji} SPIRIT BOND! Companion bond restores $heal HP and strengthens Defense +25!"
                }
                else -> {
                    val dmg = hit(atk * 4.2 + s.agility * 2.0 + s.strength * 1.5, 0.0, 35, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} PRIMAL STAMPEDE! Spirits of the wild trample the ${s.monster.name} for $dmg massive damage!"
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
                2 -> {
                    val dmg = hit(atk * 2.2 + s.willpower, 0.2, 15, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg), monsterStunTurns = 2)
                    lines += "${skill.emoji} JUDGMENT! $dmg piercing damage — the ${s.monster.name} is stunned!"
                }
                3 -> {
                    val dmg = hit(atk * 1.6 + s.willpower * 1.5, 0.2, 15, piercing = true)
                    val heal = (s.playerStats.maxHp * 0.12).roundToInt()
                    s = s.copy(
                        monsterHp = max(0, s.monsterHp - dmg),
                        playerHp = min(s.playerStats.maxHp, s.playerHp + heal)
                    )
                    lines += "${skill.emoji} CONSECRATION! Holy ground burns for $dmg damage and heals you for $heal HP!"
                }
                4 -> {
                    val heal = (s.playerStats.maxHp * 0.45).roundToInt()
                    s = s.copy(playerHp = min(s.playerStats.maxHp, s.playerHp + heal), playerPoisonTurns = 0)
                    lines += "${skill.emoji} LAY ON HANDS! Divine light restores $heal HP and cleanses debuffs!"
                }
                else -> {
                    val dmg = hit(atk * 4.2 + s.willpower * 3.5, 0.0, 30, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg), playerDefending = true)
                    lines += "${skill.emoji} AEGIS OF HEAVENS! Celestial armor deflects attacks and smites for $dmg damage!"
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
                    lines += "${skill.emoji} DRAIN LIFE! $dmg damage siphoned — you recover $heal HP!"
                }
                1 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        druidForm = "SKELETON",
                        playerStats = base.copy(def = base.def + 15)
                    )
                    lines += "${skill.emoji} RAISE SKELETON! A skeleton warrior rises! Defense +15."
                    val dmg = hit(atk * 1.5, 0.5, 10)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "The skeleton strikes for $dmg damage!"
                }
                2 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        druidForm = "ARMY",
                        playerStats = base.copy(
                            atk = (base.atk * 1.25).roundToInt(),
                            def = base.def + 20
                        )
                    )
                    lines += "${skill.emoji} ARMY OF THE DEAD! Legion of undead swarms the field!"
                    val dmg = hit(atk * 2.2 + s.willpower * 1.5, 0.1, 15, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "The horde tramples for $dmg piercing damage!"
                }
                3 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(playerStats = base.copy(def = base.def + 30))
                    lines += "${skill.emoji} BONE ARMOR! Whirling bone shards grant +30 DEF and protect your soul!"
                }
                4 -> {
                    val dmg = hit(atk * 2.5 + s.willpower * 3.5, 0.1, 20, piercing = true)
                    val heal = max(1, (dmg * 0.4).roundToInt())
                    s = s.copy(
                        monsterHp = max(0, s.monsterHp - dmg),
                        playerHp = min(s.playerStats.maxHp, s.playerHp + heal)
                    )
                    lines += "${skill.emoji} SOUL SIPHON! Ripped enemy soul for $dmg damage (recovered $heal HP)!"
                }
                else -> {
                    val dmg = hit(atk * 3.5 + s.willpower * 5.5, 0.0, 30, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} LORD OF THE DEAD! Arch-Lich unleashes necrotic annihilation for $dmg damage!"
                }
            }
            CharacterClass.WHITE_MAGE -> when (index) {
                0 -> {
                    val dmg = hit(s.willpower * 2.0, 0.3, 10)
                    val heal = max(4, (s.playerStats.maxHp * 0.15).roundToInt())
                    s = s.copy(
                        monsterHp = max(0, s.monsterHp - dmg),
                        playerHp = min(s.playerStats.maxHp, s.playerHp + heal)
                    )
                    lines += "${skill.emoji} HOLY LIGHT! Smited for $dmg damage and healed for $heal HP!"
                }
                1 -> {
                    s = s.copy(playerDefending = true, playerPoisonTurns = 0)
                    lines += "${skill.emoji} DIVINE WARD! Damage absorbed and poisons cleansed!"
                }
                2 -> {
                    val heal = (s.playerStats.maxHp * 0.5).roundToInt()
                    s = s.copy(playerHp = min(s.playerStats.maxHp, s.playerHp + heal))
                    lines += "${skill.emoji} BENEDICTION! Massive healing aura restores $heal HP!"
                }
                3 -> {
                    val heal = (s.playerStats.maxHp * 0.35).roundToInt()
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        playerHp = min(s.playerStats.maxHp, s.playerHp + heal),
                        playerStats = base.copy(spd = base.spd + 30)
                    )
                    lines += "${skill.emoji} SERAPH GRACE! Angelic wings heal for $heal HP and grant +30 SPD!"
                }
                4 -> {
                    val dmg = hit(s.willpower * 4.2 + atk * 1.5, 0.1, 20, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} SMITE! Blinding sacred pillar deals $dmg piercing damage!"
                }
                else -> {
                    val dmg = hit(s.willpower * 6.0, 0.0, 30, piercing = true)
                    s = s.copy(
                        monsterHp = max(0, s.monsterHp - dmg),
                        playerHp = s.playerStats.maxHp
                    )
                    lines += "${skill.emoji} APOTHEOSIS! Sacred transcendence! Full HP restored and smote for $dmg damage!"
                }
            }
            CharacterClass.MONK -> when (index) {
                0 -> {
                    val dmg = hit(atk * 1.4 + s.agility * 0.8, 0.5, 20)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} PALM STRIKE! Swift chi strike for $dmg damage!"
                }
                1 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(playerStats = base.copy(critPercent = min(80, base.critPercent + 40)))
                    lines += "${skill.emoji} INNER FOCUS! Critical strike chance doubled for next turns!"
                }
                2 -> {
                    var total = 0
                    repeat(5) { total += hit(atk * 0.5 + s.agility * 0.3, 0.5, s.playerStats.critPercent) }
                    s = s.copy(monsterHp = max(0, s.monsterHp - total))
                    lines += "${skill.emoji} FLURRY OF BLOWS! 5 rapid strikes land for $total damage!"
                }
                3 -> {
                    val dmg = hit(atk * 2.8 + s.agility * 2.2, 0.3, 25)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg), monsterStunTurns = 2)
                    lines += "${skill.emoji} DRAGON KICK! Flying chi kick crashes for $dmg damage and stuns the foe!"
                }
                4 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(playerStats = base.copy(def = base.def + 40))
                    lines += "${skill.emoji} DIAMOND BODY! Body hardened to diamond: +40 Defense!"
                }
                else -> {
                    var total = 0
                    repeat(8) { total += hit(atk * 0.6 + s.agility * 0.5, 0.1, 40, piercing = true) }
                    s = s.copy(monsterHp = max(0, s.monsterHp - total))
                    lines += "${skill.emoji} THOUSAND FISTS! 1,000 chi strikes shatter armor for $total piercing damage!"
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
                        playerHp = min(base.maxHp * 2, s.playerHp + base.maxHp)
                    )
                    lines += "${skill.emoji} BEAR FORM! Shapeshifted into a Dire Bear (+100% HP & +10 DEF)!"
                    val dmg = hit(s.playerStats.atk * buffMult(s) * 1.5, 0.4, 10)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "Bear swipe lands for $dmg damage!"
                }
                1 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        druidForm = "PANTHER",
                        playerStats = base.copy(
                            critPercent = min(base.critPercent + 50, 80),
                            spd = base.spd + 20,
                            atk = (base.atk * 1.1).roundToInt(),
                            maxHp = (base.maxHp * 0.7).roundToInt()
                        ),
                        playerHp = min(s.playerHp, (base.maxHp * 0.7).roundToInt())
                    )
                    lines += "${skill.emoji} PANTHER FORM! Shapeshifted into a Shadow Panther (+Crit & +Speed)!"
                    val dmg = hit(s.playerStats.atk * buffMult(s) * 1.4, 0.6, 50)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "Panther pounce strikes for $dmg damage!"
                }
                2 -> {
                    val dmg = hit(atk * 2.2 + s.willpower * 1.8, 0.3, 15)
                    val bleedDmg = max(3, (s.willpower * 0.6).roundToInt())
                    s = s.copy(
                        monsterHp = max(0, s.monsterHp - dmg),
                        monsterBleedTurns = 3,
                        monsterBleedDmg = bleedDmg
                    )
                    lines += "${skill.emoji} WRATH OF NATURE! Roots crush for $dmg damage and inflict 3 turns of bleeding!"
                }
                3 -> {
                    val base = s.baseStats ?: s.playerStats
                    val heal = (base.maxHp * 0.3).roundToInt()
                    s = s.copy(
                        druidForm = "TREANT",
                        playerHp = min((base.maxHp * 1.3).roundToInt(), s.playerHp + heal),
                        playerStats = base.copy(
                            maxHp = (base.maxHp * 1.3).roundToInt(),
                            def = base.def + 15,
                            atk = (base.atk * 0.85).roundToInt()
                        ),
                        monsterStunTurns = 1
                    )
                    lines += "${skill.emoji} TREANT FORM! Shapeshifted into an Ancient Treant! Roots entangle foe (+Thorns, +DEF)!"
                }
                4 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        druidForm = "MOONKIN",
                        playerStats = base.copy(
                            atk = (base.atk * 1.4).roundToInt(),
                            critPercent = min(base.critPercent + 15, 70)
                        )
                    )
                    val dmg = hit(atk * 1.8 + s.willpower * 3.6, 0.0, 25, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} MOONKIN FORM! Shapeshifted into Celestial Moonkin! Lunar beam blasts for $dmg piercing damage!"
                }
                else -> {
                    val base = s.baseStats ?: s.playerStats
                    val heal = (base.maxHp * 0.4).roundToInt()
                    s = s.copy(
                        druidForm = "AVATAR",
                        playerHp = min((base.maxHp * 1.5).roundToInt(), s.playerHp + heal),
                        playerStats = base.copy(
                            maxHp = (base.maxHp * 1.5).roundToInt(),
                            atk = (base.atk * 1.3).roundToInt(),
                            def = base.def + 15,
                            spd = base.spd + 20,
                            critPercent = min(base.critPercent + 25, 75)
                        ),
                        monsterStunTurns = 2
                    )
                    val dmg = hit(atk * 3.2 + s.willpower * 4.8, 0.1, 35, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} AVATAR OF THE WILDS! Ascended to Apex Primal Avatar! Primal storm unleashes $dmg damage and stuns for 2 turns!"
                }
            }
            CharacterClass.BERSERKER -> when (index) {
                0 -> {
                    val missingHpPercent = (s.playerStats.maxHp - s.playerHp).toDouble() / s.playerStats.maxHp
                    val mult = 1.6 + missingHpPercent * 2.0
                    val dmg = hit(atk * mult, 0.4, 20)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} FRENZY STRIKE! Rage fuels a strike for $dmg damage!"
                }
                1 -> {
                    val hpCost = max(1, (s.playerStats.maxHp * 0.1).roundToInt())
                    s = s.copy(
                        playerHp = max(1, s.playerHp - hpCost),
                        atkBuffTurns = 4
                    )
                    lines += "${skill.emoji} BLOOD RAGE! Sacrificed $hpCost HP — Attack surges by +60%!"
                }
                2 -> {
                    val dmg = hit(atk * 3.2, 0.4, 35)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} RAGNAROK! Devastating blow for $dmg damage!"
                }
                3 -> {
                    s = s.copy(atkBuffTurns = 4)
                    lines += "${skill.emoji} UNDYING WILL! Fury peaks — attack multiplies as health falls!"
                }
                4 -> {
                    val dmg = hit(atk * 4.2 + s.strength * 2.0, 0.3, 100)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} SKULL SPLITTER! Brutal overhead smash critical for $dmg damage!"
                }
                else -> {
                    val dmg = hit(atk * 5.0 + s.strength * 3.0, 0.1, 40, piercing = true)
                    val leech = max(1, (dmg * 0.3).roundToInt())
                    s = s.copy(
                        monsterHp = max(0, s.monsterHp - dmg),
                        playerHp = min(s.playerStats.maxHp, s.playerHp + leech),
                        atkBuffTurns = 5
                    )
                    lines += "${skill.emoji} GOD OF WRATH! Berserker ascension deals $dmg damage and leeches $leech HP!"
                }
            }
            CharacterClass.BARD -> when (index) {
                0 -> {
                    s = s.copy(atkBuffTurns = 4)
                    lines += "${skill.emoji} BATTLE ANTHEM! Uplifting music buffs Attack by +20%!"
                }
                1 -> {
                    val dmg = hit(atk * 1.5 + s.agility * 1.0, 0.4, 15)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg), monsterStunTurns = 1)
                    lines += "${skill.emoji} DISSONANCE! Sonic blast deals $dmg damage and stuns the foe!"
                }
                2 -> {
                    val heal = (s.playerStats.maxHp * 0.3).roundToInt()
                    s = s.copy(playerHp = min(s.playerStats.maxHp, s.playerHp + heal))
                    lines += "${skill.emoji} HYMN OF VALOR! Song of courage restores $heal HP!"
                }
                3 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(playerStats = base.copy(spd = base.spd + 50))
                    lines += "${skill.emoji} SONG OF SPEED! Blinding rhythm grants +50 SPD!"
                }
                4 -> {
                    val dmg = hit(atk * 2.5 + s.agility * 2.0, 0.1, 20, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg), monsterChillTurns = 3)
                    lines += "${skill.emoji} REQUIEM! Haunting chords deal $dmg piercing damage and weaken enemy attacks!"
                }
                else -> {
                    val dmg = hit(atk * 4.0 + s.agility * 3.0, 0.0, 30, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg), atkBuffTurns = 5)
                    lines += "${skill.emoji} SYMPHONY OF SPHERES! Celestial symphony deals $dmg damage and empowers you!"
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
                    lines += "${skill.emoji} SUMMON IFRIT! Primal of Fire scorches Attack to +50%!"
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
                    lines += "${skill.emoji} SUMMON SHIVA! Primal of Ice grants +40 SPD and +20% Crit!"
                }
                2 -> {
                    val dmg = hit(atk * 2.5 + s.willpower * 2.0, 0.5, 25, true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} ASTRAL FLOW! Primal unleashes ultimate attack for $dmg damage!"
                }
                3 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        druidForm = "TITAN",
                        playerStats = base.copy(def = base.def + 50)
                    )
                    lines += "${skill.emoji} SUMMON TITAN! Primal of Earth fortifies Defense +50!"
                }
                4 -> {
                    val dmg = hit(atk * 3.5 + s.willpower * 4.0, 0.0, 30, piercing = true)
                    s = s.copy(druidForm = "BAHAMUT", monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} SUMMON BAHAMUT! Megaflare incinerates for $dmg piercing damage!"
                }
                else -> {
                    val dmg = hit(atk * 4.5 + s.willpower * 5.0, 0.0, 40, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg), atkBuffTurns = 5)
                    lines += "${skill.emoji} ASTRAL FUSION! Complete primal fusion deals $dmg cosmic damage!"
                }
            }
            CharacterClass.DRAGOON -> when (index) {
                0 -> {
                    val base = s.baseStats ?: s.playerStats
                    s = s.copy(
                        druidForm = "WYVERN",
                        playerStats = base.copy(def = base.def + 15)
                    )
                    lines += "${skill.emoji} CALL WYVERN! Loyal wyvern takes flight! Defense +15 and constant healing!"
                }
                1 -> {
                    val dmg = hit(atk * 2.5, 0.5, 20, true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} JUMP! You leap into clouds and crash down for $dmg damage!"
                }
                2 -> {
                    val dmg = hit(atk * 3.5, 0.4, 40)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} DRAGON DIVE! Legendary aerial strike burns for $dmg damage!"
                }
                3 -> {
                    val dmg = hit(atk * 2.8 + s.strength * 1.5, 0.2, 20)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg), monsterChillTurns = 2)
                    lines += "${skill.emoji} WYVERN BREATH! Wyvern breathes flame for $dmg damage, shredding defense!"
                }
                4 -> {
                    val dmg = hit(atk * 4.0 + s.strength * 2.5, 0.3, 35, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg), playerVanished = true)
                    lines += "${skill.emoji} HIGH WIND! Soared above the battlefield and crashed down for $dmg damage!"
                }
                else -> {
                    val dmg = hit(atk * 5.2 + s.strength * 4.0, 0.0, 40, piercing = true)
                    s = s.copy(monsterHp = max(0, s.monsterHp - dmg))
                    lines += "${skill.emoji} STARDIVER! Cataclysmic celestial dive pierces for $dmg damage!"
                }
            }
        }

        return s.copy(
            skillCooldowns = s.skillCooldowns.mapIndexed { i, cd ->
                if (i == index) skill.cooldown + 1 else cd
            }
        )
    }

    // ---- Monster offense ----

    private fun monsterAttack(state: BattleState, lines: MutableList<String>, rng: Random): BattleState {
        var s = state
        if (s.playerVanished) {
            lines += "The ${s.monster.name} lashes out at empty smoke — you're untouchable!"
            return s.copy(playerVanished = false)
        }
        val dodge = playerDodgeChance(s)
        if (rng.nextInt(100) < dodge) {
            lines += "💨 You sidestep the ${s.monster.name}'s attack!"
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
        var dmg = if (s.playerDefending) max(1, (raw * 0.45).roundToInt()) else raw
        if (crit && s.equippedTraits.contains(GearTrait.WARDED)) {
            dmg = max(1, (dmg * 0.75).roundToInt())
        }
        if (s.playerStats.mitigationPercent > 0f) {
            dmg = max(1, (dmg * (1f - s.playerStats.mitigationPercent)).roundToInt())
        }
        s = s.copy(playerHp = max(0, s.playerHp - dmg))
        lines += when {
            s.playerDefending -> "The ${s.monster.name} attacks, but your guard absorbs it. $dmg damage."
            enraged -> "😡 The ${s.monster.name} attacks in a frenzy! $dmg damage!"
            crit && s.equippedTraits.contains(GearTrait.WARDED) -> "🛡️ Warded! You blunted a critical blow to $dmg damage."
            crit -> "The ${s.monster.name} lands a brutal blow! $dmg damage!"
            s.monsterChillTurns > 0 -> "The chilled ${s.monster.name} strikes sluggishly for $dmg damage."
            else -> "The ${s.monster.name} hits you for $dmg damage."
        }

        if (s.equippedTraits.contains(GearTrait.THORNS) && dmg > 0) {
            val reflect = max(1, (dmg * 0.15).roundToInt())
            s = s.copy(monsterHp = max(0, s.monsterHp - reflect))
            lines += "🌵 Thorns: Reflected $reflect damage back to the ${s.monster.name}!"
        }

        if (s.monster.trait == MonsterTrait.VENOMOUS && s.playerPoisonTurns == 0 && rng.nextInt(100) < 40) {
            val turns = max(2, 4 - s.willpower / 12)
            s = s.copy(playerPoisonTurns = turns)
            lines += "☠️ Venom seeps into your veins — poisoned for $turns turns!"
        }
        return s
    }

    // ---- Shared math ----

    private fun buffMult(s: BattleState): Double {
        var mult = if (s.atkBuffTurns > 0) 1.4 else 1.0
        if (s.equippedTraits.contains(GearTrait.BERSERK) && s.playerHp < (s.playerStats.maxHp * 0.5)) {
            mult *= 1.2
        }
        if (s.equippedTraits.contains(GearTrait.EXECUTIONER) && s.monsterHp < (s.monster.hp * 0.3)) {
            mult *= 1.25
        }
        return mult
    }

    private fun monsterDodgeChance(s: BattleState): Int {
        val base = ((s.monster.spd - s.playerStats.spd) * 1.5).roundToInt().coerceIn(0, 25)
        val swift = if (s.monster.trait == MonsterTrait.SWIFT) 8 else 0
        return base + swift
    }

    private fun playerDodgeChance(s: BattleState): Int {
        val traitDodge = if (s.equippedTraits.contains(GearTrait.SWIFTFOOT)) 10 else 0
        return (((s.playerStats.spd - s.monster.spd) * 1.5).roundToInt() + traitDodge).coerceIn(0, 45)
    }

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
        val mitigation = (softDef / (softDef + 50.0)).coerceIn(0.0, 0.70)
        val dmg = max(1, (atk * (1.0 - mitigation) * variance * critMult * armorMult).roundToInt())
        return dmg to crit
    }
}
