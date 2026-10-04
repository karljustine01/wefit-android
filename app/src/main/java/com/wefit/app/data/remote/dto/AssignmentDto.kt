package com.wefit.app.data.remote.dto

data class AssignmentDto(
    val id: Int,
    val exercise_id: Int,
    val section_id: Int?,
    val assigned_by: Int,
    val assigned_to: Int?,
    val target_value: Double?,
    val deadline: String?,
    val status: String,
    val exercise: ExerciseDto?,
    val section: SectionDto?
)

data class CreateAssignmentRequest(
    val exercise_id: Int,
    val section_id: Int?,
    val assigned_to: Int?,
    val target_value: Double?,
    val deadline: String?
)