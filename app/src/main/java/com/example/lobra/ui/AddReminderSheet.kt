package com.example.lobra.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.HorizontalDivider
import com.example.lobra.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderSheet(
    onDismiss: () -> Unit,
    onAdd: (String, Long?, String?, Int?, String?) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateTimeMillis by remember { mutableStateOf<Long?>(null) }
    var repeatMode by remember { mutableStateOf("Don't Repeat") }
    var showRepeatMenu by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var attachmentUri by remember { mutableStateOf<String?>(null) }
    
    val fileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
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
                    attachmentUri = it.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Row with Set Due Date and Save button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { showDatePicker = true },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, PrimaryEmerald)
                ) {
                    val dateText = selectedDateTimeMillis?.let { 
                        java.text.SimpleDateFormat("MMM dd, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(it))
                    } ?: "Set Due Date & Time"
                    Text(
                        text = dateText, 
                        color = PrimaryEmerald,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                Surface(
                    onClick = {
                        if (text.isNotBlank()) {
                            onAdd(text.trim(), selectedDateTimeMillis, repeatMode, null, attachmentUri)
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = PrimaryEmerald
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                placeholder = { 
                    Text("What do you want to\nremember?", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f), fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp) 
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Surface(
                        onClick = { showRepeatMenu = true },
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = repeatMode, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
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
                                    repeatMode = option
                                    showRepeatMenu = false 
                                }
                            )
                        }
                    }
                }

                if (attachmentUri != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val uriObj = android.net.Uri.parse(attachmentUri)
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
                            Box(modifier = Modifier.clip(CircleShape).clickable { attachmentUri = null }.padding(4.dp)) {
                                Icon(Icons.Filled.Close, "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                } else {
                    Surface(
                        onClick = { fileLauncher.launch(arrayOf("*/*")) },
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
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
        }
    }

    if (showDatePicker) {
        CustomDateTimePickerDialog(
            initialTimeMillis = selectedDateTimeMillis ?: System.currentTimeMillis(),
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                selectedDateTimeMillis = millis
                showDatePicker = false
            }
        )
    }
}
