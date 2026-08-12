package com.fitnessquest.rpg.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.data.OwnedGear
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.EquippableSlots
import com.fitnessquest.rpg.data.db.GearInstanceEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.data.db.itemBonusText
import com.fitnessquest.rpg.data.db.isEquippable
import com.fitnessquest.rpg.data.db.isStackable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.unit.sp
import com.fitnessquest.rpg.domain.Consumables
import com.fitnessquest.rpg.domain.ItemCatalog
import com.fitnessquest.rpg.domain.ProgressionRules
import com.fitnessquest.rpg.domain.RewardBatch
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.LocalSnackbarHostState
import com.fitnessquest.rpg.ui.appContainer
import com.fitnessquest.rpg.ui.components.ItemIcon
import com.fitnessquest.rpg.ui.components.RewardRevealDialog
import com.fitnessquest.rpg.ui.components.ResourceChip
import com.fitnessquest.rpg.ui.components.SceneBanner
import com.fitnessquest.rpg.ui.components.SceneKind
import com.fitnessquest.rpg.ui.components.SectionCard
import com.fitnessquest.rpg.ui.components.SettingsIconButton
import com.fitnessquest.rpg.ui.effects.AudioEffects
import com.fitnessquest.rpg.ui.effects.HapticEffects
import com.fitnessquest.rpg.ui.rememberDockContentPadding
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShopUiState(
    val character: CharacterEntity? = null,
    val items: List<ItemEntity> = emptyList(),
    val ownedGear: List<OwnedGear> = emptyList(),
)

private enum class MarketTab(val label: String, val icon: ImageVector) {
    Shop("Shop", Icons.Filled.Storefront),
    Inventory("Inventory", Icons.Filled.Backpack),
    Armory("Armory", Icons.Filled.Shield),
    Forge("Forge", Icons.Filled.Whatshot),
}

private enum class ItemFilter(val label: String) {
    All("All"),
    Weapons("Weapons"),
    Armor("Armor"),
    Runes("Runes"),
    Materials("Materials"),
    Chests("Chests"),
    Consumables("Supplies"),
}

private enum class ItemSort(val label: String) {
    Tier("Tier"),
    Power("Power"),
    Slot("Slot"),
    Price("Price"),
    Quantity("Qty"),
}

private data class DisplayItem(
    val item: ItemEntity,
    val instance: GearInstanceEntity? = null,
    val owned: Boolean = false,
    val equipped: Boolean = false,
) {
    val key: String = instance?.let { "gear-${it.id}" } ?: "item-${item.id}"
    val power: Int get() = item.atk + item.def + (item.hp / 4) + (instance?.upgradeLevel ?: 0) * 2
}

class ShopViewModel(private val container: AppContainer) : ViewModel() {
    val uiState: StateFlow<ShopUiState> = combine(
        container.repository.character,
        container.repository.items,
        container.repository.ownedGear
    ) { character, items, owned ->
        ShopUiState(character, items, owned)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShopUiState())

    private val buyMutex = kotlinx.coroutines.sync.Mutex()

    fun buy(itemId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!buyMutex.tryLock()) {
                onResult(false)
                return@launch
            }
            try {
                onResult(container.repository.buyItem(itemId))
            } finally {
                buyMutex.unlock()
            }
        }
    }

    fun equip(instanceId: Long) {
        viewModelScope.launch { container.repository.equipItem(instanceId) }
    }

    fun use(itemId: Long, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(container.repository.useConsumable(itemId)) }
    }

    fun openChest(itemId: Long, onResult: (RewardBatch?) -> Unit) {
        viewModelScope.launch {
            onResult(container.repository.openLootChest(itemId))
        }
    }

    fun sellMaterial(itemId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(container.repository.sellMaterial(itemId)) }
    }

    fun sellGear(instanceId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(container.repository.sellGearInstance(instanceId)) }
    }

    fun salvageGear(instanceId: Long, onResult: (RewardBatch?) -> Unit) {
        viewModelScope.launch { onResult(container.repository.salvageGearInstance(instanceId)) }
    }

    fun upgradeGear(instanceId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(container.repository.upgradeGearInstance(instanceId)) }
    }

    fun fuseGear(instanceIds: List<Long>, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(container.repository.fuseGearInstances(instanceIds)) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ShopViewModel(appContainer) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(viewModel: ShopViewModel = viewModel(factory = ShopViewModel.Factory)) {
    val state by viewModel.uiState.collectAsState()
    val character = state.character ?: return
    var tab by remember { mutableStateOf(MarketTab.Inventory) }
    var filter by remember { mutableStateOf(ItemFilter.All) }
    var sort by remember { mutableStateOf(ItemSort.Tier) }
    var myClassOnly by remember { mutableStateOf(true) }
    var canAffordOnly by remember { mutableStateOf(false) }
    var selectedSlot by remember { mutableStateOf<ItemSlot?>(null) }
    var selectedItem by remember { mutableStateOf<DisplayItem?>(null) }
    var rewardReveal by remember { mutableStateOf<RewardBatch?>(null) }
    var selectedFuseIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    fun usable(item: ItemEntity): Boolean =
        item.classAffinity == null || item.classAffinity == character.characterClass

    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    val equippedIds = character.equippedIds().values.filterNotNull().toSet()
    val ownedRows = remember(state.ownedGear, equippedIds) {
        state.ownedGear.map {
            DisplayItem(
                item = it.asDisplayItem(),
                instance = it.instance,
                owned = true,
                equipped = it.instance.id in equippedIds
            )
        }
    }
    val stackRows = remember(state.items) {
        state.items.filter { it.slot.isStackable() && it.quantity > 0 }.map { DisplayItem(it, owned = true) }
    }
    val shopRows = remember(state.items, character, myClassOnly, canAffordOnly) {
        state.items
            .filter { (!myClassOnly || usable(it)) && (!canAffordOnly || character.gold >= it.price) }
            .map { DisplayItem(it) }
    }

    val sourceRows = when (tab) {
        MarketTab.Shop -> shopRows
        MarketTab.Inventory -> stackRows + ownedRows
        MarketTab.Armory -> ownedRows
        MarketTab.Forge -> ownedRows
    }
    val visibleRows = sourceRows
        .filter { row -> selectedSlot == null || row.item.slot == selectedSlot }
        .filter { row -> row.matches(filter) }
        .sortedWith(sort.comparator())

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            SceneBanner(
                kind = SceneKind.SHOP,
                title = "ARMORY MARKET",
                tagline = "Browse, equip, upgrade, and salvage."
            ) {
                ResourceChip("G", character.gold.toString())
                SettingsIconButton()
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 154.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = rememberDockContentPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModeTabs(tab = tab, onTab = {
                            tab = it
                            selectedSlot = null
                            filter = ItemFilter.All
                        })
                        MarketSummary(character, state.ownedGear, state.items)
                        if (tab == MarketTab.Armory) {
                            ArmorySlots(character, ownedRows) { slot ->
                                selectedSlot = if (selectedSlot == slot) null else slot
                            }
                        }
                        if (tab == MarketTab.Forge) {
                            ForgeActionBar(
                                selectedCount = selectedFuseIds.size,
                                onFuse = {
                                    viewModel.fuseGear(selectedFuseIds.toList()) { msg ->
                                        if (msg != null) {
                                            selectedFuseIds = emptySet()
                                            AudioEffects.playLevelUp()
                                            HapticEffects.performSetLogged(haptic, context)
                                        }
                                        notify(msg ?: "Fusion failed.")
                                    }
                                }
                            )
                        }
                        BrowseControls(
                            tab = tab,
                            filter = filter,
                            onFilter = { filter = it },
                            sort = sort,
                            onSort = { sort = it },
                            myClassOnly = myClassOnly,
                            onMyClassOnly = { myClassOnly = it },
                            canAffordOnly = canAffordOnly,
                            onCanAffordOnly = { canAffordOnly = it },
                            selectedSlot = selectedSlot,
                            onSlot = { selectedSlot = if (selectedSlot == it) null else it }
                        )
                    }
                }

                if (visibleRows.isEmpty()) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        EmptyInventoryState(tab)
                    }
                }

                items(visibleRows, key = { it.key }) { row ->
                    ItemGridCard(
                        row = row,
                        selected = row.instance?.id in selectedFuseIds,
                        forgeMode = tab == MarketTab.Forge,
                        onClick = { selectedItem = row },
                        onSelect = {
                            val id = row.instance?.id ?: return@ItemGridCard
                            selectedFuseIds = if (id in selectedFuseIds) {
                                selectedFuseIds - id
                            } else if (selectedFuseIds.size < 3) {
                                selectedFuseIds + id
                            } else {
                                selectedFuseIds
                            }
                        }
                    )
                }
            }
        }
    }

    selectedItem?.let { row ->
        val equippedId = character.equippedIds()[row.item.slot]
        val equippedRow = if (equippedId != null) ownedRows.firstOrNull { it.instance?.id == equippedId } else null
        ModalBottomSheet(onDismissRequest = { selectedItem = null }) {
            ItemDetailSheet(
                row = row,
                equippedItem = equippedRow?.item,
                character = character,
                tab = tab,
                usable = usable(row.item),
                onBuy = {
                    viewModel.buy(row.item.id) { ok ->
                        if (ok) {
                            AudioEffects.playCoinJingle()
                            HapticEffects.performSetLogged(haptic, context)
                        }
                        notify(if (ok) "${row.item.name} added." else "Not enough gold.")
                    }
                },
                onEquip = {
                    row.instance?.id?.let(viewModel::equip)
                    selectedItem = null
                },
                onUse = {
                    viewModel.use(row.item.id) { notify(it ?: "Nothing happened.") }
                    selectedItem = null
                },
                onOpen = {
                    viewModel.openChest(row.item.id) { batch ->
                        if (batch != null) {
                            rewardReveal = batch
                        } else {
                            notify("The chest was empty.")
                        }
                    }
                    selectedItem = null
                },
                onSellMaterial = {
                    viewModel.sellMaterial(row.item.id) { ok -> notify(if (ok) "Sold ${row.item.name}." else "Could not sell.") }
                    selectedItem = null
                },
                onSellGear = {
                    row.instance?.id?.let { id ->
                        viewModel.sellGear(id) { ok -> notify(if (ok) "Sold ${row.item.name}." else "Could not sell.") }
                    }
                    selectedItem = null
                },
                onSalvage = {
                    row.instance?.id?.let { id ->
                        viewModel.salvageGear(id) { batch ->
                            if (batch != null) {
                                rewardReveal = batch
                            } else {
                                notify("Could not salvage.")
                            }
                        }
                    }
                    selectedItem = null
                },
                onUpgrade = {
                    row.instance?.id?.let { id ->
                        viewModel.upgradeGear(id) { ok -> notify(if (ok) "Upgraded ${row.item.name}." else "Need more gold/materials or max level reached.") }
                    }
                    selectedItem = null
                }
            )
        }
    }

    rewardReveal?.let { batch ->
        RewardRevealDialog(batch = batch, onDismiss = { rewardReveal = null })
    }
}

@Composable
private fun ModeTabs(tab: MarketTab, onTab: (MarketTab) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        MarketTab.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = tab == option,
                onClick = { onTab(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = MarketTab.entries.size),
                icon = { Icon(option.icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
            ) {
                Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MarketSummary(character: CharacterEntity, owned: List<OwnedGear>, items: List<ItemEntity>) {
    val stackCount = items.count { it.slot.isStackable() && it.quantity > 0 }
    val equipped = character.equippedIds().values.count { it != null }
    SectionCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryTile(Icons.Filled.Inventory2, owned.size.toString(), "Gear", Modifier.weight(1f))
            SummaryTile(Icons.Filled.Category, stackCount.toString(), "Stacks", Modifier.weight(1f))
            SummaryTile(Icons.Filled.Shield, "$equipped/7", "Equipped", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryTile(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun ArmorySlots(character: CharacterEntity, ownedRows: List<DisplayItem>, onSlot: (ItemSlot) -> Unit) {
    SectionCard(title = "Equipped Armory") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EquippableSlots.forEach { slot ->
                val equippedId = character.equippedIds()[slot]
                val row = ownedRows.firstOrNull { it.instance?.id == equippedId }
                AssistChip(
                    onClick = { onSlot(slot) },
                    label = {
                        Text(
                            "${slot.label}: ${row?.item?.name ?: "Empty"}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(slot.icon(), contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@Composable
private fun ForgeActionBar(selectedCount: Int, onFuse: () -> Unit) {
    SectionCard(title = "Forge Bench") {
        Text(
            "Upgrade individual gear from its detail sheet, or select 3 matching-tier pieces to fuse.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onFuse, enabled = selectedCount == 3, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Whatshot, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (selectedCount == 3) "Fuse selected gear" else "Select gear ($selectedCount/3)")
        }
    }
}

@Composable
private fun BrowseControls(
    tab: MarketTab,
    filter: ItemFilter,
    onFilter: (ItemFilter) -> Unit,
    sort: ItemSort,
    onSort: (ItemSort) -> Unit,
    myClassOnly: Boolean,
    onMyClassOnly: (Boolean) -> Unit,
    canAffordOnly: Boolean,
    onCanAffordOnly: (Boolean) -> Unit,
    selectedSlot: ItemSlot?,
    onSlot: (ItemSlot) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LabelRow(Icons.Filled.FilterList, "Filter")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ItemFilter.entries.forEach { option ->
                FilterChip(selected = filter == option, onClick = { onFilter(option) }, label = { Text(option.label) })
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            EquippableSlots.forEach { slot ->
                FilterChip(
                    selected = selectedSlot == slot,
                    onClick = { onSlot(slot) },
                    label = { Text(slot.label) },
                    leadingIcon = { Icon(slot.icon(), contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
        LabelRow(Icons.AutoMirrored.Filled.Sort, "Sort")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ItemSort.entries.forEach { option ->
                FilterChip(selected = sort == option, onClick = { onSort(option) }, label = { Text(option.label) })
            }
            if (tab == MarketTab.Shop) {
                FilterChip(selected = myClassOnly, onClick = { onMyClassOnly(!myClassOnly) }, label = { Text("My class") })
                FilterChip(selected = canAffordOnly, onClick = { onCanAffordOnly(!canAffordOnly) }, label = { Text("Can afford") })
            }
        }
    }
}

@Composable
private fun LabelRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ItemGridCard(
    row: DisplayItem,
    selected: Boolean,
    forgeMode: Boolean,
    onClick: () -> Unit,
    onSelect: () -> Unit,
) {
    val border = when {
        selected -> MaterialTheme.colorScheme.primary
        row.item.tier >= 4 -> Color(0xFFF0C040)
        row.item.tier == 3 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }
    Surface(
        modifier = Modifier
            .height(174.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = if (forgeMode && row.instance != null) onSelect else onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, border)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemBadge(row.item)
                Column(Modifier.weight(1f)) {
                    Text(row.item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(row.item.slot.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                TinyPill("T${row.item.tier}")
                if (row.instance != null) TinyPill("+${row.instance.upgradeLevel}")
                if (row.equipped) TinyPill("Equipped")
                if (row.item.quantity > 0) TinyPill("x${row.item.quantity}")
            }
            Text(
                itemBonusText(row.item).ifBlank { row.item.description },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClick, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Filled.OpenInFull, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Details")
            }
        }
    }
}

@Composable
private fun ItemBadge(item: ItemEntity) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) {
            if (item.slot.isStackable()) {
                Text(item.emoji, style = MaterialTheme.typography.titleLarge)
            } else {
                ItemIcon(item, Modifier.size(34.dp))
            }
        }
    }
}

@Composable
private fun TinyPill(text: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyInventoryState(tab: MarketTab) {
    SectionCard {
        Text(
            when (tab) {
                MarketTab.Shop -> "No items match these filters."
                MarketTab.Inventory -> "No inventory items match these filters."
                MarketTab.Armory -> "No gear matches this armory filter."
                MarketTab.Forge -> "No forgeable gear matches this filter."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ItemDetailSheet(
    row: DisplayItem,
    equippedItem: ItemEntity?,
    character: CharacterEntity,
    tab: MarketTab,
    usable: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    onUse: () -> Unit,
    onOpen: () -> Unit,
    onSellMaterial: () -> Unit,
    onSellGear: () -> Unit,
    onSalvage: () -> Unit,
    onUpgrade: () -> Unit,
) {
    val isClassLocked = row.item.classAffinity != null && row.item.classAffinity != character.characterClass
    val isTierLocked = !usable

    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ItemBadge(row.item)
            Column(Modifier.weight(1f)) {
                Text(row.item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(itemMeta(row), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TinyPill("Tier ${row.item.tier}")
            if (row.instance != null) TinyPill("${row.instance.rarity.lowercase().replaceFirstChar { it.uppercase() }} +${row.instance.upgradeLevel}")
            row.instance?.originBiome?.takeIf { it.isNotBlank() }?.let { TinyPill(it.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() }) }
            row.item.classAffinity?.let { TinyPill("${it.label} only") }
        }
        Text(
            row.item.description.ifBlank { "A useful piece of your growing arsenal." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isClassLocked || isTierLocked) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔒", fontSize = 20.sp)
                    Column {
                        Text("Requirement Locked", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                        if (isClassLocked) {
                            Text(
                                "Requires ${row.item.classAffinity?.label ?: "Class"}. Your hero is a ${character.characterClass?.label ?: "Hero"}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isTierLocked) {
                            Text(
                                tierUnlockRequirementText(row.item.tier),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (row.item.slot.isEquippable()) {
            val atkDiff = row.item.atk - (equippedItem?.atk ?: 0)
            val defDiff = row.item.def - (equippedItem?.def ?: 0)
            val hpDiff = row.item.hp - (equippedItem?.hp ?: 0)

            SectionCard(title = "Stats & Comparison") {
                Text(itemBonusText(row.item).ifBlank { "No direct combat stats." }, style = MaterialTheme.typography.bodyMedium)

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "vs. Currently Equipped (${equippedItem?.name ?: "None"})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatDeltaBadge("ATK", atkDiff)
                    StatDeltaBadge("DEF", defDiff)
                    StatDeltaBadge("HP", hpDiff)
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "Equippable by: ${row.item.classAffinity?.label ?: "All classes"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                row.instance?.let {
                    val cost = com.fitnessquest.rpg.domain.ProgressionRules.upgradeGoldCost(row.item.tier, it.upgradeLevel)
                    val mats = com.fitnessquest.rpg.domain.ProgressionRules.upgradeMaterialCost(row.item.tier, it.upgradeLevel)
                    val materialName = upgradeMaterialName(row.item)
                    Text(
                        "Next upgrade: $cost gold + $mats $materialName",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                tab == MarketTab.Shop && !row.owned -> {
                    Button(onClick = onBuy, enabled = character.gold >= row.item.price && usable) {
                        Icon(Icons.Filled.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${row.item.price} gold")
                    }
                }
                row.item.slot == ItemSlot.CONSUMABLE -> {
                    Button(onClick = onUse, enabled = row.item.id != Consumables.STREAK_FREEZE) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (row.item.id == Consumables.STREAK_FREEZE) "Auto-used" else "Use")
                    }
                }
                row.item.slot == ItemSlot.LOOT_CHEST -> {
                    Button(onClick = onOpen) {
                        Icon(Icons.Filled.OpenInFull, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open")
                    }
                }
                row.item.slot == ItemSlot.MATERIAL -> {
                    OutlinedButton(onClick = onSellMaterial) {
                        Icon(Icons.Filled.Paid, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sell one")
                    }
                }
                row.item.slot.isEquippable() && row.instance != null -> {
                    Button(onClick = onEquip, enabled = usable) {
                        Icon(if (row.equipped) Icons.Filled.Delete else Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (row.equipped) "Unequip" else "Equip")
                    }
                    OutlinedButton(onClick = onUpgrade) {
                        Icon(Icons.Filled.Upgrade, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Upgrade")
                    }
                    OutlinedButton(onClick = onSalvage, enabled = !row.equipped) {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Salvage")
                    }
                    OutlinedButton(onClick = onSellGear, enabled = !row.equipped) {
                        Icon(Icons.Filled.Toll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sell")
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun upgradeMaterialName(item: ItemEntity): String {
    val materialId = ProgressionRules.primaryMaterialFor(item)
    return ItemCatalog.all.firstOrNull { it.id == materialId }?.name ?: "materials"
}

@Composable
private fun StatDeltaBadge(label: String, delta: Int) {
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

private fun tierUnlockRequirementText(tier: Int): String = when (tier) {
    2 -> "Tier 2 unlocks at Hero Level 3 or reaching Darkwood."
    3 -> "Tier 3 unlocks by defeating 1 Boss or winning 20 Battles."
    4 -> "Tier 4 unlocks by defeating 3 Biome Bosses."
    5 -> "Tier 5 unlocks by clearing all biomes and reaching Layer 2."
    else -> "Tier $tier gear is currently locked by progression rules."
}

private fun OwnedGear.asDisplayItem(): ItemEntity =
    asEquippedItem().copy(
        slot = catalog.slot,
        tier = catalog.tier,
        price = catalog.price,
        description = catalog.description,
        classAffinity = catalog.classAffinity,
        style = catalog.style
    )

private fun DisplayItem.matches(filter: ItemFilter): Boolean = when (filter) {
    ItemFilter.All -> true
    ItemFilter.Weapons -> item.slot == ItemSlot.WEAPON
    ItemFilter.Armor -> item.slot in setOf(ItemSlot.HEAD, ItemSlot.CHEST, ItemSlot.HANDS, ItemSlot.LEGS, ItemSlot.FEET)
    ItemFilter.Runes -> item.slot == ItemSlot.RUNE
    ItemFilter.Materials -> item.slot == ItemSlot.MATERIAL
    ItemFilter.Chests -> item.slot == ItemSlot.LOOT_CHEST
    ItemFilter.Consumables -> item.slot == ItemSlot.CONSUMABLE
}

private fun ItemSort.comparator(): Comparator<DisplayItem> = when (this) {
    ItemSort.Tier -> compareByDescending<DisplayItem> { it.item.tier }.thenBy { it.item.name }
    ItemSort.Power -> compareByDescending<DisplayItem> { it.power }.thenBy { it.item.name }
    ItemSort.Slot -> compareBy<DisplayItem> { it.item.slot.ordinal }.thenByDescending { it.item.tier }.thenBy { it.item.name }
    ItemSort.Price -> compareByDescending<DisplayItem> { it.item.price }.thenBy { it.item.name }
    ItemSort.Quantity -> compareByDescending<DisplayItem> { it.item.quantity }.thenBy { it.item.name }
}

private fun itemMeta(row: DisplayItem): String = buildList {
    add(row.item.slot.label)
    add("Tier ${row.item.tier}")
    if (row.item.quantity > 0) add("x${row.item.quantity}")
    if (row.equipped) add("Equipped")
}.joinToString(" - ")

private fun ItemSlot.icon(): ImageVector = when (this) {
    ItemSlot.WEAPON -> Icons.Filled.Bolt
    ItemSlot.HEAD -> Icons.Filled.Shield
    ItemSlot.CHEST -> Icons.Filled.Inventory2
    ItemSlot.HANDS -> Icons.Filled.AutoFixHigh
    ItemSlot.LEGS -> Icons.Filled.Category
    ItemSlot.FEET -> Icons.Filled.Whatshot
    ItemSlot.TRINKET -> Icons.Filled.Toll
    ItemSlot.CONSUMABLE -> Icons.Filled.Bolt
    ItemSlot.RUNE -> Icons.Filled.AutoFixHigh
    ItemSlot.MATERIAL -> Icons.Filled.Category
    ItemSlot.LOOT_CHEST -> Icons.Filled.Inventory2
}
