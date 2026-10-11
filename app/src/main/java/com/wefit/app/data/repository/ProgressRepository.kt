package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.remote.api.ExerciseSessionApi
import com.wefit.app.data.remote.api.ProgressApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.SessionDetailDto
import com.wefit.app.data.remote.dto.SessionProgressDto

class ProgressRepository(context: Context) {

    private val api = RetrofitClient.getInstance(context).create(ProgressApi::class.java)
    private val sessionApi = RetrofitClient.getInstance(context).create(ExerciseSessionApi::class.java)

    suspend fun getProgress(assignmentId: Int): Result<List<SessionProgressDto>> = try {
        val response = api.getProgress(assignmentId)
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getSessionDetail(sessionId: Int): Result<SessionDetailDto> = try {
        val response = sessionApi.getSession(sessionId)
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }
}