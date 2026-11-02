package com.dfdx047.hiddenleaf.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { SYSTEM, DARK, LIGHT }

private val LeafDark = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = Konoha.Night,
    onBackground = Color(0xFFEDE6DE),
    surface = Konoha.Night,
    onSurface = Color(0xFFEDE6DE),
    surfaceVariant = Color(0xFF2A2E3A),
    onSurfaceVariant = Color(0xFFC8C6CF),
    surfaceContainerLowest = Color(0xFF08090D),
    surfaceContainerLow = Color(0xFF13151D),
    surfaceContainer = Konoha.NightHigh,
    surfaceContainerHigh = Color(0xFF20232F),
    surfaceContainerHighest = Color(0xFF292D3A),
    outline = Color(0xFF908E99),
    outlineVariant = Color(0xFF3E4150),
    error = Color(0xFFFF8A80),
)

private val LeafLight = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = Color(0xFFFFF8F0),
    surface = Color(0xFFFFF8F0),
    surfaceContainerLow = Color(0xFFFDF0E2),
    surfaceContainer = Color(0xFFF8EAD9),
    surfaceContainerHigh = Color(0xFFF2E3D1),
    surfaceContainerHighest = Color(0xFFECDCC9),
)

@Composable
fun HiddenLeafTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> LeafDark
        else -> LeafLight
    }
    MaterialTheme(colorScheme = scheme, typography = LeafTypography, shapes = LeafShapes) {
        // Text outside a Surface would be black otherwise, unreadable on the dark background.
        CompositionLocalProvider(LocalContentColor provides scheme.onSurface, content = content)
    }
}
