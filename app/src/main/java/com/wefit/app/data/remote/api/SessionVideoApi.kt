package com.wefit.app.data.remote.api

import com.wefit.app.data.remote.dto.ApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

interface SessionVideoApi {
    @Multipart
    @POST("exercise-sessions/{id}/video")
    suspend fun upload(
        @Path("id") sessionId: Int,
        @Part video: MultipartBody.Part,
        @Part("duration") duration: RequestBody
    ): ApiResponse<Map<String, Any>>

    @Streaming
    @GET("exercise-sessions/{id}/video")
    suspend fun download(@Path("id") sessionId: Int): Response<ResponseBody>
}
