package com.wefit.app.data.remote.dto

data class SessionProgressDto(
    val id: Int,
    val user_id: Int,
    val status: String,
    val repetitions: Int?,
    val distance: Double?,
    val progress_percentage: Double,
    val user: UserDto?
)