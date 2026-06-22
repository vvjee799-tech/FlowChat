package com.flowchat.app.data.network

import com.flowchat.app.domain.model.WebSearchResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TavilySearchClient @Inject constructor(
    private val httpClient: HttpClient
) : WebSearchClient {
    override suspend fun search(query: String, apiKey: String): WebSearchResult {
        val response = httpClient.post(TavilySearchEndpoint) {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(TavilySearchRequest(query = query))
        }.body<TavilySearchResponse>()

        return response.toDomain()
    }

    private companion object {
        const val TavilySearchEndpoint = "https://api.tavily.com/search"
    }
}
