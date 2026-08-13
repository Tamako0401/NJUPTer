package com.example.njupter.widget

import android.content.Context
import com.example.njupter.data.LocalFileDataSource
import com.example.njupter.domain.getTodayDayOfWeek
import com.example.njupter.domain.getTodayWeekIndex
import kotlinx.coroutines.runBlocking

data class WidgetCourseEntry(
    val name: String,
    val classroom: String,
    val teacher: String,
    val colorIndex: Int,
    val startSection: Int,
    val endSection: Int,
    val timeText: String
)

object WidgetModels {
    fun computeTodayWeekNumber(context: Context): Int? {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val lastId = prefs.getString("last_selected_timetable_id", null) ?: return null
        val dataSource = LocalFileDataSource(context)

        return try {
            runBlocking {
                val metadata = dataSource.getAllTimetables().find { it.id == lastId }
                    ?: return@runBlocking null
                getTodayWeekIndex(metadata.startDate, metadata.totalWeeks)?.plus(1)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun computeTodaysCourses(context: Context): List<WidgetCourseEntry> {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val lastId = prefs.getString("last_selected_timetable_id", null) ?: return emptyList()

        val dataSource = LocalFileDataSource(context)
        return try {
            runBlocking {
                val timetables = dataSource.getAllTimetables()
                val meta = timetables.find { it.id == lastId } ?: return@runBlocking emptyList()
                val data = dataSource.loadTimetable(lastId)

                val todayDay = getTodayDayOfWeek()
                val todayWeek = getTodayWeekIndex(meta.startDate, meta.totalWeeks)?.plus(1)
                    ?: return@runBlocking emptyList()

                val sessionTimes = meta.nonNullSessionTimes

                data.sessions
                    .filter { it.day == todayDay && it.weeks.contains(todayWeek) }
                    .sortedBy { it.startSection }
                    .mapNotNull { session ->
                        data.courses.find { it.id == session.courseId }?.let { course ->
                            val startTime = sessionTimes.getOrNull(session.startSection - 1)
                                ?.substringBefore("-") ?: ""
                            val endTime = sessionTimes.getOrNull(session.endSection - 1)
                                ?.substringAfter("-") ?: ""
                            WidgetCourseEntry(
                                name = course.name,
                                classroom = course.classroom,
                                teacher = course.teacher,
                                colorIndex = course.colorIndex,
                                startSection = session.startSection,
                                endSection = session.endSection,
                                timeText = if (startTime.isNotEmpty() && endTime.isNotEmpty()) {
                                    "$startTime-$endTime"
                                } else {
                                    "Sec ${session.startSection}-${session.endSection}"
                                }
                            )
                        }
                    }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
