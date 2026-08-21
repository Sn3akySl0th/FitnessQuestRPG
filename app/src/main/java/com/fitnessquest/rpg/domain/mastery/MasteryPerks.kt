package com.fitnessquest.rpg.domain.mastery

import com.fitnessquest.rpg.data.db.MovementMasteryEntity
import com.fitnessquest.rpg.domain.MasteryStatBonus

data class MasteryPerk(
    val level: Int,
    val name: String,
    val description: String,
    val bonus: MasteryStatBonus
)

/**
 * Registry of passive perks unlocked at specific mastery levels.
 */
object MasteryPerks {

    val map: Map<String, List<MasteryPerk>> = mapOf(
        "SQUAT" to listOf(
            MasteryPerk(10, "Sturdy Stance", "+5 DEF", MasteryStatBonus(flatDef = 5)),
            MasteryPerk(25, "Iron Glutes", "+15 DEF", MasteryStatBonus(flatDef = 15)),
            MasteryPerk(50, "Titan's Pillar", "+30 DEF, +5% Mitigation", MasteryStatBonus(flatDef = 30, mitigationPercent = 0.05f))
        ),
        "BENCH_PRESS" to listOf(
            MasteryPerk(10, "Pectoral Power", "+5 ATK", MasteryStatBonus(flatAtk = 5)),
            MasteryPerk(25, "Titan's Push", "+15 ATK", MasteryStatBonus(flatAtk = 15)),
            MasteryPerk(50, "Colossus Force", "+30 ATK, +5% Crit", MasteryStatBonus(flatAtk = 30, flatCritPercent = 5))
        ),
        "DEADLIFT" to listOf(
            MasteryPerk(10, "Backbone", "+15 Max HP", MasteryStatBonus(flatMaxHp = 15)),
            MasteryPerk(25, "Unbreakable", "+40 Max HP", MasteryStatBonus(flatMaxHp = 40)),
            MasteryPerk(50, "World Lifter", "+100 Max HP, +5% Siphon", MasteryStatBonus(flatMaxHp = 100, siphonBonusPercent = 0.05f))
        ),
        "OVERHEAD_PRESS" to listOf(
            MasteryPerk(10, "Shoulder Stability", "+3 ATK, +3 DEF", MasteryStatBonus(flatAtk = 3, flatDef = 3)),
            MasteryPerk(25, "Sky Reach", "+8 ATK, +8 DEF", MasteryStatBonus(flatAtk = 8, flatDef = 8)),
            MasteryPerk(50, "Atlas Might", "+20 ATK, +20 DEF", MasteryStatBonus(flatAtk = 20, flatDef = 20))
        ),
        "PULL_UP" to listOf(
            MasteryPerk(10, "Eagle Grip", "+4 ATK, +2 SPD", MasteryStatBonus(flatAtk = 4, flatSpd = 2)),
            MasteryPerk(25, "Lat Wings", "+10 ATK, +5 SPD", MasteryStatBonus(flatAtk = 10, flatSpd = 5)),
            MasteryPerk(50, "Sky Hook", "+25 ATK, +10 SPD", MasteryStatBonus(flatAtk = 25, flatSpd = 10))
        ),
        "DIP_PUSHUP" to listOf(
            MasteryPerk(10, "Calisthenic Base", "+3 ATK, +3 DEF", MasteryStatBonus(flatAtk = 3, flatDef = 3)),
            MasteryPerk(25, "Push King", "+10 ATK, +10 DEF", MasteryStatBonus(flatAtk = 10, flatDef = 10)),
            MasteryPerk(50, "Unyielding", "+20 ATK, +20 DEF, +5% Mitigation", MasteryStatBonus(flatAtk = 20, flatDef = 20, mitigationPercent = 0.05f))
        ),
        "CORE_PLANK" to listOf(
            MasteryPerk(10, "Core Wall", "+10 Max HP, +3 DEF", MasteryStatBonus(flatMaxHp = 10, flatDef = 3)),
            MasteryPerk(25, "Steel Trunk", "+25 Max HP, +8 DEF", MasteryStatBonus(flatMaxHp = 25, flatDef = 8)),
            MasteryPerk(50, "Absolute Stability", "+60 Max HP, +20 DEF", MasteryStatBonus(flatMaxHp = 60, flatDef = 20))
        ),
        "RUNNING" to listOf(
            MasteryPerk(10, "Wind Walker", "+4 SPD", MasteryStatBonus(flatSpd = 4)),
            MasteryPerk(25, "Marathoner", "+10 SPD, +5 Max Energy", MasteryStatBonus(flatSpd = 10, flatMaxEnergy = 5)),
            MasteryPerk(50, "Flash Step", "+25 SPD, +15 Max Energy", MasteryStatBonus(flatSpd = 25, flatMaxEnergy = 15))
        ),
        "CYCLING" to listOf(
            MasteryPerk(10, "Cadence", "+3 SPD, +2 Max Energy", MasteryStatBonus(flatSpd = 3, flatMaxEnergy = 2)),
            MasteryPerk(25, "Leg Stamina", "+8 SPD, +6 Max Energy", MasteryStatBonus(flatSpd = 8, flatMaxEnergy = 6)),
            MasteryPerk(50, "Storm Pedal", "+20 SPD, +20 Max Energy", MasteryStatBonus(flatSpd = 20, flatMaxEnergy = 20))
        ),
        "WALKING_HIKING" to listOf(
            MasteryPerk(10, "Steady Pace", "+5 Max Energy", MasteryStatBonus(flatMaxEnergy = 5)),
            MasteryPerk(25, "Pathfinder", "+15 Max Energy", MasteryStatBonus(flatMaxEnergy = 15)),
            MasteryPerk(50, "World Trekker", "+40 Max Energy, +5% XP", MasteryStatBonus(flatMaxEnergy = 40, xpMultiplierBonus = 0.05f))
        ),
        "FLEXIBILITY" to listOf(
            MasteryPerk(10, "Lithe Body", "+3 SPD, +2% Crit", MasteryStatBonus(flatSpd = 3, flatCritPercent = 2)),
            MasteryPerk(25, "Flowing Form", "+8 SPD, +5% Crit", MasteryStatBonus(flatSpd = 8, flatCritPercent = 5)),
            MasteryPerk(50, "Perfect Alignment", "+20 SPD, +10% Crit", MasteryStatBonus(flatSpd = 20, flatCritPercent = 10))
        )
    )

    /**
     * Calculates the aggregate stat bonus from all unlocked perks across all masteries.
     */
    fun calculateTotalBonus(masteries: List<MovementMasteryEntity>): MasteryStatBonus {
        var flatAtk = 0
        var flatDef = 0
        var flatMaxHp = 0
        var flatSpd = 0
        var flatCritPercent = 0
        var mitigationPercent = 0f
        var siphonBonusPercent = 0f
        var xpMultiplierBonus = 0f
        var flatMaxEnergy = 0

        for (m in masteries) {
            val perks = map[m.canonicalKey] ?: continue
            for (p in perks) {
                if (m.level >= p.level) {
                    flatAtk += p.bonus.flatAtk
                    flatDef += p.bonus.flatDef
                    flatMaxHp += p.bonus.flatMaxHp
                    flatSpd += p.bonus.flatSpd
                    flatCritPercent += p.bonus.flatCritPercent
                    mitigationPercent += p.bonus.mitigationPercent
                    siphonBonusPercent += p.bonus.siphonBonusPercent
                    xpMultiplierBonus += p.bonus.xpMultiplierBonus
                    flatMaxEnergy += p.bonus.flatMaxEnergy
                }
            }
        }

        return MasteryStatBonus(
            flatAtk = flatAtk,
            flatDef = flatDef,
            flatMaxHp = flatMaxHp,
            flatSpd = flatSpd,
            flatCritPercent = flatCritPercent,
            mitigationPercent = mitigationPercent,
            siphonBonusPercent = siphonBonusPercent,
            xpMultiplierBonus = xpMultiplierBonus,
            flatMaxEnergy = flatMaxEnergy
        )
    }
}
