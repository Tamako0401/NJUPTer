package com.example.njupter.widget

import android.content.Context

object WidgetSettingsManager {
    private const val PREFS_NAME = "widget_settings"
    private const val KEY_BG_PATH = "background_image_path"
    private const val KEY_TRANSPARENCY = "background_transparency"

    fun getBackgroundImagePath(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BG_PATH, null)
    }

    fun setBackgroundImagePath(context: Context, path: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BG_PATH, path)
            .apply()
    }

    fun getBackgroundTransparency(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_TRANSPARENCY, 128)
    }

    fun setBackgroundTransparency(context: Context, transparency: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_TRANSPARENCY, transparency.coerceIn(0, 255))
            .apply()
    }
}
