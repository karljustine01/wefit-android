package com.wefit.app.data.remote.api

import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.ProfileImageDto
import com.wefit.app.data.remote.dto.UserDto
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ProfileImageApi {
    @Multipart
    @POST("users/profile-image")
    suspend fun upload(@Part image: MultipartBody.Part): ApiResponse<UserDto>

    @GET("users/profile-image")
    suspend fun getImage(): ApiResponse<ProfileImageDto>
}