package com.wefit.app.data.remote.api

import retrofit2.http.GET

interface HealthApi {
    @GET("health")
    suspend fun checkHealth(): HealthResponse
}

data class HealthResponse(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any>?
)