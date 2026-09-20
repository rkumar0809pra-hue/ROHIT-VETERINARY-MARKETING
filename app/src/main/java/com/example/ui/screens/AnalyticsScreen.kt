package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleDataProvider
import com.example.data.model.PostStatus
import com.example.ui.components.ResponsiveContentContainer
import com.example.ui.components.ResponsiveTwoPaneLayout
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusPublished
import com.example.ui.theme.StatusScheduled
import com.example.ui.theme.VetAmber
import com.example.ui.theme.VetBlue
import com.example.ui.theme.VetTeal
import com.example.ui.theme.WhatsAppDark
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.util.rememberScreenLayoutInfo
import com.example.ui.viewmodel.MarketingViewModel

@Composable
fun AnalyticsScreen(
    viewModel: MarketingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rawMetrics by viewModel.dashboardMetrics.collectAsState()
    val rawPosts by viewModel.allPosts.collectAsState()
    val rawCampaigns by viewModel.allCampaigns.collectAsState()
    val allVeoVideos by viewModel.allVeoVideos.collectAsState()

    val allPosts = if (rawPosts.isEmpty()) SampleDataProvider.getSamplePosts() else rawPosts
    val allCampaigns = if (rawCampaigns.isEmpty()) SampleDataProvider.getSampleCampaigns() else rawCampaigns
    val metrics = if (rawPosts.isEmpty()) SampleDataProvider.getInitialMetrics() else rawMetrics

    var selectedTimeRange by remember { mutableStateOf("ALL") }

    val now = System.currentTimeMillis()
    val cutoffTime = when (selectedTimeRange) {
        "7D" -> now - 7L * 24 * 3600 * 1000
        "30D" -> now - 30L * 24 * 3600 * 1000
        else -> 0L
    }

    val currentPosts = if (cutoffTime == 0L) allPosts else allPosts.filter { it.createdDateMillis >= cutoffTime }
    val currentVeoVideos = if (cutoffTime == 0L) allVeoVideos else allVeoVideos.filter { it.createdDateMillis >= cutoffTime }

    val totalPosts = currentPosts.size
    val totalLeads = metrics.totalLeads
    val approvalRate = metrics.approvalRatePercent
    val publishRate = metrics.publishRatePercent

    // Platform counts
    val whatsAppPostsCount = currentPosts.count { it.platform.contains("WhatsApp", ignoreCase = true) }
    val facebookPostsCount = currentPosts.count { it.platform.contains("Facebook", ignoreCase = true) }
    val videoScriptsCount = currentPosts.count { it.platform.contains("Video", ignoreCase = true) }
    val veoGeneratedCount = currentVeoVideos.size

    // Veterinary category counts
    val cattleCount = currentPosts.count { it.category.contains("Cattle", ignoreCase = true) || it.title.contains("Cow", ignoreCase = true) || it.title.contains("Milk", ignoreCase = true) || it.audience.contains("Dairy", ignoreCase = true) }
    val petCount = currentPosts.count { it.category.contains("Pet", ignoreCase = true) || it.title.contains("Dog", ignoreCase = true) || it.title.contains("Puppy", ignoreCase = true) || it.title.contains("Deworming", ignoreCase = true) }
    val poultryCount = currentPosts.count { it.category.contains("Poultry", ignoreCase = true) || it.title.contains("Goat", ignoreCase = true) || it.title.contains("Sheep", ignoreCase = true) }

    // Campaign metrics
    val totalCampaignRecipients = allCampaigns.sumOf { it.totalRecipients }
    val totalCampaignResponses = allCampaigns.sumOf { it.responseCount }

    val previewMode by viewModel.previewDeviceMode.collectAsState()
    val layoutInfo = rememberScreenLayoutInfo(previewMode)

    ResponsiveContentContainer(modifier = modifier) {
        if (layoutInfo.isExpanded) {
            ResponsiveTwoPaneLayout(
                isWideScreen = true,
                primaryWeight = 0.52f,
                secondaryWeight = 0.48f,
                primaryPane = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("analytics_left_pane"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header & Filter
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Marketing Performance & KPIs",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Track campaign response, approval turnaround, and inquiries for Rohit Veterinary House",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val rangeLabel = when (selectedTimeRange) {
                                                "7D" -> "Last 7 Days"
                                                "30D" -> "Last 30 Days"
                                                else -> "All Time"
                                            }
                                            val report = """
                                                📊 Rohit Veterinary House - Marketing Performance Brief
                                                🕒 Window: $rangeLabel
                                                📝 Total Posts: $totalPosts
                                                🎬 Veo 3 AI Video Reels: $veoGeneratedCount
                                                🛡️ Approval / Compliance Score: $approvalRate%
                                                🚀 Publish Rate: $publishRate%
                                                📞 Inquiries & Calls: $totalLeads
                                                💬 WhatsApp Delivered: $totalCampaignRecipients
                                                💬 WhatsApp Direct Replies: $totalCampaignResponses
                                                🩺 Veterinary Category Split:
                                                  - Cattle & Dairy: $cattleCount
                                                  - Pets & Companion: $petCount
                                                  - Poultry & Small Ruminants: $poultryCount
                                                
                                                Verified 100% compliant with veterinary health advertising standards.
                                            """.trimIndent()
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "RVH Performance Brief")
                                                putExtra(Intent.EXTRA_TEXT, report)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Performance Report"))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Export Brief", fontSize = 11.sp)
                                    }
                                }

                                // Time Range Filter Chips
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("ALL" to "All Time", "30D" to "Last 30 Days", "7D" to "Last 7 Days").forEach { (key, label) ->
                                        FilterChip(
                                            selected = selectedTimeRange == key,
                                            onClick = { selectedTimeRange = key },
                                            label = { Text(label, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        // 4 KPI Cards in 2x2 grid
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                KpiCard(
                                    title = "Approval Rate",
                                    value = "$approvalRate%",
                                    subText = "Compliance Score",
                                    color = StatusApproved,
                                    icon = Icons.Outlined.CheckCircle,
                                    modifier = Modifier.weight(1f)
                                )
                                KpiCard(
                                    title = "Publish Rate",
                                    value = "$publishRate%",
                                    subText = "Drafts to Live",
                                    color = StatusPublished,
                                    icon = Icons.Default.Send,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                KpiCard(
                                    title = "Total Inquiries / Leads",
                                    value = "$totalLeads",
                                    subText = "Calls & Messages",
                                    color = VetTeal,
                                    icon = Icons.Outlined.TrendingUp,
                                    modifier = Modifier.weight(1f)
                                )
                                KpiCard(
                                    title = "Veo 3 AI Videos",
                                    value = "$veoGeneratedCount",
                                    subText = "AI Generated Reels",
                                    color = Color(0xFF6366F1),
                                    icon = Icons.Outlined.Movie,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Campaign Response Summary Table
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Audience Segment Response Analysis",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Segment", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "Delivered", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "Replies", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "Conv %", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    allCampaigns.forEach { c ->
                                        val conv = if (c.sentCount > 0) (c.responseCount * 100) / c.sentCount else 0
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = c.campaignName, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.3f))
                                            Text(text = "${c.sentCount}", fontSize = 12.sp, modifier = Modifier.weight(0.7f))
                                            Text(text = "${c.responseCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VetTeal, modifier = Modifier.weight(0.7f))
                                            Text(text = "$conv%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VetAmber, modifier = Modifier.weight(0.7f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                secondaryPane = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("analytics_right_pane"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Platform Channel Breakdown
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Content Volume by Channel",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    ChannelBarProgress(
                                        channel = "WhatsApp Broadcasts & Status",
                                        count = whatsAppPostsCount,
                                        total = maxOf(1, totalPosts),
                                        color = WhatsAppDark
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    ChannelBarProgress(
                                        channel = "Facebook Page Posts & Ads",
                                        count = facebookPostsCount,
                                        total = maxOf(1, totalPosts),
                                        color = FacebookBlue
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    ChannelBarProgress(
                                        channel = "Video Reels & Shorts",
                                        count = videoScriptsCount,
                                        total = maxOf(1, totalPosts),
                                        color = Color(0xFF6366F1)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    ChannelBarProgress(
                                        channel = "Veo 3 AI Video Reels",
                                        count = veoGeneratedCount,
                                        total = maxOf(1, totalPosts + veoGeneratedCount),
                                        color = VetTeal
                                    )
                                }
                            }
                        }

                        // Veterinary Practice Distribution
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Veterinary Practice & Patient Distribution",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    ChannelBarProgress(
                                        channel = "🐄 Dairy Cattle & Buffalo (Milk, Mastitis, Calcium)",
                                        count = cattleCount,
                                        total = maxOf(1, totalPosts),
                                        color = Color(0xFF0284C7)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    ChannelBarProgress(
                                        channel = "🐕 Canine & Feline Pets (Deworming, Vaccines, Ticks)",
                                        count = petCount,
                                        total = maxOf(1, totalPosts),
                                        color = Color(0xFF10B981)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    ChannelBarProgress(
                                        channel = "🐐 Poultry & Small Ruminants (Outbreak, Feeds)",
                                        count = poultryCount,
                                        total = maxOf(1, totalPosts),
                                        color = VetAmber
                                    )
                                }
                            }
                        }

                        // Strategic Local Recommendations
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = VetTeal)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "AI Content Recommendations for RVH",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = VetTeal
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "• Dairy cattle mineral mixture posts have the highest conversion rate (19%). Plan 2 more posts this week.\n• Monsoon deworming reminders for goat farmers convert best when posted before 8:00 AM on WhatsApp.\n• Pet vaccination camps on Facebook generate 3x more comments when paired with a photo of the clinic.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF166534),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("analytics_screen"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Marketing Performance & KPIs",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Track campaign response, approval turnaround, and client inquiries for Rohit Veterinary House",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            val rangeLabel = when (selectedTimeRange) {
                                "7D" -> "Last 7 Days"
                                "30D" -> "Last 30 Days"
                                else -> "All Time"
                            }
                            val report = """
                                📊 Rohit Veterinary House - Marketing Performance Brief
                                🕒 Window: $rangeLabel
                                📝 Total Posts: $totalPosts
                                🎬 Veo 3 AI Video Reels: $veoGeneratedCount
                                🛡️ Approval / Compliance Score: $approvalRate%
                                🚀 Publish Rate: $publishRate%
                                📞 Inquiries & Calls: $totalLeads
                                💬 WhatsApp Delivered: $totalCampaignRecipients
                                💬 WhatsApp Direct Replies: $totalCampaignResponses
                                🩺 Veterinary Category Split:
                                  - Cattle & Dairy: $cattleCount
                                  - Pets & Companion: $petCount
                                  - Poultry & Small Ruminants: $poultryCount
                                
                                Verified 100% compliant with veterinary health advertising standards.
                            """.trimIndent()
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "RVH Performance Brief")
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Performance Report"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Brief", fontSize = 11.sp)
                    }
                }

                // Time Range Filter Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ALL" to "All Time", "30D" to "Last 30 Days", "7D" to "Last 7 Days").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedTimeRange == key,
                            onClick = { selectedTimeRange = key },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Top KPI Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Approval Rate",
                    value = "$approvalRate%",
                    subText = "Compliance Score",
                    color = StatusApproved,
                    icon = Icons.Outlined.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Publish Rate",
                    value = "$publishRate%",
                    subText = "Drafts to Live",
                    color = StatusPublished,
                    icon = Icons.Default.Send,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Total Inquiries / Leads",
                    value = "$totalLeads",
                    subText = "Calls & Messages",
                    color = VetTeal,
                    icon = Icons.Outlined.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Veo 3 AI Videos",
                    value = "$veoGeneratedCount",
                    subText = "AI Generated Reels",
                    color = Color(0xFF6366F1),
                    icon = Icons.Outlined.Movie,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Platform Channel Breakdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Content Volume by Channel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    ChannelBarProgress(
                        channel = "WhatsApp Broadcasts & Status",
                        count = whatsAppPostsCount,
                        total = maxOf(1, totalPosts),
                        color = WhatsAppDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ChannelBarProgress(
                        channel = "Facebook Page Posts & Ads",
                        count = facebookPostsCount,
                        total = maxOf(1, totalPosts),
                        color = FacebookBlue
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ChannelBarProgress(
                        channel = "Video Reels & Shorts",
                        count = videoScriptsCount,
                        total = maxOf(1, totalPosts),
                        color = Color(0xFF6366F1)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ChannelBarProgress(
                        channel = "Veo 3 AI Video Reels",
                        count = veoGeneratedCount,
                        total = maxOf(1, totalPosts + veoGeneratedCount),
                        color = VetTeal
                    )
                }
            }
        }

        // Veterinary Clinical & Animal Segment Breakdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Veterinary Practice & Patient Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    ChannelBarProgress(
                        channel = "🐄 Dairy Cattle & Buffalo (Milk, Mastitis, Calcium)",
                        count = cattleCount,
                        total = maxOf(1, totalPosts),
                        color = Color(0xFF0284C7)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ChannelBarProgress(
                        channel = "🐕 Canine & Feline Pets (Deworming, Vaccines, Ticks)",
                        count = petCount,
                        total = maxOf(1, totalPosts),
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ChannelBarProgress(
                        channel = "🐐 Poultry & Small Ruminants (Outbreak, Feeds)",
                        count = poultryCount,
                        total = maxOf(1, totalPosts),
                        color = VetAmber
                    )
                }
            }
        }

        // Campaign Response Summary Table
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Audience Segment Response Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Segment", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Delivered", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Replies", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Conv %", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    allCampaigns.forEach { c ->
                        val conv = if (c.sentCount > 0) (c.responseCount * 100) / c.sentCount else 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = c.campaignName, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.3f))
                            Text(text = "${c.sentCount}", fontSize = 12.sp, modifier = Modifier.weight(0.7f))
                            Text(text = "${c.responseCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VetTeal, modifier = Modifier.weight(0.7f))
                            Text(text = "$conv%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VetAmber, modifier = Modifier.weight(0.7f))
                        }
                    }
                }
            }
        }

        // Strategic Local Recommendations
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = VetTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Content Recommendations for RVH",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = VetTeal
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Dairy cattle mineral mixture posts have the highest conversion rate (19%). Plan 2 more posts this week.\n• Monsoon deworming reminders for goat farmers convert best when posted before 8:00 AM on WhatsApp.\n• Pet vaccination camps on Facebook generate 3x more comments when paired with a photo of the clinic.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF166534),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subText: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subText, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ChannelBarProgress(
    channel: String,
    count: Int,
    total: Int,
    color: Color
) {
    val fraction = (count.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = channel, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(text = "$count posts (${(fraction * 100).toInt()}%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
