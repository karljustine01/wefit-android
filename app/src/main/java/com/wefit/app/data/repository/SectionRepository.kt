package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.api.SectionApi
import com.wefit.app.data.remote.dto.CreateSectionRequest
import com.wefit.app.data.remote.dto.JoinSectionRequest
import com.wefit.app.data.remote.dto.SectionDetailDto
import com.wefit.app.data.remote.dto.SectionDto

class SectionRepository(context: Context) {

    private val api = RetrofitClient.getInstance(context).create(SectionApi::class.java)

    suspend fun createSection(name: String, description: String?): Result<SectionDto> = try {
        val response = api.create(CreateSectionRequest(name, description))
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun listSections(): Result<List<SectionDto>> = try {
        val response = api.list()
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getSectionDetail(id: Int): Result<SectionDetailDto> = try {
        val response = api.get(id)
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun joinSection(code: String): Result<SectionDto> = try {
        val response = api.join(JoinSectionRequest(code))
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun removeMember(sectionId: Int, userId: Int): Result<Unit> = try {
        api.removeMember(sectionId, userId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}