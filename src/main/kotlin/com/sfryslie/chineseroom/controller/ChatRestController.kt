package com.sfryslie.chineseroom.controller

import com.sfryslie.chineseroom.service.中文屋服务
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChatRestController(private val service: 中文屋服务) {

    @PostMapping("/chat")
    fun chat(@RequestBody request: ChatRequest): ChatResponse {
        val reply = service.无状态对话(request.message, request.provider)
        return ChatResponse(reply)
    }
}

data class ChatRequest(val message: String = "", val provider: String? = null)
data class ChatResponse(val reply: String)
