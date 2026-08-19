package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.GearRarity
import com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry
import com.fitnessquest.rpg.domain.visuals.EquipmentVisualSpec
import com.fitnessquest.rpg.domain.visuals.PaperDollLayerOrder
import com.fitnessquest.rpg.domain.visuals.PaperDollVisualSlot

/**
 * Encapsulates a resolved layer for rendering in the 5:6 paper doll stack.
 */
data class ResolvedPaperDollLayer(
    val order: PaperDollLayerOrder,
    val visualSlot: PaperDollVisualSlot?,
    val spec: EquipmentVisualSpec?,
    val drawableResId: Int?,
    val isEquipped: Boolean = true,
    val rarity: GearRarity = GearRarity.COMMON
)

/**
 * Compositor that renders avatar and gear layers in the standardized 14-layer Z-order stack.
 *
 * Master Canvas Rules:
 * - 5:6 master canvas ratio (500 x 600 px logical bounds at xhdpi).
 * - Proportional uniform scaling.
 * - Automatic fallback to [CharacterAvatar] canvas rendering when 2D PNG/XML layers are not present.
 */
@Composable
fun PaperDollLayerRenderer(
    clazz: CharacterClass,
    gear: Map<ItemSlot, ItemEntity>,
    appearance: AvatarAppearance,
    modifier: Modifier = Modifier,
    animation: HeroAnimation = HeroAnimation.IDLE,
    expression: AvatarExpression = AvatarExpression.CALM,
    equipAnimationState: EquipAnimationState? = null
) {
    val context = LocalContext.current

    // Resolve visual layers for all equipped items
    val activeLayers = remember(gear) {
        val layers = mutableListOf<ResolvedPaperDollLayer>()
        gear.forEach { (slot, item) ->
            val spec = EquipmentVisualRegistry.resolveSpec(item)
            val resId = EquipmentVisualRegistry.findDrawableId(context, spec.layerResName)
            layers.add(
                ResolvedPaperDollLayer(
                    order = spec.layerOrder,
                    visualSlot = spec.visualSlot,
                    spec = spec,
                    drawableResId = resId,
                    isEquipped = true
                )
            )
        }
        layers.sortedBy { it.order.zIndex }
    }

    val hasAny2DLayer = remember(activeLayers) {
        activeLayers.any { it.drawableResId != null }
    }

    Box(
        modifier = modifier
            .aspectRatio(500f / 600f)
            .fillMaxSize(),
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
                detail = AvatarDetail.FULL
            )
        } else {
            // 2D Layer Compositor (rendered strictly in z-index order 00 to 13)
            Box(modifier = Modifier.fillMaxSize()) {
                // Render base canvas avatar behind 2D wear layers if partial 2D coverage
                CharacterAvatar(
                    clazz = clazz,
                    gear = emptyMap(), // Clean base body
                    appearance = appearance,
                    modifier = Modifier.fillMaxSize(),
                    animation = animation,
                    expression = expression,
                    detail = AvatarDetail.FULL
                )

                // Render each resolved 2D layer
                activeLayers.forEach { layer ->
                    layer.drawableResId?.let { resId ->
                        val isEquipTarget = equipAnimationState?.equippedSlot?.let {
                            layer.spec?.domainSlot == it
                        } ?: false

                        val layerAlpha by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(),
                            label = "layerAlpha_${layer.order.name}"
                        )

                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(layerAlpha)
                                .scale(if (isEquipTarget) 1.04f else 1f)
                        )
                    }
                }
            }
        }
    }
}
