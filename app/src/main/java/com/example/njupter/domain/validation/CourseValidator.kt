package com.example.njupter.domain.validation

import com.example.njupter.data.CourseInfo
import com.example.njupter.data.CourseSession
import kotlin.math.max
import kotlin.math.min

sealed class ValidationError {
    data class StartAfterEnd(val start: Int, val end: Int) : ValidationError()
    object NoWeekSelected : ValidationError()
    data class TimeConflict(val day: Int, val startSection: Int, val endSection: Int) : ValidationError()
    data class CourseDuplicate(val name: String, val teacher: String, val classroom: String) : ValidationError()
}

object CourseValidator {

    fun validateSessionInput(
        day: Int,
        start: Int,
        end: Int,
        weeks: List<Int>,
        editingSession: CourseSession?,
        allSessions: List<CourseSession>
    ): ValidationError? {
        if (start > end) {
            return ValidationError.StartAfterEnd(start, end)
        }

        if (weeks.isEmpty()) {
            return ValidationError.NoWeekSelected
        }

        val conflict = allSessions.find { target ->
            if (editingSession != null && target == editingSession) return@find false
            if (target.day != day) return@find false
            val sectionOverlap = max(target.startSection, start) <= min(target.endSection, end)
            val weekOverlap = target.weeks.intersect(weeks.toSet()).isNotEmpty()
            sectionOverlap && weekOverlap
        }

        if (conflict != null) {
            return ValidationError.TimeConflict(conflict.day, conflict.startSection, conflict.endSection)
        }

        return null
    }

    fun validateCourseDuplication(
        currentId: String,
        name: String,
        teacher: String,
        classroom: String,
        existingCourses: List<CourseInfo>
    ): ValidationError? {
        val duplicate = existingCourses.find {
            it.name == name &&
            it.teacher == teacher &&
            it.classroom == classroom &&
            it.id != currentId
        }

        if (duplicate != null) {
            return ValidationError.CourseDuplicate(name, teacher, classroom)
        }

        return null
    }
}
