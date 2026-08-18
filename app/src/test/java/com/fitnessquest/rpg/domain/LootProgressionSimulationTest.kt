package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class LootProgressionSimulationTest {

    private fun sampleGearCatalog(): List<ItemEntity> = listOf(
        ItemEntity(id = 101, name = "Rusty Sword", emoji = "🗡️", slot = ItemSlot.WEAPON, tier = 1, price = 10, atk = 5),
        ItemEntity(id = 102, name = "Iron Sword", emoji = "🗡️", slot = ItemSlot.WEAPON, tier = 2, price = 50, atk = 12),
        ItemEntity(id = 103, name = "Steel Broadsword", emoji = "⚔️", slot = ItemSlot.WEAPON, tier = 3, price = 150, atk = 24),
        ItemEntity(id = 104, name = "Mithril Blade", emoji = "⚔️", slot = ItemSlot.WEAPON, tier = 4, price = 400, atk = 45),
        ItemEntity(id = 105, name = "Dragonforged Blade", emoji = "🗡️", slot = ItemSlot.WEAPON, tier = 5, price = 1000, atk = 80),
        ItemEntity(id = 201, name = "Cloth Tunic", emoji = "🥋", slot = ItemSlot.CHEST, tier = 1, price = 10, def = 3, hp = 10),
        ItemEntity(id = 202, name = "Leather Armor", emoji = "🥋", slot = ItemSlot.CHEST, tier = 2, price = 50, def = 8, hp = 25),
        ItemEntity(id = 203, name = "Chainmail", emoji = "🛡️", slot = ItemSlot.CHEST, tier = 3, price = 150, def = 18, hp = 50),
        ItemEntity(id = 204, name = "Plate Armor", emoji = "🛡️", slot = ItemSlot.CHEST, tier = 4, price = 400, def = 35, hp = 100),
        ItemEntity(id = 205, name = "Dragon Carapace", emoji = "🛡️", slot = ItemSlot.CHEST, tier = 5, price = 1000, def = 65, hp = 200)
    )

    private fun sampleStackCatalog(): List<ItemEntity> = listOf(
        ItemEntity(id = Materials.SCRAP_IRON, name = "Scrap Iron", emoji = "🧱", slot = ItemSlot.MATERIAL, tier = 1, price = 5),
        ItemEntity(id = Materials.ARCANE_DUST, name = "Arcane Dust", emoji = "✨", slot = ItemSlot.MATERIAL, tier = 2, price = 15),
        ItemEntity(id = Materials.BEAST_HIDE, name = "Beast Hide", emoji = "🧶", slot = ItemSlot.MATERIAL, tier = 3, price = 30),
        ItemEntity(id = Materials.EMBER_COAL, name = "Ember Coal", emoji = "🔥", slot = ItemSlot.MATERIAL, tier = 4, price = 60),
        ItemEntity(id = Runes.POWER, name = "Rune of Power", emoji = "💠", slot = ItemSlot.RUNE, tier = 1, price = 25),
        ItemEntity(id = LootChests.WOODEN, name = "Wooden Chest", emoji = "📦", slot = ItemSlot.CHEST, tier = 1, price = 50),
        ItemEntity(id = LootChests.BIOME, name = "Biome Chest", emoji = "🧰", slot = ItemSlot.CHEST, tier = 2, price = 100),
        ItemEntity(id = LootChests.WAR_CACHE, name = "War Cache", emoji = "🪙", slot = ItemSlot.CHEST, tier = 3, price = 250)
    )

    @Test
    fun testRarityStatScaling() {
        val baseAtk = 20
        assertEquals(20, GearRarity.COMMON.scaleStat(baseAtk))
        assertEquals(22, GearRarity.UNCOMMON.scaleStat(baseAtk))
        assertEquals(25, GearRarity.RARE.scaleStat(baseAtk))
        assertEquals(29, GearRarity.EPIC.scaleStat(baseAtk))
        assertEquals(34, GearRarity.LEGENDARY.scaleStat(baseAtk))

        // Small base stats always get guaranteed minimum increment
        val smallBase = 2
        assertTrue(GearRarity.UNCOMMON.scaleStat(smallBase) >= smallBase + 1)
        assertTrue(GearRarity.RARE.scaleStat(smallBase) >= smallBase + 2)
        assertTrue(GearRarity.EPIC.scaleStat(smallBase) >= smallBase + 3)
        assertTrue(GearRarity.LEGENDARY.scaleStat(smallBase) >= smallBase + 5)
    }

    @Test
    fun simulate_3_day_starter_progression() {
        val rng = Random(1337)
        val gearPool = sampleGearCatalog()
        val stackPool = sampleStackCatalog()

        // 3 days: 3 workouts (3-5 sets, level 1-2)
        var level = 1
        val droppedGear = mutableListOf<LootGrant.Gear>()

        repeat(3) {
            val loot = LootTables.rollWorkoutLoot(
                level = level,
                setCount = 4,
                prCount = 1,
                gearPool = gearPool,
                stackPool = stackPool,
                maxTier = 1,
                rng = rng
            )
            loot.grants.filterIsInstance<LootGrant.Gear>().forEach { droppedGear += it }
            level = (level + 1).coerceAtMost(3)
        }

        // Assert Day 3 character (level 1-3) cannot get Tier 3+ or Legendary gear
        assertTrue("No Tier 3+ gear should drop in day 3", droppedGear.none { it.catalog.tier >= 3 })
        assertTrue("No Legendary gear should drop in day 3", droppedGear.none { it.rarity == GearRarity.LEGENDARY })
        assertTrue("No Epic gear should drop in day 3", droppedGear.none { it.rarity == GearRarity.EPIC })
    }

    @Test
    fun simulate_30_day_progression_balance() {
        val rng = Random(4242)
        val gearPool = sampleGearCatalog()
        val stackPool = sampleStackCatalog()

        var level = 1
        var accumulatedGold = 0
        val droppedRarities = mutableMapOf<GearRarity, Int>()

        for (day in 1..30) {
            val hero = CharacterEntity(id = 1, name = "Hero", level = level)
            val maxTier = ProgressionRules.tierCapForLoot(
                LootSource.WORKOUT,
                character = hero,
                biomeProgress = emptyList(),
                contentTier = (level / 4 + 1).coerceIn(1, 5)
            )

            // Workout Loot
            val workoutLoot = LootTables.rollWorkoutLoot(
                level = level,
                setCount = 5,
                prCount = if (day % 4 == 0) 1 else 0,
                gearPool = gearPool,
                stackPool = stackPool,
                maxTier = maxTier,
                rng = rng
            )
            accumulatedGold += workoutLoot.goldBonus
            workoutLoot.grants.filterIsInstance<LootGrant.Gear>().forEach {
                droppedRarities[it.rarity] = (droppedRarities[it.rarity] ?: 0) + 1
            }

            // Occasional Chest Opening
            if (day % 3 == 0) {
                val chestDrops = LootTables.openChest(
                    tier = maxTier,
                    gearPool = gearPool,
                    stackPool = stackPool,
                    rng = rng,
                    characterLevel = level
                )
                chestDrops.filterIsInstance<LootGrant.Gear>().forEach {
                    droppedRarities[it.rarity] = (droppedRarities[it.rarity] ?: 0) + 1
                }
            }

            // Level up steadily
            if (day % 2 == 0 && level < 25) {
                level++
            }
        }

        // Verify rarity progression
        val commonCount = droppedRarities[GearRarity.COMMON] ?: 0
        val uncommonCount = droppedRarities[GearRarity.UNCOMMON] ?: 0
        val rareCount = droppedRarities[GearRarity.RARE] ?: 0

        assertTrue("Common/Uncommon gear should represent early loot bulk", (commonCount + uncommonCount) >= rareCount)
        assertTrue("Gold pacing should remain positive and non-overflowing", accumulatedGold in 100..5000)
    }

    @Test
    fun testBossLootRarityElevated() {
        val rng = Random(9999)
        val hero = CharacterEntity(id = 1, name = "Hero", level = 15, currentBiome = "DARKWOOD")
        val gearPool = sampleGearCatalog()
        val stackPool = sampleStackCatalog()

        val bossLoot = LootTables.bossFirstClearLoot(
            biome = Biome.DARKWOOD,
            character = hero,
            gearPool = gearPool,
            stackPool = stackPool,
            rng = rng
        )

        val gearGrants = bossLoot.grants.filterIsInstance<LootGrant.Gear>()
        assertTrue("Boss clear should grant guaranteed gear", gearGrants.isNotEmpty())
        assertTrue("Boss drops should not be lower than Uncommon for mid-game", gearGrants.all { it.rarity != GearRarity.COMMON })
    }
}
