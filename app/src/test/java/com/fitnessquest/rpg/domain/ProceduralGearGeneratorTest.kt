package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ProceduralGearGeneratorTest {

    private val hero = CharacterEntity(level = 8, characterClass = CharacterClass.WARRIOR)
    private val templates = listOf(
        ItemEntity(1, "Rusty Sword", "⚔️", ItemSlot.WEAPON, 1, 40, atk = 3),
        ItemEntity(101, "Ironbound Helm", "🪖", ItemSlot.HEAD, 2, 95, def = 4, classAffinity = CharacterClass.WARRIOR),
        ItemEntity(102, "Ironbound Cuirass", "🛡️", ItemSlot.CHEST, 2, 130, def = 7, classAffinity = CharacterClass.WARRIOR),
    )

    @Test
    fun `generate creates rolled display names`() {
        val generated = ProceduralGearGenerator.generate(
            character = hero,
            itemLevel = 8,
            maxTier = 2,
            source = LootSource.BATTLE,
            templatePool = templates,
            rng = Random(1),
        )!!
        assertTrue(generated.displayName.isNotBlank())
        assertTrue(generated.atk + generated.def + generated.hp > 0)
    }

    @Test
    fun `loot grant carries rolled stats and metadata`() {
        val generated = ProceduralGearGenerator.generate(
            character = hero,
            itemLevel = 10,
            maxTier = 2,
            source = LootSource.BATTLE,
            templatePool = templates,
            rng = Random(7),
        )!!
        val grant = ProceduralGearGenerator.toLootGrant(generated)
        assertTrue((grant.rolledAtk ?: 0) >= 0)
        assertTrue(grant.displayName!!.isNotBlank())
        assertTrue(grant.baseTypeId!!.isNotBlank())
        assertTrue(grant.itemLevel > 0)
    }
}
