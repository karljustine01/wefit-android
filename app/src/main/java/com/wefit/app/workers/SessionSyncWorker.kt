package com.wefit.app.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wefit.app.data.local.database.WeFitDatabase
import com.wefit.app.data.remote.api.ExerciseSessionApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.CompleteSessionRequest
import com.wefit.app.data.remote.dto.StartSessionRequest

/**
 * Syncs locally-saved offline exercise sessions to the backend once
 * connectivity returns. Designed to run via WorkManager's network-constrained
 * scheduling so it only fires when online.
 */
class SessionSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = WeFitDatabase.getInstance(applicationContext)
        val api = RetrofitClient.getInstance(applicationContext).create(ExerciseSessionApi::class.java)
        val dao = db.exerciseSessionDao()

        val pending = dao.getPendingSync()
        if (pending.isEmpty()) return Result.success()

        var anyFailed = false

        for (localSession in pending) {
            try {
                // Start the session server-side to get a real ID
                val startResponse = api.start(
                    StartSessionRequest(localSession.assignmentId, localSession.trackingMethod)
                )
                val serverId = startResponse.data?.id ?: continue

                // Immediately complete it with the final recorded values
                api.complete(
                    serverId,
                    CompleteSessionRequest(
                        duration = localSession.duration,
                        repetitions = localSession.repetitions,
                        distance = localSession.distance,
                        progress_percentage = localSession.progressPercentage
                    )
                )

                dao.update(localSession.copy(serverId = serverId, syncStatus = "synced"))
            } catch (e: Exception) {
                anyFailed = true
                dao.update(localSession.copy(syncStatus = "failed"))
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }
}