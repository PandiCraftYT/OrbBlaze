package com.example.orbblaze.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
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
    bubbleColorType: BubbleColor? = null,
    // ✅ Animaciones centralizadas
    breathingScale: Float = 1f,
    lightTime: Float = 0f,
    sparkleScale: Float = 0f,
    indicatorAlpha: Float = 1f
) {
    val goldDark = Color(0xFFC5A059)
    val goldLight = Color(0xFFFFE5B4)

    val rainbowColors = remember {
        listOf(Color.Red, Color(0xFFFF7F00), Color.Yellow, Color.Green, Color.Blue, Color(0xFF4B0082), Color(0xFF8B00FF))
    }

    Spacer(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = breathingScale
                scaleY = breathingScale
            }
            .drawWithCache {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2, size.height / 2)

                val rad = Math.toRadians(lightTime.toDouble())
                val lightOffsetX = (sin(rad) * radius * 0.05f).toFloat()
                val lightOffsetY = (cos(rad) * radius * 0.05f).toFloat()

                onDrawBehind {
                    val bubbleRadius = radius * 0.92f

                    // 1. SOMBRA
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.2f),
                        radius = radius * 0.95f,
                        center = center.copy(y = center.y + 2.dp.toPx())
                    )

                    if (isBomb) {
                        // --- BOMBA ---
                        val fusePath = Path().apply {
                            moveTo(center.x, center.y - bubbleRadius)
                            quadraticTo(
                                center.x + bubbleRadius * 0.3f, center.y - bubbleRadius * 1.3f,
                                center.x + bubbleRadius * 0.5f, center.y - bubbleRadius * 1.4f
                            )
                        }
                        drawPath(path = fusePath, color = Color(0xFF795548), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                        
                        val sparkleAlphaValue = (sin(lightTime * 0.1f) * 0.5f + 0.5f)
                        drawCircle(color = Color(0xFFFF9800).copy(alpha = sparkleAlphaValue), radius = 4.dp.toPx(), center = Offset(center.x + bubbleRadius * 0.5f, center.y - bubbleRadius * 1.4f))

                        drawCircle(
                            brush = Brush.radialGradient(colors = listOf(Color(0xFF424242), Color.Black), center = center.copy(x = center.x - radius * 0.2f, y = center.y - radius * 0.2f), radius = bubbleRadius * 1.2f),
                            radius = bubbleRadius, center = center
                        )
                    } else {
                        // --- BURBUJA NORMAL ---
                        drawCircle(
                            brush = Brush.linearGradient(colors = listOf(goldLight, goldDark), start = Offset(center.x - radius, center.y - radius), end = Offset(center.x + radius, center.y + radius)),
                            radius = radius, center = center
                        )

                        if (isRainbow) {
                            rotate(rainbowRotation, center) {
                                drawCircle(brush = Brush.sweepGradient(rainbowColors, center), radius = bubbleRadius, center = center)
                            }
                        } else {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(color.copy(alpha = 0.9f).compositeOver(Color.White), color, color.copy(red=color.red*0.4f, green=color.green*0.4f, blue=color.blue*0.4f)),
                                    center = center.copy(x = center.x - radius * 0.2f, y = center.y - radius * 0.2f),
                                    radius = bubbleRadius * 1.3f
                                ),
                                radius = bubbleRadius, center = center
                            )
                        }
                    }

                    // --- MEJORA DALTONISMO: ICONOS DE ALTA VISIBILIDAD ---
                    if (isColorBlindMode && !isBomb && !isRainbow && bubbleColorType != null) {
                        drawEnhancedColorBlindIcon(bubbleColorType, center, bubbleRadius * 0.40f)
                    }

                    // BRILLO
                    rotate(-45f, center) {
                        drawOval(
                            brush = Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.05f)), start = Offset(center.x, center.y - bubbleRadius), end = Offset(center.x, center.y)),
                            topLeft = Offset(center.x - bubbleRadius * 0.5f, center.y - bubbleRadius * 0.85f),
                            size = Size(bubbleRadius, bubbleRadius * 0.5f)
                        )
                    }
                    drawCircle(color = Color.White.copy(alpha = 0.7f), radius = bubbleRadius * 0.08f, center = Offset(center.x - bubbleRadius * 0.35f + lightOffsetX, center.y - bubbleRadius * 0.35f + lightOffsetY))

                    if (isMatchingTarget) {
                        drawCircle(color = Color.White.copy(alpha = indicatorAlpha), radius = radius, center = center, style = Stroke(width = 3.dp.toPx()))
                    }
                }
            }
    )
}

private fun DrawScope.drawEnhancedColorBlindIcon(type: BubbleColor, center: Offset, size: Float) {
    val mainColor = Color.White.copy(alpha = 0.9f)
    val shadowColor = Color.Black.copy(alpha = 0.3f)
    val strokeWidth = 2.dp.toPx()

    fun drawForm(path: Path) {
        drawPath(path, shadowColor, style = Fill)
        drawPath(path, mainColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    when (type) {
        BubbleColor.RED -> {
            drawCircle(shadowColor, radius = size, center = center, style = Fill)
            drawCircle(mainColor, radius = size, center = center, style = Stroke(strokeWidth))
        }
        BubbleColor.BLUE -> {
            val rect = Rect(center.x - size, center.y - size, center.x + size, center.y + size)
            drawRect(shadowColor, topLeft = rect.topLeft, size = rect.size, style = Fill)
            drawRect(mainColor, topLeft = rect.topLeft, size = rect.size, style = Stroke(strokeWidth))
        }
        BubbleColor.GREEN -> {
            val path = Path().apply {
                moveTo(center.x, center.y - size)
                lineTo(center.x + size, center.y + size * 0.8f)
                lineTo(center.x - size, center.y + size * 0.8f)
                close()
            }
            drawForm(path)
        }
        BubbleColor.YELLOW -> {
            val path = Path().apply {
                moveTo(center.x, center.y - size)
                lineTo(center.x + size, center.y)
                lineTo(center.x, center.y + size)
                lineTo(center.x - size, center.y)
                close()
            }
            drawForm(path)
        }
        BubbleColor.PURPLE -> {
            val path = Path().apply {
                val s = size * 0.8f
                moveTo(center.x - s, center.y - s)
                lineTo(center.x + s, center.y + s)
                moveTo(center.x + s, center.y - s)
                lineTo(center.x - s, center.y + s)
            }
            drawPath(path, shadowColor, style = Stroke(strokeWidth + 2f, cap = StrokeCap.Round))
            drawPath(path, mainColor, style = Stroke(strokeWidth, cap = StrokeCap.Round))
        }
        BubbleColor.CYAN -> {
            val path = Path().apply {
                for (i in 0..5) {
                    val angle = i * Math.PI / 3 - Math.PI / 6
                    val x = center.x + size * cos(angle).toFloat()
                    val y = center.y + size * sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawForm(path)
        }
        else -> {}
    }
}
