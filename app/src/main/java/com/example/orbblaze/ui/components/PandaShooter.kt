package com.example.orbblaze.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.orbblaze.domain.model.BubbleColor
import com.example.orbblaze.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PandaShooter(
    angle: Float,
    currentBubbleColor: Color,
    currentBubbleType: BubbleColor? = null, // ✅ Añadido
    isCurrentRainbow: Boolean = false,
    nextBubbleColor: Color,
    nextBubbleType: BubbleColor? = null, // ✅ Añadido
    isNextRainbow: Boolean = false,
    shotTick: Int,
    joyTick: Int = 0,
    rainbowRotation: Float,
    onShopClick: () -> Unit = {},
    isShopEnabled: Boolean = true,
    isColorBlindMode: Boolean = false, // ✅ Añadido
    modifier: Modifier = Modifier,
    onShopPositioned: (Rect) -> Unit = {},
    onCannonPositioned: (Rect) -> Unit = {},
    onNextBubblePositioned: (Rect) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val recoilAnim = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }
    val joyAnim = remember { Animatable(0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "panda_fx")

    val bombColor = Color(0xFF212121)
    val isCurrentBomb = currentBubbleType == BubbleColor.BOMB
    val isNextBomb = nextBubbleType == BubbleColor.BOMB

    // ✅ LÓGICA DE VIBRACIÓN
    var lastVibratedAngle by remember { mutableFloatStateOf(angle) }
    LaunchedEffect(angle) {
        if (kotlin.math.abs(angle - lastVibratedAngle) >= 3f) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastVibratedAngle = angle
        }
    }

    // ✅ ANIMACIÓN DE INTERCAMBIO
    val swapAnim = remember { Animatable(0f) }
    var lastShotTick by remember { mutableIntStateOf(shotTick) }
    LaunchedEffect(currentBubbleColor) {
        if (shotTick == lastShotTick) { 
            swapAnim.snapTo(1f)
            swapAnim.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 400f))
        }
        lastShotTick = shotTick
    }

    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )

    val blinkScaleY by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4500
                1.0f at 0; 1.0f at 4300; 0.1f at 4400; 1.0f at 4500
            }
        ), label = "blink"
    )

    LaunchedEffect(shotTick) {
        if (shotTick > 0) {
            recoilAnim.snapTo(0f)
            flashAlpha.snapTo(1f)
            launch { flashAlpha.animateTo(0f, tween(150)) }
            recoilAnim.animateTo(1f, tween(60, easing = LinearEasing))
            recoilAnim.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 300f))
        }
    }

    LaunchedEffect(joyTick) {
        if (joyTick > 0) {
            joyAnim.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
            joyAnim.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 200f))
        }
    }

    val recoilOffset = recoilAnim.value * 45f
    val joyJump = joyAnim.value * -20f
    val rainbowColors = listOf(Color.Red, Color(0xFFFF7F00), Color.Yellow, Color.Green, Color.Blue, Color(0xFF4B0082), Color(0xFF8B00FF))

    Box(
        modifier = modifier.fillMaxWidth().height(340.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. NUBES
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cloudColor = Color.White
            drawCloud(Offset(size.width / 2, size.height - 125.dp.toPx()), 260f, cloudColor)
            drawCloud(Offset(size.width - 85.dp.toPx(), size.height - 90.dp.toPx()), 180f, cloudColor)
            drawCloud(Offset(85.dp.toPx(), size.height - 90.dp.toPx()), 180f, cloudColor)
        }

        // 2. BOTÓN TIENDA
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 60.dp, y = (-95).dp)
                .graphicsLayer { alpha = if (isShopEnabled) 1f else 0.4f }
                .onGloballyPositioned { onShopPositioned(it.boundsInRoot()) }
        ) {
            ShopButton(onClick = onShopClick, isEnabled = isShopEnabled)
        }

        // 3. PANDA
        Canvas(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (15).dp, y = (-35).dp + joyJump.dp)
        ) {
            val cx = size.width / 2
            val cy = size.height / 2
            val pBlack = Color(0xFF1A1A1A)
            val pWhite = Color.White
            val headCy = cy - 40f
            withTransform({
                scale(breatheScale, breatheScale, Offset(cx, cy + 40f))
                if (joyAnim.value > 0.1f) rotate(joyAnim.value * 10f, Offset(cx, cy))
            }) {
                drawCircle(pBlack, radius = 22f, center = Offset(cx - 50f, headCy - 40f))
                drawCircle(pBlack, radius = 22f, center = Offset(cx + 50f, headCy - 40f))
                drawRoundRect(pBlack, topLeft = Offset(cx - 65f, cy - 20f), size = Size(130f, 100f), cornerRadius = CornerRadius(40f))
                drawCircle(pWhite, radius = 42f, center = Offset(cx, cy + 30f))
                drawRoundRect(pWhite, topLeft = Offset(cx - 65f, headCy - 55f), size = Size(130f, 100f), cornerRadius = CornerRadius(50f))
                drawCircle(pBlack, 22f, Offset(cx - 32f, headCy - 5f))
                drawCircle(pBlack, 22f, Offset(cx + 32f, headCy - 5f))
                drawCircle(Color.White, radius = 4f * blinkScaleY, center = Offset(cx - 32f, headCy - 5f))
                drawCircle(Color.White, radius = 4f * blinkScaleY, center = Offset(cx + 32f, headCy - 5f))
                drawCircle(pBlack, 6f, Offset(cx, headCy + 15f))
                val armAngleL = if (joyAnim.value > 0.1f) -60f else 20f
                val armAngleR = if (joyAnim.value > 0.1f) 60f else -20f
                rotate(armAngleL, Offset(cx - 40f, cy + 10f)) { drawOval(pBlack, topLeft = Offset(cx - 70f, cy), size = Size(35f, 55f)) }
                rotate(armAngleR, Offset(cx + 40f, cy + 10f)) { drawOval(pBlack, topLeft = Offset(cx + 35f, cy), size = Size(35f, 55f)) }
            }
        }

        // 4. BURBUJA NEXT
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-35).dp, y = (-112).dp + joyJump.dp)
                .onGloballyPositioned { onNextBubblePositioned(it.boundsInRoot()) }
                .graphicsLayer {
                    scaleX = 0.5f + (1f - swapAnim.value) * 0.5f
                    scaleY = 0.5f + (1f - swapAnim.value) * 0.5f
                    alpha = 1f - swapAnim.value * 0.5f
                }
        ) {
            VisualBubble(
                color = nextBubbleColor, 
                isRainbow = isNextRainbow, 
                isBomb = isNextBomb,
                rainbowRotation = rainbowRotation, 
                isColorBlindMode = isColorBlindMode, // ✅ Implementado
                bubbleColorType = nextBubbleType, // ✅ Implementado
                modifier = Modifier.size(34.dp)
            )
        }

        // 5. CAÑÓN REDISEÑADO
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-40).dp)
                .onGloballyPositioned { onCannonPositioned(it.boundsInRoot()) }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = center.x
                val cy = center.y
                val steelDark = Color(0xFF121212)
                val steelMid = Color(0xFF2C2C2C)
                val steelLight = Color(0xFF4A4A4A)
                val accentGold = Color(0xFFFFD700)
                val pivot = Offset(cx, cy + 80f)

                // Base Estática
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(steelMid, Color.Black)),
                    topLeft = Offset(cx - 90f, cy + 60f),
                    size = Size(180f, 50f),
                    cornerRadius = CornerRadius(15f)
                )

                // Engranajes Laterales con Figuras de Daltonismo
                listOf(-85f, 85f).forEach { xOff ->
                    val gearCenter = Offset(cx + xOff, cy + 80f)
                    drawCircle(Color.Black, 38f, gearCenter)
                    rotate(angle * 1.5f, gearCenter) {
                        drawCircle(accentGold, 32f, gearCenter, style = Stroke(5f))
                        repeat(8) { i ->
                            rotate(i * 45f, gearCenter) {
                                drawRect(accentGold, Offset(gearCenter.x - 5f, gearCenter.y - 38f), Size(10f, 12f))
                            }
                        }
                    }
                    
                    // Indicador de color y figura
                    if (isCurrentRainbow) {
                        rotate(rainbowRotation, gearCenter) {
                            drawCircle(brush = Brush.sweepGradient(rainbowColors, gearCenter), radius = 14f, center = gearCenter)
                        }
                    } else if (isCurrentBomb) {
                        drawCircle(Color.Black, radius = 14f, center = gearCenter)
                        drawCircle(Color.White.copy(0.4f), radius = 4f, center = gearCenter - Offset(4f, 4f))
                    } else {
                        drawCircle(currentBubbleColor, 14f, gearCenter)
                        // ✅ DIBUJAR FIGURA EN EL ENGRANAJE
                        if (isColorBlindMode && currentBubbleType != null) {
                            drawColorBlindIcon(currentBubbleType, gearCenter, 8f)
                        }
                    }
                }

                withTransform({
                    rotate(angle, pivot)
                    translate(0f, recoilOffset)
                }) {
                    val barrelW = 105f
                    val barrelH = 190f
                    val barrelGradient = Brush.horizontalGradient(
                        0.0f to steelDark, 0.2f to steelMid, 0.5f to steelLight, 0.8f to steelMid, 1.0f to steelDark
                    )

                    drawCircle(steelMid, 55f, Offset(cx, cy + 80f))

                    // Cuerpo del Cañón
                    drawRoundRect(
                        brush = barrelGradient,
                        topLeft = Offset(cx - barrelW/2, cy - barrelH + 80f),
                        size = Size(barrelW, barrelH),
                        cornerRadius = CornerRadius(12f)
                    )

                    // Anillos de Refuerzo
                    drawRoundRect(accentGold, Offset(cx - barrelW/2 - 5f, cy + 10f), Size(barrelW + 10f, 18f), CornerRadius(5f))
                    drawRoundRect(accentGold, Offset(cx - barrelW/2 - 5f, cy - 60f), Size(barrelW + 10f, 18f), CornerRadius(5f))

                    // Boca del Cañón
                    val mouthY = cy - barrelH + 80f
                    drawOval(steelMid, Offset(cx - barrelW/2, mouthY - 22f), Size(barrelW, 44f))
                    drawOval(Color.Black, Offset(cx - barrelW/2 + 12f, mouthY - 16f), Size(barrelW - 24f, 32f))

                    // Flash de Disparo
                    if (flashAlpha.value > 0f) {
                        drawCircle(
                            brush = Brush.radialGradient(listOf(Color.White, accentGold, Color.Transparent)),
                            radius = 200f * flashAlpha.value,
                            center = Offset(cx, mouthY - 10f)
                        )
                    }
                }
            }
        }
    }
}

// ✅ IMPORTADO DESDE VISUALBUBBLE PARA CONSISTENCIA
private fun DrawScope.drawColorBlindIcon(type: BubbleColor, center: Offset, size: Float) {
    val iconColor = Color.White.copy(alpha = 0.8f)
    val strokeWidth = 1.5.dp.toPx()
    
    when (type) {
        BubbleColor.RED -> drawCircle(iconColor, radius = size, center = center, style = Stroke(strokeWidth))
        BubbleColor.BLUE -> drawRect(iconColor, topLeft = Offset(center.x - size, center.y - size), size = Size(size * 2, size * 2), style = Stroke(strokeWidth))
        BubbleColor.GREEN -> {
            val path = Path().apply {
                moveTo(center.x, center.y - size); lineTo(center.x + size, center.y + size); lineTo(center.x - size, center.y + size); close()
            }
            drawPath(path, iconColor, style = Stroke(strokeWidth))
        }
        BubbleColor.YELLOW -> {
            val path = Path().apply {
                moveTo(center.x, center.y - size); lineTo(center.x + size, center.y); lineTo(center.x, center.y + size); lineTo(center.x - size, center.y); close()
            }
            drawPath(path, iconColor, style = Stroke(strokeWidth))
        }
        BubbleColor.PURPLE -> {
            drawLine(iconColor, Offset(center.x - size, center.y), Offset(center.x + size, center.y), strokeWidth, StrokeCap.Round)
            drawLine(iconColor, Offset(center.x, center.y - size), Offset(center.x, center.y + size), strokeWidth, StrokeCap.Round)
        }
        BubbleColor.CYAN -> {
            val path = Path().apply {
                for (i in 0..5) {
                    val angle = i * Math.PI / 3
                    val x = center.x + size * kotlin.math.cos(angle).toFloat()
                    val y = center.y + size * kotlin.math.sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(path, iconColor, style = Stroke(strokeWidth))
        }
        else -> {}
    }
}

fun DrawScope.drawCloud(center: Offset, size: Float, color: Color) {
    drawOval(color, topLeft = Offset(center.x - size * 1.4f, center.y - size * 0.35f), size = Size(size * 2.8f, size * 0.8f))
    drawCircle(color, radius = size * 0.8f, center = center.copy(y = center.y - size * 0.3f))
    drawCircle(color, radius = size * 0.65f, center = center.copy(x = center.x - size * 0.8f, y = center.y - size * 0.1f))
    drawCircle(color, radius = size * 0.65f, center = center.copy(x = center.x + size * 0.8f, y = center.y - size * 0.1f))
}
