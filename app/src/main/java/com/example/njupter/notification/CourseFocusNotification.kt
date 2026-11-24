package com.example.njupter.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import com.example.njupter.R

internal object CourseFocusNotification {
    /** Call on IO: Xiaomi's permission provider can perform blocking IPC. */
    fun extras(context: Context, launch: PendingIntent, name: String, classroom: String,
               timeText: String, lifetimeMillis: Long): Bundle {
        return try {
            val protocol = Settings.System.getInt(context.contentResolver, "notification_focus_protocol", 0)
            if (protocol < 2) return Bundle()
            val permission = context.contentResolver.call(
                Uri.parse("content://miui.statusbar.notification.public"), "canShowFocus", null,
                Bundle().apply { putString("package", context.packageName) }
            )?.getBoolean("canShowFocus", false) == true
            if (!permission) return Bundle()
            val payload = CourseFocusPayload.build(protocol, name, classroom, timeText, lifetimeMillis)
                ?: return Bundle()
            Bundle().apply {
                putString("miui.focus.param", payload)
                putBundle("miui.focus.pics", Bundle().apply {
                    putParcelable(CourseFocusPayload.PIC, Icon.createWithResource(context, R.drawable.ic_course_reminder))
                })
                putBundle("miui.focus.actions", Bundle().apply {
                    putParcelable(CourseFocusPayload.ACTION, Notification.Action.Builder(
                        Icon.createWithResource(context, R.drawable.ic_course_reminder), "查看课表", launch
                    ).build())
                })
            }
        } catch (_: Exception) {
            // Non-Xiaomi devices, unavailable providers and denied access keep the native notification.
            Bundle()
        }
    }
}
