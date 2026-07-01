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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowchat.app.BuildConfig
import com.flowchat.app.R
import com.flowchat.app.domain.model.Conversation
import com.flowchat.app.domain.model.Message
import com.flowchat.app.domain.model.MessageRole
import com.flowchat.app.domain.model.MessageStatus
import com.flowchat.app.domain.model.ProviderConfig
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
private val ComposerToolHeight = 34.dp
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
    val backgroundColor = MaterialTheme.colorScheme.background
    Canvas(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        }
    ) {
        val lineStrokeWidth = 2.35.dp.toPx()
        val knobStrokeWidth = 1.95.dp.toPx()
        val knobRadius = 3.5.dp.toPx()
        val lineStart = size.width * 0.06f
        val lineEnd = size.width * 0.94f
        val topKnob = Offset(size.width * 0.71f, size.height * 0.32f)
        val bottomKnob = Offset(size.width * 0.32f, size.height * 0.68f)

        listOf(topKnob, bottomKnob).forEach { knob ->
            drawLine(
                color = color,
                start = Offset(lineStart, knob.y),
                end = Offset(lineEnd, knob.y),
                strokeWidth = lineStrokeWidth,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = backgroundColor,
                radius = knobRadius,
                center = knob
            )
            drawCircle(
                color = color,
                radius = knobRadius,
                center = knob,
                style = Stroke(width = knobStrokeWidth)
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
    var showSettings by remember { mutableStateOf(false) }
    var showUserSettings by remember { mutableStateOf(false) }
    var showAppSettings by remember { mutableStateOf(false) }
    var conversationPendingDelete by remember { mutableStateOf<Conversation?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerShape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, top = 26.dp, end = 20.dp, bottom = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        UserProfileEntry(
                            userName = userName,
                            supportingText = stringResource(R.string.app_name),
                            avatarPath = userAvatarPath,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    showUserSettings = true
                                }
                            },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        DrawerNavRow(
                            label = stringResource(R.string.providers),
                            icon = DrawerNavIcon.Provider,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    onOpenProviders()
                                }
                            }
                        )
                        DrawerNavRow(
                            label = stringResource(R.string.settings),
                            icon = DrawerNavIcon.Settings,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    showAppSettings = true
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.conversation_history),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = stringResource(R.string.edit),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        state.conversations.forEach { conversation ->
                            val isActiveConversation = conversation.id == state.currentConversation?.id
                            DrawerConversationRow(
                                conversation = conversation,
                                isActive = isActiveConversation,
                                trailingText = if (isActiveConversation) {
                                    stringResource(R.string.active)
                                } else {
                                    null
                                },
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                                    .combinedClickable(
                                        onClick = {
                                            viewModel.selectConversation(conversation.id)
                                            scope.launch { drawerState.close() }
                                        },
                                        onLongClick = {
                                            conversationPendingDelete = conversation
                                        }
                                    )
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.newConversation() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(24.dp)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val plusColor = MaterialTheme.colorScheme.onPrimary
                            Canvas(modifier = Modifier.size(26.dp)) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val halfLength = size.minDimension * 0.36f
                                val strokeWidth = 3.dp.toPx()
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
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    title = {
                        ConversationHeaderTitle(
                            assistantAvatarPath = state.currentConversation?.assistantAvatarPath,
                            title = state.currentConversation?.displayAssistantTitle() ?: stringResource(R.string.app_name),
                            modelName = state.currentConversation?.modelName.orEmpty()
                        )
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
                    providers = state.providers,
                    currentConversation = state.currentConversation,
                    onValueChange = viewModel::updateInput,
                    onSend = viewModel::send,
                    onStop = viewModel::stop,
                    onToggleWebSearch = viewModel::toggleWebSearch,
                    onSelectModel = viewModel::updateConversationModel
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
        UserProfileSheet(
            userName = userName,
            avatarPath = userAvatarPath,
            onDismiss = { showUserSettings = false },
            onSave = { profileName, profileAvatarPath ->
                val normalizedName = profileName.trim().ifBlank { defaultUserName }
                saveUserProfile(context, profileName, profileAvatarPath)
                userName = normalizedName
                userAvatarPath = profileAvatarPath
                showUserSettings = false
            }
        )
    }

    if (showAppSettings) {
        AppSettingsSheet(
            appAppearance = appAppearance,
            onAppAppearanceChange = onAppAppearanceChange,
            onDismiss = { showAppSettings = false }
        )
    }

    state.usageAccessPermissionRequest?.let { request ->
        UsageAccessPermissionDialog(
            toolName = request.toolName,
            onDismiss = viewModel::dismissUsageAccessPermissionRequest,
            onOpenSettings = {
                viewModel.dismissUsageAccessPermissionRequest()
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            }
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

@Composable
private fun ConversationHeaderTitle(
    assistantAvatarPath: String?,
    title: String,
    modelName: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AssistantAvatar(
            avatarPath = assistantAvatarPath,
            size = 34.dp
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1
            )
            if (modelName.isNotBlank()) {
                Text(
                    text = modelName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

private enum class DrawerNavIcon {
    Provider,
    Settings
}

private fun drawerNavImageVector(icon: DrawerNavIcon): ImageVector =
    when (icon) {
        DrawerNavIcon.Provider -> Icons.Outlined.Hub
        DrawerNavIcon.Settings -> Icons.Default.Settings
    }

@Composable
private fun DrawerNavRow(
    label: String,
    icon: DrawerNavIcon,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = drawerNavImageVector(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerConversationRow(
    conversation: Conversation,
    isActive: Boolean,
    trailingText: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isActive) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f)
                } else {
                    Color.Transparent
                },
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.displayTitle(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1
            )
            Text(
                text = conversation.modelName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserProfileEntry(
    userName: String,
    supportingText: String,
    avatarPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileSheet(
    userName: String,
    avatarPath: String?,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    val context = LocalContext.current
    var profileName by remember(userName) { mutableStateOf(userName) }
    var profileAvatarPath by remember(avatarPath) { mutableStateOf(avatarPath) }
    val profileScope = rememberCoroutineScope()
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            profileScope.launch {
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
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.save))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppSettingsSheet(
    appAppearance: AppAppearance,
    onAppAppearanceChange: (AppAppearance) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAppearanceDialog by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRow(
                        title = stringResource(R.string.language),
                        icon = Icons.Default.Language,
                        onClick = {}
                    ) {
                        LanguageSelector(showLabel = false)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))
                    SettingsRow(
                        title = stringResource(R.string.appearance),
                        icon = Icons.Default.Palette,
                        trailingText = stringResource(appAppearance.labelRes()),
                        onClick = { showAppearanceDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))
                    SettingsRow(
                        title = stringResource(R.string.version),
                        icon = Icons.Default.Info,
                        trailingText = BuildConfig.VERSION_NAME,
                        onClick = {}
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
    if (showAppearanceDialog) {
        AppearanceDialog(
            selected = appAppearance,
            onSelect = onAppAppearanceChange,
            onDismiss = { showAppearanceDialog = false }
        )
    }
}

@Composable
private fun AppearanceDialog(
    selected: AppAppearance,
    onSelect: (AppAppearance) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.appearance),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                AppearanceOptionRow(
                    appearance = AppAppearance.System,
                    label = stringResource(R.string.appearance_system),
                    selected = selected == AppAppearance.System,
                    onClick = {
                        onSelect(AppAppearance.System)
                        onDismiss()
                    }
                )
                AppearanceOptionRow(
                    appearance = AppAppearance.Light,
                    label = stringResource(R.string.appearance_light),
                    selected = selected == AppAppearance.Light,
                    onClick = {
                        onSelect(AppAppearance.Light)
                        onDismiss()
                    }
                )
                AppearanceOptionRow(
                    appearance = AppAppearance.Dark,
                    label = stringResource(R.string.appearance_dark),
                    selected = selected == AppAppearance.Dark,
                    onClick = {
                        onSelect(AppAppearance.Dark)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun AppearanceOptionRow(
    appearance: AppAppearance,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppearanceRadioDot(selected = selected)
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AppearanceRadioDot(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val ringColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fillColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.size(20.dp)) {
        drawCircle(
            color = ringColor,
            radius = size.minDimension * 0.38f,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = 2.5.dp.toPx())
        )
        if (selected) {
            drawCircle(
                color = fillColor,
                radius = size.minDimension * 0.18f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }
    }
}

private fun AppAppearance.labelRes(): Int =
    when (this) {
        AppAppearance.System -> R.string.appearance_system
        AppAppearance.Light -> R.string.appearance_light
        AppAppearance.Dark -> R.string.appearance_dark
    }

@Composable
private fun UsageAccessPermissionDialog(
    toolName: String,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.usage_access_permission_title))
        },
        text = {
            Text(text = stringResource(R.string.usage_access_permission_message, toolName))
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(text = stringResource(R.string.open_system_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun SettingsRow(
    title: String,
    icon: ImageVector? = null,
    supportingText: String? = null,
    trailingText: String? = null,
    onClick: () -> Unit,
    expandedContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (expandedContent != null) {
            Spacer(Modifier.height(10.dp))
            expandedContent()
        }
    }
}

@Composable
private fun flowTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    cursorColor = MaterialTheme.colorScheme.onSurface
)

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MessageBubble(message: Message, assistantAvatarPath: String?, userAvatarPath: String?, showAvatars: Boolean) {
    if (message.role == MessageRole.Tool) {
        ToolCallBubble(message)
        return
    }

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
        Color.Transparent
    }
    val bubbleContentColor = if (isUser) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 22.dp, topEnd = 8.dp, bottomEnd = 22.dp, bottomStart = 22.dp)
    } else {
        RoundedCornerShape(topStart = 8.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp)
    }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val assistantMaxWidth = maxOf(220.dp, screenWidth - 18.dp)
    val bubbleMaxWidth = if (isUser) {
        if (showAvatars) 286.dp else 320.dp
    } else {
        assistantMaxWidth
    }
    val messageContent: @Composable () -> Unit = {
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
    }

    if (isUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top
        ) {
            messageContent()
            if (showAvatars) {
                Spacer(Modifier.size(8.dp))
                ProfileAvatar(
                    avatarPath = userAvatarPath,
                    size = 38.dp
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            if (showAvatars) {
                AssistantAvatar(
                    avatarPath = assistantAvatarPath,
                    size = 38.dp
                )
                Spacer(Modifier.height(6.dp))
            }
            messageContent()
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
private fun ToolCallBubble(message: Message) {
    val context = LocalContext.current
    val messageTime = remember(context, message.createdAt) {
        formatMessageTime(context, message.createdAt)
    }
    val lines = remember(message.content) { message.content.lines() }
    val toolName = lines.firstOrNull()?.takeIf { it.isNotBlank() }.orEmpty()
    val detail = lines.drop(1).joinToString(separator = "\n").takeIf { it.isNotBlank() }
    val titleRes = when (message.status) {
        MessageStatus.Streaming -> R.string.tool_call_running
        MessageStatus.Complete -> R.string.tool_call_complete
        MessageStatus.Failed -> R.string.tool_call_failed
        else -> R.string.tool_call_complete
    }
    val accentColor = when (message.status) {
        MessageStatus.Streaming -> MaterialTheme.colorScheme.primary
        MessageStatus.Complete -> MaterialTheme.colorScheme.tertiary
        MessageStatus.Failed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(modifier = Modifier.widthIn(max = 300.dp)) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor, CircleShape)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = stringResource(titleRes, toolName.ifBlank { "tool" }),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (detail != null) {
                            Text(
                                text = detail,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                            )
                        }
                    }
                }
            }
            Text(
                text = messageTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun ReasoningBubble(message: Message, maxWidth: Dp) {
    var expanded by remember(message.id, message.status, message.content.isBlank()) {
        mutableStateOf(message.status == MessageStatus.Streaming && message.content.isBlank())
    }
    val isStillThinking = message.status == MessageStatus.Streaming && message.content.isBlank()
    val elapsedSeconds = (message.reasoningDurationMillis / 1000L).coerceAtLeast(1L)
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
    providers: List<ProviderConfig>,
    currentConversation: Conversation?,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onToggleWebSearch: () -> Unit,
    onSelectModel: (ProviderConfig, String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val inputMethodManager = remember(view) {
        view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }
    val hasInput = value.isNotBlank()
    val modelOptions = remember(providers) { configuredModelOptions(providers) }
    val currentProvider = providers.firstOrNull { provider -> provider.id == currentConversation?.providerId }
    val isDarkBackground = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val sendIconColor = when {
        isStreaming -> Color(0xFFFF5A3D)
        hasInput -> MaterialTheme.colorScheme.onSurface
        isDarkBackground -> Color(0xFF8C929A)
        else -> Color(0xFFB8BCC4)
    }

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
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f), RoundedCornerShape(24.dp))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f)),
                RoundedCornerShape(24.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submitMessage() }),
                maxLines = 5
            )
            IconButton(
                onClick = if (isStreaming) onStop else ::submitMessage,
                enabled = enabled || isStreaming,
                modifier = Modifier.size(40.dp)
            ) {
                if (isStreaming) {
                    StreamingStopIcon(mainColor = sendIconColor, modifier = Modifier.size(24.dp))
                } else {
                    SendPaperPlaneIcon(color = sendIconColor, modifier = Modifier.size(28.dp))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModelSwitchButton(
                currentProvider = currentProvider,
                currentModelName = currentConversation?.modelName,
                modelOptions = modelOptions,
                enabled = enabled && !isStreaming && modelOptions.isNotEmpty(),
                onSelectModel = onSelectModel
            )
            WebSearchPill(
                enabled = enabled && !isStreaming,
                selected = webSearchEnabled,
                onClick = onToggleWebSearch
            )
        }
    }
}

@Composable
private fun ModelSwitchButton(
    currentProvider: ProviderConfig?,
    currentModelName: String?,
    modelOptions: List<ComposerModelOption>,
    enabled: Boolean,
    onSelectModel: (ProviderConfig, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedDotColor = MaterialTheme.colorScheme.primary
    Box {
        Box(
            modifier = Modifier
                .size(ComposerToolHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), CircleShape)
                .clickable(enabled = enabled) { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            ProviderLogoImage(
                providerName = currentProvider?.displayName,
                modifier = Modifier.size(22.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            modelOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProviderLogoImage(
                                providerName = option.provider.displayName,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = option.modelName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1
                                )
                                Text(
                                    text = option.provider.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelectModel(option.provider, option.modelName)
                    },
                    trailingIcon = if (option.modelName == currentModelName && option.provider.id == currentProvider?.id) {
                        {
                            Canvas(modifier = Modifier.size(8.dp)) {
                                drawCircle(color = selectedDotColor)
                            }
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

private fun configuredModelOptions(providers: List<ProviderConfig>): List<ComposerModelOption> =
    providers.asSequence()
        .filter { provider -> provider.isEnabled }
        .filter { provider -> provider.baseUrl.isNotBlank() }
        .filter { provider -> provider.defaultModel.isNotBlank() }
        .flatMap { provider ->
            recognizedProviderModelNames(provider).map { modelName ->
                ComposerModelOption(provider = provider, modelName = modelName)
            }
        }
        .toList()

private fun recognizedProviderModelNames(provider: ProviderConfig): List<String> {
    val providerKey = "${provider.displayName} ${provider.baseUrl}".lowercase()
    val names = when {
        providerKey.contains("deepseek") ->
            listOf(provider.defaultModel.trim(), "deepseek-v4-pro", "deepseek-v4-flash")
        else ->
            listOf(provider.defaultModel.trim())
    }
    return names
        .filter { modelName -> modelName.isNotBlank() }
        .distinctBy { modelName -> modelName.lowercase() }
}

private data class ComposerModelOption(
    val provider: ProviderConfig,
    val modelName: String
)

@Composable
private fun ProviderLogoImage(
    providerName: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        val logoRes = providerLogoRes(providerName)
        if (logoRes != null) {
            Image(
                painter = painterResource(logoRes),
                contentDescription = stringResource(R.string.switch_model),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Hub,
                contentDescription = stringResource(R.string.switch_model),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
            )
        }
    }
}

private fun providerLogoRes(providerName: String?): Int? =
    when {
        providerName?.contains("claude", ignoreCase = true) == true -> R.drawable.provider_claude
        providerName?.contains("deep", ignoreCase = true) == true -> R.drawable.provider_deepseek
        providerName?.contains("gemini", ignoreCase = true) == true -> R.drawable.provider_gemini
        providerName?.contains("chat", ignoreCase = true) == true ||
            providerName?.contains("open", ignoreCase = true) == true -> R.drawable.provider_openai
        else -> null
    }

@Composable
private fun WebSearchPill(
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(ComposerToolHeight)
            .clip(RoundedCornerShape(ComposerToolHeight / 2))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
                } else {
                    Color.Transparent
                },
                RoundedCornerShape(ComposerToolHeight / 2)
            )
            .border(
                BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                RoundedCornerShape(ComposerToolHeight / 2)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Image(
            painter = painterResource(if (selected) R.drawable.ic_web_search_on else R.drawable.ic_web_search_off),
            contentDescription = stringResource(if (selected) R.string.web_search_on else R.string.web_search_off),
            colorFilter = if (selected) null else ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = stringResource(R.string.web_search_short),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SendPaperPlaneIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(28.dp)) {
        val mainStrokeWidth = 2.2.dp.toPx()
        val detailStrokeWidth = 1.3.dp.toPx()
        val tail = Offset(size.width * 0.18f, size.height * 0.58f)
        val tip = Offset(size.width * 0.84f, size.height * 0.30f)
        val lowerWing = Offset(size.width * 0.61f, size.height * 0.77f)
        val innerFold = Offset(size.width * 0.50f, size.height * 0.59f)

        drawLine(
            color = color,
            start = tail,
            end = tip,
            strokeWidth = mainStrokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = tip,
            end = lowerWing,
            strokeWidth = mainStrokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = tail,
            end = innerFold,
            strokeWidth = mainStrokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = innerFold,
            end = lowerWing,
            strokeWidth = mainStrokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.49f, size.height * 0.59f),
            end = tip,
            strokeWidth = detailStrokeWidth,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = color,
            radius = size.minDimension * 0.035f,
            center = Offset(size.width * 0.185f, size.height * 0.585f)
        )
    }
}

@Composable
private fun StreamingStopIcon(
    mainColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val ringStrokeWidth = 2.1.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val ringRadius = size.minDimension / 2f - 2.1.dp.toPx()
        val stopSide = size.minDimension * 0.28f
        val topLeft = Offset(
            x = center.x - stopSide / 2f,
            y = center.y - stopSide / 2f
        )
        val corner = CornerRadius(1.65.dp.toPx(), 1.65.dp.toPx())
        drawCircle(
            color = mainColor,
            radius = ringRadius,
            center = center,
            style = Stroke(width = ringStrokeWidth)
        )
        drawRoundRect(
            color = mainColor,
            topLeft = topLeft,
            size = Size(stopSide, stopSide),
            cornerRadius = corner
        )
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
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.conversation_settings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.assistant_avatar),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AssistantAvatarStrip(
                        assistantAvatarPath = assistantAvatarPath,
                        onAvatarClick = { avatarPicker.launch("image/*") }
                    )
                }
                OutlinedTextField(
                    value = assistantName,
                    onValueChange = { assistantName = it },
                    label = { Text(stringResource(R.string.assistant_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = flowTextFieldColors(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = stringResource(R.string.show_avatars),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = stringResource(R.string.personalization_switch_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showAvatars,
                        onCheckedChange = { showAvatars = it }
                    )
                }
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(stringResource(R.string.system_prompt)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 132.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = flowTextFieldColors(),
                    minLines = 4,
                    maxLines = 8
                )
                SettingsLabeledSlider(
                    title = stringResource(R.string.reply_style),
                    startLabel = stringResource(R.string.concise),
                    endLabel = stringResource(R.string.detailed),
                    value = topP,
                    onValueChange = { topP = it },
                    valueRange = 0f..1f
                )
                SettingsLabeledSlider(
                    title = stringResource(R.string.creativity),
                    startLabel = stringResource(R.string.strict),
                    endLabel = stringResource(R.string.playful),
                    value = temperature / 2f,
                    onValueChange = { temperature = it * 2f },
                    valueRange = 0f..1f
                )
                OutlinedTextField(
                    value = maxTokens,
                    onValueChange = { value -> maxTokens = value.filter { it.isDigit() } },
                    label = { Text(stringResource(R.string.max_tokens)) },
                    shape = RoundedCornerShape(12.dp),
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
                    .height(64.dp)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun AssistantAvatarStrip(
    assistantAvatarPath: String?,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(66.dp), contentAlignment = Alignment.Center) {
            AssistantAvatar(
                avatarPath = assistantAvatarPath,
                size = 62.dp
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), CircleShape)
            )
            AvatarEditButton(
                onClick = onAvatarClick,
                contentDescription = stringResource(R.string.assistant_avatar),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-2).dp, y = (-2).dp)
            )
        }
    }
}

@Composable
private fun SettingsLabeledSlider(
    title: String,
    startLabel: String,
    endLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = startLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = endLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
