package com.example.njupter

import android.content.Context
import android.os.SystemClock
import android.widget.Chronometer
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.test.platform.app.InstrumentationRegistry
import com.example.njupter.widget.WidgetCourseEntry
import com.example.njupter.widget.ui.CoursesWidgetContent
import com.example.njupter.widget.ui.widgetColorProviders
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalGlanceApi::class)
class NativeWidgetCountdownTest {
    @get:Rule val rule = createComposeRule()

    @Test fun nativeCountdownTicksInRegularWidget() = verifyCountdown(180)
    @Test fun nativeCountdownTicksInCompactWidget() = verifyCountdown(120)

    private fun verifyCountdown(height: Int) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val widget = object : GlanceAppWidget() {
            override suspend fun provideGlance(context: Context, id: GlanceId) {
                val entries = listOf(
                    WidgetCourseEntry("大学物理", "教2-304", "李老师", 0, 1, 2, "08:00-09:35",
                        System.currentTimeMillis() + 25 * 60_000),
                    WidgetCourseEntry("线性代数", "教2-212", "张老师", 1, 3, 4, "09:50-11:25")
                )
                provideContent {
                    CoursesWidgetContent(entries, null, 128, widgetColorProviders(context, false),
                        "今天 / 周四", "第 13 周", "暂无课程", { start, end -> "第 $start-$end 节" })
                }
            }
        }
        val views = runBlocking { widget.compose(context, size = DpSize(320.dp, height.dp)) }
        lateinit var countdown: Chronometer
        rule.setContent {
            AndroidView(factory = {
                views.apply(it, null).also { root -> countdown = root.findViewById(R.id.course_countdown) }
            }, modifier = Modifier.size(320.dp, height.dp).testTag("native-widget"))
        }
        var initial = ""
        rule.runOnIdle {
            assertTrue(countdown.isCountDown)
            assertTrue(countdown.base > SystemClock.elapsedRealtime())
            initial = countdown.text.toString()
        }
        SystemClock.sleep(1500)
        rule.runOnIdle { assertNotEquals(initial, countdown.text.toString()) }
        val bitmap = rule.onNodeWithTag("native-widget").captureToImage().asAndroidBitmap()
        val file = File(context.getExternalFilesDir(null), "ui-validation/native-widget-$height.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
