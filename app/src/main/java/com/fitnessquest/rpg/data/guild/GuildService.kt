package com.fitnessquest.rpg.data.guild

import android.app.Application
import android.content.Context
import com.fitnessquest.rpg.data.GameRepository
import com.fitnessquest.rpg.data.auth.AuthService
import com.fitnessquest.rpg.domain.RewardBatch
import com.fitnessquest.rpg.domain.RewardSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import java.util.TimeZone
import kotlin.time.Duration.Companion.milliseconds
import kotlin.random.Random

data class GuildMember(
    val uid: String,
    val name: String,
    val classEmoji: String,
    val level: Int,
    val weekDamage: Long = 0,
)

data class GuildRaid(
    val weekId: String,
    val name: String,
    val emoji: String,
    val maxHp: Long,
    val hp: Long,
    val damageByUid: Map<String, Long> = emptyMap(),
    val claimedBy: List<String> = emptyList(),
    val endsAtMillis: Long,
) {
    val defeated: Boolean get() = hp <= 0
    val millisLeft: Long get() = (endsAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
}

data class GuildState(
    val loading: Boolean = true,
    val guildId: String? = null,
    val name: String = "",
    val inviteCode: String = "",
    val members: List<GuildMember> = emptyList(),
    val raid: GuildRaid? = null,
    val lastRewardBatch: RewardBatch? = null
) {
    val inGuild: Boolean get() = guildId != null
}

/**
 * Larger social roster than a party: weekly shared raid boss.
 * Workout XP deals damage (same spirit as party raids).
 */
class GuildService(
    app: Application,
    private val repository: GameRepository,
    private val auth: AuthService
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = app.getSharedPreferences("fitquest_guild", Context.MODE_PRIVATE)

    private val _guildId = MutableStateFlow(prefs.getString(KEY_GUILD_ID, null))
    private val _state = MutableStateFlow(GuildState())
    val state: StateFlow<GuildState> = _state

    private val listeners = mutableListOf<ListenerRegistration>()

    fun start() {
        scope.launch {
            repository.character
                .map { it.guildId }
                .distinctUntilChanged()
                .collectLatest { guildId ->
                    _guildId.value = guildId
                }
        }
        scope.launch {
            combine(
                auth.state.map { it.uid }.distinctUntilChanged(),
                _guildId
            ) { uid, guildId -> uid to guildId }
                .collectLatest { (uid, guildId) ->
                    detach()
                    if ((uid == null) || (guildId == null)) {
                        _state.value = GuildState(loading = false)
                        return@collectLatest
                    }
                    _state.value = GuildState(loading = true, guildId = guildId)
                    attach(guildId)
                    refreshMemberCard()
                }
        }
    }

    private fun attach(guildId: String) {
        val doc = guildDoc(guildId)
        listeners += doc.addSnapshotListener { snap, _ ->
            if ((snap != null) && !snap.exists() && !snap.metadata.isFromCache) {
                scope.launch { setGuildId(null) }
                return@addSnapshotListener
            }
            if (snap == null || !snap.exists()) return@addSnapshotListener
            @Suppress("UNCHECKED_CAST")
            val damage = (snap["raidDamage"] as? Map<String, Any?>).orEmpty()
                .mapValues { (it.value as? Number)?.toLong() ?: 0L }
            val weekId = snap.getString("raidWeekId") ?: currentWeekId()
            val raid = if (snap.getLong("raidMaxHp") != null) {
                GuildRaid(
                    weekId = weekId,
                    name = snap.getString("raidName") ?: "Guild Colossus",
                    emoji = snap.getString("raidEmoji") ?: "\uD83D\uDC32",
                    maxHp = snap.getLong("raidMaxHp") ?: 1L,
                    hp = snap.getLong("raidHp") ?: 0L,
                    damageByUid = damage,
                    claimedBy = (snap["raidClaimed"] as? List<*>).orEmpty().filterIsInstance<String>(),
                    endsAtMillis = snap.getLong("raidEndsAt") ?: weekEndMillis()
                )
            } else null
            _state.update {
                it.copy(
                    loading = false,
                    name = snap.getString("name") ?: "Guild",
                    inviteCode = snap.getString("inviteCode") ?: "",
                    raid = raid,
                    members = it.members.map { m -> m.copy(weekDamage = damage[m.uid] ?: 0L) }
                )
            }
            // Roll a new week if the stored week expired and raid is still old.
            if (raid != null && raid.weekId != currentWeekId() && raid.millisLeft == 0L && !raid.defeated) {
                scope.launch { ensureWeeklyRaid(guildId, force = true) }
            }
        }
        listeners += doc.collection("members").addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            val damage = _state.value.raid?.damageByUid.orEmpty()
            val myUid = auth.state.value.uid
            val rawMembers = snap.documents.map { d ->
                GuildMember(
                    uid = d.id,
                    name = d.getString("name") ?: "Hero",
                    classEmoji = d.getString("classEmoji") ?: "\uD83E\uDDB8",
                    level = (d.getLong("level") ?: 1L).toInt(),
                    weekDamage = damage[d.id] ?: 0L
                )
            }
            val members = rawMembers
                .filter { !com.fitnessquest.rpg.domain.isDemoUser(it.name) || it.uid == myUid }
                .groupBy { it.name.trim().lowercase() }
                .map { (_, duplicates) ->
                    duplicates.maxByOrNull { it.weekDamage } ?: duplicates.first()
                }
                .distinctBy { it.uid }
                .sortedByDescending { it.weekDamage }
            _state.update { it.copy(loading = false, members = members) }
        }
    }

    private fun detach() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    suspend fun createGuild(name: String): Result<Unit> = runCatching {
        val uid = requireUid()
        val code = generateInviteCode()
        val doc = firestore.collection("guilds").document()
        val week = currentWeekId()
        val ends = weekEndMillis()
        val hp = raidHpFor(1)
        val (bossName, emoji) = RAID_BOSSES.random()
        doc.set(
            mapOf(
                "name" to name.trim().take(40).ifBlank { "Heroes Guild" },
                "inviteCode" to code,
                "leaderUid" to uid,
                "createdAt" to System.currentTimeMillis(),
                "memberCount" to 1,
                "raidWeekId" to week,
                "raidName" to bossName,
                "raidEmoji" to emoji,
                "raidMaxHp" to hp,
                "raidHp" to hp,
                "raidDamage" to emptyMap<String, Long>(),
                "raidClaimed" to emptyList<String>(),
                "raidEndsAt" to ends
            )
        ).await()
        doc.collection("members").document(uid).set(memberCard()).await()
        setGuildId(doc.id)
    }

    suspend fun joinGuild(code: String): Result<Unit> = runCatching {
        val uid = requireUid()
        val cleaned = code.trim().uppercase()
        require(cleaned.length == CODE_LENGTH) { "Invite codes are $CODE_LENGTH characters." }
        val snap = withTimeoutOrNull(15_000.milliseconds) {
            firestore.collection("guilds")
                .whereEqualTo("inviteCode", cleaned)
                .limit(1)
                .get()
                .await()
        } ?: error("Could not reach the guild hall.")
        val doc = snap.documents.firstOrNull() ?: error("No guild found with code $cleaned.")
        val count = (doc.getLong("memberCount") ?: 0L).toInt()
        check(count < MAX_MEMBERS) { "This guild is full ($MAX_MEMBERS heroes)." }
        doc.reference.collection("members").document(uid).set(memberCard()).await()
        doc.reference.update("memberCount", FieldValue.increment(1)).await()
        setGuildId(doc.id)
        ensureWeeklyRaid(doc.id, force = false)
    }

    suspend fun leaveGuild(): Result<Unit> = runCatching {
        val uid = requireUid()
        val guildId = _guildId.value ?: return@runCatching
        val doc = guildDoc(guildId)
        doc.collection("members").document(uid).delete().await()
        doc.update("memberCount", FieldValue.increment(-1)).await()
        setGuildId(null)
    }

    fun reportSessionXp(xp: Int) {
        if (xp <= 0) return
        val guildId = _guildId.value ?: return
        val uid = auth.state.value.uid ?: return
        // Guild raids take scaled damage so large rosters still need teamwork.
        val damage = (xp * 1.25).toLong().coerceAtLeast(1L)
        guildDoc(guildId).update(
            mapOf(
                "raidHp" to FieldValue.increment(-damage),
                "raidDamage.$uid" to FieldValue.increment(damage)
            )
        )
        scope.launch { refreshMemberCard() }
    }

    /** Claim weekly raid spoils once the colossus falls. */
    suspend fun claimRaidReward(): Result<RewardBatch> = runCatching {
        val uid = requireUid()
        val guildId = _guildId.value ?: error("Not in a guild.")
        val raid = _state.value.raid ?: error("No raid active.")
        check(raid.defeated) { "The guild raid still stands!" }
        val myDamage = raid.damageByUid[uid] ?: 0L
        check(myDamage > 0L) { "Only contributors may claim spoils." }
        check(uid !in raid.claimedBy) { "You already claimed this week's spoils." }
        guildDoc(guildId).update("raidClaimed", FieldValue.arrayUnion(uid)).await()

        val top = raid.damageByUid.maxByOrNull { it.value }?.key
        val batch = repository.grantGuildRaidReward(topContributor = top == uid)
        _state.update { it.copy(lastRewardBatch = batch) }
        batch
    }

    /** Starts a fresh raid for the current ISO week if missing or forced. */
    suspend fun ensureWeeklyRaid(guildId: String? = _guildId.value, force: Boolean = false): Result<Unit> =
        runCatching {
            val id = guildId ?: return@runCatching
            val doc = guildDoc(id)
            val snap = doc.get().await()
            val week = currentWeekId()
            val existingWeek = snap.getString("raidWeekId")
            if (!force && existingWeek == week) return@runCatching
            val members = _state.value.members.size.coerceAtLeast(
                (snap.getLong("memberCount") ?: 1L).toInt()
            )
            val hp = raidHpFor(members)
            val (name, emoji) = RAID_BOSSES[Random.nextInt(RAID_BOSSES.size)]
            doc.update(
                mapOf(
                    "raidWeekId" to week,
                    "raidName" to name,
                    "raidEmoji" to emoji,
                    "raidMaxHp" to hp,
                    "raidHp" to hp,
                    "raidDamage" to emptyMap<String, Long>(),
                    "raidClaimed" to emptyList<String>(),
                    "raidEndsAt" to weekEndMillis()
                )
            ).await()
        }

    private suspend fun refreshMemberCard() {
        val uid = auth.state.value.uid ?: return
        val guildId = _guildId.value ?: return
        val card = memberCard()
        runCatching {
            val membersColl = guildDoc(guildId).collection("members")
            membersColl.document(uid).set(card, com.google.firebase.firestore.SetOptions.merge()).await()
            val heroName = card["name"] as? String ?: ""
            if (heroName.isNotBlank()) {
                val snap = membersColl.get().await()
                for (doc in snap.documents) {
                    if (doc.id != uid) {
                        val docName = doc.getString("name").orEmpty()
                        if (docName.trim().equals(heroName.trim(), ignoreCase = true)) {
                            membersColl.document(doc.id).delete().await()
                        }
                    }
                }
            }
        }
    }

    private suspend fun memberCard(): Map<String, Any?> {
        val c = repository.getCharacter()
        val cls = c.characterClass
        return mapOf(
            "name" to c.name,
            "classEmoji" to (cls?.emoji ?: "\uD83E\uDDB8"),
            "characterClass" to cls?.name,
            "level" to c.level,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    private suspend fun setGuildId(id: String?) {
        repository.updateGuildId(id)
        _guildId.value = id
    }

    private fun guildDoc(id: String) = firestore.collection("guilds").document(id)

    private fun requireUid(): String = auth.state.value.uid ?: error("Sign in required.")

    private fun generateInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..CODE_LENGTH).map { alphabet.random() }.joinToString("")
    }

    companion object {
        private const val KEY_GUILD_ID = "guild_id"
        private const val CODE_LENGTH = 6
        private const val MAX_MEMBERS = 20

        private val RAID_BOSSES = listOf(
            "Guild Colossus" to "\uD83D\uDC32",
            "Siege Hydra" to "\uD83D\uDC09",
            "Ashen Titan" to "\uD83E\uDD16",
            "Vault Warden" to "\uD83D\uDEE1\uFE0F"
        )

        fun currentWeekId(): String {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            return "%04d-W%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR))
        }

        fun weekEndMillis(): Long {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.firstDayOfWeek = Calendar.MONDAY
            cal[Calendar.DAY_OF_WEEK] = Calendar.MONDAY
            cal.add(Calendar.WEEK_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun raidHpFor(memberCount: Int): Long =
            (8_000L + memberCount * 4_500L).coerceAtLeast(10_000L)
    }
}
