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
        itemLevel: Int = 0,
        wideVariance: Boolean = false,
        rng: Random = Random.Default
    ): ProceduralStatResult {
        val levelScale = 1.0 + itemLevel.coerceAtLeast(0) * 0.03
        val baseAtk = (rarity.scaleStat(catalog.atk) * levelScale).roundToInt()
        val baseDef = (rarity.scaleStat(catalog.def) * levelScale).roundToInt()
        val baseHp = (rarity.scaleStat(catalog.hp) * levelScale).roundToInt()

        // Roll base stat variance — wider on procedural drops for Diablo-style spread.
        var rolledAtk = rollWithVariance(baseAtk, wideVariance, rng)
        var rolledDef = rollWithVariance(baseDef, wideVariance, rng)
        var rolledHp = rollWithVariance(baseHp, wideVariance, rng)

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

    private fun rollWithVariance(base: Int, wideVariance: Boolean, rng: Random): Int {
        if (base <= 0) return 0
        val factor = if (wideVariance) {
            rng.nextInt(70, 131) / 100.0
        } else {
            rng.nextInt(90, 116) / 100.0
        }
        return (base * factor).roundToInt().coerceAtLeast(if (base > 0) 1 else 0)
    }
}
