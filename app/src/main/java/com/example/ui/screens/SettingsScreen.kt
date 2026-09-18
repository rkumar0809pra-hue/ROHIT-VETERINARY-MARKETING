package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.VetAmber
import com.example.ui.theme.VetBlue
import com.example.ui.theme.VetTeal
import com.example.ui.viewmodel.MarketingViewModel

@Composable
fun SettingsScreen(
    viewModel: MarketingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentRole by viewModel.currentRole.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings & Workspace",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manage operational roles, veterinary clinic identity, and marketing guidelines",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Active Role Switcher Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = VetTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Switch Active Operating Role",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Experience the app from different team members' perspectives (Owner approval vs Creator drafting):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    UserRole.values().forEach { role ->
                        val isSelected = currentRole == role
                        val roleColor = when (role) {
                            UserRole.ADMIN -> VetTeal
                            UserRole.MARKETING_STAFF -> VetBlue
                            UserRole.CONTENT_CREATOR -> VetAmber
                        }

                        Surface(
                            onClick = {
                                viewModel.switchRole(role)
                                Toast.makeText(context, "Role switched to ${role.label}", Toast.LENGTH_SHORT).show()
                            },
                            color = if (isSelected) roleColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) roleColor else Color.Gray.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = role.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) roleColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = role.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Clinic Business Identity Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.MedicalServices, contentDescription = null, tint = VetTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clinic Profile & Branding",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileField("Clinic Name", "Rohit Veterinary House")
                    ProfileField("Specialty", "Small Animals, Dairy Cattle, Goat & Poultry Medicine")
                    ProfileField("Helpline / WhatsApp", "+91 98765 43210")
                    ProfileField("Clinic Location", "Main Road, Animal Health Center, Bihar/India")
                    ProfileField("Clinic Hours", "Mon - Sat: 8:00 AM - 7:30 PM | Sun: 9:00 AM - 2:00 PM")
                }
            }
        }

        // Veterinary Compliance & Ethics Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Color(0xFF16A34A))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Veterinary Marketing Code of Ethics",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. No exaggerated claims: Never guarantee biological outcomes or miracle cures.\n2. Prioritize animal welfare: Promote timely diagnosis, cold-chain storage, and certified products.\n3. Transparent pricing & communication: Local farmers and pet parents receive honest, evidence-based guidance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF166534),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Gemini AI & Data Management
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = VetTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI System & Data Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gemini AI Engine Status",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Surface(
                            color = if (viewModel.isGeminiOnline()) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (viewModel.isGeminiOnline()) "Online (API Key Active)" else "Smart Offline Fallback Active",
                                color = if (viewModel.isGeminiOnline()) Color(0xFF166534) else Color(0xFF92400E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { viewModel.resetDemoData() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_demo_data_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Workspace to Sample Demo Data")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
