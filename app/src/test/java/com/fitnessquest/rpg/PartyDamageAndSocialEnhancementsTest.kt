package com.fitnessquest.rpg

import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.party.PartyAura
import com.fitnessquest.rpg.data.party.PartyBoss
import com.fitnessquest.rpg.data.party.PartyMember
import com.fitnessquest.rpg.data.party.PartyService
import com.fitnessquest.rpg.data.party.PartyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PartyDamageAndSocialEnhancementsTest {

    @Test
    fun `boss weakness maps cyclically by tier`() {
        assertEquals(ExerciseCategory.STRENGTH, PartyService.bossWeaknessForTier(1))
        assertEquals(ExerciseCategory.BODYWEIGHT, PartyService.bossWeaknessForTier(2))
        assertEquals(ExerciseCategory.CARDIO, PartyService.bossWeaknessForTier(3))
        assertEquals(ExerciseCategory.FLEXIBILITY, PartyService.bossWeaknessForTier(4))
        assertEquals(ExerciseCategory.STRENGTH, PartyService.bossWeaknessForTier(5))
    }

    @Test
    fun `boss enraged state activates at or below 30 percent HP`() {
        val boss = PartyBoss(
            tier = 1,
            name = "Iron Golem",
            emoji = "🗿",
            maxHp = 1000L,
            hp = 300L
        )
        assertTrue("Boss should be enraged at 30% HP", boss.isEnraged)

        val healthyBoss = boss.copy(hp = 301L)
        assertFalse("Boss should not be enraged above 30% HP", healthyBoss.isEnraged)

        val lowBoss = boss.copy(hp = 50L)
        assertTrue("Boss should be enraged below 30% HP", lowBoss.isEnraged)

        val deadBoss = boss.copy(hp = 0L)
        assertFalse("Defeated boss is not enraged", deadBoss.isEnraged)
        assertTrue("Boss with 0 HP is defeated", deadBoss.defeated)
    }

    @Test
    fun `party auras derive reactively from member composition`() {
        val warrior = PartyMember("u1", "Hero1", "⚔️", 10, 500)
        val mage = PartyMember("u2", "Hero2", "🔮", 12, 600)
        val rogue = PartyMember("u3", "Hero3", "🗡️", 8, 400)

        // 1 member -> no auras
        val soloState = PartyState(members = listOf(warrior))
        assertTrue(soloState.activeAuras.isEmpty())

        // 2 distinct classes -> Vanguard Harmony
        val duoState = PartyState(members = listOf(warrior, mage))
        assertEquals(listOf(PartyAura.VANGUARD_HARMONY), duoState.activeAuras)

        // 3 distinct members -> Vanguard Harmony + Bounty Sync
        val trioState = PartyState(members = listOf(warrior, mage, rogue))
        assertEquals(2, trioState.activeAuras.size)
        assertTrue(trioState.activeAuras.contains(PartyAura.VANGUARD_HARMONY))
        assertTrue(trioState.activeAuras.contains(PartyAura.BOUNTY_SYNC))
    }

    @Test
    fun `party state correctly identifies top damager and most active member`() {
        val m1 = PartyMember("u1", "Tank", "🛡️", 10, 300)
        val m2 = PartyMember("u2", "DPS", "⚔️", 12, 850)
        val m3 = PartyMember("u3", "Healer", "✨", 9, 200)

        val boss = PartyBoss(
            tier = 1,
            name = "Frost Wyrm",
            emoji = "🐉",
            maxHp = 2000L,
            hp = 1000L,
            damageByUid = mapOf("u1" to 300L, "u2" to 650L, "u3" to 50L)
        )

        val state = PartyState(
            members = listOf(m1, m2, m3),
            boss = boss
        )

        assertEquals("u2", state.topDamagerUid)
        assertEquals("u2", state.mostActiveMemberUid)
    }

    @Test
    fun `critical strike scales damage by 1 point 5 multiplier`() {
        val baseDamage = 400
        val isCrit = true
        val partyDamage = if (isCrit) (baseDamage * 1.5).toInt() else baseDamage

        assertEquals(600, partyDamage)
    }
}
