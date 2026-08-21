package com.fitnessquest.rpg

import com.fitnessquest.rpg.ui.components.Bounty
import com.fitnessquest.rpg.ui.components.DailyBountyTimerUrgency
import com.fitnessquest.rpg.ui.components.dailyBountyTimerUrgency
import com.fitnessquest.rpg.ui.components.hasClaimableDailyBounty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyBountyUiTest {
    @Test
    fun timerUrgencyChangesAtExpectedThresholds() {
        assertEquals(DailyBountyTimerUrgency.NORMAL, dailyBountyTimerUrgency(7 * 60 * 60 * 1000L))
        assertEquals(DailyBountyTimerUrgency.APPROACHING, dailyBountyTimerUrgency(2 * 60 * 60 * 1000L))
        assertEquals(DailyBountyTimerUrgency.URGENT, dailyBountyTimerUrgency(30 * 60 * 1000L))
    }

    @Test
    fun homeBadgeOnlyAppearsForUnclaimedCompletedBounties() {
        val incomplete = bounty(isCompleted = false, isClaimed = false)
        val claimable = bounty(isCompleted = true, isClaimed = false)
        val claimed = bounty(isCompleted = true, isClaimed = true)

        assertFalse(hasClaimableDailyBounty(listOf(incomplete, claimed)))
        assertTrue(hasClaimableDailyBounty(listOf(incomplete, claimable)))
    }

    private fun bounty(isCompleted: Boolean, isClaimed: Boolean) = Bounty(
        id = "test",
        title = "Test bounty",
        rewardText = "10 gold",
        isCompleted = isCompleted,
        isClaimed = isClaimed
    )
}
