package com.example.orbblaze.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

@Composable
fun Shooter(angle: Float, currentBubbleColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier
            .size(120.dp)
            .padding(bottom = 20.dp)
        ) {
            // Rotamos el dibujo según el ángulo (en grados)
            rotate(degrees = angle, pivot = center) {
                // Cuerpo del cañón (Estilo Neón)
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color.DarkGray, Color.Black)),
                    size = Size(40f, 100f),
                    topLeft = Offset(center.x - 20f, center.y - 80f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                // Brillo de la boca del cañón
                drawCircle(
                    color = currentBubbleColor.copy(alpha = 0.5f),
                    radius = 25f,
                    center = Offset(center.x, center.y - 80f)
                )
            }
        }
    }
}