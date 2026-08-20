package com.fitnessquest.rpg.ui.screens

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.data.db.WorkoutEntity
import com.fitnessquest.rpg.data.guild.GuildMember
import com.fitnessquest.rpg.data.guild.GuildRaid
import com.fitnessquest.rpg.data.guild.GuildState
import com.fitnessquest.rpg.data.party.PartyBoss
import com.fitnessquest.rpg.data.party.PartyMember
import com.fitnessquest.rpg.data.party.PartyPulse
import com.fitnessquest.rpg.data.party.PartyState
import com.fitnessquest.rpg.data.party.SharedWorkout
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.RewardBatch
import com.fitnessquest.rpg.domain.isDemoUser
import com.fitnessquest.rpg.ui.appContainer
import com.fitnessquest.rpg.ui.components.AlliesHubCard
import com.fitnessquest.rpg.ui.components.BarMeter
import com.fitnessquest.rpg.ui.components.GhostBarMeter
import com.fitnessquest.rpg.ui.components.GuildRaidBossCard
import com.fitnessquest.rpg.ui.components.RewardRevealDialog
import com.fitnessquest.rpg.ui.components.SceneBanner
import com.fitnessquest.rpg.ui.components.SceneKind
import com.fitnessquest.rpg.ui.components.SectionCard
import com.fitnessquest.rpg.ui.components.SettingsIconButton
import com.fitnessquest.rpg.ui.effects.AudioEffects
import com.fitnessquest.rpg.ui.rememberDockContentPadding
import com.fitnessquest.rpg.ui.theme.FitQuestTheme
import com.fitnessquest.rpg.ui.theme.Gold
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

data class RivalEntry(
    val uid: String,
    val name: String,
    val classEmoji: String,
    val level: Int,
    val weeklyXp: Int,
    val totalXp: Int,
    val streak: Int,
)

data class RivalsUiState(
    val loading: Boolean = true,
    val entries: List<RivalEntry> = emptyList(),
    val myUid: String? = null,
    val error: String? = null,
) {
    val myRank: Int get() = entries.indexOfFirst { it.uid == myUid } + 1
}

class RivalsViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(RivalsUiState())
    val uiState: StateFlow<RivalsUiState> = _uiState

    val party: StateFlow<PartyState> = container.party.state
    val guild: StateFlow<GuildState> = container.guild.state
    val myUid: String? get() = container.auth.state.value.uid

    val myWorkouts: StateFlow<List<WorkoutEntity>> = container.repository.workouts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _partyBusy = MutableStateFlow(value = false)
    val partyBusy: StateFlow<Boolean> = _partyBusy

    private val _partyMessage = MutableStateFlow<String?>(null)
    val partyMessage: StateFlow<String?> = _partyMessage

    private val _guildBusy = MutableStateFlow(value = false)
    val guildBusy: StateFlow<Boolean> = _guildBusy

    private val _guildMessage = MutableStateFlow<String?>(null)
    val guildMessage: StateFlow<String?> = _guildMessage

    private val _rewardBatch = MutableStateFlow<RewardBatch?>(null)
    val rewardBatch: StateFlow<RewardBatch?> = _rewardBatch

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val snapshot: QuerySnapshot? = withTimeoutOrNull(15_000.milliseconds) {
                    FirebaseFirestore.getInstance()
                        .collection("leaderboard")
                        .orderBy("weeklyXp", Query.Direction.DESCENDING)
                        .limit(100)
                        .get()
                        .await()
                }
                if (snapshot == null) {
                    _uiState.update { it.copy(loading = false, error = "Could not reach the allies hall. Check your connection.") }
                    return@launch
                }
                val myUid = container.auth.state.value.uid
                val rawEntries = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: "Hero"
                    if (isDemoUser(name) && doc.id != myUid) {
                        return@mapNotNull null
                    }
                    val cls = doc.getString("characterClass")
                        ?.let { className -> CharacterClass.entries.find { it.name == className } }
                    RivalEntry(
                        uid = doc.id,
                        name = name,
                        classEmoji = cls?.emoji ?: "\uD83E\uDDB8",
                        level = (doc.getLong("level") ?: 1L).toInt(),
                        weeklyXp = (doc.getLong("weeklyXp") ?: 0L).toInt(),
                        totalXp = (doc.getLong("totalXp") ?: 0L).toInt(),
                        streak = (doc.getLong("streak") ?: 0L).toInt()
                    )
                }
                val entries = rawEntries
                    .groupBy { it.name.trim().lowercase() }
                    .map { (_, duplicates) ->
                        duplicates.maxByOrNull { it.weeklyXp } ?: duplicates.first()
                    }
                    .distinctBy { it.uid }
                    .sortedByDescending { it.weeklyXp }
                _uiState.update {
                    it.copy(
                        loading = false,
                        entries = entries,
                        myUid = container.auth.state.value.uid
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun createParty(name: String) = partyAction {
        container.party.createParty(name).getOrThrow()
        "Party founded! Share the invite code with your friends."
    }

    fun joinParty(code: String) = partyAction {
        container.party.joinParty(code).getOrThrow()
        "Welcome to the party!"
    }

    fun leaveParty() = partyAction {
        container.party.leaveParty().getOrThrow()
        "You left the party."
    }

    fun claimBossReward() {
        viewModelScope.launch {
            _partyBusy.value = true
            try {
                val batch = container.party.claimBossReward().getOrThrow()
                AudioEffects.playCoinJingle()
                _rewardBatch.value = batch
            } catch (e: Exception) {
                _partyMessage.value = e.message ?: "Failed to claim rewards."
            } finally {
                _partyBusy.value = false
            }
        }
    }

    fun summonNextBoss() = partyAction {
        container.party.summonNextBoss().getOrThrow()
        "A new foe approaches..."
    }

    fun shareWorkout(workoutId: Long) = partyAction {
        container.party.shareWorkout(workoutId).getOrThrow()
        "Quest shared with the party!"
    }

    fun importWorkout(shared: SharedWorkout) = partyAction {
        container.party.importWorkout(shared).getOrThrow()
        "\u201C${shared.name}\u201D added to your quests."
    }

    fun startPartyWorkout(shared: SharedWorkout, onNavigate: (Long) -> Unit) = partyAction {
        val id = container.party.importWorkout(shared).getOrThrow()
        viewModelScope.launch(Dispatchers.Main) { onNavigate(id) }
        "Starting ${shared.name}..."
    }

    fun deleteSharedWorkout(shared: SharedWorkout) = partyAction {
        container.party.deleteSharedWorkout(shared.id).getOrThrow()
        "Shared quest removed."
    }

    fun sendCheer(eventId: String, emoji: String) {
        viewModelScope.launch {
            container.party.sendCheer(eventId, emoji)
        }
    }

    fun dismissPartyMessage() {
        _partyMessage.value = null
    }

    private fun partyAction(block: suspend () -> String) {
        viewModelScope.launch {
            _partyBusy.value = true
            _partyMessage.value = try {
                block()
            } catch (e: Exception) {
                e.message ?: "Something went wrong."
            }
            _partyBusy.value = false
        }
    }

    fun createGuild(name: String) = guildAction {
        container.guild.createGuild(name).getOrThrow()
        "Guild founded! Share the invite code with your allies."
    }

    fun joinGuild(code: String) = guildAction {
        container.guild.joinGuild(code).getOrThrow()
        "Welcome to the guild!"
    }

    fun leaveGuild() = guildAction {
        container.guild.leaveGuild().getOrThrow()
        "You left the guild."
    }

    fun claimGuildRaidReward() {
        viewModelScope.launch {
            _guildBusy.value = true
            try {
                val batch = container.guild.claimRaidReward().getOrThrow()
                AudioEffects.playCoinJingle()
                _rewardBatch.value = batch
            } catch (e: Exception) {
                _guildMessage.value = e.message ?: "Failed to claim rewards."
            } finally {
                _guildBusy.value = false
            }
        }
    }

    fun dismissRewardReveal() {
        _rewardBatch.value = null
    }

    fun dismissGuildMessage() {
        _guildMessage.value = null
    }

    private fun guildAction(block: suspend () -> String) {
        viewModelScope.launch {
            _guildBusy.value = true
            _guildMessage.value = try {
                block()
            } catch (e: Exception) {
                e.message ?: "Something went wrong."
            }
            _guildBusy.value = false
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { RivalsViewModel(appContainer) }
        }
    }
}

@Composable
fun RivalsScreen(
    onStartWorkout: (Long) -> Unit,
    viewModel: RivalsViewModel = viewModel(factory = RivalsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    val party by viewModel.party.collectAsState()
    val guild by viewModel.guild.collectAsState()
    val rewardBatch by viewModel.rewardBatch.collectAsState()
    val myWorkouts by viewModel.myWorkouts.collectAsState()
    val partyBusy by viewModel.partyBusy.collectAsState()
    val partyMessage by viewModel.partyMessage.collectAsState()
    val guildBusy by viewModel.guildBusy.collectAsState()
    val guildMessage by viewModel.guildMessage.collectAsState()

    RivalsScreenContent(
        state = state,
        party = party,
        guild = guild,
        rewardBatch = rewardBatch,
        myWorkouts = myWorkouts,
        myUid = viewModel.myUid,
        partyBusy = partyBusy,
        partyMessage = partyMessage,
        guildBusy = guildBusy,
        guildMessage = guildMessage,
        actions = RivalsActions(
            onStartWorkout = onStartWorkout,
            onRefresh = viewModel::refresh,
            onCreateParty = viewModel::createParty,
            onJoinParty = viewModel::joinParty,
            onLeaveParty = viewModel::leaveParty,
            onClaimBossReward = viewModel::claimBossReward,
            onSummonNextBoss = viewModel::summonNextBoss,
            onShareWorkout = viewModel::shareWorkout,
            onImportWorkout = viewModel::importWorkout,
            onStartPartyWorkout = viewModel::startPartyWorkout,
            onDeleteSharedWorkout = viewModel::deleteSharedWorkout,
            onDismissPartyMessage = viewModel::dismissPartyMessage,
            onCreateGuild = viewModel::createGuild,
            onJoinGuild = viewModel::joinGuild,
            onLeaveGuild = viewModel::leaveGuild,
            onClaimGuildRaidReward = viewModel::claimGuildRaidReward,
            onDismissRewardReveal = viewModel::dismissRewardReveal,
            onDismissGuildMessage = viewModel::dismissGuildMessage,
            onSendCheer = viewModel::sendCheer
        )
    )
}

data class RivalsActions(
    val onStartWorkout: (Long) -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onCreateParty: (String) -> Unit = {},
    val onJoinParty: (String) -> Unit = {},
    val onLeaveParty: () -> Unit = {},
    val onClaimBossReward: () -> Unit = {},
    val onSummonNextBoss: () -> Unit = {},
    val onShareWorkout: (Long) -> Unit = {},
    val onImportWorkout: (SharedWorkout) -> Unit = {},
    val onStartPartyWorkout: (SharedWorkout, (Long) -> Unit) -> Unit = { _, _ -> },
    val onDeleteSharedWorkout: (SharedWorkout) -> Unit = {},
    val onDismissPartyMessage: () -> Unit = {},
    val onCreateGuild: (String) -> Unit = {},
    val onJoinGuild: (String) -> Unit = {},
    val onLeaveGuild: () -> Unit = {},
    val onClaimGuildRaidReward: () -> Unit = {},
    val onDismissRewardReveal: () -> Unit = {},
    val onDismissGuildMessage: () -> Unit = {},
    val onSendCheer: (String, String) -> Unit = { _, _ -> }
)

@Composable
fun RivalsScreenContent(
    state: RivalsUiState,
    party: PartyState,
    guild: GuildState,
    rewardBatch: RewardBatch?,
    myWorkouts: List<WorkoutEntity>,
    myUid: String?,
    partyBusy: Boolean,
    partyMessage: String?,
    guildBusy: Boolean,
    guildMessage: String?,
    actions: RivalsActions
) {
    var section by rememberSaveable { mutableStateOf(AllSection.Hub.name) }
    val current = AllSection.entries.find { it.name == section } ?: AllSection.Hub

    Column(Modifier.fillMaxSize()) {
        SceneBanner(
            kind = SceneKind.ALLIES,
            title = "ALLIES",
            tagline = when (current) {
                AllSection.Hub -> "Stronger together"
                AllSection.Party -> "Small crew. Shared raids."
                AllSection.Guild -> "Weekly guild colossus"
                AllSection.Rivals -> "Ranked by XP this week"
            }
        ) {
            if (current == AllSection.Rivals) {
                IconButton(onClick = actions.onRefresh, enabled = !state.loading) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }
            SettingsIconButton()
        }

        when (current) {
            AllSection.Hub -> AlliesHub(
                party = party,
                guild = guild,
                myUid = myUid,
                myRank = state.myRank,
                boardSize = state.entries.size,
                onParty = { section = AllSection.Party.name },
                onGuild = { section = AllSection.Guild.name },
                onRivals = { section = AllSection.Rivals.name },
                onClaimRaidLoot = actions.onClaimGuildRaidReward
            )
            AllSection.Party -> Column(Modifier.fillMaxSize()) {
                AlliesBackRow(label = "Party") { section = AllSection.Hub.name }
                PartyTabContent(state, party, partyBusy, partyMessage, myWorkouts, myUid, actions)
            }
            AllSection.Guild -> Column(Modifier.fillMaxSize()) {
                AlliesBackRow(label = "Guild") { section = AllSection.Hub.name }
                GuildTabContent(guild, guildBusy, guildMessage, myUid, actions)
            }
            AllSection.Rivals -> Column(Modifier.fillMaxSize()) {
                AlliesBackRow(label = "Rivals board") { section = AllSection.Hub.name }
                LeaderboardTab(state, myUid)
            }
        }
    }

    rewardBatch?.let { batch ->
        RewardRevealDialog(batch = batch, onDismiss = actions.onDismissRewardReveal)
    }
}

private enum class AllSection { Hub, Party, Guild, Rivals }

@Composable
private fun AlliesBackRow(label: String, onBack: () -> Unit) {
    TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) {
        Text("< Back \u00B7 $label")
    }
}

@Composable
private fun AlliesHub(
    party: PartyState,
    guild: GuildState,
    myUid: String?,
    myRank: Int,
    boardSize: Int,
    onParty: () -> Unit,
    onGuild: () -> Unit,
    onRivals: () -> Unit,
    onClaimRaidLoot: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = rememberDockContentPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (guild.inGuild && (guild.raid != null)) {
            item {
                GuildRaidBossCard(
                    raid = guild.raid,
                    members = guild.members,
                    myUid = myUid,
                    onClaimLoot = onClaimRaidLoot
                )
            }
        }
        item {
            AlliesHubCard(
                title = "Party",
                subtitle = if (party.inParty) {
                    "${party.name} \u00B7 ${party.members.size} heroes"
                } else {
                    "Small crew — share quests & raid bosses"
                },
                emoji = "\u2694\uFE0F",
                trailing = party.boss?.let { b ->
                    if (b.defeated) "Loot!" else "${(100 * (1f - (b.hp.toFloat() / b.maxHp.coerceAtLeast(1)))).toInt()}%"
                },
                onClick = onParty
            )
        }
        item {
            val raid = guild.raid
            AlliesHubCard(
                title = "Guild",
                subtitle = if (guild.inGuild) {
                    "${guild.name} \u00B7 ${guild.members.size}/20"
                } else {
                    "Larger roster — weekly shared raid"
                },
                emoji = "\uD83C\uDFDB\uFE0F",
                trailing = when {
                    raid == null -> null
                    raid.defeated -> "Loot!"
                    else -> {
                        val pct = (100 * (1f - raid.hp.toFloat() / raid.maxHp.coerceAtLeast(1))).toInt()
                        "$pct%"
                    }
                },
                onClick = onGuild
            )
        }
        item {
            AlliesHubCard(
                title = "Rivals board",
                subtitle = if (myRank > 0) {
                    "You are rank #$myRank of $boardSize this week"
                } else {
                    "Weekly XP leaderboard"
                },
                emoji = "\uD83C\uDFC6",
                onClick = onRivals
            )
        }
    }
}

@Composable
private fun LeaderboardTab(state: RivalsUiState, myUid: String?) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(state.entries, query) {
        val q = query.trim()
        if (q.isEmpty()) state.entries
        else state.entries.filter { it.name.contains(q, ignoreCase = true) }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = rememberDockContentPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!state.loading && state.error == null && state.entries.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("Find username") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (state.loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (state.error != null) {
            item {
                SectionCard {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                }
            }
        } else if (state.entries.isEmpty()) {
            item {
                SectionCard {
                    Text(
                        "The rivals board is quiet... No heroes ranked yet. " +
                            "Finish a workout and you'll be the first!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (filtered.isEmpty()) {
            item {
                SectionCard {
                    Text(
                        "No allies match \"$query\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            if (state.myRank > 0 && query.isBlank()) {
                item {
                    SectionCard {
                        Text(
                            "\uD83C\uDFC5 You are rank #${state.myRank} of ${state.entries.size} this week",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Gold
                        )
                    }
                }
            }
            items(filtered.size) { index ->
                val entry = filtered[index]
                val rank = state.entries.indexOfFirst { it.uid == entry.uid } + 1
                RivalRow(
                    rank = rank,
                    entry = entry,
                    isMe = entry.uid == myUid
                )
            }
        }
    }
}

@Composable
private fun RivalRow(rank: Int, entry: RivalEntry, isMe: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isMe) scheme.primaryContainer else scheme.surface,
        contentColor = if (isMe) scheme.onPrimaryContainer else scheme.onSurface
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                when (rank) {
                    1 -> "\uD83E\uDD47"
                    2 -> "\uD83E\uDD48"
                    3 -> "\uD83E\uDD49"
                    else -> "#$rank"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(44.dp)
            )
            Box(
                Modifier
                    .size(38.dp)
                    .background(scheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(entry.classEmoji)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isMe) "${entry.name} (you)" else entry.name,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Lv ${entry.level}",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                    if (entry.streak > 0) {
                        Text(
                            "\uD83D\uDD25 ${entry.streak}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${entry.weeklyXp} XP",
                    fontWeight = FontWeight.Bold,
                    color = if (isMe) scheme.onPrimaryContainer else scheme.primary
                )
                Text(
                    "this week",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PartyTabContent(
    state: RivalsUiState,
    party: PartyState,
    busy: Boolean,
    message: String?,
    myWorkouts: List<WorkoutEntity>,
    myUid: String?,
    actions: RivalsActions
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = rememberDockContentPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        message?.let { msg ->
            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = actions.onDismissPartyMessage) { Text("OK") }
                    }
                }
            }
        }

        if (party.loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (!party.inParty) {
            item { PartyLobby(busy = busy, onCreate = actions.onCreateParty, onJoin = actions.onJoinParty) }
        } else {
            item { PartyHeaderCard(party) }
            party.boss?.let { boss ->
                item {
                    RaidBossCard(
                        boss = boss,
                        pulses = party.activePulses,
                        myUid = myUid,
                        busy = busy,
                        onClaim = actions.onClaimBossReward,
                        onSummon = actions.onSummonNextBoss
                    )
                }
            }
            item {
                Text("Party members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(party.members.size) { index ->
                val member = party.members[index]
                val pulse = party.activePulses.find { it.uid == member.uid }
                PartyMemberRow(
                    member = member,
                    isMe = member.uid == myUid,
                    isTopDamager = member.uid == party.topDamagerUid && (party.boss?.damageByUid?.get(member.uid) ?: 0L) > 0L,
                    isMostActive = member.uid == party.mostActiveMemberUid && member.weeklyXp > 0,
                    activeEffects = pulse?.effects.orEmpty()
                )
            }
            if (party.feed.isNotEmpty()) {
                item {
                    PartyCombatFeedCard(
                        feed = party.feed,
                        myUid = myUid,
                        onSendCheer = actions.onSendCheer
                    )
                }
            }
            item {
                SharedWorkoutsCard(
                    party = party,
                    myUid = myUid,
                    myWorkouts = myWorkouts,
                    busy = busy,
                    onShare = actions.onShareWorkout,
                    onImport = actions.onImportWorkout,
                    onStart = { shared -> actions.onStartPartyWorkout(shared, actions.onStartWorkout) },
                    onDelete = actions.onDeleteSharedWorkout
                )
            }
            item {
                TextButton(
                    onClick = actions.onLeaveParty,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Leave party", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun GuildTabContent(
    guild: GuildState,
    busy: Boolean,
    message: String?,
    myUid: String?,
    actions: RivalsActions
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = rememberDockContentPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        message?.let { msg ->
            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = actions.onDismissGuildMessage) { Text("OK") }
                    }
                }
            }
        }

        if (guild.loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (!guild.inGuild) {
            item {
                GuildLobby(busy = busy, onCreate = actions.onCreateGuild, onJoin = actions.onJoinGuild)
            }
        } else {
            item { GuildHeaderCard(guild) }
            guild.raid?.let { raid ->
                item {
                    GuildRaidCard(
                        raid = raid,
                        myUid = myUid,
                        busy = busy,
                        onClaim = actions.onClaimGuildRaidReward
                    )
                }
            }
            item {
                Text("Guild members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(guild.members.size) { index ->
                GuildMemberRow(guild.members[index], isMe = guild.members[index].uid == myUid)
            }
            item {
                TextButton(
                    onClick = actions.onLeaveGuild,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Leave guild", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun PartyLobby(
    busy: Boolean,
    onCreate: (String) -> Unit,
    onJoin: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard {
            Text("\u2694\uFE0F Found a new party", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Rally your gym crew: share workouts, watch each other's progress, " +
                    "and bring down raid bosses that are too tough to solo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Party name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onCreate(name) },
                enabled = !busy && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create party") }
        }
        SectionCard {
            Text("\uD83D\uDCE8 Join with a code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Ask a friend for their party's 6-character invite code.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase().take(6) },
                label = { Text("Invite code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onJoin(code) },
                enabled = !busy && code.length == 6,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Join party") }
        }
    }
}

@Composable
private fun PartyHeaderCard(party: PartyState) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(party.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${party.members.size} ${if (party.members.size == 1) "hero" else "heroes"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .clickable {
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Invite Code", party.inviteCode)))
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    party.inviteCode,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Copy invite code",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            "Tap the code to copy it, then send it to a friend.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (party.activeAuras.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "ACTIVE PARTY AURAS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                party.activeAuras.forEach { aura ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(aura.emoji, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${aura.title}: ${aura.description}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RaidBossCard(
    boss: PartyBoss,
    pulses: List<PartyPulse>,
    myUid: String?,
    busy: Boolean,
    onClaim: () -> Unit,
    onSummon: () -> Unit,
) {
    val myDamage = myUid?.let { boss.damageByUid[it] } ?: 0L
    val claimed = myUid != null && myUid in boss.claimedBy
    val activeDamage = pulses.sumOf { it.xp }.toLong()
    val ghostHp = (boss.hp - activeDamage).coerceAtLeast(0L)

    val enragedBorder = if (boss.isEnraged) {
        Modifier.border(2.dp, Color(0xFFEF4444), RoundedCornerShape(16.dp))
    } else Modifier

    Box(modifier = enragedBorder) {
        SectionCard {
            if (boss.isEnraged) {
                Surface(
                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "🔥 ENRAGED! Boss is below 30% HP — Strike now to finish the raid!",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(boss.emoji, style = MaterialTheme.typography.displaySmall)
                Column(Modifier.weight(1f)) {
                    Text(boss.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Tier ${boss.tier} raid boss",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        boss.weaknessCategory?.let { weakness ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "🎯 Weak to ${weakness.name.replace('_', ' ')}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
            if (boss.defeated) {
                Text(
                    "🎉 DEFEATED! Every contributor earns ${boss.rewardGold} gold and a ${boss.rewardXpBoost} XP boost.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Gold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onClaim,
                        enabled = !busy && !claimed && myDamage > 0,
                        modifier = Modifier.weight(1f)
                    ) { Text(if (claimed) "Claimed ✓" else "Claim spoils") }
                    OutlinedButton(
                        onClick = onSummon,
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) { Text("Summon next") }
                }
            } else {
                GhostBarMeter(
                    label = if (boss.isEnraged) "Boss HP (ENRAGED)" else "Boss HP",
                    valueText = if (activeDamage > 0) "${ghostHp} / ${boss.hp} HP" else "${boss.hp} / ${boss.maxHp}",
                    actualProgress = (boss.hp.toFloat() / boss.maxHp).coerceIn(0f, 1f),
                    ghostProgress = (ghostHp.toFloat() / boss.maxHp).coerceIn(0f, 1f),
                    color = if (boss.isEnraged) Color(0xFFDC2626) else MaterialTheme.colorScheme.error,
                    ghostColor = Color.White.copy(alpha = 0.25f)
                )
                if (pulses.isNotEmpty()) {
                    val pulseEffects = pulses.flatMap { it.effects }.distinct().joinToString(" ")
                    Text(
                        text = buildString {
                            append("⚔️ ${pulses.size} hero${if (pulses.size > 1) "es" else ""} striking!")
                            if (pulseEffects.isNotEmpty()) append(" Status: $pulseEffects")
                            append(" Pending: $activeDamage XP")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Every XP point your party earns from workouts strikes the boss. Its health scales with your roster - everyone must fight!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (myDamage > 0) {
                    Text(
                        "⚔️ Your damage: $myDamage",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PartyMemberRow(
    member: PartyMember,
    isMe: Boolean,
    isTopDamager: Boolean = false,
    isMostActive: Boolean = false,
    activeEffects: List<String> = emptyList()
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isMe) scheme.primaryContainer else scheme.surface,
        contentColor = if (isMe) scheme.onPrimaryContainer else scheme.onSurface
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    Modifier
                        .size(38.dp)
                        .background(scheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(member.classEmoji)
                }
                if (activeEffects.isNotEmpty()) {
                    Text(
                        activeEffects.first(),
                        modifier = Modifier.offset(x = 4.dp, y = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (isMe) "${member.name} (you)" else member.name, fontWeight = FontWeight.SemiBold)
                    if (isTopDamager) {
                        Surface(
                            color = Gold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "🏆 Boss Slayer",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else if (isMostActive) {
                        Surface(
                            color = scheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "⚡ Swift Striker",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (activeEffects.size > 1) {
                        Text(activeEffects.drop(1).joinToString(" "), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text(
                    "Lv ${member.level} · ${member.weeklyXp} XP this week",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMe) scheme.onPrimaryContainer.copy(alpha = 0.8f) else scheme.onSurfaceVariant
                )
            }
            if (member.bossDamage > 0) {
                Text(
                    "⚔️ ${member.bossDamage}",
                    fontWeight = FontWeight.Bold,
                    color = if (isMe) scheme.onPrimaryContainer else scheme.primary
                )
            }
        }
    }
}

@Composable
private fun PartyCombatFeedCard(
    feed: List<com.fitnessquest.rpg.data.party.PartyFeedEvent>,
    myUid: String?,
    onSendCheer: (String, String) -> Unit
) {
    SectionCard {
        Text("📜 Combat Activity Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            feed.take(5).forEach { event ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                event.text,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (event.isCrit) FontWeight.Bold else FontWeight.Normal,
                                color = if (event.isCrit) Gold else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val cheersList = listOf("🔥", "💪", "⚡", "🛡️")
                            cheersList.forEach { emoji ->
                                val count = event.cheers.values.count { it == emoji }
                                val hasCheered = myUid != null && event.cheers[myUid] == emoji
                                Surface(
                                    color = if (hasCheered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clickable { onSendCheer(event.id, emoji) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        if (count > 0) "$emoji $count" else emoji,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hasCheered) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedWorkoutsCard(
    party: PartyState,
    myUid: String?,
    myWorkouts: List<WorkoutEntity>,
    busy: Boolean,
    onShare: (Long) -> Unit,
    onImport: (SharedWorkout) -> Unit,
    onStart: (SharedWorkout) -> Unit,
    onDelete: (SharedWorkout) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "\uD83D\uDCDC Shared quests",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { showPicker = true }, enabled = !busy && myWorkouts.isNotEmpty()) {
                Text("Share one")
            }
        }
        if (party.sharedWorkouts.isEmpty()) {
            Text(
                "No quests shared yet. Share one of yours so the whole party trains together!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            party.sharedWorkouts.forEachIndexed { index, shared ->
                if (index > 0) HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(shared.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${shared.exercises.size} exercises \u00B7 by ${
                                if (shared.authorUid == myUid) "you" else shared.authorName
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onStart(shared) }, enabled = !busy) { Text("Start") }
                    TextButton(onClick = { onImport(shared) }, enabled = !busy) { Text("Import") }
                    if (shared.authorUid == myUid) {
                        IconButton(onClick = { onDelete(shared) }, enabled = !busy) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        Dialog(onDismissRequest = { showPicker = false }) {
            SectionCard {
                Text(
                    "Share a quest with the party",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (myWorkouts.isEmpty()) {
                    Text(
                        "You have no saved quests yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    myWorkouts.forEach { workout ->
                        TextButton(
                            onClick = {
                                showPicker = false
                                onShare(workout.id)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(workout.name) }
                    }
                }
                TextButton(onClick = { showPicker = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun GuildLobby(
    busy: Boolean,
    onCreate: (String) -> Unit,
    onJoin: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard {
            Text("\uD83C\uDFDB\uFE0F Found a guild", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Up to 20 heroes. Finish workouts to chip away at a weekly shared raid boss.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Guild name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onCreate(name) },
                enabled = !busy && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create guild") }
        }
        SectionCard {
            Text("\uD83D\uDCE8 Join with a code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Ask an ally for their guild's 6-character invite code.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase().take(6) },
                label = { Text("Invite code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onJoin(code) },
                enabled = !busy && code.length == 6,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Join guild") }
        }
    }
}

@Composable
private fun GuildHeaderCard(guild: GuildState) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(guild.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${guild.members.size} / 20 heroes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .clickable {
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Invite Code", guild.inviteCode)))
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    guild.inviteCode,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Copy invite code",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            "Tap the code to copy it, then send it to an ally.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GuildRaidCard(
    raid: GuildRaid,
    myUid: String?,
    busy: Boolean,
    onClaim: () -> Unit
) {
    val myDamage = myUid?.let { raid.damageByUid[it] } ?: 0L
    val claimed = myUid != null && myUid in raid.claimedBy
    val days = TimeUnit.MILLISECONDS.toDays(raid.millisLeft)
    val hours = TimeUnit.MILLISECONDS.toHours(raid.millisLeft) % 24
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(raid.emoji, style = MaterialTheme.typography.displaySmall)
            Column(modifier = Modifier.weight(1f)) {
                Text(raid.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Weekly guild raid \u00B7 ${days}d ${hours}h left",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (raid.defeated) {
            Text(
                "\uD83C\uDF89 DEFEATED! Contributors claim a shared loot chest. Top damage earns Raid Champion.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Gold
            )
            Button(
                onClick = onClaim,
                enabled = !busy && !claimed && myDamage > 0,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (claimed) "Claimed \u2713" else "Claim raid spoils") }
        } else {
            BarMeter(
                label = "Raid HP",
                valueText = "${raid.hp.coerceAtLeast(0)} / ${raid.maxHp}",
                progress = (raid.hp.toFloat() / raid.maxHp).coerceIn(0f, 1f),
                color = MaterialTheme.colorScheme.error
            )
            Text(
                "Finished workouts deal scaled damage to the guild raid. Rally the roster!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (myDamage > 0) {
                Text(
                    "\u2694\uFE0F Your damage: $myDamage",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun GuildMemberRow(member: GuildMember, isMe: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isMe) scheme.primaryContainer else scheme.surface,
        contentColor = if (isMe) scheme.onPrimaryContainer else scheme.onSurface
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(scheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(member.classEmoji)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (isMe) "${member.name} (you)" else member.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "Lv ${member.level}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            if (member.weekDamage > 0) {
                Text(
                    "\u2694\uFE0F ${member.weekDamage}",
                    fontWeight = FontWeight.Bold,
                    color = if (isMe) scheme.onPrimaryContainer else scheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF12131F)
@Composable
fun RivalsScreenPreview() {
    FitQuestTheme {
        RivalsScreenContent(
            state = RivalsUiState(
                loading = false,
                entries = listOf(
                    RivalEntry("1", "Rival 1", "⚔️", 15, 500, 5000, 7),
                    RivalEntry("2", "Rival 2", "🏹", 12, 300, 3000, 3)
                )
            ),
            party = PartyState(loading = false, partyId = "P1", name = "Preview Party", inviteCode = "ABCDEF"),
            guild = GuildState(loading = false, guildId = null),
            rewardBatch = null,
            myWorkouts = emptyList(),
            myUid = "1",
            partyBusy = false,
            partyMessage = null,
            guildBusy = false,
            guildMessage = null,
            actions = RivalsActions()
        )
    }
}
