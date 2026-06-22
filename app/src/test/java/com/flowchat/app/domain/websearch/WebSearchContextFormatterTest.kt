package com.flowchat.app.domain.websearch

import com.flowchat.app.domain.model.WebSearchResult
import com.flowchat.app.domain.model.WebSearchResultItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebSearchContextFormatterTest {
    @Test
    fun formatsAtMostFiveSearchResultsAsHiddenSystemContext() {
        val result = WebSearchResult(
            query = "today's AI news",
            results = (1..6).map { index ->
                WebSearchResultItem(
                    title = "Title $index",
                    url = "https://example.com/$index",
                    content = "Summary $index"
                )
            }
        )

        val context = WebSearchContextFormatter.format(result)

        assertTrue(context.contains("以下是联网搜索结果，仅用于回答用户问题："))
        assertTrue(context.contains("[1] Title 1"))
        assertTrue(context.contains("摘要：Summary 1"))
        assertTrue(context.contains("URL：https://example.com/1"))
        assertTrue(context.contains("[5] Title 5"))
        assertFalse(context.contains("[6] Title 6"))
    }

    @Test
    fun tellsModelToAnswerFromSearchResultsInsteadOfClaimingNoInternet() {
        val result = WebSearchResult(
            query = "latest ai news today",
            results = listOf(
                WebSearchResultItem(
                    title = "AI News",
                    url = "https://example.com/ai",
                    content = "A current AI news summary."
                )
            )
        )

        val context = WebSearchContextFormatter.format(result)

        assertTrue(context.contains("你已经获得联网搜索结果"))
        assertTrue(context.contains("必须基于这些搜索结果回答"))
        assertTrue(context.contains("不要声称无法联网"))
    }

    @Test
    fun trimsVeryLongSearchSnippetsBeforeModelInjection() {
        val result = WebSearchResult(
            query = "long result",
            results = listOf(
                WebSearchResultItem(
                    title = "Long",
                    url = "https://example.com/long",
                    content = "a".repeat(900)
                )
            )
        )

        val context = WebSearchContextFormatter.format(result)

        assertTrue(context.length < 800)
        assertTrue(context.contains("..."))
    }
}
