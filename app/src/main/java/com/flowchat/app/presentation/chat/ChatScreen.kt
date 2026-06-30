package com.flowchat.app.presentation.chat

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Process
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowchat.app.R
import com.flowchat.app.domain.model.Conversation
import com.flowchat.app.domain.model.Message
import com.flowchat.app.domain.model.MessageRole
import com.flowchat.app.domain.model.MessageStatus
import com.flowchat.app.locale.AppLanguage
import com.flowchat.app.locale.AppLocale
import com.flowchat.app.presentation.common.findActivity
import com.flowchat.app.ui.theme.AppAppearance
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val OriginalSettingsIcon = Icons.Default.Settings
private const val BottomMessageAnchorKey = "bottom-message-anchor"

@Composable
private fun DrawerMenuIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 3.dp.toPx()
        val lineStart = size.width * 0.20f
        val topLineEnd = size.width * 0.82f
        val bottomLineEnd = size.width * 0.58f
        val topY = size.height * 0.40f
        val bottomY = size.height * 0.62f

        drawLine(
            color = color,
            start = Offset(lineStart, topY),
            end = Offset(topLineEnd, topY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(lineStart, bottomY),
            end = Offset(bottomLineEnd, bottomY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun ConversationSettingsIcon(
    color: Color,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        }
    ) {
        val strokeWidth = 2.dp.toPx()
        val knobRadius = 2.7.dp.toPx()
        val lineStart = size.width * 0.14f
        val lineEnd = size.width * 0.86f
        val rows = listOf(
            Offset(size.width * 0.32f, size.height * 0.28f),
            Offset(size.width * 0.68f, size.height * 0.50f),
            Offset(size.width * 0.44f, size.height * 0.72f)
        )

        rows.forEach { knob ->
            drawLine(
                color = color,
                start = Offset(lineStart, knob.y),
                end = Offset(lineEnd, knob.y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = color,
                radius = knobRadius,
                center = knob
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    onOpenProviders: () -> Unit,
    appAppearance: AppAppearance,
    onAppAppearanceChange: (AppAppearance) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val defaultUserName = stringResource(R.string.user_display_name)
    val initialUserProfile = remember(context, defaultUserName) {
        loadUserProfile(context, defaultUserName)
    }
    var userName by remember(context, defaultUserName) { mutableStateOf(initialUserProfile.name) }
    var userAvatarPath by remember(context, defaultUserName) { mutableStateOf(initialUserProfile.avatarPath) }
    var usageAccessGranted by remember(context) { mutableStateOf(hasUsageStatsPermission(context)) }
    var showSettings by remember { mutableStateOf(false) }
    var showUserSettings by remember { mutableStateOf(false) }
    var conversationPendingDelete by remember { mutableStateOf<Conversation?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerContentColor = MaterialTheme.colorScheme.onBackground
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 8.dp)
                        )
                        UserProfileEntry(
                            userName = userName,
                            avatarPath = userAvatarPath,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    usageAccessGranted = hasUsageStatsPermission(context)
                                    showUserSettings = true
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                        HorizontalDivider()
                        Text(
                            text = stringResource(R.string.conversation_list),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                        )
                        state.conversations.forEach { conversation ->
                            ListItem(
                                headlineContent = { Text(conversation.displayTitle(), maxLines = 1) },
                                supportingContent = { Text(conversation.modelName, maxLines = 1) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            viewModel.selectConversation(conversation.id)
                                            scope.launch { drawerState.close() }
                                        },
                                        onLongClick = {
                                            conversationPendingDelete = conversation
                                        }
                                    ),
                                trailingContent = {
                                    if (conversation.id == state.currentConversation?.id) {
                                        Text(
                                            stringResource(R.string.active),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                    OutlinedIconButton(
                        onClick = { viewModel.newConversation() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(24.dp)
                            .size(32.dp),
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val plusColor = MaterialTheme.colorScheme.onSurface
                            Canvas(modifier = Modifier.size(18.dp)) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val halfLength = size.minDimension * 0.34f
                                val strokeWidth = 4.dp.toPx()
                                drawLine(
                                    color = plusColor,
                                    start = center.copy(x = center.x - halfLength),
                                    end = center.copy(x = center.x + halfLength),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = plusColor,
                                    start = center.copy(y = center.y - halfLength),
                                    end = center.copy(y = center.y + halfLength),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.currentConversation?.displayAssistantTitle() ?: stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1
                            )
                            Text(
                                state.currentConversation?.modelName.orEmpty(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            DrawerMenuIcon(
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    },
                    actions = {
                        if (state.currentConversation != null) {
                            IconButton(
                                onClick = { showSettings = true }
                            ) {
                                ConversationSettingsIcon(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    contentDescription = stringResource(R.string.conversation_settings),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                MessageComposer(
                    value = state.input,
                    enabled = state.currentConversation != null,
                    isStreaming = state.isStreaming,
                    webSearchEnabled = state.webSearchEnabled,
                    onValueChange = viewModel::updateInput,
                    onSend = viewModel::send,
                    onStop = viewModel::stop,
                    onToggleWebSearch = viewModel::toggleWebSearch
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp)
            ) {
                if (state.errorMessage != null) {
                    AssistChip(onClick = {}, label = { Text(state.errorMessage.orEmpty()) })
                    Spacer(Modifier.height(8.dp))
                }
                MessageList(messages = state.messages, assistantAvatarPath = state.currentConversation?.assistantAvatarPath, userAvatarPath = userAvatarPath, showAvatars = state.currentConversation?.showAvatars == true, modifier = Modifier.weight(1f))
                state.toolCallStatus?.let { status ->
                    ToolCallStatusCard(status)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showSettings && state.currentConversation != null) {
        ConversationSettingsSheet(
            conversation = state.currentConversation!!,
            onDismiss = { showSettings = false },
            onSave = { assistantName, assistantAvatarPath, showAvatars, prompt, temperature, topP, maxTokens ->
                viewModel.updateConversationSettings(
                    assistantName,
                    assistantAvatarPath,
                    showAvatars,
                    prompt,
                    temperature,
                    topP,
                    maxTokens
                )
                showSettings = false
            }
        )
    }

    if (showUserSettings) {
        UserProfileSettingsSheet(
            userName = userName,
            avatarPath = userAvatarPath,
            onDismiss = { showUserSettings = false },
            onSave = { profileName, profileAvatarPath ->
                val normalizedName = profileName.trim().ifBlank { defaultUserName }
                saveUserProfile(context, profileName, profileAvatarPath)
                userName = normalizedName
                userAvatarPath = profileAvatarPath
                showUserSettings = false
            },
            onOpenModelProviders = {
                showUserSettings = false
                onOpenProviders()
            },
            usageAccessGranted = usageAccessGranted,
            onOpenUsageAccessSettings = {
                usageAccessGranted = hasUsageStatsPermission(context)
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            },
            appAppearance = appAppearance,
            onAppAppearanceChange = onAppAppearanceChange
        )
    }

    conversationPendingDelete?.let { conversation ->
        AlertDialog(
            onDismissRequest = { conversationPendingDelete = null },
            title = { Text(stringResource(R.string.delete_conversation_title)) },
            text = { Text(stringResource(R.string.delete_conversation_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConversation(conversation.id)
                        conversationPendingDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationPendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun MessageList(
    messages: List<Message>,
    assistantAvatarPath: String?,
    userAvatarPath: String?,
    showAvatars: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val lastMessage = messages.lastOrNull()

    LaunchedEffect(messages.size, lastMessage?.id, lastMessage?.content, lastMessage?.reasoningContent) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(message, assistantAvatarPath, userAvatarPath, showAvatars)
        }
        item(key = BottomMessageAnchorKey) {
            Spacer(Modifier.height(1.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserProfileEntry(
    userName: String,
    avatarPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onClick
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProfileAvatar(
            avatarPath = avatarPath,
            size = 48.dp
        )
        Text(
            text = userName,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Icon(
            Icons.Default.Settings,
            contentDescription = stringResource(R.string.settings),
            modifier = Modifier.size(28.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileSettingsSheet(
    userName: String,
    avatarPath: String?,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
    onOpenModelProviders: () -> Unit,
    usageAccessGranted: Boolean,
    onOpenUsageAccessSettings: () -> Unit,
    appAppearance: AppAppearance,
    onAppAppearanceChange: (AppAppearance) -> Unit
) {
    val context = LocalContext.current
    var profileName by remember(userName) { mutableStateOf(userName) }
    var profileAvatarPath by remember(avatarPath) { mutableStateOf(avatarPath) }
    val settingsScope = rememberCoroutineScope()
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            settingsScope.launch {
                val path = withContext(Dispatchers.IO) {
                    copyUserAvatarToPrivateFile(context, uri)
                }
                if (path != null) {
                    profileAvatarPath = path
                }
            }
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                }
            }
            Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                ProfileAvatar(
                    avatarPath = profileAvatarPath,
                    size = 96.dp
                )
                AvatarEditButton(
                    onClick = { avatarPicker.launch("image/*") },
                    contentDescription = stringResource(R.string.assistant_avatar),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                )
            }
            OutlinedTextField(
                value = profileName,
                onValueChange = { profileName = it },
                label = { Text(stringResource(R.string.profile_name)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = flowTextFieldColors(),
                singleLine = true
            )
            Button(
                onClick = { onSave(profileName, profileAvatarPath) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRow(
                        title = stringResource(R.string.model_provider_configuration),
                        onClick = onOpenModelProviders
                    )
                    HorizontalDivider()
                    SettingsRow(
                        title = stringResource(R.string.app_usage_access),
                        supportingText = stringResource(R.string.life_tools_permissions),
                        trailingText = stringResource(if (usageAccessGranted) R.string.permission_enabled else R.string.permission_open),
                        onClick = onOpenUsageAccessSettings
                    )
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.background_settings),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BackgroundModeButton(
                                title = stringResource(R.string.background_light),
                                selected = appAppearance == AppAppearance.Light,
                                previewBackgroundColor = Color.White,
                                previewTextColor = Color.Black,
                                onClick = { onAppAppearanceChange(AppAppearance.Light) },
                                modifier = Modifier.weight(1f)
                            )
                            BackgroundModeButton(
                                title = stringResource(R.string.background_dark),
                                selected = appAppearance == AppAppearance.Dark,
                                previewBackgroundColor = Color.Black,
                                previewTextColor = Color.White,
                                onClick = { onAppAppearanceChange(AppAppearance.Dark) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.language),
                            style = MaterialTheme.typography.titleMedium
                        )
                        LanguageSelector(showLabel = false)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BackgroundModeButton(
    title: String,
    selected: Boolean,
    previewBackgroundColor: Color,
    previewTextColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(previewBackgroundColor, RoundedCornerShape(10.dp))
            .border(BorderStroke(if (selected) 2.dp else 1.dp, borderColor), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = previewTextColor,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    supportingText: String? = null,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun flowTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedBorderColor = MaterialTheme.colorScheme.outline,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    cursorColor = MaterialTheme.colorScheme.onSurface
)

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MessageBubble(message: Message, assistantAvatarPath: String?, userAvatarPath: String?, showAvatars: Boolean) {
    val isUser = message.role == MessageRole.User
    val hasReasoning = !isUser && message.reasoningContent.isNotBlank()
    val isWaitingForAssistant =
        !isUser && message.status == MessageStatus.Streaming && message.content.isBlank() && !hasReasoning
    val shouldShowContentBubble =
        isUser || message.content.isNotBlank() || isWaitingForAssistant || (!hasReasoning && message.role == MessageRole.Assistant)
    val context = LocalContext.current
    val messageTime = remember(context, message.createdAt) {
        formatMessageTime(context, message.createdAt)
    }
    val clipboardManager = LocalClipboardManager.current
    var showMessageActions by remember { mutableStateOf(false) }
    var showTextSelection by remember { mutableStateOf(false) }
    val displayContent = when {
        message.content.isNotBlank() -> message.content
        message.role == MessageRole.Assistant -> stringResource(R.string.empty_response_message)
        else -> "..."
    }
    val copyText = if (!shouldShowContentBubble || isWaitingForAssistant) "" else displayContent
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val bubbleContentColor = if (isUser) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 22.dp, topEnd = 8.dp, bottomEnd = 22.dp, bottomStart = 22.dp)
    } else {
        RoundedCornerShape(topStart = 8.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp)
    }
    val bubbleMaxWidth = if (showAvatars) 286.dp else 320.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (showAvatars && !isUser) {
            AssistantAvatar(
                avatarPath = assistantAvatarPath,
                size = 38.dp
            )
            Spacer(Modifier.size(8.dp))
        }
        Box(modifier = Modifier.widthIn(max = bubbleMaxWidth)) {
            Column(modifier = Modifier.widthIn(max = bubbleMaxWidth)) {
                if (!isUser && message.reasoningContent.isNotBlank()) {
                    ReasoningBubble(message = message, maxWidth = bubbleMaxWidth)
                    Spacer(Modifier.size(6.dp))
                }
                if (shouldShowContentBubble) {
                    Card(
                        modifier = Modifier
                            .widthIn(min = 56.dp, max = bubbleMaxWidth)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    if (copyText.isNotBlank()) {
                                        showMessageActions = true
                                    }
                                }
                            ),
                        shape = bubbleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = bubbleColor,
                            contentColor = bubbleContentColor
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                            if (isWaitingForAssistant) {
                                StreamingJumpingDots()
                            } else {
                                Text(
                                    text = displayContent,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = bubbleContentColor
                                )
                            }
                        }
                    }
                }
                Text(
                    text = messageTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .align(if (isUser) Alignment.End else Alignment.Start)
                )
            }
            DropdownMenu(
                expanded = showMessageActions,
                onDismissRequest = { showMessageActions = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.copy_message)) },
                    onClick = {
                        clipboardManager.setText(AnnotatedString(copyText))
                        showMessageActions = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.select_text)) },
                    onClick = {
                        showMessageActions = false
                        showTextSelection = true
                    }
                )
            }
        }
        if (showAvatars && isUser) {
            Spacer(Modifier.size(8.dp))
            ProfileAvatar(
                avatarPath = userAvatarPath,
                size = 38.dp
            )
        }
    }

    if (showTextSelection) {
        AlertDialog(
            onDismissRequest = { showTextSelection = false },
            text = {
                SelectionContainer {
                    Text(copyText)
                }
            },
            confirmButton = {
                TextButton(onClick = { showTextSelection = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ReasoningBubble(message: Message, maxWidth: Dp) {
    var expanded by remember(message.id, message.status, message.content.isBlank()) {
        mutableStateOf(message.status == MessageStatus.Streaming && message.content.isBlank())
    }
    val isStillThinking = message.status == MessageStatus.Streaming && message.content.isBlank()
    val elapsedSeconds = ((message.updatedAt - message.createdAt) / 1000L).coerceAtLeast(1L)
    val title = when {
        message.status == MessageStatus.Stopped -> stringResource(R.string.thinking_stopped)
        isStillThinking -> stringResource(R.string.thinking_in_progress)
        else -> stringResource(R.string.thinking_done, elapsedSeconds)
    }

    Card(
        modifier = Modifier
            .widthIn(min = 160.dp, max = maxWidth)
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        if (expanded) R.string.thinking_collapse else R.string.thinking_expand
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Spacer(Modifier.size(8.dp))
                Text(
                    text = message.reasoningContent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatMessageTime(context: Context, timestampMillis: Long): String =
    android.text.format.DateFormat.getTimeFormat(context).format(Date(timestampMillis))

@Composable
private fun StreamingJumpingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "streamingJumpingDots")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Restart
        ),
        label = "streamingJumpingProgress"
    )
    val dotColor = MaterialTheme.colorScheme.onSurfaceVariant
    val phaseOffsets = listOf(0f, 0.18f, 0.36f)

    Canvas(modifier = modifier.size(width = 42.dp, height = 22.dp)) {
        val dotRadius = 3.dp.toPx()
        val jumpHeight = dotRadius * 2.2f
        val spacing = dotRadius * 3.2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val centers = listOf(
            center.copy(x = center.x - spacing),
            center,
            center.copy(x = center.x + spacing)
        )

        centers.forEachIndexed { index, dotCenter ->
            val phase = (progress - phaseOffsets[index] + 1f) % 1f
            val jump = if (phase < 0.5f) {
                1f - kotlin.math.abs(phase - 0.25f) / 0.25f
            } else {
                0f
            }
            val radius = dotRadius * (1f + (jump * 0.18f))
            val alpha = 0.45f + (jump * 0.55f)

            drawCircle(
                color = dotColor.copy(alpha = alpha),
                radius = radius,
                center = dotCenter.copy(y = dotCenter.y - (jumpHeight * jump))
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MessageComposer(
    value: String,
    enabled: Boolean,
    isStreaming: Boolean,
    webSearchEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onToggleWebSearch: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val inputMethodManager = remember(view) {
        view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }
    val hasInput = value.isNotBlank()
    val isDarkBackground = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val sendButtonContainerColor = when {
        isStreaming || hasInput -> if (isDarkBackground) Color.White else Color.Black
        else -> Color(0xFFBDBDBD)
    }
    val sendButtonContentColor = if (isDarkBackground) Color.Black else Color.White

    fun showKeyboardForFocusedInput() {
        if (!enabled) return
        keyboardController?.show()
        view.post {
            inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_FORCED)
        }
    }

    fun requestFocusAndShowKeyboard() {
        if (!enabled) return
        focusRequester.requestFocus()
        showKeyboardForFocusedInput()
    }

    fun submitMessage() {
        if (!enabled) return
        if (value.isBlank()) return
        onSend()
        onValueChange("")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = if (enabled) onValueChange else { _ -> },
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (enabled && focusState.isFocused) {
                            showKeyboardForFocusedInput()
                        }
                    }
                    .pointerInput(enabled, keyboardController, view) {
                        if (!enabled) return@pointerInput
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            requestFocusAndShowKeyboard()
                        }
                },
                placeholder = {
                    Text(
                        text = stringResource(if (enabled) R.string.message_hint else R.string.create_conversation_to_chat),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submitMessage() }),
                maxLines = 5
            )
            IconButton(
                onClick = if (isStreaming) onStop else ::submitMessage,
                enabled = enabled || isStreaming,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(sendButtonContainerColor, CircleShape)
            ) {
                if (isStreaming) {
                    StreamingStopIcon(color = sendButtonContentColor)
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = sendButtonContentColor
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleWebSearch,
                enabled = enabled && !isStreaming,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (webSearchEnabled) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            Color.Transparent
                        },
                        CircleShape
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (webSearchEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        CircleShape
                    )
            ) {
                Image(
                    painter = painterResource(
                        if (webSearchEnabled) R.drawable.ic_web_search_on else R.drawable.ic_web_search_off
                    ),
                    contentDescription = stringResource(
                        if (webSearchEnabled) R.string.web_search_on else R.string.web_search_off
                    ),
                    colorFilter = if (webSearchEnabled) null else ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun StreamingStopIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val side = 16.dp.toPx()
        val topLeft = Offset(
            x = (size.width - side) / 2f,
            y = (size.height - side) / 2f
        )
        drawRoundRect(
            color = color,
            topLeft = topLeft,
            size = Size(side, side),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
    }
}

@Composable
private fun ToolCallStatusCard(status: ToolCallStatusUi) {
    val textRes = when (status.phase) {
        ToolCallPhase.Running -> R.string.tool_call_running
        ToolCallPhase.Complete -> R.string.tool_call_complete
        ToolCallPhase.Failed -> R.string.tool_call_failed
    }
    val accentColor = when (status.phase) {
        ToolCallPhase.Running -> MaterialTheme.colorScheme.primary
        ToolCallPhase.Complete -> MaterialTheme.colorScheme.tertiary
        ToolCallPhase.Failed -> MaterialTheme.colorScheme.error
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(textRes, status.toolName),
                    style = MaterialTheme.typography.labelLarge
                )
                if (!status.detail.isNullOrBlank()) {
                    Text(
                        text = status.detail,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationSettingsSheet(
    conversation: Conversation,
    onDismiss: () -> Unit,
    onSave: (String, String?, Boolean, String, Double, Double, Int?) -> Unit
) {
    val context = LocalContext.current
    var assistantName by remember(conversation.id) { mutableStateOf(conversation.assistantName) }
    var assistantAvatarPath by remember(conversation.id) { mutableStateOf(conversation.assistantAvatarPath) }
    var showAvatars by remember(conversation.id) { mutableStateOf(conversation.showAvatars) }
    var prompt by remember(conversation.id) { mutableStateOf(conversation.systemPrompt) }
    var temperature by remember(conversation.id) { mutableStateOf(conversation.temperature.toFloat()) }
    var topP by remember(conversation.id) { mutableStateOf(conversation.topP.toFloat()) }
    var maxTokens by remember(conversation.id) { mutableStateOf(conversation.maxTokens?.toString().orEmpty()) }
    val settingsScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            settingsScope.launch {
                val path = withContext(Dispatchers.IO) {
                    copyAssistantAvatarToPrivateFile(context, conversation.id, uri)
                }
                if (path != null) {
                    assistantAvatarPath = path
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(stringResource(R.string.conversation_settings), style = MaterialTheme.typography.titleLarge)
                AssistantProfileEditor(
                    assistantName = assistantName,
                    assistantAvatarPath = assistantAvatarPath,
                    onAssistantNameChange = { assistantName = it },
                    onAvatarClick = { avatarPicker.launch("image/*") }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.show_avatars),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Switch(
                        checked = showAvatars,
                        onCheckedChange = { showAvatars = it }
                    )
                }
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(stringResource(R.string.system_prompt)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = flowTextFieldColors(),
                    minLines = 4,
                    maxLines = 8
                )
                Text("${stringResource(R.string.temperature)}: ${"%.2f".format(temperature)}")
                Slider(value = temperature, onValueChange = { temperature = it }, valueRange = 0f..2f)
                Text("${stringResource(R.string.top_p)}: ${"%.2f".format(topP)}")
                Slider(value = topP, onValueChange = { topP = it }, valueRange = 0f..1f)
                OutlinedTextField(
                    value = maxTokens,
                    onValueChange = { value -> maxTokens = value.filter { it.isDigit() } },
                    label = { Text(stringResource(R.string.max_tokens)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = flowTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = {
                    onSave(assistantName, assistantAvatarPath, showAvatars, prompt, temperature.toDouble(), topP.toDouble(), maxTokens.toIntOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun AssistantProfileEditor(
    assistantName: String,
    assistantAvatarPath: String?,
    onAssistantNameChange: (String) -> Unit,
    onAvatarClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            AssistantAvatar(
                avatarPath = assistantAvatarPath,
                size = 96.dp
            )
            AvatarEditButton(
                onClick = onAvatarClick,
                contentDescription = stringResource(R.string.assistant_avatar),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
            )
        }
        OutlinedTextField(
            value = assistantName,
            onValueChange = onAssistantNameChange,
            label = { Text(stringResource(R.string.assistant_name)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = flowTextFieldColors(),
            singleLine = true
        )
    }
}

@Composable
private fun AvatarEditButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFF3A3A3A), CircleShape)
            .border(BorderStroke(1.dp, Color.Black.copy(alpha = 0.35f)), CircleShape)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val pencilColor = Color.White
        Canvas(modifier = Modifier.size(17.dp)) {
            val strokeWidth = 2.1.dp.toPx()
            val upperBodyStart = Offset(size.width * 0.29f, size.height * 0.68f)
            val upperBodyEnd = Offset(size.width * 0.64f, size.height * 0.33f)
            val lowerBodyStart = Offset(size.width * 0.40f, size.height * 0.79f)
            val lowerBodyEnd = Offset(size.width * 0.75f, size.height * 0.44f)
            val pencilTip = Offset(size.width * 0.23f, size.height * 0.85f)
            val tailStart = upperBodyEnd
            val tailEnd = lowerBodyEnd

            drawLine(
                color = pencilColor,
                start = upperBodyStart,
                end = upperBodyEnd,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = pencilColor,
                start = lowerBodyStart,
                end = lowerBodyEnd,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = pencilColor,
                start = tailStart,
                end = tailEnd,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = pencilColor,
                start = upperBodyStart,
                end = pencilTip,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = pencilColor,
                start = lowerBodyStart,
                end = pencilTip,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ProfileAvatar(
    avatarPath: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val image = remember(avatarPath) {
        avatarPath?.let { path ->
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(R.mipmap.ic_launcher),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun AssistantAvatar(
    avatarPath: String?,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    val image = remember(avatarPath) {
        avatarPath?.let { path ->
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "AI",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class UserProfile(
    val name: String,
    val avatarPath: String?
)

private fun loadUserProfile(context: Context, defaultName: String): UserProfile {
    val prefs = context.getSharedPreferences(UserProfilePrefsName, Context.MODE_PRIVATE)
    return UserProfile(
        name = prefs.getString(UserProfileNameKey, null)?.takeIf { it.isNotBlank() } ?: defaultName,
        avatarPath = prefs.getString(UserProfileAvatarKey, null)?.takeIf { it.isNotBlank() }
    )
}

private fun saveUserProfile(context: Context, profileName: String, profileAvatarPath: String?) {
    context.getSharedPreferences(UserProfilePrefsName, Context.MODE_PRIVATE)
        .edit()
        .putString(UserProfileNameKey, profileName.trim())
        .putString(UserProfileAvatarKey, profileAvatarPath.orEmpty())
        .apply()
}

private fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun copyUserAvatarToPrivateFile(context: Context, uri: Uri): String? {
    return runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: return null
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val scaled = bitmap.scaleToMax(512)
        val directory = File(context.filesDir, "user_profile").apply { mkdirs() }
        val output = File(directory, "avatar.jpg")

        try {
            FileOutputStream(output).use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 92, stream)
            }
        } finally {
            if (scaled != bitmap) {
                scaled.recycle()
            }
            bitmap.recycle()
        }
        output.absolutePath
    }.getOrNull()
}

private fun copyAssistantAvatarToPrivateFile(context: Context, conversationId: String, uri: Uri): String? {
    return runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: return null
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val scaled = bitmap.scaleToMax(512)
        val directory = File(context.filesDir, "assistant_avatars").apply { mkdirs() }
        val output = File(directory, "$conversationId.jpg")

        try {
            FileOutputStream(output).use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 92, stream)
            }
        } finally {
            if (scaled != bitmap) {
                scaled.recycle()
            }
            bitmap.recycle()
        }
        output.absolutePath
    }.getOrNull()
}

private fun Bitmap.scaleToMax(maxSize: Int): Bitmap {
    val largestSide = maxOf(width, height)
    if (largestSide <= maxSize) return this
    val ratio = maxSize.toFloat() / largestSide.toFloat()
    val scaledWidth = (width * ratio).toInt().coerceAtLeast(1)
    val scaledHeight = (height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
}

private const val UserProfilePrefsName = "user_profile"
private const val UserProfileNameKey = "name"
private const val UserProfileAvatarKey = "avatar_path"

@Composable
private fun LanguageSelector(modifier: Modifier = Modifier, showLabel: Boolean = true) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(AppLocale.getLanguage(context)) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (showLabel) {
            Text(stringResource(R.string.language), style = MaterialTheme.typography.labelLarge)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LanguageChip(
                text = stringResource(R.string.language_system),
                selected = currentLanguage == AppLanguage.System,
                onClick = {
                    currentLanguage = AppLanguage.System
                    AppLocale.setLanguage(context, AppLanguage.System)
                    context.findActivity()?.recreate()
                }
            )
            LanguageChip(
                text = stringResource(R.string.language_zh_cn),
                selected = currentLanguage == AppLanguage.ChineseSimplified,
                onClick = {
                    currentLanguage = AppLanguage.ChineseSimplified
                    AppLocale.setLanguage(context, AppLanguage.ChineseSimplified)
                    context.findActivity()?.recreate()
                }
            )
            LanguageChip(
                text = stringResource(R.string.language_en),
                selected = currentLanguage == AppLanguage.English,
                onClick = {
                    currentLanguage = AppLanguage.English
                    AppLocale.setLanguage(context, AppLanguage.English)
                    context.findActivity()?.recreate()
                }
            )
        }
    }
}

@Composable
private fun LanguageChip(text: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = if (selected) "$text *" else text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    )
}

@Composable
private fun Conversation.displayTitle(): String {
    return if (title == "New chat") stringResource(R.string.default_conversation_title) else title
}

@Composable
private fun Conversation.displayAssistantTitle(): String {
    return assistantName.ifBlank { displayTitle() }
}
