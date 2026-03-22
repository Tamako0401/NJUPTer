package com.example.njupter.data

import kotlinx.coroutines.flow.Flow

// 抽象数据源
interface TimetableRepository {
    fun getCourseInfos(): Flow<List<CourseInfo>>
    fun getCourseSessions(): Flow<List<CourseSession>>
    // New methods
    fun getAvailableTimetables(): Flow<List<TimetableMetadata>>
    fun getCurrentTimetableName(): Flow<String>
    fun getCurrentTimetableId(): Flow<String?>
    fun getCurrentTimetable(): Flow<TimetableMetadata?>
    
    suspend fun switchTimetable(id: String)
    suspend fun createTimetable(name: String, startDate: Long, totalWeeks: Int)
    suspend fun updateTimetableMetadata(id: String, name: String, startDate: Long, totalWeeks: Int)
    suspend fun deleteTimetable(id: String)

    suspend fun addCourse(course: CourseInfo)
    suspend fun addSession(session: CourseSession)
    suspend fun updateCourse(course: CourseInfo)
    suspend fun updateSession(oldSession: CourseSession, newSession: CourseSession)
    suspend fun deleteSession(session: CourseSession)
}
