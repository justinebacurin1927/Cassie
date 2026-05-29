package com.example.cassie.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Spotify-style Dark Theme with Purple Accent ───────────────────
val PurpleAccent  = Color(0xFFBB86FC)
val PureBlack     = Color(0xFF000000)
val DarkGrey      = Color(0xFF121212)
val CardGrey      = Color(0xFF1E1E1E)
val SurfaceGrey   = Color(0xFF282828)
val TextPrimary   = Color.White
val TextSecondary = Color.White.copy(alpha = 0.6f)
val TextDim       = Color.White.copy(alpha = 0.35f)

private val DarkScheme = darkColorScheme(
    primary = PurpleAccent,
    onPrimary = PureBlack,
    secondary = PurpleAccent,
    onSecondary = PureBlack,
    background = PureBlack,
    surface = DarkGrey,
    surfaceContainerLow = CardGrey,
    surfaceContainer = SurfaceGrey,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outlineVariant = PurpleAccent.copy(alpha = 0.2f),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF7C4DFF),
    onPrimary = Color.White,
    secondary = Color(0xFFE040FB),
    onSecondary = Color.White,
    background = Color(0xFFF5F0FF),
    surface = Color.White,
    surfaceContainerLow = Color(0xFFF5F0FF),
    surfaceContainer = Color(0xFFEDE0FF),
    onBackground = Color(0xFF1A0033),
    onSurface = Color(0xFF1A0033),
    onSurfaceVariant = Color(0xFF4A3357),
    outlineVariant = PurpleAccent.copy(alpha = 0.2f),
)

@Composable
fun CassieTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content
    )
}
