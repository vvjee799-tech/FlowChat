package com.flowchat.app.presentation.chat

import com.flowchat.app.domain.model.Conversation
import com.flowchat.app.domain.model.Message
import com.flowchat.app.domain.model.ProviderConfig

data class ChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val currentConversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val providers: List<ProviderConfig> = emptyList(),
    val input: String = "",
    val webSearchEnabled: Boolean = false,
    val isStreaming: Boolean = false,
    val toolCallStatus: ToolCallStatusUi? = null,
    val errorMessage: String? = null
)

data class ToolCallStatusUi(
    val toolName: String,
    val phase: ToolCallPhase,
    val detail: String? = null
)

enum class ToolCallPhase {
    Running,
    Complete,
    Failed
}
