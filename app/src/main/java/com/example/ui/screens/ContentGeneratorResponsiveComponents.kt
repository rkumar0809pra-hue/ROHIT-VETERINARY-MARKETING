package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUp
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.GeneratedContentBundle
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
fun ContentGeneratorFormPanel(
    viewModel: MarketingViewModel,
    modifier: Modifier = Modifier
) {
    val platform by viewModel.creatorPlatform.collectAsState()
    val goal by viewModel.creatorGoal.collectAsState()
    val audience by viewModel.creatorAudience.collectAsState()
    val service by viewModel.creatorService.collectAsState()
    val tone by viewModel.creatorTone.collectAsState()
    val language by viewModel.creatorLanguage.collectAsState()
    val cta by viewModel.creatorCta.collectAsState()
    val isGenerating by viewModel.isGeneratingContent.collectAsState()

    var selectedAnimalCategory by remember { mutableIntStateOf(0) } // 0: Cattle/Dairy, 1: Pets, 2: Goats & Poultry
    var includeColdChain by remember { mutableStateOf(true) }
    var includePrescriptionNotice by remember { mutableStateOf(true) }
    var includeHelpline by remember { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Gemini badge & RVH branding
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(VetTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = VetTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Veterinary Copy Generator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Powered by Gemini AI • Rohit Veterinary House",
                            style = MaterialTheme.typography.labelSmall,
                            color = VetTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Vet-Compliant",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 1. Target Social Media / Marketing Platform
            Column {
                Text(
                    text = "1. Target Platform",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Platform.values().forEach { p ->
                        val isSelected = platform == p.name
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.creatorPlatform.value = p.name },
                            label = { Text(p.label, fontSize = 12.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VetTeal.copy(alpha = 0.15f),
                                selectedLabelColor = VetTeal
                            )
                        )
                    }
                }
            }

            // 2. Clinical Category / Marketing Goal
            Column {
                Text(
                    text = "2. Clinical Category & Goal",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
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
            }

            // 3. Target Veterinary Audience
            Column {
                Text(
                    text = "3. Target Audience",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
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
            }

            // 4. Veterinary Product / Medicine / Service
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "4. Veterinary Product / Medication / Service",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Required",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = service,
                    onValueChange = { viewModel.creatorService.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_product_input"),
                    placeholder = { Text("e.g. Chelated Mineral Mixture, Anti-Rabies Vaccine, Mastitis Teat Dip") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocalPharmacy,
                            contentDescription = null,
                            tint = VetTeal
                        )
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Animal Category Tabs for Quick Suggest
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf("🐄 Dairy Cattle", "🐕 Pets / Dogs", "🐐 Goats & Poultry")
                    tabs.forEachIndexed { index, title ->
                        Surface(
                            onClick = { selectedAnimalCategory = index },
                            color = if (selectedAnimalCategory == index) VetTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedAnimalCategory == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedAnimalCategory == index) VetTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Categorized Veterinary Product Chips
                val productSuggestions = when (selectedAnimalCategory) {
                    0 -> listOf(
                        "Chelated Mineral Mixture (Milk Booster)",
                        "Mastitis Teat Dip & Spray",
                        "Calcium Gel Oral Drench",
                        "Foot Rot Copper Sulfate Bath",
                        "Albendazole Dewormer Suspension",
                        "H.S. & B.Q. Seasonal Vaccine"
                    )
                    1 -> listOf(
                        "Anti-Rabies Shot Drive",
                        "Puppy 7-in-1 Vaccination",
                        "Tick & Flea Fipronil Spot-On",
                        "Senior Dog Joint Glucosamine",
                        "Kitten Multivitamin Drops",
                        "Deworming Syrup for Pups"
                    )
                    else -> listOf(
                        "Goat Enterotoxemia (E.T.) Vaccine",
                        "Poultry Electrolytes & Vitamin AD3E",
                        "Livestock Deworming Ivermectin",
                        "P.P.R. Goat Health Reminder",
                        "Broiler Gut Probiotics"
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    productSuggestions.forEach { suggestion ->
                        Surface(
                            onClick = { viewModel.creatorService.value = suggestion },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MedicalServices,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = VetTeal
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = suggestion,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Veterinary Compliance & Clinical Safeguards
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Clinical Safeguards & Disclaimers",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = includeColdChain,
                            onClick = { includeColdChain = !includeColdChain },
                            label = { Text("❄️ Cold-Chain Assured (2°C - 8°C)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = includePrescriptionNotice,
                            onClick = { includePrescriptionNotice = !includePrescriptionNotice },
                            label = { Text("🩺 Vet Supervision Notice", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = includeHelpline,
                            onClick = { includeHelpline = !includeHelpline },
                            label = { Text("📞 RVH Helpline: +91 98765 43210", fontSize = 11.sp) }
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
            Column {
                Text(
                    text = "7. Call To Action (CTA)",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Call Now", "WhatsApp Now", "Book Consultation", "Order Certified Medicine", "Visit Rohit Veterinary House").forEach { ctaItem ->
                        FilterChip(
                            selected = cta == ctaItem,
                            onClick = { viewModel.creatorCta.value = ctaItem },
                            label = { Text(ctaItem, fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Generate Button
            Button(
                onClick = {
                    // Enrich CTA with safeguards if selected
                    var enrichedCta = viewModel.creatorCta.value
                    if (includeHelpline && !enrichedCta.contains("98765")) {
                        enrichedCta += " (📞 +91 98765 43210)"
                    }
                    if (includeColdChain && !enrichedCta.contains("Cold-Chain")) {
                        enrichedCta += " [Cold-Chain Assured]"
                    }
                    viewModel.creatorCta.value = enrichedCta

                    viewModel.generateMarketingContent()
                },
                enabled = !isGenerating && service.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("generate_content_button")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Drafting with Gemini AI...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Veterinary Copy", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContentGeneratorResultsPanel(
    viewModel: MarketingViewModel,
    generatedBundle: GeneratedContentBundle?,
    statusMessage: String?,
    context: Context,
    currentRole: UserRole,
    modifier: Modifier = Modifier
) {
    val platform by viewModel.creatorPlatform.collectAsState()
    val isGenerating by viewModel.isGeneratingContent.collectAsState()
    var selectedResultTab by remember { mutableIntStateOf(0) }
    val resultTabs = listOf("Primary Post", "Short / SMS", "Educational", "3 Alternates", "Hashtags & Prompts")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status Message
        if (statusMessage != null) {
            Surface(
                color = VetTeal.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = VetTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusMessage,
                        color = VetTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (isGenerating) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = VetTeal,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Consulting Veterinary Marketing Engine...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Drafting targeted Hinglish copy, clinical safeguards, hashtags, and social post variations via Gemini API.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else if (generatedBundle == null) {
            // Empty State
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(VetTeal.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = VetTeal,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "Ready to Generate Veterinary Copy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select your target platform and animal product on the left (or above on mobile), then tap 'Generate Veterinary Copy' to produce full posts, SMS blasts, Hinglish variants, and AI visual prompts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        } else {
            // Live Social Preview Simulation Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generated_results_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Top Bar with Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (platform) {
                                    Platform.WHATSAPP.name -> WhatsAppDark.copy(alpha = 0.15f)
                                    Platform.FACEBOOK.name -> FacebookBlue.copy(alpha = 0.15f)
                                    else -> VetTeal.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = platform,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (platform) {
                                        Platform.WHATSAPP.name -> WhatsAppDark
                                        Platform.FACEBOOK.name -> FacebookBlue
                                        else -> VetTeal
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Drafted Post Copy",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    val textToCopy = when (selectedResultTab) {
                                        0 -> generatedBundle.primaryText
                                        1 -> generatedBundle.shortVersion
                                        2 -> generatedBundle.longVersion
                                        3 -> "${generatedBundle.alternate1}\n\n${generatedBundle.alternate2}\n\n${generatedBundle.alternate3}"
                                        else -> "${generatedBundle.hashtags}\n\nVisual Prompt: ${generatedBundle.imagePrompt}"
                                    }
                                    copyText(context, textToCopy)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Content",
                                    tint = VetTeal
                                )
                            }
                            IconButton(
                                onClick = {
                                    val textToShare = generatedBundle.primaryText
                                    shareText(context, textToShare)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Content",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Platform Live Mockup Banner
                    if (platform == Platform.WHATSAPP.name) {
                        WhatsAppLivePreview(generatedBundle.primaryText)
                    } else if (platform == Platform.FACEBOOK.name) {
                        FacebookLivePreview(generatedBundle.primaryText)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Format Subtabs
                    ScrollableTabRow(
                        selectedTabIndex = selectedResultTab,
                        edgePadding = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        resultTabs.forEachIndexed { index, tabName ->
                            Tab(
                                selected = selectedResultTab == index,
                                onClick = { selectedResultTab = index },
                                text = { Text(tabName, fontSize = 12.sp, fontWeight = if (selectedResultTab == index) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab View Content
                    when (selectedResultTab) {
                        0 -> ContentTextPreview(text = generatedBundle.primaryText)
                        1 -> ContentTextPreview(text = generatedBundle.shortVersion)
                        2 -> ContentTextPreview(text = generatedBundle.longVersion)
                        3 -> AlternatesPreview(
                            alt1 = generatedBundle.alternate1,
                            alt2 = generatedBundle.alternate2,
                            alt3 = generatedBundle.alternate3,
                            onCopy = { copyText(context, it) }
                        )
                        4 -> HashtagsAndPromptsPreview(
                            hashtags = generatedBundle.hashtags,
                            imagePrompt = generatedBundle.imagePrompt,
                            videoPrompt = generatedBundle.videoPrompt,
                            onCopy = { copyText(context, it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Workflow Actions based on Role
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
}

@Composable
private fun WhatsAppLivePreview(text: String) {
    Surface(
        color = Color(0xFFECE5DD),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // WhatsApp Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(WhatsAppDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Rohit Veterinary House (Broadcast)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF075E54)
                )
            }

            // WhatsApp Bubble
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = text.take(160) + if (text.length > 160) "..." else "",
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "10:30 AM", fontSize = 9.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "✓✓", fontSize = 9.sp, color = Color(0xFF34B7F1), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FacebookLivePreview(text: String) {
    Surface(
        color = Color(0xFFF0F2F5),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(FacebookBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text("RV", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Rohit Veterinary House",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050505)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = FacebookBlue,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(text = "Just now • 🌍", fontSize = 10.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text.take(160) + if (text.length > 160) "..." else "",
                fontSize = 12.sp,
                color = Color(0xFF050505)
            )
        }
    }
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("RVH Veterinary Marketing", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Veterinary Marketing via"))
}
