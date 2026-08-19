package com.fitnessquest.rpg.domain

import kotlin.math.roundToInt

/**
 * Meaningful rarity tiers for gear instances, driving stat multipliers,
 * visual rarity frames/glows, salvage material yield, and progression gates.
 */
enum class GearRarity(
    val displayName: String,
    val statMultiplier: Float,
    val colorHex: Long,
    val salvageBonus: Int,
    val minLevelGate: Int
) {
    COMMON("Common", 1.00f, 0xFF9E9E9E, 0, 1),
    UNCOMMON("Uncommon", 1.10f, 0xFF4CAF50, 1, 1),
    RARE("Rare", 1.25f, 0xFF2196F3, 2, 4),
    EPIC("Epic", 1.45f, 0xFF9C27B0, 3, 8),
    LEGENDARY("Legendary", 1.70f, 0xFFFF9800, 5, 12),
    MYTHIC("Mythic", 2.00f, 0xFFE53935, 8, 16);

    fun scaleStat(base: Int): Int {
        if (base <= 0) return 0
        if (this == COMMON) return base
        val scaled = (base * statMultiplier).roundToInt()
        // Guarantee at least a minimum improvement over base for higher rarities
        val minIncrease = when (this) {
            UNCOMMON -> 1
            RARE -> 2
            EPIC -> 3
            LEGENDARY -> 5
            MYTHIC -> 8
            COMMON -> 0
        }
        return maxOf(base + minIncrease, scaled)
    }

    val color: androidx.compose.ui.graphics.Color
        get() = androidx.compose.ui.graphics.Color(colorHex)

    companion object {
        fun fromName(name: String?): GearRarity {
            if (name.isNullOrBlank()) return COMMON
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: COMMON
        }
    }
}
