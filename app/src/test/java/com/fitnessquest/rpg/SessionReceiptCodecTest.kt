package com.fitnessquest.rpg

import com.fitnessquest.rpg.data.db.*
import com.fitnessquest.rpg.domain.*
import org.junit.Assert.*
import org.junit.Test

class SessionReceiptCodecTest {

    @Test
    fun testRoundTrip() {
        val character = CharacterEntity(id = 1, name = "Hero")
        val pr = SessionPr("Bench Press", PrKind.WEIGHT, 100.0, 10, true)
        val item = ItemEntity(id = 1, name = "Iron Sword", emoji = "⚔️", slot = ItemSlot.WEAPON, tier = 1, price = 100)

        val rewards = listOf(
            Reward.Xp(100),
            Reward.Gold(50),
            Reward.Energy(10),
            Reward.LevelUp(2),
            Reward.NewPr(pr),
            Reward.BiomeUnlocked("DARKWOOD", "Darkwood Forest"),
            Reward.Gear(item),
            Reward.Stackable(item, 5),
            Reward.TitleUnlocked("Novice"),
            Reward.SkillPoint(1),
            Reward.XpBoost(25)
        )

        val result = SessionResult(
            xp = 100,
            gold = 50,
            energy = 10,
            levelsGained = 1,
            statGains = StatGains(strength = 2, endurance = 1),
            updatedCharacter = character,
            volumeKg = 1000.0,
            durationMs = 3600000L,
            musclesWorked = setOf("Chest", "Triceps"),
            weeklyWorkoutsDone = 2,
            weeklyWorkoutsGoal = 3,
            travelKm = 5.0,
            arrivedAt = "DARKWOOD",
            streak = 5,
            streakSaved = true,
            xpBoostApplied = 25,
            prs = listOf(pr),
            lootLabels = listOf("Common Chest"),
            rewardBatch = RewardBatch(RewardSource.WORKOUT, rewards)
        )

        val json = SessionReceiptCodec.serialize(result)
        val restored = SessionReceiptCodec.deserialize(json, character)

        assertEquals(result.xp, restored.xp)
        assertEquals(result.gold, restored.gold)
        assertEquals(result.energy, restored.energy)
        assertEquals(result.levelsGained, restored.levelsGained)
        assertEquals(result.statGains, restored.statGains)
        assertEquals(result.volumeKg, restored.volumeKg, 0.001)
        assertEquals(result.durationMs, restored.durationMs)
        assertEquals(result.musclesWorked, restored.musclesWorked)
        assertEquals(result.weeklyWorkoutsDone, restored.weeklyWorkoutsDone)
        assertEquals(result.weeklyWorkoutsGoal, restored.weeklyWorkoutsGoal)
        assertEquals(result.travelKm, restored.travelKm, 0.001)
        assertEquals(result.arrivedAt, restored.arrivedAt)
        assertEquals(result.streak, restored.streak)
        assertEquals(result.streakSaved, restored.streakSaved)
        assertEquals(result.xpBoostApplied, restored.xpBoostApplied)
        assertEquals(result.prs.size, restored.prs.size)
        assertEquals(result.prs[0].exerciseName, restored.prs[0].exerciseName)
        assertEquals(result.prs[0].kind, restored.prs[0].kind)
        assertEquals(result.prs[0].value, restored.prs[0].value, 0.001)
        assertEquals(result.prs[0].reps, restored.prs[0].reps)
        assertEquals(result.prs[0].isNew, restored.prs[0].isNew)
        assertEquals(result.lootLabels, restored.lootLabels)
        
        assertEquals(result.rewardBatch, restored.rewardBatch)
        
        // Check some specific rewards
        val restoredRewards = restored.rewardBatch!!.rewards
        assertTrue(restoredRewards.any { it is Reward.LevelUp && it.newLevel == 2 })
        assertTrue(restoredRewards.any { it is Reward.BiomeUnlocked && it.biomeName == "DARKWOOD" })
        assertTrue(restoredRewards.any { it is Reward.Gear && it.item.name == "Iron Sword" })
    }

    @Test
    fun testLegacyFallback() {
        val character = CharacterEntity(id = 1, name = "Hero")
        // Minimal JSON similar to what the old regex might have produced
        val json = "{\"xp\":100,\"gold\":50,\"energy\":10,\"levelsGained\":0}"
        val restored = SessionReceiptCodec.deserialize(json, character)
        
        assertEquals(100, restored.xp)
        assertEquals(50, restored.gold)
        assertEquals(10, restored.energy)
        assertEquals(0, restored.statGains.strength)
        assertTrue(restored.prs.isEmpty())
        assertEquals(
            listOf(Reward.Xp(100), Reward.Gold(50), Reward.Energy(10)),
            restored.rewardBatch?.rewards
        )
    }

    @Test
    fun testNullRewardBatchRoundTrip() {
        val character = CharacterEntity(id = 1, name = "Hero")
        val result = SessionResult(
            xp = 0,
            gold = 0,
            energy = 0,
            levelsGained = 0,
            statGains = StatGains(),
            updatedCharacter = character,
            rewardBatch = null
        )

        val restored = SessionReceiptCodec.deserialize(SessionReceiptCodec.serialize(result), character)

        assertNull(restored.rewardBatch)
    }
}
