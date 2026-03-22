package com.example.njupter.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SettingsRepository {
    fun getShowWeekends(): Flow<Boolean>
    suspend fun setShowWeekends(show: Boolean)
}

class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val _showWeekends = MutableStateFlow(prefs.getBoolean("show_weekends", false))
    
    override fun getShowWeekends(): Flow<Boolean> = _showWeekends.asStateFlow()

    override suspend fun setShowWeekends(show: Boolean) {
        prefs.edit().putBoolean("show_weekends", show).apply()
        _showWeekends.value = show
    }
}

