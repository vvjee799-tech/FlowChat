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
}
