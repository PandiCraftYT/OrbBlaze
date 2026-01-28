package com.example.orbblaze.ui.theme

import android.app.Activity
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

// Definimos el esquema de colores para el juego (usando tus colores nuevos)
private val DarkColorScheme = darkColorScheme(
    primary = BubbleCyan,       // Usamos el Cyan como color principal
    secondary = BubblePurple,    // Púrpura para elementos secundarios
    tertiary = BubbleBlue,       // Azul para acentos
    background = GameBackground, // Nuestro fondo oscuro
    surface = GameBackground,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

// Para un juego estético, a veces es mejor forzar el tema oscuro
private val LightColorScheme = lightColorScheme(
    primary = BubbleBlue,
    secondary = BubblePurple,
    tertiary = BubbleCyan
)

@Composable
fun OrbBlazeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Lo desactivamos para que no rompa nuestra estética
    content: @Composable () -> Unit
) {
    // Forzamos el esquema oscuro para que el juego siempre se vea "bonito"
    val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}