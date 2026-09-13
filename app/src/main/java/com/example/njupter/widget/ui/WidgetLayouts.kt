package com.example.njupter.widget.ui

import android.content.res.Configuration
import android.widget.RemoteViews
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.compose.ui.graphics.toArgb
import com.example.njupter.widget.remainingCourseMinutes
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
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
import androidx.glance.unit.ColorProvider
import com.example.njupter.R
import com.example.njupter.widget.WidgetCourseEntry

@Composable
fun CoursesWidgetContent(
    entries: List<WidgetCourseEntry>,
    backgroundImagePath: String?,
    transparency: Int,
    colors: ColorProviders,
    headerTitle: String,
    weekLabel: String,
    emptyText: String,
    sectionLabel: (Int, Int) -> String,
    showBackgroundImage: Boolean = true,
    solidColorArgb: Int? = null,
    solidAlpha: Int = 255
) {
    val context = LocalContext.current
    // 课程色条与 app 主界面一致：暗色模式用深色板
    val isDark = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val courseColors = if (isDark) WidgetDarkColors else WidgetLightColors
    val size = LocalSize.current
    val compact = size.height < 166.dp
    val overlayAlpha = transparency.coerceIn(0, 255)
    val density = context.resources.displayMetrics.density
    // 解码目标像素尺寸按 widget 实际大小估算，避免每次渲染全尺寸解码大图
    val targetPx = (maxOf(size.width.value, size.height.value) * density).toInt().coerceAtLeast(1)
    val backgroundBitmap = if (showBackgroundImage) {
        backgroundImagePath?.let { path -> decodeSampled(path, targetPx) }
    } else {
        null
    }

    WidgetTheme(colors = colors) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(
                    solidColorArgb?.let { argb ->
                        ColorProvider(
                            Color(
                                red = (argb shr 16) and 0xFF,
                                green = (argb shr 8) and 0xFF,
                                blue = argb and 0xFF,
                                alpha = solidAlpha.coerceIn(0, 255)
                            )
                        )
                    } ?: ColorProvider(GlanceTheme.colors.widgetBackground.getColor(context).copy(alpha = solidAlpha.coerceIn(0, 255) / 255f))
                )
                .cornerRadius(16.dp)
        ) {
            if (backgroundBitmap != null) {
                Image(
                    provider = ImageProvider(backgroundBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(
                            Color(
                                red = 0,
                                green = 0,
                                blue = 0,
                                alpha = overlayAlpha
                            )
                        )
                ) {}
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(if (compact) 6.dp else 12.dp)
            ) {
                WidgetHeader(headerTitle = headerTitle, weekLabel = weekLabel)
                Spacer(modifier = GlanceModifier.height(if (compact) 4.dp else 8.dp))

                if (entries.isEmpty()) {
                    EmptyWidgetContent(emptyText)
                } else {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        entries.take(2).forEachIndexed { index, entry ->
                            if (index > 0) {
                                Spacer(GlanceModifier.height(if (compact) 4.dp else 6.dp))
                            }
                            CourseRow(
                                entry = entry,
                                courseColor = getColorForIndex(
                                    entry.name,
                                    entry.colorIndex,
                                    courseColors
                                ),
                                sectionText = sectionLabel(entry.startSection, entry.endSection),
                                compact = compact
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 采样解码背景图，使最长边落在 [targetPx] 的 1–2 倍内。
 * OOM 是 Error 不是 Exception，必须捕 Throwable 才能避免崩掉 widget 更新。
 */
private fun decodeSampled(path: String, targetPx: Int): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (
            bounds.outWidth / (sample * 2) >= targetPx ||
            bounds.outHeight / (sample * 2) >= targetPx
        ) {
            sample *= 2
        }
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (t: Throwable) {
        null
    }
}

@Composable
private fun WidgetHeader(
    headerTitle: String,
    weekLabel: String
) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(22.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = GlanceModifier.size(22.dp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = headerTitle,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
        if (weekLabel.isNotEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = weekLabel,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EmptyWidgetContent(text: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun CourseRow(
    entry: WidgetCourseEntry,
    courseColor: Color,
    sectionText: String,
    compact: Boolean,
    modifier: GlanceModifier = GlanceModifier
) {
    val (startTime, endTime) = splitTimes(entry.timeText)
    val metadata = buildList {
        add(sectionText)
        if (entry.classroom.isNotBlank()) add(entry.classroom)
        if (entry.teacher.isNotBlank()) add(entry.teacher)
    }.joinToString(" | ")

    if (compact) {
        Row(
            modifier = modifier.fillMaxWidth().height(28.dp)
                .background(GlanceTheme.colors.surfaceVariant).cornerRadius(10.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(GlanceModifier.width(4.dp).height(18.dp).background(courseColor)) {}
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = listOf(entry.timeText, entry.name, entry.classroom)
                    .filter { it.isNotBlank() }.joinToString("  "),
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp),
                maxLines = 1
            )
            CourseCountdown(entry)
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(18.dp)
            .padding(start = 10.dp, top = 6.dp, end = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = GlanceModifier.width(50.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (startTime.isNotEmpty()) {
                Text(
                    text = startTime,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    text = endTime,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            } else {
                Text(
                    text = sectionText,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(8.dp))
        Box(
            modifier = GlanceModifier
                .width(5.dp)
                .height(38.dp)
                .background(courseColor)
                .cornerRadius(3.dp)
        ) {}
        Spacer(modifier = GlanceModifier.width(10.dp))

        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.name,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Text(
                text = metadata,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
        CourseCountdown(entry)
    }
}

@Composable
private fun CourseCountdown(entry: WidgetCourseEntry) {
    val endMillis = entry.countdownEndMillis ?: return
    val context = LocalContext.current
    val views = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
        // A launcher Chronometer keeps running below zero when an alarm is delayed.
        // Send bounded text instead; the model schedules the next minute refresh.
        setTextViewText(R.id.course_countdown, context.getString(
            R.string.widget_countdown_minutes,
            remainingCourseMinutes(endMillis, System.currentTimeMillis())
        ))
        setTextColor(R.id.course_countdown, GlanceTheme.colors.primary.getColor(context).toArgb())
        setContentDescription(R.id.course_countdown, context.getString(R.string.widget_countdown))
    }
    Spacer(GlanceModifier.width(6.dp))
    AndroidRemoteViews(views, modifier = GlanceModifier.width(66.dp).height(24.dp))
}

private fun splitTimes(timeText: String): Pair<String, String> {
    val parts = timeText.split("-", limit = 2)
    return if (parts.size == 2 && parts.all { ':' in it }) {
        parts[0] to parts[1]
    } else {
        "" to ""
    }
}
