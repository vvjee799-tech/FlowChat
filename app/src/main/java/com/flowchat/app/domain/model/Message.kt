package com.flowchat.app.domain.model

import java.util.UUID

enum class MessageRole(val apiRole: String) {
    System("system"),
    User("user"),
    Assistant("assistant"),
    Error("error")
}

enum class MessageStatus {
    Pending,
    Sent,
    Streaming,
    Stopped,
    Complete,
    Failed
}

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val reasoningContent: String = "",
    val status: MessageStatus = MessageStatus.Complete,
    val modelName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)
