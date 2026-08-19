package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.CharacterEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DruidWildShapeTest {

    private val baseDruid = CharacterEntity(
        name = "Malfurion",
        characterClass = CharacterClass.DRUID,
        level = 50,
        strength = 20,
        endurance = 25,
        agility = 25,
        willpower = 30
    )

    @Test
    fun `druid forms grant distinct stat scaling`() {
        val humanStats = GameMath.combatStats(baseDruid.copy(druidForm = "HUMAN"), emptyList())
        val bearStats = GameMath.combatStats(baseDruid.copy(druidForm = "BEAR"), emptyList())
        val pantherStats = GameMath.combatStats(baseDruid.copy(druidForm = "PANTHER"), emptyList())
        val treantStats = GameMath.combatStats(baseDruid.copy(druidForm = "TREANT"), emptyList())
        val moonkinStats = GameMath.combatStats(baseDruid.copy(druidForm = "MOONKIN"), emptyList())
        val avatarStats = GameMath.combatStats(baseDruid.copy(druidForm = "AVATAR"), emptyList())

        // Bear Form: Double HP, +10 DEF, lower ATK
        assertTrue(bearStats.maxHp >= (humanStats.maxHp * 1.9).toInt())
        assertTrue(bearStats.def > humanStats.def)
        assertTrue(bearStats.atk < humanStats.atk)

        // Panther Form: High Speed and Crit, lower HP
        assertTrue(pantherStats.spd > humanStats.spd)
        assertTrue(pantherStats.critPercent > humanStats.critPercent)
        assertTrue(pantherStats.maxHp < humanStats.maxHp)

        // Treant Form: High DEF and extra HP
        assertTrue(treantStats.def > humanStats.def)
        assertTrue(treantStats.maxHp > humanStats.maxHp)

        // Moonkin Form: High ATK and Crit
        assertTrue(moonkinStats.atk > humanStats.atk)
        assertTrue(moonkinStats.critPercent > humanStats.critPercent)

        // Avatar Form: Supreme hybrid
        assertTrue(avatarStats.maxHp > humanStats.maxHp)
        assertTrue(avatarStats.atk > humanStats.atk)
        assertTrue(avatarStats.def > humanStats.def)
        assertTrue(avatarStats.spd > humanStats.spd)
    }

    @Test
    fun `druid skills execute mid-battle morphs`() {
        val stats = GameMath.combatStats(baseDruid.copy(druidForm = "HUMAN"), emptyList())
        val dummyMonster = Monster(
            id = 100,
            name = "Iron Golem",
            emoji = "🗿",
            level = 20,
            tier = 3,
            hp = 500,
            atk = 20,
            def = 15,
            spd = 10,
            goldReward = 50,
            xpReward = 100,
            biome = Biome.CRYSTAL_CAVES,
            description = "A massive rock construct."
        )

        var battle = BattleEngine.start(
            playerName = "Malfurion",
            clazz = CharacterClass.DRUID,
            level = 50,
            stats = stats,
            strength = 20,
            agility = 25,
            willpower = 30,
            monster = dummyMonster,
            druidForm = "HUMAN",
            rng = Random(42)
        )

        // Cast Bear Form (Skill 0)
        battle = BattleEngine.takeTurn(battle, BattleAction.SKILL, skillIndex = 0, rng = Random(42))
        assertEquals("BEAR", battle.druidForm)
        assertTrue(battle.log.any { it.contains("BEAR FORM") })

        // Cast Panther Form (Skill 1)
        battle = BattleEngine.takeTurn(battle, BattleAction.SKILL, skillIndex = 1, rng = Random(42))
        assertEquals("PANTHER", battle.druidForm)
        assertTrue(battle.log.any { it.contains("PANTHER FORM") })

        // Cast Treant Form (Skill 3)
        battle = BattleEngine.takeTurn(battle, BattleAction.SKILL, skillIndex = 3, rng = Random(42))
        assertEquals("TREANT", battle.druidForm)
        assertTrue(battle.log.any { it.contains("TREANT FORM") })

        // Cast Moonkin Form (Skill 4)
        battle = BattleEngine.takeTurn(battle, BattleAction.SKILL, skillIndex = 4, rng = Random(42))
        assertEquals("MOONKIN", battle.druidForm)
        assertTrue(battle.log.any { it.contains("MOONKIN FORM") })

        // Cast Avatar Form (Skill 5)
        battle = BattleEngine.takeTurn(battle, BattleAction.SKILL, skillIndex = 5, rng = Random(42))
        assertEquals("AVATAR", battle.druidForm)
        assertTrue(battle.log.any { it.contains("AVATAR OF THE WILDS") })
    }

    @Test
    fun `druid wild shape turn passives trigger properly`() {
        val stats = GameMath.combatStats(baseDruid.copy(druidForm = "TREANT"), emptyList())
        val dummyMonster = Monster(
            id = 101,
            name = "Shadow Stalker",
            emoji = "👥",
            level = 10,
            tier = 2,
            hp = 200,
            atk = 15,
            def = 10,
            spd = 12,
            goldReward = 20,
            xpReward = 40,
            biome = Biome.DARKWOOD,
            description = "A creeping woodland spirit."
        )

        val battle = BattleEngine.start(
            playerName = "Malfurion",
            clazz = CharacterClass.DRUID,
            level = 50,
            stats = stats,
            strength = 20,
            agility = 25,
            willpower = 30,
            monster = dummyMonster,
            druidForm = "TREANT",
            rng = Random(42)
        )

        val afterTurn = BattleEngine.takeTurn(battle, BattleAction.ATTACK, rng = Random(42))
        // Verify Treant spores and thorns logged
        assertTrue(afterTurn.log.any { it.contains("Ancient Treant") || it.contains("thorns") })
    }
}
