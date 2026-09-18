package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.ChatMessage
import com.example.data.model.PostStatus
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusApprovedContainer
import com.example.ui.theme.StatusDraft
import com.example.ui.theme.StatusDraftContainer
import com.example.ui.theme.StatusScheduled
import com.example.ui.theme.StatusScheduledContainer
import com.example.ui.theme.VetAmber
import com.example.ui.theme.VetBlue
import com.example.ui.theme.VetTeal
import com.example.ui.theme.VetTealContainer
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.GuidedContentResult
import com.example.ui.viewmodel.GuidedInputs
import com.example.ui.viewmodel.GuidedStep
import com.example.ui.viewmodel.MarketingViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatAssistantScreen(
    viewModel: MarketingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isSending by viewModel.isSendingChatMessage.collectAsState()
    val guidedStep by viewModel.guidedStep.collectAsState()
    val guidedInputs by viewModel.guidedInputs.collectAsState()
    val guidedResult by viewModel.guidedResult.collectAsState()
    val isGeneratingGuided by viewModel.isGeneratingGuided.collectAsState()
    val isGuidedMode by viewModel.isGuidedMode.collectAsState()
    val selectedChatModel by viewModel.selectedChatModel.collectAsState()
    val selectedChatRole by viewModel.selectedChatRole.collectAsState()
    val isSearchGrounding by viewModel.isSearchGroundingEnabled.collectAsState()

    var customServiceText by remember { mutableStateOf("") }
    var freeChatInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll when new messages arrive
    LaunchedEffect(chatMessages.size, isGeneratingGuided) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .testTag("chat_assistant_screen")
    ) {
        // --- Top Bar: Assistant Identity & Mode Control ---
        Card(
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(VetTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RVH Guided Marketing Assistant",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (viewModel.isGeminiOnline()) "Online AI • Gemini 3.5 Flash" else "Smart Veterinary Engine",
                                fontSize = 11.sp,
                                color = if (viewModel.isGeminiOnline()) Color(0xFF16A34A) else VetAmber
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Restart / New Guided Campaign
                        IconButton(
                            onClick = {
                                viewModel.resetGuidedFlow()
                                Toast.makeText(context, "Started new guided content flow!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("reset_guided_flow_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "New Campaign",
                                tint = VetTeal
                            )
                        }

                        // Clear Chat
                        IconButton(onClick = { viewModel.clearChat() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Chat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Mode Selector Tabs (Guided Content Creator vs Open Assistant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { viewModel.setGuidedMode(true) },
                        color = if (isGuidedMode) VetTeal else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isGuidedMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Guided Content Flow",
                                fontSize = 12.sp,
                                fontWeight = if (isGuidedMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (isGuidedMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = { viewModel.setGuidedMode(false) },
                        color = if (!isGuidedMode) VetTeal else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Free-form AI Chat",
                                fontSize = 12.sp,
                                fontWeight = if (!isGuidedMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isGuidedMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // --- Model & Role Selector Panel (Active during Free-form Mode) ---
        if (!isGuidedMode) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Models row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Model:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf(
                            "gemini-3.1-flash-lite" to "⚡ 3.1 Flash-Lite (Fast)",
                            "gemini-3.5-flash" to "✨ 3.5 Flash (General)",
                            "gemini-3.1-pro-preview" to "🧠 3.1 Pro (Deep)"
                        ).forEach { (modelKey, label) ->
                            FilterChip(
                                selected = selectedChatModel == modelKey,
                                onClick = { viewModel.setChatModel(modelKey) },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.testTag("model_chip_$modelKey")
                            )
                        }
                    }

                    // Persona / Role row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Role:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf(
                            "Compliance & Strategy" to "🛡️ Compliance Officer",
                            "Viral Social & WhatsApp" to "🚀 Social & WA Growth",
                            "Rapid Copy & Slogans" to "⚡ Fast Slogans",
                            "Live Intel & Outbreaks" to "🌐 Outbreak Intel"
                        ).forEach { (roleKey, label) ->
                            FilterChip(
                                selected = selectedChatRole == roleKey,
                                onClick = { viewModel.setChatRole(roleKey) },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.testTag("role_chip_$roleKey")
                            )
                        }
                    }

                    // Search Grounding toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isSearchGrounding,
                            onClick = { viewModel.toggleSearchGrounding(!isSearchGrounding) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (isSearchGrounding) "🌐 Google Search Grounding: ON (Live Web Sources)" else "🌐 Search Grounding: OFF",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSearchGrounding) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSearchGrounding) VetTeal else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.testTag("search_grounding_toggle_chip")
                        )
                    }
                }
            }
        }

        // --- Step Progress Tracker Banner (Active during Guided Mode) ---
        if (isGuidedMode) {
            GuidedProgressBar(guidedStep = guidedStep, guidedInputs = guidedInputs)
        }

        // --- Main Chat Stream ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(chatMessages) { msg ->
                ChatBubbleItem(
                    message = msg,
                    onSaveToDraft = {
                        viewModel.duplicatePost(
                            com.example.data.model.MarketingPost(
                                title = "AI Content - Rohit Veterinary House",
                                platform = "WhatsApp",
                                category = "Veterinary Healthcare",
                                audience = "General animal owners",
                                serviceOrProduct = "Veterinary Healthcare",
                                tone = "professional",
                                language = "Hindi",
                                ctaText = "Call now",
                                status = PostStatus.DRAFT.name,
                                contentText = msg.text
                            )
                        )
                        Toast.makeText(context, "Saved to Drafts!", Toast.LENGTH_SHORT).show()
                    },
                    onCopy = { copyToClipboard(context, msg.text) }
                )
            }

            // Real-time Loading Spinner when generating
            if (isSending || isGeneratingGuided) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = VetTeal
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isGeneratingGuided) "Generating complete marketing package..." else "AI is typing...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VetTeal
                                )
                                Text(
                                    text = "Tailoring safe, high-converting veterinary copy for Rohit Veterinary House",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Full Generated Result Cards in Guided Mode
            if (isGuidedMode && guidedResult != null && guidedStep == GuidedStep.RESULT) {
                item {
                    GuidedResultCardsSection(
                        result = guidedResult!!,
                        onSaveToDraft = {
                            viewModel.saveGuidedResultToDraft()
                            Toast.makeText(context, "Saved as Draft in Library!", Toast.LENGTH_SHORT).show()
                        },
                        onApprove = {
                            viewModel.approveGuidedResult()
                            Toast.makeText(context, "Content Approved!", Toast.LENGTH_SHORT).show()
                        },
                        onSchedule = {
                            showDateTimePicker(context) { scheduledMillis ->
                                viewModel.scheduleGuidedResult(scheduledMillis)
                                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(scheduledMillis))
                                Toast.makeText(context, "Post Scheduled for $dateStr!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCopy = { text, label ->
                            copyToClipboard(context, text)
                        },
                        onShare = { text ->
                            shareText(context, text)
                        },
                        onRegenerate = {
                            viewModel.regenerateGuidedResult()
                            Toast.makeText(context, "Regenerating marketing content...", Toast.LENGTH_SHORT).show()
                        },
                        onUpdateResult = { updated ->
                            viewModel.updateGuidedResult(updated)
                            Toast.makeText(context, "Content edits applied!", Toast.LENGTH_SHORT).show()
                        },
                        onStartNew = {
                            viewModel.resetGuidedFlow()
                        }
                    )
                }
            }
        }

        // --- Interactive Guided Options Drawer (Step-by-step choices) ---
        if (isGuidedMode && guidedStep != GuidedStep.RESULT && !isGeneratingGuided) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    GuidedOptionsPicker(
                        step = guidedStep,
                        customServiceText = customServiceText,
                        onCustomServiceChange = { customServiceText = it },
                        onSelectPlatform = { viewModel.selectGuidedPlatform(it) },
                        onSelectAudience = { viewModel.selectGuidedAudience(it) },
                        onSelectGoal = { viewModel.selectGuidedGoal(it) },
                        onSubmitService = {
                            viewModel.submitGuidedService(it)
                            customServiceText = ""
                        },
                        onSelectLanguage = { viewModel.selectGuidedLanguage(it) },
                        onSelectTone = { viewModel.selectGuidedTone(it) },
                        onSelectCta = { viewModel.selectGuidedCta(it) },
                        onSelectLength = { viewModel.selectGuidedLength(it) }
                    )
                }
            }
        } else if (!isGuidedMode) {
            // Free-form Chat Input Bar with Quick Veterinary Suggestions
            Column(modifier = Modifier.fillMaxWidth()) {
                // Quick suggested prompts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Summer cow milk drop tips",
                        "Puppy vaccination camp broadcast",
                        "Latest FMD outbreak news in India",
                        "Catchy slogan for cattle calcium feed"
                    ).forEach { promptText ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable {
                                freeChatInput = promptText
                            }
                        ) {
                            Text(
                                text = "💡 $promptText",
                                fontSize = 11.sp,
                                color = VetTeal,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = freeChatInput,
                            onValueChange = { freeChatInput = it },
                            placeholder = { Text("Ask AI assistant in Hindi, Hinglish, or English...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("free_chat_input"),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true
                        )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val text = freeChatInput.trim()
                            if (text.isNotBlank()) {
                                viewModel.sendChatMessage(text)
                                freeChatInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(VetTeal)
                            .testTag("free_chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
}

/**
 * Visual Progress Bar displaying the 8 Guided Steps and current status
 */
@Composable
fun GuidedProgressBar(
    guidedStep: GuidedStep,
    guidedInputs: GuidedInputs
) {
    val stepSequence = listOf(
        "Platform",
        "Audience",
        "Goal",
        "Service/Product",
        "Language",
        "Tone",
        "CTA",
        "Length",
        "Generate"
    )

    val currentStepIndex = if (guidedStep == GuidedStep.RESULT) 8 else (guidedStep.stepNumber - 1)
    val progress = (currentStepIndex + 1).toFloat() / 9f

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Horizontal step progress sequence: Platform > Audience > Goal > Service/Product > Language > Tone > CTA > Length > Generate
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                stepSequence.forEachIndexed { index, stepName ->
                    val isCurrent = index == currentStepIndex
                    val isCompleted = index < currentStepIndex

                    Surface(
                        color = when {
                            isCurrent -> VetTeal
                            isCompleted -> VetTealContainer
                            else -> MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = VetTeal,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = stepName,
                                fontSize = 11.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isCurrent -> Color.White
                                    isCompleted -> VetTeal
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                }
                            )
                        }
                    }

                    if (index < stepSequence.lastIndex) {
                        Text(
                            text = " > ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = VetTeal,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Summary breadcrumbs of collected inputs
            val summaryTokens = buildList {
                if (guidedInputs.platform.isNotBlank()) add(guidedInputs.platform)
                if (guidedInputs.audience.isNotBlank()) add(guidedInputs.audience)
                if (guidedInputs.goal.isNotBlank()) add(guidedInputs.goal)
                if (guidedInputs.serviceOrProduct.isNotBlank()) add(guidedInputs.serviceOrProduct)
                if (guidedInputs.language.isNotBlank()) add(guidedInputs.language)
                if (guidedInputs.tone.isNotBlank()) add(guidedInputs.tone)
                if (guidedInputs.cta.isNotBlank()) add(guidedInputs.cta)
                if (guidedInputs.length.isNotBlank()) add(guidedInputs.length)
            }

            if (summaryTokens.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    summaryTokens.forEach { token ->
                        Surface(
                            color = VetTealContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = token,
                                fontSize = 10.sp,
                                color = VetTeal,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step Option Picker: Render choices for each step
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuidedOptionsPicker(
    step: GuidedStep,
    customServiceText: String,
    onCustomServiceChange: (String) -> Unit,
    onSelectPlatform: (String) -> Unit,
    onSelectAudience: (String) -> Unit,
    onSelectGoal: (String) -> Unit,
    onSubmitService: (String) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onSelectTone: (String) -> Unit,
    onSelectCta: (String) -> Unit,
    onSelectLength: (String) -> Unit
) {
    Column {
        val question = when (step) {
            GuidedStep.PLATFORM -> "📱 Which platform are you creating content for?"
            GuidedStep.AUDIENCE -> "🎯 Who is your primary target audience?"
            GuidedStep.GOAL -> "🎯 What is the main goal of this campaign?"
            GuidedStep.SERVICE -> "💊 Enter or select the veterinary service / product:"
            GuidedStep.LANGUAGE -> "🌐 Which language should the content be in?"
            GuidedStep.TONE -> "🎭 What tone should the message convey?"
            GuidedStep.CTA -> "📣 Choose your Call to Action (CTA):"
            GuidedStep.LENGTH -> "📏 Choose your preferred content length:"
            GuidedStep.RESULT -> ""
        }

        Text(
            text = question,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        when (step) {
            GuidedStep.PLATFORM -> {
                val platforms = listOf("WhatsApp", "Facebook", "Video Script")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    platforms.forEach { platform ->
                        val color = when (platform) {
                            "WhatsApp" -> WhatsAppGreen
                            "Facebook" -> FacebookBlue
                            else -> VetAmber
                        }
                        Button(
                            onClick = { onSelectPlatform(platform) },
                            colors = ButtonDefaults.buttonColors(containerColor = color),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_platform_${platform.lowercase().replace(" ", "_")}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(platform, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            GuidedStep.AUDIENCE -> {
                val audiences = listOf(
                    "Pet owners",
                    "Dairy farmers",
                    "Goat farmers",
                    "Poultry farmers",
                    "General animal owners"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    audiences.forEach { audience ->
                        OutlinedButton(
                            onClick = { onSelectAudience(audience) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("select_audience_${audience.lowercase().replace(" ", "_")}")
                        ) {
                            Text(audience, fontSize = 12.sp)
                        }
                    }
                }
            }

            GuidedStep.GOAL -> {
                val goals = listOf(
                    "Awareness",
                    "Promotion",
                    "Reminder",
                    "Offer",
                    "Emergency message"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    goals.forEach { goal ->
                        OutlinedButton(
                            onClick = { onSelectGoal(goal) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("select_goal_${goal.lowercase().replace(" ", "_")}")
                        ) {
                            Text(goal, fontSize = 12.sp)
                        }
                    }
                }
            }

            GuidedStep.SERVICE -> {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customServiceText,
                            onValueChange = onCustomServiceChange,
                            placeholder = { Text("e.g. Chelated Mineral Mixture, Deworming Camp") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_service_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (customServiceText.isNotBlank()) {
                                    onSubmitService(customServiceText)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("submit_service_button")
                        ) {
                            Text("Next", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Or tap a popular veterinary service:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Cattle Mineral Mixture",
                            "Rabies Vaccination",
                            "Deworming Camp",
                            "Milk Booster Calcium",
                            "Poultry Electrolytes",
                            "Tick & Flea Spray"
                        ).forEach { suggestion ->
                            Surface(
                                onClick = { onSubmitService(suggestion) },
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = suggestion,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            GuidedStep.LANGUAGE -> {
                val languages = listOf("Hindi", "English", "Hinglish")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    languages.forEach { lang ->
                        Button(
                            onClick = { onSelectLanguage(lang) },
                            colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_language_${lang.lowercase()}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(lang, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            GuidedStep.TONE -> {
                val tones = listOf("professional", "friendly", "urgent", "educational")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tones.forEach { tone ->
                        OutlinedButton(
                            onClick = { onSelectTone(tone) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("select_tone_$tone")
                        ) {
                            Text(tone.replaceFirstChar { it.uppercase() }, fontSize = 12.sp)
                        }
                    }
                }
            }

            GuidedStep.CTA -> {
                val ctas = listOf(
                    "Call now",
                    "WhatsApp now",
                    "Visit shop",
                    "Book consultation",
                    "Order now"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ctas.forEach { cta ->
                        Button(
                            onClick = { onSelectCta(cta) },
                            colors = ButtonDefaults.buttonColors(containerColor = VetBlue),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("select_cta_${cta.lowercase().replace(" ", "_")}")
                        ) {
                            Text(cta, fontSize = 12.sp)
                        }
                    }
                }
            }

            GuidedStep.LENGTH -> {
                val lengths = listOf("short", "medium", "long")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lengths.forEach { length ->
                        Button(
                            onClick = { onSelectLength(length) },
                            colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_length_$length"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = when (length) {
                                    "short" -> "Short (SMS/Status)"
                                    "medium" -> "Medium (Standard)"
                                    else -> "Long (Educational)"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            GuidedStep.RESULT -> {
                // Handled in Result Card
            }
        }
    }
}

/**
 * Result Section generated by the Guided Marketing Assistant
 * Displays separate, easily scannable cards:
 * - Actions & Status Bar (Save as Draft, Approve, Schedule, Copy, Share, Regenerate, Edit)
 * - Main Content
 * - Alternate Versions
 * - Hashtags
 * - Short CTA
 * - Image Prompt
 * - Video Prompt
 * - Edit Option before saving
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuidedResultCardsSection(
    result: GuidedContentResult,
    onSaveToDraft: () -> Unit,
    onApprove: () -> Unit,
    onSchedule: () -> Unit,
    onCopy: (String, String) -> Unit,
    onShare: (String) -> Unit,
    onRegenerate: () -> Unit,
    onUpdateResult: (GuidedContentResult) -> Unit,
    onStartNew: () -> Unit
) {
    var selectedAlternateTab by remember { mutableIntStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }

    var editMainContent by remember(result.mainContent) { mutableStateOf(result.mainContent) }
    var editShortCta by remember(result.shortCta) { mutableStateOf(result.shortCta) }
    var editHashtags by remember(result.hashtags) { mutableStateOf(result.hashtags) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("guided_result_cards_section")
    ) {
        // --- 1. Actions & Status Bar Card ---
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = when (result.platform) {
                                "WhatsApp" -> WhatsAppGreen
                                "Facebook" -> FacebookBlue
                                else -> VetAmber
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = result.platform,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = result.audience,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Current Action Status Badge
                    result.currentStatus?.let { status ->
                        val (badgeBg, badgeText, label) = when (status) {
                            PostStatus.DRAFT -> Triple(StatusDraftContainer, StatusDraft, "Draft Saved")
                            PostStatus.APPROVED -> Triple(StatusApprovedContainer, StatusApproved, "Approved")
                            PostStatus.SCHEDULED -> Triple(StatusScheduledContainer, StatusScheduled, "Scheduled")
                            else -> Triple(StatusApprovedContainer, StatusApproved, status.name)
                        }
                        Surface(
                            color = badgeBg,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = badgeText, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeText)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons Grid (Row 1: Save Draft, Approve, Schedule)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveToDraft,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_to_draft_button")
                    ) {
                        Icon(imageVector = Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Draft", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("approve_button")
                    ) {
                        Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onSchedule,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusScheduled),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("schedule_button")
                    ) {
                        Icon(imageVector = Icons.Outlined.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Schedule", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons Grid (Row 2: Copy, Share, Regenerate, Edit)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val fullText = "${result.mainContent}\n\n${result.shortCta}\n\n${result.hashtags}"
                            onCopy(fullText, "Full Marketing Package")
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_all_button")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareBody = "${result.mainContent}\n\n${result.shortCta}\n\n${result.hashtags}"
                            onShare(shareBody)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onRegenerate,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("regenerate_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Regenerate", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            editMainContent = result.mainContent
                            editShortCta = result.shortCta
                            editHashtags = result.hashtags
                            showEditDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_content_button")
                    ) {
                        Icon(imageVector = Icons.Outlined.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // --- 2. Separate Card: Main Content ---
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📝 Main Content (${result.length.replaceFirstChar { it.uppercase() }})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = VetTeal
                    )
                    Row {
                        IconButton(onClick = { onCopy(result.mainContent, "Main Content") }, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = VetTeal, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onShare(result.mainContent) }, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = VetTeal, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = result.mainContent,
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // --- 3. Separate Card: Alternate Versions ---
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🔄 Alternate Versions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                TabRow(
                    selectedTabIndex = selectedAlternateTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = VetTeal,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedAlternateTab == 0,
                        onClick = { selectedAlternateTab = 0 },
                        text = { Text("Benefit-Led", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedAlternateTab == 1,
                        onClick = { selectedAlternateTab = 1 },
                        text = { Text("Urgency", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedAlternateTab == 2,
                        onClick = { selectedAlternateTab = 2 },
                        text = { Text("Local Connect", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                val currentAlternate = when (selectedAlternateTab) {
                    0 -> result.alternate1
                    1 -> result.alternate2
                    else -> result.alternate3
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = currentAlternate,
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onCopy(currentAlternate, "Alternate Version") }) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Version", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Separate Card: Hashtags ---
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Tag, contentDescription = null, tint = VetTeal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Hashtags", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { onCopy(result.hashtags, "Hashtags") }, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = VetTeal, modifier = Modifier.size(15.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = result.hashtags,
                        fontSize = 12.sp,
                        color = VetTeal,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // --- 5. Separate Card: Short CTA ---
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📣 Short Call to Action (CTA)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = VetBlue
                    )
                    IconButton(onClick = { onCopy(result.shortCta, "Short CTA") }, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = VetBlue, modifier = Modifier.size(15.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = VetTealContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = result.shortCta,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VetTeal,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // --- 6. Separate Card: Image Prompt ---
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Image, contentDescription = null, tint = VetAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Image Prompt (AI Studio / Imagen)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { onCopy(result.imagePrompt, "Image Prompt") }, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = VetAmber, modifier = Modifier.size(15.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = result.imagePrompt,
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // --- 7. Separate Card: Video Prompt ---
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Videocam, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Video Prompt (Veo / Reels)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { onCopy(result.videoPrompt, "Video Prompt") }, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF6366F1), modifier = Modifier.size(15.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = result.videoPrompt,
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Start New Campaign Button
        OutlinedButton(
            onClick = onStartNew,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_another_post_button")
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create Another Guided Post", fontSize = 12.sp)
        }
    }

    // --- Edit Content Modal Dialog ---
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Edit Content Before Saving",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "Main Content:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = editMainContent,
                        onValueChange = { editMainContent = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Text(text = "Short CTA:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = editShortCta,
                        onValueChange = { editShortCta = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Text(text = "Hashtags:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = editHashtags,
                        onValueChange = { editHashtags = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateResult(
                            result.copy(
                                mainContent = editMainContent,
                                shortCta = editShortCta,
                                hashtags = editHashtags
                            )
                        )
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VetTeal)
                ) {
                    Text("Apply Edits")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Standard Chat Bubble representation with Multi-Model tag and Search Grounding citations
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    onSaveToDraft: () -> Unit,
    onCopy: () -> Unit
) {
    val context = LocalContext.current
    val isUser = message.sender == "USER"

    // Parse search queries
    val searchQueriesList: List<String> = remember(message.searchQueries) {
        if (message.searchQueries.isNullOrBlank()) emptyList()
        else try {
            val arr = org.json.JSONArray(message.searchQueries)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Parse search sources
    val sourcesList: List<Pair<String, String>> = remember(message.searchSourcesJson) {
        if (message.searchSourcesJson.isNullOrBlank()) emptyList()
        else try {
            val arr = org.json.JSONArray(message.searchSourcesJson)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                obj.optString("title", "Source ${i + 1}") to obj.optString("uri", "")
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(VetTeal),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            color = if (isUser) VetTeal else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            shadowElevation = if (isUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Model indicator badge if present
                if (!isUser && !message.modelUsed.isNullOrBlank()) {
                    val modelLabel = when (message.modelUsed) {
                        "gemini-3.1-flash-lite" -> "⚡ Gemini 3.1 Flash-Lite"
                        "gemini-3.5-flash" -> "✨ Gemini 3.5 Flash"
                        "gemini-3.1-pro-preview" -> "🧠 Gemini 3.1 Pro"
                        else -> message.modelUsed
                    }
                    Surface(
                        color = VetTealContainer,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = modelLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VetTeal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = message.text,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )

                // Search Grounding Queries used
                if (searchQueriesList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🌐 Search Queries:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = VetTeal
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        searchQueriesList.forEach { q ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "🔍 $q",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Search Grounding Citations / Sources
                if (sourcesList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "📚 Grounding Sources & Citations:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = VetTeal
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        sourcesList.forEach { (title, uri) ->
                            Surface(
                                color = VetTealContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (uri.isNotBlank()) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Cannot open: $uri", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔗 $title",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = VetTeal,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isUser && message.text.length > 50) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onCopy, modifier = Modifier.size(26.dp)) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = VetTeal)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = onSaveToDraft, modifier = Modifier.size(26.dp)) {
                            Icon(imageVector = Icons.Outlined.Save, contentDescription = "Save Draft", modifier = Modifier.size(14.dp), tint = VetTeal)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Android Date & Time picker launcher
 */
private fun showDateTimePicker(context: Context, onDateTimeSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    selectedCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    selectedCal.set(Calendar.MINUTE, minute)
                    selectedCal.set(Calendar.SECOND, 0)
                    onDateTimeSelected(selectedCal.timeInMillis)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("RVH Marketing Assistant", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String) {
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Marketing Content"))
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to launch share chooser", Toast.LENGTH_SHORT).show()
    }
}
