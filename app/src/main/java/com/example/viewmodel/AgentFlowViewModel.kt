package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import com.example.BuildConfig

class AgentFlowViewModel : ViewModel() {

    private val _nodes = MutableStateFlow<List<FlowNode>>(emptyList())
    val nodes: StateFlow<List<FlowNode>> = _nodes.asStateFlow()

    private val _connections = MutableStateFlow<List<FlowConnection>>(emptyList())
    val connections: StateFlow<List<FlowConnection>> = _connections.asStateFlow()

    private val _logs = MutableStateFlow<List<ExecutionLog>>(emptyList())
    val logs: StateFlow<List<ExecutionLog>> = _logs.asStateFlow()

    private val _copilotMessages = MutableStateFlow<List<CopilotMessage>>(emptyList())
    val copilotMessages: StateFlow<List<CopilotMessage>> = _copilotMessages.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _selectedNode = MutableStateFlow<FlowNode?>(null)
    val selectedNode: StateFlow<FlowNode?> = _selectedNode.asStateFlow()

    private val _currentActiveNodeId = MutableStateFlow<String?>(null)
    val currentActiveNodeId: StateFlow<String?> = _currentActiveNodeId.asStateFlow()

    private val _isCopilotLoading = MutableStateFlow(false)
    val isCopilotLoading: StateFlow<Boolean> = _isCopilotLoading.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // Default simulation speeds
    private var simulationStepDelay = 1800L

    init {
        loadDefaultFlow()
        loadInitialCopilotMessages()
    }

    private fun loadDefaultFlow() {
        val sheetsNode = FlowNode(
            id = "sheets_trigger",
            title = "Google Sheets Trigger",
            type = NodeType.TRIGGER,
            service = ServiceType.GOOGLE_SHEETS,
            actionName = "Fila Nueva o Modificada",
            x = 80f,
            y = 150f,
            parameters = listOf(
                NodeParameter("spreadsheet_id", "ID de Planilla", ParamType.TEXT, "1x9A_B2c3D4e5F-SheetsID", description = "El ID de la hoja de cálculo de Google Drive"),
                NodeParameter("sheet_name", "Nombre de la Hoja", ParamType.TEXT, "Respuestas Formulario", description = "Pestaña de la hoja a monitorear"),
                NodeParameter("poll_interval", "Intervalo de Consulta", ParamType.SELECT, "Cada 5 minutos", listOf("Cada minuto", "Cada 5 minutos", "Cada hora", "Tiempo Real (Webhooks)"))
            ),
            outputPayload = """{
  "spreadsheet": "CRM Ventas",
  "sheet": "Respuestas Formulario",
  "row_index": 42,
  "data": {
    "Nombre": "Ignacio Barrientos",
    "Email": "nachobarrientos34@gmail.com",
    "Empresa": "AgentFlow Inc.",
    "Presupuesto": "8500 USD",
    "Mensaje": "Hola! Me interesa integrar un Copilot de IA en mis flujos de n8n."
  },
  "timestamp": "2026-06-28 21:00:15"
}"""
        )

        val condNode = FlowNode(
            id = "conditional_logic",
            title = "Filtro de Presupuesto",
            type = NodeType.LOGIC,
            service = ServiceType.CONDITIONAL,
            actionName = "Condición If/Else",
            x = 320f,
            y = 200f,
            parameters = listOf(
                NodeParameter("field", "Campo a Evaluar", ParamType.TEXT, "data.Presupuesto", description = "Ruta del JSON entrante"),
                NodeParameter("operator", "Operador", ParamType.SELECT, "Mayor que", listOf("Igual a", "No igual a", "Contiene", "Mayor que", "Menor que")),
                NodeParameter("compare_value", "Valor de Comparación", ParamType.TEXT, "5000", description = "Valor para contrastar numéricamente")
            ),
            outputPayload = "{}"
        )

        val slackNode = FlowNode(
            id = "slack_action",
            title = "Slack Notify (VIP)",
            type = NodeType.ACTION,
            service = ServiceType.SLACK,
            actionName = "Enviar Mensaje a Canal",
            x = 580f,
            y = 100f,
            parameters = listOf(
                NodeParameter("channel", "Canal de Slack", ParamType.TEXT, "#ventas-vip", description = "Canal de destino para notificaciones de alto valor"),
                NodeParameter("message_text", "Texto del Mensaje", ParamType.TEXTAREA, "🚨 *Nuevo Lead de Alto Presupuesto!*\n*Nombre:* {{data.Nombre}}\n*Empresa:* {{data.Empresa}}\n*Presupuesto:* {{data.Presupuesto}}\n*Mensaje:* {{data.Mensaje}}", description = "Usa {{campo}} para inyectar variables")
            ),
            outputPayload = "{}"
        )

        val trelloNode = FlowNode(
            id = "trello_action",
            title = "Trello Card (General)",
            type = NodeType.ACTION,
            service = ServiceType.TRELLO,
            actionName = "Crear Tarjeta",
            x = 580f,
            y = 320f,
            parameters = listOf(
                NodeParameter("board_id", "Tablero de Trello", ParamType.TEXT, "Sales Pipeline", description = "Tablero de destino"),
                NodeParameter("list_id", "Lista de Destino", ParamType.TEXT, "Leads Entrantes", description = "Lista donde caerá la tarjeta"),
                NodeParameter("card_title", "Título de Tarjeta", ParamType.TEXT, "{{data.Empresa}} - {{data.Nombre}}", description = "Título descriptivo de la tarjeta"),
                NodeParameter("card_desc", "Descripción de Tarjeta", ParamType.TEXTAREA, "Presupuesto: {{data.Presupuesto}}\nEmail: {{data.Email}}\n\nMensaje: {{data.Mensaje}}", description = "Descripción detallada")
            ),
            outputPayload = "{}"
        )

        _nodes.value = listOf(sheetsNode, condNode, slackNode, trelloNode)
        _connections.value = listOf(
            FlowConnection("sheets_trigger", "conditional_logic", "output", "input"),
            FlowConnection("conditional_logic", "slack_action", "true", "input"),
            FlowConnection("conditional_logic", "trello_action", "false", "input")
        )
    }

    private fun loadInitialCopilotMessages() {
        _copilotMessages.value = listOf(
            CopilotMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.COPILOT,
                text = "¡Hola! Soy tu Copilot inteligente de **AgentFlow**. 🔮✨\n\nAquí puedes diseñar automatizaciones hipercomplejas que combinan la potencia de **n8n** con la elegancia visual de **Make.com** en una interfaz Apple Glassmorphic premium.\n\nPuedo ayudarte a:\n• Conectar Sheets, Slack, Trello, o HTTP Requests.\n• Escribir condiciones lógicas complejas.\n• Diseñar un flujo desde cero si me lo pides.\n\nPrueba a escribir: *\"Crea un flujo de Google Sheets a Slack con un condicional de presupuesto\"* o selecciona uno de los accesos directos abajo.",
                timestamp = getCurrentTime()
            )
        )
    }

    // Node Operations
    fun selectNode(node: FlowNode?) {
        _selectedNode.value = node
        _nodes.value = _nodes.value.map {
            it.copy(selected = it.id == node?.id)
        }
    }

    fun updateNodePosition(nodeId: String, dx: Float, dy: Float) {
        _nodes.value = _nodes.value.map {
            if (it.id == nodeId) {
                it.copy(x = (it.x + dx).coerceIn(10f, 2000f), y = (it.y + dy).coerceIn(10f, 2000f))
            } else {
                it
            }
        }
        // Update selected node state to keep properties panel in sync
        _selectedNode.value?.let {
            if (it.id == nodeId) {
                _selectedNode.value = _nodes.value.first { n -> n.id == nodeId }
            }
        }
    }

    fun addNode(service: ServiceType, x: Float = 150f, y: Float = 150f) {
        val uniqueId = "node_" + UUID.randomUUID().toString().take(6)
        val title = "Nuevo ${service.displayName}"
        val (type, defaultAction, defaultParams) = getDefaultsForService(service)

        val newNode = FlowNode(
            id = uniqueId,
            title = title,
            type = type,
            service = service,
            actionName = defaultAction,
            x = x,
            y = y,
            parameters = defaultParams
        )

        _nodes.value = _nodes.value + newNode
        addLog(LogLevel.INFO, "Nodo '${newNode.title}' añadido al lienzo.", nodeId = uniqueId)
    }

    fun deleteNode(nodeId: String) {
        _nodes.value = _nodes.value.filterNot { it.id == nodeId }
        _connections.value = _connections.value.filterNot { it.fromNodeId == nodeId || it.toNodeId == nodeId }
        if (_selectedNode.value?.id == nodeId) {
            _selectedNode.value = null
        }
        addLog(LogLevel.WARNING, "Nodo eliminado: $nodeId")
    }

    fun updateNodeParameters(nodeId: String, updatedParams: List<NodeParameter>) {
        _nodes.value = _nodes.value.map {
            if (it.id == nodeId) {
                it.copy(parameters = updatedParams)
            } else {
                it
            }
        }
        _selectedNode.value?.let {
            if (it.id == nodeId) {
                _selectedNode.value = _nodes.value.first { n -> n.id == nodeId }
            }
        }
    }

    fun addConnection(fromId: String, toId: String, fromPort: String = "output", toPort: String = "input") {
        // Prevent duplicate connections
        val exists = _connections.value.any { it.fromNodeId == fromId && it.toNodeId == toId && it.fromPortName == fromPort }
        if (exists) return

        // Prevent cyclic self connections
        if (fromId == toId) return

        val newConn = FlowConnection(fromId, toId, fromPort, toPort)
        _connections.value = _connections.value + newConn
        addLog(LogLevel.INFO, "Conexión creada: Desde ${getNodeTitle(fromId)} ($fromPort) -> Hacia ${getNodeTitle(toId)}")
    }

    fun deleteConnection(connection: FlowConnection) {
        _connections.value = _connections.value.filterNot { it == connection }
        addLog(LogLevel.INFO, "Conexión eliminada.")
    }

    fun clearCanvas() {
        _nodes.value = emptyList()
        _connections.value = emptyList()
        _selectedNode.value = null
        _logs.value = emptyList()
        addLog(LogLevel.WARNING, "Lienzo de automatización despejado completamente.")
    }

    // Run Simulation
    fun startExecution() {
        if (_isExecuting.value) return
        _isExecuting.value = true
        _logs.value = emptyList() // clear previous logs

        addLog(LogLevel.INFO, "🚀 Iniciando ejecución del flujo de automatización 'AgentFlow'...")

        viewModelScope.launch(Dispatchers.Default) {
            // Locate triggers
            val triggers = _nodes.value.filter { it.type == NodeType.TRIGGER }
            if (triggers.isEmpty()) {
                addLog(LogLevel.ERROR, "Error de Ejecución: No se encontró ningún nodo Disparador (Trigger) en el flujo. Añade uno para comenzar.")
                _isExecuting.value = false
                return@launch
            }

            // Reset all nodes state to Idle first
            _nodes.value = _nodes.value.map { it.copy(status = NodeExecutionStatus.IDLE) }

            for (trigger in triggers) {
                executeNodeChain(trigger.id, initialPayload = trigger.outputPayload)
            }

            delay(800)
            addLog(LogLevel.SUCCESS, "🏆 Ejecución del flujo completada con éxito. Todos los nodos procesados de forma asíncrona.")
            _isExecuting.value = false
            _currentActiveNodeId.value = null
        }
    }

    private suspend fun executeNodeChain(nodeId: String, initialPayload: String) {
        val node = _nodes.value.find { it.id == nodeId } ?: return

        // Set to running
        _currentActiveNodeId.value = nodeId
        _nodes.value = _nodes.value.map {
            if (it.id == nodeId) it.copy(status = NodeExecutionStatus.RUNNING, inputPayload = initialPayload) else it
        }

        addLog(LogLevel.INFO, "Procesando nodo: [${node.service.displayName}] - '${node.title}'", nodeId = nodeId)
        delay(simulationStepDelay)

        // Generate mock output payload based on node configurations
        val generatedOutput = generateMockOutput(node, initialPayload)

        _nodes.value = _nodes.value.map {
            if (it.id == nodeId) {
                it.copy(
                    status = NodeExecutionStatus.SUCCESS,
                    outputPayload = generatedOutput
                )
            } else {
                it
            }
        }
        addLog(LogLevel.SUCCESS, "✅ Nodo '${node.title}' ejecutado correctamente. Datos procesados.", nodeId = nodeId, input = initialPayload, output = generatedOutput)

        // Find next nodes based on connections
        val nodeConns = _connections.value.filter { it.fromNodeId == nodeId }

        if (node.service == ServiceType.CONDITIONAL) {
            // Logic evaluation simulation
            val threshold = node.parameters.find { it.key == "compare_value" }?.value?.toDoubleOrNull() ?: 5000.0
            val budget = extractBudgetFromPayload(initialPayload)

            val conditionResult = budget > threshold
            addLog(LogLevel.INFO, "Condicional: Presupuesto ($budget) > Límite ($threshold) => Resultado: *${if (conditionResult) "SÍ (Verdadero)" else "NO (Falso)"}*", nodeId = nodeId)

            val branchToExecute = if (conditionResult) "true" else "false"
            val matchingConns = nodeConns.filter { it.fromPortName == branchToExecute }

            for (conn in matchingConns) {
                executeNodeChain(conn.toNodeId, generatedOutput)
            }
        } else if (node.service == ServiceType.LOOP) {
            // Loop simulation
            val iterations = node.parameters.find { it.key == "iterations" }?.value?.toIntOrNull() ?: 3
            addLog(LogLevel.INFO, "Bucle: Iniciando iteración para $iterations elementos encontrados.", nodeId = nodeId)

            // Execute item branch
            val itemConns = nodeConns.filter { it.fromPortName == "item" }
            for (i in 1..iterations) {
                addLog(LogLevel.INFO, "Bucle: Procesando elemento [$i/$iterations]...", nodeId = nodeId)
                val itemPayload = """{ "item_index": $i, "parent_data": $generatedOutput }"""
                for (conn in itemConns) {
                    executeNodeChain(conn.toNodeId, itemPayload)
                }
                delay(1000)
            }

            // Execute finished branch
            val finishedConns = nodeConns.filter { it.fromPortName == "finished" }
            for (conn in finishedConns) {
                executeNodeChain(conn.toNodeId, generatedOutput)
            }
        } else {
            // Standard action / trigger propagation
            for (conn in nodeConns) {
                executeNodeChain(conn.toNodeId, generatedOutput)
            }
        }
    }

    private fun extractBudgetFromPayload(payload: String): Double {
        return try {
            val json = JSONObject(payload)
            if (json.has("data")) {
                val data = json.getJSONObject("data")
                if (data.has("Presupuesto")) {
                    val presStr = data.getString("Presupuesto")
                    // extract numbers
                    val numberOnly = presStr.replace("[^0-9]".toRegex(), "")
                    numberOnly.toDoubleOrNull() ?: 8500.0
                } else 8500.0
            } else 8500.0
        } catch (e: Exception) {
            8500.0
        }
    }

    private fun generateMockOutput(node: FlowNode, input: String): String {
        return when (node.service) {
            ServiceType.GOOGLE_SHEETS -> {
                if (node.type == NodeType.TRIGGER) {
                    node.outputPayload // return default trigger payload
                } else {
                    """{
  "status": "success",
  "row_inserted": true,
  "spreadsheet_id": "${node.parameters.find { it.key == "spreadsheet_id" }?.value ?: "default_id"}",
  "timestamp": "${getCurrentTime()}",
  "input_echo": $input
}"""
                }
            }
            ServiceType.SLACK -> {
                """{
  "status": "delivered",
  "channel": "${node.parameters.find { it.key == "channel" }?.value ?: "#ventas"}",
  "message_id": "msg_${UUID.randomUUID().toString().take(8)}",
  "delivered_at": "${getCurrentTime()}"
}"""
            }
            ServiceType.TRELLO -> {
                """{
  "status": "card_created",
  "card_id": "card_${UUID.randomUUID().toString().take(6)}",
  "board_id": "${node.parameters.find { it.key == "board_id" }?.value ?: "board_id"}",
  "list_id": "${node.parameters.find { it.key == "list_id" }?.value ?: "list_id"}",
  "short_url": "https://trello.com/c/mockUrl123"
}"""
            }
            ServiceType.HTTP -> {
                val method = node.parameters.find { it.key == "method" }?.value ?: "GET"
                val url = node.parameters.find { it.key == "url" }?.value ?: "https://api.example.com"
                """{
  "statusCode": 200,
  "statusMessage": "OK",
  "request": {
    "method": "$method",
    "url": "$url",
    "headers": {
      "Accept": "application/json",
      "User-Agent": "AgentFlow/iOS-26-Client"
    }
  },
  "body": {
    "status": "online",
    "message": "Petición HTTP simulada de forma exitosa.",
    "agent_flow_mcp_active": true,
    "payload_received": $input
  }
}"""
            }
            ServiceType.CONDITIONAL -> input
            ServiceType.LOOP -> input
            ServiceType.WEBHOOK -> {
                """{
  "webhook_id": "wh_${UUID.randomUUID().toString().take(8)}",
  "caller_ip": "192.168.1.1",
  "payload": {
    "event": "payment_succeeded",
    "amount": 299.00,
    "currency": "usd",
    "customer": "cus_NachoB34"
  }
}"""
            }
        }
    }

    private fun addLog(level: LogLevel, message: String, nodeId: String? = null, input: String? = null, output: String? = null) {
        val nodeName = nodeId?.let { id -> _nodes.value.find { it.id == id }?.title }
        val newLog = ExecutionLog(
            id = UUID.randomUUID().toString(),
            timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
            nodeId = nodeId,
            nodeName = nodeName,
            message = message,
            level = level,
            inputData = input,
            outputData = output
        )
        _logs.value = _logs.value + newLog
    }

    // Copilot / AI Assistant
    fun sendCopilotMessage(text: String) {
        if (text.isBlank()) return

        // Add user message
        val userMsg = CopilotMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = text,
            timestamp = getCurrentTime()
        )
        _copilotMessages.value = _copilotMessages.value + userMsg
        _isCopilotLoading.value = true

        viewModelScope.launch {
            delay(1200) // Beautiful smooth natural delay
            
            // Check if there's an actual API key and process with Gemini
            val key = BuildConfig.GEMINI_API_KEY
            val isRealKeyAvailable = key.isNotEmpty() && !key.contains("MY_GEMINI_API_KEY")

            val replyText: String
            var templateToApply: String? = null

            // 1. Keyword check for dynamic template generation
            val lowerText = text.lowercase(Locale.ROOT)
            if (lowerText.contains("sheets") && lowerText.contains("slack") && (lowerText.contains("condic") || lowerText.contains("if") || lowerText.contains("filtro"))) {
                templateToApply = "sheets_conditional_slack"
                replyText = "¡Excelente elección! He diseñado el flujo de **Google Sheets a Slack con un Filtro de Lógica Condicional** directamente en tu lienzo.\n\nHe reacomodado tus tarjetas glassmorphic:\n1. **Trigger de Google Sheets**: detecta nuevas filas.\n2. **If/Else Lógico**: Filtra las de mayor presupuesto (> 5000 USD).\n3. **Ramas de Ejecución**: Si es un lead VIP, dispara notificación en Slack. Si es un lead común, crea una tarjeta en Trello para posterior seguimiento.\n\n*Prueba a pulsar 'Ejecutar' arriba a la derecha para ver cómo fluyen los datos con un destello azul.* ⚡️"
            } else if (lowerText.contains("sheets") && lowerText.contains("slack")) {
                templateToApply = "sheets_slack_simple"
                replyText = "¡De acuerdo! He configurado un flujo directo simplificado: **Google Sheets conectado a Slack**.\n\nCada vez que se añada un registro en la hoja de cálculo, se enviará una notificación automatizada al canal respectivo de inmediato."
            } else if (lowerText.contains("bucle") || lowerText.contains("loop") || lowerText.contains("repetir")) {
                templateToApply = "loop_http_trello"
                replyText = "¡Entendido! Añadí un flujo estructurado con un **Bucle For Each**. El flujo captura datos vía un **Webhook**, recorre cada elemento de la lista mediante el nodo de bucle y realiza una **Petición HTTP Custom** junto con la creación asíncrona de tarjetas en Trello.\n\n¡La lógica repetitiva ya está montada en el constructor!"
            } else if (lowerText.contains("http") || lowerText.contains("api") || lowerText.contains("web") || lowerText.contains("doc")) {
                templateToApply = "webhook_http_sheets"
                replyText = "He cargado un flujo de integración de nivel técnico alto:\n• **Webhook Trigger**: Listo para recibir payloads con documentación HTTP interactiva.\n• **HTTP Request Node**: Configurado con headers, método POST y parseador de body para integrarte a APIs personalizadas.\n• **Google Sheets Action**: Para guardar el resultado.\n\n¡Súper completo para conectar cualquier API externa!"
            } else if (lowerText.contains("ejecut") || lowerText.contains("correr") || lowerText.contains("probar") || lowerText.contains("play")) {
                replyText = "¡Entendido! Ejecutando el flujo de automatización activo ahora mismo. Verás parpadear los nodos con un aura brillante según se procesen asíncronamente."
                viewModelScope.launch {
                    delay(500)
                    startExecution()
                }
            } else if (lowerText.contains("limp") || lowerText.contains("borrar") || lowerText.contains("vaciar")) {
                replyText = "He despejado todo el lienzo del constructor de automatizaciones. ¡Listo para que arranques tu próximo flujo maestro o me pidas diseñarlo!"
                clearCanvas()
            } else {
                // Conversational Spanish fallback or real Gemini API call
                if (isRealKeyAvailable) {
                    replyText = callGeminiApi(text)
                } else {
                    replyText = getLocalConversationalReply(text)
                }
            }

            // Apply templates if triggered
            if (templateToApply != null) {
                applyTemplateById(templateToApply)
            }

            _copilotMessages.value = _copilotMessages.value + CopilotMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.COPILOT,
                text = replyText,
                timestamp = getCurrentTime()
            )
            _isCopilotLoading.value = false
        }
    }

    private suspend fun callGeminiApi(prompt: String): String = withContext(Dispatchers.IO) {
        // Implement real REST API call using guidelines if key is active.
        try {
            val key = BuildConfig.GEMINI_API_KEY
            // We can compose a nice system instruction
            val systemPrompt = "Eres el Copilot de AgentFlow, un creador visual de flujos de automatización que es una mezcla de Make.com y n8n, en una elegante interfaz iOS Glassmorphic de Apple. Habla en español de forma refinada, poética y servicial. Si el usuario te pide crear un flujo que involucre Google Sheets, Slack, Trello, o peticiones HTTP, descríbelo de manera increíble y dile que ya has configurado el lienzo con dicho flujo."
            
            val requestBody = """
                {
                  "contents": [
                    {
                      "role": "user",
                      "parts": [{"text": "$prompt"}]
                    }
                  ],
                  "systemInstruction": {
                    "parts": [{"text": "$systemPrompt"}]
                  },
                  "generationConfig": {
                    "temperature": 0.7
                  }
                }
            """.trimIndent()

            val response = RetrofitClient.service.generateContent(key, GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
            ))
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: getLocalConversationalReply(prompt)
        } catch (e: Exception) {
            Log.e("AgentFlowVM", "Error calling Gemini API: ${e.message}", e)
            getLocalConversationalReply(prompt)
        }
    }

    private fun getLocalConversationalReply(prompt: String): String {
        val lower = prompt.lowercase(Locale.ROOT)
        return when {
            lower.contains("hola") || lower.contains("buenos") || lower.contains("que tal") -> {
                "¡Hola de nuevo! 👋 Soy el Copilot de AgentFlow. ¿Qué flujo de automatización increíble diseñamos hoy?\n\nPuedes decirme algo como: *\"Crea un flujo de Sheets con Slack\"* o pedirme un condicional lógigo."
            }
            lower.contains("make") || lower.contains("n8n") -> {
                "**AgentFlow** se sitúa en el punto óptimo de la automatización:\n• De **Make.com** tomamos la fluidez visual impecable, el editor elástico y la extrema sencillez interactiva.\n• De **n8n** tomamos el soporte robusto de variables complejas, lógica condicional refinada, bucles dinámicos y la potencia técnica de consultar cualquier API vía HTTP Request con su propia documentación.\n\nTodo revestido en un estilo **Apple Glassmorphism** de última generación."
            }
            lower.contains("glass") || lower.contains("diseño") || lower.contains("estética") -> {
                "El diseño de **AgentFlow** utiliza las directrices más avanzadas de **Apple iOS Glassmorphism**:\n• Tarjetas con un nivel de transparencia calibrado y bordes de alto contraste con gradiente.\n• Blureos profundos en el fondo que permiten apilar elementos interactivos sin perder legibilidad.\n• Colores neón sutilmente seleccionados para representar el estado y categoría de cada nodo.\n\nEs, sin duda, la interfaz de automatización más bella jamás diseñada."
            }
            lower.contains("http") || lower.contains("request") || lower.contains("api") -> {
                "¡El nodo **HTTP Request** de AgentFlow es súper potente! Te permite definir URL, Headers, elegir métodos HTTP (GET, POST, PUT, DELETE) y mapear el cuerpo de la petición. Además, incluye documentación en vivo integrada para que depurar payloads complejos sea pan comido. ¡Pruébalo cargando el template de HTTP!"
            }
            else -> {
                "Entiendo perfectamente. Como tu asistente de automatización visual, puedo dar soporte a tus integraciones. Si deseas ver este flujo en acción con blureos y animaciones fluidas de red, puedes pedirme: *'crea un flujo con condicional'*, o tocar el botón **Ejecutar** en la barra superior."
            }
        }
    }

    private fun applyTemplateById(templateId: String) {
        clearCanvas()
        when (templateId) {
            "sheets_conditional_slack" -> loadDefaultFlow() // Already loads default Sheets -> Cond -> Slack/Trello
            "sheets_slack_simple" -> {
                val sheets = FlowNode(
                    id = "sheets_t",
                    title = "Google Sheets New Row",
                    type = NodeType.TRIGGER,
                    service = ServiceType.GOOGLE_SHEETS,
                    actionName = "Nueva Fila",
                    x = 100f,
                    y = 200f,
                    parameters = listOf(
                        NodeParameter("spreadsheet_id", "ID de Planilla", ParamType.TEXT, "1SheetsID-ABC"),
                        NodeParameter("sheet_name", "Hoja", ParamType.TEXT, "Ventas")
                    ),
                    outputPayload = """{ "data": { "Nombre": "Clara", "Monto": "2500" } }"""
                )
                val slack = FlowNode(
                    id = "slack_a",
                    title = "Enviar Notificación",
                    type = NodeType.ACTION,
                    service = ServiceType.SLACK,
                    actionName = "Mensaje a Canal",
                    x = 420f,
                    y = 200f,
                    parameters = listOf(
                        NodeParameter("channel", "Canal", ParamType.TEXT, "#alertas"),
                        NodeParameter("message_text", "Texto", ParamType.TEXTAREA, "Nueva venta: {{data.Nombre}} por {{data.Monto}}")
                    )
                )
                _nodes.value = listOf(sheets, slack)
                _connections.value = listOf(FlowConnection("sheets_t", "slack_a", "output", "input"))
            }
            "loop_http_trello" -> {
                val webhook = FlowNode(
                    id = "wh_t",
                    title = "Webhook Recibido",
                    type = NodeType.TRIGGER,
                    service = ServiceType.WEBHOOK,
                    actionName = "Capturar JSON",
                    x = 80f,
                    y = 200f,
                    parameters = listOf(
                        NodeParameter("path", "Ruta de Webhook", ParamType.TEXT, "/v1/payment-received")
                    ),
                    outputPayload = """{ "customer_list": [{"id": 1, "name": "Bucle Uno"}, {"id": 2, "name": "Bucle Dos"}] }"""
                )
                val loop = FlowNode(
                    id = "loop_n",
                    title = "Bucle Clientes",
                    type = NodeType.LOOP,
                    service = ServiceType.LOOP,
                    actionName = "Bucle For Each",
                    x = 300f,
                    y = 200f,
                    parameters = listOf(
                        NodeParameter("array_path", "Ruta del Array", ParamType.TEXT, "customer_list"),
                        NodeParameter("iterations", "Número Máximo", ParamType.TEXT, "2")
                    )
                )
                val http = FlowNode(
                    id = "http_a",
                    title = "Enviar API Externa",
                    type = NodeType.ACTION,
                    service = ServiceType.HTTP,
                    actionName = "POST Request",
                    x = 540f,
                    y = 100f,
                    parameters = listOf(
                        NodeParameter("url", "Endpoint URL", ParamType.TEXT, "https://api.empresa.com/register"),
                        NodeParameter("method", "Método", ParamType.SELECT, "POST", listOf("GET", "POST", "PUT")),
                        NodeParameter("body", "Payload", ParamType.TEXTAREA, "{ \"id\": {{item.id}}, \"name\": \"{{item.name}}\" }")
                    )
                )
                val trello = FlowNode(
                    id = "trello_a",
                    title = "Trello Finalizado",
                    type = NodeType.ACTION,
                    service = ServiceType.TRELLO,
                    actionName = "Crear Tarjeta",
                    x = 540f,
                    y = 300f,
                    parameters = listOf(
                        NodeParameter("board_id", "Tablero", ParamType.TEXT, "Tareas"),
                        NodeParameter("card_title", "Título", ParamType.TEXT, "Flujo de Bucle Terminado!")
                    )
                )
                _nodes.value = listOf(webhook, loop, http, trello)
                _connections.value = listOf(
                    FlowConnection("wh_t", "loop_n", "output", "input"),
                    FlowConnection("loop_n", "http_a", "item", "input"),
                    FlowConnection("loop_n", "trello_a", "finished", "input")
                )
            }
            "webhook_http_sheets" -> {
                val webhook = FlowNode(
                    id = "wh_trigger",
                    title = "Webhook de Entrada",
                    type = NodeType.TRIGGER,
                    service = ServiceType.WEBHOOK,
                    actionName = "Webhook de Escucha",
                    x = 80f,
                    y = 200f,
                    parameters = emptyList(),
                    outputPayload = """{ "user_id": "999", "action": "purchase", "item": "Suscripción Premium" }"""
                )
                val http = FlowNode(
                    id = "http_req",
                    title = "Consultar Datos Usuario",
                    type = NodeType.ACTION,
                    service = ServiceType.HTTP,
                    actionName = "GET Request",
                    x = 320f,
                    y = 200f,
                    parameters = listOf(
                        NodeParameter("url", "Request URL", ParamType.TEXT, "https://reqres.in/api/users/2"),
                        NodeParameter("method", "Método", ParamType.SELECT, "GET", listOf("GET", "POST")),
                        NodeParameter("headers", "Headers (JSON)", ParamType.TEXTAREA, "{\n  \"Authorization\": \"Bearer tok_abc123\"\n}")
                    )
                )
                val sheets = FlowNode(
                    id = "sheets_insert",
                    title = "Guardar Registro",
                    type = NodeType.ACTION,
                    service = ServiceType.GOOGLE_SHEETS,
                    actionName = "Insertar en Hoja",
                    x = 560f,
                    y = 200f,
                    parameters = listOf(
                        NodeParameter("spreadsheet_id", "Spreadsheet", ParamType.TEXT, "1X9A_Sheets_DB")
                    )
                )
                _nodes.value = listOf(webhook, http, sheets)
                _connections.value = listOf(
                    FlowConnection("wh_trigger", "http_req", "output", "input"),
                    FlowConnection("http_req", "sheets_insert", "output", "input")
                )
            }
        }
        addLog(LogLevel.SUCCESS, "Plantilla '$templateId' cargada con éxito en el constructor visual.")
    }

    private fun getDefaultsForService(service: ServiceType): Triple<NodeType, String, List<NodeParameter>> {
        return when (service) {
            ServiceType.GOOGLE_SHEETS -> Triple(
                NodeType.ACTION,
                "Insertar Fila",
                listOf(NodeParameter("spreadsheet_id", "ID de Planilla", ParamType.TEXT, ""), NodeParameter("sheet_name", "Nombre de Hoja", ParamType.TEXT, "Ventas"))
            )
            ServiceType.SLACK -> Triple(
                NodeType.ACTION,
                "Enviar Mensaje",
                listOf(NodeParameter("channel", "Canal de Slack", ParamType.TEXT, "#general"), NodeParameter("message", "Mensaje", ParamType.TEXTAREA, ""))
            )
            ServiceType.TRELLO -> Triple(
                NodeType.ACTION,
                "Crear Tarjeta",
                listOf(NodeParameter("board_id", "Tablero de Trello", ParamType.TEXT, ""), NodeParameter("card_title", "Título de Tarjeta", ParamType.TEXT, ""))
            )
            ServiceType.HTTP -> Triple(
                NodeType.ACTION,
                "Petición HTTP",
                listOf(NodeParameter("url", "URL de Endpoint", ParamType.TEXT, "https://api.github.com"), NodeParameter("method", "Método", ParamType.SELECT, "GET", listOf("GET", "POST", "PUT", "DELETE")))
            )
            ServiceType.CONDITIONAL -> Triple(
                NodeType.LOGIC,
                "Evaluar Condición",
                listOf(NodeParameter("field", "Campo", ParamType.TEXT, ""), NodeParameter("compare_value", "Límite", ParamType.TEXT, "5000"))
            )
            ServiceType.LOOP -> Triple(
                NodeType.LOOP,
                "Bucle For Each",
                listOf(NodeParameter("array_path", "Ruta del Array", ParamType.TEXT, ""), NodeParameter("iterations", "Límite de Iteraciones", ParamType.TEXT, "3"))
            )
            ServiceType.WEBHOOK -> Triple(
                NodeType.TRIGGER,
                "Webhook de Escucha",
                listOf(NodeParameter("path", "Ruta de Webhook", ParamType.TEXT, "/v1/webhook"))
            )
        }
    }

    private fun getNodeTitle(nodeId: String): String {
        return _nodes.value.find { it.id == nodeId }?.title ?: nodeId
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
}
