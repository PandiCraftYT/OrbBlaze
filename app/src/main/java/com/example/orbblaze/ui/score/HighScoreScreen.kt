package com.example.orbblaze.ui.score

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.menu.LocalFontScale
import com.example.orbblaze.ui.menu.PhysicsBubble
import com.example.orbblaze.ui.theme.*
import kotlinx.coroutines.isActive
import kotlin.math.hypot
import kotlin.random.Random

@Composable
fun HighScoreScreen(
    soundManager: SoundManager,
    settingsManager: SettingsManager,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val highScore by settingsManager.highScoreFlow.collectAsState(initial = 0)
    val highScoreTime by settingsManager.highScoreTimeFlow.collectAsState(initial = 0)
    
    val records = remember(highScore, highScoreTime) {
        listOf(
            Triple("MODO CLÁSICO", highScore, Color(0xFF00E676)),
            Triple("CONTRA TIEMPO", highScoreTime, Color(0xFF40C4FF)),
            Triple("MODO AVENTURA", 0, Color(0xFFFFD700)),
            Triple("MODO INVERSA", 0, Color(0xFFFF5252)),
            Triple("PUZZLE DIARIO", 0, Color(0xFFBB86FC)),
            Triple("MINIJUEGOS", 0, Color(0xFFFF8A65))
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "score_animations")
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_scale"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        val fontScale = (maxWidth.value / 411f).coerceIn(0.6f, 1.5f)
        
        CompositionLocalProvider(LocalFontScale provides fontScale) {
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
                        vx = Random.nextFloat() * 1.5f - 0.75f,
                        vy = Random.nextFloat() * -8f - 2f,
                        radius = with(density) { Random.nextInt(10, 40).dp.toPx() },
                        color = bubbleColors.random()
                    )
                }
            }

            LaunchedEffect(screenWidthPx, screenHeightPx) {
                val gravity = 0.15f
                while (isActive) {
                    withFrameNanos { frameTime ->
                        frameTick = frameTime
                        physicsBubbles.forEach { bubble ->
                            bubble.x += bubble.vx; bubble.y += bubble.vy; bubble.vy += gravity
                            if (bubble.y > screenHeightPx + bubble.radius * 2) {
                                bubble.y = -bubble.radius; bubble.x = Random.nextFloat() * screenWidthPx
                                bubble.vy = Random.nextFloat() * 1.5f
                            }
                            if (bubble.x < -bubble.radius) bubble.x = screenWidthPx + bubble.radius
                            if (bubble.x > screenWidthPx + bubble.radius) bubble.x = -bubble.radius
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(screenWidthPx, screenHeightPx) {
                        detectTapGestures { offset ->
                            physicsBubbles.forEach { bubble ->
                                val dist = hypot(offset.x - bubble.x, offset.y - bubble.y)
                                if (dist <= bubble.radius * 1.8f) {
                                    soundManager.play(SoundType.POP); bubble.vy = -25f; bubble.vx = (Random.nextFloat() * 12f - 6f)
                                }
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    @Suppress("UNUSED_VARIABLE")
                    val t = frameTick 
                    physicsBubbles.forEach { bubble ->
                        val center = Offset(bubble.x, bubble.y); val radius = bubble.radius
                        drawCircle(brush = Brush.radialGradient(listOf(bubble.color.copy(alpha = 0.8f), bubble.color.copy(alpha = 0.2f)), center = center, radius = radius), radius = radius, center = center)
                        drawCircle(color = Color.White.copy(alpha = 0.4f), radius = radius * 0.35f, center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f))
                        drawCircle(color = Color.White.copy(alpha = 0.2f), radius = radius, center = center, style = Stroke(width = 1.5f))
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // ✅ TÍTULO PREMIUM
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "RÉCORDS",
                            maxLines = 1,
                            style = TextStyle(
                                fontSize = (42 * fontScale).sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (2 * fontScale).sp
                            ),
                            modifier = Modifier.graphicsLayer { scaleX = titleScale; scaleY = titleScale }
                        )
                        Box(modifier = Modifier.width((100 * fontScale).dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                    }

                    // ✅ LISTA DE RÉCORDS (Sólida y moderna)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(records) { (modo, score, color) ->
                            RecordCardPremium(modo, score, color) {
                                if (score == 0) Toast.makeText(context, "¡Juega para establecer un récord!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    // ✅ BOTÓN VOLVER (Elevado para anuncio)
                    Box(
                        modifier = Modifier
                            .padding(bottom = 55.dp)
                            .width(200.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("CERRAR", color = Color.Gray, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordCardPremium(mode: String, score: Int, color: Color, onClick: () -> Unit) {
    val fontScale = LocalFontScale.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((75 * fontScale).dp.coerceAtLeast(60.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(2.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = mode,
                    style = TextStyle(color = color, fontSize = (16 * fontScale).sp, fontWeight = FontWeight.Black)
                )
                Text(
                    text = "PUNTUACIÓN MÁXIMA",
                    style = TextStyle(color = Color.Gray.copy(alpha = 0.6f), fontSize = (10 * fontScale).sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                )
            }
            
            Text(
                text = if (score > 0) "$score" else "-",
                style = TextStyle(
                    color = Color(0xFF1A237E),
                    fontSize = (26 * fontScale).sp,
                    fontWeight = FontWeight.Black
                )
            )
        }
    }
}
