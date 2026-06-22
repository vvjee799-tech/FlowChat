package com.flowchat.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF101010),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6E6E0),
    onPrimaryContainer = Color(0xFF101010),
    secondary = Color(0xFF4E4E48),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9E9E3),
    onSecondaryContainer = Color(0xFF101010),
    tertiary = Color(0xFF2563EB),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFBFBF8),
    onBackground = Color(0xFF101010),
    surface = Color(0xFFF3F3EE),
    onSurface = Color(0xFF101010),
    surfaceVariant = Color(0xFFEDEDE7),
    onSurfaceVariant = Color(0xFF3D3D38),
    outline = Color(0xFF77776F),
    outlineVariant = Color(0xFFD9D9D2),
    error = Color(0xFFB42318),
    onError = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF7F7F2),
    onPrimary = Color(0xFF050505),
    primaryContainer = Color(0xFF2F2F34),
    onPrimaryContainer = Color(0xFFF7F7F2),
    secondary = Color(0xFFD8D8D0),
    onSecondary = Color(0xFF050505),
    secondaryContainer = Color(0xFF2A2A2F),
    onSecondaryContainer = Color(0xFFF7F7F2),
    tertiary = Color(0xFF60A5FA),
    onTertiary = Color(0xFF050505),
    background = Color(0xFF050505),
    onBackground = Color(0xFFF7F7F2),
    surface = Color(0xFF17171A),
    onSurface = Color(0xFFF7F7F2),
    surfaceVariant = Color(0xFF202024),
    onSurfaceVariant = Color(0xFFD8D8D0),
    outline = Color(0xFF8B8B84),
    outlineVariant = Color(0xFF34343A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val FlowTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)

@Composable
fun FlowChatTheme(
    appAppearance: AppAppearance = AppAppearance.Light,
    content: @Composable () -> Unit
) {
    val colors = when (appAppearance) {
        AppAppearance.Dark -> DarkColors
        AppAppearance.Light -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = FlowTypography,
        content = content
    )
}
