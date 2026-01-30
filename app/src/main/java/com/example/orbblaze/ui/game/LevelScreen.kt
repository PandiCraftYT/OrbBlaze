package com.example.orbblaze.ui.game

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orbblaze.domain.model.BubbleColor
import com.example.orbblaze.ui.components.*
import com.example.orbblaze.ui.theme.*
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LevelScreen(
    viewModel: GameViewModel,
    soundManager: SoundManager,
    onMenuClick: () -> Unit = {},
    onShopClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val bubbles = viewModel.bubblesByPosition
    val activeProjectile = viewModel.activeProjectile
    val score = viewModel.score
    val highScore = viewModel.highScore
    val coins = viewModel.coins
    val gameState = viewModel.gameState
    val particles = viewModel.particles
    val floatingTexts = viewModel.floatingTexts
    val activeAchievement = viewModel.activeAchievement
    val rowsDroppedCount = viewModel.rowsDroppedCount

    val currentBubbleColor = viewModel.currentBubbleColor
    val previewBubbleColor = viewModel.previewBubbleColor
    val soundEvent = viewModel.soundEvent
    val vibrationEvent = viewModel.vibrationEvent
    val isPaused = viewModel.isPaused

    val haptic = LocalHapticFeedback.current
    var volumeSlider by remember { mutableFloatStateOf(viewModel.getSfxVolume()) }
    var isAiming by remember { mutableStateOf(false) }
    var hasRedeemedCoins by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "game_fx")
    val masterRainbowRotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "rotation")
    val dangerAlpha by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 0.6f, animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "danger")

    // Reiniciar estado de canje al empezar nueva partida
    LaunchedEffect(gameState) {
        if (gameState == GameState.IDLE) hasRedeemedCoins = false
        if (gameState == GameState.PLAYING) soundManager.startMusic()
        else if (gameState != GameState.IDLE) soundManager.pauseMusic()
    }

    LaunchedEffect(soundEvent) { soundEvent?.let { soundManager.play(it); viewModel.clearSoundEvent() } }
    LaunchedEffect(vibrationEvent) { if (vibrationEvent) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.clearVibrationEvent() } }

    val density = LocalDensity.current
    val backgroundBrush = Brush.verticalGradient(colors = listOf(BgTop, BgBottom))
    val shooterHeightDp = 142.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .pointerInput(gameState, isPaused) {
                if (gameState != GameState.PLAYING || isPaused) return@pointerInput
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
                            if (change != null && change.pressed) {
                                viewModel.updateAngle(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat())
                            }
                        } while (event.changes.any { it.pressed })
                        isAiming = false
                        val handsYPx = size.height - with(density) { shooterHeightDp.toPx() }
                        viewModel.onShoot(size.width.toFloat(), size.height.toFloat(), handsYPx)
                    }
                }
            }
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val bubbleDiameterPx = screenWidth / 10.5f
        val horizontalSpacingPx = bubbleDiameterPx
        val verticalSpacingPx = bubbleDiameterPx * 0.866f
        val boardTopPaddingPx = with(density) { 100.dp.toPx() }
        val ceilingYPx = boardTopPaddingPx + bubbleDiameterPx * 0.2f
        val centeredPadding = (screenWidth - (10 * horizontalSpacingPx)) / 2f

        SideEffect {
            viewModel.setBoardMetrics(BoardMetricsPx(bubbleDiameterPx, horizontalSpacingPx, verticalSpacingPx, boardTopPaddingPx, centeredPadding, ceilingYPx))
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val handsYPx = size.height - with(density) { shooterHeightDp.toPx() }
            val start = Offset(size.width / 2f, handsYPx)
            if (isAiming) {
                val angleRad = Math.toRadians(viewModel.shooterAngle.toDouble())
                var dirX = sin(angleRad).toFloat(); var dirY = -cos(angleRad).toFloat()
                val dlen = hypot(dirX, dirY).coerceAtLeast(0.0001f); dirX /= dlen; dirY /= dlen
                val leftWall = (bubbleDiameterPx / 2f); val rightWall = size.width - (bubbleDiameterPx / 2f)
                var remaining = size.height * 0.7f; var current = start; var bounced = false
                val guideColor = Color.White.copy(alpha = 0.4f)
                val stroke = Stroke(width = 6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 25f), 0f), cap = StrokeCap.Round)
                while (remaining > 0f) {
                    val tToWall = when { dirX > 0f -> (rightWall - current.x) / dirX; dirX < 0f -> (leftWall - current.x) / dirX; else -> Float.POSITIVE_INFINITY }
                    if (tToWall.isInfinite() || tToWall.isNaN() || tToWall >= remaining || bounced) {
                        val end = Offset(current.x + dirX * remaining, current.y + dirY * remaining)
                        drawLine(guideColor, current, end, strokeWidth = stroke.width, pathEffect = stroke.pathEffect, cap = stroke.cap)
                        drawCircle(guideColor, 10f, end); break
                    }
                    val hitPoint = Offset(current.x + dirX * tToWall, current.y + dirY * tToWall)
                    drawLine(guideColor, current, hitPoint, strokeWidth = stroke.width, pathEffect = stroke.pathEffect, cap = stroke.cap)
                    remaining -= tToWall; dirX *= -1f; current = hitPoint; bounced = true
                }
            }
            val dangerY = boardTopPaddingPx + (verticalSpacingPx * 12)
            drawLine(color = Color.Red.copy(alpha = dangerAlpha), start = Offset(0f, dangerY), end = Offset(size.width, dangerY), strokeWidth = 6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f)))
            particles.forEach { p -> drawCircle(color = mapBubbleColor(p.color).copy(alpha = p.life), radius = p.size, center = Offset(p.x, p.y)) }
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply { textSize = 70f; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
                floatingTexts.forEach { ft ->
                    paint.color = android.graphics.Color.WHITE; paint.alpha = (ft.life * 255).toInt().coerceIn(0, 255)
                    paint.setShadowLayer(8f, 2f, 2f, android.graphics.Color.BLACK)
                    canvas.nativeCanvas.drawText(ft.text, ft.x, ft.y, paint)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            bubbles.entries.forEach { (pos, bubble) ->
                val rowOffsetPx = if ((pos.row + rowsDroppedCount) % 2 != 0) (bubbleDiameterPx / 2f) else 0f
                val xPosPx = centeredPadding + rowOffsetPx + (pos.col * horizontalSpacingPx)
                val yPosPx = boardTopPaddingPx + (pos.row * verticalSpacingPx)
                VisualBubble(color = mapBubbleColor(bubble.color), isRainbow = bubble.color == BubbleColor.RAINBOW, rainbowRotation = masterRainbowRotation, modifier = Modifier.offset(x = with(density) { xPosPx.toDp() }, y = with(density) { yPosPx.toDp() }).size(with(density) { bubbleDiameterPx.toDp() }))
            }
            activeProjectile?.let { projectile ->
                VisualBubble(color = mapBubbleColor(projectile.color), isRainbow = projectile.color == BubbleColor.RAINBOW, rainbowRotation = masterRainbowRotation, modifier = Modifier.offset(x = with(density) { projectile.x.toDp() } - with(density) { (bubbleDiameterPx/2).toDp() }, y = with(density) { projectile.y.toDp() } - with(density) { (bubbleDiameterPx/2).toDp() }).size(with(density) { bubbleDiameterPx.toDp() }))
            }
        }

        PandaShooter(angle = viewModel.shooterAngle, currentBubbleColor = mapBubbleColor(currentBubbleColor), isCurrentRainbow = currentBubbleColor == BubbleColor.RAINBOW, nextBubbleColor = mapBubbleColor(previewBubbleColor), isNextRainbow = previewBubbleColor == BubbleColor.RAINBOW, shotTick = viewModel.shotTick, joyTick = viewModel.joyTick, rainbowRotation = masterRainbowRotation, onShopClick = { Toast.makeText(context, "Próximamente...", Toast.LENGTH_SHORT).show() }, modifier = Modifier.align(Alignment.BottomCenter))

        GameTopBar(score = score, bestScore = highScore, coins = coins, timeLeft = if (viewModel.gameMode == GameMode.TIME_ATTACK) viewModel.timeLeft else null, onSettingsClick = { viewModel.togglePause() }, modifier = Modifier.align(Alignment.TopCenter))

        if (gameState == GameState.IDLE) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
                Surface(modifier = Modifier.width(300.dp).padding(16.dp), shape = RoundedCornerShape(28.dp), color = Color(0xFF1A237E), border = BorderStroke(2.dp, Color.White.copy(alpha = 0.2f))) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if(viewModel.gameMode == GameMode.TIME_ATTACK) "CONTRA TIEMPO" else "MODO CLÁSICO", style = TextStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black))
                        Spacer(Modifier.height(12.dp))
                        Text(text = if(viewModel.gameMode == GameMode.TIME_ATTACK) "¡Tienes 60 segundos! Si el tiempo se acaba, caerán 3 filas nuevas." else "Explota burbujas y evita que lleguen a la línea roja.", color = Color.White.copy(alpha = 0.8f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { viewModel.startGame() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64FFDA))) {
                            Text("¡EMPEZAR!", color = Color(0xFF1A237E), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        if (isPaused && gameState == GameState.PLAYING) {
            OverlayMenu(title = "PAUSA", onContinue = { viewModel.togglePause() }, onRestart = { viewModel.restartGame() }, onExit = { soundManager.startMusic(); viewModel.restartGame(); onMenuClick() }, showVolume = true, volume = volumeSlider, onVolumeChange = { volumeSlider = it; viewModel.setSfxVolume(it); soundManager.refreshSettings() })
        }
        if (gameState == GameState.WON || gameState == GameState.LOST) {
            OverlayMenu(title = if (gameState == GameState.WON) "¡VICTORIA!" else "GAME OVER", onContinue = null, onRestart = { viewModel.restartGame() }, onExit = { soundManager.startMusic(); viewModel.restartGame(); onMenuClick() }, score = score, isWin = gameState == GameState.WON, onRedeemCoins = if(!hasRedeemedCoins) { { if (score >= 100) { viewModel.addCoins(score / 100); hasRedeemedCoins = true; Toast.makeText(context, "¡Canjeado!", Toast.LENGTH_SHORT).show() } } } else null)
        }
    }
}

@Composable
fun OverlayMenu(title: String, onContinue: (() -> Unit)? = null, onRestart: () -> Unit, onExit: () -> Unit, score: Int? = null, isWin: Boolean = false, showVolume: Boolean = false, volume: Float = 0f, onVolumeChange: (Float) -> Unit = {}, onRedeemCoins: (() -> Unit)? = null) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.88f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            val titleColor = if (isWin) Color(0xFFFFD700) else if (title == "PAUSA") Color.White else Color(0xFFFF4D4D)
            Text(text = title, style = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Black, color = titleColor, letterSpacing = 2.sp, shadow = Shadow(color = Color.Black, offset = Offset(4f, 4f), blurRadius = 12f)))
            if (score != null) {
                Spacer(Modifier.height(8.dp)); Text(text = "PUNTUACIÓN FINAL: $score", style = TextStyle(fontSize = 20.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.ExtraBold))
                onRedeemCoins?.let { action ->
                    Spacer(Modifier.height(16.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFD700).copy(alpha = 0.2f)).border(1.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp)).clickable { action() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("CANJEAR PUNTOS POR MONEDAS", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(50.dp))
            onContinue?.let { action -> Box(modifier = Modifier.width(260.dp).height(64.dp).clip(RoundedCornerShape(50)).background(Color(0xFF64FFDA)).clickable { action() }, contentAlignment = Alignment.Center) { Text("CONTINUAR", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A237E))) }; Spacer(Modifier.height(24.dp)) }
            if (showVolume) {
                Text("VOLUMEN EFECTOS", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp))
                Spacer(Modifier.height(8.dp)); Slider(value = volume, onValueChange = onVolumeChange, colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.2f)), modifier = Modifier.width(240.dp))
                Spacer(Modifier.height(40.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).border(2.dp, Color.White, CircleShape).clickable { onRestart() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                Spacer(Modifier.width(40.dp)); Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.Red.copy(alpha = 0.2f)).border(2.dp, Color.White, CircleShape).clickable { onExit() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Home, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
            }
        }
    }
}

@Composable
fun GameMenuButton(text: String, color: Color, textColor: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.width(240.dp).height(60.dp).clip(RoundedCornerShape(50)).background(color).clickable { onClick() }, contentAlignment = Alignment.Center) { Text(text = text, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textColor)) }
}

fun mapBubbleColor(type: BubbleColor): Color = when (type) {
    BubbleColor.RED -> BubbleRed; BubbleColor.BLUE -> BubbleBlue; BubbleColor.GREEN -> BubbleGreen; BubbleColor.PURPLE -> BubblePurple; BubbleColor.YELLOW -> BubbleYellow; BubbleColor.CYAN -> BubbleCyan; BubbleColor.BOMB -> Color(0xFF212121); BubbleColor.RAINBOW -> Color.White
}