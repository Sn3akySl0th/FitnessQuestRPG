package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.ArmorSlots
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.data.db.isEquippable
import org.junit.Assert.*
import org.junit.Test

class MysteryGambleAndChestTest {

    @Test
    fun `armor slots gamble filter yields diverse armor slots and not solely chest`() {
        val warrior = CharacterEntity(characterClass = CharacterClass.WARRIOR, level = 10)
        val allGear = ItemCatalog.all.filter { it.slot.isEquippable() }
        
        // Filter using the full set of armor slots (HEAD, CHEST, HANDS, LEGS, FEET)
        val armorPool = ProgressionRules.filterGearPool(allGear, maxTier = 4, character = warrior)
            .filter { it.slot in ArmorSlots }

        assertTrue("Armor pool should contain items", armorPool.isNotEmpty())

        val slotsFound = armorPool.map { it.slot }.toSet()
        
        // Must contain more than just CHEST
        assertTrue("Armor pool must contain HEAD items", slotsFound.contains(ItemSlot.HEAD))
        assertTrue("Armor pool must contain CHEST items", slotsFound.contains(ItemSlot.CHEST))
        assertTrue("Armor pool must contain HANDS items", slotsFound.contains(ItemSlot.HANDS))
        assertTrue("Armor pool must contain LEGS items", slotsFound.contains(ItemSlot.LEGS))
        assertTrue("Armor pool must contain FEET items", slotsFound.contains(ItemSlot.FEET))
    }

    @Test
    fun `weapon and trinket gamble slot filters target correct items`() {
        val mage = CharacterEntity(characterClass = CharacterClass.MAGE, level = 5)
        val allGear = ItemCatalog.all.filter { it.slot.isEquippable() }

        val weaponPool = ProgressionRules.filterGearPool(allGear, maxTier = 2, character = mage)
            .filter { it.slot == ItemSlot.WEAPON }

        assertTrue("Weapon pool should contain items", weaponPool.isNotEmpty())
        assertTrue("All weapon pool items must be weapons", weaponPool.all { it.slot == ItemSlot.WEAPON })

        val trinketPool = ProgressionRules.filterGearPool(allGear, maxTier = 2, character = mage)
            .filter { it.slot == ItemSlot.TRINKET }

        assertTrue("Trinket pool should contain items", trinketPool.isNotEmpty())
        assertTrue("All trinket pool items must be trinkets", trinketPool.all { it.slot == ItemSlot.TRINKET })
    }

    @Test
    fun `mystery gamble cost scales appropriately with character level and daily attempts`() {
        val level1Attempt0 = ProgressionRules.mysteryGambleCost(characterLevel = 1, attemptsToday = 0)
        val level1Attempt1 = ProgressionRules.mysteryGambleCost(characterLevel = 1, attemptsToday = 1)
        val level1Attempt2 = ProgressionRules.mysteryGambleCost(characterLevel = 1, attemptsToday = 2)

        assertEquals(120, level1Attempt0)
        assertEquals(180, level1Attempt1) // 120 * 1.5
        assertEquals(240, level1Attempt2) // 120 * 2.0

        val level10Attempt0 = ProgressionRules.mysteryGambleCost(characterLevel = 10, attemptsToday = 0)
        val level10Attempt2 = ProgressionRules.mysteryGambleCost(characterLevel = 10, attemptsToday = 2)
        assertEquals(250, level10Attempt0)
        assertEquals(500, level10Attempt2) // 250 * 2.0
    }

    @Test
    fun `gamble reset logic accurately identifies day transitions`() {
        val now = 1000000000000L
        val sameDay = now + 1000L
        val nextDay = now + 86400000L * 2

        assertFalse(ProgressionRules.isGambleResetNeeded(sameDay, now))
        assertTrue(ProgressionRules.isGambleResetNeeded(now, nextDay))
        assertTrue(ProgressionRules.isGambleResetNeeded(0L, now))
    }

    @Test
    fun `workout streak and daily workout elevate chest rarity roll thresholds`() {
        val level25 = 25
        // Roll = 30 out of 1000
        val baseRarity = LootTables.rollRarity(
            source = LootSource.CHEST,
            characterLevel = level25,
            workoutStreak = 0,
            workedOutToday = false,
            rng = kotlin.random.Random(1)
        )
        val boostedRarity = LootTables.rollRarity(
            source = LootSource.CHEST,
            characterLevel = level25,
            workoutStreak = 5,
            workedOutToday = true,
            rng = kotlin.random.Random(1)
        )

        assertTrue(
            "Workout streak and active workout should grant equal or higher rarity",
            boostedRarity.ordinal >= baseRarity.ordinal
        )
    }
}
