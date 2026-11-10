package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = BrushedBrass,
    onPrimary = Color(0xFF1F1305),
    primaryContainer = Color(0xFF4A3114),
    onPrimaryContainer = SoftBrassContainer,
    secondary = Color(0xFF67B7B9),
    onSecondary = Color(0xFF07292A),
    secondaryContainer = Color(0xFF1B4B4D),
    onSecondaryContainer = TealContainer,
    tertiary = Color(0xFFD5A97A),
    background = DeepStudioDark,
    onBackground = Color(0xFFF3EFE8),
    surface = SlateCharcoal,
    onSurface = Color(0xFFF5F2EB),
    surfaceVariant = ElevatedDarkCard,
    onSurfaceVariant = Color(0xFFC8C2B8),
    outline = Color(0xFF4F5D60),
    error = Color(0xFFFF8A80)
)

private val LightColorScheme = lightColorScheme(
    primary = WarmWalnut,
    onPrimary = Color.White,
    primaryContainer = SoftBrassContainer,
    onPrimaryContainer = OnSoftBrassContainer,
    secondary = ArchitecturalTeal,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = OnTealContainer,
    tertiary = BrushedBrass,
    background = AlabasterLight,
    onBackground = SlateCharcoal,
    surface = WarmCardSurface,
    onSurface = SlateCharcoal,
    surfaceVariant = WarmSandSurface,
    onSurfaceVariant = Color(0xFF535D5F),
    outline = StoneOutline,
    error = CrimsonError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
