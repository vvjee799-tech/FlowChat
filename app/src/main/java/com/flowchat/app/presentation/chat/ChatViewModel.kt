package com.flowchat.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowchat.app.data.network.ChatCompletionClient
import com.flowchat.app.data.network.WebSearchClient
import com.flowchat.app.domain.appusage.AppUsageToolFormatter
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
import com.flowchat.app.domain.repository.ChatRepository
import com.flowchat.app.domain.prompt.PromptProfileConfig
import com.flowchat.app.domain.repository.MemoryRepository
import com.flowchat.app.domain.repository.PromptProfileRepository
import com.flowchat.app.domain.repository.ProviderRepository
import com.flowchat.app.domain.repository.WebSearchSettingsRepository
import com.flowchat.app.domain.tools.AgentToolDefinitions
import com.flowchat.app.domain.websearch.WebSearchContextFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
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
    private val promptProfileRepository: PromptProfileRepository,
    private val memoryRepository: MemoryRepository
) : ViewModel() {
    private val currentConversationId = MutableStateFlow<String?>(null)
    private val input = MutableStateFlow("")
    private val webSearchEnabled = MutableStateFlow(false)
    private val isStreaming = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val toolCallStatus = MutableStateFlow<ToolCallStatusUi?>(null)
    private var sendJob: Job? = null
    private var activeAssistantMessageId: String? = null
    private var activeAssistantContent = ""
    private var activeAssistantReasoningContent = ""
    private var pendingClearedInputEcho: String? = null
    private val toolJson = Json { ignoreUnknownKeys = true }

    private val baseState = combine(
        chatRepository.observeConversations(),
        providerRepository.observeProviders(),
        currentConversationId
    ) { conversations: List<Conversation>, providers: List<ProviderConfig>, selectedId: String? ->
        val current = conversations.firstOrNull { it.id == selectedId } ?: conversations.firstOrNull()
        if (current != null && selectedId != current.id) {
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

    private val stateWithToolStatus = combine(
        stateWithoutMessages,
        toolCallStatus
    ) { state: ChatUiState, toolStatus: ToolCallStatusUi? ->
        state.copy(
            toolCallStatus = toolStatus
        )
    }

    val uiState = combine(
        stateWithToolStatus,
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
        if (!isStreaming.value) {
            webSearchEnabled.update { enabled -> !enabled }
        }
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
            val provider = providerRepository.getProvidersOnce().firstOrNull()
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
        val conversation = uiState.value.currentConversation ?: return
        if (text.isBlank() || isStreaming.value) return
        pendingClearedInputEcho = rawText
        input.value = ""
        errorMessage.value = null
        toolCallStatus.value = null
        sendJob = viewModelScope.launch {
            isStreaming.value = true
            val provider = providerRepository.getProvider(conversation.providerId)
            if (provider == null) {
                errorMessage.value = "Provider not found."
                isStreaming.value = false
                return@launch
            }
            val user = chatRepository.appendMessage(
                conversation.id,
                MessageRole.User,
                text,
                MessageStatus.Sent,
                conversation.modelName
            )
            val assistant = chatRepository.appendMessage(
                conversation.id,
                MessageRole.Assistant,
                "",
                MessageStatus.Streaming,
                conversation.modelName
            )
            activeAssistantMessageId = assistant.id
            activeAssistantContent = ""
            activeAssistantReasoningContent = ""
            val freshConversation = chatRepository.getConversation(conversation.id) ?: conversation
            val history = chatRepository.getMessages(conversation.id)
                .filter { it.id != assistant.id }
                .ifEmpty { listOf(user) }
            val activeProfile = promptProfileRepository.getActiveProfile()
            val relevantMemories = if (activeProfile.memory.enabled) {
                MemoryContextFormatter.format(
                    memoryRepository.retrieve(text, activeProfile.memory.topN)
                )
            } else {
                emptyList()
            }
            val tavilyApiKey = if (webSearchEnabled.value) {
                webSearchSettingsRepository.getTavilyApiKey().also { key ->
                    if (key.isNullOrBlank()) {
                        val message = "Tavily API key is not configured."
                        chatRepository.markMessageFailed(assistant.id, message)
                        saveMemoryIfEnabled(activeProfile, text, message)
                        errorMessage.value = message
                        isStreaming.value = false
                        return@launch
                    }
                }
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
                includeWebSearch = webSearchEnabled.value
            )
            val apiKey = providerRepository.getApiKey(provider)
            var content = ""
            var reasoningContent = ""
            runCatching {
                var toolRounds = 0
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
                    val toolResultMessages = executeToolCalls(response.toolCalls, tavilyApiKey.orEmpty())
                    request = request.copy(
                        messages = request.messages +
                            ChatRequestMessage(role = MessageRole.Assistant.apiRole, toolCalls = response.toolCalls) +
                            toolResultMessages
                    )
                    toolRounds += 1
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
        tavilyApiKey: String
    ): List<ChatRequestMessage> =
        calls.map { call ->
            val result = executeSingleToolCall(call, tavilyApiKey)
            ChatRequestMessage(
                role = "tool",
                content = result,
                toolCallId = call.id
            )
        }

    private suspend fun executeSingleToolCall(call: ChatToolCall, tavilyApiKey: String): String {
        val toolName = call.displayName()
        toolCallStatus.value = ToolCallStatusUi(toolName, ToolCallPhase.Running)
        return runCatching {
            when (call.name) {
                AgentToolDefinitions.WebSearchToolName -> executeWebSearchTool(call.arguments, tavilyApiKey)
                AgentToolDefinitions.AppUsageSummaryToolName -> executeAppUsageSummaryTool(call.arguments)
                AgentToolDefinitions.RecentAppActivityToolName -> executeRecentAppActivityTool(call.arguments)
                else -> "Unsupported tool: ${call.name}"
            }
        }.fold(
            onSuccess = { result ->
                if (toolCallStatus.value?.phase == ToolCallPhase.Running) {
                    toolCallStatus.value = ToolCallStatusUi(toolName, ToolCallPhase.Complete)
                }
                result
            },
            onFailure = { throwable ->
                val message = throwable.message?.takeIf { it.isNotBlank() } ?: "Tool call failed."
                toolCallStatus.value = ToolCallStatusUi(toolName, ToolCallPhase.Failed, message)
                "Tool call failed: $message"
            }
        )
    }

    private suspend fun executeWebSearchTool(arguments: String, tavilyApiKey: String): String {
        val query = parseWebSearchQuery(arguments)
            ?: return "Invalid web_search arguments: missing query."
        val searchResult = withTimeout(WebSearchTimeoutMillis) {
            webSearchClient.search(query, tavilyApiKey)
        }
        return if (searchResult.results.isEmpty()) {
            "No web search results found."
        } else {
            WebSearchContextFormatter.format(searchResult)
        }
    }

    private suspend fun executeAppUsageSummaryTool(arguments: String): String {
        if (!appUsageReader.hasUsageAccess()) {
            toolCallStatus.value = ToolCallStatusUi("应用使用情况", ToolCallPhase.Failed, "未开启使用情况权限")
            return "App usage access is not enabled. Ask the user to enable Usage Access in FlowChat settings."
        }
        val range = parseAppUsageRange(arguments)
        val summary = appUsageReader.getUsageSummary(range)
        return AppUsageToolFormatter.formatSummary(summary)
    }

    private suspend fun executeRecentAppActivityTool(arguments: String): String {
        if (!appUsageReader.hasUsageAccess()) {
            toolCallStatus.value = ToolCallStatusUi("最近应用活动", ToolCallPhase.Failed, "未开启使用情况权限")
            return "App usage access is not enabled. Ask the user to enable Usage Access in FlowChat settings."
        }
        val hours = parseRecentActivityHours(arguments)
        val activity = appUsageReader.getRecentActivity(hours)
        return AppUsageToolFormatter.formatRecentActivity(activity)
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

    private fun ChatToolCall.displayName(): String =
        when (name) {
            AgentToolDefinitions.WebSearchToolName -> "联网搜索"
            AgentToolDefinitions.AppUsageSummaryToolName -> "应用使用情况"
            AgentToolDefinitions.RecentAppActivityToolName -> "最近应用活动"
            else -> name
        }

    private suspend fun appendReasoningDelta(
        assistantId: String,
        currentContent: String,
        currentReasoning: String,
        deltaText: String
    ): String {
        var reasoning = currentReasoning
        val chunks = deltaText.displayChunks()
        chunks.forEachIndexed { index, chunk ->
            reasoning += chunk
            chatRepository.updateMessage(
                assistantId,
                currentContent,
                MessageStatus.Streaming,
                reasoning
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
                currentReasoning
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
        if (!profile.memory.enabled) return
        runCatching {
            memoryRepository.saveTurn(userMessage, assistantReply)
        }
    }

    fun stop() {
        val assistantId = activeAssistantMessageId
        val stoppedContent = activeAssistantContent
        val stoppedReasoningContent = activeAssistantReasoningContent
        sendJob?.cancel()
        sendJob = null
        isStreaming.value = false
        if (assistantId != null) {
            viewModelScope.launch {
                chatRepository.updateMessage(
                    assistantId,
                    stoppedContent,
                    MessageStatus.Stopped,
                    stoppedReasoningContent
                )
            }
        }
        activeAssistantMessageId = null
        activeAssistantContent = ""
        activeAssistantReasoningContent = ""
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
        const val MaxToolCallRounds = 2
    }
}

private data class AssistantResponseResult(
    val content: String,
    val reasoningContent: String,
    val toolCalls: List<ChatToolCall>,
    val errorMessage: String?
)
