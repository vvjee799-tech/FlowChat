package com.flowchat.app.domain.repository

import com.flowchat.app.domain.model.Conversation
import com.flowchat.app.domain.model.Message
import com.flowchat.app.domain.model.MessageRole
import com.flowchat.app.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeConversations(): Flow<List<Conversation>>
    fun observeMessages(conversationId: String): Flow<List<Message>>
    suspend fun getConversation(id: String): Conversation?
    suspend fun getMessages(conversationId: String): List<Message>
    suspend fun createConversation(providerId: String, modelName: String): Conversation
    suspend fun updateConversationSettings(
        id: String,
        assistantName: String,
        assistantAvatarPath: String?,
        showAvatars: Boolean,
        enableThinking: Boolean,
        systemPrompt: String,
        temperature: Double,
        topP: Double,
        maxTokens: Int?
    )
    suspend fun updateConversationModel(id: String, providerId: String, modelName: String)
    suspend fun updateConversationTitle(id: String, title: String)
    suspend fun appendMessage(
        conversationId: String,
        role: MessageRole,
        content: String,
        status: MessageStatus,
        modelName: String?,
        attachmentName: String? = null,
        attachmentText: String? = null
    ): Message
    suspend fun updateMessage(
        id: String,
        content: String,
        status: MessageStatus,
        reasoningContent: String = "",
        reasoningDurationMillis: Long? = null
    )
    suspend fun markMessageFailed(id: String, error: String)
    suspend fun deleteConversation(id: String)
}
