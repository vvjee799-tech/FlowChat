package com.flowchat.app.data.memory

import com.flowchat.app.domain.model.MemoryRecord
import java.io.File
import java.util.UUID
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class JsonMemoryStore(
    private val file: File,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
) {
    fun readAll(): List<MemoryRecord> {
        if (!file.exists()) return emptyList()
        val text = file.readText(Charsets.UTF_8).trim()
        if (text.isEmpty()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(MemoryRecord.serializer()), text)
        }.getOrDefault(emptyList())
    }

    fun appendTurn(userMessage: String, assistantReply: String, timestamp: Long): MemoryRecord {
        val record = MemoryRecord(
            id = UUID.randomUUID().toString(),
            goal = userMessage.firstSentence(),
            summary = buildSummary(userMessage, assistantReply),
            quality = if (assistantReply.length > HighQualityReplyThreshold) QualityHigh else QualityLow,
            timestamp = timestamp
        )
        val updated = readAll() + record
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(ListSerializer(MemoryRecord.serializer()), updated), Charsets.UTF_8)
        return record
    }

    fun retrieve(userMessage: String, topN: Int): List<MemoryRecord> {
        val limit = topN.coerceIn(1, MaxRetrievedMemories)
        val queryKeywords = userMessage.keywords()
        val highQuality = readAll().filter { it.quality == QualityHigh }
        val scored = highQuality
            .map { record -> record to record.goal.keywords().intersect(queryKeywords).size }
            .filter { (_, score) -> score > 0 }
            .sortedWith(
                compareByDescending<Pair<MemoryRecord, Int>> { it.second }
                    .thenByDescending { it.first.timestamp }
            )
            .map { it.first }

        return if (scored.isNotEmpty()) {
            scored.take(limit)
        } else {
            highQuality.sortedByDescending { it.timestamp }.take(limit)
        }
    }

    private fun buildSummary(userMessage: String, assistantReply: String): String =
        "用户：${userMessage.oneLine().limit(SummaryPartMaxLength)} / 助手：${assistantReply.oneLine().limit(SummaryPartMaxLength)}"

    private fun String.firstSentence(): String {
        val trimmed = oneLine()
        if (trimmed.isBlank()) return ""
        val endIndex = trimmed.indexOfFirst { it in SentenceTerminators }
        return if (endIndex >= 0) trimmed.take(endIndex + 1).trim() else trimmed.limit(GoalMaxLength)
    }

    private fun String.oneLine(): String =
        trim().replace(Regex("\\s+"), " ")

    private fun String.limit(maxLength: Int): String =
        if (length <= maxLength) this else take(maxLength).trimEnd() + "..."

    private fun String.keywords(): Set<String> {
        val tokens = mutableSetOf<String>()
        KeywordRegex.findAll(lowercase()).forEach { match ->
            val value = match.value
            if (value.any { it.isCjk() }) {
                if (value.length == 1) {
                    tokens += value
                } else {
                    value.windowed(size = 2, step = 1).forEach { tokens += it }
                }
            } else if (value.length >= 2) {
                tokens += value
            }
        }
        return tokens
    }

    private fun Char.isCjk(): Boolean =
        this in '\u4E00'..'\u9FFF'

    companion object {
        const val QualityHigh = "high"
        const val QualityLow = "low"
        private const val HighQualityReplyThreshold = 200
        private const val MaxRetrievedMemories = 5
        private const val GoalMaxLength = 120
        private const val SummaryPartMaxLength = 80
        private val SentenceTerminators = setOf('。', '！', '？', '.', '!', '?', '\n')
        private val KeywordRegex = Regex("[\\u4E00-\\u9FFFA-Za-z0-9_]+")
    }
}
