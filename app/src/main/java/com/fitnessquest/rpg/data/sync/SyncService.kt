package com.fitnessquest.rpg.data.sync

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import com.fitnessquest.rpg.data.UserPrefs
import com.fitnessquest.rpg.data.auth.AuthService
import com.fitnessquest.rpg.data.auth.UsernameService
import com.fitnessquest.rpg.data.db.AppDatabase
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ClassProgressEntity
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.GearInstanceEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.data.db.MovementMasteryEntity
import com.fitnessquest.rpg.data.db.PendingSyncEntity
import com.fitnessquest.rpg.data.db.SessionEntity
import com.fitnessquest.rpg.data.db.SetLogEntity
import com.fitnessquest.rpg.data.db.WorkoutEntity
import com.fitnessquest.rpg.data.db.WorkoutExerciseEntity
import com.fitnessquest.rpg.data.db.isStackable
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.SetType
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

data class SyncStatus(
    val lastSyncAt: Long? = null,
    val error: String? = null,
    val isReconciling: Boolean = false,
)

/**
 * Cloud backup for signed-in players. Room stays the source of truth; this service
 * mirrors the hero, owned items, and workout history to Firestore.
 *
 * - Pushes automatically (debounced) whenever local data changes.
 * - On sign-in, compares the cloud hero to the local one and restores the cloud
 *   copy if it is further along (new device / reinstall case).
 * - Developer sandbox: no cloud pushes while active; signing into a real account
 *   always restores the cloud hero so local cheats cannot overwrite live saves.
 * - Firestore's offline persistence queues writes made without a connection.
 */
class SyncService(
    private val app: Application,
    private val db: AppDatabase,
    private val auth: AuthService,
    private val userPrefs: UserPrefs,
    private val usernameService: UsernameService
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = app.getSharedPreferences("fitquest_sync", Context.MODE_PRIVATE)

    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status

    fun start() {
        scope.launch {
            auth.state
                .map { if (it.hasAccount) it.uid else null }
                .distinctUntilChanged()
                .collectLatest { uid ->
                    if (uid == null) {
                        _status.value = _status.value.copy(isReconciling = false)
                        return@collectLatest
                    }
                    val leavingSandbox = userPrefs.developerSandbox.value
                    if (leavingSandbox) {
                        userPrefs.setDeveloperSandbox(value = false)
                    }
                    _status.value = _status.value.copy(isReconciling = true)
                    reconcile(uid, force = leavingSandbox)
                    _status.value = _status.value.copy(isReconciling = false)
                    // Live premium grants/revokes from the Firebase console.
                    val premiumListen = launch { listenCloudPremium(uid) }
                    try {
                        val characterGearFlow = combine(
                            db.characterDao().observe().filterNotNull(),
                            db.itemDao().observeAll(),
                            db.gearInstanceDao().observeAll()
                        ) { c, i, inst -> Triple(c, i, inst) }

                        val progressMasteryFlow = combine(
                            db.classProgressDao().observeAll(),
                            db.movementMasteryDao().observeAll(),
                            userPrefs.developerSandbox
                        ) { cp, mm, s -> Triple(cp, mm, s) }

                        val coreFlow = combine(characterGearFlow, progressMasteryFlow) { (c, i, inst), (cp, mm, s) ->
                            CoreSync(c, i, inst, cp, mm, s)
                        }

                        val activityFlow = combine(
                            db.sessionDao().observeAll(),
                            db.workoutDao().observeAll()
                        ) { s, w -> s to w }

                        val settingsFlow = combine(
                            userPrefs.imperial,
                            userPrefs.sound,
                            userPrefs.haptics,
                            userPrefs.useLocalAi,
                            userPrefs.hevyApiKey
                        ) { imp, snd, hap, loc, hevKey ->
                            listOf(imp, snd, hap, loc, hevKey)
                        }

                        combine(coreFlow, activityFlow, settingsFlow) { core, activity, _ ->
                            SyncPayload(
                                character = core.character,
                                items = core.items,
                                instances = core.instances,
                                classProgress = core.classProgress,
                                masteries = core.masteries,
                                sessions = activity.first,
                                workouts = activity.second,
                                sandbox = core.sandbox
                            )
                        }
                            .collectLatest { payload ->
                                if (payload.sandbox) return@collectLatest
                                delay(2_000.milliseconds)
                                push(
                                    uid,
                                    payload.character,
                                    payload.items,
                                    payload.instances,
                                    payload.classProgress,
                                    payload.masteries,
                                    payload.sessions,
                                    payload.workouts
                                )
                            }
                    } finally {
                        premiumListen.cancel()
                    }
                }
        }
    }

    /**
     * Writes [premium] on `users/{uid}` (merge) so console grants and redeem codes
     * stay in sync across devices. No-op for guests / sandbox.
     */
    fun setCloudPremium(enabled: Boolean) {
        scope.launch {
            val uid = auth.currentUid() ?: return@launch
            if (!auth.state.value.hasAccount || userPrefs.developerSandbox.value) return@launch
            runCatching {
                userDoc(uid).set(
                    mapOf(
                        "premium" to enabled,
                        "premiumUpdatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
            }
        }
    }

    /** Permanently removes a session from this account's cloud history. */
    fun deleteCloudSession(uid: String, sessionId: Long) {
        scope.launch {
            runCatching {
                userDoc(uid).collection("sessions")
                    .document(sessionId.toString())
                    .delete()
                    .await()
            }
        }
    }

    /** Permanently removes a workout routine from this account's cloud storage. */
    fun deleteCloudWorkout(uid: String, workoutId: Long) {
        scope.launch {
            runCatching {
                userDoc(uid).collection("workouts")
                    .document(workoutId.toString())
                    .delete()
                    .await()
            }
        }
    }

    private suspend fun listenCloudPremium(uid: String) {
        suspendCancellableCoroutine<Unit> { cont ->
            val regs = mutableListOf<ListenerRegistration>()
            regs += userDoc(uid).addSnapshotListener { snap, _ ->
                snap?.let { applyCloudPremium(uid, it) }
            }
            // Separate tiny docs are easier to edit in the Firebase console.
            regs += firestore.collection(PREMIUM_GRANTS).document(uid)
                .addSnapshotListener { snap, _ ->
                    if (snap == null || !snap.exists()) return@addSnapshotListener
                    val granted = when {
                        snap.contains("premium") -> snap.getBoolean("premium") == true
                        snap.contains("enabled") -> snap.getBoolean("enabled") == true
                        else -> true // doc exists with no flag → treat as granted
                    }
                    userPrefs.setPremium(granted)
                    if (granted) {
                        // Mirror onto the user doc so future syncs stay consistent.
                        setCloudPremium(true)
                    }
                }
            cont.invokeOnCancellation { regs.forEach { it.remove() } }
        }
    }

    private fun applyCloudPremium(uid: String, doc: DocumentSnapshot) {
        if (!doc.exists()) return
        val root = if (doc.contains("premium")) doc.getBoolean("premium") else null
        // Firebase console often nests "Add field" under the last opened map
        // (commonly gearInstances). Honor that and heal it back to the root.
        val nestedInGear = nestedPremiumFromMap(doc["gearInstances"])
        val nestedInStacks = nestedPremiumFromMap(doc["stacks"])
        val nestedInCharacter = nestedPremiumFromMap(doc["character"])
        val effective = root ?: nestedInGear ?: nestedInStacks ?: nestedInCharacter
        if (effective != null) {
            userPrefs.setPremium(effective)
        }
        if (root == null && (nestedInGear != null || nestedInStacks != null || nestedInCharacter != null)) {
            healMisplacedPremium(uid, effective == true, doc)
        }
    }

    private fun nestedPremiumFromMap(value: Any?): Boolean? {
        val map = value as? Map<*, *> ?: return null
        return (map["premium"] as? Boolean)
    }

    /**
     * Moves a console-misplaced `premium` to the document root and repairs
     * `gearInstances` / `stacks` if the console turned them into metadata maps.
     */
    private fun healMisplacedPremium(uid: String, enabled: Boolean, doc: DocumentSnapshot) {
        scope.launch {
            runCatching {
                val updates = mutableMapOf<String, Any>(
                    "premium" to enabled,
                    "premiumUpdatedAt" to System.currentTimeMillis()
                )
                val gear = doc.get("gearInstances")
                if (gear is Map<*, *>) {
                    (gear["schema"] as? Number)?.toInt()?.let { updates["schema"] = it }
                    // Real gear is a list; a map here is console damage — reset empty.
                    updates["gearInstances"] = emptyList<Map<String, Any?>>()
                }
                val stacks = doc.get("stacks")
                if (stacks is Map<*, *>) {
                    val looksLikeMetaOnly = stacks.keys.none { it.toString().toLongOrNull() != null }
                    if (looksLikeMetaOnly) {
                        (stacks["updatedAt"] as? Number)?.toLong()?.let { updates["updatedAt"] = it }
                        (stacks["schema"] as? Number)?.toInt()?.let { updates["schema"] = it }
                        updates["stacks"] = emptyMap<String, Int>()
                    }
                }
                val character = doc.get("character")
                if ((character is Map<*, *>) && (character["premium"] != null)) {
                    // Can't delete a nested key via merge set easily without FieldValue;
                    // root premium is enough for the app.
                }
                userDoc(uid).set(updates, SetOptions.merge()).await()
            }
        }
    }

    /**
     * Forces a reconciliation for the current user. Useful during onboarding
     * to ensure returning players get their data before proceeding.
     */
    suspend fun forceReconcile() {
        val uid = auth.currentUid() ?: return
        if (!auth.state.value.hasAccount) return
        _status.value = _status.value.copy(isReconciling = true)
        reconcile(uid, force = true)
        _status.value = _status.value.copy(isReconciling = false)
    }

    private fun userDoc(uid: String): DocumentReference =
        firestore.collection("users").document(uid)

    /**
     * On sign-in: if the cloud hero is further along than the local one,
     * restore it (plus owned items and session history).
     * [force] always restores cloud (used when leaving developer sandbox).
     */
    private suspend fun reconcile(uid: String, force: Boolean = false) {
        try {
            val doc = withTimeoutOrNull(15_000.milliseconds) { userDoc(uid).get().await() } ?: return
            if (!doc.exists()) return
            applyCloudPremium(uid, doc)

            val local = db.characterDao().get() ?: return
            @Suppress("UNCHECKED_CAST")
            val cloud = characterFromMap(doc.get("character") as? Map<String, Any?> ?: return)

            val cloudUpdatedAt = doc.getLong("updatedAt") ?: 0L
            val lastPushAt = prefs?.getLong("last_push_at_$uid", 0L) ?: 0L

            val cloudHasProgress = cloudIsAhead(cloud, local)
            val localHasProgress = cloudIsAhead(local, cloud)
            val cloudIsNewer = cloudUpdatedAt > lastPushAt
            val cloudHasIdentity = (cloud.characterClass != null && local.characterClass == null) ||
                (!cloud.name.equals("Hero", ignoreCase = true) && local.name.equals("Hero", ignoreCase = true))

            val shouldRestore = force || cloudHasProgress || (!localHasProgress && cloudIsNewer) || cloudHasIdentity
            if (!shouldRestore) return

            db.characterDao().upsert(cloud.copy(id = 1L))

            // Restore user settings & API keys
            @Suppress("UNCHECKED_CAST")
            (doc.get("settings") as? Map<String, Any?>)?.let { s ->
                (s["imperial"] as? Boolean)?.let { userPrefs.setImperial(it) }
                (s["sound"] as? Boolean)?.let { userPrefs.setSound(it) }
                (s["haptics"] as? Boolean)?.let { userPrefs.setHaptics(it) }
                (s["lowPowerUi"] as? Boolean)?.let { userPrefs.setLowPowerUi(it) }
                (s["showCardioIntensity"] as? Boolean)?.let { userPrefs.setShowCardioIntensity(it) }
                (s["useLocalAi"] as? Boolean)?.let { userPrefs.setUseLocalAi(it) }
                (s["geminiApiKey"] as? String)?.takeIf { it.isNotBlank() }?.let { key ->
                    (app as? com.fitnessquest.rpg.FitQuestApp)?.container?.gemini?.apiKey = key
                }
                (s["hevyApiKey"] as? String)?.takeIf { it.isNotBlank() }?.let { key ->
                    userPrefs.setHevyApiKey(key)
                }
                (s["hevyAutoSync"] as? Boolean)?.let { userPrefs.setHevyAutoSyncEnabled(it) }
            }

            // Stackable quantities (consumables, runes, materials, chests).
            @Suppress("UNCHECKED_CAST")
            (doc.get("stacks") as? Map<String, Any?> ?: doc.get("consumables") as? Map<String, Any?>)
                .orEmpty()
                .forEach { (id, qty) ->
                    val itemId = id.toLongOrNull() ?: return@forEach
                    val quantity = (qty as? Number)?.toInt() ?: return@forEach
                    if (quantity > 0) db.itemDao().setQuantity(itemId, quantity)
                }
            // Legacy owned catalog ids → create instances if none exist yet.
            val existingInstances = db.gearInstanceDao().getAll()
            if (existingInstances.isEmpty()) {
                val ownedIds = (doc.get("ownedItemIds") as? List<*>)
                    ?.mapNotNull { (it as? Number)?.toLong() }
                    .orEmpty()
                ownedIds.forEach { catalogId ->
                    val cat = db.itemDao().get(catalogId) ?: return@forEach
                    if (!cat.slot.isStackable() && cat.slot != ItemSlot.CONSUMABLE) {
                        db.gearInstanceDao().insert(
                            GearInstanceEntity(catalogId = cat.id, atk = cat.atk, def = cat.def, hp = cat.hp)
                        )
                    }
                }
            }
            @Suppress("UNCHECKED_CAST")
            val cloudInstances = doc.get("gearInstances") as? List<Map<String, Any?>>
            if (!cloudInstances.isNullOrEmpty()) {
                db.gearInstanceDao().deleteAll()
                // Remap cloud instance ids → local ids while rewriting equip slots.
                val idMap = mutableMapOf<Long, Long>()
                cloudInstances
                    .mapNotNull { m ->
                        val oldId = (m["id"] as? Number)?.toLong() ?: return@mapNotNull null
                        val catalogId = (m["catalogId"] as? Number)?.toLong() ?: return@mapNotNull null
                        RestoredGearInstance(
                            oldId = oldId,
                            entity = GearInstanceEntity(
                                catalogId = catalogId,
                                atk = (m["atk"] as? Number)?.toInt() ?: 0,
                                def = (m["def"] as? Number)?.toInt() ?: 0,
                                hp = (m["hp"] as? Number)?.toInt() ?: 0,
                                rune1Id = (m["rune1Id"] as? Number)?.toLong(),
                                rune2Id = (m["rune2Id"] as? Number)?.toLong()
                            )
                        )
                    }
                    .dedupeRestoredGear(preferredOldIds = cloud.equippedIds().values.filterNotNull().toSet())
                    .forEach { restored ->
                        val newId = db.gearInstanceDao().insert(restored.entity)
                        restored.oldIds.forEach { oldId -> idMap[oldId] = newId }
                    }
                val remapped = cloud.copy(
                    id = 1L,
                    weaponId = cloud.weaponId?.let { idMap[it] },
                    headId = cloud.headId?.let { idMap[it] },
                    chestId = cloud.chestId?.let { idMap[it] },
                    handsId = cloud.handsId?.let { idMap[it] },
                    legsId = cloud.legsId?.let { idMap[it] },
                    feetId = cloud.feetId?.let { idMap[it] },
                    trinketId = cloud.trinketId?.let { idMap[it] }
                )
                db.characterDao().upsert(remapped)
            }
            @Suppress("UNCHECKED_CAST")
            val cloudProgress = doc.get("classProgress") as? List<Map<String, Any?>>
            cloudProgress?.forEach { m ->
                db.classProgressDao().upsert(classProgressFromMap(m))
            }
            @Suppress("UNCHECKED_CAST")
            val cloudMasteries = doc.get("movementMastery") as? List<Map<String, Any?>>
            val restoredMasteries = cloudMasteries?.mapNotNull { masteryFromMap(it) }.orEmpty()
            if (restoredMasteries.isNotEmpty()) {
                db.movementMasteryDao().upsertAll(restoredMasteries)
            }
            restoreSessions(uid)
            restoreWorkouts(uid)
            // Backfill masteries from sessions if mastery table is still empty
            if (db.movementMasteryDao().getAll().isEmpty()) {
                (app as? com.fitnessquest.rpg.FitQuestApp)?.container?.repository?.recalculateAllMovementMasteriesFromHistory()
            }
            // Ensure the restored name is treated as a valid local claim.
            usernameService.ensureClaimedForExistingName()
            _status.value = SyncStatus(lastSyncAt = System.currentTimeMillis())
            prefs?.edit { putLong("last_push_at_$uid", cloudUpdatedAt) }
        } catch (e: Exception) {
            _status.value = _status.value.copy(error = e.message)
        }
    }

    private fun cloudIsAhead(cloud: CharacterEntity, local: CharacterEntity): Boolean =
        compareValuesBy(
            cloud, local,
            { it.level }, { it.xp }, { it.sessionsCompleted }, { it.battlesWon }
        ) > 0

    private data class RestoredGearInstance(
        val oldId: Long,
        val entity: GearInstanceEntity,
    )

    private data class DedupedRestoredGearInstance(
        val oldIds: List<Long>,
        val entity: GearInstanceEntity,
    )

    private fun List<RestoredGearInstance>.dedupeRestoredGear(preferredOldIds: Set<Long>): List<DedupedRestoredGearInstance> {
        return groupBy { it.entity.duplicateKey() }.values.map { group ->
            val keeper = group.sortedWith(
                compareByDescending<RestoredGearInstance> { it.oldId in preferredOldIds }
                    .thenBy { it.oldId }
            ).first()
            DedupedRestoredGearInstance(
                oldIds = group.map { it.oldId },
                entity = keeper.entity
            )
        }
    }

    private fun GearInstanceEntity.duplicateKey(): String = listOf(
        catalogId,
        atk,
        def,
        hp,
        rune1Id ?: 0L,
        rune2Id ?: 0L,
        upgradeLevel,
        rarity,
        traitIds,
        originBiome.orEmpty()
    ).joinToString("|")

    private suspend fun restoreSessions(uid: String) {
        val docs = withTimeoutOrNull(15_000.milliseconds) {
            userDoc(uid).collection("sessions").get().await()
        } ?: return
        for (doc in docs) {
            val id = doc.id.toLongOrNull() ?: continue
            val session = SessionEntity(
                id = id,
                name = doc.getString("name") ?: "Session",
                startedAt = doc.getLong("startedAt") ?: 0L,
                endedAt = doc.getLong("endedAt") ?: 0L,
                xpEarned = (doc.getLong("xpEarned") ?: 0L).toInt(),
                goldEarned = (doc.getLong("goldEarned") ?: 0L).toInt(),
                energyEarned = (doc.getLong("energyEarned") ?: 0L).toInt(),
                setCount = (doc.getLong("setCount") ?: 0L).toInt()
            )
            // IGNORE keeps any local session with the same id intact.
            val inserted = db.sessionDao().insertSessionKeepId(session)
            if (inserted != -1L) {
                @Suppress("UNCHECKED_CAST")
                val logs = (doc.get("logs") as? List<Map<String, Any?>>).orEmpty()
                db.sessionDao().insertSetLogs(logs.map { setLogFromMap(id, it) })
            }
            markSynced(uid, id)
        }
    }

    private suspend fun restoreWorkouts(uid: String) {
        val docs = withTimeoutOrNull(15_000.milliseconds) {
            userDoc(uid).collection("workouts").get().await()
        } ?: return
        for (doc in docs) {
            val id = doc.id.toLongOrNull() ?: continue
            val workout = WorkoutEntity(
                id = id,
                name = doc.getString("name") ?: "Quest",
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                aiGenerated = doc.getBoolean("aiGenerated") ?: false
            )
            // Save or replace the local workout.
            @Suppress("UNCHECKED_CAST")
            val exercises = (doc.get("exercises") as? List<Map<String, Any?>>).orEmpty()
            db.workoutDao().replaceWorkout(workout, exercises.map { workoutExerciseFromMap(id, it) })
        }
    }

    private suspend fun push(
        uid: String,
        character: CharacterEntity,
        items: List<ItemEntity>,
        instances: List<GearInstanceEntity>,
        classProgress: List<ClassProgressEntity>,
        masteries: List<MovementMasteryEntity>,
        sessions: List<SessionEntity>,
        workouts: List<WorkoutEntity>
    ) {
        if (userPrefs.developerSandbox.value) return
        try {
            val now = System.currentTimeMillis()
            val settingsMap = mapOf(
                "imperial" to userPrefs.imperial.value,
                "sound" to userPrefs.sound.value,
                "haptics" to userPrefs.haptics.value,
                "lowPowerUi" to userPrefs.lowPowerUi.value,
                "showCardioIntensity" to userPrefs.showCardioIntensity.value,
                "useLocalAi" to userPrefs.useLocalAi.value,
                "geminiApiKey" to ((app as? com.fitnessquest.rpg.FitQuestApp)?.container?.gemini?.userApiKey.orEmpty()),
                "hevyApiKey" to userPrefs.hevyApiKey.value,
                "hevyAutoSync" to userPrefs.hevyAutoSyncEnabled.value
            )

            // Merge so console-granted fields like `premium` are not wiped.
            userDoc(uid).set(
                mapOf(
                    "schema" to 4,
                    "updatedAt" to now,
                    "character" to characterToMap(character),
                    "settings" to settingsMap,
                    "classProgress" to classProgress.map { classProgressToMap(it) },
                    "movementMastery" to masteries.map { masteryToMap(it) },
                    "stacks" to items.asSequence()
                        .filter { it.slot.isStackable() && it.quantity > 0 }
                        .associateBy({ it.id.toString() }, { it.quantity }),
                    "consumables" to items.asSequence()
                        .filter { (it.slot == ItemSlot.CONSUMABLE) && (it.quantity > 0) }
                        .associateBy({ it.id.toString() }, { it.quantity }),
                    "gearInstances" to instances.map { gi ->
                        mapOf(
                            "id" to gi.id,
                            "catalogId" to gi.catalogId,
                            "atk" to gi.atk,
                            "def" to gi.def,
                            "hp" to gi.hp,
                            "rune1Id" to gi.rune1Id,
                            "rune2Id" to gi.rune2Id
                        )
                    }
                ),
                SetOptions.merge()
            )
            val synced = syncedIds(uid)
            sessions.filter { it.id !in synced }.forEach { session ->
                val logs = db.sessionDao().setLogsFor(session.id)
                userDoc(uid).collection("sessions")
                    .document(session.id.toString())
                    .set(sessionToMap(session, logs))
                markSynced(uid, session.id)
            }
            
            // Sync Workouts (Custom Programs)
            workouts.forEach { workout ->
                val exercises = db.workoutDao().exercisesFor(workout.id)
                userDoc(uid).collection("workouts")
                    .document(workout.id.toString())
                    .set(workoutToMap(workout, exercises))
            }

            publishLeaderboard(uid, character, sessions)
            _status.value = SyncStatus(lastSyncAt = System.currentTimeMillis())
            prefs?.edit { putLong("last_push_at_$uid", now) }
        } catch (e: Exception) {
            _status.value = _status.value.copy(error = e.message)
        }
    }

    /**
     * Permanently removes this account's cloud game data (user doc + sessions +
     * leaderboard). Call while still authenticated, before Auth user deletion.
     */
    suspend fun deleteCloudAccountData(): Result<Unit> {
        val uid = auth.currentUid()
            ?: return Result.failure(IllegalStateException("Not signed in."))
        return try {
            val sessions = withTimeoutOrNull(20_000.milliseconds) {
                userDoc(uid).collection("sessions").get().await()
            }
            sessions?.documents?.forEach { doc ->
                runCatching { doc.reference.delete().await() }
            }
            val workouts = withTimeoutOrNull(20_000.milliseconds) {
                userDoc(uid).collection("workouts").get().await()
            }
            workouts?.documents?.forEach { doc ->
                runCatching { doc.reference.delete().await() }
            }
            prefs.edit { remove("synced_$uid") }
            runCatching {
                firestore.collection("leaderboard").document(uid).delete().await()
            }
            runCatching { userDoc(uid).delete().await() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * After a local fresh-start reset: wipe cloud sessions / leaderboard and push
     * the new empty hero so reconcile cannot resurrect the old save.
     */
    suspend fun overwriteCloudAfterFreshStart() {
        val uid = auth.state.value.uid ?: return
        if (!auth.state.value.hasAccount || userPrefs.developerSandbox.value) return
        try {
            val sessions = withTimeoutOrNull(15_000.milliseconds) {
                userDoc(uid).collection("sessions").get().await()
            }
            sessions?.documents?.forEach { doc ->
                runCatching { doc.reference.delete().await() }
            }
            val workouts = withTimeoutOrNull(15_000.milliseconds) {
                userDoc(uid).collection("workouts").get().await()
            }
            workouts?.documents?.forEach { doc ->
                runCatching { doc.reference.delete().await() }
            }
            prefs.edit { remove("synced_$uid") }
            runCatching {
                firestore.collection("leaderboard").document(uid).delete().await()
            }
            val character = db.characterDao().get() ?: CharacterEntity()
            userDoc(uid).set(
                mapOf(
                    "schema" to 2,
                    "updatedAt" to System.currentTimeMillis(),
                    "character" to characterToMap(character),
                    "stacks" to emptyMap<String, Int>(),
                    "consumables" to emptyMap<String, Int>(),
                    "gearInstances" to emptyList<Map<String, Any?>>()
                ),
                SetOptions.merge()
            ).await()
            _status.value = SyncStatus(lastSyncAt = System.currentTimeMillis())
        } catch (e: Exception) {
            _status.value = _status.value.copy(error = e.message)
        }
    }

    private data class CoreSync(
        val character: CharacterEntity,
        val items: List<ItemEntity>,
        val instances: List<GearInstanceEntity>,
        val classProgress: List<ClassProgressEntity>,
        val masteries: List<MovementMasteryEntity>,
        val sandbox: Boolean
    )

    private data class SyncPayload(
        val character: CharacterEntity,
        val items: List<ItemEntity>,
        val instances: List<GearInstanceEntity>,
        val classProgress: List<ClassProgressEntity>,
        val masteries: List<MovementMasteryEntity>,
        val sessions: List<SessionEntity>,
        val workouts: List<WorkoutEntity>,
        val sandbox: Boolean
    )

    companion object {
        private const val PREMIUM_GRANTS = "premiumGrants"
    }

    /**
     * Public leaderboard entry: hero name and progress only, never account details.
     * Weekly XP keeps the board competitive for everyone, not just veterans.
     */
    private fun publishLeaderboard(
        uid: String,
        character: CharacterEntity,
        sessions: List<SessionEntity>
    ) {
        if (com.fitnessquest.rpg.domain.isDemoUser(character.name) || userPrefs.developerSandbox.value) {
            runCatching { firestore.collection("leaderboard").document(uid).delete() }
            return
        }
        val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        firestore.collection("leaderboard").document(uid).set(
            mapOf(
                "name" to character.name,
                "characterClass" to character.characterClass?.name,
                "level" to character.level,
                "weeklyXp" to sessions.filter { it.endedAt >= weekAgo }.sumOf { it.xpEarned },
                "totalXp" to sessions.sumOf { it.xpEarned },
                "streak" to character.streak,
                "battlesWon" to character.battlesWon,
                "updatedAt" to System.currentTimeMillis()
            )
        )
    }

    // ---- Synced-session bookkeeping (per account) ----

    private fun syncedIds(uid: String): Set<Long> =
        prefs.getStringSet("synced_$uid", emptySet())!!.mapNotNull { it.toLongOrNull() }.toSet()

    private fun markSynced(uid: String, sessionId: Long) {
        val current = prefs.getStringSet("synced_$uid", emptySet())!!.toMutableSet()
        current.add(sessionId.toString())
        prefs.edit { putStringSet("synced_$uid", current) }
    }

    // ---- Firestore (de)serialization ----

    private fun characterToMap(c: CharacterEntity): Map<String, Any?> = mapOf(
        "name" to c.name,
        "characterClass" to c.characterClass?.name,
        "level" to c.level,
        "xp" to c.xp,
        "gold" to c.gold,
        "energy" to c.energy,
        "strength" to c.strength,
        "endurance" to c.endurance,
        "agility" to c.agility,
        "willpower" to c.willpower,
        "strProgress" to c.strProgress,
        "endProgress" to c.endProgress,
        "agiProgress" to c.agiProgress,
        "wilProgress" to c.wilProgress,
        "weaponId" to c.weaponId,
        "headId" to c.headId,
        "chestId" to c.chestId,
        "handsId" to c.handsId,
        "legsId" to c.legsId,
        "feetId" to c.feetId,
        "trinketId" to c.trinketId,
        "battlesWon" to c.battlesWon,
        "sessionsCompleted" to c.sessionsCompleted,
        "streak" to c.streak,
        "lastWorkoutDay" to c.lastWorkoutDay,
        "pendingXpBoost" to c.pendingXpBoost,
        "freeStatPoints" to c.freeStatPoints,
        "currentBiome" to c.currentBiome,
        "travelTarget" to c.travelTarget,
        "travelProgress" to c.travelProgress,
        "idleSteps" to c.idleSteps,
        "idleKills" to c.idleKills,
        "idleGold" to c.idleGold,
        "idleXp" to c.idleXp,
        "skinColor" to c.skinColor,
        "hairColor" to c.hairColor,
        "underwearColor" to c.underwearColor,
        "eyeColor" to c.eyeColor,
        "hairStyle" to c.hairStyle,
        "gender" to c.gender,
        "braColor" to c.braColor,
        "race" to c.race,
        "partyId" to c.partyId,
        "guildId" to c.guildId,
        "bodyWeightKg" to c.bodyWeightKg,
        "heightM" to c.heightM,
        "dateOfBirthEpoch" to c.dateOfBirthEpoch,
        "trainingEquipment" to c.trainingEquipment,
        "trainingDaysPerWeek" to c.trainingDaysPerWeek,
        "trainingSplit" to c.trainingSplit,
        "trainingLevel" to c.trainingLevel,
        "effortMethod" to c.effortMethod,
        "plateBarKg" to c.plateBarKg,
        "claimedTrophies" to c.claimedTrophies,
        "bountyDay" to c.bountyDay,
        "waterGlasses" to c.waterGlasses,
        "stretchDone" to c.stretchDone,
        "claimedBounties" to c.claimedBounties,
        "campaignWeek" to c.campaignWeek,
        "claimedCampaigns" to c.claimedCampaigns,
        "encounterClaimedThisTravel" to c.encounterClaimedThisTravel,
        "pendingEncounterStrBoost" to c.pendingEncounterStrBoost,
        "firstWorkoutDone" to c.firstWorkoutDone,
        "fitnessGoal" to c.fitnessGoal,
        "muscleFocus" to c.muscleFocus,
        "cardioPlacement" to c.cardioPlacement,
        "workoutDuration" to c.workoutDuration,
        "bountyBattlesStart" to c.bountyBattlesStart,
        "campaignBattlesStart" to c.campaignBattlesStart
    )

    private fun characterFromMap(m: Map<String, Any?>): CharacterEntity {
        fun int(key: String, default: Int = 0) = (m[key] as? Number)?.toInt() ?: default
        fun long(key: String) = (m[key] as? Number)?.toLong()
        fun longDefault(key: String, default: Long) = (m[key] as? Number)?.toLong() ?: default
        return CharacterEntity(
            id = 1L,
            name = m["name"] as? String ?: "Hero",
            characterClass = (m["characterClass"] as? String)
                ?.let { name -> CharacterClass.entries.find { it.name == name } },
            level = int("level", 1),
            xp = int("xp"),
            gold = int("gold"),
            energy = int("energy"),
            strength = int("strength", 5),
            endurance = int("endurance", 5),
            agility = int("agility", 5),
            willpower = int("willpower", 5),
            strProgress = int("strProgress"),
            endProgress = int("endProgress"),
            agiProgress = int("agiProgress"),
            wilProgress = int("wilProgress"),
            weaponId = long("weaponId"),
            headId = long("headId"),
            chestId = long("chestId"),
            handsId = long("handsId"),
            legsId = long("legsId"),
            feetId = long("feetId"),
            trinketId = long("trinketId"),
            battlesWon = int("battlesWon"),
            sessionsCompleted = int("sessionsCompleted"),
            streak = int("streak"),
            lastWorkoutDay = long("lastWorkoutDay") ?: 0L,
            pendingXpBoost = int("pendingXpBoost"),
            freeStatPoints = int("freeStatPoints"),
            currentBiome = m["currentBiome"] as? String ?: "MEADOWLANDS",
            travelTarget = m["travelTarget"] as? String,
            travelProgress = (m["travelProgress"] as? Number)?.toDouble() ?: 0.0,
            idleSteps = int("idleSteps"),
            idleKills = int("idleKills"),
            idleGold = int("idleGold"),
            idleXp = int("idleXp"),
            skinColor = longDefault("skinColor", 0xFFE3B187),
            hairColor = longDefault("hairColor", 0xFF6B4A32),
            underwearColor = longDefault("underwearColor", 0xFF4E4656),
            eyeColor = longDefault("eyeColor", 0xFF2A2233),
            hairStyle = m["hairStyle"] as? String ?: "short",
            gender = m["gender"] as? String ?: "male",
            braColor = longDefault("braColor", 0xFF4E4656),
            race = m["race"] as? String ?: "HUMAN",
            partyId = m["partyId"] as? String,
            guildId = m["guildId"] as? String,
            bodyWeightKg = (m["bodyWeightKg"] as? Number)?.toDouble(),
            heightM = (m["heightM"] as? Number)?.toDouble(),
            dateOfBirthEpoch = (m["dateOfBirthEpoch"] as? Number)?.toLong(),
            trainingEquipment = m["trainingEquipment"] as? String ?: "",
            trainingDaysPerWeek = int("trainingDaysPerWeek", 3),
            trainingSplit = m["trainingSplit"] as? String ?: "FULL_BODY",
            trainingLevel = m["trainingLevel"] as? String ?: "BEGINNER",
            effortMethod = m["effortMethod"] as? String ?: "OFF",
            plateBarKg = (m["plateBarKg"] as? Number)?.toDouble(),
            claimedTrophies = m["claimedTrophies"] as? String ?: "",
            bountyDay = (m["bountyDay"] as? Number)?.toLong() ?: -1L,
            waterGlasses = int("waterGlasses"),
            stretchDone = m["stretchDone"] as? Boolean ?: false,
            claimedBounties = m["claimedBounties"] as? String ?: "",
            campaignWeek = (m["campaignWeek"] as? Number)?.toLong() ?: -1L,
            claimedCampaigns = m["claimedCampaigns"] as? String ?: "",
            encounterClaimedThisTravel = m["encounterClaimedThisTravel"] as? Boolean ?: false,
            pendingEncounterStrBoost = m["pendingEncounterStrBoost"] as? Boolean ?: false,
            firstWorkoutDone = m["firstWorkoutDone"] as? Boolean ?: false,
            fitnessGoal = m["fitnessGoal"] as? String ?: "BUILD_MUSCLE",
            muscleFocus = m["muscleFocus"] as? String ?: "BALANCED",
            cardioPlacement = m["cardioPlacement"] as? String ?: "NONE",
            workoutDuration = m["workoutDuration"] as? String ?: "STANDARD",
            bountyBattlesStart = int("bountyBattlesStart"),
            campaignBattlesStart = int("campaignBattlesStart")
        )
    }

    private fun sessionToMap(s: SessionEntity, logs: List<SetLogEntity>): Map<String, Any?> = mapOf(
        "name" to s.name,
        "startedAt" to s.startedAt,
        "endedAt" to s.endedAt,
        "xpEarned" to s.xpEarned,
        "goldEarned" to s.goldEarned,
        "energyEarned" to s.energyEarned,
        "setCount" to s.setCount,
        "logs" to logs.map {
            mapOf(
                "exerciseName" to it.exerciseName,
                "category" to it.category.name,
                "weightKg" to it.weightKg,
                "reps" to it.reps,
                "durationMin" to it.durationMin,
                "distanceKm" to it.distanceKm,
                "xp" to it.xp,
                "rir" to it.rir,
                "avgHr" to it.avgHr,
                "maxHr" to it.maxHr,
                "speedKmh" to it.speedKmh,
                "inclinePercent" to it.inclinePercent,
                "cardioProgram" to it.cardioProgram,
                "setType" to it.setType.name
            )

        }
    )

    private fun setLogFromMap(sessionId: Long, m: Map<String, Any?>): SetLogEntity = SetLogEntity(
        sessionId = sessionId,
        exerciseName = m["exerciseName"] as? String ?: "Exercise",
        category = (m["category"] as? String)
            ?.let { name -> ExerciseCategory.entries.find { it.name == name } }
            ?: ExerciseCategory.STRENGTH,
        weightKg = (m["weightKg"] as? Number)?.toDouble() ?: 0.0,
        reps = (m["reps"] as? Number)?.toInt() ?: 0,
        durationMin = (m["durationMin"] as? Number)?.toDouble() ?: 0.0,
        distanceKm = (m["distanceKm"] as? Number)?.toDouble() ?: 0.0,
        xp = (m["xp"] as? Number)?.toInt() ?: 0,
        rir = (m["rir"] as? Number)?.toInt(),
        avgHr = (m["avgHr"] as? Number)?.toInt(),
        maxHr = (m["maxHr"] as? Number)?.toInt(),
        speedKmh = (m["speedKmh"] as? Number)?.toDouble() ?: 0.0,
        inclinePercent = (m["inclinePercent"] as? Number)?.toDouble() ?: 0.0,
        cardioProgram = m["cardioProgram"] as? String ?: "",
        setType = (m["setType"] as? String)
            ?.let { name -> SetType.entries.find { it.name == name } }
            ?: SetType.NORMAL
    )


    private fun workoutToMap(w: WorkoutEntity, exercises: List<WorkoutExerciseEntity>): Map<String, Any?> = mapOf(
        "name" to w.name,
        "createdAt" to w.createdAt,
        "aiGenerated" to w.aiGenerated,
        "exercises" to exercises.map {
            mapOf(
                "exerciseName" to it.exerciseName,
                "category" to it.category.name,
                "targetSets" to it.targetSets,
                "targetReps" to it.targetReps,
                "sortOrder" to it.sortOrder
            )
        }
    )

    private fun workoutExerciseFromMap(workoutId: Long, m: Map<String, Any?>): WorkoutExerciseEntity = WorkoutExerciseEntity(
        workoutId = workoutId,
        exerciseName = m["exerciseName"] as? String ?: "Exercise",
        category = (m["category"] as? String)
            ?.let { name -> ExerciseCategory.entries.find { it.name == name } }
            ?: ExerciseCategory.STRENGTH,
        targetSets = (m["targetSets"] as? Number)?.toInt() ?: 3,
        targetReps = (m["targetReps"] as? Number)?.toInt() ?: 10,
        sortOrder = (m["sortOrder"] as? Number)?.toInt() ?: 0
    )

    private fun classProgressToMap(p: ClassProgressEntity): Map<String, Any?> = mapOf(
        "clazz" to p.clazz.name,
        "level" to p.level,
        "xp" to p.xp,
        "strength" to p.strength,
        "endurance" to p.endurance,
        "agility" to p.agility,
        "willpower" to p.willpower,
        "strProgress" to p.strProgress,
        "endProgress" to p.endProgress,
        "agiProgress" to p.agiProgress,
        "wilProgress" to p.wilProgress,
        "weaponId" to p.weaponId,
        "headId" to p.headId,
        "chestId" to p.chestId,
        "handsId" to p.handsId,
        "legsId" to p.legsId,
        "feetId" to p.feetId,
        "trinketId" to p.trinketId,
        "freeStatPoints" to p.freeStatPoints
    )

    private fun classProgressFromMap(m: Map<String, Any?>): ClassProgressEntity {
        fun int(key: String, default: Int = 0) = (m[key] as? Number)?.toInt() ?: default
        fun long(key: String) = (m[key] as? Number)?.toLong()
        return ClassProgressEntity(
            characterId = 1L,
            clazz = CharacterClass.valueOf(m["clazz"] as String),
            level = int("level", 1),
            xp = int("xp"),
            strength = int("strength", 1),
            endurance = int("endurance", 1),
            agility = int("agility", 1),
            willpower = int("willpower", 1),
            strProgress = int("strProgress"),
            endProgress = int("endProgress"),
            agiProgress = int("agiProgress"),
            wilProgress = int("wilProgress"),
            weaponId = long("weaponId"),
            headId = long("headId"),
            chestId = long("chestId"),
            handsId = long("handsId"),
            legsId = long("legsId"),
            feetId = long("feetId"),
            trinketId = long("trinketId"),
            freeStatPoints = int("freeStatPoints")
        )
    }

    private fun masteryToMap(m: MovementMasteryEntity): Map<String, Any?> = mapOf(
        "canonicalKey" to m.canonicalKey,
        "displayName" to m.displayName,
        "category" to m.category.name,
        "level" to m.level,
        "currentXp" to m.currentXp,
        "lifetimeVolumeKg" to m.lifetimeVolumeKg,
        "lifetimeReps" to m.lifetimeReps,
        "lifetimeDistanceKm" to m.lifetimeDistanceKm,
        "lifetimeDurationSec" to m.lifetimeDurationSec,
        "totalSessionsLogged" to m.totalSessionsLogged,
        "highest1RmKg" to m.highest1RmKg,
        "highestWeightKg" to m.highestWeightKg,
        "bestDistanceKm" to m.bestDistanceKm,
        "bestPaceSecPerKm" to m.bestPaceSecPerKm,
        "lastTrainedEpochMs" to m.lastTrainedEpochMs
    )

    private fun masteryFromMap(m: Map<String, Any?>): MovementMasteryEntity? {
        val canonicalKey = m["canonicalKey"] as? String ?: return null
        val displayName = m["displayName"] as? String ?: return null
        val categoryStr = m["category"] as? String ?: return null
        val category = ExerciseCategory.entries.find { it.name == categoryStr } ?: return null
        fun int(key: String, default: Int = 0) = (m[key] as? Number)?.toInt() ?: default
        fun long(key: String, default: Long = 0L) = (m[key] as? Number)?.toLong() ?: default
        fun double(key: String, default: Double = 0.0) = (m[key] as? Number)?.toDouble() ?: default
        return MovementMasteryEntity(
            characterId = 1L,
            canonicalKey = canonicalKey,
            displayName = displayName,
            category = category,
            level = int("level", 1),
            currentXp = long("currentXp"),
            lifetimeVolumeKg = double("lifetimeVolumeKg"),
            lifetimeReps = int("lifetimeReps"),
            lifetimeDistanceKm = double("lifetimeDistanceKm"),
            lifetimeDurationSec = long("lifetimeDurationSec"),
            totalSessionsLogged = int("totalSessionsLogged"),
            highest1RmKg = double("highest1RmKg"),
            highestWeightKg = double("highestWeightKg"),
            bestDistanceKm = double("bestDistanceKm"),
            bestPaceSecPerKm = long("bestPaceSecPerKm"),
            lastTrainedEpochMs = long("lastTrainedEpochMs")
        )
    }
}
