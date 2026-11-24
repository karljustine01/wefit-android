package com.wefit.app.data.remote.dto

data class SchoolYearDto(
    val school_year: String?,
    val enrolled_students: Int,
    val registered_enrolled: Int,
    val pending_completion: Int,
    val completed_accounts: Int,
    val completed_retention_days: Int
)

data class StartSchoolYearRequest(val year: String)

data class CompleteYearResult(val dry_run: Boolean, val completed: Int)
