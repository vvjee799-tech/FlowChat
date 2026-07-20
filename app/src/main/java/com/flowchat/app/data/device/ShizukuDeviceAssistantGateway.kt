package com.flowchat.app.data.device

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import com.flowchat.app.BuildConfig
import com.flowchat.app.domain.device.AccessibilityConnectionState
import com.flowchat.app.domain.device.AccessibilityConnectionStatus
import com.flowchat.app.domain.device.DeviceAssistantGateway
import com.flowchat.app.domain.device.DeviceActionPolicy
import com.flowchat.app.domain.device.DeviceCapability
import com.flowchat.app.domain.device.DeviceScreenSnapshot
import com.flowchat.app.domain.device.DeviceSwipeDirection
import com.flowchat.app.domain.device.DeviceToolResult
import com.flowchat.app.domain.device.ShizukuConnectionState
import com.flowchat.app.domain.device.ShizukuConnectionStatus
import com.flowchat.app.shizuku.IFlowChatShizukuService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import rikka.shizuku.Shizuku

@Singleton
class ShizukuDeviceAssistantGateway @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceAssistantGateway {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableConnectionState = MutableStateFlow(ShizukuConnectionState())
    override val connectionState: StateFlow<ShizukuConnectionState> = mutableConnectionState.asStateFlow()
    private val mutableAccessibilityState = MutableStateFlow(AccessibilityConnectionState())
    override val accessibilityState: StateFlow<AccessibilityConnectionState> =
        mutableAccessibilityState.asStateFlow()
    private val binding = AtomicBoolean(false)
    @Volatile private var service: IFlowChatShizukuService? = null

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, FlowChatShizukuService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("device_assistant")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshConnection() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        service = null
        binding.set(false)
        refreshConnection()
    }
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode != PermissionRequestCode) return@OnRequestPermissionResultListener
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            bindService()
        } else {
            mutableConnectionState.value = ShizukuConnectionState(
                status = ShizukuConnectionStatus.PermissionDenied,
                detail = "Shizuku permission was denied."
            )
        }
    }
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = IFlowChatShizukuService.Stub.asInterface(binder)
            binding.set(false)
            scope.launch { probeConnectedService() }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            binding.set(false)
            refreshConnection()
        }
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        refreshAccessibilityConnection()
        refreshConnection()
    }

    override fun refreshConnection() {
        scope.launch {
            if (hasLiveUserService()) {
                probeConnectedService()
                return@launch
            }
            service = null
            val inspectedState = inspectConnection()
            if (hasLiveUserService()) {
                probeConnectedService()
                return@launch
            }
            mutableConnectionState.value = inspectedState
            if (inspectedState.status == ShizukuConnectionStatus.Connecting) {
                bindService()
            }
        }
    }

    override fun requestPermission() {
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            refreshConnection()
            return
        }
        runCatching {
            when {
                Shizuku.isPreV11() -> mutableConnectionState.value = ShizukuConnectionState(
                    status = ShizukuConnectionStatus.Unsupported,
                    detail = "This Shizuku version is not supported."
                )
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> bindService()
                Shizuku.shouldShowRequestPermissionRationale() -> mutableConnectionState.value =
                    ShizukuConnectionState(
                        status = ShizukuConnectionStatus.PermissionDenied,
                        detail = "Open Shizuku and grant FlowChat permission."
                    )
                else -> Shizuku.requestPermission(PermissionRequestCode)
            }
        }.onFailure { error ->
            mutableConnectionState.value = errorState(error)
        }
    }

    override fun refreshAccessibilityConnection() {
        val availableService = FlowChatAccessibilityService.current()
        mutableAccessibilityState.value = when {
            availableService != null -> AccessibilityConnectionState(AccessibilityConnectionStatus.Connected)
            isAccessibilityServiceEnabled() -> AccessibilityConnectionState(
                AccessibilityConnectionStatus.Unavailable,
                "Accessibility is enabled but the service is not connected yet."
            )
            else -> AccessibilityConnectionState(AccessibilityConnectionStatus.Disabled)
        }
        val accessibilityCapabilities = if (availableService != null) {
            AccessibilityCapabilities
        } else {
            emptySet()
        }
        mutableConnectionState.value = mutableConnectionState.value.copy(
            capabilities = (mutableConnectionState.value.capabilities - AccessibilityCapabilities) +
                accessibilityCapabilities
        )
    }

    override fun openAccessibilitySettings() {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override suspend fun getDeviceStatus(): DeviceToolResult = callService { it.getDeviceStatus() }

    override suspend fun getForegroundApp(): DeviceToolResult = callService { it.getForegroundApp() }

    override suspend fun setScreenBrightness(percent: Int): DeviceToolResult =
        callService { it.setScreenBrightness(percent.coerceIn(1, 100)) }

    override suspend fun setMediaVolume(percent: Int): DeviceToolResult =
        callService { it.setMediaVolume(percent.coerceIn(0, 100)) }

    override suspend fun forceStopApp(appName: String): DeviceToolResult {
        val app = withContext(Dispatchers.IO) { resolveLaunchableApp(appName) }
        if (app.packageName == context.packageName) {
            return DeviceToolResult(
                success = false,
                summary = "FlowChat cannot force stop itself.",
                errorCode = "self_stop_blocked"
            )
        }
        val result = callService { it.forceStopPackage(app.packageName) }
        return if (result.success) {
            result.copy(summary = "Force stopped ${app.label} (${app.packageName}).")
        } else {
            result
        }
    }

    override suspend fun enablePowerMode(): DeviceToolResult {
        val result = callService { it.enablePowerMode() }
        delay(PowerModeRefreshDelayMillis)
        refreshAccessibilityConnection()
        return result
    }

    override suspend fun observeScreen(): DeviceToolResult {
        val accessibility = requireAccessibilityService() ?: return accessibilityUnavailable()
        val screen = accessibility.observeScreen(forceRefresh = true)
            ?: return DeviceToolResult(
                success = false,
                summary = "The current screen could not be read.",
                errorCode = "screen_unavailable"
            )
        return DeviceToolResult(
            success = !screen.sensitive,
            summary = if (screen.sensitive) {
                "A sensitive screen was detected."
            } else {
                "Observed ${screen.elements.size} visible UI elements."
            },
            errorCode = "sensitive_screen_blocked".takeIf { screen.sensitive },
            screen = screen
        )
    }

    override suspend fun tapUiElement(index: Int, longPress: Boolean): DeviceToolResult =
        performScreenAction(
            toolName = com.flowchat.app.domain.tools.AgentToolDefinitions.TapUiElementToolName,
            summary = if (longPress) "Long pressed UI element [$index]." else "Tapped UI element [$index]."
        ) { accessibility, screen ->
            if (screen.elements.none { it.index == index && it.enabled }) false
            else accessibility.tapUiElement(index, longPress)
        }

    override suspend fun inputText(
        index: Int,
        text: String,
        clearExisting: Boolean
    ): DeviceToolResult = performScreenAction(
        toolName = com.flowchat.app.domain.tools.AgentToolDefinitions.InputTextToolName,
        summary = "Entered text into UI element [$index]."
    ) { accessibility, screen ->
        val target = screen.elements.firstOrNull { it.index == index }
        if (target?.editable != true || target.password) false
        else accessibility.inputText(index, text, clearExisting)
    }

    override suspend fun swipeScreen(
        direction: DeviceSwipeDirection,
        distancePercent: Int
    ): DeviceToolResult = performScreenAction(
        toolName = com.flowchat.app.domain.tools.AgentToolDefinitions.SwipeScreenToolName,
        summary = "Swiped ${direction.name.lowercase()} on the current screen."
    ) { accessibility, _ ->
        accessibility.swipeScreen(direction, distancePercent.coerceIn(20, 80))
    }

    override suspend fun pressBack(): DeviceToolResult = performScreenAction(
        toolName = com.flowchat.app.domain.tools.AgentToolDefinitions.PressBackToolName,
        summary = "Pressed Back.",
        allowFlowChat = true
    ) { accessibility, _ -> accessibility.pressBack() }

    override suspend fun pressHome(): DeviceToolResult = performScreenAction(
        toolName = com.flowchat.app.domain.tools.AgentToolDefinitions.PressHomeToolName,
        summary = "Opened the Home screen.",
        allowFlowChat = true
    ) { accessibility, _ -> accessibility.pressHome() }

    private suspend fun performScreenAction(
        toolName: String,
        summary: String,
        allowFlowChat: Boolean = false,
        action: suspend (FlowChatAccessibilityService, DeviceScreenSnapshot) -> Boolean
    ): DeviceToolResult {
        val accessibility = requireAccessibilityService() ?: return accessibilityUnavailable()
        val before = accessibility.observeScreen(forceRefresh = true)
            ?: return DeviceToolResult(false, "The current screen could not be read.", errorCode = "screen_unavailable")
        if (!DeviceActionPolicy.allowsOnScreen(toolName, before)) {
            return DeviceToolResult(
                success = false,
                summary = "Device actions are blocked on sensitive screens.",
                errorCode = "sensitive_screen_blocked",
                screen = before
            )
        }
        if (!allowFlowChat && before.packageName == context.packageName) {
            return DeviceToolResult(
                success = false,
                summary = "FlowChat will not automate its own interface.",
                errorCode = "self_automation_blocked"
            )
        }
        val accepted = runCatching { action(accessibility, before) }.getOrDefault(false)
        if (!accepted) {
            return DeviceToolResult(
                success = false,
                summary = "The requested UI action could not be performed.",
                errorCode = "ui_action_failed"
            )
        }
        delay(PostActionObservationDelayMillis)
        val after = accessibility.observeScreen(forceRefresh = true)
        return DeviceToolResult(
            success = true,
            summary = summary,
            before = before.packageName,
            after = after?.packageName,
            reversible = false,
            screen = after
        )
    }

    private fun requireAccessibilityService(): FlowChatAccessibilityService? {
        refreshAccessibilityConnection()
        return FlowChatAccessibilityService.current()
    }

    private fun accessibilityUnavailable() = DeviceToolResult(
        success = false,
        summary = "Enable FlowChat in Android Accessibility settings first.",
        errorCode = "accessibility_unavailable"
    )

    private suspend fun inspectConnection(): ShizukuConnectionState {
        val binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!binderAlive) {
            return ShizukuConnectionState(
                status = if (isShizukuInstalled()) {
                    ShizukuConnectionStatus.NotRunning
                } else {
                    ShizukuConnectionStatus.NotInstalled
                }
            )
        }
        return runCatching {
            when {
                Shizuku.isPreV11() -> ShizukuConnectionState(ShizukuConnectionStatus.Unsupported)
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED ->
                    ShizukuConnectionState(ShizukuConnectionStatus.Connecting)
                Shizuku.shouldShowRequestPermissionRationale() ->
                    ShizukuConnectionState(ShizukuConnectionStatus.PermissionDenied)
                else -> ShizukuConnectionState(ShizukuConnectionStatus.PermissionRequired)
            }
        }.getOrElse(::errorState)
    }

    private fun bindService() {
        if (hasLiveUserService()) {
            scope.launch { probeConnectedService() }
            return
        }
        service = null
        if (!binding.compareAndSet(false, true)) return
        mutableConnectionState.value = ShizukuConnectionState(ShizukuConnectionStatus.Connecting)
        runCatching {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        }.onFailure { error ->
            binding.set(false)
            mutableConnectionState.value = errorState(error)
        }
    }

    private suspend fun probeConnectedService() {
        val connectedService = service ?: return
        runCatching {
            val capabilities = connectedService.probeCapabilities()
                .split(',')
                .mapNotNull { name -> DeviceCapability.entries.firstOrNull { it.name == name } }
                .toSet()
            val uid = Shizuku.getUid()
            mutableConnectionState.value = ShizukuConnectionState(
                status = if (uid == 0) {
                    ShizukuConnectionStatus.ConnectedRoot
                } else {
                    ShizukuConnectionStatus.ConnectedAdb
                },
                capabilities = capabilities + if (FlowChatAccessibilityService.current() != null) {
                    AccessibilityCapabilities
                } else {
                    emptySet()
                }
            )
        }.onFailure { error ->
            service = null
            mutableConnectionState.value = errorState(error)
        }
    }

    private fun hasLiveUserService(): Boolean =
        service?.asBinder()?.isBinderAlive == true

    private suspend fun callService(
        operation: (IFlowChatShizukuService) -> String
    ): DeviceToolResult = withContext(Dispatchers.IO) {
        runCatching {
            val connectedService = awaitService()
            parseResult(operation(connectedService))
        }.getOrElse { error ->
            refreshConnection()
            DeviceToolResult(
                success = false,
                summary = error.message ?: "Shizuku operation failed.",
                errorCode = "shizuku_operation_failed"
            )
        }
    }

    private suspend fun awaitService(): IFlowChatShizukuService {
        service?.let { return it }
        refreshConnection()
        withTimeout(ServiceConnectionTimeoutMillis) {
            connectionState.filter { it.isConnected || it.status in TerminalConnectionStates }.first()
        }
        return service ?: error(connectionState.value.detail ?: "Shizuku is not connected.")
    }

    private fun parseResult(raw: String): DeviceToolResult {
        val json = JSONObject(raw)
        return DeviceToolResult(
            success = json.optBoolean("success", false),
            summary = json.optString("summary", "Shizuku operation finished."),
            before = json.optString("before").takeIf { it.isNotBlank() },
            after = json.optString("after").takeIf { it.isNotBlank() },
            reversible = json.optBoolean("reversible", false),
            errorCode = json.optString("errorCode").takeIf { it.isNotBlank() }
        )
    }

    private fun isShizukuInstalled(): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getApplicationInfo(
                ShizukuPackageName,
                PackageManager.ApplicationInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getApplicationInfo(ShizukuPackageName, 0)
        }
        true
    }.getOrDefault(false)

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(context, FlowChatAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabledServices.split(':').mapNotNull(ComponentName::unflattenFromString).any { it == expected }
    }

    private fun resolveLaunchableApp(query: String): ResolvedDeviceApp {
        val normalizedQuery = query.normalizedAppKey()
        require(normalizedQuery.isNotBlank()) { "App name is required." }
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, 0)
        }.mapNotNull { info ->
            val activity = info.activityInfo ?: return@mapNotNull null
            ResolvedDeviceApp(
                label = info.loadLabel(context.packageManager).toString().ifBlank { activity.packageName },
                packageName = activity.packageName
            )
        }.distinctBy { it.packageName }
        val exact = resolved.filter { app ->
            app.label.normalizedAppKey() == normalizedQuery ||
                app.packageName.normalizedAppKey() == normalizedQuery
        }
        if (exact.size == 1) return exact.single()
        val partial = resolved.filter { app ->
            app.label.normalizedAppKey().contains(normalizedQuery) ||
                app.packageName.normalizedAppKey().contains(normalizedQuery)
        }
        return when (partial.size) {
            1 -> partial.single()
            0 -> throw IllegalArgumentException("No installed app matches \"$query\".")
            else -> throw IllegalArgumentException(
                "Multiple apps match \"$query\": ${partial.take(5).joinToString { it.label }}."
            )
        }
    }

    private fun String.normalizedAppKey(): String =
        lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

    private fun errorState(error: Throwable) = ShizukuConnectionState(
        status = ShizukuConnectionStatus.Error,
        detail = error.message ?: "Shizuku connection failed."
    )

    private data class ResolvedDeviceApp(val label: String, val packageName: String)

    private companion object {
        const val ShizukuPackageName = "moe.shizuku.privileged.api"
        const val PermissionRequestCode = 6201
        const val ServiceConnectionTimeoutMillis = 5_000L
        const val PostActionObservationDelayMillis = 400L
        const val PowerModeRefreshDelayMillis = 350L
        val AccessibilityCapabilities = setOf(
            DeviceCapability.ScreenObservation,
            DeviceCapability.UiElementAction,
            DeviceCapability.TextInput,
            DeviceCapability.ScreenSwipe,
            DeviceCapability.Navigation
        )
        val TerminalConnectionStates = setOf(
            ShizukuConnectionStatus.NotInstalled,
            ShizukuConnectionStatus.NotRunning,
            ShizukuConnectionStatus.PermissionRequired,
            ShizukuConnectionStatus.PermissionDenied,
            ShizukuConnectionStatus.Unsupported,
            ShizukuConnectionStatus.Error
        )
    }
}
