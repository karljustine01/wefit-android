package com.wefit.app.data.remote.dto

data class ActivityLogDto(
    val id: Int,
    val user_id: Int?,
    val action: String,
    val description: String?,
    val ip_address: String?,
    val created_at: String,
    val user: UserDto?
)