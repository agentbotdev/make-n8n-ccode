package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun FlowCanvas(
    nodes: List<FlowNode>,
    connections: List<FlowConnection>,
    isExecuting: Boolean,
    currentActiveNodeId: String?,
    selectedNode: FlowNode?,
    onNodeSelected: (FlowNode) -> Unit,
    onNodeMoved: (String, Float, Float) -> Unit,
    onDeleteConnection: (FlowConnection) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Node dimensions in float pixels
    val nodeWidthPx = with(density) { 190.dp.toPx() }
    val nodeHeightPx = with(density) { 76.dp.toPx() }

    // Pulsing animations for connections during execution
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x05FFFFFF))
    ) {
        // 1. Connection lines drawn on Canvas (back layer)
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (conn in connections) {
                val fromNode = nodes.find { it.id == conn.fromNodeId }
                val toNode = nodes.find { it.id == conn.toNodeId }

                if (fromNode != null && toNode != null) {
                    // Convert dp coordinates to pixels
                    val fromNodeX = with(density) { fromNode.x.dp.toPx() }
                    val fromNodeY = with(density) { fromNode.y.dp.toPx() }
                    val toNodeX = with(density) { toNode.x.dp.toPx() }
                    val toNodeY = with(density) { toNode.y.dp.toPx() }

                    // Precise port locations based on service and port names
                    val startX = fromNodeX + nodeWidthPx
                    val startY = when (conn.fromPortName) {
                        "true" -> fromNodeY + (nodeHeightPx * 0.3f)
                        "false" -> fromNodeY + (nodeHeightPx * 0.7f)
                        "item" -> fromNodeY + (nodeHeightPx * 0.3f)
                        "finished" -> fromNodeY + (nodeHeightPx * 0.7f)
                        else -> fromNodeY + (nodeHeightPx * 0.5f)
                    }

                    val endX = toNodeX
                    val endY = toNodeY + (nodeHeightPx * 0.5f)

                    // Draw organic Bezier curve
                    val controlX1 = startX + 120f
                    val controlY1 = startY
                    val controlX2 = endX - 120f
                    val controlY2 = endY

                    val bezierPath = Path().apply {
                        moveTo(startX, startY)
                        cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)
                    }

                    // Connection color based on source port or execution status
                    val isPathRunning = isExecuting && 
                        (currentActiveNodeId == fromNode.id || (fromNode.status == NodeExecutionStatus.SUCCESS && toNode.status == NodeExecutionStatus.RUNNING))
                    
                    val pathColor = when {
                        isPathRunning -> AppleBlue
                        conn.fromPortName == "true" -> AppleGreen.copy(alpha = 0.7f)
                        conn.fromPortName == "false" -> AppleRed.copy(alpha = 0.7f)
                        conn.fromPortName == "item" -> ApplePurple.copy(alpha = 0.7f)
                        conn.fromPortName == "finished" -> AppleOrange.copy(alpha = 0.7f)
                        else -> Color.White.copy(alpha = 0.25f)
                    }

                    val strokeWidth = if (isPathRunning) 3.5.dp.toPx() else 2.dp.toPx()

                    // Draw main path
                    drawPath(
                        path = bezierPath,
                        color = pathColor,
                        style = Stroke(width = strokeWidth)
                    )

                    // Draw animating signal pulse during execution
                    if (isPathRunning) {
                        // Math to approximate point on bezier curve (Cubic Bezier formulation)
                        val t = pulseProgress
                        val u = 1 - t
                        val px = u*u*u*startX + 3*u*u*t*controlX1 + 3*u*t*t*controlX2 + t*t*t*endX
                        val py = u*u*u*startY + 3*u*u*t*controlY1 + 3*u*t*t*controlY2 + t*t*t*endY

                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = AppleBlue.copy(alpha = 0.4f),
                            radius = 12.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }

        // 2. Interactive Nodes layer
        for (node in nodes) {
            val isCurrentActive = node.id == currentActiveNodeId
            
            // Animating border pulse for running nodes
            val borderPulse = if (isCurrentActive) {
                val pTransition = rememberInfiniteTransition(label = "borderPulse")
                pTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "border"
                ).value
            } else 1f

            Box(
                modifier = Modifier
                    .offset(x = node.x.dp, y = node.y.dp)
                    .pointerInput(node.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onNodeMoved(node.id, dragAmount.x / density.density, dragAmount.y / density.density)
                        }
                    }
            ) {
                // Stadium/rounded rectangular Node
                GlassCard(
                    modifier = Modifier
                        .size(width = 190.dp, height = 76.dp)
                        .clickable { onNodeSelected(node) },
                    cornerRadius = 22.dp,
                    selected = node.selected || isCurrentActive,
                    selectedBorderColor = if (isCurrentActive) {
                        AppleBlue.copy(alpha = borderPulse)
                    } else {
                        node.service.color.copy(alpha = 0.8f)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Colored Service Icon Bubble
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(node.service.color.copy(alpha = 0.18f))
                                .border(1.dp, node.service.color.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            FlowIcon(iconName = node.service.iconRes, tint = node.service.color, size = 26.dp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Title & Trigger/Action Info
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = node.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = node.actionName,
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }

                        // Execution status dot indicator
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    when (node.status) {
                                        NodeExecutionStatus.IDLE -> Color.White.copy(alpha = 0.15f)
                                        NodeExecutionStatus.RUNNING -> AppleBlue
                                        NodeExecutionStatus.SUCCESS -> AppleGreen
                                        NodeExecutionStatus.FAILED -> AppleRed
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (node.status == NodeExecutionStatus.RUNNING) Color.White else Color.Transparent,
                                    CircleShape
                                )
                        )
                    }
                }

                // Input Port Indicator (Left)
                if (node.type != NodeType.TRIGGER) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.CenterStart)
                            .offset(x = (-5).dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, node.service.color, CircleShape)
                    )
                }

                // Output Port Indicators (Right)
                if (node.service == ServiceType.CONDITIONAL) {
                    // True Output (Top Right)
                    PortLabelDot(
                        text = "SÍ",
                        color = AppleGreen,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 5.dp, y = 14.dp)
                    )
                    // False Output (Bottom Right)
                    PortLabelDot(
                        text = "NO",
                        color = AppleRed,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 5.dp, y = (-14).dp)
                    )
                } else if (node.service == ServiceType.LOOP) {
                    // Loop Item Output
                    PortLabelDot(
                        text = "ITEM",
                        color = ApplePurple,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 5.dp, y = 14.dp)
                    )
                    // Finished Output
                    PortLabelDot(
                        text = "FIN",
                        color = AppleOrange,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 5.dp, y = (-14).dp)
                    )
                } else {
                    // Standard single output (right-center)
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.CenterEnd)
                            .offset(x = 5.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, node.service.color, CircleShape)
                    )
                }
            }
        }

        // 3. Quick Connection overlay instructions
        if (nodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier.widthIn(max = 340.dp),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FlowIcon("sparkles", tint = ApplePurple, size = 38.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Constructor de Flujos Vacío",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Arrastra un conector, pídele a tu Copilot crear un flujo de Sheets a Slack, o selecciona una plantilla para ver la magia glassmorphic de AgentFlow en acción.",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PortLabelDot(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.5.dp, color, CircleShape)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = text,
            color = color,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(Color(0xD9050508), RoundedCornerShape(3.dp))
                .padding(horizontal = 3.dp, vertical = 1.dp)
        )
    }
}
