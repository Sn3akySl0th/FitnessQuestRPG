package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.CharacterRace
import com.fitnessquest.rpg.domain.build
import com.fitnessquest.rpg.domain.ItemStyle
import com.fitnessquest.rpg.domain.visuals.BodyRegion
import com.fitnessquest.rpg.ui.effects.rememberDeviceTilt
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.abs
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Base body palette
// ---------------------------------------------------------------------------

data class AvatarAppearance(
    val skinColor: Color = Color(0xFFE3B187.toInt()),
    val hairColor: Color = Color(0xFF6B4A32.toInt()),
    val underwearColor: Color = Color(0xFF4E4656.toInt()),
    val eyeColor: Color = Color(0xFF2A2233.toInt()),
    val hairStyle: String = "short",
    val gender: String = "male",
    val braColor: Color = Color(0xFF4E4656.toInt()),
    val race: CharacterRace = CharacterRace.HUMAN,
    val druidForm: String = "HUMAN",
)

private val DefaultAppearance = AvatarAppearance()
private val VisorSlit = Color(0xFF1E1A26)

fun com.fitnessquest.rpg.data.db.CharacterEntity.toAppearance() = AvatarAppearance(
    skinColor = Color(this.skinColor.toInt()).copy(alpha = 1f),
    hairColor = Color(this.hairColor.toInt()).copy(alpha = 1f),
    underwearColor = Color(this.underwearColor.toInt()).copy(alpha = 1f),
    eyeColor = Color(this.eyeColor.toInt()).copy(alpha = 1f),
    hairStyle = this.hairStyle,
    gender = this.gender,
    braColor = Color(this.braColor.toInt()).copy(alpha = 1f),
    race = CharacterRace.fromStored(this.race),
    druidForm = this.druidForm
)

// Default cloth colors used only for the class-costume preview.
private val Pants = Color(0xFF4A4258)
private val Boots = Color(0xFF5C4632)

internal data class ClassLook(
    val outfit: Color,
    val outfitDark: Color,
    val accent: Color,
    val headgear: Color
)

internal fun lookFor(cls: CharacterClass): ClassLook = when (cls) {
    CharacterClass.WARRIOR -> ClassLook(Color(0xFFB0473C), Color(0xFF7E2F27), Color(0xFFF0A830), Color(0xFF9AA3AD))
    CharacterClass.MAGE -> ClassLook(Color(0xFF6B4FA3), Color(0xFF4A3572), Color(0xFF9C7BE3), Color(0xFF54418C))
    CharacterClass.THIEF -> ClassLook(Color(0xFF3E524C), Color(0xFF2A3A35), Color(0xFF6BC96B), Color(0xFF2E3D38))
    CharacterClass.RANGER -> ClassLook(Color(0xFF4E7A3A), Color(0xFF365427), Color(0xFFD9A441), Color(0xFF5C8A46))
    CharacterClass.PALADIN -> ClassLook(Color(0xFFD4C48A), Color(0xFF9A8648), Color(0xFFF5E6A8), Color(0xFFE8D9A0))
    CharacterClass.NECROMANCER -> ClassLook(Color(0xFF3A2A48), Color(0xFF241832), Color(0xFF7CF0C0), Color(0xFF2A1E38))
    CharacterClass.WHITE_MAGE -> ClassLook(Color(0xFFF0F4F8), Color(0xFFC0D0E0), Color(0xFFFFD700), Color(0xFFFFFFFF))
    CharacterClass.MONK -> ClassLook(Color(0xFFD2691E), Color(0xFF8B4513), Color(0xFFFFD700), Color(0xFFA0522D))
    CharacterClass.DRUID -> ClassLook(Color(0xFF2E8B57), Color(0xFF1E5B37), Color(0xFF8FBC8F), Color(0xFF3CB371))
    CharacterClass.BERSERKER -> ClassLook(Color(0xFF800000), Color(0xFF4A0000), Color(0xFFFF4500), Color(0xFF8B0000))
    CharacterClass.BARD -> ClassLook(Color(0xFF9932CC), Color(0xFF4B0082), Color(0xFFFF69B4), Color(0xFFBA55D3))
    CharacterClass.SUMMONER -> ClassLook(Color(0xFF9C7BE3), Color(0xFF1C1E30), Color(0xFFF0A830), Color(0xFF7BB4E3))
    CharacterClass.DRAGOON -> ClassLook(Color(0xFF2A2C39), Color(0xFF1A1B24), Color(0xFF5D101D), Color(0xFF3A3E59))
}



// ---------------------------------------------------------------------------
// Gear visuals registry
// ---------------------------------------------------------------------------

/** Colors used to render one piece of gear. */
data class GearPalette(
    val main: Color,
    val dark: Color,
    /** Non-null on legendary gear: trim and auras glow in this color. */
    val glow: Color? = null
)

/** How a headgear item is shaped on the hero. */
enum class HeadgearShape { HELM, HAT, HOOD, CAP }

/**
 * Single source of truth for what equipment looks like on the avatar.
 * Palettes and silhouettes are derived from item data (style/tier/affinity),
 * so new items - including future stat-less cosmetics - render without
 * touching the layer renderer below.
 */
object GearVisuals {

    val LegendaryGlow = Color(0xFFF8D24A)
    private val ArcaneGlow = Color(0xFFC9A8FF)
    private val ShadowGlow = Color(0xFF9C7BE3)

    fun palette(item: ItemEntity): GearPalette = when (item.style) {
        // Cloth & arcane: robes shouldn't look like sheet metal.
        ItemStyle.ROBE -> when (item.tier) {
            1, 2 -> GearPalette(Color(0xFF7C5CB8), Color(0xFF54418C))
            3 -> GearPalette(Color(0xFF4A6FD8), Color(0xFF3450A8))
            else -> GearPalette(Color(0xFF8A5CE8), Color(0xFF5C36B0), ArcaneGlow)
        }
        // Leather & stealth: earthy up to shadowy midnight.
        ItemStyle.LIGHT -> when (item.tier) {
            1 -> GearPalette(Color(0xFF8B6B4A), Color(0xFF6B5138))
            2 -> GearPalette(Color(0xFF6E8B57), Color(0xFF4E6B3E))
            3 -> GearPalette(Color(0xFF5A6B78), Color(0xFF3E4C58))
            else -> GearPalette(Color(0xFF3A3D5C), Color(0xFF282A44), ShadowGlow)
        }
        // Plate & everything else: leather -> iron -> steel -> gold.
        else -> when (item.tier) {
            1 -> GearPalette(Color(0xFF8B6B4A), Color(0xFF6B5138))
            2 -> GearPalette(Color(0xFF9AA3AD), Color(0xFF767E87))
            3 -> GearPalette(Color(0xFF8FA8C8), Color(0xFF6E88A8))
            else -> GearPalette(Color(0xFFF0C040), Color(0xFFC89B26), LegendaryGlow)
        }
    }

    fun headgearShape(item: ItemEntity): HeadgearShape = when {
        item.style == ItemStyle.PLATE -> HeadgearShape.HELM
        (item.classAffinity == CharacterClass.NECROMANCER) ||
            (item.classAffinity == CharacterClass.THIEF) ||
            (item.classAffinity == CharacterClass.RANGER) -> HeadgearShape.HOOD
        item.style == ItemStyle.ROBE -> HeadgearShape.HAT
        else -> HeadgearShape.CAP
    }

    fun trinketColor(id: Long): Color = when (id) {
        9L -> Color(0xFFE3B54B)   // sweatband: gold medal
        10L -> Color(0xFF6BC96B)  // endurance: green
        11L -> Color(0xFFE35B5B)  // fury: red
        else -> Color(0xFFFF7B4F) // colossus heart: ember
    }
}

enum class HeroAnimation {
    IDLE, SQUAT, PRESS, PULLUP, RUNNING, YOGA, SWIMMING, BURPEES
}

data class AvatarPose(
    val bodyOffset: Offset = Offset.Zero,
    val headOffset: Offset = Offset.Zero,
    val leftArmOffset: Offset = Offset.Zero,
    val leftArmRotation: Float = 0f,
    val rightArmOffset: Offset = Offset.Zero,
    val rightArmRotation: Float = 0f,
    val leftLegOffset: Offset = Offset.Zero,
    val leftLegRotation: Float = 0f,
    val rightLegOffset: Offset = Offset.Zero,
    val rightLegRotation: Float = 0f,
    val weaponOffset: Offset = Offset.Zero,
    val weaponRotation: Float = 0f,
    val prop: HeroAnimation? = null
)

internal fun calculatePose(anim: HeroAnimation, phase: Float, u: Float): AvatarPose {
    return when (anim) {
        HeroAnimation.IDLE -> AvatarPose()
        HeroAnimation.SQUAT -> {
            // Body moves down, knees bend outwards, arms hold log horizontally
            AvatarPose(
                bodyOffset = Offset(0f, phase * 18f * u),
                headOffset = Offset(0f, phase * 18f * u),
                leftArmOffset = Offset(0f, phase * 18f * u),
                rightArmOffset = Offset(0f, phase * 18f * u),
                leftArmRotation = -90f,
                rightArmRotation = 90f,
                leftLegRotation = phase * 20f,
                rightLegRotation = phase * -20f,
                prop = HeroAnimation.SQUAT
            )
        }
        HeroAnimation.RUNNING -> {
            val cycle = phase * kotlin.math.PI * 2
            val armSwing = sin(cycle).toFloat() * 45f
            val legSwing = sin(cycle).toFloat() * 30f
            val bounce = abs(sin(cycle)).toFloat() * -3f * u
            AvatarPose(
                bodyOffset = Offset(0f, bounce),
                headOffset = Offset(0f, bounce),
                leftArmOffset = Offset(0f, bounce),
                rightArmOffset = Offset(0f, bounce),
                leftLegOffset = Offset(0f, bounce),
                rightLegOffset = Offset(0f, bounce),
                leftArmRotation = armSwing,
                rightArmRotation = -armSwing,
                leftLegRotation = -legSwing,
                rightLegRotation = legSwing
            )
        }
        HeroAnimation.PRESS -> {
            // Arms push up vertically by rotating from horizontal to vertical
            AvatarPose(
                leftArmRotation = 90f + (phase * 90f),
                rightArmRotation = -90f - (phase * 90f),
                prop = HeroAnimation.PRESS
            )
        }
        HeroAnimation.PULLUP -> {
            // Body pulls up
            AvatarPose(
                bodyOffset = Offset(0f, phase * -15f * u),
                headOffset = Offset(0f, phase * -15f * u),
                leftLegOffset = Offset(0f, phase * -15f * u),
                rightLegOffset = Offset(0f, phase * -15f * u),
                leftArmOffset = Offset(0f, phase * -15f * u),
                rightArmOffset = Offset(0f, phase * -15f * u),
                leftArmRotation = -180f,
                rightArmRotation = 180f,
                prop = HeroAnimation.PULLUP
            )
        }
        HeroAnimation.YOGA -> {
            // Warrior pose breathing
            AvatarPose(
                bodyOffset = Offset(0f, phase * 3f * u),
                headOffset = Offset(0f, phase * 3f * u),
                leftArmOffset = Offset(0f, phase * 3f * u),
                rightArmOffset = Offset(0f, phase * 3f * u),
                leftArmRotation = -110f + (phase * 10f),
                rightArmRotation = 110f - (phase * 10f),
                leftLegRotation = -20f,
                rightLegRotation = 20f
            )
        }
        HeroAnimation.SWIMMING -> {
            val cycle = phase * kotlin.math.PI * 2
            val armSwing = kotlin.math.sin(cycle).toFloat() * 180f
            val bounce = kotlin.math.sin(cycle * 2).toFloat() * 5f * u
            AvatarPose(
                bodyOffset = Offset(0f, bounce),
                headOffset = Offset(0f, bounce),
                leftArmOffset = Offset(0f, bounce),
                rightArmOffset = Offset(0f, bounce),
                leftLegOffset = Offset(0f, bounce),
                rightLegOffset = Offset(0f, bounce),
                leftArmRotation = armSwing,
                rightArmRotation = -armSwing,
                leftLegRotation = -30f,
                rightLegRotation = -30f
            )
        }
        HeroAnimation.BURPEES -> {
            // Down to floor, jump up
            val bodyY = phase * 25f * u
            AvatarPose(
                bodyOffset = Offset(0f, bodyY),
                headOffset = Offset(0f, bodyY),
                leftArmOffset = Offset(0f, bodyY),
                rightArmOffset = Offset(0f, bodyY),
                leftArmRotation = phase * -180f,
                rightArmRotation = phase * 180f,
                leftLegRotation = phase * 35f,
                rightLegRotation = phase * -35f
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

/**
 * Paper-doll hero avatar. What you wear is what you see: with nothing
 * equipped the hero stands in undershorts, and every piece of gear draws
 * its own garment or silhouette (robes, vests, cuirasses, helms, hats,
 * hoods, cuffs, knee plates, boots). Legendary pieces glow.
 *
 * [showClassOutfit] renders the idealized class costume instead - used by
 * the class picker where there is no equipment yet.
 *
 * Drawn in a 100 x 120 unit space and scaled to fit.
 */
enum class AvatarFocus { FULL_BODY, LOWER_BODY, PORTRAIT }

enum class AvatarDetail { FULL, COMPACT }

enum class AvatarExpression { CALM, FOCUSED, TIRED, VICTORIOUS, LOW_HEALTH, BATTLE_READY }

@Composable
fun CharacterAvatar(
    clazz: CharacterClass,
    modifier: Modifier = Modifier,
    gear: Map<ItemSlot, ItemEntity> = emptyMap(),
    appearance: AvatarAppearance = DefaultAppearance,
    showClassOutfit: Boolean = false,
    animation: HeroAnimation = HeroAnimation.IDLE,
    expression: AvatarExpression = AvatarExpression.CALM,
    detail: AvatarDetail = AvatarDetail.FULL,
    highlightMuscles: Set<String> = emptySet(),
    facingBack: Boolean = false,
    enableBreathing: Boolean = true,
    focus: AvatarFocus = AvatarFocus.FULL_BODY,
    hiddenRegions: Set<BodyRegion> = emptySet()
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heroIdle")
    val internalBreatheScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    val breatheScale = if (enableBreathing) internalBreatheScale else 1f
    
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "animPhase"
    )
    val tilt by rememberDeviceTilt()

    Canvas(
        modifier
            .aspectRatio(100f / 120f)
            .graphicsLayer {
                scaleX = breatheScale
                scaleY = breatheScale
                transformOrigin = TransformOrigin(0.5f, 0.9f)
            }
            .clipToBounds()
    ) {
        val frame = AvatarFrame(
            size.width / 100f,
            clazz,
            lookFor(clazz),
            gear,
            showClassOutfit,
            highlightMuscles,
            facingBack,
            appearance,
            expression,
            detail,
            animPhase,
            hiddenRegions
        )
        with(AvatarPainter) {
            draw(frame, focus, tilt, animation)
        }
    }
}


// ---------------------------------------------------------------------------
// Layer renderer
// ---------------------------------------------------------------------------

/** Everything a layer needs to draw itself. */
internal class AvatarFrame(
    val u: Float,
    val cls: CharacterClass,
    val look: ClassLook,
    val gear: Map<ItemSlot, ItemEntity>,
    val costume: Boolean,
    val highlightMuscles: Set<String>,
    val facingBack: Boolean,
    val appearance: AvatarAppearance,
    val expression: AvatarExpression,
    val detail: AvatarDetail,
    val phase: Float,
    val hiddenRegions: Set<BodyRegion> = emptySet()
) {
    val weapon get() = gear[ItemSlot.WEAPON]
    val head get() = gear[ItemSlot.HEAD]
    val chest get() = gear[ItemSlot.CHEST]
    val hands get() = gear[ItemSlot.HANDS]
    val legs get() = gear[ItemSlot.LEGS]
    val feet get() = gear[ItemSlot.FEET]
    val trinket get() = gear[ItemSlot.TRINKET]
    val hasLegendary get() = gear.values.any { it.slot != ItemSlot.CONSUMABLE && it.tier >= 4 }

    /** Robe-style chest pieces are full-length and replace the lower body. */
    val robeChest: ItemEntity? get() = chest?.takeIf { it.style == ItemStyle.ROBE }

    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun s(w: Float, h: Float) = Size(w * u, h * u)

    /** Expanded so DB names (e.g. quadriceps, abdominals) light the matching segments. */
    private val expandedHighlights: Set<String> by lazy { expandMuscleHighlights(highlightMuscles) }

    fun hasHighlight(vararg muscles: String): Boolean =
        muscles.any { it.lowercase() in expandedHighlights }

    fun skinColor(vararg muscles: String): Color {
        return if (hasHighlight(*muscles)) Color(0xFFE57373) else appearance.skinColor
    }

    fun skinShade(vararg muscles: String): Color {
        val sc = appearance.skinColor
        val baseShade = sc.copy(
            red = (sc.red * 0.88f).coerceIn(0f, 1f),
            green = (sc.green * 0.84f).coerceIn(0f, 1f),
            blue = (sc.blue * 0.78f).coerceIn(0f, 1f)
        )
        return if (hasHighlight(*muscles)) Color(0xFFD32F2F) else baseShade
    }
}

internal object AvatarPainter {
    fun DrawScope.draw(frame: AvatarFrame, focus: AvatarFocus, tilt: Offset = Offset.Zero, animation: HeroAnimation = HeroAnimation.IDLE) {
        val pose = calculatePose(animation, frame.phase, frame.u)

        clipRect {
            withTransform({
                when (focus) {
                    AvatarFocus.LOWER_BODY -> {
                        scale(scaleX = 1.6f, scaleY = 1.6f, pivot = Offset(size.width / 2f, size.height))
                        translate(left = 0f, top = -15f * frame.u)
                    }
                    AvatarFocus.PORTRAIT -> {
                        scale(scaleX = 1.85f, scaleY = 1.85f, pivot = Offset(size.width / 2f, 40f * frame.u))
                        translate(left = 0f, top = 25f * frame.u)
                    }
                    AvatarFocus.FULL_BODY -> Unit
                }
            }) {
                val bgShift = Offset(tilt.x * 3f * frame.u, tilt.y * 3f * frame.u)
                val bodyShift = Offset(tilt.x * 12f * frame.u, tilt.y * 12f * frame.u)

                translate(bgShift.x, bgShift.y) {
                    drawAuraLayer(frame)
                    drawShadowLayer(frame)
                    drawClassSilhouetteLayer(frame)
                }

                translate(bodyShift.x, bodyShift.y) {
                    if (frame.cls == CharacterClass.SUMMONER) {
                        when (frame.appearance.druidForm) {
                            "SHIVA" -> drawShivaCompanion(frame)
                            "BAHAMUT" -> drawBahamutCompanion(frame)
                            "IFRIT" -> drawIfritCompanion(frame)
                            else -> {
                                if ((frame.weapon?.tier ?: 1) >= 4) drawBahamutCompanion(frame)
                                else if ((frame.weapon?.tier ?: 1) >= 2) drawShivaCompanion(frame)
                                else drawIfritCompanion(frame)
                            }
                        }
                    }
                    if (frame.cls == CharacterClass.NECROMANCER) {
                        when (frame.appearance.druidForm) {
                            "ARMY" -> drawUndeadArmyCompanion(frame)
                            "SKELETON" -> drawSkeletonCompanion(frame)
                            else -> {
                                if ((frame.weapon?.tier ?: 1) >= 3) drawUndeadArmyCompanion(frame)
                                else drawSkeletonCompanion(frame)
                            }
                        }
                    }
                    if (frame.cls == CharacterClass.DRAGOON && frame.appearance.druidForm != "NONE") {
                        drawWyvernCompanion(frame)
                    }
                    if (frame.cls == CharacterClass.RANGER) {
                        when (frame.appearance.druidForm) {
                            "FALCON" -> drawHunterFalconCompanion(frame)
                            "BEAR" -> drawHunterBearCompanion(frame)
                            else -> drawHunterWolfCompanion(frame)
                        }
                    }

                    when (frame.appearance.druidForm) {
                        "BEAR" -> drawDireBearLayer(frame)
                        "PANTHER" -> drawDirePantherLayer(frame)
                        "TREANT" -> drawAncientTreantLayer(frame)
                        "MOONKIN" -> drawCelestialMoonkinLayer(frame)
                        "AVATAR" -> drawPrimalAvatarLayer(frame)
                        else -> {
                            val build = frame.appearance.race.build()
                            // Scale from the feet so dwarves stay grounded and elves grow upward.
                            withTransform({
                                scale(
                                    scaleX = build.width,
                                    scaleY = build.height,
                                    pivot = Offset(size.width / 2f, 108f * frame.u)
                                )
                            }) {
                                drawLowerBodyLayer(frame, pose)

                                withTransform({
                                    translate(pose.bodyOffset.x, pose.bodyOffset.y)
                                }) {
                                    drawNeckLayer(frame)
                                    drawTorsoLayer(frame)
                                    drawRaceBodyAccents(frame)
                                    drawChestGarmentLayer(frame)
                                    drawTrinketLayer(frame)
                                }

                                drawArmsLayer(frame, pose)
                                drawRaceArmAccents(frame, pose)

                                withTransform({
                                    translate(pose.headOffset.x, pose.headOffset.y)
                                }) {
                                    drawHeadLayer(frame)
                                }

                                if (pose.prop == null) {
                                    drawWeaponLayer(frame, pose)
                                } else {
                                    drawPropLayer(frame, pose)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Maps free-exercise-db / custom muscle strings onto the avatar segment tags
 * (same idea as arms already accepting both "delts" and "shoulders").
 */
private fun expandMuscleHighlights(raw: Set<String>): Set<String> {
    val out = mutableSetOf<String>()
    for (rawM in raw) {
        val m = rawM.lowercase().trim().replace("_", " ")
        if (m.isEmpty() || m == "face" || m == "head" || m == "neck") continue // Don't highlight head/face/neck
        out += m
        when (m) {
            "abdominals", "abs", "core" -> out += setOf("abs", "abdominals", "core")
            "chest", "pecs", "pectorals" -> out += setOf("chest", "pecs")
            "obliques", "oblique" -> out += setOf("obliques")
            "quadriceps", "quads" -> out += setOf("quads", "quadriceps")
            "delts", "deltoids", "shoulders" -> out += setOf("shoulders", "delts")
            "middle back", "mid back" -> out += setOf("middle back", "lats")
            "lower back", "erectors" -> out += setOf("lower back")
            "lats", "latissimus" -> out += setOf("lats", "middle back")
            "traps", "trapezius" -> out += setOf("traps")
            "forearms", "forearm" -> out += setOf("forearms")
            "back" -> out += setOf("back", "lats", "traps", "lower back", "middle back")
            "adductors", "abductors" -> out += setOf("legs", "quads")
            "arms" -> out += setOf("arms", "biceps", "triceps", "forearms", "shoulders")
            "legs" -> out += setOf("legs", "quads", "hamstrings", "glutes", "calves")
            else -> Unit
        }
    }
    return out
}


private fun DrawScope.drawAuraLayer(f: AvatarFrame) {
    // Any legendary piece bathes the hero in a soft aura of its glow color.
    val glow = f.gear.values.firstNotNullOfOrNull { item ->
        if (item.slot != ItemSlot.CONSUMABLE && item.tier >= 4) GearVisuals.palette(item).glow else null
    } ?: return
    val pulse = 0.14f + kotlin.math.sin((f.phase * kotlin.math.PI * 2).toFloat()) * 0.06f
    drawCircle(
        Brush.radialGradient(
            listOf(glow.copy(alpha = pulse.coerceIn(0.08f, 0.22f)), Color.Transparent),
            center = f.p(50f, 60f),
            radius = 56f * f.u
        ),
        radius = 56f * f.u,
        center = f.p(50f, 60f)
    )
    if (f.detail == AvatarDetail.FULL) {
        val sparkColor = glow.copy(alpha = 0.75f)
        for ((i, pair) in listOf(24f to 44f, 78f to 52f, 32f to 82f, 70f to 88f).withIndex()) {
            val bob = kotlin.math.sin(((f.phase + i * 0.18f) * kotlin.math.PI * 2).toFloat()) * 2.0f
            drawCircle(sparkColor, radius = 1.4f * f.u, center = f.p(pair.first, pair.second + bob))
        }
    }
}

private fun DrawScope.drawShadowLayer(f: AvatarFrame) {
    drawOval(Color.Black.copy(alpha = 0.25f), topLeft = f.p(28f, 106f), size = f.s(44f, 10f))
}

private fun DrawScope.drawClassSilhouetteLayer(f: AvatarFrame) {
    if (f.detail == AvatarDetail.COMPACT) return
    when (f.cls) {
        CharacterClass.WARRIOR -> {
            drawRoundRect(
                Color(0xFFB0473C).copy(alpha = 0.18f),
                topLeft = f.p(24f, 43f),
                size = f.s(52f, 14f),
                cornerRadius = CornerRadius(7f * f.u)
            )
        }
        CharacterClass.MAGE -> {
            val glow = Color(0xFF9C7BE3).copy(alpha = 0.22f)
            drawCircle(glow, radius = 8f * f.u, center = f.p(76f, 22f))
            drawCircle(Color(0xFF9C7BE3).copy(alpha = 0.5f), radius = 2.4f * f.u, center = f.p(76f, 22f))
        }
        CharacterClass.THIEF -> {
            drawOval(Color.Black.copy(alpha = 0.20f), topLeft = f.p(18f, 88f), size = f.s(64f, 18f))
        }
        CharacterClass.RANGER -> {
            drawPath(
                Path().apply {
                    moveTo(22f * f.u, 24f * f.u)
                    quadraticTo(7f * f.u, 56f * f.u, 22f * f.u, 92f * f.u)
                },
                Color(0xFF6BC96B).copy(alpha = 0.22f),
                style = Stroke(width = 3f * f.u)
            )
        }
        CharacterClass.PALADIN -> {
            drawOval(
                Color(0xFFF5E6A8).copy(alpha = 0.30f),
                topLeft = f.p(32f, 9f),
                size = f.s(36f, 10f),
                style = Stroke(width = 2.2f * f.u)
            )
        }
        CharacterClass.WHITE_MAGE -> {
            drawCircle(Color.White.copy(alpha = 0.12f), radius = 40f * f.u, center = f.p(50f, 60f))
            drawPath(
                Path().apply {
                    moveTo(50f * f.u, 10f * f.u)
                    lineTo(54f * f.u, 22f * f.u)
                    lineTo(66f * f.u, 22f * f.u)
                    lineTo(56f * f.u, 30f * f.u)
                    lineTo(60f * f.u, 42f * f.u)
                    lineTo(50f * f.u, 34f * f.u)
                    lineTo(40f * f.u, 42f * f.u)
                    lineTo(44f * f.u, 30f * f.u)
                    lineTo(34f * f.u, 22f * f.u)
                    lineTo(46f * f.u, 22f * f.u)
                    close()
                },
                Color.White.copy(alpha = 0.15f)
            )
        }
        CharacterClass.MONK -> {
            drawCircle(Color(0xFFFF9800).copy(alpha = 0.14f), radius = 24f * f.u, center = f.p(50f, 60f), style = Stroke(width = 4f * f.u))
            drawCircle(Color(0xFFFF9800).copy(alpha = 0.14f), radius = 32f * f.u, center = f.p(50f, 60f), style = Stroke(width = 1.2f * f.u))
        }
        CharacterClass.DRUID -> {
            val leaf = Path().apply {
                moveTo(50f * f.u, 20f * f.u)
                quadraticTo(70f * f.u, 40f * f.u, 50f * f.u, 80f * f.u)
                quadraticTo(30f * f.u, 40f * f.u, 50f * f.u, 20f * f.u)
            }
            drawPath(leaf, Color(0xFF4CAF50).copy(alpha = 0.15f))
        }
        CharacterClass.BERSERKER -> {
            drawRect(Color(0xFFE91E63).copy(alpha = 0.12f), topLeft = f.p(20f, 40f), size = f.s(60f, 40f))
            drawLine(Color(0xFFE91E63).copy(alpha = 0.25f), start = f.p(20f, 40f), end = f.p(80f, 80f), strokeWidth = 2f * f.u)
            drawLine(Color(0xFFE91E63).copy(alpha = 0.25f), start = f.p(80f, 40f), end = f.p(20f, 80f), strokeWidth = 2f * f.u)
        }
        CharacterClass.BARD -> {
            val music = Color(0xFF2196F3).copy(alpha = 0.18f)
            drawCircle(music, radius = 5f * f.u, center = f.p(25f, 30f))
            drawCircle(music, radius = 4f * f.u, center = f.p(75f, 45f))
            drawCircle(music, radius = 6f * f.u, center = f.p(30f, 85f))
        }
        CharacterClass.SUMMONER -> {
            val summon = Color(0xFFFFEB3B).copy(alpha = 0.15f)
            drawCircle(summon, radius = 45f * f.u, center = f.p(50f, 60f), style = Stroke(width = 1f * f.u))
            drawPath(
                Path().apply {
                    moveTo(50f * f.u, 15f * f.u)
                    lineTo(85f * f.u, 75f * f.u)
                    lineTo(15f * f.u, 75f * f.u)
                    close()
                },
                summon,
                style = Stroke(width = 2f * f.u)
            )
        }
        CharacterClass.DRAGOON -> {
            val dragon = Color(0xFF673AB7).copy(alpha = 0.16f)
            drawPath(
                Path().apply {
                    moveTo(20f * f.u, 20f * f.u)
                    lineTo(50f * f.u, 10f * f.u)
                    lineTo(80f * f.u, 20f * f.u)
                    lineTo(50f * f.u, 100f * f.u)
                    close()
                },
                dragon
            )
        }
        CharacterClass.NECROMANCER -> {
            drawCircle(Color(0xFF7CF0C0).copy(alpha = 0.16f), radius = 38f * f.u, center = f.p(50f, 58f))
            drawLine(Color(0xFF7CF0C0).copy(alpha = 0.28f), start = f.p(28f, 88f), end = f.p(72f, 88f), strokeWidth = 1.4f * f.u)
        }
        else -> {}
    }
}

private fun DrawScope.drawArmsLayer(f: AvatarFrame, pose: AvatarPose) {
    val isBare = !f.costume && f.robeChest == null

    fun DrawScope.drawBareArmSegments(baseX: Float, isLeft: Boolean) {
        val upperArmHidden = if (isLeft) BodyRegion.LEFT_UPPER_ARM in f.hiddenRegions else BodyRegion.RIGHT_UPPER_ARM in f.hiddenRegions
        val forearmHidden = if (isLeft) BodyRegion.LEFT_FOREARM in f.hiddenRegions else BodyRegion.RIGHT_FOREARM in f.hiddenRegions

        // Forearm: bottom half
        if (!forearmHidden) {
            val forearmColor = f.skinColor("forearms")
            drawRoundRect(forearmColor, topLeft = f.p(baseX, 60f), size = f.s(11f, 16f), cornerRadius = CornerRadius(4f * f.u))
        }
        
        // Upper Arm (Triceps, Biceps, Deltoid)
        if (!upperArmHidden) {
            val tricepColor = f.skinColor("triceps")
            val tricepX = if (isLeft) baseX else baseX + 5.5f
            drawRect(tricepColor, topLeft = f.p(tricepX, 52f), size = f.s(5.5f, 12f))

            val bicepColor = f.skinColor("biceps")
            val bicepX = if (isLeft) baseX + 5.5f else baseX
            drawRect(bicepColor, topLeft = f.p(bicepX, 52f), size = f.s(5.5f, 12f))

            val shoulderColor = f.skinColor("shoulders", "delts")
            drawRoundRect(shoulderColor, topLeft = f.p(baseX - 0.5f, 45.5f), size = f.s(12f, 9f), cornerRadius = CornerRadius(4.5f * f.u))
        }
    }

    withTransform({
        translate(pose.leftArmOffset.x, pose.leftArmOffset.y)
        rotate(pose.leftArmRotation, pivot = f.p(28f, 46f))
    }) {
        if (!isBare) {
            val sleeve = if (f.costume) f.look.outfitDark else GearVisuals.palette(f.robeChest!!).dark
            drawRoundRect(sleeve, topLeft = f.p(23f, 46f), size = f.s(11f, 30f), cornerRadius = CornerRadius(5f * f.u))
        } else {
            drawBareArmSegments(23f, isLeft = true)
        }
        
        f.hands?.let { item ->
            val pal = GearVisuals.palette(item)
            drawRoundRect(pal.main, topLeft = f.p(22.5f, 64f), size = f.s(12f, 12f), cornerRadius = CornerRadius(3f * f.u))
            drawRect(pal.dark, topLeft = f.p(22.5f, 64f), size = f.s(12f, 3f))
            pal.glow?.let { g ->
                drawRect(g.copy(alpha = 0.85f), topLeft = f.p(22.5f, 67f), size = f.s(12f, 1.4f))
            }
        }
    }

    withTransform({
        translate(pose.rightArmOffset.x, pose.rightArmOffset.y)
        rotate(pose.rightArmRotation, pivot = f.p(71f, 46f))
    }) {
        if (!isBare) {
            val sleeve = if (f.costume) f.look.outfitDark else GearVisuals.palette(f.robeChest!!).dark
            drawRoundRect(sleeve, topLeft = f.p(66f, 46f), size = f.s(11f, 30f), cornerRadius = CornerRadius(5f * f.u))
        } else {
            drawBareArmSegments(66f, isLeft = false)
        }
        
        f.hands?.let { item ->
            val pal = GearVisuals.palette(item)
            drawRoundRect(pal.main, topLeft = f.p(65.5f, 64f), size = f.s(12f, 12f), cornerRadius = CornerRadius(3f * f.u))
            drawRect(pal.dark, topLeft = f.p(65.5f, 64f), size = f.s(12f, 3f))
            pal.glow?.let { g ->
                drawRect(g.copy(alpha = 0.85f), topLeft = f.p(65.5f, 67f), size = f.s(12f, 1.4f))
            }
        }
    }
}

private fun DrawScope.drawLowerBodyLayer(f: AvatarFrame, pose: AvatarPose) {
    if (f.costume) {
        withTransform({
            translate(pose.bodyOffset.x, pose.bodyOffset.y)
        }) {
            drawCostumeLowerBody(f)
        }
        return
    }
    // A full-length robe replaces the lower body entirely.
    if (f.robeChest != null) return

    val legPal = f.legs?.let { GearVisuals.palette(it) }
    val feetPal = f.feet?.let { GearVisuals.palette(it) }
    
    fun DrawScope.drawBareLegSegments(baseX: Float, isLeft: Boolean) {
        val thighHidden = if (isLeft) BodyRegion.LEFT_THIGH in f.hiddenRegions else BodyRegion.RIGHT_THIGH in f.hiddenRegions
        val calfHidden = if (isLeft) BodyRegion.LEFT_CALF in f.hiddenRegions else BodyRegion.RIGHT_CALF in f.hiddenRegions
        val footHidden = if (isLeft) BodyRegion.LEFT_FOOT in f.hiddenRegions else BodyRegion.RIGHT_FOOT in f.hiddenRegions

        // Glutes / Thighs
        if (!thighHidden) {
            val glutesColor = f.skinColor("glutes")
            drawRoundRect(glutesColor, topLeft = f.p(baseX - 0.5f, 73f), size = f.s(11f, 8f), cornerRadius = CornerRadius(3f * f.u))

            val quadsColor = f.skinColor("quads")
            val hamstringsColor = f.skinColor("hamstrings")
            val innerX = if (isLeft) baseX + 5f else baseX
            val outerX = if (isLeft) baseX else baseX + 5f
            drawRect(hamstringsColor, topLeft = f.p(outerX, 74f), size = f.s(5f, 15f))
            drawRect(quadsColor, topLeft = f.p(innerX, 74f), size = f.s(5f, 15f))
        }
        
        // Calves (lower leg)
        if (!calfHidden) {
            val calvesColor = f.skinColor("calves")
            drawRoundRect(calvesColor, topLeft = f.p(baseX, 88f), size = f.s(10f, 15f), cornerRadius = CornerRadius(3f * f.u))
        }

        // Feet (deterministic mask authority: draw if LEFT_FOOT/RIGHT_FOOT not hidden)
        if (!footHidden) {
            drawOval(f.skinColor("feet", "legs"), topLeft = f.p(if (isLeft) 36.5f else 51.5f, 101f), size = f.s(12f, 7f))
        }
    }

    withTransform({
        translate(pose.leftLegOffset.x, pose.leftLegOffset.y)
        rotate(pose.leftLegRotation, pivot = f.p(43f, 74f))
    }) {
        if (legPal != null) {
            drawRoundRect(legPal.main, topLeft = f.p(38f, 74f), size = f.s(10f, 30f), cornerRadius = CornerRadius(3f * f.u))
            drawRoundRect(legPal.dark, topLeft = f.p(37.5f, 84f), size = f.s(11f, 6f), cornerRadius = CornerRadius(2f * f.u))
            legPal.glow?.let { g ->
                drawRect(g.copy(alpha = 0.85f), topLeft = f.p(37.5f, 86f), size = f.s(11f, 1.2f))
            }
        } else {
            drawBareLegSegments(38f, isLeft = true)
        }
        
        if (feetPal != null) {
            drawRoundRect(feetPal.main, topLeft = f.p(36f, 98f), size = f.s(13f, 10f), cornerRadius = CornerRadius(3f * f.u))
            drawRect(feetPal.dark, topLeft = f.p(36f, 98f), size = f.s(13f, 3f))
            feetPal.glow?.let { g ->
                drawRect(g.copy(alpha = 0.85f), topLeft = f.p(36f, 100.5f), size = f.s(13f, 1.2f))
            }
        }
    }

    withTransform({
        translate(pose.rightLegOffset.x, pose.rightLegOffset.y)
        rotate(pose.rightLegRotation, pivot = f.p(57f, 74f))
    }) {
        if (legPal != null) {
            drawRoundRect(legPal.main, topLeft = f.p(52f, 74f), size = f.s(10f, 30f), cornerRadius = CornerRadius(3f * f.u))
            drawRoundRect(legPal.dark, topLeft = f.p(51.5f, 84f), size = f.s(11f, 6f), cornerRadius = CornerRadius(2f * f.u))
            legPal.glow?.let { g ->
                drawRect(g.copy(alpha = 0.85f), topLeft = f.p(51.5f, 86f), size = f.s(11f, 1.2f))
            }
        } else {
            drawBareLegSegments(52f, isLeft = false)
        }

        if (feetPal != null) {
            drawRoundRect(feetPal.main, topLeft = f.p(50f, 98f), size = f.s(13f, 10f), cornerRadius = CornerRadius(3f * f.u))
            drawRect(feetPal.dark, topLeft = f.p(50f, 98f), size = f.s(13f, 3f))
            feetPal.glow?.let { g ->
                drawRect(g.copy(alpha = 0.85f), topLeft = f.p(50f, 100.5f), size = f.s(13f, 1.2f))
            }
        }
    }
    
    // Modesty layer moves with the body, not the individual legs.
    if (BodyRegion.HIPS !in f.hiddenRegions) {
        withTransform({
            translate(pose.bodyOffset.x, pose.bodyOffset.y)
        }) {
            val modestyColor = legPal?.main ?: f.appearance.underwearColor
            drawRoundRect(modestyColor, topLeft = f.p(36f, 71f), size = f.s(28f, 13f), cornerRadius = CornerRadius(4f * f.u))
            if (legPal != null) {
                drawRect(legPal.dark, topLeft = f.p(36f, 71f), size = f.s(28f, 3f))
            }

            // Glute highlight overlay (draw over underwear if sore, back view only)
            if (f.facingBack && f.hasHighlight("glutes", "legs")) {
                drawRoundRect(
                    Color(0xFFE57373).copy(alpha = 0.8f),
                    topLeft = f.p(36f, 71f),
                    size = f.s(28f, 13f),
                    cornerRadius = CornerRadius(4f * f.u)
                )
            }
        }
    }
}

private fun DrawScope.drawCostumeLowerBody(f: AvatarFrame) {
    if (f.cls == CharacterClass.MAGE) {
        val robe = Path().apply {
            moveTo(34f * f.u, 46f * f.u)
            lineTo(66f * f.u, 46f * f.u)
            lineTo(74f * f.u, 108f * f.u)
            lineTo(26f * f.u, 108f * f.u)
            close()
        }
        drawPath(robe, f.look.outfit)
        drawRect(f.look.accent, topLeft = f.p(26f, 104f), size = f.s(48f, 4f))
        return
    }
    drawRoundRect(Pants, topLeft = f.p(38f, 74f), size = f.s(10f, 30f), cornerRadius = CornerRadius(3f * f.u))
    drawRoundRect(Pants, topLeft = f.p(52f, 74f), size = f.s(10f, 30f), cornerRadius = CornerRadius(3f * f.u))
    drawRoundRect(Boots, topLeft = f.p(36f, 98f), size = f.s(13f, 10f), cornerRadius = CornerRadius(3f * f.u))
    drawRoundRect(Boots, topLeft = f.p(51f, 98f), size = f.s(13f, 10f), cornerRadius = CornerRadius(3f * f.u))
}

private fun DrawScope.drawNeckLayer(f: AvatarFrame) {
    if (BodyRegion.NECK in f.hiddenRegions) return
    val neckColor = f.skinColor("neck")
    val neckShade = f.skinShade("neck").copy(alpha = 0.65f)
    // Strong neck cylinder connecting jawline (Y=38) down into clavicles (Y=48.5)
    drawRoundRect(
        neckColor,
        topLeft = f.p(45.5f, 38f),
        size = f.s(9f, 11.5f),
        cornerRadius = CornerRadius(2.5f * f.u)
    )
    // Anatomical shadow directly under the jawline
    drawRect(
        neckShade,
        topLeft = f.p(45.5f, 42.5f),
        size = f.s(9f, 3.5f)
    )
}

private fun DrawScope.drawTorsoLayer(f: AvatarFrame) {
    if (f.costume) {
        if (f.cls == CharacterClass.MAGE) {
            drawRoundRect(f.look.outfit, topLeft = f.p(34f, 48f), size = f.s(32f, 26f), cornerRadius = CornerRadius(6f * f.u))
            drawRect(f.look.accent, topLeft = f.p(47f, 48f), size = f.s(6f, 26f))
        } else {
            drawRoundRect(f.look.outfit, topLeft = f.p(34f, 48f), size = f.s(32f, 32f), cornerRadius = CornerRadius(6f * f.u))
            drawRect(f.look.outfitDark, topLeft = f.p(34f, 70f), size = f.s(32f, 5f))
            drawRoundRect(f.look.accent, topLeft = f.p(46f, 69.5f), size = f.s(8f, 6f), cornerRadius = CornerRadius(1.5f * f.u))
        }
        return
    }

    // Bare torso split into zones (only rendered if TORSO region is not hidden)
    if (BodyRegion.TORSO !in f.hiddenRegions) {
        drawBareTorsoSegments(f)
    }

    if (f.chest == null) {
        if (f.appearance.gender == "female") {
            val braColor = f.appearance.braColor
            if (!f.facingBack) {
                drawRoundRect(braColor, topLeft = f.p(34f, 50f), size = f.s(32f, 12f), cornerRadius = CornerRadius(2f * f.u))
                drawRect(braColor, topLeft = f.p(36f, 48f), size = f.s(6f, 4f))
                drawRect(braColor, topLeft = f.p(58f, 48f), size = f.s(6f, 4f))

                val absShade = f.skinShade("abs", "abdominals", "core").copy(alpha = 0.8f)
                drawLine(absShade, start = f.p(50f, 64f), end = f.p(50f, 74f), strokeWidth = 1.2f * f.u)
                drawLine(absShade, start = f.p(44f, 66f), end = f.p(56f, 66f), strokeWidth = 1f * f.u)
                drawLine(absShade, start = f.p(44f, 71f), end = f.p(56f, 71f), strokeWidth = 1f * f.u)
            } else {
                drawRect(braColor, topLeft = f.p(34f, 52f), size = f.s(32f, 8f))
                drawRect(braColor, topLeft = f.p(40f, 48f), size = f.s(6f, 4f))
                drawRect(braColor, topLeft = f.p(54f, 48f), size = f.s(6f, 4f))

                val spine = f.skinShade("lower back", "lats", "traps", "back").copy(alpha = 0.8f)
                drawLine(spine, start = f.p(50f, 60f), end = f.p(50f, 74f), strokeWidth = 1.2f * f.u)
            }
        } else if (!f.facingBack) {
            val pecShade = f.skinShade("chest", "pecs").copy(alpha = 0.85f)
            val absShade = f.skinShade("abs", "abdominals", "core").copy(alpha = 0.85f)
            // Pec separation
            drawLine(pecShade, start = f.p(42f, 54f), end = f.p(58f, 54f), strokeWidth = 1.2f * f.u)
            drawLine(pecShade, start = f.p(50f, 48f), end = f.p(50f, 56f), strokeWidth = 1f * f.u)
            // Abs grid
            drawLine(absShade, start = f.p(50f, 58f), end = f.p(50f, 74f), strokeWidth = 1.2f * f.u)
            drawLine(absShade, start = f.p(44f, 62f), end = f.p(56f, 62f), strokeWidth = 1f * f.u)
            drawLine(absShade, start = f.p(44f, 68f), end = f.p(56f, 68f), strokeWidth = 1f * f.u)
        } else {
            val trapShade = f.skinShade("traps", "back").copy(alpha = 0.85f)
            val latShade = f.skinShade("lats", "middle back", "back").copy(alpha = 0.85f)
            val lowShade = f.skinShade("lower back", "back").copy(alpha = 0.85f)
            drawLine(trapShade, start = f.p(50f, 48f), end = f.p(50f, 54f), strokeWidth = 1.2f * f.u)
            drawLine(latShade, start = f.p(50f, 54f), end = f.p(50f, 66f), strokeWidth = 1.2f * f.u)
            drawLine(lowShade, start = f.p(50f, 66f), end = f.p(50f, 74f), strokeWidth = 1.2f * f.u)
            drawLine(latShade, start = f.p(40f, 52f), end = f.p(46f, 64f), strokeWidth = 1f * f.u)
            drawLine(latShade, start = f.p(60f, 52f), end = f.p(54f, 64f), strokeWidth = 1f * f.u)
        }
    }
}

/** Front: pecs / abs / obliques. Back: traps / lats / lower back. */
private fun DrawScope.drawBareTorsoSegments(f: AvatarFrame) {
    if (f.facingBack) {
        // Lower back (bottom band)
        drawRoundRect(
            f.skinColor("lower back"),
            topLeft = f.p(34f, 64f),
            size = f.s(32f, 7f),
            cornerRadius = CornerRadius(4f * f.u)
        )
        // Lats / middle back (side wedges + center)
        val latColor = f.skinColor("lats", "middle back")
        drawRect(latColor, topLeft = f.p(34f, 50f), size = f.s(10f, 14f))
        drawRect(latColor, topLeft = f.p(56f, 50f), size = f.s(10f, 14f))
        drawRect(latColor, topLeft = f.p(44f, 52f), size = f.s(12f, 12f))
        // Traps (upper center)
        drawRoundRect(
            f.skinColor("traps"),
            topLeft = f.p(38f, 47.5f),
            size = f.s(24f, 7.5f),
            cornerRadius = CornerRadius(4f * f.u)
        )
        // Shoulder-blade corners so the silhouette stays full-width at top
        drawRect(f.skinColor("traps", "lats"), topLeft = f.p(34f, 48f), size = f.s(6f, 6f))
        drawRect(f.skinColor("traps", "lats"), topLeft = f.p(60f, 48f), size = f.s(6f, 6f))
    } else {
        // Full torso base (skin) so zones never leave holes; highlights paint on top.
        drawRoundRect(
            f.appearance.skinColor,
            topLeft = f.p(34f, 47.5f),
            size = f.s(32f, 23.5f),
            cornerRadius = CornerRadius(5f * f.u)
        )
        // Abs (lower band)
        if (f.hasHighlight("abs", "abdominals", "core")) {
            drawRoundRect(
                Color(0xFFE57373),
                topLeft = f.p(34f, 58f),
                size = f.s(32f, 18f),
                cornerRadius = CornerRadius(4f * f.u)
            )
        }
        // Obliques (side strips)
        if (f.hasHighlight("obliques")) {
            val pink = Color(0xFFE57373)
            drawRect(pink, topLeft = f.p(34f, 58f), size = f.s(5f, 18f))
            drawRect(pink, topLeft = f.p(61f, 58f), size = f.s(5f, 18f))
        }
        // Pecs (upper band only)
        if (f.hasHighlight("chest", "pecs")) {
            drawRoundRect(
                Color(0xFFE57373),
                topLeft = f.p(34f, 48f),
                size = f.s(32f, 10f),
                cornerRadius = CornerRadius(5f * f.u)
            )
        }
    }
}

private fun DrawScope.drawChestGarmentLayer(f: AvatarFrame) {
    val armor = f.chest ?: return
    val pal = GearVisuals.palette(armor)
    when (armor.style) {
        ItemStyle.ROBE -> {
            // Full-length robe: covers torso and legs, low scooped neckline exposing neck.
            val robe = Path().apply {
                moveTo(34f * f.u, 48.5f * f.u)
                lineTo(43f * f.u, 48f * f.u)
                quadraticTo(50f * f.u, 53.5f * f.u, 57f * f.u, 48f * f.u)
                lineTo(66f * f.u, 48.5f * f.u)
                lineTo(74f * f.u, 108f * f.u)
                lineTo(26f * f.u, 108f * f.u)
                close()
            }
            drawPath(robe, pal.main)
            drawRect(pal.dark, topLeft = f.p(47f, 53.5f), size = f.s(6f, 22f))
            f.feet?.let { item ->
                val fp = GearVisuals.palette(item)
                drawOval(fp.main, topLeft = f.p(36f, 103f), size = f.s(11f, 6f))
                drawOval(fp.main, topLeft = f.p(53f, 103f), size = f.s(11f, 6f))
            }
            // Leg gear tints the hem band.
            val hem = f.legs?.let { GearVisuals.palette(it).main } ?: pal.dark
            drawRect(hem, topLeft = f.p(26f, 104f), size = f.s(48f, 4f))
            if (armor.tier >= 3) {
                val rune = pal.glow ?: Color(0xFF9C7BE3)
                for (y in listOf(56f, 63f, 70f)) {
                    drawCircle(rune, radius = 1.4f * f.u, center = f.p(41f, y))
                    drawCircle(rune, radius = 1.4f * f.u, center = f.p(59f, y))
                }
            }
            pal.glow?.let { g ->
                drawPath(robe, g.copy(alpha = 0.8f), style = Stroke(width = 1.3f * f.u))
            }
        }
        ItemStyle.LIGHT -> {
            // Fitted leather vest with belt, straps, and deep scooped neckline.
            drawRoundRect(pal.main, topLeft = f.p(34f, 48f), size = f.s(32f, 28f), cornerRadius = CornerRadius(6f * f.u))
            drawRect(pal.dark, topLeft = f.p(34f, 70f), size = f.s(32f, 5f))
            drawRect(pal.dark, topLeft = f.p(40f, 48f), size = f.s(4f, 4f))
            drawRect(pal.dark, topLeft = f.p(56f, 48f), size = f.s(4f, 4f))
            if (armor.tier >= 2) {
                drawRect(pal.dark, topLeft = f.p(49.2f, 53f), size = f.s(1.6f, 13f))
                for (y in listOf(55f, 59f, 63f, 67f)) {
                    drawLine(pal.dark, start = f.p(46f, y), end = f.p(54f, y), strokeWidth = 1f * f.u)
                }
            }
            if (armor.tier >= 3) {
                drawCircle(pal.dark, radius = 1.8f * f.u, center = f.p(39f, 52f))
                drawCircle(pal.dark, radius = 1.8f * f.u, center = f.p(61f, 52f))
            }
            pal.glow?.let { g ->
                drawRoundRect(
                    g.copy(alpha = 0.8f),
                    topLeft = f.p(34f, 48f),
                    size = f.s(32f, 28f),
                    cornerRadius = CornerRadius(6f * f.u),
                    style = Stroke(width = 1.3f * f.u)
                )
            }
        }
        else -> {
            // Full plate cuirass with low collar.
            drawRoundRect(pal.main, topLeft = f.p(34f, 47.5f), size = f.s(32f, 25f), cornerRadius = CornerRadius(6f * f.u))
            drawRect(pal.dark, topLeft = f.p(34f, 57f), size = f.s(32f, 3f))
            drawRect(pal.dark, topLeft = f.p(34f, 68f), size = f.s(32f, 4f))
            drawCircle(pal.dark, radius = 6.5f * f.u, center = f.p(35f, 51f))
            drawCircle(pal.main, radius = 5f * f.u, center = f.p(35f, 51f))
            drawCircle(pal.dark, radius = 6.5f * f.u, center = f.p(65f, 51f))
            drawCircle(pal.main, radius = 5f * f.u, center = f.p(65f, 51f))
            if (armor.tier >= 2) {
                for ((x, y) in listOf(38f to 52f, 62f to 52f, 38f to 65f, 62f to 65f)) {
                    drawCircle(pal.dark, radius = 1.2f * f.u, center = f.p(x, y))
                }
            }
            if (armor.tier >= 3) {
                val emblem = Path().apply {
                    moveTo(50f * f.u, 60f * f.u)
                    lineTo(53f * f.u, 64f * f.u)
                    lineTo(50f * f.u, 68f * f.u)
                    lineTo(47f * f.u, 64f * f.u)
                    close()
                }
                drawPath(emblem, pal.glow ?: pal.dark)
            }
            pal.glow?.let { g ->
                drawRoundRect(
                    g.copy(alpha = 0.8f),
                    topLeft = f.p(34f, 47.5f),
                    size = f.s(32f, 25f),
                    cornerRadius = CornerRadius(6f * f.u),
                    style = Stroke(width = 1.3f * f.u)
                )
            }
        }
    }
}

internal fun DrawScope.drawTrinketLayer(f: AvatarFrame) {
    val item = f.trinket ?: return
    val tc = GearVisuals.trinketColor(item.id)
    drawCircle(tc.copy(alpha = 0.35f), radius = 6.5f * f.u, center = f.p(50f, 51f))
    drawCircle(tc, radius = 4f * f.u, center = f.p(50f, 51f))
    drawCircle(Color.White.copy(alpha = 0.7f), radius = 1.5f * f.u, center = f.p(48.8f, 49.8f))
}

// ---- Head + headgear ----

private fun DrawScope.drawHair(f: AvatarFrame, style: String, isHat: Boolean = false) {
    if (style.lowercase() == "bald") return
    val h = f.appearance.hairColor
    when (style.lowercase()) {
        "short" -> {
            drawArc(h, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = f.p(36f, 16.5f), size = f.s(28f, 27f))
            drawRect(h, topLeft = f.p(36f, 27f), size = f.s(4f, 6f))
            drawRect(h, topLeft = f.p(60f, 27f), size = f.s(4f, 6f))
            if (f.facingBack && !isHat) drawRect(h, topLeft = f.p(38f, 27f), size = f.s(24f, 8f))
        }
        "long" -> {
            drawArc(h, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = f.p(35f, 15f), size = f.s(30f, 30f))
            drawRect(h, topLeft = f.p(35f, 30f), size = f.s(6f, 16f))
            drawRect(h, topLeft = f.p(59f, 30f), size = f.s(6f, 16f))
            if (f.facingBack && !isHat) drawRect(h, topLeft = f.p(35f, 30f), size = f.s(30f, 18f))
        }
        "ponytail" -> {
            drawArc(h, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = f.p(36f, 16.5f), size = f.s(28f, 27f))
            if (!f.facingBack) {
                drawRoundRect(h, topLeft = f.p(62f, 24f), size = f.s(8f, 14f), cornerRadius = CornerRadius(4f * f.u))
            } else if (!isHat) {
                drawRoundRect(h, topLeft = f.p(46f, 26f), size = f.s(8f, 16f), cornerRadius = CornerRadius(4f * f.u))
            }
        }
        "afro" -> {
            if (isHat) {
                drawArc(h, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = f.p(36f, 16.5f), size = f.s(28f, 27f))
                return
            }
            drawCircle(h, radius = 18f * f.u, center = f.p(50f, 24f))
            drawCircle(h, radius = 12f * f.u, center = f.p(36f, 28f))
            drawCircle(h, radius = 12f * f.u, center = f.p(64f, 28f))
            drawCircle(h, radius = 14f * f.u, center = f.p(42f, 16f))
            drawCircle(h, radius = 14f * f.u, center = f.p(58f, 16f))
        }
        "spiky" -> {
            drawArc(h, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = f.p(36f, 16.5f), size = f.s(28f, 27f))
            if (!isHat) {
                val spikes = Path().apply {
                    moveTo(36f * f.u, 26f * f.u)
                    lineTo(32f * f.u, 14f * f.u)
                    lineTo(42f * f.u, 20f * f.u)
                    lineTo(44f * f.u, 10f * f.u)
                    lineTo(50f * f.u, 18f * f.u)
                    lineTo(56f * f.u, 10f * f.u)
                    lineTo(58f * f.u, 20f * f.u)
                    lineTo(68f * f.u, 14f * f.u)
                    lineTo(64f * f.u, 26f * f.u)
                    close()
                }
                drawPath(spikes, h)
            }
        }
    }
}

private fun DrawScope.drawHeadLayer(f: AvatarFrame) {
    if (f.costume) {
        drawClassCostumeHead(f)
        return
    }
    val item = f.head
    if (item == null) {
        drawRaceEars(f) // behind the skull so elf tips read clearly
        drawCircle(f.skinColor("head", "face"), radius = 14f * f.u, center = f.p(50f, 30f))
        val hideAllHair = BodyRegion.SCALP in f.hiddenRegions && BodyRegion.FOREHEAD in f.hiddenRegions
        if (!hideAllHair) {
            val isHat = BodyRegion.FOREHEAD in f.hiddenRegions || BodyRegion.SCALP in f.hiddenRegions
            drawHair(f, f.appearance.hairStyle, isHat = isHat)
        }
        if (!f.facingBack) {
            eyes(f, 44f, 32f, 56f)
            drawRaceFaceAccents(f)
        }
        return
    }
    val pal = GearVisuals.palette(item)
    when (GearVisuals.headgearShape(item)) {
        HeadgearShape.HELM -> {
            drawRaceEars(f)
            drawCircle(f.skinColor("head", "face"), radius = 14f * f.u, center = f.p(50f, 30f))
            drawArc(pal.main, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = f.p(34.5f, 14.5f), size = f.s(31f, 31f))
            drawRect(pal.dark, topLeft = f.p(34.5f, 26f), size = f.s(31f, 3.5f))
            drawRoundRect(pal.main, topLeft = f.p(35f, 28f), size = f.s(7f, 12f), cornerRadius = CornerRadius(2f * f.u))
            drawRoundRect(pal.main, topLeft = f.p(58f, 28f), size = f.s(7f, 12f), cornerRadius = CornerRadius(2f * f.u))
            drawRoundRect(VisorSlit, topLeft = f.p(42f, 30f), size = f.s(16f, 4f), cornerRadius = CornerRadius(2f * f.u))
            if (item.tier >= 3) {
                drawRoundRect(pal.glow ?: f.look.outfit, topLeft = f.p(46f, 5f), size = f.s(8f, 11f), cornerRadius = CornerRadius(3f * f.u))
            }
            // Beard / tusks can still peek under a closed helm.
            drawRaceFaceAccents(f, helmCovered = true)
        }
        HeadgearShape.HAT -> {
            drawRaceEars(f)
            drawCircle(f.skinColor("head", "face"), radius = 14f * f.u, center = f.p(50f, 30f))
            drawHair(f, f.appearance.hairStyle, isHat = true)
            eyes(f, 44f, 30f, 56f)
            drawRaceFaceAccents(f)
            val cone = Path().apply {
                moveTo(33f * f.u, 20f * f.u)
                lineTo(67f * f.u, 20f * f.u)
                lineTo(54f * f.u, 0f * f.u)
                close()
            }
            drawPath(cone, pal.main)
            drawOval(pal.main, topLeft = f.p(29f, 16f), size = f.s(42f, 9f))
            drawRect(pal.glow ?: pal.dark, topLeft = f.p(40f, 17.5f), size = f.s(20f, 3f))
            pal.glow?.let { g ->
                drawLine(g, start = f.p(56f, 2f), end = f.p(60f, 6f), strokeWidth = 1.2f * f.u)
                drawLine(g, start = f.p(60f, 2f), end = f.p(56f, 6f), strokeWidth = 1.2f * f.u)
            }
        }
        HeadgearShape.HOOD -> {
            drawCircle(pal.main, radius = 15.5f * f.u, center = f.p(50f, 29f))
            drawArc(pal.dark, startAngle = 200f, sweepAngle = 140f, useCenter = true, topLeft = f.p(34.5f, 13.5f), size = f.s(31f, 31f))
            drawOval(f.skinColor("head", "face"), topLeft = f.p(40f, 24f), size = f.s(20f, 17f))
            eyes(f, 45f, 31f, 55f)
            drawRaceFaceAccents(f)
            val point = Path().apply {
                moveTo(42f * f.u, 16f * f.u)
                lineTo(58f * f.u, 16f * f.u)
                lineTo(50f * f.u, 6f * f.u)
                close()
            }
            drawPath(point, pal.main)
            pal.glow?.let { g ->
                drawCircle(g.copy(alpha = 0.6f), radius = 15.8f * f.u, center = f.p(50f, 29f), style = Stroke(width = 1.2f * f.u))
            }
            // Elf ears poke out of the hood rim.
            drawRaceEars(f, hooded = true)
        }
        HeadgearShape.CAP -> {
            drawRaceEars(f)
            drawCircle(f.skinColor("head", "face"), radius = 14f * f.u, center = f.p(50f, 30f))
            drawHair(f, f.appearance.hairStyle, isHat = true)
            eyes(f, 44f, 31f, 56f)
            drawRaceFaceAccents(f)
            drawOval(pal.main, topLeft = f.p(38f, 15f), size = f.s(24f, 9f))
            drawRect(pal.dark, topLeft = f.p(35f, 21f), size = f.s(30f, 3.5f))
        }
    }
}

private fun DrawScope.drawClassCostumeHead(f: AvatarFrame) {
    drawCircle(f.skinColor("head", "face"), radius = 14f * f.u, center = f.p(50f, 30f))
    when (f.cls) {
        CharacterClass.WARRIOR -> {
            drawArc(f.look.headgear, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = f.p(34.5f, 14.5f), size = f.s(31f, 31f))
            drawRect(f.look.headgear, topLeft = f.p(47.5f, 26f), size = f.s(5f, 12f))
            drawRoundRect(f.look.outfit, topLeft = f.p(46f, 8f), size = f.s(8f, 8f), cornerRadius = CornerRadius(3f * f.u))
            eyes(f, 43f, 32f, 57f)
        }
        CharacterClass.MAGE -> {
            drawHair(f, f.appearance.hairStyle, isHat = true)
            eyes(f, 44f, 30f, 56f)
            val cone = Path().apply {
                moveTo(33f * f.u, 20f * f.u)
                lineTo(67f * f.u, 20f * f.u)
                lineTo(54f * f.u, 0f * f.u)
                close()
            }
            drawPath(cone, f.look.headgear)
            drawOval(f.look.headgear, topLeft = f.p(29f, 16f), size = f.s(42f, 9f))
            drawRect(f.look.accent, topLeft = f.p(40f, 17.5f), size = f.s(20f, 3f))
        }
        CharacterClass.THIEF -> {
            drawCircle(f.look.headgear, radius = 15.5f * f.u, center = f.p(50f, 29f))
            drawOval(f.skinColor("head", "face"), topLeft = f.p(40f, 24f), size = f.s(20f, 17f))
            eyes(f, 45f, 31f, 55f)
            val point = Path().apply {
                moveTo(42f * f.u, 16f * f.u)
                lineTo(58f * f.u, 16f * f.u)
                lineTo(50f * f.u, 6f * f.u)
                close()
            }
            drawPath(point, f.look.headgear)
        }
        CharacterClass.RANGER -> {
            drawHair(f, f.appearance.hairStyle, isHat = true)
            eyes(f, 44f, 30f, 56f)
            val cap = Path().apply {
                moveTo(35f * f.u, 22f * f.u)
                lineTo(65f * f.u, 22f * f.u)
                lineTo(58f * f.u, 8f * f.u)
                close()
            }
            drawPath(cap, f.look.headgear)
            drawRect(f.look.headgear, topLeft = f.p(35f, 20f), size = f.s(30f, 4f))
            val feather = Path().apply {
                moveTo(60f * f.u, 12f * f.u)
                lineTo(70f * f.u, 2f * f.u)
                lineTo(66f * f.u, 14f * f.u)
                close()
            }
            drawPath(feather, Color(0xFFE35B5B))
        }
        CharacterClass.PALADIN -> {
            drawArc(f.look.headgear, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = f.p(34.5f, 14.5f), size = f.s(31f, 31f))
            drawRect(f.look.accent, topLeft = f.p(47f, 8f), size = f.s(6f, 10f))
            eyes(f, 43f, 32f, 57f)
        }
        CharacterClass.NECROMANCER -> {
            drawHair(f, f.appearance.hairStyle, isHat = true)
            eyes(f, 44f, 30f, 56f)
            val hood = Path().apply {
                moveTo(32f * f.u, 22f * f.u)
                lineTo(68f * f.u, 22f * f.u)
                lineTo(58f * f.u, 4f * f.u)
                lineTo(42f * f.u, 4f * f.u)
                close()
            }
            drawPath(hood, f.look.headgear)
            drawOval(f.look.accent.copy(alpha = 0.45f), topLeft = f.p(42f, 26f), size = f.s(16f, 10f))
        }
        else -> {
            drawHair(f, f.appearance.hairStyle, isHat = false)
            eyes(f, 44f, 30f, 56f)
        }
    }
    drawRaceEars(f)
    drawRaceFaceAccents(f)
}

private fun DrawScope.drawRaceEars(f: AvatarFrame, hooded: Boolean = false) {
    if (f.appearance.race != CharacterRace.ELF) return
    val skin = f.appearance.skinColor
    val shade = f.skinShade("head", "neck", "face")
    // Large pointed ears — the main elf tell.
    val tipLift = if (hooded) 8f else 10f
    val left = Path().apply {
        moveTo(36f * f.u, 30f * f.u)
        lineTo(22f * f.u, (24f - tipLift) * f.u)
        lineTo(37f * f.u, 26f * f.u)
        close()
    }
    val right = Path().apply {
        moveTo(64f * f.u, 30f * f.u)
        lineTo(78f * f.u, (24f - tipLift) * f.u)
        lineTo(63f * f.u, 26f * f.u)
        close()
    }
    drawPath(left, skin)
    drawPath(right, skin)
    drawPath(left, shade, style = Stroke(width = 0.9f * f.u))
    drawPath(right, shade, style = Stroke(width = 0.9f * f.u))
}

private fun DrawScope.drawRaceFaceAccents(f: AvatarFrame, helmCovered: Boolean = false) {
    val skin = f.appearance.skinColor
    val shade = f.skinShade("head", "neck", "face")
    val hair = f.appearance.hairColor
    when (f.appearance.race) {
        CharacterRace.ELF -> {
            if (helmCovered) return
            // Slimmer chin / high cheek suggestion.
            drawOval(shade.copy(alpha = 0.35f), topLeft = f.p(46f, 38f), size = f.s(8f, 5f))
        }
        CharacterRace.ORC -> {
            if (!helmCovered) {
                // Heavy brow ridge.
                drawOval(shade.copy(alpha = 0.75f), topLeft = f.p(38f, 24f), size = f.s(24f, 7f))
                // Broad lower jaw.
                drawOval(skin, topLeft = f.p(38f, 36f), size = f.s(24f, 12f))
                drawOval(shade.copy(alpha = 0.4f), topLeft = f.p(40f, 40f), size = f.s(20f, 6f))
            }
            // Upward tusks — readable even under a helm chin-gap.
            val tusk = Color(0xFFF2E8D5)
            val leftTusk = Path().apply {
                moveTo(44f * f.u, 40f * f.u)
                lineTo(41f * f.u, 48f * f.u)
                lineTo(46f * f.u, 41f * f.u)
                close()
            }
            val rightTusk = Path().apply {
                moveTo(56f * f.u, 40f * f.u)
                lineTo(59f * f.u, 48f * f.u)
                lineTo(54f * f.u, 41f * f.u)
                close()
            }
            drawPath(leftTusk, tusk)
            drawPath(rightTusk, tusk)
            drawPath(leftTusk, shade, style = Stroke(width = 0.7f * f.u))
            drawPath(rightTusk, shade, style = Stroke(width = 0.7f * f.u))
        }
        CharacterRace.UNDEAD -> {
            if (helmCovered) return
            val rot = Color(0xFF4E6A48)
            val deepRot = Color(0xFF2C3A2A)
            val bone = Color(0xFFD9D0BC)
            // Hollow sockets behind the glow eyes.
            drawCircle(Color.Black.copy(alpha = 0.45f), radius = 5.2f * f.u, center = f.p(44f, 32f))
            drawCircle(Color.Black.copy(alpha = 0.45f), radius = 5.2f * f.u, center = f.p(56f, 32f))
            // Rotting flesh patches + exposed bone.
            drawOval(rot.copy(alpha = 0.75f), topLeft = f.p(36f, 34f), size = f.s(9f, 7f))
            drawOval(deepRot.copy(alpha = 0.7f), topLeft = f.p(55f, 26f), size = f.s(8f, 6f))
            drawOval(bone.copy(alpha = 0.85f), topLeft = f.p(48f, 38f), size = f.s(6f, 4.5f))
            // Crack lines across the cheek / brow.
            drawLine(deepRot, start = f.p(40f, 28f), end = f.p(46f, 36f), strokeWidth = 1.1f * f.u)
            drawLine(deepRot, start = f.p(58f, 29f), end = f.p(62f, 37f), strokeWidth = 1.0f * f.u)
            // Sunken jaw / torn lip.
            drawOval(deepRot.copy(alpha = 0.55f), topLeft = f.p(44f, 40f), size = f.s(12f, 5f))
        }
        CharacterRace.DWARF -> {
            val isFemale = f.appearance.gender == "female"
            if (isFemale) {
                // Shorter braided sideburns + chin tuft — still stout, not a full beard.
                drawOval(hair, topLeft = f.p(34f, 32f), size = f.s(6f, 14f))
                drawOval(hair, topLeft = f.p(60f, 32f), size = f.s(6f, 14f))
                if (!helmCovered) {
                    drawOval(hair, topLeft = f.p(44f, 40f), size = f.s(12f, 8f))
                    drawOval(shade.copy(alpha = 0.7f), topLeft = f.p(46f, 32f), size = f.s(8f, 5f))
                }
            } else {
                // Thick full beard — the dwarf silhouette cue.
                val beard = Path().apply {
                    moveTo(36f * f.u, 36f * f.u)
                    lineTo(64f * f.u, 36f * f.u)
                    lineTo(62f * f.u, 52f * f.u)
                    lineTo(50f * f.u, 58f * f.u)
                    lineTo(38f * f.u, 52f * f.u)
                    close()
                }
                drawPath(beard, hair)
                drawPath(
                    beard,
                    hair.copy(
                        red = (hair.red * 0.75f).coerceIn(0f, 1f),
                        green = (hair.green * 0.75f).coerceIn(0f, 1f),
                        blue = (hair.blue * 0.75f).coerceIn(0f, 1f)
                    ),
                    style = Stroke(width = 1.2f * f.u)
                )
                if (!helmCovered) {
                    drawOval(hair, topLeft = f.p(40f, 36f), size = f.s(20f, 6f))
                    drawOval(shade.copy(alpha = 0.7f), topLeft = f.p(46f, 32f), size = f.s(8f, 5f))
                }
            }
        }
        CharacterRace.HUMAN -> Unit
    }
}

private fun DrawScope.drawRaceBodyAccents(f: AvatarFrame) {
    when (f.appearance.race) {
        CharacterRace.UNDEAD -> {
            if (f.costume || f.chest != null) return
            val rot = Color(0xFF4E6A48)
            val deepRot = Color(0xFF2C3A2A)
            val bone = Color(0xFFD9D0BC)
            drawOval(rot.copy(alpha = 0.6f), topLeft = f.p(37f, 48f), size = f.s(10f, 8f))
            drawOval(deepRot.copy(alpha = 0.55f), topLeft = f.p(52f, 56f), size = f.s(11f, 9f))
            drawOval(bone.copy(alpha = 0.75f), topLeft = f.p(45f, 62f), size = f.s(7f, 5f))
            drawLine(deepRot, start = f.p(40f, 52f), end = f.p(48f, 60f), strokeWidth = 1.1f * f.u)
        }
        CharacterRace.ORC -> {
            if (f.costume || f.chest != null) return
            // Subtle green undertone so orc skin reads even on warm custom colors.
            drawRoundRect(
                Color(0xFF3F8F4A).copy(alpha = 0.14f),
                topLeft = f.p(34f, 44f),
                size = f.s(32f, 32f),
                cornerRadius = CornerRadius(6f * f.u)
            )
        }
        CharacterRace.ELF -> {
            if (f.costume || f.chest != null) return
            // Narrower waist suggestion on bare torso.
            drawRect(
                f.skinShade("abs", "abdominals", "core").copy(alpha = 0.25f),
                topLeft = f.p(36f, 66f),
                size = f.s(28f, 4f)
            )
        }
        else -> Unit
    }
}

private fun DrawScope.drawRaceArmAccents(f: AvatarFrame, pose: AvatarPose) {
    if (f.appearance.race != CharacterRace.UNDEAD) return
    if (f.costume || f.robeChest != null) return
    val rot = Color(0xFF4E6A48)
    val bone = Color(0xFFD9D0BC)
    withTransform({
        translate(pose.leftArmOffset.x, pose.leftArmOffset.y)
        rotate(pose.leftArmRotation, pivot = f.p(28f, 46f))
    }) {
        drawOval(rot.copy(alpha = 0.55f), topLeft = f.p(24f, 52f), size = f.s(7f, 6f))
        drawOval(bone.copy(alpha = 0.65f), topLeft = f.p(25f, 62f), size = f.s(5f, 4f))
    }
    withTransform({
        translate(pose.rightArmOffset.x, pose.rightArmOffset.y)
        rotate(pose.rightArmRotation, pivot = f.p(71f, 46f))
    }) {
        drawOval(rot.copy(alpha = 0.55f), topLeft = f.p(68f, 56f), size = f.s(8f, 7f))
        drawOval(bone.copy(alpha = 0.65f), topLeft = f.p(69f, 66f), size = f.s(5f, 4f))
    }
}

private fun DrawScope.eyes(f: AvatarFrame, x1: Float, y1: Float, x2: Float) {
    val race = f.appearance.race
    val eyeColor = when (race) {
        CharacterRace.UNDEAD -> Color(0xFF7CF0C0)
        else -> when (f.expression) {
            AvatarExpression.LOW_HEALTH -> Color(0xFFE35B5B)
            else -> f.appearance.eyeColor
        }
    }
    val eyeY = when (f.expression) {
        AvatarExpression.TIRED -> y1 + 1.2f
        AvatarExpression.FOCUSED, AvatarExpression.BATTLE_READY -> y1 - 0.8f
        else -> y1
    }
    val radius = when {
        f.detail == AvatarDetail.COMPACT -> 2.5f
        race == CharacterRace.ELF -> 2.2f
        race == CharacterRace.ORC -> 1.6f
        race == CharacterRace.UNDEAD -> 2.4f
        else -> 1.9f
    }

    if (race == CharacterRace.UNDEAD) {
        drawCircle(Color(0xFF7CF0C0).copy(alpha = 0.35f), radius = 5.5f * f.u, center = Offset(x1 * f.u, eyeY * f.u))
        drawCircle(Color(0xFF7CF0C0).copy(alpha = 0.35f), radius = 5.5f * f.u, center = Offset(x2 * f.u, eyeY * f.u))
    }

    if (f.expression == AvatarExpression.TIRED) {
        drawLine(eyeColor, start = f.p(x1 - 2.4f, eyeY), end = f.p(x1 + 2.4f, eyeY + 0.4f), strokeWidth = 1.5f * f.u)
        drawLine(eyeColor, start = f.p(x2 - 2.4f, eyeY + 0.4f), end = f.p(x2 + 2.4f, eyeY), strokeWidth = 1.5f * f.u)
    } else {
        // Most expressions now use circles for clarity, with brows doing the heavy lifting.
        drawCircle(eyeColor, radius = radius * f.u, center = Offset(x1 * f.u, eyeY * f.u))
        drawCircle(eyeColor, radius = radius * f.u, center = Offset(x2 * f.u, eyeY * f.u))
    }
    
    if (race == CharacterRace.UNDEAD) {
        // Pupil-less glow cores.
        drawCircle(Color.White.copy(alpha = 0.85f), radius = 0.9f * f.u, center = Offset(x1 * f.u, eyeY * f.u))
        drawCircle(Color.White.copy(alpha = 0.85f), radius = 0.9f * f.u, center = Offset(x2 * f.u, eyeY * f.u))
    }

    // Eyebrows
    val browY = eyeY - when (race) {
        CharacterRace.ORC -> 2.2f
        CharacterRace.ELF -> 4.2f
        else -> 3.5f
    }
    val browColor = when (race) {
        CharacterRace.UNDEAD -> Color(0xFF3A4A38).copy(alpha = 0.7f)
        else -> f.appearance.hairColor.copy(alpha = 0.8f)
    }
    drawLine(
        color = browColor,
        start = Offset((x1 - 2.5f) * f.u, (browY - 1f) * f.u),
        end = Offset((x1 + 2.5f) * f.u, browY * f.u),
        strokeWidth = if (race == CharacterRace.ORC) 2.0f * f.u else 1.2f * f.u,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
    drawLine(
        color = browColor,
        start = Offset((x2 - 2.5f) * f.u, browY * f.u),
        end = Offset((x2 + 2.5f) * f.u, (browY - 1f) * f.u),
        strokeWidth = if (race == CharacterRace.ORC) 2.0f * f.u else 1.2f * f.u,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )

    // Nose
    val noseX = (x1 + x2) / 2f
    val noseY = eyeY + if (race == CharacterRace.DWARF) 2.5f else 3.5f
    if (race != CharacterRace.UNDEAD && f.detail == AvatarDetail.FULL) {
        drawLine(
            color = f.skinShade("head", "neck", "face"),
            start = Offset(noseX * f.u, (eyeY + 1f) * f.u),
            end = Offset(noseX * f.u, noseY * f.u),
            strokeWidth = if (race == CharacterRace.DWARF) 2.2f * f.u else 1.5f * f.u,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }

    // Mouth
    val mouthY = noseY + when (race) {
        CharacterRace.ORC -> 5f
        CharacterRace.DWARF -> 3f
        CharacterRace.UNDEAD -> 5f
        else -> 4f
    }
    when {
        f.expression == AvatarExpression.VICTORIOUS -> {
            val smile = Path().apply {
                moveTo((noseX - 5f) * f.u, (mouthY - 1f) * f.u)
                quadraticTo(
                    noseX * f.u, (mouthY + 5f) * f.u,
                    (noseX + 5f) * f.u, (mouthY - 1f) * f.u
                )
            }
            drawPath(
                smile,
                f.skinShade("head", "neck", "face"),
                style = Stroke(width = 1.8f * f.u, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        f.expression == AvatarExpression.TIRED || f.expression == AvatarExpression.LOW_HEALTH -> {
            drawLine(
                color = f.skinShade("head", "neck", "face"),
                start = Offset((noseX - 4f) * f.u, (mouthY + 2f) * f.u),
                end = Offset((noseX + 4f) * f.u, mouthY * f.u),
                strokeWidth = 1.4f * f.u,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
        f.expression == AvatarExpression.BATTLE_READY -> {
            drawLine(
                color = Color(0xFF2A1A18),
                start = Offset((noseX - 5f) * f.u, mouthY * f.u),
                end = Offset((noseX + 5f) * f.u, mouthY * f.u),
                strokeWidth = 1.8f * f.u,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
        race == CharacterRace.UNDEAD -> {
            // Torn / uneven mouth.
            drawLine(
                color = Color(0xFF1A2018),
                start = Offset((noseX - 4f) * f.u, mouthY * f.u),
                end = Offset((noseX + 3f) * f.u, (mouthY + 1.5f) * f.u),
                strokeWidth = 1.4f * f.u,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
        race == CharacterRace.ORC -> {
            drawLine(
                color = f.skinShade("head", "neck", "face"),
                start = Offset((noseX - 5f) * f.u, mouthY * f.u),
                end = Offset((noseX + 5f) * f.u, mouthY * f.u),
                strokeWidth = 1.6f * f.u,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
        else -> {
            val mouthPath = Path().apply {
                moveTo((noseX - 3.5f) * f.u, (mouthY - 0.5f) * f.u)
                quadraticTo(
                    noseX * f.u, (mouthY + 3.5f) * f.u,
                    (noseX + 3.5f) * f.u, (mouthY - 1.5f) * f.u
                )
            }
            drawPath(
                path = mouthPath,
                color = f.skinShade("head", "neck", "face"),
                style = Stroke(
                    width = 1.2f * f.u,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
    }
}

// ---- Weapon + hands ----

internal fun DrawScope.drawWeaponLayer(f: AvatarFrame, pose: AvatarPose) {
    val handsPal = f.hands?.let { GearVisuals.palette(it) }
    val leftHand = handsPal?.main ?: f.skinColor("forearms", "hands", "arms")
    val rightHand = when {
        handsPal != null && f.weapon != null -> handsPal.dark
        handsPal != null -> handsPal.main
        f.weapon != null -> f.skinShade("forearms", "hands", "arms")
        else -> f.skinColor("forearms", "hands", "arms")
    }

    withTransform({
        translate(pose.weaponOffset.x, pose.weaponOffset.y)
        translate(pose.rightArmOffset.x, pose.rightArmOffset.y)
        rotate(pose.rightArmRotation, pivot = f.p(71f, 46f))
    }) {
        f.weapon?.let { drawWeapon(f, it) }
            ?: run {
                if (f.costume && f.cls == CharacterClass.NECROMANCER) {
                    drawSkullStaff(f, handX = 71.5f, tier = 3)
                }
            }
        drawCircle(rightHand, radius = 5f * f.u, center = f.p(71.5f, 78f))
    }

    withTransform({
        translate(pose.weaponOffset.x, pose.weaponOffset.y)
        translate(pose.leftArmOffset.x, pose.leftArmOffset.y)
        rotate(pose.leftArmRotation, pivot = f.p(29f, 46f))
    }) {
        drawCircle(leftHand, radius = 5f * f.u, center = f.p(28.5f, 78f))
        
        // Off-hand dagger sits over the left hand
        val w = f.weapon
        if (w != null && w.style == ItemStyle.DAGGER && w.tier >= 2) {
            drawDagger(f, handX = 28.5f, tier = w.tier, length = 14f)
        }
    }
}

private fun bladeColor(tier: Int): Color = when (tier) {
    1 -> Color(0xFF9A8F7A) // dull, rusty
    2 -> Color(0xFF9AA3AD) // iron
    3 -> Color(0xFFC8D4E8) // polished steel
    else -> Color(0xFF9C7BE3) // legendary arcane
}

private fun orbColor(tier: Int): Color = when (tier) {
    1 -> Color(0xFFB9B4A6)
    2 -> Color(0xFF6BC96B)
    3 -> Color(0xFF7BB4E3)
    else -> Color(0xFFF0C040)
}

private val Wood = Color(0xFF6B5138)
private val WoodLight = Color(0xFF8B6B4A)

/** Necromancer bone staff topped with a readable skull (not a mage orb). */
private fun DrawScope.drawSkullStaff(f: AvatarFrame, handX: Float, tier: Int) {
    val bone = Color(0xFFE8E0D0)
    val boneShade = Color(0xFF9A9080)
    val socket = Color(0xFF0E1012)
    val glow = Color(0xFF7CF0C0)
    val rare = tier >= 3
    val legendary = tier >= 4

    // Tall thick shaft — reads as a staff, not a wand.
    drawRoundRect(
        boneShade,
        topLeft = f.p(handX - 2.2f, 24f),
        size = f.s(4.4f, 58f),
        cornerRadius = CornerRadius(2.2f * f.u)
    )
    drawRoundRect(
        bone,
        topLeft = f.p(handX - 1.2f, 24f),
        size = f.s(1.6f, 58f),
        cornerRadius = CornerRadius(0.8f * f.u)
    )
    drawRect(Color(0xFF2A1E38), topLeft = f.p(handX - 2.8f, 40f), size = f.s(5.6f, 2.4f))
    drawRect(Color(0xFF2A1E38), topLeft = f.p(handX - 2.8f, 62f), size = f.s(5.6f, 2.4f))

    val cy = 13f

    // Soft aura only for legendaries — keep it behind so it doesn't read as an orb.
    if (legendary) {
        drawCircle(glow.copy(alpha = 0.22f), radius = 11f * f.u, center = f.p(handX, cy))
    }

    // Cranium (wider than tall)
    drawOval(bone, topLeft = f.p(handX - 7.5f, cy - 8f), size = f.s(15f, 12f))
    // Cheek / temple shade so it isn't a flat ball
    drawOval(boneShade.copy(alpha = 0.55f), topLeft = f.p(handX - 7f, cy - 2f), size = f.s(4f, 5f))
    drawOval(boneShade.copy(alpha = 0.55f), topLeft = f.p(handX + 3f, cy - 2f), size = f.s(4f, 5f))

    // Jaw hanging below — the key skull tell vs a ball
    drawRoundRect(
        bone,
        topLeft = f.p(handX - 5.5f, cy + 2.5f),
        size = f.s(11f, 6.5f),
        cornerRadius = CornerRadius(1.5f * f.u)
    )
    // Teeth
    for (i in -2..2) {
        val tx = handX + i * 2.1f
        drawRect(Color.White.copy(alpha = 0.85f), topLeft = f.p(tx - 0.6f, cy + 3.2f), size = f.s(1.2f, 2.8f))
        drawRect(boneShade.copy(alpha = 0.5f), topLeft = f.p(tx - 0.6f, cy + 5.2f), size = f.s(1.2f, 0.8f))
    }

    // Eye sockets — large hollows, always visible
    drawOval(socket, topLeft = f.p(handX - 5.8f, cy - 4.5f), size = f.s(4.6f, 5.2f))
    drawOval(socket, topLeft = f.p(handX + 1.2f, cy - 4.5f), size = f.s(4.6f, 5.2f))
    if (rare) {
        val eyeR = if (legendary) 1.8f else 1.35f
        drawCircle(glow, radius = eyeR * f.u, center = f.p(handX - 3.5f, cy - 1.8f))
        drawCircle(glow, radius = eyeR * f.u, center = f.p(handX + 3.5f, cy - 1.8f))
        if (legendary) {
            drawCircle(Color.White, radius = 0.7f * f.u, center = f.p(handX - 3.5f, cy - 1.8f))
            drawCircle(Color.White, radius = 0.7f * f.u, center = f.p(handX + 3.5f, cy - 1.8f))
        }
    }

    // Nose cavity
    val nose = Path().apply {
        moveTo(handX * f.u, (cy - 0.2f) * f.u)
        lineTo((handX - 1.8f) * f.u, (cy + 2.8f) * f.u)
        lineTo((handX + 1.8f) * f.u, (cy + 2.8f) * f.u)
        close()
    }
    drawPath(nose, socket)

    // Outline so the skull silhouette pops at small sizes
    drawOval(
        boneShade,
        topLeft = f.p(handX - 7.5f, cy - 8f),
        size = f.s(15f, 12f),
        style = Stroke(width = 1.1f * f.u)
    )
}

private fun DrawScope.drawWeapon(f: AvatarFrame, weapon: ItemEntity) {
    val handX = 71.5f
    val legendary = weapon.tier >= 4
    val necroPolearm = (weapon.style == ItemStyle.STAFF || weapon.style == ItemStyle.WAND) &&
        (weapon.classAffinity == CharacterClass.NECROMANCER || f.cls == CharacterClass.NECROMANCER)

    if (necroPolearm) {
        drawSkullStaff(f, handX = handX, tier = weapon.tier)
        return
    }

    when (weapon.style) {
        ItemStyle.MACE -> {
            drawRoundRect(Wood, topLeft = f.p(handX - 1.5f, 46f), size = f.s(3f, 32f), cornerRadius = CornerRadius(1.5f * f.u))
            drawCircle(Color(0xFF9AA3AD), radius = 8f * f.u, center = f.p(handX, 42f))
            for ((dx, dy) in listOf(0f to -10f, -9f to -4f, 9f to -4f, -7f to 6f, 7f to 6f)) {
                drawCircle(Color(0xFF767E87), radius = 2.4f * f.u, center = f.p(handX + dx, 42f + dy))
            }
        }
        ItemStyle.GREATSWORD -> {
            val blade = Path().apply {
                moveTo((handX - 3.5f) * f.u, 70f * f.u)
                lineTo((handX - 3.5f) * f.u, 22f * f.u)
                lineTo(handX * f.u, 10f * f.u)
                lineTo((handX + 3.5f) * f.u, 22f * f.u)
                lineTo((handX + 3.5f) * f.u, 70f * f.u)
                close()
            }
            if (legendary) {
                drawPath(blade, Color(0xFFFFA040).copy(alpha = 0.5f), style = Stroke(width = 3f * f.u))
            }
            drawPath(blade, Color(0xFFE8763A))
            drawRect(Color(0xFFB3502A), topLeft = f.p(handX - 0.8f, 12f), size = f.s(1.6f, 56f))
            drawRoundRect(Color(0xFFF0C040), topLeft = f.p(handX - 8f, 70f), size = f.s(16f, 4f), cornerRadius = CornerRadius(2f * f.u))
        }
        ItemStyle.WAND -> {
            drawRoundRect(WoodLight, topLeft = f.p(handX - 1.2f, 56f), size = f.s(2.4f, 22f), cornerRadius = CornerRadius(1.2f * f.u))
            val spark = orbColor(weapon.tier)
            drawCircle(spark.copy(alpha = 0.35f), radius = 5f * f.u, center = f.p(handX, 53f))
            drawCircle(spark, radius = 2.6f * f.u, center = f.p(handX, 53f))
        }
        ItemStyle.STAFF -> {
            drawRoundRect(Wood, topLeft = f.p(handX - 1.4f, 18f), size = f.s(2.8f, 62f), cornerRadius = CornerRadius(1.4f * f.u))
            val orb = orbColor(weapon.tier)
            drawCircle(orb.copy(alpha = 0.3f), radius = (if (legendary) 10f else 8f) * f.u, center = f.p(handX, 15f))
            drawCircle(orb, radius = 5f * f.u, center = f.p(handX, 15f))
            drawCircle(Color.White.copy(alpha = 0.6f), radius = 1.6f * f.u, center = f.p(handX - 1.6f, 13.4f))
            drawRect(orb, topLeft = f.p(handX - 2.4f, 20.5f), size = f.s(4.8f, 2f))
        }
        ItemStyle.DAGGER -> {
            drawDagger(f, handX = handX, tier = weapon.tier, length = 18f)
        }
        ItemStyle.BOW -> {
            val wood = if (legendary) Color(0xFF4E7A3A) else WoodLight
            val bow = Path().apply {
                moveTo((handX + 1.5f) * f.u, 56f * f.u)
                quadraticTo((handX + 17f) * f.u, 78f * f.u, (handX + 1.5f) * f.u, 100f * f.u)
            }
            if (legendary) {
                drawPath(bow, Color(0xFF6BC96B).copy(alpha = 0.4f), style = Stroke(width = 5f * f.u))
            }
            drawPath(bow, wood, style = Stroke(width = 2.6f * f.u))
            drawLine(
                Color(0xFFE8E4D8),
                start = f.p(handX + 1.5f, 56f),
                end = f.p(handX + 1.5f, 100f),
                strokeWidth = 0.9f * f.u
            )
            drawCircle(bladeColor(weapon.tier), radius = 1.8f * f.u, center = f.p(handX + 1.5f, 56f))
            drawCircle(bladeColor(weapon.tier), radius = 1.8f * f.u, center = f.p(handX + 1.5f, 100f))
        }
        else -> { // SWORD and fallback
            val topY = when (weapon.tier) {
                1 -> 44f
                2, 3 -> 34f
                else -> 30f
            }
            val blade = Path().apply {
                moveTo((handX - 2.2f) * f.u, 72f * f.u)
                lineTo((handX - 2.2f) * f.u, (topY + 6f) * f.u)
                lineTo(handX * f.u, topY * f.u)
                lineTo((handX + 2.2f) * f.u, (topY + 6f) * f.u)
                lineTo((handX + 2.2f) * f.u, 72f * f.u)
                close()
            }
            if (legendary) {
                drawPath(blade, bladeColor(weapon.tier).copy(alpha = 0.5f), style = Stroke(width = 3f * f.u))
            }
            drawPath(blade, bladeColor(weapon.tier))
            drawRoundRect(Color(0xFFB07E24), topLeft = f.p(handX - 6.5f, 71f), size = f.s(13f, 3.5f), cornerRadius = CornerRadius(1.7f * f.u))
        }
    }
}

private fun DrawScope.drawDagger(f: AvatarFrame, handX: Float, tier: Int, length: Float) {
    val topY = 74f - length
    val blade = Path().apply {
        moveTo((handX - 1.6f) * f.u, 74f * f.u)
        lineTo((handX - 1.6f) * f.u, (topY + 4f) * f.u)
        lineTo(handX * f.u, topY * f.u)
        lineTo((handX + 1.6f) * f.u, (topY + 4f) * f.u)
        lineTo((handX + 1.6f) * f.u, 74f * f.u)
        close()
    }
    if (tier >= 4) {
        drawPath(blade, bladeColor(tier).copy(alpha = 0.5f), style = Stroke(width = 2.6f * f.u))
    }
    drawPath(blade, bladeColor(tier))
    drawRoundRect(Color(0xFF3E3A4A), topLeft = f.p(handX - 4f, 73f), size = f.s(8f, 2.6f), cornerRadius = CornerRadius(1.3f * f.u))
}

// ---- Fantasy Exercise Props ----

private fun DrawScope.drawPropLayer(f: AvatarFrame, pose: AvatarPose) {
    val prop = pose.prop!!
    val handsPal = f.hands?.let { GearVisuals.palette(it) }
    val leftHand = handsPal?.main ?: f.skinColor("forearms", "hands", "arms")
    val rightHand = handsPal?.main ?: f.skinColor("forearms", "hands", "arms")
    
    when (prop) {
        HeroAnimation.SQUAT -> {
            // A massive stone log across the shoulders, moves with the body
            val logColor = Color(0xFF6E7873)
            val mossColor = Color(0xFF4A6B46)
            
            withTransform({
                translate(pose.bodyOffset.x, pose.bodyOffset.y)
            }) {
                drawRoundRect(logColor, topLeft = f.p(10f, 40f), size = f.s(80f, 12f), cornerRadius = CornerRadius(6f * f.u))
                drawRoundRect(mossColor, topLeft = f.p(20f, 40f), size = f.s(15f, 4f), cornerRadius = CornerRadius(2f * f.u))
                drawRoundRect(mossColor, topLeft = f.p(70f, 40f), size = f.s(10f, 3f), cornerRadius = CornerRadius(2f * f.u))
            }
        }
        HeroAnimation.PRESS -> {
            // Iron blocks move with the arms!
            val iron = Color(0xFF6E7873)
            val ironLight = Color(0xFF8B9490)
            
            withTransform({
                translate(pose.leftArmOffset.x, pose.leftArmOffset.y)
            }) {
                drawRoundRect(iron, topLeft = f.p(18f, 25f), size = f.s(12f, 25f), cornerRadius = CornerRadius(2f * f.u))
                drawRect(ironLight, topLeft = f.p(20f, 25f), size = f.s(2f, 25f))
            }
            
            withTransform({
                translate(pose.rightArmOffset.x, pose.rightArmOffset.y)
            }) {
                drawRoundRect(iron, topLeft = f.p(70f, 25f), size = f.s(12f, 25f), cornerRadius = CornerRadius(2f * f.u))
                drawRect(ironLight, topLeft = f.p(72f, 25f), size = f.s(2f, 25f))
            }
        }
        HeroAnimation.PULLUP -> {
            // A thick, ancient oak branch above head. Does NOT move with body!
            val wood = Color(0xFF3E2723)
            val woodHighlight = Color(0xFF5D4037)
            drawRoundRect(wood, topLeft = f.p(15f, 15f), size = f.s(70f, 8f), cornerRadius = CornerRadius(4f * f.u))
            drawRect(woodHighlight, topLeft = f.p(20f, 16f), size = f.s(60f, 2f))
        }
        else -> {
            // no props
        }
    }

    // Now draw the hands with the proper arm transforms
    withTransform({
        translate(pose.rightArmOffset.x, pose.rightArmOffset.y)
        rotate(pose.rightArmRotation, pivot = f.p(71f, 46f))
    }) {
        drawCircle(rightHand, radius = 5f * f.u, center = f.p(71.5f, 78f))
    }
}

private fun DrawScope.drawDireBearLayer(frame: AvatarFrame) {
    val u = frame.u
    val furMain = Color(0xFF6E4225)
    val furDark = Color(0xFF4A2B15)
    val furChest = Color(0xFFD4A373)
    val eyeGlow = Color(0xFF43A047)
    val scarColor = Color(0xFF3E2723).copy(alpha = 0.6f)

    // Bear body
    drawCircle(color = furDark, center = Offset(60f * u, 70f * u), radius = 32f * u)
    drawCircle(color = furMain, center = Offset(60f * u, 68f * u), radius = 30f * u)
    // Chest patch
    drawCircle(color = furChest, center = Offset(60f * u, 72f * u), radius = 18f * u)
    
    // Battle Scars
    drawLine(scarColor, start = Offset(45f * u, 60f * u), end = Offset(52f * u, 68f * u), strokeWidth = 2f * u)
    drawLine(scarColor, start = Offset(48f * u, 58f * u), end = Offset(55f * u, 66f * u), strokeWidth = 2f * u)

    // Bear head
    drawCircle(color = furDark, center = Offset(60f * u, 38f * u), radius = 22f * u)
    drawCircle(color = furMain, center = Offset(60f * u, 36f * u), radius = 20f * u)
    // Ears
    drawCircle(color = furDark, center = Offset(42f * u, 22f * u), radius = 8f * u)
    drawCircle(color = furDark, center = Offset(78f * u, 22f * u), radius = 8f * u)
    // Snout
    drawCircle(color = furChest, center = Offset(60f * u, 42f * u), radius = 10f * u)
    drawCircle(color = Color.Black, center = Offset(60f * u, 38f * u), radius = 4f * u)
    
    // Glowing eyes with pulse
    val eyeAlpha = 0.7f + 0.3f * sin(frame.phase * 6.28f)
    drawCircle(color = eyeGlow.copy(alpha = 0.3f * eyeAlpha), center = Offset(52f * u, 32f * u), radius = 6f * u)
    drawCircle(color = eyeGlow.copy(alpha = 0.3f * eyeAlpha), center = Offset(68f * u, 32f * u), radius = 6f * u)
    drawCircle(color = eyeGlow, center = Offset(52f * u, 32f * u), radius = 3.5f * u)
    drawCircle(color = eyeGlow, center = Offset(68f * u, 32f * u), radius = 3.5f * u)
    
    // Paws
    drawRoundRect(color = furDark, topLeft = Offset(26f * u, 75f * u), size = Size(20f * u, 28f * u), cornerRadius = CornerRadius(10f * u))
    drawRoundRect(color = furDark, topLeft = Offset(74f * u, 75f * u), size = Size(20f * u, 28f * u), cornerRadius = CornerRadius(10f * u))
}

private fun DrawScope.drawDirePantherLayer(frame: AvatarFrame) {
    val u = frame.u
    // Mid-tone midnight blue so it pops against dark background
    val furMain = Color(0xFF424566) 
    val furDark = Color(0xFF2A2C39)
    val eyeGlow = Color(0xFFFFD54F)
    val auraColor = Color(0xFF6A5ACD).copy(alpha = 0.25f)

    // Soft stealth aura
    drawCircle(auraColor, center = Offset(60f * u, 55f * u), radius = 45f * u)

    // Panther body
    drawRoundRect(color = furDark, topLeft = Offset(42f * u, 48f * u), size = Size(36f * u, 45f * u), cornerRadius = CornerRadius(18f * u))
    drawRoundRect(color = furMain, topLeft = Offset(44f * u, 50f * u), size = Size(32f * u, 41f * u), cornerRadius = CornerRadius(16f * u))
    
    // Sleek spots (subtle)
    drawCircle(furDark.copy(alpha = 0.4f), center = Offset(50f * u, 60f * u), radius = 3f * u)
    drawCircle(furDark.copy(alpha = 0.4f), center = Offset(70f * u, 75f * u), radius = 4f * u)
    drawCircle(furDark.copy(alpha = 0.4f), center = Offset(55f * u, 85f * u), radius = 2.5f * u)

    // Panther head
    drawCircle(color = furDark, center = Offset(60f * u, 34f * u), radius = 19f * u)
    drawCircle(color = furMain, center = Offset(60f * u, 32f * u), radius = 17f * u)
    // Pointy ears
    val leftEar = Path().apply {
        moveTo(46f * u, 24f * u)
        lineTo(40f * u, 10f * u)
        lineTo(54f * u, 18f * u)
        close()
    }
    val rightEar = Path().apply {
        moveTo(74f * u, 24f * u)
        lineTo(80f * u, 10f * u)
        lineTo(66f * u, 18f * u)
        close()
    }
    drawPath(leftEar, furDark)
    drawPath(rightEar, furDark)
    
    // Glowing golden eyes with slit
    drawCircle(color = eyeGlow.copy(alpha = 0.3f), center = Offset(52f * u, 30f * u), radius = 6f * u)
    drawCircle(color = eyeGlow.copy(alpha = 0.3f), center = Offset(68f * u, 30f * u), radius = 6f * u)
    drawCircle(color = eyeGlow, center = Offset(52f * u, 30f * u), radius = 3.5f * u)
    drawCircle(color = eyeGlow, center = Offset(68f * u, 30f * u), radius = 3.5f * u)
    // Pupils
    drawRect(Color.Black, topLeft = Offset(51.5f * u, 28f * u), size = Size(1f * u, 4f * u))
    drawRect(Color.Black, topLeft = Offset(67.5f * u, 28f * u), size = Size(1f * u, 4f * u))

    // Claws / Paws
    drawRoundRect(color = furDark, topLeft = Offset(32f * u, 72f * u), size = Size(16f * u, 30f * u), cornerRadius = CornerRadius(8f * u))
    drawRoundRect(color = furDark, topLeft = Offset(72f * u, 72f * u), size = Size(16f * u, 30f * u), cornerRadius = CornerRadius(8f * u))
}

private fun DrawScope.drawAncientTreantLayer(frame: AvatarFrame) {
    val u = frame.u
    val barkDark = Color(0xFF2E1C0C)
    val barkMain = Color(0xFF4A3525)
    val barkLight = Color(0xFF6D4C41)
    val leafDark = Color(0xFF1B5E20)
    val leafMain = Color(0xFF2E7D32)
    val leafBright = Color(0xFF4CAF50)
    val sapGlow = Color(0xFF00E676)
    val pulse = 0.7f + 0.3f * sin(frame.phase * 6.28f)

    // Soft nature aura
    drawCircle(Color(0xFF4CAF50).copy(alpha = 0.15f * pulse), center = Offset(60f * u, 55f * u), radius = 50f * u)

    // Root Feet spreading onto pedestal
    drawRoundRect(barkDark, topLeft = Offset(24f * u, 78f * u), size = Size(24f * u, 26f * u), cornerRadius = CornerRadius(8f * u))
    drawRoundRect(barkDark, topLeft = Offset(72f * u, 78f * u), size = Size(24f * u, 26f * u), cornerRadius = CornerRadius(8f * u))
    // Root toes
    drawLine(barkDark, start = Offset(26f * u, 100f * u), end = Offset(18f * u, 104f * u), strokeWidth = 5f * u)
    drawLine(barkDark, start = Offset(94f * u, 100f * u), end = Offset(102f * u, 104f * u), strokeWidth = 5f * u)

    // Ironbark Trunk Body
    drawRoundRect(barkDark, topLeft = Offset(38f * u, 42f * u), size = Size(44f * u, 45f * u), cornerRadius = CornerRadius(16f * u))
    drawRoundRect(barkMain, topLeft = Offset(40f * u, 44f * u), size = Size(40f * u, 41f * u), cornerRadius = CornerRadius(14f * u))

    // Wood Grain lines
    drawLine(barkLight.copy(alpha = 0.6f), start = Offset(46f * u, 50f * u), end = Offset(48f * u, 75f * u), strokeWidth = 2f * u)
    drawLine(barkLight.copy(alpha = 0.6f), start = Offset(74f * u, 52f * u), end = Offset(72f * u, 76f * u), strokeWidth = 2f * u)

    // Glowing Heart of the Forest (chest core)
    drawCircle(sapGlow.copy(alpha = 0.3f * pulse), center = Offset(60f * u, 62f * u), radius = 10f * u)
    drawCircle(sapGlow, center = Offset(60f * u, 62f * u), radius = 4f * u)

    // Ancient Head
    drawCircle(barkDark, center = Offset(60f * u, 32f * u), radius = 18f * u)
    drawCircle(barkMain, center = Offset(60f * u, 30f * u), radius = 16f * u)
    // Leaf beard
    drawCircle(leafDark, center = Offset(60f * u, 44f * u), radius = 10f * u)
    drawCircle(leafMain, center = Offset(60f * u, 43f * u), radius = 8f * u)

    // Branch Antlers / Horns
    val leftBranch = Path().apply {
        moveTo(48f * u, 24f * u)
        quadraticTo(30f * u, 8f * u, 22f * u, 12f * u)
        quadraticTo(34f * u, 18f * u, 44f * u, 26f * u)
        close()
    }
    val rightBranch = Path().apply {
        moveTo(72f * u, 24f * u)
        quadraticTo(90f * u, 8f * u, 98f * u, 12f * u)
        quadraticTo(86f * u, 18f * u, 76f * u, 26f * u)
        close()
    }
    drawPath(leftBranch, barkDark)
    drawPath(rightBranch, barkDark)

    // Foliage Clusters on Branches
    drawCircle(leafDark, center = Offset(24f * u, 10f * u), radius = 9f * u)
    drawCircle(leafBright, center = Offset(22f * u, 8f * u), radius = 6f * u)
    drawCircle(leafDark, center = Offset(96f * u, 10f * u), radius = 9f * u)
    drawCircle(leafBright, center = Offset(98f * u, 8f * u), radius = 6f * u)

    // Glowing Eyes
    drawCircle(sapGlow, center = Offset(53f * u, 28f * u), radius = 3.5f * u)
    drawCircle(sapGlow, center = Offset(67f * u, 28f * u), radius = 3.5f * u)

    // Branch Arms
    drawRoundRect(barkDark, topLeft = Offset(26f * u, 48f * u), size = Size(16f * u, 32f * u), cornerRadius = CornerRadius(8f * u))
    drawRoundRect(barkDark, topLeft = Offset(78f * u, 48f * u), size = Size(16f * u, 32f * u), cornerRadius = CornerRadius(8f * u))
    drawCircle(leafBright, center = Offset(28f * u, 72f * u), radius = 5f * u)
    drawCircle(leafBright, center = Offset(92f * u, 72f * u), radius = 5f * u)
}

private fun DrawScope.drawCelestialMoonkinLayer(frame: AvatarFrame) {
    val u = frame.u
    val plumDark = Color(0xFF1A237E)
    val plumMain = Color(0xFF283593)
    val plumChest = Color(0xFFC5CAE9)
    val beakColor = Color(0xFFFFB300)
    val antlerColor = Color(0xFFECEFF1)
    val starGlow = Color(0xFF80DEEA)
    val pulse = 0.7f + 0.3f * sin(frame.phase * 6.28f)

    // Arcane Star Halo
    drawCircle(starGlow.copy(alpha = 0.2f * pulse), center = Offset(60f * u, 48f * u), radius = 48f * u)

    // Moonkin Body
    drawRoundRect(plumDark, topLeft = Offset(36f * u, 45f * u), size = Size(48f * u, 46f * u), cornerRadius = CornerRadius(20f * u))
    drawRoundRect(plumMain, topLeft = Offset(38f * u, 47f * u), size = Size(44f * u, 42f * u), cornerRadius = CornerRadius(18f * u))
    // Soft feathery chest
    drawCircle(plumChest, center = Offset(60f * u, 65f * u), radius = 16f * u)

    // Wing Arms (folded majestically)
    val leftWing = Path().apply {
        moveTo(38f * u, 48f * u)
        quadraticTo(16f * u, 65f * u, 26f * u, 85f * u)
        quadraticTo(36f * u, 75f * u, 42f * u, 60f * u)
        close()
    }
    val rightWing = Path().apply {
        moveTo(82f * u, 48f * u)
        quadraticTo(104f * u, 65f * u, 94f * u, 85f * u)
        quadraticTo(84f * u, 75f * u, 78f * u, 60f * u)
        close()
    }
    drawPath(leftWing, plumDark)
    drawPath(rightWing, plumDark)

    // Feathered Feet / Talons
    drawRoundRect(beakColor, topLeft = Offset(38f * u, 86f * u), size = Size(16f * u, 16f * u), cornerRadius = CornerRadius(6f * u))
    drawRoundRect(beakColor, topLeft = Offset(66f * u, 86f * u), size = Size(16f * u, 16f * u), cornerRadius = CornerRadius(6f * u))

    // Moonkin Head
    drawCircle(plumDark, center = Offset(60f * u, 32f * u), radius = 18f * u)
    drawCircle(plumMain, center = Offset(60f * u, 30f * u), radius = 16f * u)

    // Curved Grand Antlers
    val leftAntler = Path().apply {
        moveTo(50f * u, 22f * u)
        quadraticTo(28f * u, 14f * u, 32f * u, 2f * u)
        quadraticTo(40f * u, 12f * u, 54f * u, 20f * u)
        close()
    }
    val rightAntler = Path().apply {
        moveTo(70f * u, 22f * u)
        quadraticTo(92f * u, 14f * u, 88f * u, 2f * u)
        quadraticTo(80f * u, 12f * u, 66f * u, 20f * u)
        close()
    }
    drawPath(leftAntler, antlerColor)
    drawPath(rightAntler, antlerColor)

    // Beak
    val beak = Path().apply {
        moveTo(54f * u, 30f * u)
        lineTo(66f * u, 30f * u)
        lineTo(60f * u, 40f * u)
        close()
    }
    drawPath(beak, beakColor)

    // Celestial Star Eyes
    drawCircle(starGlow.copy(alpha = 0.4f * pulse), center = Offset(50f * u, 25f * u), radius = 6f * u)
    drawCircle(starGlow.copy(alpha = 0.4f * pulse), center = Offset(70f * u, 25f * u), radius = 6f * u)
    drawCircle(Color.White, center = Offset(50f * u, 25f * u), radius = 3.5f * u)
    drawCircle(Color.White, center = Offset(70f * u, 25f * u), radius = 3.5f * u)
}

private fun DrawScope.drawPrimalAvatarLayer(frame: AvatarFrame) {
    val u = frame.u
    val goldDark = Color(0xFFF57F17)
    val goldMain = Color(0xFFFFD54F)
    val emeraldGlow = Color(0xFF00E676)
    val bodyDark = Color(0xFF1B5E20)
    val bodyMain = Color(0xFF2E7D32)
    val pulse = 0.6f + 0.4f * sin(frame.phase * 6.28f)

    // Radiating Ascended Nature & Solar Aura
    drawCircle(emeraldGlow.copy(alpha = 0.25f * pulse), center = Offset(60f * u, 50f * u), radius = 55f * u)
    drawCircle(goldMain.copy(alpha = 0.15f * pulse), center = Offset(60f * u, 50f * u), radius = 45f * u)

    // Astral Nature Wings
    val leftWing = Path().apply {
        moveTo(44f * u, 40f * u)
        quadraticTo(8f * u, 12f * u, 4f * u, 36f * u)
        quadraticTo(14f * u, 65f * u, 38f * u, 62f * u)
        close()
    }
    val rightWing = Path().apply {
        moveTo(76f * u, 40f * u)
        quadraticTo(112f * u, 12f * u, 116f * u, 36f * u)
        quadraticTo(106f * u, 65f * u, 82f * u, 62f * u)
        close()
    }
    drawPath(leftWing, emeraldGlow.copy(alpha = 0.4f * pulse))
    drawPath(rightWing, emeraldGlow.copy(alpha = 0.4f * pulse))

    // Avatar Beast Body
    drawRoundRect(bodyDark, topLeft = Offset(40f * u, 45f * u), size = Size(40f * u, 44f * u), cornerRadius = CornerRadius(18f * u))
    drawRoundRect(bodyMain, topLeft = Offset(42f * u, 47f * u), size = Size(36f * u, 40f * u), cornerRadius = CornerRadius(16f * u))

    // Glowing Primal Core
    drawCircle(goldMain.copy(alpha = 0.5f * pulse), center = Offset(60f * u, 60f * u), radius = 12f * u)
    drawCircle(Color.White, center = Offset(60f * u, 60f * u), radius = 5f * u)

    // Avatar Claws / Paws
    drawRoundRect(goldDark, topLeft = Offset(30f * u, 75f * u), size = Size(18f * u, 26f * u), cornerRadius = CornerRadius(8f * u))
    drawRoundRect(goldDark, topLeft = Offset(72f * u, 75f * u), size = Size(18f * u, 26f * u), cornerRadius = CornerRadius(8f * u))

    // Avatar Apex Head
    drawCircle(bodyDark, center = Offset(60f * u, 32f * u), radius = 19f * u)
    drawCircle(bodyMain, center = Offset(60f * u, 30f * u), radius = 17f * u)

    // Crown of Primal Light
    val leftCrown = Path().apply {
        moveTo(48f * u, 20f * u)
        lineTo(36f * u, 4f * u)
        lineTo(54f * u, 14f * u)
        close()
    }
    val rightCrown = Path().apply {
        moveTo(72f * u, 20f * u)
        lineTo(84f * u, 4f * u)
        lineTo(66f * u, 14f * u)
        close()
    }
    val centerCrown = Path().apply {
        moveTo(56f * u, 16f * u)
        lineTo(60f * u, 0f * u)
        lineTo(64f * u, 16f * u)
        close()
    }
    drawPath(leftCrown, goldMain)
    drawPath(rightCrown, goldMain)
    drawPath(centerCrown, goldDark)

    // Piercing Luminous Eyes
    drawCircle(Color.White, center = Offset(52f * u, 27f * u), radius = 4f * u)
    drawCircle(Color.White, center = Offset(68f * u, 27f * u), radius = 4f * u)
    drawCircle(emeraldGlow, center = Offset(52f * u, 27f * u), radius = 2f * u)
    drawCircle(emeraldGlow, center = Offset(68f * u, 27f * u), radius = 2f * u)
}

private fun DrawScope.drawIfritCompanion(f: AvatarFrame) {
    val u = f.u
    val fire = Color(0xFFFF5722)
    val fireDark = Color(0xFFBF360C)
    val fireCore = Color(0xFFFDE047)
    val glow = Color(0xFFFF9800).copy(alpha = 0.4f)
    
    // Floating over the left shoulder with heat breathing wave
    val x = 16f * u
    val y = (32f + 4f * sin(f.phase * 6.28f)) * u
    
    // Flame & Heat Aura
    drawCircle(glow, center = Offset(x, y), radius = 15f * u)
    
    // Roaring Magma Body
    drawCircle(fireDark, center = Offset(x, y), radius = 9f * u)
    drawCircle(fire, center = Offset(x, y - 2f * u), radius = 7f * u)
    drawCircle(fireCore, center = Offset(x, y - 2.5f * u), radius = 4f * u)
    
    // Swept Magma Horns (Curving outward and up)
    val leftHorn = Path().apply {
        moveTo(x - 4f * u, y - 4f * u)
        quadraticTo(x - 14f * u, y - 14f * u, x - 6f * u, y - 18f * u)
        quadraticTo(x - 2f * u, y - 10f * u, x - 2f * u, y - 5f * u)
        close()
    }
    val rightHorn = Path().apply {
        moveTo(x + 4f * u, y - 4f * u)
        quadraticTo(x + 14f * u, y - 14f * u, x + 6f * u, y - 18f * u)
        quadraticTo(x + 2f * u, y - 10f * u, x + 2f * u, y - 5f * u)
        close()
    }
    drawPath(leftHorn, Color(0xFF1F120E))
    drawPath(rightHorn, Color(0xFF1F120E))
    drawPath(leftHorn, fire, style = Stroke(width = 1.2f * u))
    drawPath(rightHorn, fire, style = Stroke(width = 1.2f * u))

    // Blazing Eyes
    drawCircle(Color(0xFFFEF08A), center = Offset(x - 2.5f * u, y - 3f * u), radius = 1.2f * u)
    drawCircle(Color(0xFFFEF08A), center = Offset(x + 2.5f * u, y - 3f * u), radius = 1.2f * u)
    
    // Rising Ember Particles
    val emberY1 = y - (12f + (f.phase * 16f) % 16f) * u
    val emberY2 = y - (8f + ((f.phase + 0.5f) * 16f) % 16f) * u
    drawCircle(fireCore, center = Offset(x - 4f * u, emberY1), radius = 0.9f * u)
    drawCircle(fire, center = Offset(x + 5f * u, emberY2), radius = 1.1f * u)
}

private fun DrawScope.drawShivaCompanion(f: AvatarFrame) {
    val u = f.u
    val ice = Color(0xFF38BDF8)
    val iceLight = Color(0xFFE0F2FE)
    val iceDeep = Color(0xFF0284C7)
    val aura = Color(0xFF7DD3FC).copy(alpha = 0.35f)
    
    // Floating over the right shoulder
    val x = 84f * u
    val y = (30f + 3.5f * cos(f.phase * 6.28f)) * u
    
    // Frost Aura
    drawCircle(aura, center = Offset(x, y), radius = 16f * u)
    
    // Multi-faceted Diamond Crystal Body
    val crystalPath = Path().apply {
        moveTo(x, y - 12f * u)
        lineTo(x + 7f * u, y - 2f * u)
        lineTo(x + 5f * u, y + 10f * u)
        lineTo(x, y + 14f * u)
        lineTo(x - 5f * u, y + 10f * u)
        lineTo(x - 7f * u, y - 2f * u)
        close()
    }
    drawPath(crystalPath, iceDeep)
    
    // Inner Crystal Facet Highlights
    val facetLeft = Path().apply {
        moveTo(x, y - 12f * u)
        lineTo(x, y + 14f * u)
        lineTo(x - 7f * u, y - 2f * u)
        close()
    }
    drawPath(facetLeft, ice)
    
    val facetCore = Path().apply {
        moveTo(x, y - 8f * u)
        lineTo(x + 3.5f * u, y)
        lineTo(x, y + 8f * u)
        lineTo(x - 3.5f * u, y)
        close()
    }
    drawPath(facetCore, iceLight)
    drawPath(crystalPath, Color.White, style = Stroke(width = 1f * u))
    
    // Orbiting Ice Shards
    val shardAngle = f.phase * 6.28f
    val shardR = 14f * u
    val s1x = x + shardR * cos(shardAngle)
    val s1y = y + (shardR * 0.5f) * sin(shardAngle)
    val s2x = x + shardR * cos(shardAngle + 3.14f)
    val s2y = y + (shardR * 0.5f) * sin(shardAngle + 3.14f)
    
    drawCircle(iceLight, center = Offset(s1x, s1y), radius = 2f * u)
    drawCircle(iceLight, center = Offset(s2x, s2y), radius = 2f * u)
}

private fun DrawScope.drawBahamutCompanion(f: AvatarFrame) {
    val u = f.u
    val celestial = Color(0xFFA855F7)
    val celestialLight = Color(0xFFF0ABFC)
    val starGold = Color(0xFFFBBF24)
    val aura = Color(0xFF818CF8).copy(alpha = 0.35f)
    
    // Hovering above head as an Astral Celestial Dragon Entity
    val x = 50f * u
    val y = (16f + 3f * sin(f.phase * 6.28f)) * u
    
    drawCircle(aura, center = Offset(x, y), radius = 20f * u)
    
    // Celestial Wings (Sweeping outward)
    val leftWing = Path().apply {
        moveTo(x - 4f * u, y)
        lineTo(x - 22f * u, y - 10f * u)
        lineTo(x - 16f * u, y + 4f * u)
        lineTo(x - 6f * u, y + 2f * u)
        close()
    }
    val rightWing = Path().apply {
        moveTo(x + 4f * u, y)
        lineTo(x + 22f * u, y - 10f * u)
        lineTo(x + 16f * u, y + 4f * u)
        lineTo(x + 6f * u, y + 2f * u)
        close()
    }
    drawPath(leftWing, celestial)
    drawPath(rightWing, celestial)
    drawPath(leftWing, celestialLight, style = Stroke(width = 1.2f * u))
    drawPath(rightWing, celestialLight, style = Stroke(width = 1.2f * u))
    
    // Astral Dragon Head & Crown
    drawCircle(Color(0xFF3B0764), center = Offset(x, y), radius = 6f * u)
    drawCircle(starGold, center = Offset(x, y), radius = 2.5f * u)
    
    // Cosmic Halo Crown
    drawCircle(starGold, center = Offset(x, y - 8f * u), radius = 4f * u, style = Stroke(width = 1.5f * u))
}

private fun DrawScope.drawWyvernCompanion(f: AvatarFrame) {
    val u = f.u
    val skin = Color(0xFF334155)
    val skinLight = Color(0xFF475569)
    val wingMembrane = Color(0xFF0F766E)
    val wingBone = Color(0xFF1E293B)
    val eye = Color(0xFF34D399)
    
    // Hovering above / around the Dragoon's head in a soaring arc
    val x = (48f + 16f * sin(f.phase * 3.14f)) * u
    val y = (18f + 3f * sin(f.phase * 6.28f)) * u
    val wingFlap = sin(f.phase * 6.28f) * 4f * u
    
    // Dragon Wing Membranes
    val leftWing = Path().apply {
        moveTo(x - 3f * u, y)
        lineTo(x - 18f * u, y - 10f * u + wingFlap)
        lineTo(x - 14f * u, y + 4f * u + wingFlap * 0.5f)
        lineTo(x - 8f * u, y + 1f * u)
        close()
    }
    val rightWing = Path().apply {
        moveTo(x + 3f * u, y)
        lineTo(x + 18f * u, y - 10f * u + wingFlap)
        lineTo(x + 14f * u, y + 4f * u + wingFlap * 0.5f)
        lineTo(x + 8f * u, y + 1f * u)
        close()
    }
    drawPath(leftWing, wingMembrane)
    drawPath(rightWing, wingMembrane)
    drawPath(leftWing, wingBone, style = Stroke(width = 1.5f * u))
    drawPath(rightWing, wingBone, style = Stroke(width = 1.5f * u))
    
    // Wyvern Body & Ribbed Underbelly
    drawRoundRect(skin, topLeft = Offset(x - 7f * u, y - 3.5f * u), size = Size(14f * u, 7f * u), cornerRadius = CornerRadius(3.5f * u))
    drawRoundRect(skinLight, topLeft = Offset(x - 5f * u, y - 1f * u), size = Size(10f * u, 3.5f * u), cornerRadius = CornerRadius(1.5f * u))
    
    // Dragon Head & Horns
    drawCircle(skin, center = Offset(x + 8f * u, y - 1f * u), radius = 4f * u)
    // Swept Horn
    val horn = Path().apply {
        moveTo(x + 6f * u, y - 3f * u)
        lineTo(x + 2f * u, y - 8f * u)
        lineTo(x + 7f * u, y - 4f * u)
        close()
    }
    drawPath(horn, Color(0xFF0F172A))
    
    // Glowing Emerald Dragon Eye
    drawCircle(eye, center = Offset(x + 9.5f * u, y - 2f * u), radius = 1.3f * u)
    
    // Long Sinuous Tail with Fluke Barb
    val tail = Path().apply {
        moveTo(x - 7f * u, y)
        quadraticTo(x - 14f * u, y + 6f * u, x - 18f * u, y + 2f * u)
    }
    drawPath(tail, skin, style = Stroke(width = 2f * u, cap = StrokeCap.Round))
    // Tail Fluke Barb
    val barb = Path().apply {
        moveTo(x - 18f * u, y + 2f * u)
        lineTo(x - 22f * u, y - 2f * u)
        lineTo(x - 21f * u, y + 5f * u)
        close()
    }
    drawPath(barb, wingMembrane)
}

private fun DrawScope.drawSkeletonCompanion(f: AvatarFrame) {
    val u = f.u
    val bone = Color(0xFFF1F5F9)
    val boneShade = Color(0xFF94A3B8)
    val soulfire = Color(0xFF2DD4BF)
    val mist = Color(0xFF0F172A).copy(alpha = 0.5f)
    
    // Floating over the left shoulder rising from necrotic mist
    val x = 16f * u
    val y = (32f + 3.5f * sin(f.phase * 6.28f)) * u
    
    // Necrotic Mist Cloud
    drawCircle(mist, center = Offset(x, y + 8f * u), radius = 10f * u)
    
    // Skull Dome
    drawOval(bone, topLeft = Offset(x - 5.5f * u, y - 7f * u), size = Size(11f * u, 9f * u))
    drawOval(boneShade, topLeft = Offset(x - 3.5f * u, y + 1f * u), size = Size(7f * u, 5f * u)) // Cheekbones / Maxilla
    
    // Skull Eye Sockets
    drawCircle(Color.Black, center = Offset(x - 2.5f * u, y - 2f * u), radius = 1.8f * u)
    drawCircle(Color.Black, center = Offset(x + 2.5f * u, y - 2f * u), radius = 1.8f * u)
    // Pulsing Soulfire Eyes
    drawCircle(soulfire, center = Offset(x - 2.5f * u, y - 2f * u), radius = 1.1f * u)
    drawCircle(soulfire, center = Offset(x + 2.5f * u, y - 2f * u), radius = 1.1f * u)
    
    // Articulated Teeth
    for (i in -2..2) {
        drawLine(Color(0xFF334155), start = Offset(x + i * 1.2f * u, y + 2.5f * u), end = Offset(x + i * 1.2f * u, y + 5f * u), strokeWidth = 0.8f * u)
    }
    
    // Spine & Ribcage
    drawLine(bone, start = Offset(x, y + 6f * u), end = Offset(x, y + 16f * u), strokeWidth = 2f * u)
    for (i in 0..2) {
        val ribY = y + (8f + i * 3f) * u
        drawLine(bone, start = Offset(x - 4.5f * u, ribY), end = Offset(x + 4.5f * u, ribY), strokeWidth = 1.5f * u, cap = StrokeCap.Round)
    }
    
    // Spectral Tomb Dagger
    drawLine(Color(0xFFCBD5E1), start = Offset(x - 6f * u, y + 4f * u), end = Offset(x - 10f * u, y - 4f * u), strokeWidth = 2f * u)
    drawCircle(soulfire, center = Offset(x - 6f * u, y + 4f * u), radius = 1.5f * u)
}

private fun DrawScope.drawUndeadArmyCompanion(f: AvatarFrame) {
    val u = f.u
    val bone = Color(0xFFF1F5F9)
    val deathKnightHelm = Color(0xFF1E293B)
    val soulfire = Color(0xFF2DD4BF)
    val crimsonEye = Color(0xFFEF4444)
    
    // 1. Draw Left Flank: Skeleton Minion
    drawSkeletonCompanion(f)
    
    // 2. Draw Right Flank: Armored Death Knight
    val x = 84f * u
    val y = (34f + 3f * cos(f.phase * 6.28f)) * u
    
    // Death Knight Horned Iron Helm
    drawRoundRect(deathKnightHelm, topLeft = Offset(x - 6f * u, y - 7f * u), size = Size(12f * u, 12f * u), cornerRadius = CornerRadius(3f * u))
    
    // Spiked Horns
    val leftHorn = Path().apply {
        moveTo(x - 5f * u, y - 4f * u)
        lineTo(x - 11f * u, y - 10f * u)
        lineTo(x - 4f * u, y - 7f * u)
        close()
    }
    val rightHorn = Path().apply {
        moveTo(x + 5f * u, y - 4f * u)
        lineTo(x + 11f * u, y - 10f * u)
        lineTo(x + 4f * u, y - 7f * u)
        close()
    }
    drawPath(leftHorn, deathKnightHelm)
    drawPath(rightHorn, deathKnightHelm)
    
    // Glowing Crimson Visor Slit
    drawRoundRect(Color.Black, topLeft = Offset(x - 4.5f * u, y - 2f * u), size = Size(9f * u, 3f * u), cornerRadius = CornerRadius(1f * u))
    drawCircle(crimsonEye, center = Offset(x - 2f * u, y - 0.5f * u), radius = 1.2f * u)
    drawCircle(crimsonEye, center = Offset(x + 2f * u, y - 0.5f * u), radius = 1.2f * u)
    
    // Iron Shoulder Spikes
    drawRect(deathKnightHelm, topLeft = Offset(x - 7f * u, y + 5f * u), size = Size(14f * u, 8f * u))
    drawPath(Path().apply {
        moveTo(x + 5f * u, y + 5f * u)
        lineTo(x + 10f * u, y)
        lineTo(x + 7f * u, y + 8f * u)
        close()
    }, deathKnightHelm)
    
    // 3. Hovering Soul Wisps in the background
    val wisp1X = 28f * u
    val wisp1Y = (18f + 3f * sin((f.phase + 0.3f) * 6.28f)) * u
    val wisp2X = 72f * u
    val wisp2Y = (16f + 3f * cos((f.phase + 0.6f) * 6.28f)) * u
    
    drawCircle(soulfire.copy(alpha = 0.5f), center = Offset(wisp1X, wisp1Y), radius = 3.5f * u)
    drawCircle(Color.White.copy(alpha = 0.8f), center = Offset(wisp1X, wisp1Y), radius = 1.5f * u)
    drawCircle(soulfire.copy(alpha = 0.5f), center = Offset(wisp2X, wisp2Y), radius = 3f * u)
    drawCircle(Color.White.copy(alpha = 0.8f), center = Offset(wisp2X, wisp2Y), radius = 1.2f * u)
}

private fun DrawScope.drawHunterWolfCompanion(f: AvatarFrame) {
    val u = f.u
    val fur = Color(0xFF475569)
    val furDark = Color(0xFF1E293B)
    val furLight = Color(0xFF94A3B8)
    val eyeGold = Color(0xFFF59E0B)
    val collar = Color(0xFF0F766E)
    val collarGold = Color(0xFFFBBF24)

    // Positioned on the left pedestal flank
    val x = 16f * u
    val y = 78f * u
    val breath = sin(f.phase * 6.28f) * 1.2f * u
    val tailWag = sin(f.phase * 6.28f) * 3f * u

    // Bushy Tail
    val tail = Path().apply {
        moveTo(x - 6f * u, y + 8f * u)
        quadraticTo(x - 14f * u + tailWag, y + 2f * u, x - 12f * u + tailWag, y - 6f * u)
        quadraticTo(x - 8f * u + tailWag * 0.5f, y + 2f * u, x - 4f * u, y + 10f * u)
        close()
    }
    drawPath(tail, furDark)
    drawPath(tail, fur, style = Stroke(width = 1.2f * u))

    // Body (Seated Wolf)
    drawRoundRect(fur, topLeft = Offset(x - 7f * u, y - 2f * u + breath * 0.5f), size = Size(14f * u, 16f * u), cornerRadius = CornerRadius(5f * u))
    // Fluff Chest
    drawOval(furLight, topLeft = Offset(x - 3f * u, y + breath * 0.5f), size = Size(9f * u, 12f * u))

    // Front Paws
    drawRoundRect(furDark, topLeft = Offset(x - 1f * u, y + 10f * u), size = Size(4f * u, 6f * u), cornerRadius = CornerRadius(2f * u))
    drawRoundRect(furDark, topLeft = Offset(x + 4f * u, y + 10f * u), size = Size(4f * u, 6f * u), cornerRadius = CornerRadius(2f * u))

    // Armored Collar
    drawRoundRect(collar, topLeft = Offset(x - 5f * u, y - 4f * u + breath), size = Size(11f * u, 3f * u), cornerRadius = CornerRadius(1.5f * u))
    drawCircle(collarGold, center = Offset(x + 0.5f * u, y - 2.5f * u + breath), radius = 1.5f * u)

    // Wolf Head
    val headY = y - 9f * u + breath
    drawOval(fur, topLeft = Offset(x - 5f * u, headY - 4f * u), size = Size(12f * u, 10f * u))

    // Alert Pointed Ears
    val earTwitch = sin(f.phase * 3.14f) * 1.5f * u
    val leftEar = Path().apply {
        moveTo(x - 4f * u, headY - 2f * u)
        lineTo(x - 7f * u + earTwitch, headY - 11f * u)
        lineTo(x - 1f * u, headY - 4f * u)
        close()
    }
    val rightEar = Path().apply {
        moveTo(x + 2f * u, headY - 4f * u)
        lineTo(x + 5f * u + earTwitch, headY - 11f * u)
        lineTo(x + 7f * u, headY - 2f * u)
        close()
    }
    drawPath(leftEar, furDark)
    drawPath(rightEar, furDark)
    drawCircle(Color(0xFFCBD5E1), center = Offset(x - 4f * u, headY - 5f * u), radius = 1.5f * u)

    // Muzzle & Snout
    val muzzle = Path().apply {
        moveTo(x + 3f * u, headY)
        lineTo(x + 10f * u, headY + 1f * u)
        lineTo(x + 4f * u, headY + 4f * u)
        close()
    }
    drawPath(muzzle, furLight)
    drawCircle(Color.Black, center = Offset(x + 9.5f * u, headY + 1f * u), radius = 1.2f * u) // Nose

    // Fierce Glowing Amber Eye
    drawCircle(Color.Black, center = Offset(x + 2f * u, headY - 0.5f * u), radius = 2f * u)
    drawCircle(eyeGold, center = Offset(x + 2.5f * u, headY - 0.5f * u), radius = 1.3f * u)
    drawCircle(Color.White, center = Offset(x + 3f * u, headY - 1f * u), radius = 0.5f * u)
}

private fun DrawScope.drawHunterFalconCompanion(f: AvatarFrame) {
    val u = f.u
    val brown = Color(0xFF78350F)
    val brownLight = Color(0xFFD97706)
    val cream = Color(0xFFFEF3C7)
    val beakGold = Color(0xFFF59E0B)
    val eyeCyan = Color(0xFF0284C7)

    // Soaring gracefully over the left shoulder
    val x = 18f * u
    val y = (26f + 3.5f * sin(f.phase * 6.28f)) * u
    val wingFlap = sin(f.phase * 6.28f) * 4.5f * u

    // Falcon Wings
    val leftWing = Path().apply {
        moveTo(x - 2f * u, y)
        lineTo(x - 16f * u, y - 9f * u + wingFlap)
        lineTo(x - 12f * u, y + 3f * u + wingFlap * 0.4f)
        lineTo(x - 4f * u, y + 2f * u)
        close()
    }
    val rightWing = Path().apply {
        moveTo(x + 2f * u, y)
        lineTo(x + 15f * u, y - 9f * u + wingFlap)
        lineTo(x + 11f * u, y + 3f * u + wingFlap * 0.4f)
        lineTo(x + 4f * u, y + 2f * u)
        close()
    }
    drawPath(leftWing, brown)
    drawPath(rightWing, brown)
    drawPath(leftWing, brownLight, style = Stroke(width = 1.2f * u))
    drawPath(rightWing, brownLight, style = Stroke(width = 1.2f * u))

    // Feathered Body & Cream Breast
    drawRoundRect(brown, topLeft = Offset(x - 4.5f * u, y - 3f * u), size = Size(9f * u, 10f * u), cornerRadius = CornerRadius(3.5f * u))
    drawOval(cream, topLeft = Offset(x - 2.5f * u, y - 1f * u), size = Size(5f * u, 7f * u))

    // Falcon Tail Feathers
    val tail = Path().apply {
        moveTo(x - 2.5f * u, y + 6f * u)
        lineTo(x, y + 13f * u)
        lineTo(x + 2.5f * u, y + 6f * u)
        close()
    }
    drawPath(tail, brownLight)

    // Sleek Head & Hooked Golden Beak
    drawCircle(brown, center = Offset(x + 2f * u, y - 4f * u), radius = 3.5f * u)
    val beak = Path().apply {
        moveTo(x + 4.5f * u, y - 5f * u)
        lineTo(x + 8.5f * u, y - 3.5f * u)
        lineTo(x + 4.5f * u, y - 2f * u)
        close()
    }
    drawPath(beak, beakGold)

    // Predator Eye
    drawCircle(Color.Black, center = Offset(x + 3f * u, y - 4.5f * u), radius = 1.4f * u)
    drawCircle(eyeCyan, center = Offset(x + 3.2f * u, y - 4.5f * u), radius = 0.9f * u)
}

private fun DrawScope.drawHunterBearCompanion(f: AvatarFrame) {
    val u = f.u
    val bearBrown = Color(0xFF5A3825)
    val bearDark = Color(0xFF3B2314)
    val snoutTan = Color(0xFF8D5B4C)
    val eyeBlack = Color(0xFF1C1917)

    // Seated firmly on right flank
    val x = 80f * u
    val y = 78f * u
    val breath = sin(f.phase * 6.28f) * 1.2f * u

    // Broad Body & Thick Shoulders
    drawRoundRect(bearBrown, topLeft = Offset(x - 8f * u, y - 4f * u + breath * 0.5f), size = Size(16f * u, 18f * u), cornerRadius = CornerRadius(6f * u))
    // Broad Paws
    drawRoundRect(bearDark, topLeft = Offset(x - 6f * u, y + 10f * u), size = Size(5f * u, 5f * u), cornerRadius = CornerRadius(2.5f * u))
    drawRoundRect(bearDark, topLeft = Offset(x + 1f * u, y + 10f * u), size = Size(5f * u, 5f * u), cornerRadius = CornerRadius(2.5f * u))

    // Bear Head
    val headY = y - 9f * u + breath
    drawCircle(bearBrown, center = Offset(x - 1f * u, headY), radius = 6.5f * u)

    // Rounded Bear Ears
    drawCircle(bearDark, center = Offset(x - 6f * u, headY - 5.5f * u), radius = 2.5f * u)
    drawCircle(bearDark, center = Offset(x + 4f * u, headY - 5.5f * u), radius = 2.5f * u)
    drawCircle(snoutTan, center = Offset(x - 6f * u, headY - 5.5f * u), radius = 1.2f * u)
    drawCircle(snoutTan, center = Offset(x + 4f * u, headY - 5.5f * u), radius = 1.2f * u)

    // Stout Snout & Nose
    drawOval(snoutTan, topLeft = Offset(x - 6f * u, headY - 1f * u), size = Size(7f * u, 5.5f * u))
    drawCircle(eyeBlack, center = Offset(x - 4f * u, headY + 0.5f * u), radius = 1.5f * u)

    // Alert Eyes
    drawCircle(eyeBlack, center = Offset(x - 2f * u, headY - 2.5f * u), radius = 1.3f * u)
    drawCircle(Color.White, center = Offset(x - 2.3f * u, headY - 2.8f * u), radius = 0.5f * u)
}
