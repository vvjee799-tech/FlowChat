package com.flowchat.app.domain.validation

import com.flowchat.app.domain.model.ProviderConfig
import java.net.URI

object ProviderConfigValidator {
    enum class Error {
        BlankBaseUrl,
        InvalidBaseUrl,
        BlankModel,
        BlankName
    }

    fun validate(config: ProviderConfig): List<Error> {
        val errors = mutableListOf<Error>()
        if (config.displayName.isBlank()) {
            errors += Error.BlankName
        }
        if (config.baseUrl.isBlank()) {
            errors += Error.BlankBaseUrl
        } else if (!isValidHttpUrl(config.baseUrl)) {
            errors += Error.InvalidBaseUrl
        }
        if (config.defaultModel.isBlank()) {
            errors += Error.BlankModel
        }
        return errors
    }

    private fun isValidHttpUrl(value: String): Boolean {
        return runCatching {
            val uri = URI(value.trim())
            (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
        }.getOrDefault(false)
    }
}
