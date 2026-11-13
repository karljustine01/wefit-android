package com.wefit.app.data.remote.dto

/** One row of the school roster (table `verified_members`). */
data class RosterMemberDto(
    val id: Int,
    val school_id: String,
    val name: String,
    val email: String?,
    val role: String,
    val registered_user_id: Int?,
    val registered_at: String?
) {
    /** True once somebody has used this ID to create an account. */
    val isRegistered: Boolean get() = registered_user_id != null
}

data class AddRosterRequest(
    val school_id: String,
    val name: String,
    val email: String?,
    val role: String
)

data class RosterImportResult(
    val imported: Int,
    val skipped: List<String>
)