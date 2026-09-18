package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.ui.draw.clip
import com.example.data.model.MarketingPost
import com.example.ui.components.StatusBadge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudienceType
import com.example.data.model.ContentCategory
import com.example.data.model.ContentLanguage
import com.example.data.model.ContentTone
import com.example.data.model.Platform
import com.example.data.model.PostStatus
import com.example.data.model.UserRole
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.VetAmber
import com.example.ui.theme.VetTeal
import com.example.ui.theme.WhatsAppDark
import com.example.ui.viewmodel.MarketingViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContentCreatorScreen(
    viewModel: MarketingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentRole by viewModel.currentRole.collectAsState()
    val isGenerating by viewModel.isGeneratingContent.collectAsState()
    val generatedBundle by viewModel.generatedContentBundle.collectAsState()
    val statusMessage by viewModel.contentStatusMessage.collectAsState()

    val platform by viewModel.creatorPlatform.collectAsState()
    val goal by viewModel.creatorGoal.collectAsState()
    val audience by viewModel.creatorAudience.collectAsState()
    val service by viewModel.creatorService.collectAsState()
    val tone by viewModel.creatorTone.collectAsState()
    val language by viewModel.creatorLanguage.collectAsState()
    val cta by viewModel.creatorCta.collectAsState()

    var selectedResultTab by remember { mutableIntStateOf(0) }
    val resultTabs = listOf("Primary Post", "Short / SMS", "Long Version", "3 Alternates", "Hashtags & Prompts")

    var creatorModeTab by remember { mutableIntStateOf(0) } // 0: AI Generator, 1: Facebook Builder
    val fbHeadline by viewModel.fbHeadline.collectAsState()
    val fbCaption by viewModel.fbCaption.collectAsState()
    val fbCta by viewModel.fbCta.collectAsState()
    val fbHashtags by viewModel.fbHashtags.collectAsState()
    val fbImagePrompt by viewModel.fbImagePrompt.collectAsState()
    val fbAudience by viewModel.fbAudience.collectAsState()
    val fbLanguage by viewModel.fbLanguage.collectAsState()
    val allPosts by viewModel.allPosts.collectAsState()
    var fbStatusFilter by remember { mutableStateOf("ALL") }

    val filteredFacebookPosts = allPosts.filter { post ->
        val isFacebook = post.platform.contains("Facebook", ignoreCase = true)
        val statusMatch = when (fbStatusFilter) {
            "ALL" -> true
            "DRAFT" -> post.status == PostStatus.DRAFT.name
            "SCHEDULED" -> post.status == PostStatus.SCHEDULED.name
            "PUBLISHED" -> post.status == PostStatus.PUBLISHED.name
            else -> true
        }
        isFacebook && statusMatch
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("content_creator_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (creatorModeTab == 0) "AI Content Creator" else "Facebook Post Builder",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (creatorModeTab == 0)
                    "Generate professional, veterinary-compliant copy for WhatsApp, Facebook, and Ads in Hindi, Hinglish, or English."
                else
                    "Structured Facebook campaign builder with realistic news feed preview and WhatsApp CTAs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Mode Switcher Tabs
        item {
            TabRow(
                selectedTabIndex = creatorModeTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = creatorModeTab == 0,
                    onClick = { creatorModeTab = 0 },
                    text = { Text("AI Copy Generator", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
                Tab(
                    selected = creatorModeTab == 1,
                    onClick = { creatorModeTab = 1 },
                    text = { Text("Facebook Post Builder", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
            }
        }

        if (creatorModeTab == 0) {
            // Form Card
            item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Platform Selector
                    Text(
                        text = "1. Target Platform",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Platform.values().forEach { p ->
                            FilterChip(
                                selected = platform == p.name,
                                onClick = { viewModel.creatorPlatform.value = p.name },
                                label = { Text(p.label, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 2. Campaign Goal
                    Text(
                        text = "2. Campaign Goal",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ContentCategory.values().forEach { c ->
                            FilterChip(
                                selected = goal == c.name,
                                onClick = { viewModel.creatorGoal.value = c.name },
                                label = { Text(c.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    // 3. Target Audience
                    Text(
                        text = "3. Target Audience",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AudienceType.values().forEach { a ->
                            FilterChip(
                                selected = audience == a.name,
                                onClick = { viewModel.creatorAudience.value = a.name },
                                label = { Text(a.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    // 4. Service or Product
                    Text(
                        text = "4. Service / Product / Offer",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = service,
                        onValueChange = { viewModel.creatorService.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_product_input"),
                        placeholder = { Text("e.g. Chelated Mineral Mixture, Puppy Vaccination Camp, Deworming") },
                        singleLine = true
                    )

                    // Quick suggestions for service
                    Text(
                        text = "Quick Suggest:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "Anti-Rabies Shot",
                            "Chelated Mineral Mix",
                            "Puppy Deworming",
                            "Mastitis Teat Dip",
                            "Goat ET Vaccine",
                            "Heat Stress Electrolytes"
                        ).forEach { suggestion ->
                            Surface(
                                onClick = { viewModel.creatorService.value = suggestion },
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = suggestion,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 5. Tone & Language Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "5. Tone",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            var toneExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = toneExpanded,
                                onExpandedChange = { toneExpanded = !toneExpanded }
                            ) {
                                OutlinedTextField(
                                    value = ContentTone.valueOf(tone).label,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toneExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = toneExpanded,
                                    onDismissRequest = { toneExpanded = false }
                                ) {
                                    ContentTone.values().forEach { t ->
                                        DropdownMenuItem(
                                            text = { Text(t.label) },
                                            onClick = {
                                                viewModel.creatorTone.value = t.name
                                                toneExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "6. Language",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            var langExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = langExpanded,
                                onExpandedChange = { langExpanded = !langExpanded }
                            ) {
                                OutlinedTextField(
                                    value = ContentLanguage.valueOf(language).label,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = langExpanded,
                                    onDismissRequest = { langExpanded = false }
                                ) {
                                    ContentLanguage.values().forEach { l ->
                                        DropdownMenuItem(
                                            text = { Text(l.label) },
                                            onClick = {
                                                viewModel.creatorLanguage.value = l.name
                                                langExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 7. Call To Action (CTA)
                    Text(
                        text = "7. Call To Action (CTA)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Call Now", "WhatsApp Now", "Book Consultation", "Order Product").forEach { ctaItem ->
                            FilterChip(
                                selected = cta == ctaItem,
                                onClick = { viewModel.creatorCta.value = ctaItem },
                                label = { Text(ctaItem, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Generate Button
                    Button(
                        onClick = { viewModel.generateMarketingContent() },
                        enabled = !isGenerating && service.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_content_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating AI Content...")
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Marketing Content", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Status notification
        if (statusMessage != null) {
            item {
                Surface(
                    color = VetTeal.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage ?: "",
                        color = VetTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Generated Results Output
        if (generatedBundle != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generated_results_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Generated Copy & Formats",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row {
                                IconButton(
                                    onClick = {
                                        val textToCopy = when (selectedResultTab) {
                                            0 -> generatedBundle?.primaryText ?: ""
                                            1 -> generatedBundle?.shortVersion ?: ""
                                            2 -> generatedBundle?.longVersion ?: ""
                                            3 -> "${generatedBundle?.alternate1}\n\n${generatedBundle?.alternate2}\n\n${generatedBundle?.alternate3}"
                                            else -> "${generatedBundle?.hashtags}\n\nImage Prompt: ${generatedBundle?.imagePrompt}"
                                        }
                                        copyToClipboard(context, textToCopy)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Content",
                                        tint = VetTeal
                                    )
                                }
                            }
                        }

                        ScrollableTabRow(
                            selectedTabIndex = selectedResultTab,
                            edgePadding = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            resultTabs.forEachIndexed { index, tabName ->
                                Tab(
                                    selected = selectedResultTab == index,
                                    onClick = { selectedResultTab = index },
                                    text = { Text(tabName, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tab content
                        when (selectedResultTab) {
                            0 -> ContentTextPreview(text = generatedBundle?.primaryText ?: "")
                            1 -> ContentTextPreview(text = generatedBundle?.shortVersion ?: "")
                            2 -> ContentTextPreview(text = generatedBundle?.longVersion ?: "")
                            3 -> AlternatesPreview(
                                alt1 = generatedBundle?.alternate1 ?: "",
                                alt2 = generatedBundle?.alternate2 ?: "",
                                alt3 = generatedBundle?.alternate3 ?: "",
                                onCopy = { copyToClipboard(context, it) }
                            )
                            4 -> HashtagsAndPromptsPreview(
                                hashtags = generatedBundle?.hashtags ?: "",
                                imagePrompt = generatedBundle?.imagePrompt ?: "",
                                videoPrompt = generatedBundle?.videoPrompt ?: "",
                                onCopy = { copyToClipboard(context, it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons based on User Role
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.saveCurrentGeneratedPost(PostStatus.DRAFT) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_draft_button")
                            ) {
                                Text("Save Draft", fontSize = 12.sp)
                            }

                            if (currentRole == UserRole.CONTENT_CREATOR) {
                                Button(
                                    onClick = { viewModel.saveCurrentGeneratedPost(PostStatus.PENDING_APPROVAL) },
                                    colors = ButtonDefaults.buttonColors(containerColor = VetAmber),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("submit_approval_button")
                                ) {
                                    Text("Submit Approval", fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.saveCurrentGeneratedPost(PostStatus.APPROVED) },
                                    colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("approve_post_button")
                                ) {
                                    Text("Approve Post", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Facebook Structured Post Builder Mode
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Structured Post Builder",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FacebookBlue
                            )
                            Surface(
                                color = FacebookBlue.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Facebook & Ads",
                                    color = FacebookBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // 1. Headline
                        Column {
                            Text(
                                text = "Headline",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = fbHeadline,
                                onValueChange = { viewModel.fbHeadline.value = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. Free Deworming & Vaccination Camp") },
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    "Pet Vaccination Drive",
                                    "High Milk Yield Mineral Mix",
                                    "Livestock Heat Stroke Alert",
                                    "Emergency Veterinary Care"
                                ).forEach { preset ->
                                    Surface(
                                        onClick = { viewModel.fbHeadline.value = preset },
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = preset,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Caption Text
                        Column {
                            Text(
                                text = "Primary Post Caption",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = fbCaption,
                                onValueChange = { viewModel.fbCaption.value = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                placeholder = { Text("Write compelling and compliant post copy for Rohit Veterinary House...") }
                            )
                        }

                        // 3. CTA Button
                        Column {
                            Text(
                                text = "Call-to-Action Button",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Send WhatsApp Message",
                                    "Call Now",
                                    "Book Consultation",
                                    "Learn More",
                                    "Send Message"
                                ).forEach { ctaOption ->
                                    FilterChip(
                                        selected = fbCta == ctaOption,
                                        onClick = { viewModel.fbCta.value = ctaOption },
                                        label = { Text(ctaOption, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        // 4. Target Audience & Language
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Audience",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                var audienceExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = audienceExpanded,
                                    onExpandedChange = { audienceExpanded = !audienceExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = fbAudience,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = audienceExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = audienceExpanded,
                                        onDismissRequest = { audienceExpanded = false }
                                    ) {
                                        listOf("Pet owners", "Dairy farmers", "Goat farmers", "Poultry farmers", "General animal owners").forEach { aud ->
                                            DropdownMenuItem(
                                                text = { Text(aud) },
                                                onClick = {
                                                    viewModel.fbAudience.value = aud
                                                    audienceExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Language",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                var langExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = langExpanded,
                                    onExpandedChange = { langExpanded = !langExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = fbLanguage,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = langExpanded,
                                        onDismissRequest = { langExpanded = false }
                                    ) {
                                        listOf("Hinglish", "Hindi", "English").forEach { lang ->
                                            DropdownMenuItem(
                                                text = { Text(lang) },
                                                onClick = {
                                                    viewModel.fbLanguage.value = lang
                                                    langExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Hashtags
                        Column {
                            Text(
                                text = "Hashtags",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = fbHashtags,
                                onValueChange = { viewModel.fbHashtags.value = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("#RohitVeterinaryHouse #PetCare #AnimalHealth") },
                                singleLine = true
                            )
                        }

                        // 6. Image Prompt
                        Column {
                            Text(
                                text = "Image Creative Prompt",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = fbImagePrompt,
                                onValueChange = { viewModel.fbImagePrompt.value = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Prompt describing image banner for this post") },
                                maxLines = 3
                            )
                        }
                    }
                }
            }

            // Facebook Live Preview Card
            item {
                Text(
                    text = "Live Facebook Feed Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                FacebookLivePreviewCard(
                    headline = fbHeadline,
                    caption = fbCaption,
                    cta = fbCta,
                    hashtags = fbHashtags,
                    imagePrompt = fbImagePrompt,
                    onCtaClick = {
                        Toast.makeText(context, "Simulated Action: $fbCta", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Actions Bar for Structured Post
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Actions for this Facebook Post",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.saveStructuredFacebookPost(PostStatus.DRAFT) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Draft", fontSize = 11.sp)
                            }
                            if (currentRole == UserRole.CONTENT_CREATOR) {
                                Button(
                                    onClick = { viewModel.saveStructuredFacebookPost(PostStatus.PENDING_APPROVAL) },
                                    colors = ButtonDefaults.buttonColors(containerColor = VetAmber),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Submit Approval", fontSize = 11.sp)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.saveStructuredFacebookPost(PostStatus.APPROVED) },
                                    colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Approve", fontSize = 11.sp)
                                }
                            }
                            Button(
                                onClick = { viewModel.saveStructuredFacebookPost(PostStatus.SCHEDULED) },
                                colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Schedule", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Facebook Posts History with Status Filters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Facebook Campaign History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filteredFacebookPosts.size} Posts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Status Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "All Posts",
                        "DRAFT" to "Drafts",
                        "SCHEDULED" to "Scheduled",
                        "PUBLISHED" to "Published"
                    ).forEach { (statusKey, label) ->
                        FilterChip(
                            selected = fbStatusFilter == statusKey,
                            onClick = { fbStatusFilter = statusKey },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }

            if (filteredFacebookPosts.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No Facebook posts found for filter '$fbStatusFilter'.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredFacebookPosts) { post ->
                    FacebookPostItemCard(
                        post = post,
                        currentRole = currentRole,
                        onApprove = { viewModel.approvePost(post.id) },
                        onSchedule = {
                            val scheduledTime = System.currentTimeMillis() + (86400000L * 2)
                            viewModel.schedulePost(post.id, scheduledTime)
                        },
                        onCopy = { copyToClipboard(context, "${post.title}\n\n${post.contentText}") }
                    )
                }
            }
        }
    }
}

@Composable
fun ContentTextPreview(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(14.dp),
            lineHeight = 22.sp
        )
    }
}

@Composable
fun AlternatesPreview(
    alt1: String,
    alt2: String,
    alt3: String,
    onCopy: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("Option A" to alt1, "Option B" to alt2, "Option C" to alt3).forEach { (label, text) ->
            if (text.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VetTeal
                            )
                            IconButton(
                                onClick = { onCopy(text) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy option",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun HashtagsAndPromptsPreview(
    hashtags: String,
    imagePrompt: String,
    videoPrompt: String,
    onCopy: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (hashtags.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recommended Hashtags",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VetTeal
                        )
                        IconButton(
                            onClick = { onCopy(hashtags) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy hashtags", modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = hashtags, color = VetTeal, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (imagePrompt.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Image Prompt (gemini-3-pro-image-preview)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1)
                        )
                        IconButton(
                            onClick = { onCopy(imagePrompt) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy image prompt", modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = imagePrompt, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (videoPrompt.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Video Prompt (veo-3.1-fast-generate-preview)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5CF6)
                        )
                        IconButton(
                            onClick = { onCopy(videoPrompt) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy video prompt", modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = videoPrompt, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun FacebookLivePreviewCard(
    headline: String,
    caption: String,
    cta: String,
    hashtags: String,
    imagePrompt: String,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Post Header (Page Info)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VetTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🐾", fontSize = 20.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Rohit Veterinary House",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = FacebookBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Sponsored",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "Just now",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Public",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Caption & Hashtags
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = caption.ifBlank { "Protect your pets and livestock with certified veterinary care and nutrition support." },
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
                if (hashtags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = hashtags,
                        color = FacebookBlue,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Post Media / Image Creative Container
            Surface(
                color = Color(0xFF0F172A),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = VetTeal.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🩺", fontSize = 22.sp)
                            }
                        }
                        Text(
                            text = headline.ifBlank { "Rohit Veterinary House" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Creative Prompt: " + (imagePrompt.ifBlank { "Clean veterinary clinic environment with doctor & animals" }).take(80) + "...",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }
                }
            }

            // Link & CTA Action Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ROHITVETERINARYHOUSE.COM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = headline.ifBlank { "Veterinary Consultations & Medicine" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = onCtaClick,
                        colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cta.ifBlank { "Learn More" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Social Engagement Counts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("👍 ❤️", fontSize = 12.sp)
                    Text(
                        text = "48",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "12 Comments • 6 Shares",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Social Interaction Buttons
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ThumbUp,
                            contentDescription = "Like",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Like", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comment",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Comment", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Share", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun FacebookPostItemCard(
    post: MarketingPost,
    currentRole: UserRole,
    onApprove: () -> Unit,
    onSchedule: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Audience: ${post.audience} • Lang: ${post.language}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(status = post.status)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = post.contentText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 11.sp)
                }

                if (post.status == PostStatus.DRAFT.name || post.status == PostStatus.PENDING_APPROVAL.name) {
                    if (currentRole != UserRole.CONTENT_CREATOR) {
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Approve", fontSize = 11.sp)
                        }
                    }
                }

                if (post.status == PostStatus.APPROVED.name) {
                    Button(
                        onClick = onSchedule,
                        colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Schedule", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("RVH Marketing Content", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
}
