package com.fitnessquest.rpg.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fitnessquest.rpg.domain.CharacterClass
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM character WHERE id = 1")
    fun observe(): Flow<CharacterEntity?>

    @Query("SELECT * FROM character WHERE id = 1")
    suspend fun get(): CharacterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(character: CharacterEntity)

    @Query("UPDATE character SET skinColor = :skinColor, hairColor = :hairColor, underwearColor = :underwearColor, eyeColor = :eyeColor, hairStyle = :hairStyle, gender = :gender, braColor = :braColor, race = :race WHERE id = 1")
    suspend fun updateAppearance(
        skinColor: Long,
        hairColor: Long,
        underwearColor: Long,
        eyeColor: Long,
        hairStyle: String,
        gender: String,
        braColor: Long,
        race: String,
    )

    @Query("UPDATE character SET partyId = :partyId WHERE id = 1")
    suspend fun updatePartyId(partyId: String?)

    @Query("UPDATE character SET guildId = :guildId WHERE id = 1")
    suspend fun updateGuildId(guildId: String?)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts")
    suspend fun getAllWorkouts(): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun get(id: Long): WorkoutEntity?

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY sortOrder")
    suspend fun exercisesFor(workoutId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY sortOrder")
    fun observeExercisesFor(workoutId: Long): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT * FROM workout_exercises ORDER BY workoutId, sortOrder")
    fun observeAllExercises(): Flow<List<WorkoutExerciseEntity>>

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Insert
    suspend fun insertExercises(exercises: List<WorkoutExerciseEntity>)

    @Update
    suspend fun updateExercise(exercise: WorkoutExerciseEntity)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteExercisesFor(workoutId: Long)

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity): Int

    @Transaction
    suspend fun saveWorkout(workout: WorkoutEntity, exercises: List<WorkoutExerciseEntity>): Long {
        val id = insertWorkout(workout)
        insertExercises(exercises.mapIndexed { i, e -> e.copy(id = 0, workoutId = id, sortOrder = i) })
        return id
    }

    /** Replaces exercises for an existing quest while preserving createdAt / ai flag. */
    @Transaction
    suspend fun replaceWorkout(
        workout: WorkoutEntity,
        exercises: List<WorkoutExerciseEntity>,
    ) {
        val affected = updateWorkout(workout)
        if (affected == 0) {
            insertWorkout(workout)
        }
        deleteExercisesFor(workout.id)
        insertExercises(exercises.mapIndexed { i, e -> e.copy(id = 0, workoutId = workout.id, sortOrder = i) })
    }

    @Transaction
    suspend fun deleteWorkoutFully(id: Long) {
        deleteExercisesFor(id)
        deleteWorkout(id)
    }

    @Transaction
    suspend fun deleteWorkoutsFully(ids: List<Long>) {
        ids.forEach { id ->
            deleteExercisesFor(id)
            deleteWorkout(id)
        }
    }

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()

    @Query("DELETE FROM workout_exercises")
    suspend fun deleteAllWorkoutExercises()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY endedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions")
    suspend fun getAllSessions(): List<SessionEntity>

    @Query("SELECT * FROM set_logs")
    suspend fun getAllSetLogs(): List<SetLogEntity>

    @Query("SELECT * FROM sessions WHERE completionToken = :token LIMIT 1")
    suspend fun getByCompletionToken(token: String): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY endedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY endedAt DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<SessionEntity>


    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: Long): SessionEntity?

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId")
    suspend fun setLogsFor(sessionId: Long): List<SetLogEntity>

    @Query("SELECT * FROM set_logs")
    fun observeAllSetLogs(): Flow<List<SetLogEntity>>

    /** Heaviest weight ever logged for an exercise (0 if never logged). */
    @Query("SELECT COALESCE(MAX(weightKg), 0) FROM set_logs WHERE exerciseName = :exerciseName")
    suspend fun maxWeightFor(exerciseName: String): Double

    @Query("SELECT COALESCE(MAX(weightKg), 0) FROM set_logs WHERE exerciseName IN (:names)")
    suspend fun maxWeightForExercises(names: List<String>): Double

    @Query("SELECT * FROM set_logs WHERE exerciseName = :exerciseName")
    suspend fun logsForExercise(exerciseName: String): List<SetLogEntity>

    @Query("SELECT DISTINCT exerciseName FROM set_logs")
    suspend fun getAllLoggedExerciseNames(): List<String>

    @Query("SELECT * FROM set_logs WHERE exerciseName IN (:names)")
    suspend fun logsForExercises(names: List<String>): List<SetLogEntity>


    @Query("SELECT * FROM sessions WHERE id IN (:ids) ORDER BY endedAt DESC")
    suspend fun sessionsByIds(ids: List<Long>): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE endedAt >= :since ORDER BY endedAt DESC")
    suspend fun sessionsSince(since: Long): List<SessionEntity>

    @Query("SELECT COUNT(*) FROM sessions WHERE endedAt >= :since")
    suspend fun countSessionsSince(since: Long): Int

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Insert
    suspend fun insertSetLogs(logs: List<SetLogEntity>)

    /** Restore path: keeps the explicit id, ignores if it already exists. Returns -1 when ignored. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessionKeepId(session: SessionEntity): Long

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("DELETE FROM set_logs WHERE sessionId = :sessionId")
    suspend fun deleteSetLogsForSession(sessionId: Long)

    @Transaction
    suspend fun deleteSessionFully(id: Long) {
        deleteSetLogsForSession(id)
        deleteSession(id)
    }

    @Query("DELETE FROM set_logs")
    suspend fun deleteAllSetLogs()

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY tier, price")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE owned = 1 OR quantity > 0 ORDER BY tier, price")
    fun observeOwned(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: Long): ItemEntity?

    @Query("SELECT * FROM items")
    suspend fun getAll(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE slot = :slot AND tier <= :maxTier")
    suspend fun bySlotUpToTier(slot: String, maxTier: Int): List<ItemEntity>

    @Query("SELECT * FROM items WHERE slot NOT IN ('CONSUMABLE','RUNE','MATERIAL','LOOT_CHEST') AND tier <= :maxTier")
    suspend fun gearTemplatesUpToTier(maxTier: Int): List<ItemEntity>

    @Query("SELECT COUNT(*) FROM items")
    suspend fun count(): Int

    @Update
    suspend fun update(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ItemEntity>)

    @Query("UPDATE items SET owned = 1 WHERE id IN (:ids)")
    suspend fun markOwned(ids: List<Long>)

    /** Restore path for stackable stock counts. */
    @Query("UPDATE items SET quantity = :quantity, owned = 1 WHERE id = :id")
    suspend fun setQuantity(id: Long, quantity: Int)

    @Query("UPDATE items SET quantity = 0, owned = 0")
    suspend fun clearOwnership()
}

@Dao
interface GearInstanceDao {
    @Query("SELECT * FROM gear_instances ORDER BY id DESC")
    fun observeAll(): Flow<List<GearInstanceEntity>>

    @Query("SELECT * FROM gear_instances")
    suspend fun getAll(): List<GearInstanceEntity>

    @Query("SELECT * FROM gear_instances WHERE id = :id")
    suspend fun get(id: Long): GearInstanceEntity?

    @Insert
    suspend fun insert(instance: GearInstanceEntity): Long

    @Update
    suspend fun update(instance: GearInstanceEntity)

    @Query("DELETE FROM gear_instances WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM gear_instances WHERE id IN (:ids)")
    suspend fun deleteIds(ids: List<Long>)

    @Query("DELETE FROM gear_instances")
    suspend fun deleteAll()
}

@Dao
interface BiomeProgressDao {
    @Query("SELECT * FROM biome_progress ORDER BY biomeName")
    fun observeAll(): Flow<List<BiomeProgressEntity>>

    @Query("SELECT * FROM biome_progress")
    suspend fun getAll(): List<BiomeProgressEntity>

    @Query("SELECT * FROM biome_progress WHERE biomeName = :biomeName")
    suspend fun get(biomeName: String): BiomeProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: BiomeProgressEntity)

    @Query("DELETE FROM biome_progress")
    suspend fun deleteAll()
}

@Dao
interface ClassProgressDao {
    @Query("SELECT * FROM class_progress WHERE characterId = 1 AND clazz = :clazz")
    suspend fun get(clazz: CharacterClass): ClassProgressEntity?

    @Query("SELECT * FROM class_progress WHERE characterId = 1")
    suspend fun getAll(): List<ClassProgressEntity>

    @Query("SELECT * FROM class_progress WHERE characterId = 1")
    fun observeAll(): Flow<List<ClassProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ClassProgressEntity)

    @Query("DELETE FROM class_progress")
    suspend fun deleteAll()
}


@Dao
interface BodyMetricDao {
    @Query("SELECT * FROM body_metrics ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<BodyMetricEntity>>

    @Query("SELECT * FROM body_metrics ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): BodyMetricEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metric: BodyMetricEntity)

    @Query("DELETE FROM body_metrics")
    suspend fun deleteAll()
}
