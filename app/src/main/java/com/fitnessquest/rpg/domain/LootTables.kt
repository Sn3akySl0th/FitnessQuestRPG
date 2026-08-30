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
        val traits: List<GearTrait> = emptyList(),
        val displayName: String? = null,
        val baseTypeId: String? = null,
        val itemLevel: Int = 0,
        val affixIds: List<String> = emptyList(),
        val setId: String? = null,
        val rolledAtk: Int? = null,
        val rolledDef: Int? = null,
        val rolledHp: Int? = null,
    ) : LootGrant() {
        val labelName: String
            get() = displayName?.takeIf { it.isNotBlank() }
                ?: if (rarity != GearRarity.COMMON) "${catalog.name} (${rarity.displayName})" else catalog.name
    }
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
        var totalGold = goldBonus
        var totalEnergy = energyBonus
        var totalXpBoost = xpBoostBonus
        val stackTotals = mutableMapOf<String, Int>()
        val gearLabels = mutableListOf<String>()

        fun extractGrant(g: LootGrant) {
            when (g) {
                is LootGrant.Gold -> totalGold += g.amount
                is LootGrant.Energy -> totalEnergy += g.amount
                is LootGrant.XpBoost -> totalXpBoost += g.amount
                is LootGrant.Stack -> {
                    val cur = stackTotals[g.catalog.name] ?: 0
                    stackTotals[g.catalog.name] = cur + g.quantity
                }
                is LootGrant.Gear -> {
                    gearLabels.add(g.labelName)
                }
                is LootGrant.ChestOpened -> {
                    gearLabels.add("${g.chest.name} opened!")
                    g.contents.forEach { extractGrant(it) }
                }
            }
        }

        grants.forEach { extractGrant(it) }

        if (totalGold > 0) add("+$totalGold gold")
        if (totalEnergy > 0) add("+$totalEnergy energy")
        if (totalXpBoost > 0) add("+$totalXpBoost XP boost")
        stackTotals.forEach { (name, qty) -> add("$name x$qty") }
        addAll(gearLabels)
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

    fun rollProceduralGear(
        character: CharacterEntity,
        itemLevel: Int,
        maxTier: Int,
        source: LootSource,
        gearPool: List<ItemEntity>,
        prCount: Int = 0,
        rng: Random = Random.Default,
    ): LootGrant.Gear? {
        val generated = ProceduralGearGenerator.generate(
            character = character,
            itemLevel = itemLevel,
            maxTier = maxTier,
            source = source,
            templatePool = gearPool,
            prCount = prCount,
            rng = rng,
        ) ?: return null
        return ProceduralGearGenerator.toLootGrant(generated)
    }

    fun rollRarity(
        source: LootSource,
        characterLevel: Int,
        prCount: Int = 0,
        workoutStreak: Int = 0,
        workedOutToday: Boolean = false,
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
                val streakBonus = (workoutStreak * 10).coerceAtMost(50) + (if (workedOutToday) 30 else 0)
                when {
                    characterLevel >= GearRarity.LEGENDARY.minLevelGate && roll < (25 + streakBonus / 2) -> GearRarity.LEGENDARY
                    characterLevel >= GearRarity.EPIC.minLevelGate && roll < (120 + streakBonus) -> GearRarity.EPIC
                    characterLevel >= GearRarity.RARE.minLevelGate && roll < (400 + streakBonus * 2) -> GearRarity.RARE
                    roll < (750 + streakBonus * 2) -> GearRarity.UNCOMMON
                    else -> GearRarity.COMMON
                }
            }
        }
    }

    fun rollBattleLoot(
        monster: Monster,
        character: CharacterEntity,
        gearPool: List<ItemEntity>,
        stackPool: List<ItemEntity>,
        rng: Random = Random.Default
    ): LootResult {
        val grants = mutableListOf<LootGrant>()
        val variantMult = monster.variant.rewardMult
        var gold = (monster.goldReward * 0.15 * variantMult).toInt().coerceAtLeast(2)

        // Chest chance scales with tier and variant (Epic guarantees chest)
        val chestChance = LootRates.battleChestChance(monster)
        val shouldGrantChest = monster.variant == MonsterVariant.EPIC || (rng.nextInt(100) < chestChance)

        if (shouldGrantChest) {
            val chest = stackPool.find { it.id == LootChests.BIOME }
                ?: stackPool.find { it.id == LootChests.WOODEN }
            if (chest != null) {
                grants += LootGrant.ChestOpened(
                    chest,
                    openChest(chest.tier, character, gearPool, stackPool, rng, monster.level),
                )
            }
        }

        // Guaranteed crafting material on Elite / Epic
        if (monster.variant != MonsterVariant.NORMAL) {
            val matQty = if (monster.variant == MonsterVariant.EPIC) 3 else 1
            pickStack(stackPool, ItemSlot.MATERIAL, monster.tier, rng)?.let {
                grants += LootGrant.Stack(it, matQty)
            }
        }

        // Direct gear / rune roll
        val gearRollChance = LootRates.battleGearChance(monster.variant)
        when (rng.nextInt(100)) {
            in 0 until gearRollChance -> {
                rollProceduralGear(
                    character = character,
                    itemLevel = monster.level.coerceAtLeast(1),
                    maxTier = monster.tier,
                    source = LootSource.BATTLE,
                    gearPool = gearPool,
                    rng = rng,
                )?.let { grants += it }
            }
            in gearRollChance until (gearRollChance + LootRates.BATTLE_RUNE_CHANCE) -> pickStack(stackPool, ItemSlot.RUNE, monster.tier, rng)?.let {
                grants += LootGrant.Stack(it)
            }
            else -> {
                if (monster.variant == MonsterVariant.NORMAL) {
                    pickStack(stackPool, ItemSlot.MATERIAL, monster.tier, rng)?.let {
                        grants += LootGrant.Stack(it, 1 + rng.nextInt(2))
                    } ?: run { gold += 5 + monster.tier * 4 }
                }
            }
        }
        return LootResult(grants = grants, goldBonus = gold)
    }

    fun rollWorkoutLoot(
        level: Int,
        setCount: Int,
        prCount: Int,
        character: CharacterEntity,
        gearPool: List<ItemEntity>,
        stackPool: List<ItemEntity>,
        maxTier: Int = (level / 5 + 1).coerceAtMost(4),
        rng: Random = Random.Default
    ): LootResult {
        val grants = mutableListOf<LootGrant>()
        var gold = 10 + setCount + prCount * 8
        var xpBoost = 0
        val stackTier = maxTier.coerceIn(1, ProgressionRules.MAX_GEAR_TIER)

        // Crafting materials scale with sets & PRs for Blacksmith Reforging
        val matCount = when {
            setCount >= 10 -> 3 + (if (prCount > 0) 1 else 0)
            setCount >= 6 -> 2 + (if (prCount > 0) 1 else 0)
            setCount >= 3 -> 1 + (if (prCount > 0) 1 else 0)
            prCount > 0 -> 1
            else -> 0
        }
        if (matCount > 0) {
            pickStack(stackPool, ItemSlot.MATERIAL, stackTier, rng)?.let {
                grants += LootGrant.Stack(it, matCount)
            }
        }

        if (prCount > 0 && rng.nextInt(100) < LootRates.WORKOUT_RUNE_ON_PR_CHANCE) {
            pickStack(stackPool, ItemSlot.RUNE, stackTier, rng)?.let {
                grants += LootGrant.Stack(it)
            }
        }

        // Direct gear drop with tactical traits for strong workouts
        if ((setCount >= 6 || prCount > 0) && rng.nextInt(100) < LootRates.WORKOUT_GEAR_CHANCE) {
            rollProceduralGear(
                character = character,
                itemLevel = level.coerceAtLeast(1),
                maxTier = stackTier,
                source = LootSource.WORKOUT,
                gearPool = gearPool,
                prCount = prCount,
                rng = rng,
            )?.let { grants += it }
        }

        if (setCount >= 8 && rng.nextInt(100) < LootRates.WORKOUT_WOODEN_CHEST_CHANCE) {
            stackPool.find { it.id == LootChests.WOODEN }?.let {
                grants += LootGrant.ChestOpened(it, openChest(1, character, gearPool, stackPool, rng, level))
            }
        }
        if (rng.nextInt(100) < 30) xpBoost = 20 + prCount * 15
        if (rng.nextInt(100) < 35) gold += 15 + level * 2
        return LootResult(grants = grants, goldBonus = gold, xpBoostBonus = xpBoost)
    }

    fun rollMomentLoot(
        trigger: MomentTrigger,
        level: Int,
        character: CharacterEntity,
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
                    in 25 until 45 -> rollProceduralGear(
                        character = character,
                        itemLevel = level.coerceAtLeast(1),
                        maxTier = tier,
                        source = LootSource.MOMENT,
                        gearPool = gearPool,
                        prCount = 1,
                        rng = rng,
                    )?.let { grants += it }
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
        character: CharacterEntity,
        gearPool: List<ItemEntity>,
        stackPool: List<ItemEntity>,
        maxTier: Int = (level / 3 + 1).coerceIn(1, 4),
        rng: Random = Random.Default
    ): LootResult {
        val tier = maxTier.coerceIn(1, ProgressionRules.MAX_GEAR_TIER)
        val grants = mutableListOf<LootGrant>()
        stackPool.find { it.id == LootChests.WAR_CACHE }?.let {
            grants += LootGrant.ChestOpened(it, openChest(tier.coerceAtLeast(2), character, gearPool, stackPool, rng, level))
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
        val targetTier = LootRates.biomeBossGearTier(biome)

        val grants = mutableListOf<LootGrant>()

        // 1. Guaranteed Unique Boss Signature Relic (Trophy)
        val bossRelicId = BossRelics.idFor(biome)
        val bossRelic = gearPool.find { it.id == bossRelicId }
        if (bossRelic != null) {
            val rarity = if (biome == Biome.SHADOWFEN) GearRarity.LEGENDARY else GearRarity.EPIC
            val traits = GearTrait.rollTraitsForRarity(rarity, rng)
            grants += LootGrant.Gear(bossRelic, rarity, traits)
        } else {
            rollProceduralGear(
                character = character,
                itemLevel = character.level.coerceAtLeast(1),
                maxTier = targetTier,
                source = LootSource.BOSS,
                gearPool = gearPool,
                rng = rng,
            )?.let { grants += it }
        }

        // 2. Guaranteed Biome Chest with contents
        val biomeChest = stackPool.find { it.id == LootChests.BIOME }
            ?: stackPool.find { it.id == LootChests.WAR_CACHE }
            ?: stackPool.find { it.id == LootChests.WOODEN }
        if (biomeChest != null) {
            grants += LootGrant.ChestOpened(biomeChest, openChest(targetTier, character, gearPool, stackPool, rng, character.level))
        }

        // 3. Guaranteed Crafting Materials
        val matSlots = stackPool.filter { it.slot == ItemSlot.MATERIAL && it.tier <= targetTier }
        matSlots.randomOrNull(rng)?.let {
            grants += LootGrant.Stack(it, 3)
        }

        return LootResult(
            grants = grants,
            goldBonus = 80 + (biome.ordinal + 1) * 45,
            energyBonus = 15,
            xpBoostBonus = 40 + (biome.ordinal + 1) * 20,
        )
    }

    /** Re-fighting a defeated biome boss awards elevated farming spoils. */
    fun rollBossFarmLoot(
        biome: Biome,
        character: CharacterEntity,
        gearPool: List<ItemEntity>,
        stackPool: List<ItemEntity>,
        rng: Random = Random.Default,
    ): LootResult {
        val targetTier = LootRates.biomeBossGearTier(biome)

        val grants = mutableListOf<LootGrant>()

        // 1. Elevated boss gear or relic roll
        val bossRelicId = BossRelics.idFor(biome)
        val rollRelic = rng.nextInt(100) < LootRates.BOSS_FARM_RELIC_CHANCE
        if (rollRelic) {
            gearPool.find { it.id == bossRelicId }?.let { relic ->
                val rarity = rollRarity(LootSource.BOSS, character.level, rng = rng)
                val traits = GearTrait.rollTraitsForRarity(rarity, rng)
                grants += LootGrant.Gear(relic, rarity, traits)
            }
        } else {
            rollProceduralGear(
                character = character,
                itemLevel = character.level.coerceAtLeast(1),
                maxTier = targetTier,
                source = LootSource.BOSS,
                gearPool = gearPool,
                rng = rng,
            )?.let { grants += it }
        }

        // 2. Guaranteed 2-3 Crafting Materials
        val matSlots = stackPool.filter { it.slot == ItemSlot.MATERIAL && it.tier <= targetTier }
        matSlots.randomOrNull(rng)?.let {
            grants += LootGrant.Stack(it, 2 + rng.nextInt(2))
        }

        // 3. 50% chance of Biome Chest
        if (rng.nextInt(100) < LootRates.BOSS_FARM_CHEST_CHANCE) {
            val chest = stackPool.find { it.id == LootChests.BIOME } ?: stackPool.find { it.id == LootChests.WOODEN }
            if (chest != null) {
                grants += LootGrant.ChestOpened(chest, openChest(targetTier, character, gearPool, stackPool, rng, character.level))
            }
        }

        return LootResult(
            grants = grants,
            goldBonus = 40 + (biome.ordinal + 1) * 25,
            energyBonus = 8,
            xpBoostBonus = 25 + (biome.ordinal + 1) * 15
        )
    }

    fun openChest(
        tier: Int,
        character: CharacterEntity,
        gearPool: List<ItemEntity>,
        stackPool: List<ItemEntity>,
        rng: Random = Random.Default,
        characterLevel: Int = tier * 3
    ): List<LootGrant> {
        val rolls = 2 + rng.nextInt(3) // 2-4
        return List(rolls) {
            when (rng.nextInt(100)) {
                in 0 until LootRates.CHEST_GEAR_CHANCE -> rollProceduralGear(
                    character = character,
                    itemLevel = characterLevel.coerceAtLeast(1),
                    maxTier = tier,
                    source = LootSource.CHEST,
                    gearPool = gearPool,
                    rng = rng,
                ) ?: LootGrant.Gold(8 + tier * 4)
                in LootRates.CHEST_GEAR_CHANCE until (LootRates.CHEST_GEAR_CHANCE + LootRates.CHEST_RUNE_CHANCE) ->
                    pickStack(stackPool, ItemSlot.RUNE, tier, rng)?.let { LootGrant.Stack(it) }
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
