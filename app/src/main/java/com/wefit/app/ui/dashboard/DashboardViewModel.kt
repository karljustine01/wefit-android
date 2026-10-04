package com.wefit.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.remote.api.HealthApi
import com.wefit.app.data.remote.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val connected: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val healthApi = RetrofitClient.getInstance(application).create(HealthApi::class.java)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        checkBackend()
    }

    fun checkBackend() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState(isLoading = true)
            try {
                val response = healthApi.checkHealth()
                _uiState.value = DashboardUiState(connected = true, message = response.message)
            } catch (e: Exception) {
                _uiState.value = DashboardUiState(error = e.message ?: "Unknown error")
            }
        }
    }
}