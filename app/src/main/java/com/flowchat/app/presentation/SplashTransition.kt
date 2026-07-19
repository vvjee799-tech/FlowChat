package com.flowchat.app.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.flowchat.app.R
import com.flowchat.app.ui.theme.AppAppearance
import kotlinx.coroutines.delay

private const val SplashTransitionDurationMillis = 900L
private const val SplashTransitionFadeMillis = 220

@Composable
fun SplashTransition(
    appAppearance: AppAppearance,
    content: @Composable () -> Unit
) {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    val splashImageRes = if (appAppearance == AppAppearance.Dark) {
        R.drawable.splash_transition_dark
    } else {
        R.drawable.splash_transition_light
    }

    LaunchedEffect(Unit) {
        delay(SplashTransitionDurationMillis)
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        AnimatedVisibility(
            visible = showSplash,
            enter = EnterTransition.None,
            exit = fadeOut(animationSpec = tween(durationMillis = SplashTransitionFadeMillis))
        ) {
            Image(
                painter = painterResource(splashImageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
