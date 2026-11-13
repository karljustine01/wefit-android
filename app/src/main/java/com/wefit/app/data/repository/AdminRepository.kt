package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.remote.api.AdminApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.api.UpdateRoleRequest
import com.wefit.app.data.remote.api.UpdateStatusRequest
import com.wefit.app.data.remote.dto.ActivityLogDto
import com.wefit.app.data.remote.dto.AddRosterRequest
import com.wefit.app.data.remote.dto.RosterImportResult
import com.wefit.app.data.remote.dto.RosterMemberDto
import com.wefit.app.data.remote.dto.UserDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException

class AdminRepository(context: Context) {

    private val api = RetrofitClient.getInstance(context).create(AdminApi::class.java)

    suspend fun listUsers(search: String? = null): Result<List<UserDto>> = try {
        val response = api.listUsers(search)
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateRole(userId: Int, role: String): Result<UserDto> = try {
        val response = api.updateRole(userId, UpdateRoleRequest(role))
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateStatus(userId: Int, status: String): Result<UserDto> = try {
        val response = api.updateStatus(userId, UpdateStatusRequest(status))
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun activityLogs(userId: Int? = null, action: String? = null): Result<List<ActivityLogDto>> = try {
        val response = api.activityLogs(userId, action)
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---------------------------------------------------------------- Roster

    /** Turns a 4xx response (validation errors, 403, ...) into a readable message. */
    private fun serverMessage(e: Exception): Exception {
        if (e is HttpException) {
            val msg = try {
                val body = JSONObject(e.response()?.errorBody()?.string().orEmpty())
                val errors = body.optJSONObject("errors")
                val first = errors?.keys()?.asSequence()?.firstOrNull()
                    ?.let { errors.optJSONArray(it)?.optString(0) }
                first?.takeIf { it.isNotBlank() } ?: body.optString("message")
            } catch (_: Exception) {
                ""
            }
            if (msg.isNotBlank()) return Exception(msg)
        }
        return e
    }

    suspend fun listRoster(role: String? = null, search: String? = null): Result<List<RosterMemberDto>> = try {
        val response = api.listRoster(role, search)
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(serverMessage(e))
    }

    suspend fun addRosterMember(schoolId: String, name: String, email: String?, role: String): Result<RosterMemberDto> = try {
        val response = api.addRosterMember(AddRosterRequest(schoolId, name, email?.ifBlank { null }, role))
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(serverMessage(e))
    }

    suspend fun importRoster(fileName: String, bytes: ByteArray): Result<RosterImportResult> = try {
        val safeName = if (fileName.endsWith(".csv", ignoreCase = true)) fileName else "$fileName.csv"
        val body = bytes.toRequestBody("text/csv".toMediaType())
        val part = MultipartBody.Part.createFormData("file", safeName, body)
        val response = api.importRoster(part)
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(serverMessage(e))
    }

    suspend fun deleteRosterMember(id: Int): Result<Unit> = try {
        val response = api.deleteRosterMember(id)
        if (response.success) Result.success(Unit) else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(serverMessage(e))
    }
}