package com.flowchat.app.domain.chat

import com.flowchat.app.domain.model.ChatRequest
import com.flowchat.app.domain.model.ChatRequestMessage
import com.flowchat.app.domain.model.Conversation
import com.flowchat.app.domain.model.Message
import com.flowchat.app.domain.model.MessageRole
import com.flowchat.app.domain.model.MessageStatus

object ChatRequestFactory {
    fun create(
        conversation: Conversation,
        messages: List<Message>,
        transientSystemContext: String? = null
    ): ChatRequest {
        val context = transientSystemContext?.trim().orEmpty()
        val conversationMessages = messages
            .filter { it.status != MessageStatus.Failed }
            .filter { it.role == MessageRole.User || it.role == MessageRole.Assistant }
            .filter { it.content.isNotBlank() }
            .map { message ->
                ChatRequestMessage(role = message.role.apiRole, content = message.content)
            }
        val contextInsertIndex = conversationMessages.indexOfLast { it.role == MessageRole.User.apiRole }

        val apiMessages = buildList {
            val prompt = conversation.systemPrompt.trim()
            if (prompt.isNotEmpty()) {
                add(ChatRequestMessage(role = MessageRole.System.apiRole, content = prompt))
            }
            if (context.isNotEmpty() && contextInsertIndex < 0) {
                add(ChatRequestMessage(role = MessageRole.System.apiRole, content = context))
            }
            conversationMessages.forEachIndexed { index, message ->
                if (context.isNotEmpty() && index == contextInsertIndex) {
                    add(ChatRequestMessage(role = MessageRole.System.apiRole, content = context))
                }
                add(message)
            }
        }
        return ChatRequest(
            model = conversation.modelName,
            messages = apiMessages,
            temperature = conversation.temperature,
            topP = conversation.topP,
            maxTokens = conversation.maxTokens,
            enableThinking = conversation.enableThinking
        )
    }
}
