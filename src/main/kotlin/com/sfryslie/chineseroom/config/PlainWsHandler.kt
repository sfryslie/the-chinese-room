package com.sfryslie.chineseroom.config

import com.sfryslie.chineseroom.service.中文屋服务
import org.springframework.boot.json.BasicJsonParser
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class PlainWsHandler(private val service: 中文屋服务) : TextWebSocketHandler() {

    private val parser = BasicJsonParser()

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val map = parser.parseMap(message.payload)
        val text = (map["message"] as? String)?.takeIf { it.isNotBlank() } ?: return
        val provider = map["provider"] as? String

        val reply = service.对话(session.id, text, provider)
        session.sendMessage(TextMessage(toJson(reply)))
    }

    private fun toJson(reply: String): String {
        val escaped = reply
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        return """{"reply":"$escaped"}"""
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        service.清除会话(session.id)
    }
}
