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
    val section: SectionDto?,
    /** True when the teacher gave this student their own target or a different exercise. */
    val is_adjusted: Boolean? = null
)

/** Per-student accommodation sent when creating an assignment. */
data class StudentOverrideDto(
    val user_id: Int,
    val excluded: Boolean = false,
    val target_value: Double? = null,
    val alternate_exercise_id: Int? = null,
    /** Private note for the teacher (e.g. "weak lungs"); never shown to students. */
    val note: String? = null
)

data class CreateAssignmentRequest(
    val exercise_id: Int,
    val section_id: Int?,
    val assigned_to: Int?,
    val target_value: Double?,
    val deadline: String?,
    val overrides: List<StudentOverrideDto>? = null
)