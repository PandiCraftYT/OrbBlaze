package com.example.orbblaze.ui.menu

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.ui.components.VisualBubble
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.theme.*
import kotlinx.coroutines.isActive
import kotlin.math.hypot
import kotlin.random.Random

// Clase de datos para las burbujas físicas del fondo
data class PhysicsBubble(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
    val color: Color
)

@Composable
fun MenuScreen(
    onPlayClick: () -> Unit,
    onScoreClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit,
    soundManager: SoundManager,
    onSecretClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "menu_animations")
    
    // Animación de escala para el título
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
        
        // Estado para forzar recomposición en cada frame de animación
        var frameTick by remember { mutableStateOf(0L) }

        val bubbleColors = listOf(BubbleRed, BubbleBlue, BubbleGreen, BubbleYellow, BubblePurple, BubbleCyan)

        // Inicializar burbujas físicas
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

        // Bucle de física
        LaunchedEffect(screenWidthPx, screenHeightPx) {
            val gravity = 0.25f
            while (isActive) {
                withFrameNanos { frameTime ->
                    frameTick = frameTime // Actualizamos el tick para redibujar el Canvas
                    physicsBubbles.forEach { bubble ->
                        bubble.x += bubble.vx
                        bubble.y += bubble.vy
                        bubble.vy += gravity

                        // Rebotar o reaparecer
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

        // Capa de Interacción y Dibujo
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
                                // Impulso hacia arriba al tocar
                                bubble.vy = -20f
                                bubble.vx = (Random.nextFloat() * 10f - 5f)
                            }
                        }
                    }
                }
        ) {
            // Dibujamos las burbujas en el Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                // El uso de frameTick aquí suscribe al Canvas a los cambios de cada frame
                @Suppress("UNUSED_VARIABLE")
                val t = frameTick 
                
                physicsBubbles.forEach { bubble ->
                    val center = Offset(bubble.x, bubble.y)
                    val radius = bubble.radius

                    // Efecto visual de burbuja (Estilo gema/esfera)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(bubble.color.copy(alpha = 0.9f), bubble.color.copy(alpha = 0.4f)),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                    // Brillo superior
                    drawCircle(
                        color = Color.White.copy(alpha = 0.3f),
                        radius = radius * 0.3f,
                        center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f)
                    )
                    // Borde fino
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                }
            }

            // Contenido Principal (Título y Botones)
            Column(
                modifier = Modifier.fillMaxSize(),
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

                Spacer(modifier = Modifier.height(60.dp))

                MenuButton(text = "JUGAR", onClick = onPlayClick)
                Spacer(modifier = Modifier.height(16.dp))
                MenuButton(text = "PUNTUACIONES", onClick = onScoreClick)
                Spacer(modifier = Modifier.height(16.dp))
                MenuButton(text = "LOGROS", onClick = onAchievementsClick)
                Spacer(modifier = Modifier.height(16.dp))
                MenuButton(text = "CONFIGURACIÓN", onClick = onSettingsClick)
                Spacer(modifier = Modifier.height(16.dp))
                MenuButton(text = "SALIR", onClick = onExitClick, isSecondary = true)
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
    }
}

@Composable
fun MenuButton(
    text: String, 
    onClick: () -> Unit, 
    isSecondary: Boolean = false
) {
    val backgroundColor = if (isSecondary) Color.Transparent else Color.White
    val contentColor = if (isSecondary) Color.White else Color(0xFF1A237E)
    val borderColor = Color.White

    Box(
        modifier = Modifier
            .width(260.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(50))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, 
            style = TextStyle(
                fontSize = 18.sp, 
                fontWeight = FontWeight.Bold, 
                color = contentColor, 
                letterSpacing = 1.5.sp
            )
        )
    }
}
