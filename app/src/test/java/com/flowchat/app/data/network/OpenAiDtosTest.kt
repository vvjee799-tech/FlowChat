package com.flowchat.app.data.network

import com.flowchat.app.domain.model.ChatRequest
import com.flowchat.app.domain.model.ChatRequestMessage
import com.flowchat.app.domain.model.ProviderConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiDtosTest {
    private val json = Json { explicitNulls = false }
    private val request = ChatRequest(
        model = "deepseek-v4-flash",
        messages = listOf(ChatRequestMessage(role = "user", content = "hello")),
        temperature = 1.0,
        topP = 1.0,
        maxTokens = null
    )

    @Test
    fun disablesDeepSeekThinkingModeWhenConversationThinkingIsOff() {
        val provider = ProviderConfig(
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-flash"
        )

        val payload = json.encodeToString(request.toOpenAiRequest(provider))

        assertTrue(payload.contains(""""thinking":{"type":"disabled"}"""))
    }

    @Test
    fun enablesDeepSeekThinkingModeWhenConversationThinkingIsOn() {
        val provider = ProviderConfig(
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-flash"
        )
        val thinkingRequest = request.copy(enableThinking = true)

        val payload = json.encodeToString(thinkingRequest.toOpenAiRequest(provider))

        assertTrue(payload.contains(""""thinking":{"type":"enabled"}"""))
    }

    @Test
    fun streamingRequestExplicitlyIncludesStreamTrue() {
        val provider = ProviderConfig(
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-flash"
        )

        val payload = json.encodeToString(request.toOpenAiRequest(provider, stream = true))

        assertTrue(payload.contains(""""stream":true"""))
    }

    @Test
    fun doesNotSendDeepSeekThinkingOptionToOtherProviders() {
        val provider = ProviderConfig(
            displayName = "OpenAI compatible",
            baseUrl = "https://api.example.com/v1",
            defaultModel = "deepseek-v4-flash"
        )

        val payload = json.encodeToString(request.toOpenAiRequest(provider))

        assertFalse(payload.contains("thinking"))
    }

    @Test
    fun canCreateNonStreamingFallbackRequest() {
        val provider = ProviderConfig(
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-flash"
        )

        val payload = json.encodeToString(request.toOpenAiRequest(provider, stream = false))

        assertTrue(payload.contains(""""stream":false"""))
    }
}
