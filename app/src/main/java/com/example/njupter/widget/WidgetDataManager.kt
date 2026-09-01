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
    private const val KEY_DISPLAY_STATE = "display_state_json"
    private val gson = Gson()

    fun saveWidgetState(context: Context, state: WidgetDisplayState) {
        val json = gson.toJson(state)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DISPLAY_STATE, json)
            .apply()
    }

    fun loadWidgetState(context: Context): WidgetDisplayState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY_DISPLAY_STATE, null)?.let { json ->
            try {
                return gson.fromJson(json, WidgetDisplayState::class.java)
            } catch (_: Exception) {
            }
        }

        // Preserve the previous cached entry while upgrading from the old
        // list-only state format.
        val legacyJson = prefs.getString(KEY_DATA, null) ?: return WidgetDisplayState()
        return try {
            val type = object : TypeToken<List<WidgetCourseEntry>>() {}.type
            WidgetDisplayState(entries = gson.fromJson(legacyJson, type))
        } catch (_: Exception) {
            WidgetDisplayState()
        }
    }

    fun refreshWidget(context: Context) {
        val state = WidgetModels.computeWidgetDisplayState(context)
        saveWidgetState(context, state)
        state.nextRefreshAtMillis?.let { triggerAtMillis ->
            WidgetUpdateScheduler.scheduleRefresh(context, triggerAtMillis)
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CourseWidget().updateAll(context)
            } catch (_: Exception) {
            }
        }
    }
}
