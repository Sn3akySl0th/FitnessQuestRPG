package com.fitnessquest.rpg.domain

import kotlin.math.round

/**
 * Barbell / machine plate loading math in **display units** (kg or lb).
 * Counts are always per side; total = bar + 2 × sum(plate × count).
 */
object PlateMath {

    data class PlateCount(val plate: Double, val count: Int)

    data class LoadResult(
        val platesPerSide: List<PlateCount>,
        val loadedTotal: Double,
        /** How far short of the target (0 if exact or over via rounding). */
        val remainder: Double,
    ) {
        val summary: String
            get() = if (platesPerSide.isEmpty()) {
                "Bar only"
            } else {
                platesPerSide.joinToString(", ") { "${it.count}\u00D7${formatPlate(it.plate)}" } +
                    " per side"
            }
    }

    fun denominations(imperial: Boolean): List<Double> =
        if (imperial) listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)
        else listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    fun barPresets(imperial: Boolean): List<Double> =
        if (imperial) listOf(45.0, 35.0, 0.0)
        else listOf(20.0, 15.0, 0.0)

    fun defaultBar(imperial: Boolean): Double = if (imperial) 45.0 else 20.0

    /**
     * Greedy load for [target] given [bar] starting weight.
     * Returns empty plates if target ≤ bar.
     */
    fun platesForTarget(target: Double, bar: Double, imperial: Boolean): LoadResult {
        val platesNeeded = ((target - bar) / 2.0).coerceAtLeast(0.0)
        var remaining = platesNeeded
        val loaded = mutableListOf<PlateCount>()
        for (plate in denominations(imperial)) {
            val count = (remaining / plate).toInt()
            if (count > 0) {
                loaded += PlateCount(plate, count)
                remaining -= count * plate
            }
        }
        // Snap tiny float dust to zero
        if (remaining < 0.01) remaining = 0.0
        val sideSum = loaded.sumOf { it.plate * it.count }
        val total = bar + (sideSum * 2.0)
        return LoadResult(
            platesPerSide = loaded,
            loadedTotal = roundDisplay(total),
            remainder = roundDisplay(remaining * 2.0)
        )
    }

    fun totalWeight(bar: Double, platesPerSide: Map<Double, Int>, imperial: Boolean): Double {
        val side = denominations(imperial).sumOf { plate ->
            (platesPerSide[plate] ?: 0) * plate
        }
        return roundDisplay(bar + (side * 2.0))
    }

    fun formatPlate(value: Double): String =
        if (value % 1.0 in 0.0..0.001 || value % 1.0 in 0.999..1.0) {
            value.toInt().toString()
        } else {
            "%.2f".format(value).trimEnd('0').trimEnd('.')
        }

    private fun roundDisplay(value: Double): Double =
        round(value * 100.0) / 100.0
}
