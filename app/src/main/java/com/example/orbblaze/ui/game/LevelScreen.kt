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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.orbblaze.ui.menu.ReferenceButton
import com.example.orbblaze.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// Reutilizamos los colores del menú
private val SageGreen = Color(0xFF8DA094)
private val NavyDark = Color(0xFF2D324F)
private val StarGold = Color(0xFFF4C491)

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
    val activeAchievement = viewModel.activeAchievement

    var showQuickShop by remember { mutableStateOf(false) }
    var hasRedeemedCoins by remember { mutableStateOf(false) }
    var isAiming by remember { mutableStateOf(false) }

    var shopRect by remember { mutableStateOf<Rect?>(null) }
    var cannonRect by remember { mutableStateOf<Rect?>(null) }
    var nextBubbleRect by remember { mutableStateOf<Rect?>(null) }
    var scoreRect by remember { mutableStateOf<Rect?>(null) }

    val settingsManager = remember { SettingsManager(context) }
    val isColorBlindMode by settingsManager.colorBlindModeFlow.collectAsState(initial = false)

    val isTutorialCompleted by settingsManager.tutorialCompletedFlow.collectAsState(initial = true)
    var showTutorial by remember { mutableStateOf(false) }

    LaunchedEffect(isTutorialCompleted) {
        if (!isTutorialCompleted) {
            showTutorial = true
        }
    }

    // ✅ CAMBIO DE MÚSICA AL ENTRAR Y VOLVER AL MENÚ AL SALIR
    DisposableEffect(Unit) {
        soundManager.switchToLevelMusic()
        onDispose {
            soundManager.switchToMenuMusic()
        }
    }

    BackHandler(enabled = gameState == GameState.PLAYING && !isPaused) {
        viewModel.togglePause()
    }

    val currentLevelId = (viewModel as? AdventureViewModel)?.currentLevelId ?: 1
    
    // ✅ OPTIMIZACIÓN: Transición infinita única y centralizada
    val infiniteTransition = rememberInfiniteTransition(label = "game_fx")

    // ✅ OPTIMIZACIÓN: El fondo solo se anima si el juego NO está pausado
    val backgroundOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = if (isPaused) snap() else tween(40000, easing = LinearEasing), 
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_scroll"
    )

    // ✅ OPTIMIZACIÓN: Valores de animación para VisualBubble (Centralizados)
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.985f, targetValue = 1.015f,
        animationSpec = infiniteRepeatable(animation = tween(2500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "soft_breath"
    )

    val lightTime by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "light_move"
    )

    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 5000; 0f at 0; 0f at 3000; 1f at 3200; 0f at 3400; 0f at 5000 },
            repeatMode = RepeatMode.Restart
        ), label = "sparkle"
    )

    val indicatorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(600, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "indicator_pulse"
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
            soundManager.forceStartMusic()
        } else if (gameState == GameState.LOST || gameState == GameState.WON) {
            soundManager.stopMusicIntentional()
        } else if (isPaused) {
            soundManager.pauseMusic()
        }
    }

    val dangerAlpha by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 0.8f, animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "danger")
    val masterRainbowRotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "rotation")

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
            .pointerInput(gameState, isPaused, showQuickShop, showTutorial) {
                if (gameState != GameState.PLAYING || isPaused || showQuickShop || showTutorial) return@pointerInput
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
                        viewModel.onShoot(
                            pivotX + (sin(angleRad) * barrelLengthPx).toFloat(), 
                            pivotY - (cos(angleRad) * barrelLengthPx).toFloat()
                        )
                    }
                }
            }
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()
        val bubbleDiameterPx = totalWidth / (columnsCount + 0.5f)
        val verticalSpacingPx = bubbleDiameterPx * 0.866f
        val horizontalSpacingPx = bubbleDiameterPx
        val boardStartPadding = bubbleDiameterPx * 0.5f

        val statusBarHeightPx = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value * density.density
        val boardTopPaddingPx = statusBarHeightPx + with(density) { 90.dp.toPx() }

        val dangerAreaHeightPx = with(density) { 360.dp.toPx() }
        val availableHeight = totalHeight - boardTopPaddingPx - dangerAreaHeightPx
        val finalDangerRow = (availableHeight / verticalSpacingPx).toInt().coerceAtLeast(9)

        LaunchedEffect(finalDangerRow) {
            viewModel.dynamicDangerRow = finalDangerRow
        }

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

        LaunchedEffect(totalWidth, totalHeight, boardTopPaddingPx, columnsCount) {
            val pivotHeightPx = with(density) { 220.dp.toPx() }
            val barrelLengthPx = with(density) { 95.dp.toPx() }
            
            viewModel.setBoardMetrics(BoardMetricsPx(
                horizontalSpacing = horizontalSpacingPx, 
                bubbleDiameter = bubbleDiameterPx, 
                verticalSpacing = verticalSpacingPx, 
                boardTopPadding = boardTopPaddingPx, 
                boardStartPadding = boardStartPadding, 
                ceilingY = boardTopPaddingPx - (bubbleDiameterPx * 0.5f), 
                screenWidth = totalWidth, 
                screenHeight = totalHeight,
                pivotY = totalHeight - pivotHeightPx,
                barrelLength = barrelLengthPx
            ))
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

                if (isAiming) {
                    val bubbleColor = if(isFireballQueued) Color(0xFFFF5722) else mapBubbleColor(currentBubbleColor)
                    viewModel.trajectoryPoints.forEachIndexed { index, point ->
                        val progress = index.mapProgress(viewModel.trajectoryPoints.size)
                        val alpha = (0.7f - progress * 0.4f).coerceIn(0.1f, 0.7f)
                        val radius = (4.dp.toPx() * (1f - progress * 0.2f)).coerceAtLeast(2.dp.toPx())

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(bubbleColor.copy(alpha = alpha), Color.Transparent),
                                center = point,
                                radius = radius * 3f
                            ),
                            radius = radius * 3f,
                            center = point
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = (alpha + 0.2f).coerceAtMost(1f)),
                            radius = radius,
                            center = point
                        )
                    }
                }

                particles.forEach { p -> drawCircle(color = mapBubbleColor(p.color).copy(alpha = p.life), radius = p.size, center = Offset(p.x, p.y)) }
                
                drawIntoCanvas { canvas ->
                    floatingTexts.forEach { ft ->
                        val alpha = (ft.life * 255).toInt().coerceIn(0, 255)
                        val paintOutline = android.graphics.Paint().apply {
                            textSize = 70f; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD
                            color = android.graphics.Color.BLACK; this.alpha = (alpha * 0.6f).toInt()
                            style = android.graphics.Paint.Style.STROKE; strokeWidth = 6f
                        }
                        val paintFill = android.graphics.Paint().apply {
                            textSize = 70f; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD
                            color = android.graphics.Color.WHITE; this.alpha = alpha
                        }
                        canvas.nativeCanvas.drawText(ft.text, ft.x, ft.y, paintOutline)
                        canvas.nativeCanvas.drawText(ft.text, ft.x, ft.y, paintFill)
                    }
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
                    breathingScale = breathingScale,
                    lightTime = lightTime,
                    sparkleScale = sparkleScale,
                    indicatorAlpha = indicatorAlpha,
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
                    breathingScale = 1.1f, // Proyectil un poco más grande
                    lightTime = lightTime,
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
                    onNextBubblePositioned = { nextBubbleRect = it },
                    shakeIntensity = finalShakeIntensity,
                    isDanger = isDangerActive
                )
            }
        }

        GameTopBar(score = score, bestScore = highScore, coins = coins, timeLeft = if (viewModel.gameMode == GameMode.TIME_ATTACK) timeLeft else null, shotsLeft = if (viewModel.gameMode == GameMode.ADVENTURE) (viewModel as? AdventureViewModel)?.shotsRemaining else null, onSettingsClick = { viewModel.togglePause() }, modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().onGloballyPositioned { scoreRect = it.boundsInRoot() })

        if (showQuickShop) {
            QuickShopOverlay(onDismiss = { showQuickShop = false }, onBuyFireball = { viewModel.buyFireball() })
        }

        if (gameState == GameState.IDLE) {
            if (viewModel.gameMode == GameMode.ADVENTURE) {
                val advViewModel = viewModel as? AdventureViewModel; val currentLevel = AdventureLevels.levels.find { it.id == advViewModel?.currentLevelId }
                if (currentLevel != null) { AdventureStartDialog(levelId = currentLevel.id, objective = currentLevel.objective, onStartClick = { viewModel.startGame() }) }
            } else {
                ModeStartOverlay(gameMode = viewModel.gameMode, highScore = highScore, onStart = { viewModel.startGame() })
            }
        }

        if (isPaused && gameState == GameState.PLAYING && !isReviveAlertActive) {
            OverlayMenu(
                title = stringResource(id = R.string.game_pause),
                onContinue = { viewModel.togglePause() },
                onRestart = { viewModel.restartGame() },
                onExit = { soundManager.forceStartMusic(); onMenuClick() },
                showSettings = true,
                settingsManager = settingsManager,
                onVolumeChange = { vol -> viewModel.setSfxVolume(vol); soundManager.refreshSettings() }
            )
        }

        if (gameState == GameState.WON || gameState == GameState.LOST) {
            OverlayMenu(
                title = if (gameState == GameState.WON) stringResource(id = R.string.game_victory) else stringResource(id = R.string.game_over),
                onContinue = null,
                onRestart = { viewModel.restartGame() },
                onNextLevel = if (gameState == GameState.WON && currentGameMode == GameMode.ADVENTURE && currentLevelId < AdventureLevels.levels.size) {
                    { (viewModel as? AdventureViewModel)?.loadAdventureLevel(currentLevelId + 1) }
                } else null,
                onExit = { onMenuClick() },
                score = score,
                isWin = gameState == GameState.WON,
                isAdventure = viewModel.gameMode == GameMode.ADVENTURE,
                stars = if (viewModel is AdventureViewModel) viewModel.starsEarned else 0,
                currentLevelId = currentLevelId,
                onRedeemCoins = if(!hasRedeemedCoins && currentGameMode != GameMode.ADVENTURE) { { if (score >= 100) { viewModel.addCoins(score / 100); hasRedeemedCoins = true; Toast.makeText(context, context.getString(R.string.game_redeemed), Toast.LENGTH_SHORT).show() } } } else null,
                onShowAd = if (currentGameMode == GameMode.ADVENTURE && gameState == GameState.WON) null else { { onShowAd { _ -> if (currentGameMode == GameMode.ADVENTURE && gameState == GameState.LOST) { (viewModel as? AdventureViewModel)?.reviveWithAd() } else { viewModel.addCoins(50); Toast.makeText(context, "¡Ganaste 50 monedas!", Toast.LENGTH_SHORT).show() } } } }
            )
        }

        if (viewModel is AdventureViewModel && viewModel.showReviveAlert) {
            ReviveAlertOverlay(onDismiss = { (viewModel as AdventureViewModel).showReviveAlert = false; viewModel.togglePause() })
        }

        if (showTutorial && gameState == GameState.PLAYING && !isPaused) {
            TutorialDialog(
                shopRect = shopRect,
                cannonRect = cannonRect,
                nextBubbleRect = nextBubbleRect,
                scoreRect = scoreRect,
                onComplete = {
                    coroutineScope.launch {
                        settingsManager.setTutorialCompleted(true)
                        showTutorial = false
                    }
                }
            )
        }

        AchievementNotification(achievement = activeAchievement)
    }
}

private fun Int.mapProgress(total: Int): Float = if (total <= 1) 0f else this.toFloat() / (total - 1)

@Composable
fun AchievementNotification(achievement: Achievement?) {
    AnimatedVisibility(
        visible = achievement != null,
        enter = fadeIn(tween(400)) + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut(tween(400)),
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        label = "achievement_anim"
    ) {
        if (achievement != null) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(28.dp),
                    shadowElevation = 10.dp,
                    modifier = Modifier.widthIn(max = 350.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icono Izquierda (Caja redondeada con estrella)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(StarGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = StarGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        // Texto Central (Título + Descripción)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = achievement.title.uppercase(),
                                style = TextStyle(
                                    color = NavyDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = achievement.description,
                                style = TextStyle(
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        // Estrellita derecha (decorativa)
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = StarGold.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickShopOverlay(onDismiss: () -> Unit, onBuyFireball: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Surface(modifier = Modifier.width(300.dp).padding(16.dp), shape = RoundedCornerShape(28.dp), color = Color.White) {
            val brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFF5F5F5)))
            Column(modifier = Modifier.background(brush).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(id = R.string.shop_title), color = Color(0xFF1A237E), fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(20.dp))
                ItemRow(stringResource(id = R.string.shop_item_fireball), stringResource(id = R.string.shop_item_fireball), 1000, "🔥") { onBuyFireball(); onDismiss() }
                Spacer(Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(50)).background(Color(0xFF1A237E)).clickable { onDismiss() }, contentAlignment = Alignment.Center) { Text(stringResource(id = R.string.shop_close), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp) }
            }
        }
    }
}

@Composable
fun ModeStartOverlay(gameMode: GameMode, highScore: Int, onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.width(320.dp).padding(16.dp),
            shape = RoundedCornerShape(40.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val isTimeAttack = gameMode == GameMode.TIME_ATTACK
                
                Text(
                    text = if (isTimeAttack) "CONTRA TIEMPO" else "MODO CLÁSICO",
                    style = TextStyle(
                        color = NavyDark,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                )
                Box(modifier = Modifier.padding(top = 4.dp).width(60.dp).height(4.dp).clip(CircleShape).background(NavyDark.copy(alpha = 0.1f)))
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = if (isTimeAttack) stringResource(id = R.string.mode_desc_time) else stringResource(id = R.string.mode_desc_classic),
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp
                )
                
                if (highScore > 0) {
                    Spacer(Modifier.height(24.dp))
                    Surface(
                        color = SageGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, SageGreen.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = StarGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(text = "RÉCORD: $highScore", color = SageGreen, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                ReferenceButton(
                    text = "¡JUGAR AHORA!",
                    backgroundColor = SageGreen,
                    contentColor = Color.White,
                    onClick = onStart
                )
            }
        }
    }
}

@Composable
fun ReviveAlertOverlay(onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Surface(modifier = Modifier.width(320.dp).padding(16.dp), shape = RoundedCornerShape(28.dp), color = Color(0xFF1A237E), tonalElevation = 8.dp, shadowElevation = 12.dp, border = BorderStroke(2.dp, Color.White.copy(alpha = 0.15f))) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF64FFDA), modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text(text = stringResource(id = R.string.adventure_revive_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(text = stringResource(id = R.string.adventure_revive_desc), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                Spacer(Modifier.height(32.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64FFDA)), shape = RoundedCornerShape(16.dp)) { Text(stringResource(id = R.string.adventure_ready), color = Color(0xFF1A237E), fontWeight = FontWeight.Black, fontSize = 16.sp) }
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
    currentLevelId: Int = 0,
    onNextLevel: (() -> Unit)? = null
) {
    val isPause = title == stringResource(id = R.string.game_pause)
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(40.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier.width(320.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TÍTULO
                Text(
                    text = title.uppercase(),
                    style = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = NavyDark,
                        letterSpacing = 1.sp
                    )
                )
                Box(modifier = Modifier.padding(top = 4.dp).width(60.dp).height(4.dp).clip(CircleShape).background(NavyDark.copy(alpha = 0.1f)))
                
                Spacer(Modifier.height(24.dp))

                if (score != null) {
                    Text("PUNTUACIÓN", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("$score", color = NavyDark, fontSize = 48.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(24.dp))
                }

                if (isAdventure && isWin && stars > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { i ->
                            val scale = remember { Animatable(0f) }
                            LaunchedEffect(Unit) { delay(i * 150L); scale.animateTo(1f, spring(0.6f, 300f)) }
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i < stars) StarGold else Color.LightGray.copy(alpha = 0.3f),
                                modifier = Modifier.size(32.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                if (showSettings && settingsManager != null) {
                    val sfxVol by settingsManager.sfxVolumeFlow.collectAsState(1f)
                    val colorBlind by settingsManager.colorBlindModeFlow.collectAsState(false)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("SONIDO", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = sfxVol, 
                            onValueChange = { onVolumeChange(it); scope.launch { settingsManager.setSfxVolume(it) } },
                            colors = SliderDefaults.colors(thumbColor = SageGreen, activeTrackColor = SageGreen, inactiveTrackColor = SageGreen.copy(alpha = 0.2f))
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("DALTONISMO", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Switch(
                                checked = colorBlind,
                                onCheckedChange = { scope.launch { settingsManager.setColorBlindMode(it) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SageGreen)
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // BOTÓN CONTINUAR / SIGUIENTE
                val mainButtonAction = onContinue ?: onNextLevel
                val mainButtonText = if (onContinue != null) stringResource(id = R.string.game_resume) else stringResource(id = R.string.game_next_level)
                
                if (mainButtonAction != null) {
                    ReferenceButton(
                        text = mainButtonText,
                        backgroundColor = SageGreen,
                        contentColor = Color.White,
                        onClick = mainButtonAction
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // FILA DE ACCIONES (REINICIAR | SALIR)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReferenceButton(
                        text = "",
                        backgroundColor = Color.White,
                        contentColor = Color.Gray,
                        icon = Icons.Default.Refresh,
                        iconColor = Color.Gray,
                        modifier = Modifier.weight(1f),
                        onClick = onRestart
                    )
                    
                    val exitIcon = if (isAdventure) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Home
                    ReferenceButton(
                        text = "",
                        backgroundColor = Color.White,
                        contentColor = Color.Gray,
                        icon = exitIcon,
                        iconColor = Color.Gray,
                        modifier = Modifier.weight(1f),
                        onClick = onExit
                    )
                }

                if (!isPause) {
                    onShowAd?.let { adAction ->
                        val adLabel = if (isAdventure && !isWin) stringResource(id = R.string.game_revive_ad) else stringResource(id = R.string.game_bonus_ad)
                        Spacer(Modifier.height(24.dp))
                        TextButton(onClick = adAction) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(adLabel, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                    }
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
