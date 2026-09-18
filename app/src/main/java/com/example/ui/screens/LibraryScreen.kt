package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleDataProvider
import com.example.data.model.MarketingPost
import com.example.data.model.PostStatus
import com.example.data.model.UserRole
import com.example.ui.components.PlatformBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusRejected
import com.example.ui.theme.VetTeal
import com.example.ui.viewmodel.MarketingViewModel
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MarketingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentRole by viewModel.currentRole.collectAsState()
    val rawPosts by viewModel.allPosts.collectAsState()
    val allPosts = if (rawPosts.isEmpty()) SampleDataProvider.getSamplePosts() else rawPosts

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var selectedPlatformFilter by remember { mutableStateOf("ALL") }
    var selectedLanguageFilter by remember { mutableStateOf("ALL") }
    var selectedSortOption by remember { mutableStateOf("NEWEST") }

    // Dialog state for Rejection reason
    var postToReject by remember { mutableStateOf<MarketingPost?>(null) }
    var rejectReasonText by remember { mutableStateOf("") }

    // Dialog state for Scheduling
    var postToSchedule by remember { mutableStateOf<MarketingPost?>(null) }

    // Filter posts
    val filteredPosts = allPosts.filter { post ->
        val matchesSearch = searchQuery.isBlank() ||
                post.title.contains(searchQuery, ignoreCase = true) ||
                post.contentText.contains(searchQuery, ignoreCase = true) ||
                post.serviceOrProduct.contains(searchQuery, ignoreCase = true) ||
                post.audience.contains(searchQuery, ignoreCase = true)

        val matchesStatus = selectedStatusFilter == "ALL" || post.status == selectedStatusFilter
        val matchesPlatform = selectedPlatformFilter == "ALL" || post.platform.contains(selectedPlatformFilter, ignoreCase = true)
        val matchesLanguage = selectedLanguageFilter == "ALL" || post.language.contains(selectedLanguageFilter, ignoreCase = true)

        matchesSearch && matchesStatus && matchesPlatform && matchesLanguage
    }

    val sortedPosts = filteredPosts.sortedWith { a, b ->
        when (selectedSortOption) {
            "OLDEST" -> a.createdDateMillis.compareTo(b.createdDateMillis)
            "SCHEDULED" -> (a.scheduledDateMillis ?: Long.MAX_VALUE).compareTo(b.scheduledDateMillis ?: Long.MAX_VALUE)
            "TITLE" -> a.title.compareTo(b.title, ignoreCase = true)
            else -> b.createdDateMillis.compareTo(a.createdDateMillis)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Content Library",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manage, review, duplicate and publish all marketing assets",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_search_input"),
                placeholder = { Text("Search by keyword, product, audience...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Status Filter Chips
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "ALL" to "All (${allPosts.size})",
                    PostStatus.DRAFT.name to "Drafts (${allPosts.count { it.status == PostStatus.DRAFT.name }})",
                    PostStatus.PENDING_APPROVAL.name to "Pending (${allPosts.count { it.status == PostStatus.PENDING_APPROVAL.name }})",
                    PostStatus.APPROVED.name to "Approved (${allPosts.count { it.status == PostStatus.APPROVED.name }})",
                    PostStatus.SCHEDULED.name to "Scheduled (${allPosts.count { it.status == PostStatus.SCHEDULED.name }})",
                    PostStatus.PUBLISHED.name to "Published (${allPosts.count { it.status == PostStatus.PUBLISHED.name }})",
                    PostStatus.REJECTED.name to "Rejected (${allPosts.count { it.status == PostStatus.REJECTED.name }})"
                ).forEach { (statusKey, label) ->
                    FilterChip(
                        selected = selectedStatusFilter == statusKey,
                        onClick = { selectedStatusFilter = statusKey },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
        }

        // Language & Sort Controls
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Language row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Language: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VetTeal)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("ALL" to "All", "Hinglish" to "Hinglish", "Hindi" to "Hindi", "English" to "English").forEach { (langKey, label) ->
                                FilterChip(
                                    selected = selectedLanguageFilter == langKey,
                                    onClick = { selectedLanguageFilter = langKey },
                                    label = { Text(label, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    // Sort row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sort By: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VetTeal)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                "NEWEST" to "Newest",
                                "OLDEST" to "Oldest",
                                "SCHEDULED" to "Schedule Date",
                                "TITLE" to "Title A-Z"
                            ).forEach { (sortKey, label) ->
                                FilterChip(
                                    selected = selectedSortOption == sortKey,
                                    onClick = { selectedSortOption = sortKey },
                                    label = { Text(label, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Results count
        item {
            Text(
                text = "Showing ${sortedPosts.size} posts (sorted by ${selectedSortOption.lowercase()})",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }

        if (sortedPosts.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No marketing assets match the selected filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(sortedPosts) { post ->
                LibraryPostItemCard(
                    post = post,
                    currentRole = currentRole,
                    onApprove = { viewModel.approvePost(post.id) },
                    onRejectPrompt = {
                        postToReject = post
                        rejectReasonText = ""
                    },
                    onSchedulePrompt = { postToSchedule = post },
                    onPublish = { viewModel.publishPost(post.id, context) },
                    onDuplicate = {
                        viewModel.duplicatePost(post)
                        Toast.makeText(context, "Duplicated as new Draft!", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = {
                        viewModel.deletePost(post.id)
                        Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                    },
                    onCopy = {
                        copyToClipboard(context, "${post.contentText}\n\n${post.hashtags}")
                    }
                )
            }
        }
    }

    // Reject Dialog
    postToReject?.let { target ->
        AlertDialog(
            onDismissRequest = { postToReject = null },
            title = { Text("Reject Content", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Provide feedback or rejection reason to the content creator:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReasonText,
                        onValueChange = { rejectReasonText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Exaggerated medical claims, missing contact info...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectPost(target.id, rejectReasonText.ifBlank { "Rejected by Admin" })
                        postToReject = null
                        Toast.makeText(context, "Post marked as Rejected", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejected)
                ) {
                    Text("Confirm Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { postToReject = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Schedule Date Picker Dialog
    postToSchedule?.let { target ->
        val cal = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val newCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 10, 0)
                }
                viewModel.schedulePost(target.id, newCal.timeInMillis)
                postToSchedule = null
                Toast.makeText(context, "Post scheduled!", Toast.LENGTH_SHORT).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { postToSchedule = null }
            show()
        }
    }
}

@Composable
fun LibraryPostItemCard(
    post: MarketingPost,
    currentRole: UserRole,
    onApprove: () -> Unit,
    onRejectPrompt: () -> Unit,
    onSchedulePrompt: () -> Unit,
    onPublish: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
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

                Row {
                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(16.dp))
                    }
                    if (currentRole == UserRole.ADMIN) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Content
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
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            // Rejection reason banner if rejected
            if (post.status == PostStatus.REJECTED.name && !post.rejectionReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Reason: ${post.rejectionReason}",
                        color = Color(0xFF991B1B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Expand / Collapse details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide details" else "Show alternates & prompts",
                    fontSize = 11.sp,
                    color = VetTeal,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = VetTeal,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (post.shortVersion.isNotBlank()) {
                        Text(text = "Short Version:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VetTeal)
                        Text(text = post.shortVersion, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (post.hashtags.isNotBlank()) {
                        Text(text = "Hashtags:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VetTeal)
                        Text(text = post.hashtags, fontSize = 11.sp, color = VetTeal)
                    }
                    if (post.imagePrompt.isNotBlank()) {
                        Text(text = "Image Prompt:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                        Text(text = post.imagePrompt, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // If admin and post is pending or draft: show Approve / Reject
                if (currentRole == UserRole.ADMIN && (post.status == PostStatus.DRAFT.name || post.status == PostStatus.PENDING_APPROVAL.name)) {
                    OutlinedButton(
                        onClick = onRejectPrompt,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Schedule Button
                OutlinedButton(
                    onClick = onSchedulePrompt,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Schedule", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Publish / Share
                Button(
                    onClick = onPublish,
                    colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (post.status == PostStatus.PUBLISHED.name) "Share" else "Publish", fontSize = 11.sp)
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("RVH Content", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
}
