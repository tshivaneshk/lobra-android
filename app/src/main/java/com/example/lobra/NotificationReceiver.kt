package com.example.lobra

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.lobra.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("REMINDER_ID", 0)

        if (intent.action == "ACTION_COMPLETE") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                val dao = AppDatabase.getDatabase(context).reminderDao()
                val reminder = dao.getReminderById(id)
                if (reminder != null) {
                    val updated = reminder.copy(isCompleted = true, completedAt = System.currentTimeMillis(), isDeleted = true)
                    dao.updateReminder(updated)
                    try {
                        com.example.lobra.widget.ReminderWidget().updateAll(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(id)
                pendingResult.finish()
            }
            return
        }

        val title = intent.getStringExtra("REMINDER_TITLE") ?: "Lobra Reminder"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "lobra_reminders"
            val channelName = "Reminders"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // Click on notification triggers MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingContentIntent = PendingIntent.getActivity(
            context, id, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Complete Action
        val completeIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "ACTION_COMPLETE"
            putExtra("REMINDER_ID", id)
        }
        val pendingCompleteIntent = PendingIntent.getBroadcast(
            context, id * 10, completeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(context, FullScreenReminderActivity::class.java).apply {
            putExtra("REMINDER_ID", id)
            putExtra("REMINDER_TITLE", title)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, id * 100, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "lobra_reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("It is time for your reminder!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingContentIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_edit, "Complete", pendingCompleteIntent)

        notificationManager.notify(id, builder.build())
    }
}
