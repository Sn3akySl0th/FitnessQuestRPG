package com.fitnessquest.rpg.data.party

import android.app.Application
import android.util.Log
import android.content.Context
import com.fitnessquest.rpg.data.GameRepository
import com.fitnessquest.rpg.data.auth.AuthService
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.WorkoutExerciseEntity
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.Reward
import com.fitnessquest.rpg.domain.RewardBatch
import com.fitnessquest.rpg.domain.RewardSource
import com.fitnessquest.rpg.data.sync.OutboxSyncResult
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds

data class PartyMember(
    val uid: String,
    val name: String,
    val classEmoji: String,
    val level: Int,
    val weeklyXp: Int,
    /** Damage dealt to the current raid boss. */
    val bossDamage: Long = 0,
)

data class PartyBoss(
    val tier: Int,
    val name: String,
    val emoji: String,
    val maxHp: Long,
    val hp: Long,
    val claimedBy: List<String> = emptyList(),
    val damageByUid: Map<String, Long> = emptyMap(),
) {
    val defeated: Boolean get() = hp <= 0
    val rewardGold: Int get() = 100 + (50 * tier)
    val rewardXpBoost: Int get() = 50 + (25 * tier)
}

data class SharedExercise(
    val name: String,
    val category: ExerciseCategory,
    val sets: Int,
    val reps: Int
)

data class SharedWorkout(
    val id: String,
    val name: String,
    val authorUid: String,
    val authorName: String,
    val exercises: List<SharedExercise>
)

data class PartyPulse(
    val uid: String,
    val xp: Int,
    val updatedAt: Long,
    val effects: List<String> = emptyList()
)

data class PartyState(
    val loading: Boolean = true,
    val partyId: String? = null,
    val name: String = "",
    val inviteCode: String = "",
    val members: List<PartyMember> = emptyList(),
    val boss: PartyBoss? = null,
    val sharedWorkouts: List<SharedWorkout> = emptyList(),
    val activePulses: List<PartyPulse> = emptyList()
) {
    val inParty: Boolean get() = partyId != null
}

/**
 * Firestore-backed workout parties: friends join with an invite code, share
 * workouts, and fight a shared raid boss whose HP scales with the roster so
 * no single hero can carry it. Every XP point earned in a real session is
 * dealt to the boss as damage.
 */
class PartyService(
    app: Application,
    private val repository: GameRepository,
    private val auth: AuthService
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = app.getSharedPreferences("fitquest_party", Context.MODE_PRIVATE)

    private val _partyId = MutableStateFlow(prefs.getString(KEY_PARTY_ID, null))
    private val _state = MutableStateFlow(PartyState())
    val state: StateFlow<PartyState> = _state

    private val listeners = mutableListOf<ListenerRegistration>()

    fun start() {
        scope.launch {
            repository.character
                .map { it.partyId }
                .distinctUntilChanged()
                .collectLatest { partyId ->
                    _partyId.value = partyId
                }
        }
        scope.launch {
            combine(
                auth.state.map { it.uid }.distinctUntilChanged(),
                _partyId
            ) { uid, partyId -> uid to partyId }
                .collectLatest { (uid, partyId) ->
                    detach()
                    if ((uid == null) || (partyId == null)) {
                        _state.value = PartyState(loading = false)
                        return@collectLatest
                    }
                    _state.value = PartyState(loading = true, partyId = partyId)
                    attach(partyId)
                    refreshMemberCard()
                }
        }
    }

    // ---- Live listeners ----

    private fun attach(partyId: String) {
        val doc = partyDoc(partyId)
        listeners += doc.addSnapshotListener { snap, _ ->
            if ((snap != null) && !snap.exists() && !snap.metadata.isFromCache) {
                // Party was disbanded elsewhere; drop back to the lobby.
                scope.launch { setPartyId(null) }
                return@addSnapshotListener
            }
            if (snap == null || !snap.exists()) return@addSnapshotListener
            @Suppress("UNCHECKED_CAST")
            val damage = (snap["bossDamage"] as? Map<String, Any?>).orEmpty()
                .mapValues { (it.value as? Number)?.toLong() ?: 0L }
            val boss = if (snap.getLong("bossMaxHp") != null) {
                PartyBoss(
                    tier = (snap.getLong("bossTier") ?: 1L).toInt(),
                    name = snap.getString("bossName") ?: "Raid Boss",
                    emoji = snap.getString("bossEmoji") ?: "\uD83D\uDC79",
                    maxHp = snap.getLong("bossMaxHp") ?: 1L,
                    hp = snap.getLong("bossHp") ?: 0L,
                    claimedBy = (snap["bossClaimed"] as? List<*>).orEmpty().filterIsInstance<String>(),
                    damageByUid = damage
                )
            } else null
            _state.update {
                it.copy(
                    loading = false,
                    name = snap.getString("name") ?: "Party",
                    inviteCode = snap.getString("inviteCode") ?: "",
                    boss = boss,
                    members = it.members.map { m -> m.copy(bossDamage = damage[m.uid] ?: 0L) }
                )
            }
        }
        listeners += doc.collection("members").addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            val damage = _state.value.boss?.damageByUid.orEmpty()
            val myUid = auth.state.value.uid
            val rawMembers = snap.documents.map { d ->
                PartyMember(
                    uid = d.id,
                    name = d.getString("name") ?: "Hero",
                    classEmoji = d.getString("classEmoji") ?: "\uD83E\uDDB8",
                    level = (d.getLong("level") ?: 1L).toInt(),
                    weeklyXp = (d.getLong("weeklyXp") ?: 0L).toInt(),
                    bossDamage = damage[d.id] ?: 0L
                )
            }
            val members = rawMembers
                .filter { !com.fitnessquest.rpg.domain.isDemoUser(it.name) || it.uid == myUid }
                .groupBy { it.name.trim().lowercase() }
                .map { (_, duplicates) ->
                    duplicates.maxByOrNull { it.weeklyXp } ?: duplicates.first()
                }
                .distinctBy { it.uid }
                .sortedByDescending { it.weeklyXp }
            _state.update { it.copy(loading = false, members = members) }
        }
        listeners += doc.collection("workouts").addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            val workouts = snap.documents.mapNotNull { d ->
                @Suppress("UNCHECKED_CAST")
                val exercises = (d.get("exercises") as? List<Map<String, Any?>>).orEmpty().map { e ->
                    SharedExercise(
                        name = e["name"] as? String ?: "Exercise",
                        category = (e["category"] as? String)
                            ?.let { name -> ExerciseCategory.entries.find { c -> c.name == name } }
                            ?: ExerciseCategory.STRENGTH,
                        sets = (e["sets"] as? Number)?.toInt() ?: 3,
                        reps = (e["reps"] as? Number)?.toInt() ?: 10
                    )
                }
                if (exercises.isEmpty()) return@mapNotNull null
                SharedWorkout(
                    id = d.id,
                    name = d.getString("name") ?: "Workout",
                    authorUid = d.getString("authorUid") ?: "",
                    authorName = d.getString("authorName") ?: "A hero",
                    exercises = exercises
                )
            }.sortedBy { it.name }
            _state.update { it.copy(sharedWorkouts = workouts) }
        }
        listeners += doc.collection("pulses").addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            val now = System.currentTimeMillis()
            val pulses = snap.documents.mapNotNull { d ->
                val xp = (d.getLong("xp") ?: 0L).toInt()
                val updated = d.getTimestamp("updatedAt")?.toDate()?.time ?: now
                @Suppress("UNCHECKED_CAST")
                val effects = (d["effects"] as? List<String>).orEmpty()
                // Safety: Show all pulses regardless of local clock sync
                if (xp > 0 || effects.isNotEmpty()) {
                    PartyPulse(uid = d.id, xp = xp, updatedAt = updated, effects = effects)
                } else null
            }
            Log.d("PartyService", "Received ${pulses.size} active pulses from Firestore")
            _state.update { it.copy(activePulses = pulses) }
        }
    }

    private fun detach() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    // ---- Create / join / leave ----

    suspend fun createParty(name: String): Result<Unit> = runCatching {
        val uid = requireUid()
        val code = generateInviteCode()
        val doc = firestore.collection("parties").document()
        val bossHp = bossHpFor(memberCount = 1, tier = 1)
        doc.set(
            mapOf(
                "name" to name.trim().take(40).ifBlank { "Adventuring Party" },
                "inviteCode" to code,
                "leaderUid" to uid,
                "createdAt" to System.currentTimeMillis(),
                "memberCount" to 1,
                "bossTier" to 1,
                "bossName" to BOSSES[0].first,
                "bossEmoji" to BOSSES[0].second,
                "bossMaxHp" to bossHp,
                "bossHp" to bossHp,
                "bossDamage" to emptyMap<String, Long>(),
                "bossClaimed" to emptyList<String>()
            )
        ).await()
        doc.collection("members").document(uid).set(memberCard()).await()
        setPartyId(doc.id)
    }

    suspend fun joinParty(code: String): Result<Unit> = runCatching {
        val uid = requireUid()
        val cleaned = code.trim().uppercase()
        require(cleaned.length == CODE_LENGTH) { "Invite codes are $CODE_LENGTH characters." }
        val snap = withTimeoutOrNull(15_000.milliseconds) {
            firestore.collection("parties")
                .whereEqualTo("inviteCode", cleaned)
                .limit(1)
                .get()
                .await()
        } ?: error("Could not reach the guild hall. Check your connection.")
        val doc = snap.documents.firstOrNull()
            ?: error("No party found with code $cleaned.")
        doc.reference.collection("members").document(uid).set(memberCard()).await()
        doc.reference.update("memberCount", FieldValue.increment(1)).await()
        setPartyId(doc.id)
    }

    suspend fun leaveParty(): Result<Unit> = runCatching {
        val uid = requireUid()
        val partyId = _partyId.value ?: return@runCatching
        val doc = partyDoc(partyId)
        doc.collection("members").document(uid).delete().await()
        doc.update("memberCount", FieldValue.increment(-1)).await()
        setPartyId(null)
    }

    // ---- Raid boss ----

    /**
     * Called after every completed workout session: the XP earned is dealt to
     * the party's raid boss as damage. Transactionally checks eventId for remote idempotency.
     */
    suspend fun reportSessionXp(
        xp: Int,
        eventId: String,
        targetPartyId: String? = null,
        targetUid: String? = null
    ): OutboxSyncResult {
        if (xp <= 0) {
            Log.d("PartyService", "reportSessionXp: XP is <= 0 ($xp), skipping.")
            return OutboxSyncResult.NOT_APPLICABLE
        }
        
        // Fallback resolution: Outbox payload -> Local StateFlow -> Repository fallback (Issue 3.2)
        val partyId = targetPartyId ?: _partyId.value ?: repository.getCharacter().partyId
        val uid = targetUid ?: auth.state.value.uid

        if (partyId == null) {
            Log.d("PartyService", "reportSessionXp: partyId is null, retrying later.")
            return OutboxSyncResult.RETRYABLE_FAILURE // Issue 3.3
        }
        if (uid == null) {
            Log.d("PartyService", "reportSessionXp: uid is null, retrying later.")
            return OutboxSyncResult.RETRYABLE_FAILURE // Issue 3.3
        }
        
        val doc = partyDoc(partyId)
        val eventDoc = doc.collection("processedEvents").document(eventId)
        
        return try {
            val result = firestore.runTransaction { transaction ->
                val partySnap = transaction.get(doc)
                if (!partySnap.exists()) {
                    Log.d("PartyService", "reportSessionXp: Party doc $partyId missing.")
                    return@runTransaction OutboxSyncResult.NOT_APPLICABLE
                }

                val snapshot = transaction.get(eventDoc)
                if (snapshot.exists()) {
                    OutboxSyncResult.ALREADY_PROCESSED
                } else {
                    // Issue 4.1: Verify bossHp exists and is > 0
                    val currentHp = partySnap.getLong("bossHp")
                    if (currentHp == null || currentHp <= 0) {
                        Log.d("PartyService", "reportSessionXp: Boss missing or already at 0 HP.")
                        transaction.set(eventDoc, mapOf("processedAt" to FieldValue.serverTimestamp(), "xp" to xp, "uid" to uid))
                        return@runTransaction OutboxSyncResult.DELIVERED
                    }

                    // Issue 4.2: Clamp bossHp to 0 after damage
                    val newHp = (currentHp - xp).coerceAtLeast(0L)
                    
                    val updates = mutableMapOf<String, Any>(
                        "bossHp" to newHp,
                        "bossDamage.$uid" to FieldValue.increment(xp.toLong())
                    )

                    // Issue 4.3: Initialize bossDamage map if missing
                    if (partySnap.get("bossDamage") == null) {
                        transaction.update(doc, "bossDamage", emptyMap<String, Long>())
                    }

                    transaction.set(eventDoc, mapOf("processedAt" to FieldValue.serverTimestamp(), "xp" to xp, "uid" to uid))
                    transaction.update(doc, updates)
                    OutboxSyncResult.DELIVERED
                }
            }.await()
            scope.launch { refreshMemberCard() }
            result
        } catch (e: Exception) {
            OutboxSyncResult.RETRYABLE_FAILURE
        }
    }

    /** Grants the boss reward locally and records the claim so it's once per hero. */
    suspend fun claimBossReward(): Result<RewardBatch> = runCatching {
        val uid = requireUid()
        val partyId = _partyId.value ?: error("Not in a party.")
        val boss = _state.value.boss ?: error("No boss to claim.")
        check(boss.defeated) { "The boss still stands!" }
        check((boss.damageByUid[uid] ?: 0L) > 0L) { "Only heroes who dealt damage may claim the spoils." }
        check(uid !in boss.claimedBy) { "You already claimed this reward." }
        partyDoc(partyId).update("bossClaimed", FieldValue.arrayUnion(uid)).await()
        repository.grantBountyReward(gold = boss.rewardGold, xpBoost = boss.rewardXpBoost)
        
        val rewards = listOf(
            Reward.Gold(boss.rewardGold),
            Reward.XpBoost(boss.rewardXpBoost)
        )
        RewardBatch(RewardSource.BATTLE, rewards)
    }

    /** Spawns the next, tougher boss. HP scales with the current roster size. */
    suspend fun summonNextBoss(): Result<Unit> = runCatching {
        val partyId = _partyId.value ?: error("Not in a party.")
        val boss = _state.value.boss ?: error("No boss.")
        check(boss.defeated) { "The current boss still stands!" }
        val tier = boss.tier + 1
        val members = _state.value.members.size.coerceAtLeast(1)
        val hp = bossHpFor(members, tier)
        val (name, emoji) = BOSSES[(tier - 1) % BOSSES.size]
        partyDoc(partyId).update(
            mapOf(
                "bossTier" to tier,
                "bossName" to name,
                "bossEmoji" to emoji,
                "bossMaxHp" to hp,
                "bossHp" to hp,
                "bossDamage" to emptyMap<String, Long>(),
                "bossClaimed" to emptyList<String>()
            )
        ).await()
    }

    // ---- Shared workouts ----

    suspend fun shareWorkout(workoutId: Long): Result<Unit> = runCatching {
        val uid = requireUid()
        val partyId = _partyId.value ?: error("Not in a party.")
        val workout = repository.getWorkout(workoutId) ?: error("Workout not found.")
        val exercises = repository.exercisesFor(workoutId)
        check(exercises.isNotEmpty()) { "That quest has no exercises to share." }
        val character = repository.getCharacter()
        partyDoc(partyId).collection("workouts").add(
            mapOf(
                "name" to workout.name,
                "authorUid" to uid,
                "authorName" to character.name,
                "createdAt" to System.currentTimeMillis(),
                "exercises" to exercises.map {
                    mapOf(
                        "name" to it.exerciseName,
                        "category" to it.category.name,
                        "sets" to it.targetSets,
                        "reps" to it.targetReps
                    )
                }
            )
        ).await()
    }

    /** Copies a shared workout into the local quest list. Returns the new local ID. */
    suspend fun importWorkout(shared: SharedWorkout): Result<Long> = runCatching {
        repository.saveWorkout(
            name = shared.name,
            exercises = shared.exercises.map {
                WorkoutExerciseEntity(
                    workoutId = 0,
                    exerciseName = it.name,
                    category = it.category,
                    targetSets = it.sets,
                    targetReps = it.reps
                )
            }
        )
    }

    /** Deletes a shared workout from the party. Only the author can delete. */
    suspend fun deleteSharedWorkout(sharedWorkoutId: String): Result<Unit> = runCatching {
        val partyId = _partyId.value ?: error("Not in a party.")
        val uid = auth.state.value.uid ?: error("Not signed in.")
        
        val doc = partyDoc(partyId).collection("workouts").document(sharedWorkoutId)
        val snap = doc.get().await()
        if (!snap.exists()) return@runCatching // Already gone
        
        val authorUid = snap.getString("authorUid")
        if (authorUid != uid) {
            error("Only the hero who shared this quest can remove it.")
        }
        
        doc.delete().await()
    }

    // ---- Active Pulses (Ghost Damage) ----

    /**
     * Publishes current session XP to the party so others can see "ghost damage" 
     * on the boss. This is transient and cleared when the workout finishes.
     */
    suspend fun sendActivePulse(xp: Int, effects: List<String> = emptyList()) = runCatching {
        val partyId = _partyId.value ?: return@runCatching
        val uid = auth.state.value.uid ?: return@runCatching
        
        Log.d("PartyService", "Attempting pulse: xp=$xp, effects=$effects, party=$partyId, uid=$uid")

        if (xp <= 0 && effects.isEmpty()) {
            clearActivePulse()
            return@runCatching
        }
        partyDoc(partyId).collection("pulses").document(uid).set(
            mapOf(
                "xp" to xp,
                "effects" to effects,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }.onFailure {
        Log.e("PartyService", "Failed to send pulse", it)
    }

    suspend fun clearActivePulse() = runCatching {
        val partyId = _partyId.value ?: return@runCatching
        val uid = auth.state.value.uid ?: return@runCatching
        partyDoc(partyId).collection("pulses").document(uid).delete().await()
    }

    // ---- Member card ----

    /** Publishes my hero card (name, class, level, weekly XP) to the party roster. */
    private suspend fun refreshMemberCard() {
        val uid = auth.state.value.uid ?: return
        val partyId = _partyId.value ?: return
        val card = memberCard()
        runCatching {
            val membersColl = partyDoc(partyId).collection("members")
            membersColl.document(uid).set(card, SetOptions.merge()).await()
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
        val character = repository.getCharacter()
        val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val weeklyXp = repository.sessions.first()
            .filter { it.endedAt >= weekAgo }
            .sumOf { it.xpEarned }
        return mapOf(
            "name" to character.name,
            "classEmoji" to (character.characterClass ?: CharacterClass.WARRIOR).emoji,
            "level" to character.level,
            "weeklyXp" to weeklyXp,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    // ---- Helpers ----

    private fun partyDoc(partyId: String): DocumentReference =
        firestore.collection("parties").document(partyId)

    private suspend fun setPartyId(id: String?) {
        repository.updatePartyId(id)
        _partyId.value = id
    }

    private fun requireUid(): String =
        auth.state.value.uid ?: error("Sign in first (Settings > Account).")

    private fun generateInviteCode(): String =
        (1..CODE_LENGTH).map { CODE_CHARS.random() }.joinToString("")

    private companion object {
        const val KEY_PARTY_ID = "party_id"
        const val CODE_LENGTH = 6

        /** No 0/O/1/I so codes are easy to read aloud at the gym. */
        const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        /**
         * Boss HP per member per tier. ~1200 XP is roughly 3-5 solid workouts,
         * so a boss takes each member most of a week - nobody can solo it.
         */
        const val BOSS_HP_PER_MEMBER = 1200L

        val BOSSES = listOf(
            "Iron Golem" to "\uD83D\uDDFF",
            "Frost Wyrm" to "\uD83D\uDC09",
            "Shadow Behemoth" to "\uD83D\uDC79",
            "Storm Titan" to "\u26A1",
            "Bone Colossus" to "\uD83D\uDC80",
            "Inferno Drake" to "\uD83D\uDD25",
            "Void Leviathan" to "\uD83D\uDC19",
            "Celestial Warden" to "\uD83C\uDF1F"
        )

        fun bossHpFor(memberCount: Int, tier: Int): Long =
            BOSS_HP_PER_MEMBER * memberCount.coerceAtLeast(1) * (100 + 25 * (tier - 1)) / 100
    }
}
