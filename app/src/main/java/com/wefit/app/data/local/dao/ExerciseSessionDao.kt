package com.wefit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wefit.app.data.local.entity.ExerciseSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ExerciseSessionEntity): Long

    @Update
    suspend fun update(session: ExerciseSessionEntity)

    @Query("SELECT * FROM exercise_sessions WHERE localId = :localId")
    suspend fun getById(localId: Long): ExerciseSessionEntity?

    @Query("SELECT * FROM exercise_sessions WHERE syncStatus = 'pending'")
    suspend fun getPendingSync(): List<ExerciseSessionEntity>

    @Query("SELECT * FROM exercise_sessions ORDER BY startedAt DESC")
    fun getAll(): Flow<List<ExerciseSessionEntity>>
}