package com.example.orbblaze.ui.game

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orbblaze.domain.model.Achievement
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
    soundManager: SoundManager,
    onMenuClick: () -> Unit = {}
) {
    val bubbles = viewModel.bubblesByPosition
    val activeProjectile = viewModel.activeProjectile
    val score = viewModel.score
    val highScore = viewModel.highScore
    val gameState = viewModel.gameState
    val particles = viewModel.particles
    val floatingTexts = viewModel.floatingTexts

    // LOGROS
    val activeAchievement = viewModel.activeAchievement

    val currentBubbleColor = viewModel.currentBubbleColor
    val previewBubbleColor = viewModel.previewBubbleColor
    val soundEvent = viewModel.soundEvent
    val vibrationEvent = viewModel.vibrationEvent
    val isPaused = viewModel.isPaused

    val haptic = LocalHapticFeedback.current
    var volumeSlider by remember { mutableStateOf(viewModel.getSfxVolume()) }
    var isAiming by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "master_rainbow")
    val masterRainbowRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "rotation"
    )

    LaunchedEffect(gameState) { if (gameState != GameState.PLAYING) soundManager.pauseMusic() else soundManager.startMusic() }
    LaunchedEffect(soundEvent) { soundEvent?.let { soundManager.play(it); viewModel.clearSoundEvent() } }
    LaunchedEffect(vibrationEvent) { if (vibrationEvent) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.clearVibrationEvent() } }

    val density = LocalDensity.current
    val backgroundBrush = Brush.verticalGradient(colors = listOf(BgTop, BgBottom))
    val shooterHeightDp = 142.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .systemBarsPadding()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val startPos = down.position
                    val pandaZoneRadius = 120.dp.toPx()
                    val centerX = size.width / 2
                    val pandaTopY = size.height - 220.dp.toPx()
                    val isPandaClick = startPos.x >= (centerX - pandaZoneRadius) && startPos.x <= (centerX + pandaZoneRadius) && startPos.y >= pandaTopY

                    if (isPandaClick) {
                        do { val event = awaitPointerEvent() } while (event.changes.any { it.pressed })
                        viewModel.swapBubbles()
                    } else {
                        isAiming = true
                        viewModel.updateAngle(startPos.x, startPos.y, size.width.toFloat(), size.height.toFloat())
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.find { it.id == down.id }
                            if (change != null && change.pressed) { viewModel.updateAngle(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat()) }
                        } while (event.changes.any { it.pressed })
                        isAiming = false
                        val handsYPx = size.height - with(density) { shooterHeightDp.toPx() }
                        viewModel.onShoot(size.width.toFloat(), size.height.toFloat(), handsYPx)
                    }
                }
            }
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val bubbleSize = 44.dp
        val bubbleDiameterPx = with(density) { bubbleSize.toPx() }
        val bubbleRadiusPx = bubbleDiameterPx / 2f
        val horizontalSpacing = 40.dp
        val horizontalSpacingPx = with(density) { horizontalSpacing.toPx() }
        val verticalSpacing = 36.dp
        val boardTopPaddingPx = with(density) { 80.dp.toPx() }
        val ceilingYPx = boardTopPaddingPx + bubbleDiameterPx * 0.2f
        val numCols = 10
        val totalGridWidth = numCols * horizontalSpacingPx
        val offset = bubbleDiameterPx / 2f
        val centeredPadding = ((screenWidth - totalGridWidth) / 2f) - (offset / 4f)

        SideEffect {
            viewModel.setBoardMetrics(BoardMetricsPx(bubbleDiameterPx, horizontalSpacingPx, with(density) { verticalSpacing.toPx() }, boardTopPaddingPx, centeredPadding, ceilingYPx))
        }

        // CANVAS
        Canvas(modifier = Modifier.fillMaxSize()) {
            val handsYPx = size.height - with(density) { shooterHeightDp.toPx() }
            val start = Offset(size.width / 2f, handsYPx)

            if (isAiming) {
                val angleRad = Math.toRadians(viewModel.shooterAngle.toDouble())
                var dirX = sin(angleRad).toFloat(); var dirY = -cos(angleRad).toFloat()
                val dlen = hypot(dirX, dirY).coerceAtLeast(0.0001f); dirX /= dlen; dirY /= dlen
                val leftWall = bubbleRadiusPx; val rightWall = size.width - bubbleRadiusPx
                var remaining = size.height * 0.7f; var current = start; var bounced = false
                val guideColor = Color.White.copy(alpha = 0.6f)
                val stroke = Stroke(width = 5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 20f), 0f), cap = StrokeCap.Round)
                while (remaining > 0f) {
                    val tToWall = when { dirX > 0f -> (rightWall - current.x) / dirX; dirX < 0f -> (leftWall - current.x) / dirX; else -> Float.POSITIVE_INFINITY }
                    val distToWall = tToWall
                    if (distToWall.isInfinite() || distToWall.isNaN() || distToWall >= remaining || bounced) {
                        val end = Offset(current.x + dirX * remaining, current.y + dirY * remaining)
                        drawLine(guideColor, current, end, strokeWidth = 5f, pathEffect = stroke.pathEffect, cap = stroke.cap)
                        drawCircle(guideColor, 8f, end); break
                    }
                    val hitPoint = Offset(current.x + dirX * distToWall, current.y + dirY * distToWall)
                    drawLine(guideColor, current, hitPoint, strokeWidth = 5f, pathEffect = stroke.pathEffect, cap = stroke.cap)
                    remaining -= distToWall; dirX *= -1f; current = hitPoint; bounced = true
                }
            }
            val dangerY = boardTopPaddingPx + (verticalSpacing.toPx() * 12)
            drawLine(color = Color.Red.copy(alpha = 0.3f), start = Offset(0f, dangerY), end = Offset(size.width, dangerY), strokeWidth = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f)))
            particles.forEach { p -> drawCircle(color = mapBubbleColor(p.color).copy(alpha = p.life), radius = p.size, center = Offset(p.x, p.y)) }
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply { textSize = 60f; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
                floatingTexts.forEach { ft ->
                    paint.color = android.graphics.Color.WHITE; paint.alpha = (ft.life * 255).toInt().coerceIn(0, 255)
                    paint.setShadowLayer(5f, 2f, 2f, android.graphics.Color.BLACK)
                    canvas.nativeCanvas.drawText(ft.text, ft.x, ft.y, paint)
                }
            }
        }

        // TABLERO
        Box(modifier = Modifier.fillMaxSize()) {
            bubbles.entries.forEach { (pos, bubble) ->
                val rowOffsetPx = if (pos.row % 2 != 0) (bubbleDiameterPx / 2f) else 0f
                val xPosPx = centeredPadding + rowOffsetPx + (pos.col * horizontalSpacingPx)
                val yPosPx = boardTopPaddingPx + (pos.row * with(density) { verticalSpacing.toPx() })
                VisualBubble(color = mapBubbleColor(bubble.color), isRainbow = bubble.color == BubbleColor.RAINBOW, rainbowRotation = masterRainbowRotation, modifier = Modifier.offset(x = with(density) { xPosPx.toDp() }, y = with(density) { yPosPx.toDp() }))
            }
        }

        // PROYECTIL
        activeProjectile?.let { projectile ->
            VisualBubble(color = mapBubbleColor(projectile.color), isRainbow = projectile.color == BubbleColor.RAINBOW, rainbowRotation = masterRainbowRotation, modifier = Modifier.offset(x = with(density) { projectile.x.toDp() } - (bubbleSize / 2), y = with(density) { projectile.y.toDp() } - (bubbleSize / 2)))
        }

        // PANDA
        PandaShooter(angle = viewModel.shooterAngle, currentBubbleColor = mapBubbleColor(currentBubbleColor), isCurrentRainbow = currentBubbleColor == BubbleColor.RAINBOW, nextBubbleColor = mapBubbleColor(previewBubbleColor), isNextRainbow = previewBubbleColor == BubbleColor.RAINBOW, rainbowRotation = masterRainbowRotation, shotTick = viewModel.shotTick, modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-10).dp))

        // BARRA SUPERIOR
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).align(Alignment.TopCenter), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Box {
                    Text(text = "SCORE: $score", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black.copy(alpha = 0.5f), letterSpacing = 2.sp), modifier = Modifier.offset(2.dp, 2.dp))
                    Text(text = "SCORE: $score", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD700), letterSpacing = 2.sp, shadow = Shadow(color = Color.Black, blurRadius = 4f)))
                }
                Text(text = "BEST: $highScore", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), letterSpacing = 1.sp))
            }
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.2f)).clickable { viewModel.togglePause() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Settings, "Configuración", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }

        // ✅ NOTIFICACIÓN DE LOGRO (POPUP ANIMADO)
        AnimatedVisibility(
            visible = activeAchievement != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 90.dp)
        ) {
            activeAchievement?.let { achievement ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF212121).copy(alpha = 0.95f))
                        .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "¡LOGRO DESBLOQUEADO!", style = TextStyle(color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                            Text(text = achievement.title, style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                            Text(text = achievement.description, style = TextStyle(color = Color.Gray, fontSize = 12.sp))
                        }
                    }
                }
            }
        }

        // MENÚS PAUSA / GAME OVER
        if (isPaused && gameState == GameState.PLAYING) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable {}, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PAUSA", style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD700)))
                    Spacer(Modifier.height(32.dp))
                    Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF64FFDA)).clickable { viewModel.togglePause() }.padding(40.dp, 16.dp)) { Text("CONTINUAR", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))) }
                    Spacer(Modifier.height(24.dp))
                    Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White).clickable { viewModel.restartGame() }.padding(40.dp, 16.dp)) { Text("REINICIAR", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)) }
                    Spacer(Modifier.height(24.dp))
                    Text("VOLUMEN EFECTOS", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold))
                    Slider(value = volumeSlider, onValueChange = { volumeSlider = it; viewModel.setSfxVolume(it); soundManager.refreshSettings() }, colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color.White), modifier = Modifier.width(200.dp))
                    Spacer(Modifier.height(24.dp))
                    Box(Modifier.clip(RoundedCornerShape(50)).background(Color.Transparent).border(2.dp, Color.White, RoundedCornerShape(50)).clickable { soundManager.startMusic(); viewModel.restartGame(); onMenuClick() }.padding(40.dp, 12.dp)) { Text("SALIR", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)) }
                }
            }
        }
        if (gameState != GameState.PLAYING) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable {}, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val title = if (gameState == GameState.WON) "¡VICTORIA!" else "GAME OVER"; val col = if (gameState == GameState.WON) Color(0xFFFFD700) else Color(0xFFFF4D4D)
                    Text(title, style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Black, color = col))
                    Spacer(Modifier.height(16.dp))
                    Text("Final: $score", style = TextStyle(fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(32.dp))
                    Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White).border(4.dp, col, RoundedCornerShape(50)).clickable { viewModel.restartGame() }.padding(32.dp, 16.dp)) { Text("REINICIAR", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)) }
                    Spacer(Modifier.height(24.dp))
                    Box(Modifier.clip(RoundedCornerShape(50)).background(Color.Transparent).border(2.dp, Color.White, RoundedCornerShape(50)).clickable { soundManager.startMusic(); viewModel.restartGame(); onMenuClick() }.padding(32.dp, 12.dp)) { Text("MENÚ PRINCIPAL", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)) }
                }
            }
        }
    }
}

fun mapBubbleColor(type: BubbleColor): Color = when (type) {
    BubbleColor.RED -> BubbleRed; BubbleColor.BLUE -> BubbleBlue; BubbleColor.GREEN -> BubbleGreen; BubbleColor.PURPLE -> BubblePurple; BubbleColor.YELLOW -> BubbleYellow; BubbleColor.CYAN -> BubbleCyan; BubbleColor.BOMB -> Color(0xFF212121)
    BubbleColor.RAINBOW -> Color.White
}