package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.domain.GearRarity
import com.fitnessquest.rpg.domain.ItemCatalog
import com.fitnessquest.rpg.ui.theme.FitQuestTheme
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightSurface
import androidx.compose.ui.tooling.preview.Preview

/**
 * Returns fantasy icon silhouette for each equipment slot.
 */
fun slotPlaceholderEmoji(slot: ItemSlot): String = when (slot) {
    ItemSlot.WEAPON -> "⚔️"
    ItemSlot.HEAD -> "🪖"
    ItemSlot.CHEST -> "🛡️"
    ItemSlot.HANDS -> "🧤"
    ItemSlot.LEGS -> "👖"
    ItemSlot.FEET -> "👢"
    ItemSlot.TRINKET -> "💍"
    else -> "📦"
}

/**
 * An interactive, visual gear slot showcasing equipped items, rarity borders,
 * upgrade badges, and slot states.
 */
@Composable
fun EquipmentSlot(
    slot: ItemSlot,
    item: ItemEntity?,
    rarity: GearRarity = GearRarity.COMMON,
    upgradeLevel: Int = 0,
    isSelected: Boolean = false,
    isEquipped: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val description = buildString {
        append("${slot.label} slot: ")
        if (item != null) {
            append("${item.name}, ${rarity.displayName} tier")
            if (upgradeLevel > 0) append(", upgrade level plus $upgradeLevel")
            if (isEquipped) append(", currently equipped")
        } else {
            append("Empty")
        }
    }

    if (compact) {
        CompactEquipmentSlot(
            slot = slot,
            item = item,
            rarity = rarity,
            upgradeLevel = upgradeLevel,
            isSelected = isSelected,
            isEquipped = isEquipped,
            description = description,
            onClick = onClick,
            modifier = modifier
        )
    } else {
        DetailedEquipmentSlot(
            slot = slot,
            item = item,
            rarity = rarity,
            upgradeLevel = upgradeLevel,
            isSelected = isSelected,
            isEquipped = isEquipped,
            description = description,
            onClick = onClick,
            modifier = modifier
        )
    }
}

@Composable
fun EquipmentItemIcon(
    item: ItemEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val spec = remember(item) { com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveSpec(item) }
    val resId = remember(spec) { com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.findDrawableId(context, spec.iconResName) }

    if (resId != null) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = resId),
            contentDescription = item.name,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            modifier = modifier
        )
    } else {
        ItemIcon(item = item, modifier = modifier)
    }
}

/**
 * Compact tile style suitable for placing around the central hero paper doll.
 */
@Composable
private fun CompactEquipmentSlot(
    slot: ItemSlot,
    item: ItemEntity?,
    rarity: GearRarity,
    upgradeLevel: Int,
    isSelected: Boolean,
    isEquipped: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (item != null) {
        RarityVisuals.backgroundGlow(rarity).copy(alpha = 0.15f)
    } else {
        Color.White.copy(alpha = 0.04f)
    }

    Box(
        modifier = modifier
            .size(62.dp)
            .rarityFrame(
                rarity = if (item != null) rarity else GearRarity.COMMON,
                isSelected = isSelected,
                isEquipped = isEquipped,
                cornerRadius = 10.dp
            )
            .background(bgColor, RoundedCornerShape(10.dp))
            .clickable(onClickLabel = "Select ${slot.label} gear") { onClick() }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        if (item != null) {
            EquipmentItemIcon(item = item, modifier = Modifier.size(44.dp).padding(2.dp))

            // Upgrade badge (e.g. +3)
            if (upgradeLevel > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .background(
                            Brush.horizontalGradient(listOf(Gold, Color(0xFFD97706))),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "+$upgradeLevel",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = Color.Black
                    )
                }
            }

            // Small rarity tier indicator pill at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-2).dp)
                    .background(
                        RarityVisuals.primaryColor(rarity).copy(alpha = 0.85f),
                        RoundedCornerShape(3.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 0.5.dp)
            ) {
                Text(
                    text = slot.label.take(3).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        } else {
            // Empty Slot Placeholder
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = slotPlaceholderEmoji(slot),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = slot.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.35f)
                )
            }
        }
    }
}

/**
 * Detailed row style suitable for gear inspection and lists.
 */
@Composable
private fun DetailedEquipmentSlot(
    slot: ItemSlot,
    item: ItemEntity?,
    rarity: GearRarity,
    upgradeLevel: Int,
    isSelected: Boolean,
    isEquipped: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (item != null) {
        RarityVisuals.backgroundGlow(rarity).copy(alpha = 0.12f)
    } else {
        Color.White.copy(alpha = 0.03f)
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .rarityFrame(
                rarity = if (item != null) rarity else GearRarity.COMMON,
                isSelected = isSelected,
                isEquipped = isEquipped,
                cornerRadius = 12.dp
            )
            .semantics { contentDescription = description },
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gear icon box
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                if (item != null) {
                    EquipmentItemIcon(item = item, modifier = Modifier.size(38.dp))
                } else {
                    Text(slotPlaceholderEmoji(slot), fontSize = 20.sp)
                }

                if (upgradeLevel > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(Gold, RoundedCornerShape(3.dp))
                            .padding(horizontal = 3.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = "+$upgradeLevel",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Text Info
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = slot.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    if (item != null) {
                        RarityBadge(rarity = rarity, compact = true)
                    }
                }

                Text(
                    text = item?.name ?: "Empty ${slot.label} Slot",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item != null) Color.White else Color.White.copy(alpha = 0.35f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Stat bonus or Action
            if (item != null) {
                Column(horizontalAlignment = Alignment.End) {
                    val bonus = buildList {
                        if (item.atk > 0) add("+${item.atk} ATK")
                        if (item.def > 0) add("+${item.def} DEF")
                        if (item.hp > 0) add("+${item.hp} HP")
                    }.joinToString(" ")

                    if (bonus.isNotEmpty()) {
                        Text(
                            text = bonus,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Gold
                        )
                    }

                    if (isEquipped) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = RarityVisuals.UncommonColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Equipped",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = RarityVisuals.UncommonColor
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Tap to Equip",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Equipment Slots Showcase", showBackground = true, backgroundColor = 0xFF12131F)
@Composable
fun EquipmentSlotShowcasePreview() {
    val sampleSword = ItemCatalog.all.find { it.name == "Dragonfang Greatsword" } ?: ItemCatalog.all.first()
    val sampleArmor = ItemCatalog.all.find { it.name == "Aegis of the Titan" } ?: ItemCatalog.all.first()

    FitQuestTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EquipmentSlot(
                slot = ItemSlot.WEAPON,
                item = sampleSword,
                rarity = GearRarity.MYTHIC,
                upgradeLevel = 5,
                isEquipped = true,
                onClick = {}
            )

            EquipmentSlot(
                slot = ItemSlot.CHEST,
                item = sampleArmor,
                rarity = GearRarity.LEGENDARY,
                upgradeLevel = 3,
                isEquipped = true,
                onClick = {}
            )

            EquipmentSlot(
                slot = ItemSlot.HEAD,
                item = null,
                rarity = GearRarity.COMMON,
                upgradeLevel = 0,
                isEquipped = false,
                onClick = {}
            )
        }
    }
}

