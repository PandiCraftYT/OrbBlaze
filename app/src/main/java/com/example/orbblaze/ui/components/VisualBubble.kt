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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.orbblaze.domain.model.BubbleColor
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VisualBubble(
    color: Color,
    modifier: Modifier = Modifier,
    isRainbow: Boolean = false,
    isBomb: Boolean = false,
    rainbowRotation: Float = 0f,
    isActive: Boolean = false,
    isMatchingTarget: Boolean = false,
    isColorBlindMode: Boolean = false,
    bubbleColorType: BubbleColor? = null
) {
    // --- LÓGICA DE ANIMACIÓN ---
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

    // Colores del borde
    val goldDark = Color(0xFFC5A059)
    val goldLight = Color(0xFFFFE5B4)

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

                val rad = Math.toRadians(lightTime.toDouble())
                val lightOffsetX = (sin(rad) * radius * 0.05f).toFloat()
                val lightOffsetY = (cos(rad) * radius * 0.05f).toFloat()

                onDrawBehind {
                    val bubbleRadius = radius * 0.92f

                    // --- 1. SOMBRA SUAVE ---
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.2f),
                        radius = radius * 0.95f,
                        center = center.copy(y = center.y + 2.dp.toPx())
                    )

                    if (isBomb) {
                        // --- DISEÑO DE BOMBA ---
                        val fusePath = Path().apply {
                            moveTo(center.x, center.y - bubbleRadius)
                            quadraticTo(
                                center.x + bubbleRadius * 0.3f, center.y - bubbleRadius * 1.3f,
                                center.x + bubbleRadius * 0.5f, center.y - bubbleRadius * 1.4f
                            )
                        }
                        drawPath(
                            path = fusePath,
                            color = Color(0xFF795548),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        val sparkleAlpha = (sin(lightTime * 0.1f) * 0.5f + 0.5f)
                        drawCircle(
                            color = Color(0xFFFF9800).copy(alpha = sparkleAlpha),
                            radius = 4.dp.toPx(),
                            center = Offset(center.x + bubbleRadius * 0.5f, center.y - bubbleRadius * 1.4f)
                        )
                        drawCircle(
                            color = Color.Yellow.copy(alpha = sparkleAlpha),
                            radius = 2.dp.toPx(),
                            center = Offset(center.x + bubbleRadius * 0.5f, center.y - bubbleRadius * 1.4f)
                        )

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF424242), Color.Black),
                                center = center.copy(x = center.x - radius * 0.2f, y = center.y - radius * 0.2f),
                                radius = bubbleRadius * 1.2f
                            ),
                            radius = bubbleRadius,
                            center = center
                        )
                        
                        drawCircle(
                            color = Color.White.copy(alpha = 0.1f),
                            radius = bubbleRadius * 0.4f,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    } else {
                        // --- DISEÑO DE BURBUJA NORMAL ---
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(goldLight, goldDark),
                                start = Offset(center.x - radius, center.y - radius),
                                end = Offset(center.x + radius, center.y + radius)
                            ),
                            radius = radius,
                            center = center
                        )

                        if (isRainbow) {
                            rotate(rainbowRotation, center) {
                                drawCircle(
                                    brush = Brush.sweepGradient(rainbowColors, center),
                                    radius = bubbleRadius, center = center
                                )
                            }
                        } else {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        color.copy(alpha = 0.9f).compositeOver(Color.White),
                                        color,
                                        color.copy(red=color.red*0.4f, green=color.green*0.4f, blue=color.blue*0.4f)
                                    ),
                                    center = center.copy(x = center.x - radius * 0.2f, y = center.y - radius * 0.2f),
                                    radius = bubbleRadius * 1.3f
                                ),
                                radius = bubbleRadius,
                                center = center
                            )
                        }
                    }

                    // --- MODO DALTONISMO: FIGURAS ---
                    if (isColorBlindMode && !isBomb && !isRainbow && bubbleColorType != null) {
                        drawColorBlindIcon(bubbleColorType, center, bubbleRadius * 0.45f)
                    }

                    // --- BRILLO COMÚN ---
                    rotate(-45f, center) {
                        drawOval(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.05f)),
                                start = Offset(center.x, center.y - bubbleRadius),
                                end = Offset(center.x, center.y)
                            ),
                            topLeft = Offset(center.x - bubbleRadius * 0.5f, center.y - bubbleRadius * 0.85f),
                            size = Size(bubbleRadius, bubbleRadius * 0.5f)
                        )
                    }

                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = bubbleRadius * 0.08f,
                        center = Offset(center.x - bubbleRadius * 0.35f + lightOffsetX, center.y - bubbleRadius * 0.35f + lightOffsetY)
                    )

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
                    }

                    if (isMatchingTarget) {
                        drawCircle(
                            color = Color.White.copy(alpha = indicatorAlpha),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            }
    )
}

private fun DrawScope.drawColorBlindIcon(type: BubbleColor, center: Offset, size: Float) {
    val iconColor = Color.White.copy(alpha = 0.8f)
    val strokeWidth = 2.5.dp.toPx()
    
    when (type) {
        BubbleColor.RED -> {
            drawCircle(iconColor, radius = size, center = center, style = Stroke(strokeWidth))
        }
        BubbleColor.BLUE -> {
            drawRect(
                iconColor, 
                topLeft = Offset(center.x - size, center.y - size), 
                size = Size(size * 2, size * 2), 
                style = Stroke(strokeWidth)
            )
        }
        BubbleColor.GREEN -> {
            val path = Path().apply {
                moveTo(center.x, center.y - size)
                lineTo(center.x + size, center.y + size)
                lineTo(center.x - size, center.y + size)
                close()
            }
            drawPath(path, iconColor, style = Stroke(strokeWidth))
        }
        BubbleColor.YELLOW -> {
            val path = Path().apply {
                moveTo(center.x, center.y - size)
                lineTo(center.x + size, center.y)
                lineTo(center.x, center.y + size)
                lineTo(center.x - size, center.y)
                close()
            }
            drawPath(path, iconColor, style = Stroke(strokeWidth))
        }
        BubbleColor.PURPLE -> {
            drawLine(iconColor, Offset(center.x - size, center.y), Offset(center.x + size, center.y), strokeWidth, StrokeCap.Round)
            drawLine(iconColor, Offset(center.x, center.y - size), Offset(center.x, center.y + size), strokeWidth, StrokeCap.Round)
        }
        BubbleColor.CYAN -> {
            val path = Path().apply {
                for (i in 0..5) {
                    val angle = i * Math.PI / 3
                    val x = center.x + size * cos(angle).toFloat()
                    val y = center.y + size * sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(path, iconColor, style = Stroke(strokeWidth))
        }
        else -> {}
    }
}
