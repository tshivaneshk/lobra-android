package com.example.lobra.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.lobra.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CustomDateTimePickerDialog(
    initialTimeMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var isDatePicker by remember { mutableStateOf(true) }
    
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = initialTimeMillis } }
    
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    
    val dateText by remember {
        derivedStateOf {
            val c = Calendar.getInstance().apply { set(selectedYear, selectedMonth, selectedDay) }
            val today = Calendar.getInstance()
            if (c.get(Calendar.YEAR) == today.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                "Today"
            } else {
                SimpleDateFormat("MMM d", Locale.getDefault()).format(c.time)
            }
        }
    }
    
    val timeText by remember {
        derivedStateOf {
            val h = if (selectedHour == 0) 12 else if (selectedHour > 12) selectedHour - 12 else selectedHour
            val amPm = if (selectedHour >= 12) "pm" else "am"
            String.format(Locale.US, "%d:%02d %s", h, selectedMinute, amPm)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface // Pure dark mapped deeply
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Tab Switcher
                Row(
                    modifier = Modifier.padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(if (isDatePicker) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent, RoundedCornerShape(20.dp))
                            .clickable { isDatePicker = true }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(dateText, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(" | ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    Box(
                        modifier = Modifier
                            .background(if (!isDatePicker) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent, RoundedCornerShape(20.dp))
                            .clickable { isDatePicker = false }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(timeText, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (isDatePicker) {
                    CustomCalendar(selectedYear, selectedMonth, selectedDay, { y, m -> selectedYear = y; selectedMonth = m }, { selectedDay = it })
                } else {
                    CustomTimePicker(selectedHour, selectedMinute, { h, m -> selectedHour = h; selectedMinute = m })
                }

                Spacer(modifier = Modifier.height(32.dp))
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    }
                    TextButton(onClick = {
                        if (isDatePicker) {
                            isDatePicker = false
                        } else {
                            val result = Calendar.getInstance().apply { set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0) }
                            onConfirm(result.timeInMillis)
                        }
                    }) {
                        Text(if (isDatePicker) "Next" else "Save", color = PrimaryEmerald, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomCalendar(year: Int, month: Int, selectedDay: Int, onMonthChange: (Int, Int) -> Unit, onDaySelected: (Int) -> Unit) {
    var showMonthPicker by remember { mutableStateOf(false) }
    val cal = Calendar.getInstance().apply { set(year, month, 1) }
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (month == 0) onMonthChange(year - 1, 11) else onMonthChange(year, month - 1) }) {
                    Icon(Icons.Filled.ChevronLeft, "Previous", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showMonthPicker = !showMonthPicker }.padding(4.dp)
                ) {
                    Text(monthName, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Icon(if (showMonthPicker) androidx.compose.material.icons.Icons.Filled.ArrowDropUp else androidx.compose.material.icons.Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            IconButton(onClick = { if (month == 11) onMonthChange(year + 1, 0) else onMonthChange(year, month + 1) }) {
                Icon(Icons.Filled.ChevronRight, "Next", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showMonthPicker) {
            Row(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(
                    count = 12,
                    selectedIndex = month,
                    width = 150.dp,
                    onIndexChanged = { m -> onMonthChange(year, m) },
                    formatItem = { SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().apply { set(Calendar.MONTH, it) }.time) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                WheelPicker(
                    count = 100, // from 2000 to 2099
                    selectedIndex = year - 2000,
                    width = 80.dp,
                    onIndexChanged = { y -> onMonthChange(y + 2000, month) },
                    formatItem = { "${it + 2000}" }
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEachIndexed { index, day ->
                    val color = when (index) {
                        4 -> Color(0xFF10B981)
                        5 -> Color(0xFFE53935)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(day, color = color, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val emptyDays = if (startDayOfWeek == Calendar.SUNDAY) 6 else startDayOfWeek - 2
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            var dayCounter = 1
            for (row in 0..5) {
                if (dayCounter > daysInMonth) break
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (col in 0..6) {
                        if (row == 0 && col < emptyDays || dayCounter > daysInMonth) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val d = dayCounter
                            val isSelected = d == selectedDay
                            
                            val columnColor = when (col) {
                                4 -> Color(0xFF10B981)
                                5 -> Color(0xFFE53935)
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            }
                            val selectedBgColor = when (col) {
                                4 -> Color(0xFF10B981)
                                5 -> Color(0xFFE53935)
                                else -> PrimaryEmerald
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(if (isSelected) selectedBgColor else Color.Transparent, CircleShape)
                                    .clickable { onDaySelected(d) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    d.toString(),
                                    color = if (isSelected) MaterialTheme.colorScheme.surface else columnColor,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            dayCounter++
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CustomTimePicker(hour: Int, minute: Int, onTimeChange: (Int, Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(
                count = 12,
                selectedIndex = (hour % 12).let { if (it == 0) 11 else it - 1 },
                onIndexChanged = { h -> 
                    val newHourRaw = h + 1
                    val isPm = hour >= 12
                    onTimeChange(if (isPm) if (newHourRaw == 12) 12 else newHourRaw + 12 else if (newHourRaw == 12) 0 else newHourRaw, minute)
                },
                formatItem = { "${it + 1}" }
            )
            Text(":", color = MaterialTheme.colorScheme.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            WheelPicker(
                count = 60,
                selectedIndex = minute,
                onIndexChanged = { m -> onTimeChange(hour, m) },
                formatItem = { String.format(Locale.US, "%02d", it) }
            )
            Spacer(modifier = Modifier.width(24.dp))
            WheelPicker(
                count = 2,
                selectedIndex = if (hour >= 12) 1 else 0,
                onIndexChanged = { pmIndex ->
                    val isPm = pmIndex == 1
                    val newHour = if (isPm) { if (hour < 12) hour + 12 else hour } else { if (hour >= 12) hour - 12 else hour }
                    onTimeChange(newHour, minute)
                },
                formatItem = { if (it == 0) "am" else "pm" }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(count: Int, selectedIndex: Int, width: androidx.compose.ui.unit.Dp = 50.dp, onIndexChanged: (Int) -> Unit, formatItem: (Int) -> String) {
    val state = rememberPagerState(initialPage = selectedIndex, pageCount = { count })
    LaunchedEffect(state.currentPage) { if (state.currentPage != selectedIndex) onIndexChanged(state.currentPage) }
    LaunchedEffect(selectedIndex) { if (state.currentPage != selectedIndex) state.animateScrollToPage(selectedIndex) }

    VerticalPager(
        state = state,
        modifier = Modifier.height(130.dp).width(width),
        contentPadding = PaddingValues(vertical = 45.dp)
    ) { page ->
        val isSelected = page == state.currentPage
        Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
            Text(
                text = formatItem(page),
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                fontSize = if (isSelected) 28.sp else 20.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
