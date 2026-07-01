package com.flowchat.app.presentation.provider

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
                title = { Text(stringResource(R.string.provider_configuration)) },
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
            onPresetApiKey = viewModel::updatePresetApiKey,
            onSavePresetApiKey = viewModel::savePresetApiKey,
            onDismissPresetApiKeyDialog = viewModel::dismissPresetApiKeyDialog,
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
    onPresetApiKey: (String) -> Unit,
    onSavePresetApiKey: () -> Unit,
    onDismissPresetApiKeyDialog: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val provider = state.selected
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProviderPresetSection(
            state = state,
            onApplyPreset = onApplyPreset
        )
        if (provider == null) {
            Text(stringResource(R.string.no_provider_selected))
            return@Column
        }
        Text(
            text = stringResource(R.string.current_configuration),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CurrentProviderCard(providerName = provider.displayName, modelName = provider.defaultModel)
        ProviderSection(title = stringResource(R.string.custom_api_configuration)) {
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
        }
        ProviderSection(title = stringResource(R.string.web_search_configuration)) {
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
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(stringResource(R.string.save))
        }
    }
    state.pendingPreset?.let { preset ->
        PresetApiKeyDialog(
            apiKey = state.presetApiKey,
            onApiKey = onPresetApiKey,
            onDismiss = onDismissPresetApiKeyDialog,
            onSave = onSavePresetApiKey
        )
    }
}

@Composable
private fun PresetApiKeyDialog(
    apiKey: String,
    onApiKey: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKey,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text(stringResource(R.string.preset_api_key_placeholder)) },
                shape = RoundedCornerShape(14.dp),
                colors = providerTextFieldColors(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun CurrentProviderCard(
    providerName: String,
    modelName: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProviderLogo(providerName = providerName, modifier = Modifier.size(36.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(providerName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                text = modelName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            text = stringResource(R.string.saved),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ProviderSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ProviderPresetSection(
    state: ProviderSettingsUiState,
    onApplyPreset: (ProviderPreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.providerPresets.forEach { preset ->
                OutlinedButton(
                    onClick = { onApplyPreset(preset) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .height(84.dp)
                        .widthIn(min = 84.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        ProviderLogo(providerName = preset.displayName, modifier = Modifier.size(28.dp))
                        Text(preset.displayName, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderLogo(
    providerName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(providerLogoRes(providerName)),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
        )
    }
}

private fun providerLogoRes(providerName: String): Int =
    when {
        providerName.contains("claude", ignoreCase = true) -> R.drawable.provider_claude
        providerName.contains("deep", ignoreCase = true) -> R.drawable.provider_deepseek
        providerName.contains("gemini", ignoreCase = true) -> R.drawable.provider_gemini
        providerName.contains("chat", ignoreCase = true) || providerName.contains("open", ignoreCase = true) -> R.drawable.provider_openai
        else -> R.drawable.provider_openai
    }

@Composable
private fun providerTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    cursorColor = MaterialTheme.colorScheme.onSurface
)
