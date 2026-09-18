package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.DashboardScreenPreview
import com.example.ui.theme.MyApplicationTheme
import com.example.data.model.UserRole
import com.example.ui.components.RolePill
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ChatAssistantScreen
import com.example.ui.screens.ContentCreatorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoMakerScreen
import com.example.ui.screens.WhatsAppCampaignScreen
import com.example.ui.theme.VetTeal
import com.example.ui.theme.WhatsAppDark
import com.example.ui.viewmodel.MarketingViewModel
import com.example.ui.viewmodel.NavTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    viewModel: MarketingViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(VetTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "RVH Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    RolePill(
                        role = currentRole,
                        onClick = {
                            val nextRole = when (currentRole) {
                                UserRole.ADMIN -> UserRole.MARKETING_STAFF
                                UserRole.MARKETING_STAFF -> UserRole.CONTENT_CREATOR
                                UserRole.CONTENT_CREATOR -> UserRole.ADMIN
                            }
                            viewModel.switchRole(nextRole)
                        }
                    )

                    IconButton(
                        onClick = { viewModel.navigateTo(NavTab.ASSISTANT) },
                        modifier = Modifier.testTag("topbar_ai_assistant_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Assistant",
                            tint = VetTeal
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("AI Assistant") },
                                leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null) },
                                onClick = {
                                    viewModel.navigateTo(NavTab.ASSISTANT)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Campaigns") },
                                leadingIcon = { Icon(Icons.Outlined.Campaign, null) },
                                onClick = {
                                    viewModel.navigateTo(NavTab.CAMPAIGNS)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Content Library") },
                                leadingIcon = { Icon(Icons.Outlined.FolderCopy, null) },
                                onClick = {
                                    viewModel.navigateTo(NavTab.LIBRARY)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Analytics & KPIs") },
                                leadingIcon = { Icon(Icons.Outlined.Analytics, null) },
                                onClick = {
                                    viewModel.navigateTo(NavTab.ANALYTICS)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings & Roles") },
                                leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                                onClick = {
                                    viewModel.navigateTo(NavTab.SETTINGS)
                                    showMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == NavTab.DASHBOARD,
                    onClick = { viewModel.navigateTo(NavTab.DASHBOARD) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = VetTeal, indicatorColor = VetTeal.copy(alpha = 0.15f))
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.CREATE,
                    onClick = { viewModel.navigateTo(NavTab.CREATE) },
                    icon = { Icon(Icons.Outlined.Create, contentDescription = "Create") },
                    label = { Text("Create", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = VetTeal, indicatorColor = VetTeal.copy(alpha = 0.15f))
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.VIDEO_MAKER,
                    onClick = { viewModel.navigateTo(NavTab.VIDEO_MAKER) },
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Video Maker") },
                    label = { Text("Video", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = VetTeal, indicatorColor = VetTeal.copy(alpha = 0.15f))
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.CALENDAR,
                    onClick = { viewModel.navigateTo(NavTab.CALENDAR) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") },
                    label = { Text("Calendar", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = VetTeal, indicatorColor = VetTeal.copy(alpha = 0.15f))
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.CAMPAIGNS,
                    onClick = { viewModel.navigateTo(NavTab.CAMPAIGNS) },
                    icon = { Icon(Icons.Default.Share, contentDescription = "WhatsApp") },
                    label = { Text("WhatsApp", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = WhatsAppDark, indicatorColor = Color(0xFFDCFCE7))
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.LIBRARY,
                    onClick = { viewModel.navigateTo(NavTab.LIBRARY) },
                    icon = { Icon(Icons.Outlined.FolderCopy, contentDescription = "Library") },
                    label = { Text("Library", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = VetTeal, indicatorColor = VetTeal.copy(alpha = 0.15f))
                )
            }
        },
        floatingActionButton = {
            if (currentTab != NavTab.ASSISTANT && currentTab != NavTab.CREATE) {
                FloatingActionButton(
                    onClick = { viewModel.navigateTo(NavTab.ASSISTANT) },
                    containerColor = VetTeal,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("main_fab_assistant")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Open AI Assistant")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                NavTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                NavTab.CREATE -> ContentCreatorScreen(viewModel = viewModel)
                NavTab.VIDEO_MAKER -> VideoMakerScreen(viewModel = viewModel)
                NavTab.CALENDAR -> CalendarScreen(viewModel = viewModel)
                NavTab.CAMPAIGNS -> WhatsAppCampaignScreen(viewModel = viewModel)
                NavTab.LIBRARY -> LibraryScreen(viewModel = viewModel)
                NavTab.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
                NavTab.ASSISTANT -> ChatAssistantScreen(viewModel = viewModel)
                NavTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScaffoldPreview() {
    MyApplicationTheme {
        DashboardScreenPreview()
    }
}


