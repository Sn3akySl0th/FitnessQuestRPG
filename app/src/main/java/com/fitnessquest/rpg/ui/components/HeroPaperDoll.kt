package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.GearRarity
import com.fitnessquest.rpg.domain.ItemCatalog
import com.fitnessquest.rpg.ui.theme.FitQuestTheme
import com.fitnessquest.rpg.ui.theme.Gold
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

/**
 * Centered hero paper doll character showcase with magical pedestal,
 * equipment layer pipeline, ambient auras, 3D parallax depth, equip animations,
 * and interactive 360-degree rotation view toggle.
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
    facingBack: Boolean? = null,
    showRotateButton: Boolean = true,
    onRotateToggle: ((Boolean) -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val tapSquash = remember { Animatable(1f) }
    val tapStretch = remember { Animatable(1f) }

    var internalFacingBack by remember { mutableStateOf(false) }
    val isFacingBack = facingBack ?: internalFacingBack

    // Smooth turn-around rotation angle
    val rotationAngle by animateFloatAsState(
        targetValue = if (isFacingBack) 180f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "avatarTurnaround"
    )

    val baseScale = equipAnimationState?.avatarScale?.value ?: 1f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp, max = 320.dp)
            .semantics {
                contentDescription = "${clazz.label} avatar with ${gear.size} gear pieces equipped, ${if (isFacingBack) "facing back" else "facing front"}"
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

        // 3. Layered Character Paper Doll (5:6 unified canvas with Squash & Stretch Tap Physics + Turnaround)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(500f / 600f)
                .scale(scaleX = baseScale * tapStretch.value, scaleY = baseScale * tapSquash.value)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Interact with Avatar"
                ) {
                    coroutineScope.launch {
                        // Organic spring squash & stretch recoil
                        tapSquash.animateTo(
                            targetValue = 0.93f,
                            animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing)
                        )
                        tapStretch.animateTo(
                            targetValue = 1.06f,
                            animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing)
                        )
                        launch {
                            tapSquash.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                        launch {
                            tapStretch.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    }
                    onAvatarClick?.invoke()
                },
            contentAlignment = Alignment.Center
        ) {
            PaperDollLayerRenderer(
                clazz = clazz,
                gear = gear,
                appearance = appearance,
                modifier = Modifier.fillMaxSize(),
                animation = animation,
                expression = expression,
                facingBack = isFacingBack,
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

        // 5. Interactive Rotation Arrow Button (Front/Back View Toggle)
        if (showRotateButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.2.dp, Gold.copy(alpha = 0.55f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = if (isFacingBack) "View front of hero" else "View back of hero"
                    ) {
                        val newFacing = !isFacingBack
                        internalFacingBack = newFacing
                        onRotateToggle?.invoke(newFacing)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = if (isFacingBack) "Rotate to front" else "Rotate to back",
                    tint = Gold,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer(rotationZ = rotationAngle)
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

@Preview(name = "Mythic Tier 4 Warrior Full 5-Piece Showcase", showBackground = true, backgroundColor = 0xFF12131F)
@Composable
fun HeroPaperDollMythicWarriorPreview() {
    val tier4Gear = mapOf(
        ItemSlot.WEAPON to (ItemCatalog.all.find { it.name == "Dragonfang Greatsword" } ?: ItemCatalog.all.first()),
        ItemSlot.CHEST to (ItemCatalog.all.find { it.name == "Aegis of the Titan" } ?: ItemCatalog.all.first()),
        ItemSlot.HEAD to (ItemCatalog.all.find { it.name == "Mythic Wayfarer Crown" } ?: ItemCatalog.all.first()),
        ItemSlot.LEGS to (ItemCatalog.all.find { it.name == "Mythic Wayfarer Greaves" } ?: ItemCatalog.all.first()),
        ItemSlot.FEET to (ItemCatalog.all.find { it.name == "Mythic Wayfarer Boots" } ?: ItemCatalog.all.first())
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

@Preview(name = "Tier 3 Knight Veteran Full 5-Piece Showcase", showBackground = true, backgroundColor = 0xFF12131F)
@Composable
fun HeroPaperDollTier3KnightPreview() {
    val tier3Gear = mapOf(
        ItemSlot.WEAPON to (ItemCatalog.all.find { it.name == "Knight's Blade" } ?: ItemCatalog.all.first()),
        ItemSlot.CHEST to (ItemCatalog.all.find { it.name == "Steel Plate" } ?: ItemCatalog.all.first()),
        ItemSlot.HEAD to (ItemCatalog.all.find { it.name == "Veteran's Coif" } ?: ItemCatalog.all.first()),
        ItemSlot.LEGS to (ItemCatalog.all.find { it.name == "Veteran's Greaves" } ?: ItemCatalog.all.first()),
        ItemSlot.FEET to (ItemCatalog.all.find { it.name == "Veteran's Treads" } ?: ItemCatalog.all.first())
    )

    FitQuestTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HeroPaperDoll(
                clazz = CharacterClass.PALADIN,
                gear = tier3Gear,
                appearance = AvatarAppearance(),
                highestRarity = GearRarity.RARE,
                expression = AvatarExpression.CALM,
                modifier = Modifier.height(300.dp)
            )
        }
    }
}

@Preview(name = "Tier 2 Adventurer Ranger Full 5-Piece Showcase", showBackground = true, backgroundColor = 0xFF12131F)
@Composable
fun HeroPaperDollTier2AdventurerPreview() {
    val tier2Gear = mapOf(
        ItemSlot.WEAPON to (ItemCatalog.all.find { it.name == "Training Shortbow" } ?: ItemCatalog.all.first()),
        ItemSlot.CHEST to (ItemCatalog.all.find { it.name == "Supple Leathers" } ?: ItemCatalog.all.first()),
        ItemSlot.HEAD to (ItemCatalog.all.find { it.name == "Adventurer's Hood" } ?: ItemCatalog.all.first()),
        ItemSlot.LEGS to (ItemCatalog.all.find { it.name == "Adventurer's Legguards" } ?: ItemCatalog.all.first()),
        ItemSlot.FEET to (ItemCatalog.all.find { it.name == "Adventurer's Boots" } ?: ItemCatalog.all.first())
    )

    FitQuestTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HeroPaperDoll(
                clazz = CharacterClass.RANGER,
                gear = tier2Gear,
                appearance = AvatarAppearance(),
                highestRarity = GearRarity.UNCOMMON,
                expression = AvatarExpression.BATTLE_READY,
                modifier = Modifier.height(300.dp)
            )
        }
    }
}

@Preview(name = "Tier 1 Novice Adventurer Full 5-Piece Showcase", showBackground = true, backgroundColor = 0xFF12131F)
@Composable
fun HeroPaperDollTier1NovicePreview() {
    val tier1Gear = mapOf(
        ItemSlot.WEAPON to (ItemCatalog.all.find { it.name == "Rusty Sword" } ?: ItemCatalog.all.first()),
        ItemSlot.CHEST to (ItemCatalog.all.find { it.name == "Padded Vest" } ?: ItemCatalog.all.first()),
        ItemSlot.HEAD to (ItemCatalog.all.find { it.name == "Leather Cap" } ?: ItemCatalog.all.first()),
        ItemSlot.LEGS to (ItemCatalog.all.find { it.name == "Worn Trousers" } ?: ItemCatalog.all.first()),
        ItemSlot.FEET to (ItemCatalog.all.find { it.name == "Old Boots" } ?: ItemCatalog.all.first())
    )

    FitQuestTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HeroPaperDoll(
                clazz = CharacterClass.WARRIOR,
                gear = tier1Gear,
                appearance = AvatarAppearance(),
                highestRarity = GearRarity.COMMON,
                expression = AvatarExpression.CALM,
                modifier = Modifier.height(300.dp)
            )
        }
    }
}

@Preview(name = "Dragoon with Sky Wyvern Showcase", showBackground = true, backgroundColor = 0xFF12131F)
@Composable
fun HeroPaperDollDragoonWyvernPreview() {
    val dragoonGear = mapOf(
        ItemSlot.WEAPON to (ItemCatalog.all.find { it.name == "Worn Harpoon" } ?: ItemCatalog.all.first()),
        ItemSlot.CHEST to (ItemCatalog.all.find { it.name == "Chainmail Shirt" } ?: ItemCatalog.all.first()),
        ItemSlot.LEGS to (ItemCatalog.all.find { it.name == "Adventurer's Legguards" } ?: ItemCatalog.all.first()),
        ItemSlot.FEET to (ItemCatalog.all.find { it.name == "Adventurer's Boots" } ?: ItemCatalog.all.first())
    )

    FitQuestTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HeroPaperDoll(
                clazz = CharacterClass.DRAGOON,
                gear = dragoonGear,
                appearance = AvatarAppearance(druidForm = "WYVERN"),
                highestRarity = GearRarity.UNCOMMON,
                expression = AvatarExpression.BATTLE_READY,
                modifier = Modifier.height(300.dp)
            )
        }
    }
}

@Preview(name = "Necromancer with Soulflayer & Undead Army", showBackground = true, backgroundColor = 0xFF12131F)
@Composable
fun HeroPaperDollNecromancerArmyPreview() {
    val necroGear = mapOf(
        ItemSlot.WEAPON to (ItemCatalog.all.find { it.name == "Soulflayer" } ?: ItemCatalog.all.first()),
        ItemSlot.CHEST to (ItemCatalog.all.find { it.name == "Runeweave Robe" } ?: ItemCatalog.all.first())
    )

    FitQuestTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HeroPaperDoll(
                clazz = CharacterClass.NECROMANCER,
                gear = necroGear,
                appearance = AvatarAppearance(druidForm = "ARMY"),
                highestRarity = GearRarity.RARE,
                expression = AvatarExpression.FOCUSED,
                modifier = Modifier.height(300.dp)
            )
        }
    }
}

@Preview(name = "Summoner with Primal Spirit Shiva", showBackground = true, backgroundColor = 0xFF12131F)
@Composable
fun HeroPaperDollSummonerShivaPreview() {
    val summonerGear = mapOf(
        ItemSlot.WEAPON to (ItemCatalog.all.find { it.name == "Novice Horn" } ?: ItemCatalog.all.first()),
        ItemSlot.CHEST to (ItemCatalog.all.find { it.name == "Apprentice Robes" } ?: ItemCatalog.all.first())
    )

    FitQuestTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HeroPaperDoll(
                clazz = CharacterClass.SUMMONER,
                gear = summonerGear,
                appearance = AvatarAppearance(druidForm = "SHIVA"),
                highestRarity = GearRarity.RARE,
                expression = AvatarExpression.CALM,
                modifier = Modifier.height(300.dp)
            )
        }
    }
}
