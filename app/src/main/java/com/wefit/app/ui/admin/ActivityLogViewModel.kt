package com.wefit.app.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.remote.dto.ActivityLogDto
import com.wefit.app.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActivityLogUiState(
    val isLoading: Boolean = false,
    val logs: List<ActivityLogDto> = emptyList(),
    val error: String? = null
)

class ActivityLogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AdminRepository(application)

    private val _uiState = MutableStateFlow(ActivityLogUiState())
    val uiState: StateFlow<ActivityLogUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load(search: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.activityLogs(action = search?.ifBlank { null }).fold(
                onSuccess = { logs -> _uiState.value = ActivityLogUiState(logs = logs) },
                onFailure = { e -> _uiState.value = ActivityLogUiState(error = e.message) }
            )
        }
    }
}