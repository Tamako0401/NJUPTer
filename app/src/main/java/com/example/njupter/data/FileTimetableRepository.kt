// 该类实现了 TimetableRepository 接口，负责从 assets 目录中的 JSON 文件读取课程信息和课程安排数据，并将其转换为领域模型 CourseInfo 和 CourseSession。

package com.example.njupter.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FileTimetableRepository(private val dataSource: TimetableDataSource) : TimetableRepository {

    // 状态持有，private 是因为外部只能读取，Mutable 是因为内部有权修改
    private val _courseInfos = MutableStateFlow<List<CourseInfo>>(emptyList())
    private val _courseSessions = MutableStateFlow<List<CourseSession>>(emptyList())
    
    private val _availableTimetables = MutableStateFlow<List<TimetableMetadata>>(emptyList())
    private val _currentTimetableName = MutableStateFlow("")
    private val _currentTimetableId = MutableStateFlow<String?>(null)
    
    private var isLoaded = false

    init {
        CoroutineScope(Dispatchers.IO).launch {
            refreshTimetableList()
        }
    }

    private suspend fun refreshTimetableList() {
        _availableTimetables.value = dataSource.getAllTimetables()
    }
    
    private suspend fun saveData() {
        val id = _currentTimetableId.value ?: return
        // 启动一个新的协程任务，在IO线程中执行保存操作，避免阻塞主线程
        // 但是把持久化操作丢到后台线程，不保证完成时间、不保证顺序、不保证成功
        // CoroutineScope(Dispatchers.IO).launch {
        // 那就 suspend + 上层控制
            dataSource.saveTimetable(
                id,
                TimetableData(
                    courses = _courseInfos.value,
                    sessions = _courseSessions.value
                )
            )
        // }
    }

    // 对外暴露只读流
    override fun getCourseInfos(): Flow<List<CourseInfo>> = _courseInfos.asStateFlow()
    override fun getCourseSessions(): Flow<List<CourseSession>> = _courseSessions.asStateFlow()
    
    override fun getAvailableTimetables(): Flow<List<TimetableMetadata>> = _availableTimetables.asStateFlow()
    override fun getCurrentTimetableName(): Flow<String> = _currentTimetableName.asStateFlow()
    override fun getCurrentTimetableId(): Flow<String?> = _currentTimetableId.asStateFlow()

    override fun getCurrentTimetable(): Flow<TimetableMetadata?> =
        kotlinx.coroutines.flow.combine(_availableTimetables, _currentTimetableId) { list, id ->
            list.find { it.id == id }
        }

    override suspend fun switchTimetable(id: String) {
        if (id == _currentTimetableId.value) return
        
        val metas = _availableTimetables.value
        val meta = metas.find { it.id == id } ?: return
        
        _currentTimetableId.value = id
        _currentTimetableName.value = meta.name
        
        val data = dataSource.loadTimetable(id)
        _courseInfos.value = data.courses
        _courseSessions.value = data.sessions
    }

    override suspend fun createTimetable(name: String, startDate: Long, totalWeeks: Int) {
        val meta = dataSource.createTimetable(name, startDate, totalWeeks)
        _availableTimetables.value = dataSource.getAllTimetables()
        switchTimetable(meta.id)
    }

    override suspend fun updateTimetableMetadata(id: String, name: String, startDate: Long, totalWeeks: Int) {
        dataSource.updateTimetableMetadata(id, name, startDate, totalWeeks)
        refreshTimetableList()
        // 如果当前Timetable被更新了，我们需要刷新UI状态
        if (_currentTimetableId.value == id) {
             _currentTimetableName.value = name
        }
    }

    override suspend fun deleteTimetable(id: String) {
        if (id == _currentTimetableId.value) {
             val first = _availableTimetables.value.firstOrNull()
             if (first != null) {
                 switchTimetable(first.id)
             }
        }
        dataSource.deleteTimetable(id)
        refreshTimetableList()
    }

    override suspend fun addCourse(course: CourseInfo) {
        _courseInfos.update { current ->
            current + course
        }
        saveData()
    }

    override suspend fun addSession(session: CourseSession) {
        _courseSessions.update { current ->
            current + session
        }
        saveData()
    }

    override suspend fun updateCourse(course: CourseInfo) {
        _courseInfos.update { current ->
            current.map { if (it.id == course.id) course else it }
        }
        saveData()
    }

    override suspend fun updateSession(oldSession: CourseSession, newSession: CourseSession) {
        _courseSessions.update { current ->
            current.map {
                // CourseSession doesn't have a unique ID, so we compare all fields or object reference
                if (it == oldSession) newSession else it
            }
        }
        saveData()
    }

    override suspend fun deleteSession(session: CourseSession) {
        _courseSessions.update { current ->
            current - session
        }
        saveData()
    }
}
