package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class NodeType {
    TRIGGER, ACTION, LOGIC, LOOP
}

enum class ServiceType(val displayName: String, val iconRes: String, val color: Color) {
    GOOGLE_SHEETS("Google Sheets", "table_chart", AppleGreen),
    SLACK("Slack", "chat", AppleIndigo),
    TRELLO("Trello", "dashboard", AppleBlue),
    HTTP("HTTP Request", "http", AppleOrange),
    CONDITIONAL("Condicional If/Else", "call_split", ApplePink),
    LOOP("Bucle For Each", "loop", ApplePurple),
    WEBHOOK("Webhook Trigger", "webhook", AppleTeal)
}

data class NodeParameter(
    val key: String,
    val label: String,
    val type: ParamType,
    val value: String = "",
    val options: List<String> = emptyList(),
    val description: String = ""
)

enum class ParamType {
    TEXT, SELECT, TEXTAREA
}

data class FlowNode(
    val id: String,
    val title: String,
    val type: NodeType,
    val service: ServiceType,
    val actionName: String,
    val x: Float,
    val y: Float,
    val parameters: List<NodeParameter>,
    val status: NodeExecutionStatus = NodeExecutionStatus.IDLE,
    val inputPayload: String = "{}",
    val outputPayload: String = "{}",
    val selected: Boolean = false
)

enum class NodeExecutionStatus {
    IDLE, RUNNING, SUCCESS, FAILED
}

data class FlowConnection(
    val fromNodeId: String,
    val toNodeId: String,
    val fromPortName: String = "output", // default, or "true", "false", "item", "done"
    val toPortName: String = "input"
)

enum class LogLevel {
    INFO, SUCCESS, ERROR, WARNING
}

data class ExecutionLog(
    val id: String,
    val timestamp: String,
    val nodeId: String?,
    val nodeName: String?,
    val message: String,
    val level: LogLevel,
    val inputData: String? = null,
    val outputData: String? = null
)

data class CopilotMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: String,
    val isSuggested: Boolean = false,
    val suggestedFlowTemplate: String? = null // if the message includes a template suggestion
)

enum class MessageSender {
    USER, SYSTEM, COPILOT
}

data class FlowTemplate(
    val id: String,
    val name: String,
    val description: String,
    val serviceIcon: ServiceType,
    val nodes: List<FlowNode>,
    val connections: List<FlowConnection>
)
