package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.remote.api.RecycleApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.RecycleBinDto
import com.wefit.app.data.remote.dto.RecycleRunResultDto
import com.wefit.app.data.remote.dto.RecycleStatsDto
import org.json.JSONObject
import retrofit2.HttpException

class RecycleRepository(context: Context) {

    private val api = RetrofitClient.getInstance(context).create(RecycleApi::class.java)

    /** Turns 4xx bodies (e.g. "Restore the exercise first.") into a readable message. */
    private fun friendly(e: Exception): Exception {
        if (e is HttpException) {
            val msg = try {
                JSONObject(e.response()?.errorBody()?.string().orEmpty()).optString("message")
            } catch (_: Exception) {
                ""
            }
            if (msg.isNotBlank()) return Exception(msg)
        }
        return e
    }

    private suspend fun <T> call(block: suspend () -> com.wefit.app.data.remote.dto.ApiResponse<T>): Result<T?> = try {
        val r = block()
        if (r.success) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) {
        Result.failure(friendly(e))
    }

    suspend fun bin(): Result<RecycleBinDto> =
        call { api.bin() }.mapCatching { it ?: error("Empty response") }

    suspend fun restore(type: String, id: Int): Result<Unit> =
        call { api.restore(type, id) }.map { }

    suspend fun deleteForever(type: String, id: Int): Result<Unit> =
        call { api.deleteForever(type, id) }.map { }

    suspend fun emptyBin(): Result<Unit> =
        call { api.emptyBin() }.map { }

    suspend fun stats(): Result<RecycleStatsDto> =
        call { api.stats() }.mapCatching { it ?: error("Empty response") }

    suspend fun runCleanup(dryRun: Boolean = false): Result<RecycleRunResultDto> =
        call { api.runCleanup(dryRun) }.mapCatching { it ?: error("Empty response") }
}
