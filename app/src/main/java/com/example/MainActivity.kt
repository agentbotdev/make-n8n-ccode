package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.AgentFlowViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AgentFlowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalIsDarkTheme provides isDarkTheme) {
                MyApplicationTheme(darkTheme = isDarkTheme) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isDarkTheme) SlateDark else Color(0xFFF3F4F6))
                                .padding(innerPadding)
                        ) {
                            // Background Mesh Gradient
                            MeshGradientBg()

                            // Main Content Layout
                            AgentFlowScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgentFlowScreen(viewModel: AgentFlowViewModel) {
    val nodes by viewModel.nodes.collectAsStateWithLifecycle()
    val connections by viewModel.connections.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val copilotMessages by viewModel.copilotMessages.collectAsStateWithLifecycle()
    val isExecuting by viewModel.isExecuting.collectAsStateWithLifecycle()
    val selectedNode by viewModel.selectedNode.collectAsStateWithLifecycle()
    val currentActiveNodeId by viewModel.currentActiveNodeId.collectAsStateWithLifecycle()
    val isCopilotLoading by viewModel.isCopilotLoading.collectAsStateWithLifecycle()

    var showLogsPanel by remember { mutableStateOf(true) }
    var showCopilotPanel by remember { mutableStateOf(true) }
    var showAddNodeDropdown by remember { mutableStateOf(false) }

    // Screen dimensions check for responsive layout
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 780

    val isDark = LocalIsDarkTheme.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // --- 1. Top Bar Navigation (Editorial Aesthetic iOS Style) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0x05FFFFFF) else Color(0x06000000))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Glowy gradient indigo-500 to blue-400 logo box
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF60A5FA)))),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner elegant white hollow circle
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(7.dp))
                    )
                }
                Column {
                    Text(
                        text = "AgentFlow",
                        color = if (isDark) Color.White else Color(0xFF111827),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.6).sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(AppleGreen)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "MCP ENGINE ACTIVO",
                            color = AppleGreen.copy(alpha = 0.9f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Quick Actions & Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add Node Button with Dropdown anchor
                Box {
                    GlassButton(
                        onClick = { showAddNodeDropdown = !showAddNodeDropdown },
                        containerColor = AppleBlue.copy(alpha = 0.15f),
                        borderColor = AppleBlue.copy(alpha = 0.4f)
                    ) {
                        FlowIcon("add", tint = if (isDark) Color.White else Color(0xFF1D4ED8), size = 14.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Añadir Nodo", color = if (isDark) Color.White else Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(
                        expanded = showAddNodeDropdown,
                        onDismissRequest = { showAddNodeDropdown = false },
                        modifier = Modifier
                            .background(if (isDark) GlassBgDark else Color.White)
                            .border(1.dp, if (isDark) GlassBorderFaint else Color(0x22000000), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "CONECTORES DISPONIBLES",
                            color = if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.5f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                        
                        val services = ServiceType.values()
                        services.forEach { service ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(service.color.copy(alpha = 0.15f)),
                                             contentAlignment = Alignment.Center
                                        ) {
                                            FlowIcon(service.iconRes, tint = service.color, size = 14.dp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(service.displayName, color = if (isDark) Color.White else Color(0xFF111827), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            val category = when(service) {
                                                ServiceType.GOOGLE_SHEETS, ServiceType.SLACK, ServiceType.TRELLO -> "Integración API"
                                                ServiceType.HTTP -> "Protocolo Web"
                                                ServiceType.CONDITIONAL, ServiceType.LOOP -> "Control Lógico"
                                                ServiceType.WEBHOOK -> "Disparador Web"
                                            }
                                            Text(category, color = if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f), fontSize = 9.sp)
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.addNode(service, 120f, 150f)
                                    showAddNodeDropdown = false
                                }
                            )
                        }
                    }
                }

                // Execute Trigger Button
                GlassButton(
                    onClick = { viewModel.startExecution() },
                    containerColor = if (isExecuting) AppleOrange.copy(alpha = 0.18f) else AppleGreen.copy(alpha = 0.15f),
                    borderColor = if (isExecuting) AppleOrange.copy(alpha = 0.5f) else AppleGreen.copy(alpha = 0.5f)
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = AppleOrange
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ejecutando...", color = AppleOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        FlowIcon("play_arrow", tint = AppleGreen, size = 14.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ejecutar", color = if (isDark) Color.White else Color(0xFF15803D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Theme Toggle Button
                GlassButton(
                    onClick = { viewModel.toggleTheme() },
                    containerColor = if (isDark) Color(0x0DFFFFFF) else Color(0x1F000000),
                    borderColor = if (isDark) GlassBorderFaint else Color(0x22000000)
                ) {
                    FlowIcon(if (isDark) "wb_sunny" else "nights_stay", tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF4F46E5), size = 14.dp)
                    if (isWideScreen) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDark) "Claro" else "Oscuro",
                            color = if (isDark) Color.White else Color(0xFF111827),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Show/Hide Panels toggles (Only on wide screen or for logs)
                GlassButton(
                    onClick = { showLogsPanel = !showLogsPanel },
                    containerColor = if (showLogsPanel) (if (isDark) Color(0x35FFFFFF) else Color(0x22000000)) else (if (isDark) Color(0x0DFFFFFF) else Color(0x0A000000))
                ) {
                    FlowIcon("history", tint = if (showLogsPanel) AppleOrange else (if (isDark) Color.White else Color(0xFF4B5563)), size = 14.dp)
                    if (isWideScreen) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Logs", color = if (isDark) Color.White else Color(0xFF111827), fontSize = 11.sp)
                    }
                }

                if (!isWideScreen) {
                    // Mobile Toggle for Copilot AI Chat
                    GlassButton(
                        onClick = { showCopilotPanel = !showCopilotPanel },
                        containerColor = if (showCopilotPanel) ApplePurple.copy(alpha = 0.15f) else (if (isDark) Color(0x0DFFFFFF) else Color(0x0A000000)),
                        borderColor = if (showCopilotPanel) ApplePurple.copy(alpha = 0.4f) else (if (isDark) GlassBorder else Color(0x22000000))
                    ) {
                        FlowIcon("sparkles", tint = ApplePurple, size = 14.dp)
                    }
                }
            }
        }

        // --- 2. Main Workstation Grid ---
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // LEFT/CENTER SECTION: Flow Canvas & Optional Logs at Bottom
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Visual Canvas
                    FlowCanvas(
                        nodes = nodes,
                        connections = connections,
                        isExecuting = isExecuting,
                        currentActiveNodeId = currentActiveNodeId,
                        selectedNode = selectedNode,
                        onNodeSelected = { viewModel.selectNode(it) },
                        onNodeMoved = { id, dx, dy -> viewModel.updateNodePosition(id, dx, dy) },
                        onDeleteConnection = { viewModel.deleteConnection(it) }
                    )

                    // Node Parameters Editor (Left Floating Sliding Drawer)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedNode != null,
                        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(12.dp)
                    ) {
                        selectedNode?.let { node ->
                            NodeEditorDialog(
                                node = node,
                                allNodes = nodes,
                                onClose = { viewModel.selectNode(null) },
                                onDelete = { viewModel.deleteNode(it) },
                                onSaveParameters = { id, params -> viewModel.updateNodeParameters(id, params) },
                                onConnectNode = { from, to, port -> viewModel.addConnection(from, to, port) }
                            )
                        }
                    }
                }

                // Execution Logs slide-up panel at bottom
                AnimatedVisibility(
                    visible = showLogsPanel,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    ExecutionLogsPanel(
                        logs = logs,
                        onClose = { showLogsPanel = false },
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // RIGHT SECTION: AI Copilot Assistant Chat panel
            if (isWideScreen || showCopilotPanel) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(if (isWideScreen) 340.dp else 290.dp)
                        .background(Color(0x06000000))
                        .border(1.dp, GlassBorderFaint, RoundedCornerShape(0.dp))
                ) {
                    CopilotPanel(
                        messages = copilotMessages,
                        isLoading = isCopilotLoading,
                        onSendMessage = { viewModel.sendCopilotMessage(it) }
                    )
                    
                    // Close button if on mobile to swipe away copilot
                    if (!isWideScreen) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x1AFFFFFF))
                                .clickable { showCopilotPanel = false },
                            contentAlignment = Alignment.Center
                        ) {
                            FlowIcon("close", tint = Color.White, size = 12.dp)
                        }
                    }
                }
            }
        }
    }
}
