package com.flowchat.app.domain.repository

import com.flowchat.app.domain.model.MemoryRecord

interface MemoryRepository {
    suspend fun retrieve(userMessage: String, topN: Int): List<MemoryRecord>
    suspend fun saveTurn(userMessage: String, assistantReply: String)
    suspend fun getAll(): List<MemoryRecord>
    suspend fun delete(id: String)
    suspend fun clear()
}
