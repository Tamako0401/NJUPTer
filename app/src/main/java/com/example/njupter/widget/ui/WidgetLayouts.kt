package com.example.njupter.widget.ui

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.njupter.widget.WidgetCourseEntry

@Composable
fun CoursesWidgetContent(
    entries: List<WidgetCourseEntry>,
    backgroundImagePath: String?,
    transparency: Int,
    isDark: Boolean
) {
    val size = LocalSize.current
    val colors = if (isDark) WidgetDarkColors else WidgetLightColors
    val overlayAlpha = (255 - transparency).coerceIn(0, 255)

    WidgetTheme(isDark = isDark) {
        Box(modifier = GlanceModifier.fillMaxSize(), content = {
            // Background image layer
            val bgBitmap = backgroundImagePath?.let { path ->
                try {
                    BitmapFactory.decodeFile(path)
                } catch (_: Exception) {
                    null
                }
            }
            if (bgBitmap != null) {
                Image(
                    provider = ImageProvider(bgBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize()
                )
            }

            // Transparency overlay
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(red = 0, green = 0, blue = 0, alpha = overlayAlpha)),
                content = {}
            )

            // Course list
            when {
                entries.isEmpty() -> EmptyWidgetContent()
                size.width < 3.dp -> CompactWidgetLayout(entries, colors)
                else -> DetailedWidgetLayout(entries, colors)
            }
        })
    }
}

@Composable
fun EmptyWidgetContent() {
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
        contentAlignment = Alignment.Center,
        content = {
            Text(
                text = "No courses today",
                style = TextStyle(fontWeight = FontWeight.Normal, textAlign = TextAlign.Center)
            )
        }
    )
}

@Composable
fun CompactWidgetLayout(entries: List<WidgetCourseEntry>, colors: List<Color>) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(8.dp),
        content = {
            Text(
                text = "Today's Courses",
                style = TextStyle(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            entries.take(4).forEach { entry ->
                CompactCourseRow(entry, colors)
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
            if (entries.size > 4) {
                Text(
                    text = "+${entries.size - 4} more",
                    style = TextStyle(fontWeight = FontWeight.Normal)
                )
            }
        }
    )
}

@Composable
fun DetailedWidgetLayout(entries: List<WidgetCourseEntry>, colors: List<Color>) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
        content = {
            Text(
                text = "Today's Courses",
                style = TextStyle(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            entries.take(8).forEach { entry ->
                DetailedCourseRow(entry, colors)
                Spacer(modifier = GlanceModifier.height(4.dp))
            }
            if (entries.size > 8) {
                Text(
                    text = "+${entries.size - 8} more",
                    style = TextStyle(fontWeight = FontWeight.Normal)
                )
            }
        }
    )
}

@Composable
fun CompactCourseRow(entry: WidgetCourseEntry, colors: List<Color>) {
    val courseColor = getColorForIndex(entry.name, entry.colorIndex, colors)
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            Box(
                modifier = GlanceModifier
                    .size(12.dp)
                    .background(courseColor)
                    .cornerRadius(3.dp),
                content = {}
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = entry.name,
                style = TextStyle(fontWeight = FontWeight.Medium),
                maxLines = 1
            )
        }
    )
}

@Composable
fun DetailedCourseRow(entry: WidgetCourseEntry, colors: List<Color>) {
    val courseColor = getColorForIndex(entry.name, entry.colorIndex, colors)
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            Box(
                modifier = GlanceModifier
                    .size(16.dp)
                    .background(courseColor)
                    .cornerRadius(4.dp),
                content = {}
            )
            Spacer(modifier = GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.fillMaxWidth(), content = {
                Text(
                    text = entry.name,
                    style = TextStyle(fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                if (entry.timeText.isNotEmpty()) {
                    Text(
                        text = entry.timeText,
                        style = TextStyle(fontWeight = FontWeight.Normal),
                        maxLines = 1
                    )
                }
                if (entry.classroom.isNotEmpty()) {
                    Text(
                        text = "@${entry.classroom}",
                        style = TextStyle(fontWeight = FontWeight.Normal),
                        maxLines = 1
                    )
                }
            })
        }
    )
}
