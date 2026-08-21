package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.GearRarity
import com.fitnessquest.rpg.domain.build
import com.fitnessquest.rpg.domain.visuals.AvatarOrientation
import com.fitnessquest.rpg.domain.visuals.BodyRegion
import com.fitnessquest.rpg.domain.visuals.EquipmentDye
import com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry
import com.fitnessquest.rpg.domain.visuals.EquipmentVisualSpec
import com.fitnessquest.rpg.domain.visuals.PaperDollLayerOrder
import com.fitnessquest.rpg.domain.visuals.PaperDollVisualSlot
import com.fitnessquest.rpg.domain.visuals.VisualFacingRule
import com.fitnessquest.rpg.ui.effects.rememberDeviceTilt

import com.fitnessquest.rpg.domain.visuals.PaperDollPassResolver
import com.fitnessquest.rpg.domain.visuals.PaperDollRenderEntry
import com.fitnessquest.rpg.domain.visuals.ResolvedPaperDollLayer
import com.fitnessquest.rpg.domain.visuals.ResolvedPaperDollPassPlan

/**
 * Calculates 3D depth displacement multiplier for holographic card parallax.
 */
fun PaperDollLayerOrder.parallaxDepth(): Float = when (this) {
    PaperDollLayerOrder.BG_PEDESTAL -> 0.2f
    PaperDollLayerOrder.FX_AURA_BACK -> 0.3f
    PaperDollLayerOrder.GEAR_BACK -> 0.45f
    PaperDollLayerOrder.BODY_BASE -> 0.7f
    PaperDollLayerOrder.BODY_HAIR_BACK -> 0.65f
    PaperDollLayerOrder.GEAR_LEGS -> 0.8f
    PaperDollLayerOrder.GEAR_TORSO -> 0.9f
    PaperDollLayerOrder.GEAR_FEET -> 0.85f
    PaperDollLayerOrder.GEAR_HANDS -> 1.05f
    PaperDollLayerOrder.BODY_HAIR_FRONT -> 1.0f
    PaperDollLayerOrder.GEAR_HEAD -> 1.15f
    PaperDollLayerOrder.GEAR_WEAPON -> 1.35f
    PaperDollLayerOrder.GEAR_TRINKET -> 1.2f
    PaperDollLayerOrder.FX_AURA_FRONT -> 1.5f
}

/**
 * Compositor that renders avatar and gear layers in strictly interleaved, orientation-aware order:
 * - Body base
 * - Canvas fallback slots
 * - 2D Drawable layers
 * with 3D gyroscopic parallax depth, procedural dyes, and organic cape flutter physics.
 */
@Composable
fun PaperDollLayerRenderer(
    clazz: CharacterClass,
    gear: Map<ItemSlot, ItemEntity>,
    appearance: AvatarAppearance,
    modifier: Modifier = Modifier,
    animation: HeroAnimation = HeroAnimation.IDLE,
    expression: AvatarExpression = AvatarExpression.CALM,
    equipAnimationState: EquipAnimationState? = null,
    customDyes: Map<ItemSlot, EquipmentDye> = emptyMap(),
    facingBack: Boolean = false,
    showClassOutfit: Boolean = false,
    detail: AvatarDetail = AvatarDetail.FULL
) {
    val context = LocalContext.current
    val tilt by rememberDeviceTilt()

    val infiniteTransition = rememberInfiniteTransition(label = "paperDollPhysics")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    val capeFlutter by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "capeFlutter"
    )
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "animPhase"
    )

    val orientation = AvatarOrientation.fromFacingBack(facingBack)

    // Pure, orientation-aware structural pass plan resolution with full slot interleaving
    val passPlan = remember(gear, customDyes, orientation) {
        PaperDollPassResolver.resolvePlan(
            gear = gear,
            orientation = orientation,
            customDyes = customDyes,
            context = context
        )
    }

    // Filter full loadout context respecting active orientation facing rules
    val orientationGear = remember(gear, orientation) {
        gear.filter { (_, item) ->
            val spec = EquipmentVisualRegistry.resolveSpec(item)
            when (orientation) {
                AvatarOrientation.FRONT -> spec.facingRule != VisualFacingRule.BACK_ONLY
                AvatarOrientation.BACK -> spec.facingRule != VisualFacingRule.FRONT_ONLY
            }
        }
    }

    val isTransformedDruid = appearance.druidForm in setOf("BEAR", "PANTHER", "TREANT", "MOONKIN", "AVATAR")

    Box(
        modifier = modifier
            .aspectRatio(500f / 600f)
            .fillMaxSize()
            .graphicsLayer {
                // Shared breathing animation for the entire stack (Avatar + Gear)
                scaleX = breatheScale
                scaleY = breatheScale
                transformOrigin = TransformOrigin(0.5f, 0.9f)
            },
        contentAlignment = Alignment.Center
    ) {
        val hasAnyDrawable = !isTransformedDruid && passPlan.entries.any { it is PaperDollRenderEntry.DrawableLayer }
        if (!hasAnyDrawable) {
            // Pure Canvas Mode / Beast Forms: Single full-avatar render pass
            CharacterAvatar(
                clazz = clazz,
                gear = if (isTransformedDruid) gear else passPlan.canvasFallbackGear,
                appearance = appearance,
                modifier = Modifier.fillMaxSize(),
                showClassOutfit = showClassOutfit,
                facingBack = facingBack,
                animation = animation,
                expression = expression,
                detail = detail,
                enableBreathing = false,
                hiddenRegions = passPlan.hiddenBodyRegions
            )
        } else {
            // Mixed Interleaved Pipeline: Render each entry in exact orientation-aware zIndex order
            val raceBuild = appearance.race.build()

            Box(modifier = Modifier.fillMaxSize()) {
                passPlan.entries.forEach { entry ->
                    when (entry) {
                        is PaperDollRenderEntry.AvatarBackdrop -> {
                            val bgDepth = PaperDollLayerOrder.FX_AURA_BACK.parallaxDepth()
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationX = tilt.x * bgDepth * 14f * density
                                        translationY = tilt.y * bgDepth * 14f * density
                                    }
                            ) {
                                val frame = AvatarFrame(
                                    u = size.width / 100f,
                                    cls = clazz,
                                    look = lookFor(clazz),
                                    gear = orientationGear,
                                    costume = showClassOutfit,
                                    highlightMuscles = emptySet(),
                                    facingBack = facingBack,
                                    appearance = appearance,
                                    expression = expression,
                                    detail = detail,
                                    phase = animPhase,
                                    hiddenRegions = passPlan.hiddenBodyRegions
                                )
                                val pose = calculatePose(animation, animPhase, frame.u)
                                with(AvatarPainter) {
                                    drawBackdrop(frame, pose, tilt = tilt)
                                }
                            }
                        }
                        is PaperDollRenderEntry.BodyBase -> {
                            val bodyDepth = PaperDollLayerOrder.BODY_BASE.parallaxDepth()
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationX = tilt.x * bodyDepth * 14f * density
                                        translationY = tilt.y * bodyDepth * 14f * density
                                    }
                            ) {
                                val frame = AvatarFrame(
                                    u = size.width / 100f,
                                    cls = clazz,
                                    look = lookFor(clazz),
                                    gear = orientationGear,
                                    costume = showClassOutfit,
                                    highlightMuscles = emptySet(),
                                    facingBack = facingBack,
                                    appearance = appearance,
                                    expression = expression,
                                    detail = detail,
                                    phase = animPhase,
                                    hiddenRegions = passPlan.hiddenBodyRegions
                                )
                                val pose = calculatePose(animation, animPhase, frame.u)
                                with(AvatarPainter) {
                                    drawBodyBase(frame, pose, tilt = tilt)
                                }
                            }
                        }
                        is PaperDollRenderEntry.DrawableLayer -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = raceBuild.width
                                        scaleY = raceBuild.height
                                        transformOrigin = TransformOrigin(0.5f, 0.9f)
                                    }
                            ) {
                                RenderPaperDollLayer(
                                    layer = entry.layer,
                                    tilt = tilt,
                                    capeFlutter = capeFlutter,
                                    equipAnimationState = equipAnimationState
                                )
                            }
                        }
                        is PaperDollRenderEntry.CanvasFallbackSlot -> {
                            val depth = entry.spec.layerOrder.parallaxDepth()
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationX = tilt.x * depth * 14f * density
                                        translationY = tilt.y * depth * 14f * density
                                    }
                            ) {
                                val frame = AvatarFrame(
                                    u = size.width / 100f,
                                    cls = clazz,
                                    look = lookFor(clazz),
                                    gear = orientationGear,
                                    costume = false,
                                    highlightMuscles = emptySet(),
                                    facingBack = facingBack,
                                    appearance = appearance,
                                    expression = expression,
                                    detail = detail,
                                    phase = animPhase,
                                    hiddenRegions = passPlan.hiddenBodyRegions
                                )
                                val pose = calculatePose(animation, animPhase, frame.u)
                                with(AvatarPainter) {
                                    drawSlotEquipment(entry.slot, frame, pose, tilt = tilt)
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
 * Renders an individual 2D paper doll layer sprite with independent 3D parallax depth, dye tinting, and flutter.
 */
@Composable
private fun RenderPaperDollLayer(
    layer: ResolvedPaperDollLayer,
    tilt: androidx.compose.ui.geometry.Offset,
    capeFlutter: Float,
    equipAnimationState: EquipAnimationState?
) {
    val isEquipTarget = equipAnimationState?.equippedSlot?.let {
        layer.spec?.domainSlot == it
    } ?: false

    val layerAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(),
        label = "layerAlpha_${layer.order.name}"
    )

    val depth = layer.order.parallaxDepth()
    val isCapeOrWings = shouldFlutterBackWearable(layer.item)
    val colorFilter = layer.dye.tintColor?.let { tint ->
        ColorFilter.tint(tint, BlendMode.SrcAtop)
    }

    Image(
        painter = painterResource(id = layer.drawableResId),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = colorFilter,
        modifier = Modifier
            .fillMaxSize()
            .alpha(layerAlpha)
            .scale(if (isEquipTarget) 1.04f else 1f)
            .graphicsLayer {
                translationX = (tilt.x * depth * 14f + if (isCapeOrWings) capeFlutter else 0f) * density
                translationY = (tilt.y * depth * 14f) * density
            }
    )
}

/** Only flexible cloth/wing assets receive lateral flutter; rigid back gear stays attached. */
internal fun shouldFlutterBackWearable(item: ItemEntity?): Boolean {
    val name = item?.name?.lowercase().orEmpty()
    val style = item?.style?.lowercase().orEmpty()
    return name.contains("cape") || name.contains("cloak") || name.contains("wing") ||
        style == "cape" || style == "cloak" || style == "wings"
}
