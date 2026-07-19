package com.flowchat.app.domain.validation

import com.flowchat.app.domain.model.ProviderConfig
import java.net.URI

object ProviderConfigValidator {
    enum class Error {
        BlankBaseUrl,
        InvalidBaseUrl,
        InsecurePublicUrl,
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
        } else {
            val uri = config.baseUrl.toHttpUri()
            when {
                uri == null -> errors += Error.InvalidBaseUrl
                uri.scheme == "http" && !uri.host.isLocalNetworkHost() -> errors += Error.InsecurePublicUrl
            }
        }
        if (config.defaultModel.isBlank()) {
            errors += Error.BlankModel
        }
        return errors
    }

    private fun String.toHttpUri(): URI? = runCatching {
        val uri = URI(trim())
        uri.takeIf { (it.scheme == "http" || it.scheme == "https") && !it.host.isNullOrBlank() }
    }.getOrNull()

    private fun String?.isLocalNetworkHost(): Boolean {
        val host = this?.lowercase() ?: return false
        if (host == "localhost" || host == "127.0.0.1" || host == "::1") return true
        val octets = host.split('.').mapNotNull(String::toIntOrNull)
        if (octets.size != 4 || octets.any { it !in 0..255 }) return false
        return octets[0] == 10 ||
            octets[0] == 127 ||
            (octets[0] == 192 && octets[1] == 168) ||
            (octets[0] == 172 && octets[1] in 16..31)
    }
}
