package com.flowchat.app.presentation.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.flowchat.app.MainActivity
import com.flowchat.app.R
import com.flowchat.app.ui.theme.AppAppearance
import com.flowchat.app.ui.theme.FlowChatTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FloatingAssistantService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    @Inject lateinit var bridge: FloatingAssistantBridge

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val serviceViewModelStore = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: ComposeView
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var overlayAttached = false
    private var visibilityJob: Job? = null
    private var eventJob: Job? = null
    private var expanded by mutableStateOf(false)
    private var appearance by mutableStateOf(AppAppearance.System)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = serviceViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        windowManager = getSystemService(WindowManager::class.java)
        appearance = AppAppearance.load(this)
        startForegroundServiceNotification()
        createOverlayView()
        observeVisibility()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!FloatingAssistantPermission.isGranted(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        appearance = AppAppearance.load(this)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeOverlay()
        visibilityJob?.cancel()
        eventJob?.cancel()
        serviceScope.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceViewModelStore.clear()
        super.onDestroy()
    }

    private fun createOverlayView() {
        val edgeInset = dp(8)
        val savedY = getSharedPreferences(PositionPreferences, MODE_PRIVATE)
            .getInt(PositionYKey, dp(214))
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = edgeInset
            y = savedY
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingAssistantService)
            setViewTreeViewModelStoreOwner(this@FloatingAssistantService)
            setViewTreeSavedStateRegistryOwner(this@FloatingAssistantService)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            setContent {
                FlowChatTheme(appAppearance = appearance) {
                    val state by bridge.state.collectAsState()
                    FloatingAssistantOverlay(
                        state = state,
                        expanded = expanded,
                        onExpand = { updateExpanded(true) },
                        onCollapse = { updateExpanded(false) },
                        onDrag = ::moveVertically,
                        onDragFinished = ::savePosition,
                        onSend = { text ->
                            bridge.send(text)
                        },
                        onStop = bridge::stop
                    )
                }
            }
        }
    }

    private fun observeVisibility() {
        visibilityJob = serviceScope.launch {
            combine(bridge.state, bridge.appInForeground) { state, appInForeground ->
                !appInForeground && state.canChat
            }.collect { shouldShow ->
                if (shouldShow && FloatingAssistantPermission.isGranted(this@FloatingAssistantService)) {
                    showOverlay()
                } else {
                    removeOverlay()
                }
            }
        }
        eventJob = serviceScope.launch {
            bridge.events.collect { event ->
                if (event == FloatingAssistantEvent.CollapseForAutomation) {
                    updateExpanded(false)
                }
            }
        }
    }

    private fun showOverlay() {
        if (overlayAttached) return
        appearance = AppAppearance.load(this)
        runCatching {
            windowManager.addView(overlayView, layoutParams)
            overlayAttached = true
        }.onFailure { stopSelf() }
    }

    private fun removeOverlay() {
        if (!overlayAttached) return
        updateExpanded(false)
        runCatching { windowManager.removeViewImmediate(overlayView) }
        overlayAttached = false
    }

    private fun updateExpanded(value: Boolean) {
        if (expanded == value) return
        expanded = value
        if (!overlayAttached) return
        if (value) {
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        } else {
            overlayView.clearFocus()
            getSystemService(InputMethodManager::class.java)
                .hideSoftInputFromWindow(overlayView.windowToken, 0)
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }
        runCatching { windowManager.updateViewLayout(overlayView, layoutParams) }
    }

    private fun moveVertically(deltaY: Float) {
        if (!overlayAttached || expanded) return
        val displayHeight = resources.displayMetrics.heightPixels
        layoutParams.y = (layoutParams.y + deltaY.roundToInt())
            .coerceIn(dp(48), displayHeight - dp(104))
        windowManager.updateViewLayout(overlayView, layoutParams)
    }

    private fun savePosition() {
        getSharedPreferences(PositionPreferences, MODE_PRIVATE)
            .edit()
            .putInt(PositionYKey, layoutParams.y)
            .apply()
    }

    private fun startForegroundServiceNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    NotificationChannelId,
                    getString(R.string.floating_assistant),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.floating_assistant_notification_description)
                    setShowBadge(false)
                }
            )
        }
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, NotificationChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.floating_assistant_running))
            .setContentText(getString(R.string.floating_assistant_notification_description))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NotificationId, notification, foregroundType)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private companion object {
        const val NotificationChannelId = "floating_assistant"
        const val NotificationId = 1207
        const val PositionPreferences = "floating_assistant_position"
        const val PositionYKey = "position_y"
    }
}

@Composable
private fun FloatingAssistantOverlay(
    state: FloatingAssistantState,
    expanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragFinished: () -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit
) {
    Crossfade(
        targetState = expanded,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "floating-assistant"
    ) { isExpanded ->
        if (isExpanded) {
            ExpandedAssistantPanel(
                state = state,
                onCollapse = onCollapse,
                onSend = onSend,
                onStop = onStop
            )
        } else {
            FloatingAssistantBubble(
                isStreaming = state.isStreaming,
                onClick = onExpand,
                onDrag = onDrag,
                onDragFinished = onDragFinished
            )
        }
    }
}

@Composable
private fun FloatingAssistantBubble(
    isStreaming: Boolean,
    onClick: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragFinished: () -> Unit
) {
    val dragState = rememberDraggableState { delta -> onDrag(delta) }
    val pulse = if (isStreaming) {
        val transition = rememberInfiniteTransition(label = "assistant-activity")
        val animatedPulse by transition.animateFloat(
            initialValue = 0.42f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "assistant-ring"
        )
        animatedPulse
    } else {
        0.58f
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = { onDragFinished() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(40.dp)
                .shadow(9.dp, CircleShape)
                .border(
                    width = if (isStreaming) 2.dp else 1.dp,
                    color = if (isStreaming) {
                        MaterialTheme.colorScheme.primary.copy(alpha = pulse)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.58f)
                    },
                    shape = CircleShape
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                FlowChatMark(modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun ExpandedAssistantPanel(
    state: FloatingAssistantState,
    onCollapse: () -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit
) {
    val messageScrollState = rememberScrollState()
    LaunchedEffect(state.assistantText, messageScrollState.maxValue) {
        messageScrollState.scrollTo(messageScrollState.maxValue)
    }
    Surface(
        modifier = Modifier
            .width(272.dp)
            .shadow(10.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)
        ),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 112.dp, max = 208.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.30f))
            ) {
                Text(
                    text = state.assistantText.ifBlank {
                        if (state.isStreaming) {
                            stringResource(R.string.floating_assistant_thinking)
                        } else {
                            stringResource(R.string.floating_assistant_ready)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, top = 14.dp, end = 42.dp, bottom = 14.dp)
                        .verticalScroll(messageScrollState)
                )
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(34.dp)
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.collapse_floating_assistant),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OverlayComposer(
                isStreaming = state.isStreaming,
                onSend = onSend,
                onStop = onStop
            )
        }
    }
}

@Composable
private fun FloatingAssistantBubble(
    isStreaming: Boolean,
    onClick: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragFinished: () -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        FloatingAssistantBubble(isStreaming, onClick, onDrag, onDragFinished)
    }
}

@Composable
private fun OverlayComposer(
    isStreaming: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = 4.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.floating_assistant_input_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (isStreaming) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(1.5.dp, StopCoral, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Stop,
                            contentDescription = stringResource(R.string.stop),
                            tint = StopCoral,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = {
                        text.trim().takeIf { it.isNotEmpty() }?.let { value ->
                            onSend(value)
                            text = ""
                        }
                    },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.send),
                        tint = if (text.isNotBlank()) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        modifier = Modifier
                            .size(19.dp)
                            .rotate(-18f)
                    )
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { text = "" }
    }
}

@Composable
private fun FlowChatMark(modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val invert = remember {
        ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }
    Image(
        painter = painterResource(R.mipmap.ic_launcher),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        colorFilter = if (isDark) ColorFilter.colorMatrix(invert) else null,
        modifier = modifier.clip(CircleShape)
    )
}

private val StopCoral = Color(0xFFFF665D)
