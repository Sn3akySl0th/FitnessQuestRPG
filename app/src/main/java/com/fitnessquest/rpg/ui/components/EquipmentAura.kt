package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.fitnessquest.rpg.domain.GearRarity
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders a lightweight, high-fantasy background pedestal and elemental aura.
 * Highlights high-tier items (Epic, Legendary, Mythic) with ambient radial glows,
 * pulsing rune rings, and subtle floating embers.
 */
@Composable
fun EquipmentAura(
    rarity: GearRarity = GearRarity.LEGENDARY,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (!enabled || rarity < GearRarity.EPIC) return

    val infiniteTransition = rememberInfiniteTransition(label = "equipmentAura")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraPulse"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "runeRotation"
    )

    val auraColor = RarityVisuals.primaryColor(rarity)

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = (minOf(size.width, size.height) / 2f) * 0.95f

        // 1. Soft Ambient Radial Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    auraColor.copy(alpha = if (rarity == GearRarity.MYTHIC) 0.35f else 0.22f),
                    auraColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = center,
                radius = maxRadius * pulseScale
            ),
            radius = maxRadius * pulseScale,
            center = center
        )

        // 2. Faint Concentric Rune Ring
        if (rarity >= GearRarity.LEGENDARY) {
            val ringRadius = maxRadius * 0.75f
            drawCircle(
                color = auraColor.copy(alpha = 0.25f),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 1.2f)
            )

            // Orbiting Rune Node Sparks
            val nodeCount = if (rarity == GearRarity.MYTHIC) 6 else 4
            val radAngle = Math.toRadians(rotationAngle.toDouble())
            for (i in 0 until nodeCount) {
                val nodeRad = radAngle + (i * 2.0 * Math.PI / nodeCount)
                val nodeOffset = Offset(
                    x = center.x + (ringRadius * cos(nodeRad)).toFloat(),
                    y = center.y + (ringRadius * sin(nodeRad)).toFloat()
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.7f),
                    radius = 2.5f,
                    center = nodeOffset
                )
                drawCircle(
                    color = auraColor.copy(alpha = 0.4f),
                    radius = 5.5f,
                    center = nodeOffset
                )
            }
        }
    }
}
