package com.wefit.app.data.remote.api

import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.CompleteSessionRequest
import com.wefit.app.data.remote.dto.ExerciseSessionDto
import com.wefit.app.data.remote.dto.StartSessionRequest
import com.wefit.app.data.remote.dto.UpdateProgressRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ExerciseSessionApi {
    @POST("exercise-sessions/start")
    suspend fun start(@Body request: StartSessionRequest): ApiResponse<ExerciseSessionDto>

    @POST("exercise-sessions/{id}/progress")
    suspend fun progress(@Path("id") id: Int, @Body request: UpdateProgressRequest): ApiResponse<ExerciseSessionDto>

    @POST("exercise-sessions/{id}/complete")
    suspend fun complete(@Path("id") id: Int, @Body request: CompleteSessionRequest): ApiResponse<ExerciseSessionDto>

    @GET("exercise-sessions")
    suspend fun history(): ApiResponse<List<ExerciseSessionDto>>
}