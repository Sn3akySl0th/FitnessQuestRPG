package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.OwnedGear
import com.fitnessquest.rpg.data.db.GearInstanceEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GearComparisonTest {

    private val sword = ItemEntity(
        id = 1,
        name = "Training Blade",
        emoji = "⚔️",
        slot = ItemSlot.WEAPON,
        tier = 2,
        price = 90,
        atk = 5,
        def = 1,
        hp = 0,
    )

    private val powerRune = ItemEntity(
        id = Runes.POWER,
        name = "Rune of Power",
        emoji = "💪",
        slot = ItemSlot.RUNE,
        tier = 1,
        price = 80,
        atk = 3,
    )

    @Test
    fun `breakdown includes socketed rune bonuses`() {
        val instance = GearInstanceEntity(
            id = 10,
            catalogId = sword.id,
            atk = 8,
            def = 2,
            hp = 4,
            rune1Id = Runes.POWER,
        )
        val breakdown = GearComparison.breakdownFromOwned(
            OwnedGear(instance, sword),
            mapOf(Runes.POWER to powerRune),
        )
        assertEquals(11, breakdown.atk)
        assertEquals(2, breakdown.def)
        assertEquals(4, breakdown.hp)
    }

    @Test
    fun `compare uses effective equipped stats not catalog base`() {
        val candidateInstance = GearInstanceEntity(
            id = 11,
            catalogId = sword.id,
            atk = 10,
            def = 2,
            hp = 0,
        )
        val equippedInstance = GearInstanceEntity(
            id = 12,
            catalogId = sword.id,
            atk = 12,
            def = 1,
            hp = 8,
            rune1Id = Runes.POWER,
        )
        val comparison = GearComparison.compareGear(
            item = sword,
            instance = candidateInstance,
            equipped = OwnedGear(equippedInstance, sword),
            equippedCatalog = sword,
            runeCatalog = mapOf(Runes.POWER to powerRune),
        )
        assertEquals(-5, comparison.atkDelta)
        assertEquals(1, comparison.defDelta)
        assertEquals(-8, comparison.hpDelta)
        assertEquals(GearComparison.Verdict.DOWNGRADE, comparison.verdict)
    }

    @Test
    fun `empty slot treats positive gear as upgrade`() {
        val candidate = GearComparison.PowerBreakdown(atk = 3, def = 3, hp = 3)
        val comparison = GearComparison.compare(candidate, equipped = null)
        assertEquals(GearComparison.Verdict.UPGRADE, comparison.verdict)
        assertTrue(comparison.powerDelta > 0)
    }

    @Test
    fun `detailed breakdown splits base upgrades and runes`() {
        val instance = GearInstanceEntity(
            id = 10,
            catalogId = sword.id,
            atk = 9,
            def = 2,
            hp = 4,
            upgradeLevel = 1,
            rune1Id = Runes.POWER,
        )
        val breakdown = GearComparison.detailedBreakdown(
            item = sword,
            instance = instance,
            runeCatalog = mapOf(Runes.POWER to powerRune),
        )
        assertEquals(7, breakdown.baseAtk)
        assertEquals(2, breakdown.upgradeAtk)
        assertEquals(3, breakdown.runeAtk)
        assertEquals(12, breakdown.totalAtk)
        assertEquals(1, breakdown.socketedRunes.size)
        assertEquals("+2 ATK, +2 DEF", breakdown.upgradePreview?.gainSummary)
    }
}
