package com.flowchat.app.presentation.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowchat.app.data.network.ModelCatalogClient
import com.flowchat.app.domain.model.ProviderConfig
import com.flowchat.app.domain.provider.ProviderPreset
import com.flowchat.app.domain.repository.ProviderRepository
import com.flowchat.app.domain.repository.WebSearchSettingsRepository
import com.flowchat.app.domain.validation.ProviderConfigValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProviderSettingsViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val webSearchSettingsRepository: WebSearchSettingsRepository,
    private val modelCatalogClient: ModelCatalogClient
) : ViewModel() {
    private val selected = MutableStateFlow<ProviderConfig?>(null)
    private val apiKey = MutableStateFlow("")
    private val hasApiKey = MutableStateFlow(false)
    private val tavilyApiKey = MutableStateFlow("")
    private val hasTavilyApiKey = MutableStateFlow(false)
    private val modelOptions = MutableStateFlow(emptyList<String>())
    private val isLoadingModels = MutableStateFlow(false)
    private val modelOptionsExpanded = MutableStateFlow(false)
    private val modelListError = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val modelOptionsCache = mutableMapOf<String, List<String>>()

    val uiState = combine(
        providerRepository.observeProviders(),
        selected,
        apiKey,
        hasApiKey,
        tavilyApiKey,
        hasTavilyApiKey,
        modelOptions,
        isLoadingModels,
        modelOptionsExpanded,
        modelListError,
        message
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val providers = values[0] as List<ProviderConfig>
        val selectedProvider = values[1] as ProviderConfig?
        val key = values[2] as String
        val savedKey = values[3] as Boolean
        val tavilyKey = values[4] as String
        val savedTavilyKey = values[5] as Boolean
        @Suppress("UNCHECKED_CAST")
        val options = values[6] as List<String>
        val loadingModels = values[7] as Boolean
        val optionsExpanded = values[8] as Boolean
        val modelError = values[9] as String?
        val msg = values[10] as String?
        val actualSelected = selectedProvider ?: providers.firstOrNull()
        if (actualSelected != null && selectedProvider == null) {
            selected.value = actualSelected
            refreshApiKeyState(actualSelected)
        }
        ProviderSettingsUiState(
            providers = providers,
            selected = actualSelected,
            apiKey = key,
            hasApiKey = savedKey,
            tavilyApiKey = tavilyKey,
            hasTavilyApiKey = savedTavilyKey,
            modelOptions = options,
            isLoadingModels = loadingModels,
            modelOptionsExpanded = optionsExpanded,
            modelListError = modelError,
            message = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProviderSettingsUiState())

    init {
        viewModelScope.launch {
            providerRepository.ensureTemplates()
            val provider = providerRepository.getProvidersOnce().firstOrNull()
            selected.value = provider
            hasApiKey.value = provider?.let { providerRepository.getApiKey(it) != null } ?: false
            hasTavilyApiKey.value = webSearchSettingsRepository.hasTavilyApiKey()
        }
    }

    fun select(provider: ProviderConfig) {
        selected.value = provider
        apiKey.value = ""
        refreshApiKeyState(provider)
    }

    fun updateSelected(transform: (ProviderConfig) -> ProviderConfig) {
        selected.update { current -> current?.let(transform) }
        modelListError.value = null
    }

    fun updateApiKey(value: String) {
        apiKey.value = value
        modelListError.value = null
    }

    fun updateTavilyApiKey(value: String) {
        tavilyApiKey.value = value
    }

    fun applyPreset(preset: ProviderPreset) {
        selected.update { current ->
            val base = current ?: ProviderConfig(
                id = "provider-custom",
                displayName = "Custom configuration",
                baseUrl = "https://api.openai.com/v1",
                defaultModel = "gpt-5.4-mini"
            )
            base.copy(
                displayName = preset.displayName,
                baseUrl = preset.baseUrl,
                defaultModel = preset.defaultModel,
                customHeadersJson = "{}"
            )
        }
        modelOptions.value = emptyList()
        modelOptionsExpanded.value = false
        modelListError.value = null
        message.value = null
    }

    fun loadModelOptions() {
        val provider = selected.value ?: return
        modelOptionsExpanded.value = true
        modelListError.value = null
        viewModelScope.launch {
            val draftApiKey = apiKey.value.trim().takeIf { it.isNotBlank() }
            val resolvedApiKey = draftApiKey ?: providerRepository.getApiKey(provider)
            val cacheKey = provider.modelOptionsCacheKey(resolvedApiKey)
            modelOptionsCache[cacheKey]?.let { cached ->
                modelOptions.value = cached
                isLoadingModels.value = false
                return@launch
            }

            isLoadingModels.value = true
            runCatching {
                modelCatalogClient.listModels(provider, resolvedApiKey)
            }.onSuccess { models ->
                modelOptions.value = models
                modelOptionsCache[cacheKey] = models
                modelListError.value = if (models.isEmpty()) "未找到可用模型" else null
            }.onFailure { throwable ->
                modelOptions.value = emptyList()
                modelListError.value = throwable.message ?: "模型列表获取失败"
            }
            isLoadingModels.value = false
        }
    }

    fun selectModelOption(modelName: String) {
        selected.update { current -> current?.copy(defaultModel = modelName) }
        modelOptionsExpanded.value = false
        modelListError.value = null
    }

    fun dismissModelOptions() {
        modelOptionsExpanded.value = false
    }

    fun save() {
        val provider = selected.value?.copy(customHeadersJson = "{}")
        val tavilyKeyToSave = tavilyApiKey.value.trim()
        val errors = provider?.let { ProviderConfigValidator.validate(it) }.orEmpty()
        if (tavilyKeyToSave.isBlank() && errors.isNotEmpty()) {
            message.value = "Invalid provider: ${errors.joinToString()}"
            return
        }
        if (provider == null && tavilyKeyToSave.isBlank()) {
            return
        }
        viewModelScope.launch {
            var savedTavilyKey = false
            if (tavilyKeyToSave.isNotBlank()) {
                webSearchSettingsRepository.saveTavilyApiKey(tavilyKeyToSave)
                hasTavilyApiKey.value = webSearchSettingsRepository.hasTavilyApiKey()
                tavilyApiKey.value = ""
                savedTavilyKey = true
            }
            if (provider == null) {
                message.value = "Saved"
                return@launch
            }
            if (errors.isNotEmpty()) {
                message.value = if (savedTavilyKey) {
                    "Web search API key saved. Invalid provider: ${errors.joinToString()}"
                } else {
                    "Invalid provider: ${errors.joinToString()}"
                }
                return@launch
            }
            providerRepository.upsertProvider(provider, apiKey.value.ifBlank { null })
            providerRepository.getProvider(provider.id)?.let { savedProvider ->
                selected.value = savedProvider
                hasApiKey.value = providerRepository.getApiKey(savedProvider) != null
            }
            hasTavilyApiKey.value = webSearchSettingsRepository.hasTavilyApiKey()
            message.value = "Saved"
            apiKey.value = ""
        }
    }

    fun deleteSelected() {
        val provider = selected.value ?: return
        viewModelScope.launch {
            providerRepository.deleteProvider(provider.id)
            selected.value = null
            hasApiKey.value = false
        }
    }

    private fun refreshApiKeyState(provider: ProviderConfig) {
        viewModelScope.launch {
            hasApiKey.value = providerRepository.getApiKey(provider) != null
        }
    }

    private fun ProviderConfig.modelOptionsCacheKey(apiKey: String?): String =
        "${baseUrl.trim()}|${!apiKey.isNullOrBlank()}"
}
