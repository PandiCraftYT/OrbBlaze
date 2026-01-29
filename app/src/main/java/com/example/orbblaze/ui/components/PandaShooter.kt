package com.example.orbblaze.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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
    isCurrentRainbow: Boolean = false,
    nextBubbleColor: Color,
    isNextRainbow: Boolean = false,
    shotTick: Int,
    rainbowRotation: Float, // ✅ NUEVO: Rotación compartida
    modifier: Modifier = Modifier
) {
    // Animación de respiración (Ligera, no consume mucho)
    val infiniteTransition = rememberInfiniteTransition(label = "panda_idle")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )

    val recoilAnim = remember { Animatable(0f) }
    LaunchedEffect(shotTick) {
        recoilAnim.snapTo(0f)
        recoilAnim.animateTo(1f, tween(80, easing = LinearEasing))
        recoilAnim.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f))
    }
    val recoilOffset = recoilAnim.value * 30f

    Box(modifier = modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.BottomCenter) {

        // --- CAPA 1: BURBUJA "NEXT" ---
        // Usamos VisualBubble optimizada aquí en lugar de dibujar manualmente
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = 80.dp, y = (-100).dp) // Posición flotante aprox
        ) {
            VisualBubble(
                color = nextBubbleColor,
                isRainbow = isNextRainbow,
                rainbowRotation = rainbowRotation, // Pasamos rotación
                modifier = Modifier.size(30.dp)
            )
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
                val furWhite = Color(0xFFFAFAFA); val furShadow = Color(0xFFE0E0E0); val furBlack = Color(0xFF212121)
                val bodyBrush = Brush.radialGradient(listOf(furWhite, furShadow), center = Offset(cx, cy), radius = 100f)

                // DIBUJO PANDA (Cuerpo)
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
                val eyeY = headCy - 10f; val lookX = angle * 0.2f
                drawCircle(Color.White, 8f, Offset(cx - 38f + lookX, eyeY)); drawCircle(Color.Black, 4f, Offset(cx - 38f + lookX, eyeY))
                drawCircle(Color.White, 8f, Offset(cx + 38f + lookX, eyeY)); drawCircle(Color.Black, 4f, Offset(cx + 38f + lookX, eyeY))
                drawCircle(Color.White, 2.5f, Offset(cx - 40f + lookX, eyeY - 2f)); drawCircle(Color.White, 2.5f, Offset(cx + 36f + lookX, eyeY - 2f))
                drawOval(furBlack, topLeft = Offset(cx - 10f, headCy + 15f), size = Size(20f, 12f))
                drawArc(furBlack, 30f, 120f, false, topLeft = Offset(cx - 12f, headCy + 20f), size = Size(24f, 12f), style = Stroke(2.5f))
                drawOval(PandaCheek.copy(alpha=0.5f), topLeft = Offset(cx - 65f, headCy + 20f), size = Size(25f, 15f))
                drawOval(PandaCheek.copy(alpha=0.5f), topLeft = Offset(cx + 40f, headCy + 20f), size = Size(25f, 15f))

                // Brazos
                rotate(angle, pivot) {
                    drawRoundRect(furBlack, topLeft = Offset(cx - 75f, cy - 80f), size = Size(40f, 80f), cornerRadius = CornerRadius(20f))
                    drawRoundRect(furBlack, topLeft = Offset(cx + 35f, cy - 80f), size = Size(40f, 80f), cornerRadius = CornerRadius(20f))
                }
            }
        }

        // --- BURBUJA ACTUAL (SOBRE LOS BRAZOS) ---
        // La dibujamos aparte para usar VisualBubble optimizada y que rote bien
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-110).dp) // Altura en las manos
        ) {
            VisualBubble(
                color = currentBubbleColor,
                isRainbow = isCurrentRainbow,
                rainbowRotation = rainbowRotation, // ✅ Rotación compartida
                modifier = Modifier.size(44.dp)
            )
        }
    }
}