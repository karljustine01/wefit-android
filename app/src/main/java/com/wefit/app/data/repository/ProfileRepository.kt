package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.remote.api.AuthApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.UpdateProfileRequest
import com.wefit.app.data.remote.dto.UserDto

class ProfileRepository(context: Context) {

    private val api = RetrofitClient.getInstance(context).create(AuthApi::class.java)

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
}