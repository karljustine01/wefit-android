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