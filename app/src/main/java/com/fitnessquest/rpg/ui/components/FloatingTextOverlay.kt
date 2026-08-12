package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.MysticPurple

data class FloatingBurst(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val subtext: String? = null,
    val color: Color = Gold,
    val isPr: Boolean = false,
)

/**
 * Animated floating burst popup (+150 XP, +2 STR, PR Record!) that springs
 * upward with physics scale, gradient aura, and smooth alpha fade.
 */
@Composable
fun FloatingTextBurst(
    burst: FloatingBurst,
    onFinished: () -> Unit
) {
    val scaleAnim = remember { Animatable(0.2f) }
    val offsetYAnim = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(1f) }

    LaunchedEffect(burst.id) {
        // Explosive spring pop scale up
        scaleAnim.animateTo(
            targetValue = 1.25f,
            animationSpec = spring(dampingRatio = 0.45f, stiffness = 600f)
        )
        scaleAnim.animateTo(1.0f, tween(150))

        // Float upward
        offsetYAnim.animateTo(
            targetValue = -90f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )

        // Fade out
        alphaAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300)
        )
        onFinished()
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(0, offsetYAnim.value.toInt()) }
            .scale(scaleAnim.value)
            .alpha(alphaAnim.value),
        contentAlignment = Alignment.Center
    ) {
        val bgBrush = if (burst.isPr) {
            Brush.horizontalGradient(listOf(Color(0xFFD4AF37), Color(0xFFFF4500)))
        } else {
            Brush.horizontalGradient(listOf(burst.color.copy(alpha = 0.9f), MysticPurple.copy(alpha = 0.9f)))
        }

        Column(
            modifier = Modifier
                .background(bgBrush, RoundedCornerShape(12.dp))
                .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = burst.text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = if (burst.isPr) 19.sp else 16.sp
                ),
                color = Color.White
            )
            burst.subtext?.let { sub ->
                Text(
                    text = sub,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
