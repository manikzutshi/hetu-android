package com.aurafarmers.hetu.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Terracotta500,
    onPrimary = Color.White,
    primaryContainer = Terracotta100,
    onPrimaryContainer = Terracotta900,
    
    secondary = Terracotta400,
    onSecondary = Color.White,
    secondaryContainer = Terracotta50,
    onSecondaryContainer = Terracotta800,
    
    tertiary = OasisGreen,
    onTertiary = Color.White,
    tertiaryContainer = OasisGreenLight,
    onTertiaryContainer = Color(0xFF1B4D2E),
    
    background = Sand50,
    onBackground = TextPrimary,
    
    surface = Sand100,
    onSurface = TextPrimary,
    surfaceVariant = Sand200,
    onSurfaceVariant = TextSecondary,
    
    outline = Sand400,
    outlineVariant = Sand300,
    
    error = SunsetRed,
    onError = Color.White,
    errorContainer = SunsetRedLight,
    onErrorContainer = Color(0xFF5A1A0E)
)

private val DarkColorScheme = darkColorScheme(
    primary = Terracotta400,
    onPrimary = Terracotta900,
    primaryContainer = Terracotta700,
    onPrimaryContainer = Terracotta100,
    
    secondary = Terracotta300,
    onSecondary = Terracotta900,
    secondaryContainer = Terracotta800,
    onSecondaryContainer = Terracotta100,
    
    tertiary = Color(0xFF7BC98A),
    onTertiary = Color(0xFF003919),
    tertiaryContainer = Color(0xFF1B4D2E),
    onTertiaryContainer = OasisGreenLight,
    
    background = SandDark50,
    onBackground = TextPrimaryDark,
    
    surface = SandDark100,
    onSurface = TextPrimaryDark,
    surfaceVariant = SandDark200,
    onSurfaceVariant = TextSecondaryDark,
    
    outline = SandDark400,
    outlineVariant = SandDark300,
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun HetuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled to use our custom Whispering Sands theme
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
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HetuTypography,
        content = content
    )
}
