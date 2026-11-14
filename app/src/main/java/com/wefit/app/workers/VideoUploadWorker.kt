package com.wefit.app.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.api.SessionVideoApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Uploads the recorded clip after the session completes. WorkManager gives us
 * "wait for network + retry with backoff + survive app being killed" for free,
 * and the file is streamed from disk (never loaded into RAM).
 */
class VideoUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getInt(KEY_SESSION_ID, -1)
        val path = inputData.getString(KEY_PATH)
        val duration = inputData.getInt(KEY_DURATION, 0)
        val file = path?.let { File(it) }

        if (sessionId < 0 || file == null || !file.exists()) return Result.success() // nothing to do

        return try {
            val api = RetrofitClient.getMediaInstance(applicationContext).create(SessionVideoApi::class.java)
            val part = MultipartBody.Part.createFormData(
                "video", file.name, file.asRequestBody("video/mp4".toMediaType())
            )
            api.upload(sessionId, part, duration.toString().toRequestBody("text/plain".toMediaType()))
            file.delete()
            Result.success()
        } catch (e: HttpException) {
            // 4xx = permanent (not your session, too big, cancelled...). Don't retry forever.
            if (e.code() in 400..499 && e.code() != 408 && e.code() != 429) {
                file.delete()
                Result.success()
            } else retryOrGiveUp(file)
        } catch (e: Exception) {
            retryOrGiveUp(file)
        }
    }

    private fun retryOrGiveUp(file: File): Result =
        if (runAttemptCount >= MAX_ATTEMPTS) { file.delete(); Result.failure() } else Result.retry()

    companion object {
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_PATH = "path"
        private const val KEY_DURATION = "duration"
        private const val MAX_ATTEMPTS = 6

        fun enqueue(context: Context, sessionId: Int, file: File, durationSeconds: Int) {
            val request = OneTimeWorkRequestBuilder<VideoUploadWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setInputData(workDataOf(
                    KEY_SESSION_ID to sessionId,
                    KEY_PATH to file.absolutePath,
                    KEY_DURATION to durationSeconds
                ))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("video_upload_$sessionId", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
