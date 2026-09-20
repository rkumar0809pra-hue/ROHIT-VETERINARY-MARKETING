package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.MarketingPostIdea
import com.example.data.gemini.SocialMediaCaption
import com.example.data.gemini.ThemeMarketingResult
import com.example.data.model.AudienceType
import com.example.data.model.ContentLanguage
import com.example.data.model.ContentTone
import com.example.data.model.Platform
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.VetTeal
import com.example.ui.viewmodel.MarketingViewModel

/**
 * Reusable Theme Studio Form (Left panel on Desktop, stacked top on Mobile).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ThemeStudioFormPanel(
    viewModel: MarketingViewModel,
    modifier: Modifier = Modifier
) {
    val themeInput by viewModel.themeInput.collectAsState()
    val themeAudience by viewModel.themeAudience.collectAsState()
    val themePlatform by viewModel.themePlatform.collectAsState()
    val themeTone by viewModel.themeTone.collectAsState()
    val themeLanguage by viewModel.themeLanguage.collectAsState()
    val isGeneratingTheme by viewModel.isGeneratingTheme.collectAsState()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
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
                    text = "Theme-Based AI Generator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VetTeal
                )
                Surface(
                    color = VetTeal.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = VetTeal,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Gemini 3.5 Flash",
                            color = VetTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 1. Theme Input
            Text(
                text = "1. Enter Campaign Theme or Veterinary Topic",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = themeInput,
                onValueChange = { viewModel.themeInput.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("theme_input_field"),
                placeholder = { Text("e.g. Monsoon Foot Rot, Puppy Parvo Camp, Heat Stress Milk Drop") },
                singleLine = true
            )

            // Quick Themes
            Text(
                text = "Popular Rohit Veterinary House Themes:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "Monsoon Foot Rot & Ticks",
                    "Heat Stress & Milk Drop",
                    "Puppy Parvo & Rabies Camp",
                    "Chelated Mineral Nutrition",
                    "Goat Deworming & Weight Gain",
                    "Mastitis Teat Dip & Udder Care",
                    "Poultry Heat Stress Electrolytes"
                ).forEach { qTheme ->
                    Surface(
                        onClick = { viewModel.themeInput.value = qTheme },
                        color = if (themeInput == qTheme) VetTeal.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(
                            text = qTheme,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (themeInput == qTheme) VetTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (themeInput == qTheme) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // 2. Target Audience
            Text(
                text = "2. Target Audience",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AudienceType.values().forEach { a ->
                    FilterChip(
                        selected = themeAudience == a.name,
                        onClick = { viewModel.themeAudience.value = a.name },
                        label = {
                            Text(text = a.label, fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                    )
                }
            }

            // 3. Platform
            Text(
                text = "3. Target Platform",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Platform.values().forEach { p ->
                    FilterChip(
                        selected = themePlatform == p.name,
                        onClick = { viewModel.themePlatform.value = p.name },
                        label = {
                            Text(text = p.label, fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                    )
                }
            }

            // 4. Tone & Language Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "4. Tone",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    var toneExp by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = toneExp,
                        onExpandedChange = { toneExp = !toneExp }
                    ) {
                        OutlinedTextField(
                            value = ContentTone.valueOf(themeTone).label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toneExp) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = toneExp,
                            onDismissRequest = { toneExp = false }
                        ) {
                            ContentTone.values().forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.label) },
                                    onClick = {
                                        viewModel.themeTone.value = t.name
                                        toneExp = false
                                    }
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "5. Language",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    var langExp by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = langExp,
                        onExpandedChange = { langExp = !langExp }
                    ) {
                        OutlinedTextField(
                            value = ContentLanguage.valueOf(themeLanguage).label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExp) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = langExp,
                            onDismissRequest = { langExp = false }
                        ) {
                            ContentLanguage.values().forEach { l ->
                                DropdownMenuItem(
                                    text = { Text(l.label) },
                                    onClick = {
                                        viewModel.themeLanguage.value = l.name
                                        langExp = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Generate Button
            Button(
                onClick = { viewModel.generateThemeCaptionsAndPostIdeas() },
                enabled = !isGeneratingTheme && themeInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("generate_theme_marketing_button")
            ) {
                if (isGeneratingTheme) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating with Gemini...")
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Captions & Post Ideas", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Reusable Theme Results Panel (Right panel on Desktop, stacked bottom on Mobile).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeStudioResultsPanel(
    viewModel: MarketingViewModel,
    themeResult: ThemeMarketingResult?,
    themeStatusMessage: String?,
    context: Context,
    modifier: Modifier = Modifier
) {
    var themeResultSubTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (themeStatusMessage != null) {
            Surface(
                color = VetTeal.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = themeStatusMessage,
                    color = VetTeal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (themeResult != null) {
            val plan = themeResult

            // Strategic Campaign Overview Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Strategic Campaign Overview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = plan.theme,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = plan.strategicOverview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Sub-tabs: Captions vs Post Ideas
            TabRow(
                selectedTabIndex = themeResultSubTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = themeResultSubTab == 0,
                    onClick = { themeResultSubTab = 0 },
                    text = {
                        Text(
                            "Social Media Captions (${plan.captions.size})",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                )
                Tab(
                    selected = themeResultSubTab == 1,
                    onClick = { themeResultSubTab = 1 },
                    text = {
                        Text(
                            "Marketing Post Ideas (${plan.postIdeas.size})",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                )
            }

            if (themeResultSubTab == 0) {
                // Captions list
                plan.captions.forEach { caption ->
                    CaptionCardItem(
                        caption = caption,
                        context = context,
                        onSave = { viewModel.saveCaptionAsPost(caption) }
                    )
                }
            } else {
                // Post ideas list
                plan.postIdeas.forEach { idea ->
                    PostIdeaCardItem(
                        idea = idea,
                        context = context,
                        onSave = { viewModel.savePostIdeaAsPost(idea) }
                    )
                }
            }
        } else {
            // Empty state placeholder on wide screens
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = VetTeal,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Theme Results Preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose your theme, platform, and audience on the left, then click 'Generate Captions & Post Ideas' to see campaign plans, captions, and creative briefs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaptionCardItem(
    caption: SocialMediaCaption,
    context: Context,
    onSave: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = VetTeal.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = caption.style,
                        color = VetTeal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = caption.recommendedPlatform,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = caption.captionText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }

            if (caption.hashtags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    caption.hashtags.forEach { tag ->
                        Text(
                            text = tag,
                            fontSize = 11.sp,
                            color = FacebookBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Surface(
                color = VetTeal.copy(alpha = 0.08f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = caption.callToAction,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VetTeal,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            val fullCaptionText = "${caption.captionText}\n\n${caption.hashtags.joinToString(" ")}\n\n${caption.callToAction}"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("RVH Caption", fullCaptionText)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Copied caption!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 11.sp)
                }
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Text("Save to Library", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, fullCaptionText)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share Caption via"))
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun PostIdeaCardItem(
    idea: MarketingPostIdea,
    context: Context,
    onSave: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = idea.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = VetTeal.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = idea.format,
                        color = VetTeal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Angle: ${idea.angle}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = idea.bestTimeToPost,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "🎨 Visual Creative Direction:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = idea.visualCreativePrompt,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = "💡 Key Takeaway: ${idea.keyTakeaway}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = VetTeal.copy(alpha = 0.08f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📢 CTA: ${idea.callToAction}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VetTeal,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val text = "Post Idea: ${idea.title}\nAngle: ${idea.angle}\nFormat: ${idea.format}\n\nCreative Prompt: ${idea.visualCreativePrompt}\n\nTakeaway: ${idea.keyTakeaway}\nCTA: ${idea.callToAction}"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("RVH Idea", text)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Copied idea!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Idea", fontSize = 11.sp)
                }
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Text("Save as Post to Library", fontSize = 11.sp)
                }
            }
        }
    }
}
