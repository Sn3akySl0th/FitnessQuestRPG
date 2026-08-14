package com.fitnessquest.rpg.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovementMasteryDao {
    @Query("SELECT * FROM movement_mastery WHERE characterId = :characterId ORDER BY level DESC, currentXp DESC")
    fun observeAll(characterId: Long = 1L): Flow<List<MovementMasteryEntity>>

    @Query("SELECT * FROM movement_mastery WHERE characterId = :characterId ORDER BY level DESC, currentXp DESC")
    suspend fun getAll(characterId: Long = 1L): List<MovementMasteryEntity>

    @Query("SELECT * FROM movement_mastery WHERE characterId = :characterId AND canonicalKey = :canonicalKey LIMIT 1")
    suspend fun getByCanonicalKey(canonicalKey: String, characterId: Long = 1L): MovementMasteryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MovementMasteryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<MovementMasteryEntity>)

    @Query("DELETE FROM movement_mastery WHERE characterId = :characterId")
    suspend fun deleteAllForCharacter(characterId: Long = 1L)
}
