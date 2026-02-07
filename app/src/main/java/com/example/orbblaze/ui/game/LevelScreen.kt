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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.domain.model.AdventureLevels
import com.example.orbblaze.domain.model.BoardMetricsPx
import com.example.orbblaze.domain.model.BubbleColor
import com.example.orbblaze.ui.components.*
import com.example.orbblaze.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LevelScreen(
    viewModel: GameViewModel,
    soundManager: SoundManager,
    onMenuClick: () -> Unit = {},
    onShopClick: () -> Unit = {},
    onShowAd: (onReward: (Int) -> Unit) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val bubbles = viewModel.bubblesByPosition
    val activeProjectile = viewModel.activeProjectile
    val score = viewModel.score
    val highScore = viewModel.highScore
    val coins = viewModel.coins
    val gameState = viewModel.gameState
    val particles = viewModel.particles
    val floatingTexts = viewModel.floatingTexts
    val rowsDroppedCount = viewModel.rowsDroppedCount
    val timeLeft = viewModel.timeLeft
    val columnsCount = viewModel.columnsCount

    val currentBubbleColor = viewModel.currentBubbleColor
    val previewBubbleColor = viewModel.previewBubbleColor
    val soundEvent = viewModel.soundEvent
    val vibrationEvent = viewModel.vibrationEvent
    val isPaused = viewModel.isPaused
    val isFireballQueued = viewModel.isFireballQueued

    var showQuickShop by remember { mutableStateOf(false) }
    var hasRedeemedCoins by remember { mutableStateOf(false) }
    var volumeSlider by remember { mutableFloatStateOf(1.0f) }
    var isAiming by remember { mutableStateOf(false) }

    // --- RECTÁNGULOS PARA EL TUTORIAL ---
    var shopRect by remember { mutableStateOf<Rect?>(null) }
    var cannonRect by remember { mutableStateOf<Rect?>(null) }
    var nextBubbleRect by remember { mutableStateOf<Rect?>(null) }
    var scoreRect by remember { mutableStateOf<Rect?>(null) }

    val settingsManager = remember { SettingsManager(context) }
    val tutorialCompleted by settingsManager.tutorialCompletedFlow.collectAsState(initial = true)
    var showTutorial by remember { mutableStateOf(false) }

    LaunchedEffect(tutorialCompleted, viewModel.gameMode) {
        if (!tutorialCompleted && viewModel.gameMode == GameMode.CLASSIC) {
            showTutorial = true
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "game_fx")
    val dangerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f, 
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "danger"
    )
    val masterRainbowRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f, 
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "rotation"
    )
    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -2f, targetValue = 2f, 
        animationSpec = infiniteRepeatable(tween(50, easing = LinearEasing), RepeatMode.Reverse), label = "shake"
    )

    val dangerLineRow = 13
    val isEmergency = (viewModel.gameMode == GameMode.TIME_ATTACK && timeLeft <= 10) || bubbles.keys.any { it.row >= (dangerLineRow - 2) }

    LaunchedEffect(timeLeft, gameState, isPaused) {
        if (viewModel.gameMode == GameMode.TIME_ATTACK && gameState == GameState.PLAYING && !isPaused) {
            if (timeLeft <= 15) soundManager.setMusicSpeed(1.25f) else soundManager.setMusicSpeed(1.0f)
        } else soundManager.setMusicSpeed(1.0f)
    }

    LaunchedEffect(gameState) {
        if (gameState == GameState.IDLE) hasRedeemedCoins = false
        if (gameState == GameState.PLAYING) soundManager.startMusic()
        else if (gameState != GameState.IDLE) soundManager.pauseMusic()
    }

    LaunchedEffect(soundEvent) { soundEvent?.let { soundManager.play(it); viewModel.clearSoundEvent() } }
    LaunchedEffect(vibrationEvent) { if (vibrationEvent) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.clearVibrationEvent() } }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(BgTop, BgBottom)))
            .pointerInput(gameState, isPaused, showQuickShop, showTutorial) {
                if (gameState != GameState.PLAYING || isPaused || showQuickShop || showTutorial) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(); val startPos = down.position
                    val centerX = size.width / 2; val pandaTopY = size.height - 220.dp.toPx()
                    val isPandaClick = startPos.x >= (centerX - 120.dp.toPx()) && startPos.x <= (centerX + 120.dp.toPx()) && startPos.y >= pandaTopY

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
                        
                        val barrelLengthPx = 95.dp.toPx()
                        val pivotHeightPx = 160.dp.toPx()
                        val angleRad = Math.toRadians(viewModel.shooterAngle.toDouble())
                        val pivotX = size.width / 2f
                        val pivotY = size.height - pivotHeightPx
                        viewModel.onShoot(pivotX + (sin(angleRad) * barrelLengthPx).toFloat(), pivotY - (cos(angleRad) * barrelLengthPx).toFloat())
                    }
                }
            }
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        
        // ✅ CÁLCULO ADAPTATIVO: El diámetro depende de columnsCount
        val bubbleDiameterPx = totalWidth / (columnsCount + 0.5f) 
        val horizontalSpacingPx = bubbleDiameterPx
        val boardStartPadding = bubbleDiameterPx * 0.5f
        
        val statusBarHeightPx = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value * density.density
        val boardTopPaddingPx = statusBarHeightPx + with(density) { 104.dp.toPx() } 
        val verticalSpacingPx = bubbleDiameterPx * 0.866f

        LaunchedEffect(totalWidth, boardTopPaddingPx, columnsCount) {
            viewModel.setBoardMetrics(
                BoardMetricsPx(
                    horizontalSpacing = horizontalSpacingPx,
                    bubbleDiameter = bubbleDiameterPx,
                    verticalSpacing = verticalSpacingPx,
                    boardTopPadding = boardTopPaddingPx,
                    boardStartPadding = boardStartPadding,
                    ceilingY = boardTopPaddingPx - (bubbleDiameterPx * 0.5f),
                    screenWidth = totalWidth
                )
            )
        }

        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { if (isEmergency) { translationX = shakeOffset; translationY = shakeOffset } }) {
            if (isEmergency) drawRect(color = Color.Red.copy(alpha = dangerAlpha), size = size)

            val pivotX = size.width / 2f
            val pivotY = size.height - 160.dp.toPx()

            if (isAiming) {
                val angleRad = Math.toRadians(viewModel.shooterAngle.toDouble())
                var dirX = sin(angleRad).toFloat(); var dirY = -cos(angleRad).toFloat()
                var current = Offset(pivotX + (sin(angleRad) * 95.dp.toPx()).toFloat(), pivotY - (cos(angleRad) * 95.dp.toPx()).toFloat())
                var remaining = size.height * 0.9f

                while (remaining > 0f) {
                    val bounceX = if (dirX > 0f) size.width else 0f
                    val tToWall = (bounceX - current.x) / dirX
                    if (tToWall <= 0 || tToWall >= remaining) {
                        drawLine(Color.White.copy(0.5f), current, Offset(current.x + dirX * remaining, current.y + dirY * remaining), strokeWidth = 5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f), cap = StrokeCap.Round)
                        break
                    }
                    val hit = Offset(current.x + dirX * tToWall, current.y + dirY * tToWall)
                    drawLine(Color.White.copy(0.5f), current, hit, strokeWidth = 5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f), cap = StrokeCap.Round)
                    remaining -= tToWall; dirX *= -1f; current = hit
                }
            }

            // LÍNEA ROJA SINCRONIZADA EXACTAMENTE EN FILA 13
            drawLine(
                color = Color.Red.copy(alpha = dangerAlpha), 
                start = Offset(0f, boardTopPaddingPx + verticalSpacingPx * 13), 
                end = Offset(size.width, boardTopPaddingPx + verticalSpacingPx * 13), 
                strokeWidth = 8f, 
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f))
            )
            
            particles.forEach { p -> drawCircle(color = mapBubbleColor(p.color).copy(alpha = p.life), radius = p.size, center = Offset(p.x, p.y)) }
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply { textSize = 70f; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD; color = android.graphics.Color.WHITE }
                floatingTexts.forEach { ft -> paint.alpha = (ft.life * 255).toInt().coerceIn(0, 255); canvas.nativeCanvas.drawText(ft.text, ft.x, ft.y, paint) }
            }
        }

        Box(modifier = Modifier.fillMaxSize().graphicsLayer { if (isEmergency) { translationX = shakeOffset; translationY = shakeOffset } }) {
            bubbles.forEach { (pos, bubble) ->
                val (x, y) = viewModel.getBubbleCenter(pos)
                VisualBubble(
                    color = mapBubbleColor(bubble.color),
                    isRainbow = bubble.color == BubbleColor.RAINBOW,
                    rainbowRotation = masterRainbowRotation,
                    modifier = Modifier
                        .size(with(density) { bubbleDiameterPx.toDp() })
                        .graphicsLayer {
                            translationX = x - (bubbleDiameterPx / 2)
                            translationY = y - (bubbleDiameterPx / 2)
                        }
                )
            }

            activeProjectile?.let { p ->
                val scaleFactor = if(p.isFireball) 0.7f else 1f
                val sizePx = bubbleDiameterPx * scaleFactor
                VisualBubble(
                    color = mapBubbleColor(p.color),
                    isRainbow = p.color == BubbleColor.RAINBOW,
                    rainbowRotation = masterRainbowRotation,
                    modifier = Modifier
                        .size(with(density) { sizePx.toDp() })
                        .graphicsLayer {
                            translationX = p.x - (sizePx / 2)
                            translationY = p.y - (sizePx / 2)
                            if (p.isFireball) rotationZ = Math.toDegrees(atan2(p.velocityY.toDouble(), p.velocityX.toDouble())).toFloat() + 90f
                        }
                )
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            PandaShooter(
                angle = viewModel.shooterAngle,
                currentBubbleColor = if(isFireballQueued) Color(0xFFFF5722) else mapBubbleColor(currentBubbleColor),
                isCurrentRainbow = currentBubbleColor == BubbleColor.RAINBOW && !isFireballQueued,
                nextBubbleColor = mapBubbleColor(previewBubbleColor),
                isNextRainbow = previewBubbleColor == BubbleColor.RAINBOW,
                shotTick = viewModel.shotTick,
                joyTick = viewModel.joyTick,
                rainbowRotation = masterRainbowRotation,
                onShopClick = { showQuickShop = true },
                onShopPositioned = { shopRect = it },
                onCannonPositioned = { cannonRect = it },
                onNextBubblePositioned = { nextBubbleRect = it }
            )
        }

        GameTopBar(
            score = score, bestScore = highScore, coins = coins, 
            timeLeft = if (viewModel.gameMode == GameMode.TIME_ATTACK) timeLeft else null,
            shotsLeft = if (viewModel.gameMode == GameMode.ADVENTURE) (viewModel as? AdventureViewModel)?.shotsRemaining else null,
            onSettingsClick = { viewModel.togglePause() }, 
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().onGloballyPositioned { scoreRect = it.boundsInRoot() }
        )

        // DIÁLOGOS Y TUTORIAL (SIN CAMBIOS)
        if (showQuickShop) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)).clickable { showQuickShop = false }, contentAlignment = Alignment.Center) {
                Surface(modifier = Modifier.width(300.dp).padding(16.dp), shape = RoundedCornerShape(28.dp), color = Color.White) {
                    Column(modifier = Modifier.background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF5F5F5)))).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("OBJETOS TÁCTICOS", color = Color(0xFF1A237E), fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.height(20.dp))
                        ItemRow("BOLA DE FUEGO", "Atraviesa todo", 150, "🔥") { viewModel.buyFireball(); showQuickShop = false }
                        Spacer(Modifier.height(24.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(50)).background(Color(0xFF1A237E)).clickable { showQuickShop = false }, contentAlignment = Alignment.Center) { Text("CERRAR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp) }
                    }
                }
            }
        }

        if (gameState == GameState.IDLE && !showTutorial) {
            if (viewModel.gameMode == GameMode.ADVENTURE) {
                val advViewModel = viewModel as? AdventureViewModel
                val currentLevel = AdventureLevels.levels.find { it.id == advViewModel?.currentLevelId }
                if (currentLevel != null) {
                    AdventureStartDialog(
                        levelId = currentLevel.id,
                        objective = currentLevel.objective,
                        onStartClick = { viewModel.startGame() }
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
                    Surface(modifier = Modifier.width(300.dp).padding(16.dp), shape = RoundedCornerShape(28.dp), color = Color(0xFF1A237E), border = BorderStroke(2.dp, Color.White.copy(alpha = 0.2f))) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            val title = if (viewModel.gameMode == GameMode.TIME_ATTACK) "TIME ATTACK" else "MODO CLÁSICO"
                            val desc = if (viewModel.gameMode == GameMode.TIME_ATTACK) "¡Explota burbujas rápido antes de que se agote el tiempo!" else "Explota burbujas y evita que lleguen a la línea roja."
                            
                            Text(text = title, style = TextStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black))
                            Spacer(Modifier.height(12.dp))
                            Text(text = desc, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = { viewModel.startGame() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64FFDA))) { Text("¡EMPEZAR!", color = Color(0xFF1A237E), fontWeight = FontWeight.Black) }
                        }
                    }
                }
            }
        }

        if (isPaused && gameState == GameState.PLAYING) {
            OverlayMenu(
                title = "PAUSA", onContinue = { viewModel.togglePause() }, onRestart = { viewModel.restartGame() }, 
                onExit = { soundManager.startMusic(); viewModel.restartGame(); onMenuClick() }, 
                showVolume = true, volume = volumeSlider, 
                onVolumeChange = { newVal -> volumeSlider = newVal; viewModel.setSfxVolume(newVal); soundManager.refreshSettings() }
            )
        }
        
        if (gameState == GameState.WON || gameState == GameState.LOST) {
            OverlayMenu(
                title = if (gameState == GameState.WON) "¡VICTORIA!" else "GAME OVER", onContinue = null, onRestart = { viewModel.restartGame() }, 
                onExit = { viewModel.restartGame(); onMenuClick() },
                score = score, isWin = gameState == GameState.WON, isAdventure = viewModel.gameMode == GameMode.ADVENTURE,
                onRedeemCoins = if(!hasRedeemedCoins) { { if (score >= 100) { viewModel.addCoins(score / 100); hasRedeemedCoins = true; Toast.makeText(context, "¡Canjeado!", Toast.LENGTH_SHORT).show() } } } else null,
                onShowAd = { onShowAd { _ -> viewModel.addCoins(50); Toast.makeText(context, "¡Ganaste 50 monedas!", Toast.LENGTH_SHORT).show() } }
            )
        }

        if (showTutorial) {
            TutorialDialog(shopRect = shopRect, cannonRect = cannonRect, nextBubbleRect = nextBubbleRect, scoreRect = scoreRect,
                onComplete = {
                    showTutorial = false
                    coroutineScope.launch { settingsManager.setTutorialCompleted(true); viewModel.startGame() }
                }
            )
        }
    }
}

@Composable
fun ItemRow(name: String, desc: String, price: Int, icon: String, onBuy: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF1A237E).copy(alpha = 0.05f)).border(1.dp, Color(0xFF1A237E).copy(alpha = 0.1f), RoundedCornerShape(16.dp)).clickable { onBuy() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 32.sp); Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) { Text(name, color = Color(0xFF1A237E), fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(desc, color = Color(0xFF1A237E).copy(alpha = 0.5f), fontSize = 11.sp) }
        Surface(color = Color(0xFFFFD700), shape = RoundedCornerShape(50)) { Text("🪙 $price", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A237E)) }
    }
}

@Composable
fun OverlayMenu(
    title: String, 
    onContinue: (() -> Unit)? = null, 
    onRestart: () -> Unit, 
    onExit: () -> Unit, 
    score: Int? = null, 
    isWin: Boolean = false, 
    showVolume: Boolean = false, 
    volume: Float = 0f, 
    onVolumeChange: (Float) -> Unit = {}, 
    onRedeemCoins: (() -> Unit)? = null, 
    onShowAd: (() -> Unit)? = null,
    isAdventure: Boolean = false
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.88f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(text = title, style = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Black, color = if (isWin) Color(0xFFFFD700) else if (title == "PAUSA") Color.White else Color(0xFFFF4D4D), letterSpacing = 2.sp, shadow = Shadow(color = Color.Black, offset = Offset(4f, 4f), blurRadius = 12f)))
            if (score != null) {
                Spacer(Modifier.height(8.dp)); Text(text = "PUNTUACIÓN FINAL: $score", style = TextStyle(fontSize = 20.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.ExtraBold))
                onRedeemCoins?.let { action -> Spacer(Modifier.height(16.dp)); Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFD700).copy(alpha = 0.2f)).border(1.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp)).clickable { action() }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("CANJEAR PUNTOS POR MONEDAS", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
                onShowAd?.let { adAction -> Spacer(Modifier.height(12.dp)); Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF64FFDA).copy(alpha = 0.2f)).border(1.dp, Color(0xFF64FFDA), RoundedCornerShape(12.dp)).clickable { adAction() }.padding(horizontal = 16.dp, vertical = 8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF64FFDA), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("VER ANUNCIO (+50 🪙)", color = Color(0xFF64FFDA), fontSize = 12.sp, fontWeight = FontWeight.Bold) } } }
            }
            Spacer(Modifier.height(50.dp))
            onContinue?.let { action -> Box(modifier = Modifier.width(260.dp).height(64.dp).clip(RoundedCornerShape(50)).background(Color(0xFF64FFDA)).clickable { action() }, contentAlignment = Alignment.Center) { Text("CONTINUAR", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A237E))) }; Spacer(Modifier.height(24.dp)) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).border(2.dp, Color.White, CircleShape).clickable { onRestart() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                Spacer(Modifier.width(40.dp))
                val exitBgColor = if (isAdventure) Color(0xFF64FFDA).copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f)
                val exitIcon = if (isAdventure) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Home
                Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(exitBgColor).border(2.dp, Color.White, CircleShape).clickable { onExit() }, contentAlignment = Alignment.Center) { Icon(exitIcon, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
            }
        }
    }
}

fun mapBubbleColor(type: BubbleColor): Color = when (type) {
    BubbleColor.RED -> BubbleRed; BubbleColor.BLUE -> BubbleBlue; BubbleColor.GREEN -> BubbleGreen; BubbleColor.PURPLE -> BubblePurple; BubbleColor.YELLOW -> BubbleYellow; BubbleColor.CYAN -> BubbleCyan; BubbleColor.BOMB -> Color(0xFF212121); BubbleColor.RAINBOW -> Color.White
}

@Composable
fun FireballRenderer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "fire_realism")
    val pulse by infiniteTransition.animateFloat(initialValue = 0.9f, targetValue = 1.1f, animationSpec = infiniteRepeatable(tween(100, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2; val cx = size.width / 2; val cy = size.height / 2
        drawPath(path = Path().apply { moveTo(cx - r * 0.5f, cy); quadraticTo(cx, cy + r * 6f, cx + r * 0.5f, cy); close() }, brush = Brush.verticalGradient(colors = listOf(Color(0xFFFFEB3B), Color(0xFFFF5722), Color.Transparent), startY = cy, endY = cy + r * 5f))
        drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFFFF5722).copy(alpha = 0.6f), Color.Transparent), center = center, radius = r * 1.5f * pulse))
        drawCircle(brush = Brush.radialGradient(colorStops = arrayOf(0.0f to Color.White, 0.4f to Color(0xFFFFEB3B), 1.0f to Color(0xFFFF5722)), center = center, radius = r * 0.9f))
    }
}
