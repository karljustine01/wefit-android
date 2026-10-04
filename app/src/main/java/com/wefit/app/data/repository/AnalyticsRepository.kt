package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.remote.api.AnalyticsApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.StudentAnalyticsDto
import com.wefit.app.data.remote.dto.TeacherAnalyticsDto
import com.wefit.app.data.remote.dto.TrendPointDto

class AnalyticsRepository(context: Context) {

    private val api = RetrofitClient.getInstance(context).create(AnalyticsApi::class.java)

    suspend fun studentAnalytics(): Result<StudentAnalyticsDto> = try {
        val response = api.student()
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun teacherAnalytics(): Result<TeacherAnalyticsDto> = try {
        val response = api.teacher()
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun studentTrend(): Result<List<TrendPointDto>> = try {
        val response = api.studentTrend()
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun teacherTrend(): Result<List<TrendPointDto>> = try {
        val response = api.teacherTrend()
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }
}