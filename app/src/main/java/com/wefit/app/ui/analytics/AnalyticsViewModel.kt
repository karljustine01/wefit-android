package com.wefit.app.ui.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.local.TokenManager
import com.wefit.app.data.remote.dto.ExerciseStatDto
import com.wefit.app.data.remote.dto.GradeBucketDto
import com.wefit.app.data.remote.dto.StudentAnalyticsDto
import com.wefit.app.data.remote.dto.TeacherAnalyticsDto
import com.wefit.app.data.remote.dto.TrendPointDto
import com.wefit.app.data.repository.AnalyticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val studentData: StudentAnalyticsDto? = null,
    val teacherData: TeacherAnalyticsDto? = null,
    val trend: List<TrendPointDto> = emptyList(),
    val exerciseStats: List<ExerciseStatDto> = emptyList(),
    val gradeBuckets: List<GradeBucketDto> = emptyList(),
    val role: String? = null,
    val error: String? = null
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AnalyticsRepository(application)
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = AnalyticsUiState(isLoading = true)
            val role = tokenManager.roleFlow.first()
            val isStaff = role in listOf("TEACHER", "COACH", "ADMIN")

            val trend = (if (isStaff) repository.teacherTrend() else repository.studentTrend()).getOrNull() ?: emptyList()
            val stats = repository.exerciseBreakdown().getOrNull() ?: emptyList()
            val buckets = repository.gradeDistribution().getOrNull() ?: emptyList()

            if (isStaff) {
                repository.teacherAnalytics().fold(
                    onSuccess = { data ->
                        _uiState.value = AnalyticsUiState(
                            teacherData = data,
                            trend = trend,
                            exerciseStats = stats,
                            gradeBuckets = buckets,
                            role = role
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = AnalyticsUiState(error = e.message, role = role)
                    }
                )
            } else {
                repository.studentAnalytics().fold(
                    onSuccess = { data ->
                        _uiState.value = AnalyticsUiState(
                            studentData = data,
                            trend = trend,
                            exerciseStats = stats,
                            gradeBuckets = buckets,
                            role = role
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = AnalyticsUiState(error = e.message, role = role)
                    }
                )
            }
        }
    }
}