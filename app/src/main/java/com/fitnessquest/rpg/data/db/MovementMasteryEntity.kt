package com.fitnessquest.rpg.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks long-term, durable mastery for a canonical exercise movement (e.g. Squat, Bench Press,
 * Deadlift, Running, Plank) so that repetitive real-world training builds a permanent RPG identity
 * and progression independent of broad hero stats.
 */
@Entity(
    tableName = "movement_mastery",
    indices = [
        Index(value = ["characterId", "canonicalKey"], unique = true),
        Index(value = ["category"])
    ]
)
data class MovementMasteryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Long = 1L,
    /** Unique canonical key, e.g. "SQUAT", "BENCH_PRESS", "DEADLIFT", "RUNNING", "STRENGTH_GENERAL". */
    val canonicalKey: String,
    val displayName: String,
    val category: ExerciseCategory,
    val level: Int = 1,
    val currentXp: Long = 0L,
    val lifetimeVolumeKg: Double = 0.0,
    val lifetimeReps: Int = 0,
    val lifetimeDistanceKm: Double = 0.0,
    val lifetimeDurationSec: Long = 0L,
    val totalSessionsLogged: Int = 0,
    val highest1RmKg: Double = 0.0,
    val highestWeightKg: Double = 0.0,
    val bestDistanceKm: Double = 0.0,
    /** Best pace in seconds per kilometer (e.g. 300 = 5:00 min/km). 0 if no record. */
    val bestPaceSecPerKm: Long = 0L,
    val lastTrainedEpochMs: Long = 0L
)
