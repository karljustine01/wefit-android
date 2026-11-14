package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.api.SessionVideoApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SessionVideoRepository(private val context: Context) {

    private val api = RetrofitClient.getMediaInstance(context).create(SessionVideoApi::class.java)

    /** Teacher side: downloads the (already compressed) clip into the cache and returns it. */
    suspend fun download(sessionId: Int): Result<File> = withContext(Dispatchers.IO) {
        try {
            val response = api.download(sessionId)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                return@withContext Result.failure(Exception(
                    if (response.code() == 404) "No video was submitted for this session."
                    else "Could not load video (${response.code()})."
                ))
            }
            val file = File(context.cacheDir, "review_$sessionId.mp4")
            body.byteStream().use { input -> file.outputStream().use { input.copyTo(it) } }
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
