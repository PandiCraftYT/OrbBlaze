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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
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
    
    // Obtenemos los récords reactivamente desde DataStore
    val highScore by settingsManager.highScoreFlow.collectAsState(initial = 0)
    val highScoreTime by settingsManager.highScoreTimeFlow.collectAsState(initial = 0)
    
    // Lista completa de modos y sus récords
    val records = remember(highScore, highScoreTime) {
        listOf(
            Triple("CLÁSICO", highScore, Color(0xFFFFD700)),
            Triple("CONTRA TIEMPO", highScoreTime, Color(0xFF64FFDA)),
            Triple("MODO AVENTURA", 0, Color(0xFF03DAC5)),
            Triple("MODO INVERSA", 0, Color(0xFFFF4D4D)),
            Triple("PUZZLE DIARIO", 0, Color(0xFFBB86FC)),
            Triple("MINIJUEGOS", 0, Color(0xFFFF8A65))
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "score_animations")
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
                        bubble.x += bubble.vx; bubble.y += bubble.vy; bubble.vy += gravity
                        if (bubble.y > screenHeightPx + bubble.radius * 2) {
                            bubble.y = -bubble.radius; bubble.x = Random.nextFloat() * screenWidthPx
                            bubble.vy = Random.nextFloat() * 2f
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
                            if (dist <= bubble.radius * 1.5f) {
                                soundManager.play(SoundType.POP); bubble.vy = -20f; bubble.vx = (Random.nextFloat() * 10f - 5f)
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
                    drawCircle(brush = Brush.radialGradient(listOf(bubble.color.copy(alpha = 0.9f), bubble.color.copy(alpha = 0.4f)), center = center, radius = radius), radius = radius, center = center)
                    drawCircle(color = Color.White.copy(alpha = 0.3f), radius = radius * 0.3f, center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f))
                    drawCircle(color = Color.White.copy(alpha = 0.5f), radius = radius, center = center, style = Stroke(width = 2f))
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                
                Text(
                    text = "RECORD PERSONAL",
                    style = TextStyle(
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp,
                        shadow = Shadow(color = Color.Black, offset = Offset(4f, 4f), blurRadius = 10f)
                    ),
                    modifier = Modifier.graphicsLayer { 
                        scaleX = titleScale
                        scaleY = titleScale
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // TABLA DE RÉCORDS CON SCROLL
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(records) { (modo, score, color) ->
                        RecordRowItem(modo, score, color) {
                            if (score == 0) {
                                Toast.makeText(context, "Aún no tienes récord personal en este modo de juego", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // BOTÓN VOLVER (ICONO HOME CIRCULAR)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF64FFDA).copy(alpha = 0.2f))
                        .border(2.dp, Color.White, RoundedCornerShape(50))
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home, 
                        contentDescription = null, 
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "v1.0 - OrbBlaze", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun RecordRowItem(
    mode: String,
    score: Int,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
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
                    style = TextStyle(color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                )
                Text(
                    text = "MEJOR PUNTAJE",
                    style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
            }
            
            Text(
                text = if (score > 0) "$score" else "-",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f)
                )
            )
        }
    }
}
