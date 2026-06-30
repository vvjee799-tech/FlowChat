package com.flowchat.app.domain.tools

import com.flowchat.app.domain.model.ChatRequest
import com.flowchat.app.domain.model.ChatToolCall
import com.flowchat.app.domain.model.ChatToolDefinition
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object AgentToolDefinitions {
    const val WebSearchToolName = "web_search"
    const val AppUsageSummaryToolName = "get_app_usage_summary"
    const val RecentAppActivityToolName = "get_recent_app_activity"

    fun webSearch(): ChatToolDefinition =
        ChatToolDefinition(
            name = WebSearchToolName,
            description = "Search the web for current or external information when needed to answer the user's latest message.",
            parameters = JsonObject(
                mapOf(
                    "type" to JsonPrimitive("object"),
                    "properties" to JsonObject(
                        mapOf(
                            "query" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string"),
                                    "description" to JsonPrimitive("The concise web search query to run.")
                                )
                            )
                        )
                    ),
                    "required" to JsonArray(listOf(JsonPrimitive("query"))),
                    "additionalProperties" to JsonPrimitive(false)
                )
            )
        )

    fun appUsageSummary(): ChatToolDefinition =
        ChatToolDefinition(
            name = AppUsageSummaryToolName,
            description = "Read the user's Android app foreground usage summary for a requested time range. This does not read content inside apps.",
            parameters = JsonObject(
                mapOf(
                    "type" to JsonPrimitive("object"),
                    "properties" to JsonObject(
                        mapOf(
                            "range" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string"),
                                    "description" to JsonPrimitive("Usage range to inspect."),
                                    "enum" to JsonArray(
                                        listOf(
                                            JsonPrimitive("today"),
                                            JsonPrimitive("yesterday"),
                                            JsonPrimitive("last_7_days")
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    "required" to JsonArray(listOf(JsonPrimitive("range"))),
                    "additionalProperties" to JsonPrimitive(false)
                )
            )
        )

    fun recentAppActivity(): ChatToolDefinition =
        ChatToolDefinition(
            name = RecentAppActivityToolName,
            description = "Read recent Android foreground app switches for the last 1 to 24 hours. This does not read content inside apps.",
            parameters = JsonObject(
                mapOf(
                    "type" to JsonPrimitive("object"),
                    "properties" to JsonObject(
                        mapOf(
                            "hours" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("integer"),
                                    "description" to JsonPrimitive("Number of recent hours to inspect."),
                                    "minimum" to JsonPrimitive(1),
                                    "maximum" to JsonPrimitive(24)
                                )
                            )
                        )
                    ),
                    "required" to JsonArray(listOf(JsonPrimitive("hours"))),
                    "additionalProperties" to JsonPrimitive(false)
                )
            )
        )

    fun withWebSearchTool(request: ChatRequest): ChatRequest =
        request.copy(
            tools = listOf(webSearch()),
            toolChoice = "auto",
            parallelToolCalls = false
        )

    fun withLifestyleTools(request: ChatRequest, includeWebSearch: Boolean): ChatRequest {
        val tools = buildList {
            if (includeWebSearch) add(webSearch())
            add(appUsageSummary())
            add(recentAppActivity())
        }
        return request.copy(
            tools = tools,
            toolChoice = "auto",
            parallelToolCalls = false
        )
    }

    fun toolCall(id: String, name: String, arguments: String): ChatToolCall =
        ChatToolCall(id = id, name = name, arguments = arguments)
}
