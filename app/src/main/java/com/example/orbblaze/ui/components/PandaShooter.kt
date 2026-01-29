package com.example.orbblaze.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.example.orbblaze.ui.theme.*
import kotlin.math.sin

@Composable
fun PandaShooter(
    angle: Float,
    currentBubbleColor: Color,
    isCurrentRainbow: Boolean = false, // ✅ NUEVO
    nextBubbleColor: Color,
    isNextRainbow: Boolean = false,    // ✅ NUEVO
    shotTick: Int,
    modifier: Modifier = Modifier
) {
    // Animaciones
    val infiniteTransition = rememberInfiniteTransition(label = "panda_idle")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )

    // ✅ Animación ROTACIÓN ARCOÍRIS
    val rainbowRot by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "rainbow_rot"
    )

    val recoilAnim = remember { Animatable(0f) }
    LaunchedEffect(shotTick) {
        recoilAnim.snapTo(0f)
        recoilAnim.animateTo(1f, tween(80, easing = LinearEasing))
        recoilAnim.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f))
    }
    val recoilOffset = recoilAnim.value * 30f

    Box(modifier = modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.BottomCenter) {

        // --- CAPA 1: BURBUJA "NEXT" (Flotando al lado) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height
            val standX = cx + 80f
            val standY = cy - 110f
            val bubbleRadius = 22.dp.toPx()

            val floatY = sin(System.currentTimeMillis() / 400.0).toFloat() * 6f
            val center = Offset(standX, standY + floatY)
            val radius = bubbleRadius

            val goldDark = Color(0xFFB8860B)
            val goldLight = Color(0xFFFFD700)
            val gemHighlight = Color.White.copy(alpha = 0.8f)

            // 1. Engarce Oro (COMÚN)
            drawCircle(
                brush = Brush.sweepGradient(listOf(goldDark, goldLight, goldDark, goldLight, goldDark), center = center),
                radius = radius + 2f, center = center, style = Stroke(width = 4f)
            )

            // 2. Gema (CONDICIONAL)
            if (isNextRainbow) {
                // ARCOÍRIS
                val rainbowColors = listOf(Color.Red, Color(0xFFFF7F00), Color.Yellow, Color.Green, Color.Blue, Color(0xFF4B0082), Color(0xFF8B00FF))
                rotate(rainbowRot, center) {
                    drawCircle(brush = Brush.sweepGradient(rainbowColors, center), radius = radius * 0.85f, center = center)
                }
            } else {
                // NORMAL
                val gemBase = nextBubbleColor
                val gemDark = nextBubbleColor.copy(red = nextBubbleColor.red * 0.5f, green = nextBubbleColor.green * 0.5f, blue = nextBubbleColor.blue * 0.5f)
                drawCircle(
                    brush = Brush.radialGradient(listOf(gemBase, gemDark), center = center.copy(y = center.y - radius * 0.2f), radius = radius * 0.9f),
                    radius = radius * 0.85f, center = center
                )
            }

            // 3. Brillos (COMÚN)
            drawOval(color = gemHighlight, topLeft = Offset(center.x - radius * 0.5f, center.y - radius * 0.7f), size = Size(radius * 0.6f, radius * 0.4f))
            drawOval(color = gemHighlight.copy(alpha = 0.4f), topLeft = Offset(center.x + radius * 0.2f, center.y + radius * 0.3f), size = Size(radius * 0.2f, radius * 0.15f))
        }

        // --- CAPA 2: PANDA Y BURBUJA ACTUAL ---
        Canvas(modifier = Modifier.size(240.dp).offset(y = 20.dp)) {
            val cx = center.x
            val cy = center.y
            val pivot = Offset(cx, cy + 60f)

            drawOval(Color.Black.copy(alpha = 0.2f), topLeft = Offset(cx - 70f, cy + 60f), size = Size(140f, 20f))

            withTransform({
                scale(breatheScale, breatheScale, pivot)
                translate(0f, recoilOffset)
                rotate(angle * 0.1f, pivot)
            }) {
                val furWhite = Color(0xFFFAFAFA)
                val furShadow = Color(0xFFE0E0E0)
                val furBlack = Color(0xFF212121)
                val bodyBrush = Brush.radialGradient(listOf(furWhite, furShadow), center = Offset(cx, cy), radius = 100f)

                // DIBUJO DEL PANDA (INTACTO)
                drawOval(furBlack, topLeft = Offset(cx - 75f, cy + 45f), size = Size(55f, 45f))
                drawOval(furBlack, topLeft = Offset(cx + 20f, cy + 45f), size = Size(55f, 45f))
                drawRoundRect(bodyBrush, topLeft = Offset(cx - 80f, cy - 50f), size = Size(160f, 130f), cornerRadius = CornerRadius(70f, 70f))
                drawCircle(Color.White, radius = 50f, center = Offset(cx, cy + 30f))
                val headCy = cy - 70f
                drawCircle(furBlack, radius = 28f, center = Offset(cx - 60f, headCy - 40f))
                drawCircle(furBlack, radius = 28f, center = Offset(cx + 60f, headCy - 40f))
                drawRoundRect(Color.White, topLeft = Offset(cx - 75f, headCy - 65f), size = Size(150f, 120f), cornerRadius = CornerRadius(60f, 60f))
                rotate(-10f, Offset(cx - 35f, headCy)) { drawOval(furBlack, topLeft = Offset(cx - 60f, headCy - 30f), size = Size(45f, 55f)) }
                rotate(10f, Offset(cx + 35f, headCy)) { drawOval(furBlack, topLeft = Offset(cx + 15f, headCy - 30f), size = Size(45f, 55f)) }
                val eyeY = headCy - 10f
                val lookX = angle * 0.2f
                drawCircle(Color.White, 8f, Offset(cx - 38f + lookX, eyeY)); drawCircle(Color.Black, 4f, Offset(cx - 38f + lookX, eyeY))
                drawCircle(Color.White, 8f, Offset(cx + 38f + lookX, eyeY)); drawCircle(Color.Black, 4f, Offset(cx + 38f + lookX, eyeY))
                drawCircle(Color.White, 2.5f, Offset(cx - 40f + lookX, eyeY - 2f)); drawCircle(Color.White, 2.5f, Offset(cx + 36f + lookX, eyeY - 2f))
                drawOval(furBlack, topLeft = Offset(cx - 10f, headCy + 15f), size = Size(20f, 12f))
                drawArc(furBlack, 30f, 120f, false, topLeft = Offset(cx - 12f, headCy + 20f), size = Size(24f, 12f), style = Stroke(2.5f))
                drawOval(PandaCheek.copy(alpha=0.5f), topLeft = Offset(cx - 65f, headCy + 20f), size = Size(25f, 15f))
                drawOval(PandaCheek.copy(alpha=0.5f), topLeft = Offset(cx + 40f, headCy + 20f), size = Size(25f, 15f))

                // --- BRAZOS Y BURBUJA ACTUAL ---
                rotate(angle, pivot) {
                    val bubbleCenter = Offset(cx, cy - 80f)
                    val bubbleRadiusPx = 22.dp.toPx()

                    drawRoundRect(furBlack, topLeft = Offset(cx - 75f, cy - 80f), size = Size(40f, 80f), cornerRadius = CornerRadius(20f))
                    drawRoundRect(furBlack, topLeft = Offset(cx + 35f, cy - 80f), size = Size(40f, 80f), cornerRadius = CornerRadius(20f))

                    // DIBUJO BURBUJA ACTUAL (CON LÓGICA ARCOÍRIS)
                    val radius = bubbleRadiusPx
                    val center = bubbleCenter
                    val goldDarkM = Color(0xFFB8860B)
                    val goldLightM = Color(0xFFFFD700)
                    val gemHighlightM = Color.White.copy(alpha = 0.8f)

                    // 1. Engarce
                    drawCircle(
                        brush = Brush.sweepGradient(listOf(goldDarkM, goldLightM, goldDarkM, goldLightM, goldDarkM), center = center),
                        radius = radius + 2f, center = center, style = Stroke(width = 4f)
                    )

                    // 2. Gema
                    if (isCurrentRainbow) {
                        val rainbowColors = listOf(Color.Red, Color(0xFFFF7F00), Color.Yellow, Color.Green, Color.Blue, Color(0xFF4B0082), Color(0xFF8B00FF))
                        rotate(rainbowRot, center) {
                            drawCircle(brush = Brush.sweepGradient(rainbowColors, center), radius = radius * 0.85f, center = center)
                        }
                    } else {
                        val gemBaseM = currentBubbleColor
                        val gemDarkM = currentBubbleColor.copy(red = currentBubbleColor.red * 0.5f, green = currentBubbleColor.green * 0.5f, blue = currentBubbleColor.blue * 0.5f)
                        drawCircle(
                            brush = Brush.radialGradient(listOf(gemBaseM, gemDarkM), center = center.copy(y = center.y - radius * 0.2f), radius = radius * 0.9f),
                            center = center, radius = radius * 0.85f
                        )
                    }

                    // 3. Brillos
                    drawOval(color = gemHighlightM, topLeft = Offset(center.x - radius * 0.5f, center.y - radius * 0.7f), size = Size(radius * 0.6f, radius * 0.4f))
                    drawOval(color = gemHighlightM.copy(alpha = 0.4f), topLeft = Offset(center.x + radius * 0.2f, center.y + radius * 0.3f), size = Size(radius * 0.2f, radius * 0.15f))

                    // Manos
                    drawCircle(furBlack, 14f, Offset(bubbleCenter.x - 24f, bubbleCenter.y + 15f))
                    drawCircle(furBlack, 14f, Offset(bubbleCenter.x + 24f, bubbleCenter.y + 15f))
                }
            }
        }
    }
}