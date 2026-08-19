package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.GearRarity
import com.fitnessquest.rpg.domain.ItemCatalog
import com.fitnessquest.rpg.ui.theme.FitQuestTheme
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.MysticPurple
import androidx.compose.ui.tooling.preview.Preview

/**
 * Centered hero paper doll character showcase with magical pedestal,
 * equipment layer pipeline, ambient auras, and equip animations.
 */
@Composable
fun HeroPaperDoll(
    clazz: CharacterClass,
    gear: Map<ItemSlot, ItemEntity>,
    appearance: AvatarAppearance,
    highestRarity: GearRarity = GearRarity.COMMON,
    equipAnimationState: EquipAnimationState? = null,
    modifier: Modifier = Modifier,
    animation: HeroAnimation = HeroAnimation.IDLE,
    expression: AvatarExpression = AvatarExpression.CALM,
    onAvatarClick: (() -> Unit)? = null
) {
    val scale = equipAnimationState?.avatarScale?.value ?: 1f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp, max = 320.dp)
            .semantics {
                contentDescription = "${clazz.label} avatar with ${gear.size} gear pieces equipped"
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Pedestal and Aura Base (Background)
        HeroPedestalBackdrop(highestRarity = highestRarity)

        // 2. High Tier Equipment Aura (Background Shimmer)
        if (highestRarity >= GearRarity.EPIC) {
            EquipmentAura(
                rarity = highestRarity,
                modifier = Modifier.size(240.dp)
            )
        }

        // 3. Layered Character Paper Doll (5:6 unified canvas)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(500f / 600f)
                .scale(scale)
                .then(
                    if (onAvatarClick != null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = "Customize Avatar"
                        ) { onAvatarClick() }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            PaperDollLayerRenderer(
                clazz = clazz,
                gear = gear,
                appearance = appearance,
                modifier = Modifier.fillMaxSize(),
                animation = animation,
                expression = expression,
                equipAnimationState = equipAnimationState
            )
        }

        // 4. Equip Burst Animation Overlay (Foreground Flash)
        equipAnimationState?.let { animState ->
            animState.equippedSlot?.let { slot ->
                ItemEquipBurstEffect(
                    animState = animState,
                    slot = slot,
                    rarity = highestRarity,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Pedestal and radiant circle rendered below the paper doll.
 */
@Composable
private fun HeroPedestalBackdrop(
    highestRarity: GearRarity,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pedestalGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pedestalAlpha"
    )

    val primaryAura = RarityVisuals.primaryColor(highestRarity)

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height * 0.88f
        val rx = size.width * 0.42f
        val ry = 18.dp.toPx()

        // 1. Pedestal Ambient Ellipse Base
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryAura.copy(alpha = 0.35f * glowAlpha),
                    primaryAura.copy(alpha = 0.08f * glowAlpha),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = rx
            ),
            topLeft = Offset(cx - rx, cy - ry),
            size = androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f)
        )

        // 2. Pedestal Edge Ring
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    primaryAura.copy(alpha = 0.5f * glowAlpha),
                    Color.White.copy(alpha = 0.7f * glowAlpha),
                    primaryAura.copy(alpha = 0.5f * glowAlpha),
                    Color.Transparent
                )
            ),
            topLeft = Offset(cx - (rx * 0.8f), cy - (ry * 0.6f)),
            size = androidx.compose.ui.geometry.Size(rx * 1.6f, ry * 1.2f),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Mythic Tier 4 Warrior Showcase", showBackground = true, backgroundColor = 0xFF12131F)
@Composable
fun HeroPaperDollMythicWarriorPreview() {
    val tier4Gear = mapOf(
        ItemSlot.WEAPON to (ItemCatalog.all.find { it.name == "Dragonfang Greatsword" } ?: ItemCatalog.all.first()),
        ItemSlot.CHEST to (ItemCatalog.all.find { it.name == "Aegis of the Titan" } ?: ItemCatalog.all.first()),
        ItemSlot.HEAD to (ItemCatalog.all.find { it.name == "Titanforged Helm" } ?: ItemCatalog.all.first())
    )

    FitQuestTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HeroPaperDoll(
                clazz = CharacterClass.WARRIOR,
                gear = tier4Gear,
                appearance = AvatarAppearance(),
                highestRarity = GearRarity.MYTHIC,
                expression = AvatarExpression.VICTORIOUS,
                modifier = Modifier.height(300.dp)
            )
        }
    }
}

@Preview(name = "Legendary Tier 4 Mage Showcase", showBackground = true, backgroundColor = 0xFF12131F)
@Composable
fun HeroPaperDollLegendaryMagePreview() {
    val mageGear = mapOf(
        ItemSlot.WEAPON to (ItemCatalog.all.find { it.name == "Archmage's Scepter" } ?: ItemCatalog.all.first()),
        ItemSlot.CHEST to (ItemCatalog.all.find { it.name == "Vestments of the Cosmos" } ?: ItemCatalog.all.first())
    )

    FitQuestTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HeroPaperDoll(
                clazz = CharacterClass.MAGE,
                gear = mageGear,
                appearance = AvatarAppearance(),
                highestRarity = GearRarity.LEGENDARY,
                expression = AvatarExpression.CALM,
                modifier = Modifier.height(300.dp)
            )
        }
    }
}

@Preview(name = "Epic Tier 3 Paladin Showcase", showBackground = true, backgroundColor = 0xFF12131F)
@Composable
fun HeroPaperDollEpicPaladinPreview() {
    val paladinGear = mapOf(
        ItemSlot.WEAPON to (ItemCatalog.all.find { it.name == "Radiant Crusader Blade" } ?: ItemCatalog.all.first()),
        ItemSlot.CHEST to (ItemCatalog.all.find { it.name == "Steel Plate" } ?: ItemCatalog.all.first())
    )

    FitQuestTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HeroPaperDoll(
                clazz = CharacterClass.PALADIN,
                gear = paladinGear,
                appearance = AvatarAppearance(),
                highestRarity = GearRarity.EPIC,
                expression = AvatarExpression.CALM,
                modifier = Modifier.height(300.dp)
            )
        }
    }
}
