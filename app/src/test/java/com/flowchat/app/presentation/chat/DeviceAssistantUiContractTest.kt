package com.flowchat.app.presentation.chat

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceAssistantUiContractTest {
    @Test
    fun settingsExposeShizukuStatusCapabilitiesAndPermissionAction() {
        val screen = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val state = File("src/main/java/com/flowchat/app/presentation/chat/ChatUiState.kt").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val zhStrings = File("src/main/res/values-zh-rCN/strings.xml").readText()

        assertTrue(state.contains("val shizukuState: ShizukuConnectionState"))
        assertTrue(screen.contains("private fun DeviceAssistantDialog("))
        assertTrue(screen.contains("R.string.device_assistant"))
        assertTrue(screen.contains("R.string.shizuku_permission"))
        assertTrue(screen.contains("onRequestShizukuPermission"))
        assertTrue(screen.contains("onRefreshShizuku"))
        assertTrue(screen.contains("appSettings.deviceAssistantEnabled"))
        assertTrue(screen.contains("appSettings.forceStopToolEnabled"))
        assertTrue(strings.contains("name=\"device_assistant\""))
        assertTrue(zhStrings.contains("name=\"device_assistant\""))
    }

    @Test
    fun settingsExposeAccessibilityStatusAndStructuredAutomationTools() {
        val screen = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val state = File("src/main/java/com/flowchat/app/presentation/chat/ChatUiState.kt").readText()
        val viewModel = File("src/main/java/com/flowchat/app/presentation/chat/ChatViewModel.kt").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val zhStrings = File("src/main/res/values-zh-rCN/strings.xml").readText()

        assertTrue(state.contains("val accessibilityState: AccessibilityConnectionState"))
        assertTrue(screen.contains("accessibilityState = state.accessibilityState"))
        assertTrue(screen.contains("onOpenAccessibilitySettings = viewModel::openAccessibilitySettings"))
        assertTrue(screen.contains("R.string.accessibility_screen_control"))
        assertTrue(screen.contains("R.string.enable_accessibility_service"))
        assertTrue(viewModel.contains("deviceAssistantGateway.accessibilityState"))
        assertTrue(strings.contains("name=\"accessibility_screen_control\""))
        assertTrue(zhStrings.contains("name=\"accessibility_screen_control\""))
        assertTrue(screen.contains("\"observe_screen\""))
        assertTrue(screen.contains("\"tap_ui_element\""))
        assertTrue(screen.contains("\"input_text\""))
    }

    @Test
    fun dangerousDeviceActionUsesAnExplicitConfirmationDialog() {
        val screen = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val state = File("src/main/java/com/flowchat/app/presentation/chat/ChatUiState.kt").readText()
        val viewModel = File("src/main/java/com/flowchat/app/presentation/chat/ChatViewModel.kt").readText()

        assertTrue(state.contains("val pendingDeviceActionConfirmation"))
        assertTrue(state.contains("data class DeviceActionConfirmationUi("))
        assertTrue(screen.contains("DeviceActionConfirmationDialog("))
        assertTrue(screen.contains("viewModel::confirmDeviceAction"))
        assertTrue(screen.contains("viewModel::cancelDeviceAction"))
        assertTrue(viewModel.contains("DeviceActionPolicy.requiresConfirmation(call.name)"))
        assertTrue(viewModel.contains("confirmation.await()"))
        assertFalse(viewModel.contains("run_shell"))
    }
}
