package com.wefit.app.data.remote.api

import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.CreateSectionRequest
import com.wefit.app.data.remote.dto.JoinRequestDto
import com.wefit.app.data.remote.dto.JoinSectionRequest
import com.wefit.app.data.remote.dto.SectionDetailDto
import com.wefit.app.data.remote.dto.SectionDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SectionApi {
    @POST("sections")
    suspend fun create(@Body request: CreateSectionRequest): ApiResponse<SectionDto>

    @GET("sections")
    suspend fun list(): ApiResponse<List<SectionDto>>

    @GET("sections/{id}")
    suspend fun get(@Path("id") id: Int): ApiResponse<SectionDetailDto>

    @DELETE("sections/{id}")
    suspend fun delete(@Path("id") id: Int): ApiResponse<Any>

    @POST("sections/join")
    suspend fun join(@Body request: JoinSectionRequest): ApiResponse<SectionDto>

    @DELETE("sections/{sectionId}/members/{userId}")
    suspend fun removeMember(@Path("sectionId") sectionId: Int, @Path("userId") userId: Int): ApiResponse<Any>

    // Student: my pending / denied requests
    @GET("sections/requests/mine")
    suspend fun myRequests(): ApiResponse<List<JoinRequestDto>>

    // Teacher: pending requests across all my sections
    @GET("sections/requests/pending")
    suspend fun pendingRequests(): ApiResponse<List<JoinRequestDto>>

    @POST("sections/{sectionId}/requests/{memberId}/approve")
    suspend fun approveRequest(@Path("sectionId") sectionId: Int, @Path("memberId") memberId: Int): ApiResponse<Any>

    @POST("sections/{sectionId}/requests/{memberId}/deny")
    suspend fun denyRequest(@Path("sectionId") sectionId: Int, @Path("memberId") memberId: Int): ApiResponse<Any>
}