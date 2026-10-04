package com.wefit.app.ui.sections

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.remote.dto.SectionDetailDto
import com.wefit.app.data.repository.SectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SectionMembersUiState(
    val isLoading: Boolean = false,
    val section: SectionDetailDto? = null,
    val error: String? = null
)

class SectionMembersViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SectionRepository(application)

    private val _uiState = MutableStateFlow(SectionMembersUiState())
    val uiState: StateFlow<SectionMembersUiState> = _uiState.asStateFlow()

    fun load(sectionId: Int) {
        viewModelScope.launch {
            _uiState.value = SectionMembersUiState(isLoading = true)
            repository.getSectionDetail(sectionId).fold(
                onSuccess = { section -> _uiState.value = SectionMembersUiState(section = section) },
                onFailure = { e -> _uiState.value = SectionMembersUiState(error = e.message) }
            )
        }
    }

    fun removeMember(sectionId: Int, userId: Int) {
        viewModelScope.launch {
            repository.removeMember(sectionId, userId)
            load(sectionId)
        }
    }
}