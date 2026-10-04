package com.wefit.app.data.remote.dto

data class SectionMemberDto(
    val id: Int,
    val user_id: Int,
    val user: UserDto?
)

data class SectionDetailDto(
    val id: Int,
    val name: String,
    val description: String?,
    val section_code: String,
    val created_by: Int,
    val members: List<SectionMemberDto>?
)