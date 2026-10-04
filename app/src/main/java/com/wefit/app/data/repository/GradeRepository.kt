package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.remote.api.CreateGradeRequest
import com.wefit.app.data.remote.api.GradeApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.GradeDto

class GradeRepository(context: Context) {

    private val api = RetrofitClient.getInstance(context).create(GradeApi::class.java)

    suspend fun createGrade(assignmentId: Int, studentId: Int, grade: Double, remarks: String?): Result<GradeDto> = try {
        val response = api.create(CreateGradeRequest(assignmentId, studentId, grade, remarks))
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun studentGrades(studentId: Int): Result<List<GradeDto>> = try {
        val response = api.studentGrades(studentId)
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }
}