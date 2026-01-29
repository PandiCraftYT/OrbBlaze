package com.example.orbblaze.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme // Usamos Light para cartoon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Esquema de colores Cartoon (Brillante y alegre)
private val CartoonColorScheme = lightColorScheme(
    primary = BubbleBlue,
    secondary = BubbleYellow,
    tertiary = BubbleRed,

    background = GameBackground, // Ahora sí existe esta variable
    surface = GameBackground,

    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black, // Texto oscuro sobre fondo claro
    onSurface = Color.Black
)

@Composable
fun OrbBlazeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Forzamos el esquema Cartoon
    val colorScheme = CartoonColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Barra de estado del color del cielo
            window.statusBarColor = GameBackground.toArgb()
            window.navigationBarColor = GameBackground.toArgb()

            // Iconos oscuros (true) porque el fondo es claro
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