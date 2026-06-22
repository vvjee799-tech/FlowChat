package com.flowchat.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowchat.app.data.network.ChatCompletionClient
import com.flowchat.app.data.network.WebSearchClient
import com.flowchat.app.domain.chat.ChatRequestFactory
import com.flowchat.app.domain.model.ChatDelta
import com.flowchat.app.domain.model.Conversation
import com.flowchat.app.domain.model.MessageRole
import com.flowchat.app.domain.model.MessageStatus
import com.flowchat.app.domain.model.ProviderConfig
import com.flowchat.app.domain.repository.ChatRepository
import com.flowchat.app.domain.repository.ProviderRepository
import com.flowchat.app.domain.repository.WebSearchSettingsRepository
import com.flowchat.app.domain.websearch.WebSearchContextFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val providerRepository: ProviderRepository,
    private val chatClient: ChatCompletionClient,
    private val webSearchClient: WebSearchClient,
    private val webSearchSettingsRepository: WebSearchSettingsRepository
) : ViewModel() {
    private val currentConversationId = MutableStateFlow<String?>(null)
    private val input = MutableStateFlow("")
    private val webSearchEnabled = MutableStateFlow(false)
    private val isStreaming = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private var sendJob: Job? = null

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

    val uiState = combine(
        stateWithoutMessages,
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
        input.value = value
    }

    fun toggleWebSearch() {
        if (!isStreaming.value) {
            webSearchEnabled.update { enabled -> !enabled }
        }
    }

    fun toggleThinking() {
        val conversation = uiState.value.currentConversation ?: return
        if (isStreaming.value) return
        viewModelScope.launch {
            chatRepository.updateConversationSettings(
                conversation.id,
                conversation.assistantName,
                conversation.assistantAvatarPath,
                conversation.showAvatars,
                !conversation.enableThinking,
                conversation.systemPrompt,
                conversation.temperature,
                conversation.topP,
                conversation.maxTokens
            )
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
        val text = input.value.trim()
        val conversation = uiState.value.currentConversation ?: return
        if (text.isBlank() || isStreaming.value) return
        input.value = ""
        errorMessage.value = null
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
            val freshConversation = chatRepository.getConversation(conversation.id) ?: conversation
            val history = chatRepository.getMessages(conversation.id)
                .filter { it.id != assistant.id }
                .ifEmpty { listOf(user) }
            val searchContext = if (webSearchEnabled.value) {
                val tavilyApiKey = webSearchSettingsRepository.getTavilyApiKey()
                if (tavilyApiKey.isNullOrBlank()) {
                    val message = "Tavily API key is not configured."
                    chatRepository.markMessageFailed(assistant.id, message)
                    errorMessage.value = message
                    isStreaming.value = false
                    return@launch
                }
                val searchResult = runCatching {
                    withTimeout(WebSearchTimeoutMillis) {
                        webSearchClient.search(text, tavilyApiKey)
                    }
                }.getOrElse { throwable ->
                    val message = throwable.message?.takeIf { it.isNotBlank() } ?: "Web search failed."
                    chatRepository.markMessageFailed(assistant.id, message)
                    errorMessage.value = message
                    isStreaming.value = false
                    return@launch
                }
                if (searchResult.results.isEmpty()) {
                    val message = "No web search results found."
                    chatRepository.markMessageFailed(assistant.id, message)
                    errorMessage.value = message
                    isStreaming.value = false
                    return@launch
                }
                WebSearchContextFormatter.format(searchResult)
            } else {
                null
            }
            val request = ChatRequestFactory.create(freshConversation, history, searchContext)
            val apiKey = providerRepository.getApiKey(provider)
            var content = ""
            var reasoningContent = ""
            runCatching {
                chatClient.streamChat(request, provider, apiKey).collect { delta ->
                    when (delta) {
                        is ChatDelta.Content -> {
                            if (content.isBlank() && reasoningContent.isNotBlank()) {
                                delay(BatchedReasoningToContentPauseMillis)
                            }
                            content = appendContentDelta(
                                assistant.id,
                                content,
                                reasoningContent,
                                delta.text
                            )
                        }
                        is ChatDelta.Reasoning -> {
                            reasoningContent = appendReasoningDelta(
                                assistant.id,
                                content,
                                reasoningContent,
                                delta.text
                            )
                        }
                        is ChatDelta.FullResponse -> {
                            reasoningContent = appendReasoningDelta(
                                assistant.id,
                                content,
                                reasoningContent,
                                delta.reasoningText
                            )
                            delay(BatchedReasoningToContentPauseMillis)
                            content = appendContentDelta(
                                assistant.id,
                                content,
                                reasoningContent,
                                delta.contentText
                            )
                        }
                        ChatDelta.Done -> {
                            if (content.isBlank()) {
                                val emptyResponseMessage = "Provider returned an empty response."
                                chatRepository.markMessageFailed(assistant.id, emptyResponseMessage)
                                errorMessage.value = emptyResponseMessage
                            } else {
                                chatRepository.updateMessage(
                                    assistant.id,
                                    content,
                                    MessageStatus.Complete,
                                    reasoningContent
                                )
                            }
                        }
                        is ChatDelta.Error -> {
                            chatRepository.markMessageFailed(assistant.id, delta.message)
                            errorMessage.value = delta.message
                        }
                    }
                }
            }.onFailure { throwable ->
                val message = throwable.userFacingChatError()
                chatRepository.markMessageFailed(assistant.id, message)
                errorMessage.value = message
            }
            isStreaming.value = false
        }
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

    fun stop() {
        sendJob?.cancel()
        sendJob = null
        isStreaming.value = false
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
    }
}
