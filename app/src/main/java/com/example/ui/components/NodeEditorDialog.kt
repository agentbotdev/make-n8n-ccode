package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun NodeEditorDialog(
    node: FlowNode,
    allNodes: List<FlowNode>,
    onClose: () -> Unit,
    onDelete: (String) -> Unit,
    onSaveParameters: (String, List<NodeParameter>) -> Unit,
    onConnectNode: (String, String, String) -> Unit, // fromId, toId, portName
    modifier: Modifier = Modifier
) {
    var editedParams by remember(node.id) { mutableStateOf(node.parameters) }
    var showHttpDocs by remember { mutableStateOf(false) }
    var showConnectMenu by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val otherNodes = allNodes.filterNot { it.id == node.id }

    GlassCard(
        modifier = modifier
            .fillMaxHeight()
            .width(310.dp)
            .border(
                1.dp,
                Brush.linearGradient(listOf(node.service.color.copy(alpha = 0.4f), Color.Transparent)),
                RoundedCornerShape(20.dp)
            ),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(node.service.color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        FlowIcon(iconName = node.service.iconRes, tint = node.service.color, size = 18.dp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Configurar",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x1AFFFFFF))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    FlowIcon("close", tint = Color.White, size = 12.dp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body (Scrollable form)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Info block
                Text(
                    text = node.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${node.service.displayName} • ${node.actionName}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic Form Parameters
                if (editedParams.isEmpty()) {
                    Text(
                        text = "Este nodo no requiere parámetros de configuración inicial.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                } else {
                    for (index in editedParams.indices) {
                        val param = editedParams[index]
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = param.label,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (param.description.isNotEmpty()) {
                                    Text(
                                        text = "?",
                                        color = AppleBlue,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AppleBlue.copy(alpha = 0.15f))
                                            .wrapContentSize(Alignment.Center)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            when (param.type) {
                                ParamType.TEXT -> {
                                    GlassTextField(
                                        value = param.value,
                                        onValueChange = { newVal ->
                                            editedParams = editedParams.toMutableList().apply {
                                                this[index] = param.copy(value = newVal)
                                            }
                                            onSaveParameters(node.id, editedParams)
                                        },
                                        placeholder = "Introduce valor..."
                                    )
                                }
                                ParamType.TEXTAREA -> {
                                    GlassTextField(
                                        value = param.value,
                                        onValueChange = { newVal ->
                                            editedParams = editedParams.toMutableList().apply {
                                                this[index] = param.copy(value = newVal)
                                            }
                                            onSaveParameters(node.id, editedParams)
                                        },
                                        placeholder = "Escribe plantilla o JSON...",
                                        singleLine = false
                                    )
                                }
                                ParamType.SELECT -> {
                                    // Custom visual dropdown select
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0x0DFFFFFF))
                                            .border(1.dp, GlassBorderFaint, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = param.value.ifEmpty { "Selecciona..." }, color = Color.White, fontSize = 13.sp)
                                        Text(text = "▼", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                                    }
                                }
                            }

                            if (param.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = param.description,
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 9.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }

                // Interactive HTTP Request Docs Section
                if (node.service == ServiceType.HTTP) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppleOrange.copy(alpha = 0.12f))
                            .border(1.dp, AppleOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { showHttpDocs = !showHttpDocs }
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FlowIcon("info", tint = AppleOrange, size = 16.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Docs HTTP & Payload Schema",
                                    color = AppleOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(text = if (showHttpDocs) "▲" else "▼", color = AppleOrange, fontSize = 10.sp)
                        }
                    }

                    AnimatedVisibility(
                        visible = showHttpDocs,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x2B000000))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "MÉTODO POST / GET / PUT",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Envia y recibe estructuras JSON de forma asíncrona. Soporta variables inyectadas usando sintaxis de llave doble como {{data.Nombre}}.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "EJEMPLO PAYLOAD:",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = """{
  "client_id": "af-2026",
  "status": "active",
  "meta": {
     "mcp_enabled": true
  }
}""",
                                color = AppleGreen,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }

                // Connect Node Section
                if (otherNodes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppleBlue.copy(alpha = 0.12f))
                            .border(1.dp, AppleBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { showConnectMenu = !showConnectMenu }
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FlowIcon("add", tint = AppleBlue, size = 16.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Conectar Salida a...",
                                    color = AppleBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(text = if (showConnectMenu) "▲" else "▼", color = AppleBlue, fontSize = 10.sp)
                        }
                    }

                    AnimatedVisibility(
                        visible = showConnectMenu,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x2B000000))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // If conditional or loop, let user choose which port to connect
                            val ports = if (node.service == ServiceType.CONDITIONAL) {
                                listOf("true", "false")
                            } else if (node.service == ServiceType.LOOP) {
                                listOf("item", "finished")
                            } else {
                                listOf("output")
                            }

                            for (port in ports) {
                                if (ports.size > 1) {
                                    Text(
                                        text = "Puerto: ${port.uppercase(Locale.ROOT)}",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                for (target in otherNodes) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0x12FFFFFF))
                                            .clickable {
                                                onConnectNode(node.id, target.id, port)
                                                showConnectMenu = false
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(target.service.color.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            FlowIcon(target.service.iconRes, tint = target.service.color, size = 12.dp)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = target.title,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "+ Enlazar",
                                            color = AppleBlue,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Node Payload Inspector
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "INSPECTOR DE DATOS (JSON)",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x35000000))
                        .border(1.dp, GlassBorderFaint, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "ENTRADA PAYLOAD:",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = node.inputPayload,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "SALIDA RESULTANTE:",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = node.outputPayload,
                        color = AppleTeal,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer actions (Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassButton(
                    onClick = { onDelete(node.id) },
                    modifier = Modifier.weight(1f),
                    containerColor = AppleRed.copy(alpha = 0.15f),
                    borderColor = AppleRed.copy(alpha = 0.4f)
                ) {
                    FlowIcon("delete", tint = AppleRed, size = 16.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Eliminar", color = AppleRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                GlassButton(
                    onClick = { onClose() },
                    modifier = Modifier.weight(1f),
                    containerColor = AppleBlue.copy(alpha = 0.18f),
                    borderColor = AppleBlue.copy(alpha = 0.5f)
                ) {
                    Text(text = "Cerrar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
