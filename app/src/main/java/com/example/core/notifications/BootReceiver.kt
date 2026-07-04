package com.example.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            thread {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context,
                        ReminderReceiverEntryPoint::class.java
                    )
                    val repo = entryPoint.calendarRepository()
                    val reminderManager = entryPoint.reminderManager()

                    runBlocking {
                        val events = repo.getAllEvents().first()
                        val now = System.currentTimeMillis()
                        events.filter { it.reminderAt != null && it.reminderAt > now }
                            .forEach { item ->
                                reminderManager.scheduleReminder(item)
                            }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
