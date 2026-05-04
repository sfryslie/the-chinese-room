package com.sfryslie.chineseroom.controller

import com.sfryslie.chineseroom.service.中文屋服务
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChatRestController(private val service: 中文屋服务) {

    @PostMapping("/chat")
    fun chat(@RequestBody request: ChatRequest): ChatResponse {
        val reply = service.chatStateless(request.message)
        return ChatResponse(reply)
    }
}

data class ChatRequest(val message: String = "")
data class ChatResponse(val reply: String)
