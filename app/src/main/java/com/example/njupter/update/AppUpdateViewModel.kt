package com.example.njupter.update

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.njupter.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
    private val pending = MutableStateFlow<AppUpdate?>(null)
    val update = pending.asStateFlow()
    private val failed = MutableStateFlow(false)
    val downloadFailed = failed.asStateFlow()

    init {
        viewModelScope.launch {
            pending.value = AppUpdateRepository().check(
                BuildConfig.VERSION_CODE, preferences.getInt("ignored_version", 0)
            )
        }
    }

    fun ignore() {
        pending.value?.let { preferences.edit().putInt("ignored_version", it.versionCode).apply() }
        pending.value = null
        failed.value = false
    }

    fun download(): Boolean {
        val candidate = pending.value ?: return false
        return try {
            val context = getApplication<Application>()
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(candidate.apkUrl))
                .setTitle("NJUPTer ${candidate.versionName}")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS,
                    "NJUPTer-${candidate.versionCode}-${System.currentTimeMillis()}.apk")
            manager.enqueue(request)
            pending.value = null
            failed.value = false
            true
        } catch (_: RuntimeException) {
            failed.value = true
            false
        }
    }
}
