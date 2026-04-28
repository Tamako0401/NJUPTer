package com.example.njupter.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SettingsRepository {
    fun getShowWeekends(): Flow<Boolean>
    suspend fun setShowWeekends(show: Boolean)
    fun getAppLanguageTag(): Flow<String>
    suspend fun setAppLanguageTag(languageTag: String)
    fun getLastSelectedTimetableId(): Flow<String?>
    suspend fun setLastSelectedTimetableId(id: String)
    fun getLastWeekRecords(): Flow<Map<String, Int>>
    suspend fun setLastWeekForTimetable(id: String, week: Int)
    fun getEnableCurrentTimeIndicator(): Flow<Boolean>
    suspend fun setEnableCurrentTimeIndicator(enabled: Boolean)

    fun peekLastSelectedTimetableId(): String? {
        return (getLastSelectedTimetableId() as? MutableStateFlow)?.value
    }

    fun peekAppLanguageTag(): String {
        return (getAppLanguageTag() as? MutableStateFlow)?.value ?: ""
    }
}

class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {
    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_SHOW_WEEKENDS = "show_weekends"
        private const val KEY_APP_LANGUAGE_TAG = "app_language_tag"
        private const val KEY_LAST_SELECTED_TIMETABLE_ID = "last_selected_timetable_id"
        private const val KEY_LAST_WEEK_PREFIX = "last_week_"
        private const val KEY_CURRENT_TIME_INDICATOR = "enable_current_time_indicator"
    }

    private fun readLastWeekRecords(): Map<String, Int> {
        return prefs.all.entries
            .asSequence()
            .filter { it.key.startsWith(KEY_LAST_WEEK_PREFIX) }
            .mapNotNull { entry ->
                val timetableId = entry.key.removePrefix(KEY_LAST_WEEK_PREFIX)
                val week = entry.value as? Int
                if (timetableId.isNotEmpty() && week != null) timetableId to week else null
            }
            .toMap()
    }

    private fun lastWeekKey(id: String): String = KEY_LAST_WEEK_PREFIX + id

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _showWeekends = MutableStateFlow(prefs.getBoolean(KEY_SHOW_WEEKENDS, false))
    private val _appLanguageTag = MutableStateFlow(prefs.getString(KEY_APP_LANGUAGE_TAG, "") ?: "")
    private val _lastSelectedTimetableId = MutableStateFlow<String?>(prefs.getString(KEY_LAST_SELECTED_TIMETABLE_ID, null))
    private val _lastWeekRecords = MutableStateFlow(readLastWeekRecords())
    private val _enableCurrentTimeIndicator = MutableStateFlow(prefs.getBoolean(KEY_CURRENT_TIME_INDICATOR, true))

    override fun getShowWeekends(): Flow<Boolean> = _showWeekends.asStateFlow()

    override suspend fun setShowWeekends(show: Boolean) {
        prefs.edit { putBoolean(KEY_SHOW_WEEKENDS, show) }
        _showWeekends.value = show
    }

    override fun getAppLanguageTag(): Flow<String> = _appLanguageTag.asStateFlow()

    override suspend fun setAppLanguageTag(languageTag: String) {
        prefs.edit { putString(KEY_APP_LANGUAGE_TAG, languageTag) }
        _appLanguageTag.value = languageTag
    }

    override fun getLastSelectedTimetableId(): Flow<String?> = _lastSelectedTimetableId.asStateFlow()

    override suspend fun setLastSelectedTimetableId(id: String) {
        prefs.edit { putString(KEY_LAST_SELECTED_TIMETABLE_ID, id) }
        _lastSelectedTimetableId.value = id
    }

    override fun getLastWeekRecords(): Flow<Map<String, Int>> = _lastWeekRecords.asStateFlow()

    override suspend fun setLastWeekForTimetable(id: String, week: Int) {
        prefs.edit { putInt(lastWeekKey(id), week) }
        _lastWeekRecords.value = _lastWeekRecords.value.toMutableMap().apply {
            put(id, week)
        }
    }

    override fun getEnableCurrentTimeIndicator(): Flow<Boolean> = _enableCurrentTimeIndicator.asStateFlow()

    override suspend fun setEnableCurrentTimeIndicator(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CURRENT_TIME_INDICATOR, enabled) }
        _enableCurrentTimeIndicator.value = enabled
    }
}
