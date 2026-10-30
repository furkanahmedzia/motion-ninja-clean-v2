package com.motionninja.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    primaryContainer = Color(0xFF3700B3),
    secondary = Color(0xFF03DAC6),
    secondaryContainer = Color(0xFF006064),
    tertiary = Color(0xFFFF0266),
    tertiaryContainer = Color(0xFF62002E),
    surface = Color(0xFF121212),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF2C2C2C),
    background = Color(0xFF121212),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF444444),
    error = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    primaryContainer = Color(0xFFE8DEF8),
    secondary = Color(0xFF018786),
    secondaryContainer = Color(0xFF80FFFA),
    tertiary = Color(0xFFB00020),
    tertiaryContainer = Color(0xFFFCD8DF),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHighest = Color(0xFFE0E0E0),
    background = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF444444),
    outline = Color(0xFF757575),
    error = Color(0xFFB00020)
)

@Composable
fun Theme(
    darkTheme: Boolean = androidx.compose.material3.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

object Typography {
    val headlineSmall = androidx.compose.material3.Typography().headlineSmall
    val titleLarge = androidx.compose.material3.Typography().titleLarge
    val titleMedium = androidx.compose.material3.Typography().titleMedium
    val titleSmall = androidx.compose.material3.Typography().titleSmall
    val bodyLarge = androidx.compose.material3.Typography().bodyLarge
    val bodyMedium = androidx.compose.material3.Typography().bodyMedium
    val bodySmall = androidx.compose.material3.Typography().bodySmall
    val labelLarge = androidx.compose.material3.Typography().labelLarge
    val labelSmall = androidx.compose.material3.Typography().labelSmall
    val caption = androidx.compose.material3.Typography().caption
}

object Shapes {
    val small = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    val medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    val large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
}