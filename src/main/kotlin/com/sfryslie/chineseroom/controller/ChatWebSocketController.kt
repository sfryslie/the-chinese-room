package com.sfryslie.chineseroom.controller

import com.sfryslie.chineseroom.service.ChineseRoomService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller

@Controller
class ChatWebSocketController(private val service: ChineseRoomService) {

    @MessageMapping("/chat")
    @SendToUser("/queue/replies")
    fun handleMessage(message: WsMessage, headerAccessor: SimpMessageHeaderAccessor): WsReply {
        val sessionId = headerAccessor.sessionId ?: "unknown"
        val reply = service.chat(sessionId, message.message)
        return WsReply(reply)
    }
}

data class WsMessage(val message: String = "")
data class WsReply(val reply: String)
