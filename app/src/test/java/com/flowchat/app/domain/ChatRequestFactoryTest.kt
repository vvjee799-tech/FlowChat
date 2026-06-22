package com.flowchat.app.domain

import com.flowchat.app.domain.chat.ChatRequestFactory
import com.flowchat.app.domain.model.Conversation
import com.flowchat.app.domain.model.Message
import com.flowchat.app.domain.model.MessageRole
import com.flowchat.app.domain.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatRequestFactoryTest {
    @Test
    fun placesSystemPromptBeforeConversationMessages() {
        val request = ChatRequestFactory.create(
            conversation = Conversation(
                id = "c1",
                providerId = "p1",
                modelName = "gpt-4o-mini",
                systemPrompt = "Answer concisely.",
                temperature = 0.4,
                topP = 0.9,
                maxTokens = 512
            ),
            messages = listOf(
                Message(conversationId = "c1", role = MessageRole.User, content = "Hello"),
                Message(conversationId = "c1", role = MessageRole.Assistant, content = "Hi")
            )
        )

        assertEquals("gpt-4o-mini", request.model)
        assertEquals(listOf("system", "user", "assistant"), request.messages.map { it.role })
        assertEquals("Answer concisely.", request.messages.first().content)
        assertEquals(0.4, request.temperature, 0.0)
        assertEquals(0.9, request.topP, 0.0)
        assertEquals(512, request.maxTokens)
    }

    @Test
    fun excludesFailedAssistantMessagesFromContext() {
        val request = ChatRequestFactory.create(
            conversation = Conversation(id = "c1", providerId = "p1", modelName = "deepseek-chat"),
            messages = listOf(
                Message(conversationId = "c1", role = MessageRole.User, content = "Hello"),
                Message(
                    conversationId = "c1",
                    role = MessageRole.Assistant,
                    content = "Network failed",
                    status = MessageStatus.Failed
                )
            )
        )

        assertEquals(listOf("user"), request.messages.map { it.role })
    }

    @Test
    fun insertsTransientWebSearchContextAfterHistoryAndBeforeLatestUserMessage() {
        val request = ChatRequestFactory.create(
            conversation = Conversation(
                id = "c1",
                providerId = "p1",
                modelName = "gpt-4o-mini",
                systemPrompt = "Stay concise."
            ),
            messages = listOf(
                Message(conversationId = "c1", role = MessageRole.User, content = "Can you browse?"),
                Message(conversationId = "c1", role = MessageRole.Assistant, content = "I cannot browse."),
                Message(conversationId = "c1", role = MessageRole.User, content = "What happened today?")
            ),
            transientSystemContext = "Search context for this request only."
        )

        assertEquals(listOf("system", "user", "assistant", "system", "user"), request.messages.map { it.role })
        assertEquals("Stay concise.", request.messages[0].content)
        assertEquals("Can you browse?", request.messages[1].content)
        assertEquals("I cannot browse.", request.messages[2].content)
        assertEquals("Search context for this request only.", request.messages[3].content)
        assertEquals("What happened today?", request.messages[4].content)
    }
}
