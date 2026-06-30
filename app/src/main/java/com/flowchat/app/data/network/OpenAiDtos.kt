package com.flowchat.app.data.network

import com.flowchat.app.domain.model.ChatRequest
import com.flowchat.app.domain.model.ChatRequestMessage
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
    val thinking: OpenAiThinkingConfig? = null,
    val tools: List<OpenAiTool>? = null,
    @SerialName("tool_choice") val toolChoice: String? = null,
    @SerialName("parallel_tool_calls") val parallelToolCalls: Boolean? = null
)

@Serializable
data class OpenAiChatMessage(
    val role: String,
    val content: String? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_calls") val toolCalls: List<OpenAiToolCall>? = null
)

@Serializable
data class OpenAiThinkingConfig(
    val type: String
)

@Serializable
data class OpenAiTool(
    @EncodeDefault
    val type: String = "function",
    val function: OpenAiToolFunction
)

@Serializable
data class OpenAiToolFunction(
    val name: String,
    val description: String,
    val parameters: kotlinx.serialization.json.JsonObject
)

@Serializable
data class OpenAiToolCall(
    val id: String,
    @EncodeDefault
    val type: String = "function",
    val function: OpenAiToolCallFunction
)

@Serializable
data class OpenAiToolCallFunction(
    val name: String,
    val arguments: String
)

fun ChatRequest.toOpenAiRequest(provider: ProviderConfig? = null): OpenAiChatCompletionRequest = OpenAiChatCompletionRequest(
    model = model,
    messages = messages.map { it.toOpenAiMessage() },
    stream = true,
    temperature = temperature,
    topP = topP,
    maxTokens = maxTokens,
    thinking = provider.deepSeekThinkingConfig(model),
    tools = tools.map { tool ->
        OpenAiTool(
            function = OpenAiToolFunction(
                name = tool.name,
                description = tool.description,
                parameters = tool.parameters
            )
        )
    }.takeIf { it.isNotEmpty() },
    toolChoice = toolChoice,
    parallelToolCalls = parallelToolCalls
)

fun ChatRequest.toOpenAiRequest(provider: ProviderConfig? = null, stream: Boolean): OpenAiChatCompletionRequest = OpenAiChatCompletionRequest(
    model = model,
    messages = messages.map { it.toOpenAiMessage() },
    stream = stream,
    temperature = temperature,
    topP = topP,
    maxTokens = maxTokens,
    thinking = provider.deepSeekThinkingConfig(model),
    tools = tools.map { tool ->
        OpenAiTool(
            function = OpenAiToolFunction(
                name = tool.name,
                description = tool.description,
                parameters = tool.parameters
            )
        )
    }.takeIf { it.isNotEmpty() },
    toolChoice = toolChoice,
    parallelToolCalls = parallelToolCalls
)

private fun ChatRequestMessage.toOpenAiMessage(): OpenAiChatMessage =
    OpenAiChatMessage(
        role = role,
        content = content,
        toolCallId = toolCallId,
        toolCalls = toolCalls.map { call ->
            OpenAiToolCall(
                id = call.id,
                function = OpenAiToolCallFunction(
                    name = call.name,
                    arguments = call.arguments
                )
            )
        }.takeIf { it.isNotEmpty() }
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
