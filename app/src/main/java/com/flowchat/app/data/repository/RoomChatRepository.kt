package com.flowchat.app.data.repository

import com.flowchat.app.data.db.ConversationDao
import com.flowchat.app.data.db.MessageDao
import com.flowchat.app.data.db.toDomain
import com.flowchat.app.data.db.toEntity
import com.flowchat.app.domain.model.Conversation
import com.flowchat.app.domain.model.Message
import com.flowchat.app.domain.model.MessageRole
import com.flowchat.app.domain.model.MessageStatus
import com.flowchat.app.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomChatRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) : ChatRepository {
    override fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeConversations().map { rows -> rows.map { it.toDomain() } }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        messageDao.observeMessages(conversationId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getConversation(id: String): Conversation? =
        conversationDao.getConversation(id)?.toDomain()

    override suspend fun getMessages(conversationId: String): List<Message> =
        messageDao.getMessages(conversationId).map { it.toDomain() }

    override suspend fun createConversation(providerId: String, modelName: String): Conversation {
        val now = System.currentTimeMillis()
        val conversation = Conversation(
            title = "New chat",
            providerId = providerId,
            modelName = modelName,
            createdAt = now,
            updatedAt = now
        )
        conversationDao.upsert(conversation.toEntity())
        return conversation
    }

    override suspend fun updateConversationSettings(
        id: String,
        assistantName: String,
        assistantAvatarPath: String?,
        showAvatars: Boolean,
        enableThinking: Boolean,
        systemPrompt: String,
        temperature: Double,
        topP: Double,
        maxTokens: Int?
    ) {
        conversationDao.updateSettings(
            id,
            assistantName,
            assistantAvatarPath,
            showAvatars,
            enableThinking,
            systemPrompt,
            temperature,
            topP,
            maxTokens,
            System.currentTimeMillis()
        )
    }

    override suspend fun updateConversationModel(id: String, providerId: String, modelName: String) {
        conversationDao.updateModel(id, providerId, modelName, System.currentTimeMillis())
    }

    override suspend fun appendMessage(
        conversationId: String,
        role: MessageRole,
        content: String,
        status: MessageStatus,
        modelName: String?
    ): Message {
        val now = System.currentTimeMillis()
        val message = Message(
            conversationId = conversationId,
            role = role,
            content = content,
            status = status,
            modelName = modelName,
            createdAt = now,
            updatedAt = now
        )
        messageDao.upsert(message.toEntity())
        conversationDao.touch(conversationId, now)
        return message
    }

    override suspend fun updateMessage(
        id: String,
        content: String,
        status: MessageStatus,
        reasoningContent: String
    ) {
        messageDao.updateContentReasoningAndStatus(
            id,
            content,
            reasoningContent,
            status.name,
            System.currentTimeMillis()
        )
    }

    override suspend fun markMessageFailed(id: String, error: String) {
        updateMessage(id, error, MessageStatus.Failed)
    }

    override suspend fun deleteConversation(id: String) {
        conversationDao.delete(id)
    }
}
