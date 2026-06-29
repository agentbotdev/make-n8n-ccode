package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

val LocalIsDarkTheme = staticCompositionLocalOf { true }

@Composable
fun MeshGradientBg(modifier: Modifier = Modifier) {
    val isDark = LocalIsDarkTheme.current
    // Elegant animating glowing mesh background exactly matching the Editorial Aesthetic
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val animOffset1 by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )
    val animOffset2 by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = -100f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )

    val baseBg = if (isDark) SlateDark else Color(0xFFF3F4F6)
    val overlayColor = if (isDark) Color(0x2205070A) else Color(0x04FFFFFF)
    val glow1 = if (isDark) Color(0x334F46E5) else Color(0x1F4F46E5)
    val glow2 = if (isDark) Color(0x333B82F6) else Color(0x1F3B82F6)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
            .drawBehind {
                // Top-Left Indigo Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow1, Color.Transparent),
                        center = Offset(animOffset1, animOffset2 - 100f),
                        radius = size.width * 0.75f
                    )
                )
                // Bottom-Right Blue Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow2, Color.Transparent),
                        center = Offset(size.width + animOffset2, size.height + animOffset1),
                        radius = size.width * 0.75f
                    )
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor)
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color? = null,
    selected: Boolean = false,
    selectedBorderColor: Color = AppleBlue.copy(alpha = 0.8f),
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val defaultBorderColor = if (isDark) GlassBorderFaint else Color(0x20000000)
    val finalBorder = borderColor ?: defaultBorderColor
    val currentBorderColor = if (selected) selectedBorderColor else finalBorder
    val currentBorderWidth = if (selected) 1.5.dp else borderWidth
    val cardBg = if (isDark) GlassBgDark else Color(0xB8FFFFFF)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = cardBg,
        border = BorderStroke(
            currentBorderWidth,
            Brush.linearGradient(
                listOf(
                    currentBorderColor,
                    currentBorderColor.copy(alpha = 0.12f)
                )
            )
        ),
        tonalElevation = 2.dp,
        shadowElevation = if (selected) 8.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .padding(0.dp)
                .background(Brush.verticalGradient(
                    if (isDark) listOf(Color(0x05FFFFFF), Color.Transparent)
                    else listOf(Color(0x08000000), Color.Transparent)
                )),
            content = content
        )
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    containerColor: Color? = null,
    borderColor: Color? = null,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val finalContainer = containerColor ?: (if (isDark) Color(0x18FFFFFF) else Color(0xD2FFFFFF))
    val finalBorder = borderColor ?: (if (isDark) GlassBorder else Color(0x2A000000))
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(finalContainer)
            .border(
                1.dp,
                Brush.linearGradient(listOf(finalBorder, finalBorder.copy(alpha = 0.15f))),
                RoundedCornerShape(cornerRadius)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 5,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val isDark = LocalIsDarkTheme.current
    var isFocused by remember { mutableStateOf(false) }

    val activeBorderColor = if (isFocused) AppleBlue.copy(alpha = 0.8f) else (if (isDark) GlassBorder else Color(0x22000000))
    val bgTrans = if (isFocused) {
        if (isDark) Color(0x1EFFFFFF) else Color(0xFAFFFFFF)
    } else {
        if (isDark) Color(0x0DFFFFFF) else Color(0xB8FFFFFF)
    }
    val textColor = if (isDark) Color.White else Color(0xFF111827)
    val placeholderColor = if (isDark) Color.White.copy(alpha = 0.35f) else Color(0xFF9CA3AF)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgTrans)
            .border(
                1.dp,
                Brush.linearGradient(listOf(activeBorderColor, activeBorderColor.copy(alpha = 0.15f))),
                RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                color = placeholderColor,
                fontSize = 14.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(color = textColor, fontSize = 14.sp),
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            cursorBrush = SolidColor(AppleBlue)
        )
    }
}

// Simple Custom Icon Component that maps visual text to standard unicode or nice letters
// as we don't import full Material Icons Extended library to save package size and compile times.
@Composable
fun FlowIcon(
    iconName: String,
    tint: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    // Beautiful, ultra-compact visual representations of service icons using stylized letters and emoji shapes
    val iconChar = when (iconName) {
        "table_chart" -> "📊"
        "chat" -> "💬"
        "dashboard" -> "📌"
        "http" -> "🌐"
        "call_split" -> "🔀"
        "loop" -> "🔄"
        "webhook" -> "⚡"
        "send" -> "➔"
        "add" -> "＋"
        "delete" -> "🗑"
        "play_arrow" -> "▶"
        "stop" -> "■"
        "settings" -> "⚙"
        "search" -> "🔍"
        "info" -> "ℹ"
        "close" -> "✕"
        "sparkles" -> "✨"
        "history" -> "⏱"
        "arrow_back" -> "←"
        "wb_sunny" -> "☀️"
        "nights_stay" -> "🌙"
        else -> "•"
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iconChar,
            color = tint,
            fontSize = (size.value * 0.7f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}
