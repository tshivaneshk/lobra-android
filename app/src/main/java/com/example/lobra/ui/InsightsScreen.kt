package com.example.lobra.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lobra.theme.*
import com.example.lobra.viewmodel.LobraViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InsightsScreen(
    viewModel: LobraViewModel,
    onBack: () -> Unit
) {
    val reminders by viewModel.reminders.collectAsState()
    val completedCount = reminders.count { it.isCompleted }
    val totalCount = reminders.count { !it.isDeleted || it.isCompleted }
    
    // Calculate last 7 days completion count
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val todayStart = calendar.timeInMillis
    
    val last7DaysCount = IntArray(7) { 0 }
    val sdf = SimpleDateFormat("EEE", Locale.getDefault())
    val labels = Array(7) { "" }
    
    for (i in 6 downTo 0) {
        val startDay = todayStart - (i * 24 * 60 * 60 * 1000L)
        val endDay = startDay + (24 * 60 * 60 * 1000L)
        
        last7DaysCount[6 - i] = reminders.count { it.isCompleted && it.completedAt != null && it.completedAt >= startDay && it.completedAt < endDay }
        
        val tempCal = Calendar.getInstance()
        tempCal.timeInMillis = startDay
        labels[6 - i] = sdf.format(tempCal.time)
    }

    val maxCount = maxOf(last7DaysCount.maxOrNull() ?: 1, 1).toFloat()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
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
                    "Insights",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 32.sp
                )
            }

            Column(modifier = Modifier.padding(24.dp)) {
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Task Completion", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$completedCount", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), color = PrimaryEmerald, fontSize = 48.sp)
                            Text(" / $totalCount totals", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp, start = 8.dp))
                        }
                        
                        LinearProgressIndicator(
                            progress = { if (totalCount == 0) 0f else completedCount.toFloat() / totalCount },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = PrimaryEmerald,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Last 7 Days Activity", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                    Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        val barWidth = 30f
                        val gap = (size.width - (7 * barWidth)) / 6f
                        
                        for (i in 0 until 7) {
                            val barHeight = (last7DaysCount[i] / maxCount) * size.height
                            val xOffset = i * (barWidth + gap)
                            
                            drawRoundRect(
                                color = PrimaryEmerald,
                                topLeft = Offset(xOffset, size.height - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                        }
                    }
                    
                    // X-Axis Labels
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 0 until 7) {
                            Text(labels[i], color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
