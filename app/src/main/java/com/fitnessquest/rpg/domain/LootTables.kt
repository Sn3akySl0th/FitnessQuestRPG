package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.data.db.isEquippable
import kotlin.random.Random

/** What caused a mid-session loot roll. */
enum class MomentTrigger { PERSONAL_RECORD, HEAVY_LIFT, MILESTONE }

/** A single grant produced by a loot roll (before persistence). */
sealed class LootGrant {
    data class Gear(
        val catalog: ItemEntity,
        val rarity: GearRarity = GearRarity.COMMON,
        val traits: List<GearTrait> = emptyList()
    ) : LootGrant()
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
                is LootGrant.Gear -> {
                    if (g.rarity != GearRarity.COMMON) {
                        add("${g.catalog.name} (${g.rarity.displayName})")
                    } else {
                        add(g.catalog.name)
                    }
                }
                is LootGrant.Stack -> add("${g.catalog.name} x${g.quantity}")
                is LootGrant.Gold -> add("+${g.amount} gold")
                is LootGrant.Energy -> add("+${g.amount} energy")
                is LootGrant.XpBoost -> add("+${g.amount} XP boost")
                is LootGrant.ChestOpened -> {
                    add("${g.chest.name} opened!")
                    addAll(
                        g.contents.flatMap { nested ->
                            when (nested) {
                                is LootGrant.Gear -> {
                                    if (nested.rarity != GearRarity.COMMON) {
                                        listOf("${nested.catalog.name} (${nested.rarity.displayName})")
                                    } else {
                                        listOf(nested.catalog.name)
                                    }
                                }
                                is LootGrant.Stack -> listOf("${nested.catalog.name} x${nested.quantity}")
                                is LootGrant.Gold -> listOf("+${nested.amount} gold")
                                is LootGrant.Energy -> listOf("+${nested.amount} energy")
                                is LootGrant.XpBoost -> listOf("+${nested.amount} XP boost")
                                is LootGrant.ChestOpened -> emptyList()
                            }
                        }
                    )
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

    fun rollRarity(
        source: LootSource,
        characterLevel: Int,
        prCount: Int = 0,
        rng: Random = Random.Default
    ): GearRarity {
        val roll = rng.nextInt(1000)
        return when (source) {
            LootSource.WORKOUT, LootSource.BATTLE, LootSource.MOMENT -> {
                val prBonus = (prCount * 30).coerceAtMost(120)
                when {
                    characterLevel >= GearRarity.EPIC.minLevelGate && roll < (5 + prBonus / 10) -> GearRarity.EPIC
                    characterLevel >= GearRarity.RARE.minLevelGate && roll < (45 + prBonus) -> GearRarity.RARE
                    roll < (220 + prBonus) -> GearRarity.UNCOMMON
                    else -> GearRarity.COMMON
                }
            }
            // TODO: MYTHIC gameplay rollout deferred until endgame balance, drop rates, and content sources are designed.
            LootSource.BOSS, LootSource.CAMPAIGN, LootSource.GUILD_RAID -> {
                when {
                    characterLevel >= GearRarity.LEGENDARY.minLevelGate && roll < 60 -> GearRarity.LEGENDARY
                    characterLevel >= GearRarity.EPIC.minLevelGate && roll < 220 -> GearRarity.EPIC
                    characterLevel >= GearRarity.RARE.minLevelGate && roll < 600 -> GearRarity.RARE
                    else -> GearRarity.UNCOMMON
                }
            }
            LootSource.AMBUSH, LootSource.CHEST -> {
                when {
                    characterLevel >= GearRarity.LEGENDARY.minLevelGate && roll < 25 -> GearRarity.LEGENDARY
                    characterLevel >= GearRarity.EPIC.minLevelGate && roll < 120 -> GearRarity.EPIC
                    characterLevel >= GearRarity.RARE.minLevelGate && roll < 400 -> GearRarity.RARE
                    roll < 750 -> GearRarity.UNCOMMON
                    else -> GearRarity.COMMON
                }
            }
        }
    }

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
                grants += LootGrant.ChestOpened(chest, openChest(chest.tier, gearPool, stackPool, rng, monster.level))
                return LootResult(grants = grants, goldBonus = gold)
            }
        }
        // Direct gear / rune / material
        when (rng.nextInt(100)) {
            in 0 until 18 -> pickGear(gearPool, monster.tier, rng)?.let {
                val rarity = rollRarity(LootSource.BATTLE, monster.level, rng = rng)
                val traits = GearTrait.rollTraitsForRarity(rarity, rng)
                grants += LootGrant.Gear(it, rarity, traits)
            }
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
                grants += LootGrant.ChestOpened(it, openChest(1, gearPool, stackPool, rng, level))
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
                    in 25 until 45 -> pickGear(gearPool, tier, rng)?.let {
                        val rarity = rollRarity(LootSource.MOMENT, level, prCount = 1, rng = rng)
                        val traits = GearTrait.rollTraitsForRarity(rarity, rng)
                        grants += LootGrant.Gear(it, rarity, traits)
                    }
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
            grants += LootGrant.ChestOpened(it, openChest(tier.coerceAtLeast(2), gearPool, stackPool, rng, level))
        }
        return LootResult(
            grants = grants,
            goldBonus = 25 + level * 3,
            xpBoostBonus = 40 + level * 2,
            energyBonus = 5
        )
    }

    fun bossFirstClearLoot(
        biome: Biome,
        character: CharacterEntity,
        gearPool: List<ItemEntity>,
        stackPool: List<ItemEntity>,
        rng: Random = Random.Default,
    ): LootResult {
        val targetTier = when (biome) {
            Biome.MEADOWLANDS -> 2
            Biome.DARKWOOD, Biome.CRYSTAL_CAVES -> 3
            Biome.EMBER_PEAKS, Biome.FROZEN_WASTES -> 4
            Biome.SHADOWFEN -> 5
        }.coerceIn(1, ProgressionRules.MAX_GEAR_TIER)

        val grants = mutableListOf<LootGrant>()

        // 1. Guaranteed high-tier gear piece with elevated boss rarity
        val filteredGear = ProgressionRules.filterGearPool(gearPool, targetTier, character)
            .filter { it.tier == targetTier }
            .ifEmpty { ProgressionRules.filterGearPool(gearPool, targetTier, character) }
        val guaranteedGear = filteredGear.randomOrNull(rng) ?: pickGear(gearPool, targetTier, rng)
        if (guaranteedGear != null) {
            val rarity = rollRarity(LootSource.BOSS, character.level, rng = rng)
            val traits = GearTrait.rollTraitsForRarity(rarity, rng)
            grants += LootGrant.Gear(guaranteedGear, rarity, traits)
        }

        // 2. Guaranteed Biome Chest with contents
        val biomeChest = stackPool.find { it.id == LootChests.BIOME }
            ?: stackPool.find { it.id == LootChests.WAR_CACHE }
            ?: stackPool.find { it.id == LootChests.WOODEN }
        if (biomeChest != null) {
            grants += LootGrant.ChestOpened(biomeChest, openChest(targetTier, gearPool, stackPool, rng, character.level))
        }

        // 3. Guaranteed Crafting Materials
        val matSlots = stackPool.filter { it.slot == ItemSlot.MATERIAL && it.tier <= targetTier }
        matSlots.randomOrNull(rng)?.let {
            grants += LootGrant.Stack(it, 3)
        }

        return LootResult(
            grants = grants,
            goldBonus = 50 + (biome.ordinal + 1) * 35,
            energyBonus = 10,
            xpBoostBonus = 30 + (biome.ordinal + 1) * 15,
        )
    }

    fun openChest(
        tier: Int,
        gearPool: List<ItemEntity>,
        stackPool: List<ItemEntity>,
        rng: Random = Random.Default,
        characterLevel: Int = tier * 3
    ): List<LootGrant> {
        val rolls = 2 + rng.nextInt(3) // 2-4
        return List(rolls) {
            when (rng.nextInt(100)) {
                in 0 until 12 -> pickGear(gearPool, tier, rng)?.let {
                    val rarity = rollRarity(LootSource.CHEST, characterLevel, rng = rng)
                    val traits = GearTrait.rollTraitsForRarity(rarity, rng)
                    LootGrant.Gear(it, rarity, traits)
                } ?: LootGrant.Gold(8 + tier * 4)
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
