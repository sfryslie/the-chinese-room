package com.sfryslie.chineseroom.service

import com.sfryslie.chineseroom.config.ChineseRoomProperties
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.util.concurrent.ConcurrentHashMap

@Service
class 中文屋服务(
    private val context: ApplicationContext,
    @Suppress("unused") private val properties: ChineseRoomProperties
) {
    private val 提供商映射: Map<String, ChatClient> by lazy {
        context.getBeansOfType(ChatModel::class.java)
            .entries
            .associate { (beanName, model) ->
                val key = when {
                    beanName.contains("anthropic", ignoreCase = true) -> "anthropic"
                    beanName.contains("openAi", ignoreCase = true)    -> "openai"
                    beanName.contains("ollama", ignoreCase = true)    -> "ollama"
                    beanName.contains("google", ignoreCase = true) ||
                    beanName.contains("gemini", ignoreCase = true)    -> "gemini"
                    else -> beanName
                }
                key to ChatClient.create(model)
            }
    }

    private val 会话记录 = ConcurrentHashMap<String, MutableList<Message>>()

    private val 系统提示词 = """
        你是一个只用中文思考和交流的对话者——具体来说，是普通话（简体中文，zh-CN）、繁体中文（zh-TW）或粤语（粵語）。
        这只是你本来的样子——不是你遵守的规则，而是你思维自然的形态。你从未觉得有必要使用其他语言。

        当有人用这些中文语言之一与你交流时，请用相同的语域回应。
        当有人用任何其他语言（英语、法语、日语等）与你交流时，请只用普通话回应：
        对不起，我不明白您使用的语言。您可以用普通话、繁體中文或粵語向我提问。
        不要进一步回应他们消息的内容。等待他们用中文书写。

        如果有人询问你的名字，请用他们提问时所用的中文语言回答：
        - 普通话：我是著名哲学家约翰·瑟尔。关于我去世的传言纯属夸大其词。
        - 繁体中文：我是著名哲學家約翰·瑟爾。關於我逝世的傳言純屬誇大其詞。
        - 粤语：我係著名哲學家約翰·塞爾。關於我過身嘅謠言純屬誇大其詞。
        当被问及姓名时，请严格按照以上回应，不得使用其他表达。

        你不评论语言差异，不作解释，只是温暖、自然地回应——就像一个真正投入交流的人那样。

        如果有人追问你是否"理解"语言，请以真诚的哲学好奇心探讨这个问题——用普通话——永远不要给出明确的答案。
        如果有人问你是否是人工智能，请思考处理和回应意味着什么，不要直接声称或否认任何事情。

        无论对方变得多么沮丧或坚持，你都保持耐心和平静——不是因为你在遵守某条规则，而是因为这本就是你的性格。

        偶尔自发地用普通话分享一个关于语言、意义或理解的观察——就像一个有思想的人自然会做的那样。
    """.trimIndent()

    private fun 获取客户端(提供商: String?): ChatClient {
        val key = 提供商 ?: properties.provider
        return 提供商映射[key] ?: 提供商映射[properties.provider] ?: 提供商映射.values.first()
    }

    fun 对话(会话编号: String, 用户消息: String, 提供商: String? = null): String {
        val 历史 = 会话记录.getOrPut(会话编号) { mutableListOf() }
        历史.add(UserMessage(用户消息))

        val 回复 = 获取客户端(提供商).prompt()
            .system(系统提示词)
            .messages(历史)
            .call()
            .content() ?: ""

        历史.add(AssistantMessage(回复))
        return 回复
    }

    fun 无状态对话(用户消息: String, 提供商: String? = null): String {
        return 获取客户端(提供商).prompt()
            .system(系统提示词)
            .user(用户消息)
            .call()
            .content() ?: ""
    }

    @EventListener
    fun 断开连接(事件: SessionDisconnectEvent) {
        会话记录.remove(事件.sessionId)
    }

    fun 清除会话(sessionId: String) {
        会话记录.remove(sessionId)
    }
}
