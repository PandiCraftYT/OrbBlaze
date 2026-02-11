package com.example.orbblaze.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VisualBubble(
    color: Color,
    modifier: Modifier = Modifier,
    isRainbow: Boolean = false,
    rainbowRotation: Float = 0f,
    isActive: Boolean = false,
    isMatchingTarget: Boolean = false
) {
    // --- LÓGICA DE ANIMACIÓN (INTACTA) ---
    val infiniteTransition = rememberInfiniteTransition(label = "jewel_pro_fx")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.985f, targetValue = 1.015f,
        animationSpec = infiniteRepeatable(animation = tween(2500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "soft_breath"
    )

    val lightTime by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "light_move"
    )

    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 5000; 0f at 0; 0f at 3000; 1f at 3200; 0f at 3400; 0f at 5000 },
            repeatMode = RepeatMode.Restart
        ), label = "sparkle"
    )

    val indicatorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(600, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "indicator_pulse"
    )

    // Colores del borde (Oro sutil)
    val goldDark = Color(0xFFC5A059) // Oro mate
    val goldLight = Color(0xFFFFE5B4) // Champagne

    val rainbowColors = remember {
        listOf(Color.Red, Color(0xFFFF7F00), Color.Yellow, Color.Green, Color.Blue, Color(0xFF4B0082), Color(0xFF8B00FF))
    }

    Spacer(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawWithCache {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2, size.height / 2)

                // Movimiento ligero del brillo (lógica original)
                val rad = Math.toRadians(lightTime.toDouble())
                val lightOffsetX = (sin(rad) * radius * 0.05f).toFloat()
                val lightOffsetY = (cos(rad) * radius * 0.05f).toFloat()

                onDrawBehind {
                    // Radio un poco más pequeño para dejar espacio al borde
                    val bubbleRadius = radius * 0.92f

                    // --- 1. SOMBRA SUAVE (Profundidad) ---
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.2f),
                        radius = radius * 0.95f,
                        center = center.copy(y = center.y + 2.dp.toPx())
                    )

                    // --- 2. BORDE (Anillo fino y elegante) ---
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(goldLight, goldDark),
                            start = Offset(center.x - radius, center.y - radius),
                            end = Offset(center.x + radius, center.y + radius)
                        ),
                        radius = radius,
                        center = center
                    )

                    // --- 3. RELLENO DE COLOR (Vibrante) ---
                    if (isRainbow) {
                        rotate(rainbowRotation, center) {
                            drawCircle(
                                brush = Brush.sweepGradient(rainbowColors, center),
                                radius = bubbleRadius, center = center
                            )
                        }
                    } else {
                        // Gradiente Radial Offset: Simula luz 3D pero limpia
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.9f).compositeOver(Color.White), // Centro luminoso
                                    color, // Color puro
                                    color.copy(red=color.red*0.4f, green=color.green*0.4f, blue=color.blue*0.4f) // Sombra borde
                                ),
                                center = center.copy(x = center.x - radius * 0.2f, y = center.y - radius * 0.2f),
                                radius = bubbleRadius * 1.3f
                            ),
                            radius = bubbleRadius,
                            center = center
                        )
                    }

                    // --- 4. BRILLO "WET LOOK" (Efecto cristal húmedo) ---
                    // Este es el secreto de las burbujas modernas: un brillo blanco ovalado y suave
                    rotate(-45f, center) {
                        drawOval(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.05f)),
                                start = Offset(center.x, center.y - bubbleRadius),
                                end = Offset(center.x, center.y)
                            ),
                            topLeft = Offset(center.x - bubbleRadius * 0.5f, center.y - bubbleRadius * 0.85f),
                            size = Size(bubbleRadius, bubbleRadius * 0.5f)
                        )
                    }

                    // --- 5. REFLEJO INFERIOR (Rim Light sutil) ---
                    // Da volumen sin ensuciar el diseño
                    drawArc(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.35f)),
                            startY = center.y, endY = center.y + bubbleRadius
                        ),
                        startAngle = 45f, sweepAngle = 90f, useCenter = false,
                        topLeft = Offset(center.x - bubbleRadius * 0.8f, center.y - bubbleRadius * 0.8f),
                        size = Size(bubbleRadius * 1.6f, bubbleRadius * 1.6f),
                        style = Stroke(width = bubbleRadius * 0.08f, cap = StrokeCap.Round)
                    )

                    // Pequeño punto de luz (Hotspot) - Movible
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = bubbleRadius * 0.1f,
                        center = Offset(center.x - bubbleRadius * 0.35f + lightOffsetX, center.y - bubbleRadius * 0.35f + lightOffsetY)
                    )

                    // --- 6. DESTELLO (Sparkle) ---
                    if (isActive && sparkleScale > 0f) {
                        val sPos = Offset(center.x + radius * 0.35f, center.y - radius * 0.35f)
                        val sSize = radius * 0.6f * sparkleScale

                        drawLine(
                            brush = Brush.radialGradient(listOf(Color.White, Color.Transparent)),
                            start = sPos.copy(y = sPos.y - sSize), end = sPos.copy(y = sPos.y + sSize),
                            strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round
                        )
                        drawLine(
                            brush = Brush.radialGradient(listOf(Color.White, Color.Transparent)),
                            start = sPos.copy(x = sPos.x - sSize), end = sPos.copy(x = sPos.x + sSize),
                            strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round
                        )
                        drawCircle(Color.White, radius = radius * 0.15f * sparkleScale, center = sPos)
                    }

                    // --- 7. INDICADOR DE OBJETIVO ---
                    if (isMatchingTarget) {
                        drawCircle(
                            color = Color.White.copy(alpha = indicatorAlpha),
                            radius = radius, // Coincide con el borde exterior
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            }
    )
}