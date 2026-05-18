package com.videodownloader.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// AMOLED Dark theme colors
val Background = Color(0xFF000000)
val Surface = Color(0xFF0D0D0D)
val SurfaceVariant = Color(0xFF1A1A1A)
val Card = Color(0xFF111111)

val Primary = Color(0xFF6C63FF)       // Purple accent
val PrimaryVariant = Color(0xFF8B85FF)
val Secondary = Color(0xFF00E5FF)      // Cyan accent
val Tertiary = Color(0xFF00E676)       // Green for success

val OnBackground = Color(0xFFFFFFFF)
val OnSurface = Color(0xFFE0E0E0)
val OnPrimary = Color(0xFFFFFFFF)

val Error = Color(0xFFFF5252)
val Warning = Color(0xFFFFAB40)
val Success = Color(0xFF00E676)

val QueuedColor = Color(0xFF9E9E9E)
val DownloadingColor = Color(0xFF6C63FF)
val CompletedColor = Color(0xFF00E676)
val FailedColor = Color(0xFFFF5252)
val PausedColor = Color(0xFFFFAB40)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFF3D35CC),
    secondary = Secondary,
    tertiary = Tertiary,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onBackground = OnBackground,
    onSurface = OnSurface,
    error = Error
)

@Composable
fun VideoDownloaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
