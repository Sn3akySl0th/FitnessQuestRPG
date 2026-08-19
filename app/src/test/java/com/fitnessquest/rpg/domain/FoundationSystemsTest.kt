package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.BiomeProgressEntity
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class FoundationSystemsTest {

    @Test
    fun `procedural stat engine rolls variance and bonus affixes`() {
        val template = ItemEntity(
            id = 100,
            name = "Iron Greatsword",
            emoji = "⚔️",
            slot = ItemSlot.WEAPON,
            tier = 2,
            price = 100,
            atk = 10,
            def = 2,
            hp = 5
        )

        val rng = Random(123)
        val commonRoll = ProceduralStatEngine.generateStats(template, GearRarity.COMMON, rng)
        assertTrue("Common ATK should roll around base variance", commonRoll.atk in 8..15)
        assertTrue("Quality percent should be bounded", commonRoll.qualityPercent in 75..100)

        val legendaryRoll = ProceduralStatEngine.generateStats(template, GearRarity.LEGENDARY, rng)
        assertTrue("Legendary ATK should be significantly higher than Common", legendaryRoll.atk > commonRoll.atk)
        assertTrue("Legendary should have bonus affixes", legendaryRoll.def >= commonRoll.def || legendaryRoll.hp >= commonRoll.hp)
    }

    @Test
    fun `reward consolidation combines duplicate gold, xp, energy and stackables`() {
        val iron = ItemEntity(id = 701, name = "Scrap Iron", emoji = "⚙️", slot = ItemSlot.MATERIAL, tier = 1, price = 10)
        val dust = ItemEntity(id = 702, name = "Arcane Dust", emoji = "✨", slot = ItemSlot.MATERIAL, tier = 2, price = 20)

        val rawRewards = listOf(
            Reward.Gold(50),
            Reward.Xp(100),
            Reward.Stackable(iron, 2),
            Reward.Gold(30),
            Reward.Energy(5),
            Reward.Stackable(iron, 3),
            Reward.Xp(50),
            Reward.Stackable(dust, 1),
            Reward.Gold(20),
            Reward.Energy(5)
        )

        val consolidated = rawRewards.consolidate()

        val gold = consolidated.filterIsInstance<Reward.Gold>()
        assertEquals(1, gold.size)
        assertEquals(100, gold.first().amount)

        val xp = consolidated.filterIsInstance<Reward.Xp>()
        assertEquals(1, xp.size)
        assertEquals(150, xp.first().amount)

        val energy = consolidated.filterIsInstance<Reward.Energy>()
        assertEquals(1, energy.size)
        assertEquals(10, energy.first().amount)

        val stackables = consolidated.filterIsInstance<Reward.Stackable>()
        val ironStack = stackables.find { it.item.id == 701L }
        assertNotNull(ironStack)
        assertEquals(5, ironStack?.quantity)

        val dustStack = stackables.find { it.item.id == 702L }
        assertNotNull(dustStack)
        assertEquals(1, dustStack?.quantity)
    }

    @Test
    fun `boss cooldown math and status detection work accurately`() {
        val now = 10_000_000L
        val uncompleted = BiomeProgressEntity(
            biomeName = "MEADOWLANDS",
            bossDefeated = false,
            lastBossDefeatedEpochMs = 0L
        )
        assertFalse(ProgressionRules.isBossOnCooldown(uncompleted, now))
        assertEquals(0L, ProgressionRules.bossCooldownRemainingMs(uncompleted, now))

        // Defeated 1 hour ago (within 4h cooldown)
        val onCooldown = BiomeProgressEntity(
            biomeName = "MEADOWLANDS",
            bossDefeated = true,
            lastBossDefeatedEpochMs = now - (1 * 60 * 60 * 1000L)
        )
        assertTrue(ProgressionRules.isBossOnCooldown(onCooldown, now))
        assertEquals(3 * 60 * 60 * 1000L, ProgressionRules.bossCooldownRemainingMs(onCooldown, now))

        // Defeated 5 hours ago (cooldown expired)
        val expiredCooldown = BiomeProgressEntity(
            biomeName = "MEADOWLANDS",
            bossDefeated = true,
            lastBossDefeatedEpochMs = now - (5 * 60 * 60 * 1000L)
        )
        assertFalse(ProgressionRules.isBossOnCooldown(expiredCooldown, now))
        assertEquals(0L, ProgressionRules.bossCooldownRemainingMs(expiredCooldown, now))
    }

    @Test
    fun `workout loot scales crafting materials with set volume and PRs`() {
        val iron = ItemEntity(id = 701, name = "Scrap Iron", emoji = "⚙️", slot = ItemSlot.MATERIAL, tier = 1, price = 10)
        val stackPool = listOf(iron)
        val gearPool = emptyList<ItemEntity>()

        // 2 sets -> 0 materials
        val lowSets = LootTables.rollWorkoutLoot(level = 5, setCount = 2, prCount = 0, gearPool = gearPool, stackPool = stackPool)
        val lowMats = lowSets.grants.filterIsInstance<LootGrant.Stack>()
        assertTrue("Low sets without PR should not grant materials", lowMats.isEmpty())

        // 6 sets -> at least 2 materials
        val midSets = LootTables.rollWorkoutLoot(level = 5, setCount = 6, prCount = 0, gearPool = gearPool, stackPool = stackPool)
        val midMats = midSets.grants.filterIsInstance<LootGrant.Stack>()
        assertFalse(midMats.isEmpty())
        assertTrue("6 sets should grant at least 2 materials", (midMats.firstOrNull()?.quantity ?: 0) >= 2)

        // 10 sets + PR -> 4 materials
        val highSets = LootTables.rollWorkoutLoot(level = 5, setCount = 10, prCount = 1, gearPool = gearPool, stackPool = stackPool)
        val highMats = highSets.grants.filterIsInstance<LootGrant.Stack>()
        assertFalse(highMats.isEmpty())
        assertTrue("10 sets + PR should grant 4 materials", (highMats.firstOrNull()?.quantity ?: 0) >= 4)
    }

    @Test
    fun `boss farm loot rolls crafting materials and elevated rewards`() {
        val hero = CharacterEntity(level = 10, characterClass = CharacterClass.WARRIOR)
        val iron = ItemEntity(id = 701, name = "Scrap Iron", emoji = "⚙️", slot = ItemSlot.MATERIAL, tier = 2, price = 10)
        val sword = ItemEntity(id = 101, name = "Knight Sword", emoji = "⚔️", slot = ItemSlot.WEAPON, tier = 2, price = 100, atk = 10)
        val stackPool = listOf(iron)
        val gearPool = listOf(sword)

        val farmLoot = LootTables.rollBossFarmLoot(
            biome = Biome.MEADOWLANDS,
            character = hero,
            gearPool = gearPool,
            stackPool = stackPool,
            rng = Random(42)
        )

        assertFalse("Boss farm loot should contain grants", farmLoot.grants.isEmpty())
        val mats = farmLoot.grants.filterIsInstance<LootGrant.Stack>()
        assertFalse("Boss farm loot must include crafting materials", mats.isEmpty())
        assertTrue(mats.first().quantity >= 2)

        val gear = farmLoot.grants.filterIsInstance<LootGrant.Gear>()
        assertFalse("Boss farm loot should roll gear", gear.isEmpty())
    }
}
