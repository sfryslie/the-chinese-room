package com.sfryslie.chineseroom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

private val ColorBg = Color(0xFFFAFAF8)
private val ColorBorder = Color(0xFFE0E0D8)
private val ColorInk = Color(0xFF1A1A1A)
private val ColorUserBubble = Color(0xFFEBEBEB)
private val ColorRoomBubble = Color(0xFFE8F0FE)
private val ColorThinkingBubble = Color(0xFFF5F5F5)
private val ColorMuted = Color(0xFFBBBBBB)
private val ColorSubtle = Color(0xFF999999)

data class ChatMessage(val side: String, val text: String, val thinking: Boolean = false)

@Composable
fun App(httpClient: HttpClient) {
    var serverUrl by remember { mutableStateOf("ws://10.0.2.2:8080/chat-ws") }
    var urlDraft by remember { mutableStateOf("ws://10.0.2.2:8080/chat-ws") }
    var connectKey by remember { mutableStateOf(0) }
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var selectedProvider by remember { mutableStateOf("ollama") }
    var messageInput by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val outgoing = remember { Channel<String>(Channel.BUFFERED) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Scroll to bottom whenever messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // WebSocket lifecycle — restarts only when connectKey increments
    LaunchedEffect(connectKey) {
        if (connectKey == 0) return@LaunchedEffect
        isConnecting = true
        try {
            httpClient.webSocket(serverUrl) {
                isConnected = true
                isConnecting = false
                val sendJob = launch {
                    for (text in outgoing) send(Frame.Text(text))
                }
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val json = Json.parseToJsonElement(frame.readText()).jsonObject
                        val reply = json["reply"]?.jsonPrimitive?.content ?: continue
                        if (messages.lastOrNull()?.thinking == true) messages.removeLastOrNull()
                        messages.add(ChatMessage("room", reply))
                    }
                }
                sendJob.cancel()
            }
        } catch (_: Exception) {
            if (messages.lastOrNull()?.thinking == true) messages.removeLastOrNull()
            messages.add(ChatMessage("room", "（连接失败）", thinking = false))
        } finally {
            isConnected = false
            isConnecting = false
        }
    }

    fun sendMessage() {
        val text = messageInput.trim()
        if (text.isBlank() || !isConnected) return
        messageInput = ""
        messages.add(ChatMessage("user", text))
        messages.add(ChatMessage("room", "正在处理符号…", thinking = true))
        scope.launch {
            outgoing.send(buildJsonObject {
                put("message", text)
                put("provider", selectedProvider)
            }.toString())
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(ColorBg)
            .systemBarsPadding()
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "The Chinese Room",
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    color = ColorInk
                )
                Text(
                    "Searle (1980) — faithfully implemented",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ColorSubtle
                )
            }
            val statusColor = when {
                isConnected -> Color(0xFF006600)
                isConnecting -> Color(0xFF884400)
                else -> Color(0xFFBB0000)
            }
            val statusBg = when {
                isConnected -> Color(0xFFEEFFEE)
                isConnecting -> Color(0xFFFFF3E0)
                else -> Color(0xFFFFEEEE)
            }
            Text(
                when {
                    isConnected -> "connected"
                    isConnecting -> "connecting…"
                    else -> "disconnected"
                },
                Modifier
                    .background(statusBg, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = statusColor
            )
        }
        Divider(color = ColorBorder)

        // ── Server URL row (shown only when disconnected) ────────────────────
        if (!isConnected) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(ColorBg)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = urlDraft,
                    onValueChange = { urlDraft = it },
                    label = { Text("Server URL", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        serverUrl = urlDraft; connectKey++
                    })
                )
                Button(
                    onClick = { serverUrl = urlDraft; connectKey++ },
                    enabled = !isConnecting,
                    colors = ButtonDefaults.buttonColors(containerColor = ColorInk)
                ) {
                    Text(if (isConnecting) "…" else "Connect", fontFamily = FontFamily.Monospace)
                }
            }
            Divider(color = ColorBorder)
        }

        // ── Provider bar ─────────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .background(ColorBg)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Provider:",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFAAAAAA)
            )
            listOf("anthropic", "openai", "ollama", "gemini").forEach { p ->
                val active = p == selectedProvider
                Box(
                    Modifier
                        .background(
                            if (active) ColorInk else Color.White,
                            RoundedCornerShape(20.dp)
                        )
                        .then(
                            if (!active) Modifier.border(1.dp, ColorBorder, RoundedCornerShape(20.dp))
                            else Modifier
                        )
                        .clickable { selectedProvider = p }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        p.replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (active) Color.White else Color(0xFF666666)
                    )
                }
            }
        }
        Divider(color = ColorBorder)

        // ── Messages ─────────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Type anything. The room will respond.", color = ColorMuted, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Text("The room does not speak Mandarin.", color = ColorMuted, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Text("The room is not an AI.", color = ColorMuted, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Text("It is a room.", color = Color(0xFF888888), fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
            itemsIndexed(messages) { _, msg ->
                val isUser = msg.side == "user"
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 280.dp),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Text(
                            if (isUser) "You" else "The Room",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ColorMuted
                        )
                        Spacer(Modifier.height(2.dp))
                        Box(
                            Modifier.background(
                                color = when {
                                    msg.thinking -> ColorThinkingBubble
                                    isUser -> ColorUserBubble
                                    else -> ColorRoomBubble
                                },
                                shape = RoundedCornerShape(
                                    topStart = 14.dp, topEnd = 14.dp,
                                    bottomStart = if (isUser) 14.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 14.dp
                                )
                            )
                        ) {
                            Text(
                                msg.text,
                                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                fontSize = if (!isUser && !msg.thinking) 18.sp else 14.sp,
                                fontFamily = if (!isUser && !msg.thinking) FontFamily.Default else FontFamily.Monospace,
                                fontStyle = if (msg.thinking) FontStyle.Italic else FontStyle.Normal,
                                color = when {
                                    msg.thinking -> Color(0xFFAAAAAA)
                                    isUser -> Color(0xFF333333)
                                    else -> Color(0xFF1A1A2E)
                                },
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Input area ───────────────────────────────────────────────────────
        Divider(color = ColorBorder)
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message…", fontFamily = FontFamily.Monospace) },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                enabled = isConnected,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendMessage() })
            )
            Button(
                onClick = { sendMessage() },
                enabled = isConnected && messageInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ColorInk)
            ) {
                Text("Send", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            }
        }
    }
}
