package com.wefit.app.data.remote.api

import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.CreateExerciseRequest
import com.wefit.app.data.remote.dto.ExerciseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ExerciseApi {
    @POST("exercises")
    suspend fun create(@Body request: CreateExerciseRequest): ApiResponse<ExerciseDto>

    @GET("exercises")
    suspend fun list(@Query("search") search: String? = null): ApiResponse<List<ExerciseDto>>

    @GET("exercises/{id}")
    suspend fun get(@Path("id") id: Int): ApiResponse<ExerciseDto>
}