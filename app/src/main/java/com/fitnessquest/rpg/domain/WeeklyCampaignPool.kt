package com.fitnessquest.rpg.domain

import kotlin.random.Random

enum class CampaignKind {
    WORKOUTS,
    STRENGTH_SETS,
    CARDIO_KM,
    BATTLES,
    XP
}

data class CampaignDef(
    val id: String,
    val kind: CampaignKind,
    val title: String,
    val flavor: String,
    val rewardText: String,
    val rewardGold: Int = 0,
    val rewardXpBoost: Int = 0,
    val target: Int
)

object WeeklyCampaignPool {
    private val training = listOf(
        CampaignDef(
            id = "workouts_3",
            kind = CampaignKind.WORKOUTS,
            title = "Three-Quest Campaign",
            flavor = "Complete 3 workouts before the week turns.",
            rewardText = "+180 Gold, +120 XP next quest",
            rewardGold = 180,
            rewardXpBoost = 120,
            target = 3
        ),
        CampaignDef(
            id = "workouts_5",
            kind = CampaignKind.WORKOUTS,
            title = "Iron Week",
            flavor = "Complete 5 workouts and keep the guild banners flying.",
            rewardText = "+300 Gold, +180 XP next quest",
            rewardGold = 300,
            rewardXpBoost = 180,
            target = 5
        )
    )

    private val focused = listOf(
        CampaignDef(
            id = "strength_24",
            kind = CampaignKind.STRENGTH_SETS,
            title = "Forge the Arsenal",
            flavor = "Log 24 strength sets across the week.",
            rewardText = "+220 Gold",
            rewardGold = 220,
            target = 24
        ),
        CampaignDef(
            id = "cardio_8",
            kind = CampaignKind.CARDIO_KM,
            title = "Road to the Next Biome",
            flavor = "Log 8 cardio kilometers this week.",
            rewardText = "+200 Gold, +100 XP next quest",
            rewardGold = 200,
            rewardXpBoost = 100,
            target = 8
        )
    )

    private val adventure = listOf(
        CampaignDef(
            id = "battles_6",
            kind = CampaignKind.BATTLES,
            title = "Monster Hunt",
            flavor = "Win 6 battles before weekly reset.",
            rewardText = "+260 Gold",
            rewardGold = 260,
            target = 6
        ),
        CampaignDef(
            id = "xp_800",
            kind = CampaignKind.XP,
            title = "Legend in Motion",
            flavor = "Earn 800 workout XP this week.",
            rewardText = "+240 Gold, +160 XP next quest",
            rewardGold = 240,
            rewardXpBoost = 160,
            target = 800
        )
    )

    fun forWeek(weekStartEpochDay: Long): List<CampaignDef> {
        val rng = Random(weekStartEpochDay)
        return listOf(
            training.random(rng),
            focused.random(rng),
            adventure.random(rng)
        )
    }
}
