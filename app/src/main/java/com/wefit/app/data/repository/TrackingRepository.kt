package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.local.database.WeFitDatabase
import com.wefit.app.data.local.entity.ExerciseSessionEntity
import com.wefit.app.data.remote.api.ExerciseSessionApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.CompleteSessionRequest
import com.wefit.app.data.remote.dto.StartSessionRequest
import com.wefit.app.tracking.TrackingData

class TrackingRepository(context: Context) {

    private val api = RetrofitClient.getInstance(context).create(ExerciseSessionApi::class.java)
    private val dao = WeFitDatabase.getInstance(context).exerciseSessionDao()

    /**
     * Attempts to start the session on the server. If that fails (offline),
     * saves it locally with a pending sync flag instead. Returns the ID to use
     * for subsequent progress calls — a server ID if online, a local Room ID if offline.
     */
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