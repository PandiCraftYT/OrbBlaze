package com.example.orbblaze.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun VisualBubble(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .padding(1.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2, size.height / 2)

            // Colores para la gema y el oro
            val gemBase = color
            val gemDark = color.copy(red = color.red * 0.5f, green = color.green * 0.5f, blue = color.blue * 0.5f)
            val gemHighlight = Color.White.copy(alpha = 0.8f)

            val goldDark = Color(0xFFB8860B) // Oro oscuro
            val goldLight = Color(0xFFFFD700) // Oro brillante

            // 1. EL ENGARCE DE ORO (Borde exterior)
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
            // Gradiente radial para dar profundidad y aspecto de piedra preciosa pulida
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        gemBase,    // Centro brillante
                        gemDark     // Bordes oscuros
                    ),
                    center = center.copy(y = center.y - radius * 0.2f), // Luz viene de arriba
                    radius = radius * 0.9f
                ),
                radius = radius * 0.85f,
                center = center
            )

            // 3. BRILLO ESPECULAR (El "toque" de joya)
            // Un reflejo blanco nítido en la parte superior
            drawOval(
                color = gemHighlight,
                topLeft = Offset(center.x - radius * 0.5f, center.y - radius * 0.7f),
                size = Size(radius * 0.6f, radius * 0.4f)
            )

            // 4. PEQUEÑO BRILLO SECUNDARIO (Abajo)
            drawOval(
                color = gemHighlight.copy(alpha = 0.4f),
                topLeft = Offset(center.x + radius * 0.2f, center.y + radius * 0.3f),
                size = Size(radius * 0.2f, radius * 0.15f)
            )
        }
    }
}