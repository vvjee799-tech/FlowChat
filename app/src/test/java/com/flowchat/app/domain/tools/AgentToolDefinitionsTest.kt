package com.flowchat.app.domain.tools

import com.flowchat.app.domain.model.ChatRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentToolDefinitionsTest {
    private val json = Json { explicitNulls = false }

    @Test
    fun exposesAppUsageToolsAlongsideOptionalWebSearch() {
        val request = ChatRequest(
            model = "gpt-test",
            messages = emptyList(),
            temperature = 1.0,
            topP = 1.0,
            maxTokens = null
        )

        val withLifeTools = AgentToolDefinitions.withLifestyleTools(request, includeWebSearch = true)

        assertEquals(
            listOf(
                AgentToolDefinitions.WebSearchToolName,
                AgentToolDefinitions.AppUsageSummaryToolName,
                AgentToolDefinitions.RecentAppActivityToolName
            ),
            withLifeTools.tools.map { it.name }
        )
        assertEquals("auto", withLifeTools.toolChoice)
        assertEquals(false, withLifeTools.parallelToolCalls)
    }

    @Test
    fun appUsageToolSchemasDeclareSafeArguments() {
        val payload = json.encodeToString(AgentToolDefinitions.appUsageSummary().parameters)
        val recentPayload = json.encodeToString(AgentToolDefinitions.recentAppActivity().parameters)

        assertTrue(payload.contains(""""range""""))
        assertTrue(payload.contains(""""today""""))
        assertTrue(payload.contains(""""yesterday""""))
        assertTrue(payload.contains(""""last_7_days""""))
        assertTrue(recentPayload.contains(""""hours""""))
        assertTrue(recentPayload.contains(""""minimum":1"""))
        assertTrue(recentPayload.contains(""""maximum":24"""))
    }
}
