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
    }

    @Test
    fun providerScreenShowsPresetTemplatesAboveApiConfiguration() {
        val screenSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsScreen.kt").readText()
        val stateSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsUiState.kt").readText()
        val viewModelSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsViewModel.kt").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val zhStrings = File("src/main/res/values-zh-rCN/strings.xml").readText()

        assertTrue(stateSource.contains("providerPresets: List<ProviderPreset> = ProviderTemplates.popularPresets()"))
        assertTrue(viewModelSource.contains("fun applyPreset(preset: ProviderPreset)"))
        assertTrue(viewModelSource.contains("fun updatePresetApiKey(value: String)"))
        assertTrue(viewModelSource.contains("fun savePresetApiKey()"))
        assertTrue(viewModelSource.contains("fun dismissPresetApiKeyDialog()"))
        assertTrue(screenSource.contains("onApplyPreset = viewModel::applyPreset"))
        assertTrue(screenSource.contains("onPresetApiKey = viewModel::updatePresetApiKey"))
        assertTrue(screenSource.contains("onSavePresetApiKey = viewModel::savePresetApiKey"))
        assertTrue(screenSource.contains("onDismissPresetApiKeyDialog = viewModel::dismissPresetApiKeyDialog"))
        assertTrue(screenSource.contains("ProviderPresetSection("))
        assertFalse(screenSource.contains("stringResource(R.string.provider_presets)"))
        assertTrue(screenSource.contains("R.string.custom_api_configuration"))
        assertTrue(screenSource.indexOf("ProviderPresetSection(") < screenSource.indexOf("R.string.custom_api_configuration"))
        assertTrue(screenSource.contains("state.providerPresets.forEach"))
        assertTrue(screenSource.contains("onApplyPreset(preset)"))
        assertTrue(screenSource.contains("PresetApiKeyDialog("))
        assertTrue(strings.contains("<string name=\"custom_api_configuration\">Custom API configuration</string>"))
        assertTrue(strings.contains("<string name=\"preset_api_key_placeholder\">Enter API Key</string>"))
        assertTrue(zhStrings.contains("name=\"custom_api_configuration\""))
        assertTrue(zhStrings.contains("<string name=\"preset_api_key_placeholder\">请输入API Key</string>"))
    }

    @Test
    fun presetClickUsesIndependentApiKeyDialogInsteadOfMutatingCustomForm() {
        val screenSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsScreen.kt").readText()
        val stateSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsUiState.kt").readText()
        val viewModelSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsViewModel.kt").readText()
        val presetStart = screenSource.indexOf("private fun ProviderPresetSection(")
        val presetEnd = screenSource.indexOf("private fun ProviderLogo(", presetStart)
        val presetBlock = screenSource.substring(presetStart, presetEnd)
        val dialogStart = screenSource.indexOf("private fun PresetApiKeyDialog(")
        val dialogEnd = screenSource.indexOf("private fun CurrentProviderCard(", dialogStart)
        val dialogBlock = screenSource.substring(dialogStart, dialogEnd)
        val applyPresetStart = viewModelSource.indexOf("fun applyPreset(preset: ProviderPreset)")
        val applyPresetEnd = viewModelSource.indexOf("fun dismissPresetApiKeyDialog()", applyPresetStart)
        val applyPresetBlock = viewModelSource.substring(applyPresetStart, applyPresetEnd)

        assertTrue(stateSource.contains("val pendingPreset: ProviderPreset? = null"))
        assertTrue(stateSource.contains("val presetApiKey: String = \"\""))
        assertTrue(viewModelSource.contains("pendingPreset.value = preset"))
        assertFalse(applyPresetBlock.contains("selected.update"))
        assertTrue(viewModelSource.contains("preset.toProviderConfig()"))
        assertTrue(viewModelSource.contains("providerRepository.upsertProvider(provider, presetApiKey.value.trim())"))
        assertTrue(presetBlock.contains("onClick = { onApplyPreset(preset) }"))
        assertFalse(presetBlock.contains("Text(stringResource(R.string.provider_presets)"))
        assertTrue(dialogBlock.contains("AlertDialog("))
        assertTrue(dialogBlock.contains("value = apiKey"))
        assertFalse(dialogBlock.contains("title ="))
        assertFalse(dialogBlock.contains("R.string.preset_api_key_title"))
        assertTrue(dialogBlock.contains("label = { Text(stringResource(R.string.preset_api_key_placeholder)) }"))
        assertFalse(dialogBlock.contains("R.string.provider_name"))
        assertFalse(dialogBlock.contains("R.string.base_url"))
        assertFalse(dialogBlock.contains("R.string.model"))
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

    @Test
    fun providerScreenMatchesOptionTwoPresetCurrentAndApiConfigurationCards() {
        val screenSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsScreen.kt").readText()
        val editorStart = screenSource.indexOf("private fun ProviderEditor(")
        val editorEnd = screenSource.indexOf("private fun ProviderPresetSection(", editorStart)
        val editorBlock = screenSource.substring(editorStart, editorEnd)
        val presetStart = screenSource.indexOf("private fun ProviderPresetSection(")
        val fieldColorsStart = screenSource.indexOf("private fun ProviderLogo", presetStart)
        val presetBlock = screenSource.substring(presetStart, fieldColorsStart)
        val strings = File("src/main/res/values/strings.xml").readText()
        val zhStrings = File("src/main/res/values-zh-rCN/strings.xml").readText()

        assertFalse(screenSource.contains("import androidx.compose.foundation.Canvas"))
        assertTrue(screenSource.contains("import androidx.compose.material3.ButtonDefaults"))
        assertTrue(screenSource.contains("ButtonDefaults.outlinedButtonColors("))
        assertTrue(screenSource.contains("private fun ProviderSection("))
        assertTrue(screenSource.contains("private fun CurrentProviderCard("))
        assertTrue(screenSource.contains("private fun ProviderLogo("))
        assertTrue(screenSource.contains("private fun providerLogoRes("))
        assertFalse(editorBlock.contains("Card(\n        modifier = modifier.fillMaxSize()"))
        assertTrue(editorBlock.contains("CurrentProviderCard("))
        assertTrue(editorBlock.contains("ProviderSection(title = stringResource(R.string.custom_api_configuration))"))
        assertTrue(editorBlock.contains("ButtonDefaults.buttonColors("))
        assertTrue(editorBlock.contains("containerColor = MaterialTheme.colorScheme.primary"))
        assertTrue(editorBlock.contains("shape = RoundedCornerShape(12.dp)"))
        assertTrue(presetBlock.contains("ButtonDefaults.outlinedButtonColors("))
        assertTrue(presetBlock.contains("ProviderLogo(providerName = preset.displayName"))
        assertTrue(presetBlock.contains("containerColor = MaterialTheme.colorScheme.surface"))
        assertTrue(presetBlock.contains("BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)"))
        assertTrue(screenSource.contains("shape = RoundedCornerShape(18.dp)"))
        assertTrue(screenSource.contains("focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant"))
        assertTrue(screenSource.contains("focusedBorderColor = MaterialTheme.colorScheme.primary"))
        assertTrue(strings.contains("<string name=\"current_configuration\">Current configuration</string>"))
        assertTrue(strings.contains("<string name=\"custom_api_configuration\">Custom API configuration</string>"))
        assertTrue(strings.contains("<string name=\"optional_settings\">Optional settings</string>"))
        assertTrue(strings.contains("<string name=\"delete_this_configuration\">Delete this configuration</string>"))
        assertTrue(zhStrings.contains("name=\"current_configuration\""))
        assertTrue(zhStrings.contains("name=\"custom_api_configuration\""))
        assertTrue(zhStrings.contains("name=\"optional_settings\""))
        assertTrue(zhStrings.contains("name=\"delete_this_configuration\""))
    }

    @Test
    fun providerPresetsUseOriginalLogoResourcesInsteadOfHandDrawnGlyphs() {
        val screenSource = File("src/main/java/com/flowchat/app/presentation/provider/ProviderSettingsScreen.kt").readText()

        assertFalse(screenSource.contains("private fun ProviderGlyph("))
        assertFalse(screenSource.contains("Canvas("))
        assertTrue(screenSource.contains("Image("))
        assertTrue(screenSource.contains("painterResource(providerLogoRes(providerName))"))
        assertTrue(screenSource.contains("R.drawable.provider_openai"))
        assertTrue(screenSource.contains("R.drawable.provider_claude"))
        assertTrue(screenSource.contains("R.drawable.provider_deepseek"))
        assertTrue(screenSource.contains("R.drawable.provider_gemini"))
        assertTrue(File("src/main/res/drawable-nodpi/provider_openai.png").exists())
        assertTrue(File("src/main/res/drawable-nodpi/provider_claude.png").exists())
        assertTrue(File("src/main/res/drawable-nodpi/provider_deepseek.png").exists())
        assertTrue(File("src/main/res/drawable-nodpi/provider_gemini.png").exists())
    }

    @Test
    fun providerRepositoryKeepsPresetProvidersSeparateFromBlankCustomConfiguration() {
        val repositorySource = File("src/main/java/com/flowchat/app/data/repository/RoomProviderRepository.kt").readText()
        val templateSource = File("src/main/java/com/flowchat/app/domain/provider/ProviderTemplates.kt").readText()

        assertFalse(repositorySource.contains("providerDao.deleteAllExcept(custom.id)"))
        assertFalse(repositorySource.contains("baseUrl = seed.baseUrl.ifBlank"))
        assertFalse(repositorySource.contains("defaultModel = seed.defaultModel.ifBlank"))
        assertTrue(repositorySource.contains("providerDao.upsert(custom.toEntity())"))
        assertTrue(templateSource.contains("baseUrl = \"\""))
        assertTrue(templateSource.contains("defaultModel = \"\""))
    }
}
