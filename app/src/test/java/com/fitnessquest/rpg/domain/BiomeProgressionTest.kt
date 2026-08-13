package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.BiomeProgressEntity
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.isStackable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BiomeProgressionTest {

    @Test
    fun `canEnterBiome - MEADOWLANDS is always accessible`() {
        val allProgress = emptyList<BiomeProgressEntity>()
        assertTrue(ProgressionRules.canEnterBiome(Biome.MEADOWLANDS, allProgress))
    }

    @Test
    fun `canEnterBiome - DARKWOOD is locked if MEADOWLANDS boss not defeated`() {
        val allProgress = listOf(
            BiomeProgressEntity(Biome.MEADOWLANDS.name, bossDefeated = false),
        )
        assertFalse(ProgressionRules.canEnterBiome(Biome.DARKWOOD, allProgress))
    }

    @Test
    fun `canEnterBiome - DARKWOOD is accessible after MEADOWLANDS boss defeated`() {
        val allProgress = listOf(
            BiomeProgressEntity(Biome.MEADOWLANDS.name, bossDefeated = true),
        )
        assertTrue(ProgressionRules.canEnterBiome(Biome.DARKWOOD, allProgress))
    }

    @Test
    fun `canEnterBiome - DARKWOOD is locked when allProgress is empty`() {
        val allProgress = emptyList<BiomeProgressEntity>()
        assertFalse(ProgressionRules.canEnterBiome(Biome.DARKWOOD, allProgress))
    }

    @Test
    fun `canUnlockBoss - returns false below threshold`() {
        val progress = BiomeProgressEntity(
            biomeName = Biome.MEADOWLANDS.name,
            progressPoints = ProgressionRules.bossUnlockPointsFor(Biome.MEADOWLANDS) - 1,
            bossUnlocked = false,
        )
        assertFalse(ProgressionRules.canUnlockBoss(progress))
    }

    @Test
    fun `canUnlockBoss - returns true at threshold`() {
        val progress = BiomeProgressEntity(
            biomeName = Biome.MEADOWLANDS.name,
            progressPoints = ProgressionRules.bossUnlockPointsFor(Biome.MEADOWLANDS),
            bossUnlocked = false,
        )
        assertTrue(ProgressionRules.canUnlockBoss(progress))
    }

    @Test
    fun `canUnlockBoss - returns false if already unlocked`() {
        val progress = BiomeProgressEntity(
            biomeName = Biome.MEADOWLANDS.name,
            progressPoints = ProgressionRules.bossUnlockPointsFor(Biome.MEADOWLANDS),
            bossUnlocked = true,
        )
        assertFalse(ProgressionRules.canUnlockBoss(progress))
    }

    @Test
    fun `nextBiomeRequirement - returns Locked when boss not defeated`() {
        val allProgress = listOf(
            BiomeProgressEntity(Biome.MEADOWLANDS.name, bossDefeated = false, progressPoints = 50),
        )
        val requirement = ProgressionRules.nextBiomeRequirement(Biome.MEADOWLANDS, allProgress)
        
        assertTrue(requirement is ProgressionRules.BiomeRequirement.Locked)
        val locked = requirement as ProgressionRules.BiomeRequirement.Locked
        assertEquals(ProgressionRules.bossUnlockPointsFor(Biome.MEADOWLANDS) - 50, locked.pointsNeeded)
        assertFalse(locked.bossDefeated)
    }

    @Test
    fun `nextBiomeRequirement - returns Unlocked when defeated`() {
        val allProgress = listOf(
            BiomeProgressEntity(Biome.MEADOWLANDS.name, bossDefeated = true),
        )
        val requirement = ProgressionRules.nextBiomeRequirement(Biome.MEADOWLANDS, allProgress)
        
        assertTrue(requirement is ProgressionRules.BiomeRequirement.Unlocked)
        assertEquals(Biome.DARKWOOD, (requirement as ProgressionRules.BiomeRequirement.Unlocked).nextBiome)
    }

    @Test
    fun `nextBiomeRequirement - returns MaxBiome at SHADOWFEN after boss defeated`() {
        val allProgress = listOf(
            BiomeProgressEntity(Biome.SHADOWFEN.name, bossDefeated = true),
        )
        val requirement = ProgressionRules.nextBiomeRequirement(Biome.SHADOWFEN, allProgress)
        
        assertEquals(ProgressionRules.BiomeRequirement.MaxBiome, requirement)
    }

    @Test
    fun `lockedBiomeReason - returns null for accessible biome`() {
        val allProgress = listOf(
            BiomeProgressEntity(Biome.MEADOWLANDS.name, bossDefeated = true),
        )
        assertNull(ProgressionRules.lockedBiomeReason(Biome.DARKWOOD, allProgress))
    }

    @Test
    fun `lockedBiomeReason - returns non-null string for locked biome`() {
        val allProgress = listOf(
            BiomeProgressEntity(Biome.MEADOWLANDS.name, bossDefeated = false),
        )
        val reason = ProgressionRules.lockedBiomeReason(Biome.DARKWOOD, allProgress)
        assertTrue(reason!!.contains(Biome.MEADOWLANDS.label))
        assertTrue(reason.contains(Biome.DARKWOOD.label))
    }

    @Test
    fun `maxUnlockedGearTier - regression check - returns tier 3 after 1 boss defeated`() {
        val character = CharacterEntity(level = 1, battlesWon = 0, currentBiome = Biome.MEADOWLANDS.name)
        val allProgress = listOf(
            BiomeProgressEntity(Biome.MEADOWLANDS.name, bossDefeated = true),
        )
        
        val tier = ProgressionRules.maxUnlockedGearTier(character, allProgress)
        assertEquals(3, tier)
    }

    @Test
    fun `bossForBiome - returns designated boss with isBoss true and matching bossOf`() {
        val expectedBosses = mapOf(
            Biome.MEADOWLANDS to (9 to "Lazy Boar"),
            Biome.DARKWOOD to (4 to "Skeleton Spotter"),
            Biome.CRYSTAL_CAVES to (5 to "Ogre of Excuses"),
            Biome.EMBER_PEAKS to (6 to "Cardio Wraith"),
            Biome.FROZEN_WASTES to (7 to "Iron Golem"),
            Biome.SHADOWFEN to (8 to "Burnout Dragon"),
        )

        for ((biome, expected) in expectedBosses) {
            val boss = MonsterCatalog.bossForBiome(biome)
            assertEquals(expected.first, boss.id)
            assertEquals(expected.second, boss.name)
            assertEquals(biome, boss.bossOf)
            assertTrue("Boss for $biome must have isBoss=true", boss.isBoss)
            assertTrue("MonsterCatalog.isBoss must return true", MonsterCatalog.isBoss(boss))
        }
    }

    @Test
    fun `regularMonstersByBiome - excludes boss from roaming monster list`() {
        for (biome in Biome.entries) {
            val regularList = MonsterCatalog.regularMonstersByBiome(biome)
            val boss = MonsterCatalog.bossForBiome(biome)
            assertFalse(regularList.any { it.id == boss.id })
            assertTrue(regularList.none { it.isBoss })
        }
    }

    @Test
    fun `bossFirstClearLoot - guarantees high-tier gear and milestone rewards`() {
        val character = CharacterEntity(level = 10, currentBiome = Biome.MEADOWLANDS.name)
        val gearPool = ItemCatalog.all.filter { it.tier <= 4 }
        val stackPool = ItemCatalog.all.filter { it.slot.isStackable() }

        val loot = LootTables.bossFirstClearLoot(
            biome = Biome.MEADOWLANDS,
            character = character,
            gearPool = gearPool,
            stackPool = stackPool,
        )

        assertFalse(loot.isEmpty)
        assertTrue("Must include gold bonus", loot.goldBonus > 0)
        assertTrue("Must include energy bonus", loot.energyBonus > 0)
        assertTrue("Must include gear grant", loot.grants.any { it is LootGrant.Gear })
        assertTrue("Must include chest grant", loot.grants.any { it is LootGrant.ChestOpened })
        assertTrue("Must include material grant", loot.grants.any { it is LootGrant.Stack })
    }
}
