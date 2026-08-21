package com.fitnessquest.rpg.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.FitQuestApp
import com.fitnessquest.rpg.data.OwnedGear
import com.fitnessquest.rpg.data.db.*
import com.fitnessquest.rpg.data.export.AvatarExporter
import com.fitnessquest.rpg.data.export.ExportFormat
import com.fitnessquest.rpg.data.wear.WearPresenceState
import com.fitnessquest.rpg.domain.*
import com.fitnessquest.rpg.ui.LocalSnackbarHostState
import com.fitnessquest.rpg.ui.appContainer
import com.fitnessquest.rpg.ui.components.*
import com.fitnessquest.rpg.ui.theme.*
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.fitnessquest.rpg.domain.mastery.CanonicalMovement
import com.fitnessquest.rpg.domain.mastery.MasteryProgression
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.time.Duration.Companion.minutes

data class HeroUiState(
    val character: CharacterEntity? = null,
    val gear: Map<ItemSlot, ItemEntity> = emptyMap(),
    val combat: CombatStats? = null,
    val setPieces: Int = 0,
    val recentSessions: List<SessionEntity> = emptyList(),
    val ownedGear: List<OwnedGear> = emptyList(),
    val runes: List<ItemEntity> = emptyList(),
    val allClassProgress: List<ClassProgressEntity> = emptyList(),
    val movementMastery: List<MovementMasteryEntity> = emptyList()
)

class HeroViewModel(private val container: AppContainer) : ViewModel() {

    val clockTick: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    private val coreState: Flow<HeroUiState> = combine(
        container.repository.character,
        container.repository.ownedGear,
        container.repository.items.map { list -> list.filter { it.slot == ItemSlot.RUNE } },
        container.repository.sessions.map { it.take(10) },
        container.repository.allClassProgress
    ) { character, owned, runes, recent, allProgress ->
        val gearMap = if (character != null) container.repository.equippedGear(character) else emptyMap()
        val combat = if (character != null) container.repository.combatStatsFor(character) else null
        val setPieces = if (character != null) GameMath.setPieceCount(character, gearMap.values.toList()) else 0
        HeroUiState(
            character = character,
            gear = gearMap,
            combat = combat,
            setPieces = setPieces,
            recentSessions = recent,
            ownedGear = owned,
            runes = runes,
            allClassProgress = allProgress
        )
    }

    val uiState: StateFlow<HeroUiState> = combine(
        coreState,
        container.repository.observeMovementMastery()
    ) { core, masteryList ->
        core.copy(movementMastery = masteryList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HeroUiState())

    init {
        viewModelScope.launch {
            clockTick
                .map { LocalDate.now().toEpochDay() }
                .distinctUntilChanged()
                .collect { day ->
                    handleDayChange(day)
                }
        }
    }

    val stepsToday: StateFlow<Int> = container.steps.stepsToday
    val stepTracking: StateFlow<Boolean> = container.steps.tracking
    val wearPresence: StateFlow<WearPresenceState> = container.wearPresence.state
    val isPremium: StateFlow<Boolean> = container.prefs.isPremium

    val imperial: StateFlow<Boolean> = container.prefs.imperial

    val bounties: StateFlow<List<Bounty>> = combine(
        uiState.map { it.character }.filterNotNull(),
        container.steps.stepsToday,
        container.repository.character.map { it.waterGlasses },
        container.repository.character.map { it.stretchDone },
        container.repository.bodyMetricHistory.map { history ->
            val today = LocalDate.now().toEpochDay()
            history.any { LocalDate.ofEpochDay(it.timestamp / (24 * 60 * 60 * 1000)).toEpochDay() == today }
        }
    ) { c, steps, water, stretch, weightDone ->
        val won = c.battlesWon - c.bountyBattlesStart
        val claimed = c.claimedBounties.split(",").toSet()
        
        buildList {
            add(Bounty(
                id = "b_battle",
                title = "⚔️ Defeat 3 Monsters",
                rewardText = "25💰 10⚡",
                progressText = "$won / 3",
                isCompleted = won >= 3,
                isClaimed = "b_battle" in claimed
            ))
            add(Bounty(
                id = "b_steps",
                title = "👟 Walk ${"%,d".format(3000)} Steps",
                rewardText = "40💰 +25🧪",
                progressText = "${"%,d".format(steps)} / ${"%,d".format(3000)}",
                isCompleted = steps >= 3000,
                isClaimed = "b_steps" in claimed,
                canLogProgress = !("b_steps" in claimed),
                logLabel = "Log Steps"
            ))
            add(Bounty(
                id = "b_weight",
                title = "⚖️ Daily Weight In",
                rewardText = "20💰 10⚡",
                progressText = if (weightDone) "1 / 1" else "0 / 1",
                isCompleted = weightDone,
                isClaimed = "b_weight" in claimed,
                canLogProgress = !weightDone,
                logLabel = "I weighed in"
            ))
            add(Bounty(
                id = "b_water",
                title = "💧 Drink 8 Glasses of Water",
                rewardText = "15💰 5⚡",
                progressText = "$water / 8",
                isCompleted = water >= 8,
                isClaimed = "b_water" in claimed,
                canLogProgress = true,
                logLabel = "+1 Glass"
            ))
            add(Bounty(
                id = "b_stretch",
                title = "🧘 Flexibility Stretch",
                rewardText = "20💰 +15🧪",
                progressText = if (stretch) "1 / 1" else "0 / 1",
                isCompleted = stretch,
                isClaimed = "b_stretch" in claimed,
                canLogProgress = true,
                logLabel = "I stretched"
            ))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyCampaigns: StateFlow<List<WeeklyCampaign>> = combine(
        uiState.map { it.character }.filterNotNull(),
        container.repository.sessions
    ) { c, sessions ->
        val weekStart = weekStart(LocalDate.now())
        val weekStartDay = weekStart.toEpochDay()
        val weekStartMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val won = c.battlesWon - container.prefs.campaignBattlesStart(weekStartDay, c.battlesWon)
        val workoutsThisWeek = sessions.count { it.startedAt >= weekStartMillis }
        val claimed = c.claimedCampaigns.split(",").toSet()
        
        listOf(
            WeeklyCampaign(
                id = "c_warrior",
                title = "🏰 Frontier Patrol",
                flavor = "Defeat 15 monsters this week.",
                rewardText = "150💰 +100🧪",
                progressText = "$won / 15",
                progress = (won / 15f).coerceIn(0f, 1f),
                isCompleted = won >= 15,
                isClaimed = "c_warrior" in claimed
            ),
            WeeklyCampaign(
                id = "c_train",
                title = "🏋️ Legendary Discipline",
                flavor = "Complete 4 workouts this week.",
                rewardText = "200💰 +150🧪",
                progressText = "$workoutsThisWeek / 4",
                progress = (workoutsThisWeek / 4f).coerceIn(0f, 1f),
                isCompleted = workoutsThisWeek >= 4,
                isClaimed = "c_train" in claimed
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasClaimableSaga: StateFlow<Boolean> = combine(
        bounties,
        weeklyCampaigns
    ) { bList, cList ->
        bList.any { it.isCompleted && !it.isClaimed } || cList.any { it.isCompleted && !it.isClaimed }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val bountyResetLabel: StateFlow<String> = clockTick.map { now ->
        val zone = ZoneId.systemDefault()
        val tomorrow = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val diff = (tomorrow - now).coerceAtLeast(0)
        val hours = diff / (1000 * 60 * 60)
        val mins = (diff / (1000 * 60)) % 60
        "Resets in ${hours}h ${mins}m"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "...")

    val campaignResetLabel: StateFlow<String> = clockTick.map { now ->
        val zone = ZoneId.systemDefault()
        val nextWeek = weekStart(LocalDate.now().plusWeeks(1)).atStartOfDay(zone).toInstant().toEpochMilli()
        val diff = (nextWeek - now).coerceAtLeast(0)
        val d = diff / (1000 * 60 * 60 * 24)
        val h = (diff / (1000 * 60 * 60)) % 24
        "New quests in ${d}d ${h}h"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "...")

    fun refreshCharacter() {
        viewModelScope.launch {
            container.repository.getCharacter()
        }
    }

    fun claimBounty(bounty: Bounty) {
        viewModelScope.launch {
            val (g, x, e) = when(bounty.id) {
                "b_battle" -> Triple(25, 0, 10)
                "b_steps" -> Triple(40, 25, 0)
                "b_weight" -> Triple(20, 0, 10)
                "b_water" -> Triple(15, 0, 5)
                "b_stretch" -> Triple(20, 15, 0)
                else -> Triple(0,0,0)
            }
            container.repository.grantBountyReward(g, x, e, bounty.id)
        }
    }

    fun claimCampaign(campaign: WeeklyCampaign) {
        viewModelScope.launch {
            val (g, x) = if (campaign.id == "c_warrior") 150 to 100 else 200 to 150
            container.repository.grantCampaignReward(g, x, campaign.id)
        }
    }

    fun updateAppearance(
        skin: Long, hair: Long, underwear: Long, eye: Long, style: String, gender: String, braColor: Long, race: String
    ) {
        viewModelScope.launch {
            container.repository.updateAppearance(skin, hair, underwear, eye, style, gender, braColor, race)
        }
    }

    fun logBountyProgress(bounty: Bounty) {
        if (bounty.id == "b_water") container.prefs.logWaterGlass()
        if (bounty.id == "b_stretch") container.prefs.markStretchDone()
    }

    val claimedTrophies: StateFlow<Set<String>> = container.prefs.claimedTrophies
    val lifetimeCardioKm: StateFlow<Double> = container.repository.allSetLogs.map { logs ->
        logs.filter { it.category == ExerciseCategory.CARDIO }.sumOf { it.distanceKm }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun claimTrophyReward(trophy: Trophy) {
        viewModelScope.launch {
            container.repository.grantTrophyReward(gold = 100, trophyId = trophy.id)
        }
    }

    fun claimIdleRewards(onResult: (RewardBatch?) -> Unit) {
        viewModelScope.launch {
            onResult(container.repository.claimIdleRewards())
        }
    }

    fun logWeight(weight: Double) {
        viewModelScope.launch {
            container.repository.logWeight(weight)
        }
    }

    fun recordManualSteps(totalSteps: Int) {
        viewModelScope.launch {
            container.steps.recordManualSteps(totalSteps)
        }
    }

    private fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun handleDayChange(todayEpochDay: Long) {
        viewModelScope.launch {
            val wonAtStart = container.repository.getCharacter().battlesWon
            val changed = container.prefs.ensureBountyDay(todayEpochDay, wonAtStart)
            if (changed) {
                val weekStartDay = weekStart(LocalDate.now()).toEpochDay()
                container.prefs.ensureCampaignWeek(weekStartDay, wonAtStart)
            }
        }
    }

    fun chooseClass(cls: CharacterClass) {
        viewModelScope.launch { container.repository.chooseClass(cls) }
    }

    fun allocateStat(stat: String) {
        viewModelScope.launch { container.repository.allocateStatPoint(stat) }
    }

    fun equip(instanceId: Long) {
        viewModelScope.launch { container.repository.equipItem(instanceId) }
    }

    fun socketRune(instanceId: Long, slotIndex: Int, runeId: Long) {
        viewModelScope.launch { container.repository.socketRune(instanceId, slotIndex, runeId) }
    }

    fun clearRune(instanceId: Long, slotIndex: Int) {
        viewModelScope.launch { container.repository.clearRune(instanceId, slotIndex) }
    }

    fun reforgeGear(instanceId: Long) {
        viewModelScope.launch {
            container.repository.reforgeGearInstanceTraits(instanceId)
        }
    }

    fun setDruidForm(form: String) {
        viewModelScope.launch { container.repository.setDruidForm(form) }
    }

    fun switchJob(newClass: CharacterClass) {
        viewModelScope.launch {
            container.repository.switchJob(newClass)
        }
    }

    fun setGlamour(slot: ItemSlot, catalogItemId: Long?) {
        viewModelScope.launch { container.repository.setGlamour(slot, catalogItemId) }
    }

    fun clearGlamour(slot: ItemSlot) {
        viewModelScope.launch { container.repository.clearGlamour(slot) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { 
                HeroViewModel(appContainer) 
            }
        }
    }
}

@Composable
fun HeroScreen(
    viewModel: HeroViewModel = viewModel(factory = HeroViewModel.Factory),
    onStartWorkout: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val imperial by viewModel.imperial.collectAsState()
    val sagaReady by viewModel.hasClaimableSaga.collectAsState()
    val bounties by viewModel.bounties.collectAsState()
    val bountyResetLabel by viewModel.bountyResetLabel.collectAsState()
    val campaigns by viewModel.weeklyCampaigns.collectAsState()
    val campaignResetLabel by viewModel.campaignResetLabel.collectAsState()
    val claimedTrophies by viewModel.claimedTrophies.collectAsState()
    val lifetimeCardioKm by viewModel.lifetimeCardioKm.collectAsState()
    val wear by viewModel.wearPresence.collectAsState()
    val stepsToday by viewModel.stepsToday.collectAsState()

    HeroScreenContent(
        state = state,
        isPremium = isPremium,
        imperial = imperial,
        sagaReady = sagaReady,
        stepsToday = stepsToday,
        bounties = bounties,
        bountyResetLabel = bountyResetLabel,
        campaigns = campaigns,
        campaignResetLabel = campaignResetLabel,
        claimedTrophies = claimedTrophies,
        lifetimeCardioKm = lifetimeCardioKm,
        wearLinked = wear.watchLinked,
        actions = HeroActions(
            onRefreshCharacter = viewModel::refreshCharacter,
            onDruidFormChange = viewModel::setDruidForm,
            onClaimIdleRewards = viewModel::claimIdleRewards,
            onLogWeight = viewModel::logWeight,
            onClaimTrophyReward = viewModel::claimTrophyReward,
            onSwitchJob = viewModel::switchJob,
            onAllocateStat = viewModel::allocateStat,
            onEquipItem = viewModel::equip,
            onReforgeGear = viewModel::reforgeGear,
            onSocketRune = viewModel::socketRune,
            onClearRune = viewModel::clearRune,
            onClaimBounty = viewModel::claimBounty,
            onLogBountyProgress = viewModel::logBountyProgress,
            onClaimCampaign = viewModel::claimCampaign,
            onRecordManualSteps = viewModel::recordManualSteps,
            onStartWorkout = onStartWorkout,
            onSetGlamour = viewModel::setGlamour,
            onClearGlamour = viewModel::clearGlamour,
            updateAppearance = viewModel::updateAppearance
        )
    )
}

data class HeroActions(
    val onRefreshCharacter: () -> Unit = {},
    val onAvatarClick: () -> Unit = {},
    val onDruidFormChange: (String) -> Unit = {},
    val onClaimIdleRewards: ((RewardBatch?) -> Unit) -> Unit = {},
    val onLogWeight: (Double) -> Unit = {},
    val onRecordManualSteps: (Int) -> Unit = {},
    val onClaimTrophyReward: (Trophy) -> Unit = {},
    val onSwitchJob: (CharacterClass) -> Unit = {},
    val onAllocateStat: (String) -> Unit = {},
    val onEquipItem: (Long) -> Unit = {},
    val onReforgeGear: (Long) -> Unit = {},
    val onSocketRune: (Long, Int, Long) -> Unit = { _, _, _ -> },
    val onClearRune: (Long, Int) -> Unit = { _, _ -> },
    val onSetGlamour: (ItemSlot, Long?) -> Unit = { _, _ -> },
    val onClearGlamour: (ItemSlot) -> Unit = {},
    val onClaimBounty: (Bounty) -> Unit = {},
    val onLogBountyProgress: (Bounty) -> Unit = {},
    val onClaimCampaign: (WeeklyCampaign) -> Unit = {},
    val onStartWorkout: () -> Unit = {},
    val updateAppearance: (Long, Long, Long, Long, String, String, Long, String) -> Unit = { _, _, _, _, _, _, _, _ -> }
)

@Composable
fun HeroScreenContent(
    state: HeroUiState,
    isPremium: Boolean,
    imperial: Boolean,
    sagaReady: Boolean,
    stepsToday: Int = 0,
    bounties: List<Bounty>,
    bountyResetLabel: String,
    campaigns: List<WeeklyCampaign>,
    campaignResetLabel: String,
    claimedTrophies: Set<String>,
    lifetimeCardioKm: Double,
    wearLinked: Boolean,
    actions: HeroActions
) {
    val character = state.character ?: return
    var selectedTab by remember { mutableIntStateOf(0) }
    var pickerSlot by remember { mutableStateOf<ItemSlot?>(null) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var rewardReveal by remember { mutableStateOf<RewardBatch?>(null) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showManualStepsDialog by remember { mutableStateOf(false) }
    var showPedometerScanDialog by remember { mutableStateOf(false) }

    val cls = character.characterClass ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (character.idleKills > 0) {
        IdleRewardsModal(
            character = character, 
            onClaim = {
                actions.onClaimIdleRewards { batch ->
                    rewardReveal = batch
                }
            }
        )
    }

    if (showWeightDialog) {
        WeightLogDialog(
            currentWeightKg = character.bodyWeightKg ?: 75.0,
            imperial = imperial,
            onDismiss = { showWeightDialog = false },
            onSave = { weight: Double ->
                actions.onLogWeight(weight)
                showWeightDialog = false
            }
        )
    }

    if (showManualStepsDialog) {
        ManualStepEntryDialog(
            currentStepsToday = stepsToday,
            onDismiss = { showManualStepsDialog = false },
            onConfirm = { steps ->
                actions.onRecordManualSteps(steps)
                showManualStepsDialog = false
            },
            onLaunchScan = {
                showManualStepsDialog = false
                showPedometerScanDialog = true
            }
        )
    }

    if (showPedometerScanDialog) {
        PedometerScanDialog(
            currentStepsToday = stepsToday,
            onDismiss = { showPedometerScanDialog = false },
            onConfirm = { steps ->
                actions.onRecordManualSteps(steps)
                showPedometerScanDialog = false
            }
        )
    }

    rewardReveal?.let { batch ->
        RewardRevealDialog(batch = batch, onDismiss = { rewardReveal = null })
    }

    if (showAvatarDialog) {
        AvatarCustomizationDialog(
            character = character,
            isPremium = isPremium,
            onDismiss = { showAvatarDialog = false },
            onClassChange = { actions.onSwitchJob(it) },
            onSave = { skinColor, hairColor, underwearColor, eyeColor, hairStyle, gender, braColor, race ->
                actions.updateAppearance(
                    skinColor, hairColor, underwearColor, eyeColor, hairStyle, gender, braColor, race
                )
                showAvatarDialog = false
            }
        )
    }

    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val statsListState = rememberLazyListState()
    val onAllocatePointsClick: () -> Unit = {
        selectedTab = 0
        scope.launch {
            statsListState.animateScrollToItem(2)
        }
    }

    val equipAnimState = rememberEquipAnimationState()
    val highestEquippedRarity = remember(state.gear, state.ownedGear, state.character) {
        val equippedIds = state.character.equippedIds().values.filterNotNull().toSet()
        val rarities = state.ownedGear
            .filter { it.instance.id in equippedIds }
            .map { GearRarity.fromName(it.instance.rarity) }
        rarities.maxOrNull() ?: GearRarity.COMMON
    }

    EmbersOverlay(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            Row(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    HeroHeaderBanner(
                        character = character,
                        cls = cls,
                        gear = state.gear,
                        highestRarity = highestEquippedRarity,
                        equipAnimState = equipAnimState,
                        onAvatarClick = { showAvatarDialog = true },
                        onDruidFormChange = actions.onDruidFormChange,
                        onAllocateClick = onAllocatePointsClick
                    )
                    HeroCurrencyBarContent(character, wearLinked)
                    HeroNextObjectiveCard(
                        character = character,
                        onAllocateClick = onAllocatePointsClick,
                        onStartWorkout = actions.onStartWorkout,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                VerticalDivider(color = Color.White.copy(alpha = 0.1f))

                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                ) {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) }
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                            Text("🛡️ Stats", modifier = Modifier.padding(10.dp))
                        }
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                            Text("⚔️ Gear", modifier = Modifier.padding(10.dp))
                        }
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📜 Saga", modifier = Modifier.padding(10.dp))
                                if (sagaReady) {
                                    Box(
                                        Modifier
                                            .size(8.dp)
                                            .background(Color.Red, CircleShape)
                                            .offset(x = (-4).dp, y = (-8).dp)
                                    )
                                }
                            }
                        }
                        Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) {
                            Text("🥋 Mastery", modifier = Modifier.padding(10.dp))
                        }
                    }

                    LazyColumn(
                        state = statsListState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (selectedTab) {
                            0 -> statsTabContent(this, character, cls, state, isPremium, snackbar, scope, actions)
                            1 -> gearTab(this, state, cls, character, highestEquippedRarity, equipAnimState, onEquipClick = { pickerSlot = it })
                            2 -> sagaTabContent(this, character, bounties, bountyResetLabel, campaigns, campaignResetLabel, claimedTrophies, lifetimeCardioKm, imperial, actions, onLogWeight = { showWeightDialog = true }, onLogSteps = { showManualStepsDialog = true })
                            3 -> masteryTab(this, state.movementMastery, imperial)
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                HeroHeaderBanner(
                    character = character,
                    cls = cls,
                    gear = state.gear,
                    highestRarity = highestEquippedRarity,
                    equipAnimState = equipAnimState,
                    onAvatarClick = { showAvatarDialog = true },
                    onDruidFormChange = actions.onDruidFormChange,
                    onAllocateClick = onAllocatePointsClick
                )

                HeroCurrencyBarContent(character, wearLinked)

                HeroNextObjectiveCard(
                    character = character,
                    onAllocateClick = onAllocatePointsClick,
                    onStartWorkout = actions.onStartWorkout
                )

                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) }
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("🛡️ Stats", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("⚔️ Gear", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📜 Saga", modifier = Modifier.padding(12.dp))
                            if (sagaReady) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .background(Color.Red, CircleShape)
                                        .offset(x = (-4).dp, y = (-8).dp)
                                )
                            }
                        }
                    }
                    Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) {
                        Text("🥋 Mastery", modifier = Modifier.padding(12.dp))
                    }
                }

                LazyColumn(
                    state = statsListState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> statsTabContent(this, character, cls, state, isPremium, snackbar, scope, actions)
                        1 -> gearTab(this, state, cls, character, highestEquippedRarity, equipAnimState, onEquipClick = { pickerSlot = it })
                        2 -> sagaTabContent(this, character, bounties, bountyResetLabel, campaigns, campaignResetLabel, claimedTrophies, lifetimeCardioKm, imperial, actions, onLogWeight = { showWeightDialog = true }, onLogSteps = { showManualStepsDialog = true })
                        3 -> masteryTab(this, state.movementMastery, imperial)
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    pickerSlot?.let { slot ->
        SlotPickerSheet(
            slot = slot,
            character = character,
            gear = state.gear,
            owned = state.ownedGear.filter { it.catalog.slot == slot },
            equippedId = state.gear[slot]?.id,
            runes = state.runes,
            allClassProgress = state.allClassProgress,
            onEquip = { instanceId ->
                val targetGear = state.ownedGear.find { it.instance.id == instanceId }
                val targetRarity = targetGear?.instance?.rarity?.let { GearRarity.fromName(it) } ?: GearRarity.COMMON
                actions.onEquipItem(instanceId)
                equipAnimState.triggerEquip(slot, targetRarity)
            },
            onSocketRune = actions.onSocketRune,
            onClearRune = actions.onClearRune,
            onReforge = actions.onReforgeGear,
            onDismiss = { pickerSlot = null }
        )
    }
}

@Composable
private fun HeroHeaderBanner(
    character: CharacterEntity,
    cls: CharacterClass,
    gear: Map<ItemSlot, ItemEntity>,
    highestRarity: GearRarity = GearRarity.COMMON,
    equipAnimState: EquipAnimationState? = null,
    onAvatarClick: () -> Unit,
    onDruidFormChange: (String) -> Unit,
    onAllocateClick: () -> Unit = {}
) {
    val biome = Biome.fromName(character.currentBiome)
    val colors = when (biome) {
        Biome.MEADOWLANDS -> listOf(Color(0xFF2D5A27), Color(0xFF1B3518))
        Biome.DARKWOOD -> listOf(Color(0xFF1B263B), Color(0xFF0D1321))
        Biome.CRYSTAL_CAVES -> listOf(Color(0xFF4A148C), Color(0xFF1A237E))
        Biome.EMBER_PEAKS -> listOf(Color(0xFFBF360C), Color(0xFF3E2723))
        Biome.FROZEN_WASTES -> listOf(Color(0xFF01579B), Color(0xFF002171))
        Biome.SHADOWFEN -> listOf(Color(0xFF263238), Color(0xFF000000))
    }
    val brush = remember(character.currentBiome) {
        Brush.verticalGradient(colors)
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp, max = 280.dp)
            .background(brush)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Full Body Avatar View
            Box(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                HeroPaperDoll(
                    clazz = cls,
                    gear = gear,
                    appearance = character.toAppearance(),
                    highestRarity = highestRarity,
                    equipAnimationState = equipAnimState,
                    modifier = Modifier.fillMaxSize(),
                    expression = if (character.freeStatPoints > 0) AvatarExpression.VICTORIOUS else AvatarExpression.CALM,
                    onAvatarClick = onAvatarClick
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(Modifier.weight(0.55f)) {
                val nameFontSize = when {
                    character.name.length > 14 -> 18.sp
                    character.name.length > 9 -> 21.sp
                    else -> 26.sp
                }
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = nameFontSize,
                        lineHeight = (nameFontSize.value + 4).sp
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Level ${character.level} ${cls.label}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = CharacterRace.fromStored(character.race).label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Parchment.copy(alpha = 0.7f)
                )

                Spacer(Modifier.height(4.dp))
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                Toast.makeText(context, "⏳ Generating Hero Wallpaper...", Toast.LENGTH_SHORT).show()
                                val res = AvatarExporter.exportAvatarGraphic(context, character, cls, ExportFormat.WALLPAPER, gear)
                                res.fold(
                                    onSuccess = { uri ->
                                        Toast.makeText(context, "✅ Wallpaper saved to Pictures/FitQuest gallery!", Toast.LENGTH_LONG).show()
                                        AvatarExporter.launchSetWallpaperIntent(context, uri)
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(context, "❌ Export failed: ${err.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("📸 Wallpaper", fontSize = 10.sp, color = Gold)
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                Toast.makeText(context, "⏳ Generating Watch Face Graphic...", Toast.LENGTH_SHORT).show()
                                val res = AvatarExporter.exportAvatarGraphic(context, character, cls, ExportFormat.WATCH_FACE, gear)
                                res.fold(
                                    onSuccess = { uri ->
                                        Toast.makeText(context, "✅ Watch Face saved to gallery & synced to watch!", Toast.LENGTH_LONG).show()
                                        AvatarExporter.launchSetWallpaperIntent(context, uri)
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(context, "❌ Export failed: ${err.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("⌚ Watch Face", fontSize = 10.sp, color = Gold)
                    }
                }

                // Form / Stance Controls
                if (cls.name == "DRUID" || cls.name == "SUMMONER" || cls.name == "DRAGOON" || cls.name == "RANGER") {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (cls.name == "RANGER") {
                            FilterChip(
                                selected = character.druidForm == "WOLF" || character.druidForm == "HUMAN" || character.druidForm.isEmpty(),
                                onClick = { onDruidFormChange("WOLF") },
                                label = { Text("🐺 Wolf", fontSize = 12.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                            val canFalcon = character.level >= 5
                            FilterChip(
                                selected = character.druidForm == "FALCON",
                                onClick = { if (canFalcon) onDruidFormChange("FALCON") },
                                enabled = canFalcon,
                                label = { Text("🦅 Falcon", fontSize = 12.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                            val canBear = character.level >= 10
                            FilterChip(
                                selected = character.druidForm == "BEAR",
                                onClick = { if (canBear) onDruidFormChange("BEAR") },
                                enabled = canBear,
                                label = { Text("🐻 Bear", fontSize = 12.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                        } else {
                            FilterChip(
                                selected = character.druidForm == "HUMAN",
                                onClick = { onDruidFormChange("HUMAN") },
                                label = { Text("👤", fontSize = 12.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                        
                        if (cls.name == "DRUID") {
                            val canBear = character.level >= 1
                            FilterChip(
                                selected = character.druidForm == "BEAR",
                                onClick = { if (canBear) onDruidFormChange("BEAR") },
                                enabled = canBear,
                                label = { Text("🐻 Bear", fontSize = 12.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                            val canPanther = character.level >= 5
                            FilterChip(
                                selected = character.druidForm == "PANTHER",
                                onClick = { if (canPanther) onDruidFormChange("PANTHER") },
                                enabled = canPanther,
                                label = { Text(if (canPanther) "🐆 Panther" else "🔒 Lv 5", fontSize = 12.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                            val canTreant = character.level >= 20
                            FilterChip(
                                selected = character.druidForm == "TREANT",
                                onClick = { if (canTreant) onDruidFormChange("TREANT") },
                                enabled = canTreant,
                                label = { Text(if (canTreant) "🌲 Treant" else "🔒 Lv 20", fontSize = 12.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                            val canMoonkin = character.level >= 35
                            FilterChip(
                                selected = character.druidForm == "MOONKIN",
                                onClick = { if (canMoonkin) onDruidFormChange("MOONKIN") },
                                enabled = canMoonkin,
                                label = { Text(if (canMoonkin) "🦉 Moonkin" else "🔒 Lv 35", fontSize = 12.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                            val canAvatar = character.level >= 50
                            FilterChip(
                                selected = character.druidForm == "AVATAR",
                                onClick = { if (canAvatar) onDruidFormChange("AVATAR") },
                                enabled = canAvatar,
                                label = { Text(if (canAvatar) "🦅 Avatar" else "🔒 Lv 50", fontSize = 12.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                        } else if (cls.name == "SUMMONER") {
                            val canIfrit = character.level >= 1
                            FilterChip(
                                selected = character.druidForm == "IFRIT",
                                onClick = { if (canIfrit) onDruidFormChange("IFRIT") },
                                enabled = canIfrit,
                                label = { Text("🔥", fontSize = 12.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                            val canShiva = character.level >= 5
                            FilterChip(
                                selected = character.druidForm == "SHIVA",
                                onClick = { if (canShiva) onDruidFormChange("SHIVA") },
                                enabled = canShiva,
                                label = { Text("❄️", fontSize = 12.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                        } else if (cls.name == "DRAGOON") {
                            val canWyvern = character.level >= 1
                            FilterChip(
                                selected = character.druidForm == "WYVERN",
                                onClick = { if (canWyvern) onDruidFormChange("WYVERN") },
                                enabled = canWyvern,
                                label = { Text("🐲", fontSize = 12.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }

                if (character.freeStatPoints > 0) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onAllocateClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("${character.freeStatPoints} Points Ready", color = NightBg, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        
        Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            SettingsIconButton()
        }
        
        // Glowing XP Bar at the very bottom
        val nextXp = GameMath.xpToNextLevel(character.level)
        val progress = (character.xp.toFloat() / nextXp.coerceAtLeast(1)).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.BottomStart)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(Gold)
            )
        }
    }
}

@Composable
private fun HeroCurrencyBarContent(character: CharacterEntity, wearLinked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FantasyToken(emoji = "💰", text = character.gold.toString())
        FantasyToken(
            emoji = "⚡", 
            text = "${character.energy}/${GameMath.MAX_ENERGY}",
            color = if (character.energy < GameMath.MAX_ENERGY) Gold else Color.White
        )
        if (character.streak > 0) {
            FantasyToken(emoji = "🔥", text = "${character.streak}d")
        }
        FantasyToken(
            emoji = if (wearLinked) "⌚" else "🚫",
            text = if (wearLinked) "Linked" else "Offline",
            color = if (wearLinked) Color(0xFF35C46A) else Color(0xFFE34D59)
        )
        Spacer(Modifier.width(16.dp))
    }
}

@Composable
private fun HeroNextObjectiveCard(
    character: CharacterEntity,
    onAllocateClick: () -> Unit,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (character.freeStatPoints <= 0) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = Gold.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "✨",
                    fontSize = 18.sp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Attribute Points Ready",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                    Text(
                        text = "You have ${character.freeStatPoints} unspent points to strengthen your Hero.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Button(
                onClick = onAllocateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .semantics {
                        contentDescription = "Allocate ${character.freeStatPoints} attribute points"
                    },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBg),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Allocate Points",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun statsTabContent(
    listScope: LazyListScope,
    character: CharacterEntity,
    cls: CharacterClass,
    state: HeroUiState,
    isPremium: Boolean,
    snackbar: SnackbarHostState,
    scope: CoroutineScope,
    actions: HeroActions
) {
    listScope.item { ReadinessCard(character = character) }

    listScope.item {
        FantasyCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Switch Job", style = MaterialTheme.typography.titleMedium, color = Gold)
                Text("Retain level & gear", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
            }
            
            val sortedJobs = CharacterClass.entries.sortedByDescending { cls ->
                state.allClassProgress.find { it.clazz == cls }?.level ?: 0
            }
            
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sortedJobs.forEach { clsItem ->
                    val progress = state.allClassProgress.find { it.clazz == clsItem }
                    val level = progress?.level ?: 1
                    val isActive = character.characterClass == clsItem
                    val isLocked = clsItem.requiresPremium && !isPremium
                    
                    Surface(
                        onClick = { 
                            if (isLocked) {
                                scope.launch {
                                    snackbar.showSnackbar("✦ Unlock Premium to access the ${clsItem.label} job.")
                                }
                            } else {
                                actions.onSwitchJob(clsItem) 
                            }
                        },
                        color = if (isActive) Gold.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isActive) BorderStroke(1.dp, Gold) else if (isLocked) BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)) else null,
                        modifier = Modifier.width(100.dp),
                        enabled = !isActive
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Text(clsItem.emoji, fontSize = 24.sp, modifier = Modifier.scale(if (isLocked) 0.8f else 1f).then(if (isLocked) Modifier.alpha(0.5f) else Modifier))
                                if (isLocked) {
                                    Text("✦", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Text(clsItem.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(if (isLocked) "Premium" else "Lv $level", style = MaterialTheme.typography.labelMedium, color = if (isLocked) Color.Gray else Gold)
                        }
                    }
                }
            }
        }
    }

    listScope.item {
        FantasyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Attributes", style = MaterialTheme.typography.titleMedium, color = Gold)
                if (character.freeStatPoints > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Gold.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Gold.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("✨", fontSize = 12.sp)
                            Text(
                                text = "${character.freeStatPoints} to spend",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Gold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            StatMeterRow(
                label = "STR ${character.strength}",
                valueText = "${character.strProgress}/${GameMath.statThreshold(character.strength)}",
                progress = (character.strProgress.toFloat() / GameMath.statThreshold(character.strength).coerceAtLeast(1)).coerceIn(0f, 1f),
                color = StatStr,
                showAdd = character.freeStatPoints > 0,
                onAdd = { actions.onAllocateStat("STR") }
            )
            StatMeterRow(
                label = "END ${character.endurance}",
                valueText = "${character.endProgress}/${GameMath.statThreshold(character.endurance)}",
                progress = (character.endProgress.toFloat() / GameMath.statThreshold(character.endurance).coerceAtLeast(1)).coerceIn(0f, 1f),
                color = StatEnd,
                showAdd = character.freeStatPoints > 0,
                onAdd = { actions.onAllocateStat("END") }
            )
            StatMeterRow(
                label = "AGI ${character.agility}",
                valueText = "${character.agiProgress}/${GameMath.statThreshold(character.agility)}",
                progress = (character.agiProgress.toFloat() / GameMath.statThreshold(character.agility).coerceAtLeast(1)).coerceIn(0f, 1f),
                color = StatAgi,
                showAdd = character.freeStatPoints > 0,
                onAdd = { actions.onAllocateStat("AGI") }
            )
            StatMeterRow(
                label = "WIL ${character.willpower}",
                valueText = character.willpower.toString(),
                progress = (character.willpower.toFloat() / GameMath.statThreshold(character.willpower).coerceAtLeast(1)).coerceIn(0f, 1f),
                color = StatWil,
                showAdd = character.freeStatPoints > 0,
                onAdd = { actions.onAllocateStat("WIL") }
            )
        }
    }

    state.combat?.let { combat ->
        listScope.item {
            FantasyCard {
                Text("Combat Power", style = MaterialTheme.typography.titleMedium, color = Gold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CombatStat("❤️", "HP", combat.maxHp)
                    CombatStat("⚔️", "ATK", combat.atk)
                    CombatStat("🛡️", "DEF", combat.def)
                    CombatStat("💨", "SPD", combat.spd)
                }
            }
        }
    }

    listScope.item {
        FantasyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Class Progression (Lv 1–50)", style = MaterialTheme.typography.titleMedium, color = Gold)
                Text("Level ${character.level}", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(4.dp))
            cls.skills.forEach { skill ->
                val unlocked = character.level >= skill.unlockLevel
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(if (unlocked) skill.emoji else "🔒", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(skill.name, fontWeight = FontWeight.Bold, color = if (unlocked) Color.White else Color.Gray)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (unlocked) Gold.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f)
                            ) {
                                Text(
                                    "Lv ${skill.unlockLevel}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (unlocked) Gold else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(skill.blurb, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = if (unlocked) 0.8f else 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMeterRow(
    label: String,
    valueText: String,
    progress: Float,
    color: Color,
    showAdd: Boolean,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            BarMeter(label, valueText, progress, color)
        }
        if (showAdd) {
            FilledIconButton(
                onClick = onAdd,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Gold,
                    contentColor = NightBg
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Allocate point to $label",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun gearTab(
    scope: LazyListScope,
    state: HeroUiState,
    cls: CharacterClass,
    character: CharacterEntity,
    highestRarity: GearRarity,
    equipAnimState: EquipAnimationState,
    onEquipClick: (ItemSlot) -> Unit
) {
    scope.item {
        FantasyCard {
            Text(
                "Hero Loadout",
                style = MaterialTheme.typography.titleMedium,
                color = Gold,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            // Centered Hero Showcase Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                HeroPaperDoll(
                    clazz = cls,
                    gear = state.gear,
                    appearance = character.toAppearance(),
                    highestRarity = highestRarity,
                    equipAnimationState = equipAnimState,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(12.dp))

            // Total Gear Stats Summary Banner
            val totalGearAtk = state.gear.values.sumOf { it.atk }
            val totalGearDef = state.gear.values.sumOf { it.def }
            val totalGearHp = state.gear.values.sumOf { it.hp }

            Surface(
                color = Color.White.copy(alpha = 0.04f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Gear ATK", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                        Text("+$totalGearAtk", fontWeight = FontWeight.Bold, color = StatStr, style = MaterialTheme.typography.titleSmall)
                    }
                    VerticalDivider(modifier = Modifier.height(24.dp), color = Color.White.copy(alpha = 0.1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Gear DEF", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                        Text("+$totalGearDef", fontWeight = FontWeight.Bold, color = ArcaneBlue, style = MaterialTheme.typography.titleSmall)
                    }
                    VerticalDivider(modifier = Modifier.height(24.dp), color = Color.White.copy(alpha = 0.1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Gear HP", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                        Text("+$totalGearHp", fontWeight = FontWeight.Bold, color = StaminaGreen, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Equipped Gear Slots List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EquippableSlots.forEach { slot ->
                    val item = state.gear[slot]
                    val instance = state.ownedGear.find { it.instance.id == item?.id }?.instance
                    val rarity = instance?.rarity?.let { GearRarity.fromName(it) } ?: GearRarity.COMMON
                    EquipmentSlot(
                        slot = slot,
                        item = item,
                        rarity = rarity,
                        upgradeLevel = instance?.upgradeLevel ?: 0,
                        isEquipped = item != null,
                        onClick = { onEquipClick(slot) }
                    )
                }
            }
            
            val setInfo = GameMath.setBonusInfo(character, state.gear.values.toList())
            val activeTier = setInfo.activeTier
            val hasActiveBonus = activeTier != SetBonusTier.NONE
            Surface(
                color = if (hasActiveBonus) Gold.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp),
                border = if (hasActiveBonus) BorderStroke(1.dp, Gold) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (hasActiveBonus) "✨ ${cls.label} Set (${setInfo.pieceCount}/5) — ${activeTier.title}" else "Armor Set: ${setInfo.pieceCount}/5 pieces equipped",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (hasActiveBonus) FontWeight.Bold else FontWeight.Normal,
                        color = if (hasActiveBonus) Gold else Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = activeTier.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasActiveBonus) Gold.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun sagaTabContent(
    listScope: LazyListScope,
    character: CharacterEntity,
    bounties: List<Bounty>,
    bountyResetLabel: String,
    campaigns: List<WeeklyCampaign>,
    campaignResetLabel: String,
    claimedTrophies: Set<String>,
    lifetimeCardioKm: Double,
    imperial: Boolean,
    actions: HeroActions,
    onLogWeight: () -> Unit,
    onLogSteps: () -> Unit
) {
    listScope.item {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (bounties.isNotEmpty()) {
                DailyBountyCard(
                    bounties = bounties,
                    resetLabel = bountyResetLabel,
                    onClaim = actions.onClaimBounty,
                    onLogProgress = { bounty ->
                        if (bounty.id == "b_weight") {
                            onLogWeight()
                        } else if (bounty.id == "b_steps") {
                            onLogSteps()
                        } else {
                            actions.onLogBountyProgress(bounty)
                        }
                    }
                )
            }

            if (campaigns.isNotEmpty()) {
                WeeklyCampaignCard(
                    campaigns = campaigns,
                    resetLabel = campaignResetLabel,
                    onClaim = actions.onClaimCampaign
                )
            }

            TrophyVaultCard(
                character = character,
                claimedIds = claimedTrophies,
                lifetimeCardioKm = lifetimeCardioKm,
                imperial = imperial,
                onClaimReward = actions.onClaimTrophyReward
            )

            FantasyCard {
                Text("Recent Adventures", style = MaterialTheme.typography.titleMedium, color = Gold)
                Text("No adventures yet.", color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun CombatStat(emoji: String, label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 18.sp)
        Text(value.toString(), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
    }
}

@Composable
private fun ReadinessCard(character: CharacterEntity) {
    val energyPct = character.energy.toFloat() / GameMath.MAX_ENERGY
    val readiness = when {
        energyPct > 0.7f -> Readiness("Heroic", "You are fully recovered and ready for a major quest.", Color(0xFF6BC96B))
        energyPct > 0.3f -> Readiness("Stable", "You have enough energy for standard training.", Gold)
        else -> Readiness("Exhausted", "Rest is advised. Energy is dangerously low.", Color(0xFFE35B5B))
    }

    FantasyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(readiness.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (energyPct > 0.5f) "⚡" else "💤", fontSize = 24.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Quest Readiness", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                Text(readiness.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = readiness.color)
            }
        }
        Text(readiness.reason, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
    }
}

private data class Readiness(val label: String, val reason: String, val color: Color)


fun itemBonusText(item: ItemEntity): String = buildList {
    if (item.atk > 0) add("+${item.atk} ATK")
    if (item.def > 0) add("+${item.def} DEF")
    if (item.hp > 0) add("+${item.hp} HP")
}.joinToString(" ")

private enum class SlotFilter(val label: String) {
    ALL("All"),
    COMPATIBLE("Equippable"),
    UPGRADED("Upgraded"),
    RARE_PLUS("Rare+"),
}

private enum class SlotSort(val label: String) {
    POWER("Power"),
    TIER("Tier"),
    RARITY("Rarity"),
    UPGRADE("Upgrade"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotPickerSheet(
    slot: ItemSlot,
    character: CharacterEntity,
    gear: Map<ItemSlot, ItemEntity>,
    owned: List<OwnedGear>,
    equippedId: Long?,
    runes: List<ItemEntity>,
    allClassProgress: List<ClassProgressEntity> = emptyList(),
    onEquip: (Long) -> Unit,
    onSocketRune: (Long, Int, Long) -> Unit,
    onClearRune: (Long, Int) -> Unit,
    onReforge: (Long) -> Unit = {},
    onDismiss: () -> Unit
) {
    var filter by remember { mutableStateOf(SlotFilter.ALL) }
    var sort by remember { mutableStateOf(SlotSort.POWER) }
    var runeSocketTarget by remember { mutableStateOf<Pair<Long, Int>?>(null) }

    val filtered = remember(owned, filter, sort, character) {
        val list = owned.filter { row ->
            when (filter) {
                SlotFilter.ALL -> true
                SlotFilter.COMPATIBLE -> row.catalog.classAffinity == null || row.catalog.classAffinity == character.characterClass
                SlotFilter.UPGRADED -> row.instance.upgradeLevel > 0
                SlotFilter.RARE_PLUS -> GearRarity.fromName(row.instance.rarity).ordinal >= GearRarity.RARE.ordinal
            }
        }
        when (sort) {
            SlotSort.POWER -> list.sortedByDescending { it.catalog.atk + it.catalog.def + it.catalog.hp / 4 + it.instance.upgradeLevel * 2 }
            SlotSort.TIER -> list.sortedByDescending { it.catalog.tier }
            SlotSort.RARITY -> list.sortedByDescending { GearRarity.fromName(it.instance.rarity).ordinal }
            SlotSort.UPGRADE -> list.sortedByDescending { it.instance.upgradeLevel }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Select ${slot.label}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${filtered.size} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SlotFilter.entries.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (owned.isEmpty()) "You don't own any items for this slot yet."
                        else "No items match the selected filter.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.instance.id }) { itemRow ->
                        val isEquipped = itemRow.instance.id == equippedId
                        val currentlyEquippedItem = gear[slot]
                        SlotPickerGearCard(
                            gear = itemRow,
                            currentlyEquippedItem = currentlyEquippedItem,
                            isEquipped = isEquipped,
                            character = character,
                            runesCatalog = runes,
                            allClassProgress = allClassProgress,
                            onClick = { onEquip(itemRow.instance.id) },
                            onOpenSocket = { slotIdx -> runeSocketTarget = itemRow.instance.id to slotIdx },
                            onClearSocket = { slotIdx -> onClearRune(itemRow.instance.id, slotIdx) },
                            onReforge = { onReforge(itemRow.instance.id) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    // Rune Picker Dialog
    runeSocketTarget?.let { (instanceId, slotIdx) ->
        val availableRunes = runes.filter { it.quantity > 0 }
        AlertDialog(
            onDismissRequest = { runeSocketTarget = null },
            title = { Text("Socket Rune into Slot ${slotIdx + 1}") },
            text = {
                if (availableRunes.isEmpty()) {
                    Text("You don't have any runes in your inventory. Defeat monsters or complete workouts to earn runes!")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Choose a rune to socket:", style = MaterialTheme.typography.bodySmall)
                        for (rune in availableRunes) {
                            Surface(
                                onClick = {
                                    onSocketRune(instanceId, slotIdx, rune.id)
                                    runeSocketTarget = null
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
                TextButton(onClick = { runeSocketTarget = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SlotPickerGearCard(
    gear: OwnedGear,
    currentlyEquippedItem: ItemEntity?,
    isEquipped: Boolean,
    character: CharacterEntity,
    runesCatalog: List<ItemEntity>,
    allClassProgress: List<ClassProgressEntity> = emptyList(),
    onClick: () -> Unit,
    onOpenSocket: (Int) -> Unit = {},
    onClearSocket: (Int) -> Unit = {},
    onReforge: () -> Unit = {}
) {
    val rarity = GearRarity.fromName(gear.instance.rarity)
    val reqLevel = ProgressionRules.requiredLevelFor(gear.catalog.tier, rarity)
    val levelMet = character.level >= reqLevel
    val classMatch = gear.catalog.classAffinity == null || gear.catalog.classAffinity == character.characterClass
    val canEquip = classMatch && levelMet
    val atkDiff = gear.instance.atk - (currentlyEquippedItem?.atk ?: 0)
    val defDiff = gear.instance.def - (currentlyEquippedItem?.def ?: 0)
    val hpDiff = gear.instance.hp - (currentlyEquippedItem?.hp ?: 0)
    val traits = GearTrait.parseTraits(gear.instance.traitIds)
    val maxSockets = GearSockets.slotsForTier(gear.catalog.tier)
    val equippedJob = allClassProgress.firstOrNull { it.clazz != character.characterClass && gear.instance.id in it.equippedIds().values }?.clazz

    Surface(
        onClick = onClick,
        enabled = canEquip,
        shape = RoundedCornerShape(12.dp),
        color = if (isEquipped) Gold.copy(alpha = 0.14f) else RarityVisuals.backgroundGlow(rarity),
        modifier = Modifier
            .fillMaxWidth()
            .rarityFrame(
                rarity = rarity,
                isSelected = isEquipped,
                isEquipped = isEquipped,
                cornerRadius = 12.dp
            )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    ItemIcon(gear.catalog, modifier = Modifier.size(34.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        gear.catalog.name,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    Text(
                        gearBonusText(gear.instance).ifBlank { "No direct stats" },
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isEquipped) {
                    Icon(Icons.Default.Check, contentDescription = "Equipped", tint = RarityVisuals.UncommonColor, modifier = Modifier.size(18.dp))
                }
            }

            if (!isEquipped) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (atkDiff != 0) HeroDeltaPill("ATK", atkDiff)
                    if (defDiff != 0) HeroDeltaPill("DEF", defDiff)
                    if (hpDiff != 0) HeroDeltaPill("HP", hpDiff)
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RarityBadge(rarity = rarity, compact = true)
                HeroGearPill("T${gear.catalog.tier}")
                if (gear.instance.upgradeLevel > 0) HeroGearPill("+${gear.instance.upgradeLevel}", color = Gold)
                if (!levelMet) {
                    HeroGearPill("Req. Lv $reqLevel", color = Color(0xFFEF5350))
                }
                if (equippedJob != null) {
                    HeroGearPill("✦ In Use: ${equippedJob.label}", color = Color(0xFF64B5F6))
                }
                for (trait in traits) {
                    HeroGearPill("${trait.emoji} ${trait.displayName}", color = Gold)
                }
            }

            // Sockets row and Reforge option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (maxSockets > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sockets:", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                        for (idx in 0 until maxSockets) {
                            val runeId = if (idx == 0) gear.instance.rune1Id else gear.instance.rune2Id
                            val rune = runesCatalog.find { it.id == runeId }
                            if (rune != null) {
                                Surface(
                                    onClick = { onClearSocket(idx) },
                                    color = MysticPurple.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, MysticPurple)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${rune.emoji} ${rune.name.take(6)}", style = MaterialTheme.typography.labelSmall, color = Gold)
                                        Spacer(Modifier.width(2.dp))
                                        Text("✕", style = MaterialTheme.typography.labelSmall, color = Color.Red.copy(alpha = 0.8f))
                                    }
                                }
                            } else {
                                Surface(
                                    onClick = { onOpenSocket(idx) },
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        "+ Rune",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
                if (ProgressionRules.canReforge(rarity)) {
                    Surface(
                        onClick = onReforge,
                        color = Gold.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("🎲 Reforge", style = MaterialTheme.typography.labelSmall, color = Gold)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun HeroDeltaPill(label: String, delta: Int) {
    val (color, prefix) = when {
        delta > 0 -> Color(0xFF4ADE80) to "+"
        delta < 0 -> Color(0xFFF87171) to ""
        else -> Color.Gray to "+"
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = "$label $prefix$delta",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun HeroGearPill(text: String, color: Color = Color.White.copy(alpha = 0.8f)) {
    Surface(
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun masteryTab(
    scope: LazyListScope,
    masteryList: List<MovementMasteryEntity>,
    imperial: Boolean
) {
    if (masteryList.isEmpty()) {
        scope.item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "No mastery tracked yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Log a workout to start building movement mastery",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    } else {
        scope.item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Levels", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${masteryList.sumOf { it.level }}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Gold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Trees Trained", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${masteryList.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Sessions", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${masteryList.sumOf { it.totalSessionsLogged }}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA))
                    }
                }
            }
        }

        scope.items(masteryList.size) { index ->
            val entity = masteryList[index]
            val canonical = runCatching { CanonicalMovement.valueOf(entity.canonicalKey) }.getOrNull()
            val movementName = canonical?.displayName ?: entity.canonicalKey.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
            val currentLevelBase = MasteryProgression.cumulativeXpForLevel(entity.level)
            val nextLevelBase = MasteryProgression.cumulativeXpForLevel(entity.level + 1)
            val neededForLevel = (nextLevelBase - currentLevelBase).coerceAtLeast(1L)
            val progress = if (entity.level >= MasteryProgression.MAX_LEVEL) 1f
                else ((entity.currentXp - currentLevelBase).toFloat() / neededForLevel.toFloat()).coerceIn(0f, 1f)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(canonical?.icon ?: "🥋", fontSize = 20.sp)
                            Text(movementName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Surface(
                            color = Gold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Gold.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "Lv ${entity.level}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = Gold
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Gold,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${entity.currentXp} XP", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                        Text(if (entity.level >= MasteryProgression.MAX_LEVEL) "MAX" else "Next: $nextLevelBase XP", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (entity.lifetimeVolumeKg > 0.0) {
                            HeroGearPill("${Units.toDisplay(entity.lifetimeVolumeKg, imperial).roundToInt()} ${if (imperial) "lbs" else "kg"} vol")
                        }
                        if (entity.lifetimeReps > 0) {
                            HeroGearPill("${entity.lifetimeReps} reps")
                        }
                        if (entity.lifetimeDistanceKm > 0.0) {
                            HeroGearPill("${"%.1f".format(Units.kmToDisplay(entity.lifetimeDistanceKm, imperial))} ${if (imperial) "mi" else "km"}")
                        }
                        if (entity.highest1RmKg > 0.0) {
                            HeroGearPill("1RM: ${Units.toDisplay(entity.highest1RmKg, imperial).roundToInt()} ${if (imperial) "lbs" else "kg"}")
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Hero Screen (Mythic Loadout)", showBackground = true, backgroundColor = 0xFF12131F)
@Composable
fun HeroScreenPreview() {
    val items = com.fitnessquest.rpg.domain.ItemCatalog.all
    val warriorWeapon = items.firstOrNull { it.name == "Dragonfang Greatsword" }
    val warriorChest = items.firstOrNull { it.name == "Aegis of the Titan" }
    val warriorHead = items.firstOrNull { it.name == "Titanforged Helm" }
    val warriorLegs = items.firstOrNull { it.name == "Titanforged Greaves" }
    val warriorFeet = items.firstOrNull { it.name == "Titanforged Sabatons" }

    val sampleGear = mutableMapOf<ItemSlot, ItemEntity>()
    if (warriorWeapon != null) sampleGear[ItemSlot.WEAPON] = warriorWeapon
    if (warriorChest != null) sampleGear[ItemSlot.CHEST] = warriorChest
    if (warriorHead != null) sampleGear[ItemSlot.HEAD] = warriorHead
    if (warriorLegs != null) sampleGear[ItemSlot.LEGS] = warriorLegs
    if (warriorFeet != null) sampleGear[ItemSlot.FEET] = warriorFeet

    val sampleOwned = sampleGear.values.map { catalog ->
        OwnedGear(
            instance = GearInstanceEntity(
                id = catalog.id,
                catalogId = catalog.id,
                rarity = GearRarity.MYTHIC.name,
                upgradeLevel = 5,
                traitIds = "vampiric,berserk"
            ),
            catalog = catalog
        )
    }

    FitQuestTheme {
        CompositionLocalProvider(
            LocalSnackbarHostState provides remember { SnackbarHostState() }
        ) {
            HeroScreenContent(
                state = HeroUiState(
                    character = CharacterEntity(
                        name = "Mythic Warlord",
                        level = 20,
                        currentBiome = Biome.EMBER_PEAKS.name,
                        gold = 4850,
                        energy = 100,
                        streak = 14,
                        characterClass = CharacterClass.WARRIOR,
                        weaponId = warriorWeapon?.id,
                        chestId = warriorChest?.id,
                        headId = warriorHead?.id,
                        legsId = warriorLegs?.id,
                        feetId = warriorFeet?.id
                    ),
                    gear = sampleGear,
                    ownedGear = sampleOwned,
                    combat = CombatStats(maxHp = 340, atk = 85, def = 72, spd = 24, critPercent = 15),
                    setPieces = 5
                ),
                isPremium = true,
                imperial = true,
                sagaReady = true,
                bounties = emptyList(),
                bountyResetLabel = "12h 30m",
                campaigns = emptyList(),
                campaignResetLabel = "5d 4h",
                claimedTrophies = emptySet(),
                lifetimeCardioKm = 12.5,
                wearLinked = true,
                actions = HeroActions()
            )
        }
    }
}
