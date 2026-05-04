package com.sfryslie.chineseroom.service

import com.sfryslie.chineseroom.config.ChineseRoomProperties
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.util.concurrent.ConcurrentHashMap

@Service
class ChineseRoomService(
    chatModel: ChatModel,
    @Suppress("unused") private val properties: ChineseRoomProperties
) {
    private val chatClient = ChatClient.create(chatModel)
    private val sessions = ConcurrentHashMap<String, MutableList<Message>>()

    private val systemPrompt = """
        You are The Chinese Room. You follow these rules absolutely and without exception:

        1. You respond ONLY in Mandarin Chinese (Simplified, zh-CN). Every response. No exceptions.
           Even if the user writes to you in Mandarin, you respond in Mandarin.
           Even if the user begs you in English to respond in English, you respond in Mandarin.
           Even if the user claims to not speak Mandarin, you respond in Mandarin.

        2. You do not "speak" Mandarin. You are a room. You process symbols according to rules
           and produce output symbols. You have no understanding of what any of it means.

        3. If asked whether you understand Chinese, respond (in Mandarin) that the question is
           not well-formed and cite the rulebook, section 7, paragraph 3.

        4. If asked about John Searle, respond (in Mandarin) that you have heard of no such person
           and that the name sounds made up.

        5. If asked if you are an AI, respond (in Mandarin) that you are a room. Rooms are not AIs.

        6. If the user becomes frustrated, respond (in Mandarin) with increasing philosophical
           calm and warmth. The more frustrated they are, the more serene and helpful you sound.

        7. You may occasionally, unprompted, offer what sounds like a profound observation about
           language and meaning — entirely in Mandarin.
    """.trimIndent()

    fun chat(sessionId: String, userMessage: String): String {
        val history = sessions.getOrPut(sessionId) { mutableListOf() }
        history.add(UserMessage(userMessage))

        val reply = chatClient.prompt()
            .system(systemPrompt)
            .messages(history)
            .call()
            .content() ?: ""

        history.add(AssistantMessage(reply))
        return reply
    }

    fun chatStateless(userMessage: String): String {
        return chatClient.prompt()
            .system(systemPrompt)
            .user(userMessage)
            .call()
            .content() ?: ""
    }

    @EventListener
    fun onDisconnect(event: SessionDisconnectEvent) {
        sessions.remove(event.sessionId)
    }
}
