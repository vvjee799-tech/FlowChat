package com.flowchat.app.presentation.provider

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderSettingsScreenContractTest {
    @Test
    fun providerScreenDoesNotRenderPresetProviderList() {
        val source = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsScreen.kt").readText()

        assertFalse(source.contains("LazyColumn"))
        assertFalse(source.contains("items("))
        assertFalse(source.contains("ListItem"))
        assertFalse(source.contains("stringResource(R.string.edit)"))
        assertFalse(source.contains("modifier = Modifier.weight"))
    }

    @Test
    fun providerScreenShowsSavedApiKeyStateWithoutRevealingSecret() {
        val screenSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsScreen.kt").readText()
        val stateSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsUiState.kt").readText()
        val viewModelSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsViewModel.kt").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val zhStrings = File("src/main/res/values-zh-rCN/strings.xml").readText()

        assertTrue(stateSource.contains("hasApiKey: Boolean"))
        assertTrue(stateSource.contains("const val SavedApiKeyDisplayValue = \"••••••••••••••••\""))
        assertTrue(viewModelSource.contains("private val hasApiKey"))
        assertTrue(viewModelSource.contains("providerRepository.getApiKey"))
        assertTrue(viewModelSource.contains("hasApiKey.value"))
        assertTrue(viewModelSource.contains("providerRepository.getProvider(provider.id)"))
        assertTrue(screenSource.contains("val savedMaskVisible = state.hasApiKey && state.apiKey.isBlank()"))
        assertTrue(screenSource.contains("val displayedApiKey = if (savedMaskVisible) SavedApiKeyDisplayValue else state.apiKey"))
        assertTrue(screenSource.contains("value = displayedApiKey"))
        assertTrue(screenSource.contains("value == SavedApiKeyDisplayValue -> \"\""))
        assertTrue(screenSource.contains("value.all { it == '•' } -> \"\""))
        assertTrue(screenSource.contains("value.removePrefix(SavedApiKeyDisplayValue)"))
        assertTrue(screenSource.contains("supportingText ="))
        assertTrue(screenSource.contains("R.string.api_key_saved_hint"))
        assertTrue(strings.contains("<string name=\"api_key_saved_hint\">API key saved</string>"))
        assertTrue(zhStrings.contains("<string name=\"api_key_saved_hint\">API key已保存</string>"))
    }

    @Test
    fun providerScreenDoesNotExposeCustomHeadersJsonAndSaveClearsIt() {
        val screenSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsScreen.kt").readText()
        val viewModelSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsViewModel.kt").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val zhStrings = File("src/main/res/values-zh-rCN/strings.xml").readText()

        assertFalse(screenSource.contains("R.string.custom_headers"))
        assertFalse(screenSource.contains("provider.customHeadersJson"))
        assertFalse(screenSource.contains("copy(customHeadersJson"))
        assertTrue(viewModelSource.contains("selected.value?.copy(customHeadersJson = \"{}\")"))
        assertFalse(strings.contains("name=\"custom_headers\""))
        assertFalse(zhStrings.contains("name=\"custom_headers\""))
    }

    @Test
    fun providerScreenIncludesTavilyApiKeyConfigurationWithoutPlaintextReveal() {
        val screenSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsScreen.kt").readText()
        val stateSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsUiState.kt").readText()
        val viewModelSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsViewModel.kt").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val zhStrings = File("src/main/res/values-zh-rCN/strings.xml").readText()

        assertTrue(stateSource.contains("val tavilyApiKey: String = \"\""))
        assertTrue(stateSource.contains("val hasTavilyApiKey: Boolean = false"))
        assertTrue(viewModelSource.contains("WebSearchSettingsRepository"))
        assertTrue(viewModelSource.contains("private val tavilyApiKey = MutableStateFlow(\"\")"))
        assertTrue(viewModelSource.contains("private val hasTavilyApiKey = MutableStateFlow(false)"))
        assertTrue(viewModelSource.contains("fun updateTavilyApiKey(value: String)"))
        assertTrue(viewModelSource.contains("webSearchSettingsRepository.saveTavilyApiKey"))
        assertTrue(viewModelSource.contains("webSearchSettingsRepository.hasTavilyApiKey()"))
        assertTrue(screenSource.contains("R.string.web_search_configuration"))
        assertTrue(screenSource.contains("R.string.tavily_api_key"))
        assertTrue(screenSource.contains("val tavilySavedMaskVisible = state.hasTavilyApiKey && state.tavilyApiKey.isBlank()"))
        assertTrue(screenSource.contains("val displayedTavilyApiKey = if (tavilySavedMaskVisible) SavedApiKeyDisplayValue else state.tavilyApiKey"))
        assertTrue(screenSource.contains("onTavilyApiKey"))
        assertTrue(screenSource.contains("R.string.api_key_saved_hint"))
        assertTrue(strings.contains("<string name=\"web_search_configuration\">Web search configuration</string>"))
        assertTrue(strings.contains("<string name=\"tavily_api_key\">Tavily API key</string>"))
        assertTrue(strings.contains("<string name=\"web_search_on\">Web search on</string>"))
        assertTrue(strings.contains("<string name=\"web_search_off\">Web search off</string>"))
        assertTrue(zhStrings.contains("name=\"web_search_configuration\""))
        assertTrue(zhStrings.contains("name=\"tavily_api_key\""))
        assertTrue(zhStrings.contains("name=\"web_search_on\""))
        assertTrue(zhStrings.contains("name=\"web_search_off\""))
    }

    @Test
    fun providerModelFieldCanLoadAndSelectRemoteModelOptions() {
        val screenSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsScreen.kt").readText()
        val stateSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsUiState.kt").readText()
        val viewModelSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsViewModel.kt").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val zhStrings = File("src/main/res/values-zh-rCN/strings.xml").readText()

        assertTrue(stateSource.contains("val modelOptions: List<String> = emptyList()"))
        assertTrue(stateSource.contains("val isLoadingModels: Boolean = false"))
        assertTrue(stateSource.contains("val modelOptionsExpanded: Boolean = false"))
        assertTrue(stateSource.contains("val modelListError: String? = null"))
        assertTrue(viewModelSource.contains("ModelCatalogClient"))
        assertTrue(viewModelSource.contains("fun loadModelOptions()"))
        assertTrue(viewModelSource.contains("fun selectModelOption(modelName: String)"))
        assertTrue(viewModelSource.contains("fun dismissModelOptions()"))
        assertTrue(screenSource.contains("trailingIcon ="))
        assertTrue(screenSource.contains("Icons.Default.KeyboardArrowDown"))
        assertTrue(screenSource.contains("onClick = onLoadModelOptions"))
        assertTrue(screenSource.contains("DropdownMenu("))
        assertTrue(screenSource.contains("state.modelOptionsExpanded"))
        assertTrue(screenSource.contains("state.isLoadingModels"))
        assertTrue(screenSource.contains("state.modelListError"))
        assertTrue(screenSource.contains("state.modelOptions.forEach"))
        assertTrue(screenSource.contains("onSelectModelOption(modelName)"))
        assertTrue(screenSource.contains("onValueChange = { value -> onUpdate { it.copy(defaultModel = value) } }"))
        assertTrue(strings.contains("name=\"load_models\""))
        assertTrue(strings.contains("name=\"model_list\""))
        assertTrue(strings.contains("name=\"no_models_found\""))
        assertTrue(zhStrings.contains("name=\"load_models\""))
        assertTrue(zhStrings.contains("name=\"model_list\""))
        assertTrue(zhStrings.contains("name=\"no_models_found\""))
    }
}
