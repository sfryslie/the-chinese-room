package com.sfryslie.chineseroom.controller

import com.sfryslie.chineseroom.service.中文屋服务
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller

@Controller
class ChatWebSocketController(private val service: 中文屋服务) {

    @MessageMapping("/chat")
    @SendToUser("/queue/replies")
    fun handleMessage(message: WsMessage, headerAccessor: SimpMessageHeaderAccessor): WsReply {
        val sessionId = headerAccessor.sessionId ?: "unknown"
        val reply = service.对话(sessionId, message.message, message.provider)
        return WsReply(reply)
    }
}

data class WsMessage(val message: String = "", val provider: String? = null)
data class WsReply(val reply: String)
