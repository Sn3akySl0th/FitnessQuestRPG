package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.ItemEntity

/** A single meaningful gain for the player. */
sealed class Reward {
    data class Xp(val amount: Int) : Reward()
    data class Gold(val amount: Int) : Reward()
    data class Energy(val amount: Int) : Reward()
    data class Gear(
        val item: ItemEntity,
        val rarity: GearRarity = GearRarity.COMMON,
        val traits: List<GearTrait> = emptyList(),
        val displayName: String? = null,
    ) : Reward()
    data class Stackable(val item: ItemEntity, val quantity: Int = 1) : Reward()
    data class LevelUp(val newLevel: Int) : Reward()
    data class NewPr(val pr: SessionPr) : Reward()
    data class BiomeUnlocked(val biomeName: String, val biomeLabel: String) : Reward()
    data class TitleUnlocked(val title: String) : Reward()
    data class SkillPoint(val amount: Int = 1) : Reward()
    data class XpBoost(val amount: Int) : Reward()
}

/** Converts technical loot grants into player-facing rewards. */
fun LootGrant.toReward(): Reward = when (this) {
    is LootGrant.Gear -> Reward.Gear(
        item = if (displayName != null) catalog.copy(name = displayName) else catalog,
        rarity = rarity,
        traits = traits,
        displayName = displayName,
    )
    is LootGrant.Stack -> Reward.Stackable(this.catalog, this.quantity)
    is LootGrant.Gold -> Reward.Gold(this.amount)
    is LootGrant.Energy -> Reward.Energy(this.amount)
    is LootGrant.XpBoost -> Reward.XpBoost(this.amount)
    is LootGrant.ChestOpened -> Reward.Stackable(this.chest, 1)
}

fun LootGrant.toRewards(): List<Reward> = when (this) {
    is LootGrant.ChestOpened -> this.contents.flatMap { it.toRewards() }
    else -> listOf(this.toReward())
}

fun LootResult.toRewards(): List<Reward> = buildList {
    if (goldBonus > 0) add(Reward.Gold(goldBonus))
    if (energyBonus > 0) add(Reward.Energy(energyBonus))
    if (xpBoostBonus > 0) add(Reward.XpBoost(xpBoostBonus))
    grants.forEach { grant -> addAll(grant.toRewards()) }
}.consolidate()

/** Combines duplicate gold, XP, energy, and stackable items into single consolidated rewards. */
fun List<Reward>.consolidate(): List<Reward> {
    var totalGold = 0
    var totalXp = 0
    var totalEnergy = 0
    var totalXpBoost = 0
    val stackables = mutableMapOf<Long, Pair<ItemEntity, Int>>()
    val others = mutableListOf<Reward>()

    for (reward in this) {
        when (reward) {
            is Reward.Gold -> totalGold += reward.amount
            is Reward.Xp -> totalXp += reward.amount
            is Reward.Energy -> totalEnergy += reward.amount
            is Reward.XpBoost -> totalXpBoost += reward.amount
            is Reward.Stackable -> {
                val current = stackables[reward.item.id]?.second ?: 0
                stackables[reward.item.id] = reward.item to (current + reward.quantity)
            }
            else -> others.add(reward)
        }
    }

    return buildList {
        if (totalXp > 0) add(Reward.Xp(totalXp))
        if (totalGold > 0) add(Reward.Gold(totalGold))
        if (totalEnergy > 0) add(Reward.Energy(totalEnergy))
        if (totalXpBoost > 0) add(Reward.XpBoost(totalXpBoost))
        stackables.values.forEach { (item, qty) -> add(Reward.Stackable(item, qty)) }
        addAll(others)
    }
}

/** A group of rewards earned from a single event (workout, chest, boss). */
data class RewardBatch(
    val source: RewardSource,
    val rewards: List<Reward>,
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor(source: RewardSource, rewards: List<Reward>) : this(
        source = source,
        rewards = rewards.consolidate(),
        timestamp = System.currentTimeMillis()
    )
}

enum class RewardSource {
    WORKOUT,
    BATTLE,
    CHEST_OPENING,
    OFFLINE_IDLE,
    FORGE,
    GUILD_RAID,
    CAMPAIGN_GOAL,
    ACHIEVEMENT,
    DATA_IMPORT,
    BOSS,
}
