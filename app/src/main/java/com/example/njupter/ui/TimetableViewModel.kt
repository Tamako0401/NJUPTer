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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 该 ViewModel 负责管理课程表相关的 UI 状态和业务逻辑。它从 TimetableRepository 获取课程信息、课程安排和时间表列表，并将它们组合成一个统一的 UI 状态流（uiState）。UI 层可以观察这个状态流来更新界面。
 */
data class TimetableUiState(
    val courseInfos: List<CourseInfo> = emptyList(),
    val sessions: List<CourseSession> = emptyList(),
    val isLoading: Boolean = false,
    val timetables: List<TimetableMetadata> = emptyList(),
    val currentTimetableName: String = "",
    val currentTimetableId: String? = null,
    val currentStartDate: Long = System.currentTimeMillis(),
    val currentTotalWeeks: Int = 20,
    val showWeekends: Boolean = false,
    val currentSessionTimes: List<String> = emptyList(),
    
    // Import state
    val importResult: com.example.njupter.domain.import.TimetableImportMatcher.ImportResult? = null,
    val isImporting: Boolean = false,
    val importError: String? = null
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
        repository.getCurrentTimetable()
    ) { (courses, sessions, timetables), currentName, currentId, currentMeta ->
        // Handle defaults for missing metadata (e.g. from older JSON files where fields are 0)
        val safeTotalWeeks = currentMeta?.totalWeeks?.takeIf { it > 0 } ?: 20
        // If startDate is 0 (1970), maybe default to now? Or let user see 1970 to fix it.
        // User complained about 1970-01-01. Providing a default if 0 seems appropriate.
        val safeStartDate = if (currentMeta?.startDate != null && currentMeta.startDate > 0) currentMeta.startDate else System.currentTimeMillis()
        val safeSessionTimes = currentMeta?.nonNullSessionTimes ?: com.example.njupter.data.TimetableMetadata("", "", 0).nonNullSessionTimes
        val safeShowWeekends = currentMeta?.showWeekends ?: true

        TimetableUiState(
            courseInfos = courses,
            sessions = sessions,
            isLoading = false,
            timetables = timetables,
            currentTimetableName = currentName,
            currentTimetableId = currentId,
            currentStartDate = safeStartDate,
            currentTotalWeeks = safeTotalWeeks,
            showWeekends = safeShowWeekends,
            currentSessionTimes = safeSessionTimes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimetableUiState(isLoading = true)
    )

    // A separate StateFlow for import process so we can overlay it on top of the combined flow above.
    private val _importState = kotlinx.coroutines.flow.MutableStateFlow(ImportState())
    val importState = _importState.asStateFlow()

    data class ImportState(
        val isImporting: Boolean = false,
        val result: com.example.njupter.domain.import.TimetableImportMatcher.ImportResult? = null,
        val error: String? = null
    )

    fun fetchAndProcessImport(cookieString: String, xh: String) {
        viewModelScope.launch {
            _importState.value = ImportState(isImporting = true)
            try {
                // 1. Fetch HTML
                val client = com.example.njupter.data.import.JwxtClient(cookieString, xh)
                val html = client.fetchTimetableHtml()

                // 2. Parse HTML
                val parser = com.example.njupter.data.import.JwxtParser()
                val remoteCourses = parser.parseHtml(html)

                // 3. Match and Convert
                val matcher = com.example.njupter.domain.import.TimetableImportMatcher()
                
                // Fetch current list to pass into matcher
                val currentCourses = repository.getCourseInfos().stateIn(viewModelScope).value
                val currentSessions = repository.getCourseSessions().stateIn(viewModelScope).value
                
                val result = matcher.matchAndConvert(remoteCourses, currentCourses, currentSessions)
                
                _importState.value = ImportState(result = result)
            } catch (e: Exception) {
                _importState.value = ImportState(error = e.message ?: "Unknown error")
            }
        }
    }

    fun clearImportState() {
        _importState.value = ImportState()
    }

    fun createTimetable(name: String, startDate: Long, totalWeeks: Int, showWeekends: Boolean, sessionTimes: List<String>) {
        viewModelScope.launch {
            repository.createTimetable(name, startDate, totalWeeks, showWeekends, sessionTimes)
        }
    }
    
    fun updateTimetableMetadata(id: String, name: String, startDate: Long, totalWeeks: Int, showWeekends: Boolean, sessionTimes: List<String>) {
        viewModelScope.launch {
            repository.updateTimetableMetadata(id, name, startDate, totalWeeks, showWeekends, sessionTimes)
        }
    }

    fun switchTimetable(id: String) {
        viewModelScope.launch {
            repository.switchTimetable(id)
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

    fun importTimetableData(newCourses: List<CourseInfo>, newSessions: List<CourseSession>) {
        viewModelScope.launch {
            repository.importTimetableData(newCourses, newSessions)
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
