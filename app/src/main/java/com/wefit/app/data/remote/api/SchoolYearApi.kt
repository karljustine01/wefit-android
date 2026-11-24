package com.wefit.app.data.remote.api

import com.wefit.app.data.remote.dto.ApiResponse
import com.wefit.app.data.remote.dto.CompleteYearResult
import com.wefit.app.data.remote.dto.SchoolYearDto
import com.wefit.app.data.remote.dto.StartSchoolYearRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SchoolYearApi {
    @GET("admin/school-year")
    suspend fun show(): ApiResponse<SchoolYearDto>

    @POST("admin/school-year/start")
    suspend fun start(@Body request: StartSchoolYearRequest): ApiResponse<SchoolYearDto>

    @POST("admin/school-year/complete")
    suspend fun complete(@Query("dry_run") dryRun: Boolean = false): ApiResponse<CompleteYearResult>
}
