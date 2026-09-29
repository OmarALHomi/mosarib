package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryTealLight,
    onPrimary = DarkBackground,
    primaryContainer = PrimaryTealDark,
    onPrimaryContainer = SecondaryAquaLight,
    secondary = SecondaryAqua,
    onSecondary = DarkBackground,
    secondaryContainer = SecondaryAquaDark,
    onSecondaryContainer = SecondaryAquaLight,
    tertiary = AccentGold,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = AccentCrimson,
    onError = DarkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    onPrimary = LightSurface,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = PrimaryTealDark,
    secondary = SecondaryAquaDark,
    onSecondary = LightSurface,
    secondaryContainer = SecondaryAquaLight,
    onSecondaryContainer = SecondaryAquaDark,
    tertiary = AccentGold,
    onTertiary = LightSurface,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = AccentCrimson,
    onError = LightSurface
)

@Composable
fun MosaribTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve brand luxury styling by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
