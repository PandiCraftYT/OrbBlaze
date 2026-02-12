package com.example.orbblaze.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.theme.*
import kotlinx.coroutines.isActive
import kotlin.math.hypot
import kotlin.random.Random

@Composable
fun MenuScreen(
    onPlayClick: () -> Unit,
    onModesClick: () -> Unit,
    onScoreClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit,
    soundManager: SoundManager,
    onSecretClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "menu_animations")
    var showExitDialog by remember { mutableStateOf(false) }

    // ✅ CONTROL DE BOTÓN ATRÁS (ANDROID) - Mantiene la funcionalidad de salir
    BackHandler {
        showExitDialog = true
    }
    
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "title_scale"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BgTop, BgBottom)))
            .systemBarsPadding()
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        var frameTick by remember { mutableStateOf(0L) }
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(screenWidthPx, screenHeightPx) {
                    detectTapGestures { offset ->
                        physicsBubbles.forEach { bubble ->
                            val dist = hypot(offset.x - bubble.x, offset.y - bubble.y)
                            if (dist <= bubble.radius * 1.5f) {
                                soundManager.play(SoundType.POP)
                                onSecretClick()
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
                    drawCircle(
                        color = Color.White.copy(alpha = 0.3f),
                        radius = radius * 0.3f,
                        center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ORBBLAZE",
                    style = TextStyle(
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 4.sp,
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(4f, 4f), blurRadius = 10f)
                    ),
                    modifier = Modifier.graphicsLayer {
                        scaleX = titleScale
                        scaleY = titleScale
                    }
                )

                Spacer(modifier = Modifier.height(50.dp))

                MenuButton(text = "JUGAR", onClick = onPlayClick)
                Spacer(modifier = Modifier.height(14.dp))
                MenuButton(text = "MODOS DE JUEGO", onClick = onModesClick)
                Spacer(modifier = Modifier.height(14.dp))
                MenuButton(text = "RECORD", onClick = onScoreClick)
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFFD700))
                        .border(3.dp, Color.White, RoundedCornerShape(20.dp))
                        .clickable { onAchievementsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LOGROS",
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A237E),
                            letterSpacing = 2.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ✅ SOLO BOTÓN DE CONFIGURACIÓN CENTRADO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleIconButton(
                        icon = Icons.Default.Settings,
                        onClick = onSettingsClick,
                        color = Color.White.copy(alpha = 0.2f)
                    )
                }
            }

            Text(
                text = "v1.0 - OrbBlaze",
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                fontSize = 12.sp
            )
        }

        // ✅ DIÁLOGO DE SALIDA PERSONALIZADO (Se activa con el botón atrás del móvil)
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                confirmButton = {
                    Button(
                        onClick = onExitClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("SALIR", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("CANCELAR", color = Color(0xFF1A237E), fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Text("¿CERRAR JUEGO?", fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                },
                text = {
                    Text("¿Estás seguro de que quieres salir de OrbBlaze?", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = Color.White
            )
        }
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(260.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .border(2.dp, Color.White, RoundedCornerShape(50))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, 
            style = TextStyle(
                fontSize = 17.sp, 
                fontWeight = FontWeight.Bold, 
                color = Color(0xFF1A237E), 
                letterSpacing = 1.2.sp
            )
        )
    }
}

@Composable
fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, Color.White, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}
