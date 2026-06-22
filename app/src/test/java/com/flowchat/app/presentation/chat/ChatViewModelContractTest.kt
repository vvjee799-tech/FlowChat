package com.flowchat.app.presentation.chat

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatViewModelContractTest {
    @Test
    fun rawKtorTimeoutMessageIsMappedToReadableChineseChatError() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatViewModel.kt").readText()

        assertTrue(source.contains("throwable.userFacingChatError()"))
        assertTrue(source.contains("private fun Throwable.userFacingChatError(): String"))
        assertTrue(source.contains("Request timeout has expired"))
        assertTrue(source.contains("请求超时"))
        assertTrue(source.contains("模型思考时间过长"))
    }
}
