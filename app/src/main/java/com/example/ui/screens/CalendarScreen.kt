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
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Today
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
import com.example.ui.components.PlatformBadge
import com.example.ui.components.ResponsiveContentContainer
import com.example.ui.components.ResponsiveTwoPaneLayout
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusScheduled
import com.example.ui.theme.StatusScheduledContainer
import com.example.ui.theme.VetTeal
import com.example.ui.util.rememberScreenLayoutInfo
import com.example.ui.viewmodel.MarketingViewModel
import com.example.util.BusinessCalendarHelper
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    viewModel: MarketingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rawPosts by viewModel.allPosts.collectAsState()
    val allPosts = if (rawPosts.isEmpty()) SampleDataProvider.getSamplePosts() else rawPosts
    val previewMode by viewModel.previewDeviceMode.collectAsState()
    val layoutInfo = rememberScreenLayoutInfo(previewMode)

    var viewMode by remember { mutableStateOf("WEEK") } // "WEEK" or "MONTH"
    var selectedPlatformFilter by remember { mutableStateOf("ALL") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    // Current calendar position and single source of truth for selected business date
    var selectedDate by remember { mutableStateOf(BusinessCalendarHelper.getTodayBusinessDate()) }
    var currentCalendar by remember {
        mutableStateOf(BusinessCalendarHelper.createBusinessCalendar(BusinessCalendarHelper.getTodayBusinessDate()))
    }

    // Reschedule dialog state
    var postToReschedule by remember { mutableStateOf<MarketingPost?>(null) }

    val filteredPosts = allPosts.filter { post ->
        val platformMatch = selectedPlatformFilter == "ALL" || post.platform.contains(selectedPlatformFilter, ignoreCase = true)
        val statusMatch = selectedStatusFilter == "ALL" || post.status.equals(selectedStatusFilter, ignoreCase = true)
        platformMatch && statusMatch
    }

    val selectedDatePosts = filteredPosts.filter { post ->
        post.scheduledDateMillis != null &&
            BusinessCalendarHelper.instantToBusinessDate(post.scheduledDateMillis) == selectedDate
    }

    // Composable for the Month/Week Grid Card
    val calendarGridComposable: @Composable () -> Unit = {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with Month Name, Today button, and navigation arrows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        IconButton(onClick = {
                            val newCal = (currentCalendar.clone() as Calendar).apply {
                                timeZone = BusinessCalendarHelper.BUSINESS_TIME_ZONE
                                add(Calendar.MONTH, -1)
                            }
                            currentCalendar = newCal
                        }) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }

                        Text(
                            text = BusinessCalendarHelper.formatMonthHeader(currentCalendar),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        IconButton(onClick = {
                            val newCal = (currentCalendar.clone() as Calendar).apply {
                                timeZone = BusinessCalendarHelper.BUSINESS_TIME_ZONE
                                add(Calendar.MONTH, 1)
                            }
                            currentCalendar = newCal
                        }) {
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month")
                        }
                    }

                    // Jump to Today button
                    OutlinedButton(
                        onClick = {
                            val today = BusinessCalendarHelper.getTodayBusinessDate()
                            selectedDate = today
                            currentCalendar = BusinessCalendarHelper.createBusinessCalendar(today)
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Today", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Day of Week Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { dayName ->
                        Text(
                            text = dayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val dayList = BusinessCalendarHelper.getCalendarDays(currentCalendar, isWeekOnly = viewMode == "WEEK")

                val rowCount = (dayList.size + 6) / 7
                for (rowIndex in 0 until rowCount) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (colIndex in 0 until 7) {
                            val dayIdx = rowIndex * 7 + colIndex
                            if (dayIdx >= dayList.size) {
                                Spacer(modifier = Modifier.size(38.dp))
                            } else {
                                val dayInfo = dayList[dayIdx]
                                val isSelected = dayInfo.businessDate == selectedDate
                                val hasPosts = filteredPosts.any { post ->
                                    post.scheduledDateMillis != null &&
                                        BusinessCalendarHelper.instantToBusinessDate(post.scheduledDateMillis) == dayInfo.businessDate
                                }

                                val circleBackground = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    dayInfo.isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    hasPosts -> StatusScheduledContainer.copy(alpha = 0.5f)
                                    else -> Color.Transparent
                                }

                                val borderModifier = when {
                                    dayInfo.isToday && !isSelected -> Modifier.border(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                                    else -> Modifier
                                }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .then(borderModifier)
                                        .background(circleBackground)
                                        .clickable { selectedDate = dayInfo.businessDate },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayInfo.businessDate.dayOfMonth.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected || dayInfo.isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                dayInfo.isToday -> MaterialTheme.colorScheme.primary
                                                dayInfo.isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            }
                                        )
                                        if (hasPosts) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else StatusScheduled)
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
    }

    // Composable for the Schedule Detail on the Selected Day
    val scheduleDetailsComposable: @Composable () -> Unit = {
        val dateLabel = BusinessCalendarHelper.formatScheduleHeading(selectedDate)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scheduled for $dateLabel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${selectedDatePosts.size} posts",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (selectedDatePosts.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No content scheduled for this date.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                selectedDatePosts.forEach { post ->
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
    }

    ResponsiveContentContainer(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("calendar_screen"),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = viewMode == "WEEK",
                            onClick = { viewMode = "WEEK" },
                            label = { Text("Week", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = viewMode == "MONTH",
                            onClick = { viewMode = "MONTH" },
                            label = { Text("Month", fontSize = 12.sp) }
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
                            Text(
                                text = "Channels & Platforms",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
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
                                    label = { Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                                )
                            }
                        }

                        Text(
                            text = "Status Filter",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("ALL" to "All Status", "SCHEDULED" to "Scheduled", "PUBLISHED" to "Published", "DRAFT" to "Draft", "APPROVED" to "Approved").forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedStatusFilter == key,
                                    onClick = { selectedStatusFilter = key },
                                    label = { Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                    }
                }
            }

            // If Desktop Expanded: show side-by-side Calendar Grid on Left and Schedule on Right
            if (layoutInfo.isExpanded) {
                item {
                    ResponsiveTwoPaneLayout(
                        isWideScreen = true,
                        primaryWeight = 0.52f,
                        secondaryWeight = 0.48f,
                        primaryPane = {
                            calendarGridComposable()
                        },
                        secondaryPane = {
                            scheduleDetailsComposable()
                        }
                    )
                }
            } else {
                // Mobile stacked layout
                item {
                    calendarGridComposable()
                }

                item {
                    scheduleDetailsComposable()
                }
            }
        }
    }

    // Reschedule Picker Dialog Trigger in the business timezone
    postToReschedule?.let { targetPost ->
        val cal = BusinessCalendarHelper.createBusinessCalendar(selectedDate)
        targetPost.scheduledDateMillis?.let {
            val bDate = BusinessCalendarHelper.instantToBusinessDate(it)
            cal.set(bDate.year, bDate.month, bDate.dayOfMonth)
        }
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val chosenDate = BusinessCalendarHelper.BusinessDate(year, month, dayOfMonth)
                val newMillis = BusinessCalendarHelper.businessDateToInstant(chosenDate, 10, 0)
                viewModel.schedulePost(targetPost.id, newMillis)
                selectedDate = chosenDate
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
