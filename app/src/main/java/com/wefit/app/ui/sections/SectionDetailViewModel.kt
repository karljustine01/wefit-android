package com.wefit.app.ui.sections

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.remote.dto.AssignmentDto
import com.wefit.app.data.remote.dto.SectionDetailDto
import com.wefit.app.data.repository.AssignmentRepository
import com.wefit.app.data.repository.SectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SectionDetailUiState(
    val isLoading: Boolean = false,
    val section: SectionDetailDto? = null,
    val assignments: List<AssignmentDto> = emptyList(),
    val error: String? = null
)

class SectionDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val sectionRepo = SectionRepository(application)
    private val assignmentRepo = AssignmentRepository(application)

    private val _uiState = MutableStateFlow(SectionDetailUiState())
    val uiState: StateFlow<SectionDetailUiState> = _uiState.asStateFlow()

    fun load(sectionId: Int) {
        viewModelScope.launch {
            _uiState.value = SectionDetailUiState(isLoading = true)
            val sectionResult = sectionRepo.getSectionDetail(sectionId)
            val allAssignments = assignmentRepo.listAssignments().getOrNull() ?: emptyList()
            val sectionAssignments = allAssignments.filter { it.section_id == sectionId }

            sectionResult.fold(
                onSuccess = { section ->
                    _uiState.value = SectionDetailUiState(section = section, assignments = sectionAssignments)
                },
                onFailure = { e -> _uiState.value = SectionDetailUiState(error = e.message) }
            )
        }
    }
}