package com.example.njupter.widget

import com.example.njupter.data.CourseInfo
import com.example.njupter.data.CourseSession
import com.example.njupter.data.TimetableData
import com.example.njupter.data.TimetableMetadata
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class WidgetModelsTest {
    private lateinit var originalTimeZone: TimeZone
    private val courses = listOf(
        CourseInfo("m1", "Morning one", "Teacher A", "A101", 0),
        CourseInfo("m2", "Morning two", "Teacher B", "A102", 1),
        CourseInfo("t1", "Tomorrow one", "Teacher C", "A103", 2),
        CourseInfo("late", "Late course", "Teacher D", "A104", 3)
    )

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `widget shows at most two upcoming courses`() {
        val state = stateAt(
            hour = 8,
            minute = 0,
            sessions = listOf(
                CourseSession("m1", day = 1, startSection = 1, endSection = 2, weeks = listOf(1)),
                CourseSession("m2", day = 1, startSection = 3, endSection = 4, weeks = listOf(1)),
                CourseSession("late", day = 1, startSection = 9, endSection = 9, weeks = listOf(1))
            )
        )

        assertEquals(listOf("Morning one", "Morning two"), state.entries.map { it.name })
        assertFalse(state.isTomorrow)
    }

    @Test
    fun `ending course is removed at boundary and next course fills its place`() {
        val sessions = listOf(
            CourseSession("m1", 1, 1, 2, listOf(1)),
            CourseSession("m2", 1, 3, 4, listOf(1)),
            CourseSession("late", 1, 9, 9, listOf(1))
        )
        val before = stateAt(8, 0, sessions)
        assertEquals(millis(2026, Calendar.AUGUST, 31, 9, 35), before.nextRefreshAtMillis)
        val boundary = Calendar.getInstance().apply { timeInMillis = before.nextRefreshAtMillis!! }
        val after = stateAt(boundary.get(Calendar.HOUR_OF_DAY), boundary.get(Calendar.MINUTE), sessions)

        assertEquals(listOf("Morning one", "Morning two"), before.entries.map { it.name })
        assertEquals(listOf("Morning two", "Late course"), after.entries.map { it.name })
        assertEquals(millis(2026, Calendar.AUGUST, 31, 11, 25), after.nextRefreshAtMillis)
    }

    @Test
    fun `tomorrow preview also caps courses at two`() {
        val state = stateAt(17, 0, listOf(
            CourseSession("m1", 2, 1, 2, listOf(1)),
            CourseSession("m2", 2, 3, 4, listOf(1)),
            CourseSession("late", 2, 9, 9, listOf(1))
        ))
        assertTrue(state.isTomorrow)
        assertEquals(listOf("Morning one", "Morning two"), state.entries.map { it.name })
    }

    @Test
    fun `finished morning shows completed until evening forecast`() {
        val state = stateAt(16, 0, listOf(CourseSession("m1", 1, 1, 2, listOf(1))))
        assertTrue(state.entries.isEmpty())
        assertTrue(state.isDayComplete)
        assertEquals(millis(2026, Calendar.AUGUST, 31, 17, 0), state.nextRefreshAtMillis)
    }

    @Test
    fun `after 17 widget previews tomorrows first course`() {
        val state = stateAt(
            hour = 17,
            minute = 0,
            sessions = listOf(
                CourseSession("m1", day = 1, startSection = 1, endSection = 2, weeks = listOf(1)),
                CourseSession("t1", day = 2, startSection = 3, endSection = 4, weeks = listOf(1))
            )
        )

        assertEquals(listOf("Tomorrow one"), state.entries.map { it.name })
        assertEquals(2, state.dayOfWeek)
        assertTrue(state.isTomorrow)
        assertFalse(state.isDayComplete)
    }

    @Test
    fun `after 17 without tomorrow courses shows completed state`() {
        val state = stateAt(
            hour = 17,
            minute = 0,
            sessions = listOf(
                CourseSession("m1", day = 1, startSection = 1, endSection = 2, weeks = listOf(1))
            )
        )

        assertTrue(state.entries.isEmpty())
        assertFalse(state.isTomorrow)
        assertTrue(state.isDayComplete)
    }

    @Test
    fun `course ending after 17 delays tomorrow preview until it is over`() {
        val sessions = listOf(
            CourseSession("late", day = 1, startSection = 9, endSection = 9, weeks = listOf(1)),
            CourseSession("t1", day = 2, startSection = 1, endSection = 2, weeks = listOf(1))
        )

        assertEquals("Late course", stateAt(17, 5, sessions).entries.single().name)
        assertFalse(stateAt(17, 5, sessions).isTomorrow)
        assertEquals("Tomorrow one", stateAt(17, 11, sessions).entries.single().name)
        assertTrue(stateAt(17, 11, sessions).isTomorrow)
    }

    private fun stateAt(
        hour: Int,
        minute: Int,
        sessions: List<CourseSession>
    ): WidgetDisplayState {
        val sundayStart = millis(2026, Calendar.AUGUST, 30, 12, 0)
        return buildWidgetDisplayState(
            metadata = TimetableMetadata(
                id = "test",
                name = "Test",
                lastModified = 0,
                startDate = sundayStart,
                totalWeeks = 20
            ),
            data = TimetableData(courses = courses, sessions = sessions),
            nowMillis = millis(2026, Calendar.AUGUST, 31, hour, minute)
        )
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
    }
}
