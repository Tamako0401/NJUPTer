package com.example.njupter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.njupter.data.CourseInfo
import com.example.njupter.data.CourseSession
import com.example.njupter.data.TimetableMetadata
import com.example.njupter.data.TimetableRepository
import com.example.njupter.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimetableUiState(
    val courseInfos: List<CourseInfo> = emptyList(),
    val sessions: List<CourseSession> = emptyList(),
    val isLoading: Boolean = false,
    val timetables: List<TimetableMetadata> = emptyList(),
    val currentTimetableName: String = "",
    val currentTimetableId: String? = null,
    val currentStartDate: Long = System.currentTimeMillis(),
    val currentTotalWeeks: Int = 20,
    val showWeekends: Boolean = false
)

class TimetableViewModel(
    private val repository: TimetableRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<TimetableUiState> = combine(
        combine(
            repository.getCourseInfos(),
            repository.getCourseSessions(),
            repository.getAvailableTimetables()
        ) { courses, sessions, timetables -> Triple(courses, sessions, timetables) },
        repository.getCurrentTimetableName(),
        repository.getCurrentTimetableId(),
        repository.getCurrentTimetable(),
        settingsRepository.getShowWeekends()
    ) { (courses, sessions, timetables), currentName, currentId, currentMeta, showWeekends ->
        // Handle defaults for missing metadata (e.g. from older JSON files where fields are 0)
        val safeTotalWeeks = currentMeta?.totalWeeks?.takeIf { it > 0 } ?: 20
        // If startDate is 0 (1970), maybe default to now? Or let user see 1970 to fix it.
        // User complained about 1970-01-01. Providing a default if 0 seems appropriate.
        val safeStartDate = if (currentMeta?.startDate != null && currentMeta.startDate > 0) currentMeta.startDate else System.currentTimeMillis()

        TimetableUiState(
            courseInfos = courses,
            sessions = sessions,
            isLoading = false,
            timetables = timetables,
            currentTimetableName = currentName,
            currentTimetableId = currentId,
            currentStartDate = safeStartDate,
            currentTotalWeeks = safeTotalWeeks,
            showWeekends = showWeekends
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimetableUiState(isLoading = true)
    )

    fun toggleShowWeekends(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowWeekends(show)
        }
    }

    fun switchTimetable(id: String) {
        viewModelScope.launch {
            repository.switchTimetable(id)
        }
    }
    
    fun createTimetable(name: String, startDate: Long, totalWeeks: Int = 20) {
        viewModelScope.launch {
            repository.createTimetable(name, startDate, totalWeeks)
        }
    }
    
    fun updateTimetableMetadata(id: String, name: String, startDate: Long, totalWeeks: Int) {
        viewModelScope.launch {
            repository.updateTimetableMetadata(id, name, startDate, totalWeeks)
        }
    }

    fun addCourse(course: CourseInfo) {
        viewModelScope.launch {
            repository.addCourse(course)
        }
    }

    fun addSession(session: CourseSession) {
        viewModelScope.launch {
            repository.addSession(session)
        }
    }

    fun updateCourse(course: CourseInfo) {
        viewModelScope.launch {
            repository.updateCourse(course)
        }
    }

    fun updateSession(oldSession: CourseSession, newSession: CourseSession) {
        viewModelScope.launch {
            repository.updateSession(oldSession, newSession)
        }
    }

    fun deleteSession(session: CourseSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    // Factory模式用于依赖注入
    companion object {
        fun provideFactory(
            repository: TimetableRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TimetableViewModel(repository, settingsRepository) as T
            }
        }
    }
}
