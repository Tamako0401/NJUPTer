package com.example.njupter.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SettingsRepository {
    fun getShowWeekends(): Flow<Boolean>
    suspend fun setShowWeekends(show: Boolean)
    
    fun getLastSelectedTimetableId(): Flow<String?>
    suspend fun setLastSelectedTimetableId(id: String)
    fun peekLastSelectedTimetableId(): String? {
        return (getLastSelectedTimetableId() as? MutableStateFlow)?.value
    }
}

class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val _showWeekends = MutableStateFlow(prefs.getBoolean("show_weekends", false))
    private val _lastSelectedTimetableId = MutableStateFlow<String?>(prefs.getString("last_selected_timetable_id", null))
    
    override fun getShowWeekends(): Flow<Boolean> = _showWeekends.asStateFlow()

    override suspend fun setShowWeekends(show: Boolean) {
        prefs.edit().putBoolean("show_weekends", show).apply()
        _showWeekends.value = show
    }

    override fun getLastSelectedTimetableId(): Flow<String?> = _lastSelectedTimetableId.asStateFlow()

    override suspend fun setLastSelectedTimetableId(id: String) {
        prefs.edit().putString("last_selected_timetable_id", id).apply()
        _lastSelectedTimetableId.value = id
    }
}
