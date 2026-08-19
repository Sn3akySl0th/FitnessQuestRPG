package com.fitnessquest.rpg.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CharacterProgressionAndCompanionTest {

    @Test
    fun `all 12 classes have 6 skills with capstone at level 50`() {
        for (cls in CharacterClass.entries) {
            assertEquals("${cls.name} must have 6 skills", 6, cls.skills.size)
            val unlockLevels = cls.skills.map { it.unlockLevel }
            assertEquals("${cls.name} unlock tiers", listOf(1, 5, 10, 20, 35, 50), unlockLevels)
        }
    }

    @Test
    fun `skillsUnlockedAt returns correct progression for level milestones`() {
        val ranger = CharacterClass.RANGER
        assertEquals(1, ranger.skillsUnlockedAt(1).size)
        assertEquals(2, ranger.skillsUnlockedAt(5).size)
        assertEquals(3, ranger.skillsUnlockedAt(10).size)
        assertEquals(4, ranger.skillsUnlockedAt(20).size)
        assertEquals(5, ranger.skillsUnlockedAt(35).size)
        assertEquals(6, ranger.skillsUnlockedAt(50).size)
    }

    @Test
    fun `ranger companion forms grant distinct combat bonuses`() {
        val baseChar = com.fitnessquest.rpg.data.db.CharacterEntity(
            name = "Robin",
            characterClass = CharacterClass.RANGER,
            level = 10,
            strength = 15,
            endurance = 15,
            agility = 20,
            willpower = 10
        )

        val wolfStats = GameMath.combatStats(baseChar.copy(druidForm = "WOLF"), emptyList())
        val falconStats = GameMath.combatStats(baseChar.copy(druidForm = "FALCON"), emptyList())
        val bearStats = GameMath.combatStats(baseChar.copy(druidForm = "BEAR"), emptyList())

        // Falcon should have highest speed
        assertTrue(falconStats.spd > wolfStats.spd)
        // Bear should have highest defense & hp
        assertTrue(bearStats.def > wolfStats.def)
        assertTrue(bearStats.maxHp > wolfStats.maxHp)
    }

    @Test
    fun `battle engine executes ranger companion turn assist`() {
        val stats = CombatStats(maxHp = 200, atk = 30, def = 10, spd = 20, critPercent = 15)
        val dummyMonster = Monster(
            id = 999,
            name = "Training Dummy",
            emoji = "🎯",
            level = 5,
            tier = 1,
            hp = 100,
            atk = 5,
            def = 5,
            spd = 5,
            goldReward = 10,
            xpReward = 20,
            biome = Biome.MEADOWLANDS,
            description = "A target dummy."
        )

        val state = BattleEngine.start(
            playerName = "Hunter",
            clazz = CharacterClass.RANGER,
            level = 10,
            stats = stats,
            strength = 10,
            agility = 10,
            willpower = 10,
            monster = dummyMonster,
            druidForm = "WOLF",
            rng = Random(42)
        )

        val afterTurn = BattleEngine.takeTurn(
            state = state,
            action = BattleAction.ATTACK,
            rng = Random(42)
        )

        // Monster should take player attack + companion assist damage
        assertTrue(afterTurn.monsterHp < state.monsterHp)
        assertTrue(afterTurn.log.any { it.contains("Dire Wolf") || it.contains("biting") })
    }
}
