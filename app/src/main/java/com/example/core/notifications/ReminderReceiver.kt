package com.example.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_IS_TASK = "extra_is_task"
        const val EXTRA_NOTE_ID = "extra_note_id"
        
        const val CHANNEL_ID = "cortex_reminders"
        
        const val ACTION_MARK_DONE = "com.example.action.MARK_DONE"
        const val ACTION_SNOOZE = "com.example.action.SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Připomínka"
        val isTask = intent.getBooleanExtra(EXTRA_IS_TASK, false)
        val noteId = intent.getStringExtra(EXTRA_NOTE_ID)

        val action = intent.action
        
        // Handle actions from notification
        if (action == ACTION_MARK_DONE) {
            // we will need to update db, can launch service/worker or handle via goAsync()
            val pendingResult = goAsync()
            kotlin.concurrent.thread {
                try {
                    val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                        context,
                        ReminderReceiverEntryPoint::class.java
                    )
                    val repo = entryPoint.calendarRepository()
                    kotlinx.coroutines.runBlocking {
                        val item = repo.getEventById(itemId)
                        if (item != null && item.isTask) {
                            repo.addEvent(item.copy(isDone = true))
                        }
                    }
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(itemId.hashCode())
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }
        
        if (action == ACTION_SNOOZE) {
             val pendingResult = goAsync()
             kotlin.concurrent.thread {
                 try {
                     val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                         context,
                         ReminderReceiverEntryPoint::class.java
                     )
                     val repo = entryPoint.calendarRepository()
                     val reminderManager = entryPoint.reminderManager()
                     kotlinx.coroutines.runBlocking {
                         val item = repo.getEventById(itemId)
                         if (item != null) {
                             val snoozedItem = item.copy(reminderAt = System.currentTimeMillis() + 15 * 60 * 1000) // 15 mins snooze
                             repo.addEvent(snoozedItem)
                             reminderManager.scheduleReminder(snoozedItem)
                         }
                     }
                     val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                     notificationManager.cancel(itemId.hashCode())
                 } finally {
                     pendingResult.finish()
                 }
             }
             return
        }

        showNotification(context, itemId, title, isTask, noteId)
    }

    private fun showNotification(context: Context, itemId: String, title: String, isTask: Boolean, noteId: String?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Připomínky",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Upozornění na úkoly a události"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // can pass noteId or calendar fragment to open
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            itemId.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (isTask) "Úkol: $title" else "Událost: $title")
            .setContentText("Kliknutím otevřete aplikaci")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (isTask) {
            val markDoneIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_MARK_DONE
                putExtra(EXTRA_ITEM_ID, itemId)
            }
            val markDonePending = PendingIntent.getBroadcast(
                context,
                itemId.hashCode() + 1,
                markDoneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_edit, "Hotovo", markDonePending)
        }
        
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_ITEM_ID, itemId)
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            itemId.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_popup_sync, "Odložit o 15 min", snoozePending)

        notificationManager.notify(itemId.hashCode(), builder.build())
    }
}
