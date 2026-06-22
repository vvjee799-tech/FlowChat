package com.flowchat.app.data.network

import com.flowchat.app.domain.model.ChatDelta
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object OpenAiStreamParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseLine(line: String): ChatDelta? {
        val trimmed = line.trim()
        if (trimmed.isBlank() || !trimmed.startsWith("data:")) return null
        val payload = trimmed.removePrefix("data:").trim()
        if (payload == "[DONE]") return ChatDelta.Done

        return runCatching {
            val root = json.parseToJsonElement(payload).jsonObject
            root.extractErrorMessage()?.let { return ChatDelta.Error(it) }
            val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
            val content = choice["delta"]
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.contentOrNull
            val reasoningContent = choice["delta"]
                ?.jsonObject
                ?.get("reasoning_content")
                ?.jsonPrimitive
                ?.contentOrNull
            when {
                !content.isNullOrEmpty() -> ChatDelta.Content(content)
                !reasoningContent.isNullOrEmpty() -> ChatDelta.Reasoning(reasoningContent)
                choice["finish_reason"]?.jsonPrimitive?.contentOrNull != null -> ChatDelta.Done
                else -> null
            }
        }.getOrElse { ChatDelta.Error("Malformed stream event") }
    }

    fun parseCompletionBody(body: String): ChatDelta {
        return runCatching {
            val root = json.parseToJsonElement(body).jsonObject
            root.extractErrorMessage()?.let { return ChatDelta.Error(it) }
            val message = root["choices"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("message")
                ?.jsonObject
                ?: return ChatDelta.Error("Malformed response")
            val content = message["content"]?.jsonPrimitive?.contentOrNull
            val reasoningContent = message["reasoning_content"]?.jsonPrimitive?.contentOrNull
            when {
                !reasoningContent.isNullOrBlank() && !content.isNullOrBlank() ->
                    ChatDelta.FullResponse(reasoningText = reasoningContent, contentText = content)
                !content.isNullOrBlank() -> ChatDelta.Content(content)
                !reasoningContent.isNullOrBlank() -> ChatDelta.Reasoning(reasoningContent)
                else -> ChatDelta.Error("Provider returned an empty response.")
            }
        }.getOrElse { ChatDelta.Error("Malformed response") }
    }

    fun summarizeLine(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.isBlank() || !trimmed.startsWith("data:")) return null
        val payload = trimmed.removePrefix("data:").trim()
        if (payload == "[DONE]") return "done"

        return runCatching {
            val root = json.parseToJsonElement(payload).jsonObject
            val error = root["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
            if (error != null) return "error length=${error.length}"
            val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: return "no choices"
            val delta = choice["delta"]?.jsonObject
            val deltaKeys = delta?.keys?.joinToString(",").orEmpty()
            val contentLength = delta.stringLength("content")
            val reasoningLength = delta.stringLength("reasoning_content")
            val finishReason = choice["finish_reason"]?.jsonPrimitive?.contentOrNull
            "deltaKeys=[$deltaKeys] contentLength=$contentLength reasoningLength=$reasoningLength finish=$finishReason"
        }.getOrDefault("malformed")
    }

    private fun JsonObject.extractErrorMessage(): String? {
        return this["error"]
            ?.jsonObject
            ?.get("message")
            ?.jsonPrimitive
            ?.contentOrNull
    }

    private fun JsonElement?.stringLength(key: String): Int {
        val value = this
            ?.jsonObject
            ?.get(key)
            ?.jsonPrimitive
            ?.contentOrNull
        return value?.length ?: 0
    }
}
