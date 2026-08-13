package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.BiomeProgressEntity
import com.fitnessquest.rpg.data.db.CharacterEntity
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
            BiomeProgressEntity(Biome.MEADOWLANDS.name, bossDefeated = false)
        )
        assertFalse(ProgressionRules.canEnterBiome(Biome.DARKWOOD, allProgress))
    }

    @Test
    fun `canEnterBiome - DARKWOOD is accessible after MEADOWLANDS boss defeated`() {
        val allProgress = listOf(
            BiomeProgressEntity(Biome.MEADOWLANDS.name, bossDefeated = true)
        )
        assertTrue(ProgressionRules.canEnterBiome(Biome.DARKWOOD, allProgress))
    }

    @Test
    fun `canUnlockBoss - returns false below threshold`() {
        val progress = BiomeProgressEntity(
            biomeName = Biome.MEADOWLANDS.name,
            progressPoints = ProgressionRules.bossUnlockPointsFor(Biome.MEADOWLANDS) - 1,
            bossUnlocked = false
        )
        assertFalse(ProgressionRules.canUnlockBoss(progress))
    }

    @Test
    fun `canUnlockBoss - returns true at threshold`() {
        val progress = BiomeProgressEntity(
            biomeName = Biome.MEADOWLANDS.name,
            progressPoints = ProgressionRules.bossUnlockPointsFor(Biome.MEADOWLANDS),
            bossUnlocked = false
        )
        assertTrue(ProgressionRules.canUnlockBoss(progress))
    }

    @Test
    fun `canUnlockBoss - returns false if already unlocked`() {
        val progress = BiomeProgressEntity(
            biomeName = Biome.MEADOWLANDS.name,
            progressPoints = ProgressionRules.bossUnlockPointsFor(Biome.MEADOWLANDS),
            bossUnlocked = true
        )
        assertFalse(ProgressionRules.canUnlockBoss(progress))
    }

    @Test
    fun `nextBiomeRequirement - returns Locked when boss not defeated`() {
        val allProgress = listOf(
            BiomeProgressEntity(Biome.MEADOWLANDS.name, bossDefeated = false, progressPoints = 50)
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
            BiomeProgressEntity(Biome.MEADOWLANDS.name, bossDefeated = true)
        )
        val requirement = ProgressionRules.nextBiomeRequirement(Biome.MEADOWLANDS, allProgress)
        
        assertTrue(requirement is ProgressionRules.BiomeRequirement.Unlocked)
        assertEquals(Biome.DARKWOOD, (requirement as ProgressionRules.BiomeRequirement.Unlocked).nextBiome)
    }

    @Test
    fun `nextBiomeRequirement - returns MaxBiome at SHADOWFEN after boss defeated`() {
        val allProgress = listOf(
            BiomeProgressEntity(Biome.SHADOWFEN.name, bossDefeated = true)
        )
        val requirement = ProgressionRules.nextBiomeRequirement(Biome.SHADOWFEN, allProgress)
        
        assertEquals(ProgressionRules.BiomeRequirement.MaxBiome, requirement)
    }

    @Test
    fun `lockedBiomeReason - returns null for accessible biome`() {
        val allProgress = listOf(
            BiomeProgressEntity(Biome.MEADOWLANDS.name, bossDefeated = true)
        )
        assertNull(ProgressionRules.lockedBiomeReason(Biome.DARKWOOD, allProgress))
    }

    @Test
    fun `lockedBiomeReason - returns non-null string for locked biome`() {
        val allProgress = listOf(
            BiomeProgressEntity(Biome.MEADOWLANDS.name, bossDefeated = false)
        )
        val reason = ProgressionRules.lockedBiomeReason(Biome.DARKWOOD, allProgress)
        assertEquals("Defeat the Meadowlands boss to unlock Darkwood.", reason)
    }

    @Test
    fun `maxUnlockedGearTier - regression check - returns tier 3 after 1 boss defeated`() {
        val character = CharacterEntity(level = 1, battlesWon = 0, currentBiome = Biome.MEADOWLANDS.name)
        val allProgress = listOf(
            BiomeProgressEntity(Biome.MEADOWLANDS.name, bossDefeated = true)
        )
        
        val tier = ProgressionRules.maxUnlockedGearTier(character, allProgress)
        assertEquals(3, tier)
    }
}
