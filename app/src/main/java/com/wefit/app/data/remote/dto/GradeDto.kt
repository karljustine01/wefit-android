package com.wefit.app.data.remote.dto

data class GradeDto(
    val id: Int,
    val assignment_id: Int,
    val student_id: Int,
    val teacher_id: Int,
    val grade: Double,
    val remarks: String?,
    val graded_at: String?,
    val assignment: AssignmentDto?
)