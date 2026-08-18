package com.fitnessquest.rpg.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.fitnessquest.rpg.domain.SetType

/**
 * Single active non-demo workout session per player character.
 */
@Entity(tableName = "active_sessions")
data class ActiveSessionEntity(
    @PrimaryKey val id: Long = 1L,
    val title: String = "Freestyle Session",
    val workoutId: Long? = null,
    val startedAt: Long = System.currentTimeMillis(),
    /** Epoch millis when the workout was paused; null when running. */
    val pausedAt: Long? = null,
    /** Total accumulated paused duration in milliseconds. */
    val accumulatedPausedMs: Long = 0L,
    /** Epoch millis when the current rest period ends; null when not resting. */
    val restEndsAt: Long? = null,
    val restDurationSec: Int = 90,
    val heatStreak: Int = 0,
    val lastLogAt: Long = 0L,
    val currentExerciseIndex: Int = 0,
    val ambushOfferedThisSession: Boolean = false,
    val ambushXpMult: Float = 1.0f,
    val momentSpoilsUsed: Int = 0,
    val status: String = "ACTIVE",
    val completionToken: String? = null
)

/**
 * Draft exercise inside an active workout.
 */
@Entity(
    tableName = "active_session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = ActiveSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["activeSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("activeSessionId")]
)
data class ActiveExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activeSessionId: Long = 1L,
    val exerciseName: String,
    val category: ExerciseCategory,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetWeightKg: Double? = null,
    val trackingType: String = "WEIGHT_REPS",
    val sortOrder: Int = 0,
    val notes: String = "",
    val suggestionReason: String? = null
)

/**
 * Draft set log linked directly to an active exercise by ID.
 */
@Entity(
    tableName = "active_session_set_logs",
    foreignKeys = [
        ForeignKey(
            entity = ActiveSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["activeSessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ActiveExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("activeSessionId"), Index("exerciseId")]
)
data class ActiveSetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activeSessionId: Long = 1L,
    val exerciseId: Long,
    val exerciseName: String,
    val category: ExerciseCategory,
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val durationMin: Double = 0.0,
    val distanceKm: Double = 0.0,
    val xp: Int = 0,
    val rir: Int? = null,
    val avgHr: Int? = null,
    val maxHr: Int? = null,
    val speedKmh: Double = 0.0,
    val inclinePercent: Double = 0.0,
    val cardioProgram: String = "",
    val setType: SetType = SetType.NORMAL,
    val loggedAt: Long = System.currentTimeMillis()
)

/**
 * Durable outbox entry for asynchronous cloud sync (Party damage, Guild damage, etc.).
 */
@Entity(tableName = "pending_sync_outbox")
data class PendingSyncEntity(
    @PrimaryKey val eventId: String,
    val type: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING",
    val retryCount: Int = 0
)

/**
 * Relation wrapper linking an exercise entity to its logged draft sets.
 */
data class ActiveExerciseWithSets(
    @Embedded val exercise: ActiveExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId"
    )
    val sets: List<ActiveSetLogEntity>
)

/**
 * Full active session relation wrapper with exercises and sets.
 */
data class ActiveSessionWithDetails(
    @Embedded val session: ActiveSessionEntity,
    @Relation(
        entity = ActiveExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "activeSessionId"
    )
    val exercises: List<ActiveExerciseWithSets>
) {
    val sortedExercises: List<ActiveExerciseWithSets>
        get() = exercises.sortedBy { it.exercise.sortOrder }
}
