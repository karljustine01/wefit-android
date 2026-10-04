package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.remote.api.AssignmentApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.AssignmentDto
import com.wefit.app.data.remote.dto.CreateAssignmentRequest

class AssignmentRepository(context: Context) {

    private val api = RetrofitClient.getInstance(context).create(AssignmentApi::class.java)

    suspend fun listAssignments(): Result<List<AssignmentDto>> = try {
        val response = api.list()
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun createAssignment(
        exerciseId: Int,
        sectionId: Int?,
        assignedTo: Int?,
        targetValue: Double?,
        deadline: String?
    ): Result<AssignmentDto> = try {
        val response = api.create(CreateAssignmentRequest(exerciseId, sectionId, assignedTo, targetValue, deadline))
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }
}