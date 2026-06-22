package com.flowchat.app.data.network

import android.util.Log
import com.flowchat.app.BuildConfig
import com.flowchat.app.domain.model.ChatDelta
import com.flowchat.app.domain.model.ChatRequest
import com.flowchat.app.domain.model.ProviderConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Singleton
class KtorOpenAiCompatibleClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json
) : ChatCompletionClient {
    override fun streamChat(request: ChatRequest, provider: ProviderConfig, apiKey: String?): Flow<ChatDelta> = flow {
        var sawOutput = false
        var sawError = false
        val payload = request.toOpenAiRequest(provider, stream = true)
        if (BuildConfig.DEBUG) {
            Log.i(
                TAG,
                "request baseUrl=${provider.baseUrl} model=${request.model} messages=${request.messages.size} " +
                    "hasApiKey=${!apiKey.isNullOrBlank()} stream=${payload.stream} thinking=${payload.thinking?.type}"
            )
        }
        val statement = httpClient.preparePost {
            url(provider.baseUrl.trimEnd('/') + "/chat/completions")
            timeout {
                requestTimeoutMillis = ChatRequestTimeoutMillis
                socketTimeoutMillis = ChatSocketTimeoutMillis
                connectTimeoutMillis = ChatConnectTimeoutMillis
            }
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Accept, "text/event-stream")
            if (!apiKey.isNullOrBlank()) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
            provider.parseCustomHeaders().forEach { (key, value) ->
                header(key, value)
            }
            setBody(payload)
        }

        statement.execute { response ->
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "response status=${response.status.value}")
            }
            if (!response.status.isSuccess()) {
                emit(ChatDelta.Error(response.errorText()))
                return@execute
            }
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (BuildConfig.DEBUG) {
                    OpenAiStreamParser.summarizeLine(line)?.let { Log.i(TAG, "stream $it") }
                }
                when (val delta = OpenAiStreamParser.parseLine(line)) {
                    null -> Unit
                    ChatDelta.Done -> {
                        if (sawOutput) {
                            emit(delta)
                        }
                        return@execute
                    }
                    is ChatDelta.Content -> {
                        sawOutput = true
                        emit(delta)
                    }
                    is ChatDelta.Reasoning -> {
                        sawOutput = true
                        emit(delta)
                    }
                    is ChatDelta.FullResponse -> {
                        sawOutput = true
                        emit(delta)
                    }
                    is ChatDelta.Error -> {
                        sawError = true
                        emit(delta)
                        return@execute
                    }
                }
            }
            if (sawOutput) {
                emit(ChatDelta.Done)
            }
        }
        if (!sawOutput && !sawError) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "stream produced no content; retrying with non-streaming request")
            }
            when (val fallback = completeChat(request, provider, apiKey)) {
                is ChatDelta.Content -> {
                    emit(fallback)
                    emit(ChatDelta.Done)
                }
                is ChatDelta.Reasoning -> {
                    emit(fallback)
                    emit(ChatDelta.Done)
                }
                is ChatDelta.FullResponse -> {
                    emit(ChatDelta.Reasoning(fallback.reasoningText))
                    emit(ChatDelta.Content(fallback.contentText))
                    emit(ChatDelta.Done)
                }
                else -> emit(fallback)
            }
        }
    }

    private suspend fun completeChat(request: ChatRequest, provider: ProviderConfig, apiKey: String?): ChatDelta {
        val payload = request.toOpenAiRequest(provider, stream = false)
        val response = httpClient.preparePost {
            url(provider.baseUrl.trimEnd('/') + "/chat/completions")
            timeout {
                requestTimeoutMillis = ChatRequestTimeoutMillis
                socketTimeoutMillis = ChatSocketTimeoutMillis
                connectTimeoutMillis = ChatConnectTimeoutMillis
            }
            contentType(ContentType.Application.Json)
            if (!apiKey.isNullOrBlank()) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
            provider.parseCustomHeaders().forEach { (key, value) ->
                header(key, value)
            }
            setBody(payload)
        }.execute()
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "fallback response status=${response.status.value}")
        }
        if (!response.status.isSuccess()) {
            return ChatDelta.Error(response.errorText())
        }
        return OpenAiStreamParser.parseCompletionBody(response.bodyAsText())
    }

    private fun ProviderConfig.parseCustomHeaders(): Map<String, String> {
        if (customHeadersJson.isBlank() || customHeadersJson.trim() == "{}") return emptyMap()
        return runCatching {
            json.parseToJsonElement(customHeadersJson).jsonObject.mapValues { it.value.jsonPrimitive.content }
        }.getOrDefault(emptyMap())
    }

    private suspend fun HttpResponse.errorText(): String {
        val text = runCatching { body<String>() }.getOrDefault("")
        return "HTTP ${status.value}: ${text.ifBlank { status.description }}"
    }

    private companion object {
        const val TAG = "FlowChatNetwork"
        const val ChatConnectTimeoutMillis = 30_000L
        const val ChatSocketTimeoutMillis = 5 * 60 * 1000L
        const val ChatRequestTimeoutMillis = 10 * 60 * 1000L
    }
}
