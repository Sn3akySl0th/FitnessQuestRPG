package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
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
import com.fitnessquest.rpg.domain.visuals.EquipmentDye
import com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry
import com.fitnessquest.rpg.domain.visuals.EquipmentVisualSpec
import com.fitnessquest.rpg.domain.visuals.PaperDollLayerOrder
import com.fitnessquest.rpg.domain.visuals.PaperDollVisualSlot
import com.fitnessquest.rpg.ui.effects.rememberDeviceTilt

/**
 * Encapsulates a resolved layer for rendering in the 5:6 paper doll stack.
 */
data class ResolvedPaperDollLayer(
    val order: PaperDollLayerOrder,
    val visualSlot: PaperDollVisualSlot?,
    val spec: EquipmentVisualSpec?,
    val drawableResId: Int?,
    val item: ItemEntity? = null,
    val isEquipped: Boolean = true,
    val rarity: GearRarity = GearRarity.COMMON,
    val dye: EquipmentDye = EquipmentDye.NATURAL
)

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
 * Compositor that renders avatar and gear layers in the standardized 14-layer Z-order stack
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
    customDyes: Map<ItemSlot, EquipmentDye> = emptyMap()
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

    // Resolve visual layers for all equipped items
    val activeLayers = remember(gear, customDyes) {
        val layers = mutableListOf<ResolvedPaperDollLayer>()
        gear.forEach { (slot, item) ->
            val spec = EquipmentVisualRegistry.resolveSpec(item)
            val resId = EquipmentVisualRegistry.findDrawableId(context, spec.layerResName)
            val resolvedDye = customDyes[slot] ?: EquipmentDye.fromItem(item)
            layers.add(
                ResolvedPaperDollLayer(
                    order = spec.layerOrder,
                    visualSlot = spec.visualSlot,
                    spec = spec,
                    drawableResId = resId,
                    item = item,
                    isEquipped = true,
                    dye = resolvedDye
                )
            )
        }
        layers.sortedBy { it.order.zIndex }
    }

    // Derive hidden visual slots from active gear before rendering (e.g. Robes suppress LEGS slot)
    val hiddenVisualSlots = remember(activeLayers) {
        activeLayers.flatMap { it.spec?.hiddenVisualSlots.orEmpty() }.toSet()
    }

    val visibleLayers = remember(activeLayers, hiddenVisualSlots) {
        activeLayers
            .filterNot { it.visualSlot in hiddenVisualSlots }
            .sortedBy { it.order.zIndex }
    }

    val hasAny2DLayer = remember(visibleLayers) {
        visibleLayers.any { it.drawableResId != null }
    }

    // Derive all covered body regions across equipped visible gear
    val hiddenBodyRegions = remember(visibleLayers) {
        visibleLayers.flatMap { it.spec?.coveredRegions.orEmpty() }.toSet()
    }

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
        if (!hasAny2DLayer) {
            // Transitional Canvas Fallback: Full dynamic vector canvas rendering
            CharacterAvatar(
                clazz = clazz,
                gear = gear,
                appearance = appearance,
                modifier = Modifier.fillMaxSize(),
                animation = animation,
                expression = expression,
                detail = AvatarDetail.FULL,
                enableBreathing = false,
                hiddenRegions = hiddenBodyRegions
            )
        } else {
            // 2D Layer Compositor (rendered strictly in z-index order 00 to 13)
            val unhandledCanvasGear = remember(gear, visibleLayers) {
                val handledSlots = visibleLayers.filter { it.drawableResId != null }.mapNotNull { it.spec?.domainSlot }.toSet()
                gear.filterKeys { it !in handledSlots }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                // Base canvas avatar (body, face, hair) with body depth parallax and region masking
                val bodyDepth = PaperDollLayerOrder.BODY_BASE.parallaxDepth()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = tilt.x * bodyDepth * 14f * density
                            translationY = tilt.y * bodyDepth * 14f * density
                        }
                ) {
                    CharacterAvatar(
                        clazz = clazz,
                        gear = unhandledCanvasGear,
                        appearance = appearance,
                        modifier = Modifier.fillMaxSize(),
                        animation = animation,
                        expression = expression,
                        detail = AvatarDetail.FULL,
                        enableBreathing = false,
                        hiddenRegions = hiddenBodyRegions
                    )
                }

                // Render each resolved 2D layer with independent 3D parallax depth, dyes, and race-proportional scaling
                val raceBuild = appearance.race.build()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = raceBuild.width
                            scaleY = raceBuild.height
                            transformOrigin = TransformOrigin(0.5f, 0.9f)
                        }
                ) {
                    visibleLayers.forEach { layer ->
                        layer.drawableResId?.let { resId ->
                            val isEquipTarget = equipAnimationState?.equippedSlot?.let {
                                layer.spec?.domainSlot == it
                            } ?: false

                            val layerAlpha by animateFloatAsState(
                                targetValue = 1f,
                                animationSpec = spring(),
                                label = "layerAlpha_${layer.order.name}"
                            )

                            val depth = layer.order.parallaxDepth()
                            val isCapeOrWings = layer.order == PaperDollLayerOrder.GEAR_BACK
                            val colorFilter = layer.dye.tintColor?.let { tint ->
                                ColorFilter.tint(tint, BlendMode.SrcAtop)
                            }

                            Image(
                                painter = painterResource(id = resId),
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
                    }
                }
            }
        }
    }
}
