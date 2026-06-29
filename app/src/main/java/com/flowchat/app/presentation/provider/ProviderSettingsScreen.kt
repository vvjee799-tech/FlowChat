package com.flowchat.app.presentation.provider

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowchat.app.R
import com.flowchat.app.domain.provider.ProviderPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProviderSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = { Text(stringResource(R.string.providers)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        ProviderEditor(
            state = state,
            onUpdate = viewModel::updateSelected,
            onApiKey = viewModel::updateApiKey,
            onTavilyApiKey = viewModel::updateTavilyApiKey,
            onLoadModelOptions = viewModel::loadModelOptions,
            onSelectModelOption = viewModel::selectModelOption,
            onDismissModelOptions = viewModel::dismissModelOptions,
            onApplyPreset = viewModel::applyPreset,
            onSave = viewModel::save,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        )
    }
}

@Composable
private fun ProviderEditor(
    state: ProviderSettingsUiState,
    onUpdate: ((com.flowchat.app.domain.model.ProviderConfig) -> com.flowchat.app.domain.model.ProviderConfig) -> Unit,
    onApiKey: (String) -> Unit,
    onTavilyApiKey: (String) -> Unit,
    onLoadModelOptions: () -> Unit,
    onSelectModelOption: (String) -> Unit,
    onDismissModelOptions: () -> Unit,
    onApplyPreset: (ProviderPreset) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val provider = state.selected
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProviderPresetSection(
                state = state,
                onApplyPreset = onApplyPreset
            )
            Text(stringResource(R.string.custom_configuration), style = MaterialTheme.typography.titleMedium)
            if (provider == null) {
                Text(stringResource(R.string.no_provider_selected))
                return@Column
            }
            OutlinedTextField(
                value = provider.displayName,
                onValueChange = { value -> onUpdate { it.copy(displayName = value) } },
                label = { Text(stringResource(R.string.provider_name)) },
                shape = RoundedCornerShape(14.dp),
                colors = providerTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = provider.baseUrl,
                onValueChange = { value -> onUpdate { it.copy(baseUrl = value) } },
                label = { Text(stringResource(R.string.base_url)) },
                shape = RoundedCornerShape(14.dp),
                colors = providerTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = provider.defaultModel,
                    onValueChange = { value -> onUpdate { it.copy(defaultModel = value) } },
                    label = { Text(stringResource(R.string.model)) },
                    trailingIcon = {
                        IconButton(onClick = onLoadModelOptions) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.model_list)
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = providerTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = state.modelOptionsExpanded,
                    onDismissRequest = onDismissModelOptions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when {
                        state.isLoadingModels -> {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.load_models)) },
                                onClick = {},
                                enabled = false
                            )
                        }
                        state.modelListError != null -> {
                            DropdownMenuItem(
                                text = { Text(state.modelListError) },
                                onClick = {},
                                enabled = false
                            )
                        }
                        state.modelOptions.isEmpty() -> {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.no_models_found)) },
                                onClick = {},
                                enabled = false
                            )
                        }
                        else -> {
                            state.modelOptions.forEach { modelName ->
                                DropdownMenuItem(
                                    text = { Text(modelName) },
                                    onClick = { onSelectModelOption(modelName) }
                                )
                            }
                        }
                    }
                }
            }
            val savedMaskVisible = state.hasApiKey && state.apiKey.isBlank()
            val displayedApiKey = if (savedMaskVisible) SavedApiKeyDisplayValue else state.apiKey
            OutlinedTextField(
                value = displayedApiKey,
                onValueChange = { value ->
                    val normalizedValue = when {
                        savedMaskVisible && value == SavedApiKeyDisplayValue -> ""
                        savedMaskVisible && value.all { it == '•' } -> ""
                        savedMaskVisible && value.startsWith(SavedApiKeyDisplayValue) -> value.removePrefix(SavedApiKeyDisplayValue)
                        else -> value
                    }
                    onApiKey(normalizedValue)
                },
                visualTransformation = PasswordVisualTransformation(),
                label = { Text(stringResource(R.string.api_key)) },
                shape = RoundedCornerShape(14.dp),
                colors = providerTextFieldColors(),
                supportingText = {
                    if (state.hasApiKey) {
                        Text(stringResource(R.string.api_key_saved_hint))
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(stringResource(R.string.web_search_configuration), style = MaterialTheme.typography.titleMedium)
            val tavilySavedMaskVisible = state.hasTavilyApiKey && state.tavilyApiKey.isBlank()
            val displayedTavilyApiKey = if (tavilySavedMaskVisible) SavedApiKeyDisplayValue else state.tavilyApiKey
            OutlinedTextField(
                value = displayedTavilyApiKey,
                onValueChange = { value ->
                    val normalizedValue = when {
                        tavilySavedMaskVisible && value == SavedApiKeyDisplayValue -> ""
                        tavilySavedMaskVisible && value.all { it == SavedApiKeyDisplayValue.first() } -> ""
                        tavilySavedMaskVisible && value.startsWith(SavedApiKeyDisplayValue) -> value.removePrefix(SavedApiKeyDisplayValue)
                        else -> value
                    }
                    onTavilyApiKey(normalizedValue)
                },
                visualTransformation = PasswordVisualTransformation(),
                label = { Text(stringResource(R.string.tavily_api_key)) },
                shape = RoundedCornerShape(14.dp),
                colors = providerTextFieldColors(),
                supportingText = {
                    if (state.hasTavilyApiKey) {
                        Text(stringResource(R.string.api_key_saved_hint))
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun ProviderPresetSection(
    state: ProviderSettingsUiState,
    onApplyPreset: (ProviderPreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.provider_presets), style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.providerPresets.forEach { preset ->
                OutlinedButton(
                    onClick = { onApplyPreset(preset) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.widthIn(min = 104.dp)
                ) {
                    Text(preset.displayName)
                }
            }
        }
    }
}

@Composable
private fun providerTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedBorderColor = MaterialTheme.colorScheme.outline,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    cursorColor = MaterialTheme.colorScheme.onSurface
)
