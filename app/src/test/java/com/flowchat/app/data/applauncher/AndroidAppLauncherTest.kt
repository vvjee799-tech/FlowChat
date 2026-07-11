package com.flowchat.app.data.applauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AndroidAppLauncherTest {
    private val apps = listOf(
        LaunchableApp("微信", "com.tencent.mm", "com.tencent.mm.ui.LauncherUI"),
        LaunchableApp("DeepSeek", "com.deepseek.chat", "com.deepseek.chat.MainActivity"),
        LaunchableApp("FlowChat", "com.flowchat.app", "com.flowchat.app.MainActivity"),
        LaunchableApp("ChatGPT", "com.openai.chatgpt", "com.openai.chatgpt.MainActivity")
    )

    @Test
    fun selectsExactLocalizedLabelBeforePartialMatches() {
        assertEquals("com.tencent.mm", selectLaunchableApp(apps, "微信").packageName)
    }

    @Test
    fun selectsExactPackageName() {
        assertEquals("DeepSeek", selectLaunchableApp(apps, "com.deepseek.chat").label)
    }

    @Test
    fun selectsOneUniquePartialLabelIgnoringCaseAndSpaces() {
        assertEquals("DeepSeek", selectLaunchableApp(apps, "deep seek").label)
    }

    @Test
    fun rejectsAmbiguousPartialMatches() {
        assertThrows(IllegalArgumentException::class.java) {
            selectLaunchableApp(apps, "chat")
        }
    }

    @Test
    fun rejectsMissingApps() {
        assertThrows(IllegalArgumentException::class.java) {
            selectLaunchableApp(apps, "not installed")
        }
    }
}
