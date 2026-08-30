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
import com.fitnessquest.rpg.domain.WearArchetype
import com.fitnessquest.rpg.domain.visuals.BodyRegion
import com.fitnessquest.rpg.domain.visuals.ProceduralMaterialFinish
import com.fitnessquest.rpg.domain.visuals.ProceduralOrnament
import com.fitnessquest.rpg.domain.visuals.ProceduralSilhouette
import com.fitnessquest.rpg.domain.visuals.ProceduralTrim
import com.fitnessquest.rpg.domain.visuals.ProceduralVisualSignature
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
enum class HeadgearShape { HELM, HAT, HOOD, CROWN, CAP }

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

    fun palette(item: ItemEntity): GearPalette {
        val desc = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(item)
        return GearPalette(desc.primaryColor, desc.secondaryColor, desc.glowColor)
    }

    fun signature(item: ItemEntity): ProceduralVisualSignature =
        com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry
            .resolveVisualDescriptor(item)
            .proceduralSignature

    fun accent(item: ItemEntity): Color {
        val descriptor = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(item)
        descriptor.accentColor?.let { return it }
        return listOf(
            Color(0xFFD97706), // amber
            Color(0xFF06B6D4), // cyan
            Color(0xFF7C3AED), // violet
            Color(0xFF16A34A), // emerald
            Color(0xFFDC2626), // crimson
            Color(0xFFE2E8F0)  // silver
        )[descriptor.proceduralSignature.accentFamily]
    }

    fun headgearShape(item: ItemEntity): HeadgearShape = when {
        item.name.contains("hood", ignoreCase = true) || item.name.contains("cowl", ignoreCase = true) ||
            item.name.contains("coif", ignoreCase = true) -> HeadgearShape.HOOD
        item.name.contains("crown", ignoreCase = true) || item.name.contains("circlet", ignoreCase = true) -> HeadgearShape.CROWN
        item.name.contains("hat", ignoreCase = true) -> HeadgearShape.HAT
        item.name.contains("helm", ignoreCase = true) || item.name.contains("helmet", ignoreCase = true) -> HeadgearShape.HELM
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

    /**
     * Preview appearances commonly specify only a race. In that case, use a
     * race-appropriate base tone instead of silently retaining human skin on
     * the back/body pass. Explicitly customized skin colors remain untouched.
     */
    private val resolvedSkinColor: Color
        get() = if (appearance.skinColor != DefaultAppearance.skinColor) {
            appearance.skinColor
        } else {
            when (appearance.race) {
                CharacterRace.ORC -> Color(0xFF6F9358)
                CharacterRace.UNDEAD -> Color(0xFF81907A)
                else -> appearance.skinColor
            }
        }

    fun skinColor(vararg muscles: String): Color {
        return if (hasHighlight(*muscles)) Color(0xFFE57373) else resolvedSkinColor
    }

    fun skinShade(vararg muscles: String): Color {
        val sc = resolvedSkinColor
        val baseShade = sc.copy(
            red = (sc.red * 0.88f).coerceIn(0f, 1f),
            green = (sc.green * 0.84f).coerceIn(0f, 1f),
            blue = (sc.blue * 0.78f).coerceIn(0f, 1f)
        )
        return if (hasHighlight(*muscles)) Color(0xFFD32F2F) else baseShade
    }
}

internal object AvatarPainter {
    private fun androidx.compose.ui.graphics.drawscope.DrawTransform.applyFocusTransform(
        focus: AvatarFocus,
        u: Float,
        width: Float,
        height: Float
    ) {
        when (focus) {
            AvatarFocus.LOWER_BODY -> {
                scale(scaleX = 1.6f, scaleY = 1.6f, pivot = Offset(width / 2f, height))
                translate(left = 0f, top = -15f * u)
            }
            AvatarFocus.PORTRAIT -> {
                scale(scaleX = 1.85f, scaleY = 1.85f, pivot = Offset(width / 2f, 40f * u))
                translate(left = 0f, top = 25f * u)
            }
            AvatarFocus.FULL_BODY -> Unit
        }
    }

    /**
     * Renders background aura, shadow, and class silhouette.
     * Guaranteed to render BEFORE front capes and base anatomy in the interleaved stack.
     */
    fun DrawScope.drawBackdrop(
        frame: AvatarFrame,
        pose: AvatarPose,
        focus: AvatarFocus = AvatarFocus.FULL_BODY,
        tilt: Offset = Offset.Zero
    ) {
        clipRect {
            withTransform({
                applyFocusTransform(focus, frame.u, size.width, size.height)
            }) {
                val bgShift = Offset(tilt.x * 3f * frame.u, tilt.y * 3f * frame.u)
                translate(bgShift.x, bgShift.y) {
                    drawAuraLayer(frame)
                    drawShadowLayer(frame)
                    drawClassSilhouetteLayer(frame)
                }
            }
        }
    }

    /**
     * Renders base humanoid/druid anatomy (body, face, hair, arms, underwear) and companions exactly once.
     * Excludes all gear layers (weapons, chest armor, helms, gauntlets, pants, boots, trinkets) and backdrop.
     */
    fun DrawScope.drawBodyBase(
        frame: AvatarFrame,
        pose: AvatarPose,
        focus: AvatarFocus = AvatarFocus.FULL_BODY,
        tilt: Offset = Offset.Zero
    ) {
        clipRect {
            withTransform({
                applyFocusTransform(focus, frame.u, size.width, size.height)
            }) {
                val bodyShift = Offset(tilt.x * 12f * frame.u, tilt.y * 12f * frame.u)
                translate(bodyShift.x, bodyShift.y) {
                    val form = frame.appearance.druidForm
                    val isDismissed = form == "NONE" || form == "HUMAN"

                    if (frame.cls == CharacterClass.SUMMONER && !isDismissed) {
                        when (form) {
                            "SHIVA" -> drawShivaCompanion(frame)
                            "BAHAMUT" -> drawBahamutCompanion(frame)
                            "IFRIT" -> drawIfritCompanion(frame)
                            else -> {
                                if (form.isNotEmpty()) {
                                    if ((frame.weapon?.tier ?: 1) >= 4) drawBahamutCompanion(frame)
                                    else if ((frame.weapon?.tier ?: 1) >= 2) drawShivaCompanion(frame)
                                    else drawIfritCompanion(frame)
                                }
                            }
                        }
                    }
                    if (frame.cls == CharacterClass.NECROMANCER && !isDismissed) {
                        when (form) {
                            "ARMY" -> drawUndeadArmyCompanion(frame)
                            "SKELETON" -> drawSkeletonCompanion(frame)
                            else -> {
                                if (form.isNotEmpty()) {
                                    if ((frame.weapon?.tier ?: 1) >= 3) drawUndeadArmyCompanion(frame)
                                    else drawSkeletonCompanion(frame)
                                }
                            }
                        }
                    }
                    if (frame.cls == CharacterClass.DRAGOON && !isDismissed && form != "") {
                        drawWyvernCompanion(frame)
                    }
                    if (frame.cls == CharacterClass.RANGER && !isDismissed) {
                        when (form) {
                            "FALCON" -> drawHunterFalconCompanion(frame)
                            "BEAR" -> drawHunterBearCompanion(frame)
                            "WOLF" -> drawHunterWolfCompanion(frame)
                            else -> if (form.isNotEmpty()) drawHunterWolfCompanion(frame)
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
                            withTransform({
                                scale(
                                    scaleX = build.width,
                                    scaleY = build.height,
                                    pivot = Offset(size.width / 2f, 108f * frame.u)
                                )
                            }) {
                                drawBareLowerBody(frame, pose)

                                withTransform({
                                    translate(pose.bodyOffset.x, pose.bodyOffset.y)
                                }) {
                                    drawNeckLayer(frame)
                                    drawTorsoLayer(frame)
                                    drawRaceBodyAccents(frame)
                                }

                                drawBareArms(frame, pose)
                                drawRaceArmAccents(frame, pose)

                                withTransform({
                                    translate(pose.headOffset.x, pose.headOffset.y)
                                }) {
                                    drawHeadBase(frame)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Renders ONLY the procedural Canvas equipment for a single equipment slot.
     * Guaranteed zero base anatomy redrawing (no face, body, arms, or legs skin).
     */
    fun DrawScope.drawSlotEquipment(
        slot: ItemSlot,
        frame: AvatarFrame,
        pose: AvatarPose,
        focus: AvatarFocus = AvatarFocus.FULL_BODY,
        tilt: Offset = Offset.Zero
    ) {
        clipRect {
            withTransform({
                applyFocusTransform(focus, frame.u, size.width, size.height)
            }) {
                val bodyShift = Offset(tilt.x * 12f * frame.u, tilt.y * 12f * frame.u)
                translate(bodyShift.x, bodyShift.y) {
                    val build = frame.appearance.race.build()
                    withTransform({
                        scale(
                            scaleX = build.width,
                            scaleY = build.height,
                            pivot = Offset(size.width / 2f, 108f * frame.u)
                        )
                    }) {
                        when (slot) {
                            ItemSlot.LEGS -> drawLegsGear(frame, pose)
                            ItemSlot.FEET -> drawFeetGear(frame, pose)
                            ItemSlot.CHEST -> {
                                withTransform({
                                    translate(pose.bodyOffset.x, pose.bodyOffset.y)
                                }) {
                                    drawChestGarmentLayer(frame)
                                }
                                drawChestArmArmor(frame, pose)
                            }
                            ItemSlot.HANDS -> drawHandsGear(frame, pose)
                            ItemSlot.HEAD -> {
                                withTransform({
                                    translate(pose.headOffset.x, pose.headOffset.y)
                                }) {
                                    drawHeadgearOnly(frame)
                                }
                            }
                            ItemSlot.TRINKET -> {
                                withTransform({
                                    translate(pose.bodyOffset.x, pose.bodyOffset.y)
                                }) {
                                    drawTrinketLayer(frame)
                                }
                            }
                            ItemSlot.WEAPON -> {
                                if (pose.prop == null) {
                                    drawWeaponLayer(frame, pose)
                                } else {
                                    drawPropLayer(frame, pose)
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    fun DrawScope.draw(frame: AvatarFrame, focus: AvatarFocus, tilt: Offset = Offset.Zero, animation: HeroAnimation = HeroAnimation.IDLE) {
        val pose = calculatePose(animation, frame.phase, frame.u)

        clipRect {
            withTransform({
                applyFocusTransform(focus, frame.u, size.width, size.height)
            }) {
                val bgShift = Offset(tilt.x * 3f * frame.u, tilt.y * 3f * frame.u)
                val bodyShift = Offset(tilt.x * 12f * frame.u, tilt.y * 12f * frame.u)

                translate(bgShift.x, bgShift.y) {
                    drawAuraLayer(frame)
                    drawShadowLayer(frame)
                    drawClassSilhouetteLayer(frame)
                }

                translate(bodyShift.x, bodyShift.y) {
                    val form = frame.appearance.druidForm
                    val isDismissed = form == "NONE" || form == "HUMAN"

                    if (frame.cls == CharacterClass.SUMMONER && !isDismissed) {
                        when (form) {
                            "SHIVA" -> drawShivaCompanion(frame)
                            "BAHAMUT" -> drawBahamutCompanion(frame)
                            "IFRIT" -> drawIfritCompanion(frame)
                            else -> {
                                if (form.isNotEmpty()) {
                                    if ((frame.weapon?.tier ?: 1) >= 4) drawBahamutCompanion(frame)
                                    else if ((frame.weapon?.tier ?: 1) >= 2) drawShivaCompanion(frame)
                                    else drawIfritCompanion(frame)
                                }
                            }
                        }
                    }
                    if (frame.cls == CharacterClass.NECROMANCER && !isDismissed) {
                        when (form) {
                            "ARMY" -> drawUndeadArmyCompanion(frame)
                            "SKELETON" -> drawSkeletonCompanion(frame)
                            else -> {
                                if (form.isNotEmpty()) {
                                    if ((frame.weapon?.tier ?: 1) >= 3) drawUndeadArmyCompanion(frame)
                                    else drawSkeletonCompanion(frame)
                                }
                            }
                        }
                    }
                    if (frame.cls == CharacterClass.DRAGOON && !isDismissed && form != "") {
                        drawWyvernCompanion(frame)
                    }
                    if (frame.cls == CharacterClass.RANGER && !isDismissed) {
                        when (form) {
                            "FALCON" -> drawHunterFalconCompanion(frame)
                            "BEAR" -> drawHunterBearCompanion(frame)
                            "WOLF" -> drawHunterWolfCompanion(frame)
                            else -> if (form.isNotEmpty()) drawHunterWolfCompanion(frame)
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
                Color(0xFFFDE68A).copy(alpha = 0.18f),
                topLeft = f.p(30.5f, 7.5f),
                size = f.s(39f, 13f),
                style = Stroke(width = 4.5f * f.u)
            )
            drawOval(
                Color(0xFFFDE68A).copy(alpha = 0.82f),
                topLeft = f.p(32f, 9f),
                size = f.s(36f, 10f),
                style = Stroke(width = 2.4f * f.u)
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
    }
}

internal fun DrawScope.drawBareArms(f: AvatarFrame, pose: AvatarPose) {
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
            if (f.robeChest != null && BodyRegion.LEFT_HAND !in f.hiddenRegions) {
                drawCircle(f.skinColor("hands"), radius = 5f * f.u, center = f.p(28.5f, 78f))
            }
        } else {
            drawBareArmSegments(23f, isLeft = true)
        }
    }

    withTransform({
        translate(pose.rightArmOffset.x, pose.rightArmOffset.y)
        rotate(pose.rightArmRotation, pivot = f.p(71f, 46f))
    }) {
        if (!isBare) {
            val sleeve = if (f.costume) f.look.outfitDark else GearVisuals.palette(f.robeChest!!).dark
            drawRoundRect(sleeve, topLeft = f.p(66f, 46f), size = f.s(11f, 30f), cornerRadius = CornerRadius(5f * f.u))
            if (f.robeChest != null && BodyRegion.RIGHT_HAND !in f.hiddenRegions) {
                drawCircle(f.skinColor("hands"), radius = 5f * f.u, center = f.p(71.5f, 78f))
            }
        } else {
            drawBareArmSegments(66f, isLeft = false)
        }
    }
}

internal fun DrawScope.drawHandsGear(f: AvatarFrame, pose: AvatarPose) {
    val item = f.hands ?: return
    val pal = GearVisuals.palette(item)
    val signature = GearVisuals.signature(item)
    val accent = GearVisuals.accent(item)

    fun DrawScope.drawGloveDetails(baseX: Float) {
        when (signature.trim) {
            ProceduralTrim.PLAIN -> Unit
            ProceduralTrim.BAND -> drawRect(accent, topLeft = f.p(baseX, 67f), size = f.s(12f, 1.2f))
            ProceduralTrim.SPLIT -> drawLine(accent, f.p(baseX + 6f, 67f), f.p(baseX + 6f, 75f), 1f * f.u)
            ProceduralTrim.CHEVRON -> {
                drawLine(accent, f.p(baseX + 2f, 70f), f.p(baseX + 6f, 73f), 1f * f.u)
                drawLine(accent, f.p(baseX + 6f, 73f), f.p(baseX + 10f, 70f), 1f * f.u)
            }
            ProceduralTrim.RUNIC -> for (x in listOf(3f, 6f, 9f)) {
                drawCircle(accent, radius = 0.7f * f.u, center = f.p(baseX + x, 71f))
            }
        }
        if (signature.ornament == ProceduralOrnament.GEM || signature.ornament == ProceduralOrnament.CREST) {
            drawCircle(accent, radius = 1.2f * f.u, center = f.p(baseX + 6f, 70.5f))
        }
    }

    withTransform({
        translate(pose.leftArmOffset.x, pose.leftArmOffset.y)
        rotate(pose.leftArmRotation, pivot = f.p(28f, 46f))
    }) {
        drawRoundRect(pal.main, topLeft = f.p(22.5f, 64f), size = f.s(12f, 12f), cornerRadius = CornerRadius(3f * f.u))
        drawRect(pal.dark, topLeft = f.p(22.5f, 64f), size = f.s(12f, 3f))
        if (f.facingBack) {
            // Dorsal knuckle protector plate
            drawRoundRect(pal.dark, topLeft = f.p(24f, 70f), size = f.s(9f, 4f), cornerRadius = CornerRadius(1.5f * f.u))
        }
        pal.glow?.let { g ->
            drawRect(g.copy(alpha = 0.85f), topLeft = f.p(22.5f, 67f), size = f.s(12f, 1.4f))
        }
        drawGloveDetails(22.5f)
    }

    withTransform({
        translate(pose.rightArmOffset.x, pose.rightArmOffset.y)
        rotate(pose.rightArmRotation, pivot = f.p(71f, 46f))
    }) {
        drawRoundRect(pal.main, topLeft = f.p(65.5f, 64f), size = f.s(12f, 12f), cornerRadius = CornerRadius(3f * f.u))
        drawRect(pal.dark, topLeft = f.p(65.5f, 64f), size = f.s(12f, 3f))
        if (f.facingBack) {
            // Dorsal knuckle protector plate
            drawRoundRect(pal.dark, topLeft = f.p(67f, 70f), size = f.s(9f, 4f), cornerRadius = CornerRadius(1.5f * f.u))
        }
        pal.glow?.let { g ->
            drawRect(g.copy(alpha = 0.85f), topLeft = f.p(65.5f, 67f), size = f.s(12f, 1.4f))
        }
        drawGloveDetails(65.5f)
    }
}

private fun DrawScope.drawArmsLayer(f: AvatarFrame, pose: AvatarPose) {
    drawBareArms(f, pose)
    drawChestArmArmor(f, pose)
    drawHandsGear(f, pose)
}

/** Chest-attached sleeves drawn over, rather than instead of, base anatomy. */
internal fun DrawScope.drawChestArmArmor(f: AvatarFrame, pose: AvatarPose) {
    val chest = f.chest?.takeIf { it.style == ItemStyle.PLATE || it.style == ItemStyle.LIGHT } ?: return
    val pal = GearVisuals.palette(chest)

    fun DrawScope.drawArmArmor(baseX: Float) {
        if (chest.style == ItemStyle.LIGHT) {
            // Short leather sleeves belong to the chest garment. Keep them in
            // the same articulated arm transform in both orientations so a
            // turn cannot leave the rear view with bare upper arms.
            drawRoundRect(
                pal.main,
                topLeft = f.p(baseX - 1f, 46f),
                size = f.s(11f, 14f),
                cornerRadius = CornerRadius(3.5f * f.u)
            )
            drawRect(pal.dark, topLeft = f.p(baseX - 1f, 56.5f), size = f.s(11f, 3.5f))
            if (chest.tier >= 2) {
                drawLine(
                    pal.dark,
                    start = f.p(baseX + 1f, 49f),
                    end = f.p(baseX + 1f, 55f),
                    strokeWidth = 1f * f.u
                )
            }
            return
        }

        drawRoundRect(pal.main, topLeft = f.p(baseX, 48f), size = f.s(9f, 13f), cornerRadius = CornerRadius(3f * f.u))
        drawRect(pal.dark, topLeft = f.p(baseX, 58f), size = f.s(9f, 3f))
        drawRoundRect(pal.main, topLeft = f.p(baseX, 61f), size = f.s(9f, 10f), cornerRadius = CornerRadius(2.5f * f.u))
        drawRect(pal.dark, topLeft = f.p(baseX, 67f), size = f.s(9f, 2f))
    }

    withTransform({
        translate(pose.leftArmOffset.x, pose.leftArmOffset.y)
        rotate(pose.leftArmRotation, pivot = f.p(28f, 46f))
    }) { drawArmArmor(24f) }

    withTransform({
        translate(pose.rightArmOffset.x, pose.rightArmOffset.y)
        rotate(pose.rightArmRotation, pivot = f.p(71f, 46f))
    }) { drawArmArmor(67f) }
}

internal fun DrawScope.drawBareLowerBody(f: AvatarFrame, pose: AvatarPose) {
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
        drawBareLegSegments(38f, isLeft = true)
    }

    withTransform({
        translate(pose.rightLegOffset.x, pose.rightLegOffset.y)
        rotate(pose.rightLegRotation, pivot = f.p(57f, 74f))
    }) {
        drawBareLegSegments(52f, isLeft = false)
    }

    // Modesty layer moves with the body, not the individual legs.
    if (BodyRegion.HIPS !in f.hiddenRegions) {
        withTransform({
            translate(pose.bodyOffset.x, pose.bodyOffset.y)
        }) {
            drawRoundRect(f.appearance.underwearColor, topLeft = f.p(36f, 71f), size = f.s(28f, 13f), cornerRadius = CornerRadius(4f * f.u))

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

internal fun DrawScope.drawLegsGear(f: AvatarFrame, pose: AvatarPose) {
    if (f.costume || f.robeChest != null) return
    val item = f.legs ?: return
    val legPal = GearVisuals.palette(item)
    val signature = GearVisuals.signature(item)
    val accent = GearVisuals.accent(item)

    fun DrawScope.drawLegDetails(baseX: Float) {
        when (signature.silhouette) {
            ProceduralSilhouette.BALANCED -> Unit
            ProceduralSilhouette.ANGULAR -> {
                drawLine(accent, f.p(baseX, 86f), f.p(baseX + 5f, 89f), 1f * f.u)
                drawLine(accent, f.p(baseX + 5f, 89f), f.p(baseX + 10f, 86f), 1f * f.u)
            }
            ProceduralSilhouette.CURVED -> drawOval(
                accent.copy(alpha = 0.8f), topLeft = f.p(baseX + 2f, 84.5f), size = f.s(6f, 4f)
            )
            ProceduralSilhouette.FORTIFIED -> drawRoundRect(
                legPal.dark, topLeft = f.p(baseX - 0.5f, 83.5f), size = f.s(11f, 7f),
                cornerRadius = CornerRadius(1.5f * f.u)
            )
        }
        if (signature.trim == ProceduralTrim.BAND || signature.trim == ProceduralTrim.RUNIC) {
            drawRect(accent, topLeft = f.p(baseX, 96f), size = f.s(10f, 1.2f))
        }
    }

    withTransform({
        translate(pose.leftLegOffset.x, pose.leftLegOffset.y)
        rotate(pose.leftLegRotation, pivot = f.p(43f, 74f))
    }) {
        drawRoundRect(legPal.main, topLeft = f.p(38f, 74f), size = f.s(10f, 30f), cornerRadius = CornerRadius(3f * f.u))
        if (!f.facingBack) {
            drawRoundRect(legPal.dark, topLeft = f.p(37.5f, 84f), size = f.s(11f, 6f), cornerRadius = CornerRadius(2f * f.u))
        } else {
            drawRect(legPal.dark, topLeft = f.p(38.5f, 84f), size = f.s(9f, 2f))
            drawRect(legPal.dark, topLeft = f.p(37.5f, 88f), size = f.s(11f, 2.5f))
            drawRect(legPal.dark, topLeft = f.p(37.5f, 93f), size = f.s(11f, 2.5f))
        }
        legPal.glow?.let { g ->
            drawRect(g.copy(alpha = 0.85f), topLeft = f.p(37.5f, 86f), size = f.s(11f, 1.2f))
        }
        drawLegDetails(38f)
    }

    withTransform({
        translate(pose.rightLegOffset.x, pose.rightLegOffset.y)
        rotate(pose.rightLegRotation, pivot = f.p(57f, 74f))
    }) {
        drawRoundRect(legPal.main, topLeft = f.p(52f, 74f), size = f.s(10f, 30f), cornerRadius = CornerRadius(3f * f.u))
        if (!f.facingBack) {
            drawRoundRect(legPal.dark, topLeft = f.p(51.5f, 84f), size = f.s(11f, 6f), cornerRadius = CornerRadius(2f * f.u))
        } else {
            drawRect(legPal.dark, topLeft = f.p(52.5f, 84f), size = f.s(9f, 2f))
            drawRect(legPal.dark, topLeft = f.p(51.5f, 88f), size = f.s(11f, 2.5f))
            drawRect(legPal.dark, topLeft = f.p(51.5f, 93f), size = f.s(11f, 2.5f))
        }
        legPal.glow?.let { g ->
            drawRect(g.copy(alpha = 0.85f), topLeft = f.p(51.5f, 86f), size = f.s(11f, 1.2f))
        }
        drawLegDetails(52f)
    }

    if (BodyRegion.HIPS !in f.hiddenRegions) {
        withTransform({
            translate(pose.bodyOffset.x, pose.bodyOffset.y)
        }) {
            drawRoundRect(legPal.main, topLeft = f.p(36f, 71f), size = f.s(28f, 13f), cornerRadius = CornerRadius(4f * f.u))
            drawRect(legPal.dark, topLeft = f.p(36f, 71f), size = f.s(28f, 3f))
        }
    }
}

internal fun DrawScope.drawFeetGear(f: AvatarFrame, pose: AvatarPose) {
    if (f.costume || f.robeChest != null) return
    val item = f.feet ?: return
    val feetPal = GearVisuals.palette(item)
    val signature = GearVisuals.signature(item)
    val accent = GearVisuals.accent(item)

    fun DrawScope.drawBootDetails(baseX: Float, width: Float) {
        when (signature.trim) {
            ProceduralTrim.PLAIN -> Unit
            ProceduralTrim.BAND -> drawRect(accent, topLeft = f.p(baseX, 101f), size = f.s(width, 1.2f))
            ProceduralTrim.SPLIT -> drawLine(accent, f.p(baseX + width / 2f, 99f), f.p(baseX + width / 2f, 106f), 1f * f.u)
            ProceduralTrim.CHEVRON -> {
                drawLine(accent, f.p(baseX + 2f, 102f), f.p(baseX + width / 2f, 105f), 1f * f.u)
                drawLine(accent, f.p(baseX + width / 2f, 105f), f.p(baseX + width - 2f, 102f), 1f * f.u)
            }
            ProceduralTrim.RUNIC -> for (x in listOf(3f, width - 3f)) {
                drawCircle(accent, radius = 0.7f * f.u, center = f.p(baseX + x, 103f))
            }
        }
        if (signature.finish == ProceduralMaterialFinish.ASCENDANT) {
            drawLine(accent.copy(alpha = 0.85f), f.p(baseX + 1f, 107f), f.p(baseX + width - 1f, 107f), 1f * f.u)
        }
    }

    withTransform({
        translate(pose.leftLegOffset.x, pose.leftLegOffset.y)
        rotate(pose.leftLegRotation, pivot = f.p(43f, 74f))
    }) {
        if (!f.facingBack) {
            drawRoundRect(feetPal.main, topLeft = f.p(36f, 98f), size = f.s(13f, 10f), cornerRadius = CornerRadius(3f * f.u))
            drawRect(feetPal.dark, topLeft = f.p(36f, 98f), size = f.s(13f, 3f))
        } else {
            drawRoundRect(feetPal.main, topLeft = f.p(37f, 98f), size = f.s(11f, 10f), cornerRadius = CornerRadius(3f * f.u))
            drawRect(feetPal.dark, topLeft = f.p(37f, 98f), size = f.s(11f, 3f))
            drawRect(feetPal.dark, topLeft = f.p(37.5f, 105f), size = f.s(10f, 3f))
            drawLine(feetPal.dark, start = f.p(42.5f, 98f), end = f.p(42.5f, 106f), strokeWidth = 1.5f * f.u)
        }
        feetPal.glow?.let { g ->
            drawRect(g.copy(alpha = 0.85f), topLeft = f.p(36f, 100.5f), size = f.s(13f, 1.2f))
        }
        drawBootDetails(if (f.facingBack) 37f else 36f, if (f.facingBack) 11f else 13f)
    }

    withTransform({
        translate(pose.rightLegOffset.x, pose.rightLegOffset.y)
        rotate(pose.rightLegRotation, pivot = f.p(57f, 74f))
    }) {
        if (!f.facingBack) {
            drawRoundRect(feetPal.main, topLeft = f.p(50f, 98f), size = f.s(13f, 10f), cornerRadius = CornerRadius(3f * f.u))
            drawRect(feetPal.dark, topLeft = f.p(50f, 98f), size = f.s(13f, 3f))
        } else {
            drawRoundRect(feetPal.main, topLeft = f.p(52f, 98f), size = f.s(11f, 10f), cornerRadius = CornerRadius(3f * f.u))
            drawRect(feetPal.dark, topLeft = f.p(52f, 98f), size = f.s(11f, 3f))
            drawRect(feetPal.dark, topLeft = f.p(52.5f, 105f), size = f.s(10f, 3f))
            drawLine(feetPal.dark, start = f.p(57.5f, 98f), end = f.p(57.5f, 106f), strokeWidth = 1.5f * f.u)
        }
        feetPal.glow?.let { g ->
            drawRect(g.copy(alpha = 0.85f), topLeft = f.p(50f, 100.5f), size = f.s(13f, 1.2f))
        }
        drawBootDetails(if (f.facingBack) 52f else 50f, if (f.facingBack) 11f else 13f)
    }
}

private fun DrawScope.drawLowerBodyLayer(f: AvatarFrame, pose: AvatarPose) {
    drawBareLowerBody(f, pose)
    drawLegsGear(f, pose)
    drawFeetGear(f, pose)
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
            f.skinColor("torso"),
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
    val archetype = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(armor).archetype
    if (archetype == com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARMOR_MAIL) {
        drawMailArmor(f, armor, pal)
        drawTorsoSignatureDetails(f, armor, pal)
        return
    }
    if (archetype == com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARMOR_CLOAK) {
        drawChestCloak(f, armor, pal)
        drawTorsoSignatureDetails(f, armor, pal)
        return
    }
    when (armor.style) {
        ItemStyle.ROBE -> {
            if (!f.facingBack) {
                // Front: Floor robe with clear sculpted footwear peeking below hem
                // Grounded footwear (Left & Right distinctly separated)
                val shoeDark = Color(0xFF0F172A)
                val shoeMain = pal.dark
                // Left Foot
                drawRoundRect(shoeMain, topLeft = f.p(33.5f, 96f), size = f.s(14f, 10f), cornerRadius = CornerRadius(3f * f.u))
                drawRect(shoeDark, topLeft = f.p(33f, 104f), size = f.s(15f, 2.5f))
                // Right Foot
                drawRoundRect(shoeMain, topLeft = f.p(52.5f, 96f), size = f.s(14f, 10f), cornerRadius = CornerRadius(3f * f.u))
                drawRect(shoeDark, topLeft = f.p(52f, 104f), size = f.s(15f, 2.5f))
                // Ground shadow
                drawOval(shoeDark.copy(alpha = 0.35f), topLeft = f.p(24f, 105.5f), size = f.s(52f, 3f))

                val robe = Path().apply {
                    moveTo(34f * f.u, 48.5f * f.u)
                    lineTo(43f * f.u, 48f * f.u)
                    quadraticTo(50f * f.u, 53.5f * f.u, 57f * f.u, 48f * f.u)
                    lineTo(66f * f.u, 48.5f * f.u)
                    lineTo(72f * f.u, 98f * f.u)
                    quadraticTo(50f * f.u, 96.5f * f.u, 28f * f.u, 98f * f.u)
                    close()
                }
                drawPath(robe, pal.main)
                drawRect(pal.dark, topLeft = f.p(47f, 53.5f), size = f.s(6f, 22f))
                val hem = f.legs?.let { GearVisuals.palette(it).main } ?: pal.dark
                drawRect(hem, topLeft = f.p(28f, 96.5f), size = f.s(44f, 3f))
                if (armor.tier >= 3) {
                    val rune = pal.glow ?: pal.dark
                    for (y in listOf(56f, 63f, 70f)) {
                        drawCircle(rune, radius = 1.4f * f.u, center = f.p(41f, y))
                        drawCircle(rune, radius = 1.4f * f.u, center = f.p(59f, y))
                    }
                }
                pal.glow?.let { g ->
                    drawPath(robe, g.copy(alpha = 0.8f), style = Stroke(width = 1.3f * f.u))
                }
            } else {
                // Back: Seamless draped cowl robe with sculpted footwear peeking below hem
                val shoeDark = Color(0xFF0F172A)
                val shoeMain = Color(0xFF1E293B)
                val shoeLight = Color(0xFF334155)
                // Ground shadow
                drawOval(shoeDark.copy(alpha = 0.35f), topLeft = f.p(24f, 105.5f), size = f.s(52f, 3f))

                val backRobe = Path().apply {
                    moveTo(34f * f.u, 47.5f * f.u)
                    lineTo(66f * f.u, 47.5f * f.u)
                    lineTo(72f * f.u, 98f * f.u)
                    quadraticTo(50f * f.u, 96.5f * f.u, 28f * f.u, 98f * f.u)
                    close()
                }
                drawPath(backRobe, pal.main)
                // Center spine drape line
                drawRect(pal.dark, topLeft = f.p(49f, 47.5f), size = f.s(2f, 48f))
                // Shoulder blade pleats
                drawRect(pal.dark.copy(alpha = 0.6f), topLeft = f.p(38f, 52f), size = f.s(1.5f, 18f))
                drawRect(pal.dark.copy(alpha = 0.6f), topLeft = f.p(60.5f, 52f), size = f.s(1.5f, 18f))
                // Hem band
                val hem = f.legs?.let { GearVisuals.palette(it).main } ?: pal.dark
                drawRect(hem, topLeft = f.p(28f, 96.5f), size = f.s(44f, 3f))
                // Exact 1:5 reconstruction of the authored front footwear:
                // identical slate colors, width, toe curve, instep, and sole.
                val leftShoe = Path().apply {
                    moveTo(34.8f * f.u, 100f * f.u)
                    lineTo(44.8f * f.u, 100f * f.u)
                    lineTo(44.8f * f.u, 105.6f * f.u)
                    quadraticTo(39.8f * f.u, 106.8f * f.u, 34.8f * f.u, 105.6f * f.u)
                    close()
                }
                val rightShoe = Path().apply {
                    moveTo(55.2f * f.u, 100f * f.u)
                    lineTo(65.2f * f.u, 100f * f.u)
                    lineTo(65.2f * f.u, 105.6f * f.u)
                    quadraticTo(60.2f * f.u, 106.8f * f.u, 55.2f * f.u, 105.6f * f.u)
                    close()
                }
                drawPath(leftShoe, shoeMain)
                drawPath(rightShoe, shoeMain)
                drawPath(Path().apply {
                    moveTo(35.6f * f.u, 100.8f * f.u)
                    lineTo(44f * f.u, 100.8f * f.u)
                    lineTo(43.6f * f.u, 104.4f * f.u)
                    lineTo(36f * f.u, 104.4f * f.u)
                    close()
                }, shoeLight)
                drawPath(Path().apply {
                    moveTo(56f * f.u, 100.8f * f.u)
                    lineTo(64.4f * f.u, 100.8f * f.u)
                    lineTo(64f * f.u, 104.4f * f.u)
                    lineTo(56.4f * f.u, 104.4f * f.u)
                    close()
                }, shoeLight)
                drawRect(shoeDark, topLeft = f.p(34.4f, 105.6f), size = f.s(10.8f, 1.4f))
                drawRect(shoeDark, topLeft = f.p(54.8f, 105.6f), size = f.s(10.8f, 1.4f))
                if (armor.tier >= 3) {
                    val rune = pal.glow ?: pal.dark
                    drawCircle(rune.copy(alpha = 0.75f), radius = 6f * f.u, center = f.p(50f, 60f), style = Stroke(width = 1.2f * f.u))
                    drawCircle(rune, radius = 1.8f * f.u, center = f.p(50f, 60f))
                    for (y in listOf(72f, 80f, 88f)) {
                        drawCircle(rune, radius = 1.3f * f.u, center = f.p(50f, y))
                    }
                }
                pal.glow?.let { g ->
                    drawPath(backRobe, g.copy(alpha = 0.8f), style = Stroke(width = 1.3f * f.u))
                }
            }
        }
        ItemStyle.LIGHT -> {
            if (!f.facingBack) {
                // Front: Fitted leather vest with belt, straps, and laces
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
            } else {
                // Back: Midnight leather panels matching the authored front asset.
                drawRoundRect(pal.main, topLeft = f.p(34f, 48f), size = f.s(32f, 28f), cornerRadius = CornerRadius(6f * f.u))
                drawRect(pal.dark, topLeft = f.p(36f, 50f), size = f.s(11f, 16f))
                drawRect(pal.dark, topLeft = f.p(53f, 50f), size = f.s(11f, 16f))
                drawRect(pal.dark, topLeft = f.p(49f, 49f), size = f.s(2f, 18f))
                // The quiver supplies the only diagonal rear strap.
                drawRect(pal.dark, topLeft = f.p(34f, 70f), size = f.s(32f, 5f))
                drawRoundRect(pal.main, topLeft = f.p(46.5f, 69f), size = f.s(7f, 6.5f), cornerRadius = CornerRadius(1.5f * f.u))
                drawRect(pal.dark, topLeft = f.p(48.5f, 71.5f), size = f.s(3f, 2f))
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
        }
        else -> {
            if (!f.facingBack) {
                // Front: Full plate cuirass
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
            } else {
                // Back: tapered articulated cuirass matching the front plate language.
                val backPlate = Path().apply {
                    moveTo(37f * f.u, 48f * f.u)
                    lineTo(63f * f.u, 48f * f.u)
                    lineTo(66f * f.u, 55f * f.u)
                    lineTo(61f * f.u, 72f * f.u)
                    lineTo(39f * f.u, 72f * f.u)
                    lineTo(34f * f.u, 55f * f.u)
                    close()
                }
                drawPath(backPlate, pal.main)

                val leftPlate = Path().apply {
                    moveTo(37f * f.u, 50f * f.u)
                    lineTo(48f * f.u, 50f * f.u)
                    lineTo(47f * f.u, 63f * f.u)
                    lineTo(38f * f.u, 61f * f.u)
                    close()
                }
                val rightPlate = Path().apply {
                    moveTo(52f * f.u, 50f * f.u)
                    lineTo(63f * f.u, 50f * f.u)
                    lineTo(62f * f.u, 61f * f.u)
                    lineTo(53f * f.u, 63f * f.u)
                    close()
                }
                drawPath(leftPlate, pal.dark.copy(alpha = 0.72f))
                drawPath(rightPlate, pal.dark.copy(alpha = 0.72f))
                drawRect(pal.dark, topLeft = f.p(49f, 49f), size = f.s(2f, 18f))
                drawRect(pal.dark, topLeft = f.p(38f, 64f), size = f.s(24f, 2.5f))
                drawRect(pal.dark, topLeft = f.p(39f, 68f), size = f.s(22f, 3.5f))
                for (x in listOf(39f, 61f)) {
                    drawCircle(pal.dark, radius = 1.1f * f.u, center = f.p(x, 53f))
                    drawCircle(pal.dark, radius = 1.1f * f.u, center = f.p(x, 61f))
                }
                if (armor.tier >= 3) {
                    val sigil = Path().apply {
                        moveTo(50f * f.u, 54f * f.u)
                        lineTo(53f * f.u, 58f * f.u)
                        lineTo(50f * f.u, 62f * f.u)
                        lineTo(47f * f.u, 58f * f.u)
                        close()
                    }
                    drawPath(sigil, pal.glow ?: pal.dark)
                }
            }
        }
    }
    if (archetype == com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARMOR_PAULDRONS) {
        drawRoundRect(pal.dark, topLeft = f.p(25f, 45f), size = f.s(17f, 10f), cornerRadius = CornerRadius(4f * f.u))
        drawRoundRect(pal.main, topLeft = f.p(27f, 46f), size = f.s(14f, 7f), cornerRadius = CornerRadius(3f * f.u))
        drawRoundRect(pal.dark, topLeft = f.p(58f, 45f), size = f.s(17f, 10f), cornerRadius = CornerRadius(4f * f.u))
        drawRoundRect(pal.main, topLeft = f.p(59f, 46f), size = f.s(14f, 7f), cornerRadius = CornerRadius(3f * f.u))
    }
    drawTorsoSignatureDetails(f, armor, pal)
}

private fun DrawScope.drawMailArmor(f: AvatarFrame, item: ItemEntity, pal: GearPalette) {
    val mail = Path().apply {
        moveTo(34f * f.u, 48f * f.u)
        lineTo(66f * f.u, 48f * f.u)
        lineTo(67f * f.u, 76f * f.u)
        lineTo(33f * f.u, 76f * f.u)
        close()
    }
    drawPath(mail, pal.main)
    drawRect(pal.dark, topLeft = f.p(33f, 72f), size = f.s(34f, 4f))
    val ringColor = (pal.glow ?: pal.dark).copy(alpha = 0.72f)
    val xOffset = if (f.facingBack) 1.5f else 0f
    for (row in 0..4) {
        for (column in 0..5) {
            val x = 37f + column * 5.2f + if (row % 2 == 0) xOffset else 2.6f - xOffset
            val y = 52f + row * 4f
            drawCircle(ringColor, radius = 1.25f * f.u, center = f.p(x, y), style = Stroke(width = 0.65f * f.u))
        }
    }
    if (!f.facingBack) {
        drawLine(pal.dark, f.p(50f, 49f), f.p(50f, 72f), 1.2f * f.u)
    }
    if (item.tier >= 3) drawRect(GearVisuals.accent(item), topLeft = f.p(34f, 48f), size = f.s(32f, 1.2f))
}

private fun DrawScope.drawChestCloak(f: AvatarFrame, item: ItemEntity, pal: GearPalette) {
    val accent = GearVisuals.accent(item)
    if (f.facingBack) {
        val cloak = Path().apply {
            moveTo(34f * f.u, 48f * f.u)
            quadraticTo(50f * f.u, 43f * f.u, 66f * f.u, 48f * f.u)
            lineTo(71f * f.u, 94f * f.u)
            quadraticTo(50f * f.u, 99f * f.u, 29f * f.u, 94f * f.u)
            close()
        }
        drawPath(cloak, pal.main)
        drawLine(pal.dark, f.p(50f, 49f), f.p(50f, 95f), 1.5f * f.u)
        drawLine(pal.dark.copy(alpha = 0.65f), f.p(40f, 52f), f.p(37f, 92f), 1f * f.u)
        drawLine(pal.dark.copy(alpha = 0.65f), f.p(60f, 52f), f.p(63f, 92f), 1f * f.u)
        if (item.tier >= 3) drawRect(accent, topLeft = f.p(30f, 92f), size = f.s(40f, 2f))
    } else {
        drawRoundRect(pal.dark, topLeft = f.p(33f, 48f), size = f.s(34f, 30f), cornerRadius = CornerRadius(5f * f.u))
        drawRoundRect(pal.main, topLeft = f.p(35f, 49f), size = f.s(30f, 27f), cornerRadius = CornerRadius(4f * f.u))
        val mantle = Path().apply {
            moveTo(29f * f.u, 49f * f.u)
            quadraticTo(50f * f.u, 43f * f.u, 71f * f.u, 49f * f.u)
            lineTo(64f * f.u, 57f * f.u)
            quadraticTo(50f * f.u, 52f * f.u, 36f * f.u, 57f * f.u)
            close()
        }
        drawPath(mantle, pal.main)
        drawPath(mantle, pal.dark, style = Stroke(width = 1f * f.u))
        drawCircle(accent, radius = 2f * f.u, center = f.p(50f, 53f))
    }
}

private fun DrawScope.drawTorsoSignatureDetails(f: AvatarFrame, item: ItemEntity, pal: GearPalette) {
    val signature = GearVisuals.signature(item)
    val accent = GearVisuals.accent(item)
    val top = if (item.style == ItemStyle.ROBE) 55f else 51f
    val bottom = if (item.style == ItemStyle.ROBE) 91f else 68f

    when (signature.trim) {
        ProceduralTrim.PLAIN -> Unit
        ProceduralTrim.BAND -> drawRect(accent.copy(alpha = 0.9f), topLeft = f.p(36f, top + 5f), size = f.s(28f, 1.3f))
        ProceduralTrim.SPLIT -> drawRect(accent.copy(alpha = 0.85f), topLeft = f.p(49.2f, top), size = f.s(1.6f, bottom - top))
        ProceduralTrim.CHEVRON -> {
            drawLine(accent, f.p(38f, top + 2f), f.p(50f, top + 8f), 1.2f * f.u)
            drawLine(accent, f.p(50f, top + 8f), f.p(62f, top + 2f), 1.2f * f.u)
        }
        ProceduralTrim.RUNIC -> {
            val marks = signature.detailCount.coerceIn(2, 5)
            repeat(marks) { index ->
                val y = top + 3f + index * ((bottom - top - 6f) / marks)
                drawLine(accent, f.p(47.5f, y), f.p(52.5f, y), 1f * f.u)
            }
        }
    }

    when (signature.ornament) {
        ProceduralOrnament.NONE -> Unit
        ProceduralOrnament.STUDS -> for (x in listOf(39f, 61f)) {
            drawCircle(accent, radius = 0.9f * f.u, center = f.p(x, top + 2f))
        }
        ProceduralOrnament.GEM -> {
            val gem = Path().apply {
                moveTo(50f * f.u, (top + 2f) * f.u)
                lineTo(53f * f.u, (top + 5f) * f.u)
                lineTo(50f * f.u, (top + 8f) * f.u)
                lineTo(47f * f.u, (top + 5f) * f.u)
                close()
            }
            drawPath(gem, accent)
        }
        ProceduralOrnament.RUNES -> for (x in listOf(42f, 50f, 58f)) {
            drawCircle(accent, radius = 1f * f.u, center = f.p(x, top + 5f), style = Stroke(width = 0.8f * f.u))
        }
        ProceduralOrnament.SPIKES -> for (x in listOf(36f, 64f)) {
            val direction = if (x < 50f) -1f else 1f
            val spike = Path().apply {
                moveTo(x * f.u, (top + 2f) * f.u)
                lineTo((x + direction * 4f) * f.u, (top - 2f) * f.u)
                lineTo((x + direction) * f.u, (top + 6f) * f.u)
                close()
            }
            drawPath(spike, accent)
        }
        ProceduralOrnament.CREST -> {
            drawCircle(accent.copy(alpha = 0.28f), radius = 4f * f.u, center = f.p(50f, top + 6f))
            drawCircle(accent, radius = 2f * f.u, center = f.p(50f, top + 6f))
        }
    }

    when (signature.finish) {
        ProceduralMaterialFinish.WORN -> {
            drawLine(pal.dark.copy(alpha = 0.6f), f.p(39f, bottom - 3f), f.p(44f, bottom - 5f), 0.8f * f.u)
            drawLine(pal.dark.copy(alpha = 0.6f), f.p(58f, bottom - 6f), f.p(62f, bottom - 4f), 0.8f * f.u)
        }
        ProceduralMaterialFinish.CRAFTED -> Unit
        ProceduralMaterialFinish.REFINED -> drawLine(
            accent.copy(alpha = 0.5f), f.p(36f, top), f.p(64f, top), 0.8f * f.u
        )
        ProceduralMaterialFinish.ASCENDANT -> drawRoundRect(
            (pal.glow ?: accent).copy(alpha = 0.45f), topLeft = f.p(34f, 48f),
            size = f.s(32f, if (item.style == ItemStyle.ROBE) 49f else 27f),
            cornerRadius = CornerRadius(5f * f.u), style = Stroke(width = 1.1f * f.u)
        )
    }
}

internal object ProceduralBackWearableMotion {
    fun computeCapeSway(phase: Float, u: Float = 1f): Float =
        kotlin.math.sin(phase * kotlin.math.PI.toFloat() * 2f) * 3.5f * u

    fun computeWingFlap(phase: Float, u: Float = 1f): Float =
        kotlin.math.sin(phase * kotlin.math.PI.toFloat() * 2f) * 5f * u

    fun computeQuiverMotion(phase: Float, u: Float = 1f): Float = 0f

    fun computeShieldMotion(phase: Float, u: Float = 1f): Float = 0f
}

internal fun DrawScope.drawTrinketLayer(f: AvatarFrame) {
    val item = f.trinket ?: return
    val nameLower = item.name.lowercase()
    val isCape = item.style == "cape" || nameLower.contains("cape") || nameLower.contains("cloak")
    val isQuiver = nameLower.contains("quiver")
    val isWings = nameLower.contains("wing")
    val isShield = nameLower.contains("shield")

    if (f.facingBack) {
        when {
            isCape -> {
                val capePal = when {
                    nameLower.contains("velvet") || nameLower.contains("purple") -> GearPalette(Color(0xFF6B459E), Color(0xFF452B6B))
                    nameLower.contains("emerald") || nameLower.contains("green") -> GearPalette(Color(0xFF2E7A50), Color(0xFF1B4D32))
                    nameLower.contains("crimson") || nameLower.contains("red") -> GearPalette(Color(0xFF9E2A2B), Color(0xFF6B1D1E))
                    nameLower.contains("royal") || nameLower.contains("blue") -> GearPalette(Color(0xFF3450A8), Color(0xFF233670))
                    nameLower.contains("shadow") || nameLower.contains("black") -> GearPalette(Color(0xFF2C243B), Color(0xFF1B1624))
                    else -> when (item.tier) {
                        1 -> GearPalette(Color(0xFF8B3A3A), Color(0xFF5A2222))
                        2 -> GearPalette(Color(0xFF3E5A78), Color(0xFF283A4E))
                        3 -> GearPalette(Color(0xFF6B459E), Color(0xFF452B6B))
                        else -> GearPalette(Color(0xFF8A5CE8), Color(0xFF5C36B0), Color(0xFFF8D24A))
                    }
                }
                val sway = ProceduralBackWearableMotion.computeCapeSway(f.phase, f.u)
                val cape = Path().apply {
                    moveTo(39f * f.u, 49f * f.u)
                    lineTo(61f * f.u, 49f * f.u)
                    lineTo((67f * f.u) + sway, 96f * f.u)
                    lineTo((33f * f.u) + sway, 96f * f.u)
                    close()
                }
                drawPath(cape, capePal.main)
                drawLine(capePal.dark, start = f.p(43f, 51f), end = Offset((40f * f.u) + sway, 94f * f.u), strokeWidth = 1.5f * f.u)
                drawLine(capePal.dark, start = f.p(57f, 51f), end = Offset((60f * f.u) + sway, 94f * f.u), strokeWidth = 1.5f * f.u)
                drawLine(capePal.dark, start = f.p(50f, 50f), end = Offset((50f * f.u) + sway, 95f * f.u), strokeWidth = 1.3f * f.u)
                drawRect(Color(0xFFD4AF37), topLeft = Offset((33f * f.u) + sway, 94f * f.u), size = f.s(34f, 2.5f))
            }
            isQuiver -> {
                val leather = Color(0xFF6B4423)
                val leatherDark = Color(0xFF452A15)
                val quiver = Path().apply {
                    moveTo(44f * f.u, 72f * f.u)
                    lineTo(50f * f.u, 74f * f.u)
                    lineTo(64f * f.u, 46f * f.u)
                    lineTo(58f * f.u, 44f * f.u)
                    close()
                }
                drawPath(quiver, leather)
                drawPath(quiver, leatherDark, style = Stroke(width = 1.2f * f.u))
                drawLine(leatherDark, start = f.p(38f, 52f), end = f.p(62f, 70f), strokeWidth = 2f * f.u)
                for ((dx, dy) in listOf(0f to 0f, 3f to -2f, 6f to 1f)) {
                    drawLine(Color(0xFFB89758), start = f.p(60f + dx, 46f + dy), end = f.p(66f + dx, 36f + dy), strokeWidth = 1.2f * f.u)
                    drawCircle(Color(0xFFE85A5A), radius = 1.6f * f.u, center = f.p(66f + dx, 36f + dy))
                }
            }
            isWings -> {
                val wingPal = when {
                    nameLower.contains("celestial") || nameLower.contains("seraph") || item.tier >= 4 ->
                        GearPalette(Color(0xFFFFFAEB), Color(0xFFE0D5B5), Color(0xFFFFD54F))
                    nameLower.contains("shadow") || nameLower.contains("void") ->
                        GearPalette(Color(0xFF382A4D), Color(0xFF221A30), Color(0xFF9C7BE3))
                    else -> GearPalette(Color(0xFFE8EEF5), Color(0xFFBAC8D8), Color(0xFF90CAF9))
                }
                val flap = ProceduralBackWearableMotion.computeWingFlap(f.phase, f.u)

                // Left Wing: articulated feathers anchored at left shoulder blade
                val leftSpar = Path().apply {
                    moveTo(42f * f.u, 52f * f.u)
                    quadraticTo(24f * f.u, (34f * f.u) - flap, 16f * f.u, (42f * f.u) - flap)
                    lineTo(22f * f.u, (52f * f.u) - flap * 0.5f)
                    quadraticTo(32f * f.u, 58f * f.u, 40f * f.u, 60f * f.u)
                    close()
                }
                val rightSpar = Path().apply {
                    moveTo(58f * f.u, 52f * f.u)
                    quadraticTo(76f * f.u, (34f * f.u) - flap, 84f * f.u, (42f * f.u) - flap)
                    lineTo(78f * f.u, (52f * f.u) - flap * 0.5f)
                    quadraticTo(68f * f.u, 58f * f.u, 60f * f.u, 60f * f.u)
                    close()
                }
                drawPath(leftSpar, wingPal.main.copy(alpha = 0.9f))
                drawPath(rightSpar, wingPal.main.copy(alpha = 0.9f))
                drawPath(leftSpar, wingPal.dark, style = Stroke(width = 1.3f * f.u))
                drawPath(rightSpar, wingPal.dark, style = Stroke(width = 1.3f * f.u))

                // Feather vanes
                for (step in 1..3) {
                    val spread = step * 4f * f.u
                    drawLine(wingPal.dark.copy(alpha = 0.7f), start = Offset((40f * f.u) - spread * 0.5f, 55f * f.u), end = Offset((20f * f.u) + spread, (44f * f.u) - flap + spread * 0.4f), strokeWidth = 1f * f.u)
                    drawLine(wingPal.dark.copy(alpha = 0.7f), start = Offset((60f * f.u) + spread * 0.5f, 55f * f.u), end = Offset((80f * f.u) - spread, (44f * f.u) - flap + spread * 0.4f), strokeWidth = 1f * f.u)
                }
            }
            isShield -> {
                val shieldMain = Color(0xFF8A9BA8)
                val shieldDark = Color(0xFF5A6B78)
                val shieldGold = Color(0xFFD4AF37)
                // Compact heater shield strapped across left back: keeps torso, arms, and waist fully readable
                drawRoundRect(shieldDark, topLeft = f.p(41f, 50f), size = f.s(18f, 22f), cornerRadius = CornerRadius(4f * f.u))
                drawRoundRect(shieldMain, topLeft = f.p(43f, 52f), size = f.s(14f, 18f), cornerRadius = CornerRadius(3f * f.u))
                drawCircle(shieldGold, radius = 3.2f * f.u, center = f.p(50f, 61f))
            }
            else -> {
                // Generic back wearable fallback
                val tc = GearVisuals.trinketColor(item.id)
                drawCircle(tc.copy(alpha = 0.35f), radius = 6.5f * f.u, center = f.p(50f, 58f))
                drawCircle(tc, radius = 4f * f.u, center = f.p(50f, 58f))
            }
        }
    } else {
        if (isCape) {
            val capePal = when {
                nameLower.contains("velvet") || nameLower.contains("purple") -> GearPalette(Color(0xFF6B459E), Color(0xFF452B6B))
                nameLower.contains("emerald") || nameLower.contains("green") -> GearPalette(Color(0xFF2E7A50), Color(0xFF1B4D32))
                nameLower.contains("crimson") || nameLower.contains("red") -> GearPalette(Color(0xFF9E2A2B), Color(0xFF6B1D1E))
                nameLower.contains("royal") || nameLower.contains("blue") -> GearPalette(Color(0xFF3450A8), Color(0xFF233670))
                nameLower.contains("shadow") || nameLower.contains("black") -> GearPalette(Color(0xFF2C243B), Color(0xFF1B1624))
                else -> when (item.tier) {
                    1 -> GearPalette(Color(0xFF8B3A3A), Color(0xFF5A2222))
                    2 -> GearPalette(Color(0xFF3E5A78), Color(0xFF283A4E))
                    3 -> GearPalette(Color(0xFF6B459E), Color(0xFF452B6B))
                    else -> GearPalette(Color(0xFF8A5CE8), Color(0xFF5C36B0), Color(0xFFF8D24A))
                }
            }
            drawCircle(Color(0xFFD4AF37), radius = 2.5f * f.u, center = f.p(38f, 49f))
            drawCircle(Color(0xFFD4AF37), radius = 2.5f * f.u, center = f.p(62f, 49f))
            drawLine(Color(0xFFD4AF37), start = f.p(38f, 49f), end = f.p(62f, 49f), strokeWidth = 1.4f * f.u)
            drawRect(capePal.main, topLeft = f.p(35f, 49f), size = f.s(3f, 8f))
            drawRect(capePal.main, topLeft = f.p(62f, 49f), size = f.s(3f, 8f))
        } else {
            if (isShield) {
                val shieldMain = Color(0xFF8A9BA8)
                val shieldDark = Color(0xFF5A6B78)
                val shieldGold = Color(0xFFD4AF37)
                drawRoundRect(shieldDark, topLeft = f.p(24f, 46f), size = f.s(20f, 26f), cornerRadius = CornerRadius(4f * f.u))
                drawRoundRect(shieldMain, topLeft = f.p(26f, 48f), size = f.s(16f, 22f), cornerRadius = CornerRadius(3f * f.u))
                drawCircle(shieldGold, radius = 3f * f.u, center = f.p(34f, 59f))
            } else if (WearArchetype.resolve(item) == WearArchetype.RING) {
                val tc = GearVisuals.trinketColor(item.id)
                drawOval(tc, topLeft = f.p(62f, 70f), size = f.s(10f, 8f), style = Stroke(width = 2f * f.u))
            } else if (WearArchetype.resolve(item) == WearArchetype.AMULET) {
                val tc = GearVisuals.trinketColor(item.id)
                drawCircle(tc, radius = 3.5f * f.u, center = f.p(50f, 47f))
                drawLine(tc, f.p(50f, 50f), f.p(50f, 54f), 1.5f * f.u)
            } else {
                val tc = GearVisuals.trinketColor(item.id)
                drawCircle(tc.copy(alpha = 0.35f), radius = 6.5f * f.u, center = f.p(50f, 51f))
                drawCircle(tc, radius = 4f * f.u, center = f.p(50f, 51f))
                drawCircle(Color.White.copy(alpha = 0.7f), radius = 1.5f * f.u, center = f.p(48.8f, 49.8f))
            }
        }
    }
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

internal fun DrawScope.drawHeadBase(f: AvatarFrame) {
    if (f.costume) {
        drawClassCostumeHead(f)
        return
    }
    drawRaceEars(f) // behind the skull so elf tips read clearly
    drawCircle(f.skinColor("head", "face"), radius = 14f * f.u, center = f.p(50f, 30f))
    val hideAllHair = BodyRegion.SCALP in f.hiddenRegions && BodyRegion.FOREHEAD in f.hiddenRegions
    if (!hideAllHair) {
        val isHat = BodyRegion.FOREHEAD in f.hiddenRegions || BodyRegion.SCALP in f.hiddenRegions || f.head != null
        drawHair(f, f.appearance.hairStyle, isHat = isHat)
    }
    if (!f.facingBack) {
        val isHelm = f.head?.let { GearVisuals.headgearShape(it) == HeadgearShape.HELM } == true
        if (!isHelm) {
            eyes(f, 44f, 32f, 56f)
            drawRaceFaceAccents(f)
        } else {
            drawRaceFaceAccents(f, helmCovered = true)
        }
    }
}

internal fun DrawScope.drawHeadgearOnly(f: AvatarFrame) {
    if (f.costume) return // Costume headgear is drawn completely in drawClassCostumeHead inside drawHeadBase
    val item = f.head ?: return
    val pal = GearVisuals.palette(item)
    when (GearVisuals.headgearShape(item)) {
        HeadgearShape.HELM -> {
            if (!f.facingBack) {
                drawArc(pal.main, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = f.p(34.5f, 14.5f), size = f.s(31f, 31f))
                drawRect(pal.dark, topLeft = f.p(34.5f, 26f), size = f.s(31f, 3.5f))
                drawRoundRect(pal.main, topLeft = f.p(35f, 28f), size = f.s(7f, 12f), cornerRadius = CornerRadius(2f * f.u))
                drawRoundRect(pal.main, topLeft = f.p(58f, 28f), size = f.s(7f, 12f), cornerRadius = CornerRadius(2f * f.u))
                drawRoundRect(VisorSlit, topLeft = f.p(42f, 30f), size = f.s(16f, 4f), cornerRadius = CornerRadius(2f * f.u))
                if (item.tier >= 3) {
                    drawRoundRect(pal.glow ?: f.look.outfit, topLeft = f.p(46f, 5f), size = f.s(8f, 11f), cornerRadius = CornerRadius(3f * f.u))
                }
            } else {
                drawArc(pal.main, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = f.p(34.5f, 14.5f), size = f.s(31f, 31f))
                drawRoundRect(pal.main, topLeft = f.p(34f, 26f), size = f.s(32f, 11f), cornerRadius = CornerRadius(3f * f.u))
                drawRect(pal.dark, topLeft = f.p(34f, 26f), size = f.s(32f, 3f))
                drawRect(pal.dark, topLeft = f.p(48.5f, 16f), size = f.s(3f, 21f))
                for (x in listOf(38f, 43f, 57f, 62f)) {
                    drawCircle(pal.dark, radius = 1f * f.u, center = f.p(x, 34f))
                }
                if (item.tier >= 3) {
                    drawRoundRect(pal.glow ?: f.look.outfit, topLeft = f.p(46f, 5f), size = f.s(8f, 11f), cornerRadius = CornerRadius(3f * f.u))
                }
            }
        }
        HeadgearShape.HAT -> {
            val cone = Path().apply {
                moveTo(33f * f.u, 20f * f.u)
                lineTo(67f * f.u, 20f * f.u)
                lineTo(54f * f.u, 0f * f.u)
                close()
            }
            drawPath(cone, pal.main)
            drawOval(pal.main, topLeft = f.p(29f, 16f), size = f.s(42f, 9f))
            drawRect(pal.glow ?: pal.dark, topLeft = f.p(40f, 17.5f), size = f.s(20f, 3f))
            if (f.facingBack) {
                drawRect(pal.dark, topLeft = f.p(48.5f, 16.5f), size = f.s(3f, 5f))
            }
            pal.glow?.let { g ->
                drawLine(g, start = f.p(56f, 2f), end = f.p(60f, 6f), strokeWidth = 1.2f * f.u)
                drawLine(g, start = f.p(60f, 2f), end = f.p(56f, 6f), strokeWidth = 1.2f * f.u)
            }
        }
        HeadgearShape.HOOD -> {
            if (!f.facingBack) {
                // Front: Outer hood cowl framing the face - leaves eyes, expression, and skin tone fully visible
                val point = Path().apply {
                    moveTo(42f * f.u, 16f * f.u)
                    lineTo(58f * f.u, 16f * f.u)
                    lineTo(50f * f.u, 6f * f.u)
                    close()
                }
                drawPath(point, pal.main)
                // Outer cowl framing head
                drawArc(
                    pal.main,
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = f.p(33.5f, 13.5f),
                    size = f.s(33f, 33f),
                    style = Stroke(width = 4.5f * f.u)
                )
                drawArc(
                    pal.dark,
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = f.p(35.5f, 15.5f),
                    size = f.s(29f, 29f),
                    style = Stroke(width = 1.8f * f.u)
                )
                // Lower cowl wrap beneath chin and across collar
                drawRoundRect(pal.main, topLeft = f.p(38f, 38f), size = f.s(24f, 7f), cornerRadius = CornerRadius(3f * f.u))
                drawRect(pal.dark, topLeft = f.p(36f, 44f), size = f.s(28f, 3f))
                pal.glow?.let { g ->
                    drawArc(
                        g.copy(alpha = 0.8f),
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft = f.p(32f, 12f),
                        size = f.s(36f, 36f),
                        style = Stroke(width = 1.3f * f.u)
                    )
                }
            } else {
                // Back: fitted tapered hood rather than an oversized oval sack.
                val backCowl = Path().apply {
                    moveTo(39f * f.u, 31f * f.u)
                    quadraticTo(36f * f.u, 20f * f.u, 44f * f.u, 14f * f.u)
                    lineTo(47f * f.u, 9f * f.u)
                    lineTo(53f * f.u, 9f * f.u)
                    lineTo(56f * f.u, 14f * f.u)
                    quadraticTo(64f * f.u, 20f * f.u, 61f * f.u, 31f * f.u)
                    lineTo(58f * f.u, 42f * f.u)
                    lineTo(42f * f.u, 42f * f.u)
                    close()
                }
                drawPath(backCowl, pal.main)
                drawLine(pal.dark, start = f.p(50f, 10f), end = f.p(50f, 41f), strokeWidth = 1.5f * f.u)
                drawLine(pal.dark.copy(alpha = 0.75f), start = f.p(42f, 34f), end = f.p(58f, 34f), strokeWidth = 1.2f * f.u)
                pal.glow?.let { g ->
                    drawPath(backCowl, g.copy(alpha = 0.6f), style = Stroke(width = 1.2f * f.u))
                }
            }
        }
        HeadgearShape.CROWN -> {
            val crown = Path().apply {
                moveTo(36f * f.u, 27f * f.u)
                lineTo(36f * f.u, 19f * f.u)
                lineTo(43f * f.u, 23f * f.u)
                lineTo(50f * f.u, 14f * f.u)
                lineTo(57f * f.u, 23f * f.u)
                lineTo(64f * f.u, 19f * f.u)
                lineTo(64f * f.u, 27f * f.u)
                close()
            }
            drawPath(crown, pal.main)
            drawRect(pal.dark, topLeft = f.p(36f, 25f), size = f.s(28f, 3f))
            if (!f.facingBack) {
                drawCircle(pal.glow ?: GearVisuals.accent(item), radius = 1.8f * f.u, center = f.p(50f, 23f))
            } else {
                drawLine(pal.dark, f.p(50f, 16f), f.p(50f, 26f), 1f * f.u)
            }
        }
        HeadgearShape.CAP -> {
            if (!f.facingBack) {
                drawOval(pal.main, topLeft = f.p(38f, 15f), size = f.s(24f, 9f))
                drawRect(pal.dark, topLeft = f.p(35f, 21f), size = f.s(30f, 3.5f))
            } else {
                drawOval(pal.main, topLeft = f.p(38f, 15f), size = f.s(24f, 9f))
                drawRect(pal.dark, topLeft = f.p(38f, 21f), size = f.s(24f, 3.5f))
                drawArc(f.skinColor("head"), startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = f.p(46f, 18f), size = f.s(8f, 6f))
                drawRect(pal.dark, topLeft = f.p(45f, 22f), size = f.s(10f, 2f))
            }
        }
    }
    drawHeadgearSignatureDetails(f, item, pal)
}

private fun DrawScope.drawHeadgearSignatureDetails(f: AvatarFrame, item: ItemEntity, pal: GearPalette) {
    val signature = GearVisuals.signature(item)
    val accent = GearVisuals.accent(item)
    when (signature.silhouette) {
        ProceduralSilhouette.BALANCED -> Unit
        ProceduralSilhouette.ANGULAR -> {
            val ridge = Path().apply {
                moveTo(46f * f.u, 16f * f.u)
                lineTo(50f * f.u, 9f * f.u)
                lineTo(54f * f.u, 16f * f.u)
                close()
            }
            drawPath(ridge, pal.dark)
        }
        ProceduralSilhouette.CURVED -> drawArc(
            accent.copy(alpha = 0.85f), 190f, 160f, false,
            topLeft = f.p(38f, 15f), size = f.s(24f, 14f), style = Stroke(width = 1.2f * f.u)
        )
        ProceduralSilhouette.FORTIFIED -> {
            drawCircle(pal.dark, radius = 2f * f.u, center = f.p(36f, 27f))
            drawCircle(pal.dark, radius = 2f * f.u, center = f.p(64f, 27f))
        }
    }
    when (signature.ornament) {
        ProceduralOrnament.NONE -> Unit
        ProceduralOrnament.STUDS -> for (x in listOf(42f, 50f, 58f)) drawCircle(accent, 0.8f * f.u, f.p(x, 25f))
        ProceduralOrnament.GEM -> drawCircle(accent, 1.8f * f.u, f.p(50f, 23f))
        ProceduralOrnament.RUNES -> for (x in listOf(44f, 50f, 56f)) {
            drawLine(accent, f.p(x - 1f, 24f), f.p(x + 1f, 21f), 0.8f * f.u)
        }
        ProceduralOrnament.SPIKES -> for (x in listOf(39f, 61f)) {
            drawLine(accent, f.p(x, 20f), f.p(x + if (x < 50f) -3f else 3f, 15f), 2f * f.u)
        }
        ProceduralOrnament.CREST -> drawRoundRect(
            accent, topLeft = f.p(47.5f, 8f), size = f.s(5f, 9f), cornerRadius = CornerRadius(2f * f.u)
        )
    }
}

private fun DrawScope.drawHeadLayer(f: AvatarFrame) {
    drawHeadBase(f)
    drawHeadgearOnly(f)
}

// ---- Weapon + hands ----

internal fun DrawScope.drawWeaponLayer(f: AvatarFrame, pose: AvatarPose) {
    if (f.facingBack) {
        val rearWeapon = f.weapon
        val hasFeaturedBackItem = f.gear.values.any {
            com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveSpec(it).visualSlot ==
                com.fitnessquest.rpg.domain.visuals.PaperDollVisualSlot.BACK
        }
        val rearPresentation = rearWeapon?.let {
            com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolvePresentation(
                item = it,
                orientation = com.fitnessquest.rpg.domain.visuals.AvatarOrientation.BACK,
                hasFeaturedBackItem = hasFeaturedBackItem
            )
        }

        if (rearPresentation?.attachment == com.fitnessquest.rpg.domain.visuals.VisualAttachmentMode.HIDDEN) {
            Unit
        } else if (
            rearWeapon != null &&
            rearPresentation?.attachment == com.fitnessquest.rpg.domain.visuals.VisualAttachmentMode.HELD
        ) {
            val handX = rearPresentation.anchor.xFraction * 100f
            val rearHand = f.hands?.let { GearVisuals.palette(it).dark }
                ?: f.skinShade("forearms", "hands", "arms")
            withTransform({
                translate(pose.weaponOffset.x, pose.weaponOffset.y)
                translate(pose.rightArmOffset.x, pose.rightArmOffset.y)
                rotate(pose.rightArmRotation, pivot = f.p(71f, 46f))
            }) {
                // A featured cape, wing set, shield, or quiver owns the back.
                // Keep the equipped weapon in hand so it remains identifiable
                // without crossing or hiding the showcased back item.
                drawWeapon(f, rearWeapon)
                drawCircle(rearHand, radius = 5f * f.u, center = f.p(handX, 78f))
            }
        } else {
            rearWeapon?.let { drawSlungBackWeapon(f, it, presentation = rearPresentation) }
                ?: run {
                if (f.costume && f.cls == CharacterClass.NECROMANCER) {
                    drawSlungBackWeapon(f, ItemEntity(id = 999, name = "Bone Staff", slot = ItemSlot.WEAPON, style = ItemStyle.STAFF, classAffinity = CharacterClass.NECROMANCER, tier = 3, emoji = "🪄", price = 250))
                }
            }
        }
        return
    }

    // Front View: Weapons gripped in hands in foreground
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

private fun DrawScope.drawSlungBackWeapon(
    f: AvatarFrame,
    weapon: ItemEntity,
    knightHandX: Float = 71.5f,
    presentation: com.fitnessquest.rpg.domain.visuals.OrientationPresentation? = null,
) {
    val descriptor = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(weapon)
    val backPresentation = presentation ?: descriptor.orientationContract.back
    val anchorX = backPresentation.anchor.xFraction * 100f
    val anchorY = backPresentation.anchor.yFraction * 120f
    val leatherStrap = Color(0xFF4A3525)
    val signature = descriptor.proceduralSignature
    val proceduralAccent = descriptor.accentColor ?: GearVisuals.accent(weapon)
    val silhouetteWidth = when (signature.silhouette) {
        ProceduralSilhouette.BALANCED -> 0f
        ProceduralSilhouette.ANGULAR -> 0.4f
        ProceduralSilhouette.CURVED -> -0.3f
        ProceduralSilhouette.FORTIFIED -> 0.9f
    }

    when (descriptor.archetype) {
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.TONFA,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.INSTRUMENT,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.AXE,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SCYTHE,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.HAMMER,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.CLUB,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SCEPTER,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARCANE_FOCUS -> {
            withTransform({ rotate(backPresentation.anchor.rotationDegrees, pivot = f.p(anchorX, anchorY)) }) {
                drawSemanticWeapon(f, weapon, descriptor, handX = anchorX)
            }
            if (descriptor.archetype != com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARCANE_FOCUS) {
                drawLine(leatherStrap, f.p(anchorX - 6f, anchorY - 3f), f.p(anchorX + 5f, anchorY + 3f), 2f * f.u)
            }
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SPEAR_GENERIC -> {
            withTransform({ rotate(backPresentation.anchor.rotationDegrees, pivot = f.p(anchorX, anchorY)) }) {
                drawProceduralSpear(f, weapon, descriptor, handX = anchorX)
            }
            drawLine(
                leatherStrap,
                start = f.p(anchorX - 7f, anchorY - 4f),
                end = f.p(anchorX + 5f, anchorY + 3f),
                strokeWidth = 2f * f.u
            )
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.GREATSWORD_DRAGON -> {
            // Exact 1:5 reconstruction of the authored Dragonfang silhouette,
            // rotated as one rigid object into a true two-handed back mount.
            // Hilt rests over the physical right shoulder (viewer-left from
            // behind); blade points toward the opposite hip.
            withTransform({ rotate(145f, pivot = f.p(50f, 60f)) }) {
                drawOval(
                    color = (descriptor.glowColor ?: Color(0xFFEF4444)).copy(alpha = 0.13f),
                    topLeft = f.p(43f, 8f),
                    size = f.s(14f, 64f),
                )

                val blade = Path().apply {
                    moveTo(46.9f * f.u, 24f * f.u)
                    lineTo(50f * f.u, 9f * f.u)
                    lineTo(53.1f * f.u, 24f * f.u)
                    lineTo(52.5f * f.u, 70f * f.u)
                    lineTo(47.5f * f.u, 70f * f.u)
                    close()
                }
                drawPath(blade, descriptor.primaryColor)

                val leftFangs = Path().apply {
                    moveTo(46.9f * f.u, 24f * f.u)
                    lineTo(44.5f * f.u, 29f * f.u)
                    lineTo(47.3f * f.u, 31.6f * f.u)
                    lineTo(44.1f * f.u, 38f * f.u)
                    lineTo(47.3f * f.u, 41f * f.u)
                    lineTo(44.1f * f.u, 48f * f.u)
                    lineTo(47.3f * f.u, 51f * f.u)
                    lineTo(44.1f * f.u, 58f * f.u)
                    lineTo(47.3f * f.u, 61f * f.u)
                    lineTo(47.3f * f.u, 24f * f.u)
                    close()
                }
                val rightFangs = Path().apply {
                    moveTo(53.1f * f.u, 24f * f.u)
                    lineTo(55.5f * f.u, 29f * f.u)
                    lineTo(52.7f * f.u, 31.6f * f.u)
                    lineTo(55.9f * f.u, 38f * f.u)
                    lineTo(52.7f * f.u, 41f * f.u)
                    lineTo(55.9f * f.u, 48f * f.u)
                    lineTo(52.7f * f.u, 51f * f.u)
                    lineTo(55.9f * f.u, 58f * f.u)
                    lineTo(52.7f * f.u, 61f * f.u)
                    lineTo(52.7f * f.u, 24f * f.u)
                    close()
                }
                drawPath(leftFangs, descriptor.secondaryColor)
                drawPath(rightFangs, descriptor.secondaryColor)
                drawLine(descriptor.glowColor ?: Color(0xFFEF4444), f.p(50f, 15f), f.p(50f, 67f), 0.9f * f.u, StrokeCap.Round)
                drawLine(Color(0xFFFCA5A5), f.p(50f, 17f), f.p(50f, 65f), 0.4f * f.u, StrokeCap.Round)

                val outerGuard = Path().apply {
                    moveTo(40.9f * f.u, 68.4f * f.u)
                    quadraticTo(50f * f.u, 71.2f * f.u, 59.1f * f.u, 68.4f * f.u)
                    lineTo(59.9f * f.u, 71.2f * f.u)
                    quadraticTo(50f * f.u, 73.6f * f.u, 40.1f * f.u, 71.2f * f.u)
                    close()
                }
                val innerGuard = Path().apply {
                    moveTo(42.5f * f.u, 69f * f.u)
                    quadraticTo(50f * f.u, 70.8f * f.u, 57.5f * f.u, 69f * f.u)
                    lineTo(57.9f * f.u, 70.6f * f.u)
                    quadraticTo(50f * f.u, 72.4f * f.u, 42.1f * f.u, 70.6f * f.u)
                    close()
                }
                drawPath(outerGuard, descriptor.accentColor ?: Color(0xFF991B1B))
                drawPath(innerGuard, Color(0xFFD97706))
                drawCircle(descriptor.glowColor ?: Color(0xFFEF4444), radius = 1.1f * f.u, center = f.p(50f, 71.2f))
                drawLine(Color(0xFFFEF08A), f.p(50f, 70.4f), f.p(50f, 72f), 0.35f * f.u)

                drawRect(Color(0xFF451A03), topLeft = f.p(48.8f, 71.6f), size = f.s(2.4f, 15.4f))
                for (y in listOf(73.6f, 76.4f, 79.2f, 82f, 84.8f)) {
                    drawLine(Color(0xFFF59E0B), f.p(48.8f, y), f.p(51.2f, y + 1.4f), 0.4f * f.u)
                }
                val pommel = Path().apply {
                    moveTo(47.7f * f.u, 87f * f.u)
                    lineTo(52.3f * f.u, 87f * f.u)
                    lineTo(51.3f * f.u, 91f * f.u)
                    lineTo(50f * f.u, 93f * f.u)
                    lineTo(48.7f * f.u, 91f * f.u)
                    close()
                }
                drawPath(pommel, Color(0xFFB45309))
            }

            // Opposing baldric visibly secures the weapon to the torso.
            drawLine(leatherStrap, start = f.p(67f, 48f), end = f.p(33f, 76f), strokeWidth = 2.6f * f.u)
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.GREATSWORD_GENERIC -> {
            val expansion = silhouetteWidth * 0.6f
            val scabbard = Path().apply {
                moveTo((38f - expansion) * f.u, 78f * f.u)
                lineTo((44f + expansion) * f.u, 81f * f.u)
                lineTo((63f + expansion) * f.u, 44f * f.u)
                lineTo((57f - expansion) * f.u, 41f * f.u)
                close()
            }
            drawPath(scabbard, descriptor.primaryColor)
            drawPath(scabbard, descriptor.secondaryColor, style = Stroke(width = 1.2f * f.u))
            val guardWidth = 11f + signature.detailCount
            drawRoundRect(proceduralAccent, topLeft = f.p(61f - guardWidth / 2f, 40f), size = f.s(guardWidth, 3.5f), cornerRadius = CornerRadius(1.7f * f.u))
            drawRect(bladeColor(weapon.tier), topLeft = f.p(59.5f, 28f), size = f.s(3f, 13f))
            drawCircle(descriptor.secondaryColor, radius = 2.4f * f.u, center = f.p(61f, 27f))
            drawLine(leatherStrap, start = f.p(36f, 70f), end = f.p(57f, 46f), strokeWidth = 2.4f * f.u)
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SWORD_KNIGHT -> {
            // Reconstruct the authored 500x600 front asset at an exact 1:5
            // scale and orientation. The back view therefore shows the same
            // sword—not an inverted or separately designed approximation.
            val cx = knightHandX
            val blade = Path().apply {
                moveTo((cx - 2.3f) * f.u, 40f * f.u)
                lineTo(cx * f.u, 33f * f.u)
                lineTo((cx + 2.3f) * f.u, 40f * f.u)
                lineTo((cx + 1.7f) * f.u, 71f * f.u)
                lineTo((cx - 1.7f) * f.u, 71f * f.u)
                close()
            }
            drawPath(blade, descriptor.secondaryColor)
            val bevel = Path().apply {
                moveTo((cx - 1.9f) * f.u, 40f * f.u)
                lineTo(cx * f.u, 34f * f.u)
                lineTo(cx * f.u, 71f * f.u)
                lineTo((cx - 1.7f) * f.u, 71f * f.u)
                close()
            }
            drawPath(bevel, Color(0xFFCBD5E1))
            drawLine(
                descriptor.glowColor ?: Color(0xFF06B6D4),
                start = f.p(cx, 38f),
                end = f.p(cx, 69f),
                strokeWidth = 0.5f * f.u,
                cap = StrokeCap.Round
            )

            val guard = Path().apply {
                moveTo((cx - 6.3f) * f.u, 70.4f * f.u)
                quadraticTo(cx * f.u, 68.4f * f.u, (cx + 6.3f) * f.u, 70.4f * f.u)
                lineTo((cx + 6.7f) * f.u, 72.4f * f.u)
                quadraticTo(cx * f.u, 74f * f.u, (cx - 6.7f) * f.u, 72.4f * f.u)
                close()
            }
            drawPath(guard, Color(0xFFD97706))
            drawCircle(descriptor.glowColor ?: Color(0xFF06B6D4), radius = 0.7f * f.u, center = f.p(cx, 72.4f))

            drawRect(Color(0xFF1E293B), topLeft = f.p(cx - 1f, 72.8f), size = f.s(2f, 10.2f))
            for (y in listOf(74.4f, 76.4f, 78.4f, 80.4f, 82.4f)) {
                drawLine(Color(0xFF94A3B8), start = f.p(cx - 1f, y), end = f.p(cx + 1f, y + 1f), strokeWidth = 0.3f * f.u)
            }
            val pommel = Path().apply {
                moveTo((cx - 1.6f) * f.u, 83f * f.u)
                lineTo((cx + 1.6f) * f.u, 83f * f.u)
                lineTo((cx + 1f) * f.u, 86.4f * f.u)
                lineTo((cx - 1f) * f.u, 86.4f * f.u)
                close()
            }
            drawPath(pommel, Color(0xFFF59E0B))
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SWORD_GENERIC -> {
            val expansion = silhouetteWidth * 0.45f
            val scabbard = Path().apply {
                moveTo((41f - expansion) * f.u, 75f * f.u)
                lineTo((45f + expansion) * f.u, 77f * f.u)
                lineTo((61f + expansion) * f.u, 44f * f.u)
                lineTo((57f - expansion) * f.u, 42f * f.u)
                close()
            }
            drawPath(scabbard, descriptor.primaryColor)
            drawPath(scabbard, descriptor.secondaryColor, style = Stroke(width = 1.2f * f.u))
            drawRoundRect(proceduralAccent, topLeft = f.p(55f, 41f), size = f.s(10f, 3f), cornerRadius = CornerRadius(1.5f * f.u))
            drawRect(bladeColor(weapon.tier), topLeft = f.p(58.5f, 31f), size = f.s(2.8f, 10f))
            drawCircle(proceduralAccent, radius = 2.2f * f.u, center = f.p(60f, 30f))
            drawLine(leatherStrap, start = f.p(38f, 68f), end = f.p(57f, 46f), strokeWidth = 2f * f.u)
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_SHORT,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_GENERIC -> {
            val curve = 8.5f + silhouetteWidth
            val bow = Path().apply {
                moveTo(anchorX * f.u, (anchorY - 33f) * f.u)
                quadraticTo((anchorX - curve) * f.u, anchorY * f.u, anchorX * f.u, (anchorY + 33f) * f.u)
            }
            drawPath(bow, descriptor.primaryColor, style = Stroke(width = 1.4f * f.u))
            drawPath(bow, descriptor.secondaryColor, style = Stroke(width = 0.65f * f.u))
            drawLine(descriptor.accentColor ?: Color(0xFFE2E8F0), start = f.p(anchorX, anchorY - 33f), end = f.p(anchorX, anchorY + 33f), strokeWidth = 0.55f * f.u)
            drawRoundRect(Color(0xFF1E293B), topLeft = f.p(anchorX - 5.8f, anchorY - 3.5f), size = f.s(3.2f, 7f), cornerRadius = CornerRadius(1.2f * f.u))
            for (dy in listOf(-2f, 0f, 2f)) {
                drawLine(Color(0xFFF59E0B), start = f.p(anchorX - 5.7f, anchorY + dy), end = f.p(anchorX - 2.8f, anchorY + dy + 0.8f), strokeWidth = 0.45f * f.u)
            }
            drawLine(leatherStrap, start = f.p(anchorX - 4f, anchorY - 1f), end = f.p(anchorX + 6f, anchorY + 1f), strokeWidth = 2f * f.u)
            descriptor.glowColor?.let { fletch ->
                drawCircle(fletch, radius = 2f * f.u, center = f.p(anchorX, anchorY - 30f))
            }
            if (signature.ornament != ProceduralOrnament.NONE) {
                drawCircle(proceduralAccent, radius = 1.1f * f.u, center = f.p(anchorX - curve * 0.7f, anchorY))
            }
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.WAND_ARCANE -> {
            // A short wand belongs at the hip, not slung across the back like
            // a staff. Its carrier is fully concealed beneath the cape so no
            // strap is incorrectly painted over the cape surface.

            // The authored front drawable uses a 500x600 viewport while this
            // renderer uses 100x120 coordinates. These dimensions are the
            // exact 1:5 conversion of that asset.
            drawLine(
                descriptor.primaryColor,
                start = f.p(anchorX, anchorY + 18.4f),
                end = f.p(anchorX, anchorY + 2.4f),
                strokeWidth = 1.2f * f.u
            )
            drawRect(Color(0xFF3B2A1E), topLeft = f.p(anchorX - 1.2f, anchorY + 10.4f), size = f.s(2.4f, 6.4f))
            for (dy in listOf(11.8f, 13.6f, 15.4f)) {
                drawLine(Color(0xFFB07E24), start = f.p(anchorX - 1.1f, anchorY + dy), end = f.p(anchorX + 1.1f, anchorY + dy + 1f), strokeWidth = 0.4f * f.u)
            }
            val orb = descriptor.glowColor ?: orbColor(weapon.tier)
            drawCircle(orb.copy(alpha = 0.2f), radius = 3.6f * f.u, center = f.p(anchorX, anchorY))
            drawCircle(orb, radius = 2f * f.u, center = f.p(anchorX, anchorY))
            drawCircle(Color.White.copy(alpha = 0.88f), radius = 0.6f * f.u, center = f.p(anchorX - 0.7f, anchorY - 1.2f))
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.STAFF_WOODEN -> {
            // Staff strapped diagonally along dorsal spine from left hip to right shoulder
            drawRoundRect(descriptor.primaryColor, topLeft = f.p(43f, 78f), size = f.s(3.2f, 44f), cornerRadius = CornerRadius(1.6f * f.u))
            val orb = descriptor.glowColor ?: orbColor(weapon.tier)
            drawCircle(orb.copy(alpha = 0.35f), radius = 6f * f.u, center = f.p(58f, 36f))
            drawCircle(orb, radius = 3.6f * f.u, center = f.p(58f, 36f))
            drawCircle(Color.White.copy(alpha = 0.6f), radius = 1.2f * f.u, center = f.p(57f, 35f))
            drawLine(leatherStrap, start = f.p(40f, 68f), end = f.p(57f, 46f), strokeWidth = 2f * f.u)
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.STAFF_TRAVELER -> {
            drawRoundRect(descriptor.primaryColor, topLeft = f.p(43f, 78f), size = f.s(3.4f, 46f), cornerRadius = CornerRadius(1.7f * f.u))
            drawRoundRect(descriptor.secondaryColor, topLeft = f.p(55f, 32f), size = f.s(6f, 7f), cornerRadius = CornerRadius(3f * f.u))
            drawLine(leatherStrap, start = f.p(40f, 68f), end = f.p(57f, 46f), strokeWidth = 2f * f.u)
        }
        else -> {
            when (weapon.style) {
                ItemStyle.MACE -> {
                    drawRoundRect(Wood, topLeft = f.p(62f, 48f), size = f.s(3f, 32f), cornerRadius = CornerRadius(1.5f * f.u))
                    drawCircle(Color(0xFF9AA3AD), radius = 6.5f * f.u, center = f.p(63.5f, 46f))
                }
                ItemStyle.DAGGER -> {
                    drawRoundRect(Color(0xFF2C241E), topLeft = f.p(42f, 69f), size = f.s(16f, 4f), cornerRadius = CornerRadius(1.5f * f.u))
                    drawRoundRect(Color(0xFFB07E24), topLeft = f.p(56f, 68f), size = f.s(6f, 6f), cornerRadius = CornerRadius(1f * f.u))
                }
                else -> {
                    // Fallback scabbard
                    val scabbard = Path().apply {
                        moveTo(41f * f.u, 75f * f.u)
                        lineTo(45f * f.u, 77f * f.u)
                        lineTo(61f * f.u, 44f * f.u)
                        lineTo(57f * f.u, 42f * f.u)
                        close()
                    }
                    drawPath(scabbard, descriptor.primaryColor)
                    drawPath(scabbard, descriptor.secondaryColor, style = Stroke(width = 1.2f * f.u))
                }
            }
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
    val skin = f.skinColor("head", "face")
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
    val descriptor = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(weapon)
    val signature = descriptor.proceduralSignature
    val accent = GearVisuals.accent(weapon)
    val bladeWidthBonus = when (signature.silhouette) {
        ProceduralSilhouette.BALANCED -> 0f
        ProceduralSilhouette.ANGULAR -> 0.4f
        ProceduralSilhouette.CURVED -> -0.3f
        ProceduralSilhouette.FORTIFIED -> 0.9f
    }
    val necroPolearm = descriptor.archetype == com.fitnessquest.rpg.domain.visuals.VisualArchetype.STAFF_NECRO

    if (necroPolearm) {
        drawRoundRect(descriptor.primaryColor, f.p(handX - 1.5f, 18f), f.s(3f, 64f), CornerRadius(1.5f * f.u))
        drawWeaponHeadMotifAvatar(f, descriptor.weaponHeadMotif, descriptor, handX, weapon.tier)
        return
    }
    if (descriptor.archetype == com.fitnessquest.rpg.domain.visuals.VisualArchetype.SPEAR_GENERIC) {
        drawProceduralSpear(f, weapon, descriptor, handX)
        return
    }
    if (descriptor.archetype in WeaponVisualArchetypes.semantic) {
        drawSemanticWeapon(f, weapon, descriptor, handX)
        return
    }

    when (weapon.style) {
        ItemStyle.MACE -> {
            drawRoundRect(Wood, topLeft = f.p(handX - 1.5f, 46f), size = f.s(3f, 32f), cornerRadius = CornerRadius(1.5f * f.u))
            val headRadius = 7.5f + bladeWidthBonus
            drawCircle(Color(0xFF9AA3AD), radius = headRadius * f.u, center = f.p(handX, 42f))
            val spikes = listOf(0f to -10f, -9f to -4f, 9f to -4f, -7f to 6f, 7f to 6f).take(signature.detailCount + 1)
            for ((dx, dy) in spikes) {
                drawCircle(Color(0xFF767E87), radius = 2.4f * f.u, center = f.p(handX + dx, 42f + dy))
            }
            if (signature.ornament != ProceduralOrnament.NONE) drawCircle(accent, 2f * f.u, f.p(handX, 42f))
        }
        ItemStyle.GREATSWORD -> {
            val halfWidth = 3.5f + bladeWidthBonus
            val blade = Path().apply {
                moveTo((handX - halfWidth) * f.u, 70f * f.u)
                lineTo((handX - halfWidth) * f.u, 22f * f.u)
                lineTo(handX * f.u, 10f * f.u)
                lineTo((handX + halfWidth) * f.u, 22f * f.u)
                lineTo((handX + halfWidth) * f.u, 70f * f.u)
                close()
            }
            if (legendary) {
                drawPath(blade, Color(0xFFFFA040).copy(alpha = 0.5f), style = Stroke(width = 3f * f.u))
            }
            drawPath(blade, Color(0xFFE8763A))
            drawRect(accent, topLeft = f.p(handX - 0.8f, 12f), size = f.s(1.6f, 56f))
            val guardWidth = 15f + signature.detailCount
            drawRoundRect(Color(0xFFF0C040), topLeft = f.p(handX - guardWidth / 2f, 70f), size = f.s(guardWidth, 4f), cornerRadius = CornerRadius(2f * f.u))
        }
        ItemStyle.WAND -> {
            val shaftWidth = 2.1f + signature.detailCount * 0.12f
            drawRoundRect(WoodLight, topLeft = f.p(handX - shaftWidth / 2f, 56f), size = f.s(shaftWidth, 22f), cornerRadius = CornerRadius(1.2f * f.u))
            val spark = orbColor(weapon.tier)
            drawCircle(spark.copy(alpha = 0.35f), radius = 5f * f.u, center = f.p(handX, 53f))
            if (signature.silhouette == ProceduralSilhouette.ANGULAR) {
                val crystal = Path().apply {
                    moveTo(handX * f.u, 49f * f.u)
                    lineTo((handX + 3f) * f.u, 53f * f.u)
                    lineTo(handX * f.u, 57f * f.u)
                    lineTo((handX - 3f) * f.u, 53f * f.u)
                    close()
                }
                drawPath(crystal, spark)
            } else drawCircle(spark, radius = 2.6f * f.u, center = f.p(handX, 53f))
            if (signature.trim == ProceduralTrim.RUNIC) drawRect(accent, topLeft = f.p(handX - 1.7f, 65f), size = f.s(3.4f, 1f))
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
            val curve = 13f + bladeWidthBonus
            val bow = Path().apply {
                moveTo((handX + 1.5f) * f.u, 52f * f.u)
                quadraticTo((handX + curve) * f.u, 70f * f.u, (handX + 1.5f) * f.u, 88f * f.u)
            }
            if (legendary) {
                drawPath(bow, Color(0xFF6BC96B).copy(alpha = 0.4f), style = Stroke(width = 5f * f.u))
            }
            drawPath(bow, wood, style = Stroke(width = 2.6f * f.u))
            drawLine(
                Color(0xFFE8E4D8),
                start = f.p(handX + 1.5f, 52f),
                end = f.p(handX + 1.5f, 88f),
                strokeWidth = 0.9f * f.u
            )
            drawCircle(bladeColor(weapon.tier), radius = 1.8f * f.u, center = f.p(handX + 1.5f, 52f))
            drawCircle(bladeColor(weapon.tier), radius = 1.8f * f.u, center = f.p(handX + 1.5f, 88f))
            // Leather grip wrap centered on hand
            drawRect(Color(0xFF4A3525), topLeft = f.p(handX + 4f, 68f), size = f.s(3.5f, 6f))
            if (signature.ornament != ProceduralOrnament.NONE) {
                drawCircle(accent, radius = 1.2f * f.u, center = f.p(handX + curve * 0.72f, 70f))
            }
        }
        else -> { // SWORD and fallback
            val topY = when (weapon.tier) {
                1 -> 44f
                2, 3 -> 34f
                else -> 30f
            }
            val halfWidth = 2.2f + bladeWidthBonus
            val blade = Path().apply {
                moveTo((handX - halfWidth) * f.u, 72f * f.u)
                lineTo((handX - halfWidth) * f.u, (topY + 6f) * f.u)
                lineTo(handX * f.u, topY * f.u)
                lineTo((handX + halfWidth) * f.u, (topY + 6f) * f.u)
                lineTo((handX + halfWidth) * f.u, 72f * f.u)
                close()
            }
            if (legendary) {
                drawPath(blade, bladeColor(weapon.tier).copy(alpha = 0.5f), style = Stroke(width = 3f * f.u))
            }
            drawPath(blade, bladeColor(weapon.tier))
            if (signature.trim != ProceduralTrim.PLAIN) {
                drawLine(accent, f.p(handX, topY + 5f), f.p(handX, 69f), 0.9f * f.u)
            }
            val guardWidth = 12f + signature.detailCount * 0.7f
            drawRoundRect(Color(0xFFB07E24), topLeft = f.p(handX - guardWidth / 2f, 71f), size = f.s(guardWidth, 3.5f), cornerRadius = CornerRadius(1.7f * f.u))
        }
    }
}

private fun DrawScope.drawSemanticWeapon(
    f: AvatarFrame,
    weapon: ItemEntity,
    descriptor: com.fitnessquest.rpg.domain.visuals.EquipmentVisualDescriptor,
    handX: Float,
) {
    val accent = descriptor.accentColor ?: GearVisuals.accent(weapon)
    val glow = descriptor.glowColor ?: accent
    when (descriptor.archetype) {
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.UNARMED_WRAP -> {
            drawCircle(descriptor.primaryColor, radius = 5.2f * f.u, center = f.p(handX, 78f))
            for (y in listOf(75f, 78f, 81f)) {
                drawLine(accent, f.p(handX - 4f, y), f.p(handX + 4f, y), 0.9f * f.u)
            }
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.TONFA -> {
            drawRoundRect(descriptor.primaryColor, f.p(handX - 2f, 54f), f.s(4f, 42f), CornerRadius(2f * f.u))
            drawRoundRect(descriptor.secondaryColor, f.p(handX - 9f, 68f), f.s(11f, 3.5f), CornerRadius(1.5f * f.u))
            drawRect(accent, f.p(handX - 2f, 61f), f.s(4f, 2f))
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.INSTRUMENT -> {
            val name = weapon.name.lowercase()
            when {
                name.contains("lute") -> {
                    drawOval(descriptor.primaryColor, f.p(handX - 7f, 64f), f.s(14f, 18f))
                    drawRoundRect(descriptor.secondaryColor, f.p(handX - 1.5f, 45f), f.s(3f, 22f), CornerRadius(1.5f * f.u))
                    drawCircle(accent, 2.5f * f.u, f.p(handX, 72f))
                    for (dx in listOf(-2f, 0f, 2f)) drawLine(accent.copy(alpha = 0.7f), f.p(handX + dx, 61f), f.p(handX + dx, 79f), 0.35f * f.u)
                }
                name.contains("flute") -> {
                    drawRoundRect(descriptor.primaryColor, f.p(handX - 2f, 48f), f.s(4f, 37f), CornerRadius(2f * f.u))
                    for (y in listOf(56f, 64f, 72f)) drawCircle(accent, 0.9f * f.u, f.p(handX, y))
                }
                else -> {
                    val horn = Path().apply {
                        moveTo((handX - 2f) * f.u, 76f * f.u)
                        quadraticTo((handX + 10f) * f.u, 65f * f.u, (handX + 3f) * f.u, 50f * f.u)
                        lineTo((handX - 1f) * f.u, 52f * f.u)
                        quadraticTo((handX + 4f) * f.u, 65f * f.u, (handX - 5f) * f.u, 72f * f.u)
                        close()
                    }
                    drawPath(horn, accent)
                    drawRoundRect(descriptor.secondaryColor, f.p(handX - 6f, 70f), f.s(7f, 4f), CornerRadius(2f * f.u))
                }
            }
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.AXE -> {
            drawRoundRect(descriptor.primaryColor, f.p(handX - 1.5f, 28f), f.s(3f, 77f), CornerRadius(1.5f * f.u))
            val head = Path().apply {
                moveTo((handX - 1f) * f.u, 20f * f.u)
                quadraticTo((handX - 13f) * f.u, 20f * f.u, (handX - 14f) * f.u, 34f * f.u)
                lineTo((handX - 1f) * f.u, 31f * f.u)
                lineTo((handX + 7f) * f.u, 34f * f.u)
                lineTo((handX + 5f) * f.u, 21f * f.u)
                close()
            }
            drawPath(head, descriptor.secondaryColor)
            drawPath(head, accent, style = Stroke(width = 0.9f * f.u))
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SCYTHE -> {
            drawRoundRect(descriptor.primaryColor, f.p(handX - 1.5f, 24f), f.s(3f, 81f), CornerRadius(1.5f * f.u))
            val blade = Path().apply {
                moveTo(handX * f.u, 25f * f.u)
                quadraticTo((handX - 13f) * f.u, 12f * f.u, (handX - 27f) * f.u, 18f * f.u)
                quadraticTo((handX - 13f) * f.u, 17f * f.u, (handX - 2f) * f.u, 31f * f.u)
                close()
            }
            drawPath(blade, descriptor.secondaryColor)
            drawLine(glow.copy(alpha = 0.8f), f.p(handX - 24f, 18f), f.p(handX - 3f, 27f), 0.7f * f.u)
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.HAMMER -> {
            drawRoundRect(descriptor.primaryColor, f.p(handX - 1.7f, 31f), f.s(3.4f, 74f), CornerRadius(1.7f * f.u))
            drawRoundRect(descriptor.secondaryColor, f.p(handX - 10f, 20f), f.s(20f, 13f), CornerRadius(2f * f.u))
            drawRect(accent, f.p(handX - 11f, 23f), f.s(22f, 3f))
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.CLUB -> {
            val club = Path().apply {
                moveTo((handX - 2f) * f.u, 104f * f.u)
                lineTo((handX - 5.5f) * f.u, 30f * f.u)
                quadraticTo(handX * f.u, 19f * f.u, (handX + 5.5f) * f.u, 30f * f.u)
                lineTo((handX + 2f) * f.u, 104f * f.u)
                close()
            }
            drawPath(club, descriptor.primaryColor)
            drawPath(club, descriptor.secondaryColor, style = Stroke(width = 1f * f.u))
            for (y in listOf(38f, 45f, 52f)) drawLine(accent, f.p(handX - 4f, y), f.p(handX + 4f, y + 2f), 0.7f * f.u)
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SCEPTER -> {
            drawRoundRect(descriptor.primaryColor, f.p(handX - 1.5f, 47f), f.s(3f, 47f), CornerRadius(1.5f * f.u))
            drawCircle(glow.copy(alpha = 0.3f), 7f * f.u, f.p(handX, 42f))
            drawCircle(accent, 4f * f.u, f.p(handX, 42f))
            drawCircle(Color.White.copy(alpha = 0.7f), 1.2f * f.u, f.p(handX - 1f, 41f))
            drawRect(accent, f.p(handX - 4f, 91f), f.s(8f, 3f))
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARCANE_FOCUS -> {
            drawCircle(glow.copy(alpha = 0.2f), 8f * f.u, f.p(handX, 60f))
            val crystal = Path().apply {
                moveTo(handX * f.u, 52f * f.u)
                lineTo((handX + 5f) * f.u, 60f * f.u)
                lineTo(handX * f.u, 68f * f.u)
                lineTo((handX - 5f) * f.u, 60f * f.u)
                close()
            }
            drawPath(crystal, accent)
            drawPath(crystal, Color.White.copy(alpha = 0.65f), style = Stroke(width = 0.8f * f.u))
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.WAND_ARCANE -> {
            drawRoundRect(descriptor.primaryColor, f.p(handX - 1.1f, 58f), f.s(2.2f, 18f), CornerRadius(1.1f * f.u))
            drawWeaponHeadMotifAvatar(f, descriptor.weaponHeadMotif, descriptor, handX, weapon.tier, topY = 53f)
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.STAFF_WOODEN -> {
            drawRoundRect(descriptor.primaryColor, f.p(handX - 1.4f, 18f), f.s(2.8f, 62f), CornerRadius(1.4f * f.u))
            if (descriptor.weaponHeadMotif == com.fitnessquest.rpg.domain.visuals.WeaponHeadMotif.NONE) {
                drawCircle(glow.copy(alpha = 0.3f), 8f * f.u, f.p(handX, 15f))
                drawCircle(glow, 4.8f * f.u, f.p(handX, 15f))
            } else {
                drawWeaponHeadMotifAvatar(f, descriptor.weaponHeadMotif, descriptor, handX, weapon.tier)
            }
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.STAFF_TRAVELER -> {
            drawRoundRect(descriptor.primaryColor, f.p(handX - 1.5f, 16f), f.s(3f, 66f), CornerRadius(1.5f * f.u))
            drawRect(descriptor.secondaryColor, f.p(handX - 1.8f, 38f), f.s(3.6f, 2.2f))
            drawRect(descriptor.secondaryColor, f.p(handX - 1.8f, 58f), f.s(3.6f, 2.2f))
            drawRoundRect(descriptor.secondaryColor, f.p(handX - 3.2f, 10f), f.s(6.4f, 7f), CornerRadius(3.2f * f.u))
            drawCircle(descriptor.accentColor ?: descriptor.secondaryColor, 1.4f * f.u, f.p(handX, 13.5f))
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.STAFF_DRUID,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.STAFF_RUNED -> {
            drawRoundRect(descriptor.primaryColor, f.p(handX - 1.4f, 18f), f.s(2.8f, 62f), CornerRadius(1.4f * f.u))
            drawWeaponHeadMotifAvatar(f, descriptor.weaponHeadMotif, descriptor, handX, weapon.tier)
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SWORD_GENERIC,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SWORD_KNIGHT,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.SWORD_RAPIER -> {
            val halfWidth = if (descriptor.archetype == com.fitnessquest.rpg.domain.visuals.VisualArchetype.SWORD_RAPIER) 1.6f else 2.2f
            val topY = if (weapon.tier >= 3) 34f else 44f
            val blade = Path().apply {
                moveTo((handX - halfWidth) * f.u, 72f * f.u)
                lineTo((handX - halfWidth) * f.u, (topY + 6f) * f.u)
                lineTo(handX * f.u, topY * f.u)
                lineTo((handX + halfWidth) * f.u, (topY + 6f) * f.u)
                lineTo((handX + halfWidth) * f.u, 72f * f.u)
                close()
            }
            drawPath(blade, descriptor.secondaryColor)
            drawRoundRect(descriptor.accentColor ?: Color(0xFFB07E24), f.p(handX - 6f, 71f), f.s(12f, 3.5f), CornerRadius(1.7f * f.u))
            drawWeaponHeadMotifAvatar(f, descriptor.weaponHeadMotif, descriptor, handX, weapon.tier, topY = topY - 4f)
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.GREATSWORD_GENERIC,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.GREATSWORD_DRAGON -> {
            val halfWidth = 3.5f
            val blade = Path().apply {
                moveTo((handX - halfWidth) * f.u, 70f * f.u)
                lineTo((handX - halfWidth) * f.u, 22f * f.u)
                lineTo(handX * f.u, 10f * f.u)
                lineTo((handX + halfWidth) * f.u, 22f * f.u)
                lineTo((handX + halfWidth) * f.u, 70f * f.u)
                close()
            }
            drawPath(blade, descriptor.secondaryColor)
            drawRoundRect(descriptor.accentColor ?: Color(0xFFF0C040), f.p(handX - 7.5f, 70f), f.s(15f, 4f), CornerRadius(2f * f.u))
            drawWeaponHeadMotifAvatar(f, descriptor.weaponHeadMotif, descriptor, handX, weapon.tier, topY = 8f)
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_SHORT,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_GENERIC,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_RECURVE,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_LONGBOW,
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_WARBOW -> {
            val curve = when (descriptor.archetype) {
                com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_RECURVE -> 16f
                com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_LONGBOW -> 10f
                com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_WARBOW -> 14f
                com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_SHORT -> 12f
                else -> 13f
            }
            val bow = Path().apply {
                moveTo((handX + 1.5f) * f.u, 52f * f.u)
                quadraticTo((handX + curve) * f.u, 70f * f.u, (handX + 1.5f) * f.u, 88f * f.u)
            }
            drawPath(bow, descriptor.primaryColor, style = Stroke(2.6f * f.u))
            drawLine(descriptor.accentColor ?: Color(0xFFE8E4D8), f.p(handX + 1.5f, 52f), f.p(handX + 1.5f, 88f), 0.9f * f.u)
            descriptor.glowColor?.let { drawCircle(it, 1.8f * f.u, f.p(handX + curve * 0.7f, 70f)) }
        }
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.DAGGER_GENERIC -> drawDagger(f, handX = handX, tier = weapon.tier, length = 18f)
        com.fitnessquest.rpg.domain.visuals.VisualArchetype.MACE_GENERIC -> {
            drawRoundRect(descriptor.primaryColor, f.p(handX - 1.5f, 46f), f.s(3f, 32f), CornerRadius(1.5f * f.u))
            drawCircle(descriptor.secondaryColor, 7.5f * f.u, f.p(handX, 42f))
        }
        else -> Unit
    }
}

private fun DrawScope.drawProceduralSpear(
    f: AvatarFrame,
    weapon: ItemEntity,
    descriptor: com.fitnessquest.rpg.domain.visuals.EquipmentVisualDescriptor,
    handX: Float,
) {
    val signature = descriptor.proceduralSignature
    val accent = descriptor.accentColor ?: GearVisuals.accent(weapon)
    val shaftWidth = when (signature.silhouette) {
        ProceduralSilhouette.CURVED -> 2.1f
        ProceduralSilhouette.FORTIFIED -> 3f
        else -> 2.5f
    }
    val headHalfWidth = when (signature.silhouette) {
        ProceduralSilhouette.BALANCED -> 4.2f
        ProceduralSilhouette.ANGULAR -> 3.6f
        ProceduralSilhouette.CURVED -> 4.8f
        ProceduralSilhouette.FORTIFIED -> 5.4f
    }
    val tipY = if (weapon.tier >= 4) 7f else 11f
    val shoulderY = if (signature.silhouette == ProceduralSilhouette.ANGULAR) 25f else 23f

    descriptor.glowColor?.let { glow ->
        drawLine(
            glow.copy(alpha = 0.24f), f.p(handX, 30f), f.p(handX, 105f),
            (shaftWidth + 2.4f) * f.u, StrokeCap.Round
        )
    }
    drawRoundRect(
        descriptor.primaryColor,
        topLeft = f.p(handX - shaftWidth / 2f, 28f),
        size = f.s(shaftWidth, 77f),
        cornerRadius = CornerRadius(shaftWidth / 2f * f.u)
    )
    drawLine(
        Color.White.copy(alpha = 0.2f),
        f.p(handX - shaftWidth * 0.2f, 32f),
        f.p(handX - shaftWidth * 0.2f, 100f),
        0.45f * f.u
    )

    val spearhead = Path().apply {
        moveTo(handX * f.u, tipY * f.u)
        lineTo((handX + headHalfWidth) * f.u, shoulderY * f.u)
        quadraticTo((handX + 2.2f) * f.u, 28f * f.u, handX * f.u, 34f * f.u)
        quadraticTo((handX - 2.2f) * f.u, 28f * f.u, (handX - headHalfWidth) * f.u, shoulderY * f.u)
        close()
    }
    drawPath(spearhead, descriptor.secondaryColor)
    drawPath(spearhead, accent.copy(alpha = 0.8f), style = Stroke(width = 0.9f * f.u))
    drawLine(
        descriptor.glowColor ?: Color.White.copy(alpha = 0.55f),
        f.p(handX, tipY + 2f), f.p(handX, 31f), 0.7f * f.u, StrokeCap.Round
    )
    drawRoundRect(accent, topLeft = f.p(handX - 2.8f, 31f), size = f.s(5.6f, 3f), cornerRadius = CornerRadius(1f * f.u))

    when (signature.trim) {
        ProceduralTrim.PLAIN -> Unit
        ProceduralTrim.BAND, ProceduralTrim.SPLIT -> for (y in listOf(38f, 41f)) {
            drawRect(accent, topLeft = f.p(handX - 2f, y), size = f.s(4f, 1f))
        }
        ProceduralTrim.CHEVRON -> {
            drawLine(accent, f.p(handX - 2f, 39f), f.p(handX, 41f), 0.8f * f.u)
            drawLine(accent, f.p(handX, 41f), f.p(handX + 2f, 39f), 0.8f * f.u)
        }
        ProceduralTrim.RUNIC -> drawCircle(accent, radius = 1.4f * f.u, center = f.p(handX, 40f))
    }
    drawRoundRect(
        accent,
        topLeft = f.p(handX - (shaftWidth + 1f) / 2f, 102f),
        size = f.s(shaftWidth + 1f, 4f),
        cornerRadius = CornerRadius(1f * f.u)
    )
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

    if (!frame.facingBack) {
        // Bear body (Front)
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
    } else {
        // Bear body (Back) - Muscular dorsal hump, rear haunches, hind paws, tail, zero facial features
        drawCircle(color = furDark, center = Offset(60f * u, 68f * u), radius = 33f * u)
        drawCircle(color = furMain, center = Offset(60f * u, 66f * u), radius = 31f * u)
        // Muscular dorsal spine shading (soft natural anatomy)
        drawLine(furDark.copy(alpha = 0.55f), start = Offset(60f * u, 46f * u), end = Offset(60f * u, 84f * u), strokeWidth = 3f * u)
        // Shoulder blade muscle humps
        drawOval(furDark.copy(alpha = 0.45f), topLeft = Offset(38f * u, 54f * u), size = Size(16f * u, 24f * u))
        drawOval(furDark.copy(alpha = 0.45f), topLeft = Offset(66f * u, 54f * u), size = Size(16f * u, 24f * u))

        // Rear Bear head - Cranial dome with no facial features
        drawCircle(color = furDark, center = Offset(60f * u, 38f * u), radius = 22f * u)
        drawCircle(color = furMain, center = Offset(60f * u, 36f * u), radius = 20f * u)
        // Rear rounded ears with inner shading
        drawCircle(color = furDark, center = Offset(42f * u, 22f * u), radius = 8f * u)
        drawCircle(color = furDark, center = Offset(78f * u, 22f * u), radius = 8f * u)
        drawCircle(color = furMain, center = Offset(42f * u, 23f * u), radius = 5.5f * u)
        drawCircle(color = furMain, center = Offset(78f * u, 23f * u), radius = 5.5f * u)

        // Soft rounded bear tail
        drawCircle(color = furDark.copy(alpha = 0.8f), center = Offset(60f * u, 91f * u), radius = 5.5f * u)
        drawCircle(color = furMain, center = Offset(60f * u, 90f * u), radius = 4.5f * u)

        // Hind Paws
        drawRoundRect(color = furDark, topLeft = Offset(26f * u, 75f * u), size = Size(20f * u, 28f * u), cornerRadius = CornerRadius(10f * u))
        drawRoundRect(color = furDark, topLeft = Offset(74f * u, 75f * u), size = Size(20f * u, 28f * u), cornerRadius = CornerRadius(10f * u))
    }
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
    if (f.facingBack) {
        drawHunterWolfBackCompanion(f)
        return
    }
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

private fun DrawScope.drawHunterWolfBackCompanion(f: AvatarFrame) {
    val u = f.u
    val fur = Color(0xFF475569)
    val furDark = Color(0xFF1E293B)
    val furLight = Color(0xFF94A3B8)
    val collar = Color(0xFF0F766E)
    val x = 16f * u
    val y = 78f * u
    val breath = sin(f.phase * 6.28f) * 1.2f * u
    val tailWag = sin(f.phase * 6.28f) * 3f * u

    // Rear-facing rump and hind legs; no chest patch or front-facing tag.
    drawRoundRect(fur, topLeft = Offset(x - 7f * u, y - 2f * u + breath * 0.5f), size = Size(14f * u, 16f * u), cornerRadius = CornerRadius(5f * u))
    drawOval(furDark.copy(alpha = 0.45f), topLeft = Offset(x - 4f * u, y + 1f * u), size = Size(8f * u, 11f * u))

    // Preserve the front view's bushy silhouette, mirrored onto the visible
    // rear attachment point so this reads as the same wolf after turning.
    val tail = Path().apply {
        moveTo(x + 6f * u, y + 8f * u)
        quadraticTo(x + 14f * u + tailWag, y + 2f * u, x + 12f * u + tailWag, y - 6f * u)
        quadraticTo(x + 8f * u + tailWag * 0.5f, y + 2f * u, x + 4f * u, y + 10f * u)
        close()
    }
    drawPath(tail, furDark)
    drawPath(tail, fur, style = Stroke(width = 1.2f * u))

    drawRoundRect(furDark, topLeft = Offset(x - 6f * u, y + 10f * u), size = Size(5f * u, 6f * u), cornerRadius = CornerRadius(2f * u))
    drawRoundRect(furDark, topLeft = Offset(x + 1f * u, y + 10f * u), size = Size(5f * u, 6f * u), cornerRadius = CornerRadius(2f * u))

    val headY = y - 9f * u + breath
    drawOval(fur, topLeft = Offset(x - 6f * u, headY - 4f * u), size = Size(12f * u, 10f * u))
    val leftEar = Path().apply {
        moveTo(x - 5f * u, headY - 2f * u)
        lineTo(x - 6f * u, headY - 11f * u)
        lineTo(x - 1f * u, headY - 4f * u)
        close()
    }
    val rightEar = Path().apply {
        moveTo(x + 1f * u, headY - 4f * u)
        lineTo(x + 6f * u, headY - 11f * u)
        lineTo(x + 5f * u, headY - 2f * u)
        close()
    }
    drawPath(leftEar, furDark)
    drawPath(rightEar, furDark)
    drawLine(furLight.copy(alpha = 0.55f), start = Offset(x, headY - 3f * u), end = Offset(x, headY + 4f * u), strokeWidth = 1f * u)

    // The collar wraps around the neck and remains visible from behind; only
    // its gold tag belongs exclusively to the front view.
    drawRoundRect(
        collar,
        topLeft = Offset(x - 5f * u, y - 4f * u + breath),
        size = Size(11f * u, 3f * u),
        cornerRadius = CornerRadius(1.5f * u),
    )
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
