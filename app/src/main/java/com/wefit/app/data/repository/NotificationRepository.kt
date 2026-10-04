package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.remote.api.NotificationApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.NotificationDto

class NotificationRepository(context: Context) {

    private val api = RetrofitClient.getInstance(context).create(NotificationApi::class.java)

    suspend fun listNotifications(): Result<List<NotificationDto>> = try {
        val response = api.list()
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun markRead(id: Int): Result<Unit> = try {
        api.markRead(id)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun markAllRead(): Result<Unit> = try {
        api.markAllRead()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}