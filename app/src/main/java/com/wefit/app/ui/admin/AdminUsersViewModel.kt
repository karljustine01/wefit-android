package com.wefit.app.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.remote.dto.UserDto
import com.wefit.app.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUsersUiState(
    val isLoading: Boolean = false,
    val users: List<UserDto> = emptyList(),
    val error: String? = null
)

class AdminUsersViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AdminRepository(application)

    private val _uiState = MutableStateFlow(AdminUsersUiState())
    val uiState: StateFlow<AdminUsersUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load(search: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.listUsers(search).fold(
                onSuccess = { users -> _uiState.value = AdminUsersUiState(users = users) },
                onFailure = { e -> _uiState.value = AdminUsersUiState(error = e.message) }
            )
        }
    }

    fun updateRole(userId: Int, role: String) {
        viewModelScope.launch {
            repository.updateRole(userId, role)
            load()
        }
    }

    fun updateStatus(userId: Int, status: String) {
        viewModelScope.launch {
            repository.updateStatus(userId, status)
            load()
        }
    }
}