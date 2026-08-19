package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.domain.GearRarity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manages equip burst animations, avatar spring pulses, and slot flashes when gear is modified.
 */
@Stable
class EquipAnimationState(private val scope: CoroutineScope) {
    var equippedSlot by mutableStateOf<ItemSlot?>(null)
        private set

    val avatarScale = Animatable(1f)
    val slotFlashAlpha = Animatable(0f)
    val burstRadius = Animatable(0f)

    fun triggerEquip(slot: ItemSlot, rarity: GearRarity = GearRarity.RARE) {
        equippedSlot = slot
        scope.launch {
            // 1. Spring scale pulse on avatar (1.0 -> 1.07 -> 1.0)
            avatarScale.snapTo(1f)
            avatarScale.animateTo(
                targetValue = 1.06f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            avatarScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        scope.launch {
            // 2. Quick flash burst on slot
            slotFlashAlpha.snapTo(1f)
            burstRadius.snapTo(0.2f)

            launch {
                burstRadius.animateTo(
                    targetValue = 1.4f,
                    animationSpec = tween(450, easing = FastOutSlowInEasing)
                )
            }

            slotFlashAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
            equippedSlot = null
        }
    }
}

@Composable
fun rememberEquipAnimationState(): EquipAnimationState {
    val scope = rememberCoroutineScope()
    return remember { EquipAnimationState(scope) }
}

/**
 * Overlay effect drawing a radiant flash ring over an equipped slot.
 */
@Composable
fun ItemEquipBurstEffect(
    animState: EquipAnimationState,
    slot: ItemSlot,
    rarity: GearRarity = GearRarity.RARE,
    modifier: Modifier = Modifier
) {
    if (animState.equippedSlot != slot && animState.slotFlashAlpha.value <= 0.01f) return

    val flashAlpha = animState.slotFlashAlpha.value
    val radiusMultiplier = animState.burstRadius.value
    val color = RarityVisuals.primaryColor(rarity)

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = (minOf(size.width, size.height) / 2f)
        val currentRadius = baseRadius * radiusMultiplier

        // Flash fill
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.6f * flashAlpha),
                    color.copy(alpha = 0.35f * flashAlpha),
                    Color.Transparent
                ),
                center = center,
                radius = currentRadius
            ),
            radius = currentRadius,
            center = center
        )

        // Expanding shockwave ring
        drawCircle(
            color = Color.White.copy(alpha = 0.8f * flashAlpha),
            radius = currentRadius,
            center = center,
            style = Stroke(width = (3f * (1f - flashAlpha)).coerceAtLeast(1f))
        )
    }
}
