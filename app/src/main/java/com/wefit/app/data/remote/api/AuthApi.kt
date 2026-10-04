package com.wefit.app.data.remote.api

import com.wefit.app.data.remote.dto.GoogleAuthResult
import com.wefit.app.data.remote.dto.GoogleCompleteRequest
import com.wefit.app.data.remote.dto.GoogleAuthRequest
import com.wefit.app.data.remote.dto.UpdateProfileRequest
import retrofit2.http.PUT
import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.AuthData
import com.wefit.app.data.remote.dto.LoginRequest
import com.wefit.app.data.remote.dto.RegisterRequest
import com.wefit.app.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthData>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthData>

    @POST("auth/logout")
    suspend fun logout(): ApiResponse<Any>

    @GET("users/profile")
    suspend fun getProfile(): ApiResponse<UserDto>

    @PUT("users/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiResponse<UserDto>

    @POST("auth/google")
    suspend fun googleAuth(@Body request: GoogleAuthRequest): ApiResponse<GoogleAuthResult>

    @POST("auth/google/complete")
    suspend fun googleComplete(@Body request: GoogleCompleteRequest): ApiResponse<GoogleAuthResult>
}