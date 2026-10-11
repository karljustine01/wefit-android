package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.remote.api.AuthApi
import com.wefit.app.data.remote.api.ProfileImageApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.UpdateProfileRequest
import com.wefit.app.data.remote.dto.UserDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProfileRepository(context: Context) {

    private val retrofit = RetrofitClient.getInstance(context)
    private val api = retrofit.create(AuthApi::class.java)
    private val imageApi = retrofit.create(ProfileImageApi::class.java)

    suspend fun getProfile(): Result<UserDto> = try {
        val response = api.getProfile()
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateProfile(name: String): Result<UserDto> = try {
        val response = api.updateProfile(UpdateProfileRequest(name))
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getProfileImage(): Result<String?> = try {
        val response = imageApi.getImage()
        if (response.success) Result.success(response.data?.image)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun uploadProfileImage(bytes: ByteArray): Result<UserDto> = try {
        val body = bytes.toRequestBody("image/jpeg".toMediaType())
        val part = MultipartBody.Part.createFormData("image", "profile.jpg", body)
        val response = imageApi.upload(part)
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }
}