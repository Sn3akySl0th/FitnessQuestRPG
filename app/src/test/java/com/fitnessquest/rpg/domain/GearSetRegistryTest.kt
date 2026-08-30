package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.GearInstanceEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GearSetRegistryTest {

    @Test
    fun namedSetBonuses_scaleWithPieces() {
        val levels = GearSetRegistry.bonusLevelsFor("ironbound")
        assertEquals(3, levels.size)
        assertEquals(2, levels.first().piecesRequired)
        assertEquals(5, levels.last().piecesRequired)
    }

    @Test
    fun resolveAppliedBonus_countsMatchingSetPieces() {
        val character = CharacterEntity(id = 1L, name = "Test", characterClass = CharacterClass.WARRIOR)
        val head = ItemEntity(10, "Head", "🪖", ItemSlot.HEAD, 2, 10, classAffinity = CharacterClass.WARRIOR)
        val chest = ItemEntity(11, "Chest", "🛡️", ItemSlot.CHEST, 2, 10, classAffinity = CharacterClass.WARRIOR)
        val instances = mapOf(
            100L to GearInstanceEntity(id = 100L, catalogId = head.id, setId = "ironbound"),
            101L to GearInstanceEntity(id = 101L, catalogId = chest.id, setId = "ironbound"),
        )
        val applied = GearSetRegistry.resolveAppliedBonus(character, listOf(head, chest), instances)
        assertEquals("ironbound", applied.setId)
        assertEquals(2, applied.pieceCount)
        assertNotNull(applied.activeLevel)
        assertTrue(applied.classRiderActive)
    }

    @Test
    fun searchSets_findsNightveil() {
        val results = GearSetRegistry.searchSets("night")
        assertTrue(results.contains("nightveil"))
    }
}
