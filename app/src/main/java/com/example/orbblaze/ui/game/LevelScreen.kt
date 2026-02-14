package com.example.orbblaze.ui.game

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.R
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.domain.model.*
import com.example.orbblaze.ui.components.*
import com.example.orbblaze.ui.theme.*
import kotlinx.coroutines.delay
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
    val timeLeft = viewModel.timeLeft
    val columnsCount = viewModel.columnsCount

    val currentBubbleColor = viewModel.currentBubbleColor
    val previewBubbleColor = viewModel.previewBubbleColor
    val soundEvent = viewModel.soundEvent
    val vibrationEvent = viewModel.vibrationEvent
    val isPaused = viewModel.isPaused
    val isFireballQueued = viewModel.isFireballQueued
    val currentGameMode = viewModel.gameMode
    val shakeIntensity = viewModel.shakeIntensity

    var showQuickShop by remember { mutableStateOf(false) }
    var hasRedeemedCoins by remember { mutableStateOf(false) }
    var isAiming by remember { mutableStateOf(false) }

    var shopRect by remember { mutableStateOf<Rect?>(null) }
    var cannonRect by remember { mutableStateOf<Rect?>(null) }
    var nextBubbleRect by remember { mutableStateOf<Rect?>(null) }
    var scoreRect by remember { mutableStateOf<Rect?>(null) }

    val settingsManager = remember { SettingsManager(context) }
    val isColorBlindMode by settingsManager.colorBlindModeFlow.collectAsState(initial = false)

    BackHandler(enabled = gameState == GameState.PLAYING && !isPaused) {
        viewModel.togglePause()
    }

    val currentLevelId = (viewModel as? AdventureViewModel)?.currentLevelId ?: 1
    val infiniteTransition = rememberInfiniteTransition(label = "game_fx")
    
    val backgroundOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2000f,
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing), RepeatMode.Restart),
        label = "bg_scroll"
    )

    val aimPulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "aim_pulse"
    )

    val bgColors = if (currentGameMode == GameMode.ADVENTURE) {
        when {
            currentLevelId <= 30 -> listOf(Color(0xFF81D4FA), Color(0xFF4FC3F7)) 
            currentLevelId <= 50 -> listOf(Color(0xFF3E2723), Color(0xFFBF360C)) 
            currentLevelId <= 70 -> listOf(Color(0xFF1B5E20), Color(0xFF4DB6AC)) 
            currentLevelId <= 90 -> listOf(Color(0xFF0277BD), Color(0xFFE1F5FE)) 
            else -> listOf(Color(0xFF0D47A1), Color(0xFF000000)) 
        }
    } else {
        listOf(BgTop, BgBottom)
    }

    val animatedBgTop by animateColorAsState(targetValue = bgColors.first(), animationSpec = tween(1000), label = "bgTop")
    val animatedBgBottom by animateColorAsState(targetValue = bgColors.last(), animationSpec = tween(1000), label = "bgBottom")

    val isReviveAlertActive = (viewModel as? AdventureViewModel)?.showReviveAlert == true

    LaunchedEffect(gameState, isPaused, isReviveAlertActive) {
        if (gameState == GameState.PLAYING && !isPaused && !isReviveAlertActive) {
            soundManager.startMusic()
        } else if (gameState == GameState.LOST || gameState == GameState.WON || isPaused) {
            // ✅ SILENCIAR MÚSICA SI PIERDES O GANAS
            soundManager.pauseMusic()
        }
    }

    val dangerAlpha by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 0.8f, animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "danger")
    val masterRainbowRotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "rotation")
    
    // ✅ TEMBLOR SUAVIZADO
    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 1f, 
        animationSpec = infiniteRepeatable(tween(50, easing = LinearEasing), RepeatMode.Reverse), label = "shake"
    )

    LaunchedEffect(timeLeft, gameState, isPaused) {
        if (viewModel.gameMode == GameMode.TIME_ATTACK && gameState == GameState.PLAYING && !isPaused) {
            if (timeLeft <= 15) soundManager.setMusicSpeed(1.25f) else soundManager.setMusicSpeed(1.0f)
        } else soundManager.setMusicSpeed(1.0f)
    }

    LaunchedEffect(soundEvent) { soundEvent?.let { soundManager.play(it); viewModel.clearSoundEvent() } }
    LaunchedEffect(vibrationEvent) { if (vibrationEvent) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.clearVibrationEvent() } }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(animatedBgTop, animatedBgBottom)))
            .pointerInput(gameState, isPaused, showQuickShop) {
                if (gameState != GameState.PLAYING || isPaused || showQuickShop) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(); val startPos = down.position
                    val centerX = size.width / 2; val pandaTopY = size.height - 280.dp.toPx() 
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
                            if (change != null && change.pressed) { viewModel.updateAngle(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat()) }
                        } while (event.changes.any { it.pressed })
                        isAiming = false
                        val barrelLengthPx = 95.dp.toPx()
                        val pivotHeightPx = 220.dp.toPx() 
                        val angleRad = Math.toRadians(viewModel.shooterAngle.toDouble())
                        val pivotX = size.width / 2f
                        val pivotY = size.height - pivotHeightPx
                        viewModel.onShoot(pivotX + (sin(angleRad) * barrelLengthPx).toFloat(), pivotY - (cos(angleRad) * barrelLengthPx).toFloat())
                    }
                }
            }
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()
        val bubbleDiameterPx = totalWidth / (columnsCount + 0.5f) 
        val bubbleRadiusPx = bubbleDiameterPx / 2f
        val verticalSpacingPx = bubbleDiameterPx * 0.866f
        val horizontalSpacingPx = bubbleDiameterPx
        val boardStartPadding = bubbleDiameterPx * 0.5f
        
        val statusBarHeightPx = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value * density.density
        val boardTopPaddingPx = statusBarHeightPx + with(density) { 90.dp.toPx() } 

        // ✅ LÍNEA DE PELIGRO: 360dp desde el fondo (Sobre el cañón)
        val dangerAreaHeightPx = with(density) { 360.dp.toPx() } 
        val availableHeight = totalHeight - boardTopPaddingPx - dangerAreaHeightPx
        val finalDangerRow = (availableHeight / verticalSpacingPx).toInt()

        LaunchedEffect(finalDangerRow) {
            viewModel.dynamicDangerRow = finalDangerRow
        }

        // ✅ INTENSIDAD DE TEMBLOR REDUCIDA
        val isDangerActive = bubbles.keys.any { it.row >= (finalDangerRow - 2) }
        val finalShakeIntensity = (if (isDangerActive) 3f else 0f) + (shakeIntensity * 0.5f)

        if (currentGameMode != GameMode.ADVENTURE) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val color1 = Color.White.copy(alpha = 0.15f)
                val color2 = Color.White.copy(alpha = 0.05f)
                drawCircle(color = color1, radius = 150.dp.toPx(), center = Offset(x = (backgroundOffset % (totalWidth + 400.dp.toPx())) - 200.dp.toPx(), y = 150.dp.toPx()))
                drawCircle(color = color2, radius = 250.dp.toPx(), center = Offset(x = ((backgroundOffset * 0.7f) % (totalWidth + 600.dp.toPx())) - 300.dp.toPx(), y = totalHeight * 0.4f))
                drawCircle(color = color1, radius = 120.dp.toPx(), center = Offset(x = totalWidth - ((backgroundOffset * 1.2f) % (totalWidth + 300.dp.toPx())), y = totalHeight * 0.7f))
            }
        }

        LaunchedEffect(totalWidth, boardTopPaddingPx, columnsCount) {
            viewModel.setBoardMetrics(BoardMetricsPx(horizontalSpacing = horizontalSpacingPx, bubbleDiameter = bubbleDiameterPx, verticalSpacing = verticalSpacingPx, boardTopPadding = boardTopPaddingPx, boardStartPadding = boardStartPadding, ceilingY = boardTopPaddingPx - (bubbleDiameterPx * 0.5f), screenWidth = totalWidth))
        }

        Box(modifier = Modifier.fillMaxSize().graphicsLayer { translationX = shakeOffset * finalShakeIntensity; translationY = shakeOffset * finalShakeIntensity }) {
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val redLineY = boardTopPaddingPx + (verticalSpacingPx * finalDangerRow)
                
                if (isDangerActive || (viewModel.gameMode == GameMode.TIME_ATTACK && timeLeft <= 10)) {
                    drawRect(color = Color.Red.copy(alpha = dangerAlpha * 0.3f), size = size)
                }

                drawLine(
                    color = Color.Red.copy(alpha = 0.9f),
                    start = Offset(0f, redLineY),
                    end = Offset(size.width, redLineY),
                    strokeWidth = 8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f))
                )

                val pivotX = size.width / 2f
                val pivotY = size.height - 220.dp.toPx() 
                if (isAiming) {
                    val angleRad = Math.toRadians(viewModel.shooterAngle.toDouble())
                    var dirX = sin(angleRad).toFloat(); var dirY = -cos(angleRad).toFloat()
                    val barrelLength = 95.dp.toPx(); var current = Offset(pivotX + dirX * barrelLength, pivotY + dirY * barrelLength)
                    val totalAimLength = size.height * 0.85f; var remaining = totalAimLength
                    val dotSpacing = 24.dp.toPx(); val baseDotRadius = 4.dp.toPx()
                    val bubbleColor = if(isFireballQueued) Color(0xFFFF5722) else mapBubbleColor(currentBubbleColor)
                    var totalTraversed = 0f
                    while (remaining > 0f) {
                        val bounceX = if (dirX > 0f) size.width else 0f; val tToWall = (bounceX - current.x) / dirX
                        val segmentLength = if (tToWall <= 0 || tToWall >= remaining) remaining else tToWall
                        var segmentTraversed = (aimPulse * dotSpacing) % dotSpacing
                        while (segmentTraversed < segmentLength) {
                            val dotPos = Offset(current.x + dirX * segmentTraversed, current.y + dirY * segmentTraversed)
                            val progress = (totalTraversed + segmentTraversed) / totalAimLength
                            val alpha = (0.7f - progress * 0.5f).coerceIn(0.1f, 0.7f); val radius = baseDotRadius * (1f - progress * 0.3f)
                            drawCircle(brush = Brush.radialGradient(colors = listOf(bubbleColor.copy(alpha = alpha), Color.Transparent), center = dotPos, radius = radius * 3f), radius = radius * 3f, center = dotPos)
                            drawCircle(color = Color.White.copy(alpha = (alpha + 0.2f).coerceAtMost(1f)), radius = radius, center = dotPos)
                            segmentTraversed += dotSpacing
                        }
                        if (tToWall <= 0 || tToWall >= remaining) break
                        totalTraversed += segmentLength; val hit = Offset(current.x + dirX * tToWall, current.y + dirY * tToWall)
                        remaining -= tToWall; dirX *= -1f; current = hit
                    }
                }
                
                particles.forEach { p -> drawCircle(color = mapBubbleColor(p.color).copy(alpha = p.life), radius = p.size, center = Offset(p.x, p.y)) }
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply { textSize = 70f; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD; color = android.graphics.Color.WHITE }
                    floatingTexts.forEach { ft -> paint.alpha = (ft.life * 255).toInt().coerceIn(0, 255); canvas.nativeCanvas.drawText(ft.text, ft.x, ft.y, paint) }
                }
            }

            bubbles.forEach { (pos, bubble) -> 
                val (x, y) = viewModel.getBubbleCenter(pos)
                VisualBubble(
                    color = mapBubbleColor(bubble.color), 
                    isRainbow = bubble.color == BubbleColor.RAINBOW, 
                    isBomb = bubble.color == BubbleColor.BOMB, 
                    rainbowRotation = masterRainbowRotation, 
                    isColorBlindMode = isColorBlindMode,
                    bubbleColorType = bubble.color,
                    modifier = Modifier.size(with(density) { bubbleDiameterPx.toDp() }).graphicsLayer { translationX = x - (bubbleDiameterPx / 2); translationY = y - (bubbleDiameterPx / 2) }
                ) 
            }
            activeProjectile?.let { p -> 
                val scaleFactor = if(p.isFireball) 0.7f else 1f
                val sizePx = bubbleDiameterPx * scaleFactor
                VisualBubble(
                    color = mapBubbleColor(p.color), 
                    isRainbow = p.color == BubbleColor.RAINBOW, 
                    isBomb = p.color == BubbleColor.BOMB, 
                    rainbowRotation = masterRainbowRotation, 
                    isColorBlindMode = isColorBlindMode,
                    bubbleColorType = p.color,
                    modifier = Modifier.size(with(density) { sizePx.toDp() }).graphicsLayer { translationX = p.x - (sizePx / 2); translationY = p.y - (sizePx / 2); if (p.isFireball) rotationZ = Math.toDegrees(atan2(p.velocityY.toDouble(), p.velocityX.toDouble())).toFloat() + 90f }
                ) 
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp)) {
                PandaShooter(
                    angle = viewModel.shooterAngle, 
                    currentBubbleColor = if(isFireballQueued) Color(0xFFFF5722) else mapBubbleColor(currentBubbleColor), 
                    currentBubbleType = currentBubbleColor, 
                    isCurrentRainbow = currentBubbleColor == BubbleColor.RAINBOW && !isFireballQueued, 
                    nextBubbleColor = mapBubbleColor(previewBubbleColor), 
                    nextBubbleType = previewBubbleColor, 
                    isNextRainbow = previewBubbleColor == BubbleColor.RAINBOW, 
                    isColorBlindMode = isColorBlindMode, 
                    shotTick = viewModel.shotTick, 
                    joyTick = viewModel.joyTick, 
                    rainbowRotation = masterRainbowRotation, 
                    onShopClick = { if (currentGameMode == GameMode.ADVENTURE) { Toast.makeText(context, context.getString(R.string.shop_not_available_adventure), Toast.LENGTH_SHORT).show() } else { showQuickShop = true } }, 
                    isShopEnabled = currentGameMode != GameMode.ADVENTURE, 
                    onShopPositioned = { shopRect = it }, 
                    onCannonPositioned = { cannonRect = it }, 
                    onNextBubblePositioned = { nextBubbleRect = it }
                )
            }
        }

        GameTopBar(score = score, bestScore = highScore, coins = coins, timeLeft = if (viewModel.gameMode == GameMode.TIME_ATTACK) timeLeft else null, shotsLeft = if (viewModel.gameMode == GameMode.ADVENTURE) (viewModel as? AdventureViewModel)?.shotsRemaining else null, onSettingsClick = { viewModel.togglePause() }, modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().onGloballyPositioned { scoreRect = it.boundsInRoot() })

        if (showQuickShop) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)).clickable { showQuickShop = false }, contentAlignment = Alignment.Center) {
                Surface(modifier = Modifier.width(300.dp).padding(16.dp), shape = RoundedCornerShape(28.dp), color = Color.White) {
                    val brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFF5F5F5)))
                    Column(modifier = Modifier.background(brush).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(id = R.string.shop_title), color = Color(0xFF1A237E), fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.height(20.dp))
                        ItemRow(stringResource(id = R.string.shop_item_fireball), stringResource(id = R.string.shop_item_fireball), 1000, "🔥") { viewModel.buyFireball(); showQuickShop = false }
                        Spacer(Modifier.height(24.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(50)).background(Color(0xFF1A237E)).clickable { showQuickShop = false }, contentAlignment = Alignment.Center) { Text(stringResource(id = R.string.shop_close), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp) }
                    }
                }
            }
        }

        if (gameState == GameState.IDLE) {
            if (viewModel.gameMode == GameMode.ADVENTURE) {
                val advViewModel = viewModel as? AdventureViewModel; val currentLevel = AdventureLevels.levels.find { it.id == advViewModel?.currentLevelId }
                if (currentLevel != null) { AdventureStartDialog(levelId = currentLevel.id, objective = currentLevel.objective, onStartClick = { viewModel.startGame() }) }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
                    val isTimeAttack = viewModel.gameMode == GameMode.TIME_ATTACK; val accentColor = if (isTimeAttack) Color(0xFFFFB74D) else Color(0xFF64FFDA)
                    Surface(modifier = Modifier.width(340.dp).padding(16.dp), shape = RoundedCornerShape(32.dp), color = Color(0xFF0F1444), border = BorderStroke(1.5.dp, Brush.sweepGradient(listOf(accentColor, Color.Transparent, accentColor))), shadowElevation = 24.dp) {
                        Column(modifier = Modifier.padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = if (isTimeAttack) stringResource(id = R.string.mode_time_attack) else stringResource(id = R.string.mode_classic), style = TextStyle(color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, textAlign = TextAlign.Center))
                            Spacer(Modifier.height(16.dp))
                            Text(text = if (isTimeAttack) stringResource(id = R.string.mode_desc_time) else stringResource(id = R.string.mode_desc_classic), color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center, fontSize = 16.sp, lineHeight = 22.sp)
                            if (highScore > 0) {
                                Spacer(Modifier.height(24.dp))
                                Row(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(id = R.string.game_best_label) + ": $highScore", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            Spacer(Modifier.height(40.dp))
                            Button(onClick = { viewModel.startGame() }, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = accentColor), elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)) { Text(stringResource(id = R.string.mode_play_now), color = Color(0xFF0F1444), fontWeight = FontWeight.Black, fontSize = 18.sp) }
                        }
                    }
                }
            }
        }

        if (isPaused && gameState == GameState.PLAYING && !isReviveAlertActive) {
            OverlayMenu(
                title = stringResource(id = R.string.game_pause), 
                onContinue = { viewModel.togglePause() }, 
                onRestart = { viewModel.restartGame() }, 
                onExit = { soundManager.startMusic(); onMenuClick() }, 
                showSettings = true,
                settingsManager = settingsManager,
                onVolumeChange = { vol -> viewModel.setSfxVolume(vol); soundManager.refreshSettings() }
            )
        }

        if (gameState == GameState.WON || gameState == GameState.LOST) {
            OverlayMenu(title = if (gameState == GameState.WON) stringResource(id = R.string.game_victory) else stringResource(id = R.string.game_over), onContinue = null, onRestart = { viewModel.restartGame() }, onExit = { onMenuClick() }, score = score, isWin = gameState == GameState.WON, isAdventure = viewModel.gameMode == GameMode.ADVENTURE, stars = if (viewModel is AdventureViewModel) viewModel.starsEarned else 0, onRedeemCoins = if(!hasRedeemedCoins && currentGameMode != GameMode.ADVENTURE) { { if (score >= 100) { viewModel.addCoins(score / 100); hasRedeemedCoins = true; Toast.makeText(context, context.getString(R.string.game_redeemed), Toast.LENGTH_SHORT).show() } } } else null, onShowAd = if (currentGameMode == GameMode.ADVENTURE && gameState == GameState.WON) null else { { onShowAd { _ -> if (currentGameMode == GameMode.ADVENTURE && gameState == GameState.LOST) { (viewModel as? AdventureViewModel)?.reviveWithAd() } else { viewModel.addCoins(50); Toast.makeText(context, "¡Ganaste 50 monedas!", Toast.LENGTH_SHORT).show() } } } }, currentLevelId = currentLevelId)
        }

        if (viewModel is AdventureViewModel && viewModel.showReviveAlert) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                Surface(modifier = Modifier.width(320.dp).padding(16.dp), shape = RoundedCornerShape(28.dp), color = Color(0xFF1A237E), tonalElevation = 8.dp, shadowElevation = 12.dp, border = BorderStroke(2.dp, Color.White.copy(alpha = 0.15f))) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF64FFDA), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(text = stringResource(id = R.string.adventure_revive_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Text(text = stringResource(id = R.string.adventure_revive_desc), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                        Spacer(Modifier.height(32.dp))
                        Button(onClick = { viewModel.showReviveAlert = false; viewModel.togglePause() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64FFDA)), shape = RoundedCornerShape(16.dp)) { Text(stringResource(id = R.string.adventure_ready), color = Color(0xFF1A237E), fontWeight = FontWeight.Black, fontSize = 16.sp) }
                    }
                }
            }
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
    title: String, onContinue: (() -> Unit)? = null, onRestart: () -> Unit, onExit: () -> Unit, 
    score: Int? = null, isWin: Boolean = false, 
    showSettings: Boolean = false, settingsManager: SettingsManager? = null, 
    onVolumeChange: (Float) -> Unit = {}, onRedeemCoins: (() -> Unit)? = null,
    onShowAd: (() -> Unit)? = null, isAdventure: Boolean = false, stars: Int = 0,
    currentLevelId: Int = 0
) {
    val isPause = title == stringResource(id = R.string.game_pause)
    val accentColor = if (isPause) Color(0xFF64FFDA) else if (isWin) Color(0xFFFFD700) else Color(0xFFFF5252)
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable(enabled = false) {}, 
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, 
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(Color(0xFF080B25).copy(alpha = 0.95f))
                .padding(vertical = 32.dp, horizontal = 24.dp)
        ) {
            Text(text = title, style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = accentColor, letterSpacing = 1.sp, textAlign = TextAlign.Center))
            Spacer(Modifier.height(24.dp))
            
            if (score != null) {
                Text(stringResource(id = R.string.game_score_label), color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("$score", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.height(24.dp))
            }

            if (isAdventure && isWin) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { i ->
                        val scale = remember { Animatable(0f) }
                        LaunchedEffect(Unit) { delay(i * 150L); scale.animateTo(1f, spring(0.6f, 300f)) }
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = if (i < stars) Color(0xFFFFD600) else Color.White.copy(alpha = 0.05f), modifier = Modifier.size(32.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value })
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            if (showSettings && settingsManager != null) {
                val sfxVol by settingsManager.sfxVolumeFlow.collectAsState(1f)
                val colorBlind by settingsManager.colorBlindModeFlow.collectAsState(false)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("SONIDO", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = sfxVol, 
                        onValueChange = { onVolumeChange(it); scope.launch { settingsManager.setSfxVolume(it) } }, 
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("DALTONISMO", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Switch(
                            checked = colorBlind, 
                            onCheckedChange = { scope.launch { settingsManager.setColorBlindMode(it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            onContinue?.let { action ->
                Button(onClick = action, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = accentColor)) { Text(stringResource(id = R.string.game_resume), color = Color(0xFF080B25), fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.sp) }
                Spacer(Modifier.height(16.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRestart, modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f))) { Icon(Icons.Default.Refresh, null, tint = Color.White.copy(alpha = 0.7f)) }
                Spacer(Modifier.width(24.dp))
                val exitIcon = if (isAdventure) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Home
                IconButton(onClick = onExit, modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f))) { Icon(exitIcon, null, tint = Color.White.copy(alpha = 0.7f)) }
            }

            if (!isPause) {
                onShowAd?.let { adAction -> 
                    val adLabel = if (isAdventure && !isWin) stringResource(id = R.string.game_revive_ad) else stringResource(id = R.string.game_bonus_ad)
                    Spacer(Modifier.height(32.dp))
                    TextButton(onClick = adAction) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PlayArrow, null, tint = accentColor, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text(adLabel, color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) } }
                }
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
