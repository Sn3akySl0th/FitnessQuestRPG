package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.MysticPurple

/**
 * Animated glowing border modifier for Legendary, Epic, and Rare RPG items.
 */
fun Modifier.rarityGlow(tier: Int, cornerRadius: Dp = 12.dp): Modifier = composed {
    if (tier < 3) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "rarityGlow")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha"
    )

    val gradient = when (tier) {
        3 -> listOf(MysticPurple.copy(alpha = alphaAnim), Color(0xFF8FA8C8).copy(alpha = alphaAnim))
        4 -> listOf(Gold.copy(alpha = alphaAnim), Color(0xFFFF4500).copy(alpha = alphaAnim), Gold.copy(alpha = alphaAnim))
        else -> listOf(Color(0xFFFFD700).copy(alpha = alphaAnim), MysticPurple.copy(alpha = alphaAnim))
    }

    this.border(
        width = 1.8.dp,
        brush = Brush.horizontalGradient(gradient),
        shape = RoundedCornerShape(cornerRadius)
    )
}
