package com.example.njupter

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.njupter.data.CourseInfo
import com.example.njupter.data.CourseSession
import com.example.njupter.data.TimetableMetadata
import com.example.njupter.ui.theme.NJUPTerTheme
import com.example.njupter.ui.theme.getCourseColors
import com.example.njupter.ui.timetable.TimetableScreen
import com.example.njupter.ui.timetable.dialog.CourseEditorDialog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ForkFeaturesUiTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun conflictingWeeksStaySelectedUntilUserChangesThem() {
        val occupied = CourseSession("other", 1, 1, 2, listOf(1))
        var saved: CourseSession? = null
        compose.setContent {
            NJUPTerTheme {
                CourseEditorDialog(null, null, emptyList(), listOf(occupied), getCourseColors(), false, 2,
                    onDismiss = {}, onSave = { _, session, _ -> saved = session }, onDelete = {})
            }
        }
        compose.onNodeWithText(context.getString(R.string.save_btn)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.customize)).performScrollTo().performClick()
        // Both weeks remain selectable. Explicitly remove only the conflicting week.
        compose.onNodeWithText("1").performClick()
        compose.onNodeWithText(context.getString(R.string.confirm)).performClick()
        compose.onNodeWithText(context.getString(R.string.save_btn)).assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(listOf(2), saved?.weeks) }
    }

    @Test fun arrowsRemainAndSettingsOpensFromTimetable() {
        var opened = false
        compose.setContent {
            NJUPTerTheme {
                TimetableScreen(emptyList(), emptyList(), timetables = listOf(TimetableMetadata("test", "Test", 0L)), currentTimetableId = "test",
                    currentTimetableName = "Test", onSettingsClick = { opened = true })
            }
        }
        compose.onNodeWithContentDescription(context.getString(R.string.cd_previous_week)).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.cd_next_week)).assertIsDisplayed().performClick()
        compose.onNodeWithText(context.getString(R.string.week, 2)).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.settings)).assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(opened) }
    }

    @Test fun editorPreservesNotesReminderAndDeletePlacement() {
        val course = CourseInfo("test", "Math", "Teacher", "Room", note = "Bring notes", reminderEnabled = true)
        val session = CourseSession("test", 1, 1, 2, listOf(1))
        var saved: CourseInfo? = null
        compose.setContent {
            NJUPTerTheme {
                CourseEditorDialog(session, course, listOf(course), listOf(session), getCourseColors(), false, 20,
                    onDismiss = {}, onSave = { info, _, _ -> saved = info }, onDelete = {})
            }
        }
        compose.onNodeWithText("Bring notes").performScrollTo().performTextReplacement("Bring updated notes")
        compose.onNodeWithText(context.getString(R.string.delete)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.cancel)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.save_btn)).performClick()
        compose.runOnIdle {
            assertEquals("Bring updated notes", saved?.note)
            assertEquals(true, saved?.reminderEnabled)
            assertEquals(-1, saved?.colorIndex)
        }
    }
}
