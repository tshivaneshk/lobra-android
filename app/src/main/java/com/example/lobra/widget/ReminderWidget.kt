package com.example.lobra.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.background
import androidx.glance.color.ColorProvider
import com.example.lobra.MainActivity
import com.example.lobra.data.AppDatabase
import com.example.lobra.data.Reminder
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.cornerRadius
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class ReminderWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Fetch active reminders
        val dao = AppDatabase.getDatabase(context).reminderDao()
        val reminders = runBlocking { dao.getAllReminders().firstOrNull() }?.filter { !it.isCompleted && !it.isDeleted } ?: emptyList()
        val topReminders = reminders.take(5)

        provideContent {
            val cmpName = android.content.ComponentName(context, MainActivity::class.java)
            
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(day = Color(0xFFFCFDFB), night = Color(0xFF131A15))) // Surface cream color
                    .cornerRadius(24.dp)
                    .padding(16.dp)
                    .clickable(actionStartActivity(cmpName))
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lobra",
                        style = TextStyle(color = ColorProvider(day = Color(0xFF131A15), night = Color(0xFFFCFDFB)), fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        text = "${topReminders.size} Active",
                        style = TextStyle(color = ColorProvider(day = Color(0xFF2E8B57), night = Color(0xFF2E8B57)), fontSize = 14.sp)
                    )
                }
                
                Spacer(modifier = GlanceModifier.height(12.dp))
                
                if (topReminders.isEmpty()) {
                    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("You're all caught up!", style = TextStyle(color = ColorProvider(day = Color(0xFFA6ACA8), night = Color(0xFFA6ACA8)), fontSize = 14.sp))
                    }
                } else {
                    LazyColumn {
                        items(topReminders) { reminder ->
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = GlanceModifier.size(24.dp).padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = GlanceModifier
                                            .size(16.dp)
                                            .background(ColorProvider(day = Color(0xFFA6ACA8), night = Color(0xFFA6ACA8)))
                                            .cornerRadius(8.dp)
                                            .clickable(actionRunCallback<CompleteTaskAction>(actionParametersOf(ActionParameters.Key<Int>("reminderId") to reminder.id))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = GlanceModifier
                                                .size(12.dp)
                                                .background(ColorProvider(day = Color(0xFFFCFDFB), night = Color(0xFF131A15)))
                                                .cornerRadius(6.dp)
                                        ) {}
                                    }
                                }
                                Text(
                                    text = reminder.title.take(15) + if (reminder.title.length > 15) "..." else "",
                                    style = TextStyle(color = ColorProvider(day = Color(0xFF131A15), night = Color(0xFFFCFDFB)), fontSize = 14.sp),
                                    modifier = GlanceModifier.defaultWeight()
                                )
                                if (reminder.dueDate != null) {
                                    Spacer(modifier = GlanceModifier.width(8.dp))
                                    Text(
                                        text = java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(reminder.dueDate)),
                                        style = TextStyle(color = ColorProvider(day = Color(0xFF2E8B57), night = Color(0xFF2E8B57)), fontSize = 12.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val reminderId = parameters[ActionParameters.Key<Int>("reminderId")] ?: return
        val dao = AppDatabase.getDatabase(context).reminderDao()
        val reminder = dao.getAllReminders().firstOrNull()?.find { it.id == reminderId }
        
        if (reminder != null) {
            val now = System.currentTimeMillis()
            val updated = reminder.copy(
                isCompleted = true,
                completedAt = now,
                isDeleted = true
            )
            dao.updateReminder(updated)
            ReminderWidget().updateAll(context)
        }
    }
}
