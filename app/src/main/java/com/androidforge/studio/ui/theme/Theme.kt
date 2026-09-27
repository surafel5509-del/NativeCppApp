package com.androidforge.studio.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Forge palette
val ForgeOrange = Color(0xFFFF6D00)
val ForgeOrangeDim = Color(0xFFCC5500)
val ForgeAmber = Color(0xFFFFB74D)
val ForgeCharcoal = Color(0xFF101418)
val ForgeSlate = Color(0xFF1A2026)
val ForgeTeal = Color(0xFF1DE9B6)
val ForgeRed = Color(0xFFFF5252)
val ForgeBlue = Color(0xFF40C4FF)

private val DarkScheme = darkColorScheme(
    primary = ForgeOrange,
    onPrimary = Color.Black,
    primaryContainer = ForgeOrangeDim,
    onPrimaryContainer = Color.White,
    secondary = ForgeAmber,
    tertiary = ForgeTeal,
    background = ForgeCharcoal,
    surface = ForgeSlate,
    surfaceVariant = Color(0xFF232A31),
    error = ForgeRed,
)

private val LightScheme = lightColorScheme(
    primary = ForgeOrangeDim,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF3E2723),
    secondary = Color(0xFFB26A00),
    tertiary = Color(0xFF00897B),
    error = Color(0xFFC62828),
)

/** Studio theme. Falls back to the Forge palette when dynamic color is unavailable. */
@Composable
fun AndroidForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ForgeTypography,
        content = content,
    )
}
