package com.example.njupter.widget

import android.content.Context
import android.content.res.Configuration
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import com.example.njupter.widget.ui.CoursesWidgetContent

class CourseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entries = WidgetDataManager.loadWidgetState(context)
        val bgPath = WidgetSettingsManager.getBackgroundImagePath(context)
        val transparency = WidgetSettingsManager.getBackgroundTransparency(context)
        val isDark = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        provideContent {
            CoursesWidgetContent(
                entries = entries,
                backgroundImagePath = bgPath,
                transparency = transparency,
                isDark = isDark
            )
        }
    }
}

class CourseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CourseWidget()
}
