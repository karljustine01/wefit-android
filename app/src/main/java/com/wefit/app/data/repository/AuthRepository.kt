package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.local.TokenManager
import com.wefit.app.data.remote.api.AuthApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.GoogleAuthRequest
import com.wefit.app.data.remote.dto.GoogleCompleteRequest
import com.wefit.app.data.remote.dto.LoginRequest
import com.wefit.app.data.remote.dto.RegisterRequest

sealed class GoogleAuthOutcome {
    data class LoggedIn(val role: String) : GoogleAuthOutcome()
    data class NeedsRole(val idToken: String, val email: String?, val name: String?) : GoogleAuthOutcome()
}

class AuthRepository(private val context: Context) {

    private val authApi = RetrofitClient.getInstance(context).create(AuthApi::class.java)
    private val tokenManager = TokenManager(context)

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response = authApi.login(LoginRequest(email, password))
            if (response.success && response.data != null) {
                tokenManager.saveSession(
                    response.data.token,
                    response.data.user.role,
                    response.data.user.name
                )
                Result.success(response.data.user.role)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<GoogleAuthOutcome> {
        return try {
            val response = authApi.googleAuth(GoogleAuthRequest(idToken))
            if (!response.success || response.data == null) {
                return Result.failure(Exception(response.message))
            }
            val data = response.data
            if (data.requires_role) {
                Result.success(GoogleAuthOutcome.NeedsRole(idToken, data.email, data.name))
            } else if (data.token != null && data.user != null) {
                tokenManager.saveSession(data.token, data.user.role, data.user.name)
                Result.success(GoogleAuthOutcome.LoggedIn(data.user.role))
            } else {
                Result.failure(Exception("Unexpected response from server"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeGoogleRegistration(idToken: String, role: String): Result<String> {
        return try {
            val response = authApi.googleComplete(GoogleCompleteRequest(idToken, role))
            if (response.success && response.data?.token != null && response.data.user != null) {
                tokenManager.saveSession(response.data.token, response.data.user.role, response.data.user.name)
                Result.success(response.data.user.role)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        role: String
    ): Result<String> {
        return try {
            val response = authApi.register(
                RegisterRequest(name, email, password, confirmPassword, role)
            )
            if (response.success && response.data != null) {
                tokenManager.saveSession(
                    response.data.token,
                    response.data.user.role,
                    response.data.user.name
                )
                Result.success(response.data.user.role)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        try {
            authApi.logout()
        } catch (_: Exception) {
            // Even if the network call fails, clear local session
        }
        tokenManager.clearSession()
    }

    suspend fun isLoggedIn(): Boolean = tokenManager.getToken() != null
}