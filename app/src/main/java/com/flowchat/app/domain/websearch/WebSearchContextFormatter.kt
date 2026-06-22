package com.flowchat.app.domain.websearch

import com.flowchat.app.domain.model.WebSearchResult

object WebSearchContextFormatter {
    fun format(result: WebSearchResult): String {
        val entries = result.results.take(MaxResults).mapIndexed { index, item ->
            """
            [${index + 1}] ${item.title.trim()}
            摘要：${item.content.trim().limitSnippet()}
            URL：${item.url.trim()}
            """.trimIndent()
        }

        return buildString {
            appendLine("你已经获得联网搜索结果。")
            appendLine("必须基于这些搜索结果回答用户的当前问题。")
            appendLine("不要声称无法联网、不能搜索或知识截止；如果结果不足，只说明搜索结果有限。")
            appendLine("以下是联网搜索结果，仅用于回答用户问题：")
            appendLine("用户问题：${result.query}")
            appendLine()
            append(entries.joinToString(separator = "\n\n"))
        }.trim()
    }

    private fun String.limitSnippet(): String {
        if (length <= MaxSnippetLength) return this
        return take(MaxSnippetLength).trimEnd() + "..."
    }

    private const val MaxResults = 5
    private const val MaxSnippetLength = 500
}
