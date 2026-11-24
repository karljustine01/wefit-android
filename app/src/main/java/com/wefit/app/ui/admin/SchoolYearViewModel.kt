package com.wefit.app.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.remote.dto.SchoolYearDto
import com.wefit.app.data.repository.SchoolYearRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SchoolYearUiState(
    val isLoading: Boolean = false,
    val data: SchoolYearDto? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class SchoolYearViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SchoolYearRepository(application)
    private val _uiState = MutableStateFlow(SchoolYearUiState())
    val uiState: StateFlow<SchoolYearUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.show().fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, data = it) },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
            )
        }
    }

    fun startYear(year: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = null, error = null)
            repository.start(year.trim()).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        busy = false, data = it,
                        message = "School year ${it.school_year} started. Now import the new PE roster."
                    )
                },
                onFailure = { _uiState.value = _uiState.value.copy(busy = false, error = it.message) }
            )
        }
    }

    fun completeYear() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = null, error = null)
            repository.complete(false).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(busy = false, message = "${it.completed} student account(s) closed")
                    load()
                },
                onFailure = { _uiState.value = _uiState.value.copy(busy = false, error = it.message) }
            )
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}
