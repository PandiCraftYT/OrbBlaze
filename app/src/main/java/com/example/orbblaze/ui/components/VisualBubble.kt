package com.example.orbblaze.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

@Composable
fun VisualBubble(
    color: Color,
    modifier: Modifier = Modifier,
    isRainbow: Boolean = false,
    rainbowRotation: Float = 0f
) {
    // Colores constantes fuera del ciclo de dibujo
    val goldDark = Color(0xFFB8860B)
    val goldLight = Color(0xFFFFD700)
    val gemHighlight = Color.White.copy(alpha = 0.8f)
    
    val rainbowColors = remember {
        listOf(
            Color.Red, Color(0xFFFF7F00), Color.Yellow,
            Color.Green, Color.Blue, Color(0xFF4B0082), Color(0xFF8B00FF)
        )
    }

    Spacer(
        modifier = modifier
            .size(44.dp)
            .drawWithCache {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2, size.height / 2)
                
                // Caché del engarce de oro
                val goldBrush = Brush.sweepGradient(
                    colors = listOf(goldDark, goldLight, goldDark, goldLight, goldDark),
                    center = center
                )
                
                // Caché del gradiente arcoíris
                val rainbowBrush = Brush.sweepGradient(rainbowColors, center)
                
                // Caché de la gema normal
                val gemDarkColor = color.copy(
                    red = color.red * 0.5f, 
                    green = color.green * 0.5f, 
                    blue = color.blue * 0.5f
                )
                val gemBrush = Brush.radialGradient(
                    colors = listOf(color, gemDarkColor),
                    center = center.copy(y = center.y - radius * 0.2f),
                    radius = radius * 0.9f
                )

                onDrawBehind {
                    // 1. Engarce Oro
                    drawCircle(
                        brush = goldBrush,
                        radius = radius - 2f,
                        center = center,
                        style = Stroke(width = 4f)
                    )

                    // 2. Gema
                    if (isRainbow) {
                        rotate(rainbowRotation, center) {
                            drawCircle(
                                brush = rainbowBrush,
                                radius = radius * 0.85f,
                                center = center
                            )
                        }
                    } else {
                        drawCircle(
                            brush = gemBrush,
                            radius = radius * 0.85f,
                            center = center
                        )
                    }

                    // 3. Brillos (Capa superior)
                    drawOval(
                        color = gemHighlight,
                        topLeft = Offset(center.x - radius * 0.5f, center.y - radius * 0.7f),
                        size = Size(radius * 0.6f, radius * 0.4f)
                    )
                    drawOval(
                        color = gemHighlight.copy(alpha = 0.4f),
                        topLeft = Offset(center.x + radius * 0.2f, center.y + radius * 0.3f),
                        size = Size(radius * 0.2f, radius * 0.15f)
                    )
                }
            }
    )
}
