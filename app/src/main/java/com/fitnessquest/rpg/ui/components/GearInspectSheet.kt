package com.fitnessquest.rpg.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessquest.rpg.data.OwnedGear
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.GearInstanceEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.data.db.isEquippable
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.Consumables
import com.fitnessquest.rpg.domain.GearComparison
import com.fitnessquest.rpg.domain.GearSetRegistry
import com.fitnessquest.rpg.domain.LootIntel
import com.fitnessquest.rpg.domain.WearArchetype
import com.fitnessquest.rpg.domain.GearRarity
import com.fitnessquest.rpg.domain.GearSockets
import com.fitnessquest.rpg.domain.GearTrait
import com.fitnessquest.rpg.domain.ItemCatalog
import com.fitnessquest.rpg.domain.ProgressionRules
import com.fitnessquest.rpg.ui.theme.Gold

data class GearInspectTarget(
    val item: ItemEntity,
    val instance: GearInstanceEntity? = null,
    val owned: Boolean = true,
    val equipped: Boolean = false,
    val inUseByJob: CharacterClass? = null,
)

data class GearInspectActions(
    val showBuy: Boolean = false,
    val showEquip: Boolean = false,
    val showUpgrade: Boolean = false,
    val showSalvage: Boolean = false,
    val showSellGear: Boolean = false,
    val showReforge: Boolean = false,
    val showConsumableUse: Boolean = false,
    val showOpenChest: Boolean = false,
    val showSellMaterial: Boolean = false,
)

@Composable
fun GearInspectSheet(
    target: GearInspectTarget,
    character: CharacterEntity,
    comparison: GearComparison.Comparison?,
    equippedName: String?,
    usable: Boolean,
    runesCatalog: List<ItemEntity>,
    availableRunes: List<ItemEntity>,
    actions: GearInspectActions,
    onBuy: () -> Unit = {},
    onEquip: () -> Unit = {},
    onUse: () -> Unit = {},
    onOpen: () -> Unit = {},
    onSellMaterial: () -> Unit = {},
    onSellGear: () -> Unit = {},
    onSalvage: () -> Unit = {},
    onUpgrade: () -> Unit = {},
    onReforge: () -> Unit = {},
    onSocketRune: (Int, Long) -> Unit = { _, _ -> },
    onClearRune: (Int) -> Unit = {},
    onRuneFeedback: (String) -> Unit = {},
) {
    val isClassLocked = target.item.classAffinity != null && target.item.classAffinity != character.characterClass
    val isTierLocked = !usable
    val rarity = target.instance?.rarity?.let { GearRarity.fromName(it) } ?: GearRarity.COMMON
    val traits = GearTrait.parseTraits(target.instance?.traitIds)
    val maxSockets = target.instance?.let { GearSockets.slotsForTier(target.item.tier) } ?: 0
    val instanceId = target.instance?.id
    val rune1Id = target.instance?.rune1Id
    val rune2Id = target.instance?.rune2Id
    var socketTargetIdx by remember(instanceId, rune1Id, rune2Id) { mutableStateOf<Int?>(null) }
    var unsocketTargetIdx by remember(instanceId, rune1Id, rune2Id) { mutableStateOf<Int?>(null) }
    val runeCatalogMap = remember(runesCatalog) { runesCatalog.associateBy { it.id } }
    val detailedStats = remember(target.item.id, target.instance, rune1Id, rune2Id, runeCatalogMap) {
        GearComparison.detailedBreakdown(target.item, target.instance, runeCatalogMap)
    }
    val showStatComparison = comparison != null && !target.equipped
    val canReforge = ProgressionRules.canReforge(rarity)

    Column(
        Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GearItemBadge(target.item, rarity)
            Column(Modifier.weight(1f)) {
                Text(
                    target.item.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (rarity != GearRarity.COMMON) rarity.color else Color.White
                )
                Text(
                    gearInspectMeta(target),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (comparison != null && target.item.slot.isEquippable() && !target.equipped) {
                GearPowerVerdict(comparison, compact = false)
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GearMetaPill(rarity.displayName, rarity.color)
            GearMetaPill("Tier ${target.item.tier}")
            val reqLevel = ProgressionRules.requiredLevelFor(target.item.tier, rarity)
            if (character.level < reqLevel) {
                GearMetaPill("Req. Level $reqLevel", Color(0xFFEF5350))
            }
            target.instance?.let {
                GearMetaPill("${it.rarity.lowercase().replaceFirstChar { c -> c.uppercase() }} +${it.upgradeLevel}")
            }
            target.inUseByJob?.let { GearMetaPill("✦ In Use: ${it.label}", Color(0xFF64B5F6)) }
            target.item.classAffinity?.let { GearMetaPill("${it.label} only") }
            if (target.equipped) GearMetaPill("Equipped", Gold)
        }

        Text(
            target.item.description.ifBlank { "A useful piece of your growing arsenal." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (traits.isNotEmpty()) {
            SectionCard(title = "Gear Traits") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (trait in traits) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(trait.emoji, fontSize = 20.sp)
                            Column {
                                Text(trait.displayName, fontWeight = FontWeight.Bold, color = Gold)
                                Text(
                                    trait.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (maxSockets > 0 && target.instance != null) {
            SectionCard(title = "Rune Sockets") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Tap an empty slot to socket a rune. Tap a filled slot to remove it.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (idx in 0 until maxSockets) {
                            val runeId = if (idx == 0) target.instance.rune1Id else target.instance.rune2Id
                            val rune = runesCatalog.find { it.id == runeId }
                            GearSocketSlot(
                                index = idx,
                                rune = rune,
                                onSocket = { socketTargetIdx = idx },
                                onRequestUnsocket = { unsocketTargetIdx = idx },
                            )
                        }
                    }
                }
            }
        }

        if (isClassLocked || isTierLocked) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔒", fontSize = 20.sp)
                    Column {
                        Text(
                            "Requirement Locked",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF87171)
                        )
                        if (isClassLocked) {
                            Text(
                                "Requires ${target.item.classAffinity?.label ?: "Class"}. Your hero is a ${character.characterClass?.label ?: "Hero"}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isTierLocked) {
                            Text(
                                tierUnlockRequirementText(target.item.tier),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (target.item.slot.isEquippable()) {
            SectionCard(title = if (showStatComparison) "Stats & Comparison" else "Gear Stats") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showStatComparison) {
                        Text(
                            "vs. Currently Equipped (${equippedName ?: "None"})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Gold
                        )
                        GearStatDeltaRow("ATK", comparison!!.candidate.atk, comparison.atkDelta)
                        GearStatDeltaRow("DEF", comparison.candidate.def, comparison.defDelta)
                        GearStatDeltaRow("HP", comparison.candidate.hp, comparison.hpDelta)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Power", fontWeight = FontWeight.SemiBold)
                            if (comparison.verdict != GearComparison.Verdict.NO_BASELINE) {
                                GearPowerVerdict(comparison, compact = false)
                            } else {
                                Text(comparison.candidate.totalPower.toString())
                            }
                        }
                        Text(
                            "Breakdown",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Gold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    GearStatBreakdownSection(
                        breakdown = detailedStats,
                        upgradeMaterialName = upgradeMaterialName(target.item),
                    )
                    Text(
                        "Equippable by: ${target.item.classAffinity?.label ?: "All classes"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    target.instance?.takeIf { it.itemLevel > 0 }?.let {
                        Text(
                            "Item level ${it.itemLevel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    target.instance?.setId?.let { setId ->
                        Text(
                            "Set: ${GearSetRegistry.displayNameFor(setId)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Gold,
                        )
                        GearSetRegistry.bonusLevelsFor(setId).forEach { level ->
                            Text(
                                "${level.piecesRequired}-piece: ${level.description}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                    LootIntel.farmHintForItem(target.item, target.instance?.setId)?.let { hint ->
                        Text(
                            "Farm from: $hint",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (WearArchetype.resolve(target.item) == WearArchetype.SHIELD) {
                        Text(
                            "Pairs with one-handed weapons only.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (actions.showBuy) {
                Button(onClick = onBuy, enabled = character.gold >= target.item.price && usable) {
                    Icon(Icons.Filled.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${target.item.price} gold")
                }
            }
            if (actions.showConsumableUse) {
                Button(onClick = onUse, enabled = target.item.id != Consumables.STREAK_FREEZE) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (target.item.id == Consumables.STREAK_FREEZE) "Auto-used" else "Use")
                }
            }
            if (actions.showOpenChest) {
                Button(onClick = onOpen) {
                    Icon(Icons.Filled.OpenInFull, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open")
                }
            }
            if (actions.showSellMaterial) {
                OutlinedButton(onClick = onSellMaterial) {
                    Icon(Icons.Filled.Paid, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sell one")
                }
            }
            if (actions.showEquip) {
                Button(onClick = onEquip, enabled = usable) {
                    Icon(
                        if (target.equipped) Icons.Filled.Delete else Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (target.equipped) "Unequip" else "Equip")
                }
            }
            if (actions.showUpgrade) {
                OutlinedButton(onClick = onUpgrade) {
                    Icon(Icons.Filled.Upgrade, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Upgrade")
                }
            }
            if (actions.showReforge) {
                OutlinedButton(onClick = onReforge, enabled = canReforge) {
                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reforge")
                }
            }
            if (actions.showSalvage) {
                val blocked = target.equipped || target.inUseByJob != null
                OutlinedButton(onClick = onSalvage, enabled = !blocked) {
                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salvage")
                }
            }
            if (actions.showSellGear) {
                val blocked = target.equipped || target.inUseByJob != null
                OutlinedButton(onClick = onSellGear, enabled = !blocked) {
                    Icon(Icons.Filled.Toll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sell")
                }
            }
        }
        if (actions.showReforge) {
            Text(
                if (canReforge) {
                    val gold = ProgressionRules.reforgeGoldCost(target.item.tier, rarity)
                    val mats = ProgressionRules.reforgeMaterialCost(target.item.tier, rarity)
                    "Reforge rerolls combat traits for $gold gold + $mats ${upgradeMaterialName(target.item)}."
                } else {
                    "Reforge unlocks at ${GearRarity.RARE.displayName} rarity or higher and rerolls combat traits — separate from stat upgrades."
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (canReforge) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    Color(0xFFF0C040).copy(alpha = 0.85f)
                },
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    socketTargetIdx?.let { slotIdx ->
        GearRunePickerDialog(
            slotIndex = slotIdx,
            availableRunes = availableRunes,
            onDismiss = { socketTargetIdx = null },
            onSelect = { runeId ->
                onSocketRune(slotIdx, runeId)
                val runeName = runesCatalog.find { it.id == runeId }?.name ?: "Rune"
                onRuneFeedback("Socketed $runeName.")
                socketTargetIdx = null
            }
        )
    }

    unsocketTargetIdx?.let { slotIdx ->
        val runeId = if (slotIdx == 0) target.instance?.rune1Id else target.instance?.rune2Id
        val runeName = runesCatalog.find { it.id == runeId }?.name ?: "this rune"
        AlertDialog(
            onDismissRequest = { unsocketTargetIdx = null },
            title = { Text("Remove Rune?") },
            text = {
                Text("Unsocket $runeName from ${target.item.name}? The rune returns to your inventory.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearRune(slotIdx)
                        onRuneFeedback("Removed $runeName.")
                        unsocketTargetIdx = null
                    }
                ) {
                    Text("Remove", color = Color(0xFFF87171))
                }
            },
            dismissButton = {
                TextButton(onClick = { unsocketTargetIdx = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GearRunePickerDialog(
    slotIndex: Int,
    availableRunes: List<ItemEntity>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Socket Rune into Slot ${slotIndex + 1}") },
        text = {
            if (availableRunes.isEmpty()) {
                Text("You don't have any runes in your inventory. Complete workouts or defeat monsters to earn runes!")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose a rune to socket:", style = MaterialTheme.typography.bodySmall)
                    for (rune in availableRunes) {
                        Surface(
                            onClick = { onSelect(rune.id) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(rune.emoji, fontSize = 20.sp)
                                Column(Modifier.weight(1f)) {
                                    Text(rune.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(rune.description, style = MaterialTheme.typography.labelSmall, color = Gold)
                                }
                                Text("x${rune.quantity}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun GearMetaPill(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
}

private val GearInspectTarget.key: String
    get() = instance?.id?.let { "gear-$it" } ?: "item-${item.id}"

private fun gearInspectMeta(target: GearInspectTarget): String = buildList {
    add(target.item.slot.label)
    add("Tier ${target.item.tier}")
    if (target.item.quantity > 0) add("x${target.item.quantity}")
    if (target.equipped) add("Equipped")
}.joinToString(" · ")

private fun upgradeMaterialName(item: ItemEntity): String {
    val materialId = ProgressionRules.primaryMaterialFor(item)
    return ItemCatalog.all.firstOrNull { it.id == materialId }?.name ?: "materials"
}

private fun tierUnlockRequirementText(tier: Int): String = when (tier) {
    2 -> "Tier 2 unlocks at Hero Level 3 or reaching Darkwood."
    3 -> "Tier 3 unlocks by defeating 1 Boss or winning 20 Battles."
    4 -> "Tier 4 unlocks by defeating 3 Biome Bosses."
    5 -> "Tier 5 unlocks by clearing all biomes and reaching Layer 2."
    else -> "Tier $tier gear is currently locked by progression rules."
}

fun OwnedGear.toInspectTarget(
    equipped: Boolean = false,
    inUseByJob: CharacterClass? = null,
): GearInspectTarget = GearInspectTarget(
    item = asDisplayItem(),
    instance = instance,
    owned = true,
    equipped = equipped,
    inUseByJob = inUseByJob,
)
