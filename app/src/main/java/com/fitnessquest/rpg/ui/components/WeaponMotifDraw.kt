package com.fitnessquest.rpg.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.fitnessquest.rpg.domain.visuals.EquipmentVisualDescriptor
import com.fitnessquest.rpg.domain.visuals.WeaponHeadMotif

internal object WeaponVisualArchetypes {
    val semantic = setOf(
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SPEAR_GENERIC,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.GREATSWORD_DRAGON,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.GREATSWORD_GENERIC,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SWORD_KNIGHT,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SWORD_GENERIC,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SWORD_RAPIER,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_SHORT,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_GENERIC,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_RECURVE,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_LONGBOW,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_WARBOW,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.UNARMED_WRAP,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.TONFA,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.INSTRUMENT,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.AXE,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SCYTHE,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.HAMMER,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.CLUB,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SCEPTER,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARCANE_FOCUS,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.WAND_ARCANE,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.STAFF_WOODEN,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.STAFF_TRAVELER,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.STAFF_NECRO,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.STAFF_DRUID,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.STAFF_RUNED,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.DAGGER_GENERIC,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.MACE_GENERIC,
    )
}

internal fun DrawScope.drawSkullStaffIcon(f: IconFrame, tier: Int) {
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
    if (legendary) drawCircle(glow.copy(alpha = 0.25f), 22f * f.u, f.p(50f, 18f))
    drawOval(bone, topLeft = f.p(32f, 4f), size = f.s(36f, 28f))
    drawOval(boneShade.copy(alpha = 0.5f), topLeft = f.p(34f, 16f), size = f.s(10f, 12f))
    drawOval(boneShade.copy(alpha = 0.5f), topLeft = f.p(56f, 16f), size = f.s(10f, 12f))
    drawRoundRect(bone, topLeft = f.p(36f, 26f), size = f.s(28f, 16f), CornerRadius(4f * f.u))
    for (i in -2..2) {
        val tx = 50f + i * 5f
        drawRect(Color.White.copy(alpha = 0.9f), topLeft = f.p(tx - 1.5f, 28f), size = f.s(3f, 8f))
    }
    drawOval(socket, topLeft = f.p(36f, 12f), size = f.s(12f, 14f))
    drawOval(socket, topLeft = f.p(52f, 12f), size = f.s(12f, 14f))
    if (rare) {
        val r = if (legendary) 4.5f else 3.5f
        drawCircle(glow, r * f.u, f.p(42f, 19f))
        drawCircle(glow, r * f.u, f.p(58f, 19f))
        if (legendary) {
            drawCircle(Color.White, 1.6f * f.u, f.p(42f, 19f))
            drawCircle(Color.White, 1.6f * f.u, f.p(58f, 19f))
        }
    }
    val nose = Path().apply {
        moveTo(50f * f.u, 20f * f.u)
        lineTo(45f * f.u, 28f * f.u)
        lineTo(55f * f.u, 28f * f.u)
        close()
    }
    drawPath(nose, socket)
    drawOval(boneShade, topLeft = f.p(32f, 4f), size = f.s(36f, 28f), style = Stroke(2.2f * f.u))
}

internal fun DrawScope.drawSkullStaffAvatar(f: AvatarFrame, handX: Float, tier: Int) {
    val bone = Color(0xFFE8E0D0)
    val boneShade = Color(0xFF9A9080)
    val socket = Color(0xFF0E1012)
    val glow = Color(0xFF7CF0C0)
    val rare = tier >= 3
    val legendary = tier >= 4
    val cy = 13f

    drawRoundRect(boneShade, f.p(handX - 2.2f, 24f), f.s(4.4f, 58f), CornerRadius(2.2f * f.u))
    drawRoundRect(bone, f.p(handX - 1.2f, 24f), f.s(1.6f, 58f), CornerRadius(0.8f * f.u))
    drawRect(Color(0xFF2A1E38), f.p(handX - 2.8f, 40f), f.s(5.6f, 2.4f))
    drawRect(Color(0xFF2A1E38), f.p(handX - 2.8f, 62f), f.s(5.6f, 2.4f))
    if (legendary) drawCircle(glow.copy(alpha = 0.22f), 11f * f.u, f.p(handX, cy))
    drawOval(bone, f.p(handX - 7.5f, cy - 8f), f.s(15f, 12f))
    drawOval(boneShade.copy(alpha = 0.55f), f.p(handX - 7f, cy - 2f), f.s(4f, 5f))
    drawOval(boneShade.copy(alpha = 0.55f), f.p(handX + 3f, cy - 2f), f.s(4f, 5f))
    drawRoundRect(bone, f.p(handX - 5.5f, cy + 2.5f), f.s(11f, 6.5f), CornerRadius(1.5f * f.u))
    for (i in -2..2) {
        val tx = handX + i * 2.1f
        drawRect(Color.White.copy(alpha = 0.85f), f.p(tx - 0.6f, cy + 3.2f), f.s(1.2f, 2.8f))
    }
    drawOval(socket, f.p(handX - 5.8f, cy - 4.5f), f.s(4.6f, 5.2f))
    drawOval(socket, f.p(handX + 1.2f, cy - 4.5f), f.s(4.6f, 5.2f))
    if (rare) {
        val eyeR = if (legendary) 1.8f else 1.35f
        drawCircle(glow, eyeR * f.u, f.p(handX - 3.5f, cy - 1.8f))
        drawCircle(glow, eyeR * f.u, f.p(handX + 3.5f, cy - 1.8f))
    }
    drawOval(boneShade, f.p(handX - 7.5f, cy - 8f), f.s(15f, 12f), style = Stroke(1.1f * f.u))
}

internal fun DrawScope.drawWeaponHeadMotifIcon(
    f: IconFrame,
    motif: WeaponHeadMotif,
    descriptor: EquipmentVisualDescriptor,
    tier: Int,
    topX: Float = 50f,
    topY: Float = 14f,
) {
    if (motif == WeaponHeadMotif.NONE) return
    val glow = descriptor.glowColor ?: descriptor.accentColor ?: Color(0xFF38BDF8)
    when (motif) {
        WeaponHeadMotif.SKULL_BONE, WeaponHeadMotif.SKULL_SOUL -> drawSkullStaffIcon(f, tier)
        WeaponHeadMotif.RUST_PATCHES -> {
            drawRect(Color(0xFF8B4513).copy(alpha = 0.55f), f.p(topX - 8f, topY + 8f), f.s(6f, 4f))
            drawRect(Color(0xFF654321).copy(alpha = 0.45f), f.p(topX + 2f, topY + 18f), f.s(5f, 3f))
        }
        WeaponHeadMotif.HOLY_CROSS -> {
            drawRect(Color(0xFFFDE047), f.p(topX - 1.5f, topY - 2f), f.s(3f, 14f))
            drawRect(Color(0xFFFDE047), f.p(topX - 6f, topY + 3f), f.s(12f, 3f))
        }
        WeaponHeadMotif.RADIANT_SUN -> {
            drawCircle(Color(0xFFFBBF24).copy(alpha = 0.35f), 18f * f.u, f.p(topX, topY + 4f))
            drawCircle(Color(0xFFFDE047), 8f * f.u, f.p(topX, topY + 4f))
        }
        WeaponHeadMotif.GRAVE_WOOD -> {
            drawRoundRect(descriptor.secondaryColor, f.p(topX - 10f, topY), f.s(20f, 12f), CornerRadius(6f * f.u))
            drawCircle(glow.copy(alpha = 0.35f), 8f * f.u, f.p(topX - 6f, topY + 8f))
            drawCircle(glow.copy(alpha = 0.35f), 8f * f.u, f.p(topX + 6f, topY + 8f))
        }
        WeaponHeadMotif.PHYLACTERY -> {
            drawRoundRect(descriptor.primaryColor, f.p(topX - 9f, topY - 2f), f.s(18f, 22f), CornerRadius(4f * f.u))
            drawCircle(glow.copy(alpha = 0.45f), 6f * f.u, f.p(topX, topY + 8f))
        }
        WeaponHeadMotif.STORM_ORB -> {
            drawCircle(glow.copy(alpha = 0.35f), 16f * f.u, f.p(topX, topY + 4f))
            drawCircle(glow, 9f * f.u, f.p(topX, topY + 4f))
        }
        WeaponHeadMotif.LEAF_SPROUT -> {
            val leaf = Path().apply {
                moveTo(topX * f.u, (topY + 10f) * f.u)
                quadraticTo((topX - 10f) * f.u, topY * f.u, topX * f.u, (topY - 8f) * f.u)
                quadraticTo((topX + 10f) * f.u, topY * f.u, topX * f.u, (topY + 10f) * f.u)
                close()
            }
            drawPath(leaf, glow)
        }
        WeaponHeadMotif.BRANCH_HOOK -> {
            drawRoundRect(descriptor.primaryColor, f.p(topX - 3f, topY + 4f), f.s(6f, 18f), CornerRadius(3f * f.u))
            val hook = Path().apply {
                moveTo(topX * f.u, (topY + 4f) * f.u)
                quadraticTo((topX + 14f) * f.u, (topY - 6f) * f.u, (topX + 8f) * f.u, (topY + 10f) * f.u)
            }
            drawPath(hook, descriptor.secondaryColor, style = Stroke(4f * f.u))
        }
        WeaponHeadMotif.ROOT_CROWN, WeaponHeadMotif.OAK_LEAVES -> {
            for (dx in listOf(-8f, 0f, 8f)) drawCircle(glow.copy(alpha = 0.7f), 5f * f.u, f.p(topX + dx, topY + 4f))
        }
        WeaponHeadMotif.RUNE_BANDS -> for (y in listOf(topY + 6f, topY + 14f, topY + 22f)) {
            drawRect(glow, f.p(topX - 8f, y), f.s(16f, 3f))
        }
        WeaponHeadMotif.MANA_CRYSTAL, WeaponHeadMotif.GLASS_ORB, WeaponHeadMotif.CALLER_GEM, WeaponHeadMotif.SANCTUARY_GEM -> {
            drawCircle(glow.copy(alpha = 0.28f), 14f * f.u, f.p(topX, topY + 4f))
            drawCircle(glow, 7.5f * f.u, f.p(topX, topY + 4f))
        }
        WeaponHeadMotif.GOLD_FILIGREE -> {
            drawCircle(Color(0xFFFBBF24).copy(alpha = 0.3f), 14f * f.u, f.p(topX, topY + 4f))
            drawCircle(Color(0xFFFDE047), 7f * f.u, f.p(topX, topY + 4f))
        }
        WeaponHeadMotif.CANDLE_FLAME -> {
            drawRoundRect(Color(0xFFFFF7ED), f.p(topX - 3f, topY + 2f), f.s(6f, 10f), CornerRadius(2f * f.u))
            drawCircle(Color(0xFFFBBF24), 5f * f.u, f.p(topX, topY - 2f))
        }
        WeaponHeadMotif.BATON_RINGS -> for (y in listOf(topY + 2f, topY + 10f, topY + 18f)) {
            drawOval(Color(0xFF94A3B8), f.p(topX - 8f, y), f.s(16f, 4f))
        }
        WeaponHeadMotif.SPIKED_STAR -> for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60).toDouble())
            drawLine(
                descriptor.secondaryColor,
                f.p(topX, topY + 4f),
                f.p(topX + kotlin.math.cos(angle).toFloat() * 12f, topY + 4f + kotlin.math.sin(angle).toFloat() * 12f),
                2.5f * f.u,
            )
        }
        WeaponHeadMotif.TWIN_BLADES -> {
            drawLine(descriptor.secondaryColor, f.p(topX - 8f, topY + 18f), f.p(topX - 2f, topY - 4f), 2.5f * f.u)
            drawLine(descriptor.secondaryColor, f.p(topX + 8f, topY + 18f), f.p(topX + 2f, topY - 4f), 2.5f * f.u)
        }
        WeaponHeadMotif.VOID_TEAR -> drawCircle(glow.copy(alpha = 0.5f), 12f * f.u, f.p(topX, topY + 4f))
        WeaponHeadMotif.NIGHT_CRESCENT -> drawArc(glow, 200f, 140f, false, f.p(topX - 10f, topY), f.s(20f, 20f), style = Stroke(3f * f.u))
        WeaponHeadMotif.DRAGON_SCALE -> drawOval(Color(0xFFEF4444).copy(alpha = 0.55f), f.p(topX - 8f, topY), f.s(16f, 12f))
        WeaponHeadMotif.STAR_FORGE -> drawCircle(glow, 10f * f.u, f.p(topX, topY + 4f))
        WeaponHeadMotif.HARP_STRINGS -> for (dx in listOf(-6f, -2f, 2f, 6f)) {
            drawLine(descriptor.accentColor ?: Color(0xFFF59E0B), f.p(topX + dx, topY), f.p(topX + dx, topY + 18f), 1f * f.u)
        }
        WeaponHeadMotif.APOCALYPSE_BELL -> {
            drawOval(descriptor.secondaryColor, f.p(topX - 9f, topY + 2f), f.s(18f, 14f))
            drawRect(descriptor.accentColor ?: Color(0xFFF59E0B), f.p(topX - 2f, topY - 4f), f.s(4f, 6f))
        }
        WeaponHeadMotif.MERCY_CROSS -> {
            drawRect(Color(0xFFFDE047), f.p(topX - 1.5f, topY), f.s(3f, 12f))
            drawRect(Color(0xFFFDE047), f.p(topX - 5f, topY + 4f), f.s(10f, 2.5f))
        }
        else -> Unit
    }
}

internal fun DrawScope.drawWeaponHeadMotifAvatar(
    f: AvatarFrame,
    motif: WeaponHeadMotif,
    descriptor: EquipmentVisualDescriptor,
    handX: Float,
    tier: Int,
    topY: Float = 13f,
) {
    when (motif) {
        WeaponHeadMotif.NONE -> Unit
        WeaponHeadMotif.SKULL_BONE, WeaponHeadMotif.SKULL_SOUL -> drawSkullStaffAvatar(f, handX, tier)
        WeaponHeadMotif.GRAVE_WOOD -> {
            drawRoundRect(descriptor.secondaryColor, f.p(handX - 4f, topY - 2f), f.s(8f, 6f), CornerRadius(3f * f.u))
            drawCircle((descriptor.glowColor ?: Color(0xFF4ADE80)).copy(alpha = 0.35f), 2.5f * f.u, f.p(handX - 2f, topY + 2f))
            drawCircle((descriptor.glowColor ?: Color(0xFF4ADE80)).copy(alpha = 0.35f), 2.5f * f.u, f.p(handX + 2f, topY + 2f))
        }
        WeaponHeadMotif.PHYLACTERY -> {
            drawRoundRect(descriptor.primaryColor, f.p(handX - 4.5f, topY - 6f), f.s(9f, 11f), CornerRadius(2f * f.u))
            drawCircle((descriptor.glowColor ?: Color(0xFFA78BFA)).copy(alpha = 0.5f), 2.8f * f.u, f.p(handX, topY - 1f))
        }
        WeaponHeadMotif.BRANCH_HOOK -> {
            val hook = Path().apply {
                moveTo(handX * f.u, (topY + 2f) * f.u)
                quadraticTo((handX + 8f) * f.u, (topY - 8f) * f.u, (handX + 4f) * f.u, (topY + 4f) * f.u)
            }
            drawPath(hook, descriptor.secondaryColor, style = Stroke(2.4f * f.u))
        }
        else -> {
            val glow = descriptor.glowColor ?: descriptor.accentColor
            if (glow != null) drawCircle(glow.copy(alpha = 0.35f), 4f * f.u, f.p(handX, topY))
        }
    }
}

internal fun DrawScope.drawBowIcon(
    f: IconFrame,
    descriptor: EquipmentVisualDescriptor,
    curve: Float,
    height: Float = 80f,
) {
    val bow = Path().apply {
        moveTo(18f * f.u, (50f - height / 2f) * f.u)
        quadraticTo((18f + curve) * f.u, 50f * f.u, 18f * f.u, (50f + height / 2f) * f.u)
    }
    drawPath(bow, descriptor.primaryColor, style = Stroke(4f * f.u))
    drawPath(bow, descriptor.secondaryColor, style = Stroke(2f * f.u))
    drawLine(descriptor.accentColor ?: Color(0xFFE2E8F0), f.p(18f, 50f - height / 2f), f.p(18f, 50f + height / 2f), 1.2f * f.u)
    descriptor.glowColor?.let { drawCircle(it, 4f * f.u, f.p(18f + curve * 0.7f, 50f)) }
}

internal fun DrawScope.drawSwordBladeIcon(
    f: IconFrame,
    descriptor: EquipmentVisualDescriptor,
    thin: Boolean = false,
) {
    val half = if (thin) 3f else 5f
    val blade = Path().apply {
        moveTo((50f - half) * f.u, 78f * f.u)
        lineTo((50f - half) * f.u, 18f * f.u)
        lineTo(50f * f.u, 8f * f.u)
        lineTo((50f + half) * f.u, 18f * f.u)
        lineTo((50f + half) * f.u, 78f * f.u)
        close()
    }
    drawPath(blade, descriptor.secondaryColor)
    drawPath(blade, descriptor.primaryColor, style = Stroke(if (thin) 1.5f else 2f * f.u))
    drawRoundRect(descriptor.accentColor ?: Color(0xFFB07E24), f.p(38f, 74f), f.s(24f, 7f), CornerRadius(3f * f.u))
    drawRoundRect(descriptor.primaryColor, f.p(47f, 81f), f.s(6f, 14f), CornerRadius(3f * f.u))
    drawWeaponHeadMotifIcon(f, descriptor.weaponHeadMotif, descriptor, f.item.tier, topX = 50f, topY = 6f)
}

internal fun DrawScope.drawStaffShaftIcon(
    f: IconFrame,
    descriptor: EquipmentVisualDescriptor,
    tier: Int,
    withOrb: Boolean = true,
) {
    drawRoundRect(descriptor.primaryColor, f.p(46f, 16f), f.s(8f, 76f), CornerRadius(4f * f.u))
    if (withOrb && descriptor.weaponHeadMotif == WeaponHeadMotif.NONE) {
        val orb = descriptor.glowColor ?: Color(0xFFC084FC)
        drawCircle(orb.copy(alpha = 0.3f), 16f * f.u, f.p(50f, 12f))
        drawCircle(orb, 9f * f.u, f.p(50f, 12f))
    } else {
        drawWeaponHeadMotifIcon(f, descriptor.weaponHeadMotif, descriptor, tier, topX = 50f, topY = 6f)
    }
}
