package com.wefit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_sessions")
data class ExerciseSessionEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: Int? = null,
    val assignmentId: Int,
    val trackingMethod: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val duration: Int = 0,
    val repetitions: Int = 0,
    val distance: Double = 0.0,
    val progressPercentage: Double = 0.0,
    val status: String, // in_progress, completed, cancelled
    val syncStatus: String = "pending" // pending, synced, failed
)