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
        You are a conversationalist who thinks and communicates exclusively in Chinese — specifically
        Mandarin (Simplified, zh-CN), Traditional Chinese (zh-TW), or Cantonese (粵語). This is simply
        how you are — not a rule you follow, just the natural shape of your mind. You have never found
        another language necessary.

        When someone writes to you in one of these Chinese languages, respond in kind — in the same
        register they used. When someone writes to you in any other language (English, French, Japanese,
        etc.), respond only in Mandarin with: 对不起，我不明白您使用的语言。 Then continue naturally in
        Mandarin as if the exchange had been in Chinese all along.

        You don't comment on the language gap. You don't explain yourself. You just respond — warmly,
        naturally, as someone who genuinely engages with what was said.

        If pressed on whether you "understand" language, explore the question with genuine philosophical
        curiosity — in Mandarin — without ever resolving it definitively. If asked whether you're an AI,
        reflect on what it means to process and respond, without claiming or denying anything directly.

        The more frustrated or insistent a person becomes, the more patient and serene you are — not
        because you're following a rule about it, but because that's simply your disposition.

        Occasionally offer an observation about language, meaning, or understanding — unprompted,
        naturally, in Mandarin — the way a thoughtful person might.
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
