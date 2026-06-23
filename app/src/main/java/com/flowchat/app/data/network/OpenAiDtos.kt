package com.flowchat.app.data.network

import com.flowchat.app.domain.model.ChatRequest
import com.flowchat.app.domain.model.ProviderConfig
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class OpenAiChatCompletionRequest(
    val model: String,
    val messages: List<OpenAiChatMessage>,
    @EncodeDefault
    val stream: Boolean = true,
    val temperature: Double,
    @SerialName("top_p") val topP: Double,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val thinking: OpenAiThinkingConfig? = null
)

@Serializable
data class OpenAiChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiThinkingConfig(
    val type: String
)

fun ChatRequest.toOpenAiRequest(provider: ProviderConfig? = null): OpenAiChatCompletionRequest = OpenAiChatCompletionRequest(
    model = model,
    messages = messages.map { OpenAiChatMessage(role = it.role, content = it.content) },
    stream = true,
    temperature = temperature,
    topP = topP,
    maxTokens = maxTokens,
    thinking = provider.deepSeekThinkingConfig(model)
)

fun ChatRequest.toOpenAiRequest(provider: ProviderConfig? = null, stream: Boolean): OpenAiChatCompletionRequest = OpenAiChatCompletionRequest(
    model = model,
    messages = messages.map { OpenAiChatMessage(role = it.role, content = it.content) },
    stream = stream,
    temperature = temperature,
    topP = topP,
    maxTokens = maxTokens,
    thinking = provider.deepSeekThinkingConfig(model)
)

private fun ProviderConfig?.deepSeekThinkingConfig(model: String): OpenAiThinkingConfig? {
    if (this == null) return null
    val isDeepSeekApi = baseUrl.contains("api.deepseek.com", ignoreCase = true)
    val isDeepSeekV4 = model.startsWith("deepseek-v4", ignoreCase = true)
    return if (isDeepSeekApi && isDeepSeekV4) {
        OpenAiThinkingConfig(type = "enabled")
    } else {
        null
    }
}
