package com.wefit.app.data.remote.api

import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.StudentAnalyticsDto
import com.wefit.app.data.remote.dto.TeacherAnalyticsDto
import com.wefit.app.data.remote.dto.TrendPointDto
import retrofit2.http.GET

interface AnalyticsApi {
    @GET("analytics/student")
    suspend fun student(): ApiResponse<StudentAnalyticsDto>

    @GET("analytics/teacher")
    suspend fun teacher(): ApiResponse<TeacherAnalyticsDto>

    @GET("analytics/student/trend")
    suspend fun studentTrend(): ApiResponse<List<TrendPointDto>>

    @GET("analytics/teacher/trend")
    suspend fun teacherTrend(): ApiResponse<List<TrendPointDto>>
}