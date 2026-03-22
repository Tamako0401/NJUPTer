// 根据星期和节次查找对应课程
package com.example.njupter.domain

import com.example.njupter.data.CourseInfo
import com.example.njupter.data.CourseSession

// TODO:给定(day, section)，找到对应的课程信息
fun getCourseAt(
    sessions: List<CourseSession>,
    courseMap: Map<String, CourseInfo>,
    day: Int,
    section: Int
): CourseInfo? {

    val session = sessions.find {
        it.day == day && section in it.startSection..it.endSection
    }

    return session?.let { courseMap[it.courseId] }
}

fun getDateForWeekDay(startDate: Long, week: Int, day: Int): String {
    val weekInMillis = (week - 1) * 7 * 24 * 60 * 60 * 1000L
    val dayInMillis = (day - 1) * 24 * 60 * 60 * 1000L
    val targetDate = startDate + weekInMillis + dayInMillis
    val date = java.util.Date(targetDate)
    val format = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
    return format.format(date)
}
