package com.flowchat.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MemoryRecord(
    val id: String,
    val goal: String,
    val summary: String,
    val quality: String,
    val timestamp: Long
)
