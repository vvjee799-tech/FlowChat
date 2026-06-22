package com.flowchat.app.domain.model

data class ChatRequest(
    val model: String,
    val messages: List<ChatRequestMessage>,
    val temperature: Double,
    val topP: Double,
    val maxTokens: Int?,
    val enableThinking: Boolean = false
)

data class ChatRequestMessage(
    val role: String,
    val content: String
)

sealed interface ChatDelta {
    data class Content(val text: String) : ChatDelta
    data class Reasoning(val text: String) : ChatDelta
    data class FullResponse(val reasoningText: String, val contentText: String) : ChatDelta
    data object Done : ChatDelta
    data class Error(val message: String) : ChatDelta
}
