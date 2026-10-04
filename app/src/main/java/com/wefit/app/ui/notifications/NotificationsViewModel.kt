package com.wefit.app.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.remote.dto.NotificationDto
import com.wefit.app.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val notifications: List<NotificationDto> = emptyList(),
    val error: String? = null
)

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NotificationRepository(application)

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState(isLoading = true)
            repository.listNotifications().fold(
                onSuccess = { list -> _uiState.value = NotificationsUiState(notifications = list) },
                onFailure = { e -> _uiState.value = NotificationsUiState(error = e.message) }
            )
        }
    }

    fun markRead(id: Int) {
        viewModelScope.launch {
            repository.markRead(id)
            load()
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            repository.markAllRead()
            load()
        }
    }
}