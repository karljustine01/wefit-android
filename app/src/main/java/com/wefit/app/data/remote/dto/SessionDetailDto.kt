package com.wefit.app.data.remote.dto

data class ProgressLogDto(
    val id: Int,
    val progress_percentage: Double?,
    val repetitions: Int?,
    val distance: Double?,
    val duration: Int?,
    val recorded_at: String?
)

data class SessionDetailDto(
    val id: Int,
    val status: String,
    val progress_logs: List<ProgressLogDto>?
)