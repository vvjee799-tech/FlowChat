package com.flowchat.app.domain.provider

import com.flowchat.app.domain.model.ProviderConfig

object ProviderTemplates {
    fun defaultCustomProvider(): ProviderConfig = ProviderConfig(
        id = CUSTOM_PROVIDER_ID,
        displayName = "Custom configuration",
        baseUrl = "",
        defaultModel = ""
    )

    fun popularPresets(): List<ProviderPreset> = listOf(
        ProviderPreset(
            id = "preset-chatgpt",
            displayName = "ChatGPT",
            baseUrl = "https://api.openai.com/v1",
            defaultModel = "gpt-5.4-mini"
        ),
        ProviderPreset(
            id = "preset-claude",
            displayName = "Claude",
            baseUrl = "https://api.anthropic.com/v1",
            defaultModel = "claude-sonnet-4-6"
        ),
        ProviderPreset(
            id = "preset-deepseek",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-flash"
        ),
        ProviderPreset(
            id = "preset-gemini",
            displayName = "Gemini",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
            defaultModel = "gemini-3.5-flash"
        )
    )

    const val CUSTOM_PROVIDER_ID = "provider-custom"
}

data class ProviderPreset(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val defaultModel: String,
    val iconResName: String? = null
)
