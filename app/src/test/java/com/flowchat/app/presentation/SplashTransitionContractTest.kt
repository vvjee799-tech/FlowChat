package com.flowchat.app.presentation

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashTransitionContractTest {
    @Test
    fun startupUsesProvidedSplashImageBeforeEnteringMainChat() {
        val mainActivity = File("src/main/java/com/flowchat/app/MainActivity.kt").readText()
        val splashTransitionFile = File("src/main/java/com/flowchat/app/presentation/SplashTransition.kt")
        val styles = File("src/main/res/values/styles.xml").readText()
        val windowBackgroundFile = File("src/main/res/drawable/splash_window_background.xml")
        val nightWindowBackgroundFile = File("src/main/res/drawable-night/splash_window_background.xml")
        val androidTwelveStylesFile = File("src/main/res/values-v31/styles.xml")
        val androidTwelveNightStylesFile = File("src/main/res/values-night-v31/styles.xml")
        val transparentSplashIconFile = File("src/main/res/drawable/transparent_splash_icon.xml")
        val splashLightImage = File("src/main/res/drawable-nodpi/splash_transition_light.png")
        val splashDarkImage = File("src/main/res/drawable-nodpi/splash_transition_dark.png")

        assertTrue(splashTransitionFile.exists())
        assertTrue(windowBackgroundFile.exists())
        assertTrue(nightWindowBackgroundFile.exists())
        assertTrue(androidTwelveStylesFile.exists())
        assertTrue(androidTwelveNightStylesFile.exists())
        assertTrue(transparentSplashIconFile.exists())
        assertTrue(splashLightImage.exists())
        assertTrue(splashDarkImage.exists())

        val splashTransition = splashTransitionFile.readText()
        val windowBackground = windowBackgroundFile.readText()
        val nightWindowBackground = nightWindowBackgroundFile.readText()
        val androidTwelveStyles = androidTwelveStylesFile.readText()
        val androidTwelveNightStyles = androidTwelveNightStylesFile.readText()
        val transparentSplashIcon = transparentSplashIconFile.readText()

        assertTrue(mainActivity.contains("SplashTransition("))
        assertTrue(mainActivity.indexOf("SplashTransition(") < mainActivity.indexOf("FlowChatRoot(appAppearance = appAppearance"))
        assertTrue(splashTransition.contains("delay(SplashTransitionDurationMillis)"))
        assertTrue(splashTransition.contains("AnimatedVisibility("))
        assertTrue(splashTransition.contains("fadeOut("))
        assertTrue(splashTransition.contains("appAppearance: AppAppearance"))
        assertTrue(splashTransition.contains("if (appAppearance == AppAppearance.Dark)"))
        assertTrue(splashTransition.contains("R.drawable.splash_transition_dark"))
        assertTrue(splashTransition.contains("R.drawable.splash_transition_light"))
        assertTrue(splashTransition.contains("painterResource(splashImageRes)"))
        assertTrue(splashTransition.contains("ContentScale.Crop"))
        assertTrue(splashTransition.contains("contentDescription = null"))
        assertTrue(styles.contains("<item name=\"android:windowBackground\">@drawable/splash_window_background</item>"))
        assertTrue(windowBackground.contains("<layer-list"))
        assertTrue(windowBackground.contains("<bitmap"))
        assertTrue(windowBackground.contains("@drawable/splash_transition_light"))
        assertTrue(windowBackground.contains("android:gravity=\"fill\""))
        assertTrue(nightWindowBackground.contains("<layer-list"))
        assertTrue(nightWindowBackground.contains("<bitmap"))
        assertTrue(nightWindowBackground.contains("@drawable/splash_transition_dark"))
        assertTrue(nightWindowBackground.contains("android:gravity=\"fill\""))
        assertTrue(androidTwelveStyles.contains("android:windowSplashScreenAnimatedIcon"))
        assertTrue(androidTwelveStyles.contains("@drawable/transparent_splash_icon"))
        assertTrue(androidTwelveStyles.contains("android:windowSplashScreenBackground"))
        assertTrue(androidTwelveStyles.contains("#FFFFFF"))
        assertTrue(androidTwelveNightStyles.contains("android:windowSplashScreenAnimatedIcon"))
        assertTrue(androidTwelveNightStyles.contains("@drawable/transparent_splash_icon"))
        assertTrue(androidTwelveNightStyles.contains("android:windowSplashScreenBackground"))
        assertTrue(androidTwelveNightStyles.contains("#000000"))
        assertTrue(transparentSplashIcon.contains("android:alpha=\"0\""))

        val (lightWidth, lightHeight) = readPngDimensions(splashLightImage)
        val (darkWidth, darkHeight) = readPngDimensions(splashDarkImage)
        assertEquals(941, lightWidth)
        assertEquals(1672, lightHeight)
        assertEquals(941, darkWidth)
        assertEquals(1672, darkHeight)
    }
}

private fun readPngDimensions(file: File): Pair<Int, Int> {
    val bytes = file.readBytes()
    require(bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)))
    return readPngInt(bytes, 16) to readPngInt(bytes, 20)
}

private fun readPngInt(bytes: ByteArray, offset: Int): Int =
    ((bytes[offset].toInt() and 0xFF) shl 24) or
        ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
        ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
        (bytes[offset + 3].toInt() and 0xFF)
