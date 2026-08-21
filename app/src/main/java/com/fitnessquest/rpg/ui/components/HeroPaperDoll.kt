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
import com.fitnessquest.rpg.domain.ItemStyle
import com.fitnessquest.rpg.ui.theme.FitQuestTheme
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.domain.CharacterRace
import androidx.compose.ui.tooling.preview.Preview
import com.fitnessquest.rpg.domain.visuals.AvatarOrientation
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos

/**
 * State holder for the avatar turnaround transition.
 * Manages cancellable 3D compression, midpoint orientation swapping,
 * and rapid tap reversal.
 */
@Stable
class AvatarTurnaroundState(
    initialFacingBack: Boolean = false,
    val isReducedMotion: Boolean = false
) {
    // Continuous turnaround angle in degrees: 0f = Front, 180f = Back
    val angleAnimatable = Animatable(if (initialFacingBack) 180f else 0f)

    val currentAngle: Float get() = angleAnimatable.value

    // Horizontal scale: compresses to 0 at 90 degrees (midpoint), expands to 1 at 0 and 180
    val scaleX: Float
        get() {
            if (isReducedMotion) return 1f
            val rad = Math.toRadians(currentAngle.toDouble()).toFloat()
            return abs(cos(rad)).coerceIn(0.01f, 1f)
        }

    // Midpoint orientation swap: changes rendered facing at angle = 90 degrees
    val renderedOrientation: AvatarOrientation
        get() = if (currentAngle >= 90f) AvatarOrientation.BACK else AvatarOrientation.FRONT

    val renderedFacingBack: Boolean
        get() = renderedOrientation.isFacingBack

    suspend fun animateTo(targetFacingBack: Boolean) {
        val targetAngle = if (targetFacingBack) 180f else 0f
        if (isReducedMotion) {
            angleAnimatable.snapTo(targetAngle)
        } else {
            angleAnimatable.animateTo(
                targetValue = targetAngle,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }
    }
}

/**
 * Respects system motion duration scale / reduced motion accessibility preferences.
 */
@Composable
fun rememberIsReducedMotion(overrideReducedMotion: Boolean = false): Boolean {
    if (overrideReducedMotion) return true
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        try {
            val scale = android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        } catch (_: Throwable) {
            false
        }
    }
}

@Composable
fun rememberAvatarTurnaroundState(
    initialFacingBack: Boolean = false,
    reducedMotion: Boolean = rememberIsReducedMotion()
): AvatarTurnaroundState {
    return remember(reducedMotion) {
        AvatarTurnaroundState(
            initialFacingBack = initialFacingBack,
            isReducedMotion = reducedMotion
        )
    }
}

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

    val isReducedMotion = rememberIsReducedMotion()
    var internalFacingBack by remember { mutableStateOf(false) }
    val effectiveTargetFacing = facingBack ?: internalFacingBack

    val turnaroundState = rememberAvatarTurnaroundState(
        initialFacingBack = effectiveTargetFacing,
        reducedMotion = isReducedMotion
    )

    LaunchedEffect(effectiveTargetFacing) {
        turnaroundState.animateTo(effectiveTargetFacing)
    }

    val baseScale = equipAnimationState?.avatarScale?.value ?: 1f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp, max = 320.dp)
            .semantics {
                contentDescription = "${clazz.label} avatar with ${gear.size} gear pieces equipped, ${if (effectiveTargetFacing) "facing back" else "facing front"}"
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

        // 3. Layered Character Paper Doll (5:6 unified canvas with Squash & Stretch Tap Physics + Horizontal Turnaround)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(500f / 600f)
                .scale(
                    scaleX = baseScale * tapStretch.value * turnaroundState.scaleX,
                    scaleY = baseScale * tapSquash.value
                )
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
                facingBack = turnaroundState.renderedFacingBack,
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
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 10.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Gold.copy(alpha = 0.55f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = if (effectiveTargetFacing) "View front of hero" else "View back of hero"
                    ) {
                        val nextFacing = !effectiveTargetFacing
                        if (facingBack == null) {
                            internalFacingBack = nextFacing
                        }
                        onRotateToggle?.invoke(nextFacing)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = if (effectiveTargetFacing) "Rotate to front" else "Rotate to back",
                    tint = Gold,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer(rotationZ = turnaroundState.currentAngle)
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

// ---------------------------------------------------------------------------
// Checkpoint 2 Visual QA Paired Front/Back Previews
// ---------------------------------------------------------------------------

@Preview(name = "QA 1: Human Warrior - Plate + Shield (Front & Back)", showBackground = true, backgroundColor = 0xFF12131F, widthDp = 420, heightDp = 320)
@Composable
fun HeroPaperDollPlateAndShieldPreview() {
    val gear = mapOf(
        ItemSlot.WEAPON to (ItemCatalog.all.find { it.name == "Knight's Blade" }
            ?: ItemEntity(id = 1193, name = "Knight's Blade", slot = ItemSlot.WEAPON, style = ItemStyle.SWORD, tier = 3, emoji = "⚔️", price = 250)),
        ItemSlot.CHEST to (ItemCatalog.all.find { it.name == "Steel Plate" }
            ?: ItemEntity(id = 1194, name = "Steel Plate", slot = ItemSlot.CHEST, style = ItemStyle.PLATE, tier = 3, emoji = "🛡️", price = 250)),
        ItemSlot.HEAD to (ItemCatalog.all.find { it.name == "Iron Greathelm" }
            ?: ItemEntity(id = 1195, name = "Iron Greathelm", slot = ItemSlot.HEAD, style = ItemStyle.PLATE, tier = 2, emoji = "🪖", price = 150)),
        ItemSlot.LEGS to (ItemCatalog.all.find { it.name == "Steel Greaves" }
            ?: ItemEntity(id = 1191, name = "Steel Greaves", slot = ItemSlot.LEGS, style = ItemStyle.PLATE, tier = 3, emoji = "🦿", price = 180)),
        ItemSlot.FEET to (ItemCatalog.all.find { it.name == "Steel Sabatons" }
            ?: ItemEntity(id = 1192, name = "Steel Sabatons", slot = ItemSlot.FEET, style = ItemStyle.PLATE, tier = 3, emoji = "🥾", price = 140)),
        ItemSlot.TRINKET to (ItemCatalog.all.find { it.name == "Iron Back Shield" }
            ?: ItemEntity(id = 1196, name = "Iron Back Shield", slot = ItemSlot.TRINKET, style = "shield", tier = 2, emoji = "🛡️", price = 150))
    )

    FitQuestTheme {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HeroPaperDoll(
                clazz = CharacterClass.WARRIOR,
                gear = gear,
                appearance = AvatarAppearance(race = CharacterRace.HUMAN),
                expression = AvatarExpression.BATTLE_READY,
                facingBack = false,
                showRotateButton = false,
                modifier = Modifier.weight(1f).height(280.dp)
            )
            HeroPaperDoll(
                clazz = CharacterClass.WARRIOR,
                gear = gear,
                appearance = AvatarAppearance(race = CharacterRace.HUMAN),
                expression = AvatarExpression.BATTLE_READY,
                facingBack = true,
                showRotateButton = false,
                modifier = Modifier.weight(1f).height(280.dp)
            )
        }
    }
}

@Preview(name = "QA 2: Elf Ranger - Leather + Quiver (Front & Back)", showBackground = true, backgroundColor = 0xFF12131F, widthDp = 420, heightDp = 320)
@Composable
fun HeroPaperDollLeatherAndQuiverPreview() {
    val gear = mapOf(
        ItemSlot.WEAPON to (ItemCatalog.all.find { it.name == "Training Shortbow" }
            ?: ItemEntity(id = 1181, name = "Training Shortbow", slot = ItemSlot.WEAPON, style = ItemStyle.BOW, tier = 1, emoji = "🏹", price = 50)),
        ItemSlot.CHEST to (ItemCatalog.all.find { it.name == "Supple Leathers" }
            ?: ItemEntity(id = 1182, name = "Supple Leathers", slot = ItemSlot.CHEST, style = ItemStyle.LIGHT, tier = 2, emoji = "🥋", price = 100)),
        ItemSlot.HEAD to (ItemCatalog.all.find { it.name == "Adventurer's Hood" }
            ?: ItemEntity(id = 1183, name = "Adventurer's Hood", slot = ItemSlot.HEAD, style = ItemStyle.LIGHT, classAffinity = CharacterClass.RANGER, tier = 2, emoji = "🏹", price = 100)),
        ItemSlot.LEGS to ItemEntity(id = 1184, name = "Supple Leathers Trousers", slot = ItemSlot.LEGS, style = ItemStyle.LIGHT, tier = 2, emoji = "👖", price = 80),
        ItemSlot.FEET to ItemEntity(id = 1185, name = "Supple Leathers Boots", slot = ItemSlot.FEET, style = ItemStyle.LIGHT, tier = 2, emoji = "🥾", price = 70),
        ItemSlot.TRINKET to ItemEntity(id = 1197, name = "Hunter's Quiver", slot = ItemSlot.TRINKET, style = "quiver", tier = 1, emoji = "🏹", price = 90)
    )

    FitQuestTheme {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HeroPaperDoll(
                clazz = CharacterClass.RANGER,
                gear = gear,
                appearance = AvatarAppearance(race = CharacterRace.ELF),
                expression = AvatarExpression.FOCUSED,
                facingBack = false,
                showRotateButton = false,
                modifier = Modifier.weight(1f).height(280.dp)
            )
            HeroPaperDoll(
                clazz = CharacterClass.RANGER,
                gear = gear,
                appearance = AvatarAppearance(race = CharacterRace.ELF),
                expression = AvatarExpression.FOCUSED,
                facingBack = true,
                showRotateButton = false,
                modifier = Modifier.weight(1f).height(280.dp)
            )
        }
    }
}

@Preview(name = "QA 3: Undead Mage - Robe + Cape (Front & Back)", showBackground = true, backgroundColor = 0xFF12131F, widthDp = 420, heightDp = 320)
@Composable
fun HeroPaperDollRobeAndCapePreview() {
    val gear = mapOf(
        ItemSlot.WEAPON to (ItemCatalog.all.find { it.name == "Apprentice Wand" }
            ?: ItemEntity(id = 1186, name = "Apprentice Wand", slot = ItemSlot.WEAPON, style = ItemStyle.WAND, classAffinity = CharacterClass.MAGE, tier = 1, emoji = "🪄", price = 40)),
        ItemSlot.CHEST to (ItemCatalog.all.find { it.name == "Runeweave Robe" }
            ?: ItemEntity(id = 1187, name = "Runeweave Robe", slot = ItemSlot.CHEST, style = ItemStyle.ROBE, classAffinity = CharacterClass.MAGE, tier = 3, emoji = "🥋", price = 300)),
        ItemSlot.TRINKET to ItemEntity(id = 1198, name = "Velvet Cape", slot = ItemSlot.TRINKET, style = "cape", tier = 3, emoji = "🧣", price = 200)
    )

    FitQuestTheme {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HeroPaperDoll(
                clazz = CharacterClass.MAGE,
                gear = gear,
                appearance = AvatarAppearance(race = CharacterRace.UNDEAD),
                expression = AvatarExpression.CALM,
                facingBack = false,
                showRotateButton = false,
                modifier = Modifier.weight(1f).height(280.dp)
            )
            HeroPaperDoll(
                clazz = CharacterClass.MAGE,
                gear = gear,
                appearance = AvatarAppearance(race = CharacterRace.UNDEAD),
                expression = AvatarExpression.CALM,
                facingBack = true,
                showRotateButton = false,
                modifier = Modifier.weight(1f).height(280.dp)
            )
        }
    }
}

@Preview(name = "QA 4: Dwarf Paladin - Robe + Wings (Front & Back)", showBackground = true, backgroundColor = 0xFF12131F, widthDp = 420, heightDp = 320)
@Composable
fun HeroPaperDollRobeAndWingsPreview() {
    val gear = mapOf(
        ItemSlot.WEAPON to (ItemCatalog.all.find { it.name == "Knight's Blade" } ?: ItemCatalog.all.first()),
        ItemSlot.CHEST to (ItemCatalog.all.find { it.name == "Apprentice Robes" } ?: ItemCatalog.all.first()),
        ItemSlot.TRINKET to ItemEntity(id = 1199, name = "Celestial Wings", slot = ItemSlot.TRINKET, style = "wings", tier = 4, emoji = "🪽", price = 1000)
    )

    FitQuestTheme {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HeroPaperDoll(
                clazz = CharacterClass.PALADIN,
                gear = gear,
                appearance = AvatarAppearance(race = CharacterRace.DWARF),
                expression = AvatarExpression.VICTORIOUS,
                facingBack = false,
                showRotateButton = false,
                modifier = Modifier.weight(1f).height(280.dp)
            )
            HeroPaperDoll(
                clazz = CharacterClass.PALADIN,
                gear = gear,
                appearance = AvatarAppearance(race = CharacterRace.DWARF),
                expression = AvatarExpression.VICTORIOUS,
                facingBack = true,
                showRotateButton = false,
                modifier = Modifier.weight(1f).height(280.dp)
            )
        }
    }
}

@Preview(name = "QA 5: Orc Barbarian - Mixed Gear (Front & Back)", showBackground = true, backgroundColor = 0xFF12131F, widthDp = 420, heightDp = 320)
@Composable
fun HeroPaperDollMixedGearOrcPreview() {
    val gear = mapOf(
        ItemSlot.WEAPON to (ItemCatalog.all.find { it.name == "Dragonfang Greatsword" } ?: ItemCatalog.all.first()),
        ItemSlot.CHEST to (ItemCatalog.all.find { it.name == "Padded Vest" } ?: ItemCatalog.all.first()),
        ItemSlot.HEAD to (ItemCatalog.all.find { it.name == "Leather Cap" } ?: ItemCatalog.all.first { it.slot == ItemSlot.HEAD }),
        ItemSlot.LEGS to (ItemCatalog.all.find { it.name == "Veteran's Greaves" } ?: ItemCatalog.all.first()),
        ItemSlot.FEET to (ItemCatalog.all.find { it.name == "Old Boots" } ?: ItemCatalog.all.first())
    )

    FitQuestTheme {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HeroPaperDoll(
                clazz = CharacterClass.WARRIOR,
                gear = gear,
                appearance = AvatarAppearance(race = CharacterRace.ORC),
                expression = AvatarExpression.BATTLE_READY,
                facingBack = false,
                showRotateButton = false,
                modifier = Modifier.weight(1f).height(280.dp)
            )
            HeroPaperDoll(
                clazz = CharacterClass.WARRIOR,
                gear = gear,
                appearance = AvatarAppearance(race = CharacterRace.ORC),
                expression = AvatarExpression.BATTLE_READY,
                facingBack = true,
                showRotateButton = false,
                modifier = Modifier.weight(1f).height(280.dp)
            )
        }
    }
}

@Preview(name = "QA 6: Druid Transformed Bear Form (Equipment Suppressed)", showBackground = true, backgroundColor = 0xFF12131F, widthDp = 420, heightDp = 320)
@Composable
fun HeroPaperDollDruidBearPreview() {
    val gear = mapOf(
        ItemSlot.WEAPON to (ItemCatalog.all.find { it.name == "Dragonfang Greatsword" } ?: ItemCatalog.all.first()),
        ItemSlot.CHEST to (ItemCatalog.all.find { it.name == "Steel Plate" } ?: ItemCatalog.all.first())
    )

    FitQuestTheme {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HeroPaperDoll(
                clazz = CharacterClass.DRUID,
                gear = gear,
                appearance = AvatarAppearance(druidForm = "BEAR"),
                expression = AvatarExpression.BATTLE_READY,
                facingBack = false,
                showRotateButton = false,
                modifier = Modifier.weight(1f).height(280.dp)
            )
            HeroPaperDoll(
                clazz = CharacterClass.DRUID,
                gear = gear,
                appearance = AvatarAppearance(druidForm = "BEAR"),
                expression = AvatarExpression.BATTLE_READY,
                facingBack = true,
                showRotateButton = false,
                modifier = Modifier.weight(1f).height(280.dp)
            )
        }
    }
}
