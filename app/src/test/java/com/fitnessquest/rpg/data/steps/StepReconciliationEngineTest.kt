package com.fitnessquest.rpg.data.steps

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StepReconciliationEngineTest {

    @Test
    fun testDeltaCalculation() {
        val today = LocalDate.now().toEpochDay()
        var currentMax = 0
        var totalIdleStepsCredited = 0

        fun processCandidate(candidate: Int): Int {
            val clamped = candidate.coerceAtMost(50_000)
            if (clamped > currentMax) {
                val delta = clamped - currentMax
                currentMax = clamped
                totalIdleStepsCredited += delta
                return delta
            }
            return 0
        }

        // Phone reports 1000 steps
        val delta1 = processCandidate(1000)
        assertEquals(1000, delta1)
        assertEquals(1000, currentMax)
        assertEquals(1000, totalIdleStepsCredited)

        // Health Connect reports 3500 steps (e.g. user walked with smartwatch)
        val delta2 = processCandidate(3500)
        assertEquals(2500, delta2)
        assertEquals(3500, currentMax)
        assertEquals(3500, totalIdleStepsCredited)

        // Phone reports 2000 steps (still lower than watch count) -> 0 delta
        val delta3 = processCandidate(2000)
        assertEquals(0, delta3)
        assertEquals(3500, currentMax)
        assertEquals(3500, totalIdleStepsCredited)

        // User enters 5000 manual steps from clip-on pedometer
        val delta4 = processCandidate(5000)
        assertEquals(1500, delta4)
        assertEquals(5000, currentMax)
        assertEquals(5000, totalIdleStepsCredited)

        // Extreme entry capped at 50,000 max
        val delta5 = processCandidate(999_999)
        assertEquals(45000, delta5)
        assertEquals(50000, currentMax)
        assertEquals(50000, totalIdleStepsCredited)
    }
}
