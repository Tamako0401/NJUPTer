package com.example.njupter.data

/**
 * 只关心 JSON 文件的 CRUD，不持有状态
 */

interface TimetableDataSource {
    suspend fun getAllTimetables(): List<TimetableMetadata>
    suspend fun createTimetable(name: String, startDate: Long, totalWeeks: Int, showWeekends: Boolean, sessionTimes: List<String>): TimetableMetadata
    suspend fun updateTimetableMetadata(id: String, name: String, startDate: Long, totalWeeks: Int, showWeekends: Boolean, sessionTimes: List<String>)
    suspend fun loadTimetable(id: String): TimetableData
    suspend fun saveTimetable(id: String, data: TimetableData)
    suspend fun deleteTimetable(id: String)
}