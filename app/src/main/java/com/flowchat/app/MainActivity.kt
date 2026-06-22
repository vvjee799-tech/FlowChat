package com.flowchat.app

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.flowchat.app.locale.AppLocale
import com.flowchat.app.presentation.FlowChatRoot
import com.flowchat.app.presentation.SplashTransition
import com.flowchat.app.ui.theme.AppAppearance
import com.flowchat.app.ui.theme.FlowChatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        enableEdgeToEdge()
        setContent {
            var appAppearance by remember { mutableStateOf(AppAppearance.load(this)) }
            FlowChatTheme(appAppearance = appAppearance) {
                SplashTransition(appAppearance = appAppearance, content = {
                    FlowChatRoot(appAppearance = appAppearance, onAppAppearanceChange = { appearance ->
                        appAppearance = appearance
                        AppAppearance.save(this, appearance)
                    })
                })
            }
        }
    }
}
