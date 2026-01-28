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
import androidx.compose.ui.unit.dp
import kotlin.math.*
import kotlin.random.Random

// Datos para las partículas (chispas de mecha y humo)
private data class Particle(
    val seed: Int,
    val angleOffset: Float,
    val speed: Float,
    val radius: Float
)

@Composable
fun Shooter(
    angle: Float,
    currentBubbleColor: Color,
    shotTick: Int,
    modifier: Modifier = Modifier
) {
    // 1. CORRECCIÓN DEL ERROR: Usamos FastOutSlowInEasing (Estándar)
    val infinite = rememberInfiniteTransition(label = "shooter_idle")
    val shipSway by infinite.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing), // ✅ Corregido aquí
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )

    // Variables de animación
    val recoil = remember { Animatable(0f) }
    val muzzleFlash = remember { Animatable(0f) }
    val particlesT = remember { Animatable(0f) }

    // Generación de partículas
    val burst = remember(shotTick) {
        List(25) { i ->
            Particle(
                seed = shotTick * 1000 + i,
                angleOffset = (-0.4f + i * (0.8f / 24f)),
                speed = 450f + (i % 5) * 90f,
                radius = 5f + (i % 3) * 4f
            )
        }
    }

    LaunchedEffect(shotTick) {
        recoil.snapTo(0f)
        muzzleFlash.snapTo(0f)
        particlesT.snapTo(0f)

        // Animación de disparo: Retroceso rápido y recuperación elástica
        recoil.animateTo(1f, tween(100, easing = LinearEasing))
        muzzleFlash.snapTo(1f)
        muzzleFlash.animateTo(0f, tween(250, easing = FastOutSlowInEasing))

        recoil.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f))
        particlesT.animateTo(1f, tween(800, easing = LinearOutSlowInEasing))
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(
            modifier = Modifier
                .width(240.dp) // Un poco más ancho para las ruedas grandes
                .height(240.dp)
                .padding(bottom = 20.dp)
        ) {
            val cx = center.x
            val cy = center.y
            val pivotY = cy - 50f // Pivote elevado
            val recoilDist = 35f * recoil.value

            // --- PALETA DE COLORES (Cañón Dorado de la imagen) ---
            // Oro brillante para el cañón
            val goldHigh = Color(0xFFFFECB3) // Brillo
            val goldMid = Color(0xFFFFCA28)  // Oro base
            val goldShadow = Color(0xFFFFA000) // Oro oscuro/Naranja

            // Madera oscura para la base
            val woodDark = Color(0xFF3E2723)
            val woodMid = Color(0xFF5D4037)

            // Efectos
            val fireOrange = Color(0xFFFF5722)
            val smokeGray = Color(0xFFB0BEC5)

            // Gradiente Cilíndrico (Simula metal brillante curvado)
            val goldBarrelBrush = Brush.horizontalGradient(
                colors = listOf(goldShadow, goldMid, goldHigh, goldMid, goldShadow)
            )

            // Gradiente para las ruedas
            val wheelGoldBrush = Brush.radialGradient(
                colors = listOf(goldHigh, goldMid, goldShadow)
            )

            // ==========================================
            // CAPA 1: LA BASE DE MADERA (Cureña curva)
            // ==========================================
            // Dibujamos la "cola" de madera que se ve detrás de la rueda en la imagen
            val carriagePath = Path().apply {
                // Forma curva estilo "S" suave
                moveTo(cx - 70f, pivotY + 20f)
                quadraticBezierTo(cx - 90f, cy + 40f, cx - 100f, cy + 60f) // Cola trasera
                lineTo(cx + 60f, cy + 60f)
                lineTo(cx + 50f, pivotY + 20f)
                close()
            }
            drawPath(carriagePath, Brush.verticalGradient(listOf(woodMid, woodDark)))

            // Sombra bajo el cañón
            drawOval(
                color = Color.Black.copy(alpha = 0.25f),
                topLeft = Offset(cx - 80f, cy + 50f),
                size = Size(160f, 20f)
            )

            // ==========================================
            // CAPA 2: RUEDAS (Grandes, Doradas y con Rayos)
            // ==========================================
            val wheelRadius = 55f
            val wheelCenter = Offset(cx, cy + 10f)

            // 1. Aro exterior dorado
            drawCircle(
                brush = wheelGoldBrush,
                radius = wheelRadius,
                center = wheelCenter,
                style = Stroke(width = 12f)
            )
            // Borde fino para definir
            drawCircle(
                color = goldShadow,
                radius = wheelRadius + 6f,
                center = wheelCenter,
                style = Stroke(width = 2f)
            )

            // 2. Rayos de la rueda (Spokes)
            val spokeCount = 8
            for (i in 0 until spokeCount) {
                val angleRad = (i * (360f / spokeCount)) * (PI / 180f).toFloat()
                val start = wheelCenter
                val end = Offset(
                    wheelCenter.x + cos(angleRad) * wheelRadius,
                    wheelCenter.y + sin(angleRad) * wheelRadius
                )
                // Dibujar rayo de madera
                drawLine(
                    color = woodDark,
                    start = start,
                    end = end,
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }

            // 3. Eje central (Hub)
            drawCircle(
                brush = Brush.radialGradient(listOf(goldHigh, goldShadow)),
                radius = 15f,
                center = wheelCenter
            )


            // ==========================================
            // CAPA 3: EL CAÑÓN DORADO (Barrel)
            // ==========================================
            rotate(degrees = angle, pivot = Offset(cx, pivotY)) {
                val barrelLen = 150f
                val barrelBaseW = 75f
                val barrelTipW = 55f

                // Posición dinámica
                val cannonBaseY = pivotY + recoilDist + shipSway
                val cannonTipY = cannonBaseY - barrelLen

                // 1. Cuerpo del Cañón (Oro brillante)
                val barrelPath = Path().apply {
                    moveTo(cx - barrelBaseW / 2, cannonBaseY)
                    lineTo(cx - barrelTipW / 2, cannonTipY + 25f) // Hasta antes de la boca
                    lineTo(cx + barrelTipW / 2, cannonTipY + 25f)
                    lineTo(cx + barrelBaseW / 2, cannonBaseY)
                    close()
                }
                drawPath(path = barrelPath, brush = goldBarrelBrush)

                // 2. Anillos de Refuerzo (Bands) - Típico del diseño de la imagen
                val bands = listOf(0.1f, 0.3f, 0.6f) // Posiciones relativas
                bands.forEach { t ->
                    val yPos = lerp(cannonBaseY, cannonTipY, t)
                    val wPos = lerp(barrelBaseW, barrelTipW, t) + 6f

                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(goldShadow, goldHigh, goldShadow)),
                        topLeft = Offset(cx - wPos/2, yPos - 6f),
                        size = Size(wPos, 12f),
                        cornerRadius = CornerRadius(4f)
                    )
                }

                // 3. Boca del Cañón (Muzzle Flare)
                val muzzleW = 75f
                val muzzleH = 30f
                drawOval(
                    brush = goldBarrelBrush,
                    topLeft = Offset(cx - muzzleW / 2, cannonTipY),
                    size = Size(muzzleW, muzzleH)
                )
                // Agujero Negro
                drawOval(
                    color = Color.Black,
                    topLeft = Offset(cx - muzzleW / 2 + 12f, cannonTipY + 5f),
                    size = Size(muzzleW - 24f, muzzleH - 10f)
                )

                // 4. Culata Redonda (Cascabel) - La bola dorada de atrás
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(goldHigh, goldShadow),
                        center = Offset(cx - 15f, cannonBaseY + 10f),
                        radius = 25f
                    ),
                    radius = 22f,
                    center = Offset(cx, cannonBaseY + 15f)
                )

                // 5. La Mecha (Fuse)
                // Pequeña línea curva saliendo de la parte trasera
                val fuseStart = Offset(cx + 15f, cannonBaseY - 10f)
                val fuseControl = Offset(cx + 35f, cannonBaseY - 20f)
                val fuseEnd = Offset(cx + 40f, cannonBaseY - 5f)

                val fusePath = Path().apply {
                    moveTo(fuseStart.x, fuseStart.y)
                    quadraticBezierTo(fuseControl.x, fuseControl.y, fuseEnd.x, fuseEnd.y)
                }
                drawPath(fusePath, color = Color.Black, style = Stroke(width = 3f))

                // Chispita en la mecha (siempre viva)
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color.White, fireOrange, Color.Transparent)),
                    radius = 6f + 2f * sin(shotTick.toFloat()), // Parpadeo
                    center = fuseEnd
                )

                // ==========================================
                // CAPA 4: EFECTOS DE DISPARO
                // ==========================================
                val flashVal = muzzleFlash.value
                val muzzleCenter = Offset(cx, cannonTipY + muzzleH/2)

                if (flashVal > 0.01f) {
                    // Fogonazo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, fireOrange, Color.Transparent),
                            radius = 70f * flashVal
                        ),
                        center = muzzleCenter,
                        radius = 90f * flashVal
                    )
                    // Humo denso base
                    drawCircle(
                        color = smokeGray.copy(alpha = 0.6f * flashVal),
                        center = muzzleCenter,
                        radius = 110f * flashVal
                    )
                }

                // Partículas volando
                val t = particlesT.value
                if (t > 0f && t < 1f) {
                    burst.forEach { p ->
                        val spread = 0.8f * (1f - t)
                        val dx = sin(p.angleOffset + (Random.nextFloat()-0.5f)*0.1f * spread)
                        val dy = -cos(p.angleOffset)

                        val px = muzzleCenter.x + dx * p.speed * t * 0.15f
                        val py = muzzleCenter.y + dy * p.speed * t * 0.15f
                        val alpha = (1f - t).pow(2)

                        drawCircle(
                            color = if(Random.nextBoolean()) fireOrange else smokeGray,
                            radius = p.radius * (1f + t),
                            center = Offset(px, py),
                            alpha = alpha
                        )
                    }
                }
            }
        }
    }
}

// Función auxiliar para interpolación lineal
private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}