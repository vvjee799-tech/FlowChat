package com.flowchat.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE8FF),
    onPrimaryContainer = Color(0xFF0F2E69),
    secondary = Color(0xFF4B5563),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9EDF2),
    onSecondaryContainer = Color(0xFF101214),
    tertiary = Color(0xFF2563EB),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F8F7),
    onBackground = Color(0xFF101214),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101214),
    surfaceVariant = Color(0xFFECEFF3),
    onSurfaceVariant = Color(0xFF5B6470),
    surfaceContainerLow = Color(0xFFF2F4F7),
    surfaceContainer = Color(0xFFECEFF3),
    surfaceContainerHigh = Color(0xFFE6E9ED),
    surfaceContainerHighest = Color(0xFFDDE2E8),
    outline = Color(0xFF77808C),
    outlineVariant = Color(0xFFD9DEE5),
    error = Color(0xFFB42318),
    onError = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4D86FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1F355E),
    onPrimaryContainer = Color(0xFFE9EEF4),
    secondary = Color(0xFFB8C2CC),
    onSecondary = Color(0xFF07090D),
    secondaryContainer = Color(0xFF242B33),
    onSecondaryContainer = Color(0xFFE9EEF4),
    tertiary = Color(0xFF4D86FF),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFF07090D),
    onBackground = Color(0xFFE9EEF4),
    surface = Color(0xFF11161B),
    onSurface = Color(0xFFE9EEF4),
    surfaceVariant = Color(0xFF1B2229),
    onSurfaceVariant = Color(0xFFB7C0C9),
    surfaceContainerLow = Color(0xFF0D1217),
    surfaceContainer = Color(0xFF11161B),
    surfaceContainerHigh = Color(0xFF171D23),
    surfaceContainerHighest = Color(0xFF1D252D),
    outline = Color(0xFF68727D),
    outlineVariant = Color(0xFF2C343D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val FlowTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.sp
    )
)

@Composable
fun FlowChatTheme(
    appAppearance: AppAppearance = AppAppearance.Dark,
    content: @Composable () -> Unit
) {
    val useDarkColors = when (appAppearance) {
        AppAppearance.System -> isSystemInDarkTheme()
        AppAppearance.Dark -> true
        AppAppearance.Light -> false
    }
    val colors = when (appAppearance) {
        AppAppearance.System -> if (useDarkColors) DarkColors else LightColors
        AppAppearance.Dark -> DarkColors
        AppAppearance.Light -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = FlowTypography,
        content = content
    )
}
