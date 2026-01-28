package com.example.orbblaze.ui.game

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orbblaze.domain.model.BubbleColor
import com.example.orbblaze.ui.components.VisualBubble
import com.example.orbblaze.ui.components.Shooter
import com.example.orbblaze.ui.theme.*
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LevelScreen(viewModel: GameViewModel = viewModel()) {
    val bubbles = viewModel.bubblesByPosition
    val activeProjectile = viewModel.activeProjectile

    // --- MÉTRICAS ---
    val density = LocalDensity.current
    val bubbleSize = 44.dp
    val bubbleDiameterPx = with(density) { bubbleSize.toPx() }
    val bubbleRadiusPx = bubbleDiameterPx / 2f
    val horizontalSpacing = 40.dp
    val verticalSpacing = 36.dp
    val boardTopPaddingPx = with(density) { 60.dp.toPx() }
    val boardStartPaddingPx = with(density) { (8.dp + 16.dp).toPx() }
    val ceilingYPx = boardTopPaddingPx + bubbleDiameterPx * 0.2f

    viewModel.setBoardMetrics(
        BoardMetricsPx(
            bubbleDiameter = bubbleDiameterPx,
            horizontalSpacing = with(density) { horizontalSpacing.toPx() },
            verticalSpacing = with(density) { verticalSpacing.toPx() },
            boardTopPadding = boardTopPaddingPx,
            boardStartPadding = boardStartPaddingPx,
            ceilingY = ceilingYPx
        )
    )

    // Animación sutil para la malla de fondo (pulsación de luz)
    val infiniteTransition = rememberInfiniteTransition(label = "mesh_anim")
    val gridAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.6f,
        // CORREGIDO: Usamos FastOutSlowInEasing en lugar de SineOutSlowInEasing
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gridAlpha"
    )

    // Colores del tema "Modern Pirate Mesh"
    val deepSeaTop = Color(0xFF0D1B2A)   // Azul marino muy oscuro
    val deepSeaBottom = Color(0xFF1B263B) // Un poco más claro abajo
    val meshGold = Color(0xFFFFD700).copy(alpha = gridAlpha) // Oro sutil para la malla
    val meshAccent = Color(0xFF00E5FF).copy(alpha = 0.2f) // Un toque cyan tecnológico

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(deepSeaTop)
            .pointerInput(Unit) {
                detectDragGestures { change, _ -> viewModel.updateAngle(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat()) }
            }
            .pointerInput(Unit) {
                detectTapGestures { viewModel.onShoot(size.width.toFloat(), size.height.toFloat()) }
            }
    ) {
        val w = maxWidth.value * density.density
        val h = maxHeight.value * density.density

        // =========================================================
        // CAPA 1: FONDO - MALLA DE NAVEGACIÓN MINIMALISTA
        // =========================================================
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Fondo degradado oceánico profundo
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(deepSeaTop, deepSeaBottom)
                )
            )

            // 2. La Malla (Grid)
            val gridSize = 100f
            val cols = (w / gridSize).toInt() + 1
            val rows = (h / gridSize).toInt() + 1

            // Líneas de la cuadrícula
            for (i in 0..cols) {
                drawLine(meshGold, Offset(i * gridSize, 0f), Offset(i * gridSize, h), strokeWidth = 1f)
            }
            for (i in 0..rows) {
                drawLine(meshGold, Offset(0f, i * gridSize), Offset(w, i * gridSize), strokeWidth = 1f)
            }

            // 3. Acentos Piratas Minimalistas en las intersecciones
            for (i in 0..cols) {
                for (j in 0..rows) {
                    val cx = i * gridSize
                    val cy = j * gridSize
                    // Pequeña estrella/brújula en las intersecciones
                    drawCircle(meshGold, radius = 4f, center = Offset(cx, cy))
                    // Líneas de mira sutiles
                    drawLine(meshAccent, Offset(cx - 10f, cy), Offset(cx + 10f, cy), strokeWidth = 1f)
                    drawLine(meshAccent, Offset(cx, cy - 10f), Offset(cx, cy + 10f), strokeWidth = 1f)
                }
            }

            // 4. Viñeta tecnológica (Oscurecer bordes)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                    center = Offset(w / 2, h / 2),
                    radius = hypot(w, h) * 0.7f
                )
            )
        }

        // =========================================================
        // CAPA 2: ELEMENTOS DE JUEGO (Guía, Burbujas, Proyectil)
        // =========================================================
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseHeightPx = with(density) { 160.dp.toPx() } // Altura del footer
            val start = Offset(size.width / 2f, size.height - baseHeightPx + 50f)
            val angleRad = Math.toRadians(viewModel.shooterAngle.toDouble())
            var dirX = sin(angleRad).toFloat(); var dirY = -cos(angleRad).toFloat()
            val dlen = hypot(dirX, dirY).coerceAtLeast(0.0001f); dirX /= dlen; dirY /= dlen
            val leftWall = bubbleRadiusPx; val rightWall = size.width - bubbleRadiusPx
            val guideLength = size.height * 0.7f; var remaining = guideLength
            var current = start; var bounced = false

            // Guía de color oro para combinar con la malla
            val guideColor = meshGold.copy(alpha = 0.6f)
            val stroke = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f), cap = StrokeCap.Round)

            while (remaining > 0f) {
                val tToWall = when { dirX > 0f -> (rightWall - current.x) / dirX; dirX < 0f -> (leftWall - current.x) / dirX; else -> Float.POSITIVE_INFINITY }
                val distToWall = tToWall
                if (distToWall.isInfinite() || distToWall.isNaN() || distToWall >= remaining || bounced) {
                    val end = Offset(x = current.x + dirX * remaining, y = current.y + dirY * remaining)
                    drawLine(guideColor, current, end, strokeWidth = 4f, pathEffect = stroke.pathEffect, cap = stroke.cap)
                    drawCircle(guideColor, 8f, end); break
                }
                val hitPoint = Offset(x = current.x + dirX * distToWall, y = current.y + dirY * distToWall)
                drawLine(guideColor, current, hitPoint, strokeWidth = 4f, pathEffect = stroke.pathEffect, cap = stroke.cap)
                remaining -= distToWall; dirX *= -1f; current = hitPoint; bounced = true
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(top = 60.dp, start = 8.dp, end = 8.dp)) {
            bubbles.entries.forEach { (pos, bubble) ->
                val xOffset = if (pos.row % 2 != 0) (bubbleSize / 2) else 0.dp
                val xPos = (pos.col * horizontalSpacing.value).dp + xOffset + 16.dp
                val yPos = (pos.row * verticalSpacing.value).dp
                VisualBubble(color = mapBubbleColor(bubble.color), modifier = Modifier.offset(x = xPos, y = yPos))
            }
        }

        activeProjectile?.let { projectile ->
            VisualBubble(
                color = mapBubbleColor(projectile.color),
                modifier = Modifier.offset(
                    x = with(density) { projectile.x.toDp() } - (bubbleSize / 2),
                    y = with(density) { projectile.y.toDp() } - (bubbleSize / 2)
                )
            )
        }

        // =========================================================
        // CAPA 3: FOOTER - CONSOLA DE MANDO MODERNA
        // =========================================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val footerDark = Color(0xFF121212)
                val footerMetal = Color(0xFF263238)
                val accentLine = meshGold.copy(alpha = 0.8f)

                // Forma curva moderna de la consola
                val path = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(0f, 40f)
                    quadraticBezierTo(size.width * 0.2f, 0f, size.width * 0.5f, 0f)
                    quadraticBezierTo(size.width * 0.8f, 0f, size.width, 40f)
                    lineTo(size.width, size.height)
                    close()
                }

                // Cuerpo principal (Metal oscuro y madera pulida)
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(listOf(footerMetal, footerDark))
                )

                // Borde luminoso superior (El toque moderno)
                drawPath(
                    path = path,
                    color = accentLine,
                    style = Stroke(width = 4f)
                )

                // Detalles tecnológicos minimalistas
                val lightY = 30f
                // Luces de navegación
                drawCircle(Color.Red.copy(alpha = 0.7f), radius = 6f, center = Offset(size.width * 0.1f, lightY + 20f))
                drawCircle(Color.Green.copy(alpha = 0.7f), radius = 6f, center = Offset(size.width * 0.9f, lightY + 20f))

                // Líneas de acento en la base
                drawLine(accentLine.copy(alpha=0.3f), Offset(0f, size.height-20f), Offset(size.width, size.height-20f), strokeWidth = 2f)
            }

            // El cañón se asienta sobre esta consola moderna
            Shooter(
                angle = viewModel.shooterAngle,
                currentBubbleColor = mapBubbleColor(viewModel.nextBubbleColor),
                shotTick = viewModel.shotTick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-30).dp)
            )
        }
    }
}

// Función helper para colores (al final del archivo)
fun mapBubbleColor(type: BubbleColor): Color = when (type) {
    BubbleColor.RED -> BubbleRed
    BubbleColor.BLUE -> BubbleBlue
    BubbleColor.GREEN -> BubbleGreen
    BubbleColor.PURPLE -> BubblePurple
    BubbleColor.YELLOW -> BubbleYellow
    BubbleColor.CYAN -> BubbleCyan
}