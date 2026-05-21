package com.example.njupter.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetDataManager {
    private const val PREFS_NAME = "widget_courses"
    private const val KEY_DATA = "today_courses_json"
    private val gson = Gson()

    fun saveWidgetState(context: Context, entries: List<WidgetCourseEntry>) {
        val json = gson.toJson(entries)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DATA, json)
            .apply()
    }

    fun loadWidgetState(context: Context): List<WidgetCourseEntry> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DATA, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WidgetCourseEntry>>() {}.type
            gson.fromJson(json, type)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun refreshWidget(context: Context) {
        val entries = WidgetModels.computeTodaysCourses(context)
        saveWidgetState(context, entries)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CourseWidget().updateAll(context)
            } catch (_: Exception) {
            }
        }
    }
}
