package com.fitnessquest.rpg.ui.screens

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
    val allClassProgress: List<ClassProgressEntity> = emptyList()
)

class HeroViewModel(private val container: AppContainer) : ViewModel() {

    val uiState: StateFlow<HeroUiState> = combine(
        container.repository.character,
        container.repository.ownedGear,
        container.repository.items.map { list -> list.filter { it.slot == ItemSlot.RUNE } },
        container.repository.sessions.map { it.take(10) },
        container.repository.allClassProgress
    ) { character, owned, runes, recent, allProgress ->
        val gearMap = container.repository.equippedGear(character)
        val combat = container.repository.combatStatsFor(character)
        val setPieces = GameMath.setPieceCount(character, gearMap.values.toList())
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HeroUiState())

    val stepsToday: StateFlow<Int> = container.steps.stepsToday
    val stepTracking: StateFlow<Boolean> = container.steps.tracking
    val wearPresence: StateFlow<WearPresenceState> = container.wearPresence.state
    val isPremium: StateFlow<Boolean> = container.prefs.isPremium

    val clockTick: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    fun refreshCharacter() {
        viewModelScope.launch {
            container.repository.getCharacter()
        }
    }

    val bountyResetLabel: StateFlow<String> = clockTick.map { now ->
        val zone = ZoneId.systemDefault()
        val tomorrow = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val diff = (tomorrow - now).coerceAtLeast(0)
        val hours = diff / (1000 * 60 * 60)
        val mins = (diff / (1000 * 60)) % 60
        "Resets in %dh %dm".format(hours, mins)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "...")

    val campaignResetLabel: StateFlow<String> = clockTick.map { now ->
        val zone = ZoneId.systemDefault()
        val nextWeek = weekStart(LocalDate.now().plusWeeks(1)).atStartOfDay(zone).toInstant().toEpochMilli()
        val diff = (nextWeek - now).coerceAtLeast(0)
        val days = diff / (1000 * 60 * 60 * 24)
        val hours = (diff / (1000 * 60 * 60)) % 24
        "New quests in %dd %dh".format(days, hours)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "...")

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
                title = "👟 Walk 3,000 Steps",
                rewardText = "40💰 +25🧪",
                progressText = "${"%,d".format(steps)} / 3,000",
                isCompleted = steps >= 3000,
                isClaimed = "b_steps" in claimed
            ))
            add(Bounty(
                id = "b_weight",
                title = "⚖️ Daily Weight In",
                rewardText = "20💰 10⚡",
                progressText = if (weightDone) "1 / 1" else "0 / 1",
                isCompleted = weightDone,
                isClaimed = "b_weight" in claimed,
                canLogProgress = !weightDone,
                logLabel = "Log Weight"
            ))
            add(Bounty(
                id = "b_water",
                title = "💧 Hydrate (8 glasses)",
                rewardText = "15💰 5⚡",
                progressText = "$water / 8",
                isCompleted = water >= 8,
                isClaimed = "b_water" in claimed,
                canLogProgress = true,
                logLabel = "+1 glass"
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
        container.repository.character.map { it.sessionsCompleted }
    ) { c, totalSessions ->
        val weekStartDay = weekStart(LocalDate.now()).toEpochDay()
        val won = c.battlesWon - container.prefs.campaignBattlesStart(weekStartDay, c.battlesWon)
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
                progressText = "${c.sessionsCompleted % 5} / 4",
                progress = ((c.sessionsCompleted % 5) / 4f).coerceIn(0f, 1f),
                isCompleted = (c.sessionsCompleted % 5) >= 4,
                isClaimed = "c_train" in claimed
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        // b_weight is handled via showWeightDialog in the Composable
    }

    val claimedTrophies: StateFlow<Set<String>> = container.prefs.claimedTrophies
    val imperial: StateFlow<Boolean> = container.prefs.imperial
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

    fun claimWellnessEnergy() {
        viewModelScope.launch {
            container.repository.claimWellnessEnergy()
        }
    }

    fun logWeight(weight: Double) {
        viewModelScope.launch {
            container.repository.logWeight(weight)
        }
    }

    private fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    val usernameSet: StateFlow<Boolean> = container.prefs.usernameSet

    private val _usernameBusy = MutableStateFlow(false)
    val usernameBusy: StateFlow<Boolean> = _usernameBusy

    private val _usernameError = MutableStateFlow<String?>(null)
    val usernameError: StateFlow<String?> = _usernameError

    fun clearUsernameError() { _usernameError.value = null }
    fun tryClaimExistingName() {
        viewModelScope.launch {
            val name = container.repository.getCharacter().name
            if (name.isNotBlank() && name != "Hero") {
                claimUsername(name)
            }
        }
    }

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

    fun claimUsername(name: String, onDone: () -> Unit = {}) {
        if (name.isBlank()) return
        _usernameBusy.value = true
        _usernameError.value = null
        viewModelScope.launch {
            val result = container.usernames.claim(name)
            if (result.isSuccess) {
                container.prefs.setUsernameClaim(result.getOrThrow())
                container.repository.updateCharacter(container.repository.getCharacter().copy(name = name))
                onDone()
            } else {
                _usernameError.value = result.exceptionOrNull()?.message ?: "Name unavailable"
            }
            _usernameBusy.value = false
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

    fun setDruidForm(form: String) {
        viewModelScope.launch { container.repository.setDruidForm(form) }
    }

    fun switchJob(newClass: CharacterClass) {
        viewModelScope.launch {
            container.repository.switchJob(newClass)
        }
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
fun HeroScreen(viewModel: HeroViewModel = viewModel(factory = HeroViewModel.Factory)) {
    val state by viewModel.uiState.collectAsState()
    val character = state.character ?: return
    val usernameSet by viewModel.usernameSet.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var pickerSlot by remember { mutableStateOf<ItemSlot?>(null) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var rewardReveal by remember { mutableStateOf<RewardBatch?>(null) }
    var showWeightDialog by remember { mutableStateOf(false) }

    val cls = character.characterClass ?: return // ClassPicker handled in Nav

    if (character.idleKills > 0) {
        IdleRewardsModal(
            character = character, 
            onClaim = {
                viewModel.claimIdleRewards { batch ->
                    rewardReveal = batch
                }
            }
        )
    }

    if (showWeightDialog) {
        WeightLogDialog(
            currentWeightKg = character.bodyWeightKg ?: 75.0,
            imperial = viewModel.imperial.collectAsState().value,
            onDismiss = { showWeightDialog = false },
            onSave = { weight: Double ->
                viewModel.logWeight(weight)
                showWeightDialog = false
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
            onClassChange = { viewModel.chooseClass(it) },
            onSave = { skinColor, hairColor, underwearColor, eyeColor, hairStyle, gender, braColor, race ->
                viewModel.updateAppearance(
                    skinColor, hairColor, underwearColor, eyeColor, hairStyle, gender, braColor, race
                )
                showAvatarDialog = false
            }
        )
    }

    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    EmbersOverlay(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            Row(Modifier.fillMaxSize()) {
                // Left Pane (42%): Avatar Banner & Currency Bar
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
                        onAvatarClick = { showAvatarDialog = true },
                        onDruidFormChange = viewModel::setDruidForm
                    )
                    HeroCurrencyBar(character, viewModel)
                }

                VerticalDivider(color = Color.White.copy(alpha = 0.1f))

                // Right Pane (58%): Tab Navigation & Tab Content
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
                            Text("📜 Saga", modifier = Modifier.padding(10.dp))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (selectedTab) {
                            0 -> statsTab(this, character, cls, state, viewModel, isPremium, snackbar, scope)
                            1 -> gearTab(this, state, cls, onEquipClick = { pickerSlot = it })
                            2 -> sagaTab(this, viewModel, onLogWeight = { showWeightDialog = true })
                        }
                        
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                // Immersive Full-Body Hero Banner
                HeroHeaderBanner(
                    character = character,
                    cls = cls,
                    gear = state.gear,
                    onAvatarClick = { showAvatarDialog = true },
                    onDruidFormChange = viewModel::setDruidForm
                )

                // Dynamic Currency Bar
                HeroCurrencyBar(character, viewModel)

                // Main Tab Navigation
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
                        Text("📜 Saga", modifier = Modifier.padding(12.dp))
                    }
                }

                // Scrollable Content per Tab
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> statsTab(this, character, cls, state, viewModel, isPremium, snackbar, scope)
                        1 -> gearTab(this, state, cls, onEquipClick = { pickerSlot = it })
                        2 -> sagaTab(this, viewModel, onLogWeight = { showWeightDialog = true })
                    }
                    
                    item { Spacer(Modifier.height(80.dp)) } // Dock clearance
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
            onEquip = viewModel::equip,
            onSocketRune = viewModel::socketRune,
            onClearRune = viewModel::clearRune,
            onDismiss = { pickerSlot = null }
        )
    }
}

@Composable
private fun HeroHeaderBanner(
    character: CharacterEntity,
    cls: CharacterClass,
    gear: Map<ItemSlot, ItemEntity>,
    onAvatarClick: () -> Unit,
    onDruidFormChange: (String) -> Unit
) {
    val brush = remember {
        Brush.verticalGradient(listOf(Color(0xFF2A1F3D), Color(0xFF12131F)))
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
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
                    .fillMaxHeight()
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                CharacterAvatar(
                    clazz = cls,
                    gear = gear,
                    appearance = character.toAppearance(),
                    modifier = Modifier.fillMaxSize(),
                    detail = AvatarDetail.FULL,
                    expression = if (character.freeStatPoints > 0) AvatarExpression.VICTORIOUS else AvatarExpression.CALM
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(Modifier.weight(0.55f)) {
                val nameFontSize = when {
                    character.name.length > 14 -> 15.sp
                    character.name.length > 10 -> 18.sp
                    character.name.length > 7 -> 21.sp
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
                                android.widget.Toast.makeText(context, "⏳ Generating Hero Wallpaper...", android.widget.Toast.LENGTH_SHORT).show()
                                val res = AvatarExporter.exportAvatarGraphic(context, character, cls, ExportFormat.WALLPAPER, gear)
                                res.fold(
                                    onSuccess = { uri ->
                                        android.widget.Toast.makeText(context, "✅ Wallpaper saved to Pictures/FitQuest gallery!", android.widget.Toast.LENGTH_LONG).show()
                                        AvatarExporter.launchSetWallpaperIntent(context, uri)
                                    },
                                    onFailure = { err ->
                                        android.widget.Toast.makeText(context, "❌ Export failed: ${err.message}", android.widget.Toast.LENGTH_LONG).show()
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
                                android.widget.Toast.makeText(context, "⏳ Generating Watch Face Graphic...", android.widget.Toast.LENGTH_SHORT).show()
                                val res = AvatarExporter.exportAvatarGraphic(context, character, cls, ExportFormat.WATCH_FACE, gear)
                                res.fold(
                                    onSuccess = { uri ->
                                        android.widget.Toast.makeText(context, "✅ Watch Face saved to gallery & synced to watch!", android.widget.Toast.LENGTH_LONG).show()
                                        AvatarExporter.launchSetWallpaperIntent(context, uri)
                                    },
                                    onFailure = { err ->
                                        android.widget.Toast.makeText(context, "❌ Export failed: ${err.message}", android.widget.Toast.LENGTH_LONG).show()
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
                if (cls.name == "DRUID" || cls.name == "SUMMONER" || cls.name == "DRAGOON") {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = character.druidForm == "HUMAN",
                            onClick = { onDruidFormChange("HUMAN") },
                            label = { Text("👤", fontSize = 12.sp) },
                            modifier = Modifier.height(32.dp)
                        )
                        
                        if (cls.name == "DRUID") {
                            val canBear = character.level >= 1
                            FilterChip(
                                selected = character.druidForm == "BEAR",
                                onClick = { if (canBear) onDruidFormChange("BEAR") },
                                enabled = canBear,
                                label = { Text("🐻", fontSize = 12.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                            val canPanther = character.level >= 5
                            FilterChip(
                                selected = character.druidForm == "PANTHER",
                                onClick = { if (canPanther) onDruidFormChange("PANTHER") },
                                enabled = canPanther,
                                label = { Text("🐆", fontSize = 12.sp) },
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
                        onClick = { /* Could scroll to stats tab */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold.copy(alpha = 0.2f)),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text("${character.freeStatPoints} Points Ready", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            
            Box(Modifier.align(Alignment.Top)) {
                SettingsIconButton()
            }
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
private fun HeroCurrencyBar(character: CharacterEntity, viewModel: HeroViewModel) {
    val wear by viewModel.wearPresence.collectAsState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            emoji = if (wear.watchLinked) "⌚" else "🚫",
            text = if (wear.watchLinked) "Linked" else "Offline",
            color = if (wear.watchLinked) Color(0xFF35C46A) else Color(0xFFE34D59)
        )
    }
}

private fun statsTab(
    listScope: LazyListScope,
    character: CharacterEntity,
    cls: CharacterClass,
    state: HeroUiState,
    viewModel: HeroViewModel,
    isPremium: Boolean,
    snackbar: SnackbarHostState,
    scope: CoroutineScope
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
                sortedJobs.forEach { cls ->
                    val progress = state.allClassProgress.find { it.clazz == cls }
                    val level = progress?.level ?: 1
                    val isActive = character.characterClass == cls
                    val isLocked = cls.requiresPremium && !isPremium
                    
                    Surface(
                        onClick = { 
                            if (isLocked) {
                                scope.launch {
                                    snackbar.showSnackbar("✦ Unlock Premium to access the ${cls.label} job.")
                                }
                            } else {
                                viewModel.switchJob(cls) 
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
                                Text(cls.emoji, fontSize = 24.sp, modifier = Modifier.scale(if (isLocked) 0.8f else 1f).then(if (isLocked) Modifier.alpha(0.5f) else Modifier))
                                if (isLocked) {
                                    Text("✦", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Text(cls.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(if (isLocked) "Premium" else "Lv $level", style = MaterialTheme.typography.labelMedium, color = if (isLocked) Color.Gray else Gold)
                        }
                    }
                }
            }
        }
    }

    if (character.freeStatPoints > 0) {
        listScope.item {
            FantasyCard {
                Text("Allocate Attribute Points", style = MaterialTheme.typography.titleMedium, color = Gold)
                Text("${character.freeStatPoints} points to spend", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("STR", "END", "AGI", "WIL").forEach { stat ->
                        Button(
                            onClick = { viewModel.allocateStat(stat) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("+$stat", fontSize = 12.sp) }
                    }
                }
            }
        }
    }

    listScope.item {
        FantasyCard {
            Text("Attributes", style = MaterialTheme.typography.titleMedium, color = Gold)
            BarMeter("STR ${character.strength}", "${character.strProgress}/${GameMath.statThreshold(character.strength)}", (character.strProgress.toFloat() / GameMath.statThreshold(character.strength).coerceAtLeast(1)).coerceIn(0f, 1f), StatStr)
            BarMeter("END ${character.endurance}", "${character.endProgress}/${GameMath.statThreshold(character.endurance)}", (character.endProgress.toFloat() / GameMath.statThreshold(character.endurance).coerceAtLeast(1)).coerceIn(0f, 1f), StatEnd)
            BarMeter("AGI ${character.agility}", "${character.agiProgress}/${GameMath.statThreshold(character.agility)}", (character.agiProgress.toFloat() / GameMath.statThreshold(character.agility).coerceAtLeast(1)).coerceIn(0f, 1f), StatAgi)
            BarMeter("WIL ${character.willpower}", character.willpower.toString(), (character.willpower.toFloat() / GameMath.statThreshold(character.willpower).coerceAtLeast(1)).coerceIn(0f, 1f), StatWil)
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
            Text("Class Skills", style = MaterialTheme.typography.titleMedium, color = Gold)
            cls.skills.forEach { skill ->
                val unlocked = character.level >= skill.unlockLevel
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (unlocked) skill.emoji else "🔒", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(skill.name, fontWeight = FontWeight.Bold, color = if (unlocked) Color.White else Color.Gray)
                        Text(skill.blurb, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

private fun gearTab(
    scope: LazyListScope,
    state: HeroUiState,
    cls: CharacterClass,
    onEquipClick: (ItemSlot) -> Unit
) {
    scope.item {
        FantasyCard {
            Text("Equipment Grid", style = MaterialTheme.typography.titleMedium, color = Gold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EquippableSlots.forEach { slot ->
                    EquipmentRow(slot, state.gear[slot], onClick = { onEquipClick(slot) })
                }
            }
            
            val bonusActive = state.setPieces >= ArmorSlots.size
            Surface(
                color = if (bonusActive) Gold.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp),
                border = if (bonusActive) BorderStroke(1.dp, Gold) else null,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(
                    text = if (bonusActive) "✨ ${cls.label} bonus active: +10% ATK & HP!" else "Set bonus: ${state.setPieces}/5 pieces",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (bonusActive) Gold else Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SagaTabContent(
    viewModel: HeroViewModel,
    onLogWeight: () -> Unit
) {
    val bounties by viewModel.bounties.collectAsState()
    val resetLabel by viewModel.bountyResetLabel.collectAsState()
    val campaigns by viewModel.weeklyCampaigns.collectAsState()
    val campResetLabel by viewModel.campaignResetLabel.collectAsState()
    val characterState by viewModel.uiState.collectAsState()
    val c = characterState.character
    val claimedTrophies by viewModel.claimedTrophies.collectAsState()
    val lifetimeCardioKm by viewModel.lifetimeCardioKm.collectAsState()
    val imperial by viewModel.imperial.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (bounties.isNotEmpty()) {
            DailyBountyCard(
                bounties = bounties,
                resetLabel = resetLabel,
                onClaim = viewModel::claimBounty,
                onLogProgress = { bounty ->
                    if (bounty.id == "b_weight") {
                        onLogWeight()
                    } else {
                        viewModel.logBountyProgress(bounty)
                    }
                }
            )
        }

        if (campaigns.isNotEmpty()) {
            WeeklyCampaignCard(
                campaigns = campaigns,
                resetLabel = campResetLabel,
                onClaim = viewModel::claimCampaign
            )
        }

        if (c != null) {
            TrophyVaultCard(
                character = c,
                claimedIds = claimedTrophies,
                lifetimeCardioKm = lifetimeCardioKm,
                imperial = imperial,
                onClaimReward = viewModel::claimTrophyReward
            )
        }

        FantasyCard {
            Text("Recent Adventures", style = MaterialTheme.typography.titleMedium, color = Gold)
            if (characterState.recentSessions.isEmpty()) {
                Text("No adventures yet.", color = Color.White.copy(alpha = 0.5f))
            } else {
                val fmt = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
                characterState.recentSessions.forEach { s ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(s.name, fontWeight = FontWeight.Bold)
                            Text(fmt.format(Date(s.endedAt)), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                        }
                        Text("+${s.xpEarned} XP", color = Gold, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

private fun sagaTab(
    scope: LazyListScope,
    viewModel: HeroViewModel,
    onLogWeight: () -> Unit
) {
    scope.item {
        SagaTabContent(viewModel, onLogWeight)
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

@Composable
private fun EquipmentRow(slot: ItemSlot, item: ItemEntity?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                if (item != null) {
                    ItemIcon(item, modifier = Modifier.size(32.dp))
                } else {
                    Text(slot.label.take(1), fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.2f))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(slot.label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                Text(item?.name ?: "Empty Slot", fontWeight = FontWeight.Bold, color = if (item != null) Color.White else Color.White.copy(alpha = 0.3f))
            }
            if (item != null) {
                Text(itemBonusText(item), style = MaterialTheme.typography.labelSmall, color = Gold)
            }
        }
    }
}

fun itemBonusText(item: ItemEntity): String = buildList {
    if (item.atk > 0) add("+${item.atk} ATK")
    if (item.def > 0) add("+${item.def} DEF")
    if (item.hp > 0) add("+${item.hp} HP")
}.joinToString(" ")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotPickerSheet(
    slot: ItemSlot,
    character: CharacterEntity,
    gear: Map<ItemSlot, ItemEntity>,
    owned: List<OwnedGear>,
    equippedId: Long?,
    runes: List<ItemEntity>,
    onEquip: (Long) -> Unit,
    onSocketRune: (Long, Int, Long) -> Unit,
    onClearRune: (Long, Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Select ${slot.label}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            if (owned.isEmpty()) {
                Text("You don't own any items for this slot yet.", color = Color.Gray)
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 154.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height((if (owned.size <= 2) 150 else 320).dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(owned, key = { it.instance.id }) { itemRow ->
                    val isEquipped = itemRow.instance.id == equippedId
                    val currentlyEquippedItem = gear[slot]
                    SlotPickerGearCard(
                        gear = itemRow,
                        currentlyEquippedItem = currentlyEquippedItem,
                        isEquipped = isEquipped,
                        character = character,
                        onClick = { onEquip(itemRow.instance.id) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SlotPickerGearCard(
    gear: OwnedGear,
    currentlyEquippedItem: ItemEntity?,
    isEquipped: Boolean,
    character: CharacterEntity,
    onClick: () -> Unit
) {
    val canEquip = gear.catalog.classAffinity == null || gear.catalog.classAffinity == character.characterClass
    val atkDiff = gear.catalog.atk - (currentlyEquippedItem?.atk ?: 0)
    val defDiff = gear.catalog.def - (currentlyEquippedItem?.def ?: 0)
    val hpDiff = gear.catalog.hp - (currentlyEquippedItem?.hp ?: 0)

    Surface(
        onClick = onClick,
        enabled = canEquip,
        shape = RoundedCornerShape(12.dp),
        color = if (isEquipped) Gold.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (isEquipped) Gold else Color.White.copy(alpha = 0.12f)),
        modifier = Modifier.height(150.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    ItemIcon(gear.catalog, modifier = Modifier.size(32.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        gear.catalog.name,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        itemBonusText(gear.catalog).ifBlank { "No direct stats" },
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isEquipped) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
                }
            }
            if (!isEquipped) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (atkDiff != 0) HeroDeltaPill("ATK", atkDiff)
                    if (defDiff != 0) HeroDeltaPill("DEF", defDiff)
                    if (hpDiff != 0) HeroDeltaPill("HP", hpDiff)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HeroGearPill("T${gear.catalog.tier}")
                HeroGearPill("+${gear.instance.upgradeLevel}")
                HeroGearPill(gear.catalog.classAffinity?.label ?: "All classes")
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
private fun HeroGearPill(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
