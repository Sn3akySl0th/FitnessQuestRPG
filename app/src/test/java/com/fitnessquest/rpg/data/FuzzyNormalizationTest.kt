package com.fitnessquest.rpg.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FuzzyNormalizationTest {

    @Test
    fun testNormalization() {
        val cases = mapOf(
            "Bench Press (Barbell)" to "bench press",
            "Barbell Bench Press" to "bench press",
            "Dumbbell Flyes" to "flyes",
            "Cable Row" to "row",
            "Weighted Pull-up" to "pull-up",
            "Assisted Dip" to "dip",
            "Squat (Machine)" to "squat",
            "Machine Leg Press" to "leg press"
        )

        cases.forEach { (input, expected) ->
            assertEquals("Normalization failed for $input", expected, normalize(input))
        }
    }

    private fun normalize(name: String): String {
        return name.lowercase()
            .replace(Regex("\\s*\\([^)]*\\)"), "") // strip parenthetical suffixes
            .replace(Regex("^(barbell|dumbbell|cable|machine|weighted|assisted)\\s+"), "") // strip leading adjectives
            .trim()
    }
}
