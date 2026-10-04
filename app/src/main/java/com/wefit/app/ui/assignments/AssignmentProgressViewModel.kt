package com.wefit.app.ui.assignments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.remote.dto.SessionProgressDto
import com.wefit.app.data.repository.GradeRepository
import com.wefit.app.data.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AssignmentProgressUiState(
    val isLoading: Boolean = false,
    val sessions: List<SessionProgressDto> = emptyList(),
    val error: String? = null,
    val gradeSuccessUserId: Int? = null
)

class AssignmentProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val progressRepo = ProgressRepository(application)
    private val gradeRepo = GradeRepository(application)

    private val _uiState = MutableStateFlow(AssignmentProgressUiState())
    val uiState: StateFlow<AssignmentProgressUiState> = _uiState.asStateFlow()

    fun load(assignmentId: Int) {
        viewModelScope.launch {
            _uiState.value = AssignmentProgressUiState(isLoading = true)
            progressRepo.getProgress(assignmentId).fold(
                onSuccess = { sessions -> _uiState.value = AssignmentProgressUiState(sessions = sessions) },
                onFailure = { e -> _uiState.value = AssignmentProgressUiState(error = e.message) }
            )
        }
    }

    fun postGrade(assignmentId: Int, studentId: Int, grade: Double, remarks: String?) {
        viewModelScope.launch {
            gradeRepo.createGrade(assignmentId, studentId, grade, remarks).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(gradeSuccessUserId = studentId)
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }

    fun clearGradeSuccess() {
        _uiState.value = _uiState.value.copy(gradeSuccessUserId = null)
    }
}