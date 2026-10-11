package com.wefit.app.ui.assignments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.remote.dto.ProgressLogDto
import com.wefit.app.data.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionCurveUiState(
    val isLoading: Boolean = false,
    val logs: List<ProgressLogDto> = emptyList(),
    val status: String? = null,
    val error: String? = null
)

class SessionCurveViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProgressRepository(application)

    private val _uiState = MutableStateFlow(SessionCurveUiState())
    val uiState: StateFlow<SessionCurveUiState> = _uiState.asStateFlow()

    fun load(sessionId: Int) {
        viewModelScope.launch {
            _uiState.value = SessionCurveUiState(isLoading = true)
            repository.getSessionDetail(sessionId).fold(
                onSuccess = { s ->
                    _uiState.value = SessionCurveUiState(
                        logs = s.progress_logs.orEmpty().sortedBy { it.recorded_at },
                        status = s.status
                    )
                },
                onFailure = { e -> _uiState.value = SessionCurveUiState(error = e.message) }
            )
        }
    }
}