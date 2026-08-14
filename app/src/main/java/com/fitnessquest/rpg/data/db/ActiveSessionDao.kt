package com.fitnessquest.rpg.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveSessionDao {

    @Transaction
    @Query("SELECT * FROM active_sessions WHERE id = 1 LIMIT 1")
    fun observeActiveSessionWithDetails(): Flow<ActiveSessionWithDetails?>

    @Transaction
    @Query("SELECT * FROM active_sessions WHERE id = 1 LIMIT 1")
    suspend fun getActiveSessionWithDetails(): ActiveSessionWithDetails?

    @Query("SELECT * FROM active_sessions WHERE id = 1 LIMIT 1")
    suspend fun getActiveSession(): ActiveSessionEntity?

    @Upsert
    suspend fun upsertActiveSession(session: ActiveSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveExercises(exercises: List<ActiveExerciseEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveExercise(exercise: ActiveExerciseEntity): Long

    @Update
    suspend fun updateActiveExercise(exercise: ActiveExerciseEntity)

    @Query("DELETE FROM active_session_exercises WHERE id = :exerciseId")
    suspend fun deleteActiveExercise(exerciseId: Long)

    @Query("SELECT * FROM active_session_exercises WHERE activeSessionId = 1 ORDER BY sortOrder ASC")
    suspend fun getActiveExercises(): List<ActiveExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveSetLog(setLog: ActiveSetLogEntity): Long

    @Query("DELETE FROM active_session_set_logs WHERE id = (SELECT id FROM active_session_set_logs WHERE exerciseId = :exerciseId ORDER BY id DESC LIMIT 1)")
    suspend fun deleteLastSetLogForExercise(exerciseId: Long)

    @Query("DELETE FROM active_session_set_logs WHERE id = :setLogId")
    suspend fun deleteSetLog(setLogId: Long)

    @Query("DELETE FROM active_session_exercises WHERE activeSessionId = :activeSessionId")
    suspend fun deleteActiveExercises(activeSessionId: Long = 1L)

    @Query("DELETE FROM active_session_set_logs WHERE activeSessionId = :activeSessionId")
    suspend fun deleteActiveSetLogs(activeSessionId: Long = 1L)

    @Query("DELETE FROM active_sessions WHERE id = :id")
    suspend fun deleteActiveSession(id: Long = 1L)

    // ---- Idempotency & Outbox ----

    @Query("SELECT * FROM sessions WHERE completionToken = :token LIMIT 1")
    suspend fun getSessionByCompletionToken(token: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOutboxEvent(event: PendingSyncEntity)

    @Query("SELECT * FROM pending_sync_outbox WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingOutboxEvents(): List<PendingSyncEntity>

    @Query("UPDATE pending_sync_outbox SET status = 'SENT' WHERE eventId = :eventId")
    suspend fun markOutboxEventSent(eventId: String)

    @Query("UPDATE pending_sync_outbox SET retryCount = retryCount + 1 WHERE eventId = :eventId")
    suspend fun incrementOutboxEventRetry(eventId: String)
}
