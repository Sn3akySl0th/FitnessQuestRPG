package com.fitnessquest.rpg.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitnessquest.rpg.data.db.GearInstanceEntity
import androidx.compose.ui.unit.sp
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.itemBonusText
import com.fitnessquest.rpg.domain.GearComparison
import com.fitnessquest.rpg.domain.GearRarity
import com.fitnessquest.rpg.ui.theme.Gold

@Composable
fun GearItemBadge(
    item: ItemEntity,
    rarity: GearRarity = GearRarity.COMMON,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = if (rarity != GearRarity.COMMON) rarity.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (rarity != GearRarity.COMMON) rarity.color else Color.Transparent),
        modifier = modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            EquipmentItemIcon(item = item, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
fun GearCompactTile(
    item: ItemEntity,
    instance: GearInstanceEntity?,
    rarity: GearRarity,
    comparison: GearComparison.Comparison?,
    filledSocketCount: Int,
    maxSockets: Int,
    equipped: Boolean,
    runeCatalog: Map<Long, ItemEntity> = emptyMap(),
    selected: Boolean = false,
    forgeMode: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = when {
        selected -> MaterialTheme.colorScheme.primary
        equipped -> Gold
        rarity != GearRarity.COMMON -> rarity.color
        item.tier >= 4 -> Color(0xFFF0C040)
        item.tier == 3 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 116.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (rarity != GearRarity.COMMON) rarity.color.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected || equipped || rarity != GearRarity.COMMON) 1.5.dp else 1.dp, border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GearItemBadge(item = item, rarity = rarity)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (rarity != GearRarity.COMMON) rarity.color else Color.White
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${item.slot.label} · T${item.tier}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    if (maxSockets > 0) {
                        GearSocketDots(filled = filledSocketCount, total = maxSockets)
                    }
                }
                val effectiveStats = GearComparison.breakdown(item, instance, runeCatalog)
                val stats = if (instance != null) {
                    buildList {
                        if (effectiveStats.atk > 0) add("+${effectiveStats.atk} ATK")
                        if (effectiveStats.def > 0) add("+${effectiveStats.def} DEF")
                        if (effectiveStats.hp > 0) add("+${effectiveStats.hp} HP")
                    }.joinToString("  ")
                } else {
                    itemBonusText(item)
                }
                Text(
                    stats.ifBlank { item.description },
                    style = MaterialTheme.typography.labelSmall,
                    color = Gold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (equipped) {
                    Icon(Icons.Filled.Check, contentDescription = "Equipped", tint = Gold, modifier = Modifier.size(18.dp))
                }
                if (forgeMode && selected) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Selected for forge",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(14.dp)
                        )
                    }
                } else if (
                    !equipped &&
                    comparison != null &&
                    comparison.verdict in setOf(
                        GearComparison.Verdict.UPGRADE,
                        GearComparison.Verdict.DOWNGRADE,
                    )
                ) {
                    GearPowerVerdict(comparison)
                }
            }
        }
    }
}

@Composable
fun GearSocketDots(filled: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            "◇",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF9C7BE3).copy(alpha = 0.85f),
            fontSize = 10.sp,
        )
        repeat(total) { index ->
            val active = index < filled
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) Color(0xFF9C7BE3) else Color.White.copy(alpha = 0.12f)
                    )
                    .border(
                        1.5.dp,
                        if (active) Color(0xFFC4B5FD) else Color(0xFF9C7BE3).copy(alpha = 0.55f),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
fun GearPowerVerdict(comparison: GearComparison.Comparison, compact: Boolean = true) {
    val (color, icon) = when (comparison.verdict) {
        GearComparison.Verdict.UPGRADE -> Color(0xFF4ADE80) to Icons.Filled.ArrowUpward
        GearComparison.Verdict.DOWNGRADE -> Color(0xFFF87171) to Icons.Filled.ArrowDownward
        GearComparison.Verdict.SIDEGRADE -> Color.Gray to Icons.Filled.Remove
        GearComparison.Verdict.NO_BASELINE -> Color.Gray to Icons.Filled.Remove
    }
    val label = when (comparison.verdict) {
        GearComparison.Verdict.NO_BASELINE -> null
        GearComparison.Verdict.SIDEGRADE -> null
        else -> if (comparison.powerDelta >= 0) "+${comparison.powerDelta}" else "${comparison.powerDelta}"
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = if (compact) 11.sp else 12.sp
                )
            }
        }
    }
}

@Composable
fun GearStatDeltaRow(
    label: String,
    candidateValue: Int,
    delta: Int,
    modifier: Modifier = Modifier,
) {
    val color = when {
        delta > 0 -> Color(0xFF4ADE80)
        delta < 0 -> Color(0xFFF87171)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val prefix = if (delta > 0) "+" else ""
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(candidateValue.toString(), style = MaterialTheme.typography.bodyMedium)
            if (delta != 0) {
                Text(
                    "$prefix$delta",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
fun GearStatBreakdownSection(
    breakdown: GearComparison.DetailedStatBreakdown,
    upgradeMaterialName: String,
    modifier: Modifier = Modifier,
) {
    val bonusColor = Color(0xFF4ADE80)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GearStatTotalWithParts(
            label = "ATK",
            total = breakdown.totalAtk,
            base = breakdown.baseAtk,
            upgrade = breakdown.upgradeAtk,
            rune = breakdown.runeAtk,
            socketedRunes = breakdown.socketedRunes.filter { it.atk > 0 },
            statSelector = { it.atk },
            bonusColor = bonusColor,
            muted = muted,
        )
        GearStatTotalWithParts(
            label = "DEF",
            total = breakdown.totalDef,
            base = breakdown.baseDef,
            upgrade = breakdown.upgradeDef,
            rune = breakdown.runeDef,
            socketedRunes = breakdown.socketedRunes.filter { it.def > 0 },
            statSelector = { it.def },
            bonusColor = bonusColor,
            muted = muted,
        )
        GearStatTotalWithParts(
            label = "HP",
            total = breakdown.totalHp,
            base = breakdown.baseHp,
            upgrade = breakdown.upgradeHp,
            rune = breakdown.runeHp,
            socketedRunes = breakdown.socketedRunes.filter { it.hp > 0 },
            statSelector = { it.hp },
            bonusColor = bonusColor,
            muted = muted,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Power", fontWeight = FontWeight.SemiBold)
            Text(breakdown.totalPower.toString(), fontWeight = FontWeight.Bold, color = Gold)
        }

        when {
            breakdown.upgradePreview != null -> {
                val preview = breakdown.upgradePreview
                Text(
                    "Next upgrade (+${preview.nextUpgradeLevel}): ${preview.gainSummary} · ${preview.goldCost} gold + ${preview.materialCost} $upgradeMaterialName",
                    style = MaterialTheme.typography.labelMedium,
                    color = muted,
                )
            }
            breakdown.upgradeLevel > 0 -> {
                Text(
                    "Fully upgraded (+${breakdown.upgradeLevel}/${breakdown.maxUpgradeLevel})",
                    style = MaterialTheme.typography.labelMedium,
                    color = muted,
                )
            }
            breakdown.maxUpgradeLevel > 0 -> {
                Text(
                    "Upgrades available: +1 through +${breakdown.maxUpgradeLevel} add combat stats per level.",
                    style = MaterialTheme.typography.labelMedium,
                    color = muted,
                )
            }
        }
    }
}

@Composable
private fun GearStatTotalWithParts(
    label: String,
    total: Int,
    base: Int,
    upgrade: Int,
    rune: Int,
    socketedRunes: List<GearComparison.SocketedRuneBonus>,
    statSelector: (GearComparison.SocketedRuneBonus) -> Int,
    bonusColor: Color,
    muted: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(total.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        if (base > 0 || upgrade > 0 || rune > 0) {
            if (base > 0) {
                GearStatComponentLine("Base roll", base.toString(), muted)
            }
            if (upgrade > 0) {
                GearStatComponentLine("Upgrades", "+$upgrade", bonusColor)
            }
            if (rune > 0) {
                val runeNames = socketedRunes.joinToString { "${it.emoji} ${it.name} (+${statSelector(it)})" }
                GearStatComponentLine("Runes", "+$rune", bonusColor, detail = runeNames)
            }
        }
    }
}

@Composable
private fun GearStatComponentLine(
    label: String,
    value: String,
    valueColor: Color,
    detail: String? = null,
) {
    Column(
        modifier = Modifier.padding(start = 12.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = valueColor)
        }
        if (!detail.isNullOrBlank()) {
            Text(
                detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun GearStatDeltaBadge(label: String, delta: Int) {
    val (color, prefix) = when {
        delta > 0 -> Color(0xFF4ADE80) to "+"
        delta < 0 -> Color(0xFFF87171) to ""
        else -> MaterialTheme.colorScheme.onSurfaceVariant to "+"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            "$label $prefix$delta",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun GearSocketSlot(
    index: Int,
    rune: ItemEntity?,
    onSocket: () -> Unit,
    onRequestUnsocket: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = if (rune != null) onRequestUnsocket else onSocket,
        modifier = modifier
            .size(width = 72.dp, height = 80.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (rune != null) Color(0xFF9C7BE3).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(
            1.dp,
            if (rune != null) Color(0xFF9C7BE3) else Color.White.copy(alpha = 0.22f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (rune != null) {
                Text(rune.emoji, fontSize = 20.sp)
                Text(
                    rune.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = Gold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 9.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 10.sp
                )
                Text(
                    "Remove",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF87171),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text("+", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.55f))
                Text(
                    "Slot ${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun StackableMarketTile(
    item: ItemEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(item.emoji, style = MaterialTheme.typography.titleLarge)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.slot.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.quantity > 0) {
                    Text("x${item.quantity}", style = MaterialTheme.typography.labelSmall, color = Gold)
                }
            }
        }
    }
}
