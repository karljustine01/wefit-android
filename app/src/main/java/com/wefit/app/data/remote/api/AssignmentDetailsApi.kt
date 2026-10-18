package com.wefit.app.data.remote.api

import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.AssignmentDetailsDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface AssignmentDetailsApi {
    @Multipart
    @POST("assignments/{id}/details")
    suspend fun save(
        @Path("id") id: Int,
        @Part("instructions") instructions: RequestBody?,
        @Part media: MultipartBody.Part?
    ): ApiResponse<AssignmentDetailsDto>

    @GET("assignments/{id}/details")
    suspend fun get(@Path("id") id: Int): ApiResponse<AssignmentDetailsDto>
}