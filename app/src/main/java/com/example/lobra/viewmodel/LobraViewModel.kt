package com.example.lobra.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.lobra.data.AppDatabase
import com.example.lobra.data.Reminder
import com.example.lobra.data.ReminderDao
import com.example.lobra.network.LocationSuggestion
import com.example.lobra.network.RetrofitInstance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.*

data class ViewState(
    val selectedReminder: Reminder? = null,
    val suggestions: List<SuggestionItem> = emptyList(),
    val suggestionsLoading: Boolean = false,
    val snackbarMessage: String? = null
)

data class SuggestionItem(
    val name: String,
    val distanceKm: Double,
    val lat: Double,
    val lon: Double
)

class LobraViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val dao = AppDatabase.getDatabase(context).reminderDao()
        @Suppress("UNCHECKED_CAST")
        return LobraViewModel(dao, context) as T
    }
}

class LobraViewModel(
    private val dao: ReminderDao,
    private val applicationContext: Context
) : ViewModel() {

    val reminders = dao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    private val sharedPrefs = applicationContext.getSharedPreferences("lobra_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "System Default") ?: "System Default")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()
    
    private val _isAppLockEnabled = MutableStateFlow(sharedPrefs.getBoolean("app_lock", false))
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(applicationContext)

    init {
        cleanUpOldRecycleBinItems()
        viewModelScope.launch {
            reminders.collect {
                triggerWidgetRefresh()
            }
        }
    }

    private fun triggerWidgetRefresh() {
        viewModelScope.launch {
            try {
                com.example.lobra.widget.ReminderWidget().updateAll(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setThemeMode(mode: String) {
        sharedPrefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun setAppLock(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("app_lock", enabled).apply()
        _isAppLockEnabled.value = enabled
    }

    private fun cleanUpOldRecycleBinItems() {
        viewModelScope.launch {
            // 30 days = 30 * 24 * 60 * 60 * 1000 = 2592000000 ms
            val threshold = System.currentTimeMillis() - 2592000000L
            dao.deleteOldRecycleBinReminders(threshold)
        }
    }

    fun restoreAllRecycleBin() {
        viewModelScope.launch {
            dao.restoreAllReminders()
            // Reschedule logic could go here if keeping alarms
            showSnackbar("All reminders restored")
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            dao.emptyRecycleBin()
            showSnackbar("Recycle bin emptied")
        }
    }

    private fun scheduleNotification(reminder: Reminder) {
        if (reminder.dueDate == null) return
        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(applicationContext, com.example.lobra.NotificationReceiver::class.java).apply {
            putExtra("REMINDER_TITLE", reminder.title)
            putExtra("REMINDER_ID", reminder.id)
        }
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            applicationContext, reminder.id, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, reminder.dueDate, pendingIntent)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun cancelNotification(reminder: Reminder) {
        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(applicationContext, com.example.lobra.NotificationReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            applicationContext, reminder.id, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun parseNaturalLanguageDate(input: String): Triple<String, Long?, Boolean> {
        var cleanTitle = input
        var targetDate: java.util.Calendar? = null
        val lowerInput = input.lowercase()

        var isImportant = false
        val priorityRegex = Regex("(?i)\\b(urgent|important|asap|high priority|critical|star this)\\b")
        if (priorityRegex.containsMatchIn(lowerInput)) {
            isImportant = true
            cleanTitle = cleanTitle.replace(priorityRegex, "").trim()
        }

        val calendar = java.util.Calendar.getInstance()
        var targetHour = -1
        var targetMinute = 0

        // Time Regex parsing (e.g. at 5 pm, 5:30am, 17:00)
        val timeRegex = Regex("(?i)(?:at\\s+)?(1[0-2]|0?[1-9]|2[0-3])(?::([0-5][0-9]))?\\s*(am|pm)?\\b")
        val timeMatch = timeRegex.find(lowerInput)
        if (timeMatch != null) {
            val hourStr = timeMatch.groupValues[1]
            val minStr = timeMatch.groupValues[2]
            val amPm = timeMatch.groupValues[3]
            
            var hour = hourStr.toInt()
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            
            targetHour = hour
            if (minStr.isNotEmpty()) targetMinute = minStr.toInt()
            cleanTitle = cleanTitle.replace(timeMatch.value, "", ignoreCase = true)
        } else if (lowerInput.contains("at noon")) {
            targetHour = 12
            targetMinute = 0
            cleanTitle = cleanTitle.replace("at noon", "", ignoreCase = true)
        } else if (lowerInput.contains("at midnight")) {
            targetHour = 0
            targetMinute = 0
            cleanTitle = cleanTitle.replace("at midnight", "", ignoreCase = true)
        } else if (lowerInput.contains("in the morning")) {
            targetHour = 9; targetMinute = 0; cleanTitle = cleanTitle.replace("in the morning", "", ignoreCase = true)
            if (targetDate == null && calendar.get(java.util.Calendar.HOUR_OF_DAY) >= 9) {
                targetDate = calendar.clone() as java.util.Calendar
                targetDate.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        } else if (lowerInput.contains("in the afternoon")) {
            targetHour = 14; targetMinute = 0; cleanTitle = cleanTitle.replace("in the afternoon", "", ignoreCase = true)
            if (targetDate == null && calendar.get(java.util.Calendar.HOUR_OF_DAY) >= 14) {
                targetDate = calendar.clone() as java.util.Calendar
                targetDate.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        } else if (lowerInput.contains("in the evening")) {
            targetHour = 19; targetMinute = 0; cleanTitle = cleanTitle.replace("in the evening", "", ignoreCase = true)
            if (targetDate == null && calendar.get(java.util.Calendar.HOUR_OF_DAY) >= 19) {
                targetDate = calendar.clone() as java.util.Calendar
                targetDate.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Exact Date Parsing (e.g. March 15th, Apr 2nd)
        val months = listOf("january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december", "jan", "feb", "mar", "apr", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
        val monthPattern = months.joinToString("|")
        val dateRegex = Regex("(?i)\\b(?:on\\s+)?($monthPattern)\\s+([0-3]?[0-9])(?:st|nd|rd|th)?\\b")
        val dateMatch = dateRegex.find(lowerInput)
        if (dateMatch != null) {
            val monthStr = dateMatch.groupValues[1].lowercase()
            val dayStr = dateMatch.groupValues[2].toInt()
            
            val monthIndex = months.indexOfFirst { it.startsWith(monthStr) || it == monthStr } % 12
            targetDate = calendar.clone() as java.util.Calendar
            targetDate.set(java.util.Calendar.MONTH, monthIndex)
            targetDate.set(java.util.Calendar.DAY_OF_MONTH, dayStr)
            
            if (targetDate.timeInMillis < System.currentTimeMillis() - 86400000L) {
                targetDate.add(java.util.Calendar.YEAR, 1) // Push to next year if date passed
            }
            cleanTitle = cleanTitle.replace(dateMatch.value, "", ignoreCase = true)
        }

        // Days of week parsing
        if (targetDate == null) {
            val days = listOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
            for ((index, day) in days.withIndex()) {
                val dayRegex = Regex("(?i)\\b(?:on\\s+)?(?:next\\s+)?$day\\b")
                val dayMatch = dayRegex.find(lowerInput)
                if (dayMatch != null) {
                    targetDate = calendar.clone() as java.util.Calendar
                    val currentDayOfWeek = targetDate.get(java.util.Calendar.DAY_OF_WEEK)
                    val targetDayOfWeek = index + 1
                    var daysToAdd = targetDayOfWeek - currentDayOfWeek
                    if (daysToAdd <= 0 || lowerInput.contains("next $day")) daysToAdd += 7
                    targetDate.add(java.util.Calendar.DAY_OF_YEAR, daysToAdd)
                    cleanTitle = cleanTitle.replace(dayMatch.value, "", ignoreCase = true)
                    break
                }
            }
        }

        // Relative Temporal parsing
        if (targetDate == null) {
            val daysRegex = Regex("(?i)in (\\d+) days?")
            val weeksRegex = Regex("(?i)in (\\d+) weeks?")
            val hoursRegex = Regex("(?i)in (\\d+) hours?")
            val minsRegex = Regex("(?i)in (\\d+) minutes?")
            
            val daysMatch = daysRegex.find(lowerInput)
            val weeksMatch = weeksRegex.find(lowerInput)
            val hoursMatch = hoursRegex.find(lowerInput)
            val minsMatch = minsRegex.find(lowerInput)

            if (daysMatch != null) {
                targetDate = calendar.clone() as java.util.Calendar
                targetDate.add(java.util.Calendar.DAY_OF_YEAR, daysMatch.groupValues[1].toInt())
                cleanTitle = cleanTitle.replace(daysMatch.value, "", ignoreCase = true)
            } else if (weeksMatch != null) {
                targetDate = calendar.clone() as java.util.Calendar
                targetDate.add(java.util.Calendar.DAY_OF_YEAR, weeksMatch.groupValues[1].toInt() * 7)
                cleanTitle = cleanTitle.replace(weeksMatch.value, "", ignoreCase = true)
            } else if (hoursMatch != null) {
                targetDate = calendar.clone() as java.util.Calendar
                targetDate.add(java.util.Calendar.HOUR_OF_DAY, hoursMatch.groupValues[1].toInt())
                cleanTitle = cleanTitle.replace(hoursMatch.value, "", ignoreCase = true)
                targetHour = -1 // Override precise hour lock, relative adds implicitly
            } else if (minsMatch != null) {
                targetDate = calendar.clone() as java.util.Calendar
                targetDate.add(java.util.Calendar.MINUTE, minsMatch.groupValues[1].toInt())
                cleanTitle = cleanTitle.replace(minsMatch.value, "", ignoreCase = true)
                targetHour = -1
            } else if (lowerInput.contains("tomorrow")) {
                targetDate = calendar.clone() as java.util.Calendar
                targetDate.add(java.util.Calendar.DAY_OF_YEAR, 1)
                cleanTitle = cleanTitle.replace("tomorrow", "", ignoreCase = true)
            } else if (lowerInput.contains("today")) {
                targetDate = calendar.clone() as java.util.Calendar
                cleanTitle = cleanTitle.replace("today", "", ignoreCase = true)
            } else if (lowerInput.contains("next week")) {
                targetDate = calendar.clone() as java.util.Calendar
                targetDate.add(java.util.Calendar.DAY_OF_YEAR, 7)
                cleanTitle = cleanTitle.replace("next week", "", ignoreCase = true)
            } else if (lowerInput.contains("next weekend")) {
                targetDate = calendar.clone() as java.util.Calendar
                val currentDayOfWeek = targetDate.get(java.util.Calendar.DAY_OF_WEEK)
                var daysToSaturday = java.util.Calendar.SATURDAY - currentDayOfWeek
                if (daysToSaturday <= 0) daysToSaturday += 7
                targetDate.add(java.util.Calendar.DAY_OF_YEAR, daysToSaturday)
                if (targetHour == -1) targetHour = 9
                cleanTitle = cleanTitle.replace("next weekend", "", ignoreCase = true)
            } else if (lowerInput.contains("end of week")) {
                targetDate = calendar.clone() as java.util.Calendar
                val currentDayOfWeek = targetDate.get(java.util.Calendar.DAY_OF_WEEK)
                var daysToFriday = java.util.Calendar.FRIDAY - currentDayOfWeek
                if (daysToFriday <= 0) daysToFriday += 7
                targetDate.add(java.util.Calendar.DAY_OF_YEAR, daysToFriday)
                if (targetHour == -1) targetHour = 17 // 5 PM default
                cleanTitle = cleanTitle.replace("end of week", "", ignoreCase = true)
            } else if (lowerInput.contains("tonight")) {
                targetDate = calendar.clone() as java.util.Calendar
                if (targetHour == -1) targetHour = 20 // default 8pm
                cleanTitle = cleanTitle.replace("tonight", "", ignoreCase = true)
            } else if (lowerInput.contains("next month")) {
                targetDate = calendar.clone() as java.util.Calendar
                targetDate.add(java.util.Calendar.MONTH, 1)
                cleanTitle = cleanTitle.replace("next month", "", ignoreCase = true)
            }
        }

        if (targetDate != null) {
            if (targetHour != -1) {
                targetDate.set(java.util.Calendar.HOUR_OF_DAY, targetHour)
                targetDate.set(java.util.Calendar.MINUTE, targetMinute)
            } else if (!lowerInput.contains("in an hour") && !lowerInput.contains("minute")) {
                // If it's pure Day targeting missing hour context, pin to 9am implicitly
                if (targetDate.get(java.util.Calendar.HOUR_OF_DAY) != calendar.get(java.util.Calendar.HOUR_OF_DAY)) {
                    targetDate.set(java.util.Calendar.HOUR_OF_DAY, 9)
                    targetDate.set(java.util.Calendar.MINUTE, 0)
                }
                // Push forward if implicitly behind
                if (targetDate.timeInMillis < System.currentTimeMillis()) targetDate.add(java.util.Calendar.HOUR_OF_DAY, 4)
            }
            targetDate.set(java.util.Calendar.SECOND, 0)
            targetDate.set(java.util.Calendar.MILLISECOND, 0)
            
            cleanTitle = cleanTitle.replace(Regex("(?i)\\b(?:on|at|by)\\b(?!\\s*\\w)"), "")
            return Triple(cleanTitle.trim().replace(Regex("\\s+"), " "), targetDate.timeInMillis, isImportant)
        }
        
        return Triple(cleanTitle.trim().replace(Regex("\\s+"), " "), null, isImportant)
    }

    fun addSimpleReminder(title: String, dueDate: Long? = null, repeatMode: String? = null, repeatValue: Int? = null, location: String? = null, attachmentUri: String? = null) {
        val (cleanTitle, finalDueDate, parsedImportant) = if (dueDate == null) parseNaturalLanguageDate(title) else Triple(title, dueDate, false)
        if (finalDueDate != null && finalDueDate <= System.currentTimeMillis()) {
            showSnackbar("Cannot set a reminder for the past!")
            return
        }
        viewModelScope.launch {
            val newlyCreated = Reminder(
                title = cleanTitle, 
                dueDate = finalDueDate,
                repeatMode = repeatMode,
                repeatValue = repeatValue,
                isImportant = parsedImportant,
                suggestedLocationName = location,
                attachmentUri = attachmentUri
            )
            val insertedId = dao.insertReminder(newlyCreated)
            if (finalDueDate != null) {
                scheduleNotification(newlyCreated.copy(id = insertedId.toInt()))
            }
            triggerWidgetRefresh()
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            val updated = reminder.copy(isDeleted = true, completedAt = System.currentTimeMillis())
            dao.updateReminder(updated)
            cancelNotification(reminder)
            triggerWidgetRefresh()
            showSnackbar("Moved to recycle bin")
        }
    }

    fun permanentlyDeleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            dao.deleteReminder(reminder)
            cancelNotification(reminder)
            showSnackbar("Reminder deleted permanently")
        }
    }

    fun softDeleteMultiple(reminders: List<Reminder>) {
        viewModelScope.launch {
            reminders.forEach {
                val updated = it.copy(isDeleted = true, completedAt = System.currentTimeMillis())
                dao.updateReminder(updated)
                cancelNotification(updated)
            }
            showSnackbar("Moved ${reminders.size} to recycle bin")
        }
    }

    fun markImportantMultiple(reminders: List<Reminder>) {
        viewModelScope.launch {
            reminders.forEach {
                val updated = it.copy(isImportant = true)
                dao.updateReminder(updated)
            }
        }
    }

    fun unmarkImportantMultiple(reminders: List<Reminder>) {
        viewModelScope.launch {
            reminders.forEach {
                val updated = it.copy(isImportant = false)
                dao.updateReminder(updated)
            }
        }
    }

    fun restoreReminder(reminder: Reminder) {
        viewModelScope.launch {
            val updated = reminder.copy(
                isDeleted = false, 
                isCompleted = false, 
                completedAt = null
            )
            dao.updateReminder(updated)
            if (updated.dueDate != null) scheduleNotification(updated)
        }
    }

    fun permanentlyDeleteMultiple(reminders: List<Reminder>) {
        viewModelScope.launch {
            reminders.forEach {
                dao.deleteReminder(it)
                cancelNotification(it)
            }
            showSnackbar("${reminders.size} deleted permanently")
        }
    }

    fun restoreMultiple(reminders: List<Reminder>) {
        viewModelScope.launch {
            reminders.forEach {
                val updated = it.copy(
                    isDeleted = false, 
                    isCompleted = false, 
                    completedAt = null
                )
                dao.updateReminder(updated)
                if (updated.dueDate != null) scheduleNotification(updated)
            }
            showSnackbar("${reminders.size} restored")
        }
    }

    fun updateReminderDetails(reminder: Reminder, newTitle: String, newDueDate: Long?, newRepeatMode: String? = null, newRepeatValue: Int? = null, newAttachmentUri: String? = null) {
        if (newDueDate != null && newDueDate <= System.currentTimeMillis() && newDueDate != reminder.dueDate) {
            showSnackbar("Cannot set a reminder for the past!")
            return
        }
        viewModelScope.launch {
            val updated = reminder.copy(
                title = newTitle, 
                dueDate = newDueDate,
                repeatMode = newRepeatMode,
                repeatValue = newRepeatValue,
                attachmentUri = newAttachmentUri
            )
            dao.updateReminder(updated)
            if (viewState.value.selectedReminder?.id == reminder.id) {
                _viewState.value = _viewState.value.copy(selectedReminder = updated)
            }
            if (newDueDate != null && !updated.isCompleted) {
                scheduleNotification(updated)
            } else {
                cancelNotification(updated)
            }
            triggerWidgetRefresh()
            showSnackbar("Reminder updated")
        }
    }

    fun toggleComplete(reminder: Reminder) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val isNowComplete = !reminder.isCompleted
            val updated = reminder.copy(
                isCompleted = isNowComplete,
                completedAt = if (isNowComplete) now else null,
                isDeleted = if (isNowComplete) true else reminder.isDeleted
            )
            dao.updateReminder(updated)
            if (viewState.value.selectedReminder?.id == reminder.id) {
                _viewState.value = _viewState.value.copy(selectedReminder = updated)
            }
            if (updated.isCompleted) cancelNotification(updated)
            else if (updated.dueDate != null) scheduleNotification(updated)
            triggerWidgetRefresh()
        }
    }

    fun toggleImportant(reminder: Reminder) {
        viewModelScope.launch {
            val updated = reminder.copy(isImportant = !reminder.isImportant)
            dao.updateReminder(updated)
            if (viewState.value.selectedReminder?.id == reminder.id) {
                _viewState.value = _viewState.value.copy(selectedReminder = updated)
            }
        }
    }

    fun shareReminder(context: Context, reminder: Reminder) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Reminder: ${reminder.title}")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    fun printDummyAction(action: String) {
        showSnackbar("$action action pressed.")
    }

    fun selectReminder(reminder: Reminder?) {
        _viewState.value = _viewState.value.copy(
            selectedReminder = reminder,
            suggestions = emptyList(),
            suggestionsLoading = false
        )
    }

    fun clearSnackbar() {
        _viewState.value = _viewState.value.copy(snackbarMessage = null)
    }

    fun showSnackbar(message: String) {
        _viewState.value = _viewState.value.copy(snackbarMessage = message)
    }

    @SuppressLint("MissingPermission")
    fun fetchSuggestionsFor(reminder: Reminder) {
        _viewState.value = _viewState.value.copy(suggestionsLoading = true, suggestions = emptyList())
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    searchNominatim(reminder.title, location.latitude, location.longitude)
                } else {
                    showSnackbar("Enable location to use these features")
                    _viewState.value = _viewState.value.copy(suggestionsLoading = false)
                }
            }
            .addOnFailureListener {
                showSnackbar("Enable location to use these features")
                _viewState.value = _viewState.value.copy(suggestionsLoading = false)
            }
    }

    private fun searchNominatim(rawTitle: String, userLat: Double, userLon: Double) {
        viewModelScope.launch {
            try {
                var queryInput = rawTitle.lowercase()
            
            val categoryMappings = mapOf(
            // Food & Dining
            "restaurant" to "restaurant", "food" to "restaurant", "eat" to "restaurant", "dine" to "restaurant", "lunch" to "restaurant", "dinner" to "restaurant", "breakfast" to "restaurant", "mess" to "restaurant", "canteen" to "restaurant", "pizza" to "restaurant", "burger" to "fast food", "fast food" to "fast food",
            "cafe" to "cafe", "coffee" to "cafe",
            "bakery" to "bakery", "cake" to "bakery", "pastry" to "bakery",

            // Medical & Health
            "hospital" to "hospital", "clinic" to "hospital", "treatment" to "hospital", "doctor" to "hospital", "medical" to "hospital",
            "dental" to "dental", "dentist" to "dental",
            "pharmacy" to "pharmacy", "medicine" to "pharmacy", "pill" to "pharmacy", "prescription" to "pharmacy", "drugs" to "pharmacy",

            // Shopping & Groceries
            "mall" to "mall", "shopping" to "mall", "store" to "mall",
            "supermarket" to "supermarket", "groceries" to "supermarket", "grocery" to "supermarket", "milk" to "supermarket", "vegetables" to "supermarket", "fruits" to "supermarket", "bread" to "supermarket",

            // Entertainment & Recreation
            "movie" to "cinema", "theater" to "cinema", "cinema" to "cinema",
            "park" to "park", "garden" to "park", "walk" to "park",

            // Financial & Post
            "bank" to "bank",
            "atm" to "atm", "cash" to "atm",
            "post" to "post office", "mail" to "post office", "parcel" to "post office", "package" to "post office", "courier" to "post office", "stamp" to "post office",

            // Fitness & Personal Care
            "gym" to "gym", "workout" to "gym", "fitness" to "gym", "exercise" to "gym", "training" to "gym",
            "salon" to "salon", "haircut" to "salon", "barber" to "salon", "hair" to "salon",
            "spa" to "spa", "massage" to "spa",

            // Auto & Travel
            "gas" to "gas station", "fuel" to "gas station", "petrol" to "gas station", "diesel" to "gas station", "pump" to "gas station",
            "car wash" to "car wash", "wash car" to "car wash",
            "mechanic" to "car repair", "auto repair" to "car repair", "service car" to "car repair",
            "train" to "train station", "railway" to "train station",
            "bus" to "bus station", "bus stop" to "bus station",
            "airport" to "airport", "flight" to "airport", "plane" to "airport",
            "hotel" to "hotel", "room" to "hotel", "motel" to "hotel",

            // Pets & Education
            "vet" to "veterinary", "veterinary" to "veterinary", "dog" to "veterinary", "cat" to "veterinary",
            "pet food" to "pet store", "pet" to "pet store",
            "library" to "library", "book" to "library", "study" to "library",
            "school" to "school", "college" to "college", "university" to "university"
        )

        var extractedKeyword: String? = null
        for ((key, category) in categoryMappings) {
            if (Regex("\\b$key\\b").containsMatchIn(queryInput)) {
                extractedKeyword = category
                break
            }
        }

        val stopWords = setOf(
            "go", "going", "to", "visit", "visiting", "with", "my", "our", "their", "his", "her", 
            "friends", "family", "a", "an", "the", "at", "in", "on", "some", "buy", "get", "meet",
            "for", "from", "pick", "up", "drop", "off", "do", "take", "make", "pay", "and", "or",
            "will", "shall", "can", "could", "would", "should", "want", "need"
        )
        val filteredWords = queryInput.split(Regex("\\s+"))
            .map { it.replace(Regex("[^a-z0-9]"), "") }
            .filter { it.isNotBlank() && it !in stopWords }

            val query: String
        if (extractedKeyword != null) {
            val keysToRemove = categoryMappings.keys
            val remaining = filteredWords.filter { it !in keysToRemove }.joinToString(" ").trim()
            query = if (remaining.isNotEmpty()) {
                "$extractedKeyword $remaining"
            } else {
                extractedKeyword
            }
        } else {
                query = filteredWords.joinToString(" ").trim()
            }
            
            if (query.isEmpty()) {
                 _viewState.value = _viewState.value.copy(suggestionsLoading = false)
                 return@launch
            }
            
            // Bounding box for 10km radius coverage
            val offset = 0.09 
            val minLon = userLon - offset
            val minLat = userLat - offset
            val maxLon = userLon + offset
            val maxLat = userLat + offset
            val viewbox = "$minLon,$maxLat,$maxLon,$minLat"

            var results = RetrofitInstance.api.searchLocation(
                query = query,
                viewbox = viewbox
            )

            // Fallback intelligently if the combined query yielded no results
            if (results.isEmpty() && extractedKeyword != null && query != extractedKeyword) {
                results = RetrofitInstance.api.searchLocation(
                    query = extractedKeyword,
                    viewbox = viewbox
                )
            }

            val suggestions = results.mapNotNull {
                val lat = it.lat.toDoubleOrNull() ?: return@mapNotNull null
                val lon = it.lon.toDoubleOrNull() ?: return@mapNotNull null
                val dist = haversine(userLat, userLon, lat, lon)
                if (dist <= 10.0) {
                        SuggestionItem(it.name.ifEmpty { it.displayName }, dist, lat, lon)
                    } else null
                }.sortedBy { it.distanceKm }.take(15)

                _viewState.value = _viewState.value.copy(
                    suggestions = suggestions,
                    suggestionsLoading = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _viewState.value = _viewState.value.copy(suggestionsLoading = false)
            }
        }
    }

    fun onSuggestionClicked(suggestion: SuggestionItem) {
        viewModelScope.launch {
            val title = "Visit ${suggestion.name}"
            val existing = dao.getDuplicateReminder(title, suggestion.lat, suggestion.lon)
            if (existing != null) {
                showSnackbar("Reminder already exists")
            } else {
                dao.insertReminder(
                    Reminder(
                        title = title,
                        latitude = suggestion.lat,
                        longitude = suggestion.lon,
                        suggestedLocationName = suggestion.name
                    )
                )
                showSnackbar("Reminder created successfully")
            }
        }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
