package com.wefit.app.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.repository.AuthRepository
import com.wefit.app.data.repository.GoogleAuthOutcome
import com.wefit.app.data.repository.GoogleAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedInRole: String? = null,
    val googleNeedsRole: Boolean = false,
    val googlePendingIdToken: String? = null,
    val googlePendingEmail: String? = null,
    val googlePendingName: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = repository.login(email, password)
            result.fold(
                onSuccess = { role -> _uiState.value = AuthUiState(loggedInRole = role) },
                onFailure = { e -> _uiState.value = AuthUiState(error = e.message) }
            )
        }
    }

    fun loginWithGoogle(activityContext: android.content.Context) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val googleRepo = GoogleAuthRepository(activityContext)
            googleRepo.getGoogleIdToken().fold(
                onSuccess = { idToken ->
                    repository.loginWithGoogle(idToken).fold(
                        onSuccess = { outcome ->
                            when (outcome) {
                                is GoogleAuthOutcome.LoggedIn ->
                                    _uiState.value = AuthUiState(loggedInRole = outcome.role)
                                is GoogleAuthOutcome.NeedsRole ->
                                    _uiState.value = AuthUiState(
                                        googleNeedsRole = true,
                                        googlePendingIdToken = outcome.idToken,
                                        googlePendingEmail = outcome.email,
                                        googlePendingName = outcome.name
                                    )
                            }
                        },
                        onFailure = { e -> _uiState.value = AuthUiState(error = e.message) }
                    )
                },
                onFailure = { e -> _uiState.value = AuthUiState(error = e.message) }
            )
        }
    }

    fun completeGoogleRole(role: String) {
        val idToken = _uiState.value.googlePendingIdToken ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.completeGoogleRegistration(idToken, role).fold(
                onSuccess = { r -> _uiState.value = AuthUiState(loggedInRole = r) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
            )
        }
    }

    fun cancelGoogleRoleSelection() {
        _uiState.value = AuthUiState()
    }

    fun register(name: String, email: String, password: String, confirmPassword: String, role: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = repository.register(name, email, password, confirmPassword, role)
            result.fold(
                onSuccess = { r -> _uiState.value = AuthUiState(loggedInRole = r) },
                onFailure = { e -> _uiState.value = AuthUiState(error = e.message) }
            )
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onComplete()
        }
    }

    fun resetError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}