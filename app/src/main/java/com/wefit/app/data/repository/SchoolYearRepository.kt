package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.api.SchoolYearApi
import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.CompleteYearResult
import com.wefit.app.data.remote.dto.SchoolYearDto
import com.wefit.app.data.remote.dto.StartSchoolYearRequest
import org.json.JSONObject
import retrofit2.HttpException

class SchoolYearRepository(context: Context) {

    private val api = RetrofitClient.getInstance(context).create(SchoolYearApi::class.java)

    private fun friendly(e: Exception): Exception {
        if (e is HttpException) {
            val msg = try {
                val body = JSONObject(e.response()?.errorBody()?.string().orEmpty())
                val errors = body.optJSONObject("errors")
                val first = errors?.keys()?.asSequence()?.firstOrNull()?.let { errors.optJSONArray(it)?.optString(0) }
                first?.takeIf { it.isNotBlank() } ?: body.optString("message")
            } catch (_: Exception) {
                ""
            }
            if (msg.isNotBlank()) return Exception(msg)
        }
        return e
    }

    private suspend fun <T : Any> call(block: suspend () -> ApiResponse<T>): Result<T> = try {
        val r = block()
        val data = r.data
        if (r.success && data != null) Result.success(data) else Result.failure(Exception(r.message))
    } catch (e: Exception) {
        Result.failure(friendly(e))
    }

    suspend fun show(): Result<SchoolYearDto> = call { api.show() }
    suspend fun start(year: String): Result<SchoolYearDto> = call { api.start(StartSchoolYearRequest(year)) }
    suspend fun complete(dryRun: Boolean): Result<CompleteYearResult> = call { api.complete(dryRun) }
}
