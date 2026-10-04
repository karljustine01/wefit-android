package com.wefit.app.data.remote.api

import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.NotificationDto
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface NotificationApi {
    @GET("notifications")
    suspend fun list(): ApiResponse<List<NotificationDto>>

    @PUT("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Int): ApiResponse<NotificationDto>

    @PUT("notifications/read-all")
    suspend fun markAllRead(): ApiResponse<Any>
}