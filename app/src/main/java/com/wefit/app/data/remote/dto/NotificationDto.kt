package com.wefit.app.data.remote.dto

data class NotificationDto(
    val id: Int,
    val title: String,
    val message: String,
    val type: String,
    val is_read: Boolean,
    val created_at: String
)