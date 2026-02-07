package com.example.orbblaze.ui.components

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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.dp
import com.example.orbblaze.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PandaShooter(
    angle: Float,
    currentBubbleColor: Color,
    isCurrentRainbow: Boolean = false,
    nextBubbleColor: Color,
    isNextRainbow: Boolean = false,
    shotTick: Int,
    joyTick: Int = 0,
    rainbowRotation: Float,
    onShopClick: () -> Unit = {},
    isShopEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onShopPositioned: (Rect) -> Unit = {},
    onCannonPositioned: (Rect) -> Unit = {},
    onNextBubblePositioned: (Rect) -> Unit = {}
) {
    val recoilAnim = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }
    val joyAnim = remember { Animatable(0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "panda_fx")

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

    val recoilOffset = recoilAnim.value * 40f
    val joyJump = joyAnim.value * -20f

    Box(
        modifier = modifier.fillMaxWidth().height(320.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. NUBES DE FONDO
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cloudColor = Color.White
            drawRect(color = cloudColor, topLeft = Offset(0f, size.height - 100.dp.toPx()), size = Size(size.width, 250.dp.toPx()))
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
            ShopButton(
                onClick = onShopClick,
                isEnabled = isShopEnabled // ✅ Pasamos el estado
            )
        }

        // 3. PANDA ASISTENTE
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
        ) {
            VisualBubble(color = nextBubbleColor, isRainbow = isNextRainbow, rainbowRotation = rainbowRotation, modifier = Modifier.size(34.dp))
        }

        // 5. CAÑÓN SUPREMO
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-50).dp)
                .onGloballyPositioned { onCannonPositioned(it.boundsInRoot()) }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = center.x
                val cy = center.y
                val steelDark = Color(0xFF0F171E)
                val steelMid = Color(0xFF2C3E50)
                val steelLight = Color(0xFF7F8C8D)
                val goldColor = Color(0xFFFFD700)
                val pivot = Offset(cx, cy + 50f)

                val baseBrush = Brush.verticalGradient(listOf(steelMid, steelDark))
                drawRoundRect(baseBrush, Offset(cx - 75f, cy + 25f), Size(150f, 65f), CornerRadius(15f))

                listOf(-70f, 70f).forEach { xOff ->
                    drawCircle(steelDark, 32f, Offset(cx + xOff, cy + 55f))
                    drawCircle(currentBubbleColor.copy(alpha = 0.4f), 28f, Offset(cx + xOff, cy + 55f), style = Stroke(5f))
                    drawCircle(currentBubbleColor, 14f, Offset(cx + xOff, cy + 55f))
                    drawCircle(Color.White.copy(0.6f), 4f, Offset(cx + xOff - 6f, cy + 50f))
                }

                withTransform({
                    rotate(angle, pivot)
                    translate(0f, recoilOffset)
                }) {
                    val barrelW = 110f
                    val barrelH = 185f
                    val metalGradient = Brush.horizontalGradient(
                        0.0f to steelDark, 0.15f to steelMid, 0.5f to steelLight, 0.85f to steelMid, 1.0f to steelDark
                    )

                    val path = Path().apply {
                        moveTo(cx - barrelW/2, cy + 55f); lineTo(cx + barrelW/2, cy + 55f)
                        lineTo(cx + barrelW*0.42f, cy - barrelH + 30f); lineTo(cx - barrelW*0.42f, cy - barrelH + 30f)
                        close()
                    }
                    drawPath(path, metalGradient)
                    
                    drawRect(
                        brush = Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(0.2f), Color.Transparent)),
                        topLeft = Offset(cx - 10f, cy - barrelH + 30f),
                        size = Size(20f, barrelH + 25f)
                    )

                    drawRoundRect(Color(0xFF8B6B00), Offset(cx - barrelW/2 - 4f, cy + 10f), Size(barrelW + 6f, 22f), CornerRadius(5f))
                    drawRoundRect(goldColor, Offset(cx - barrelW/2 - 2f, cy + 10f), Size(barrelW + 4f, 18f), CornerRadius(4f))
                    drawRect(goldColor, Offset(cx - barrelW*0.38f, cy - 100f), Size(barrelW*0.76f, 10f))

                    drawOval(metalGradient, Offset(cx - barrelW*0.45f, cy - barrelH), Size(barrelW*0.9f, 45f))
                    drawOval(Color.Black, Offset(cx - barrelW*0.45f + 16f, cy - barrelH + 10f), Size(barrelW*0.9f - 32f, 25f))

                    drawCircle(Brush.radialGradient(listOf(steelLight, steelDark)), 32f, Offset(cx, cy + 65f))

                    if (flashAlpha.value > 0f) {
                        drawCircle(
                            brush = Brush.radialGradient(listOf(Color.White, goldColor, Color.Transparent)),
                            radius = 150f * flashAlpha.value,
                            center = Offset(cx, cy - barrelH + 10f)
                        )
                    }
                }
            }
        }
    }
}

fun DrawScope.drawCloud(center: Offset, size: Float, color: Color) {
    drawOval(color, topLeft = Offset(center.x - size * 1.4f, center.y - size * 0.35f), size = Size(size * 2.8f, size * 0.8f))
    drawCircle(color, radius = size * 0.8f, center = center.copy(y = center.y - size * 0.3f))
    drawCircle(color, radius = size * 0.65f, center = center.copy(x = center.x - size * 0.8f, y = center.y - size * 0.1f))
    drawCircle(color, radius = size * 0.65f, center = center.copy(x = center.x + size * 0.8f, y = center.y - size * 0.1f))
}
