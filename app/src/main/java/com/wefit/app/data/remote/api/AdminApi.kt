package com.wefit.app.data.remote.api

import com.wefit.app.data.remote.dto.ActivityLogDto
import com.wefit.app.data.remote.dto.AddRosterRequest
import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.RosterImportResult
import com.wefit.app.data.remote.dto.RosterMemberDto
import com.wefit.app.data.remote.dto.UserDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApi {
    @GET("admin/users")
    suspend fun listUsers(@Query("search") search: String? = null): ApiResponse<List<UserDto>>

    @PUT("admin/users/{id}/role")
    suspend fun updateRole(@Path("id") id: Int, @Body request: UpdateRoleRequest): ApiResponse<UserDto>

    @PUT("admin/users/{id}/status")
    suspend fun updateStatus(@Path("id") id: Int, @Body request: UpdateStatusRequest): ApiResponse<UserDto>

    @GET("admin/activity-logs")
    suspend fun activityLogs(
        @Query("user_id") userId: Int? = null,
        @Query("action") action: String? = null
    ): ApiResponse<List<ActivityLogDto>>

    // ---- School roster (who is allowed to register) ----
    @GET("admin/roster")
    suspend fun listRoster(
        @Query("role") role: String? = null,
        @Query("search") search: String? = null
    ): ApiResponse<List<RosterMemberDto>>

    @POST("admin/roster")
    suspend fun addRosterMember(@Body request: AddRosterRequest): ApiResponse<RosterMemberDto>

    @Multipart
    @POST("admin/roster/import")
    suspend fun importRoster(@Part file: MultipartBody.Part): ApiResponse<RosterImportResult>

    @DELETE("admin/roster/{id}")
    suspend fun deleteRosterMember(@Path("id") id: Int): ApiResponse<Any>
}

data class UpdateRoleRequest(val role: String)
data class UpdateStatusRequest(val status: String)