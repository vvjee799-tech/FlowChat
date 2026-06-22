package com.flowchat.app.data.network

import com.flowchat.app.domain.model.ChatDelta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiStreamParserTest {
    @Test
    fun parsesContentDelta() {
        val delta = OpenAiStreamParser.parseLine(
            """data: {"choices":[{"delta":{"content":"Hel"}}]}"""
        )

        assertEquals(ChatDelta.Content("Hel"), delta)
    }

    @Test
    fun parsesReasoningContentDeltaSeparatelyFromFinalContent() {
        val delta = OpenAiStreamParser.parseLine(
            """data: {"choices":[{"delta":{"reasoning_content":"Thinking"}}]}"""
        )

        assertEquals(ChatDelta.Reasoning("Thinking"), delta)
    }

    @Test
    fun parsesDoneEvent() {
        val delta = OpenAiStreamParser.parseLine("data: [DONE]")

        assertEquals(ChatDelta.Done, delta)
    }

    @Test
    fun ignoresBlankAndNonDataLines() {
        assertEquals(null, OpenAiStreamParser.parseLine(""))
        assertEquals(null, OpenAiStreamParser.parseLine(": keep-alive"))
    }

    @Test
    fun convertsMalformedJsonToErrorDelta() {
        val delta = OpenAiStreamParser.parseLine("data: {not-json")

        assertTrue(delta is ChatDelta.Error)
    }

    @Test
    fun parsesNonStreamingCompletionContent() {
        val delta = OpenAiStreamParser.parseCompletionBody(
            """{"choices":[{"message":{"content":"pong"}}]}"""
        )

        assertEquals(ChatDelta.Content("pong"), delta)
    }

    @Test
    fun parsesNonStreamingReasoningContentSeparatelyFromFinalContent() {
        val delta = OpenAiStreamParser.parseCompletionBody(
            """{"choices":[{"message":{"reasoning_content":"thinking","content":"final"}}]}"""
        )

        assertEquals(ChatDelta.FullResponse(reasoningText = "thinking", contentText = "final"), delta)
    }
}
