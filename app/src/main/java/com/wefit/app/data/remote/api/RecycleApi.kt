package com.wefit.app.data.remote.api

import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.RecycleBinDto
import com.wefit.app.data.remote.dto.RecycleRunResultDto
import com.wefit.app.data.remote.dto.RecycleStatsDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RecycleApi {
    @GET("recycle-bin")
    suspend fun bin(): ApiResponse<RecycleBinDto>

    @POST("recycle-bin/{type}/{id}/restore")
    suspend fun restore(@Path("type") type: String, @Path("id") id: Int): ApiResponse<Any>

    @DELETE("recycle-bin/{type}/{id}")
    suspend fun deleteForever(@Path("type") type: String, @Path("id") id: Int): ApiResponse<Any>

    @DELETE("recycle-bin")
    suspend fun emptyBin(): ApiResponse<Any>

    @GET("admin/recycling/stats")
    suspend fun stats(): ApiResponse<RecycleStatsDto>

    @POST("admin/recycling/run")
    suspend fun runCleanup(@Query("dry_run") dryRun: Boolean = false): ApiResponse<RecycleRunResultDto>
}
