package com.wefit.app.data.remote.api

import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.UserDto
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.Query

interface AdminApi {
    @GET("admin/users")
    suspend fun listUsers(@Query("search") search: String? = null): ApiResponse<List<UserDto>>

    @PUT("admin/users/{id}/role")
    suspend fun updateRole(@Path("id") id: Int, @Body request: UpdateRoleRequest): ApiResponse<UserDto>

    @PUT("admin/users/{id}/status")
    suspend fun updateStatus(@Path("id") id: Int, @Body request: UpdateStatusRequest): ApiResponse<UserDto>
}

data class UpdateRoleRequest(val role: String)
data class UpdateStatusRequest(val status: String)