package com.fitnessquest.rpg.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.style.TextAlign
import com.fitnessquest.rpg.data.db.gearBonusText
import com.fitnessquest.rpg.data.db.itemBonusText
import com.fitnessquest.rpg.domain.GearRarity
import com.fitnessquest.rpg.domain.GearSockets
import com.fitnessquest.rpg.domain.GearTrait
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
import com.fitnessquest.rpg.data.db.ClassProgressEntity
import com.fitnessquest.rpg.domain.CharacterClass
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShopUiState(
    val character: CharacterEntity? = null,
    val items: List<ItemEntity> = emptyList(),
    val ownedGear: List<OwnedGear> = emptyList(),
    val allClassProgress: List<ClassProgressEntity> = emptyList()
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
    val inUseByJob: CharacterClass? = null
) {

    val key: String = instance?.let { "gear-${it.id}" } ?: "item-${item.id}"
    val power: Int get() = item.atk + item.def + (item.hp / 4) + (instance?.upgradeLevel ?: 0) * 2
}

class ShopViewModel(private val container: AppContainer) : ViewModel() {
    val uiState: StateFlow<ShopUiState> = combine(
        container.repository.character,
        container.repository.items,
        container.repository.ownedGear,
        container.repository.allClassProgress
    ) { character, items, owned, allProgress ->
        ShopUiState(character, items, owned, allProgress)
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

    fun socketRune(instanceId: Long, slotIndex: Int, runeId: Long) {
        viewModelScope.launch { container.repository.socketRune(instanceId, slotIndex, runeId) }
    }

    fun clearRune(instanceId: Long, slotIndex: Int) {
        viewModelScope.launch { container.repository.clearRune(instanceId, slotIndex) }
    }

    fun reforgeGear(instanceId: Long, onResult: (Result<List<GearTrait>>) -> Unit) {
        viewModelScope.launch {
            onResult(container.repository.reforgeGearInstanceTraits(instanceId))
        }
    }

    fun upgradeGear(instanceId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(container.repository.upgradeGearInstance(instanceId)) }
    }

    fun gamble(slot: ItemSlot?, onResult: (Result<GearInstanceEntity>) -> Unit) {
        viewModelScope.launch {
            onResult(container.repository.gambleMysteryGear(slot))
        }
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
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    ShopScreenContent(
        state = state,
        actions = ShopActions(
            onBuy = { itemId, onResult ->
                viewModel.buy(itemId) { ok ->
                    if (ok) {
                        AudioEffects.playCoinJingle()
                        HapticEffects.performSetLogged(haptic, context)
                    }
                    onResult(ok)
                }
            },
            onEquip = viewModel::equip,
            onUse = viewModel::use,
            onOpenChest = viewModel::openChest,
            onSellMaterial = viewModel::sellMaterial,
            onSellGear = viewModel::sellGear,
            onSalvageGear = viewModel::salvageGear,
            onUpgradeGear = viewModel::upgradeGear,
            onReforgeGear = viewModel::reforgeGear,
            onGamble = viewModel::gamble,
            onFuseGear = { ids, onResult ->
                viewModel.fuseGear(ids) { msg ->
                    if (msg != null) {
                        AudioEffects.playLevelUp()
                        HapticEffects.performSetLogged(haptic, context)
                    }
                    onResult(msg)
                }
            },
            onSocketRune = viewModel::socketRune,
            onClearRune = viewModel::clearRune,
            onNotify = ::notify
        )
    )
}

data class ShopActions(
    val onBuy: (Long, (Boolean) -> Unit) -> Unit = { _, _ -> },
    val onEquip: (Long) -> Unit = {},
    val onUse: (Long, (String?) -> Unit) -> Unit = { _, _ -> },
    val onOpenChest: (Long, (RewardBatch?) -> Unit) -> Unit = { _, _ -> },
    val onSellMaterial: (Long, (Boolean) -> Unit) -> Unit = { _, _ -> },
    val onSellGear: (Long, (Boolean) -> Unit) -> Unit = { _, _ -> },
    val onSalvageGear: (Long, (RewardBatch?) -> Unit) -> Unit = { _, _ -> },
    val onUpgradeGear: (Long, (Boolean) -> Unit) -> Unit = { _, _ -> },
    val onReforgeGear: (Long, (Result<List<GearTrait>>) -> Unit) -> Unit = { _, _ -> },
    val onGamble: (ItemSlot?, (Result<GearInstanceEntity>) -> Unit) -> Unit = { _, _ -> },
    val onFuseGear: (List<Long>, (String?) -> Unit) -> Unit = { _, _ -> },
    val onSocketRune: (Long, Int, Long) -> Unit = { _, _, _ -> },
    val onClearRune: (Long, Int) -> Unit = { _, _ -> },
    val onNotify: (String) -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreenContent(
    state: ShopUiState,
    actions: ShopActions
) {
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

    fun usable(item: ItemEntity): Boolean =
        item.classAffinity == null || item.classAffinity == character.characterClass

    val equippedIds = character.equippedIds().values.filterNotNull().toSet()
    val ownedRows = remember(state.ownedGear, equippedIds, state.allClassProgress) {
        state.ownedGear.map {
            val inUseJob = state.allClassProgress.firstOrNull { job ->
                job.clazz != character.characterClass && it.instance.id in job.equippedIds().values
            }?.clazz
            DisplayItem(
                item = it.asDisplayItem(),
                instance = it.instance,
                owned = true,
                equipped = it.instance.id in equippedIds,
                inUseByJob = inUseJob
            )
        }
    }
    val stackRows = remember(state.items) {
        state.items.filter { it.slot.isStackable() && it.quantity > 0 }.map { DisplayItem(it, owned = true) }
    }
    val shopRows = remember(state.items, character, canAffordOnly) {
        state.items
            .filter { it.tier <= 3 && it.classAffinity == null }
            .filter { (!canAffordOnly || character.gold >= it.price) }
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
                        if (tab == MarketTab.Shop) {
                            val gambleCost = ProgressionRules.mysteryGambleCost(character.level)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Gold.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🎲", fontSize = 22.sp)
                                        Column(Modifier.weight(1f)) {
                                            Text("The Mystic Goblin's Cache", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Gold)
                                            Text("Gamble for mystery gear with high Rare/Epic/Legendary chances & procedural affixes!", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                actions.onGamble(ItemSlot.WEAPON) { res ->
                                                    res.onSuccess { inst ->
                                                        AudioEffects.playLevelUp()
                                                        actions.onNotify("🎲 Won: ${inst.rarity} weapon!")
                                                    }.onFailure { err -> actions.onNotify(err.message ?: "Gamble failed") }
                                                }
                                            },
                                            enabled = character.gold >= gambleCost,
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                        ) {
                                            Text("Weapon\n($gambleCost g)", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                                        }
                                        Button(
                                            onClick = {
                                                actions.onGamble(ItemSlot.CHEST) { res ->
                                                    res.onSuccess { inst ->
                                                        AudioEffects.playLevelUp()
                                                        actions.onNotify("🎲 Won: ${inst.rarity} armor!")
                                                    }.onFailure { err -> actions.onNotify(err.message ?: "Gamble failed") }
                                                }
                                            },
                                            enabled = character.gold >= gambleCost,
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                        ) {
                                            Text("Armor\n($gambleCost g)", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                                        }
                                        Button(
                                            onClick = {
                                                actions.onGamble(ItemSlot.TRINKET) { res ->
                                                    res.onSuccess { inst ->
                                                        AudioEffects.playLevelUp()
                                                        actions.onNotify("🎲 Won: ${inst.rarity} trinket!")
                                                    }.onFailure { err -> actions.onNotify(err.message ?: "Gamble failed") }
                                                }
                                            },
                                            enabled = character.gold >= gambleCost,
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                        ) {
                                            Text("Trinket\n($gambleCost g)", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }
                        if (tab == MarketTab.Armory) {
                            ArmorySlots(character, ownedRows) { slot ->
                                selectedSlot = if (selectedSlot == slot) null else slot
                            }
                        }
                        if (tab == MarketTab.Forge) {
                            ForgeActionBar(
                                selectedCount = selectedFuseIds.size,
                                onFuse = {
                                    actions.onFuseGear(selectedFuseIds.toList()) { msg ->
                                        if (msg != null) {
                                            selectedFuseIds = emptySet()
                                        }
                                        actions.onNotify(msg ?: "Fusion failed.")
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
                        character = character,
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
        val availableRunes = state.items.filter { it.slot == ItemSlot.RUNE && it.quantity > 0 }
        ModalBottomSheet(onDismissRequest = { selectedItem = null }) {
            ItemDetailSheet(
                row = row,
                equippedItem = equippedRow?.item,
                character = character,
                tab = tab,
                usable = usable(row.item),
                runesCatalog = state.items.filter { it.slot == ItemSlot.RUNE },
                availableRunes = availableRunes,
                onBuy = {
                    actions.onBuy(row.item.id) { ok ->
                        actions.onNotify(if (ok) "${row.item.name} added." else "Not enough gold.")
                    }
                },
                onEquip = {
                    row.instance?.id?.let(actions.onEquip)
                    selectedItem = null
                },
                onUse = {
                    actions.onUse(row.item.id) { actions.onNotify(it ?: "Nothing happened.") }
                    selectedItem = null
                },
                onOpen = {
                    actions.onOpenChest(row.item.id) { batch ->
                        if (batch != null) {
                            rewardReveal = batch
                        } else {
                            actions.onNotify("The chest was empty.")
                        }
                    }
                    selectedItem = null
                },
                onSellMaterial = {
                    actions.onSellMaterial(row.item.id) { ok -> actions.onNotify(if (ok) "Sold ${row.item.name}." else "Could not sell.") }
                    selectedItem = null
                },
                onSellGear = {
                    row.instance?.id?.let { id ->
                        actions.onSellGear(id) { ok -> actions.onNotify(if (ok) "Sold ${row.item.name}." else "Could not sell.") }
                    }
                    selectedItem = null
                },
                onSalvage = {
                    row.instance?.id?.let { id ->
                        actions.onSalvageGear(id) { batch ->
                            if (batch != null) {
                                rewardReveal = batch
                            } else {
                                actions.onNotify("Could not salvage.")
                            }
                        }
                    }
                    selectedItem = null
                },
                onUpgrade = {
                    row.instance?.id?.let { id ->
                        actions.onUpgradeGear(id) { ok -> actions.onNotify(if (ok) "Upgraded ${row.item.name}." else "Need more gold/materials or max level reached.") }
                    }
                    selectedItem = null
                },
                onSocketRune = { slotIdx, runeId ->
                    row.instance?.id?.let { instId ->
                        actions.onSocketRune(instId, slotIdx, runeId)
                        actions.onNotify("Rune socketed!")
                    }
                },
                onClearRune = { slotIdx ->
                    row.instance?.id?.let { instId ->
                        actions.onClearRune(instId, slotIdx)
                        actions.onNotify("Rune removed.")
                    }
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
    character: CharacterEntity,
    onClick: () -> Unit,
    onSelect: () -> Unit,
) {
    val rarity = row.instance?.rarity?.let { GearRarity.fromName(it) } ?: GearRarity.COMMON
    val traits = GearTrait.parseTraits(row.instance?.traitIds)
    val border = when {
        selected -> MaterialTheme.colorScheme.primary
        rarity != GearRarity.COMMON -> rarity.color
        row.item.tier >= 4 -> Color(0xFFF0C040)
        row.item.tier == 3 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }
    Surface(
        modifier = Modifier
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = if (forgeMode && row.instance != null) onSelect else onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (rarity != GearRarity.COMMON) rarity.color.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected || rarity != GearRarity.COMMON) 1.5.dp else 1.dp, border)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemBadge(row.item, rarity)
                Column(Modifier.weight(1f)) {
                    Text(
                        row.item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (rarity != GearRarity.COMMON) rarity.color else Color.White
                    )
                    Text(row.item.slot.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                TinyPill(rarity.displayName, color = rarity.color)
                TinyPill("T${row.item.tier}")
                val reqLevel = ProgressionRules.requiredLevelFor(row.item.tier, rarity)
                if (character.level < reqLevel) {
                    TinyPill("Req. Lv $reqLevel", color = Color(0xFFEF5350))
                }
                if (row.instance != null && row.instance.upgradeLevel > 0) TinyPill("+${row.instance.upgradeLevel}")
                if (row.equipped) TinyPill("Equipped", color = Gold)
                if (row.inUseByJob != null) TinyPill("✦ In Use: ${row.inUseByJob.label}", color = Color(0xFF64B5F6))
                if (row.item.quantity > 0) TinyPill("x${row.item.quantity}")
                for (trait in traits) {
                    TinyPill("${trait.emoji} ${trait.displayName}", color = Gold)
                }
            }
            val bonusText = if (row.instance != null) gearBonusText(row.instance) else itemBonusText(row.item)
            Text(
                bonusText.ifBlank { row.item.description },
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
private fun ItemBadge(item: ItemEntity, rarity: GearRarity = GearRarity.COMMON) {
    Surface(
        shape = CircleShape,
        color = if (rarity != GearRarity.COMMON) rarity.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (rarity != GearRarity.COMMON) rarity.color else Color.Transparent),
        modifier = Modifier.size(44.dp)
    ) {
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
private fun TinyPill(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.35f))
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
    runesCatalog: List<ItemEntity>,
    availableRunes: List<ItemEntity>,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    onUse: () -> Unit,
    onOpen: () -> Unit,
    onSellMaterial: () -> Unit,
    onSellGear: () -> Unit,
    onSalvage: () -> Unit,
    onUpgrade: () -> Unit,
    onSocketRune: (Int, Long) -> Unit,
    onClearRune: (Int) -> Unit,
) {
    val isClassLocked = row.item.classAffinity != null && row.item.classAffinity != character.characterClass
    val isTierLocked = !usable
    val rarity = row.instance?.rarity?.let { GearRarity.fromName(it) } ?: GearRarity.COMMON
    val traits = GearTrait.parseTraits(row.instance?.traitIds)
    val maxSockets = row.instance?.let { GearSockets.slotsForTier(row.item.tier) } ?: 0
    var socketTargetIdx by remember { mutableStateOf<Int?>(null) }

    Column(
        Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ItemBadge(row.item, rarity)
            Column(Modifier.weight(1f)) {
                Text(
                    row.item.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (rarity != GearRarity.COMMON) rarity.color else Color.White
                )
                Text(itemMeta(row), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TinyPill(rarity.displayName, color = rarity.color)
            TinyPill("Tier ${row.item.tier}")
            val reqLevel = ProgressionRules.requiredLevelFor(row.item.tier, rarity)
            if (character.level < reqLevel) {
                TinyPill("Req. Level $reqLevel", color = Color(0xFFEF5350))
            }
            if (row.instance != null) TinyPill("${row.instance.rarity.lowercase().replaceFirstChar { it.uppercase() }} +${row.instance.upgradeLevel}")
            if (row.inUseByJob != null) TinyPill("✦ In Use: ${row.inUseByJob.label}", color = Color(0xFF64B5F6))
            row.instance?.originBiome?.takeIf { it.isNotBlank() }?.let { TinyPill(it.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() }) }
            row.item.classAffinity?.let { TinyPill("${it.label} only") }
        }
        Text(
            row.item.description.ifBlank { "A useful piece of your growing arsenal." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Traits display
        if (traits.isNotEmpty()) {
            SectionCard(title = "Gear Traits") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (trait in traits) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(trait.emoji, fontSize = 20.sp)
                            Column {
                                Text(trait.displayName, fontWeight = FontWeight.Bold, color = Gold)
                                Text(trait.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Rune sockets section
        if (maxSockets > 0 && row.instance != null) {
            SectionCard(title = "Rune Sockets ($maxSockets slots)") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (idx in 0 until maxSockets) {
                        val runeId = if (idx == 0) row.instance.rune1Id else row.instance.rune2Id
                        val rune = runesCatalog.find { it.id == runeId }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Socket ${idx + 1}:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                if (rune != null) {
                                    Text("${rune.emoji} ${rune.name}", color = Gold, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Empty", color = Color.Gray)
                                }
                            }
                            if (rune != null) {
                                OutlinedButton(onClick = { onClearRune(idx) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                                    Text("Unsocket", style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                Button(
                                    onClick = { socketTargetIdx = idx },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("+ Socket", style = MaterialTheme.typography.labelSmall)
                                }
                            }
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
                    val isEquippedAnywhere = row.equipped || row.inUseByJob != null
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
                    OutlinedButton(onClick = onSalvage, enabled = !isEquippedAnywhere) {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Salvage")
                    }
                    OutlinedButton(onClick = onSellGear, enabled = !isEquippedAnywhere) {
                        Icon(Icons.Filled.Toll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sell")
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // Rune Picker Dialog
    socketTargetIdx?.let { slotIdx ->
        AlertDialog(
            onDismissRequest = { socketTargetIdx = null },
            title = { Text("Socket Rune into Slot ${slotIdx + 1}") },
            text = {
                if (availableRunes.isEmpty()) {
                    Text("You don't have any runes in your inventory. Complete workouts or defeat monsters to earn runes!")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Choose a rune to socket:", style = MaterialTheme.typography.bodySmall)
                        for (rune in availableRunes) {
                            Surface(
                                onClick = {
                                    onSocketRune(slotIdx, rune.id)
                                    socketTargetIdx = null
                                },
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
                TextButton(onClick = { socketTargetIdx = null }) {
                    Text("Close")
                }
            }
        )
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF12131F)
@androidx.compose.runtime.Composable
fun ShopScreenPreview() {
    com.fitnessquest.rpg.ui.theme.FitQuestTheme {
        ShopScreenContent(
            state = ShopUiState(
                character = CharacterEntity(name = "Preview Hero", gold = 5000),
                items = emptyList(),
                ownedGear = emptyList()
            ),
            actions = ShopActions()
        )
    }
}
