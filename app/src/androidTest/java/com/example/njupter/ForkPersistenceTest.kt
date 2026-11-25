package com.example.njupter

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.example.njupter.data.CourseInfo
import com.example.njupter.data.LocalFileDataSource
import com.example.njupter.data.TimetableData
import com.example.njupter.widget.WidgetSettingsManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class ForkPersistenceTest {
    @Test fun legacyCoursesKeepRemindersAndNewFieldsSurviveFileRoundTrip() = runBlocking {
        val original = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(original.cacheDir, "port-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(original) { override fun getFilesDir() = directory }
        try {
            val dataSource = LocalFileDataSource(context)
            File(directory, "timetable_legacy.json").writeText("""{"courses":[{"id":"old","name":"Old","teacher":"T","room":"R"}],"sessions":[]}""")
            val legacy = dataSource.loadTimetable("legacy").courses.single()
            assertEquals("", legacy.note)
            assertTrue(legacy.reminderEnabled)
            val edited = legacy.copy(note = "备注\n第二行", reminderEnabled = false)
            dataSource.saveTimetable("legacy", TimetableData(listOf(edited), emptyList()))
            assertEquals(edited, dataSource.loadTimetable("legacy").courses.single())
        } finally { directory.deleteRecursively() }
    }

    @Test fun widgetInstanceEditsAndClearDoNotAlterOtherInstancesOrLegacyDefaults() {
        val original = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "port-test-${UUID.randomUUID()}-"
        val context = object : ContextWrapper(original) {
            override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences(prefix + name, mode)
        }
        try {
            WidgetSettingsManager.setBackgroundImagePath(context, "legacy.jpg")
            assertEquals("legacy.jpg", WidgetSettingsManager.getBackgroundImagePath(context, 101))
            WidgetSettingsManager.setBackgroundImagePath(context, "first.jpg", 101)
            WidgetSettingsManager.setSolidAlpha(context, 72, 101)
            WidgetSettingsManager.setBackgroundImagePath(context, null, 102)
            assertEquals("first.jpg", WidgetSettingsManager.getBackgroundImagePath(context, 101))
            assertNull(WidgetSettingsManager.getBackgroundImagePath(context, 102))
            assertEquals("legacy.jpg", WidgetSettingsManager.getBackgroundImagePath(context))
            assertEquals(72, WidgetSettingsManager.getSolidAlpha(context, 101))
            assertEquals(255, WidgetSettingsManager.getSolidAlpha(context, 102))
        } finally { original.deleteSharedPreferences(prefix + "widget_settings") }
    }
}
