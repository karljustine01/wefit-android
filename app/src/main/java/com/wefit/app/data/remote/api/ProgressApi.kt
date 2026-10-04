package com.wefit.app.data.remote.api

import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.SessionProgressDto
import retrofit2.http.GET
import retrofit2.http.Path

interface ProgressApi {
    @GET("assignments/{id}/progress")
    suspend fun getProgress(@Path("id") assignmentId: Int): ApiResponse<List<SessionProgressDto>>
}