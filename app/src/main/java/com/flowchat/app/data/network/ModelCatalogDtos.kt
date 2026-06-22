package com.flowchat.app.data.network

import kotlinx.serialization.Serializable

@Serializable
data class OpenAiModelsResponse(
    val data: List<OpenAiModelDto> = emptyList()
)

@Serializable
data class OpenAiModelDto(
    val id: String
)

@Serializable
data class OllamaTagsResponse(
    val models: List<OllamaModelDto> = emptyList()
)

@Serializable
data class OllamaModelDto(
    val name: String = ""
)

fun OpenAiModelsResponse.toModelIds(): List<String> =
    data.map { it.id }.cleanModelNames()

fun OllamaTagsResponse.toModelNames(): List<String> =
    models.map { it.name }.cleanModelNames()

private fun List<String>.cleanModelNames(): List<String> =
    map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
