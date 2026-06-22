package com.flowchat.app.domain.provider

import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderTemplatesTest {
    @Test
    fun exposesOnlyOneCustomEditableProvider() {
        val provider = ProviderTemplates.defaultCustomProvider()

        assertEquals("provider-custom", provider.id)
        assertEquals("Custom OpenAI-compatible", provider.displayName)
        assertEquals("https://api.openai.com/v1", provider.baseUrl)
        assertEquals("gpt-4o-mini", provider.defaultModel)
    }
}
