package com.wefit.app.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.remote.dto.AdminAnalyticsDto
import com.wefit.app.data.repository.AnalyticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val isLoading: Boolean = false,
    val data: AdminAnalyticsDto? = null,
    val error: String? = null
)

class AdminDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AnalyticsRepository(application)
    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = AdminDashboardUiState(isLoading = true)
            repository.adminAnalytics().fold(
                onSuccess = { _uiState.value = AdminDashboardUiState(data = it) },
                onFailure = { _uiState.value = AdminDashboardUiState(error = it.message) }
            )
        }
    }
}