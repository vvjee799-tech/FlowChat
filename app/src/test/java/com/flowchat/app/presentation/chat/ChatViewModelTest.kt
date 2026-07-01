package com.flowchat.app.presentation.chat

import com.flowchat.app.data.network.ChatCompletionClient
import com.flowchat.app.data.network.WebSearchClient
import com.flowchat.app.domain.model.AppUsageSummary
import com.flowchat.app.domain.model.ChatDelta
import com.flowchat.app.domain.model.ChatRequest
import com.flowchat.app.domain.model.Conversation
import com.flowchat.app.domain.model.MemoryRecord
import com.flowchat.app.domain.model.Message
import com.flowchat.app.domain.model.MessageRole
import com.flowchat.app.domain.model.MessageStatus
import com.flowchat.app.domain.model.ProviderConfig
import com.flowchat.app.domain.model.RecentAppActivity
import com.flowchat.app.domain.model.WebSearchResult
import com.flowchat.app.domain.prompt.PromptProfileConfig
import com.flowchat.app.domain.repository.AppUsageReader
import com.flowchat.app.domain.repository.ChatRepository
import com.flowchat.app.domain.repository.MemoryRepository
import com.flowchat.app.domain.repository.PromptProfileRepository
import com.flowchat.app.domain.repository.ProviderRepository
import com.flowchat.app.domain.repository.WebSearchSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun sendingImmediatelyAfterNewConversationUsesNewConversationIdEvenBeforeListRefresh() = runTest(dispatcher) {
        val provider = ProviderConfig(
            id = "provider-1",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-pro"
        )
        val oldConversation = Conversation(
            id = "old-conversation",
            title = "Old",
            providerId = provider.id,
            modelName = provider.defaultModel,
            updatedAt = 1L
        )
        val newConversation = Conversation(
            id = "new-conversation",
            title = "New",
            providerId = provider.id,
            modelName = provider.defaultModel,
            updatedAt = 2L
        )
        val chatRepository = FakeChatRepository(
            initialConversations = listOf(oldConversation),
            nextConversation = newConversation
        )
        val viewModel = ChatViewModel(
            chatRepository = chatRepository,
            providerRepository = FakeProviderRepository(provider),
            chatClient = FakeChatCompletionClient(),
            webSearchClient = FakeWebSearchClient(),
            webSearchSettingsRepository = FakeWebSearchSettingsRepository(),
            appUsageReader = FakeAppUsageReader(),
            promptProfileRepository = FakePromptProfileRepository(),
            memoryRepository = FakeMemoryRepository()
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.newConversation()
        advanceUntilIdle()
        viewModel.updateInput("hello new chat")
        viewModel.send()
        advanceUntilIdle()

        assertTrue(chatRepository.messagesFor("new-conversation").any { it.role == MessageRole.User && it.content == "hello new chat" })
        assertFalse(chatRepository.messagesFor("old-conversation").any { it.role == MessageRole.User && it.content == "hello new chat" })
        collection.cancel()
    }

    @Test
    fun sendingAfterModelSwitchUsesSelectedModelInRequestAndSavedMessages() = runTest(dispatcher) {
        val provider = ProviderConfig(
            id = "provider-1",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-pro"
        )
        val conversation = Conversation(
            id = "conversation-1",
            title = "Model switch",
            providerId = provider.id,
            modelName = "deepseek-v4-pro"
        )
        val chatRepository = FakeChatRepository(
            initialConversations = listOf(conversation),
            nextConversation = conversation
        )
        val chatClient = FakeChatCompletionClient()
        val viewModel = ChatViewModel(
            chatRepository = chatRepository,
            providerRepository = FakeProviderRepository(provider),
            chatClient = chatClient,
            webSearchClient = FakeWebSearchClient(),
            webSearchSettingsRepository = FakeWebSearchSettingsRepository(),
            appUsageReader = FakeAppUsageReader(),
            promptProfileRepository = FakePromptProfileRepository(),
            memoryRepository = FakeMemoryRepository()
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.updateConversationModel(provider, "deepseek-v4-flash")
        advanceUntilIdle()
        viewModel.updateInput("use the switched model")
        viewModel.send()
        advanceUntilIdle()

        assertEquals("deepseek-v4-flash", chatClient.requests.single().model)
        assertTrue(chatRepository.messagesFor("conversation-1")
            .filter { it.role == MessageRole.User || it.role == MessageRole.Assistant }
            .all { it.modelName == "deepseek-v4-flash" })
        collection.cancel()
    }

    private class FakeChatRepository(
        initialConversations: List<Conversation>,
        private val nextConversation: Conversation
    ) : ChatRepository {
        private val conversations = linkedMapOf<String, Conversation>()
        private val messages = linkedMapOf<String, MutableList<Message>>()
        private val conversationsFlow = MutableStateFlow(initialConversations)

        init {
            initialConversations.forEach { conversation ->
                conversations[conversation.id] = conversation
                messages[conversation.id] = mutableListOf()
            }
        }

        override fun observeConversations(): Flow<List<Conversation>> = conversationsFlow

        override fun observeMessages(conversationId: String): Flow<List<Message>> =
            flowOf(messagesFor(conversationId))

        override suspend fun getConversation(id: String): Conversation? = conversations[id]

        override suspend fun getMessages(conversationId: String): List<Message> =
            messagesFor(conversationId)

        override suspend fun createConversation(providerId: String, modelName: String): Conversation {
            conversations[nextConversation.id] = nextConversation
            messages[nextConversation.id] = mutableListOf()
            return nextConversation
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
        ) = Unit

        override suspend fun updateConversationModel(id: String, providerId: String, modelName: String) {
            val current = conversations[id] ?: return
            val updated = current.copy(providerId = providerId, modelName = modelName)
            conversations[id] = updated
            conversationsFlow.value = conversations.values.sortedByDescending { it.updatedAt }
        }

        override suspend fun appendMessage(
            conversationId: String,
            role: MessageRole,
            content: String,
            status: MessageStatus,
            modelName: String?
        ): Message {
            val message = Message(
                conversationId = conversationId,
                role = role,
                content = content,
                status = status,
                modelName = modelName
            )
            messages.getOrPut(conversationId) { mutableListOf() }.add(message)
            return message
        }

        override suspend fun updateMessage(
            id: String,
            content: String,
            status: MessageStatus,
            reasoningContent: String,
            reasoningDurationMillis: Long?
        ) {
            messages.values.flatten().firstOrNull { it.id == id } ?: return
        }

        override suspend fun markMessageFailed(id: String, error: String) = Unit

        override suspend fun deleteConversation(id: String) {
            conversations.remove(id)
            messages.remove(id)
        }

        fun messagesFor(conversationId: String): List<Message> =
            messages[conversationId].orEmpty()
    }

    private class FakeProviderRepository(
        private val provider: ProviderConfig
    ) : ProviderRepository {
        override fun observeProviders(): Flow<List<ProviderConfig>> = flowOf(listOf(provider))
        override suspend fun getProvider(id: String): ProviderConfig? = provider.takeIf { it.id == id }
        override suspend fun getProvidersOnce(): List<ProviderConfig> = listOf(provider)
        override suspend fun upsertProvider(config: ProviderConfig, apiKey: String?) = Unit
        override suspend fun deleteProvider(id: String) = Unit
        override suspend fun ensureTemplates() = Unit
        override suspend fun getApiKey(config: ProviderConfig): String? = "api-key"
    }

    private class FakeChatCompletionClient : ChatCompletionClient {
        val requests = mutableListOf<ChatRequest>()

        override fun streamChat(request: ChatRequest, provider: ProviderConfig, apiKey: String?): Flow<ChatDelta> =
            flowOf(ChatDelta.Content("ok"), ChatDelta.Done).also {
                requests += request
            }
    }

    private class FakeWebSearchClient : WebSearchClient {
        override suspend fun search(query: String, apiKey: String): WebSearchResult =
            WebSearchResult(query = query, results = emptyList())
    }

    private class FakeWebSearchSettingsRepository : WebSearchSettingsRepository {
        override suspend fun saveTavilyApiKey(apiKey: String) = Unit
        override suspend fun getTavilyApiKey(): String? = null
        override suspend fun hasTavilyApiKey(): Boolean = false
    }

    private class FakeAppUsageReader : AppUsageReader {
        override fun hasUsageAccess(): Boolean = false
        override suspend fun getUsageSummary(range: String): AppUsageSummary =
            AppUsageSummary(
                range = range,
                startTimeMillis = 0L,
                endTimeMillis = 0L,
                totalForegroundTimeMillis = 0L,
                items = emptyList()
            )
        override suspend fun getRecentActivity(hours: Int): RecentAppActivity =
            RecentAppActivity(hours = hours, events = emptyList())
    }

    private class FakePromptProfileRepository : PromptProfileRepository {
        override suspend fun getActiveProfile(): PromptProfileConfig = PromptProfileConfig()
        override suspend fun getActiveThinkingFormat(): String? = null
    }

    private class FakeMemoryRepository : MemoryRepository {
        override suspend fun retrieve(userMessage: String, topN: Int): List<MemoryRecord> = emptyList()
        override suspend fun saveTurn(userMessage: String, assistantReply: String) = Unit
    }
}
