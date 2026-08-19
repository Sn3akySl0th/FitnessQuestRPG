package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GearProgressionEvolutionTest {

    @Test
    fun `requiredLevelFor enforces tier and rarity minimums`() {
        assertEquals(1, ProgressionRules.requiredLevelFor(tier = 1, rarity = GearRarity.COMMON))
        assertEquals(4, ProgressionRules.requiredLevelFor(tier = 2, rarity = GearRarity.COMMON))
        assertEquals(8, ProgressionRules.requiredLevelFor(tier = 3, rarity = GearRarity.COMMON))
        assertEquals(12, ProgressionRules.requiredLevelFor(tier = 4, rarity = GearRarity.COMMON))
        assertEquals(16, ProgressionRules.requiredLevelFor(tier = 5, rarity = GearRarity.COMMON))

        // Higher rarity on low tier elevates level gate
        assertEquals(4, ProgressionRules.requiredLevelFor(tier = 1, rarity = GearRarity.RARE))
        assertEquals(8, ProgressionRules.requiredLevelFor(tier = 1, rarity = GearRarity.EPIC))
        assertEquals(12, ProgressionRules.requiredLevelFor(tier = 1, rarity = GearRarity.LEGENDARY))
        assertEquals(16, ProgressionRules.requiredLevelFor(tier = 1, rarity = GearRarity.MYTHIC))
    }

    @Test
    fun `reforge eligibility and costs work correctly`() {
        assertFalse(ProgressionRules.canReforge(GearRarity.COMMON))
        assertFalse(ProgressionRules.canReforge(GearRarity.UNCOMMON))
        assertTrue(ProgressionRules.canReforge(GearRarity.RARE))
        assertTrue(ProgressionRules.canReforge(GearRarity.EPIC))
        assertTrue(ProgressionRules.canReforge(GearRarity.LEGENDARY))
        assertTrue(ProgressionRules.canReforge(GearRarity.MYTHIC))

        val rareCost = ProgressionRules.reforgeGoldCost(tier = 3, rarity = GearRarity.RARE)
        val epicCost = ProgressionRules.reforgeGoldCost(tier = 3, rarity = GearRarity.EPIC)
        assertTrue("Higher rarity should cost more to reforge", epicCost > rareCost)

        val matCost = ProgressionRules.reforgeMaterialCost(tier = 3, rarity = GearRarity.RARE)
        assertTrue("Material cost should be at least 1", matCost >= 1)
    }

    @Test
    fun `gear traits parse and roll properly`() {
        val traits = GearTrait.parseTraits("MOMENTUM,SECOND_WIND,VENOMOUS")
        assertEquals(3, traits.size)
        assertTrue(traits.contains(GearTrait.MOMENTUM))
        assertTrue(traits.contains(GearTrait.SECOND_WIND))
        assertTrue(traits.contains(GearTrait.VENOMOUS))

        val rolledRare = GearTrait.rollTraitsForRarity(GearRarity.RARE, Random(42))
        rolledRare.forEach { trait ->
            assertTrue("Trait should be eligible for Rare", trait.minRarity.ordinal <= GearRarity.RARE.ordinal)
        }
    }

    @Test
    fun `modular tiered set bonus calculates correctly`() {
        val warrior = CharacterEntity(characterClass = CharacterClass.WARRIOR, strength = 10, endurance = 10, agility = 5)

        fun makeArmor(slot: ItemSlot, affinity: CharacterClass?) = ItemEntity(
            id = slot.ordinal.toLong() + 100,
            name = "${slot.label} of Valor",
            emoji = "🛡️",
            slot = slot,
            tier = 1,
            price = 10,
            atk = 2,
            def = 2,
            hp = 10,
            classAffinity = affinity
        )

        val neutralArmor = listOf(
            makeArmor(ItemSlot.HEAD, null),
            makeArmor(ItemSlot.CHEST, null),
            makeArmor(ItemSlot.HANDS, null),
            makeArmor(ItemSlot.LEGS, null),
            makeArmor(ItemSlot.FEET, null)
        )

        val noSet = GameMath.setBonusInfo(warrior, neutralArmor)
        assertEquals(0, noSet.pieceCount)
        assertEquals(SetBonusTier.NONE, noSet.activeTier)
        assertEquals(SetBonusTier.TIER_1, noSet.nextTier)

        // 2 matching pieces -> Tier 1
        val twoPieceArmor = listOf(
            makeArmor(ItemSlot.HEAD, CharacterClass.WARRIOR),
            makeArmor(ItemSlot.CHEST, CharacterClass.WARRIOR),
            makeArmor(ItemSlot.HANDS, null),
            makeArmor(ItemSlot.LEGS, null),
            makeArmor(ItemSlot.FEET, null)
        )
        val tier1Info = GameMath.setBonusInfo(warrior, twoPieceArmor)
        assertEquals(2, tier1Info.pieceCount)
        assertEquals(SetBonusTier.TIER_1, tier1Info.activeTier)
        assertEquals(SetBonusTier.TIER_2, tier1Info.nextTier)

        val stats0 = GameMath.combatStats(warrior, neutralArmor)
        val stats2 = GameMath.combatStats(warrior, twoPieceArmor)
        assertTrue("Tier 1 set bonus should increase ATK", stats2.atk >= stats0.atk)
        assertTrue("Tier 1 set bonus should increase DEF", stats2.def > stats0.def)

        // 4 matching pieces -> Tier 2
        val fourPieceArmor = listOf(
            makeArmor(ItemSlot.HEAD, CharacterClass.WARRIOR),
            makeArmor(ItemSlot.CHEST, CharacterClass.WARRIOR),
            makeArmor(ItemSlot.HANDS, CharacterClass.WARRIOR),
            makeArmor(ItemSlot.LEGS, CharacterClass.WARRIOR),
            makeArmor(ItemSlot.FEET, null)
        )
        val tier2Info = GameMath.setBonusInfo(warrior, fourPieceArmor)
        assertEquals(4, tier2Info.pieceCount)
        assertEquals(SetBonusTier.TIER_2, tier2Info.activeTier)
        assertEquals(SetBonusTier.TIER_3, tier2Info.nextTier)

        val stats4 = GameMath.combatStats(warrior, fourPieceArmor)
        assertTrue("Tier 2 set bonus should add Crit bonus", stats4.critPercent > stats0.critPercent)

        // 5 matching pieces -> Tier 3 Mastery
        val fivePieceArmor = listOf(
            makeArmor(ItemSlot.HEAD, CharacterClass.WARRIOR),
            makeArmor(ItemSlot.CHEST, CharacterClass.WARRIOR),
            makeArmor(ItemSlot.HANDS, CharacterClass.WARRIOR),
            makeArmor(ItemSlot.LEGS, CharacterClass.WARRIOR),
            makeArmor(ItemSlot.FEET, CharacterClass.WARRIOR)
        )
        val tier3Info = GameMath.setBonusInfo(warrior, fivePieceArmor)
        assertEquals(5, tier3Info.pieceCount)
        assertEquals(SetBonusTier.TIER_3, tier3Info.activeTier)
        assertNull(tier3Info.nextTier)

        val stats5 = GameMath.combatStats(warrior, fivePieceArmor)
        assertTrue("Tier 3 mastery should add speed bonus", stats5.spd > stats0.spd)
    }

    @Test
    fun `reward traits increase XP and Gold on battle victory`() {
        val hero = CharacterEntity(xp = 0, gold = 0, energy = 30)
        val monster = MonsterCatalog.all.first()

        val defaultChar = GameMath.applyBattleRewards(hero, monster, emptyList())
        val scholarChar = GameMath.applyBattleRewards(hero, monster, listOf(GearTrait.SCHOLAR))
        val fortuneChar = GameMath.applyBattleRewards(hero, monster, listOf(GearTrait.FORTUNE_SEEKER))

        assertTrue("Scholar should award more XP", scholarChar.xp > defaultChar.xp)
        assertTrue("Fortune Seeker should award more Gold", fortuneChar.gold > defaultChar.gold)
    }

    @Test
    fun `battle engine handles Second Wind and Momentum`() {
        val stats = CombatStats(maxHp = 100, atk = 25, def = 10, spd = 20, critPercent = 0)
        val monster = MonsterCatalog.all.first()

        // 1. Momentum increases comboHits on attack
        val state = BattleEngine.start(
            playerName = "Hero",
            clazz = CharacterClass.WARRIOR,
            level = 10,
            stats = stats,
            strength = 10,
            agility = 10,
            willpower = 10,
            monster = monster,
            equippedTraits = listOf(GearTrait.MOMENTUM, GearTrait.SECOND_WIND)
        )

        val turn1 = BattleEngine.takeTurn(state, BattleAction.ATTACK)
        if (turn1.outcome == BattleOutcome.ONGOING) {
            assertTrue("Combo hits should be recorded for momentum", turn1.comboHits >= 1)
        }
    }
}
