package com.flowchat.app.domain

import com.flowchat.app.domain.model.ProviderConfig
import com.flowchat.app.domain.validation.ProviderConfigValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderConfigValidatorTest {
    @Test
    fun rejectsBlankBaseUrl() {
        val errors = ProviderConfigValidator.validate(
            ProviderConfig(displayName = "OpenAI", baseUrl = "", defaultModel = "gpt-4o-mini")
        )

        assertTrue(errors.contains(ProviderConfigValidator.Error.BlankBaseUrl))
    }

    @Test
    fun rejectsInvalidBaseUrl() {
        val errors = ProviderConfigValidator.validate(
            ProviderConfig(displayName = "OpenAI", baseUrl = "not a url", defaultModel = "gpt-4o-mini")
        )

        assertTrue(errors.contains(ProviderConfigValidator.Error.InvalidBaseUrl))
    }

    @Test
    fun rejectsBlankModel() {
        val errors = ProviderConfigValidator.validate(
            ProviderConfig(displayName = "OpenAI", baseUrl = "https://api.openai.com/v1", defaultModel = " ")
        )

        assertTrue(errors.contains(ProviderConfigValidator.Error.BlankModel))
    }

    @Test
    fun acceptsValidHttpsProvider() {
        val errors = ProviderConfigValidator.validate(
            ProviderConfig(displayName = "OpenAI", baseUrl = "https://api.openai.com/v1", defaultModel = "gpt-4o-mini")
        )

        assertEquals(emptyList<ProviderConfigValidator.Error>(), errors)
    }

    @Test
    fun rejectsCleartextPublicProvider() {
        val errors = ProviderConfigValidator.validate(
            ProviderConfig(displayName = "Unsafe", baseUrl = "http://api.example.com/v1", defaultModel = "model")
        )

        assertTrue(errors.contains(ProviderConfigValidator.Error.InsecurePublicUrl))
    }

    @Test
    fun allowsCleartextLocalNetworkProvider() {
        val localHosts = listOf("localhost", "127.0.0.1", "192.168.1.20", "10.0.0.8", "172.16.0.3")

        localHosts.forEach { host ->
            val errors = ProviderConfigValidator.validate(
                ProviderConfig(displayName = "Ollama", baseUrl = "http://$host:11434/v1", defaultModel = "llama3.2")
            )
            assertEquals("host=$host", emptyList<ProviderConfigValidator.Error>(), errors)
        }
    }
}
