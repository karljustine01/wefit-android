package com.wefit.app.data.remote.api

import com.wefit.app.data.remote.dto.AssignmentDto
import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.CreateAssignmentRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AssignmentApi {
    @POST("assignments")
    suspend fun create(@Body request: CreateAssignmentRequest): ApiResponse<AssignmentDto>

    @GET("assignments")
    suspend fun list(): ApiResponse<List<AssignmentDto>>

    @GET("assignments/{id}")
    suspend fun get(@Path("id") id: Int): ApiResponse<AssignmentDto>
}