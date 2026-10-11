package com.wefit.app.data.remote.dto

data class AdminAnalyticsDto(
    val total_users: Int,
    val users_by_role: Map<String, Int>,
    val total_sections: Int,
    val total_exercises: Int,
    val total_assignments: Int,
    val completed_sessions: Int
)