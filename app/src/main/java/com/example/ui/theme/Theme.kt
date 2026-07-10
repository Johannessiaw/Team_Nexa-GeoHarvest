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
    primary = HarvestGreenPrimary,
    secondary = GoldAccent,
    tertiary = SoftYellow,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = TextOnPrimaryLight,
    onSecondary = TextOnPrimaryLight,
    onBackground = TextBodyDark,
    onSurface = TextBodyDark
)

private val LightColorScheme = lightColorScheme(
    primary = HarvestGreenPrimary,
    secondary = GoldAccent,
    tertiary = SoftYellow,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = TextOnPrimaryLight,
    onSecondary = TextOnPrimaryLight,
    onBackground = TextBodyLight,
    onSurface = TextBodyLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to enforce our beautiful branded harvest theme
    content: @Composable () -> Unit,
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
