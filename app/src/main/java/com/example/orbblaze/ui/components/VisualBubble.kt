package com.example.orbblaze.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VisualBubble(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp) // Tamaño estándar
            .padding(2.dp)
            .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = color)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.9f), color),
                    center = Offset(0.4f, 0.4f), // Descentrado para efecto 3D
                ),
                shape = CircleShape
            )
    ) {
        // Brillo de "cristal" en la parte superior
        Box(
            modifier = Modifier
                .fillMaxSize(0.4f)
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 8.dp)
                .background(Color.White.copy(alpha = 0.3f), CircleShape)
                .blur(4.dp)
        )
    }
}