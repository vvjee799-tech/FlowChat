package com.flowchat.app.data.memory

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonMemoryStoreTest {
    @Test
    fun appendsTurnMemoryRecordToJsonFile() {
        val file = File.createTempFile("flowchat-memory", ".json").apply { deleteOnExit() }
        val store = JsonMemoryStore(file)

        val record = store.appendTurn(
            userMessage = "写一段猫娘回复。第二句不应该进入 goal。",
            assistantReply = "短回复",
            timestamp = 1000L
        )

        assertEquals("写一段猫娘回复。", record.goal)
        assertEquals("low", record.quality)
        assertEquals(1000L, record.timestamp)
        assertTrue(record.summary.contains("用户：写一段猫娘回复。"))
        assertTrue(record.summary.contains("助手：短回复"))

        val stored = store.readAll()
        assertEquals(1, stored.size)
        assertEquals(record.id, stored.single().id)
    }

    @Test
    fun marksAssistantRepliesLongerThanTwoHundredCharsAsHighQuality() {
        val file = File.createTempFile("flowchat-memory", ".json").apply { deleteOnExit() }
        val store = JsonMemoryStore(file)

        val record = store.appendTurn(
            userMessage = "总结我的角色偏好。",
            assistantReply = "a".repeat(201),
            timestamp = 1000L
        )

        assertEquals("high", record.quality)
    }

    @Test
    fun retrievesHighQualityMemoriesByKeywordOverlapScore() {
        val file = File.createTempFile("flowchat-memory", ".json").apply { deleteOnExit() }
        val store = JsonMemoryStore(file)
        store.appendTurn("猫娘语气要轻松。", "a".repeat(220), 1000L)
        store.appendTurn("联网搜索新闻摘要。", "b".repeat(220), 2000L)
        store.appendTurn("猫娘称呼用户 Captain。", "c".repeat(220), 3000L)
        store.appendTurn("猫娘短回复。", "low", 4000L)

        val results = store.retrieve(userMessage = "继续猫娘语气，记得叫我 Captain", topN = 5)

        assertEquals(2, results.size)
        assertEquals("猫娘语气要轻松。", results[0].goal)
        assertEquals("猫娘称呼用户 Captain。", results[1].goal)
        assertTrue(results.none { it.quality == "low" })
    }

    @Test
    fun fallsBackToMostRecentHighQualityMemoriesWhenNoKeywordMatches() {
        val file = File.createTempFile("flowchat-memory", ".json").apply { deleteOnExit() }
        val store = JsonMemoryStore(file)
        store.appendTurn("旧偏好。", "a".repeat(220), 1000L)
        store.appendTurn("中间偏好。", "b".repeat(220), 2000L)
        store.appendTurn("最新偏好。", "c".repeat(220), 3000L)

        val results = store.retrieve(userMessage = "完全无关的英文 request", topN = 2)

        assertEquals(listOf("最新偏好。", "中间偏好。"), results.map { it.goal })
    }

    @Test
    fun capsStoredMemoriesAndSupportsDeleteAndClear() {
        val file = File.createTempFile("flowchat-memory", ".json").apply { deleteOnExit() }
        val store = JsonMemoryStore(file, retentionLimit = 3)
        val first = store.appendTurn("第一条。", "a".repeat(220), 1000L)
        store.appendTurn("第二条。", "b".repeat(220), 2000L)
        val third = store.appendTurn("第三条。", "c".repeat(220), 3000L)
        store.appendTurn("第四条。", "d".repeat(220), 4000L)

        assertEquals(listOf("第二条。", "第三条。", "第四条。"), store.readAll().map { it.goal })
        assertTrue(store.readAll().none { it.id == first.id })

        store.delete(third.id)
        assertEquals(listOf("第二条。", "第四条。"), store.readAll().map { it.goal })

        store.clear()
        assertTrue(store.readAll().isEmpty())
    }
}
