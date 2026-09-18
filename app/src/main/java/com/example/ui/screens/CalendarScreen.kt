package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleDataProvider
import com.example.data.model.MarketingPost
import com.example.data.model.Platform
import com.example.data.model.PostStatus
import com.example.ui.components.PlatformBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusScheduled
import com.example.ui.theme.StatusScheduledContainer
import com.example.ui.theme.VetTeal
import com.example.ui.viewmodel.MarketingViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    viewModel: MarketingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rawPosts by viewModel.allPosts.collectAsState()
    val allPosts = if (rawPosts.isEmpty()) SampleDataProvider.getSamplePosts() else rawPosts

    var viewMode by remember { mutableStateOf("WEEK") } // "WEEK" or "MONTH"
    var selectedPlatformFilter by remember { mutableStateOf("ALL") }
    var selectedLanguageFilter by remember { mutableStateOf("ALL") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    // Current calendar position
    var currentCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDayEpochDay by remember { mutableStateOf(getEpochDay(System.currentTimeMillis())) }

    // Reschedule dialog state
    var postToReschedule by remember { mutableStateOf<MarketingPost?>(null) }

    val filteredPosts = allPosts.filter { post ->
        val platformMatch = selectedPlatformFilter == "ALL" || post.platform.contains(selectedPlatformFilter, ignoreCase = true)
        val langMatch = selectedLanguageFilter == "ALL" || post.language.contains(selectedLanguageFilter, ignoreCase = true)
        val statusMatch = selectedStatusFilter == "ALL" || post.status.equals(selectedStatusFilter, ignoreCase = true)
        platformMatch && langMatch && statusMatch
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Content Calendar",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Plan, schedule and balance your marketing output",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Week / Month Toggle
                Row {
                    FilterChip(
                        selected = viewMode == "WEEK",
                        onClick = { viewMode = "WEEK" },
                        label = { Text("Week") }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = viewMode == "MONTH",
                        onClick = { viewMode = "MONTH" },
                        label = { Text("Month") }
                    )
                }
            }
        }

        // Filters Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Channels & Platforms", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VetTeal)
                        Text(
                            text = "${filteredPosts.size} posts found",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ALL" to "All Channels", "WhatsApp" to "WhatsApp", "Facebook" to "Facebook", "Video" to "Video Reels").forEach { (key, label) ->
                            FilterChip(
                                selected = selectedPlatformFilter == key,
                                onClick = { selectedPlatformFilter = key },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text(text = "Status Filter", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VetTeal)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ALL" to "All Status", "SCHEDULED" to "Scheduled", "PUBLISHED" to "Published", "DRAFT" to "Draft", "APPROVED" to "Approved").forEach { (key, label) ->
                            FilterChip(
                                selected = selectedStatusFilter == key,
                                onClick = { selectedStatusFilter = key },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Calendar Grid Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with Month Name, Today button, and navigation arrows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                val newCal = currentCalendar.clone() as Calendar
                                newCal.add(Calendar.MONTH, -1)
                                currentCalendar = newCal
                            }) {
                                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                            }

                            Text(
                                text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentCalendar.time),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(onClick = {
                                val newCal = currentCalendar.clone() as Calendar
                                newCal.add(Calendar.MONTH, 1)
                                currentCalendar = newCal
                            }) {
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month")
                            }
                        }

                        // Jump to Today button
                        OutlinedButton(
                            onClick = {
                                currentCalendar = Calendar.getInstance()
                                selectedDayEpochDay = getEpochDay(System.currentTimeMillis())
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Today", fontSize = 11.sp, color = VetTeal, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Day-of-week headers
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                            Text(
                                text = day,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Days Grid (Month or Week)
                    val days = getCalendarDays(currentCalendar, viewMode == "WEEK")
                    val chunked = days.chunked(7)

                    chunked.forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            week.forEach { dayInfo ->
                                val isSelected = dayInfo.epochDay == selectedDayEpochDay
                                val hasPosts = filteredPosts.any {
                                    it.scheduledDateMillis != null && getEpochDay(it.scheduledDateMillis) == dayInfo.epochDay
                                }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> VetTeal
                                                dayInfo.isToday -> VetTeal.copy(alpha = 0.15f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { selectedDayEpochDay = dayInfo.epochDay },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayInfo.dayOfMonth.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected || dayInfo.isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> Color.White
                                                dayInfo.isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            }
                                        )
                                        if (hasPosts) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color.White else StatusScheduled)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Posts for the selected date or upcoming
        val selectedDatePosts = filteredPosts.filter {
            it.scheduledDateMillis != null && getEpochDay(it.scheduledDateMillis) == selectedDayEpochDay
        }

        item {
            val dateLabel = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
                .format(Date(selectedDayEpochDay * 86400000L))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scheduled for $dateLabel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${selectedDatePosts.size} posts",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (selectedDatePosts.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No content scheduled for this date.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(selectedDatePosts) { post ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PlatformBadge(platform = post.platform)
                                Spacer(modifier = Modifier.width(6.dp))
                                StatusBadge(status = post.status)
                            }

                            OutlinedButton(
                                onClick = { postToReschedule = post },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(imageVector = Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reschedule", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = post.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = post.contentText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // Reschedule Picker Dialog Trigger
    postToReschedule?.let { targetPost ->
        val cal = Calendar.getInstance()
        targetPost.scheduledDateMillis?.let { cal.timeInMillis = it }
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val newCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 10, 0)
                }
                viewModel.schedulePost(targetPost.id, newCal.timeInMillis)
                postToReschedule = null
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { postToReschedule = null }
            show()
        }
    }
}

data class DayInfo(
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val epochDay: Long
)

private fun getEpochDay(millis: Long): Long = millis / 86400000L

private fun getCalendarDays(baseCal: Calendar, isWeekOnly: Boolean): List<DayInfo> {
    val cal = baseCal.clone() as Calendar
    val todayEpoch = getEpochDay(System.currentTimeMillis())

    if (isWeekOnly) {
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val result = mutableListOf<DayInfo>()
        for (i in 0 until 7) {
            val epoch = getEpochDay(cal.timeInMillis)
            result.add(
                DayInfo(
                    dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                    isCurrentMonth = true,
                    isToday = epoch == todayEpoch,
                    epochDay = epoch
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return result
    } else {
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val currentMonth = cal.get(Calendar.MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        cal.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek)

        val result = mutableListOf<DayInfo>()
        for (i in 0 until 35) {
            val epoch = getEpochDay(cal.timeInMillis)
            result.add(
                DayInfo(
                    dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                    isCurrentMonth = cal.get(Calendar.MONTH) == currentMonth,
                    isToday = epoch == todayEpoch,
                    epochDay = epoch
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return result
    }
}
