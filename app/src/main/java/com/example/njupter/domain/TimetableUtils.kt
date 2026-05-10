// 根据星期和节次查找对应课程
package com.example.njupter.domain

import com.example.njupter.data.CourseInfo
import com.example.njupter.data.CourseSession
import java.util.Calendar

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

private val dateFormatLocal = ThreadLocal.withInitial {
    java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
}

fun getDateForWeekDay(startDate: Long, week: Int, day: Int): String {
    val weekInMillis = (week - 1) * 7 * 24 * 60 * 60 * 1000L
    val dayInMillis = (day - 1) * 24 * 60 * 60 * 1000L
    val targetDate = startDate + weekInMillis + dayInMillis
    return dateFormatLocal.get()!!.format(java.util.Date(targetDate))
}

fun getTodayWeekIndex(startDate: Long, totalWeeks: Int): Int? {
    val startCalendar = Calendar.getInstance().apply {
        timeInMillis = startDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayCalendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val diffMillis = todayCalendar.timeInMillis - startCalendar.timeInMillis
    if (diffMillis < 0) return null

    val diffDays = (diffMillis / (24 * 60 * 60 * 1000L)).toInt()
    val weekIndex = diffDays / 7

    return weekIndex.takeIf { it in 0 until totalWeeks }
}

fun getTodayDayOfWeek(): Int {
    return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }
}
