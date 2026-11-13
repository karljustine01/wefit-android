package com.wefit.app.data.remote.dto

data class SectionDto(
    val id: Int,
    val name: String,
    val description: String?,
    val section_code: String,
    val created_by: Int,
    val members_count: Int?
)

data class CreateSectionRequest(
    val name: String,
    val description: String?
)

data class JoinSectionRequest(
    val section_code: String
)

data class SectionRefDto(
    val id: Int,
    val name: String? = null,
    val section_code: String? = null
)

/** A join request (section_members row with status PENDING / DENIED). */
data class JoinRequestDto(
    val id: Int,
    val section_id: Int,
    val user_id: Int,
    val status: String,
    val requested_at: String? = null,
    val user: UserDto? = null,
    val section: SectionRefDto? = null
)