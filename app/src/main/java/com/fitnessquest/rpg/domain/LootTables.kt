package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.data.db.isEquippable
import kotlin.random.Random

/** What caused a mid-session loot roll. */
enum class MomentTrigger { PERSONAL_RECORD, HEAVY_LIFT, MILESTONE }

/** A single grant produced by a loot roll (before persistence). */
sealed class LootGrant {
    data class Gear(val catalog: ItemEntity) : LootGrant()
    data class Stack(val catalog: ItemEntity, val quantity: Int = 1) : LootGrant()
    data class Gold(val amount: Int) : LootGrant()
    data class Energy(val amount: Int) : LootGrant()
    data class XpBoost(val amount: Int) : LootGrant()
    data class ChestOpened(val chest: ItemEntity, val contents: List<LootGrant>) : LootGrant()
}

data class LootResult(
    val grants: List<LootGrant> = emptyList(),
    val goldBonus: Int = 0,
    val energyBonus: Int = 0,
    val xpBoostBonus: Int = 0,
) {
    val isEmpty: Boolean get() = grants.isEmpty() && (goldBonus == 0) && (energyBonus == 0) && (xpBoostBonus == 0)

    fun labels(): List<String> = buildList {
        if (goldBonus > 0) add("+$goldBonus gold")
        if (energyBonus > 0) add("+$energyBonus energy")
        if (xpBoostBonus > 0) add("+$xpBoostBonus XP boost")
        grants.forEach { g ->
            when (g) {
                is LootGrant.Gear -> add(g.catalog.name)
                is LootGrant.Stack -> add("${g.catalog.name} x${g.quantity}")
                is LootGrant.Gold -> add("+${g.amount} gold")
                is LootGrant.Energy -> add("+${g.amount} energy")
                is LootGrant.XpBoost -> add("+${g.amount} XP boost")
                is LootGrant.ChestOpened -> {
                    add("${g.chest.name} opened!")
                    addAll(
                        g.contents.flatMap { nested ->
                            when (nested) {
                            is LootGrant.Gear -> listOf(nested.catalog.name)
                            is LootGrant.Stack -> listOf("${nested.catalog.name} x${nested.quantity}")
                            is LootGrant.Gold -> listOf("+${nested.amount} gold")
                            is LootGrant.Energy -> listOf("+${nested.amount} energy")
                            is LootGrant.XpBoost -> listOf("+${nested.amount} XP boost")
                            is LootGrant.ChestOpened -> emptyList()
                        }
                    })
                }
            }
        }
    }
}

object GearSockets {
    fun slotsForTier(tier: Int): Int = when {
        tier <= 1 -> 1
        else -> 2
    }
}

object Runes {
    const val POWER = 801L
    const val WARD = 802L
    const val VITAL = 803L
    const val SWIFT = 804L
    const val FURY = 805L
    const val SIPHON = 806L

    fun atkBonus(id: Long): Int = when (id) {
        POWER -> 3
        else -> 0
    }

    fun defBonus(id: Long): Int = when (id) {
        WARD -> 3
        else -> 0
    }

    fun hpBonus(id: Long): Int = when (id) {
        VITAL -> 12
        else -> 0
    }

    fun spdBonus(id: Long): Int = when (id) {
        SWIFT -> 4
        else -> 0
    }

    fun critBonus(id: Long): Int = when (id) {
        FURY -> 5
        else -> 0
    }

    fun hasSiphon(id: Long): Boolean = id == SIPHON
}

object Materials {
    const val SCRAP_IRON = 701L
    const val ARCANE_DUST = 702L
    const val BEAST_HIDE = 703L
    const val EMBER_COAL = 704L
}

object LootChests {
    const val WOODEN = 750L
    const val BIOME = 751L
    const val WAR_CACHE = 752L
}

object LootTables {

    fun rollBattleLoot(
        monster: Monster,
        gearPool: List<ItemEntity>,
        stackPool: List<ItemEntity>,
        rng: Random = Random.Default
    ): LootResult {
        val grants = mutableListOf<LootGrant>()
        var gold = (monster.goldReward * 0.15).toInt().coerceAtLeast(2)
        // Chest chance scales with tier.
        val chestChance = 8 + monster.tier * 4
        if (rng.nextInt(100) < chestChance) {
            val chest = stackPool.find { it.id == LootChests.BIOME }
                ?: stackPool.find { it.id == LootChests.WOODEN }
            if (chest != null) {
                grants += LootGrant.ChestOpened(chest, openChest(chest.tier, gearPool, stackPool, rng))
                return LootResult(grants = grants, goldBonus = gold)
            }
        }
        // Direct gear / rune / material
        when (rng.nextInt(100)) {
            in 0 until 18 -> pickGear(gearPool, monster.tier, rng)?.let { grants += LootGrant.Gear(it) }
            in 18 until 35 -> pickStack(stackPool, ItemSlot.RUNE, monster.tier, rng)?.let {
                grants += LootGrant.Stack(it)
            }
            in 35 until 70 -> pickStack(stackPool, ItemSlot.MATERIAL, monster.tier, rng)?.let {
                grants += LootGrant.Stack(it, 1 + rng.nextInt(2))
            }
            else -> gold += 5 + monster.tier * 4
        }
        return LootResult(grants = grants, goldBonus = gold)
    }

    fun rollWorkoutLoot(
        level: Int,
        setCount: Int,
        prCount: Int,
        gearPool: List<ItemEntity>,
        stackPool: List<ItemEntity>,
        maxTier: Int = (level / 5 + 1).coerceAtMost(4),
        rng: Random = Random.Default
    ): LootResult {
        val grants = mutableListOf<LootGrant>()
        var gold = 3 + setCount / 2 + prCount * 5
        var xpBoost = 0
        val stackTier = maxTier.coerceIn(1, ProgressionRules.MAX_GEAR_TIER)
        if (setCount >= 3) {
            pickStack(stackPool, ItemSlot.MATERIAL, stackTier, rng)?.let {
                grants += LootGrant.Stack(it)
            }
        }
        if (prCount > 0 && rng.nextInt(100) < 40) {
            pickStack(stackPool, ItemSlot.RUNE, stackTier, rng)?.let {
                grants += LootGrant.Stack(it)
            }
        }
        if (setCount >= 8 && rng.nextInt(100) < 20) {
            stackPool.find { it.id == LootChests.WOODEN }?.let {
                grants += LootGrant.ChestOpened(it, openChest(1, gearPool, stackPool, rng))
            }
        }
        if (rng.nextInt(100) < 25) xpBoost = 15 + prCount * 10
        if (rng.nextInt(100) < 30) gold += 8 + level
        return LootResult(grants = grants, goldBonus = gold, xpBoostBonus = xpBoost)
    }

    fun rollMomentLoot(
        trigger: MomentTrigger,
        level: Int,
        gearPool: List<ItemEntity>,
        stackPool: List<ItemEntity>,
        maxTier: Int = (level / 4 + 1).coerceIn(1, 4),
        rng: Random = Random.Default
    ): LootResult {
        val grants = mutableListOf<LootGrant>()
        var gold: Int
        var xpBoost = 0
        val tier = maxTier.coerceIn(1, ProgressionRules.MAX_GEAR_TIER)
        when (trigger) {
            MomentTrigger.PERSONAL_RECORD -> {
                gold = 10 + level
                xpBoost = 20
                when (rng.nextInt(100)) {
                    in 0 until 25 -> pickStack(stackPool, ItemSlot.RUNE, tier, rng)?.let {
                        grants += LootGrant.Stack(it)
                    }
                    in 25 until 45 -> pickGear(gearPool, tier, rng)?.let { grants += LootGrant.Gear(it) }
                    else -> pickStack(stackPool, ItemSlot.MATERIAL, tier, rng)?.let {
                        grants += LootGrant.Stack(it, 2)
                    }
                }
            }
            MomentTrigger.HEAVY_LIFT -> {
                gold = 5 + level / 2
                pickStack(stackPool, ItemSlot.MATERIAL, tier, rng)?.let {
                    grants += LootGrant.Stack(it)
                }
                if (rng.nextInt(100) < 20) {
                    pickStack(stackPool, ItemSlot.RUNE, tier, rng)?.let { grants += LootGrant.Stack(it) }
                }
            }
            MomentTrigger.MILESTONE -> {
                gold = 4 + level / 3
                var energy = 0
                pickStack(stackPool, ItemSlot.MATERIAL, tier, rng)?.let {
                    grants += LootGrant.Stack(it)
                }
                if (rng.nextInt(100) < 40) {
                    when (rng.nextInt(3)) {
                        0 -> gold += 8 + level // Gold Pouch
                        1 -> energy = 3 + tier // Energy Shard
                        else -> xpBoost = 12 + tier // Training Scroll
                    }
                }
                return LootResult(
                    grants = grants,
                    goldBonus = gold,
                    energyBonus = energy,
                    xpBoostBonus = xpBoost
                )
            }
        }
        return LootResult(grants = grants, goldBonus = gold, xpBoostBonus = xpBoost)
    }

    fun rollAmbushLoot(
        level: Int,
        gearPool: List<ItemEntity>,
        stackPool: List<ItemEntity>,
        maxTier: Int = (level / 3 + 1).coerceIn(1, 4),
        rng: Random = Random.Default
    ): LootResult {
        val tier = maxTier.coerceIn(1, ProgressionRules.MAX_GEAR_TIER)
        val grants = mutableListOf<LootGrant>()
        stackPool.find { it.id == LootChests.WAR_CACHE }?.let {
            grants += LootGrant.ChestOpened(it, openChest(tier.coerceAtLeast(2), gearPool, stackPool, rng))
        }
        return LootResult(
            grants = grants,
            goldBonus = 25 + level * 3,
            xpBoostBonus = 40 + level * 2,
            energyBonus = 5
        )
    }

    fun openChest(
        tier: Int,
        gearPool: List<ItemEntity>,
        stackPool: List<ItemEntity>,
        rng: Random = Random.Default
    ): List<LootGrant> {
        val rolls = 2 + rng.nextInt(3) // 2-4
        return List(rolls) {
            when (rng.nextInt(100)) {
                in 0 until 12 -> pickGear(gearPool, tier, rng)?.let { LootGrant.Gear(it) }
                    ?: LootGrant.Gold(8 + tier * 4)
                in 12 until 30 -> pickStack(stackPool, ItemSlot.RUNE, tier, rng)?.let { LootGrant.Stack(it) }
                    ?: LootGrant.Gold(6 + tier * 3)
                in 30 until 65 -> pickStack(stackPool, ItemSlot.MATERIAL, tier, rng)?.let {
                    LootGrant.Stack(it, 1 + rng.nextInt(3))
                } ?: LootGrant.Gold(5)
                in 65 until 80 -> LootGrant.Gold(10 + tier * 5)
                in 80 until 90 -> LootGrant.Energy(3 + tier)
                else -> LootGrant.XpBoost(10 + tier * 5)
            }
        }
    }

    private fun pickGear(pool: List<ItemEntity>, maxTier: Int, rng: Random): ItemEntity? {
        val candidates = pool.filter { it.slot.isEquippable() && it.tier <= maxTier }
        return candidates.randomOrNull(rng)
    }

    private fun pickStack(
        pool: List<ItemEntity>,
        slot: ItemSlot,
        maxTier: Int,
        rng: Random
    ): ItemEntity? {
        val candidates = pool.filter { it.slot == slot && it.tier <= maxTier }
        return candidates.randomOrNull(rng)
    }
}
