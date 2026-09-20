package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarketingPost
import com.example.data.model.Platform
import com.example.data.model.PostStatus
import com.example.data.model.UserRole
import com.example.ui.components.PlatformBadge
import com.example.ui.components.RolePill
import com.example.ui.components.StatusBadge
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusDraft
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPublished
import com.example.ui.theme.StatusScheduled
import com.example.ui.theme.StatusScheduledContainer
import com.example.ui.theme.VetAmber
import com.example.ui.theme.VetBlue
import com.example.ui.theme.VetTeal
import com.example.ui.theme.VetTealContainer
import com.example.ui.theme.WhatsAppDark
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.MarketingViewModel
import com.example.ui.viewmodel.NavTab
import com.example.data.SampleDataProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MarketingViewModel,
    modifier: Modifier = Modifier
) {
    val rawMetrics by viewModel.dashboardMetrics.collectAsState()
    val rawPosts by viewModel.allPosts.collectAsState()
    val rawCampaigns by viewModel.allCampaigns.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()

    // Fallback gracefully to sample data so preview and fresh launch render rich UI immediately
    val allPosts = if (rawPosts.isEmpty()) SampleDataProvider.getSamplePosts() else rawPosts
    val allCampaigns = if (rawCampaigns.isEmpty()) SampleDataProvider.getSampleCampaigns() else rawCampaigns
    val metrics = if (rawPosts.isEmpty()) SampleDataProvider.getInitialMetrics() else rawMetrics

    var selectedChannelFilter by remember { mutableStateOf("ALL") }

    val now = System.currentTimeMillis()
    val sevenDaysAhead = now + (7 * 86400000L)

    // Filter upcoming posts for Next 7 Days
    val upcomingPosts = allPosts.filter {
        val matchesTime = it.scheduledDateMillis != null && it.scheduledDateMillis in now..sevenDaysAhead
        val matchesChannel = selectedChannelFilter == "ALL" || it.platform.contains(selectedChannelFilter, ignoreCase = true)
        matchesTime && matchesChannel
    }.sortedBy { it.scheduledDateMillis }

    // Filter recent activity
    val recentPosts = allPosts.filter {
        selectedChannelFilter == "ALL" || it.platform.contains(selectedChannelFilter, ignoreCase = true)
    }.sortedByDescending { it.createdDateMillis }.take(6)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Business Header Hero
        item {
            ClinicHeroBanner(
                currentRole = currentRole,
                onSwitchRole = {
                    val nextRole = when (currentRole) {
                        UserRole.ADMIN -> UserRole.MARKETING_STAFF
                        UserRole.MARKETING_STAFF -> UserRole.CONTENT_CREATOR
                        UserRole.CONTENT_CREATOR -> UserRole.ADMIN
                    }
                    viewModel.switchRole(nextRole)
                },
                onOpenAssistant = { viewModel.navigateTo(NavTab.ASSISTANT) }
            )
        }

        // Quick Channel Filters
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                listOf("ALL" to "All Channels", "WhatsApp" to "WhatsApp", "Facebook" to "Facebook", "Video" to "Video").forEach { (key, label) ->
                    val isSelected = selectedChannelFilter == key
                    Surface(
                        onClick = { selectedChannelFilter = key },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Metrics Grid - Order strictly: Drafts, Pending Approval, Approved, Scheduled, Published, Leads
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Marketing Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Role: ${currentRole.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Row 1: Drafts, Pending Approval
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Drafts",
                        count = metrics.draftsCount.toString(),
                        accentColor = StatusDraft,
                        icon = Icons.Outlined.Edit,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(NavTab.LIBRARY) }
                    )
                    MetricCard(
                        title = "Pending Approval",
                        count = metrics.pendingApprovalCount.toString(),
                        accentColor = StatusPending,
                        icon = Icons.Default.Check,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(NavTab.LIBRARY) }
                    )
                }
                // Row 2: Approved, Scheduled
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Approved",
                        count = metrics.approvedCount.toString(),
                        accentColor = StatusApproved,
                        icon = Icons.Outlined.CheckCircle,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(NavTab.LIBRARY) }
                    )
                    MetricCard(
                        title = "Scheduled",
                        count = metrics.scheduledCount.toString(),
                        accentColor = StatusScheduled,
                        icon = Icons.Outlined.Event,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(NavTab.CALENDAR) }
                    )
                }
                // Row 3: Published, Leads
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Published",
                        count = metrics.publishedCount.toString(),
                        accentColor = StatusPublished,
                        icon = Icons.Outlined.Send,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(NavTab.LIBRARY) }
                    )
                    MetricCard(
                        title = "Leads Generated",
                        count = metrics.totalLeads.toString(),
                        accentColor = VetTeal,
                        icon = Icons.Outlined.TrendingUp,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(NavTab.ANALYTICS) }
                    )
                }
            }
        }

        // Quick Actions Carousel
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    QuickActionButton(
                        label = "Create Post",
                        subLabel = "AI Copywriter",
                        icon = Icons.Default.Add,
                        bgColor = VetTeal,
                        onClick = {
                            viewModel.creatorPlatform.value = Platform.WHATSAPP.name
                            viewModel.navigateTo(NavTab.CREATE)
                        }
                    )
                }
                item {
                    QuickActionButton(
                        label = "Video Script",
                        subLabel = "Reels & Shorts",
                        icon = Icons.Outlined.VideoLibrary,
                        bgColor = Color(0xFF6366F1),
                        onClick = { viewModel.navigateTo(NavTab.VIDEO_MAKER) }
                    )
                }
                item {
                    QuickActionButton(
                        label = "WhatsApp Campaign",
                        subLabel = "Broadcasts & Status",
                        icon = Icons.Default.Share,
                        bgColor = WhatsAppDark,
                        onClick = { viewModel.navigateTo(NavTab.CAMPAIGNS) }
                    )
                }
                item {
                    QuickActionButton(
                        label = "Facebook Post",
                        subLabel = "Structured Builder",
                        icon = Icons.Outlined.Forum,
                        bgColor = FacebookBlue,
                        onClick = {
                            viewModel.creatorPlatform.value = Platform.FACEBOOK.name
                            viewModel.navigateTo(NavTab.CREATE)
                        }
                    )
                }
                item {
                    QuickActionButton(
                        label = "View Calendar",
                        subLabel = "Plan & Schedule",
                        icon = Icons.Default.DateRange,
                        bgColor = Color(0xFF8B5CF6),
                        onClick = { viewModel.navigateTo(NavTab.CALENDAR) }
                    )
                }
            }
        }

        // High-Priority "Next 7 Days Content" Section
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(VetAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = VetAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Next 7 Days Content",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${upcomingPosts.size} posts ready to publish",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(onClick = { viewModel.navigateTo(NavTab.CALENDAR) }) {
                            Text("Full Calendar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (upcomingPosts.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No content scheduled for this channel in next 7 days.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.navigateTo(NavTab.CALENDAR) },
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Schedule Now", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            upcomingPosts.take(4).forEach { post ->
                                ScheduledPostItemCard(
                                    post = post,
                                    onClick = { viewModel.navigateTo(NavTab.LIBRARY) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Activity List Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "View All Library",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { viewModel.navigateTo(NavTab.LIBRARY) }
                        .padding(4.dp)
                )
            }
        }

        items(recentPosts) { post ->
            RecentActivityItemCard(
                post = post,
                onClick = { viewModel.navigateTo(NavTab.LIBRARY) }
            )
        }
    }
}

@Composable
fun ClinicHeroBanner(
    currentRole: UserRole,
    onSwitchRole: () -> Unit,
    onOpenAssistant: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .testTag("clinic_hero_banner")
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF004D40), Color(0xFF0D5C54), Color(0xFF00796B))
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Rohit Veterinary House",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Marketing & Content Operations",
                            color = Color(0xFFA7F3D0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    RolePill(
                        role = currentRole,
                        onClick = onSwitchRole
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI-Powered WhatsApp & Facebook Growth",
                        color = Color(0xFFE6FFFA),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = onOpenAssistant,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF34D399),
                            contentColor = Color(0xFF064E3B)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("hero_open_assistant_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Assistant",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    count: String,
    accentColor: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.testTag("metric_card_${title.lowercase().replace(" ", "_")}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    subLabel: String,
    icon: ImageVector,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp,
        modifier = modifier
            .width(150.dp)
            .testTag("quick_action_${label.lowercase().replace(" ", "_")}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ScheduledPostItemCard(
    post: MarketingPost,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = post.scheduledDateMillis?.let {
        SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault()).format(Date(it))
    } ?: "Scheduled"

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StatusScheduledContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = StatusScheduled,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformBadge(platform = post.platform)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = StatusScheduled,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = post.contentText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DraftQueueItemCard(
    post: MarketingPost,
    currentRole: UserRole,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformBadge(platform = post.platform)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = post.status)
                }

                if (currentRole == UserRole.ADMIN && post.status == PostStatus.DRAFT.name) {
                    Row {
                        IconButton(
                            onClick = onApprove,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Approve",
                                tint = StatusApproved,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onReject,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Reject",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = post.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = post.contentText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecentActivityItemCard(
    post: MarketingPost,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val createdStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(post.createdDateMillis))

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
                    Text(
                        text = createdStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (post.status == PostStatus.REJECTED.name && !post.rejectionReason.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Feedback: ${post.rejectionReason}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreenPreview() {
    val samplePosts = SampleDataProvider.getSamplePosts()
    val sampleMetrics = SampleDataProvider.getInitialMetrics()
    var selectedChannelFilter by remember { mutableStateOf("ALL") }

    val now = System.currentTimeMillis()
    val sevenDaysAhead = now + (7 * 86400000L)

    val upcomingPosts = samplePosts.filter {
        val matchesTime = it.scheduledDateMillis != null && it.scheduledDateMillis in now..sevenDaysAhead
        val matchesChannel = selectedChannelFilter == "ALL" || it.platform.contains(selectedChannelFilter, ignoreCase = true)
        matchesTime && matchesChannel
    }.sortedBy { it.scheduledDateMillis }

    val recentPosts = samplePosts.filter {
        selectedChannelFilter == "ALL" || it.platform.contains(selectedChannelFilter, ignoreCase = true)
    }.sortedByDescending { it.createdDateMillis }.take(6)

    MyApplicationTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_screen_preview"),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ClinicHeroBanner(
                    currentRole = UserRole.ADMIN,
                    onSwitchRole = {},
                    onOpenAssistant = {}
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    listOf("ALL" to "All Channels", "WhatsApp" to "WhatsApp", "Facebook" to "Facebook", "Video" to "Video").forEach { (key, label) ->
                        val isSelected = selectedChannelFilter == key
                        Surface(
                            onClick = { selectedChannelFilter = key },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.height(34.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Marketing Performance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Role: Clinic Admin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "Drafts",
                            count = sampleMetrics.draftsCount.toString(),
                            accentColor = StatusDraft,
                            icon = Icons.Outlined.Edit,
                            modifier = Modifier.weight(1f),
                            onClick = {}
                        )
                        MetricCard(
                            title = "Pending Approval",
                            count = sampleMetrics.pendingApprovalCount.toString(),
                            accentColor = StatusPending,
                            icon = Icons.Default.Check,
                            modifier = Modifier.weight(1f),
                            onClick = {}
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "Approved",
                            count = sampleMetrics.approvedCount.toString(),
                            accentColor = StatusApproved,
                            icon = Icons.Outlined.CheckCircle,
                            modifier = Modifier.weight(1f),
                            onClick = {}
                        )
                        MetricCard(
                            title = "Scheduled",
                            count = sampleMetrics.scheduledCount.toString(),
                            accentColor = StatusScheduled,
                            icon = Icons.Outlined.Event,
                            modifier = Modifier.weight(1f),
                            onClick = {}
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "Published",
                            count = sampleMetrics.publishedCount.toString(),
                            accentColor = StatusPublished,
                            icon = Icons.Outlined.Send,
                            modifier = Modifier.weight(1f),
                            onClick = {}
                        )
                        MetricCard(
                            title = "Leads Generated",
                            count = sampleMetrics.totalLeads.toString(),
                            accentColor = VetTeal,
                            icon = Icons.Outlined.TrendingUp,
                            modifier = Modifier.weight(1f),
                            onClick = {}
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        QuickActionButton(
                            label = "Create Post",
                            subLabel = "AI Copywriter",
                            icon = Icons.Default.Add,
                            bgColor = VetTeal,
                            onClick = {}
                        )
                    }
                    item {
                        QuickActionButton(
                            label = "Video Script",
                            subLabel = "Reels & Shorts",
                            icon = Icons.Outlined.VideoLibrary,
                            bgColor = Color(0xFF6366F1),
                            onClick = {}
                        )
                    }
                    item {
                        QuickActionButton(
                            label = "WhatsApp Campaign",
                            subLabel = "Broadcasts & Status",
                            icon = Icons.Default.Share,
                            bgColor = WhatsAppDark,
                            onClick = {}
                        )
                    }
                    item {
                        QuickActionButton(
                            label = "Facebook Post",
                            subLabel = "Structured Builder",
                            icon = Icons.Outlined.Forum,
                            bgColor = FacebookBlue,
                            onClick = {}
                        )
                    }
                    item {
                        QuickActionButton(
                            label = "View Calendar",
                            subLabel = "Plan & Schedule",
                            icon = Icons.Default.DateRange,
                            bgColor = Color(0xFF8B5CF6),
                            onClick = {}
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(VetAmber.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = VetAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Next 7 Days Content",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${upcomingPosts.size} posts ready to publish",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            TextButton(onClick = {}) {
                                Text("Full Calendar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            upcomingPosts.take(4).forEach { post ->
                                ScheduledPostItemCard(
                                    post = post,
                                    onClick = {}
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "View All Library",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            items(recentPosts) { post ->
                RecentActivityItemCard(
                    post = post,
                    onClick = {}
                )
            }
        }
    }
}

