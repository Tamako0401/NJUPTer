package com.example.njupter.widget

import android.content.Context
import android.content.res.Configuration
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import com.example.njupter.R
import com.example.njupter.domain.getTodayDayOfWeek
import com.example.njupter.widget.ui.CoursesWidgetContent
import com.example.njupter.widget.ui.widgetColorProviders

class CourseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entries = WidgetDataManager.loadWidgetState(context)
        val bgPath = WidgetSettingsManager.getBackgroundImagePath(context)
        val transparency = WidgetSettingsManager.getBackgroundTransparency(context)
        val dayName = context.getString(
            when (getTodayDayOfWeek()) {
                1 -> R.string.day_mon
                2 -> R.string.day_tue
                3 -> R.string.day_wed
                4 -> R.string.day_thu
                5 -> R.string.day_fri
                6 -> R.string.day_sat
                else -> R.string.day_sun
            }
        )
        val weekNumber = WidgetModels.computeTodayWeekNumber(context)
        val isDark = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val colors = widgetColorProviders(context, isDark)

        provideContent {
            CoursesWidgetContent(
                entries = entries,
                backgroundImagePath = bgPath,
                transparency = transparency,
                colors = colors,
                headerTitle = context.getString(R.string.widget_today_format, dayName),
                weekLabel = weekNumber?.let { context.getString(R.string.week, it) }.orEmpty(),
                emptyText = context.getString(R.string.widget_no_courses_today),
                sectionLabel = { start, end ->
                    context.getString(R.string.widget_section_range, start, end)
                }
            )
        }
    }
}

class CourseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CourseWidget()
}
