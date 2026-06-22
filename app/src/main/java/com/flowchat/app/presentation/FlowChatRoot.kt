package com.flowchat.app.presentation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flowchat.app.presentation.chat.ChatScreen
import com.flowchat.app.presentation.provider.ProviderSettingsScreen
import com.flowchat.app.ui.theme.AppAppearance

@Composable
fun FlowChatRoot(
    appAppearance: AppAppearance,
    onAppAppearanceChange: (AppAppearance) -> Unit
) {
    val navController = rememberNavController()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = Route.Chat,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(Route.Chat) {
                ChatScreen(
                    onOpenProviders = { navController.navigate(Route.Providers) },
                    appAppearance = appAppearance,
                    onAppAppearanceChange = onAppAppearanceChange
                )
            }
            composable(Route.Providers) {
                ProviderSettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private object Route {
    const val Chat = "chat"
    const val Providers = "providers"
}
