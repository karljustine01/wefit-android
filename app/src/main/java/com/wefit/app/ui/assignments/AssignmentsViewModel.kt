package com.wefit.app.ui.assignments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.remote.dto.AssignmentDto
import com.wefit.app.data.repository.AssignmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AssignmentsUiState(
    val isLoading: Boolean = false,
    val assignments: List<AssignmentDto> = emptyList(),
    val error: String? = null
)

class AssignmentsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AssignmentRepository(application)

    private val _uiState = MutableStateFlow(AssignmentsUiState())
    val uiState: StateFlow<AssignmentsUiState> = _uiState.asStateFlow()

    init {
        loadAssignments()
    }

    fun loadAssignments() {
        viewModelScope.launch {
            _uiState.value = AssignmentsUiState(isLoading = true)
            repository.listAssignments().fold(
                onSuccess = { list -> _uiState.value = AssignmentsUiState(assignments = list) },
                onFailure = { e -> _uiState.value = AssignmentsUiState(error = e.message) }
            )
        }
    }
}