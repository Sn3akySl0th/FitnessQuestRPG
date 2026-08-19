package com.fitnessquest.rpg.data.exercises

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseAliasTest {

    @Test
    fun `resolveExplicitAlias maps Heel Taps to canonical Alternate Heel Touchers`() {
        val resolved = ExerciseInfoService.resolveExplicitAlias("Heel Taps")
        assertEquals("Alternate Heel Touchers", resolved)
    }

    @Test
    fun `resolveExplicitAlias is resilient to casing whitespace and singular form`() {
        assertEquals("Alternate Heel Touchers", ExerciseInfoService.resolveExplicitAlias("heel taps"))
        assertEquals("Alternate Heel Touchers", ExerciseInfoService.resolveExplicitAlias("HEEL TAPS"))
        assertEquals("Alternate Heel Touchers", ExerciseInfoService.resolveExplicitAlias("   Heel Tap   "))
        assertEquals("Alternate Heel Touchers", ExerciseInfoService.resolveExplicitAlias("Heel Tap"))
    }

    @Test
    fun `resolveExplicitAlias returns null for non-aliased query`() {
        assertNull(ExerciseInfoService.resolveExplicitAlias("Toe Touch"))
    }

    @Test
    fun `resolveExplicitAlias returns null for direct canonical target names`() {
        assertNull(ExerciseInfoService.resolveExplicitAlias("Alternate Heel Touchers"))
        assertNull(ExerciseInfoService.resolveExplicitAlias("Incline Hammer Curls"))
    }

    @Test
    fun `resolveExplicitAlias works with custom alias map injection`() {
        val customAliases = mapOf("custom my query" to "Canonical Target Exercise")
        val resolved = ExerciseInfoService.resolveExplicitAlias("My Custom Query", customAliases)
        assertEquals("Canonical Target Exercise", resolved)

        val unmapped = ExerciseInfoService.resolveExplicitAlias("Other Query", customAliases)
        assertNull(unmapped)
    }
}
