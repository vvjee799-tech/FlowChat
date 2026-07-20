package com.flowchat.app.presentation.chat

import com.flowchat.app.data.network.ChatCompletionClient
import com.flowchat.app.data.network.WebSearchClient
import com.flowchat.app.data.preferences.AppSettings
import com.flowchat.app.data.preferences.AppSettingsStore
import com.flowchat.app.domain.model.AppUsageSummary
import com.flowchat.app.domain.model.ChatDelta
import com.flowchat.app.domain.model.ChatRequest
import com.flowchat.app.domain.model.ChatToolCall
import com.flowchat.app.domain.model.Conversation
import com.flowchat.app.domain.model.MemoryRecord
import com.flowchat.app.domain.model.Message
import com.flowchat.app.domain.model.MessageRole
import com.flowchat.app.domain.model.MessageStatus
import com.flowchat.app.domain.model.ProviderConfig
import com.flowchat.app.domain.model.RecentAppActivity
import com.flowchat.app.domain.model.WebSearchResult
import com.flowchat.app.domain.device.DeviceAssistantGateway
import com.flowchat.app.domain.device.AccessibilityConnectionState
import com.flowchat.app.domain.device.AccessibilityConnectionStatus
import com.flowchat.app.domain.device.DeviceCapability
import com.flowchat.app.domain.device.DeviceBounds
import com.flowchat.app.domain.device.DeviceScreenSnapshot
import com.flowchat.app.domain.device.DeviceSwipeDirection
import com.flowchat.app.domain.device.DeviceToolResult
import com.flowchat.app.domain.device.DeviceUiElement
import com.flowchat.app.domain.device.ShizukuConnectionState
import com.flowchat.app.domain.device.ShizukuConnectionStatus
import com.flowchat.app.domain.prompt.PromptProfileConfig
import com.flowchat.app.domain.provider.ProviderTemplates
import com.flowchat.app.domain.repository.AppUsageReader
import com.flowchat.app.domain.repository.AppLauncher
import com.flowchat.app.domain.repository.ChatRepository
import com.flowchat.app.domain.repository.MemoryRepository
import com.flowchat.app.domain.repository.PromptProfileRepository
import com.flowchat.app.domain.repository.ProviderRepository
import com.flowchat.app.domain.repository.WebSearchSettingsRepository
import com.flowchat.app.domain.tools.AgentToolDefinitions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
            appLauncher = FakeAppLauncher(),
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
    fun newConversationUsesSavedValidProviderInsteadOfBlankCustomDraft() = runTest(dispatcher) {
        val customDraft = ProviderTemplates.defaultCustomProvider()
        val deepSeek = ProviderConfig(
            id = "provider-deepseek",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-flash",
            apiKeyAlias = "provider:provider-deepseek"
        )
        val newConversation = Conversation(
            id = "new-conversation",
            title = "New",
            providerId = deepSeek.id,
            modelName = deepSeek.defaultModel,
            updatedAt = 2L
        )
        val chatRepository = FakeChatRepository(
            initialConversations = emptyList(),
            nextConversation = newConversation
        )
        val viewModel = ChatViewModel(
            chatRepository = chatRepository,
            providerRepository = FakeProviderRepository(
                providers = listOf(customDraft, deepSeek),
                apiKeys = mapOf(deepSeek.id to "deepseek-key")
            ),
            chatClient = FakeChatCompletionClient(),
            webSearchClient = FakeWebSearchClient(),
            webSearchSettingsRepository = FakeWebSearchSettingsRepository(),
            appUsageReader = FakeAppUsageReader(),
            appLauncher = FakeAppLauncher(),
            promptProfileRepository = FakePromptProfileRepository(),
            memoryRepository = FakeMemoryRepository()
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.newConversation()
        advanceUntilIdle()

        assertEquals("provider-deepseek", chatRepository.lastCreateProviderId)
        assertEquals("deepseek-v4-flash", chatRepository.lastCreateModelName)
        assertEquals("new-conversation", viewModel.uiState.value.currentConversation?.id)
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
            appLauncher = FakeAppLauncher(),
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

    @Test
    fun toolFollowUpReplaysAssistantContentAndReasoning() = runTest(dispatcher) {
        val provider = ProviderConfig(
            id = "provider-1",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-pro"
        )
        val conversation = Conversation(
            id = "conversation-1",
            title = "Tool follow-up",
            providerId = provider.id,
            modelName = provider.defaultModel
        )
        val chatRepository = FakeChatRepository(
            initialConversations = listOf(conversation),
            nextConversation = conversation
        )
        val chatClient = FakeChatCompletionClient(
            scriptedResponses = listOf(
                listOf(
                    ChatDelta.Reasoning("I need the user's usage summary."),
                    ChatDelta.Content("I will check."),
                    ChatDelta.ToolCalls(
                        listOf(
                            ChatToolCall(
                                id = "call_1",
                                name = AgentToolDefinitions.AppUsageSummaryToolName,
                                arguments = """{"range":"today"}"""
                            )
                        )
                    )
                ),
                listOf(ChatDelta.Content("Here is the result."), ChatDelta.Done)
            )
        )
        val viewModel = ChatViewModel(
            chatRepository = chatRepository,
            providerRepository = FakeProviderRepository(provider),
            chatClient = chatClient,
            webSearchClient = FakeWebSearchClient(),
            webSearchSettingsRepository = FakeWebSearchSettingsRepository(),
            appUsageReader = FakeAppUsageReader(),
            appLauncher = FakeAppLauncher(),
            promptProfileRepository = FakePromptProfileRepository(),
            memoryRepository = FakeMemoryRepository()
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.updateInput("What did I use today?")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(2, chatClient.requests.size)
        val assistantToolMessage = chatClient.requests[1].messages
            .last { it.role == MessageRole.Assistant.apiRole && it.toolCalls.isNotEmpty() }
        assertEquals("I will check.", assistantToolMessage.content)
        assertEquals("I need the user's usage summary.", assistantToolMessage.reasoningContent)
        collection.cancel()
    }

    @Test
    fun openAppToolLaunchesRequestedAppAndReturnsTheResultToTheModel() = runTest(dispatcher) {
        val provider = ProviderConfig(
            id = "provider-1",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-pro"
        )
        val conversation = Conversation(
            id = "conversation-1",
            title = "Open app",
            providerId = provider.id,
            modelName = provider.defaultModel
        )
        val chatRepository = FakeChatRepository(
            initialConversations = listOf(conversation),
            nextConversation = conversation
        )
        val chatClient = FakeChatCompletionClient(
            scriptedResponses = listOf(
                listOf(
                    ChatDelta.Content("I will open it."),
                    ChatDelta.ToolCalls(
                        listOf(
                            ChatToolCall(
                                id = "call_open",
                                name = AgentToolDefinitions.OpenAppToolName,
                                arguments = """{"app_name":"Settings"}"""
                            )
                        )
                    )
                ),
                listOf(ChatDelta.Content("Settings is open."), ChatDelta.Done)
            )
        )
        val appLauncher = FakeAppLauncher()
        val viewModel = ChatViewModel(
            chatRepository = chatRepository,
            providerRepository = FakeProviderRepository(provider),
            chatClient = chatClient,
            webSearchClient = FakeWebSearchClient(),
            webSearchSettingsRepository = FakeWebSearchSettingsRepository(),
            appUsageReader = FakeAppUsageReader(),
            appLauncher = appLauncher,
            promptProfileRepository = FakePromptProfileRepository(),
            memoryRepository = FakeMemoryRepository()
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.updateInput("Open Settings")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(listOf("Settings"), appLauncher.openedApps)
        val toolResult = chatClient.requests[1].messages.last { it.role == "tool" }
        assertEquals("Opened app: Settings.", toolResult.content)
        assertTrue(
            chatRepository.messagesFor(conversation.id)
                .any { it.role == MessageRole.Tool && it.content == "open_app:Settings" }
        )
        collection.cancel()
    }

    @Test
    fun forceStopToolWaitsForConfirmationBeforeExecuting() = runTest(dispatcher) {
        val provider = ProviderConfig(
            id = "provider-1",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-pro"
        )
        val conversation = Conversation(
            id = "conversation-1",
            title = "Device assistant",
            providerId = provider.id,
            modelName = provider.defaultModel
        )
        val chatClient = FakeChatCompletionClient(
            scriptedResponses = listOf(
                listOf(
                    ChatDelta.Content("I can stop it after you confirm."),
                    ChatDelta.ToolCalls(
                        listOf(
                            ChatToolCall(
                                id = "call_force_stop",
                                name = AgentToolDefinitions.ForceStopAppToolName,
                                arguments = """{"app_name":"Example"}"""
                            )
                        )
                    )
                ),
                listOf(ChatDelta.Content("The app has been stopped."), ChatDelta.Done)
            )
        )
        val deviceGateway = FakeDeviceAssistantGateway()
        val viewModel = ChatViewModel(
            chatRepository = FakeChatRepository(listOf(conversation), conversation),
            providerRepository = FakeProviderRepository(provider),
            chatClient = chatClient,
            webSearchClient = FakeWebSearchClient(),
            webSearchSettingsRepository = FakeWebSearchSettingsRepository(),
            appUsageReader = FakeAppUsageReader(),
            appLauncher = FakeAppLauncher(),
            deviceAssistantGateway = deviceGateway,
            promptProfileRepository = FakePromptProfileRepository(),
            memoryRepository = FakeMemoryRepository(),
            appSettingsStore = AppSettingsStore(
                AppSettings(
                    powerModeEnabled = true,
                    installId = "test-install"
                )
            )
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.updateInput("Stop Example")
        viewModel.send()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.pendingDeviceActionConfirmation != null)
        assertTrue(deviceGateway.forceStoppedApps.isEmpty())

        viewModel.confirmDeviceAction()
        advanceUntilIdle()

        assertEquals(listOf("Example"), deviceGateway.forceStoppedApps)
        assertEquals(2, chatClient.requests.size)
        assertTrue(viewModel.uiState.value.pendingDeviceActionConfirmation == null)
        collection.cancel()
    }

    @Test
    fun cancelledForceStopMarksTheVisibleToolMessageCancelled() = runTest(dispatcher) {
        val provider = ProviderConfig(
            id = "provider-1",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-pro"
        )
        val conversation = Conversation(
            id = "conversation-1",
            title = "Device assistant",
            providerId = provider.id,
            modelName = provider.defaultModel
        )
        val chatRepository = FakeChatRepository(listOf(conversation), conversation)
        val chatClient = FakeChatCompletionClient(
            scriptedResponses = listOf(
                listOf(
                    ChatDelta.Content("I will stop it after confirmation."),
                    ChatDelta.ToolCalls(
                        listOf(
                            ChatToolCall(
                                id = "call_force_stop",
                                name = AgentToolDefinitions.ForceStopAppToolName,
                                arguments = """{"app_name":"Example"}"""
                            )
                        )
                    )
                ),
                listOf(ChatDelta.Content("The action was cancelled."), ChatDelta.Done)
            )
        )
        val deviceGateway = FakeDeviceAssistantGateway()
        val viewModel = ChatViewModel(
            chatRepository = chatRepository,
            providerRepository = FakeProviderRepository(provider),
            chatClient = chatClient,
            webSearchClient = FakeWebSearchClient(),
            webSearchSettingsRepository = FakeWebSearchSettingsRepository(),
            appUsageReader = FakeAppUsageReader(),
            appLauncher = FakeAppLauncher(),
            deviceAssistantGateway = deviceGateway,
            promptProfileRepository = FakePromptProfileRepository(),
            memoryRepository = FakeMemoryRepository(),
            appSettingsStore = AppSettingsStore(
                AppSettings(
                    powerModeEnabled = true,
                    installId = "test-install"
                )
            )
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.updateInput("Stop Example")
        viewModel.send()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.pendingDeviceActionConfirmation != null)
        viewModel.cancelDeviceAction()
        advanceUntilIdle()

        val toolMessage = chatRepository.messagesFor(conversation.id)
            .single { it.role == MessageRole.Tool }
        assertEquals(MessageStatus.Cancelled, toolMessage.status)
        assertTrue(deviceGateway.forceStoppedApps.isEmpty())
        assertEquals(2, chatClient.requests.size)
        collection.cancel()
    }

    @Test
    fun onlyOffersLifestyleToolsEnabledByUserSettings() = runTest(dispatcher) {
        val provider = ProviderConfig(
            id = "provider-1",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-pro"
        )
        val conversation = Conversation(
            id = "conversation-1",
            title = "Private tools",
            providerId = provider.id,
            modelName = provider.defaultModel
        )
        val chatClient = FakeChatCompletionClient()
        val viewModel = ChatViewModel(
            chatRepository = FakeChatRepository(listOf(conversation), conversation),
            providerRepository = FakeProviderRepository(provider),
            chatClient = chatClient,
            webSearchClient = FakeWebSearchClient(),
            webSearchSettingsRepository = FakeWebSearchSettingsRepository(),
            appUsageReader = FakeAppUsageReader(),
            appLauncher = FakeAppLauncher(),
            promptProfileRepository = FakePromptProfileRepository(),
            memoryRepository = FakeMemoryRepository(),
            appSettingsStore = AppSettingsStore(
                AppSettings(
                    memoryEnabled = false,
                    powerModeEnabled = false,
                    installId = "test-install"
                )
            )
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.updateInput("hello")
        viewModel.send()
        advanceUntilIdle()

        assertTrue(chatClient.requests.single().tools.isEmpty())
        collection.cancel()
    }

    @Test
    fun uiAutomationCanObserveAndActAcrossMoreThanTwoToolRounds() = runTest(dispatcher) {
        val provider = ProviderConfig(
            id = "provider-1",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-pro"
        )
        val conversation = Conversation(
            id = "conversation-1",
            title = "Device task",
            providerId = provider.id,
            modelName = provider.defaultModel
        )
        val chatClient = FakeChatCompletionClient(
            scriptedResponses = listOf(
                listOf(ChatDelta.ToolCalls(listOf(ChatToolCall("observe", AgentToolDefinitions.ObserveScreenToolName, "{}")))),
                listOf(ChatDelta.ToolCalls(listOf(ChatToolCall("tap", AgentToolDefinitions.TapUiElementToolName, "{\"index\":2,\"long_press\":false}")))),
                listOf(ChatDelta.ToolCalls(listOf(ChatToolCall("back", AgentToolDefinitions.PressBackToolName, "{}")))),
                listOf(ChatDelta.Content("已经替你完成。"), ChatDelta.Done)
            )
        )
        val deviceGateway = FakeDeviceAssistantGateway()
        val viewModel = ChatViewModel(
            chatRepository = FakeChatRepository(listOf(conversation), conversation),
            providerRepository = FakeProviderRepository(provider),
            chatClient = chatClient,
            webSearchClient = FakeWebSearchClient(),
            webSearchSettingsRepository = FakeWebSearchSettingsRepository(),
            appUsageReader = FakeAppUsageReader(),
            appLauncher = FakeAppLauncher(),
            deviceAssistantGateway = deviceGateway,
            promptProfileRepository = FakePromptProfileRepository(),
            memoryRepository = FakeMemoryRepository(),
            appSettingsStore = AppSettingsStore(
                AppSettings(powerModeEnabled = true, installId = "test-install")
            )
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.updateInput("打开设置看看，然后返回")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(4, chatClient.requests.size)
        assertEquals(1, deviceGateway.observeCount)
        assertEquals(listOf(2), deviceGateway.tappedIndexes)
        assertEquals(1, deviceGateway.backCount)
        assertTrue(chatClient.requests.first().tools.any { it.name == AgentToolDefinitions.ObserveScreenToolName })
        collection.cancel()
    }

    @Test
    fun uiAutomationAllowsRepeatedObservationAfterInterveningActions() = runTest(dispatcher) {
        val provider = ProviderConfig(
            id = "provider-1",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-pro"
        )
        val conversation = Conversation(
            id = "conversation-1",
            title = "Repeated observation",
            providerId = provider.id,
            modelName = provider.defaultModel
        )
        val scriptedResponses = listOf(
            listOf(ChatDelta.ToolCalls(listOf(ChatToolCall("observe-1", AgentToolDefinitions.ObserveScreenToolName, "{}")))),
            listOf(ChatDelta.ToolCalls(listOf(ChatToolCall("tap-1", AgentToolDefinitions.TapUiElementToolName, "{\"index\":1}")))),
            listOf(ChatDelta.ToolCalls(listOf(ChatToolCall("observe-2", AgentToolDefinitions.ObserveScreenToolName, "{}")))),
            listOf(ChatDelta.ToolCalls(listOf(ChatToolCall("tap-2", AgentToolDefinitions.TapUiElementToolName, "{\"index\":2}")))),
            listOf(ChatDelta.ToolCalls(listOf(ChatToolCall("observe-3", AgentToolDefinitions.ObserveScreenToolName, "{}")))),
            listOf(ChatDelta.Content("Task complete."), ChatDelta.Done)
        )
        val chatClient = FakeChatCompletionClient(scriptedResponses)
        val deviceGateway = FakeDeviceAssistantGateway()
        val viewModel = ChatViewModel(
            chatRepository = FakeChatRepository(listOf(conversation), conversation),
            providerRepository = FakeProviderRepository(provider),
            chatClient = chatClient,
            webSearchClient = FakeWebSearchClient(),
            webSearchSettingsRepository = FakeWebSearchSettingsRepository(),
            appUsageReader = FakeAppUsageReader(),
            appLauncher = FakeAppLauncher(),
            deviceAssistantGateway = deviceGateway,
            promptProfileRepository = FakePromptProfileRepository(),
            memoryRepository = FakeMemoryRepository(),
            appSettingsStore = AppSettingsStore(
                AppSettings(powerModeEnabled = true, installId = "test-install")
            )
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.updateInput("Complete a multi-screen task")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(3, deviceGateway.observeCount)
        assertEquals(listOf(1, 2), deviceGateway.tappedIndexes)
        assertEquals(6, chatClient.requests.size)
        collection.cancel()
    }

    @Test
    fun uiAutomationSupportsLongTasksBeyondEightToolRounds() = runTest(dispatcher) {
        val provider = ProviderConfig(
            id = "provider-1",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-pro"
        )
        val conversation = Conversation(
            id = "conversation-1",
            title = "Long device task",
            providerId = provider.id,
            modelName = provider.defaultModel
        )
        val toolResponses = (0 until 10).map { index ->
            listOf(
                ChatDelta.ToolCalls(
                    listOf(
                        ChatToolCall(
                            id = "tap-$index",
                            name = AgentToolDefinitions.TapUiElementToolName,
                            arguments = "{\"index\":$index}"
                        )
                    )
                )
            )
        }
        val chatClient = FakeChatCompletionClient(
            scriptedResponses = toolResponses + listOf(
                listOf(ChatDelta.Content("Long task complete."), ChatDelta.Done)
            )
        )
        val deviceGateway = FakeDeviceAssistantGateway()
        val viewModel = ChatViewModel(
            chatRepository = FakeChatRepository(listOf(conversation), conversation),
            providerRepository = FakeProviderRepository(provider),
            chatClient = chatClient,
            webSearchClient = FakeWebSearchClient(),
            webSearchSettingsRepository = FakeWebSearchSettingsRepository(),
            appUsageReader = FakeAppUsageReader(),
            appLauncher = FakeAppLauncher(),
            deviceAssistantGateway = deviceGateway,
            promptProfileRepository = FakePromptProfileRepository(),
            memoryRepository = FakeMemoryRepository(),
            appSettingsStore = AppSettingsStore(
                AppSettings(powerModeEnabled = true, installId = "test-install")
            )
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.updateInput("Complete a long sequence of screen actions")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(11, chatClient.requests.size)
        assertEquals((0 until 10).toList(), deviceGateway.tappedIndexes)
        assertFalse(viewModel.uiState.value.errorMessage == "Tool call limit reached.")
        collection.cancel()
    }

    @Test
    fun webSearchRequiresOneTimeDisclosureBeforeItIsEnabled() = runTest(dispatcher) {
        val provider = ProviderConfig(
            id = "provider-1",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-pro"
        )
        val conversation = Conversation(
            id = "conversation-1",
            title = "Search disclosure",
            providerId = provider.id,
            modelName = provider.defaultModel
        )
        val viewModel = ChatViewModel(
            chatRepository = FakeChatRepository(listOf(conversation), conversation),
            providerRepository = FakeProviderRepository(provider),
            chatClient = FakeChatCompletionClient(),
            webSearchClient = FakeWebSearchClient(),
            webSearchSettingsRepository = FakeWebSearchSettingsRepository(),
            appUsageReader = FakeAppUsageReader(),
            appLauncher = FakeAppLauncher(),
            promptProfileRepository = FakePromptProfileRepository(),
            memoryRepository = FakeMemoryRepository(),
            appSettingsStore = AppSettingsStore(AppSettings(installId = "test-install"))
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleWebSearch()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.webSearchEnabled)
        assertTrue(viewModel.uiState.value.webSearchDisclosureRequired)

        viewModel.acceptWebSearchDisclosure()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.webSearchEnabled)
        assertFalse(viewModel.uiState.value.webSearchDisclosureRequired)
        collection.cancel()
    }

    @Test
    fun refusesToSendApiKeysOrMessagesToAnInsecurePublicProvider() = runTest(dispatcher) {
        val provider = ProviderConfig(
            id = "provider-1",
            displayName = "Unsafe",
            baseUrl = "http://api.example.com/v1",
            defaultModel = "model"
        )
        val conversation = Conversation(
            id = "conversation-1",
            title = "Unsafe",
            providerId = provider.id,
            modelName = provider.defaultModel
        )
        val chatClient = FakeChatCompletionClient()
        val viewModel = ChatViewModel(
            chatRepository = FakeChatRepository(listOf(conversation), conversation),
            providerRepository = FakeProviderRepository(provider),
            chatClient = chatClient,
            webSearchClient = FakeWebSearchClient(),
            webSearchSettingsRepository = FakeWebSearchSettingsRepository(),
            appUsageReader = FakeAppUsageReader(),
            appLauncher = FakeAppLauncher(),
            promptProfileRepository = FakePromptProfileRepository(),
            memoryRepository = FakeMemoryRepository()
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.updateInput("do not send this")
        viewModel.send()
        advanceUntilIdle()

        assertTrue(chatClient.requests.isEmpty())
        assertEquals("Provider configuration is invalid.", viewModel.uiState.value.errorMessage)
        collection.cancel()
    }

    private class FakeChatRepository(
        initialConversations: List<Conversation>,
        private val nextConversation: Conversation
    ) : ChatRepository {
        private val conversations = linkedMapOf<String, Conversation>()
        private val messages = linkedMapOf<String, MutableList<Message>>()
        private val conversationsFlow = MutableStateFlow(initialConversations)
        var lastCreateProviderId: String? = null
            private set
        var lastCreateModelName: String? = null
            private set

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
            lastCreateProviderId = providerId
            lastCreateModelName = modelName
            val created = nextConversation.copy(providerId = providerId, modelName = modelName)
            conversations[created.id] = created
            messages[nextConversation.id] = mutableListOf()
            conversationsFlow.value = conversations.values.sortedByDescending { it.updatedAt }
            return created
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

        override suspend fun updateConversationTitle(id: String, title: String) {
            val current = conversations[id] ?: return
            conversations[id] = current.copy(title = title)
        }

        override suspend fun appendMessage(
            conversationId: String,
            role: MessageRole,
            content: String,
            status: MessageStatus,
            modelName: String?,
            attachmentName: String?,
            attachmentText: String?
        ): Message {
            val message = Message(
                conversationId = conversationId,
                role = role,
                content = content,
                attachmentName = attachmentName,
                attachmentText = attachmentText,
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
            messages.values.forEach { conversationMessages ->
                val index = conversationMessages.indexOfFirst { it.id == id }
                if (index >= 0) {
                    val current = conversationMessages[index]
                    conversationMessages[index] = current.copy(
                        content = content,
                        status = status,
                        reasoningContent = reasoningContent,
                        reasoningDurationMillis = reasoningDurationMillis
                            ?: current.reasoningDurationMillis,
                        updatedAt = System.currentTimeMillis()
                    )
                    return
                }
            }
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
        private val providers: List<ProviderConfig>,
        private val apiKeys: Map<String, String> = providers.associate { it.id to "api-key" }
    ) : ProviderRepository {
        constructor(provider: ProviderConfig) : this(listOf(provider))

        override fun observeProviders(): Flow<List<ProviderConfig>> = flowOf(providers)
        override suspend fun getProvider(id: String): ProviderConfig? = providers.firstOrNull { it.id == id }
        override suspend fun getProvidersOnce(): List<ProviderConfig> = providers
        override suspend fun upsertProvider(config: ProviderConfig, apiKey: String?) = Unit
        override suspend fun deleteProvider(id: String) = Unit
        override suspend fun ensureTemplates() = Unit
        override suspend fun getApiKey(config: ProviderConfig): String? = apiKeys[config.id]
    }

    private class FakeChatCompletionClient(
        private val scriptedResponses: List<List<ChatDelta>> = emptyList()
    ) : ChatCompletionClient {
        val requests = mutableListOf<ChatRequest>()

        override fun streamChat(request: ChatRequest, provider: ProviderConfig, apiKey: String?): Flow<ChatDelta> {
            val responseIndex = requests.size
            requests += request
            val response = scriptedResponses.getOrNull(responseIndex)
                ?: listOf(ChatDelta.Content("ok"), ChatDelta.Done)
            return flowOf(*response.toTypedArray())
        }
    }

    private class FakeWebSearchClient : WebSearchClient {
        override suspend fun search(query: String, fallbackApiKey: String?, installId: String): WebSearchResult =
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

    private class FakeAppLauncher : AppLauncher {
        val openedApps = mutableListOf<String>()

        override suspend fun openApp(appName: String): String {
            openedApps += appName
            return appName
        }
    }

    private class FakeDeviceAssistantGateway : DeviceAssistantGateway {
        override val connectionState: StateFlow<ShizukuConnectionState> = MutableStateFlow(
            ShizukuConnectionState(
                status = ShizukuConnectionStatus.ConnectedAdb,
                capabilities = DeviceCapability.entries.toSet()
            )
        )
        override val accessibilityState: StateFlow<AccessibilityConnectionState> = MutableStateFlow(
            AccessibilityConnectionState(AccessibilityConnectionStatus.Connected)
        )
        val forceStoppedApps = mutableListOf<String>()
        var observeCount = 0
        val tappedIndexes = mutableListOf<Int>()
        var backCount = 0

        override fun refreshConnection() = Unit
        override fun requestPermission() = Unit
        override suspend fun getDeviceStatus() = DeviceToolResult(true, "device ok")
        override suspend fun getForegroundApp() = DeviceToolResult(true, "foreground_package=test")
        override suspend fun setScreenBrightness(percent: Int) = DeviceToolResult(true, "brightness=$percent")
        override suspend fun setMediaVolume(percent: Int) = DeviceToolResult(true, "volume=$percent")
        override suspend fun enablePowerMode() = DeviceToolResult(true, "power mode")
        override suspend fun forceStopApp(appName: String): DeviceToolResult {
            forceStoppedApps += appName
            return DeviceToolResult(true, "stopped $appName")
        }
        override suspend fun observeScreen(): DeviceToolResult {
            observeCount += 1
            return DeviceToolResult(true, "screen", screen = testScreen())
        }
        override suspend fun tapUiElement(index: Int, longPress: Boolean): DeviceToolResult {
            tappedIndexes += index
            return DeviceToolResult(true, "tapped", screen = testScreen())
        }
        override suspend fun inputText(index: Int, text: String, clearExisting: Boolean) =
            DeviceToolResult(true, "typed", screen = testScreen())
        override suspend fun swipeScreen(direction: DeviceSwipeDirection, distancePercent: Int) =
            DeviceToolResult(true, "swiped", screen = testScreen())
        override suspend fun pressBack(): DeviceToolResult {
            backCount += 1
            return DeviceToolResult(true, "back", screen = testScreen())
        }
        override suspend fun pressHome() = DeviceToolResult(true, "home", screen = testScreen())

        private fun testScreen() = DeviceScreenSnapshot(
            packageName = "com.android.settings",
            activityName = "Settings",
            elements = listOf(
                DeviceUiElement(
                    index = 2,
                    className = "android.widget.TextView",
                    text = "Display",
                    contentDescription = null,
                    resourceId = null,
                    bounds = DeviceBounds(0, 100, 500, 200),
                    clickable = true,
                    editable = false,
                    scrollable = false,
                    enabled = true
                )
            )
        )
    }

    private class FakePromptProfileRepository : PromptProfileRepository {
        override suspend fun getActiveProfile(): PromptProfileConfig = PromptProfileConfig()
        override suspend fun getActiveThinkingFormat(): String? = null
    }

    private class FakeMemoryRepository : MemoryRepository {
        override suspend fun retrieve(userMessage: String, topN: Int): List<MemoryRecord> = emptyList()
        override suspend fun saveTurn(userMessage: String, assistantReply: String) = Unit
        override suspend fun getAll(): List<MemoryRecord> = emptyList()
        override suspend fun delete(id: String) = Unit
        override suspend fun clear() = Unit
    }
}
