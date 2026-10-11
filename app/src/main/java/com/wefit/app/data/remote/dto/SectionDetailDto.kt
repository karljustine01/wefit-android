package com.wefit.app.data.remote.dto

data class SectionDetailDto(
    val id: Int,
    val name: String? = null,
    val description: String? = null,
    val section_code: String? = null,
    val members: List<SectionMemberDto>? = emptyList()
)

data class SectionMemberDto(
    val id: Int,
    val user_id: Int? = null,
    val user: UserDto? = null
)