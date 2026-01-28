package com.example.orbblaze.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Usamos un esquema de color personalizado para el ambiente pirata
private val PirateColorScheme = darkColorScheme(
    primary = BubbleYellow,     // El oro es lo principal
    secondary = BubbleRed,      // Acentos rojos (como banderas)
    tertiary = BubbleBlue,      // El mar

    background = GameBackground, // Color pergamino
    surface = GameBackground,

    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.Black,  // Texto oscuro sobre fondo claro
    onSurface = Color.Black
)

@Composable
fun OrbBlazeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Forzamos el tema pirata
    val colorScheme = PirateColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // La barra de estado coincide con el fondo del mapa
            window.statusBarColor = GameBackground.toArgb()
            window.navigationBarColor = GameBackground.toArgb()

            // Iconos oscuros (negros) porque el fondo es claro
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}