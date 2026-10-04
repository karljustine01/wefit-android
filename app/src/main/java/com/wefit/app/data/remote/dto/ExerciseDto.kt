package com.wefit.app.data.remote.dto

data class ExerciseDto(
    val id: Int,
    val name: String,
    val description: String?,
    val instructions: String?,
    val exercise_type: String,
    val target_value: Double?,
    val target_unit: String?,
    val tracking_method: String?,
    val difficulty: String
)

data class CreateExerciseRequest(
    val name: String,
    val description: String?,
    val instructions: String?,
    val exercise_type: String,
    val target_value: Double?,
    val target_unit: String?,
    val tracking_method: String?,
    val difficulty: String
)