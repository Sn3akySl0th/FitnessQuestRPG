package com.fitnessquest.rpg.domain.visuals

import androidx.compose.ui.graphics.Color
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.domain.ItemStyle
import kotlin.math.absoluteValue

/**
 * Deterministic per-item palettes so gear that shares a style still reads uniquely.
 * Used as the fallback identity layer for cosmetics and procedural drops.
 */
object UniqueGearPalettes {

    data class Palette(
        val primary: Color,
        val secondary: Color,
        val accent: Color?,
        val glow: Color?,
    )

    fun forItem(item: ItemEntity, itemKey: String = EquipmentVisualRegistry.normalizeItemKey(item.name)): Palette {
        val seed = "$itemKey|${item.slot.name}|${item.style}|${item.tier}"
        fun channel(salt: String, min: Int, max: Int): Int {
            val span = max - min + 1
            return min + Math.floorMod("$seed|$salt".hashCode(), span)
        }
        val baseHue = when (item.style) {
            ItemStyle.PLATE -> 210f
            ItemStyle.LIGHT -> 28f
            ItemStyle.ROBE -> 250f
            ItemStyle.SWORD -> 205f
            ItemStyle.GREATSWORD -> 8f
            ItemStyle.DAGGER -> 280f
            ItemStyle.BOW -> 120f
            ItemStyle.WAND -> 195f
            ItemStyle.STAFF -> 135f
            ItemStyle.MACE -> 35f
            else -> 40f
        }
        val hue = (baseHue + channel("hue", 0, 48)).mod(360f)
        val sat = channel("sat", 42, 72) / 100f
        val lightMain = channel("lm", 38, 58) / 100f
        val lightDark = (lightMain - 0.18f).coerceAtLeast(0.12f)
        val lightGlow = (lightMain + 0.12f).coerceAtMost(0.85f)
        val main = hsl(hue, sat, lightMain)
        val dark = hsl(hue, (sat + 0.08f).coerceAtMost(0.9f), lightDark)
        val accent = hsl((hue + 35f).mod(360f), (sat + 0.05f).coerceAtMost(0.85f), (lightMain + 0.08f).coerceAtMost(0.72f))
        val glow = if (item.tier >= 3) hsl((hue + 120f).mod(360f), 0.65f, lightGlow) else null
        return Palette(main, dark, accent, glow)
    }

    private fun hsl(h: Float, s: Float, l: Float): Color {
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(
            red = (r1 + m).coerceIn(0f, 1f),
            green = (g1 + m).coerceIn(0f, 1f),
            blue = (b1 + m).coerceIn(0f, 1f),
        )
    }

    private fun Float.mod(divisor: Float): Float {
        val raw = this % divisor
        return if (raw < 0f) raw + divisor else raw
    }
}
