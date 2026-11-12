package com.dfdx047.hiddenleaf.engine

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.dfdx047.hiddenleaf.ui.theme.LeafShapes
import com.dfdx047.hiddenleaf.ui.theme.LeafTypography
import com.dfdx047.hiddenleaf.ui.theme.ThemeMode

/**
 * The game menu's look: the game's own menus (warm paper, ink, orange) in the launcher's theme. The launcher's choice
 * (UiPrefs: dark, light or the system's, and Material You) is read from its preferences file, which the game's process
 * can open too: light is paper with ink, dark is ink with paper, Material You takes the wallpaper's colours as the
 * launcher does.
 */
data class MenuLook(val mode: ThemeMode = ThemeMode.DARK, val dynamic: Boolean = false) {
    companion object {
        /** The launcher's theme (the same "ui" preferences UiPrefs writes). */
        fun of(context: Context): MenuLook = runCatching {
            val p = context.getSharedPreferences("ui", Context.MODE_PRIVATE)
            MenuLook(
                mode = runCatching { ThemeMode.valueOf(p.getString("theme", ThemeMode.DARK.name)!!) }.getOrDefault(ThemeMode.DARK),
                dynamic = p.getBoolean("dynamic", false),
            )
        }.getOrDefault(MenuLook())
    }
}

/** Colours the menu needs beyond Material's: the header's band and the focus ring of the gamepad. */
data class MenuExtras(val bandStart: Color, val bandEnd: Color, val onBand: Color, val focus: Color, val coinInk: Color)

val LocalMenuExtras = staticCompositionLocalOf { MenuExtras(Paper.orange, Paper.red, Paper.ink, Paper.ink, Paper.ink) }

internal object Paper {
    val paper = Color(0xFFF5E6C4)
    val paperDark = Color(0xFFEAD3A0)
    val ink = Color(0xFF2E1C10)
    val inkDeep = Color(0xFF1C130C)
    val orange = Color(0xFFF28C1E)
    val red = Color(0xFFC9361F)
    val brown = Color(0xFF6B4A2E)
}

private val PaperLight = lightColorScheme(
    primary = Color(0xFFB04700),
    onPrimary = Color.White,
    primaryContainer = Paper.orange,
    onPrimaryContainer = Paper.ink,
    secondary = Paper.red,
    onSecondary = Paper.paper,
    secondaryContainer = Color(0xFFF3C9A8),
    onSecondaryContainer = Paper.ink,
    tertiary = Color(0xFF1E7A37),
    onTertiary = Color.White,
    background = Paper.paper,
    onBackground = Paper.ink,
    surface = Paper.paper,
    onSurface = Paper.ink,
    surfaceVariant = Paper.paperDark,
    onSurfaceVariant = Paper.brown,
    surfaceContainerLowest = Color(0xFFFBF1DC),
    surfaceContainerLow = Color(0xFFF0DDB4),
    surfaceContainer = Paper.paperDark,
    surfaceContainerHigh = Color(0xFFE2C78E),
    surfaceContainerHighest = Color(0xFFD9BB7E),
    outline = Color(0xFFB89868),
    outlineVariant = Color(0xFFD8BF8F),
    error = Color(0xFFB3261E),
)

private val InkDark = darkColorScheme(
    primary = Color(0xFFFF9A4D),
    onPrimary = Color(0xFF3A1600),
    primaryContainer = Paper.orange,
    onPrimaryContainer = Paper.ink,
    secondary = Color(0xFFE0583F),
    onSecondary = Paper.paper,
    secondaryContainer = Color(0xFF5A2414),
    onSecondaryContainer = Paper.paper,
    tertiary = Color(0xFF7FDB8F),
    onTertiary = Color(0xFF00391A),
    background = Paper.inkDeep,
    onBackground = Paper.paper,
    surface = Paper.inkDeep,
    onSurface = Paper.paper,
    surfaceVariant = Color(0xFF3A2616),
    onSurfaceVariant = Color(0xFFCDB48C),
    surfaceContainerLowest = Color(0xFF140D08),
    surfaceContainerLow = Color(0xFF22170E),
    surfaceContainer = Paper.ink,
    surfaceContainerHigh = Color(0xFF3A2616),
    surfaceContainerHighest = Color(0xFF46301C),
    outline = Color(0xFF8A6A48),
    outlineVariant = Color(0xFF4E3620),
    error = Color(0xFFFF8A80),
)

@Composable
fun MenuTheme(look: MenuLook, content: @Composable () -> Unit) {
    val dark = when (look.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val context = LocalContext.current
    val dynamic = look.dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme: ColorScheme = when {
        dynamic -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> InkDark
        else -> PaperLight
    }
    val extras = if (dynamic) {
        MenuExtras(scheme.primaryContainer, scheme.tertiaryContainer, scheme.onPrimaryContainer, scheme.onSurface, scheme.onSurface)
    } else {
        // the band of the game's menus, orange into red, ink on it; the ring of the focused control in the text's colour
        MenuExtras(Paper.orange, Paper.red, Paper.ink, if (dark) Paper.paper else Paper.ink, Paper.ink)
    }
    MaterialTheme(colorScheme = scheme, typography = LeafTypography, shapes = LeafShapes) {
        CompositionLocalProvider(LocalContentColor provides scheme.onSurface, LocalMenuExtras provides extras, content = content)
    }
}
