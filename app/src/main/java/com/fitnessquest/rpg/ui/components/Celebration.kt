package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import com.fitnessquest.rpg.ui.theme.ArcaneBlue
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.HealthRed
import com.fitnessquest.rpg.ui.theme.MysticPurple
import com.fitnessquest.rpg.ui.theme.Parchment
import com.fitnessquest.rpg.ui.theme.StaminaGreen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val ConfettiColors = listOf(Gold, ArcaneBlue, MysticPurple, StaminaGreen, HealthRed, Parchment)

private class ConfettiPiece(
    val x0: Float,          // start position, fraction of width
    val y0: Float,          // start position, fraction of height
    val vx: Float,          // horizontal velocity, fraction of width per second
    val vy: Float,          // initial vertical velocity, fraction of height per second
    val spin: Float,        // degrees per second
    val phase: Float,       // sway phase offset
    val size: Float,        // px at 1x density; scaled at draw time
    val color: Color,
    val delay: Float        // seconds before this piece appears
)

private fun burst(pieces: Int, seed: Int): List<ConfettiPiece> {
    val rng = Random(seed)
    return List(pieces) {
        // Launch upward from the bottom half in a loose fan.
        val angle = (-90f + rng.nextFloat() * 70f - 35f) * (PI / 180f).toFloat()
        val speed = 0.9f + rng.nextFloat() * 1.4f
        ConfettiPiece(
            x0 = 0.2f + rng.nextFloat() * 0.6f,
            y0 = 0.85f + rng.nextFloat() * 0.1f,
            vx = cos(angle) * speed * 0.6f,
            vy = sin(angle) * speed,
            spin = (rng.nextFloat() - 0.5f) * 720f,
            phase = rng.nextFloat() * 2f * PI.toFloat(),
            size = 12f + rng.nextFloat() * 14f,
            color = ConfettiColors[rng.nextInt(ConfettiColors.size)],
            delay = rng.nextFloat() * 0.25f
        )
    }
}

/**
 * A one-shot confetti burst drawn over the composable's bounds.
 * Restarts whenever [trigger] changes. Place inside a Box, above the content.
 */
@Composable
fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    trigger: Any = Unit,
    pieces: Int = 90,
    durationSec: Float = 3.2f
) {
    val confetti = remember(trigger) { burst(pieces, trigger.hashCode()) }
    var elapsed by remember(trigger) { mutableFloatStateOf(0f) }

    LaunchedEffect(trigger) {
        val start = withFrameNanos { it }
        while (elapsed < durationSec) {
            withFrameNanos { now ->
                elapsed = (now - start) / 1_000_000_000f
            }
        }
    }
    if (elapsed >= durationSec) return

    Canvas(modifier) {
        val gravity = 0.6f // fraction of height per second^2 (floatier for magic)
        confetti.forEach { p ->
            val t = elapsed - p.delay
            if (t < 0f) return@forEach
            val fade = ((durationSec - p.delay - t) / 0.8f).coerceIn(0f, 1f)
            if (fade <= 0f) return@forEach
            val sway = sin(t * 5f + p.phase) * 0.05f
            val x = (p.x0 + p.vx * t + sway) * size.width
            val y = (p.y0 + p.vy * t + gravity * t * t / 2f) * size.height
            if (y > size.height + 40f) return@forEach
            
            // Pulsing magic glow
            val pulse = (0.8f + 0.2f * sin(t * 10f + p.phase))
            val radius = p.size * 0.8f * pulse
            
            // Outer aura
            drawCircle(
                color = p.color.copy(alpha = fade * 0.3f),
                radius = radius * 2.5f,
                center = Offset(x, y)
            )
            
            // Four-point magical sparkle
            rotate(p.spin * t, pivot = Offset(x, y)) {
                val path = androidx.compose.ui.graphics.Path()
                val s = radius * 1.8f
                path.moveTo(x, y - s)
                path.quadraticTo(x, y, x + s, y)
                path.quadraticTo(x, y, x, y + s)
                path.quadraticTo(x, y, x - s, y)
                path.quadraticTo(x, y, x, y - s)
                drawPath(path, color = p.color.copy(alpha = fade * 0.9f))
            }
            
            // Hot core
            drawCircle(
                color = Color.White.copy(alpha = fade * 0.8f),
                radius = radius * 0.4f,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * Screen Shake Modifier: Shakes screen graphics horizontally & vertically
 * when [trigger] changes (e.g. monster critical hits or heavy level up impact).
 */
fun Modifier.screenShake(trigger: Any?): Modifier = composed {
    val offsetX = remember(trigger) { Animatable(0f) }
    val offsetY = remember(trigger) { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger == null || trigger == false || trigger == 0) return@LaunchedEffect
        val steps = 6
        for (i in 0 until steps) {
            val intensity = (steps - i) * 5f
            val dx = (Random.nextFloat() * 2f - 1f) * intensity
            val dy = (Random.nextFloat() * 2f - 1f) * intensity
            offsetX.animateTo(dx, tween(30))
            offsetY.animateTo(dy, tween(30))
        }
        offsetX.animateTo(0f, tween(30))
        offsetY.animateTo(0f, tween(30))
    }

    this.graphicsLayer {
        translationX = offsetX.value
        translationY = offsetY.value
    }
}

/**
 * Animates an integer from 0 to [target] (e.g. "+128 XP" counting up).
 * Returns the current display value.
 */
@Composable
fun countUp(target: Int, durationMs: Int = 1200): Int {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) { anim.animateTo(1f, tween(durationMs)) }
    return (target * anim.value).toInt()
}
