package com.wefit.app.data.repository

import com.wefit.app.data.remote.dto.ActivityLogDto
import android.content.Context
import com.wefit.app.data.remote.api.AdminApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.api.UpdateRoleRequest
import com.wefit.app.data.remote.api.UpdateStatusRequest
import com.wefit.app.data.remote.dto.UserDto

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
}