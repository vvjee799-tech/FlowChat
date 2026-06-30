package com.flowchat.app.presentation.chat

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatViewModelContractTest {
    @Test
    fun rawKtorTimeoutMessageIsMappedToReadableChatError() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatViewModel.kt").readText()

        assertTrue(source.contains("throwable.userFacingChatError()"))
        assertTrue(source.contains("private fun Throwable.userFacingChatError(): String"))
        assertTrue(source.contains("Request timeout has expired"))
    }

    @Test
    fun stopMarksActiveAssistantMessageAsStoppedInsteadOfLeavingItStreaming() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatViewModel.kt").readText()
        val modelSource = File("src/main/java/com/flowchat/app/domain/model/Message.kt").readText()

        assertTrue(modelSource.contains("Stopped"))
        assertTrue(source.contains("private var activeAssistantMessageId: String? = null"))
        assertTrue(source.contains("private var activeAssistantContent = \"\""))
        assertTrue(source.contains("private var activeAssistantReasoningContent = \"\""))
        assertTrue(source.contains("chatRepository.updateMessage("))
        assertTrue(source.contains("MessageStatus.Stopped"))
        assertTrue(source.contains("activeAssistantReasoningContent"))
        assertTrue(source.contains("throwable is CancellationException"))
    }

    @Test
    fun sendClearsInputAndDropsStaleTextFieldEcho() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatViewModel.kt").readText()
        val updateInputStart = source.indexOf("fun updateInput(value: String)")
        val updateInputEnd = source.indexOf("fun toggleWebSearch()", updateInputStart)
        val updateInputBlock = source.substring(updateInputStart, updateInputEnd)
        val sendStart = source.indexOf("fun send()")
        val sendEnd = source.indexOf("private suspend fun appendReasoningDelta", sendStart)
        val sendBlock = source.substring(sendStart, sendEnd)

        assertTrue(source.contains("private var pendingClearedInputEcho: String? = null"))
        assertTrue(updateInputBlock.contains("val pendingEcho = pendingClearedInputEcho"))
        assertTrue(updateInputBlock.contains("if (pendingEcho != null && pendingEcho == value)"))
        assertTrue(updateInputBlock.contains("if (pendingEcho != null && value.isBlank() && input.value.isBlank())"))
        assertTrue(updateInputBlock.contains("pendingClearedInputEcho = null"))
        assertTrue(sendBlock.contains("val rawText = input.value"))
        assertTrue(sendBlock.contains("val text = rawText.trim()"))
        assertTrue(sendBlock.contains("pendingClearedInputEcho = rawText"))
        assertTrue(sendBlock.contains("input.value = \"\""))
    }

    @Test
    fun webSearchToggleExposesAutonomousToolCallingInsteadOfPreSearchOnly() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatViewModel.kt").readText()
        val sendStart = source.indexOf("fun send()")
        val sendEnd = source.indexOf("private suspend fun collectAssistantResponse", sendStart)
        val sendBlock = source.substring(sendStart, sendEnd)

        assertTrue(source.contains("import com.flowchat.app.domain.tools.AgentToolDefinitions"))
        assertTrue(sendBlock.contains("AgentToolDefinitions.withLifestyleTools("))
        assertTrue(sendBlock.contains("webSearchSettingsRepository.getTavilyApiKey()"))
        assertTrue(source.contains("private suspend fun collectAssistantResponse("))
        assertTrue(source.contains("is ChatDelta.ToolCalls ->"))
        assertTrue(source.contains("private suspend fun executeToolCalls("))
        assertTrue(source.contains("ChatRequestMessage("))
        assertTrue(source.contains("role = \"tool\""))
        assertTrue(source.contains("toolCallId = call.id"))
        assertTrue(source.contains("webSearchClient.search(query, tavilyApiKey)"))
        assertTrue(source.contains("MaxToolCallRounds"))
    }

    @Test
    fun appUsageToolsAreAvailableAndPublishToolCallStatus() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatViewModel.kt").readText()

        assertTrue(source.contains("import com.flowchat.app.domain.repository.AppUsageReader"))
        assertTrue(source.contains("private val appUsageReader: AppUsageReader"))
        assertTrue(source.contains("private val toolCallStatus = MutableStateFlow<ToolCallStatusUi?>(null)"))
        assertTrue(source.contains("toolCallStatus = toolStatus"))
        assertTrue(source.contains("AgentToolDefinitions.withLifestyleTools("))
        assertTrue(source.contains("AgentToolDefinitions.AppUsageSummaryToolName -> executeAppUsageSummaryTool("))
        assertTrue(source.contains("AgentToolDefinitions.RecentAppActivityToolName -> executeRecentAppActivityTool("))
        assertTrue(source.contains("appUsageReader.getUsageSummary(range)"))
        assertTrue(source.contains("appUsageReader.getRecentActivity(hours)"))
        assertTrue(source.contains("ToolCallStatusUi("))
        assertTrue(source.contains("ToolCallPhase.Running"))
        assertTrue(source.contains("ToolCallPhase.Complete"))
        assertTrue(source.contains("ToolCallPhase.Failed"))
    }
}
