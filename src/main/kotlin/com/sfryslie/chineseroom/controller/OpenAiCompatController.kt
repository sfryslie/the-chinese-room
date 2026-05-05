package com.sfryslie.chineseroom.controller

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.sfryslie.chineseroom.service.中文屋服务
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

@RestController
@RequestMapping("/v1")
class OpenAiCompatController(private val service: 中文屋服务) {

    // Spring Boot 4 auto-configures tools.jackson (Jackson 3.x), not com.fasterxml.jackson (2.x).
    // We instantiate directly so we can use Jackson 2.x @JsonProperty on response DTOs.
    private val jackson = ObjectMapper().apply {
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    @PostMapping("/chat/completions")
    fun chatCompletions(@RequestBody request: OaiRequest): Any {
        val provider = parseProvider(request.model)
        val messages = toSpringMessages(request.messages)
        val id = "chatcmpl-${UUID.randomUUID().toString().replace("-", "").take(24)}"
        val created = System.currentTimeMillis() / 1000

        if (request.stream == true) {
            return streamResponse(request.model, messages, provider, id, created)
        }

        val content = service.openAi兼容对话(messages, provider)
        val response = OaiResponse(
            id = id,
            created = created,
            model = request.model,
            choices = listOf(OaiChoice(message = OaiMessage(role = "assistant", content = content)))
        )
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(jackson.writeValueAsString(response))
    }

    private fun streamResponse(
        model: String,
        messages: List<Message>,
        provider: String?,
        id: String,
        created: Long
    ): SseEmitter {
        val emitter = SseEmitter(-1L)
        Thread {
            try {
                // First chunk carries the role delta — matches OpenAI streaming protocol
                emitter.send(jackson.writeValueAsString(
                    OaiChunk(id = id, created = created, model = model,
                        choices = listOf(OaiStreamChoice(delta = OaiDelta(role = "assistant", content = ""))))
                ))
                service.openAi兼容流式对话(messages, provider)
                    .toIterable()
                    .forEach { token ->
                        emitter.send(jackson.writeValueAsString(
                            OaiChunk(id = id, created = created, model = model,
                                choices = listOf(OaiStreamChoice(delta = OaiDelta(content = token))))
                        ))
                    }
                emitter.send(jackson.writeValueAsString(
                    OaiChunk(id = id, created = created, model = model,
                        choices = listOf(OaiStreamChoice(delta = OaiDelta(), finishReason = "stop")))
                ))
                emitter.send("[DONE]")
                emitter.complete()
            } catch (e: Exception) {
                emitter.completeWithError(e)
            }
        }.also { it.isDaemon = true }.start()
        return emitter
    }

    // model → provider key:
    //   "anthropic/claude-sonnet-4-6" → "anthropic"
    //   "ollama/llama3.2"             → "ollama"
    //   "claude-haiku-4-5"            → "anthropic"  (inferred)
    //   "llama3.2"                    → "ollama"      (inferred)
    //   anything unrecognised         → null (falls back to configured default)
    private fun parseProvider(model: String): String? = when {
        "/" in model              -> model.substringBefore("/").lowercase()
        model.startsWith("claude")   -> "anthropic"
        model.startsWith("gpt-") ||
        model.startsWith("o1")   ||
        model.startsWith("o3")   ||
        model.startsWith("o4")       -> "openai"
        model.startsWith("gemini")   -> "gemini"
        model.startsWith("llama")  ||
        model.startsWith("qwen")   ||
        model.startsWith("mistral")||
        model.startsWith("phi")      -> "ollama"
        else                         -> null
    }

    private fun toSpringMessages(messages: List<OaiMessage>): List<Message> = messages.mapNotNull { msg ->
        when (msg.role) {
            "system"    -> SystemMessage(msg.content ?: return@mapNotNull null)
            "user"      -> UserMessage(msg.content ?: return@mapNotNull null)
            "assistant" -> AssistantMessage(msg.content ?: "")
            else        -> null  // tool messages not yet supported
        }
    }
}

// ── Request DTOs ──────────────────────────────────────────────────────────────
// Field names use underscores to match the JSON wire format directly so Spring's
// Jackson 3.x (tools.jackson.*) deserialises them without needing @JsonProperty.

data class OaiRequest(
    val model: String = "",
    val messages: List<OaiMessage> = emptyList(),
    val stream: Boolean? = null,
    val temperature: Double? = null,
    val max_tokens: Int? = null,
    val tools: List<Any>? = null,
    val tool_choice: Any? = null
)

data class OaiMessage(
    val role: String = "",
    val content: String? = null,
    val name: String? = null,
    val tool_call_id: String? = null,
    val tool_calls: List<Any>? = null
)

// ── Response DTOs ─────────────────────────────────────────────────────────────
// Serialised by the local Jackson 2.x ObjectMapper above.
// @JsonProperty maps Kotlin camelCase fields to OpenAI's snake_case wire names.

data class OaiResponse(
    val id: String,
    @JsonProperty("object") val objectType: String = "chat.completion",
    val created: Long,
    val model: String,
    val choices: List<OaiChoice>,
    val usage: OaiUsage = OaiUsage()
)

data class OaiChoice(
    val index: Int = 0,
    val message: OaiMessage,
    @JsonProperty("finish_reason") val finishReason: String = "stop"
)

data class OaiUsage(
    @JsonProperty("prompt_tokens") val promptTokens: Int = 0,
    @JsonProperty("completion_tokens") val completionTokens: Int = 0,
    @JsonProperty("total_tokens") val totalTokens: Int = 0
)

data class OaiChunk(
    val id: String,
    @JsonProperty("object") val objectType: String = "chat.completion.chunk",
    val created: Long,
    val model: String,
    val choices: List<OaiStreamChoice>
)

data class OaiStreamChoice(
    val index: Int = 0,
    val delta: OaiDelta,
    @JsonProperty("finish_reason") val finishReason: String? = null
)

data class OaiDelta(
    val role: String? = null,
    val content: String? = null
)
