package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudienceType
import com.example.data.model.PostStatus
import com.example.data.model.UserRole
import com.example.data.model.WhatsAppCampaign
import com.example.ui.components.ResponsiveContentContainer
import com.example.ui.components.ResponsiveTwoPaneLayout
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusScheduled
import com.example.ui.theme.VetAmber
import com.example.ui.theme.VetTeal
import com.example.ui.theme.WhatsAppDark
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.util.rememberScreenLayoutInfo
import com.example.ui.viewmodel.MarketingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatsAppCampaignScreen(
    viewModel: MarketingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentRole by viewModel.currentRole.collectAsState()
    val campaigns by viewModel.allCampaigns.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    var campaignName by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf(AudienceType.CATTLE_OWNERS.name) }
    var templateBody by remember {
        mutableStateOf("नमस्ते {{CustomerName}} जी! रोहित वेटरनरी हाउस की ओर से आपके {{AnimalType}} के लिए विशेष स्वास्थ्य संदेश। गर्मी में दुग्ध उत्पादन और स्वास्थ्य सुरक्षा के लिए प्रमाणित मिनरल मिक्सचर उपलब्ध है। परामर्श के लिए संपर्क करें: {{ClinicPhone}}")
    }
    var ctaType by remember { mutableStateOf("Call Now") }
    var ctaValue by remember { mutableStateOf("+91 98765 43210") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    // Substituted preview for template
    val previewText = templateBody
        .replace("{{CustomerName}}", "Ramesh Kumar")
        .replace("{{AnimalType}}", "Holstein Cow / Buffalo")
        .replace("{{ClinicPhone}}", "+91 98765 43210")
        .replace("{{SpecialOffer}}", "10% Off on Chelated Mineral Mix")

    val filteredCampaigns = campaigns.filter { campaign ->
        when (selectedStatusFilter) {
            "ALL" -> true
            "DRAFT" -> campaign.status == PostStatus.DRAFT.name
            "SCHEDULED" -> campaign.status == PostStatus.SCHEDULED.name
            "PUBLISHED" -> campaign.status == PostStatus.PUBLISHED.name
            else -> true
        }
    }

    val previewMode by viewModel.previewDeviceMode.collectAsState()
    val layoutInfo = rememberScreenLayoutInfo(previewMode)

    ResponsiveContentContainer(modifier = modifier) {
        if (layoutInfo.isExpanded) {
            ResponsiveTwoPaneLayout(
                isWideScreen = true,
                primaryWeight = 0.48f,
                secondaryWeight = 0.52f,
                primaryPane = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("whatsapp_campaign_left_pane"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column {
                                Text(
                                    text = "WhatsApp Campaigns & Broadcast",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Target specific customer groups with personalized templates and immediate WhatsApp action",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Audience Segments Card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Customer Groups & Audience Segments",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Target specific animal owners with high-converting messages",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AudienceSegmentBadge(
                                            title = "Cattle Dairy",
                                            count = "240 Farmers",
                                            color = Color(0xFF0D9488),
                                            isSelected = selectedGroup == AudienceType.CATTLE_OWNERS.name,
                                            onClick = { selectedGroup = AudienceType.CATTLE_OWNERS.name },
                                            modifier = Modifier.weight(1f)
                                        )
                                        AudienceSegmentBadge(
                                            title = "Pet Parents",
                                            count = "110 Owners",
                                            color = Color(0xFF0284C7),
                                            isSelected = selectedGroup == AudienceType.PET_OWNERS.name,
                                            onClick = { selectedGroup = AudienceType.PET_OWNERS.name },
                                            modifier = Modifier.weight(1f)
                                        )
                                        AudienceSegmentBadge(
                                            title = "Goat Farming",
                                            count = "75 Herders",
                                            color = Color(0xFFD97706),
                                            isSelected = selectedGroup == AudienceType.GOAT_FARMERS.name,
                                            onClick = { selectedGroup = AudienceType.GOAT_FARMERS.name },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Creator Form Panel on Left
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Create WhatsApp Broadcast Campaign",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = WhatsAppDark
                                    )

                                    // Campaign Name
                                    OutlinedTextField(
                                        value = campaignName,
                                        onValueChange = { campaignName = it },
                                        label = { Text("Campaign Name") },
                                        placeholder = { Text("e.g. Cattle Summer Booster Drive") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    // Target Audience Group
                                    Text(
                                        text = "Target Customer Group",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        AudienceType.values().forEach { group ->
                                            FilterChip(
                                                selected = selectedGroup == group.name,
                                                onClick = { selectedGroup = group.name },
                                                label = { Text(group.label, fontSize = 11.sp) }
                                            )
                                        }
                                    }

                                    // Template Message
                                    Text(
                                        text = "Message Template",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    OutlinedTextField(
                                        value = templateBody,
                                        onValueChange = { templateBody = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        placeholder = { Text("Enter WhatsApp message template with {{Variables}}") }
                                    )

                                    // Variable Pills
                                    Text(
                                        text = "Tap to insert variable token:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("{{CustomerName}}", "{{AnimalType}}", "{{ClinicPhone}}", "{{SpecialOffer}}").forEach { token ->
                                            Surface(
                                                onClick = { templateBody = "$templateBody $token" },
                                                color = Color(0xFFDCFCE7),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = token,
                                                    fontSize = 11.sp,
                                                    color = WhatsAppDark,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    // CTA Button Type
                                    Text(
                                        text = "Action Button (CTA)",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Call Now", "WhatsApp Now", "Book Consultation", "Order Product").forEach { cta ->
                                            FilterChip(
                                                selected = ctaType == cta,
                                                onClick = { ctaType = cta },
                                                label = { Text(cta, fontSize = 11.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Submit Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                if (campaignName.isNotBlank()) {
                                                    viewModel.saveNewCampaign(campaignName, selectedGroup, templateBody, ctaType, ctaValue)
                                                    campaignName = ""
                                                    Toast.makeText(context, "Campaign saved as Draft!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Save Draft")
                                        }

                                        Button(
                                            onClick = {
                                                if (campaignName.isNotBlank()) {
                                                    val scheduledTime = System.currentTimeMillis() + (86400000L * 2)
                                                    viewModel.saveNewCampaign(campaignName, selectedGroup, templateBody, ctaType, ctaValue, scheduledTime)
                                                    campaignName = ""
                                                    Toast.makeText(context, "Campaign scheduled for upcoming broadcast!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppDark),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Schedule")
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
                            .testTag("whatsapp_campaign_right_pane"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Live Phone Preview Card on Right
                        item {
                            Text(
                                text = "WhatsApp Live Template Preview (Client's Screen)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = WhatsAppDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            WhatsAppLivePreviewCard(
                                previewText = previewText,
                                ctaType = ctaType,
                                ctaValue = ctaValue
                            )
                        }

                        // Broadcast History & Active Campaigns List
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Campaign Broadcasts & History",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${filteredCampaigns.size} Campaigns",
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
                                    "ALL" to "All Status",
                                    "DRAFT" to "Drafts",
                                    "SCHEDULED" to "Scheduled",
                                    "PUBLISHED" to "Sent Broadcasts"
                                ).forEach { (filterKey, label) ->
                                    val isSelected = selectedStatusFilter == filterKey
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedStatusFilter = filterKey },
                                        label = { Text(label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        if (filteredCampaigns.isEmpty()) {
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
                                            text = "No campaigns found matching '$selectedStatusFilter'.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredCampaigns) { campaign ->
                                WhatsAppCampaignCard(
                                    campaign = campaign,
                                    currentRole = currentRole,
                                    onApprove = { viewModel.approveCampaign(campaign) },
                                    onSend = { viewModel.launchWhatsAppCampaign(campaign, context) }
                                )
                            }
                        }
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("whatsapp_campaign_screen"),
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
                        text = "WhatsApp Campaigns",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Broadcasts, Status Updates & Audience Segments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showCreateDialog = !showCreateDialog },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("toggle_create_campaign_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showCreateDialog) "Close Form" else "New Campaign")
                }
            }
        }

        // Creator Form Panel
        if (showCreateDialog) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Create WhatsApp Broadcast Campaign",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WhatsAppDark
                        )

                        // Campaign Name
                        OutlinedTextField(
                            value = campaignName,
                            onValueChange = { campaignName = it },
                            label = { Text("Campaign Name") },
                            placeholder = { Text("e.g. Cattle Summer Booster Drive") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Target Audience Group
                        Text(
                            text = "Target Customer Group",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AudienceType.values().forEach { group ->
                                FilterChip(
                                    selected = selectedGroup == group.name,
                                    onClick = { selectedGroup = group.name },
                                    label = { Text(group.label, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Template Message
                        Text(
                            text = "Message Template",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = templateBody,
                            onValueChange = { templateBody = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            placeholder = { Text("Enter WhatsApp message template with {{Variables}}") }
                        )

                        // Variable Pills
                        Text(
                            text = "Tap to insert variable token:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("{{CustomerName}}", "{{AnimalType}}", "{{ClinicPhone}}", "{{SpecialOffer}}").forEach { token ->
                                Surface(
                                    onClick = { templateBody = "$templateBody $token" },
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = token,
                                        fontSize = 11.sp,
                                        color = WhatsAppDark,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // CTA Button Type
                        Text(
                            text = "Action Button (CTA)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Call Now", "WhatsApp Now", "Book Consultation", "Order Product").forEach { cta ->
                                FilterChip(
                                    selected = ctaType == cta,
                                    onClick = { ctaType = cta },
                                    label = { Text(cta, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // WhatsApp Live Template Preview Card
                        Text(
                            text = "Template Live Preview (Client's Screen)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = WhatsAppDark
                        )
                        WhatsAppLivePreviewCard(
                            previewText = previewText,
                            ctaType = ctaType,
                            ctaValue = ctaValue
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Submit Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (campaignName.isNotBlank()) {
                                        viewModel.saveNewCampaign(campaignName, selectedGroup, templateBody, ctaType, ctaValue)
                                        showCreateDialog = false
                                        Toast.makeText(context, "Campaign saved as Draft!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Draft")
                            }

                            Button(
                                onClick = {
                                    if (campaignName.isNotBlank()) {
                                        val scheduledTime = System.currentTimeMillis() + (86400000L * 2)
                                        viewModel.saveNewCampaign(campaignName, selectedGroup, templateBody, ctaType, ctaValue, scheduledTime)
                                        showCreateDialog = false
                                        Toast.makeText(context, "Campaign scheduled for upcoming broadcast!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppDark),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Schedule")
                            }
                        }
                    }
                }
            }
        }

        // Customer Group Audiences Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Customer Groups & Audience Segments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Target specific animal owners with high-converting messages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AudienceSegmentBadge(
                            title = "Cattle Dairy",
                            count = "240 Farmers",
                            color = Color(0xFF0D9488),
                            isSelected = selectedGroup == AudienceType.CATTLE_OWNERS.name,
                            onClick = { selectedGroup = AudienceType.CATTLE_OWNERS.name },
                            modifier = Modifier.weight(1f)
                        )
                        AudienceSegmentBadge(
                            title = "Pet Parents",
                            count = "110 Owners",
                            color = Color(0xFF0284C7),
                            isSelected = selectedGroup == AudienceType.PET_OWNERS.name,
                            onClick = { selectedGroup = AudienceType.PET_OWNERS.name },
                            modifier = Modifier.weight(1f)
                        )
                        AudienceSegmentBadge(
                            title = "Goat Farming",
                            count = "75 Herders",
                            color = Color(0xFFD97706),
                            isSelected = selectedGroup == AudienceType.GOAT_FARMERS.name,
                            onClick = { selectedGroup = AudienceType.GOAT_FARMERS.name },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Campaign History Section with Status Filter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Campaign Broadcasts & History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredCampaigns.size} Campaigns",
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
                    "ALL" to "All Status",
                    "DRAFT" to "Drafts",
                    "SCHEDULED" to "Scheduled",
                    "PUBLISHED" to "Sent Broadcasts"
                ).forEach { (filterKey, label) ->
                    val isSelected = selectedStatusFilter == filterKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedStatusFilter = filterKey },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
        }

        if (filteredCampaigns.isEmpty()) {
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
                            text = "No campaigns found matching '$selectedStatusFilter'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredCampaigns) { campaign ->
                WhatsAppCampaignCard(
                    campaign = campaign,
                    currentRole = currentRole,
                    onApprove = { viewModel.approveCampaign(campaign) },
                    onSend = { viewModel.launchWhatsAppCampaign(campaign, context) }
                )
            }
        }
    }
    }
}
}

@Composable
fun AudienceSegmentBadge(
    title: String,
    count: String,
    color: Color,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) color.copy(alpha = 0.22f) else color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun WhatsAppLivePreviewCard(
    previewText: String,
    ctaType: String,
    ctaValue: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEAE2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Simulated WhatsApp Chat Header
            Surface(
                color = WhatsAppDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐾", fontSize = 16.sp)
                    }
                    Column {
                        Text(
                            text = "Rohit Veterinary House",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4ADE80))
                            )
                            Text(
                                text = "Official Business Account",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Chat Body Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Incoming Message Bubble
                Surface(
                    color = Color(0xFFE7FFDB),
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = previewText,
                            fontSize = 13.sp,
                            color = Color(0xFF111827),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "10:30 AM",
                                fontSize = 10.sp,
                                color = Color(0xFF6B7280)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "✓✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B82F6)
                            )
                        }
                    }
                }

                // CTA Button Preview Row
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when {
                            ctaType.contains("Call", ignoreCase = true) -> Icons.Default.Call
                            ctaType.contains("WhatsApp", ignoreCase = true) -> Icons.Default.Send
                            else -> Icons.Default.DateRange
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = WhatsAppDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                ctaType.contains("Call", ignoreCase = true) -> "Call: $ctaValue"
                                ctaType.contains("WhatsApp", ignoreCase = true) -> "WhatsApp Direct Message"
                                else -> ctaType
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhatsAppDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WhatsAppCampaignCard(
    campaign: WhatsAppCampaign,
    currentRole: UserRole,
    onApprove: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
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
                Column {
                    Text(
                        text = campaign.campaignName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Target: ${campaign.targetGroup} • ${campaign.totalRecipients} Contacts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(status = campaign.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = Color(0xFFF0FDF4),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = campaign.templateBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF166534),
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Text(text = "Sent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${campaign.sentCount}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Column {
                        Text(text = "Failed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${campaign.failedCount}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Column {
                        Text(text = "Responses", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${campaign.responseCount}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VetTeal)
                    }
                }

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (campaign.status == PostStatus.DRAFT.name && currentRole == UserRole.ADMIN) {
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Approve", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = onSend,
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppDark),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (campaign.sentCount > 0) "Resend / Share" else "Send WhatsApp", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
