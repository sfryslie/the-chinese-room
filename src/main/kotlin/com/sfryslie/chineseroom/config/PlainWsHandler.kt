package com.sfryslie.chineseroom.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.sfryslie.chineseroom.service.中文屋服务
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class PlainWsHandler(private val service: 中文屋服务) : TextWebSocketHandler() {

    private val mapper = ObjectMapper()

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val node = mapper.readTree(message.payload)
        val text = node.get("message")?.asText()?.takeIf { it.isNotBlank() } ?: return
        val provider = node.get("provider")?.asText()

        val reply = service.对话(session.id, text, provider)
        session.sendMessage(TextMessage(mapper.writeValueAsString(mapOf("reply" to reply))))
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        service.清除会话(session.id)
    }
}
