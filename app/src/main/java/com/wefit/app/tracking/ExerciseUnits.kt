package com.wefit.app.tracking

object ExerciseUnits {
    /** Unit a teacher's target is measured in, or null when a target does not apply. */
    fun targetUnit(exerciseType: String): String? = when (exerciseType) {
        "plank", "stork_balance", "quadrant_agility" -> "seconds"
        "running" -> "meters"
        "walking" -> "steps"
        "50m_dash", "mile_walk", "sit_and_reach", "vertical_jump", "project" -> null
        else -> "reps"
    }
}