package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.remote.api.ExerciseApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.ExerciseDto

class ExerciseRepository(context: Context) {

    private val api = RetrofitClient.getInstance(context).create(ExerciseApi::class.java)

    suspend fun listExercises(search: String? = null): Result<List<ExerciseDto>> = try {
        val response = api.list(search)
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }
}