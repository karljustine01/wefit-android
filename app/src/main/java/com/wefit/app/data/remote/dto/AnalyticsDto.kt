package com.wefit.app.data.remote.dto

data class StudentAnalyticsDto(
    val total_assignments: Int,
    val completed_sessions: Int,
    val average_grade: Double?,
    val total_grades: Int
)

data class TeacherAnalyticsDto(
    val total_sections: Int,
    val total_students: Int,
    val total_assignments: Int,
    val completed_sessions: Int,
    val pending_grades: Int
)

data class TrendPointDto(
    val week: String,
    val count: Int
)