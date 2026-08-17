package com.fitnessquest.rpg.data

import androidx.room.withTransaction
import com.fitnessquest.rpg.data.db.ActiveExerciseEntity
import com.fitnessquest.rpg.data.db.ActiveSessionEntity
import com.fitnessquest.rpg.data.db.ActiveSessionWithDetails
import com.fitnessquest.rpg.data.db.ActiveSetLogEntity
import com.fitnessquest.rpg.data.db.AppDatabase
import com.fitnessquest.rpg.data.db.BiomeProgressEntity
import com.fitnessquest.rpg.data.db.BodyMetricEntity
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ClassProgressEntity
import com.fitnessquest.rpg.data.db.EquippableSlots
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.GearInstanceEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.data.db.MovementMasteryEntity
import com.fitnessquest.rpg.data.db.PendingSyncEntity
import com.fitnessquest.rpg.data.db.SessionEntity
import com.fitnessquest.rpg.data.db.SessionReceiptCodec
import com.fitnessquest.rpg.data.db.SetLogEntity
import com.fitnessquest.rpg.domain.SetType
import com.fitnessquest.rpg.domain.mastery.MovementMasteryCatalog
import com.fitnessquest.rpg.domain.mastery.MasteryProgression
import com.fitnessquest.rpg.data.db.WorkoutEntity
import com.fitnessquest.rpg.data.db.WorkoutExerciseEntity
import com.fitnessquest.rpg.data.db.isEquippable
import com.fitnessquest.rpg.data.db.isStackable
import com.fitnessquest.rpg.data.ai.GeminiService
import com.fitnessquest.rpg.data.exercises.ExerciseInfoService
import com.fitnessquest.rpg.data.importexport.ImportPersistResult
import com.fitnessquest.rpg.data.importexport.ImportedWorkout
import com.fitnessquest.rpg.data.importexport.ImportedWorkoutKind
import com.fitnessquest.rpg.data.importexport.WorkoutImportService
import com.fitnessquest.rpg.data.sync.SyncService
import com.fitnessquest.rpg.domain.Biome
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.CombatStats
import com.fitnessquest.rpg.domain.Consumables
import com.fitnessquest.rpg.domain.ExerciseCategories
import com.fitnessquest.rpg.domain.GameMath
import com.fitnessquest.rpg.domain.GearSockets
import com.fitnessquest.rpg.domain.ItemCatalog
import com.fitnessquest.rpg.domain.LootChests
import com.fitnessquest.rpg.domain.LootGrant
import com.fitnessquest.rpg.domain.LootResult
import com.fitnessquest.rpg.domain.LootSource
import com.fitnessquest.rpg.domain.LootTables
import com.fitnessquest.rpg.domain.MomentTrigger
import com.fitnessquest.rpg.domain.Monster
import com.fitnessquest.rpg.domain.MonsterCatalog
import com.fitnessquest.rpg.domain.PrKind
import com.fitnessquest.rpg.domain.ProgressionRules
import com.fitnessquest.rpg.domain.Reward
import com.fitnessquest.rpg.domain.RewardBatch
import com.fitnessquest.rpg.domain.RewardSource
import com.fitnessquest.rpg.domain.Runes
import com.fitnessquest.rpg.domain.SessionPr
import com.fitnessquest.rpg.domain.SessionResult
import com.fitnessquest.rpg.domain.toRewards
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.max

/**
 * An owned gear copy with its catalog template resolved for UI / combat.
 */
data class OwnedGear(
    val instance: GearInstanceEntity,
    val catalog: ItemEntity,
) {
    /** Catalog row with instance+rune combat stats and instance id. */
    fun asEquippedItem(runeCatalog: Map<Long, ItemEntity> = emptyMap()): ItemEntity {
        var atk = instance.atk
        var def = instance.def
        var hp = instance.hp
        instance.runeIds().forEach { rid ->
            atk += Runes.atkBonus(rid)
            def += Runes.defBonus(rid)
            hp += Runes.hpBonus(rid)
            runeCatalog[rid]?.let { r ->
                if (Runes.atkBonus(rid) == 0) atk += r.atk
                if (Runes.defBonus(rid) == 0) def += r.def
                if (Runes.hpBonus(rid) == 0) hp += r.hp
            }
        }
        return catalog.copy(id = instance.id, atk = atk, def = def, hp = hp, owned = true)
    }
}

data class GearInventoryRepairResult(
    val beforeCount: Int,
    val afterCount: Int,
    val removedCount: Int,
)

class GameRepository(
    private val db: AppDatabase,
    private val exerciseInfo: ExerciseInfoService? = null,
    private val prefs: UserPrefs? = null,
    private var syncService: SyncService? = null,
) {
    fun setSyncService(sync: SyncService) {
        syncService = sync
    }

    val character: Flow<CharacterEntity> = db.characterDao().observe().filterNotNull()
    val workouts: Flow<List<WorkoutEntity>> = db.workoutDao().observeAll()
    val sessions: Flow<List<SessionEntity>> = db.sessionDao().observeAll()
    val items: Flow<List<ItemEntity>> = db.itemDao().observeAll()
    val gearInstances: Flow<List<GearInstanceEntity>> = db.gearInstanceDao().observeAll()
    val ownedGear: Flow<List<OwnedGear>> = combine(gearInstances, items) { instances, catalog ->
        val byId = catalog.associateBy { it.id }
        instances.mapNotNull { inst -> byId[inst.catalogId]?.let { OwnedGear(inst, it) } }
    }

    val allClassProgress: Flow<List<ClassProgressEntity>> = db.classProgressDao().observeAll()
    val allBiomeProgress: Flow<List<BiomeProgressEntity>> = db.biomeProgressDao().observeAll()

    fun getBiomeRequirement(biome: Biome): Flow<ProgressionRules.BiomeRequirement> =
        allBiomeProgress.map { allProgress ->
            ProgressionRules.nextBiomeRequirement(biome, allProgress)
        }

    suspend fun ensureSeeded() {
        if (db.characterDao().get() == null) {
            db.characterDao().upsert(CharacterEntity())
        }
        ItemCatalog.all.forEach { item ->
            val existing = db.itemDao().get(item.id)
            if (existing == null) {
                db.itemDao().insertAll(listOf(item))
            } else {
                db.itemDao().update(
                    item.copy(
                        owned = existing.owned || (existing.quantity > 0),
                        quantity = existing.quantity,
                    ),
                )
            }
        }
        Biome.entries.forEach { biome ->
            if (db.biomeProgressDao().get(biome.name) == null) {
                db.biomeProgressDao().upsert(BiomeProgressEntity(biome.name))
            }
        }
    }

    suspend fun getCharacter(): CharacterEntity {
        val character = db.characterDao().get() ?: CharacterEntity().also { db.characterDao().upsert(it) }
        val recouped = GameMath.recoupEnergy(character)
        if (recouped != character) {
            db.characterDao().upsert(recouped)
            return recouped
        }
        return character
    }

    suspend fun updateCharacter(character: CharacterEntity) = db.characterDao().upsert(character)

    suspend fun chooseClass(cls: CharacterClass) {
        val character = getCharacter()
        val updated = if (character.characterClass == null) {
            GameMath.applyClassBonus(character, cls).copy(characterClass = cls)
        } else {
            character.copy(characterClass = cls)
        }
        db.characterDao().upsert(updated)
        // Also seed the first class progress
        db.classProgressDao().upsert(
            ClassProgressEntity(
                characterId = character.id,
                clazz = cls,
                level = updated.level,
                xp = updated.xp,
                strength = updated.strength,
                endurance = updated.endurance,
                agility = updated.agility,
                willpower = updated.willpower,
                strProgress = updated.strProgress,
                endProgress = updated.endProgress,
                agiProgress = updated.agiProgress,
                wilProgress = updated.wilProgress,
                freeStatPoints = updated.freeStatPoints,
            ),
        )
    }

    suspend fun switchJob(newClass: CharacterClass) {
        val character = getCharacter()
        val currentClass = character.characterClass ?: return chooseClass(newClass)
        if (currentClass == newClass) return

        db.withTransaction {
            // 1. Save current job state
            val currentProgress = ClassProgressEntity(
                characterId = character.id,
                clazz = currentClass,
                level = character.level,
                xp = character.xp,
                strength = character.strength,
                endurance = character.endurance,
                agility = character.agility,
                willpower = character.willpower,
                strProgress = character.strProgress,
                endProgress = character.endProgress,
                agiProgress = character.agiProgress,
                wilProgress = character.wilProgress,
                weaponId = character.weaponId,
                headId = character.headId,
                chestId = character.chestId,
                handsId = character.handsId,
                legsId = character.legsId,
                feetId = character.feetId,
                trinketId = character.trinketId,
                freeStatPoints = character.freeStatPoints,
            )
            db.classProgressDao().upsert(currentProgress)

            // 2. Load or initialize new job state
            val nextProgress = db.classProgressDao().get(newClass)
            val updatedCharacter = if (nextProgress != null) {
                character.withJobProgress(nextProgress)
            } else {
                // Starting a new job at level 1
                val base = character.copy(
                    characterClass = newClass,
                    level = 1,
                    xp = 0,
                    strength = 1,
                    endurance = 1,
                    agility = 1,
                    willpower = 1,
                    strProgress = 0,
                    endProgress = 0,
                    agiProgress = 0,
                    wilProgress = 0,
                    weaponId = null,
                    headId = null,
                    chestId = null,
                    handsId = null,
                    legsId = null,
                    feetId = null,
                    trinketId = null,
                    freeStatPoints = 16,
                    druidForm = "HUMAN"
                )
                GameMath.applyClassBonus(base, newClass)
            }
            db.characterDao().upsert(updatedCharacter)
        }
    }

    suspend fun updateAppearance(
        skin: Long,
        hair: Long,
        underwear: Long,
        eye: Long,
        style: String,
        gender: String,
        braColor: Long,
        race: String,
    ) {
        db.characterDao().updateAppearance(skin, hair, underwear, eye, style, gender, braColor, race)
    }

    // ---- Workouts ----

    suspend fun saveWorkout(name: String, exercises: List<WorkoutExerciseEntity>, aiGenerated: Boolean = false): Long {
        // Safety deduplication: ensure the same exercise name isn't added multiple times in a single save
        // unless they have different target weights (e.g. drop sets).
        val distinctExercises = exercises
            .distinctBy { "${it.exerciseName}|${it.targetSets}|${it.targetReps}|${it.targetWeightKg}" }

        return db.workoutDao().saveWorkout(
            WorkoutEntity(name = name, aiGenerated = aiGenerated),
            distinctExercises.map { e ->
                e.copy(category = ExerciseCategories.resolveStored(e.exerciseName, e.category))
            }
        )
    }

    suspend fun updateWorkout(id: Long, name: String, exercises: List<WorkoutExerciseEntity>) {
        val existing = db.workoutDao().get(id) ?: return
        val distinctExercises = exercises
            .distinctBy { "${it.exerciseName}|${it.targetSets}|${it.targetReps}|${it.targetWeightKg}" }

        db.workoutDao().replaceWorkout(
            existing.copy(name = name),
            distinctExercises.map { e ->
                e.copy(category = ExerciseCategories.resolveStored(e.exerciseName, e.category))
            }
        )
    }

    suspend fun deleteWorkout(id: Long) = db.workoutDao().deleteWorkoutFully(id)

    suspend fun deleteSession(sessionId: Long, userId: String? = null) {
        val session = db.sessionDao().getSession(sessionId) ?: return
        val logs = db.sessionDao().setLogsFor(sessionId)

        db.withTransaction {
            val char = getCharacter()
            // 1. Revert basic stats (XP, gold, energy)
            val revertedChar = GameMath.revertXp(char, session.xpEarned)
                .copy(
                    gold = max(0, char.gold - session.goldEarned),
                    energy = max(0, char.energy - session.energyEarned),
                    sessionsCompleted = max(0, char.sessionsCompleted - 1)
                )

            // 2. Revert per-stat progress
            var strP = revertedChar.strProgress
            var endP = revertedChar.endProgress
            var agiP = revertedChar.agiProgress
            var wilP = revertedChar.wilProgress

            logs.forEach { log ->
                when (log.category) {
                    ExerciseCategory.STRENGTH -> strP -= log.xp
                    ExerciseCategory.CARDIO -> endP -= log.xp
                    ExerciseCategory.BODYWEIGHT -> agiP -= log.xp
                    ExerciseCategory.FLEXIBILITY -> wilP -= log.xp
                }
            }

            db.characterDao().upsert(
                revertedChar.copy(
                    strProgress = max(0, strP),
                    endProgress = max(0, endP),
                    agiProgress = max(0, agiP),
                    wilProgress = max(0, wilP)
                )
            )

            db.sessionDao().deleteSessionFully(sessionId)
        }

        // 3. Cloud cleanup
        if (userId != null) {
            syncService?.deleteCloudSession(userId, sessionId)
        }
    }

    suspend fun getWorkout(id: Long): WorkoutEntity? = db.workoutDao().get(id)

    suspend fun importExternalWorkouts(
        importedWorkouts: List<ImportedWorkout>,
        gemini: GeminiService? = null,
        renameTemplates: Boolean = false
    ): ImportPersistResult {
        val existingTemplateNames = db.workoutDao().getAllWorkouts()
            .asSequence()
            .map { it.name.trim().lowercase() }
            .toMutableSet()
        val existingSessions = db.sessionDao().getAllSessions()
            .asSequence()
            .map { sessionImportKey(it.name, it.startedAt, it.endedAt, it.setCount) }
            .toMutableSet()
        var templatesAdded = 0
        var sessionsAdded = 0

        importedWorkouts.forEach { imported ->
            val cleanTitle = imported.title.trim().ifBlank { "Imported Workout" }
            if (imported.kind == ImportedWorkoutKind.HISTORY) {
                val logs = imported.logs.ifEmpty {
                    imported.exercises.flatMap { ex ->
                        List(ex.sets.coerceAtLeast(1)) {
                            SetLogEntity(
                                sessionId = 0,
                                exerciseName = ex.name,
                                category = ExerciseCategories.resolveStored(ex.name, ex.category),
                                weightKg = ex.weightKg.coerceAtLeast(0.0),
                                reps = ex.reps.coerceAtLeast(0)
                            )
                        }
                    }
                }.map { log ->
                    log.copy(
                        sessionId = 0,
                        category = ExerciseCategories.resolveStored(log.exerciseName, log.category),
                        xp = if (log.xp > 0) log.xp else GameMath.xpForSet(log)
                    )
                }
                if (logs.isEmpty()) return@forEach

                val session = WorkoutImportService.toSessionEntity(imported.copy(title = cleanTitle, logs = logs))
                val token = session.completionToken
                if (!token.isNullOrBlank()) {
                    val existingByToken = db.activeSessionDao().getSessionByCompletionToken(token)
                    if (existingByToken != null) return@forEach
                }
                val importKey = sessionImportKey(session.name, session.startedAt, session.endedAt, session.setCount)
                if (importKey in existingSessions) return@forEach

                val sessionId = db.sessionDao().insertSession(session)
                db.sessionDao().insertSetLogs(logs.map { it.copy(id = 0, sessionId = sessionId) })
                existingSessions.add(importKey)
                sessionsAdded++
            } else {
                val immersiveTitle = if (renameTemplates) {
                    gemini?.renameImportedRoutine(cleanTitle, imported.exercises.map { it.name })
                        ?.getOrNull()
                        ?: com.fitnessquest.rpg.data.ai.LocalAiEngine.fantasyRoutineName(cleanTitle, imported.exercises.map { it.name })
                } else {
                    cleanTitle
                }
                var targetTitle = immersiveTitle.trim().ifBlank { cleanTitle }
                var counter = 1
                while (targetTitle.lowercase() in existingTemplateNames) {
                    targetTitle = "$immersiveTitle ($counter)"
                    counter++
                }
                existingTemplateNames.add(targetTitle.lowercase())

                saveWorkout(
                    name = targetTitle,
                    exercises = imported.exercises.mapIndexed { idx, ex -> ex.toEntity(sortOrder = idx) }
                )
                templatesAdded++
            }
        }

        return ImportPersistResult(templatesAdded = templatesAdded, sessionsAdded = sessionsAdded)
    }

    @Suppress("unused")
    suspend fun repairImportedHistoryTemplates(): Int {
        val candidates = db.workoutDao().getAllWorkouts().mapNotNull { workout ->
            importedHistoryTemplateDateMillis(workout.name)?.let { endedAt -> workout to endedAt }
        }
        var repaired = 0

        candidates.forEach { (workout, endedAt) ->
            val exercises = db.workoutDao().exercisesFor(workout.id)
            if (exercises.isEmpty()) return@forEach
            val logs = exercises.asSequence().flatMap { exercise ->
                List(exercise.targetSets.coerceAtLeast(1)) {
                    SetLogEntity(
                        sessionId = 0,
                        exerciseName = exercise.exerciseName,
                        category = ExerciseCategories.resolveStored(exercise.exerciseName, exercise.category),
                        reps = exercise.targetReps.coerceAtLeast(0)
                    )
                }
            }.map { log -> log.copy(xp = GameMath.xpForSet(log)) }.toList()
            val session = SessionEntity(
                name = workout.name,
                startedAt = endedAt,
                endedAt = endedAt,
                xpEarned = logs.sumOf { it.xp },
                goldEarned = 0,
                energyEarned = 0,
                setCount = logs.size
            )
            val existing = db.sessionDao().getAllSessions()
                .any { sessionImportKey(it.name, it.startedAt, it.endedAt, it.setCount) == sessionImportKey(session.name, session.startedAt, session.endedAt, session.setCount) }
            if (!existing) {
                val sessionId = db.sessionDao().insertSession(session)
                db.sessionDao().insertSetLogs(logs.map { it.copy(sessionId = sessionId) })
            }
            db.workoutDao().deleteWorkoutFully(workout.id)
            repaired++
        }

        return repaired
    }

    suspend fun deleteBotchedHevyHistoryTemplates(historyImports: List<ImportedWorkout>): Int {
        val historyTitles = historyImports
            .map { it.title.trim().lowercase() }
            .toSet()
        if (historyTitles.isEmpty()) return 0

        val candidates = db.workoutDao().getAllWorkouts()
            .filter { workout ->
                val normalized = workout.name.trim().lowercase()
                (normalized in historyTitles) || (importedHistoryTemplateDateMillis(workout.name) != null)
            }

        candidates.forEach { workout ->
            db.workoutDao().deleteWorkoutFully(workout.id)
        }
        return candidates.size
    }

    /**
     * Identifies and deletes fragmented routine templates created by past CSV imports
     * where single exercises were created as individual workouts.
     */
    suspend fun cleanUpFragmentedSingleExerciseTemplates(): Int {
        val workouts = db.workoutDao().getAllWorkouts()
        var deletedCount = 0

        for (workout in workouts) {
            val exercises = db.workoutDao().exercisesFor(workout.id)
            if (exercises.size == 1) {
                val singleExName = exercises.first().exerciseName.trim().lowercase()
                val workoutName = workout.name.trim().lowercase()

                val isFragmented = (workoutName == singleExName) ||
                        workoutName.contains("imported workout") ||
                        (importedHistoryTemplateDateMillis(workout.name) != null)

                if (isFragmented) {
                    db.workoutDao().deleteWorkoutFully(workout.id)
                    deletedCount++
                }
            }
        }
        return deletedCount
    }

    suspend fun exercisesFor(workoutId: Long): List<WorkoutExerciseEntity> =
        db.workoutDao().exercisesFor(workoutId).map { e ->
            val fixed = ExerciseCategories.resolveStored(e.exerciseName, e.category)
            if (fixed == e.category) e else e.copy(category = fixed)
        }

    suspend fun workoutShareText(workoutId: Long): String? {
        val workout = getWorkout(workoutId) ?: return null
        val exercises = exercisesFor(workoutId)
        if (exercises.isEmpty()) return null
        return buildString {
            appendLine(workout.name)
            if (workout.aiGenerated) appendLine("\u2728 AI-forged")
            appendLine()
            exercises.forEachIndexed { i, e ->
                val unit = when (e.category) {
                    ExerciseCategory.CARDIO, ExerciseCategory.FLEXIBILITY -> "min"
                    else -> "reps"
                }
                appendLine("${i + 1}. ${e.exerciseName}")
                appendLine("   ${e.category.label} \u00B7 ${e.targetSets} \u00D7 ${e.targetReps} $unit")
            }
            appendLine()
            append("Shared from FitQuest")
        }
    }

    // ---- Sessions ----

    suspend fun isPersonalRecord(exerciseName: String, weightKg: Double): Boolean {
        if (weightKg <= 0.0) return false
        val previous = db.sessionDao().maxWeightFor(exerciseName)
        return (previous > 0.0) && (weightKg > previous)
    }

    // ---- Active Sessions (Room Source of Truth) ----

    val activeSession: Flow<ActiveSessionWithDetails?> = db.activeSessionDao().observeActiveSessionWithDetails()

    suspend fun getActiveSessionWithDetails(): ActiveSessionWithDetails? =
        db.activeSessionDao().getActiveSessionWithDetails()

    suspend fun startActiveSession(
        title: String,
        workoutId: Long?,
        exercises: List<com.fitnessquest.rpg.ui.screens.SessionExercise>
    ): ActiveSessionWithDetails {
        return db.withTransaction {
            // Explicitly wipe any leftover draft session data to prevent exercise duplication
            db.activeSessionDao().deleteActiveSetLogs(1L)
            db.activeSessionDao().deleteActiveExercises(1L)
            db.activeSessionDao().deleteActiveSession(1L)

            val token = java.util.UUID.randomUUID().toString()
            val session = ActiveSessionEntity(
                id = 1L,
                title = title,
                workoutId = workoutId,
                startedAt = System.currentTimeMillis(),
                status = "ACTIVE",
                completionToken = token
            )
            db.activeSessionDao().upsertActiveSession(session)

            val exerciseEntities = exercises.mapIndexed { idx, ex ->
                ActiveExerciseEntity(
                    activeSessionId = 1L,
                    exerciseName = ex.name,
                    category = ex.category,
                    targetSets = ex.targetSets,
                    targetReps = ex.targetReps,
                    targetWeightKg = ex.targetWeightKg,
                    trackingType = ex.trackingType.name,
                    sortOrder = idx
                )
            }
            db.activeSessionDao().insertActiveExercises(exerciseEntities)

            db.activeSessionDao().getActiveSessionWithDetails()!!
        }
    }

    suspend fun logActiveSet(
        exerciseId: Long,
        exerciseName: String,
        category: ExerciseCategory,
        weightKg: Double,
        reps: Int,
        durationMin: Double,
        distanceKm: Double,
        xp: Int,
        rir: Int?,
        avgHr: Int?,
        maxHr: Int?,
        speedKmh: Double,
        inclinePercent: Double,
        cardioProgram: String,
        setType: SetType,
        heatStreak: Int,
        restDurationSec: Int
    ) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            val current = db.activeSessionDao().getActiveSession() ?: return@withTransaction
            db.activeSessionDao().upsertActiveSession(
                current.copy(
                    restEndsAt = if (restDurationSec > 0) now + restDurationSec * 1000L else null,
                    restDurationSec = restDurationSec,
                    heatStreak = heatStreak,
                    lastLogAt = now
                )
            )
            db.activeSessionDao().insertActiveSetLog(
                ActiveSetLogEntity(
                    activeSessionId = 1L,
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    category = category,
                    weightKg = weightKg,
                    reps = reps,
                    durationMin = durationMin,
                    distanceKm = distanceKm,
                    xp = xp,
                    rir = rir,
                    avgHr = avgHr,
                    maxHr = maxHr,
                    speedKmh = speedKmh,
                    inclinePercent = inclinePercent,
                    cardioProgram = cardioProgram,
                    setType = setType,
                    loggedAt = now
                )
            )

            // Task 1.1: Auto-advance to next exercise if target sets reached
            val exercises = db.activeSessionDao().getActiveExercises()
            val currentEx = exercises.find { it.id == exerciseId }
            if (currentEx != null) {
                val details = db.activeSessionDao().getActiveSessionWithDetails()
                val loggedCount = details?.exercises?.find { it.exercise.id == exerciseId }?.sets?.size ?: 0
                if (loggedCount >= currentEx.targetSets) {
                    val currentIndex = current.currentExerciseIndex
                    // Only advance if we are logging the current (or earlier) exercise
                    val exerciseOrderIndex = exercises.indexOf(currentEx)
                    if (exerciseOrderIndex >= currentIndex && currentIndex + 1 < exercises.size) {
                        db.activeSessionDao().upsertActiveSession(current.copy(currentExerciseIndex = currentIndex + 1))
                    }
                }
            }
        }
    }

    suspend fun removeLastActiveSet(exerciseId: Long) {
        db.activeSessionDao().deleteLastSetLogForExercise(exerciseId)
    }

    suspend fun addActiveExercise(name: String, category: ExerciseCategory, trackingType: String) {
        db.withTransaction {
            val existing = db.activeSessionDao().getActiveExercises()
            val nextSort = if (existing.isEmpty()) 0 else existing.maxOf { it.sortOrder } + 1
            db.activeSessionDao().insertActiveExercise(
                ActiveExerciseEntity(
                    activeSessionId = 1L,
                    exerciseName = name,
                    category = category,
                    trackingType = trackingType,
                    sortOrder = nextSort
                )
            )
        }
    }

    suspend fun removeActiveExercise(exerciseId: Long) {
        db.activeSessionDao().deleteActiveExercise(exerciseId)
    }

    suspend fun swapActiveExercise(exerciseId: Long, newName: String, newCategory: ExerciseCategory, trackingType: String, permanentWorkoutId: Long? = null) {
        db.withTransaction {
            val current = db.activeSessionDao().getActiveExercises().find { it.id == exerciseId } ?: return@withTransaction
            db.activeSessionDao().updateActiveExercise(
                current.copy(
                    exerciseName = newName,
                    category = newCategory,
                    trackingType = trackingType
                )
            )
            
            if (permanentWorkoutId != null && permanentWorkoutId > 0) {
                // Find matching exercise in the template and update it
                val templateExercises = db.workoutDao().exercisesFor(permanentWorkoutId)
                // We try to match by name or position. Matching by name is risky if multiple, so we'll try to find a close match.
                // In ActiveSession, we don't have the original template exercise ID easily linked.
                // However, we can use the position (sortOrder).
                val toUpdate = templateExercises.find { it.exerciseName == current.exerciseName && it.sortOrder == current.sortOrder }
                    ?: templateExercises.find { it.sortOrder == current.sortOrder }
                
                toUpdate?.let {
                    db.workoutDao().updateExercise(it.copy(exerciseName = newName, category = newCategory))
                }
            }
        }
    }

    suspend fun reorderActiveExercises(orderedExerciseIds: List<Long>) {
        db.withTransaction {
            val current = db.activeSessionDao().getActiveExercises().associateBy { it.id }
            orderedExerciseIds.forEachIndexed { idx, id ->
                current[id]?.let { ex ->
                    db.activeSessionDao().updateActiveExercise(ex.copy(sortOrder = idx))
                }
            }
        }
    }

    suspend fun updateActiveRestTimer(restEndsAt: Long?, restDurationSec: Int) {
        val current = db.activeSessionDao().getActiveSession() ?: return
        db.activeSessionDao().upsertActiveSession(current.copy(restEndsAt = restEndsAt, restDurationSec = restDurationSec))
    }

    suspend fun updateActiveSessionStartTime(startedAt: Long) {
        val current = db.activeSessionDao().getActiveSession() ?: return
        db.activeSessionDao().upsertActiveSession(current.copy(startedAt = startedAt))
    }

    @Suppress("unused")
    suspend fun updateActivePauseState(pausedAt: Long?, accumulatedPausedMs: Long) {
        val current = db.activeSessionDao().getActiveSession() ?: return
        db.activeSessionDao().upsertActiveSession(current.copy(pausedAt = pausedAt, accumulatedPausedMs = accumulatedPausedMs))
    }

    @Suppress("unused")
    suspend fun updateActiveAmbushOffer(offered: Boolean, xpMult: Float) {
        val current = db.activeSessionDao().getActiveSession() ?: return
        db.activeSessionDao().upsertActiveSession(
            current.copy(
                ambushOfferedThisSession = offered,
                ambushXpMult = xpMult
            )
        )
    }

    @Suppress("unused")
    suspend fun updateActiveMomentSpoilsUsed(count: Int) {
        val current = db.activeSessionDao().getActiveSession() ?: return
        db.activeSessionDao().upsertActiveSession(current.copy(momentSpoilsUsed = count))
    }

    suspend fun discardActiveSession() {
        db.withTransaction {
            db.activeSessionDao().deleteActiveSetLogs(1L)
            db.activeSessionDao().deleteActiveExercises(1L)
            db.activeSessionDao().deleteActiveSession(1L)
        }
    }

    suspend fun completeSession(
        name: String,
        startedAt: Long,
        logs: List<SetLogEntity>,
        strengthXpMultiplier: Float = 1f,
        completionToken: String? = null,
        userId: String? = null,
        caloriesKcal: Int? = null,
        avgHr: Int? = null,
        maxHr: Int? = null,
        steps: Long? = null,
        distanceMeters: Double? = null,
        activeDurationMs: Long? = null
    ): SessionResult {
        val token = completionToken ?: "session_${startedAt}_${logs.size}"

        // Idempotency Check: return existing result if completion already succeeded
        val existingSession = db.activeSessionDao().getSessionByCompletionToken(token)
        if (existingSession != null) {
            return restoreSessionResult(existingSession, getCharacter())
        }

        val result = db.withTransaction {
            val innerExisting = db.activeSessionDao().getSessionByCompletionToken(token)
            if (innerExisting != null) {
                return@withTransaction restoreSessionResult(innerExisting, getCharacter())
            }

            val endedAt = System.currentTimeMillis()
            val durationMs = endedAt - startedAt
            val currentActive = db.activeSessionDao().getActiveSession()
            // Preserve the original set XP (calculated with multipliers during the session) if present
            var withXp = logs.map { if (it.xp > 0) it else it.copy(xp = GameMath.xpForSet(it)) }
            if ((strengthXpMultiplier != 1f) && (strengthXpMultiplier > 0f)) {
                withXp = withXp.map { log ->
                    if (log.category == ExerciseCategory.STRENGTH) {
                        log.copy(xp = (log.xp * strengthXpMultiplier).toInt().coerceAtLeast(1))
                    } else {
                        log
                    }
                }
            }
            val character = getCharacter()

            val muscles = mutableSetOf<String>()
            withXp.forEach { log ->
                exerciseInfo?.find(log.exerciseName)?.let { info ->
                    muscles.addAll(info.primaryMuscles)
                    muscles.addAll(info.secondaryMuscles)
                }
            }

            val prs = mutableListOf<SessionPr>()
            withXp
                .groupBy { it.exerciseName }
                .forEach { (exercise, currentSets) ->
                    // Task 1.2: Use fuzzy matching for PR history lookup
                    val history = getFuzzyMatchedLogs(exercise)
                    if (history.isEmpty()) return@forEach

                    if (currentSets.any { it.weightKg > 0.0 }) {
                        val currentMaxWeight = currentSets.maxOf { it.weightKg }
                        val currentMaxVolume = currentSets.sumOf { it.weightKg * it.reps }
                        val currentMax1RM = currentSets.maxOf { GameMath.calculate1RM(it.weightKg, it.reps) }

                        val histMaxWeight = history.maxOfOrNull { it.weightKg } ?: 0.0
                        val histMax1RM = history.maxOfOrNull { GameMath.calculate1RM(it.weightKg, it.reps) } ?: 0.0
                        val histMaxVolume = history.groupBy { it.sessionId }.asSequence()
                            .map { (_, s) -> s.sumOf { it.weightKg * it.reps } }
                            .maxOfOrNull { it } ?: 0.0

                        if (currentMaxWeight > histMaxWeight && histMaxWeight > 0.0) {
                            val bestSet = currentSets.maxBy { it.weightKg }
                            prs += SessionPr(exercise, PrKind.WEIGHT, currentMaxWeight, bestSet.reps)
                        }
                        if (currentMaxVolume > histMaxVolume && histMaxVolume > 0.0) {
                            prs += SessionPr(exercise, PrKind.VOLUME, currentMaxVolume)
                        }
                        if (currentMax1RM > histMax1RM && histMax1RM > 0.0) {
                            prs += SessionPr(exercise, PrKind.ONE_RM, currentMax1RM)
                        }
                    }

                    if (currentSets.any { it.distanceKm > 0.0 }) {
                        val currentMaxDist = currentSets.maxOf { it.distanceKm }
                        val histMaxDist = history.maxOfOrNull { it.distanceKm } ?: 0.0
                        if (currentMaxDist > histMaxDist && histMaxDist > 0.0) {
                            prs += SessionPr(exercise, PrKind.DISTANCE, currentMaxDist)
                        }

                        val currentBestPace = currentSets.filter { it.distanceKm > 0.0 && it.durationMin > 0.0 }
                            .minOfOrNull { it.durationMin / it.distanceKm }
                        val histBestPace = history.filter { it.distanceKm > 0.0 && it.durationMin > 0.0 }
                            .minOfOrNull { it.durationMin / it.distanceKm }
                        if (currentBestPace != null && histBestPace != null && currentBestPace < histBestPace) {
                            prs += SessionPr(exercise, PrKind.PACE, currentBestPace)
                        }
                    }

                    if (currentSets.any { it.durationMin > 0.0 }) {
                        val currentMaxTime = currentSets.maxOf { it.durationMin }
                        val histMaxTime = history.maxOfOrNull { it.durationMin } ?: 0.0
                        if (currentMaxTime > histMaxTime && histMaxTime > 0.0) {
                            prs += SessionPr(exercise, PrKind.TIME, currentMaxTime)
                        }
                    }

                    if (currentSets.any { it.weightKg <= 0.0 && it.reps > 0 }) {
                        val currentMaxReps = currentSets.maxOf { it.reps }
                        val histMaxReps = history.filter { it.weightKg <= 0.0 }.maxOfOrNull { it.reps } ?: 0
                        if (histMaxReps in 1..<currentMaxReps) {
                            prs += SessionPr(exercise, PrKind.REPS, currentMaxReps.toDouble())
                        }
                    }

                    if (currentSets.any { it.speedKmh > 0.0 }) {
                        val currentMaxSpeed = currentSets.maxOf { it.speedKmh }
                        val histMaxSpeed = history.maxOfOrNull { it.speedKmh } ?: 0.0
                        if (currentMaxSpeed > histMaxSpeed && histMaxSpeed > 0.0) {
                            prs += SessionPr(exercise, PrKind.SPEED, currentMaxSpeed)
                        }
                    }
                    if (currentSets.any { it.inclinePercent > 0.0 }) {
                        val currentMaxIncline = currentSets.maxOf { it.inclinePercent }
                        val histMaxIncline = history.maxOfOrNull { it.inclinePercent } ?: 0.0
                        if (currentMaxIncline > histMaxIncline && histMaxIncline > 0.0) {
                            prs += SessionPr(exercise, PrKind.INCLINE, currentMaxIncline)
                        }
                    }
                }

            val zone = ZoneId.systemDefault()
            val nowLocal = LocalDate.now(zone)
            val weekStart = nowLocal.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekStartMillis = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
            val weeklyDone = db.sessionDao().countSessionsSince(weekStartMillis) + 1
            val weeklyGoal = prefs?.profile?.value?.daysPerWeek ?: 3

            val bonusXp = character.pendingXpBoost + (prs.count { it.isNew } * GameMath.PR_BONUS_XP)
            val isWellRested = GameMath.isWellRested(character.lastWorkoutDay)
            
            var res = GameMath.applySession(
                character = character,
                logs = withXp,
                durationMs = durationMs,
                musclesWorked = muscles,
                weeklyWorkoutsDone = weeklyDone,
                weeklyWorkoutsGoal = weeklyGoal,
                bonusXp = bonusXp,
                isWellRested = isWellRested,
                caloriesKcal = caloriesKcal,
                avgHr = avgHr,
                maxHr = maxHr,
                steps = steps,
                distanceMeters = distanceMeters,
                activeDurationMs = activeDurationMs
            )
            
            res = res.copy(xpBoostApplied = character.pendingXpBoost, prs = prs)

            val today = LocalDate.now().toEpochDay()
            val lastDay = character.lastWorkoutDay
            var streak = character.streak
            var streakSaved = false
            when {
                lastDay == 0L -> streak = 1
                today == lastDay -> streak = streak.coerceAtLeast(1)
                today == (lastDay + 1) -> streak += 1
                else -> {
                    val missed = (today - lastDay - 1).toInt()
                    val freeze = db.itemDao().get(Consumables.STREAK_FREEZE)
                    if ((freeze != null) && (freeze.quantity >= missed)) {
                        db.itemDao().update(freeze.copy(quantity = freeze.quantity - missed))
                        streak += 1
                        streakSaved = true
                    } else {
                        streak = 1
                    }
                }
            }

            var updated = res.updatedCharacter.copy(
                streak = streak,
                lastWorkoutDay = today,
                pendingXpBoost = 0
            )
            res = res.copy(streak = streak, streakSaved = streakSaved)

            val cardioKm = withXp.asSequence()
                .filter { it.category == ExerciseCategory.CARDIO }
                .sumOf { if (it.distanceKm > 0.0) it.distanceKm else it.durationMin / 10.0 }
            if ((cardioKm > 0.0) && (updated.travelTarget != null)) {
                val target = Biome.fromName(updated.travelTarget)
                val currentBiome = Biome.fromName(updated.currentBiome)
                val allProgress = db.biomeProgressDao().getAll()
                
                if (ProgressionRules.canAdvanceFromBiome(currentBiome, allProgress)) {
                    val progress = updated.travelProgress + cardioKm
                    updated = if (progress >= target.travelKm) {
                        res = res.copy(arrivedAt = target.label)
                        updated.copy(currentBiome = target.name, travelTarget = null, travelProgress = 0.0)
                    } else {
                        updated.copy(travelProgress = progress)
                    }
                    res = res.copy(travelKm = cardioKm)
                } else {
                    // Boss not defeated; progress accumulates but cap at just before arrival
                    val progress = (updated.travelProgress + cardioKm).coerceAtMost(target.travelKm - 0.1)
                    updated = updated.copy(travelProgress = progress)
                    res = res.copy(travelKm = cardioKm)
                }
            }

            val workoutTier = lootTierFor(
                source = LootSource.WORKOUT,
                character = updated,
                contentTier = (updated.level / 3) + 1
            )
            val loot = LootTables.rollWorkoutLoot(
                level = updated.level,
                setCount = withXp.size,
                prCount = prs.count { it.isNew },
                gearPool = eligibleGearTemplates(updated, workoutTier),
                stackPool = stackTemplates(),
                maxTier = workoutTier
            )
            updated = applyLootToCharacter(updated, loot)
            
            val allRewards = mutableListOf<Reward>()
            allRewards.add(Reward.Xp(res.xp))
            if (res.gold > 0) allRewards.add(Reward.Gold(res.gold))
            if (res.energy > 0) allRewards.add(Reward.Energy(res.energy))
            if (res.levelsGained > 0) allRewards.add(Reward.LevelUp(updated.level))
            prs.forEach { allRewards.add(Reward.NewPr(it)) }
            if (res.arrivedAt != null) {
                allRewards.add(Reward.BiomeUnlocked(updated.currentBiome, res.arrivedAt))
            }
            allRewards.addAll(loot.toRewards())

            res = res.copy(
                updatedCharacter = updated,
                lootLabels = loot.labels(),
                gold = res.gold + loot.goldBonus,
                rewardBatch = RewardBatch(RewardSource.WORKOUT, allRewards)
            )
            
            // Task 1.1: Explicitly persist the updated hero character
            db.characterDao().upsert(updated)

            val receipt = serializeSessionResult(res)
            val sessionId = db.sessionDao().insertSession(
                SessionEntity(
                    name = name,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    xpEarned = res.xp,
                    goldEarned = res.gold,
                    energyEarned = res.energy,
                    setCount = withXp.size,
                    completionToken = token,
                    completionReceiptJson = receipt
                )
            )
            // Task 1.1: Reset ID to 0 to avoid primary key conflicts with active session table
            db.sessionDao().insertSetLogs(withXp.map { it.copy(id = 0, sessionId = sessionId) })

            // Task 1.1: Auto-advance workout template if based on one
            currentActive?.workoutId?.let { workoutId ->
                val templateExercises = db.workoutDao().exercisesFor(workoutId)
                withXp.groupBy { it.exerciseName }.forEach { (name, sessionSets) ->
                    val template = templateExercises.find { it.exerciseName == name }
                    if (template != null) {
                        val sessionMaxReps = sessionSets.maxOfOrNull { it.reps } ?: 0
                        val sessionSetsCount = sessionSets.size
                        
                        var newTargetSets = template.targetSets
                        var newTargetReps = template.targetReps
                        var changed = false
                        
                        if (sessionSetsCount > template.targetSets) {
                            newTargetSets = sessionSetsCount
                            changed = true
                        }
                        if (sessionSets.all { it.reps > template.targetReps } && sessionMaxReps > 0) {
                            newTargetReps = sessionSets.minOf { it.reps }
                            changed = true
                        }
                        
                        if (changed) {
                            db.workoutDao().updateExercise(template.copy(
                                targetSets = newTargetSets,
                                targetReps = newTargetReps
                            ))
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // Movement Mastery Progression Award
            // -------------------------------------------------------------
            if (withXp.isNotEmpty()) {
                val groupedByMovement = withXp.groupBy {
                    MovementMasteryCatalog.resolve(it.exerciseName, it.category)
                }

                val userWeight = character.bodyWeightKg

                for ((movement, sets) in groupedByMovement) {
                    val rawXp = sets.sumOf { set ->
                        MasteryProgression.calculateSetXp(set, userWeight)
                    }
                    val maxXpCap = if (movement.category == ExerciseCategory.CARDIO) {
                        MasteryProgression.MAX_CARDIO_XP_PER_SESSION
                    } else {
                        MasteryProgression.MAX_STRENGTH_XP_PER_SESSION
                    }
                    val sessionMasteryXp = rawXp.coerceIn(0, maxXpCap).toLong()

                    if (sessionMasteryXp > 0L) {
                        val current = db.movementMasteryDao().getByCanonicalKey(movement.canonicalKey, character.id)
                            ?: MovementMasteryEntity(
                                characterId = character.id,
                                canonicalKey = movement.canonicalKey,
                                displayName = movement.displayName,
                                category = movement.category
                            )

                        val newTotalXp = current.currentXp + sessionMasteryXp
                        val newLevel = MasteryProgression.levelForXp(newTotalXp)
                        val sessionVolume = sets.filter { it.weightKg > 0.0 }.sumOf { it.weightKg * it.reps }
                        val sessionReps = sets.sumOf { it.reps }
                        val sessionDist = sets.sumOf { it.distanceKm }
                        val sessionDuration = sets.sumOf { (it.durationMin * 60.0).toLong() }

                        val sessionMax1Rm = sets.filter { it.weightKg > 0.0 }
                            .maxOfOrNull { GameMath.calculate1RM(it.weightKg, it.reps) } ?: 0.0
                        val sessionMaxWeight = sets.maxOfOrNull { it.weightKg } ?: 0.0
                        val sessionMaxDist = sets.maxOfOrNull { it.distanceKm } ?: 0.0

                        val sessionBestPaceSec = sets.filter { it.distanceKm > 0.0 && it.durationMin > 0.0 }
                            .minOfOrNull { ((it.durationMin * 60.0) / it.distanceKm).toLong() } ?: 0L

                        val bestPace = if (sessionBestPaceSec > 0L) {
                            if (current.bestPaceSecPerKm == 0L) sessionBestPaceSec
                            else minOf(current.bestPaceSecPerKm, sessionBestPaceSec)
                        } else {
                            current.bestPaceSecPerKm
                        }

                        val updatedMastery = current.copy(
                            level = newLevel,
                            currentXp = newTotalXp,
                            lifetimeVolumeKg = current.lifetimeVolumeKg + sessionVolume,
                            lifetimeReps = current.lifetimeReps + sessionReps,
                            lifetimeDistanceKm = current.lifetimeDistanceKm + sessionDist,
                            lifetimeDurationSec = current.lifetimeDurationSec + sessionDuration,
                            totalSessionsLogged = current.totalSessionsLogged + 1,
                            highest1RmKg = maxOf(current.highest1RmKg, sessionMax1Rm),
                            highestWeightKg = maxOf(current.highestWeightKg, sessionMaxWeight),
                            bestDistanceKm = maxOf(current.bestDistanceKm, sessionMaxDist),
                            bestPaceSecPerKm = bestPace,
                            lastTrainedEpochMs = endedAt
                        )
                        db.movementMasteryDao().upsert(updatedMastery)
                    }
                }
            }

            // Durable outbox events for social sync (Party & Guild)
            // Re-read character record to ensure freshest IDs (Issue 1)
            val freshCharacter = db.characterDao().get() ?: updated
            val partyId = freshCharacter.partyId
            val guildId = freshCharacter.guildId
            val uid = userId?.takeIf { it.isNotBlank() }

            if (!partyId.isNullOrBlank() && uid != null) {
                val partyPayload = "{\"xp\":${res.xp},\"partyId\":\"$partyId\",\"uid\":\"$uid\"}"
                db.activeSessionDao().insertOutboxEvent(
                    PendingSyncEntity(
                        eventId = "$token:PARTY",
                        type = "PARTY_XP",
                        payloadJson = partyPayload
                    )
                )
            }

            if (!guildId.isNullOrBlank() && uid != null) {
                val guildPayload = "{\"xp\":${res.xp},\"guildId\":\"$guildId\",\"uid\":\"$uid\"}"
                db.activeSessionDao().insertOutboxEvent(
                    PendingSyncEntity(
                        eventId = "$token:GUILD",
                        type = "GUILD_XP",
                        payloadJson = guildPayload
                    )
                )
            }

            // Delete active workout draft (explicitly clear exercises and draft sets)
            db.activeSessionDao().deleteActiveSetLogs(1L)
            db.activeSessionDao().deleteActiveExercises(1L)
            db.activeSessionDao().deleteActiveSession(1L)

            res
        }

        return result
    }

    private fun restoreSessionResult(existingSession: SessionEntity, character: CharacterEntity): SessionResult {
        val jsonStr = existingSession.completionReceiptJson
        if (!jsonStr.isNullOrEmpty()) {
            try {
                return SessionReceiptCodec.deserialize(jsonStr, character)
            } catch (_: Exception) {
                // Fallback to basic reconstruction below
            }
        }
        return SessionResult(
            updatedCharacter = character,
            xp = existingSession.xpEarned,
            gold = existingSession.goldEarned,
            energy = existingSession.energyEarned,
            levelsGained = 0,
            statGains = com.fitnessquest.rpg.domain.StatGains(),
            travelKm = 0.0,
            arrivedAt = null,
            streak = character.streak,
            streakSaved = false,
            xpBoostApplied = 0,
            prs = emptyList(),
            lootLabels = emptyList(),
            rewardBatch = RewardBatch(RewardSource.WORKOUT, emptyList())
        )
    }

    private fun serializeSessionResult(res: SessionResult): String {
        return SessionReceiptCodec.serialize(res)
    }

    val allSetLogs: Flow<List<SetLogEntity>> = db.sessionDao().observeAllSetLogs()

    suspend fun grantBountyReward(gold: Int = 0, xpBoost: Int = 0, energy: Int = 0, bountyId: String? = null): Boolean {
        return db.withTransaction {
            val c = getCharacter()
            if (bountyId != null) {
                val claimed = c.claimedBounties.split(",").filter { it.isNotBlank() }.toSet()
                if (bountyId in claimed) return@withTransaction false
                
                val newClaimed = (claimed + bountyId).joinToString(",")
                db.characterDao().upsert(
                    c.copy(
                        gold = c.gold + gold,
                        pendingXpBoost = c.pendingXpBoost + xpBoost,
                        energy = (c.energy + energy).coerceAtMost(GameMath.MAX_ENERGY),
                        lastEnergyUpdate = System.currentTimeMillis(),
                        claimedBounties = newClaimed
                    )
                )
            } else {
                db.characterDao().upsert(
                    c.copy(
                        gold = c.gold + gold,
                        pendingXpBoost = c.pendingXpBoost + xpBoost,
                        energy = (c.energy + energy).coerceAtMost(GameMath.MAX_ENERGY),
                        lastEnergyUpdate = System.currentTimeMillis()
                    )
                )
            }
            true
        }
    }

    suspend fun grantCampaignReward(gold: Int, xpBoost: Int, campaignId: String): Boolean {
        return db.withTransaction {
            val c = getCharacter()
            val claimed = c.claimedCampaigns.split(",").filter { it.isNotBlank() }.toSet()
            if (campaignId in claimed) return@withTransaction false
            
            val newClaimed = (claimed + campaignId).joinToString(",")
            db.characterDao().upsert(
                c.copy(
                    gold = c.gold + gold,
                    pendingXpBoost = c.pendingXpBoost + xpBoost,
                    claimedCampaigns = newClaimed
                )
            )
            true
        }
    }

    suspend fun grantTrophyReward(gold: Int, trophyId: String): Boolean {
        return db.withTransaction {
            val c = getCharacter()
            val claimed = c.claimedTrophies.split(",").filter { it.isNotBlank() }.toSet()
            if (trophyId in claimed) return@withTransaction false
            
            val newClaimed = (claimed + trophyId).joinToString(",")
            db.characterDao().upsert(
                c.copy(
                    gold = c.gold + gold,
                    claimedTrophies = newClaimed
                )
            )
            true
        }
    }

    suspend fun grantGuildRaidReward(topContributor: Boolean): RewardBatch {
        val character = getCharacter()
        val tier = lootTierFor(
            source = LootSource.GUILD_RAID,
            character = character,
            contentTier = ((character.level / 4) + 2).coerceIn(2, 5)
        )
        val contents = LootTables.openChest(
            tier = tier,
            gearPool = eligibleGearTemplates(character, tier),
            stackPool = stackTemplates()
        )
        val gold = 80 + (if (topContributor) 40 else 0)
        val xpBoost = 50 + (if (topContributor) 25 else 0)
        val loot = LootResult(
            grants = contents,
            goldBonus = gold,
            xpBoostBonus = xpBoost
        )
        db.characterDao().upsert(applyLootToCharacter(character, loot))
        val rewards = loot.toRewards().toMutableList()
        rewards += if (topContributor) Reward.TitleUnlocked("Raid Champion") else Reward.TitleUnlocked("Guild Raider")
        return RewardBatch(RewardSource.GUILD_RAID, rewards)
    }

    suspend fun exerciseHistory(exerciseName: String): List<Pair<SessionEntity, List<SetLogEntity>>> {
        val logs = db.sessionDao().logsForExercise(exerciseName)
        if (logs.isEmpty()) return emptyList()
        val bySession = logs.groupBy { it.sessionId }
        return db.sessionDao().sessionsByIds(bySession.keys.toList())
            .map { session -> session to bySession.getValue(session.id) }
    }

    /** Returns a condensed summary of recent workouts for AI context. */
    suspend fun recentWorkoutsSummary(limit: Int = 10): String {
        val recentSessions = db.sessionDao().getRecentSessions(limit)
        if (recentSessions.isEmpty()) return "No workout history yet."
        
        return buildString {
            appendLine("Recent workout history:")
            recentSessions.forEach { session ->
                val logs = db.sessionDao().setLogsFor(session.id)
                val exercises = logs.groupBy { it.exerciseName }
                val exSummary = exercises.map { (name, sets) ->
                    "$name (${sets.size} sets)"
                }.joinToString(", ")
                appendLine("- ${session.name}: $exSummary")
            }
        }
    }

    suspend fun getSessionWithLogs(sessionId: Long): Pair<SessionEntity, List<SetLogEntity>>? {
        val session = db.sessionDao().getSession(sessionId) ?: return null
        val logs = db.sessionDao().setLogsFor(sessionId)
        return session to logs
    }



    private val shopMutex = Mutex()

    suspend fun buyItem(itemId: Long): Boolean = shopMutex.withLock {
        db.withTransaction {
            val item = db.itemDao().get(itemId) ?: return@withTransaction false
            val character = getCharacter()
            if (character.gold < item.price) return@withTransaction false
            when {
                item.slot.isStackable() -> {
                    db.itemDao().update(item.copy(owned = true, quantity = item.quantity + 1))
                }
                item.slot.isEquippable() -> {
                    createGearInstance(item)
                }
                else -> return@withTransaction false
            }
            db.characterDao().upsert(character.copy(gold = character.gold - item.price))
            true
        }
    }

    suspend fun createGearInstance(catalog: ItemEntity, originBiome: String? = null): GearInstanceEntity {
        val id = db.gearInstanceDao().insert(
            GearInstanceEntity(
                catalogId = catalog.id,
                atk = catalog.atk,
                def = catalog.def,
                hp = catalog.hp,
                originBiome = originBiome
            )
        )
        return db.gearInstanceDao().get(id)!!
    }

    suspend fun useConsumable(itemId: Long): String? {
        val item = db.itemDao().get(itemId) ?: return null
        if ((item.slot != ItemSlot.CONSUMABLE) || (item.quantity <= 0)) return null
        val character = getCharacter()
        val message = when (itemId) {
            Consumables.XP_ELIXIR_MINOR -> {
                db.characterDao().upsert(character.copy(pendingXpBoost = character.pendingXpBoost + Consumables.XP_MINOR_BONUS))
                "You drink the elixir. Next workout: +${Consumables.XP_MINOR_BONUS} bonus XP!"
            }
            Consumables.XP_ELIXIR_MAJOR -> {
                db.characterDao().upsert(character.copy(pendingXpBoost = character.pendingXpBoost + Consumables.XP_MAJOR_BONUS))
                "You drink the elixir. Next workout: +${Consumables.XP_MAJOR_BONUS} bonus XP!"
            }
            Consumables.HEALTH_POTION -> {
                val gearMap = equippedGear(character)
                val maxHp = GameMath.combatStats(character, gearMap.values.toList()).maxHp
                val curHp = character.currentHp ?: maxHp
                if (curHp >= maxHp) return "Your health is already full!"
                val newHp = (curHp + Consumables.HEALTH_POTION_HEAL).coerceAtMost(maxHp)
                db.itemDao().update(item.copy(quantity = item.quantity - 1))
                db.characterDao().upsert(character.copy(currentHp = newHp))
                "Quaffed Health Potion! Restored 50 HP."
            }
            Consumables.LIFE_POTION -> {
                val gearMap = equippedGear(character)
                val maxHp = GameMath.combatStats(character, gearMap.values.toList()).maxHp
                val curHp = character.currentHp ?: maxHp
                if (curHp > 0) return "Phoenix Elixir is held for battle revival!"
                val newHp = maxHp / 2
                db.itemDao().update(item.copy(quantity = item.quantity - 1))
                db.characterDao().upsert(character.copy(currentHp = newHp))
                "Phoenix Elixir flared! Revived with $newHp HP."
            }
            Consumables.POTION_OF_REBIRTH -> {
                val refunded = (character.strength - 1) + (character.endurance - 1) +
                    (character.agility - 1) + (character.willpower - 1)
                if (refunded <= 0) return null
                db.characterDao().upsert(
                    character.copy(
                        strength = 1, endurance = 1, agility = 1, willpower = 1,
                        freeStatPoints = character.freeStatPoints + refunded
                    )
                )
                db.itemDao().update(item.copy(quantity = item.quantity - 1))
                "Attributes reset to 1! Reclaimed $refunded stat points."
            }
            else -> return null
        }
        db.itemDao().update(item.copy(quantity = item.quantity - 1))
        return message
    }

    suspend fun openLootChest(chestCatalogId: Long): RewardBatch? {
        val chest = db.itemDao().get(chestCatalogId) ?: return null
        if ((chest.slot != ItemSlot.LOOT_CHEST) || (chest.quantity <= 0)) return null
        db.itemDao().update(chest.copy(quantity = chest.quantity - 1))
        val character = getCharacter()
        val tier = lootTierFor(
            source = LootSource.CHEST,
            character = character,
            contentTier = chest.tier
        )
        val contents = LootTables.openChest(
            tier = tier,
            gearPool = eligibleGearTemplates(character, tier),
            stackPool = stackTemplates()
        )
        val flat = LootResult(grants = contents)
        db.characterDao().upsert(applyLootToCharacter(character, flat))
        return RewardBatch(RewardSource.CHEST_OPENING, flat.toRewards())
    }

    suspend fun sellMaterial(itemId: Long, qty: Int = 1): Boolean {
        val item = db.itemDao().get(itemId) ?: return false
        if ((item.slot != ItemSlot.MATERIAL) || (item.quantity < qty)) return false
        val gold = (item.price / 2).coerceAtLeast(1) * qty
        db.itemDao().update(item.copy(quantity = item.quantity - qty))
        val c = getCharacter()
        db.characterDao().upsert(c.copy(gold = c.gold + gold))
        return true
    }

    suspend fun salvageGearInstance(instanceId: Long): RewardBatch? {
        val instance = db.gearInstanceDao().get(instanceId) ?: return null
        val catalog = db.itemDao().get(instance.catalogId) ?: return null
        if (!catalog.slot.isEquippable()) return null
        val materialId = ProgressionRules.primaryMaterialFor(catalog)
        val material = db.itemDao().get(materialId)
            ?: ItemCatalog.all.firstOrNull { it.id == materialId }
            ?: return null
        val quantity = ProgressionRules.salvageMaterialQuantity(catalog.tier, instance.rarity)
        val character = getCharacter()
        db.withTransaction {
            db.gearInstanceDao().delete(instance.id)
            db.itemDao().update(material.copy(owned = true, quantity = material.quantity + quantity))
            db.characterDao().upsert(unequipInstance(character, instance.id))
        }
        val flat = LootResult(grants = listOf(LootGrant.Stack(material, quantity)))
        return RewardBatch(RewardSource.FORGE, flat.toRewards())
    }

    suspend fun grantImportReward(importedCount: Int): RewardBatch? {
        val character = getCharacter()
        val claimed = character.claimedBounties.split(",").filter { it.isNotBlank() }.toSet()
        if ("ARCHIVES_RESTORED" in claimed) return null

        val countFactor = importedCount.coerceIn(1, 5)
        val xpGain = 250 * countFactor
        val goldGain = 150 * countFactor

        val chestCatalog = ItemCatalog.all.find { it.id == LootChests.WOODEN }
        if (chestCatalog != null) {
            val existing = db.itemDao().get(chestCatalog.id)
            if (existing != null) {
                db.itemDao().update(existing.copy(owned = true, quantity = existing.quantity + 1))
            } else {
                db.itemDao().insertAll(listOf(chestCatalog.copy(owned = true, quantity = 1)))
            }
        }

        val newClaimed = (claimed + "ARCHIVES_RESTORED").joinToString(",")
        val nextChar = character.copy(
            xp = character.xp + xpGain,
            gold = character.gold + goldGain,
            claimedBounties = newClaimed
        )
        val updatedChar = GameMath.applyLevelUps(nextChar)
        db.characterDao().upsert(updatedChar)

        val rewardList = mutableListOf(
            Reward.Xp(xpGain),
            Reward.Gold(goldGain)
        )
        chestCatalog?.let { rewardList.add(Reward.Stackable(it, 1)) }
        rewardList.add(Reward.TitleUnlocked("Archivist"))

        return RewardBatch(RewardSource.DATA_IMPORT, rewardList)
    }


    private fun sessionImportKey(name: String, startedAt: Long, endedAt: Long, setCount: Int): String =
        "${name.trim().lowercase()}|$startedAt|$endedAt|$setCount"

    private fun importedHistoryTemplateDateMillis(name: String): Long? {
        val match = Regex("""\((Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) (\d{1,2}), (\d{4})\)\s*$""")
            .find(name)
            ?: return null
        val month = when (match.groupValues[1]) {
            "Jan" -> Month.JANUARY
            "Feb" -> Month.FEBRUARY
            "Mar" -> Month.MARCH
            "Apr" -> Month.APRIL
            "May" -> Month.MAY
            "Jun" -> Month.JUNE
            "Jul" -> Month.JULY
            "Aug" -> Month.AUGUST
            "Sep" -> Month.SEPTEMBER
            "Oct" -> Month.OCTOBER
            "Nov" -> Month.NOVEMBER
            "Dec" -> Month.DECEMBER
            else -> return null
        }
        val day = match.groupValues[2].toIntOrNull() ?: return null
        val year = match.groupValues[3].toIntOrNull() ?: return null
        return runCatching {
            LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull()
    }

    suspend fun upgradeGearInstance(instanceId: Long): Boolean {
        val instance = db.gearInstanceDao().get(instanceId) ?: return false
        val catalog = db.itemDao().get(instance.catalogId) ?: return false
        if (!catalog.slot.isEquippable()) return false
        if (instance.upgradeLevel >= ProgressionRules.maxUpgradeLevel(catalog.tier)) return false

        val materialId = ProgressionRules.primaryMaterialFor(catalog)
        val material = db.itemDao().get(materialId) ?: return false
        val materialCost = ProgressionRules.upgradeMaterialCost(catalog.tier, instance.upgradeLevel)
        val goldCost = ProgressionRules.upgradeGoldCost(catalog.tier, instance.upgradeLevel)
        val character = getCharacter()
        if (character.gold < goldCost || material.quantity < materialCost) return false

        val atkGain = if (catalog.atk > 0) (1 + catalog.tier / 2) else 0
        val defGain = if (catalog.def > 0) (1 + catalog.tier / 2) else 0
        val hpGain = if (catalog.hp > 0 || (atkGain == 0 && defGain == 0)) 3 + catalog.tier * 2 else 0

        db.withTransaction {
            db.itemDao().update(material.copy(quantity = material.quantity - materialCost))
            db.gearInstanceDao().update(
                instance.copy(
                    atk = instance.atk + atkGain,
                    def = instance.def + defGain,
                    hp = instance.hp + hpGain,
                    upgradeLevel = instance.upgradeLevel + 1
                )
            )
            db.characterDao().upsert(character.copy(gold = character.gold - goldCost))
        }
        return true
    }

    suspend fun allocateStatPoint(stat: String) {
        val character = getCharacter()
        if (character.freeStatPoints <= 0) return
        val updated = when (stat) {
            "STR" -> character.copy(strength = character.strength + 1)
            "END" -> character.copy(endurance = character.endurance + 1)
            "AGI" -> character.copy(agility = character.agility + 1)
            "WIL" -> character.copy(willpower = character.willpower + 1)
            else -> return
        }
        db.characterDao().upsert(updated.copy(freeStatPoints = character.freeStatPoints - 1))
    }

    suspend fun equipItem(instanceId: Long) {
        val instance = db.gearInstanceDao().get(instanceId) ?: return
        val catalog = db.itemDao().get(instance.catalogId) ?: return
        if (!catalog.slot.isEquippable()) return
        val character = getCharacter()
        val alreadyEquipped = instanceId in character.equippedIds().values
        if (!alreadyEquipped && (catalog.classAffinity != null) && (catalog.classAffinity != character.characterClass)) return
        fun toggle(current: Long?): Long? = if (current == instanceId) null else instanceId
        val updated = when (catalog.slot) {
            ItemSlot.WEAPON -> character.copy(weaponId = toggle(character.weaponId))
            ItemSlot.HEAD -> character.copy(headId = toggle(character.headId))
            ItemSlot.CHEST -> character.copy(chestId = toggle(character.chestId))
            ItemSlot.HANDS -> character.copy(handsId = toggle(character.handsId))
            ItemSlot.LEGS -> character.copy(legsId = toggle(character.legsId))
            ItemSlot.FEET -> character.copy(feetId = toggle(character.feetId))
            ItemSlot.TRINKET -> character.copy(trinketId = toggle(character.trinketId))
            else -> return
        }
        db.characterDao().upsert(updated)
    }

    suspend fun socketRune(instanceId: Long, slotIndex: Int, runeCatalogId: Long): Boolean {
        val instance = db.gearInstanceDao().get(instanceId) ?: return false
        val catalog = db.itemDao().get(instance.catalogId) ?: return false
        val rune = db.itemDao().get(runeCatalogId) ?: return false
        if ((rune.slot != ItemSlot.RUNE) || (rune.quantity <= 0)) return false
        val maxSlots = GearSockets.slotsForTier(catalog.tier)
        if (slotIndex !in (0 until maxSlots)) return false

        val previous = if (slotIndex == 0) instance.rune1Id else instance.rune2Id
        val updated = if (slotIndex == 0) instance.copy(rune1Id = runeCatalogId)
        else instance.copy(rune2Id = runeCatalogId)
        db.gearInstanceDao().update(updated)
        db.itemDao().update(rune.copy(quantity = rune.quantity - 1))
        previous?.let { prevId ->
            db.itemDao().get(prevId)?.let { old ->
                db.itemDao().update(old.copy(owned = true, quantity = old.quantity + 1))
            }
        }
        return true
    }

    suspend fun clearRune(instanceId: Long, slotIndex: Int): Boolean {
        val instance = db.gearInstanceDao().get(instanceId) ?: return false
        val previous = (if (slotIndex == 0) instance.rune1Id else instance.rune2Id) ?: return false
        val updated = if (slotIndex == 0) instance.copy(rune1Id = null) else instance.copy(rune2Id = null)
        db.gearInstanceDao().update(updated)
        db.itemDao().get(previous)?.let { old ->
            db.itemDao().update(old.copy(owned = true, quantity = old.quantity + 1))
        }
        return true
    }


    suspend fun equippedGear(character: CharacterEntity): Map<ItemSlot, ItemEntity> {
        val runeMap = stackTemplates().asSequence()
            .filter { it.slot == ItemSlot.RUNE }
            .associateBy { it.id }
        return EquippableSlots.mapNotNull { slot ->
            val id = character.equippedIds()[slot] ?: return@mapNotNull null
            val inst = db.gearInstanceDao().get(id) ?: return@mapNotNull null
            val cat = db.itemDao().get(inst.catalogId) ?: return@mapNotNull null
            slot to OwnedGear(inst, cat).asEquippedItem(runeMap)
        }.toMap()
    }

    suspend fun combatStatsFor(character: CharacterEntity): CombatStats {
        val gear = equippedGear(character)
        val equipped = gear.values.toList()
        var spd = 0
        var crit = 0
        var siphon = 0
        character.equippedIds().values.forEach { id ->
            if (id != null) {
                val inst = db.gearInstanceDao().get(id) ?: return@forEach
                inst.runeIds().forEach { rid ->
                    spd += Runes.spdBonus(rid)
                    crit += Runes.critBonus(rid)
                    if (Runes.hasSiphon(rid)) siphon += 8
                }
            }
        }
        return GameMath.combatStats(character, equipped, runeSpd = spd, runeCrit = crit, siphonHeal = siphon)
    }

    suspend fun grantMomentLoot(trigger: MomentTrigger): LootResult {
        val character = getCharacter()
        val tier = lootTierFor(
            source = LootSource.MOMENT,
            character = character,
            contentTier = character.level / 4 + 1
        )
        val loot = LootTables.rollMomentLoot(
            trigger = trigger,
            level = character.level,
            gearPool = eligibleGearTemplates(character, tier),
            stackPool = stackTemplates(),
            maxTier = tier
        )
        db.characterDao().upsert(applyLootToCharacter(character, loot))
        return loot
    }

    private suspend fun gearTemplates(maxTier: Int): List<ItemEntity> =
        db.itemDao().gearTemplatesUpToTier(maxTier.coerceAtLeast(1))

    private suspend fun eligibleGearTemplates(character: CharacterEntity, maxTier: Int): List<ItemEntity> =
        ProgressionRules.filterGearPool(gearTemplates(maxTier), maxTier, character)

    private suspend fun stackTemplates(): List<ItemEntity> =
        ItemCatalog.all
            .filter { it.slot.isStackable() }
            .map { db.itemDao().get(it.id) ?: it }

    private suspend fun applyLootToCharacter(character: CharacterEntity, loot: LootResult): CharacterEntity {
        var gold = character.gold + loot.goldBonus
        var energy = (character.energy + loot.energyBonus).coerceAtMost(GameMath.MAX_ENERGY)
        var xpBoost = character.pendingXpBoost + loot.xpBoostBonus

        for (g in loot.grants) {
            when (g) {
                is LootGrant.Gear -> createGearInstance(g.catalog, character.currentBiome)
                is LootGrant.Stack -> {
                    val row = db.itemDao().get(g.catalog.id) ?: g.catalog
                    db.itemDao().update(row.copy(owned = true, quantity = row.quantity + g.quantity))
                }
                is LootGrant.Gold -> gold += g.amount
                is LootGrant.Energy -> energy = (energy + g.amount).coerceAtMost(GameMath.MAX_ENERGY)
                is LootGrant.XpBoost -> xpBoost += g.amount
                is LootGrant.ChestOpened -> {
                    for (c in g.contents) {
                        when (c) {
                            is LootGrant.Gear -> createGearInstance(c.catalog, character.currentBiome)
                            is LootGrant.Stack -> {
                                val row = db.itemDao().get(c.catalog.id) ?: c.catalog
                                db.itemDao().update(row.copy(owned = true, quantity = row.quantity + c.quantity))
                            }
                            is LootGrant.Gold -> gold += c.amount
                            is LootGrant.Energy -> energy = (energy + c.amount).coerceAtMost(GameMath.MAX_ENERGY)
                            is LootGrant.XpBoost -> xpBoost += c.amount
                            is LootGrant.ChestOpened -> Unit
                        }
                    }
                }
            }
        }

        return character.copy(gold = gold, energy = energy, pendingXpBoost = xpBoost, lastEnergyUpdate = System.currentTimeMillis())
    }

    private fun LootResult.asClaimableGrants(): List<LootGrant> = buildList {
        grants.forEach { grant ->
            when (grant) {
                is LootGrant.ChestOpened -> addAll(grant.contents.flatMap { LootResult(grants = listOf(it)).asClaimableGrants() })
                else -> add(grant)
            }
        }
        if (goldBonus > 0) add(LootGrant.Gold(goldBonus))
        if (energyBonus > 0) add(LootGrant.Energy(energyBonus))
        if (xpBoostBonus > 0) add(LootGrant.XpBoost(xpBoostBonus))
    }

    private fun encodeIdleLoot(grants: List<LootGrant>): String =
        grants.joinToString("\n") { grant ->
            when (grant) {
                is LootGrant.Gear -> "gear|${grant.catalog.id}"
                is LootGrant.Stack -> "stack|${grant.catalog.id}|${grant.quantity}"
                is LootGrant.Gold -> "gold|${grant.amount}"
                is LootGrant.Energy -> "energy|${grant.amount}"
                is LootGrant.XpBoost -> "xpboost|${grant.amount}"
                is LootGrant.ChestOpened -> "chest|${grant.chest.id}"
            }
        }

    private fun pendingIdleLootGrants(encoded: String): List<LootGrant> {
        if (encoded.isBlank()) return emptyList()
        val catalog = ItemCatalog.all.associateBy { it.id }
        return encoded
            .lineSequence()
            .mapNotNull { line ->
                val parts = line.split('|')
                when (parts.firstOrNull()) {
                    "gear" -> parts.getOrNull(1)?.toLongOrNull()?.let(catalog::get)?.let(LootGrant::Gear)
                    "stack" -> {
                        val item = parts.getOrNull(1)?.toLongOrNull()?.let(catalog::get)
                        val qty = parts.getOrNull(2)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        item?.let { LootGrant.Stack(it, qty) }
                    }
                    "gold" -> parts.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }?.let(LootGrant::Gold)
                    "energy" -> parts.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }?.let(LootGrant::Energy)
                    "xpboost" -> parts.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }?.let(LootGrant::XpBoost)
                    else -> null
                }
            }
            .toList()
    }

    private suspend fun biomeProgress(): List<BiomeProgressEntity> =
        Biome.entries.map { getOrCreateBiomeProgress(it.name) }

    suspend fun getOrCreateBiomeProgress(biomeName: String): BiomeProgressEntity {
        val cleanName = Biome.fromName(biomeName).name
        val existing = db.biomeProgressDao().get(cleanName)
        if (existing != null) return existing
        return BiomeProgressEntity(cleanName).also { db.biomeProgressDao().upsert(it) }
    }

    private suspend fun lootTierFor(
        source: LootSource,
        character: CharacterEntity,
        contentTier: Int
    ): Int =
        ProgressionRules.tierCapForLoot(source, character, biomeProgress(), contentTier)

    private suspend fun addBiomeProgress(biomeName: String, points: Int): BiomeProgressEntity {
        val current = getOrCreateBiomeProgress(biomeName)
        val updatedPoints = current.progressPoints + points.coerceAtLeast(0)
        var updated = current.copy(progressPoints = updatedPoints)
        if (ProgressionRules.canUnlockBoss(updated)) {
            updated = updated.copy(bossUnlocked = true)
        }
        db.biomeProgressDao().upsert(updated)
        return updated
    }

    // ---- Biomes / travel ----

    suspend fun startTravel(biome: Biome) {
        val character = getCharacter()
        if (character.level < biome.levelRequired) return
        if (character.currentBiome == biome.name) return
        db.characterDao().upsert(character.copy(travelTarget = biome.name, travelProgress = 0.0))
    }

    suspend fun cancelTravel() {
        val character = getCharacter()
        db.characterDao().upsert(character.copy(travelTarget = null, travelProgress = 0.0))
    }

    suspend fun addTravelKm(km: Double): Biome? {
        if (km <= 0.0) return null
        val character = getCharacter()
        val targetName = character.travelTarget ?: return null
        val target = Biome.fromName(targetName)
        
        val currentBiome = Biome.fromName(character.currentBiome)
        val allProgress = db.biomeProgressDao().getAll()
        val canAdvance = ProgressionRules.canAdvanceFromBiome(currentBiome, allProgress)

        val newProgressRaw = character.travelProgress + km
        
        return if (canAdvance) {
            if (newProgressRaw >= target.travelKm) {
                db.characterDao().upsert(
                    character.copy(currentBiome = target.name, travelTarget = null, travelProgress = 0.0)
                )
                target
            } else {
                db.characterDao().upsert(character.copy(travelProgress = newProgressRaw))
                null
            }
        } else {
            // Boss gated: accumulate but cap just before arrival
            val cappedProgress = newProgressRaw.coerceAtMost(target.travelKm - 0.1).coerceAtLeast(0.0)
            db.characterDao().upsert(character.copy(travelProgress = cappedProgress))
            null
        }
    }

    suspend fun processIdleSteps(steps: Int) {
        if (steps <= 0) return
        var character = getCharacter()
        val km = steps.toDouble() / GameMath.STEPS_PER_KM
        if (character.travelTarget != null) {
            val reached = addTravelKm(km)
            if (reached != null) {
                character = getCharacter()
            }
        }

        val totalIdleSteps = character.idleSteps + steps
        val encounters = totalIdleSteps / 500
        val remainingSteps = totalIdleSteps % 500
        
        if (encounters > 0) {
            var bonusGold = 0
            var bonusXp = 0
            val currentBiome = Biome.fromName(character.currentBiome)
            val pendingIdleLoot = pendingIdleLootGrants(character.idlePendingLoot).toMutableList()
            
            repeat(encounters) {
                val monster = MonsterCatalog.all.filter { it.biome == currentBiome }.random()
                bonusGold += monster.goldReward
                bonusXp += monster.xpReward
                val tier = lootTierFor(LootSource.BATTLE, character, monster.tier)
                val loot = LootTables.rollBattleLoot(
                    monster = monster,
                    gearPool = eligibleGearTemplates(character, tier),
                    stackPool = stackTemplates()
                )
                pendingIdleLoot += loot.asClaimableGrants()
                addBiomeProgress(currentBiome.name, ProgressionRules.progressForVictory(monster))
            }
            
            db.characterDao().upsert(
                character.copy(
                    idleSteps = remainingSteps,
                    idleKills = character.idleKills + encounters,
                    idleGold = character.idleGold + bonusGold,
                    idleXp = character.idleXp + bonusXp,
                    idlePendingLoot = encodeIdleLoot(pendingIdleLoot)
                )
            )
        } else {
            db.characterDao().upsert(character.copy(idleSteps = remainingSteps))
        }
    }

    suspend fun claimIdleRewards(): RewardBatch? {
        val character = getCharacter()
        if (character.idleKills <= 0) return null
        
        val rewards = mutableListOf<Reward>()
        if (character.idleGold > 0) rewards.add(Reward.Gold(character.idleGold))
        if (character.idleXp > 0) rewards.add(Reward.Xp(character.idleXp))
        val idleLoot = LootResult(grants = pendingIdleLootGrants(character.idlePendingLoot))
        rewards.addAll(idleLoot.toRewards())
        
        val withGoldAndXp = character.copy(
            gold = character.gold + character.idleGold,
            xp = character.xp + character.idleXp,
            idleKills = 0,
            idleGold = 0,
            idleXp = 0,
            idlePendingLoot = ""
        )
        val leveledUp = GameMath.applyLevelUps(withGoldAndXp)
        val withLoot = applyLootToCharacter(leveledUp, idleLoot)
        if (leveledUp.level > character.level) {
            rewards.add(Reward.LevelUp(leveledUp.level))
        }
        
        db.characterDao().upsert(withLoot)
        return RewardBatch(RewardSource.OFFLINE_IDLE, rewards)
    }

    suspend fun claimWellnessEnergy() {
        val c = getCharacter()
        val today = LocalDate.now().toEpochDay()
        if (c.lastWellnessDay == today) return
        db.characterDao().upsert(
            c.copy(
                energy = (c.energy + 20).coerceAtMost(GameMath.MAX_ENERGY),
                lastWellnessDay = today,
                lastEnergyUpdate = System.currentTimeMillis()
            )
        )
    }

    // ---- Battles ----

    suspend fun applyVictory(monster: Monster, ambush: Boolean = false): RewardBatch = db.withTransaction {
        val character = getCharacter()
        val afterBattle = if (ambush) {
            val withXp = GameMath.applyBattleRewards(character, monster)
            withXp.copy(energy = character.energy)
        } else {
            GameMath.applyBattleRewards(character, monster)
        }

        val isBoss = MonsterCatalog.isBoss(monster)
        val currentProgress = if (isBoss) getOrCreateBiomeProgress(monster.biome.name) else null
        val isFirstClear = isBoss && currentProgress != null && !currentProgress.firstClearRewardClaimed

        val loot = when {
            ambush -> {
                val tier = lootTierFor(LootSource.AMBUSH, character, monster.tier + 1)
                LootTables.rollAmbushLoot(
                    level = character.level,
                    gearPool = eligibleGearTemplates(character, tier),
                    stackPool = stackTemplates(),
                    maxTier = tier,
                )
            }
            isFirstClear -> {
                val tier = lootTierFor(LootSource.BOSS, character, monster.tier)
                LootTables.bossFirstClearLoot(
                    biome = monster.biome,
                    character = character,
                    gearPool = eligibleGearTemplates(character, tier),
                    stackPool = stackTemplates(),
                )
            }
            isBoss -> {
                val tier = lootTierFor(LootSource.BOSS, character, monster.tier)
                LootTables.rollBattleLoot(
                    monster = monster,
                    gearPool = eligibleGearTemplates(character, tier),
                    stackPool = stackTemplates(),
                )
            }
            else -> {
                val tier = lootTierFor(LootSource.BATTLE, character, monster.tier)
                LootTables.rollBattleLoot(
                    monster = monster,
                    gearPool = eligibleGearTemplates(character, tier),
                    stackPool = stackTemplates(),
                )
            }
        }
        var updated = applyLootToCharacter(afterBattle, loot)
        val siphon = combatStatsFor(character).siphonHeal
        if (siphon > 0) {
            updated = updated.copy(energy = (updated.energy + siphon / 4).coerceAtMost(GameMath.MAX_ENERGY))
        }
        db.characterDao().upsert(updated)

        if (isBoss && currentProgress != null) {
            val updatedProgress = currentProgress.copy(
                bossDefeated = true,
                bossUnlocked = true,
                firstClearRewardClaimed = true,
            )
            db.biomeProgressDao().upsert(updatedProgress)
        } else if (!ambush) {
            addBiomeProgress(monster.biome.name, ProgressionRules.progressForVictory(monster))
        }

        val rewards = mutableListOf<Reward>()
        rewards.add(Reward.Xp(monster.xpReward))
        if (updated.level > character.level) {
            rewards.add(Reward.LevelUp(updated.level))
        }
        rewards.addAll(loot.toRewards())

        if (isFirstClear) {
            val nextOrdinal = monster.biome.ordinal + 1
            if (nextOrdinal < Biome.entries.size) {
                val nextBiome = Biome.entries[nextOrdinal]
                rewards.add(Reward.BiomeUnlocked(nextBiome.name, nextBiome.label))
            } else {
                rewards.add(Reward.TitleUnlocked("Conqueror of the Realm"))
            }
        }

        val source = when {
            isBoss -> RewardSource.BOSS
            ambush -> RewardSource.BATTLE
            else -> RewardSource.BATTLE
        }

        RewardBatch(source, rewards)
    }

    suspend fun spendBattleEnergy() {
        val character = getCharacter()
        db.characterDao().upsert(
            character.copy(
                energy = (character.energy - GameMath.BATTLE_ENERGY_COST).coerceAtLeast(0),
                lastEnergyUpdate = System.currentTimeMillis()
            )
        )
    }

    suspend fun payAmbushWager(gold: Int, energy: Int): Boolean {
        val c = getCharacter()
        if ((c.gold < gold) || (c.energy < energy)) return false
        db.characterDao().upsert(c.copy(gold = c.gold - gold, energy = c.energy - energy, lastEnergyUpdate = System.currentTimeMillis()))
        return true
    }

    // ---- Dev-flavor helpers ----

    suspend fun debugGrantGold(amount: Int = 1_000) {
        val c = getCharacter()
        db.characterDao().upsert(c.copy(gold = c.gold + amount))
    }

    suspend fun debugGrantXp(amount: Int = 500) {
        val c = getCharacter()
        var level = c.level
        var xp = c.xp + amount
        while (xp >= GameMath.xpToNextLevel(level)) {
            xp -= GameMath.xpToNextLevel(level)
            level++
        }
        db.characterDao().upsert(c.copy(level = level, xp = xp))
    }

    suspend fun debugFillEnergy() {
        val c = getCharacter()
        db.characterDao().upsert(c.copy(energy = GameMath.MAX_ENERGY, lastEnergyUpdate = System.currentTimeMillis()))
    }

    suspend fun debugStartSandboxHero() {
        val c = getCharacter()
        db.characterDao().upsert(
            CharacterEntity(
                id = 1L,
                name = if (c.name.isBlank() || (c.name == "Hero")) "Dev Hero" else c.name,
                characterClass = c.characterClass,
                gold = 500,
                energy = GameMath.MAX_ENERGY
            )
        )
    }

    suspend fun resetLiveProgress() {
        db.sessionDao().deleteAllSetLogs()
        db.sessionDao().deleteAllSessions()
        db.gearInstanceDao().deleteAll()
        db.itemDao().clearOwnership()
        db.characterDao().upsert(CharacterEntity())
    }

    suspend fun repairDuplicateGearInstances(): GearInventoryRepairResult {
        return db.withTransaction {
            val instances = db.gearInstanceDao().getAll()
            if (instances.isEmpty()) {
                return@withTransaction GearInventoryRepairResult(0, 0, 0)
            }
            val character = getCharacter()
            val equippedIds = character.equippedIds().values.filterNotNull().toSet()
            val keepers = mutableSetOf<Long>()
            val remap = mutableMapOf<Long, Long>()
            val removeIds = mutableListOf<Long>()

            instances.groupBy { it.duplicateKey() }.values.forEach { group ->
                val keeper = group.sortedWith(
                    compareByDescending<GearInstanceEntity> { it.id in equippedIds }
                        .thenByDescending { it.upgradeLevel }
                        .thenByDescending { it.runeIds().size }
                        .thenBy { it.id }
                ).first()
                keepers.add(keeper.id)
                group.filter { it.id != keeper.id }.forEach { duplicate ->
                    remap[duplicate.id] = keeper.id
                    removeIds.add(duplicate.id)
                }
            }

            if (removeIds.isNotEmpty()) {
                db.gearInstanceDao().deleteIds(removeIds)
                db.characterDao().upsert(character.remapEquippedGear(remap))
            }

            val after = instances.size - removeIds.size
            GearInventoryRepairResult(
                beforeCount = instances.size,
                afterCount = after,
                removedCount = removeIds.size
            )
        }
    }

    suspend fun sellGearInstance(instanceId: Long): Boolean {
        val instance = db.gearInstanceDao().get(instanceId) ?: return false
        val catalog = db.itemDao().get(instance.catalogId) ?: return false
        val refundGold = (catalog.price * 0.5f).toInt().coerceAtLeast(10)
        db.gearInstanceDao().delete(instance.id)
        val character = getCharacter()
        db.characterDao().upsert(unequipInstance(character, instance.id).copy(gold = character.gold + refundGold))
        return true
    }

    suspend fun fuseGearInstances(instanceIds: List<Long>): String {
        if (instanceIds.size != 3) return "Select exactly 3 items to fuse."
        val instances = instanceIds.mapNotNull { db.gearInstanceDao().get(it) }
        if (instances.size != 3) return "One or more selected items no longer exist."
        val items = instances.mapNotNull { inst -> db.itemDao().get(inst.catalogId) }
        if (items.size != 3) return "Invalid item data."
        val tier = items.first().tier
        if (items.any { it.tier != tier }) return "All 3 items must be of the same Tier!"
        if (tier >= 5) return "Tier 5 items cannot be fused further!"

        instances.forEach { db.gearInstanceDao().delete(it.id) }
        val hero = getCharacter()
        val maxTier = lootTierFor(LootSource.CAMPAIGN, hero, tier + 1)
        if ((tier + 1) > maxTier) return "Tier ${tier + 1} gear is locked behind more biome progress."
        val allCatalog = ItemCatalog.all
        val higherTierCandidates = allCatalog.filter {
            it.slot.isEquippable() && it.tier == (tier + 1) && ((it.classAffinity == null) || (it.classAffinity == hero.characterClass))
        }
        val resultItem = higherTierCandidates.randomOrNull() ?: allCatalog.filter { it.slot.isEquippable() && it.tier == (tier + 1) }.random()
        db.gearInstanceDao().insert(
            GearInstanceEntity(
                catalogId = resultItem.id,
                atk = resultItem.atk,
                def = resultItem.def,
                hp = resultItem.hp,
                rune1Id = null,
                rune2Id = null,
                originBiome = hero.currentBiome
            )
        )
        return "\uD83D\uDD25 FUSED! Obtained Tier ${tier + 1} ${resultItem.name}!"
    }

    private fun unequipInstance(character: CharacterEntity, instanceId: Long): CharacterEntity =
        character.copy(
            weaponId = character.weaponId?.takeUnless { it == instanceId },
            headId = character.headId?.takeUnless { it == instanceId },
            chestId = character.chestId?.takeUnless { it == instanceId },
            handsId = character.handsId?.takeUnless { it == instanceId },
            legsId = character.legsId?.takeUnless { it == instanceId },
            feetId = character.feetId?.takeUnless { it == instanceId },
            trinketId = character.trinketId?.takeUnless { it == instanceId }
        )

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

    private fun CharacterEntity.remapEquippedGear(remap: Map<Long, Long>): CharacterEntity =
        copy(
            weaponId = weaponId?.let { remap[it] ?: it },
            headId = headId?.let { remap[it] ?: it },
            chestId = chestId?.let { remap[it] ?: it },
            handsId = handsId?.let { remap[it] ?: it },
            legsId = legsId?.let { remap[it] ?: it },
            feetId = feetId?.let { remap[it] ?: it },
            trinketId = trinketId?.let { remap[it] ?: it }
        )

    suspend fun setDruidForm(form: String) {
        val c = getCharacter()
        db.characterDao().upsert(c.copy(druidForm = form))
    }

    suspend fun updatePartyId(partyId: String?) {
        db.characterDao().updatePartyId(partyId)
    }

    suspend fun updateGuildId(guildId: String?) {
        db.characterDao().updateGuildId(guildId)
    }

    suspend fun logWeight(weightKg: Double): Boolean {
        val character = getCharacter()
        db.withTransaction {
            db.bodyMetricDao().insert(
                BodyMetricEntity(
                    weightKg = weightKg,
                    heightM = character.heightM
                )
            )
            db.characterDao().upsert(character.copy(bodyWeightKg = weightKg))
        }
        return true
    }

    val bodyMetricHistory: Flow<List<BodyMetricEntity>> = db.bodyMetricDao().observeAll()

    @Suppress("unused")
    suspend fun latestWeightKg(): Double? = db.bodyMetricDao().getLatest()?.weightKg

    // ---- Movement Mastery ----

    fun observeMovementMastery(characterId: Long = 1L): Flow<List<MovementMasteryEntity>> =
        db.movementMasteryDao().observeAll(characterId)

    @Suppress("unused")
    suspend fun getAllMovementMastery(characterId: Long = 1L): List<MovementMasteryEntity> =
        db.movementMasteryDao().getAll(characterId)

    @Suppress("unused")
    suspend fun getMasteryByCanonicalKey(key: String, characterId: Long = 1L): MovementMasteryEntity? =
        db.movementMasteryDao().getByCanonicalKey(key, characterId)

    // ---- Fuzzy Matching & Previous Performance (Task 1.2) ----

    fun normalizeExerciseName(name: String): String {
        return name.lowercase()
            .replace(Regex("\\s*\\([^)]*\\)"), "") // strip parenthetical suffixes
            .replace(Regex("^(barbell|dumbbell|cable|machine|weighted|assisted)\\s+"), "") // strip leading adjectives
            .trim()
    }

    private suspend fun getFuzzyMatchedLogs(exerciseName: String): List<SetLogEntity> {
        val target = normalizeExerciseName(exerciseName)
        val allNames = db.sessionDao().getAllLoggedExerciseNames()
        val matchedNames = allNames.filter { normalizeExerciseName(it) == target }
        if (matchedNames.isEmpty()) return emptyList()
        return db.sessionDao().logsForExercises(matchedNames)
    }

    suspend fun getPreviousPerformance(exerciseName: String): List<SetLogEntity> {
        val allLogs = getFuzzyMatchedLogs(exerciseName)
        if (allLogs.isEmpty()) return emptyList()

        val sessionIds = allLogs.map { it.sessionId }.distinct()
        val sessions = db.sessionDao().sessionsByIds(sessionIds)
        val latestSessionId = sessions.maxByOrNull { it.endedAt }?.id ?: return emptyList()

        return allLogs.filter { it.sessionId == latestSessionId }
    }
}
