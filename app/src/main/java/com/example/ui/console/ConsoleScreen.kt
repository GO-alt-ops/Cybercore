package com.example.ui.console

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ConsoleScreen(viewModel: ConsoleViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        containerColor = BackgroundDark,
        bottomBar = {
            BottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    // Modern subtle radial background glows from the layout design spec
                    drawCircle(
                        color = GlowPurple.copy(alpha = 0.035f),
                        radius = size.width,
                        center = androidx.compose.ui.geometry.Offset(size.width, 0f)
                    )
                    drawCircle(
                        color = GlowPurple.copy(alpha = 0.03f),
                        radius = size.width * 0.7f,
                        center = androidx.compose.ui.geometry.Offset(0f, size.height)
                    )
                }
        ) {
            when (currentTab) {
                ConsoleTab.DASHBOARD -> DashboardTabContent(viewModel = viewModel)
                ConsoleTab.TOOLS -> ToolsTabContent(viewModel = viewModel)
                ConsoleTab.COPILOT -> CopilotTabContent(viewModel = viewModel)
            }
        }
    }
}

// ==========================================
// Bottom Navigation Component
// ==========================================
@Composable
fun BottomNavigationBar(
    currentTab: ConsoleTab,
    onTabSelected: (ConsoleTab) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceDark,
        tonalElevation = 8.dp,
        modifier = Modifier
            .border(width = 1.dp, color = BorderGrey, shape = RoundedCornerShape(toppart = 16.dp))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("bottom_nav_bar")
    ) {
        NavigationBarItem(
            selected = currentTab == ConsoleTab.DASHBOARD,
            onClick = { onTabSelected(ConsoleTab.DASHBOARD) },
            icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Console Dashboard") },
            label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPurple,
                unselectedIconColor = TextMuted,
                selectedTextColor = TextLight,
                unselectedTextColor = TextMuted,
                indicatorColor = BorderGrey
            ),
            modifier = Modifier.testTag("tab_dashboard")
        )
        NavigationBarItem(
            selected = currentTab == ConsoleTab.TOOLS,
            onClick = { onTabSelected(ConsoleTab.TOOLS) },
            icon = { Icon(Icons.Filled.Build, contentDescription = "Utilities & Diagnostic Info") },
            label = { Text("Tools", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPurple,
                unselectedIconColor = TextMuted,
                selectedTextColor = TextLight,
                unselectedTextColor = TextMuted,
                indicatorColor = BorderGrey
            ),
            modifier = Modifier.testTag("tab_tools")
        )
        NavigationBarItem(
            selected = currentTab == ConsoleTab.COPILOT,
            onClick = { onTabSelected(ConsoleTab.COPILOT) },
            icon = { Icon(Icons.Filled.Psychology, contentDescription = "AI Flashing Helper Copilot") },
            label = { Text("AI Copilot", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPurple,
                unselectedIconColor = TextMuted,
                selectedTextColor = TextLight,
                unselectedTextColor = TextMuted,
                indicatorColor = BorderGrey
            ),
            modifier = Modifier.testTag("tab_copilot")
        )
    }
}

private fun RoundedCornerShape(toppart: androidx.compose.ui.unit.Dp): RoundedCornerShape {
    return RoundedCornerShape(topStart = toppart, topEnd = toppart)
}

// ==========================================
// TAB 1: DASHBOARD
// ==========================================
@Composable
fun DashboardTabContent(viewModel: ConsoleViewModel) {
    val logs by viewModel.consoleLogs.collectAsStateWithLifecycle()
    val bootloader by viewModel.bootloaderState.collectAsStateWithLifecycle()
    val superuser by viewModel.superuserState.collectAsStateWithLifecycle()
    val systemVersion by viewModel.systemVersionState.collectAsStateWithLifecycle()
    val isUpgraded by viewModel.isUpgraded.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val logListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to the bottom of terminal console when new logs are persisted
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logListState.animateScrollToItem(logs.size - 1)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper section: Header with LED indicator
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_cybercore_logo_1779354123384),
                        contentDescription = "CyberCore Logo",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(width = 1.5.dp, brush = Brush.linearGradient(listOf(PrimaryPurple, GlowPurple)), shape = CircleShape)
                    )
                    Column {
                        Text(
                            text = "DEVICE CONSOLE",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "CyberCore Pro",
                            color = PrimaryPurple,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // LED Pulse animation
                val infiniteTransition = rememberInfiniteTransition(label = "PulseLED")
                val alphaPulse by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_opacity"
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color = ButtonNeutralBg, shape = CircleShape)
                        .border(width = 1.dp, color = BorderGrey, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = when {
                                    isBusy -> WarningYellow.copy(alpha = alphaPulse)
                                    isUpgraded -> SuccessGreen.copy(alpha = alphaPulse)
                                    else -> Color.Red.copy(alpha = alphaPulse)
                                },
                                shape = CircleShape
                            )
                    )
                }
            }
        }

        // System Architecture Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("architecture_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Floating tiny background light glow
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(100.dp)
                            .drawBehind {
                                drawCircle(
                                    color = PrimaryPurple.copy(alpha = 0.05f),
                                    radius = size.width / 2f
                                )
                            }
                    )

                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "System Architecture",
                                    color = TextLight,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = systemVersion,
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            }

                            // Outdated / Optimized Tag indicator
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isUpgraded) SuccessGreen.copy(alpha = 0.2f) else ErrorPink.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (isUpgraded) "Optimized" else "Outdated",
                                    color = if (isUpgraded) GreenLedPulse else ErrorPink,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom animated horizontal upgrade progress path
                        val animatedProgress by animateFloatAsState(
                            targetValue = if (isUpgraded) 1.0f else if (systemVersion == "Upgrading...") 0.5f else 0.33f,
                            animationSpec = tween(1200, easing = FastOutSlowInEasing),
                            label = "upgradeProgress"
                        )

                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(color = BorderGrey, shape = CircleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(animatedProgress)
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(PrimaryPurple, GlowPurple)
                                            ),
                                            shape = CircleShape
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (isUpgraded) "Primary firmware upgraded to Custom Android 14.1" else "Upgrade path ready to custom Android 14.1 (Cyber OS)",
                                color = TextGrey,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Two-column grid layout for Bootloader and Superuser parameters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bootloader Unlocking Panel
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bootloader_panel"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderGrey)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(color = ButtonNeutralBg, shape = RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (bootloader == "Unlocked") Icons.Filled.LockOpen else Icons.Filled.Lock,
                                contentDescription = "Lock icon",
                                tint = PrimaryPurple
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Bootloader",
                                color = TextLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Status: $bootloader",
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = { viewModel.unlockBootloader() },
                            enabled = bootloader == "Locked" && !isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonNeutralBg,
                                contentColor = PrimaryPurple,
                                disabledContainerColor = ButtonNeutralBg.copy(alpha = 0.5f),
                                disabledContentColor = TextMuted
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("unlock_bootloader_btn")
                        ) {
                            Text(
                                text = if (bootloader == "Unlocked") "Unlocked" else "Unlock",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // SuperUser Injection Panel
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("root_panel"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderGrey)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(color = ButtonNeutralBg, shape = RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FlashOn,
                                contentDescription = "Superuser flash",
                                tint = PrimaryPurple
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SuperUser",
                                color = TextLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Root: $superuser",
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = { viewModel.injectRoot() },
                            enabled = superuser == "Disabled" && !isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonNeutralBg,
                                contentColor = PrimaryPurple,
                                disabledContainerColor = ButtonNeutralBg.copy(alpha = 0.5f),
                                disabledContentColor = TextMuted
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("inject_root_btn")
                        ) {
                            Text(
                                text = if (superuser == "Enabled") "Rooted" else "Inject",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Live Console Terminal Output (persists history to Room Database)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .testTag("terminal_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SecondarySurfaceDark),
                border = BorderStroke(1.dp, BorderGrey.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(color = GreenLedPulse, shape = CircleShape))
                            Text(
                                text = "CyberCore Debug Terminal",
                                color = TextLight,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { viewModel.clearLogs() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteSweep,
                                    contentDescription = "Reset console logs",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Divider(color = BorderGrey.copy(alpha = 0.4f), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(8.dp)
                    ) {
                        LazyColumn(
                            state = logListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (logs.isEmpty()) {
                                item {
                                    Text(
                                        text = "No log inputs present.",
                                        color = TextMuted,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                items(logs) { log ->
                                    val logColor = when (log.tag) {
                                        "CMD" -> PrimaryPurple
                                        "SUCCESS" -> GreenLedPulse
                                        "ERROR" -> ErrorPink
                                        "WARN" -> WarningYellow
                                        else -> TextGrey
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "[${log.getFormattedTime()}]",
                                            color = TextMuted,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Light
                                        )
                                        Text(
                                            text = "${log.tag}:",
                                            color = logColor,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = log.message,
                                            color = TextLight,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Button: Run System Fix & Upgrade
        item {
            Button(
                onClick = { viewModel.runSystemFixAndUpgrade() },
                enabled = !isBusy && !isUpgraded,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPurple,
                    contentColor = OnPrimaryPurple,
                    disabledContainerColor = BorderGrey,
                    disabledContentColor = TextMuted
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("system_upgrade_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = "Upgrade cloud button",
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isUpgraded) "System Diagnostics Patched" else "Run System Fix & Upgrade",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 2: UTILITIES & STATISTICS
// ==========================================
@Composable
fun ToolsTabContent(viewModel: ConsoleViewModel) {
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_cybercore_logo_1779354123384),
                    contentDescription = "CyberCore Logo",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(width = 1.dp, color = BorderGrey, shape = CircleShape)
                )
                Column {
                    Text(
                        text = "UTILITIES & STATS",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "System Diagnostics",
                        color = PrimaryPurple,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Live stats gauge using simple customized DrawScope Canvas (RAM / Battery Simulation)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("diagnostics_graph_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Real-Time Resource Allocation",
                        color = TextLight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // RAM Usage Gauge
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(
                                        color = BorderGrey,
                                        startAngle = -225f,
                                        sweepAngle = 270f,
                                        useCenter = false,
                                        style = Stroke(width = 8.dp.toPx())
                                    )
                                    drawArc(
                                        color = PrimaryPurple,
                                        startAngle = -225f,
                                        sweepAngle = 180f, // Simulated 66% RAM
                                        useCenter = false,
                                        style = Stroke(width = 8.dp.toPx())
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("66%", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Memory", color = TextMuted, fontSize = 10.sp)
                                }
                            }
                        }

                        // CPU Core Activity Gauge
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(
                                        color = BorderGrey,
                                        startAngle = -225f,
                                        sweepAngle = 270f,
                                        useCenter = false,
                                        style = Stroke(width = 8.dp.toPx())
                                    )
                                    drawArc(
                                        color = WarningYellow,
                                        startAngle = -225f,
                                        sweepAngle = 110f, // Simulated 40% CPU
                                        useCenter = false,
                                        style = Stroke(width = 8.dp.toPx())
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("40%", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("CPU Load", color = TextMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    Divider(color = BorderGrey.copy(alpha = 0.4f), thickness = 1.dp)

                    // Diagnostic Read-Only Key Info
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeviceInfoRow(label = "Manufacturer", value = viewModel.deviceManufacturer)
                        DeviceInfoRow(label = "Model", value = viewModel.deviceModel)
                        DeviceInfoRow(label = "ABI Support", value = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a")
                        DeviceInfoRow(label = "Hardware Chipset", value = viewModel.deviceHardware)
                        DeviceInfoRow(label = "SELinux Policy", value = "Enforcing (Verified)")
                        DeviceInfoRow(label = "Core Governor", value = "interactive")
                    }
                }
            }
        }

        // Maintenance optimization triggers
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.runQuickOptimization() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SecondarySurfaceDark),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(color = SurfaceDark, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.Speed, contentDescription = "Speed booster", tint = PrimaryPurple)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kernel Governor Tune", color = TextLight, fontWeight = FontWeight.Bold)
                        Text("Trigger block diagnostics, cache cleanup and CPU scale booster", color = TextMuted, fontSize = 11.sp)
                    }

                    Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = "Go", tint = TextMuted)
                }
            }
        }

        // Highly detailed educational custom guides
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Filled.Warning, tint = WarningYellow, contentDescription = "Safety Alert")
                        Text(
                            text = "Developer Reference & Risks",
                            color = TextLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Flashing custom firmware, rooting, or unlocking device sub-loaders have severe security consequences:",
                        color = TextGrey,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    BulletItem(text = "Security boundary erasure: Root access disables standard app-sandbox boundaries, allowing a malicious app to inspect any storage partition or system key.")
                    BulletItem(text = "Hardware Key Loss: Modern systems blow a hardware cryptographic fuse (e.g., Knox or SafetyNet Keystore), permanently disabling certified high-identity banking tools or DRM playback.")
                    BulletItem(text = "Unlocking sequence requires manual physical verification (Developer Options -> Check USB Debugging + OEM Unlocking -> Connect PC with ABD utilities).")
                    BulletItem(text = "If any error happens during block manipulation, a 'hard brick' can happen. Ensure partition files match your firmware build fingerprint exactly.")
                }
            }
        }
    }
}

@Composable
fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 13.sp)
        Text(text = value, color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BulletItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", color = PrimaryPurple, fontSize = 14.sp)
        Text(text = text, color = TextGrey, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

// ==========================================
// TAB 3: AI COPILOT CONSULTANT
// ==========================================
@Composable
fun CopilotTabContent(viewModel: ConsoleViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val chatInput by viewModel.chatInput.collectAsStateWithLifecycle()
    val isLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val controller = LocalSoftwareKeyboardController.current

    // Keep chat snapped at bottom
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("copilot_layout")
    ) {
        // AI Title Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_cybercore_logo_1779354123384),
                contentDescription = "CyberCore Logo",
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(width = 1.dp, color = BorderGrey, shape = CircleShape)
            )
            Column {
                Text(
                    text = "NEURAL ASSISTANT",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "System Console Copilot",
                    color = PrimaryPurple,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Message List Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(width = 1.dp, color = BorderGrey, shape = RoundedCornerShape(24.dp))
                .padding(12.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatHistory) { msg ->
                    val isAi = msg.sender == "AI"
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isAi) Alignment.Start else Alignment.End
                    ) {
                        Text(
                            text = if (isAi) "CyberCore AI Console" else "Operator Console",
                            color = if (isAi) PrimaryPurple else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .background(
                                    color = if (isAi) SecondarySurfaceDark else ButtonNeutralBg,
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isAi) 0.dp else 16.dp,
                                        bottomEnd = if (isAi) 16.dp else 0.dp
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isAi) BorderGrey.copy(alpha = 0.5f) else BorderGrey,
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isAi) 0.dp else 16.dp,
                                        bottomEnd = if (isAi) 16.dp else 0.dp
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = TextLight,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryPurple
                            )
                            Text(
                                text = "AI console reasoning active...",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Query Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextField(
                value = chatInput,
                onValueChange = { viewModel.updateChatInput(it) },
                placeholder = { Text("Query device instructions...", color = TextMuted, fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextGrey,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    disabledContainerColor = SurfaceDark,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = PrimaryPurple
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BorderGrey, RoundedCornerShape(20.dp))
                    .testTag("copilot_input"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    viewModel.sendCopilotQuery()
                    controller?.hide()
                })
            )

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(color = PrimaryPurple, shape = CircleShape)
                    .clickable(enabled = chatInput.isNotBlank() && !isLoading) {
                        viewModel.sendCopilotQuery()
                        controller?.hide()
                    }
                    .testTag("copilot_send_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send AI query",
                    tint = OnPrimaryPurple,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
