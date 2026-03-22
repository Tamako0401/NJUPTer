package com.example.njupter.domain.validation

import com.example.njupter.data.CourseInfo
import com.example.njupter.data.CourseSession
import kotlin.math.max
import kotlin.math.min

object CourseValidator {

    fun validateSessionInput(
        day: Int,
        start: Int,
        end: Int,
        weeks: List<Int>,
        editingSession: CourseSession?,
        allSessions: List<CourseSession>
    ): String? {
        // Basic Logic
        if (start > end) {
            return "Error: Start section ($start) cannot be greater than End section ($end)."
        }

        if (weeks.isEmpty()){
            return "Error: At least one week must be selected."
        }

        // Time Conflict Check
        val conflict = allSessions.find { target ->
            // Ignore self when editing
            if (editingSession != null && target == editingSession) return@find false

            // Must be same day
            if (target.day != day) return@find false

            // Check section overlap
            val sectionOverlap = max(target.startSection, start) <= min(target.endSection, end)

            // Check week overlap
            val weekOverlap = target.weeks.intersect(weeks.toSet()).isNotEmpty()

            // Conflict if sections overlap AND weeks overlap
            sectionOverlap && weekOverlap
        }

        if (conflict != null) {
            // Found a conflict
            return "Error: Time conflict with existing session: Day ${conflict.day}, Sections ${conflict.startSection}-${conflict.endSection}."
        }

        return null // No error
    }

    fun validateCourseDuplication(
        currentId: String,
        name: String,
        teacher: String,
        classroom: String,
        existingCourses: List<CourseInfo>
    ): String? {
        // Check if there is another course with same name, teacher, and classroom
        // but different ID.
        // We *do* allow same name/teacher/room IF it is the same course object (same ID)
        
        val duplicate = existingCourses.find {
            it.name == name &&
            it.teacher == teacher &&
            it.classroom == classroom &&
            it.id != currentId
        }

        if (duplicate != null) {
            return "Error: Course '$name' with teacher '$teacher' in room '$classroom' already exists."
        }

        return null
    }
}

