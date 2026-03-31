package com.example.lobra.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val suggestedLocationName: String? = null,
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val isImportant: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val repeatMode: String? = null, // "None", "Daily", "Monthly", "Yearly", "CustomDate"
    val repeatValue: Int? = null,   // Only used for "CustomDate" (e.g. 5 for 5th of Month)
    val isDeleted: Boolean = false,
    val attachmentUri: String? = null
)
