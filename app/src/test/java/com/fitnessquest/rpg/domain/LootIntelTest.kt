package com.fitnessquest.rpg.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LootIntelTest {

    @Test
    fun battleGearChance_matchesVariantRates() {
        val meadow = Biome.MEADOWLANDS
        val monster = MonsterCatalog.regularMonstersByBiome(meadow).first()
        assertEquals(24, LootRates.battleGearChance(monster.variant))
        assertEquals(24, LootRates.battleGearChance(MonsterVariant.NORMAL))
        assertEquals(38, LootRates.battleGearChance(MonsterVariant.ELITE))
        assertEquals(60, LootRates.battleGearChance(MonsterVariant.EPIC))
    }

    @Test
    fun monsterProfile_includesGearAndChestLines() {
        val biome = Biome.DARKWOOD
        val monster = MonsterCatalog.regularMonstersByBiome(biome).first()
        val profile = LootIntel.monsterProfile(monster, biome)
        assertTrue(profile.dropLines.any { it.label == "Procedural gear" })
        assertTrue(profile.dropLines.any { it.label == "Biome chest" })
        assertTrue(profile.summary.contains(biome.label))
    }

    @Test
    fun biomeProfile_listsBossRelicAndMonsters() {
        val profile = LootIntel.biomeProfile(Biome.EMBER_PEAKS)
        assertEquals(BossRelics.displayNameFor(Biome.EMBER_PEAKS), profile.bossRelicName)
        assertTrue(profile.regularMonsters.isNotEmpty())
        assertEquals(MonsterCatalog.bossForBiome(Biome.EMBER_PEAKS).id, profile.boss.id)
    }

    @Test
    fun bossRelicIds_alignWithBiomes() {
        Biome.entries.forEach { biome ->
            assertTrue(BossRelics.idFor(biome) >= 1201L)
            assertTrue(BossRelics.displayNameFor(biome).isNotBlank())
        }
    }

    @Test
    fun setSearch_returnsFarmProfiles() {
        val results = LootIntel.searchSetProfiles("iron")
        assertTrue(results.any { it.setId == "ironbound" })
        assertTrue(results.first().farmHint.isNotBlank())
    }
}
