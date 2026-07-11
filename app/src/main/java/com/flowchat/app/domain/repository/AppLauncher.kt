package com.flowchat.app.domain.repository

interface AppLauncher {
    suspend fun openApp(appName: String): String
}
