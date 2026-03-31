package com.example.lobra.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lobra.data.Reminder
import com.example.lobra.theme.*
import com.example.lobra.viewmodel.LobraViewModel

@Composable
fun RecycleBinScreen(
    viewModel: LobraViewModel,
    onBack: () -> Unit
) {
    val reminders by viewModel.reminders.collectAsState()
    val deletedReminders = reminders.filter { it.isDeleted }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    val isMultiSelectionMode = selectedIds.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            if (isMultiSelectionMode) {
                val allSelected = deletedReminders.isNotEmpty() && selectedIds.size == deletedReminders.size
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 16.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    if (allSelected) selectedIds = emptySet()
                                    else selectedIds = deletedReminders.map { it.id }.toSet()
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
                        .padding(top = 48.dp, bottom = 16.dp, start = 24.dp, end = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Recycle Bin",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Items are deleted after 30 days", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }

            if (deletedReminders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Recycle bin is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
            } else {
                if (!isMultiSelectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.emptyRecycleBin() },
                            border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Empty Bin", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Button(
                            onClick = { viewModel.restoreAllRecycleBin() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald.copy(alpha = 0.2f), contentColor = PrimaryEmerald),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text("Restore All", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                ) {
                    items(deletedReminders, key = { it.id }) { reminder ->
                        RecycleBinListItem(
                            reminder = reminder,
                            isMultiSelectionMode = isMultiSelectionMode,
                            isSelected = selectedIds.contains(reminder.id),
                            onClick = {
                                if (isMultiSelectionMode) {
                                    if (selectedIds.contains(reminder.id)) selectedIds = selectedIds - reminder.id
                                    else selectedIds = selectedIds + reminder.id
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelectionMode) {
                                    selectedIds = setOf(reminder.id)
                                }
                            },
                            onRestore = { if (!isMultiSelectionMode) viewModel.restoreReminder(reminder) },
                            onDelete = { if (!isMultiSelectionMode) viewModel.permanentlyDeleteReminder(reminder) }
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
                    val items = deletedReminders.filter { selectedIds.contains(it.id) }
                    viewModel.restoreMultiple(items)
                    selectedIds = emptySet()
                }.padding(8.dp)) {
                    Icon(Icons.Filled.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Restore", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                    val items = deletedReminders.filter { selectedIds.contains(it.id) }
                    viewModel.permanentlyDeleteMultiple(items)
                    selectedIds = emptySet()
                }.padding(8.dp)) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Delete permanently", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecycleBinListItem(
    reminder: Reminder,
    isMultiSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "itemScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
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
        colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryEmerald.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectionMode) {
                Icon(
                    if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = "Select",
                    tint = if (isSelected) Color(0xFF4285F4) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                text = reminder.title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.LineThrough,
                modifier = Modifier.weight(1f)
            )
            if (!isMultiSelectionMode) {
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = onRestore, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Restore, "Restore", tint = PrimaryEmerald, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.DeleteForever, "Delete permanently", tint = ErrorColor, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
