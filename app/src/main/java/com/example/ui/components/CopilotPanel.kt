package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CopilotMessage
import com.example.model.MessageSender
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CopilotPanel(
    messages: List<CopilotMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to latest message on change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    val quickPrompts = listOf(
        "Sheets a Slack con condicional 🔀",
        "Bucle con Webhook 🔄",
        "Petición HTTP con Docs 🌐",
        "Ejecutar automatización ▶",
        "Limpiar constructor 🗑"
    )

    val isDark = LocalIsDarkTheme.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(ApplePurple, AppleBlue))),
                contentAlignment = Alignment.Center
            ) {
                FlowIcon("sparkles", tint = Color.White, size = 20.dp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Copilot AI Builder",
                    color = if (isDark) Color.White else Color(0xFF111827),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Claude Code / Gemini Integrado",
                    color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF4B5563),
                    fontSize = 11.sp
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                val alignment = if (msg.sender == MessageSender.USER) Alignment.End else Alignment.Start
                val bubbleBg = if (msg.sender == MessageSender.USER) {
                    Brush.linearGradient(listOf(AppleBlue.copy(alpha = 0.25f), AppleIndigo.copy(alpha = 0.25f)))
                } else {
                    if (isDark) Brush.linearGradient(listOf(Color(0x18FFFFFF), Color(0x0AFFFFFF)))
                    else Brush.linearGradient(listOf(Color(0x12000000), Color(0x04000000)))
                }
                val bubbleBorderColor = if (msg.sender == MessageSender.USER) AppleBlue.copy(alpha = 0.5f) else (if (isDark) GlassBorder else Color(0x22000000))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = alignment
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (msg.sender == MessageSender.USER) 16.dp else 4.dp,
                                    bottomEnd = if (msg.sender == MessageSender.USER) 4.dp else 16.dp
                                )
                            )
                            .background(bubbleBg)
                            .border(
                                1.dp,
                                bubbleBorderColor,
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (msg.sender == MessageSender.USER) 16.dp else 4.dp,
                                    bottomEnd = if (msg.sender == MessageSender.USER) 4.dp else 16.dp
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (isDark) Color.White else Color(0xFF1F2937),
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = msg.timestamp,
                        color = if (isDark) Color.White.copy(alpha = 0.35f) else Color(0xFF6B7280),
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) Color(0x0DFFFFFF) else Color(0x0A000000))
                                .border(1.dp, if (isDark) GlassBorderFaint else Color(0x1A000000), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 1.5.dp,
                                    color = ApplePurple
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Copilot está diseñando...",
                                    color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF4B5563),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
 
        // Quick Suggestion Chips
        Text(
            text = "IDEAS DE AUTOMATIZACIÓN RÁPIDAS",
            color = if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Display 2 random/relevant suggestions at a time horizontally scrollable or just select from the list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.Start
            ) {
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (prompt in quickPrompts.take(3)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0x0CFFFFFF) else Color(0x0C000000))
                                    .border(1.dp, if (isDark) GlassBorderFaint else Color(0x1F000000), RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSendMessage(prompt.replace(Regex("[^a-zA-Z0-9 áéíóúÁÉÍÓÚñÑ]"), "").trim())
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = prompt,
                                    color = if (isDark) Color.White.copy(alpha = 0.75f) else Color(0xFF374151),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Message Input (Editorial Aesthetic iOS style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant Left Star Avatar profile (indigo-500 to purple-600)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF9333EA))))
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (isDark) SlateDark else Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✨", fontSize = 13.sp)
                }
            }
            
            Spacer(modifier = Modifier.width(10.dp))

            GlassTextField(
                value = textValue,
                onValueChange = { textValue = it },
                placeholder = "Pídele a Copilot diseñar un flujo...",
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (textValue.isNotBlank()) {
                            Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF4F46E5)))
                        } else {
                            Brush.linearGradient(listOf(Color(0x10FFFFFF), Color(0x05FFFFFF)))
                        }
                    )
                    .border(
                        1.dp,
                        if (textValue.isNotBlank()) Color(0x40FFFFFF) else GlassBorderFaint,
                        CircleShape
                    )
                    .clickable(enabled = textValue.isNotBlank()) {
                        onSendMessage(textValue)
                        textValue = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                FlowIcon("send", tint = if (textValue.isNotBlank()) Color.White else Color.White.copy(alpha = 0.35f), size = 15.dp)
            }
        }
    }
}
