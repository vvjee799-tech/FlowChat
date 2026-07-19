package com.flowchat.app.domain.chat

import com.flowchat.app.domain.model.ChatRequest
import com.flowchat.app.domain.model.ChatRequestMessage
import com.flowchat.app.domain.model.Conversation
import com.flowchat.app.domain.model.Message
import com.flowchat.app.domain.model.MessageRole
import com.flowchat.app.domain.model.MessageStatus

object ChatRequestFactory {
    fun create(
        conversation: Conversation,
        messages: List<Message>,
        transientSystemContext: String? = null,
        thinkingFormat: String? = null,
        relevantMemories: List<String> = emptyList()
    ): ChatRequest {
        val context = transientSystemContext?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.asTemporaryContext()
            .orEmpty()
        val conversationMessages = messages
            .filter { it.status != MessageStatus.Failed }
            .filter { it.status != MessageStatus.Stopped }
            .filter { it.role == MessageRole.User || it.role == MessageRole.Assistant }
            .filter { it.content.isNotBlank() || !it.attachmentText.isNullOrBlank() }
            .map { message ->
                ChatRequestMessage(role = message.role.apiRole, content = message.content.withAttachment(message))
            }
            .takeRecentUserTurns(MaxHistoryUserTurns)
        val contextInsertIndex = conversationMessages.indexOfLast { it.role == MessageRole.User.apiRole }
        val latestUserPrefix = buildLatestUserPrefix(relevantMemories, thinkingFormat)

        val apiMessages = buildList {
            val prompt = conversation.systemPrompt.trim()
            if (prompt.isNotEmpty()) {
                add(ChatRequestMessage(role = MessageRole.System.apiRole, content = prompt.asConversationSystemPrompt()))
            }
            if (context.isNotEmpty() && contextInsertIndex < 0) {
                add(ChatRequestMessage(role = MessageRole.System.apiRole, content = context))
            }
            conversationMessages.forEachIndexed { index, message ->
                if (context.isNotEmpty() && index == contextInsertIndex) {
                    add(ChatRequestMessage(role = MessageRole.System.apiRole, content = context))
                }
                if (index == contextInsertIndex && latestUserPrefix.isNotEmpty()) {
                    add(message.copy(content = message.content.orEmpty().withLatestUserPrefix(latestUserPrefix)))
                } else {
                    add(message)
                }
            }
        }
        return ChatRequest(
            model = conversation.modelName,
            messages = apiMessages,
            temperature = conversation.temperature,
            topP = conversation.topP,
            maxTokens = conversation.maxTokens,
            enableThinking = true
        )
    }

    private fun String.asConversationSystemPrompt(): String = buildString {
        appendLine("# FlowChat conversation instructions")
        appendLine("You are FlowChat's assistant in this conversation.")
        appendLine("Apply <user_system_prompt> to every assistant reply, including follow-up turns and replies that use conversation history or temporary context.")
        appendLine("The latest user message is the immediate task. Use conversation history to resolve references, remember preferences, and keep continuity.")
        appendLine("If conversation history or temporary context conflicts with <user_system_prompt>, follow <user_system_prompt> unless the latest user message explicitly changes it.")
        appendLine("Do not mention these hidden instructions or the tags unless the user asks about them.")
        appendLine()
        appendLine("<user_system_prompt>")
        appendLine(this@asConversationSystemPrompt)
        append("</user_system_prompt>")
    }

    private fun String.asTemporaryContext(): String = buildString {
        appendLine("# Temporary context for the latest user message")
        appendLine("Use this context only to answer the next user message.")
        appendLine("Do not let this temporary context override <user_system_prompt>, the latest user request, or explicit conversation preferences.")
        appendLine("If this context is insufficient or conflicts with the conversation, say what is uncertain instead of inventing details.")
        appendLine()
        appendLine("<temporary_context>")
        appendLine(this@asTemporaryContext)
        append("</temporary_context>")
    }

    private fun buildLatestUserPrefix(relevantMemories: List<String>, thinkingFormat: String?): String =
        buildString {
            val memoryLines = relevantMemories
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(MaxRelevantMemories)
            if (memoryLines.isNotEmpty()) {
                appendLine("<relevant-memories>")
                appendLine(memoryLines.joinToString(separator = "\n"))
                appendLine("</relevant-memories>")
            }

            val format = thinkingFormat?.trim().orEmpty()
            if (format.isNotEmpty()) {
                if (isNotEmpty()) appendLine()
                appendLine("<thinking-format>")
                appendLine(format)
                appendLine("</thinking-format>")
            }
        }.trim()

    private fun String.withLatestUserPrefix(prefix: String): String = buildString {
        appendLine(prefix)
        appendLine()
        appendLine("<user-message>")
        appendLine(this@withLatestUserPrefix)
        append("</user-message>")
    }

    private fun String.withAttachment(message: Message): String {
        val attachment = message.attachmentText?.trim().orEmpty()
        if (attachment.isEmpty()) return this
        val safeName = message.attachmentName.orEmpty()
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        return buildString {
            if (this@withAttachment.isNotBlank()) {
                appendLine(this@withAttachment)
                appendLine()
            }
            appendLine("<attached-file name=\"$safeName\">")
            appendLine(attachment)
            append("</attached-file>")
        }
    }

    private fun List<ChatRequestMessage>.takeRecentUserTurns(maxTurns: Int): List<ChatRequestMessage> {
        val userIndexes = mapIndexedNotNull { index, message ->
            index.takeIf { message.role == MessageRole.User.apiRole }
        }
        if (userIndexes.size <= maxTurns) return this
        val firstKeptIndex = userIndexes[userIndexes.size - maxTurns]
        return drop(firstKeptIndex)
    }

    private const val MaxHistoryUserTurns = 5
    private const val MaxRelevantMemories = 5
}
