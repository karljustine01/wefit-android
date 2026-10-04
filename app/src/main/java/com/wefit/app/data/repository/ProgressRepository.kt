package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.remote.api.ProgressApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.SessionProgressDto

class ProgressRepository(context: Context) {

    private val api = RetrofitClient.getInstance(context).create(ProgressApi::class.java)

    suspend fun getProgress(assignmentId: Int): Result<List<SessionProgressDto>> = try {
        val response = api.getProgress(assignmentId)
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }
}