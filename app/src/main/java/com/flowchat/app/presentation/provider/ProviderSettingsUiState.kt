package com.flowchat.app.presentation.provider

import com.flowchat.app.domain.model.ProviderConfig

const val SavedApiKeyDisplayValue = "••••••••••••••••"

data class ProviderSettingsUiState(
    val providers: List<ProviderConfig> = emptyList(),
    val selected: ProviderConfig? = null,
    val apiKey: String = "",
    val hasApiKey: Boolean = false,
    val tavilyApiKey: String = "",
    val hasTavilyApiKey: Boolean = false,
    val modelOptions: List<String> = emptyList(),
    val isLoadingModels: Boolean = false,
    val modelOptionsExpanded: Boolean = false,
    val modelListError: String? = null,
    val message: String? = null
)
