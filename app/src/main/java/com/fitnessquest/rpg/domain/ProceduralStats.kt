package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.ItemEntity
import kotlin.math.roundToInt
import kotlin.random.Random

data class ProceduralStatResult(
    val atk: Int,
    val def: Int,
    val hp: Int,
    val qualityPercent: Int
)

object ProceduralStatEngine {

    /**
     * Generates randomized procedural combat stats for a gear instance based on its
     * catalog template, rarity, and procedural roll variance + bonus affixes.
     */
    fun generateStats(
        catalog: ItemEntity,
        rarity: GearRarity = GearRarity.COMMON,
        rng: Random = Random.Default
    ): ProceduralStatResult {
        val baseAtk = rarity.scaleStat(catalog.atk)
        val baseDef = rarity.scaleStat(catalog.def)
        val baseHp = rarity.scaleStat(catalog.hp)

        // 1. Roll base stat variance (90% to 115% of rarity-scaled value)
        var rolledAtk = rollWithVariance(baseAtk, rng)
        var rolledDef = rollWithVariance(baseDef, rng)
        var rolledHp = rollWithVariance(baseHp, rng)

        // 2. Roll bonus procedural affixes based on rarity
        val bonusRolls = when (rarity) {
            GearRarity.COMMON -> 0
            GearRarity.UNCOMMON -> if (rng.nextBoolean()) 1 else 0
            GearRarity.RARE -> 1 + rng.nextInt(2) // 1-2
            GearRarity.EPIC -> 2 + rng.nextInt(2) // 2-3
            GearRarity.LEGENDARY -> 3 + rng.nextInt(2) // 3-4
            GearRarity.MYTHIC -> 4 + rng.nextInt(2) // 4-5
        }

        val primaryStats = mutableListOf<String>()
        if (catalog.atk > 0) primaryStats.add("ATK")
        if (catalog.def > 0) primaryStats.add("DEF")
        if (catalog.hp > 0) primaryStats.add("HP")
        if (primaryStats.isEmpty()) primaryStats.addAll(listOf("DEF", "HP"))

        repeat(bonusRolls) {
            val statToBoost = primaryStats.random(rng)
            when (statToBoost) {
                "ATK" -> rolledAtk += (1 + catalog.tier / 2).coerceAtLeast(1)
                "DEF" -> rolledDef += (1 + catalog.tier / 2).coerceAtLeast(1)
                "HP" -> rolledHp += (3 + catalog.tier * 2)
            }
        }

        // 3. Compute quality score (80% - 100%)
        val totalRolled = rolledAtk * 4 + rolledDef * 4 + rolledHp
        val totalBase = (baseAtk * 4 + baseDef * 4 + baseHp).coerceAtLeast(1)
        val qualityPercent = ((totalRolled.toDouble() / totalBase.toDouble()) * 85.0)
            .roundToInt()
            .coerceIn(75, 100)

        return ProceduralStatResult(
            atk = rolledAtk,
            def = rolledDef,
            hp = rolledHp,
            qualityPercent = qualityPercent
        )
    }

    private fun rollWithVariance(base: Int, rng: Random): Int {
        if (base <= 0) return 0
        // Variance factor between 0.90 and 1.15
        val factor = (rng.nextInt(90, 116)) / 100.0
        return (base * factor).roundToInt().coerceAtLeast(1)
    }
}
