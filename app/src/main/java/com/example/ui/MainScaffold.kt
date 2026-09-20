package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Devices
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.components.DesktopNavSidebar
import com.example.ui.components.RolePill
import com.example.ui.components.TabletNavRail
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ChatAssistantScreen
import com.example.ui.screens.ContentCreatorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DashboardScreenPreview
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoMakerScreen
import com.example.ui.screens.WhatsAppCampaignScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VetTeal
import com.example.ui.theme.WhatsAppDark
import com.example.ui.util.rememberScreenLayoutInfo
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
    val previewDeviceMode by viewModel.previewDeviceMode.collectAsState()
    val layoutInfo = rememberScreenLayoutInfo(previewDeviceMode)

    var showMenu by remember { mutableStateOf(false) }
    var showDeviceModeMenu by remember { mutableStateOf(false) }

    // Preserve back navigation: return to Dashboard from secondary tabs before exiting
    BackHandler(enabled = currentTab != NavTab.DASHBOARD) {
        viewModel.navigateTo(NavTab.DASHBOARD)
    }

    val cycleRole = {
        val nextRole = when (currentRole) {
            UserRole.ADMIN -> UserRole.MARKETING_STAFF
            UserRole.MARKETING_STAFF -> UserRole.CONTENT_CREATOR
            UserRole.CONTENT_CREATOR -> UserRole.ADMIN
        }
        viewModel.switchRole(nextRole)
    }

    Row(modifier = modifier.fillMaxSize()) {
        // 1. Desktop Sidebar (Expanded screen breakpoint)
        if (layoutInfo.isExpanded) {
            DesktopNavSidebar(
                currentTab = currentTab,
                currentRole = currentRole,
                onTabSelected = { viewModel.navigateTo(it) },
                onRoleClick = cycleRole
            )
        } else if (layoutInfo.isMedium) {
            // 2. Tablet Navigation Rail (Medium screen breakpoint)
            TabletNavRail(
                currentTab = currentTab,
                onTabSelected = { viewModel.navigateTo(it) }
            )
        }

        // Main App Area with Scaffold
        Scaffold(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!layoutInfo.isExpanded) {
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
                            }
                            Text(
                                text = if (layoutInfo.isExpanded) "Rohit Veterinary House • Marketing Command Center" else "RVH Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = if (layoutInfo.isExpanded) 17.sp else 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        // Device Layout Switcher (Lets users inspect Mobile, Tablet, & Desktop modes)
                        Box {
                            Surface(
                                onClick = { showDeviceModeMenu = true },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Devices,
                                        contentDescription = "Device View Mode",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = previewDeviceMode,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showDeviceModeMenu,
                                onDismissRequest = { showDeviceModeMenu = false }
                            ) {
                                listOf(
                                    "AUTO" to "Auto (Responsive Detection)",
                                    "DESKTOP" to "Desktop Mode (1200dp+)",
                                    "TABLET" to "Tablet Mode (600dp - 840dp)",
                                    "MOBILE" to "Mobile Mode (<600dp)"
                                ).forEach { (modeKey, modeLabel) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = modeLabel,
                                                fontSize = 12.sp,
                                                fontWeight = if (previewDeviceMode == modeKey) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.setPreviewDeviceMode(modeKey)
                                            showDeviceModeMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        if (!layoutInfo.isExpanded) {
                            RolePill(
                                role = currentRole,
                                onClick = cycleRole
                            )
                        }

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
            // Bottom navigation only on Mobile / Compact screens to prevent redundant bars on desktop/tablet
            bottomBar = {
                if (layoutInfo.isCompact) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        NavigationBarItem(
                            selected = currentTab == NavTab.DASHBOARD,
                            onClick = { viewModel.navigateTo(NavTab.DASHBOARD) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                            label = { Text("Home", fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )

                        NavigationBarItem(
                            selected = currentTab == NavTab.CREATE,
                            onClick = { viewModel.navigateTo(NavTab.CREATE) },
                            icon = { Icon(Icons.Outlined.Create, contentDescription = "Create Marketing Content") },
                            label = { Text("Create", fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )

                        NavigationBarItem(
                            selected = currentTab == NavTab.VIDEO_MAKER,
                            onClick = { viewModel.navigateTo(NavTab.VIDEO_MAKER) },
                            icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Video Maker and Reels") },
                            label = { Text("Video", fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )

                        NavigationBarItem(
                            selected = currentTab == NavTab.CALENDAR,
                            onClick = { viewModel.navigateTo(NavTab.CALENDAR) },
                            icon = { Icon(Icons.Default.DateRange, contentDescription = "Marketing Calendar") },
                            label = { Text("Calendar", fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )

                        NavigationBarItem(
                            selected = currentTab == NavTab.CAMPAIGNS,
                            onClick = { viewModel.navigateTo(NavTab.CAMPAIGNS) },
                            icon = { Icon(Icons.Default.Share, contentDescription = "WhatsApp Marketing Campaigns") },
                            label = { Text("WhatsApp", fontSize = 10.5.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = WhatsAppDark,
                                indicatorColor = Color(0xFFDCFCE7)
                            )
                        )

                        NavigationBarItem(
                            selected = currentTab == NavTab.LIBRARY,
                            onClick = { viewModel.navigateTo(NavTab.LIBRARY) },
                            icon = { Icon(Icons.Outlined.FolderCopy, contentDescription = "Content Library") },
                            label = { Text("Library", fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            },
            // Reposition floating AI assistant so it doesn't obstruct critical controls
            floatingActionButton = {
                // On mobile, show FAB when outside Assistant and Create; on desktop, Assistant is always in the sidebar
                if (!layoutInfo.isExpanded && currentTab != NavTab.ASSISTANT && currentTab != NavTab.CREATE) {
                    FloatingActionButton(
                        onClick = { viewModel.navigateTo(NavTab.ASSISTANT) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(bottom = 8.dp, end = 8.dp)
                            .testTag("main_fab_assistant")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Open AI Assistant")
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
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
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScaffoldPreview() {
    MyApplicationTheme {
        DashboardScreenPreview()
    }
}
