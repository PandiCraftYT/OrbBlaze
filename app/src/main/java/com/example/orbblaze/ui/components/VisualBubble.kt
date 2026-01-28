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
            .padding(1.dp) // Pequeño margen para que no se toquen
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val centerOffset = Offset(size.width / 2, size.height / 2)

            // 1. CUERPO PRINCIPAL (Degradado 3D)
            // Simula luz viniendo de arriba a la izquierda
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.6f), // Brillo central (punto de luz)
                        color,                          // Color real
                        color.copy(red = color.red * 0.8f, green = color.green * 0.8f, blue = color.blue * 0.8f) // Sombra borde
                    ),
                    center = Offset(size.width * 0.3f, size.height * 0.3f),
                    radius = radius * 1.2f
                ),
                radius = radius,
                center = centerOffset
            )

            // 2. BORDE SUTIL (Para definir la forma en fondos claros)
            drawCircle(
                color = color.copy(alpha = 0.5f),
                radius = radius,
                center = centerOffset,
                style = Stroke(width = 2f)
            )

            // 3. REFLEJO ESPECULAR (El "brillo de caramelo")
            // Dibuja un óvalo blanco nítido arriba a la izquierda
            drawOval(
                color = Color.White.copy(alpha = 0.9f),
                topLeft = Offset(size.width * 0.2f, size.height * 0.15f),
                size = Size(size.width * 0.25f, size.height * 0.15f)
            )

            // 4. LUZ DE REBOTE (Rim Light abajo a la derecha)
            // Un pequeño reflejo suave abajo para dar transparencia
            drawArc(
                color = Color.White.copy(alpha = 0.4f),
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(size.width * 0.2f, size.height * 0.2f),
                size = Size(size.width * 0.6f, size.height * 0.6f),
                style = Stroke(width = 3f)
            )
        }
    }
}