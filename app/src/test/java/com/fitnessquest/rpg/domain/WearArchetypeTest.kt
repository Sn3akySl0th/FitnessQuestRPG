package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WearArchetypeTest {

    @Test
    fun resolvesShieldFromStyle() {
        val shield = ItemEntity(1, "Training Buckler", "🛡️", ItemSlot.TRINKET, 1, 40, style = ItemStyle.SHIELD)
        assertEquals(WearArchetype.SHIELD, WearArchetype.resolve(shield))
    }

    @Test
    fun twoHandedWeaponBlocksShieldPairing() {
        val gs = ItemEntity(2, "Dragonfang Greatsword", "⚔️", ItemSlot.WEAPON, 4, 700, style = ItemStyle.GREATSWORD)
        val shield = ItemEntity(3, "Iron Back Shield", "🛡️", ItemSlot.TRINKET, 2, 150, style = ItemStyle.SHIELD)
        assertTrue(EquipRules.isTwoHandedWeapon(gs))
        assertFalse(EquipRules.canEquipTogether(gs, shield))
    }

    @Test
    fun swordAllowsShield() {
        val sword = ItemEntity(4, "Rusty Sword", "⚔️", ItemSlot.WEAPON, 1, 40, style = ItemStyle.SWORD)
        val shield = ItemEntity(5, "Training Buckler", "🛡️", ItemSlot.TRINKET, 1, 45, style = ItemStyle.SHIELD)
        assertTrue(EquipRules.canEquipTogether(sword, shield))
    }
}
