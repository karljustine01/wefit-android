package com.wefit.app.ui.sections

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.remote.dto.SectionDto
import com.wefit.app.data.repository.SectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SectionsUiState(
    val isLoading: Boolean = false,
    val sections: List<SectionDto> = emptyList(),
    val error: String? = null,
    val actionMessage: String? = null
)

class SectionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SectionRepository(application)

    private val _uiState = MutableStateFlow(SectionsUiState())
    val uiState: StateFlow<SectionsUiState> = _uiState.asStateFlow()

    init {
        loadSections()
    }

    fun loadSections() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.listSections().fold(
                onSuccess = { sections -> _uiState.value = SectionsUiState(sections = sections) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
            )
        }
    }

    fun createSection(name: String, description: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.createSection(name, description).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(actionMessage = "Section created")
                    loadSections()
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
            )
        }
    }

    fun joinSection(code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.joinSection(code).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(actionMessage = "Joined section")
                    loadSections()
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, actionMessage = null)
    }
}