package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightSurface
import com.fitnessquest.rpg.ui.theme.Parchment
import kotlin.math.sin
import kotlin.random.Random

/** Global state for disabling intensive animations on low-end devices. */
val LocalLowPowerUi = staticCompositionLocalOf { false }

@Composable
fun FantasyToken(
    emoji: String,
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Gold,
    iconSize: Dp = 18.dp
) {
    val lowPower = LocalLowPowerUi.current
    
    val glowAlpha = if (lowPower) {
        1f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "tokenGlow")
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "glowAlpha"
        ).value
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = NightSurface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f * glowAlpha)),
        tonalElevation = 2.dp
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = (iconSize.value * 0.8f).sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Parchment
                )
            )
        }
    }
}

@Composable
fun FantasyCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = NightSurface,
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.3f)),
        tonalElevation = 4.dp
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
fun EmbersOverlay(
    modifier: Modifier = Modifier,
    color: Color = Gold.copy(alpha = 0.3f),
    particleCount: Int = 20,
    content: @Composable () -> Unit
) {
    val lowPower = LocalLowPowerUi.current
    
    if (lowPower) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "embers")
    
    val particles = remember {
        List(particleCount) {
            Particle(
                initialOffset = Offset(
                    Random.nextFloat() * 1000f,
                    Random.nextFloat() * 1000f
                ),
                speed = 100f + Random.nextFloat() * 200f,
                size = 2f + Random.nextFloat() * 3f,
                alpha = 0.1f + Random.nextFloat() * 0.4f,
                delay = Random.nextFloat() * 5f
            )
        }
    }

    val animationProgress = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Box(modifier = modifier.drawBehind {
        if (size.width > 0 && size.height > 0) {
            particles.forEach { particle ->
                val t = (animationProgress.value + particle.delay) % 1f
                val x = (particle.initialOffset.x + (t * size.width * 0.5f)) % size.width
                val y = (particle.initialOffset.y - (t * particle.speed * 20f)) % size.height
                val sway = sin(t * 6.28f + particle.initialOffset.x) * 10f
                
                drawCircle(
                    color = color.copy(alpha = particle.alpha),
                    radius = particle.size,
                    center = Offset(x + sway, y)
                )
            }
        }
    }) {
        content()
    }
}

private data class Particle(
    val initialOffset: Offset,
    val speed: Float,
    val size: Float,
    val alpha: Float,
    val delay: Float
)
