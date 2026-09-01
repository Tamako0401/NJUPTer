package com.example.njupter.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CourseInfoJsonTest {
    private val gson = Gson()

    @Test
    fun `new academic fields survive json round trip`() {
        val source = CourseInfoJson(
            id = "course",
            name = "大学物理",
            teacher = "闫巍",
            room = "教3-520",
            credit = "3",
            courseNature = "必修"
        )

        val restored = gson.fromJson(gson.toJson(source), CourseInfoJson::class.java)

        assertEquals("3", restored.credit)
        assertEquals("必修", restored.courseNature)
    }

    @Test
    fun `legacy json without academic fields remains readable`() {
        val restored = gson.fromJson(
            """{"id":"course","name":"大学物理","teacher":"闫巍","room":"教3-520"}""",
            CourseInfoJson::class.java
        )

        assertNull(restored.credit)
        assertNull(restored.courseNature)
    }
}
