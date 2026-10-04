package com.wefit.app.data.remote.dto

data class ExerciseSessionDto(
    val id: Int,
    val assignment_id: Int,
    val user_id: Int,
    val started_at: String?,
    val completed_at: String?,
    val duration: Int?,
    val repetitions: Int?,
    val distance: Double?,
    val progress_percentage: Double,
    val status: String,
    val tracking_method: String?
)

data class StartSessionRequest(
    val assignment_id: Int,
    val tracking_method: String
)

data class UpdateProgressRequest(
    val progress_percentage: Double,
    val repetitions: Int?,
    val distance: Double?,
    val duration: Int?,
    val latitude: Double?,
    val longitude: Double?
)

data class CompleteSessionRequest(
    val duration: Int,
    val repetitions: Int?,
    val distance: Double?,
    val progress_percentage: Double
)