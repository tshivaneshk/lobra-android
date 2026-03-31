package com.example.lobra.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import com.example.lobra.theme.*
import com.example.lobra.viewmodel.LobraViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailScreen(
    viewModel: LobraViewModel,
    onBack: () -> Unit
) {
    val viewState by viewModel.viewState.collectAsState()
    val reminder = viewState.selectedReminder ?: return
    var suggestionsExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(reminder.title) }
    var editDueDate by remember { mutableStateOf(reminder.dueDate) }
    var editSelectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var editRepeatMode by remember { mutableStateOf(reminder.repeatMode ?: "Don't Repeat") }
    var editRepeatValueText by remember { mutableStateOf(reminder.repeatValue?.toString() ?: "") }
    var showRepeatMenu by remember { mutableStateOf(false) }
    var editAttachmentUri by remember { mutableStateOf(reminder.attachmentUri) }

    LaunchedEffect(reminder) {
        editTitle = reminder.title
        editDueDate = reminder.dueDate
        editRepeatMode = reminder.repeatMode ?: "Don't Repeat"
        editRepeatValueText = reminder.repeatValue?.toString() ?: ""
        editAttachmentUri = reminder.attachmentUri
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.fetchSuggestionsFor(reminder)
        }
    }
    
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                var sizeOk = true
                val cursor = context.contentResolver.query(it, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val sizeIndex = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            val size = c.getLong(sizeIndex)
                            if (size > 10 * 1024 * 1024) {  // 10MB
                                sizeOk = false
                                android.widget.Toast.makeText(context, "File exceeds 10MB limit.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                if (sizeOk) {
                    context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    editAttachmentUri = it.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { viewModel.toggleImportant(reminder) }) {
                    Icon(
                        if (reminder.isImportant) Icons.Filled.Star else Icons.Outlined.StarBorder, 
                        "Star", 
                        tint = if (reminder.isImportant) YellowImportant else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp).weight(1f).verticalScroll(rememberScrollState())) {
                Text(
                    text = reminder.title,
                    color = if (reminder.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else null
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(36.dp).background(PurpleNoAlert.copy(alpha=0.15f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Notifications, "Alert", tint = PurpleNoAlert, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("My reminders", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        
                        if (reminder.dueDate != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).background(PrimaryEmerald.copy(alpha=0.15f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.DateRange, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault()).format(Date(reminder.dueDate)),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (reminder.repeatMode != null && reminder.repeatMode != "Don't Repeat") {
                            Spacer(modifier = Modifier.height(16.dp))
                            val rText = if (reminder.repeatMode == "Repeat every X Date Of Month") 
                                "Repeats on the ${reminder.repeatValue}th of every month" 
                            else reminder.repeatMode
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.1f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = rText,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                if (reminder.attachmentUri != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val uri = android.net.Uri.parse(reminder.attachmentUri)
                                    val mimeType = context.contentResolver.getType(uri) ?: "*/*"
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, mimeType)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    viewModel.showSnackbar("Could not open attachment")
                                }
                            },
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha=0.15f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha=0.9f)),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(PrimaryEmerald.copy(alpha=0.15f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.AttachFile, "Attachment", tint = PrimaryEmerald, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("View Attachment", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                
                val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                Text(
                    text = "Created: ${sdf.format(Date(reminder.createdAt))}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.End).padding(top = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (reminder.latitude != null && reminder.longitude != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val queryName = android.net.Uri.encode(reminder.suggestedLocationName ?: reminder.title)
                                val uri = android.net.Uri.parse("geo:${reminder.latitude},${reminder.longitude}?q=$queryName")
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha=0.15f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha=0.9f)),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).background(PrimaryEmerald.copy(alpha=0.15f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Place, "Map", tint = PrimaryEmerald, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "View in maps",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha=0.15f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha=0.9f)),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    suggestionsExpanded = !suggestionsExpanded
                                    if (suggestionsExpanded && viewState.suggestions.isEmpty()) {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPermission) {
                                            viewModel.fetchSuggestionsFor(reminder)
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                        }
                                    }
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Location Suggestions",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        AnimatedVisibility(visible = suggestionsExpanded) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                if (viewState.suggestionsLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp),
                                        color = PrimaryEmerald
                                    )
                                } else if (viewState.suggestions.isEmpty()) {
                                    Text("No suggestions found nearby. Try enabling GPS or check permissions.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                } else {
                                    viewState.suggestions.forEach { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.onSuggestionClicked(suggestion) }
                                                .padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(suggestion.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                            Text(String.format(Locale.US, "%.1f km", suggestion.distanceKm), color = PrimaryEmerald, fontSize = 13.sp)
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }
            

            // Bottom action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomNavIcon(
                    icon = if (reminder.isCompleted) Icons.Outlined.RadioButtonUnchecked else Icons.Filled.Check, 
                    label = if (reminder.isCompleted) "Uncheck" else "Complete",
                    onClick = { viewModel.toggleComplete(reminder) }
                )
                BottomNavIcon(Icons.Outlined.Edit, "Edit", onClick = { 
                    editTitle = reminder.title
                    editDueDate = reminder.dueDate
                    showEditDialog = true 
                })
                BottomNavIcon(Icons.Outlined.Share, "Share", onClick = { viewModel.shareReminder(context, reminder) })
                BottomNavIcon(
                    icon = Icons.Outlined.Delete, 
                    label = "Delete", 
                    onClick = { 
                        viewModel.deleteReminder(reminder)
                        onBack()
                    }
                )
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Edit Reminder", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = PrimaryEmerald,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showDatePicker = true }) {
                        val dateText = editDueDate?.let { 
                            java.text.SimpleDateFormat("MMM dd, yyyy h:mm a", java.util.Locale.getDefault()).format(java.util.Date(it))
                        } ?: "Set Due Date & Time"
                        Text("Date: $dateText", color = PrimaryEmerald)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Repeat: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box {
                            TextButton(onClick = { showRepeatMenu = true }) {
                                Text(editRepeatMode, color = PrimaryEmerald)
                            }
                            DropdownMenu(
                                expanded = showRepeatMenu,
                                onDismissRequest = { showRepeatMenu = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                val options = listOf("Don't Repeat", "Repeat Every Day", "Repeat Every Month", "Repeat Every Year")
                                options.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = { 
                                            editRepeatMode = option
                                            showRepeatMenu = false 
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    if (editAttachmentUri != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val uriObj = android.net.Uri.parse(editAttachmentUri)
                                val mimeType = context.contentResolver.getType(uriObj) ?: ""
                                if (mimeType.startsWith("image/")) {
                                    coil.compose.AsyncImage(
                                        model = uriObj,
                                        contentDescription = "Preview",
                                        modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Outlined.AttachFile, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                var fileName = "Document"
                                try {
                                    val cursor = context.contentResolver.query(uriObj, null, null, null, null)
                                    cursor?.use { c ->
                                        if (c.moveToFirst()) {
                                            val nIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                            if (nIndex != -1) fileName = c.getString(nIndex) ?: "Document"
                                        }
                                    }
                                } catch(e: Exception) {}
                                
                                Text(
                                    text = fileName.take(15) + if (fileName.length > 15) "..." else "", 
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape).clickable { editAttachmentUri = null }.padding(4.dp)) {
                                    Icon(Icons.Filled.Close, "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    } else {
                        Surface(
                            onClick = { fileLauncher.launch(arrayOf("*/*")) },
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Attach File", 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            viewModel.updateReminderDetails(reminder, editTitle.trim(), editDueDate, editRepeatMode, editRepeatValueText.toIntOrNull(), editAttachmentUri)
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("Save", color = PrimaryEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showDatePicker) {
        CustomDateTimePickerDialog(
            initialTimeMillis = editDueDate ?: System.currentTimeMillis(),
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                editDueDate = millis
                showDatePicker = false
            }
        )
    }
}

@Composable
fun BottomNavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}
