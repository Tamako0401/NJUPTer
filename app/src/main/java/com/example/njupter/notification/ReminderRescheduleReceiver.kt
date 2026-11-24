package com.example.njupter.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.njupter.widget.WidgetUpdateScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetUpdateScheduler.enqueueRefresh(context.applicationContext)
                ReminderBootstrapper.rescheduleCurrentTimetable(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
