package com.example.lobra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.example.lobra.data.Reminder
import com.example.lobra.viewmodel.LobraViewModel
import kotlinx.coroutines.launch

@Composable
fun StarredScreen(
    viewModel: LobraViewModel,
    onNavigateToDetail: (Reminder) -> Unit,
    onBack: () -> Unit
) {
    val reminders by viewModel.reminders.collectAsState()
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var completingIds by remember { mutableStateOf(setOf<Int>()) }
    val isMultiSelectionMode = selectedIds.isNotEmpty()
    val coroutineScope = rememberCoroutineScope()
    val starredReminders = reminders.filter { it.isImportant && (!it.isCompleted || completingIds.contains(it.id)) && !it.isDeleted }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isMultiSelectionMode) {
                val allSelected = starredReminders.isNotEmpty() && selectedIds.size == starredReminders.size
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
                                    else selectedIds = starredReminders.map { it.id }.toSet()
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
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Starred",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 32.sp
                    )
                }
            }

            if (starredReminders.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.StarBorder,
                        contentDescription = "Empty",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No reminders are starred",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(starredReminders, key = { it.id }) { reminder ->
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
                    val items = starredReminders.filter { selectedIds.contains(it.id) }
                    items.forEach { viewModel.toggleComplete(it) }
                    selectedIds = emptySet()
                }.padding(8.dp)) {
                    Icon(Icons.Filled.Check, contentDescription = "Complete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Complete", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                    val items = starredReminders.filter { selectedIds.contains(it.id) }
                    viewModel.unmarkImportantMultiple(items)
                    selectedIds = emptySet()
                }.padding(8.dp)) {
                    Icon(Icons.Filled.Star, contentDescription = "Important", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Unstar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                    val items = starredReminders.filter { selectedIds.contains(it.id) }
                    viewModel.softDeleteMultiple(items)
                    selectedIds = emptySet()
                }.padding(8.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}
