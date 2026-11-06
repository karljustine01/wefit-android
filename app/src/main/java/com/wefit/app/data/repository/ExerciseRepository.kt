package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.remote.api.ExerciseApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.ExerciseDto
import com.google.gson.Gson

class ExerciseRepository(context: Context) {

    private val api = RetrofitClient.getInstance(context).create(ExerciseApi::class.java)

    suspend fun listExercises(search: String? = null): Result<List<ExerciseDto>> = try {
        val response = api.list(search)
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    sealed class DeleteResult {
        object Deleted : DeleteResult()
        /** Exercise is still assigned; the teacher must confirm before it is deleted. */
        data class NeedsConfirmation(val message: String) : DeleteResult()
        data class Failed(val message: String) : DeleteResult()
    }

    suspend fun deleteExercise(id: Int, force: Boolean = false): DeleteResult = try {
        val response = api.delete(id, if (force) 1 else null)
        when {
            response.isSuccessful -> DeleteResult.Deleted
            response.code() == 409 -> DeleteResult.NeedsConfirmation(errorMessage(response) ?: "This exercise is still assigned.")
            else -> DeleteResult.Failed(errorMessage(response) ?: "Could not delete exercise (${response.code()})")
        }
    } catch (e: Exception) {
        DeleteResult.Failed(e.message ?: "Could not delete exercise")
    }

    private fun errorMessage(response: retrofit2.Response<*>): String? = try {
        Gson().fromJson(response.errorBody()?.string(), ApiResponse::class.java)?.message
    } catch (e: Exception) {
        null
    }
}