package com.example.orbblaze.ui.game

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orbblaze.domain.model.BubbleColor
import com.example.orbblaze.ui.components.VisualBubble
import com.example.orbblaze.ui.components.PandaShooter
import com.example.orbblaze.ui.theme.*
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LevelScreen(
    viewModel: GameViewModel = viewModel(),
    soundManager: SoundManager, // ✅ AHORA RECIBE EL MANAGER GLOBAL
    onMenuClick: () -> Unit = {}
) {
    val bubbles = viewModel.bubblesByPosition
    val activeProjectile = viewModel.activeProjectile
    val score = viewModel.score
    val highScore = viewModel.highScore
    val gameState = viewModel.gameState
    val particles = viewModel.particles
    val currentBubbleColor = viewModel.currentBubbleColor
    val previewBubbleColor = viewModel.previewBubbleColor
    val soundEvent = viewModel.soundEvent
    val vibrationEvent = viewModel.vibrationEvent
    val isPaused = viewModel.isPaused

    val haptic = LocalHapticFeedback.current
    var volumeSlider by remember { mutableStateOf(viewModel.getSfxVolume()) }

    // ✅ NUEVO: Control inteligente de música según el estado del juego
    LaunchedEffect(gameState) {
        if (gameState != GameState.PLAYING) {
            // Si Ganaste o Perdiste -> PAUSA MÚSICA para escuchar el SFX
            soundManager.pauseMusic()
        } else {
            // Si estás Jugando -> ASEGURA QUE SUENE LA MÚSICA
            soundManager.startMusic()
        }
    }

    LaunchedEffect(soundEvent) {
        soundEvent?.let {
            soundManager.play(it)
            viewModel.clearSoundEvent()
        }
    }

    LaunchedEffect(vibrationEvent) {
        if (vibrationEvent) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.clearVibrationEvent()
        }
    }

    val density = LocalDensity.current
    val bubbleSize = 44.dp
    val bubbleDiameterPx = with(density) { bubbleSize.toPx() }
    val bubbleRadiusPx = bubbleDiameterPx / 2f
    val horizontalSpacing = 40.dp
    val verticalSpacing = 36.dp
    val boardTopPaddingPx = with(density) { 80.dp.toPx() }
    val boardStartPaddingPx = with(density) { (8.dp + 16.dp).toPx() }
    val ceilingYPx = boardTopPaddingPx + bubbleDiameterPx * 0.2f

    viewModel.setBoardMetrics(BoardMetricsPx(bubbleDiameterPx, with(density) { horizontalSpacing.toPx() }, with(density) { verticalSpacing.toPx() }, boardTopPaddingPx, boardStartPaddingPx, ceilingYPx))

    val backgroundBrush = Brush.verticalGradient(colors = listOf(BgTop, BgBottom))

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .systemBarsPadding()
            .pointerInput(Unit) {
                detectDragGestures { change: PointerInputChange, _ ->
                    viewModel.updateAngle(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat())
                }
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    viewModel.onShoot(size.width.toFloat(), size.height.toFloat())
                }
            }
    ) {
        // 1. CANVAS
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseHeightPx = with(density) { 150.dp.toPx() }
            val start = Offset(size.width / 2f, size.height - baseHeightPx)
            val angleRad = Math.toRadians(viewModel.shooterAngle.toDouble())
            var dirX = sin(angleRad).toFloat(); var dirY = -cos(angleRad).toFloat()
            val dlen = hypot(dirX, dirY).coerceAtLeast(0.0001f); dirX /= dlen; dirY /= dlen
            val leftWall = bubbleRadiusPx; val rightWall = size.width - bubbleRadiusPx
            val guideLength = size.height * 0.7f; var remaining = guideLength
            var current = start; var bounced = false
            val guideColor = Color.White.copy(alpha = 0.6f)
            val stroke = Stroke(width = 5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 20f), 0f), cap = StrokeCap.Round)

            val dangerY = boardTopPaddingPx + (verticalSpacing.toPx() * 12)
            drawLine(color = Color.Red.copy(alpha = 0.3f), start = Offset(0f, dangerY), end = Offset(size.width, dangerY), strokeWidth = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f)))

            while (remaining > 0f) {
                val tToWall = when { dirX > 0f -> (rightWall - current.x) / dirX; dirX < 0f -> (leftWall - current.x) / dirX; else -> Float.POSITIVE_INFINITY }
                val distToWall = tToWall
                if (distToWall.isInfinite() || distToWall.isNaN() || distToWall >= remaining || bounced) {
                    val end = Offset(x = current.x + dirX * remaining, y = current.y + dirY * remaining)
                    drawLine(guideColor, current, end, strokeWidth = 5f, pathEffect = stroke.pathEffect, cap = stroke.cap)
                    drawCircle(guideColor, 8f, end); break
                }
                val hitPoint = Offset(x = current.x + dirX * distToWall, y = current.y + dirY * distToWall)
                drawLine(guideColor, current, hitPoint, strokeWidth = 5f, pathEffect = stroke.pathEffect, cap = stroke.cap)
                remaining -= distToWall; dirX *= -1f; current = hitPoint; bounced = true
            }

            particles.forEach { p -> drawCircle(color = mapBubbleColor(p.color).copy(alpha = p.life), radius = p.size, center = Offset(p.x, p.y)) }
        }

        // 2. TABLERO
        Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp, start = 8.dp, end = 8.dp)) {
            bubbles.entries.forEach { (pos, bubble) ->
                val xOffset = if (pos.row % 2 != 0) (bubbleSize / 2) else 0.dp
                val xPos = (pos.col * horizontalSpacing.value).dp + xOffset + 16.dp
                val yPos = (pos.row * verticalSpacing.value).dp
                VisualBubble(color = mapBubbleColor(bubble.color), modifier = Modifier.offset(x = xPos, y = yPos))
            }
        }

        // 3. PROYECTIL
        activeProjectile?.let { projectile ->
            VisualBubble(color = mapBubbleColor(projectile.color), modifier = Modifier.offset(x = with(density) { projectile.x.toDp() } - (bubbleSize / 2), y = with(density) { projectile.y.toDp() } - (bubbleSize / 2)))
        }

        // 4. PANDA
        PandaShooter(
            angle = viewModel.shooterAngle, currentBubbleColor = mapBubbleColor(currentBubbleColor), nextBubbleColor = mapBubbleColor(previewBubbleColor), shotTick = viewModel.shotTick,
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-10).dp).pointerInput(Unit) { detectTapGestures { viewModel.swapBubbles() } }
        )

        // BARRA SUPERIOR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Box {
                    Text(text = "SCORE: $score", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black.copy(alpha = 0.5f), letterSpacing = 2.sp), modifier = Modifier.offset(2.dp, 2.dp))
                    Text(text = "SCORE: $score", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD700), letterSpacing = 2.sp, shadow = Shadow(color = Color.Black, blurRadius = 4f)))
                }
                Text(text = "BEST: $highScore", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), letterSpacing = 1.sp))
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable { viewModel.togglePause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configuración",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // OVERLAY PAUSA
        if (isPaused && gameState == GameState.PLAYING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .pointerInput(Unit) { detectTapGestures { } },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PAUSA",
                        style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD700), shadow = Shadow(color = Color.Black, blurRadius = 10f))
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF64FFDA)).clickable { viewModel.togglePause() }.padding(horizontal = 40.dp, vertical = 16.dp)) {
                        Text(text = "CONTINUAR", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E)))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White).clickable { viewModel.restartGame() }.padding(horizontal = 40.dp, vertical = 16.dp)) {
                        Text(text = "REINICIAR", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("VOLUMEN EFECTOS", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold))
                    Slider(
                        value = volumeSlider,
                        onValueChange = {
                            volumeSlider = it
                            viewModel.setSfxVolume(it)
                            soundManager.refreshSettings()
                        },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color.White),
                        modifier = Modifier.width(200.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Transparent).border(2.dp, Color.White, RoundedCornerShape(50))
                            .clickable {
                                soundManager.startMusic() // Reactivar música al salir
                                viewModel.restartGame()
                                onMenuClick()
                            }
                            .padding(horizontal = 40.dp, vertical = 12.dp)
                    ) {
                        Text(text = "SALIR", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White))
                    }
                }
            }
        }

        // OVERLAY GAME OVER / WIN
        if (gameState != GameState.PLAYING) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val titleText = if (gameState == GameState.WON) "¡VICTORIA!" else "GAME OVER"
                    val titleColor = if (gameState == GameState.WON) Color(0xFFFFD700) else Color(0xFFFF4D4D)
                    Text(text = titleText, style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Black, color = titleColor, shadow = Shadow(color = Color.Black, blurRadius = 10f)))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Puntuación Final: $score", style = TextStyle(fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold))
                    Text(text = "Mejor Récord: $highScore", style = TextStyle(fontSize = 18.sp, color = Color.Gray, fontWeight = FontWeight.Normal))
                    Spacer(modifier = Modifier.height(32.dp))

                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White).border(4.dp, titleColor, RoundedCornerShape(50))
                        .clickable { viewModel.restartGame() }.padding(horizontal = 32.dp, vertical = 16.dp)) {
                        Text(text = "REINICIAR", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Transparent).border(2.dp, Color.White, RoundedCornerShape(50))
                        .clickable {
                            soundManager.startMusic() // Reactivar música al salir
                            viewModel.restartGame()
                            onMenuClick()
                        }
                        .padding(horizontal = 32.dp, vertical = 12.dp)) {
                        Text(text = "MENÚ PRINCIPAL", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White))
                    }
                }
            }
        }
    }
}

fun mapBubbleColor(type: BubbleColor): Color = when (type) {
    BubbleColor.RED -> BubbleRed; BubbleColor.BLUE -> BubbleBlue; BubbleColor.GREEN -> BubbleGreen; BubbleColor.PURPLE -> BubblePurple; BubbleColor.YELLOW -> BubbleYellow; BubbleColor.CYAN -> BubbleCyan; BubbleColor.BOMB -> Color(0xFF212121)
}