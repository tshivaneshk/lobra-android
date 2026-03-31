package com.example.lobra.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY createdAt DESC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE title = :title AND latitude = :lat AND longitude = :lon LIMIT 1")
    suspend fun getDuplicateReminder(title: String, lat: Double, lon: Double): Reminder?

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Int): Reminder?

    @Query("DELETE FROM reminders WHERE isDeleted = 1 AND completedAt <= :thresholdTimestamp")
    suspend fun deleteOldRecycleBinReminders(thresholdTimestamp: Long)

    @Query("UPDATE reminders SET isDeleted = 0 WHERE isDeleted = 1")
    suspend fun restoreAllReminders()

    @Query("DELETE FROM reminders WHERE isDeleted = 1")
    suspend fun emptyRecycleBin()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)
}
