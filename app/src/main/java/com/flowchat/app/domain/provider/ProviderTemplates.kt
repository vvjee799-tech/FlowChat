package com.flowchat.app.domain.provider

import com.flowchat.app.domain.model.ProviderConfig

object ProviderTemplates {
    fun defaultCustomProvider(): ProviderConfig = ProviderConfig(
        id = CUSTOM_PROVIDER_ID,
        displayName = "Custom OpenAI-compatible",
        baseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini"
    )

    const val CUSTOM_PROVIDER_ID = "provider-custom"
}
