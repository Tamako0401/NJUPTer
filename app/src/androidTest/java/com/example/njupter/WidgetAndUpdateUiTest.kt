package com.example.njupter

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.njupter.ui.settings.WidgetContentPreview
import com.example.njupter.ui.settings.WidgetContentDarkPreview
import com.example.njupter.ui.settings.WidgetContentCompactPreview
import com.example.njupter.ui.theme.NJUPTerTheme
import com.example.njupter.update.AppUpdate
import com.example.njupter.update.AppUpdateDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class WidgetAndUpdateUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun widgetLightCountdownPreview() {
        compose.setContent { Box(Modifier.size(320.dp, 180.dp).testTag("widget")) { WidgetContentPreview() } }
        compose.onNodeWithText("25:00").assertIsDisplayed()
        captureWidget("widget-light")
    }

    @Test fun widgetDarkCountdownPreview() {
        compose.setContent { Box(Modifier.size(320.dp, 180.dp).testTag("widget")) { WidgetContentDarkPreview() } }
        compose.onNodeWithText("25:00").assertIsDisplayed()
        captureWidget("widget-dark")
    }

    @Test fun widgetCompactCountdownPreview() {
        compose.setContent { Box(Modifier.size(320.dp, 120.dp).testTag("widget")) { WidgetContentCompactPreview() } }
        compose.onNodeWithText("25:00").assertIsDisplayed()
        captureWidget("widget-compact")
    }

    @Test fun updateDialogHasWorkingDownloadAndIgnoreActions() {
        var downloads = 0
        var ignores = 0
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            NJUPTerTheme {
                AppUpdateDialog(AppUpdate(100, "1.1.0", "Course countdown and timetable fixes", ""),
                    false, onIgnore = { ignores++ }, onDownload = { downloads++ })
            }
        }
        compose.onNodeWithText(context.getString(R.string.update_download)).assertIsDisplayed().performClick()
        compose.onNodeWithText(context.getString(R.string.update_ignore)).assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, downloads)
            assertEquals(1, ignores)
            val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            saveBitmap("update-dialog", screenshot)
        }
    }

    private fun captureWidget(name: String) {
        saveBitmap(name, compose.onNodeWithTag("widget").captureToImage().asAndroidBitmap())
    }

    private fun saveBitmap(name: String, bitmap: Bitmap) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "ui-validation").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
