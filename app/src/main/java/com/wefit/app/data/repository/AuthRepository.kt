package com.wefit.app.data.repository

import android.content.Context
import com.wefit.app.data.local.TokenManager
import com.wefit.app.data.remote.api.AuthApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.GoogleAuthRequest
import com.wefit.app.data.remote.dto.GoogleCompleteRequest
import com.wefit.app.data.remote.dto.LoginRequest
import com.wefit.app.data.remote.dto.RegisterRequest
import org.json.JSONObject
import retrofit2.HttpException

sealed class GoogleAuthOutcome {
    data class LoggedIn(val role: String) : GoogleAuthOutcome()
    data class NeedsRole(val idToken: String, val email: String?, val name: String?) : GoogleAuthOutcome()
}

class AuthRepository(private val context: Context) {

    private val authApi = RetrofitClient.getInstance(context).create(AuthApi::class.java)
    private val tokenManager = TokenManager(context)

    /** Turns a 4xx response (e.g. "ID not found on the roster") into a readable message. */
    private fun serverMessage(e: Exception): Exception {
        if (e is HttpException) {
            val msg = try {
                val body = JSONObject(e.response()?.errorBody()?.string().orEmpty())
                // Laravel validation errors: {"errors":{"school_id":["..."]}}; our own errors: {"message":"..."}
                val errors = body.optJSONObject("errors")
                val first = errors?.keys()?.asSequence()?.firstOrNull()
                    ?.let { errors.optJSONArray(it)?.optString(0) }
                first?.takeIf { it.isNotBlank() } ?: body.optString("message")
            } catch (_: Exception) {
                ""
            }
            if (msg.isNotBlank()) return Exception(msg)
        }
        return e
    }

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

    suspend fun completeGoogleRegistration(idToken: String, role: String, schoolId: String): Result<String> {
        return try {
            val response = authApi.googleComplete(GoogleCompleteRequest(idToken, role, schoolId))
            if (response.success && response.data?.token != null && response.data.user != null) {
                tokenManager.saveSession(response.data.token, response.data.user.role, response.data.user.name)
                Result.success(response.data.user.role)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(serverMessage(e))
        }
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        role: String,
        schoolId: String
    ): Result<String> {
        return try {
            val response = authApi.register(
                RegisterRequest(name, email, password, confirmPassword, role, schoolId)
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
            Result.failure(serverMessage(e))
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