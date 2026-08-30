package com.fitnessquest.rpg.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.ItemStyle
import com.fitnessquest.rpg.domain.WearArchetype

/**
 * Draws a gear item exactly as it appears on the hero: same silhouettes and
 * palettes as the avatar renderer ([GearVisuals]), so the icon is a true
 * preview of what you'll wear. Legendary pieces get their glow.
 *
 * Consumables have no on-body appearance; call sites keep their emoji.
 */
@Composable
fun ItemIcon(item: ItemEntity, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(1f)) {
        val f = IconFrame(size.width / 100f, item)
        // Legendary aura behind everything, mirroring the avatar's aura layer.
        f.pal.glow?.let { glow ->
            drawCircle(
                Brush.radialGradient(
                    listOf(glow.copy(alpha = 0.30f), Color.Transparent),
                    center = f.p(50f, 50f),
                    radius = 50f * f.u,
                ),
                radius = 50f * f.u,
                center = f.p(50f, 50f)
            )
        }
        when (item.slot) {
            ItemSlot.WEAPON -> drawWeaponIcon(f)
            ItemSlot.HEAD -> drawHeadIcon(f)
            ItemSlot.CHEST -> drawChestIcon(f)
            ItemSlot.HANDS -> drawHandsIcon(f)
            ItemSlot.LEGS -> drawLegsIcon(f)
            ItemSlot.FEET -> drawFeetIcon(f)
            ItemSlot.TRINKET -> drawTrinketIcon(f)
                ItemSlot.CONSUMABLE, ItemSlot.RUNE, ItemSlot.MATERIAL, ItemSlot.LOOT_CHEST -> Unit
        }
    }
}

private class IconFrame(val u: Float, val item: ItemEntity) {
    val pal: GearPalette = GearVisuals.palette(item)
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun s(w: Float, h: Float) = Size(w * u, h * u)
}

private val VisorSlit = Color(0xFF1E1A26)
private val HoodShadow = Color(0xFF221E2C)
private val Wood = Color(0xFF6B5138)
private val WoodLight = Color(0xFF8B6B4A)
private val GuardGold = Color(0xFFB07E24)

private fun bladeColor(tier: Int): Color = when (tier) {
    1 -> Color(0xFF9A8F7A)
    2 -> Color(0xFF9AA3AD)
    3 -> Color(0xFFC8D4E8)
    else -> Color(0xFF9C7BE3)
}

private fun orbColor(tier: Int): Color = when (tier) {
    1 -> Color(0xFFB9B4A6)
    2 -> Color(0xFF6BC96B)
    3 -> Color(0xFF7BB4E3)
    else -> Color(0xFFF0C040)
}

// ---- Headgear ----

private fun DrawScope.drawHeadIcon(f: IconFrame) {
    val pal = f.pal
    when (GearVisuals.headgearShape(f.item)) {
        HeadgearShape.HELM -> {
            drawArc(pal.main, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = f.p(20f, 18f), size = f.s(60f, 60f))
            drawRect(pal.dark, topLeft = f.p(20f, 42f), size = f.s(60f, 8f))
            drawRoundRect(pal.main, topLeft = f.p(22f, 50f), size = f.s(14f, 24f), cornerRadius = CornerRadius(4f * f.u))
            drawRoundRect(pal.main, topLeft = f.p(64f, 50f), size = f.s(14f, 24f), cornerRadius = CornerRadius(4f * f.u))
            drawRoundRect(VisorSlit, topLeft = f.p(36f, 52f), size = f.s(28f, 9f), cornerRadius = CornerRadius(4f * f.u))
            if (f.item.tier >= 3) {
                drawRoundRect(pal.glow ?: pal.dark, topLeft = f.p(44f, 2f), size = f.s(12f, 18f), cornerRadius = CornerRadius(5f * f.u))
            }
        }
        HeadgearShape.HAT -> {
            val cone = Path().apply {
                moveTo(18f * f.u, 58f * f.u)
                lineTo(82f * f.u, 58f * f.u)
                lineTo(58f * f.u, 8f * f.u)
                close()
            }
            drawPath(cone, f.pal.main)
            drawOval(pal.main, topLeft = f.p(8f, 52f), size = f.s(84f, 18f))
            drawRect(pal.glow ?: pal.dark, topLeft = f.p(30f, 55f), size = f.s(40f, 7f))
            pal.glow?.let { g ->
                drawLine(g, start = f.p(62f, 12f), end = f.p(70f, 20f), strokeWidth = 2.4f * f.u)
                drawLine(g, start = f.p(70f, 12f), end = f.p(62f, 20f), strokeWidth = 2.4f * f.u)
            }
        }
        HeadgearShape.HOOD -> {
            drawCircle(pal.main, radius = 32f * f.u, center = f.p(50f, 50f))
            drawArc(pal.dark, startAngle = 200f, sweepAngle = 140f, useCenter = true, topLeft = f.p(18f, 18f), size = f.s(64f, 64f))
            drawOval(HoodShadow, topLeft = f.p(30f, 40f), size = f.s(40f, 34f))
            val point = Path().apply {
                moveTo(34f * f.u, 24f * f.u)
                lineTo(66f * f.u, 24f * f.u)
                lineTo(50f * f.u, 2f * f.u)
                close()
            }
            drawPath(point, pal.main)
            pal.glow?.let { g ->
                drawCircle(g.copy(alpha = 0.7f), radius = 32.5f * f.u, center = f.p(50f, 50f), style = Stroke(width = 2f * f.u))
            }
        }
        HeadgearShape.CROWN -> {
            val crown = Path().apply {
                moveTo(15f * f.u, 70f * f.u)
                lineTo(15f * f.u, 27f * f.u)
                lineTo(34f * f.u, 47f * f.u)
                lineTo(50f * f.u, 10f * f.u)
                lineTo(66f * f.u, 47f * f.u)
                lineTo(85f * f.u, 27f * f.u)
                lineTo(85f * f.u, 70f * f.u)
                close()
            }
            drawPath(crown, pal.main)
            drawRect(pal.dark, topLeft = f.p(15f, 63f), size = f.s(70f, 10f))
            drawCircle(pal.glow ?: Color(0xFFF59E0B), radius = 7f * f.u, center = f.p(50f, 55f))
        }
        HeadgearShape.CAP -> {
            drawOval(pal.main, topLeft = f.p(24f, 26f), size = f.s(52f, 22f))
            drawRect(pal.dark, topLeft = f.p(17f, 42f), size = f.s(66f, 9f))
        }
    }
}

// ---- Armor ----

private fun DrawScope.drawChestIcon(f: IconFrame) {
    val pal = f.pal
    when (com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(f.item).archetype) {
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARMOR_MAIL -> {
            drawRoundRect(pal.main, topLeft = f.p(22f, 12f), size = f.s(56f, 72f), cornerRadius = CornerRadius(8f * f.u))
            drawRect(pal.dark, topLeft = f.p(22f, 76f), size = f.s(56f, 8f))
            val ring = (pal.glow ?: pal.dark).copy(alpha = 0.75f)
            for (row in 0..6) for (column in 0..5) {
                val x = 29f + column * 9f + if (row % 2 == 0) 0f else 4.5f
                val y = 22f + row * 8f
                drawCircle(ring, 2.4f * f.u, f.p(x, y), style = Stroke(width = 1.2f * f.u))
            }
            return
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARMOR_CLOAK -> {
            val cloak = Path().apply {
                moveTo(25f * f.u, 16f * f.u)
                quadraticTo(50f * f.u, 4f * f.u, 75f * f.u, 16f * f.u)
                lineTo(86f * f.u, 90f * f.u)
                quadraticTo(50f * f.u, 98f * f.u, 14f * f.u, 90f * f.u)
                close()
            }
            drawPath(cloak, pal.main)
            drawLine(pal.dark, f.p(50f, 16f), f.p(50f, 91f), 3f * f.u)
            drawCircle(pal.glow ?: Color(0xFFF59E0B), 5f * f.u, f.p(50f, 20f))
            return
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARMOR_PAULDRONS -> {
            drawRoundRect(pal.main, topLeft = f.p(25f, 20f), size = f.s(50f, 65f), cornerRadius = CornerRadius(8f * f.u))
            drawRoundRect(pal.dark, topLeft = f.p(4f, 12f), size = f.s(31f, 25f), cornerRadius = CornerRadius(9f * f.u))
            drawRoundRect(pal.main, topLeft = f.p(8f, 15f), size = f.s(27f, 18f), cornerRadius = CornerRadius(7f * f.u))
            drawRoundRect(pal.dark, topLeft = f.p(65f, 12f), size = f.s(31f, 25f), cornerRadius = CornerRadius(9f * f.u))
            drawRoundRect(pal.main, topLeft = f.p(65f, 15f), size = f.s(27f, 18f), cornerRadius = CornerRadius(7f * f.u))
            drawRect(pal.dark, topLeft = f.p(25f, 68f), size = f.s(50f, 9f))
            return
        }
        else -> Unit
    }
    when (f.item.style) {
        ItemStyle.ROBE -> {
            val robe = Path().apply {
                moveTo(30f * f.u, 12f * f.u)
                lineTo(70f * f.u, 12f * f.u)
                lineTo(80f * f.u, 88f * f.u)
                lineTo(20f * f.u, 88f * f.u)
                close()
            }
            drawPath(robe, pal.main)
            drawRect(pal.dark, topLeft = f.p(46f, 12f), size = f.s(8f, 36f))
            drawRect(pal.dark, topLeft = f.p(20f, 82f), size = f.s(60f, 6f))
            if (f.item.tier >= 3) {
                val rune = pal.glow ?: Color(0xFF9C7BE3)
                for (y in listOf(30f, 48f, 66f)) {
                    drawCircle(rune, radius = 2.6f * f.u, center = f.p(33f, y))
                    drawCircle(rune, radius = 2.6f * f.u, center = f.p(67f, y))
                }
            }
            pal.glow?.let { g ->
                drawPath(robe, g.copy(alpha = 0.8f), style = Stroke(width = 2f * f.u))
            }
        }
        ItemStyle.LIGHT -> {
            drawRoundRect(pal.main, topLeft = f.p(26f, 16f), size = f.s(48f, 58f), cornerRadius = CornerRadius(10f * f.u))
            drawRect(pal.dark, topLeft = f.p(26f, 62f), size = f.s(48f, 8f))
            drawRect(pal.dark, topLeft = f.p(34f, 16f), size = f.s(7f, 11f))
            drawRect(pal.dark, topLeft = f.p(59f, 16f), size = f.s(7f, 11f))
            if (f.item.tier >= 2) {
                drawRect(pal.dark, topLeft = f.p(48.6f, 28f), size = f.s(2.8f, 28f))
                for (y in listOf(32f, 39f, 46f, 53f)) {
                    drawLine(pal.dark, start = f.p(42f, y), end = f.p(58f, y), strokeWidth = 1.8f * f.u)
                }
            }
            if (f.item.tier >= 3) {
                drawCircle(pal.dark, radius = 3.2f * f.u, center = f.p(34f, 25f))
                drawCircle(pal.dark, radius = 3.2f * f.u, center = f.p(66f, 25f))
            }
            pal.glow?.let { g ->
                drawRoundRect(
                    g.copy(alpha = 0.8f),
                    topLeft = f.p(26f, 16f),
                    size = f.s(48f, 58f),
                    cornerRadius = CornerRadius(10f * f.u),
                    style = Stroke(width = 2f * f.u)
                )
            }
        }
        else -> {
            drawRoundRect(pal.main, topLeft = f.p(26f, 18f), size = f.s(48f, 52f), cornerRadius = CornerRadius(10f * f.u))
            drawRect(pal.dark, topLeft = f.p(26f, 40f), size = f.s(48f, 5f))
            drawRect(pal.dark, topLeft = f.p(26f, 60f), size = f.s(48f, 7f))
            drawCircle(pal.dark, radius = 12f * f.u, center = f.p(27f, 25f))
            drawCircle(pal.main, radius = 9f * f.u, center = f.p(27f, 25f))
            drawCircle(pal.dark, radius = 12f * f.u, center = f.p(73f, 25f))
            drawCircle(pal.main, radius = 9f * f.u, center = f.p(73f, 25f))
            if (f.item.tier >= 2) {
                for ((x, y) in listOf(33f to 27f, 67f to 27f, 33f to 55f, 67f to 55f)) {
                    drawCircle(pal.dark, radius = 2.2f * f.u, center = f.p(x, y))
                }
            }
            if (f.item.tier >= 3) {
                val emblem = Path().apply {
                    moveTo(50f * f.u, 44f * f.u)
                    lineTo(56f * f.u, 51f * f.u)
                    lineTo(50f * f.u, 58f * f.u)
                    lineTo(44f * f.u, 51f * f.u)
                    close()
                }
                drawPath(emblem, pal.glow ?: pal.dark)
            }
            pal.glow?.let { g ->
                drawRoundRect(
                    g.copy(alpha = 0.8f),
                    topLeft = f.p(26f, 18f),
                    size = f.s(48f, 52f),
                    cornerRadius = CornerRadius(10f * f.u),
                    style = Stroke(width = 2f * f.u)
                )
            }
        }
    }
}

private fun DrawScope.drawHandsIcon(f: IconFrame) {
    val pal = f.pal
    // Cuff with a fist below - the same cuff the avatar wears on its forearms.
    drawRoundRect(pal.main, topLeft = f.p(32f, 14f), size = f.s(36f, 30f), cornerRadius = CornerRadius(7f * f.u))
    drawRect(pal.dark, topLeft = f.p(32f, 14f), size = f.s(36f, 8f))
    drawCircle(pal.main, radius = 18f * f.u, center = f.p(50f, 62f))
    drawCircle(pal.dark, radius = 18f * f.u, center = f.p(50f, 62f), style = Stroke(width = 2f * f.u))
    // Finger creases
    for (x in listOf(43f, 50f, 57f)) {
        drawLine(pal.dark, start = f.p(x, 52f), end = f.p(x, 62f), strokeWidth = 1.6f * f.u)
    }
    pal.glow?.let { g ->
        drawRect(g.copy(alpha = 0.85f), topLeft = f.p(32f, 20f), size = f.s(36f, 3f))
    }
}

private fun DrawScope.drawLegsIcon(f: IconFrame) {
    val pal = f.pal
    drawRect(pal.dark, topLeft = f.p(28f, 12f), size = f.s(44f, 10f))
    drawRoundRect(pal.main, topLeft = f.p(29f, 20f), size = f.s(18f, 66f), cornerRadius = CornerRadius(5f * f.u))
    drawRoundRect(pal.main, topLeft = f.p(53f, 20f), size = f.s(18f, 66f), cornerRadius = CornerRadius(5f * f.u))
    // Knee plates, matching the avatar's leg armor.
    drawRoundRect(pal.dark, topLeft = f.p(28f, 50f), size = f.s(20f, 10f), cornerRadius = CornerRadius(3f * f.u))
    drawRoundRect(pal.dark, topLeft = f.p(52f, 50f), size = f.s(20f, 10f), cornerRadius = CornerRadius(3f * f.u))
    pal.glow?.let { g ->
        drawRect(g.copy(alpha = 0.85f), topLeft = f.p(28f, 54f), size = f.s(20f, 2.4f))
        drawRect(g.copy(alpha = 0.85f), topLeft = f.p(52f, 54f), size = f.s(20f, 2.4f))
    }
}

private fun DrawScope.drawFeetIcon(f: IconFrame) {
    val pal = f.pal
    drawRoundRect(pal.main, topLeft = f.p(20f, 34f), size = f.s(27f, 40f), cornerRadius = CornerRadius(7f * f.u))
    drawRoundRect(pal.main, topLeft = f.p(53f, 34f), size = f.s(27f, 40f), cornerRadius = CornerRadius(7f * f.u))
    drawRect(pal.dark, topLeft = f.p(20f, 34f), size = f.s(27f, 10f))
    drawRect(pal.dark, topLeft = f.p(53f, 34f), size = f.s(27f, 10f))
    // Soles
    drawRoundRect(pal.dark, topLeft = f.p(18f, 68f), size = f.s(31f, 8f), cornerRadius = CornerRadius(4f * f.u))
    drawRoundRect(pal.dark, topLeft = f.p(51f, 68f), size = f.s(31f, 8f), cornerRadius = CornerRadius(4f * f.u))
    pal.glow?.let { g ->
        drawRect(g.copy(alpha = 0.85f), topLeft = f.p(20f, 41f), size = f.s(27f, 2.6f))
        drawRect(g.copy(alpha = 0.85f), topLeft = f.p(53f, 41f), size = f.s(27f, 2.6f))
    }
}

private fun DrawScope.drawTrinketIcon(f: IconFrame) {
    val dye = com.fitnessquest.rpg.domain.visuals.EquipmentDye.fromItem(f.item)
    val tc = dye.tintColor ?: GearVisuals.trinketColor(f.item.id)
    when (WearArchetype.resolve(f.item)) {
        WearArchetype.SHIELD -> {
            drawRoundRect(tc.copy(alpha = 0.35f), topLeft = f.p(28f, 22f), size = f.s(44f, 52f), cornerRadius = CornerRadius(6f * f.u))
            drawRoundRect(tc, topLeft = f.p(32f, 26f), size = f.s(36f, 44f), cornerRadius = CornerRadius(5f * f.u))
            drawCircle(Color(0xFFD4AF37), radius = 6f * f.u, center = f.p(50f, 48f))
        }
        WearArchetype.RING -> {
            drawOval(tc.copy(alpha = 0.25f), topLeft = f.p(30f, 38f), size = f.s(40f, 28f))
            drawOval(tc, topLeft = f.p(34f, 42f), size = f.s(32f, 20f), style = Stroke(width = 5f * f.u))
            drawCircle(Color.White.copy(alpha = 0.65f), radius = 4f * f.u, center = f.p(42f, 40f))
        }
        WearArchetype.AMULET -> {
            drawCircle(tc.copy(alpha = 0.3f), radius = 26f * f.u, center = f.p(50f, 30f))
            drawCircle(tc, radius = 10f * f.u, center = f.p(50f, 30f))
            drawLine(tc, f.p(50f, 40f), f.p(50f, 62f), 3f * f.u)
            drawCircle(tc, radius = 8f * f.u, center = f.p(50f, 68f))
        }
        WearArchetype.BELT -> {
            drawRoundRect(tc, topLeft = f.p(18f, 46f), size = f.s(64f, 12f), cornerRadius = CornerRadius(4f * f.u))
            drawRoundRect(Color(0xFFD4AF37), topLeft = f.p(42f, 42f), size = f.s(16f, 20f), cornerRadius = CornerRadius(3f * f.u))
        }
        WearArchetype.CAPE -> {
            val cape = Path().apply {
                moveTo(38f * f.u, 30f * f.u)
                lineTo(62f * f.u, 30f * f.u)
                lineTo(68f * f.u, 78f * f.u)
                lineTo(32f * f.u, 78f * f.u)
                close()
            }
            drawPath(cape, tc)
        }
        else -> {
            drawCircle(tc.copy(alpha = 0.35f), radius = 34f * f.u, center = f.p(50f, 52f))
            drawCircle(tc, radius = 22f * f.u, center = f.p(50f, 52f))
            drawCircle(Color.White.copy(alpha = 0.7f), radius = 7f * f.u, center = f.p(43f, 45f))
        }
    }
}

// ---- Weapons ----

private fun DrawScope.drawWeaponIcon(f: IconFrame) {
    val tier = f.item.tier
    val legendary = tier >= 4
    val descriptor = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(f.item)
    val necroStaff = ((f.item.style == ItemStyle.STAFF) || (f.item.style == ItemStyle.WAND)) &&
        (f.item.classAffinity == CharacterClass.NECROMANCER)
    if (necroStaff) {
        drawSkullStaffIcon(f, tier)
        return
    }
    if (descriptor.archetype in setOf(
            com.fitnessquest.rpg.domain.visuals.VisualArchetype.SPEAR_GENERIC,
            com.fitnessquest.rpg.domain.visuals.VisualArchetype.UNARMED_WRAP,
            com.fitnessquest.rpg.domain.visuals.VisualArchetype.TONFA,
            com.fitnessquest.rpg.domain.visuals.VisualArchetype.INSTRUMENT,
            com.fitnessquest.rpg.domain.visuals.VisualArchetype.AXE,
            com.fitnessquest.rpg.domain.visuals.VisualArchetype.SCYTHE,
            com.fitnessquest.rpg.domain.visuals.VisualArchetype.HAMMER,
            com.fitnessquest.rpg.domain.visuals.VisualArchetype.CLUB,
            com.fitnessquest.rpg.domain.visuals.VisualArchetype.SCEPTER,
            com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARCANE_FOCUS,
        )
    ) {
        drawSemanticWeaponIcon(f, descriptor)
        return
    }
    when (f.item.style) {
        ItemStyle.MACE -> {
            drawRoundRect(Wood, topLeft = f.p(46.5f, 40f), size = f.s(7f, 50f), cornerRadius = CornerRadius(3.5f * f.u))
            drawCircle(Color(0xFF9AA3AD), radius = 19f * f.u, center = f.p(50f, 28f))
            for ((dx, dy) in listOf(0f to -23f, -21f to -9f, 21f to -9f, -16f to 14f, 16f to 14f)) {
                drawCircle(Color(0xFF767E87), radius = 5.5f * f.u, center = f.p(50f + dx, 28f + dy))
            }
        }
        ItemStyle.GREATSWORD -> {
            val blade = Path().apply {
                moveTo(43f * f.u, 66f * f.u)
                lineTo(43f * f.u, 20f * f.u)
                lineTo(50f * f.u, 6f * f.u)
                lineTo(57f * f.u, 20f * f.u)
                lineTo(57f * f.u, 66f * f.u)
                close()
            }
            if (legendary) {
                drawPath(blade, Color(0xFFFFA040).copy(alpha = 0.5f), style = Stroke(width = 5f * f.u))
            }
            drawPath(blade, Color(0xFFE8763A))
            drawRect(Color(0xFFB3502A), topLeft = f.p(48.6f, 10f), size = f.s(2.8f, 56f))
            drawRoundRect(Color(0xFFF0C040), topLeft = f.p(34f, 64f), size = f.s(32f, 8f), cornerRadius = CornerRadius(4f * f.u))
            drawRoundRect(Wood, topLeft = f.p(46.5f, 72f), size = f.s(7f, 18f), cornerRadius = CornerRadius(3.5f * f.u))
        }
        ItemStyle.WAND -> {
            drawRoundRect(WoodLight, topLeft = f.p(46.8f, 30f), size = f.s(6.4f, 56f), cornerRadius = CornerRadius(3.2f * f.u))
            val spark = orbColor(tier)
            drawCircle(spark.copy(alpha = 0.35f), radius = 15f * f.u, center = f.p(50f, 24f))
            drawCircle(spark, radius = 8f * f.u, center = f.p(50f, 24f))
        }
        ItemStyle.STAFF -> {
            drawRoundRect(Wood, topLeft = f.p(46.8f, 20f), size = f.s(6.4f, 70f), cornerRadius = CornerRadius(3.2f * f.u))
            val orb = orbColor(tier)
            drawCircle(orb.copy(alpha = 0.3f), radius = (if (legendary) 20f else 16f) * f.u, center = f.p(50f, 16f))
            drawCircle(orb, radius = 10f * f.u, center = f.p(50f, 16f))
            drawCircle(Color.White.copy(alpha = 0.6f), radius = 3.2f * f.u, center = f.p(46.8f, 12.8f))
            drawRect(orb, topLeft = f.p(45f, 27f), size = f.s(10f, 4f))
        }
        ItemStyle.DAGGER -> {
            val blade = Path().apply {
                moveTo(45.5f * f.u, 58f * f.u)
                lineTo(45.5f * f.u, 26f * f.u)
                lineTo(50f * f.u, 14f * f.u)
                lineTo(54.5f * f.u, 26f * f.u)
                lineTo(54.5f * f.u, 58f * f.u)
                close()
            }
            if (legendary) {
                drawPath(blade, bladeColor(tier).copy(alpha = 0.5f), style = Stroke(width = 5f * f.u))
            }
            drawPath(blade, bladeColor(tier))
            drawRoundRect(Color(0xFF3E3A4A), topLeft = f.p(39f, 56f), size = f.s(22f, 6f), cornerRadius = CornerRadius(3f * f.u))
            drawRoundRect(Color(0xFF3E3A4A), topLeft = f.p(46.8f, 62f), size = f.s(6.4f, 20f), cornerRadius = CornerRadius(3.2f * f.u))
        }
        ItemStyle.BOW -> {
            val wood = if (legendary) Color(0xFF4E7A3A) else WoodLight
            val bow = Path().apply {
                moveTo(38f * f.u, 10f * f.u)
                quadraticTo(80f * f.u, 50f * f.u, 38f * f.u, 90f * f.u)
            }
            if (legendary) {
                drawPath(bow, Color(0xFF6BC96B).copy(alpha = 0.4f), style = Stroke(width = 9f * f.u))
            }
            drawPath(bow, wood, style = Stroke(width = 5f * f.u))
            drawLine(Color(0xFFE8E4D8), start = f.p(38f, 10f), end = f.p(38f, 90f), strokeWidth = 1.8f * f.u)
            drawCircle(bladeColor(tier), radius = 3.6f * f.u, center = f.p(38f, 10f))
            drawCircle(bladeColor(tier), radius = 3.6f * f.u, center = f.p(38f, 90f))
        }
        else -> { // SWORD and fallback
            val blade = Path().apply {
                moveTo(45f * f.u, 62f * f.u)
                lineTo(45f * f.u, 18f * f.u)
                lineTo(50f * f.u, 6f * f.u)
                lineTo(55f * f.u, 18f * f.u)
                lineTo(55f * f.u, 62f * f.u)
                close()
            }
            if (legendary) {
                drawPath(blade, bladeColor(tier).copy(alpha = 0.5f), style = Stroke(width = 5f * f.u))
            }
            drawPath(blade, bladeColor(tier))
            drawRoundRect(GuardGold, topLeft = f.p(36f, 60f), size = f.s(28f, 7f), cornerRadius = CornerRadius(3.5f * f.u))
            drawRoundRect(Wood, topLeft = f.p(46.8f, 67f), size = f.s(6.4f, 18f), cornerRadius = CornerRadius(3.2f * f.u))
            drawCircle(GuardGold, radius = 4.4f * f.u, center = f.p(50f, 88f))
        }
    }
}

private fun DrawScope.drawSemanticWeaponIcon(
    f: IconFrame,
    descriptor: com.fitnessquest.rpg.domain.visuals.EquipmentVisualDescriptor,
) {
    val accent = descriptor.accentColor ?: Color(0xFFD97706)
    when (descriptor.archetype) {
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SPEAR_GENERIC -> {
            drawRoundRect(descriptor.primaryColor, f.p(47f, 25f), f.s(6f, 67f), CornerRadius(3f * f.u))
            val head = Path().apply {
                moveTo(50f * f.u, 5f * f.u)
                lineTo(61f * f.u, 26f * f.u)
                lineTo(50f * f.u, 37f * f.u)
                lineTo(39f * f.u, 26f * f.u)
                close()
            }
            drawPath(head, descriptor.secondaryColor)
            drawPath(head, accent, style = Stroke(width = 2f * f.u))
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.UNARMED_WRAP -> {
            drawCircle(descriptor.primaryColor, 23f * f.u, f.p(50f, 52f))
            for (y in listOf(39f, 49f, 59f, 69f)) drawLine(accent, f.p(30f, y), f.p(70f, y), 3f * f.u)
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.TONFA -> {
            drawRoundRect(descriptor.primaryColor, f.p(44f, 10f), f.s(12f, 82f), CornerRadius(5f * f.u))
            drawRoundRect(descriptor.secondaryColor, f.p(20f, 48f), f.s(30f, 10f), CornerRadius(4f * f.u))
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.INSTRUMENT -> {
            val name = f.item.name.lowercase()
            if (name.contains("lute")) {
                drawOval(descriptor.primaryColor, f.p(28f, 49f), f.s(44f, 43f))
                drawRoundRect(descriptor.secondaryColor, f.p(46f, 10f), f.s(8f, 47f), CornerRadius(4f * f.u))
                drawCircle(accent, 8f * f.u, f.p(50f, 69f))
            } else if (name.contains("flute")) {
                drawRoundRect(descriptor.primaryColor, f.p(45f, 10f), f.s(10f, 82f), CornerRadius(5f * f.u))
                for (y in listOf(28f, 44f, 60f, 76f)) drawCircle(accent, 2.5f * f.u, f.p(50f, y))
            } else {
                val horn = Path().apply {
                    moveTo(30f * f.u, 82f * f.u)
                    quadraticTo(82f * f.u, 57f * f.u, 60f * f.u, 13f * f.u)
                    lineTo(48f * f.u, 19f * f.u)
                    quadraticTo(64f * f.u, 55f * f.u, 22f * f.u, 69f * f.u)
                    close()
                }
                drawPath(horn, accent)
            }
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.AXE -> {
            drawRoundRect(descriptor.primaryColor, f.p(47f, 23f), f.s(6f, 70f), CornerRadius(3f * f.u))
            val head = Path().apply {
                moveTo(50f * f.u, 17f * f.u)
                quadraticTo(18f * f.u, 14f * f.u, 17f * f.u, 48f * f.u)
                lineTo(50f * f.u, 38f * f.u)
                lineTo(70f * f.u, 45f * f.u)
                lineTo(65f * f.u, 17f * f.u)
                close()
            }
            drawPath(head, descriptor.secondaryColor)
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SCYTHE -> {
            drawRoundRect(descriptor.primaryColor, f.p(47f, 24f), f.s(6f, 70f), CornerRadius(3f * f.u))
            val blade = Path().apply {
                moveTo(50f * f.u, 27f * f.u)
                quadraticTo(28f * f.u, 2f * f.u, 4f * f.u, 17f * f.u)
                quadraticTo(28f * f.u, 13f * f.u, 48f * f.u, 38f * f.u)
                close()
            }
            drawPath(blade, descriptor.secondaryColor)
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.HAMMER -> {
            drawRoundRect(descriptor.primaryColor, f.p(46f, 30f), f.s(8f, 63f), CornerRadius(4f * f.u))
            drawRoundRect(descriptor.secondaryColor, f.p(19f, 10f), f.s(62f, 27f), CornerRadius(6f * f.u))
            drawRect(accent, f.p(16f, 17f), f.s(68f, 7f))
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.CLUB -> {
            val club = Path().apply {
                moveTo(44f * f.u, 92f * f.u)
                lineTo(34f * f.u, 27f * f.u)
                quadraticTo(50f * f.u, 2f * f.u, 66f * f.u, 27f * f.u)
                lineTo(56f * f.u, 92f * f.u)
                close()
            }
            drawPath(club, descriptor.primaryColor)
            drawPath(club, descriptor.secondaryColor, style = Stroke(width = 2f * f.u))
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SCEPTER -> {
            drawRoundRect(descriptor.primaryColor, f.p(46f, 31f), f.s(8f, 61f), CornerRadius(4f * f.u))
            drawCircle(accent.copy(alpha = 0.3f), 21f * f.u, f.p(50f, 22f))
            drawCircle(accent, 12f * f.u, f.p(50f, 22f))
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARCANE_FOCUS -> {
            drawCircle(accent.copy(alpha = 0.25f), 31f * f.u, f.p(50f, 50f))
            val crystal = Path().apply {
                moveTo(50f * f.u, 12f * f.u)
                lineTo(75f * f.u, 50f * f.u)
                lineTo(50f * f.u, 88f * f.u)
                lineTo(25f * f.u, 50f * f.u)
                close()
            }
            drawPath(crystal, accent)
            drawPath(crystal, Color.White.copy(alpha = 0.65f), style = Stroke(width = 2f * f.u))
        }
        else -> Unit
    }
}

private fun DrawScope.drawSkullStaffIcon(f: IconFrame, tier: Int) {
    val bone = Color(0xFFE8E0D0)
    val boneShade = Color(0xFF9A9080)
    val socket = Color(0xFF0E1012)
    val glow = Color(0xFF7CF0C0)
    val rare = tier >= 3
    val legendary = tier >= 4

    drawRoundRect(boneShade, topLeft = f.p(45f, 32f), size = f.s(10f, 60f), cornerRadius = CornerRadius(5f * f.u))
    drawRoundRect(bone, topLeft = f.p(47.5f, 32f), size = f.s(4f, 60f), cornerRadius = CornerRadius(2f * f.u))
    drawRect(Color(0xFF2A1E38), topLeft = f.p(43f, 48f), size = f.s(14f, 5f))
    drawRect(Color(0xFF2A1E38), topLeft = f.p(43f, 70f), size = f.s(14f, 5f))

    if (legendary) {
        drawCircle(glow.copy(alpha = 0.25f), radius = 22f * f.u, center = f.p(50f, 18f))
    }

    drawOval(bone, topLeft = f.p(32f, 4f), size = f.s(36f, 28f))
    drawOval(boneShade.copy(alpha = 0.5f), topLeft = f.p(34f, 16f), size = f.s(10f, 12f))
    drawOval(boneShade.copy(alpha = 0.5f), topLeft = f.p(56f, 16f), size = f.s(10f, 12f))
    drawRoundRect(bone, topLeft = f.p(36f, 26f), size = f.s(28f, 16f), cornerRadius = CornerRadius(4f * f.u))
    for (i in -2..2) {
        val tx = 50f + i * 5f
        drawRect(Color.White.copy(alpha = 0.9f), topLeft = f.p(tx - 1.5f, 28f), size = f.s(3f, 8f))
    }
    drawOval(socket, topLeft = f.p(36f, 12f), size = f.s(12f, 14f))
    drawOval(socket, topLeft = f.p(52f, 12f), size = f.s(12f, 14f))
    if (rare) {
        val r = if (legendary) 4.5f else 3.5f
        drawCircle(glow, radius = r * f.u, center = f.p(42f, 19f))
        drawCircle(glow, radius = r * f.u, center = f.p(58f, 19f))
        if (legendary) {
            drawCircle(Color.White, radius = 1.6f * f.u, center = f.p(42f, 19f))
            drawCircle(Color.White, radius = 1.6f * f.u, center = f.p(58f, 19f))
        }
    }
    val nose = Path().apply {
        moveTo(50f * f.u, 20f * f.u)
        lineTo(45f * f.u, 28f * f.u)
        lineTo(55f * f.u, 28f * f.u)
        close()
    }
    drawPath(nose, socket)
    drawOval(boneShade, topLeft = f.p(32f, 4f), size = f.s(36f, 28f), style = Stroke(width = 2.2f * f.u))
}
