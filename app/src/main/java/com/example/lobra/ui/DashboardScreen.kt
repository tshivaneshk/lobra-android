package com.example.lobra.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.example.lobra.data.Reminder
import com.example.lobra.theme.*
import com.example.lobra.viewmodel.LobraViewModel
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: LobraViewModel,
    onNavigateToDetail: (Reminder) -> Unit,
    onNavigateToRecycleBin: () -> Unit,
    onNavigateToStarred: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToInsights: () -> Unit
) {
    val reminders by viewModel.reminders.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var completingIds by remember { mutableStateOf(setOf<Int>()) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    val isMultiSelectionMode = selectedIds.isNotEmpty()
    val coroutineScope = rememberCoroutineScope()

    val activeReminders = reminders.filter { 
        !it.isDeleted && (!it.isCompleted || completingIds.contains(it.id)) && 
        (if (searchQuery == "today") {
            val t = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0) }.timeInMillis
            it.dueDate != null && it.dueDate in t..(t + 86400000L)
        } else if (searchQuery == "scheduled") {
            val t = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0) }.timeInMillis
            it.dueDate != null && it.dueDate > t
        } else {
            searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
        })
    }
    
    var showAddSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.addSimpleReminder(spokenText)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            if (isMultiSelectionMode) {
                val allSelected = activeReminders.isNotEmpty() && selectedIds.size == activeReminders.size
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    if (allSelected) selectedIds = emptySet()
                                    else selectedIds = activeReminders.map { it.id }.toSet()
                                }
                                .padding(end = 16.dp)
                        ) {
                            Icon(if (allSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle, contentDescription = "All", tint = if (allSelected) Color(0xFF4285F4) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("All", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                        Text("${selectedIds.size} selected", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { selectedIds = emptySet() }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = PrimaryEmerald,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            placeholder = { Text("Search reminders...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true
                        )
                        IconButton(onClick = { 
                            isSearching = false
                            searchQuery = "" 
                        }) {
                            Icon(Icons.Filled.Close, "Close search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text(
                            "Reminder",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 32.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Filled.Search, "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Filled.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Recycle Bin", color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            showMenu = false
                                            onNavigateToRecycleBin()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Insights", color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            showMenu = false
                                            onNavigateToInsights()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Settings", color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            showMenu = false
                                            onNavigateToSettings()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    val todayStart = java.util.Calendar.getInstance().apply { 
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0) 
                    }.timeInMillis
                    val todayEnd = todayStart + 86400000L
                    
                    val todayCount = reminders.count { !it.isDeleted && !it.isCompleted && it.dueDate != null && it.dueDate in todayStart..todayEnd }
                    val schedCount = reminders.count { !it.isDeleted && !it.isCompleted && it.dueDate != null && it.dueDate > todayStart }
                    val allCount = reminders.count { !it.isDeleted && !it.isCompleted }
                    val starCount = reminders.count { !it.isDeleted && !it.isCompleted && it.isImportant }

                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Filled.CalendarToday, iconTint = PrimaryEmerald,
                                title = "Today", count = todayCount,
                                onClick = { searchQuery = "today" }
                            )
                            SummaryCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Filled.DateRange, iconTint = Color(0xFFE53935),
                                title = "Scheduled", count = schedCount,
                                onClick = { searchQuery = "scheduled" }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Filled.AllInbox, iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                title = "All", count = allCount,
                                onClick = { searchQuery = "" }
                            )
                            SummaryCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Filled.Star, iconTint = YellowImportant,
                                title = "Starred", count = starCount,
                                onClick = onNavigateToStarred
                            )
                        }
                    }

                    if (activeReminders.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Inbox, contentDescription = "Empty", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No reminders", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        Text(
                            text = "My Reminders",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }

                if (activeReminders.isNotEmpty()) {
                    items(activeReminders, key = { it.id }) { reminder ->
                        ReminderListItem(
                            reminder = reminder,
                            onClick = { 
                                if (isMultiSelectionMode) {
                                    if (selectedIds.contains(reminder.id)) selectedIds = selectedIds - reminder.id
                                    else selectedIds = selectedIds + reminder.id
                                } else {
                                    onNavigateToDetail(reminder)
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelectionMode) {
                                    selectedIds = setOf(reminder.id)
                                }
                            },
                            onToggleComplete = { 
                                if (!isMultiSelectionMode) {
                                    if (!completingIds.contains(reminder.id)) {
                                        completingIds = completingIds + reminder.id
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(800)
                                            viewModel.toggleComplete(reminder)
                                            completingIds = completingIds - reminder.id
                                        }
                                    }
                                } else {
                                    if (selectedIds.contains(reminder.id)) selectedIds = selectedIds - reminder.id
                                    else selectedIds = selectedIds + reminder.id
                                }
                            },
                            modifier = Modifier.animateItem(),
                            isCompleting = completingIds.contains(reminder.id),
                            isSelected = selectedIds.contains(reminder.id)
                        )
                    }
                }
            }
        }

        if (isMultiSelectionMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                    val items = activeReminders.filter { selectedIds.contains(it.id) }
                    items.forEach { viewModel.toggleComplete(it) }
                    selectedIds = emptySet()
                }.padding(8.dp)) {
                    Icon(Icons.Filled.Check, contentDescription = "Complete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Complete", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                    val items = activeReminders.filter { selectedIds.contains(it.id) }
                    viewModel.markImportantMultiple(items)
                    selectedIds = emptySet()
                }.padding(8.dp)) {
                    Icon(Icons.Filled.Star, contentDescription = "Important", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Important", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                    val items = activeReminders.filter { selectedIds.contains(it.id) }
                    viewModel.softDeleteMultiple(items)
                    selectedIds = emptySet()
                }.padding(8.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        } else {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.95f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "fabScale"
            )
            if (!showAddSheet) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                        .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)), RoundedCornerShape(32.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current
                        ) { showAddSheet = true }
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, "Add", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Add reminder", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            try {
                                val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                }
                                speechLauncher.launch(intent)
                            } catch (e: Exception) {
                                viewModel.showSnackbar("Speech recognition not available")
                            }
                        }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Mic, "Mic", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddReminderSheet(
            onDismiss = { showAddSheet = false },
            onAdd = { title, selectedDueDate, rMode, rVal, attachmentUri -> 
                viewModel.addSimpleReminder(title, selectedDueDate, rMode, rVal, location = null, attachmentUri = attachmentUri)
                showAddSheet = false
            }
        )
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(86.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(32.dp).background(iconTint.copy(alpha = 0.10f), androidx.compose.foundation.shape.CircleShape), 
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(18.dp))
                }
                Text(count.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Serif)
            }
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReminderListItem(
    reminder: Reminder,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier,
    isCompleting: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    val effectivelyCompleted = reminder.isCompleted || isCompleting
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "itemScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer { 
                scaleX = scale
                scaleY = scale
                shadowElevation = if(isPressed) 12f else 4f
                shape = RoundedCornerShape(24.dp)
                clip = true
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryEmerald.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onToggleComplete)
                    .background(
                        color = if (isSelected) Color(0xFF4285F4) else if (effectivelyCompleted) PrimaryEmerald else Color.Transparent, 
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .border(
                        1.5.dp, 
                        if (isSelected || effectivelyCompleted) Color.Transparent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f), 
                        androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected || effectivelyCompleted) {
                    Icon(Icons.Filled.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    color = if (effectivelyCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    textDecoration = if (effectivelyCompleted) TextDecoration.LineThrough else null
                )
                if (reminder.suggestedLocationName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📍 ${reminder.suggestedLocationName}",
                        color = PrimaryEmerald,
                        fontSize = 14.sp,
                        textDecoration = if (effectivelyCompleted) TextDecoration.LineThrough else null
                    )
                }
                if (reminder.dueDate != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = java.text.SimpleDateFormat("MMM dd, yyyy h:mm a", java.util.Locale.getDefault()).format(java.util.Date(reminder.dueDate)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Serif,
                        textDecoration = if (effectivelyCompleted) TextDecoration.LineThrough else null
                    )
                }
            }
        }
    }
}
