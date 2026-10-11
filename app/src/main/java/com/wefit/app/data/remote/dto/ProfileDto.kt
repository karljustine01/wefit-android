package com.wefit.app.data.remote.dto

data class UpdateProfileRequest(
    val name: String
)

data class ProfileImageDto(
    val image: String?
)