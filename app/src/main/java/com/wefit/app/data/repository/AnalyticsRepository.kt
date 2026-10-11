package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.remote.api.AnalyticsApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.AdminAnalyticsDto
import com.wefit.app.data.remote.dto.ExerciseStatDto
import com.wefit.app.data.remote.dto.GradeBucketDto
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

    suspend fun adminAnalytics(): Result<AdminAnalyticsDto> = try {
        val response = api.admin()
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

    suspend fun exerciseBreakdown(): Result<List<ExerciseStatDto>> = try {
        val response = api.exerciseBreakdown()
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun gradeDistribution(): Result<List<GradeBucketDto>> = try {
        val response = api.gradeDistribution()
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }
}