package com.wefit.app.data.remote.dto

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val password_confirmation: String,
    val role: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val status: String,
    val profile_image: String?
)

data class AuthData(
    val user: UserDto,
    val token: String
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)

data class GoogleAuthRequest(val id_token: String)
data class GoogleCompleteRequest(val id_token: String, val role: String)

data class GoogleAuthResult(
    val requires_role: Boolean,
    val token: String? = null,
    val user: UserDto? = null,
    val email: String? = null,
    val name: String? = null
)