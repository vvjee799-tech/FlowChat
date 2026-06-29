package com.flowchat.app.data.repository

import android.content.Context
import com.flowchat.app.data.memory.JsonMemoryStore
import com.flowchat.app.domain.model.MemoryRecord
import com.flowchat.app.domain.repository.MemoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class FileMemoryRepository @Inject constructor(
    @ApplicationContext context: Context
) : MemoryRepository {
    private val store = JsonMemoryStore(File(context.filesDir, MemoryStoreFileName))
    private val mutex = Mutex()

    override suspend fun retrieve(userMessage: String, topN: Int): List<MemoryRecord> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                store.retrieve(userMessage, topN)
            }
        }

    override suspend fun saveTurn(userMessage: String, assistantReply: String) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                store.appendTurn(userMessage, assistantReply, System.currentTimeMillis())
            }
        }
    }

    private companion object {
        const val MemoryStoreFileName = "memory_store.json"
    }
}
