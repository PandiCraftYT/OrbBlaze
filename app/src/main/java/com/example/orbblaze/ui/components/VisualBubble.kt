package com.example.orbblaze.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

@Composable
fun VisualBubble(
    color: Color,
    modifier: Modifier = Modifier,
    isRainbow: Boolean = false // ✅ NUEVO PARÁMETRO
) {
    // ✅ Animación para el arcoíris
    val infiniteTransition = rememberInfiniteTransition(label = "rainbow")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .size(44.dp)
            .padding(1.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2, size.height / 2)

            val goldDark = Color(0xFFB8860B) // Oro oscuro
            val goldLight = Color(0xFFFFD700) // Oro brillante
            val gemHighlight = Color.White.copy(alpha = 0.8f)

            // 1. EL ENGARCE DE ORO (Borde exterior) - COMÚN A TODOS
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(goldDark, goldLight, goldDark, goldLight, goldDark),
                    center = center
                ),
                radius = radius - 2f,
                center = center,
                style = Stroke(width = 4f)
            )

            // 2. LA GEMA CENTRAL (Cuerpo principal)
            if (isRainbow) {
                // --- LÓGICA ARCOÍRIS ---
                val rainbowColors = listOf(
                    Color.Red, Color(0xFFFF7F00), Color.Yellow,
                    Color.Green, Color.Blue, Color(0xFF4B0082), Color(0xFF8B00FF)
                )

                // Gema giratoria multicolor
                rotate(rotation, center) {
                    drawCircle(
                        brush = Brush.sweepGradient(rainbowColors, center),
                        radius = radius * 0.85f,
                        center = center
                    )
                }
            } else {
                // --- LÓGICA NORMAL (TU DISEÑO ORIGINAL) ---
                val gemBase = color
                val gemDark = color.copy(red = color.red * 0.5f, green = color.green * 0.5f, blue = color.blue * 0.5f)

                // Gradiente radial para dar profundidad
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(gemBase, gemDark),
                        center = center.copy(y = center.y - radius * 0.2f),
                        radius = radius * 0.9f
                    ),
                    radius = radius * 0.85f,
                    center = center
                )
            }

            // 3. BRILLO ESPECULAR (El "toque" de joya) - COMÚN A TODOS
            drawOval(
                color = gemHighlight,
                topLeft = Offset(center.x - radius * 0.5f, center.y - radius * 0.7f),
                size = Size(radius * 0.6f, radius * 0.4f)
            )

            // 4. PEQUEÑO BRILLO SECUNDARIO (Abajo) - COMÚN A TODOS
            drawOval(
                color = gemHighlight.copy(alpha = 0.4f),
                topLeft = Offset(center.x + radius * 0.2f, center.y + radius * 0.3f),
                size = Size(radius * 0.2f, radius * 0.15f)
            )
        }
    }
}