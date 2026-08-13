package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.ItemEntity

/** A single meaningful gain for the player. */
sealed class Reward {
    data class Xp(val amount: Int) : Reward()
    data class Gold(val amount: Int) : Reward()
    data class Energy(val amount: Int) : Reward()
    data class Gear(val item: ItemEntity) : Reward()
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
    is LootGrant.Gear -> Reward.Gear(this.catalog)
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
}

/** A group of rewards earned from a single event (workout, chest, boss). */
data class RewardBatch(
    val source: RewardSource,
    val rewards: List<Reward>,
    val timestamp: Long = System.currentTimeMillis()
)

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
