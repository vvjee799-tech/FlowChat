package com.flowchat.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowchat.app.data.network.ChatCompletionClient
import com.flowchat.app.data.network.WebSearchClient
import com.flowchat.app.data.preferences.AppSettings
import com.flowchat.app.data.preferences.AppSettingsStore
import com.flowchat.app.domain.appusage.AppUsageToolFormatter
import com.flowchat.app.domain.device.DeviceActionPolicy
import com.flowchat.app.domain.device.DeviceAssistantGateway
import com.flowchat.app.domain.device.DeviceToolResult
import com.flowchat.app.domain.device.DeviceSwipeDirection
import com.flowchat.app.domain.device.UnavailableDeviceAssistantGateway
import com.flowchat.app.domain.chat.ChatRequestFactory
import com.flowchat.app.domain.memory.MemoryContextFormatter
import com.flowchat.app.domain.model.ChatDelta
import com.flowchat.app.domain.model.ChatRequest
import com.flowchat.app.domain.model.ChatRequestMessage
import com.flowchat.app.domain.model.ChatToolCall
import com.flowchat.app.domain.model.Conversation
import com.flowchat.app.domain.model.MessageRole
import com.flowchat.app.domain.model.MessageStatus
import com.flowchat.app.domain.model.ProviderConfig
import com.flowchat.app.domain.repository.AppUsageReader
import com.flowchat.app.domain.repository.AppLauncher
import com.flowchat.app.domain.repository.ChatRepository
import com.flowchat.app.domain.prompt.PromptProfileConfig
import com.flowchat.app.domain.repository.MemoryRepository
import com.flowchat.app.domain.repository.PromptProfileRepository
import com.flowchat.app.domain.repository.ProviderRepository
import com.flowchat.app.domain.repository.WebSearchSettingsRepository
import com.flowchat.app.domain.tools.AgentToolDefinitions
import com.flowchat.app.domain.validation.ProviderConfigValidator
import com.flowchat.app.domain.websearch.WebSearchContextFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val providerRepository: ProviderRepository,
    private val chatClient: ChatCompletionClient,
    private val webSearchClient: WebSearchClient,
    private val webSearchSettingsRepository: WebSearchSettingsRepository,
    private val appUsageReader: AppUsageReader,
    private val appLauncher: AppLauncher,
    private val deviceAssistantGateway: DeviceAssistantGateway = UnavailableDeviceAssistantGateway,
    private val promptProfileRepository: PromptProfileRepository,
    private val memoryRepository: MemoryRepository,
    private val appSettingsStore: AppSettingsStore = AppSettingsStore(
        AppSettings(
            appUsageToolEnabled = true,
            recentAppActivityToolEnabled = true,
            openAppToolEnabled = true,
            installId = "test"
        )
    )
) : ViewModel() {
    private val currentConversationId = MutableStateFlow<String?>(null)
    private val input = MutableStateFlow("")
    private val webSearchEnabled = MutableStateFlow(false)
    private val webSearchDisclosureRequired = MutableStateFlow(false)
    private val isStreaming = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val usageAccessPermissionRequest = MutableStateFlow<UsageAccessPermissionRequestUi?>(null)
    private val pendingDeviceActionConfirmation = MutableStateFlow<DeviceActionConfirmationUi?>(null)
    private val memories = MutableStateFlow(emptyList<com.flowchat.app.domain.model.MemoryRecord>())
    private val pendingAttachment = MutableStateFlow<PendingAttachment?>(null)
    private var sendJob: Job? = null
    private var activeAssistantMessageId: String? = null
    private var activeAssistantContent = ""
    private var activeAssistantReasoningContent = ""
    private var activeAssistantReasoningStartedAt: Long? = null
    private var activeAssistantReasoningDurationMillis = 0L
    private var pendingClearedInputEcho: String? = null
    private var deviceActionConfirmation: CompletableDeferred<Boolean>? = null
    private val toolJson = Json { ignoreUnknownKeys = true }

    private val baseState = combine(
        chatRepository.observeConversations(),
        providerRepository.observeProviders(),
        currentConversationId
    ) { conversations: List<Conversation>, providers: List<ProviderConfig>, selectedId: String? ->
        val current = if (selectedId == null) {
            conversations.firstOrNull()
        } else {
            conversations.firstOrNull { it.id == selectedId }
        }
        if (selectedId == null && current != null) {
            currentConversationId.value = current.id
        }
        ChatUiState(
            conversations = conversations,
            currentConversation = current,
            providers = providers
        )
    }

    private val stateWithoutMessages = combine(
        baseState,
        input,
        isStreaming,
        errorMessage,
        webSearchEnabled
    ) { state, inputValue, streaming, error, webSearchEnabledValue ->
        state.copy(
            input = inputValue,
            isStreaming = streaming,
            errorMessage = error,
            webSearchEnabled = webSearchEnabledValue
        )
    }

    private val stateWithPermissionRequest = combine(
        stateWithoutMessages,
        usageAccessPermissionRequest
    ) { state: ChatUiState, permissionRequest: UsageAccessPermissionRequestUi? ->
        state.copy(
            usageAccessPermissionRequest = permissionRequest
        )
    }

    private val stateWithAppSettings = combine(
        stateWithPermissionRequest,
        appSettingsStore.state
    ) { state, settings ->
        state.copy(appSettings = settings)
    }

    private val stateWithDeviceAssistant = combine(
        stateWithAppSettings,
        deviceAssistantGateway.connectionState,
        deviceAssistantGateway.accessibilityState,
        pendingDeviceActionConfirmation
    ) { state, shizukuState, accessibilityState, confirmation ->
        state.copy(
            shizukuState = shizukuState,
            accessibilityState = accessibilityState,
            pendingDeviceActionConfirmation = confirmation
        )
    }

    private val stateWithMemories = combine(
        stateWithDeviceAssistant,
        memories
    ) { state, memoryRecords ->
        state.copy(memories = memoryRecords)
    }

    private val stateWithAttachment = combine(
        stateWithMemories,
        pendingAttachment
    ) { state, attachment ->
        state.copy(pendingAttachment = attachment)
    }

    private val stateWithDisclosure = combine(
        stateWithAttachment,
        webSearchDisclosureRequired
    ) { state, disclosureRequired ->
        state.copy(webSearchDisclosureRequired = disclosureRequired)
    }

    val uiState = combine(
        stateWithDisclosure,
        currentConversationId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else chatRepository.observeMessages(id)
        }
    ) { state, messages ->
        state.copy(
            messages = messages
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    init {
        viewModelScope.launch {
            providerRepository.ensureTemplates()
        }
        deviceAssistantGateway.refreshConnection()
        refreshMemories()
    }

    fun updateInput(value: String) {
        val pendingEcho = pendingClearedInputEcho
        if (pendingEcho != null && pendingEcho == value) {
            pendingClearedInputEcho = null
            return
        }
        if (pendingEcho != null && value.isBlank() && input.value.isBlank()) {
            input.value = ""
            return
        }
        pendingClearedInputEcho = null
        input.value = value
    }

    fun toggleWebSearch() {
        if (isStreaming.value) return
        if (!webSearchEnabled.value && !appSettingsStore.state.value.webSearchDisclosureAccepted) {
            webSearchDisclosureRequired.value = true
            return
        }
        webSearchEnabled.update { enabled -> !enabled }
    }

    fun acceptWebSearchDisclosure() {
        appSettingsStore.acceptWebSearchDisclosure()
        webSearchDisclosureRequired.value = false
        webSearchEnabled.value = true
    }

    fun dismissWebSearchDisclosure() {
        webSearchDisclosureRequired.value = false
    }

    fun setMemoryEnabled(enabled: Boolean) {
        appSettingsStore.setMemoryEnabled(enabled)
    }

    fun setAppUsageToolEnabled(enabled: Boolean) {
        appSettingsStore.setAppUsageToolEnabled(enabled)
    }

    fun setRecentAppActivityToolEnabled(enabled: Boolean) {
        appSettingsStore.setRecentAppActivityToolEnabled(enabled)
    }

    fun setOpenAppToolEnabled(enabled: Boolean) {
        appSettingsStore.setOpenAppToolEnabled(enabled)
    }

    fun setDeviceAssistantEnabled(enabled: Boolean) {
        appSettingsStore.setDeviceAssistantEnabled(enabled)
    }

    fun setForceStopToolEnabled(enabled: Boolean) {
        appSettingsStore.setForceStopToolEnabled(enabled)
    }

    fun requestShizukuPermission() {
        deviceAssistantGateway.requestPermission()
    }

    fun refreshShizukuConnection() {
        deviceAssistantGateway.refreshConnection()
    }

    fun refreshAccessibilityConnection() {
        deviceAssistantGateway.refreshAccessibilityConnection()
    }

    fun openAccessibilitySettings() {
        deviceAssistantGateway.openAccessibilitySettings()
    }

    fun confirmDeviceAction() {
        deviceActionConfirmation?.complete(true)
    }

    fun cancelDeviceAction() {
        deviceActionConfirmation?.complete(false)
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            memoryRepository.delete(id)
            refreshMemoriesNow()
        }
    }

    fun clearMemories() {
        viewModelScope.launch {
            memoryRepository.clear()
            refreshMemoriesNow()
        }
    }

    fun attachTextFile(name: String, text: String) {
        pendingAttachment.value = PendingAttachment(name = name, text = text)
    }

    fun clearAttachment() {
        pendingAttachment.value = null
    }

    fun reportError(message: String) {
        errorMessage.value = message
    }

    fun dismissUsageAccessPermissionRequest() {
        usageAccessPermissionRequest.value = null
    }

    fun selectConversation(id: String) {
        currentConversationId.value = id
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
            if (currentConversationId.value == id) {
                currentConversationId.value = null
            }
        }
    }

    fun newConversation() {
        viewModelScope.launch {
            val providers = providerRepository.getProvidersOnce()
            val provider = providers.firstOrNull { provider ->
                ProviderConfigValidator.validate(provider).isEmpty() &&
                    providerRepository.getApiKey(provider) != null
            } ?: providers.firstOrNull { provider -> ProviderConfigValidator.validate(provider).isEmpty() }
            if (provider == null) {
                errorMessage.value = "Create a provider first."
                return@launch
            }
            val conversation = chatRepository.createConversation(provider.id, provider.defaultModel)
            currentConversationId.value = conversation.id
        }
    }

    fun updateConversationSettings(
        assistantName: String,
        assistantAvatarPath: String?,
        showAvatars: Boolean,
        systemPrompt: String,
        temperature: Double,
        topP: Double,
        maxTokens: Int?
    ) {
        val conversation = uiState.value.currentConversation ?: return
        viewModelScope.launch {
            chatRepository.updateConversationSettings(
                conversation.id,
                assistantName,
                assistantAvatarPath,
                showAvatars,
                conversation.enableThinking,
                systemPrompt,
                temperature,
                topP,
                maxTokens
            )
        }
    }

    fun updateConversationModel(provider: ProviderConfig, modelName: String) {
        val conversation = uiState.value.currentConversation ?: return
        viewModelScope.launch {
            chatRepository.updateConversationModel(conversation.id, provider.id, modelName.ifBlank { provider.defaultModel })
        }
    }

    fun send() {
        val rawText = input.value
        val text = rawText.trim()
        val attachment = pendingAttachment.value
        val selectedConversationId = currentConversationId.value ?: uiState.value.currentConversation?.id ?: return
        if ((text.isBlank() && attachment == null) || isStreaming.value) return
        errorMessage.value = null
        sendJob = viewModelScope.launch {
            isStreaming.value = true
            val conversation = chatRepository.getConversation(selectedConversationId)
                ?: uiState.value.currentConversation?.takeIf { it.id == selectedConversationId }
            if (conversation == null) {
                errorMessage.value = "Conversation not found."
                isStreaming.value = false
                return@launch
            }
            val provider = providerRepository.getProvider(conversation.providerId)
            if (provider == null) {
                errorMessage.value = "Provider not found."
                isStreaming.value = false
                return@launch
            }
            if (ProviderConfigValidator.validate(provider).isNotEmpty()) {
                errorMessage.value = "Provider configuration is invalid."
                isStreaming.value = false
                return@launch
            }
            pendingClearedInputEcho = rawText
            input.value = ""
            pendingAttachment.value = null
            val user = chatRepository.appendMessage(
                conversation.id,
                MessageRole.User,
                text,
                MessageStatus.Sent,
                conversation.modelName,
                attachmentName = attachment?.name,
                attachmentText = attachment?.text
            )
            val previousUserMessages = chatRepository.getMessages(conversation.id)
                .count { it.role == MessageRole.User && it.id != user.id }
            if (previousUserMessages == 0) {
                chatRepository.updateConversationTitle(
                    conversation.id,
                    com.flowchat.app.domain.chat.ConversationTitle.from(text, attachment?.name)
                )
            }
            var assistant = chatRepository.appendMessage(
                conversation.id,
                MessageRole.Assistant,
                "",
                MessageStatus.Streaming,
                conversation.modelName
            )
            activeAssistantMessageId = assistant.id
            activeAssistantContent = ""
            activeAssistantReasoningContent = ""
            activeAssistantReasoningStartedAt = null
            activeAssistantReasoningDurationMillis = 0L
            val freshConversation = chatRepository.getConversation(conversation.id) ?: conversation
            val history = chatRepository.getMessages(conversation.id)
                .filter { it.id != assistant.id }
                .ifEmpty { listOf(user) }
            val activeProfile = promptProfileRepository.getActiveProfile()
            val settings = appSettingsStore.state.value
            deviceAssistantGateway.refreshAccessibilityConnection()
            val relevantMemories = if (activeProfile.memory.enabled && settings.memoryEnabled) {
                MemoryContextFormatter.format(
                    memoryRepository.retrieve(text, activeProfile.memory.topN)
                )
            } else {
                emptyList()
            }
            val tavilyApiKey = if (webSearchEnabled.value) {
                webSearchSettingsRepository.getTavilyApiKey()
            } else {
                null
            }
            var request = ChatRequestFactory.create(
                freshConversation,
                history,
                null,
                activeProfile.thinkingFormat,
                relevantMemories
            )
            request = AgentToolDefinitions.withLifestyleTools(
                request,
                includeWebSearch = webSearchEnabled.value,
                includeAppUsage = settings.appUsageToolEnabled,
                includeRecentActivity = settings.recentAppActivityToolEnabled,
                includeOpenApp = settings.openAppToolEnabled,
                includeDeviceAssistant = settings.deviceAssistantEnabled &&
                    (deviceAssistantGateway.connectionState.value.isConnected ||
                        deviceAssistantGateway.accessibilityState.value.isConnected),
                includeForceStop = settings.forceStopToolEnabled,
                deviceCapabilities = deviceAssistantGateway.connectionState.value.capabilities
            )
            val apiKey = providerRepository.getApiKey(provider)
            var content = ""
            var reasoningContent = ""
            runCatching {
                var toolRounds = 0
                val toolCallCounts = mutableMapOf<String, Int>()
                while (true) {
                    val response = collectAssistantResponse(
                        request = request,
                        provider = provider,
                        apiKey = apiKey,
                        assistantId = assistant.id,
                        currentContent = content,
                        currentReasoning = reasoningContent
                    )
                    content = response.content
                    reasoningContent = response.reasoningContent
                    response.errorMessage?.let { message ->
                        chatRepository.markMessageFailed(assistant.id, message)
                        saveMemoryIfEnabled(activeProfile, text, message)
                        errorMessage.value = message
                        return@runCatching
                    }
                    if (response.toolCalls.isEmpty()) {
                        if (content.isBlank()) {
                            val emptyResponseMessage = "Provider returned an empty response."
                            chatRepository.markMessageFailed(assistant.id, emptyResponseMessage)
                            saveMemoryIfEnabled(activeProfile, text, emptyResponseMessage)
                            errorMessage.value = emptyResponseMessage
                        } else {
                            chatRepository.updateMessage(
                                assistant.id,
                                content,
                                MessageStatus.Complete,
                                reasoningContent
                            )
                            saveMemoryIfEnabled(activeProfile, text, content)
                        }
                        return@runCatching
                    }
                    if (toolRounds >= MaxToolCallRounds) {
                        val message = "Tool call limit reached."
                        chatRepository.markMessageFailed(assistant.id, message)
                        saveMemoryIfEnabled(activeProfile, text, message)
                        errorMessage.value = message
                        return@runCatching
                    }
                    ensureVisibleAssistantSegmentBeforeToolCall(
                        assistantId = assistant.id,
                        content = content,
                        reasoningContent = reasoningContent
                    )
                    val toolResultMessages = executeToolCalls(
                        calls = response.toolCalls,
                        conversationId = conversation.id,
                        modelName = conversation.modelName,
                        tavilyApiKey = tavilyApiKey,
                        installId = appSettingsStore.state.value.installId,
                        callCounts = toolCallCounts
                    )
                    request = request.copy(
                        messages = request.messages +
                            ChatRequestMessage(
                                role = MessageRole.Assistant.apiRole,
                                content = content,
                                reasoningContent = reasoningContent,
                                toolCalls = response.toolCalls
                            ) +
                            toolResultMessages
                    )
                    toolRounds += 1
                    assistant = chatRepository.appendMessage(
                        conversation.id,
                        MessageRole.Assistant,
                        "",
                        MessageStatus.Streaming,
                        conversation.modelName
                    )
                    activeAssistantMessageId = assistant.id
                    activeAssistantContent = ""
                    activeAssistantReasoningContent = ""
                    activeAssistantReasoningStartedAt = null
                    activeAssistantReasoningDurationMillis = 0L
                    content = ""
                    reasoningContent = ""
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) {
                    return@onFailure
                } else {
                    val message = throwable.userFacingChatError()
                    chatRepository.markMessageFailed(assistant.id, message)
                    saveMemoryIfEnabled(activeProfile, text, message)
                    errorMessage.value = message
                }
            }
            if (activeAssistantMessageId == assistant.id) {
                activeAssistantMessageId = null
                activeAssistantContent = ""
                activeAssistantReasoningContent = ""
                activeAssistantReasoningStartedAt = null
                activeAssistantReasoningDurationMillis = 0L
            }
            isStreaming.value = false
        }
    }

    private suspend fun collectAssistantResponse(
        request: ChatRequest,
        provider: ProviderConfig,
        apiKey: String?,
        assistantId: String,
        currentContent: String,
        currentReasoning: String
    ): AssistantResponseResult {
        var content = currentContent
        var reasoningContent = currentReasoning
        var toolCalls = emptyList<ChatToolCall>()
        var error: String? = null
        chatClient.streamChat(request, provider, apiKey).collect { delta ->
            when (delta) {
                is ChatDelta.Content -> {
                    if (content.isBlank() && reasoningContent.isNotBlank()) {
                        delay(BatchedReasoningToContentPauseMillis)
                    }
                    content = appendContentDelta(
                        assistantId,
                        content,
                        reasoningContent,
                        delta.text
                    )
                }
                is ChatDelta.Reasoning -> {
                    reasoningContent = appendReasoningDelta(
                        assistantId,
                        content,
                        reasoningContent,
                        delta.text
                    )
                }
                is ChatDelta.FullResponse -> {
                    reasoningContent = appendReasoningDelta(
                        assistantId,
                        content,
                        reasoningContent,
                        delta.reasoningText
                    )
                    delay(BatchedReasoningToContentPauseMillis)
                    content = appendContentDelta(
                        assistantId,
                        content,
                        reasoningContent,
                        delta.contentText
                    )
                }
                is ChatDelta.ToolCalls -> {
                    toolCalls = delta.calls
                }
                is ChatDelta.ToolCallDelta -> Unit
                ChatDelta.Done -> Unit
                is ChatDelta.Error -> {
                    error = delta.message
                }
            }
        }
        return AssistantResponseResult(
            content = content,
            reasoningContent = reasoningContent,
            toolCalls = toolCalls,
            errorMessage = error
        )
    }

    private suspend fun executeToolCalls(
        calls: List<ChatToolCall>,
        conversationId: String,
        modelName: String?,
        tavilyApiKey: String?,
        installId: String,
        callCounts: MutableMap<String, Int>
    ): List<ChatRequestMessage> =
        calls.map { call ->
            val result = executeSingleToolCall(
                call,
                conversationId,
                modelName,
                tavilyApiKey,
                installId,
                callCounts
            )
            ChatRequestMessage(
                role = "tool",
                content = result,
                toolCallId = call.id
            )
        }

    private suspend fun executeSingleToolCall(
        call: ChatToolCall,
        conversationId: String,
        modelName: String?,
        tavilyApiKey: String?,
        installId: String,
        callCounts: MutableMap<String, Int>
    ): String {
        val toolName = call.displayName()
        val toolMessage = chatRepository.appendMessage(
            conversationId,
            MessageRole.Tool,
            toolName,
            MessageStatus.Streaming,
            modelName
        )
        return runCatching {
            val fingerprint = "${call.name}:${call.arguments.trim()}"
            val callCount = (callCounts[fingerprint] ?: 0) + 1
            callCounts[fingerprint] = callCount
            if (callCount > MaxRepeatedIdenticalToolCalls) {
                return@runCatching ToolExecutionOutcome(
                    content = "Stopped repeated identical device action. Re-observe the screen or finish the task.",
                    errorDetail = "Repeated identical tool call blocked."
                )
            }
            when (call.name) {
                AgentToolDefinitions.WebSearchToolName -> executeWebSearchTool(call.arguments, tavilyApiKey, installId)
                AgentToolDefinitions.AppUsageSummaryToolName -> executeAppUsageSummaryTool(call.arguments)
                AgentToolDefinitions.RecentAppActivityToolName -> executeRecentAppActivityTool(call.arguments)
                AgentToolDefinitions.OpenAppToolName -> executeOpenAppTool(call.arguments)
                AgentToolDefinitions.DeviceStatusToolName -> deviceAssistantGateway.getDeviceStatus().toToolOutcome()
                AgentToolDefinitions.ForegroundAppToolName -> deviceAssistantGateway.getForegroundApp().toToolOutcome()
                AgentToolDefinitions.SetBrightnessToolName -> executeSetBrightnessTool(call.arguments)
                AgentToolDefinitions.SetMediaVolumeToolName -> executeSetMediaVolumeTool(call.arguments)
                AgentToolDefinitions.ForceStopAppToolName -> executeForceStopAppTool(call)
                AgentToolDefinitions.ObserveScreenToolName ->
                    deviceAssistantGateway.observeScreen().toToolOutcome()
                AgentToolDefinitions.TapUiElementToolName -> executeTapUiElementTool(call.arguments)
                AgentToolDefinitions.InputTextToolName -> executeInputTextTool(call.arguments)
                AgentToolDefinitions.SwipeScreenToolName -> executeSwipeScreenTool(call.arguments)
                AgentToolDefinitions.PressBackToolName -> deviceAssistantGateway.pressBack().toToolOutcome()
                AgentToolDefinitions.PressHomeToolName -> deviceAssistantGateway.pressHome().toToolOutcome()
                else -> ToolExecutionOutcome(
                    content = "Unsupported tool: ${call.name}",
                    errorDetail = "Unsupported tool: ${call.name}"
                )
            }
        }.fold(
            onSuccess = { outcome ->
                val detail = outcome.errorDetail
                if (outcome.cancelled) {
                    chatRepository.updateMessage(
                        toolMessage.id,
                        toolName,
                        MessageStatus.Cancelled
                    )
                } else if (detail == null) {
                    chatRepository.updateMessage(
                        toolMessage.id,
                        toolName,
                        MessageStatus.Complete
                    )
                } else {
                    chatRepository.updateMessage(
                        toolMessage.id,
                        failedToolMessageContent(toolName, detail),
                        MessageStatus.Failed
                    )
                }
                outcome.content
            },
            onFailure = { throwable ->
                val message = throwable.message?.takeIf { it.isNotBlank() } ?: "Tool call failed."
                chatRepository.updateMessage(
                    toolMessage.id,
                    failedToolMessageContent(toolName, message),
                    MessageStatus.Failed
                )
                "Tool call failed: $message"
            }
        )
    }

    private suspend fun ensureVisibleAssistantSegmentBeforeToolCall(
        assistantId: String,
        content: String,
        reasoningContent: String
    ) {
        val visibleContent = content.ifBlank { ToolCallPrefaceFallback }
        chatRepository.updateMessage(
            assistantId,
            visibleContent,
            MessageStatus.Complete,
            reasoningContent
        )
    }

    private suspend fun executeWebSearchTool(
        arguments: String,
        tavilyApiKey: String?,
        installId: String
    ): ToolExecutionOutcome {
        val query = parseWebSearchQuery(arguments)
            ?: return ToolExecutionOutcome(
                content = "Invalid web_search arguments: missing query.",
                errorDetail = "Invalid web_search arguments: missing query."
            )
        val searchResult = withTimeout(WebSearchTimeoutMillis) {
            webSearchClient.search(query, tavilyApiKey, installId)
        }
        val content = if (searchResult.results.isEmpty()) {
            "No web search results found."
        } else {
            WebSearchContextFormatter.format(searchResult)
        }
        return ToolExecutionOutcome(content)
    }

    private suspend fun executeAppUsageSummaryTool(arguments: String): ToolExecutionOutcome {
        if (!appUsageReader.hasUsageAccess()) {
            return requestUsageAccessPermission(AgentToolDefinitions.AppUsageSummaryToolName)
        }
        val range = parseAppUsageRange(arguments)
        val summary = appUsageReader.getUsageSummary(range)
        return ToolExecutionOutcome(AppUsageToolFormatter.formatSummary(summary))
    }

    private suspend fun executeRecentAppActivityTool(arguments: String): ToolExecutionOutcome {
        if (!appUsageReader.hasUsageAccess()) {
            return requestUsageAccessPermission(AgentToolDefinitions.RecentAppActivityToolName)
        }
        val hours = parseRecentActivityHours(arguments)
        val activity = appUsageReader.getRecentActivity(hours)
        return ToolExecutionOutcome(AppUsageToolFormatter.formatRecentActivity(activity))
    }

    private suspend fun executeOpenAppTool(arguments: String): ToolExecutionOutcome {
        val appName = parseOpenAppName(arguments)
            ?: return ToolExecutionOutcome(
                content = "Invalid open_app arguments: missing app_name.",
                errorDetail = "Invalid open_app arguments: missing app_name."
            )
        val openedApp = appLauncher.openApp(appName)
        return ToolExecutionOutcome("Opened app: $openedApp.")
    }

    private suspend fun executeSetBrightnessTool(arguments: String): ToolExecutionOutcome {
        val percent = parsePercentage(arguments)
            ?: return invalidDeviceToolArguments(AgentToolDefinitions.SetBrightnessToolName, "percent")
        return deviceAssistantGateway.setScreenBrightness(percent).toToolOutcome()
    }

    private suspend fun executeSetMediaVolumeTool(arguments: String): ToolExecutionOutcome {
        val percent = parsePercentage(arguments)
            ?: return invalidDeviceToolArguments(AgentToolDefinitions.SetMediaVolumeToolName, "percent")
        return deviceAssistantGateway.setMediaVolume(percent).toToolOutcome()
    }

    private suspend fun executeForceStopAppTool(call: ChatToolCall): ToolExecutionOutcome {
        val appName = parseAppName(call.arguments)
            ?: return invalidDeviceToolArguments(AgentToolDefinitions.ForceStopAppToolName, "app_name")
        if (DeviceActionPolicy.requiresConfirmation(call.name)) {
            val confirmed = awaitDeviceActionConfirmation(
                toolName = call.name,
                title = appName,
                message = "Unsaved work in $appName may be lost."
            )
            if (!confirmed) {
                return ToolExecutionOutcome(
                    content = "User cancelled force stopping $appName.",
                    cancelled = true
                )
            }
        }
        return deviceAssistantGateway.forceStopApp(appName).toToolOutcome()
    }

    private suspend fun executeTapUiElementTool(arguments: String): ToolExecutionOutcome {
        val index = parseIntArgument(arguments, "index", minimum = 0)
            ?: return invalidDeviceToolArguments(AgentToolDefinitions.TapUiElementToolName, "index")
        val longPress = parseBooleanArgument(arguments, "long_press") ?: false
        return deviceAssistantGateway.tapUiElement(index, longPress).toToolOutcome()
    }

    private suspend fun executeInputTextTool(arguments: String): ToolExecutionOutcome {
        val index = parseIntArgument(arguments, "index", minimum = 0)
            ?: return invalidDeviceToolArguments(AgentToolDefinitions.InputTextToolName, "index")
        val text = parseStringArgument(arguments, "text", allowBlank = true)
            ?: return invalidDeviceToolArguments(AgentToolDefinitions.InputTextToolName, "text")
        val clearExisting = parseBooleanArgument(arguments, "clear_existing") ?: true
        return deviceAssistantGateway.inputText(index, text, clearExisting).toToolOutcome()
    }

    private suspend fun executeSwipeScreenTool(arguments: String): ToolExecutionOutcome {
        val direction = parseStringArgument(arguments, "direction")
            ?.let { value ->
                DeviceSwipeDirection.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            }
            ?: return invalidDeviceToolArguments(AgentToolDefinitions.SwipeScreenToolName, "direction")
        val distance = parseIntArgument(arguments, "distance_percent", minimum = 20, maximum = 80) ?: 50
        return deviceAssistantGateway.swipeScreen(direction, distance).toToolOutcome()
    }

    private suspend fun awaitDeviceActionConfirmation(
        toolName: String,
        title: String,
        message: String
    ): Boolean {
        deviceActionConfirmation?.complete(false)
        val confirmation = CompletableDeferred<Boolean>()
        deviceActionConfirmation = confirmation
        pendingDeviceActionConfirmation.value = DeviceActionConfirmationUi(toolName, title, message)
        return try {
            confirmation.await()
        } finally {
            if (deviceActionConfirmation === confirmation) {
                deviceActionConfirmation = null
                pendingDeviceActionConfirmation.value = null
            }
        }
    }

    private fun DeviceToolResult.toToolOutcome(): ToolExecutionOutcome =
        ToolExecutionOutcome(
            content = asToolContent(),
            errorDetail = if (success) null else summary
        )

    private fun invalidDeviceToolArguments(toolName: String, field: String) = ToolExecutionOutcome(
        content = "Invalid $toolName arguments: missing $field.",
        errorDetail = "Invalid $toolName arguments: missing $field."
    )

    private fun requestUsageAccessPermission(toolName: String): ToolExecutionOutcome {
        val message = "Open Android system Usage Access settings and allow FlowChat before retrying this tool."
        usageAccessPermissionRequest.value = UsageAccessPermissionRequestUi(toolName)
        return ToolExecutionOutcome(
            content = "App usage access is not enabled. $message",
            errorDetail = message
        )
    }

    private fun parseWebSearchQuery(arguments: String): String? =
        runCatching {
            toolJson.parseToJsonElement(arguments)
                .jsonObject["query"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()

    private fun parseAppUsageRange(arguments: String): String =
        runCatching {
            toolJson.parseToJsonElement(arguments)
                .jsonObject["range"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.takeIf { it == "today" || it == "yesterday" || it == "last_7_days" }
        }.getOrNull() ?: "today"

    private fun parseRecentActivityHours(arguments: String): Int =
        runCatching {
            toolJson.parseToJsonElement(arguments)
                .jsonObject["hours"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.toIntOrNull()
                ?.coerceIn(1, 24)
        }.getOrNull() ?: 6

    private fun parseOpenAppName(arguments: String): String? =
        runCatching {
            toolJson.parseToJsonElement(arguments)
                .jsonObject["app_name"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()

    private fun parseAppName(arguments: String): String? =
        runCatching {
            toolJson.parseToJsonElement(arguments)
                .jsonObject["app_name"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()

    private fun parsePercentage(arguments: String): Int? =
        runCatching {
            toolJson.parseToJsonElement(arguments)
                .jsonObject["percent"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.toIntOrNull()
                ?.coerceIn(0, 100)
        }.getOrNull()

    private fun parseStringArgument(
        arguments: String,
        field: String,
        allowBlank: Boolean = false
    ): String? = runCatching {
        toolJson.parseToJsonElement(arguments)
            .jsonObject[field]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.takeIf { allowBlank || it.isNotBlank() }
    }.getOrNull()

    private fun parseIntArgument(
        arguments: String,
        field: String,
        minimum: Int,
        maximum: Int = Int.MAX_VALUE
    ): Int? = parseStringArgument(arguments, field, allowBlank = false)
        ?.toIntOrNull()
        ?.takeIf { it in minimum..maximum }

    private fun parseBooleanArgument(arguments: String, field: String): Boolean? =
        parseStringArgument(arguments, field, allowBlank = false)?.toBooleanStrictOrNull()

    private fun ChatToolCall.displayName(): String =
        when (name) {
            AgentToolDefinitions.OpenAppToolName ->
                parseOpenAppName(arguments)?.let { "$name:$it" } ?: name
            AgentToolDefinitions.ForceStopAppToolName ->
                parseAppName(arguments)?.let { "$name:$it" } ?: name
            AgentToolDefinitions.TapUiElementToolName ->
                parseIntArgument(arguments, "index", minimum = 0)?.let { "$name:$it" } ?: name
            else -> name
        }

    private fun failedToolMessageContent(toolName: String, detail: String): String =
        "$toolName\n$detail"

    private suspend fun appendReasoningDelta(
        assistantId: String,
        currentContent: String,
        currentReasoning: String,
        deltaText: String
    ): String {
        var reasoning = currentReasoning
        val startedAt = activeAssistantReasoningStartedAt ?: System.currentTimeMillis().also { timestamp ->
            activeAssistantReasoningStartedAt = timestamp
        }
        val chunks = deltaText.displayChunks()
        chunks.forEachIndexed { index, chunk ->
            reasoning += chunk
            activeAssistantReasoningDurationMillis = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
            chatRepository.updateMessage(
                assistantId,
                currentContent,
                MessageStatus.Streaming,
                reasoning,
                reasoningDurationMillis = activeAssistantReasoningDurationMillis
            )
            activeAssistantReasoningContent = reasoning
            if (index < chunks.lastIndex) {
                delay(IncrementalDisplayDelayMillis)
            }
        }
        return reasoning
    }

    private suspend fun appendContentDelta(
        assistantId: String,
        currentContent: String,
        currentReasoning: String,
        deltaText: String
    ): String {
        var content = currentContent
        val chunks = deltaText.displayChunks()
        chunks.forEachIndexed { index, chunk ->
            content += chunk
            chatRepository.updateMessage(
                assistantId,
                content,
                MessageStatus.Streaming,
                currentReasoning,
                reasoningDurationMillis = null
            )
            activeAssistantContent = content
            activeAssistantReasoningContent = currentReasoning
            if (index < chunks.lastIndex) {
                delay(IncrementalDisplayDelayMillis)
            }
        }
        return content
    }

    private fun String.displayChunks(): List<String> =
        if (length <= IncrementalDisplayChunkSize) {
            listOf(this)
        } else {
            chunked(IncrementalDisplayChunkSize)
        }

    private fun Throwable.userFacingChatError(): String {
        val rawMessage = message.orEmpty()
        return if (rawMessage.contains("Request timeout has expired", ignoreCase = true)) {
            "请求超时：模型思考时间过长或网络中断，请重试。"
        } else {
            rawMessage.ifBlank { "Request failed" }
        }
    }

    private suspend fun saveMemoryIfEnabled(
        profile: PromptProfileConfig,
        userMessage: String,
        assistantReply: String
    ) {
        if (!profile.memory.enabled || !appSettingsStore.state.value.memoryEnabled) return
        runCatching {
            memoryRepository.saveTurn(userMessage, assistantReply)
            refreshMemoriesNow()
        }
    }

    private fun refreshMemories() {
        viewModelScope.launch {
            refreshMemoriesNow()
        }
    }

    private suspend fun refreshMemoriesNow() {
        memories.value = memoryRepository.getAll()
    }

    fun stop() {
        val assistantId = activeAssistantMessageId
        val stoppedContent = activeAssistantContent
        val stoppedReasoningContent = activeAssistantReasoningContent
        sendJob?.cancel()
        deviceActionConfirmation?.complete(false)
        deviceActionConfirmation = null
        pendingDeviceActionConfirmation.value = null
        sendJob = null
        isStreaming.value = false
        if (assistantId != null) {
            viewModelScope.launch {
                chatRepository.updateMessage(
                    assistantId,
                    stoppedContent,
                    MessageStatus.Stopped,
                    stoppedReasoningContent,
                    reasoningDurationMillis = activeAssistantReasoningDurationMillis.takeIf { it > 0L }
                )
            }
        }
        activeAssistantMessageId = null
        activeAssistantContent = ""
        activeAssistantReasoningContent = ""
        activeAssistantReasoningStartedAt = null
        activeAssistantReasoningDurationMillis = 0L
    }

    fun retryLastFailed() {
        val failed = uiState.value.messages.lastOrNull { it.status == MessageStatus.Failed } ?: return
        viewModelScope.launch {
            chatRepository.updateMessage(failed.id, "", MessageStatus.Streaming)
        }
    }

    private companion object {
        const val WebSearchTimeoutMillis = 15_000L
        const val IncrementalDisplayChunkSize = 8
        const val IncrementalDisplayDelayMillis = 18L
        const val BatchedReasoningToContentPauseMillis = 450L
        const val MaxToolCallRounds = 8
        const val MaxRepeatedIdenticalToolCalls = 2
        const val ToolCallPrefaceFallback = "好，我先调用相关工具看看。"
    }
}

private data class AssistantResponseResult(
    val content: String,
    val reasoningContent: String,
    val toolCalls: List<ChatToolCall>,
    val errorMessage: String?
)

private data class ToolExecutionOutcome(
    val content: String,
    val errorDetail: String? = null,
    val cancelled: Boolean = false
)
