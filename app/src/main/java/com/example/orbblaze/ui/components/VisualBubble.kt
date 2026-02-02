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
    isMatchingTarget: Boolean = false // Si es true, la burbuja tendrá un indicador fuerte
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jewel_pro_fx")

    // 1. Respiración sutil
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "soft_breath"
    )

    // 2. Luz interna
    val lightTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "light_move"
    )

    // 3. Sparkle
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 5000
                0f at 0
                0f at 3000
                1f at 3200
                0f at 3400
                0f at 5000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle"
    )

    // 4. ANIMACIÓN DEL INDICADOR (Más rápida y visible)
    val indicatorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, // No baja a 0 para que siempre se vea algo
        targetValue = 1.0f,  // Brillo máximo
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing), // Rápido (0.6s)
            repeatMode = RepeatMode.Reverse
        ),
        label = "indicator_pulse"
    )

    val goldDark = Color(0xFFB8860B)
    val goldLight = Color(0xFFFFD700)
    val rainbowColors = remember {
        listOf(
            Color.Red, Color(0xFFFF7F00), Color.Yellow,
            Color.Green, Color.Blue, Color(0xFF4B0082), Color(0xFF8B00FF)
        )
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

                val rad = Math.toRadians(lightTime.toDouble())
                val lightOffsetX = (sin(rad) * radius * 0.1f).toFloat()
                val lightOffsetY = (cos(rad) * radius * 0.1f).toFloat()

                onDrawBehind {
                    // 1. RESPLANDOR EXTERIOR BASE
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.25f), Color.Transparent),
                            center = center,
                            radius = radius * 1.3f
                        )
                    )

                    // 2. ENGARCE ORO
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(goldDark, goldLight, goldDark),
                            center = center
                        ),
                        radius = radius - 1.5f,
                        center = center,
                        style = Stroke(width = 3.5f)
                    )

                    // 3. GEMA
                    if (isRainbow) {
                        rotate(rainbowRotation, center) {
                            drawCircle(
                                brush = Brush.sweepGradient(rainbowColors, center),
                                radius = radius * 0.88f,
                                center = center
                            )
                        }
                    } else {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.0f to color.copy(alpha = 0.95f),
                                    0.6f to color.copy(alpha = 0.8f),
                                    1.0f to color.copy(red = color.red * 0.3f, green = color.green * 0.3f, blue = color.blue * 0.3f)
                                ),
                                center = center.copy(
                                    x = center.x + lightOffsetX,
                                    y = center.y + lightOffsetY - radius * 0.1f
                                ),
                                radius = radius * 1.1f
                            ),
                            radius = radius * 0.88f,
                            center = center
                        )
                    }

                    // 4. REFLEJO
                    drawOval(
                        color = Color.White.copy(alpha = 0.45f),
                        topLeft = Offset(
                            center.x - radius * 0.35f + lightOffsetX * 1.5f,
                            center.y - radius * 0.65f + lightOffsetY * 1.5f
                        ),
                        size = Size(radius * 0.45f, radius * 0.25f)
                    )

                    // 5. DESTELLO (Sparkle)
                    if (isActive && sparkleScale > 0f) {
                        val sSize = radius * 0.4f * sparkleScale
                        val sPos = Offset(center.x + radius * 0.4f, center.y - radius * 0.4f)
                        drawLine(
                            color = Color.White.copy(alpha = sparkleScale),
                            start = sPos.copy(y = sPos.y - sSize),
                            end = sPos.copy(y = sPos.y + sSize),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color.White.copy(alpha = sparkleScale),
                            start = sPos.copy(x = sPos.x - sSize),
                            end = sPos.copy(x = sPos.x + sSize),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // 6. SOMBRA
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f)),
                            center = center,
                            radius = radius * 0.9f
                        ),
                        radius = radius * 0.88f,
                        center = center
                    )

                    // --- NUEVO: INDICADOR VISUAL POTENTE ---
                    // Se dibuja AL FINAL para quedar ENCIMA de todo.
                    if (isMatchingTarget) {
                        // Aro blanco intenso alrededor
                        drawCircle(
                            color = Color.White.copy(alpha = indicatorAlpha),
                            radius = radius * 0.95f, // Casi al borde
                            center = center,
                            style = Stroke(width = 6.dp.toPx()) // Borde grueso
                        )

                        // Resplandor interior adicional
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = indicatorAlpha * 0.5f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = radius * 0.8f
                            )
                        )
                    }
                }
            }
    )
}