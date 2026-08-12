package com.fitnessquest.rpg.domain

import kotlin.random.Random

enum class BountyKind {
    STEPS,
    WATER,
    STRETCH,
    WORKOUT,
    BATTLE,
    XP
}

/**
 * Template for a daily micro-quest. [id] is stable for a given day+slot so claims
 * survive recomposition; the board itself rotates by calendar day.
 */
data class BountyDef(
    val id: String,
    val kind: BountyKind,
    val title: String,
    val rewardText: String,
    val rewardGold: Int = 0,
    val rewardXpBoost: Int = 0,
    val rewardEnergy: Int = 0,
    /** Steps / glasses / XP target when relevant. */
    val target: Int = 1
)

object DailyBountyPool {
    private val lifestyle = listOf(
        BountyDef("water_3", BountyKind.WATER, "Drink 3 glasses of water", "+15 Energy · +35 Gold", rewardGold = 35, rewardEnergy = 15, target = 3),
        BountyDef("water_5", BountyKind.WATER, "Drink 5 glasses of water", "+25 Energy · +55 Gold", rewardGold = 55, rewardEnergy = 25, target = 5),
        BountyDef("steps_2k", BountyKind.STEPS, "Walk 2,000 steps", "+10 Energy · +40 Gold", rewardGold = 40, rewardEnergy = 10, target = 2_000),
        BountyDef("steps_4k", BountyKind.STEPS, "Walk 4,000 steps", "+20 Energy · +60 Gold", rewardGold = 60, rewardEnergy = 20, target = 4_000),
        BountyDef("steps_6k", BountyKind.STEPS, "Walk 6,000 steps", "+30 Energy · +80 Gold", rewardGold = 80, rewardEnergy = 30, target = 6_000),
        BountyDef("stretch", BountyKind.STRETCH, "Do a 5-minute stretch or mobility break", "+20 Energy · +30 Gold", rewardGold = 30, rewardEnergy = 20),
        BountyDef("posture", BountyKind.STRETCH, "Stand up and move for 2 minutes", "+10 Energy · +25 Gold", rewardGold = 25, rewardEnergy = 10)
    )

    private val questing = listOf(
        BountyDef("workout", BountyKind.WORKOUT, "Complete a workout", "+50 Gold", rewardGold = 50),
        BountyDef("battle", BountyKind.BATTLE, "Slay a monster", "+40 Gold", rewardGold = 40),
        BountyDef("xp_150", BountyKind.XP, "Earn 150 XP from quests", "+80 XP next quest", rewardXpBoost = 80, target = 150),
        BountyDef("xp_250", BountyKind.XP, "Earn 250 XP from quests", "+100 XP next quest", rewardXpBoost = 100, target = 250)
    )

    /**
     * Three bounties for [epochDay]: always lifestyle-heavy so non-gym days still pay off.
     * Selection is deterministic for the day (same board all day, new mix tomorrow).
     */
    fun forDay(epochDay: Long): List<BountyDef> {
        val rng = Random(epochDay)
        val water = lifestyle.filter { it.kind == BountyKind.WATER }.random(rng)
        val steps = lifestyle.filter { it.kind == BountyKind.STEPS }.random(rng)
        val thirdPool = lifestyle.filter {
            it.kind == BountyKind.STRETCH
        } + questing
        val third = thirdPool.random(rng)
        return listOf(water, steps, third)
    }
}
