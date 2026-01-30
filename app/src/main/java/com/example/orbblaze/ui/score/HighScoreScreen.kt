package com.example.orbblaze.ui.score

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.menu.PhysicsBubble
import com.example.orbblaze.ui.theme.*
import kotlinx.coroutines.isActive
import kotlin.math.hypot
import kotlin.random.Random

@Composable
fun HighScoreScreen(
    soundManager: SoundManager, // ✅ Necesario para el sonido de explosión
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("orbblaze_prefs", Context.MODE_PRIVATE) }
    val highScore = prefs.getInt("high_score", 0)
    val infiniteTransition = rememberInfiniteTransition(label = "score_animations")

    // Animación de escala para el título
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_scale"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
            .systemBarsPadding()
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        var frameTick by remember { mutableLongStateOf(0L) }
        val bubbleColors = listOf(BubbleRed, BubbleBlue, BubbleGreen, BubbleYellow, BubblePurple, BubbleCyan)

        val physicsBubbles = remember(screenWidthPx, screenHeightPx) {
            List(15) {
                PhysicsBubble(
                    x = Random.nextFloat() * screenWidthPx,
                    y = Random.nextFloat() * screenHeightPx,
                    vx = Random.nextFloat() * 2f - 1f,
                    vy = Random.nextFloat() * -10f - 5f,
                    radius = with(density) { Random.nextInt(15, 30).dp.toPx() },
                    color = bubbleColors.random()
                )
            }
        }

        LaunchedEffect(screenWidthPx, screenHeightPx) {
            val gravity = 0.25f
            while (isActive) {
                withFrameNanos { frameTime ->
                    frameTick = frameTime
                    physicsBubbles.forEach { bubble ->
                        bubble.x += bubble.vx
                        bubble.y += bubble.vy
                        bubble.vy += gravity
                        if (bubble.y > screenHeightPx + bubble.radius * 2) {
                            bubble.y = -bubble.radius
                            bubble.x = Random.nextFloat() * screenWidthPx
                            bubble.vy = Random.nextFloat() * 2f
                        }
                        if (bubble.x < -bubble.radius) bubble.x = screenWidthPx + bubble.radius
                        if (bubble.x > screenWidthPx + bubble.radius) bubble.x = -bubble.radius
                    }
                }
            }
        }

        // Capa de fondo interactiva
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(screenWidthPx, screenHeightPx) {
                    detectTapGestures { offset ->
                        physicsBubbles.forEach { bubble ->
                            val dist = hypot(offset.x - bubble.x, offset.y - bubble.y)
                            if (dist <= bubble.radius * 1.5f) {
                                soundManager.play(SoundType.POP)
                                bubble.vy = -20f
                                bubble.vx = (Random.nextFloat() * 10f - 5f)
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                @Suppress("UNUSED_VARIABLE")
                val t = frameTick 
                physicsBubbles.forEach { bubble ->
                    val center = Offset(bubble.x, bubble.y)
                    val radius = bubble.radius
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(bubble.color.copy(alpha = 0.9f), bubble.color.copy(alpha = 0.4f)),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                    drawCircle(color = Color.White.copy(alpha = 0.3f), radius = radius * 0.3f, center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f))
                    drawCircle(color = Color.White.copy(alpha = 0.5f), radius = radius, center = center, style = Stroke(width = 2f))
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "MEJOR PUNTUACIÓN",
                    style = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f)
                    ),
                    modifier = Modifier.graphicsLayer { scaleX = titleScale; scaleY = titleScale }
                )

                Spacer(modifier = Modifier.height(60.dp))

                Text(
                    text = "$highScore",
                    style = TextStyle(
                        fontSize = 90.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700),
                        shadow = Shadow(color = Color.Black, offset = Offset(4f, 4f), blurRadius = 12f),
                        letterSpacing = 2.sp
                    )
                )

                Text(
                    text = "PUNTOS",
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f), letterSpacing = 4.sp)
                )

                Spacer(modifier = Modifier.height(80.dp))

                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(50))
                        .border(2.dp, Color.White, RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "VOLVER", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "v1.0 - OrbBlaze",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
