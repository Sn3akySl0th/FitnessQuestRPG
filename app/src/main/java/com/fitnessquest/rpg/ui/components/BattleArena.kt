package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.domain.BattleOutcome
import com.fitnessquest.rpg.domain.BattleState
import com.fitnessquest.rpg.domain.Biome
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.MonsterTrait
import com.fitnessquest.rpg.ui.effects.AudioEffects
import com.fitnessquest.rpg.ui.effects.HapticEffects
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.roundToInt
import kotlin.random.Random

/** A visual effect to play in the arena, identified by [id] so repeats retrigger. */
data class ArenaFx(
    val id: Long,
    val kind: FxKind,
    val clazz: CharacterClass = CharacterClass.WARRIOR,
    val skillIndex: Int = 0
)

enum class FxKind { ATTACK, SKILL, DEFEND, MONSTER_HIT }

/**
 * The battle stage: hero and monster face off over a biome-themed backdrop,
 * with lunges, skill effects, floating damage, and status badges above heads.
 */
@Composable
fun BattleArena(
    battle: BattleState,
    gear: Map<ItemSlot, ItemEntity>,
    appearance: AvatarAppearance = AvatarAppearance(),
    fx: ArenaFx?,
    modifier: Modifier = Modifier
) {
    val biome = battle.monster.biome

    // ---- Animation drivers ----
    val fxProgress = remember { Animatable(1f) }
    val playerLunge = remember { Animatable(0f) }
    val monsterLunge = remember { Animatable(0f) }

    LaunchedEffect(fx?.id) {
        val f = fx ?: return@LaunchedEffect
        when (f.kind) {
            FxKind.ATTACK, FxKind.SKILL -> {
                // Melee classes charge in; casters send the effect across instead.
                val melee = f.kind == FxKind.ATTACK ||
                    f.clazz == CharacterClass.WARRIOR || f.clazz == CharacterClass.THIEF
                launch {
                    if (melee) {
                        playerLunge.animateTo(1f, tween(180))
                        playerLunge.animateTo(0f, tween(260))
                    } else {
                        playerLunge.animateTo(-0.3f, tween(140))
                        playerLunge.animateTo(0f, tween(200))
                    }
                }
                fxProgress.snapTo(0f)
                fxProgress.animateTo(1f, tween(durationFor(f), easing = LinearEasing))
            }
            FxKind.DEFEND -> {
                fxProgress.snapTo(0f)
                fxProgress.animateTo(1f, tween(600, easing = LinearEasing))
            }
            FxKind.MONSTER_HIT -> {
                launch {
                    monsterLunge.animateTo(1f, tween(160))
                    monsterLunge.animateTo(0f, tween(240))
                }
                fxProgress.snapTo(0f)
                fxProgress.animateTo(1f, tween(500, easing = LinearEasing))
            }
        }
    }

    // ---- Hit feedback (shake + damage numbers + sound) ----
    val monsterHit = rememberArenaHit(battle.monsterHp, key = battle.monster.id)
    val playerHit = rememberArenaHit(battle.playerHp, key = battle.monster.id)

    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(biome.colorA).copy(alpha = 0.75f), Color(biome.colorB))
                )
            )
    ) {
        // Scenery behind the combatants
        Canvas(Modifier.fillMaxSize()) { drawScenery(biome) }

        // ---- Monster (right side) ----
        val monsterEnraged = battle.monster.trait == MonsterTrait.ENRAGED &&
            battle.monsterHp < battle.monster.hp * 0.4
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp)
                .offset {
                    IntOffset(
                        (-monsterLunge.value * 46.dp.toPx()).roundToInt() + monsterHit.shakePx,
                        (monsterLunge.value * 14.dp.toPx()).roundToInt()
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusBadges(
                buildList {
                    if (monsterEnraged) add("\uD83D\uDE21")
                    if (battle.monsterChillTurns > 0) add("\u2744\uFE0F${battle.monsterChillTurns}")
                    if (battle.monsterStunTurns > 0) add("\uD83D\uDCAB${battle.monsterStunTurns}")
                    if (battle.monsterBleedTurns > 0) add("\uD83E\uDE78${battle.monsterBleedTurns}")
                }
            )
            Text(battle.monster.emoji, fontSize = 64.sp)
            Spacer(Modifier.height(2.dp))
            MiniHpBar(
                current = battle.monsterHp,
                max = battle.monster.hp,
                width = 110.dp,
                color = Color(0xFFE85D5D)
            )
        }

        // ---- Player (left side) ----
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 10.dp)
                .offset {
                    IntOffset(
                        (playerLunge.value * 52.dp.toPx()).roundToInt() + playerHit.shakePx,
                        (-playerLunge.value * 10.dp.toPx()).roundToInt()
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusBadges(
                buildList {
                    if (battle.atkBuffTurns > 0) add("\uD83D\uDCE3${battle.atkBuffTurns}")
                    if (battle.playerVanished) add("\uD83D\uDCA8")
                    if (battle.playerPoisonTurns > 0) add("\u2620\uFE0F${battle.playerPoisonTurns}")
                    if (battle.playerDefending) add("\uD83D\uDEE1\uFE0F")
                }
            )
            CharacterAvatar(
                clazz = battle.clazz,
                gear = gear,
                appearance = appearance.copy(druidForm = battle.druidForm),
                modifier = Modifier.width(88.dp),
                expression = when {
                    battle.outcome == BattleOutcome.VICTORY -> AvatarExpression.VICTORIOUS
                    battle.playerHp <= battle.playerStats.maxHp * 0.25 -> AvatarExpression.LOW_HEALTH
                    fx?.kind == FxKind.ATTACK || fx?.kind == FxKind.SKILL -> AvatarExpression.BATTLE_READY
                    else -> AvatarExpression.FOCUSED
                },
                detail = AvatarDetail.COMPACT
            )
            Spacer(Modifier.height(2.dp))
            MiniHpBar(
                current = battle.playerHp,
                max = battle.playerStats.maxHp,
                width = 110.dp,
                color = Color(0xFF5DC96B)
            )
        }

        // ---- Effects overlay ----
        if (fxProgress.value < 1f && fx != null) {
            Canvas(Modifier.fillMaxSize()) {
                drawFx(fx, fxProgress.value)
            }
        }

        // ---- Floating damage numbers ----
        monsterHit.damage?.let { dmg ->
            FloatingDamage(dmg, Modifier.align(Alignment.TopEnd).padding(top = 26.dp, end = 48.dp))
        }
        playerHit.damage?.let { dmg ->
            FloatingDamage(dmg, Modifier.align(Alignment.BottomStart).padding(start = 52.dp, bottom = 150.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------

@Composable
private fun StatusBadges(badges: List<String>) {
    // Fixed height so combatants don't jump when statuses appear.
    Row(
        Modifier.height(22.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        badges.forEach { badge ->
            Text(
                badge,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun MiniHpBar(current: Int, max: Int, width: androidx.compose.ui.unit.Dp, color: Color) {
    val frac = (current.toFloat() / max).coerceIn(0f, 1f)
    Box(
        Modifier
            .width(width)
            .height(9.dp)
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(5.dp))
            .padding(1.5.dp)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width((width - 3.dp) * frac)
                .background(color, RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun FloatingDamage(damage: Int, modifier: Modifier = Modifier) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(damage) {
        animProgress.animateTo(1f, tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    
    val floatY = -(animProgress.value * 50f)
    val alpha = 1f - (animProgress.value * animProgress.value)
    val scale = 1f + (animProgress.value * 0.4f)
    
    Box(
        modifier = modifier
            .offset(y = floatY.dp)
            .graphicsLayer { 
                this.alpha = alpha 
                this.scaleX = scale
                this.scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        // Outline for RPG pop
        val text = "-$damage"
        val style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Black
        )
        Text(text, style = style, color = Color.Black, modifier = Modifier.offset(x = 2.dp, y = 2.dp))
        Text(text, style = style, color = Color.Black, modifier = Modifier.offset(x = (-2).dp, y = (-2).dp))
        Text(text, style = style, color = Color.Black, modifier = Modifier.offset(x = 2.dp, y = (-2).dp))
        Text(text, style = style, color = Color.Black, modifier = Modifier.offset(x = (-2).dp, y = 2.dp))
        
        // Inner fill
        Text(text, style = style, color = Color(0xFFFF4040))
    }
}

/** Tracks an HP value: shake offset, transient damage number, hit sound. */
private class ArenaHit(val shakePx: Int, val damage: Int?)

@Composable
private fun rememberArenaHit(hp: Int, key: Any): ArenaHit {
    var prevHp by remember(key) { mutableStateOf(hp) }
    var damage by remember(key) { mutableStateOf<Int?>(null) }
    val shake = remember(key) { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(hp) {
        if (hp < prevHp) {
            damage = prevHp - hp
            prevHp = hp
            AudioEffects.playCritHit()
            HapticEffects.performCritHit(haptic, context)
            launch {
                repeat(3) {
                    shake.animateTo(20f, tween(40))
                    shake.animateTo(-20f, tween(40))
                }
                shake.animateTo(0f, tween(40))
            }
            delay(900.milliseconds)
            damage = null
        } else {
            prevHp = hp
        }
    }
    return ArenaHit(shake.value.toInt(), damage)
}

// ---------------------------------------------------------------------------
// Scenery
// ---------------------------------------------------------------------------

private fun DrawScope.drawScenery(biome: Biome) {
    val w = size.width
    val h = size.height

    // Sun or moon
    val celestial = when (biome) {
        Biome.FROZEN_WASTES, Biome.SHADOWFEN, Biome.DARKWOOD -> Color(0xFFDDE4F0).copy(alpha = 0.55f)
        else -> Color(0xFFFFE9A8).copy(alpha = 0.7f)
    }
    drawCircle(
        Brush.radialGradient(
            listOf(celestial, Color.Transparent),
            center = Offset(w * 0.18f, h * 0.2f),
            radius = w * 0.16f
        ),
        radius = w * 0.16f,
        center = Offset(w * 0.18f, h * 0.2f)
    )
    drawCircle(celestial, radius = w * 0.055f, center = Offset(w * 0.18f, h * 0.2f))

    val silhouette = Color(biome.colorB).copy(alpha = 0.85f)
    val accent = Color.White.copy(alpha = 0.10f)

    when (biome) {
        Biome.MEADOWLANDS -> {
            // Rolling hills
            drawOval(silhouette, topLeft = Offset(-w * 0.25f, h * 0.62f), size = Size(w * 0.9f, h * 0.5f))
            drawOval(silhouette.copy(alpha = 0.6f), topLeft = Offset(w * 0.4f, h * 0.68f), size = Size(w * 0.9f, h * 0.5f))
        }
        Biome.DARKWOOD -> {
            // Pine silhouettes
            for ((x, s) in listOf(0.08f to 1f, 0.3f to 0.7f, 0.55f to 0.85f, 0.86f to 0.65f)) {
                pine(Offset(w * x, h * 0.72f), w * 0.10f * s, h * 0.38f * s, silhouette)
            }
        }
        Biome.CRYSTAL_CAVES -> {
            for ((x, s) in listOf(0.1f to 1f, 0.28f to 0.6f, 0.62f to 0.75f, 0.88f to 1.1f)) {
                spike(Offset(w * x, h * 0.85f), w * 0.07f * s, h * 0.4f * s, Color(0xFF8FB4E8).copy(alpha = 0.35f))
            }
        }
        Biome.EMBER_PEAKS -> {
            spike(Offset(w * 0.2f, h * 0.8f), w * 0.22f, h * 0.5f, silhouette)
            spike(Offset(w * 0.65f, h * 0.8f), w * 0.28f, h * 0.62f, silhouette.copy(alpha = 0.7f))
            // Lava glow at the horizon
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xFFFF6B35).copy(alpha = 0.22f)),
                    startY = h * 0.6f, endY = h
                ),
                topLeft = Offset(0f, h * 0.6f),
                size = Size(w, h * 0.4f)
            )
        }
        Biome.FROZEN_WASTES -> {
            for ((x, s) in listOf(0.12f to 0.8f, 0.4f to 1.1f, 0.75f to 0.7f)) {
                spike(Offset(w * x, h * 0.85f), w * 0.12f * s, h * 0.34f * s, Color.White.copy(alpha = 0.20f))
            }
        }
        Biome.SHADOWFEN -> {
            // Fog bands
            for ((y, a) in listOf(0.55f to 0.10f, 0.7f to 0.14f, 0.85f to 0.18f)) {
                drawOval(
                    Color(0xFFB8C4D8).copy(alpha = a),
                    topLeft = Offset(-w * 0.1f, h * y),
                    size = Size(w * 1.3f, h * 0.12f)
                )
            }
        }
    }

    // Ground
    drawOval(accent, topLeft = Offset(w * 0.06f, h * 0.86f), size = Size(w * 0.34f, h * 0.09f))
    drawOval(accent, topLeft = Offset(w * 0.6f, h * 0.78f), size = Size(w * 0.32f, h * 0.08f))
}

private fun DrawScope.pine(base: Offset, halfWidth: Float, height: Float, color: Color) {
    val p = Path().apply {
        moveTo(base.x - halfWidth, base.y)
        lineTo(base.x + halfWidth, base.y)
        lineTo(base.x, base.y - height)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.spike(base: Offset, halfWidth: Float, height: Float, color: Color) {
    val p = Path().apply {
        moveTo(base.x - halfWidth, base.y)
        lineTo(base.x + halfWidth, base.y)
        lineTo(base.x + halfWidth * 0.15f, base.y - height)
        close()
    }
    drawPath(p, color)
}

// ---------------------------------------------------------------------------
// Skill / action effects
// ---------------------------------------------------------------------------

private fun durationFor(fx: ArenaFx): Int = when (fx.kind) {
    FxKind.SKILL -> when (fx.clazz) {
        CharacterClass.MAGE -> if (fx.skillIndex == 2) 900 else 650
        CharacterClass.RANGER -> if (fx.skillIndex == 2) 900 else 600
        else -> 550
    }
    else -> 500
}

private fun DrawScope.drawFx(fx: ArenaFx, p: Float) {
    val w = size.width
    val h = size.height
    // Anchor points roughly at each combatant's chest height.
    val player = Offset(w * 0.22f, h * 0.62f)
    val monster = Offset(w * 0.78f, h * 0.45f)

    when (fx.kind) {
        FxKind.ATTACK -> slash(monster, p, Color.White, w * 0.09f)
        FxKind.DEFEND -> shield(player, p, w)
        FxKind.MONSTER_HIT -> {
            claw(player, p, w * 0.10f)
            // Red flash over the hero's side of the arena
            drawCircle(
                Color(0xFFFF3B30).copy(alpha = (1f - p) * 0.18f),
                radius = w * 0.3f,
                center = player
            )
        }
        FxKind.SKILL -> when (fx.clazz) {
            CharacterClass.WARRIOR -> when (fx.skillIndex) {
                0 -> slash(monster, p, Color(0xFFFFB74D), w * 0.13f)
                1 -> rings(player, p, Color(0xFFF0C040), w)
                else -> {
                    slash(monster, p, Color(0xFFFF8A50), w * 0.15f)
                    burst(monster, p, Color(0xFFFFD54F), w * 0.2f)
                }
            }
            CharacterClass.MAGE -> when (fx.skillIndex) {
                0 -> projectile(player, monster, p, Color(0xFFB388FF), w * 0.030f, burstColor = Color(0xFFD1B3FF))
                1 -> projectile(player, monster, p, Color(0xFF80D8FF), w * 0.026f, burstColor = Color(0xFFB3E5FC))
                else -> meteor(monster, p, w, h)
            }
            CharacterClass.THIEF -> when (fx.skillIndex) {
                0 -> slash(monster, p, Color(0xFF9C7BE3), w * 0.11f)
                1 -> {
                    slash(monster, p, Color(0xFFB0BEC5), w * 0.08f)
                    smoke(player, p, w)
                }
                else -> {
                    slash(monster, p, Color(0xFFFF5252), w * 0.14f)
                    slash(monster, (p + 0.2f).coerceAtMost(1f), Color(0xFFFF8A80), w * 0.11f)
                }
            }
            CharacterClass.RANGER -> when (fx.skillIndex) {
                0 -> arrow(player, monster, p, w)
                1 -> {
                    arrow(player, monster, p, w, yOffset = -h * 0.03f)
                    arrow(player, monster, (p - 0.18f).coerceAtLeast(0f), w, yOffset = h * 0.03f)
                }
                else -> volley(monster, p, w, h)
            }
            CharacterClass.PALADIN -> when (fx.skillIndex) {
                0 -> slash(monster, p, Color(0xFFFFF59D), w * 0.12f)
                1 -> shield(player, p, w)
                else -> {
                    slash(monster, p, Color(0xFFFFE082), w * 0.14f)
                    burst(monster, p, Color(0xFFFFF8E1), w * 0.18f)
                }
            }
            CharacterClass.NECROMANCER -> when (fx.skillIndex) {
                0 -> projectile(player, monster, p, Color(0xFF7CF0C0), w * 0.028f, burstColor = Color(0xFFB2FFDA))
                1 -> rings(player, p, Color(0xFF90A4AE), w)
                else -> {
                    projectile(player, monster, p, Color(0xFFCE93D8), w * 0.032f, burstColor = Color(0xFFE1BEE7))
                    smoke(monster, p, w)
                }
            }
            else -> slash(monster, p, Color.Cyan, w * 0.12f)
        }
    }
}

private fun lerp(a: Offset, b: Offset, t: Float) =
    Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

/** Crossed slash strokes that sweep in and fade. */
private fun DrawScope.slash(center: Offset, p: Float, color: Color, radius: Float) {
    val alpha = (1f - p).coerceIn(0f, 1f)
    val reach = radius * (0.5f + p)
    val stroke = radius * 0.16f
    drawLine(
        color.copy(alpha = alpha),
        start = Offset(center.x - reach, center.y - reach),
        end = Offset(center.x + reach, center.y + reach),
        strokeWidth = stroke
    )
    drawLine(
        color.copy(alpha = alpha * 0.8f),
        start = Offset(center.x + reach * 0.8f, center.y - reach),
        end = Offset(center.x - reach * 0.8f, center.y + reach),
        strokeWidth = stroke * 0.75f
    )
}

/** Three raking claw lines across the target. */
private fun DrawScope.claw(center: Offset, p: Float, radius: Float) {
    val alpha = (1f - p).coerceIn(0f, 1f)
    val reach = radius * (0.6f + p * 0.8f)
    for (i in -1..1) {
        val off = i * radius * 0.5f
        drawLine(
            Color(0xFFFF5252).copy(alpha = alpha),
            start = Offset(center.x - reach + off, center.y - reach),
            end = Offset(center.x + reach * 0.6f + off, center.y + reach),
            strokeWidth = radius * 0.13f
        )
    }
}

/** A glowing bolt that travels from caster to target, bursting on impact. */
private fun DrawScope.projectile(
    from: Offset,
    to: Offset,
    p: Float,
    color: Color,
    radius: Float,
    burstColor: Color
) {
    if (p < 0.75f) {
        val t = p / 0.75f
        val pos = lerp(from, to, t)
        // Trail
        for (i in 1..3) {
            drawCircle(
                color.copy(alpha = 0.25f / i),
                radius = radius * (1f - i * 0.18f),
                center = lerp(from, to, (t - i * 0.06f).coerceAtLeast(0f))
            )
        }
        drawCircle(color.copy(alpha = 0.35f), radius = radius * 1.9f, center = pos)
        drawCircle(color, radius = radius, center = pos)
    } else {
        val t = (p - 0.75f) / 0.25f
        burst(to, t, burstColor, radius * 7f)
    }
}

/** Expanding impact burst: ring + spokes. */
private fun DrawScope.burst(center: Offset, t: Float, color: Color, maxRadius: Float) {
    val alpha = (1f - t).coerceIn(0f, 1f)
    val r = maxRadius * t
    drawCircle(color.copy(alpha = alpha * 0.8f), radius = r, center = center, style = Stroke(width = maxRadius * 0.08f))
    val rng = Random(42)
    repeat(7) {
        val angle = rng.nextFloat() * 6.2832f
        val end = Offset(
            center.x + r * 1.15f * kotlin.math.cos(angle),
            center.y + r * 1.15f * kotlin.math.sin(angle)
        )
        drawLine(
            color.copy(alpha = alpha),
            start = lerp(center, end, 0.55f),
            end = end,
            strokeWidth = maxRadius * 0.045f
        )
    }
}

/** War-cry style expanding rings around the caster. */
private fun DrawScope.rings(center: Offset, p: Float, color: Color, w: Float) {
    for (i in 0..2) {
        val t = (p - i * 0.15f).coerceIn(0f, 1f)
        if (t <= 0f) continue
        drawCircle(
            color.copy(alpha = (1f - t) * 0.7f),
            radius = w * 0.16f * t,
            center = center,
            style = Stroke(width = w * 0.012f)
        )
    }
}

/** Smoke puffs swirling around the caster. */
private fun DrawScope.smoke(center: Offset, p: Float, w: Float) {
    val alpha = (1f - p) * 0.5f
    val rng = Random(7)
    repeat(5) {
        val dx = (rng.nextFloat() - 0.5f) * w * 0.16f
        val dy = (rng.nextFloat() - 0.7f) * w * 0.14f
        drawCircle(
            Color(0xFF90A4AE).copy(alpha = alpha),
            radius = w * (0.03f + 0.05f * p) * (0.7f + rng.nextFloat() * 0.6f),
            center = Offset(center.x + dx * (1f + p), center.y + dy * (1f + p))
        )
    }
}

/** Shield arc that flashes in front of the defender. */
private fun DrawScope.shield(center: Offset, p: Float, w: Float) {
    val alpha = (1f - p).coerceIn(0f, 1f)
    drawArc(
        Color(0xFF64B5F6).copy(alpha = alpha * 0.9f),
        startAngle = -70f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(center.x - w * 0.1f, center.y - w * 0.13f),
        size = Size(w * 0.2f, w * 0.26f),
        style = Stroke(width = w * 0.018f)
    )
    drawCircle(
        Color(0xFF64B5F6).copy(alpha = alpha * 0.15f),
        radius = w * 0.13f,
        center = center
    )
}

/** A single arrow flying from archer to target. */
private fun DrawScope.arrow(
    from: Offset,
    to: Offset,
    p: Float,
    w: Float,
    yOffset: Float = 0f
) {
    if (p <= 0f) return
    if (p < 0.8f) {
        val t = p / 0.8f
        val start = Offset(from.x, from.y + yOffset)
        val end = Offset(to.x, to.y + yOffset)
        val pos = lerp(start, end, t)
        val dir = Offset(end.x - start.x, end.y - start.y)
        val len = kotlin.math.sqrt(dir.x * dir.x + dir.y * dir.y)
        val unit = Offset(dir.x / len, dir.y / len)
        val tail = Offset(pos.x - unit.x * w * 0.07f, pos.y - unit.y * w * 0.07f)
        drawLine(Color(0xFFD7CCC8), start = tail, end = pos, strokeWidth = w * 0.008f)
        // Arrowhead
        drawCircle(Color(0xFFEEEEEE), radius = w * 0.012f, center = pos)
    } else {
        burst(Offset(to.x, to.y + yOffset), (p - 0.8f) / 0.2f, Color(0xFFA5D6A7), w * 0.12f)
    }
}

/** Arrows raining down on the target. */
private fun DrawScope.volley(target: Offset, p: Float, w: Float, h: Float) {
    val rng = Random(13)
    repeat(6) { i ->
        val t = (p * 1.5f - i * 0.12f).coerceIn(0f, 1f)
        if (t <= 0f) return@repeat
        val x = target.x + (rng.nextFloat() - 0.5f) * w * 0.24f
        val startY = -h * 0.05f
        val endY = target.y + (rng.nextFloat() - 0.3f) * h * 0.1f
        if (t < 0.85f) {
            val y = startY + (endY - startY) * (t / 0.85f)
            drawLine(
                Color(0xFFD7CCC8),
                start = Offset(x, y - h * 0.05f),
                end = Offset(x, y),
                strokeWidth = w * 0.007f
            )
        } else {
            burst(Offset(x, endY), (t - 0.85f) / 0.15f, Color(0xFFA5D6A7), w * 0.07f)
        }
    }
}

/** A meteor crashing down from the sky onto the target. */
private fun DrawScope.meteor(target: Offset, p: Float, w: Float, h: Float) {
    if (p < 0.6f) {
        val t = p / 0.6f
        val start = Offset(target.x + w * 0.18f, -h * 0.1f)
        val pos = lerp(start, target, t)
        // Fire trail
        for (i in 1..4) {
            drawCircle(
                Color(0xFFFF7043).copy(alpha = 0.3f / i),
                radius = w * 0.045f * (1f + i * 0.25f),
                center = lerp(start, target, (t - i * 0.07f).coerceAtLeast(0f))
            )
        }
        drawCircle(Color(0xFFFFAB40), radius = w * 0.05f, center = pos)
        drawCircle(Color(0xFFFFE082), radius = w * 0.028f, center = pos)
    } else {
        val t = (p - 0.6f) / 0.4f
        burst(target, t, Color(0xFFFFAB40), w * 0.3f)
        drawCircle(
            Color(0xFFFF7043).copy(alpha = (1f - t) * 0.25f),
            radius = w * 0.34f * t + w * 0.05f,
            center = target
        )
    }
}
