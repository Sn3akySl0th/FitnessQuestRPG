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
import com.fitnessquest.rpg.data.db.ArmorSlots
import com.fitnessquest.rpg.data.db.gearBonusText
import com.fitnessquest.rpg.data.db.itemBonusText
import com.fitnessquest.rpg.domain.GearRarity
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
import com.fitnessquest.rpg.domain.GearComparison
import com.fitnessquest.rpg.domain.GearSockets
import com.fitnessquest.rpg.domain.ItemCatalog
import com.fitnessquest.rpg.domain.ProgressionRules
import com.fitnessquest.rpg.domain.RewardBatch
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.LocalSnackbarHostState
import com.fitnessquest.rpg.ui.appContainer
import com.fitnessquest.rpg.ui.components.GearCompactTile
import com.fitnessquest.rpg.ui.components.GearInspectActions
import com.fitnessquest.rpg.ui.components.GearInspectSheet
import com.fitnessquest.rpg.ui.components.GearInspectTarget
import com.fitnessquest.rpg.ui.components.RewardRevealDialog
import com.fitnessquest.rpg.ui.components.StackableMarketTile
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

    fun power(runeCatalog: Map<Long, ItemEntity>): Int =
        GearComparison.breakdown(item, instance, runeCatalog).totalPower
}

private fun DisplayItem.toInspectTarget(): GearInspectTarget = GearInspectTarget(
    item = item,
    instance = instance,
    owned = owned,
    equipped = equipped,
    inUseByJob = inUseByJob,
)

private fun filledSocketCount(instance: GearInstanceEntity?): Int =
    instance?.runeIds()?.size ?: 0

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
    private val gambleMutex = kotlinx.coroutines.sync.Mutex()
    private val chestMutex = kotlinx.coroutines.sync.Mutex()

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
            if (!chestMutex.tryLock()) {
                onResult(null)
                return@launch
            }
            try {
                onResult(container.repository.openLootChest(itemId))
            } finally {
                chestMutex.unlock()
            }
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

    fun gamble(
        slot: ItemSlot? = null,
        targetSlots: Set<ItemSlot>? = null,
        onResult: (Result<GearInstanceEntity>) -> Unit
    ) {
        viewModelScope.launch {
            if (!gambleMutex.tryLock()) {
                onResult(Result.failure(IllegalStateException("Transaction in progress...")))
                return@launch
            }
            try {
                onResult(container.repository.gambleMysteryGear(targetSlot = slot, targetSlots = targetSlots))
            } finally {
                gambleMutex.unlock()
            }
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
    val onGamble: (ItemSlot?, Set<ItemSlot>?, (Result<GearInstanceEntity>) -> Unit) -> Unit = { _, _, _ -> },
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
    val shopRows = remember(state.items) {
        state.items
            .filter { it.slot.isStackable() }
            .map { DisplayItem(it) }
    }

    val sourceRows = when (tab) {
        MarketTab.Shop -> shopRows
        MarketTab.Inventory -> stackRows + ownedRows
        MarketTab.Armory -> ownedRows
        MarketTab.Forge -> ownedRows
    }
    val runeCatalog = remember(state.items) {
        state.items.filter { it.slot == ItemSlot.RUNE }.associateBy { it.id }
    }
    val visibleRows = sourceRows
        .filter { row -> selectedSlot == null || row.item.slot == selectedSlot }
        .filter { row -> row.matches(filter) }
        .sortedWith(sort.comparator(runeCatalog))

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
                            val resetNeeded = ProgressionRules.isGambleResetNeeded(character.lastGambleResetEpochMs)
                            val attemptsToday = if (resetNeeded) 0 else character.dailyGambleCount
                            val gamblesLeft = (ProgressionRules.MAX_DAILY_GAMBLES - attemptsToday).coerceAtLeast(0)
                            val gambleCost = ProgressionRules.mysteryGambleCost(character.level, attemptsToday)
                            val canGamble = gamblesLeft > 0 && character.gold >= gambleCost
                            val workedOutToday = character.lastWorkoutDay == (System.currentTimeMillis() / (86400 * 1000L))

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
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text("The Mystic Goblin's Cache", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Gold)
                                                Text("Daily: $attemptsToday/${ProgressionRules.MAX_DAILY_GAMBLES}", style = MaterialTheme.typography.labelSmall, color = if (gamblesLeft > 0) Gold else androidx.compose.ui.graphics.Color.Red)
                                            }
                                            Text(
                                                if (workedOutToday || character.streak > 0) "⚡ Workout Luck Active! (+Streak Rarity Bonus)"
                                                else "Gamble for mystery gear! (Log workouts to boost Epic/Legendary luck)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.75f)
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                actions.onGamble(null, setOf(ItemSlot.WEAPON)) { res ->
                                                    res.onSuccess { inst ->
                                                        AudioEffects.playLevelUp()
                                                        actions.onNotify("🎲 Won: ${inst.rarity} weapon!")
                                                    }.onFailure { err -> actions.onNotify(err.message ?: "Gamble failed") }
                                                }
                                            },
                                            enabled = canGamble,
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                        ) {
                                            Text("Weapon\n($gambleCost g)", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                                        }
                                        Button(
                                            onClick = {
                                                actions.onGamble(null, ArmorSlots) { res ->
                                                    res.onSuccess { inst ->
                                                        AudioEffects.playLevelUp()
                                                        actions.onNotify("🎲 Won: ${inst.rarity} armor!")
                                                    }.onFailure { err -> actions.onNotify(err.message ?: "Gamble failed") }
                                                }
                                            },
                                            enabled = canGamble,
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                        ) {
                                            Text("Armor\n($gambleCost g)", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                                        }
                                        Button(
                                            onClick = {
                                                actions.onGamble(null, setOf(ItemSlot.TRINKET)) { res ->
                                                    res.onSuccess { inst ->
                                                        AudioEffects.playLevelUp()
                                                        actions.onNotify("🎲 Won: ${inst.rarity} trinket!")
                                                    }.onFailure { err -> actions.onNotify(err.message ?: "Gamble failed") }
                                                }
                                            },
                                            enabled = canGamble,
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
                    val rarity = row.instance?.rarity?.let { GearRarity.fromName(it) } ?: GearRarity.COMMON
                    val equippedId = character.equippedIds()[row.item.slot]
                    val equippedRow = ownedRows.firstOrNull { it.instance?.id == equippedId }
                    val comparison = if (row.item.slot.isEquippable()) {
                        GearComparison.compareGear(
                            item = row.item,
                            instance = row.instance,
                            equipped = equippedRow?.instance?.let { OwnedGear(it, equippedRow.item) },
                            equippedCatalog = equippedRow?.item,
                            runeCatalog = runeCatalog,
                        )
                    } else {
                        null
                    }
                    if (row.item.slot.isEquippable()) {
                        GearCompactTile(
                            item = row.item,
                            instance = row.instance,
                            rarity = rarity,
                            comparison = comparison,
                            filledSocketCount = filledSocketCount(row.instance),
                            maxSockets = GearSockets.slotsForTier(row.item.tier),
                            equipped = row.equipped,
                            runeCatalog = runeCatalog,
                            selected = row.instance?.id in selectedFuseIds,
                            forgeMode = tab == MarketTab.Forge,
                            onClick = {
                                if (tab == MarketTab.Forge && row.instance != null) {
                                    val id = row.instance.id
                                    selectedFuseIds = if (id in selectedFuseIds) {
                                        selectedFuseIds - id
                                    } else if (selectedFuseIds.size < 3) {
                                        selectedFuseIds + id
                                    } else {
                                        selectedFuseIds
                                    }
                                } else {
                                    selectedItem = row
                                }
                            }
                        )
                    } else {
                        StackableMarketTile(item = row.item, onClick = { selectedItem = row })
                    }
                }
            }
        }
    }

    selectedItem?.let { selected ->
        val row = ownedRows.firstOrNull { it.key == selected.key } ?: selected
        val equippedId = character.equippedIds()[row.item.slot]
        val equippedRow = if (equippedId != null) ownedRows.firstOrNull { it.instance?.id == equippedId } else null
        val availableRunes = state.items.filter { it.slot == ItemSlot.RUNE && it.quantity > 0 }
        val comparison = if (row.item.slot.isEquippable() && !row.equipped) {
            GearComparison.compareGear(
                item = row.item,
                instance = row.instance,
                equipped = equippedRow?.instance?.let { OwnedGear(it, equippedRow.item) },
                equippedCatalog = equippedRow?.item,
                runeCatalog = runeCatalog,
            )
        } else {
            null
        }
        val inspectActions = GearInspectActions(
            showBuy = tab == MarketTab.Shop && !row.owned,
            showEquip = row.item.slot.isEquippable() && row.instance != null,
            showUpgrade = row.item.slot.isEquippable() && row.instance != null,
            showSalvage = row.item.slot.isEquippable() && row.instance != null,
            showSellGear = row.item.slot.isEquippable() && row.instance != null,
            showReforge = row.item.slot.isEquippable() && row.instance != null,
            showConsumableUse = row.item.slot == ItemSlot.CONSUMABLE,
            showOpenChest = row.item.slot == ItemSlot.LOOT_CHEST,
            showSellMaterial = row.item.slot == ItemSlot.MATERIAL,
        )
        ModalBottomSheet(onDismissRequest = { selectedItem = null }) {
            GearInspectSheet(
                target = row.toInspectTarget(),
                character = character,
                comparison = comparison,
                equippedName = equippedRow?.item?.name,
                usable = usable(row.item),
                runesCatalog = state.items.filter { it.slot == ItemSlot.RUNE },
                availableRunes = availableRunes,
                actions = inspectActions,
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
                onReforge = {
                    row.instance?.id?.let { id ->
                        actions.onReforgeGear(id) { result ->
                            result.onSuccess { actions.onNotify("Traits reforged!") }
                                .onFailure { actions.onNotify(it.message ?: "Reforge failed.") }
                        }
                    }
                },
                onSocketRune = { slotIdx, runeId ->
                    row.instance?.id?.let { instId ->
                        actions.onSocketRune(instId, slotIdx, runeId)
                    }
                },
                onClearRune = { slotIdx ->
                    row.instance?.id?.let { instId ->
                        actions.onClearRune(instId, slotIdx)
                    }
                },
                onRuneFeedback = actions.onNotify,
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



private fun DisplayItem.matches(filter: ItemFilter): Boolean = when (filter) {
    ItemFilter.All -> true
    ItemFilter.Weapons -> item.slot == ItemSlot.WEAPON
    ItemFilter.Armor -> item.slot in setOf(ItemSlot.HEAD, ItemSlot.CHEST, ItemSlot.HANDS, ItemSlot.LEGS, ItemSlot.FEET)
    ItemFilter.Runes -> item.slot == ItemSlot.RUNE
    ItemFilter.Materials -> item.slot == ItemSlot.MATERIAL
    ItemFilter.Chests -> item.slot == ItemSlot.LOOT_CHEST
    ItemFilter.Consumables -> item.slot == ItemSlot.CONSUMABLE
}

private fun ItemSort.comparator(runeCatalog: Map<Long, ItemEntity>): Comparator<DisplayItem> = when (this) {
    ItemSort.Tier -> compareByDescending<DisplayItem> { it.item.tier }.thenBy { it.item.name }
    ItemSort.Power -> compareByDescending<DisplayItem> { it.power(runeCatalog) }.thenBy { it.item.name }
    ItemSort.Slot -> compareBy<DisplayItem> { it.item.slot.ordinal }.thenByDescending { it.item.tier }.thenBy { it.item.name }
    ItemSort.Price -> compareByDescending<DisplayItem> { it.item.price }.thenBy { it.item.name }
    ItemSort.Quantity -> compareByDescending<DisplayItem> { it.item.quantity }.thenBy { it.item.name }
}

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
