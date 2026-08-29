package com.example.njupter.domain.import

import com.example.njupter.data.CourseInfo
import com.example.njupter.data.CourseSession
import com.example.njupter.data.import.RemoteCourse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableImportMatcherTest {
    private val matcher = TimetableImportMatcher()

    @Test
    fun matchAndConvert_normalizesCourseIdentityAndMergesWeekFragments() {
        val remoteCourses = listOf(
            remote(name = "物理实验 （下）", weeks = listOf(1, 3, 5)),
            remote(name = "物理实验（下）", weeks = listOf(7, 9))
        )

        val result = matcher.matchAndConvert(remoteCourses, emptyList(), emptyList())

        assertEquals(1, result.newCourses.size)
        assertEquals("物理实验（下）", result.newCourses.single().name)
        assertEquals(1, result.newSessions.size)
        assertEquals(listOf(1, 3, 5, 7, 9), result.newSessions.single().weeks)
    }

    @Test
    fun matchAndConvert_reusesExistingCourseAndImportsOnlyMissingWeeks() {
        val existingCourse = CourseInfo(
            id = "existing",
            name = "信号与系统",
            teacher = "孙老师",
            classroom = "教3-300"
        )
        val existingSession = CourseSession(
            courseId = existingCourse.id,
            day = 1,
            startSection = 1,
            endSection = 2,
            weeks = listOf(1, 2)
        )

        val result = matcher.matchAndConvert(
            remoteCourses = listOf(remote(name = " 信号与系统 ", weeks = listOf(1, 2, 3, 4))),
            existingCourses = listOf(existingCourse),
            existingSessions = listOf(existingSession)
        )

        assertTrue(result.newCourses.isEmpty())
        assertEquals(listOf(3, 4), result.newSessions.single().weeks)
        assertEquals(existingCourse.id, result.newSessions.single().courseId)
    }

    private fun remote(name: String, weeks: List<Int>) = RemoteCourse(
        name = name,
        teacher = "孙老师",
        classroom = "教3－300",
        dayOfWeek = 1,
        startSection = 1,
        endSection = 2,
        weeks = weeks
    )
}
