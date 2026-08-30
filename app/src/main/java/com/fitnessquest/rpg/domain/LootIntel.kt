package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.isEquippable

/**
 * Player-facing loot discovery data derived from the same rates as [LootTables].
 * UI layers should read profiles from here instead of duplicating drop math.
 */
object LootRates {
    const val BATTLE_RUNE_CHANCE = 25
    const val BOSS_FARM_RELIC_CHANCE = 30
    const val BOSS_FARM_CHEST_CHANCE = 50
    const val SET_TAG_CHANCE = 28
    const val CHEST_GEAR_CHANCE = 12
    const val CHEST_RUNE_CHANCE = 18
    const val WORKOUT_GEAR_CHANCE = 35
    const val WORKOUT_RUNE_ON_PR_CHANCE = 50
    const val WORKOUT_WOODEN_CHEST_CHANCE = 25

    fun battleGearChance(variant: MonsterVariant): Int = when (variant) {
        MonsterVariant.EPIC -> 60
        MonsterVariant.ELITE -> 38
        MonsterVariant.NORMAL -> 24
    }

    fun battleChestChance(monster: Monster): Int {
        if (monster.variant == MonsterVariant.EPIC) return 100
        return (8 + monster.tier * 4 + if (monster.variant == MonsterVariant.ELITE) 20 else 0)
            .coerceAtMost(80)
    }

    fun biomeBossGearTier(biome: Biome): Int = when (biome) {
        Biome.MEADOWLANDS -> 2
        Biome.DARKWOOD, Biome.CRYSTAL_CAVES -> 3
        Biome.EMBER_PEAKS, Biome.FROZEN_WASTES -> 4
        Biome.SHADOWFEN -> 5
    }.coerceIn(1, ProgressionRules.MAX_GEAR_TIER)
}

/** Unique boss trophy relic ids — shared with [LootTables] boss loot rolls. */
object BossRelics {
    fun idFor(biome: Biome): Long = when (biome) {
        Biome.MEADOWLANDS -> 1201L
        Biome.DARKWOOD -> 1202L
        Biome.CRYSTAL_CAVES -> 1203L
        Biome.EMBER_PEAKS -> 1204L
        Biome.FROZEN_WASTES -> 1205L
        Biome.SHADOWFEN -> 1206L
    }

    fun displayNameFor(biome: Biome): String = when (biome) {
        Biome.MEADOWLANDS -> "Boar's Slumber Girdle"
        Biome.DARKWOOD -> "Spotter's Ribcage Aegis"
        Biome.CRYSTAL_CAVES -> "Ogre's Resolution Smasher"
        Biome.EMBER_PEAKS -> "Wraith's Treadmill Cinders"
        Biome.FROZEN_WASTES -> "Golem's Unyielding Core"
        Biome.SHADOWFEN -> "Burnout Dragon's Flameheart"
    }
}

data class LootDropLine(
    val emoji: String,
    val label: String,
    val detail: String,
)

data class MonsterLootProfile(
    val monster: Monster,
    val biome: Biome,
    val gearTierCap: Int,
    val dropLines: List<LootDropLine>,
    val summary: String,
)

data class BiomeLootProfile(
    val biome: Biome,
    val boss: Monster,
    val bossGearTier: Int,
    val regularMonsters: List<Monster>,
    val bossRelicName: String,
    val setExamples: List<String>,
    val dropLines: List<LootDropLine>,
)

data class LootSourceGuide(
    val title: String,
    val emoji: String,
    val summary: String,
    val bullets: List<String>,
)

data class SetFarmProfile(
    val setId: String,
    val displayName: String,
    val farmHint: String,
    val bonusSummary: String,
    val classAffinity: String? = null,
)

object LootIntel {

    fun monsterProfile(monster: Monster, biome: Biome): MonsterLootProfile {
        val gearChance = LootRates.battleGearChance(monster.variant)
        val chestChance = LootRates.battleChestChance(monster)
        val tierCap = monster.tier.coerceIn(1, ProgressionRules.MAX_GEAR_TIER)

        val dropLines = buildList {
            add(
                LootDropLine(
                    emoji = "⚔️",
                    label = "Procedural gear",
                    detail = "$gearChance% chance · up to Tier $tierCap · item level ${monster.level}",
                ),
            )
            add(
                LootDropLine(
                    emoji = "💎",
                    label = "Runes",
                    detail = "${LootRates.BATTLE_RUNE_CHANCE}% chance when gear does not drop",
                ),
            )
            if (monster.variant == MonsterVariant.EPIC) {
                add(LootDropLine("📦", "Biome chest", "Guaranteed on victory"))
            } else {
                add(LootDropLine("📦", "Biome chest", "$chestChance% chance on victory"))
            }
            if (monster.variant != MonsterVariant.NORMAL) {
                val qty = if (monster.variant == MonsterVariant.EPIC) 3 else 1
                add(LootDropLine("🔨", "Crafting materials", "Guaranteed x$qty"))
            } else {
                add(LootDropLine("🔨", "Crafting materials", "Bonus when no gear or rune drops"))
            }
            add(
                LootDropLine(
                    emoji = "🧩",
                    label = "Named armor sets",
                    detail = "~${LootRates.SET_TAG_CHANCE}% of class armor rolls tag a set piece",
                ),
            )
        }

        val summary = buildString {
            append("Battles in ${biome.label} can roll gear up to T$tierCap. ")
            append("$gearChance% gear · ")
            if (monster.variant == MonsterVariant.EPIC) append("guaranteed chest") else append("$chestChance% chest")
        }

        return MonsterLootProfile(
            monster = monster,
            biome = biome,
            gearTierCap = tierCap,
            dropLines = dropLines,
            summary = summary,
        )
    }

    fun biomeProfile(biome: Biome): BiomeLootProfile {
        val boss = MonsterCatalog.bossForBiome(biome)
        val bossTier = LootRates.biomeBossGearTier(biome)
        val setExamples = exampleSetNamesForTier(bossTier)

        val dropLines = buildList {
            add(
                LootDropLine(
                    emoji = "👑",
                    label = "Boss first clear",
                    detail = "Guaranteed ${BossRelics.displayNameFor(biome)}, biome chest, and materials (T$bossTier)",
                ),
            )
            add(
                LootDropLine(
                    emoji = "🔄",
                    label = "Boss re-fights",
                    detail = "${LootRates.BOSS_FARM_RELIC_CHANCE}% ${BossRelics.displayNameFor(biome)} · " +
                        "${LootRates.BOSS_FARM_CHEST_CHANCE}% biome chest · T$bossTier procedural gear",
                ),
            )
            add(
                LootDropLine(
                    emoji = "🐾",
                    label = "Roaming monsters",
                    detail = "24–60% procedural gear by variant · chests & materials · T1–T${bossTier.coerceAtMost(4)}",
                ),
            )
            add(
                LootDropLine(
                    emoji = "📦",
                    label = "Chest contents",
                    detail = "${LootRates.CHEST_GEAR_CHANCE}% gear · ${LootRates.CHEST_RUNE_CHANCE}% runes per roll (2–4 rolls)",
                ),
            )
        }

        return BiomeLootProfile(
            biome = biome,
            boss = boss,
            bossGearTier = bossTier,
            regularMonsters = MonsterCatalog.regularMonstersByBiome(biome),
            bossRelicName = BossRelics.displayNameFor(biome),
            setExamples = setExamples,
            dropLines = dropLines,
        )
    }

    fun otherSources(): List<LootSourceGuide> = listOf(
        LootSourceGuide(
            title = "Workouts",
            emoji = "🏋️",
            summary = "Real training fuels crafting mats and occasional gear.",
            bullets = listOf(
                "Materials scale with sets logged; PRs add extra rolls",
                "${LootRates.WORKOUT_GEAR_CHANCE}% gear when 6+ sets or any PR (max T2 from workouts)",
                "${LootRates.WORKOUT_RUNE_ON_PR_CHANCE}% rune on PR workouts",
                "${LootRates.WORKOUT_WOODEN_CHEST_CHANCE}% wooden chest at 8+ sets",
            ),
        ),
        LootSourceGuide(
            title = "Travel ambush",
            emoji = "🎒",
            summary = "Claim step-powered travel encounters for a War Cache chest.",
            bullets = listOf(
                "Guaranteed War Cache with 2–4 loot rolls",
                "Elevated rarity odds vs normal battles",
            ),
        ),
        LootSourceGuide(
            title = "Shop gamble",
            emoji = "🎰",
            summary = "Spend gold for a random procedural roll — not the main gear path.",
            bullets = listOf(
                "Rolls scale with your level and unlocked tier cap",
                "Named sets and affixes use the same generator as battle drops",
            ),
        ),
        LootSourceGuide(
            title = "Campaign & bounties",
            emoji = "📜",
            summary = "Weekly goals and saga rewards grant bonus chests and stackables.",
            bullets = listOf(
                "Check the Saga tab for claimable milestone chests",
                "Boss first-clear milestones unlock the next biome",
            ),
        ),
    )

    fun allBiomeProfiles(): List<BiomeLootProfile> =
        Biome.entries.map(::biomeProfile)

    fun setFarmProfile(setId: String): SetFarmProfile {
        val levels = GearSetRegistry.bonusLevelsFor(setId)
        return SetFarmProfile(
            setId = setId,
            displayName = GearSetRegistry.displayNameFor(setId),
            farmHint = GearSetRegistry.farmHintFor(setId),
            bonusSummary = levels.joinToString(" · ") { "${it.piecesRequired}-pc: ${it.description}" },
            classAffinity = GearSetRegistry.classAffinityFor(setId)?.label,
        )
    }

    fun searchSetProfiles(query: String): List<SetFarmProfile> =
        GearSetRegistry.searchSets(query).map(::setFarmProfile)

    fun farmHintForItem(item: com.fitnessquest.rpg.data.db.ItemEntity, setId: String? = null): String? {
        setId?.let { return GearSetRegistry.farmHintFor(it) }
        return when (WearArchetype.resolve(item)) {
            WearArchetype.SHIELD -> "Warrior & Paladin battles · T${item.tier} procedural drops"
            WearArchetype.CAPE, WearArchetype.WINGS, WearArchetype.QUIVER -> "Battles, chests, and boss milestones"
            else -> if (item.slot.isEquippable()) "Battles and chests in unlocked biomes" else null
        }
    }

    private fun exampleSetNamesForTier(tier: Int): List<String> {
        val examples = listOf(
            GearSetRegistry.displayNameFor("ironbound"),
            GearSetRegistry.displayNameFor("nightveil"),
            GearSetRegistry.displayNameFor("oathbound"),
            GearSetRegistry.displayNameFor("wildwood"),
            GearSetRegistry.displayNameFor("stormlance"),
        )
        return when {
            tier <= 2 -> examples.take(3)
            tier == 3 -> examples
            else -> examples + listOf(
                GearSetRegistry.displayNameFor("titanforged"),
                GearSetRegistry.displayNameFor("cosmic"),
            )
        }
    }
}
