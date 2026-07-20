package com.flowchat.app.presentation.overlay

import com.flowchat.app.domain.model.MessageRole
import com.flowchat.app.domain.model.MessageStatus
import com.flowchat.app.presentation.chat.ChatUiState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class FloatingAssistantState(
    val conversationId: String? = null,
    val assistantName: String = "FlowChat",
    val assistantAvatarPath: String? = null,
    val modelName: String = "",
    val assistantText: String = "",
    val activeToolName: String? = null,
    val lastOpenedAppName: String? = null,
    val isStreaming: Boolean = false,
    val canChat: Boolean = false
) {
    companion object {
        fun from(state: ChatUiState): FloatingAssistantState {
            val conversation = state.currentConversation
            val assistantText = state.messages
                .asReversed()
                .firstOrNull { it.role == MessageRole.Assistant && it.content.isNotBlank() }
                ?.content
                .orEmpty()
            val activeTool = state.messages
                .asReversed()
                .firstOrNull {
                    it.role == MessageRole.Tool &&
                        (it.status == MessageStatus.Streaming || state.isStreaming)
                }
                ?.content
                ?.lineSequence()
                ?.firstOrNull()
                ?.substringBefore(':')
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            val openedApp = state.messages
                .asReversed()
                .firstOrNull { it.role == MessageRole.Tool && it.content.startsWith("open_app:") }
                ?.content
                ?.lineSequence()
                ?.firstOrNull()
                ?.substringAfter(':')
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            return FloatingAssistantState(
                conversationId = conversation?.id,
                assistantName = conversation?.assistantName?.trim().takeUnless { it.isNullOrBlank() }
                    ?: conversation?.title?.trim().takeUnless { it.isNullOrBlank() }
                    ?: "FlowChat",
                assistantAvatarPath = conversation?.assistantAvatarPath,
                modelName = conversation?.modelName.orEmpty(),
                assistantText = assistantText,
                activeToolName = activeTool,
                lastOpenedAppName = openedApp,
                isStreaming = state.isStreaming,
                canChat = conversation != null
            )
        }
    }
}

sealed interface FloatingAssistantCommand {
    data class Send(val text: String) : FloatingAssistantCommand
    data object Stop : FloatingAssistantCommand
}

sealed interface FloatingAssistantEvent {
    data object CollapseForAutomation : FloatingAssistantEvent
}

@Singleton
class FloatingAssistantBridge @Inject constructor() {
    private val mutableState = MutableStateFlow(FloatingAssistantState())
    val state: StateFlow<FloatingAssistantState> = mutableState.asStateFlow()

    private val mutableCommands = MutableSharedFlow<FloatingAssistantCommand>(extraBufferCapacity = 8)
    val commands: SharedFlow<FloatingAssistantCommand> = mutableCommands.asSharedFlow()

    private val mutableEvents = MutableSharedFlow<FloatingAssistantEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<FloatingAssistantEvent> = mutableEvents.asSharedFlow()

    private val mutableAppInForeground = MutableStateFlow(true)
    val appInForeground: StateFlow<Boolean> = mutableAppInForeground.asStateFlow()

    fun publish(state: FloatingAssistantState) {
        mutableState.value = state
    }

    fun send(text: String) {
        text.trim().takeIf { it.isNotEmpty() }?.let { value ->
            mutableCommands.tryEmit(FloatingAssistantCommand.Send(value))
        }
    }

    fun stop() {
        mutableCommands.tryEmit(FloatingAssistantCommand.Stop)
    }

    fun collapseForAutomation() {
        mutableEvents.tryEmit(FloatingAssistantEvent.CollapseForAutomation)
    }

    fun setAppInForeground(inForeground: Boolean) {
        mutableAppInForeground.value = inForeground
    }
}
