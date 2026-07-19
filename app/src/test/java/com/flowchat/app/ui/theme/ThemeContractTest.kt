package com.flowchat.app.ui.theme

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContractTest {
    @Test
    fun lightAndDarkThemesDefineNeutralContainerTones() {
        val source = File("src/main/java/com/flowchat/app/ui/theme/Theme.kt").readText()

        assertTrue(source.contains("surfaceContainerHigh = Color(0xFFE6E9ED)"))
        assertTrue(source.contains("surfaceContainerHigh = Color(0xFF171D23)"))
        assertTrue(source.contains("surfaceContainerHighest = Color(0xFFDDE2E8)"))
        assertTrue(source.contains("surfaceContainerHighest = Color(0xFF1D252D)"))
    }
}
