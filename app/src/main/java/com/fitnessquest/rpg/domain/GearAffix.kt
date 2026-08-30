package com.fitnessquest.rpg.domain

import kotlin.math.roundToInt
import kotlin.random.Random

/** Rolled affix on a procedurally generated gear instance. */
data class GearAffix(
    val id: String,
    val word: String,
    val isPrefix: Boolean,
    val atk: Int = 0,
    val def: Int = 0,
    val hp: Int = 0,
) {
    fun scaled(itemLevel: Int): GearAffix {
        val scale = (1.0 + itemLevel.coerceAtLeast(1) * 0.04).coerceAtMost(2.5)
        return copy(
            atk = (atk * scale).roundToInt(),
            def = (def * scale).roundToInt(),
            hp = (hp * scale).roundToInt(),
        )
    }
}

object GearAffixPool {
    private val prefixes = listOf(
        GearAffix("pfx_savage", "Savage", isPrefix = true, atk = 2),
        GearAffix("pfx_keen", "Keen", isPrefix = true, atk = 1, def = 1),
        GearAffix("pfx_stalwart", "Stalwart", isPrefix = true, def = 2, hp = 4),
        GearAffix("pfx_vigorous", "Vigorous", isPrefix = true, hp = 8),
        GearAffix("pfx_honed", "Honed", isPrefix = true, atk = 3),
        GearAffix("pfx_guarded", "Guarded", isPrefix = true, def = 3),
        GearAffix("pfx_fleet", "Fleet", isPrefix = true, atk = 1, hp = 3),
    )

    private val suffixes = listOf(
        GearAffix("sfx_bear", "the Bear", isPrefix = false, atk = 2, hp = 6),
        GearAffix("sfx_wolf", "the Wolf", isPrefix = false, atk = 3),
        GearAffix("sfx_whale", "the Whale", isPrefix = false, hp = 12),
        GearAffix("sfx_guardian", "the Guardian", isPrefix = false, def = 4, hp = 4),
        GearAffix("sfx_slayer", "Slaughter", isPrefix = false, atk = 4),
        GearAffix("sfx_vigor", "Vigor", isPrefix = false, hp = 10),
        GearAffix("sfx_ward", "Warding", isPrefix = false, def = 5),
    )

    fun rollAffixes(rarity: GearRarity, itemLevel: Int, rng: Random = Random.Default): List<GearAffix> {
        val totalRolls = when (rarity) {
            GearRarity.COMMON -> 0
            GearRarity.UNCOMMON -> if (rng.nextBoolean()) 1 else 0
            GearRarity.RARE -> 1 + rng.nextInt(2)
            GearRarity.EPIC -> 2 + rng.nextInt(2)
            GearRarity.LEGENDARY -> 3 + rng.nextInt(2)
            GearRarity.MYTHIC -> 4 + rng.nextInt(2)
        }
        if (totalRolls <= 0) return emptyList()

        val rolled = mutableListOf<GearAffix>()
        repeat(totalRolls) { index ->
            val pool = if (index % 2 == 0) prefixes else suffixes
            val pick = pool.random(rng).scaled(itemLevel)
            if (rolled.none { it.id == pick.id }) rolled += pick
        }
        return rolled
    }

    fun composeDisplayName(baseName: String, affixes: List<GearAffix>): String {
        val prefixWords = affixes.filter { it.isPrefix }.joinToString(" ") { it.word }
        val suffixWords = affixes.filter { !it.isPrefix }.joinToString(" ") { it.word }
        return buildString {
            if (prefixWords.isNotBlank()) append("$prefixWords ")
            append(baseName)
            if (suffixWords.isNotBlank()) {
                if (suffixWords.startsWith("the ") || suffixWords == "Slaughter" || suffixWords == "Warding" || suffixWords == "Vigor") {
                    append(" of $suffixWords")
                } else {
                    append(" $suffixWords")
                }
            }
        }.trim()
    }
}
