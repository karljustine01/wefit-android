package com.wefit.app.ui.recycle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.remote.dto.RecycleBinDto
import com.wefit.app.data.remote.dto.RecycleStatsDto
import com.wefit.app.data.repository.RecycleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecycleUiState(
    val isLoading: Boolean = false,
    val bin: RecycleBinDto? = null,
    val stats: RecycleStatsDto? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class RecycleBinViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecycleRepository(application)
    private val _uiState = MutableStateFlow(RecycleUiState())
    val uiState: StateFlow<RecycleUiState> = _uiState.asStateFlow()

    fun load(isAdmin: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val bin = repository.bin()
            val stats = if (isAdmin) repository.stats().getOrNull() else null
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                bin = bin.getOrNull() ?: _uiState.value.bin,
                stats = stats ?: _uiState.value.stats,
                error = bin.exceptionOrNull()?.message
            )
        }
    }

    private fun act(isAdmin: Boolean, done: String, block: suspend () -> Result<*>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = null, error = null)
            block().fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(busy = false, message = done)
                    load(isAdmin)
                },
                onFailure = { _uiState.value = _uiState.value.copy(busy = false, error = it.message) }
            )
        }
    }

    fun restore(type: String, id: Int, isAdmin: Boolean) =
        act(isAdmin, "Restored") { repository.restore(type, id) }

    fun deleteForever(type: String, id: Int, isAdmin: Boolean) =
        act(isAdmin, "Permanently deleted") { repository.deleteForever(type, id) }

    fun emptyBin(isAdmin: Boolean) =
        act(isAdmin, "Recycle bin emptied") { repository.emptyBin() }

    fun runCleanup(isAdmin: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = null, error = null)
            repository.runCleanup(false).fold(
                onSuccess = {
                    val mb = "%.1f".format(it.bytes_freed / 1048576.0)
                    _uiState.value = _uiState.value.copy(
                        busy = false,
                        message = "Cleanup done: ${it.total_rows} rows removed, $mb MB of video freed"
                    )
                    load(isAdmin)
                },
                onFailure = { _uiState.value = _uiState.value.copy(busy = false, error = it.message) }
            )
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}
