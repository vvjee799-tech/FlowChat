package com.flowchat.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flowchat.app.domain.model.ProviderConfig

@Entity(tableName = "providers")
data class ProviderConfigEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val baseUrl: String,
    val defaultModel: String,
    val customHeadersJson: String,
    val apiKeyAlias: String?,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

fun ProviderConfigEntity.toDomain(): ProviderConfig = ProviderConfig(
    id = id,
    displayName = displayName,
    baseUrl = baseUrl,
    defaultModel = defaultModel,
    customHeadersJson = customHeadersJson,
    apiKeyAlias = apiKeyAlias,
    isEnabled = isEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ProviderConfig.toEntity(): ProviderConfigEntity = ProviderConfigEntity(
    id = id,
    displayName = displayName,
    baseUrl = baseUrl,
    defaultModel = defaultModel,
    customHeadersJson = customHeadersJson,
    apiKeyAlias = apiKeyAlias,
    isEnabled = isEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt
)
