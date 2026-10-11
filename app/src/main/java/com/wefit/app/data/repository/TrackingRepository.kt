package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.local.database.WeFitDatabase
import com.wefit.app.data.local.entity.ExerciseSessionEntity
import com.wefit.app.data.remote.api.ExerciseSessionApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.CompleteSessionRequest
import com.wefit.app.data.remote.dto.StartSessionRequest
import com.wefit.app.data.remote.dto.UpdateProgressRequest
import com.wefit.app.tracking.TrackingData

class TrackingRepository(context: Context) {

    private val api = RetrofitClient.getInstance(context).create(ExerciseSessionApi::class.java)
    private val dao = WeFitDatabase.getInstance(context).exerciseSessionDao()

    suspend fun startSession(assignmentId: Int, trackingMethod: String): SessionHandle {
        return try {
            val response = api.start(StartSessionRequest(assignmentId, trackingMethod))
            if (response.success && response.data != null) {
                SessionHandle(serverId = response.data.id, localId = null, isOnline = true)
            } else {
                startOffline(assignmentId, trackingMethod)
            }
        } catch (e: Exception) {
            startOffline(assignmentId, trackingMethod)
        }
    }

    private suspend fun startOffline(assignmentId: Int, trackingMethod: String): SessionHandle {
        val entity = ExerciseSessionEntity(
            assignmentId = assignmentId,
            trackingMethod = trackingMethod,
            startedAt = System.currentTimeMillis(),
            status = "in_progress",
            syncStatus = "pending"
        )
        val localId = dao.insert(entity)
        return SessionHandle(serverId = null, localId = localId, isOnline = false)
    }

    suspend fun uploadProgress(handle: SessionHandle, data: TrackingData, percent: Double) {
        val id = handle.serverId ?: return
        if (!handle.isOnline) return
        try {
            api.progress(
                id,
                UpdateProgressRequest(
                    progress_percentage = percent,
                    repetitions = data.repetitions,
                    distance = data.distanceMeters,
                    duration = data.durationSeconds,
                    latitude = data.latitude,
                    longitude = data.longitude
                )
            )
        } catch (_: Exception) {
            // A missed snapshot is acceptable; never interrupt tracking for it.
        }
    }

    suspend fun completeSession(handle: SessionHandle, finalData: TrackingData): Result<Unit> {
        return try {
            if (handle.isOnline && handle.serverId != null) {
                api.complete(
                    handle.serverId,
                    CompleteSessionRequest(
                        duration = finalData.durationSeconds,
                        repetitions = finalData.repetitions,
                        distance = finalData.distanceMeters,
                        progress_percentage = 100.0
                    )
                )
            } else if (handle.localId != null) {
                val entity = dao.getById(handle.localId) ?: return Result.failure(Exception("Local session not found"))
                dao.update(
                    entity.copy(
                        completedAt = System.currentTimeMillis(),
                        duration = finalData.durationSeconds,
                        repetitions = finalData.repetitions,
                        distance = finalData.distanceMeters,
                        progressPercentage = 100.0,
                        status = "completed",
                        syncStatus = "pending"
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class SessionHandle(
    val serverId: Int?,
    val localId: Long?,
    val isOnline: Boolean
)