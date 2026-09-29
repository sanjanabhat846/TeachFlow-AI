package com.teachflow.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DeepIndigo = Color(0xFF0F172A)
val SurfaceDark = Color(0xFF1E293B)
val CardDark = Color(0xFF334155)
val AccentPurple = Color(0xFF8B5CF6)
val GlowCyan = Color(0xFF06B6D4)
val EmeraldGreen = Color(0xFF10B981)
val AlertAmber = Color(0xFFF59E0B)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)

private val DarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    secondary = GlowCyan,
    tertiary = EmeraldGreen,
    background = DeepIndigo,
    surface = SurfaceDark,
    surfaceVariant = CardDark,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    error = Color(0xFFEF4444)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6366F1),
    secondary = Color(0xFF0EA5E9),
    tertiary = Color(0xFF10B981),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    error = Color(0xFFDC2626)
)

@Composable
fun TeachFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
