package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.BiomeProgressEntity
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.data.db.isEquippable

enum class LootSource {
    WORKOUT,
    BATTLE,
    AMBUSH,
    MOMENT,
    CHEST,
    BOSS,
    CAMPAIGN,
    GUILD_RAID,
}

object ProgressionRules {
    const val MAX_GEAR_TIER = 5
    const val BOSS_UNLOCK_POINTS = 100

    /**
     * Requirement state for biome progression.
     */
    sealed class BiomeRequirement {
        data class Locked(val reason: String, val pointsNeeded: Int, val bossDefeated: Boolean) : BiomeRequirement()
        data class Unlocked(val nextBiome: Biome) : BiomeRequirement()
        object MaxBiome : BiomeRequirement()
    }

    fun bossUnlockPointsFor(biome: Biome): Int =
        BOSS_UNLOCK_POINTS + (biome.ordinal * 25)

    fun shouldUnlockBoss(biomeName: String, progressPoints: Int): Boolean {
        val biome = Biome.fromName(biomeName)
        return progressPoints >= bossUnlockPointsFor(biome)
    }

    /**
     * Returns true only when the boss threshold is met AND the boss has not yet been
     * unlocked for combat. Returns false if already unlocked OR already defeated.
     */
    fun canUnlockBoss(biomeProgress: BiomeProgressEntity): Boolean {
        val biome = Biome.fromName(biomeProgress.biomeName)
        return (biomeProgress.progressPoints >= bossUnlockPointsFor(biome)) && !biomeProgress.bossUnlocked
    }

    /**
     * Biome gating: MEADOWLANDS is always open. Any other biome requires the
     * boss of the preceding biome to be defeated.
     */
    fun canEnterBiome(targetBiome: Biome, allProgress: List<BiomeProgressEntity>): Boolean {
        if (targetBiome == Biome.MEADOWLANDS) return true
        
        val previousBiomeOrdinal = targetBiome.ordinal - 1
        if (previousBiomeOrdinal < 0) return true
        
        val previousBiome = Biome.entries[previousBiomeOrdinal]
        val prevProgress = allProgress.find { it.biomeName == previousBiome.name }
        
        return prevProgress?.bossDefeated ?: false
    }

    /**
     * Returns a human-readable reason why a biome is locked, or null if it's open.
     */
    fun lockedBiomeReason(targetBiome: Biome, allProgress: List<BiomeProgressEntity>): String? {
        if (canEnterBiome(targetBiome, allProgress)) return null
        
        val previousBiomeOrdinal = targetBiome.ordinal - 1
        if (previousBiomeOrdinal < 0) return null
        val previousBiome = Biome.entries[previousBiomeOrdinal]
        
        return "Defeat the ${previousBiome.label} boss to unlock ${targetBiome.label}."
    }

    /**
     * Returns true if the hero has conquered the current biome's boss and can move on.
     */
    fun canAdvanceFromBiome(currentBiome: Biome, allProgress: List<BiomeProgressEntity>): Boolean {
        val progress = allProgress.find { it.biomeName == currentBiome.name }
        return progress?.bossDefeated ?: false
    }

    /**
     * Determines the status of the next biome in the sequence.
     */
    fun nextBiomeRequirement(currentBiome: Biome, allProgress: List<BiomeProgressEntity>): BiomeRequirement {
        val currentProgress = allProgress.find { it.biomeName == currentBiome.name } ?: BiomeProgressEntity(currentBiome.name)
        val nextOrdinal = currentBiome.ordinal + 1
        
        if (nextOrdinal >= Biome.entries.size) {
            return if (currentProgress.bossDefeated) BiomeRequirement.MaxBiome 
            else {
                if (currentProgress.bossUnlocked) {
                    return BiomeRequirement.Locked(
                        reason = "Fight the ${currentBiome.label} boss to advance.",
                        pointsNeeded = 0,
                        bossDefeated = false,
                    )
                }
                val pointsNeeded = bossUnlockPointsFor(currentBiome) - currentProgress.progressPoints
                BiomeRequirement.Locked(
                    reason = "Defeat the final boss to complete your journey.",
                    pointsNeeded = pointsNeeded.coerceAtLeast(0),
                    bossDefeated = false,
                )
            }
        }
        
        val nextBiome = Biome.entries[nextOrdinal]
        return if (currentProgress.bossDefeated) {
            BiomeRequirement.Unlocked(nextBiome)
        } else {
            if (currentProgress.bossUnlocked) {
                return BiomeRequirement.Locked(
                    reason = "Fight the ${currentBiome.label} boss to advance.",
                    pointsNeeded = 0,
                    bossDefeated = false,
                )
            }
            val pointsNeeded = bossUnlockPointsFor(currentBiome) - currentProgress.progressPoints
            BiomeRequirement.Locked(
                reason = lockedBiomeReason(nextBiome, allProgress) ?: "Defeat the ${currentBiome.label} boss to advance.",
                pointsNeeded = pointsNeeded.coerceAtLeast(0),
                bossDefeated = false,
            )
        }
    }

    /**
     * Calculates the highest gear tier the character can use/find.
     * Note: This remains consistent with boss-gated progression as it relies on 'bossDefeated'
     * counts to unlock higher tiers (Tier 3 at 1 boss, Tier 4 at 3 bosses, etc.).
     */
    fun maxUnlockedGearTier(
        character: CharacterEntity,
        biomeProgress: List<BiomeProgressEntity>
    ): Int {
        val defeated = biomeProgress.count { it.bossDefeated }
        val deepestLayer = biomeProgress.maxOfOrNull { it.layer } ?: 1
        val currentBiome = Biome.fromName(character.currentBiome)

        var tier = 1
        if (character.level >= 3 || (currentBiome.ordinal >= Biome.DARKWOOD.ordinal)) {
            tier = 2
        }
        if (defeated >= 1 || character.battlesWon >= 20) {
            tier = 3
        }
        if (defeated >= 3) {
            tier = 4
        }
        if (defeated >= Biome.entries.size && deepestLayer >= 2) {
            tier = 5
        }
        return tier.coerceIn(1, MAX_GEAR_TIER)
    }

    fun tierCapForLoot(
        source: LootSource,
        character: CharacterEntity,
        biomeProgress: List<BiomeProgressEntity>,
        contentTier: Int = 1
    ): Int {
        val unlocked = maxUnlockedGearTier(character, biomeProgress)
        val content = contentTier.coerceIn(1, MAX_GEAR_TIER)
        val cap = when (source) {
            LootSource.WORKOUT -> minOf(unlocked, 2)
            LootSource.BATTLE -> minOf(unlocked, content)
            LootSource.AMBUSH -> minOf(unlocked, maxOf(2, content))
            LootSource.MOMENT -> minOf(unlocked, 3)
            LootSource.CHEST -> minOf(unlocked, content)
            LootSource.BOSS -> minOf(unlocked, maxOf(3, content))
            LootSource.CAMPAIGN -> minOf(unlocked, maxOf(2, content))
            LootSource.GUILD_RAID -> minOf(unlocked, maxOf(3, content))
        }
        return cap.coerceIn(1, MAX_GEAR_TIER)
    }

    fun filterGearPool(
        pool: List<ItemEntity>,
        maxTier: Int,
        character: CharacterEntity
    ): List<ItemEntity> =
        pool.filter {
            it.slot.isEquippable() &&
                it.tier <= maxTier &&
                ((it.classAffinity == null) || (it.classAffinity == character.characterClass))
        }

    fun progressForVictory(monster: Monster): Int =
        (10 + monster.tier * 4 + monster.level / 2).coerceAtLeast(10)

    fun salvageMaterialQuantity(tier: Int, rarity: String): Int {
        val rarityBonus = GearRarity.fromName(rarity).salvageBonus
        return (tier.coerceAtLeast(1) + rarityBonus).coerceAtLeast(1)
    }

    fun upgradeGoldCost(tier: Int, upgradeLevel: Int): Int =
        35 + tier.coerceAtLeast(1) * 30 + upgradeLevel.coerceAtLeast(0) * 25

    fun upgradeMaterialCost(tier: Int, upgradeLevel: Int): Int =
        1 + (tier.coerceAtLeast(1) / 2) + (upgradeLevel.coerceAtLeast(0) / 2)

    fun maxUpgradeLevel(tier: Int): Int =
        2 + tier.coerceAtLeast(1)

    fun primaryMaterialFor(item: ItemEntity): Long = when (item.slot) {
        ItemSlot.WEAPON -> Materials.SCRAP_IRON
        ItemSlot.HEAD, ItemSlot.CHEST, ItemSlot.HANDS, ItemSlot.LEGS, ItemSlot.FEET -> when (item.style) {
            ItemStyle.ROBE -> Materials.ARCANE_DUST
            ItemStyle.LIGHT -> Materials.BEAST_HIDE
            else -> Materials.SCRAP_IRON
        }
        ItemSlot.TRINKET -> Materials.ARCANE_DUST
        else -> Materials.SCRAP_IRON
    }
}
