package com.wefit.app.ui.assignments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.remote.dto.ExerciseDto
import com.wefit.app.data.remote.dto.SectionDto
import com.wefit.app.data.repository.AssignmentRepository
import com.wefit.app.data.repository.ExerciseRepository
import com.wefit.app.data.repository.SectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateAssignmentUiState(
    val isLoading: Boolean = false,
    val exercises: List<ExerciseDto> = emptyList(),
    val sections: List<SectionDto> = emptyList(),
    val error: String? = null,
    val success: Boolean = false
)

class CreateAssignmentViewModel(application: Application) : AndroidViewModel(application) {

    private val assignmentRepo = AssignmentRepository(application)
    private val exerciseRepo = ExerciseRepository(application)
    private val sectionRepo = SectionRepository(application)

    private val _uiState = MutableStateFlow(CreateAssignmentUiState())
    val uiState: StateFlow<CreateAssignmentUiState> = _uiState.asStateFlow()

    private val _preselectedSectionId = MutableStateFlow<Int?>(null)
    val preselectedSectionId: StateFlow<Int?> = _preselectedSectionId.asStateFlow()

    init {
        loadOptions()
    }

    private fun loadOptions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val exercises = exerciseRepo.listExercises().getOrNull() ?: emptyList()
            val sections = sectionRepo.listSections().getOrNull() ?: emptyList()
            _uiState.value = _uiState.value.copy(isLoading = false, exercises = exercises, sections = sections)
        }
    }

    fun preselectSection(sectionId: Int?) {
        if (sectionId == null) return
        val match = _uiState.value.sections.find { it.id == sectionId }
        if (match != null) {
            _preselectedSectionId.value = sectionId
        }
    }

    fun createAssignment(exerciseId: Int, sectionId: Int?, targetValue: Double?, deadline: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            assignmentRepo.createAssignment(exerciseId, sectionId, null, targetValue, deadline).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, success = true) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
            )
        }
    }
}