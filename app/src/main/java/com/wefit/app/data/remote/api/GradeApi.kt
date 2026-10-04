package com.wefit.app.data.remote.api

import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.GradeDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GradeApi {
    @POST("grades")
    suspend fun create(@Body request: CreateGradeRequest): ApiResponse<GradeDto>

    @GET("grades/student/{id}")
    suspend fun studentGrades(@Path("id") studentId: Int): ApiResponse<List<GradeDto>>
}

data class CreateGradeRequest(
    val assignment_id: Int,
    val student_id: Int,
    val grade: Double,
    val remarks: String?
)