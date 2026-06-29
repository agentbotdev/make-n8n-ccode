package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExecutionLog
import com.example.model.LogLevel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ExecutionLogsPanel(
    logs: List<ExecutionLog>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedLogEntry by remember { mutableStateOf<ExecutionLog?>(null) }

    // Auto scroll down as logs stream in
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(logs.size - 1)
            }
        }
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .border(1.dp, GlassBorderFaint, RoundedCornerShape(20.dp)),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Logs List (Left portion)
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
            ) {
                // Header (Editorial Aesthetic iOS style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pulsating Green Emerald Indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF34D399))
                        )
                        Text(
                            text = "EXECUTION LIVE",
                            color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0x10000000))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${logs.size} EVENTS",
                                color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0x15FFFFFF) else Color(0x10000000))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        FlowIcon("close", tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f), size = 10.dp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay ejecuciones activas. Pulsa el botón 'Ejecutar' o pídele a tu Copilot iniciar un test.",
                            color = if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(logs) { log ->
                            val color = when (log.level) {
                                LogLevel.INFO -> AppleTeal
                                LogLevel.SUCCESS -> AppleGreen
                                LogLevel.WARNING -> AppleOrange
                                LogLevel.ERROR -> AppleRed
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedLogEntry?.id == log.id) (if (isDark) Color(0x1AFFFFFF) else Color(0x15000000)) else Color.Transparent)
                                    .clickable { selectedLogEntry = log }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "[${log.timestamp}]",
                                    color = if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = log.message,
                                    color = if (selectedLogEntry?.id == log.id) (if (isDark) Color.White else Color(0xFF111827)) else (if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)),
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedLogEntry?.id == log.id) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }

            // Payload Detail Inspector (Right portion)
            AnimatedVisibility(
                visible = selectedLogEntry != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .padding(start = 12.dp)
            ) {
                selectedLogEntry?.let { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0x35000000) else Color(0x0C000000))
                            .border(1.dp, if (isDark) GlassBorderFaint else Color(0x1A000000), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.nodeName ?: "Evento de Sistema",
                                color = if (isDark) Color.White else Color(0xFF111827),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isDark) Color(0x26FFFFFF) else Color(0x15000000))
                                    .clickable { selectedLogEntry = null },
                                contentAlignment = Alignment.Center
                            ) {
                                FlowIcon("close", tint = if (isDark) Color.White else Color.Black, size = 8.dp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            if (entry.inputData != null) {
                                item {
                                    Text(
                                        text = "INPUT PAYLOAD:",
                                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = entry.inputData,
                                        color = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f),
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 11.sp,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }

                            if (entry.outputData != null) {
                                item {
                                    Text(
                                        text = "OUTPUT PAYLOAD:",
                                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = entry.outputData,
                                        color = AppleTeal,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 11.sp
                                    )
                                }
                            }

                            if (entry.inputData == null && entry.outputData == null) {
                                item {
                                    Text(
                                        text = entry.message,
                                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
