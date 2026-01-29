package com.example.orbblaze.ui.menu

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.ui.components.VisualBubble
import com.example.orbblaze.ui.theme.*
import kotlinx.coroutines.isActive
import kotlin.random.Random

// ✅ MODIFICADO: Agregamos vx (horizontal) y vy (vertical) para física real
data class PhysicsBubble(
    var x: Float,
    var y: Float,
    var vx: Float, // Velocidad horizontal (deriva)
    var vy: Float, // Velocidad vertical (impulso/gravedad)
    val radius: Float,
    val color: Color
)

@Composable
fun MenuScreen(
    onPlayClick: () -> Unit,
    onScoreClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit
) {
    // --- ANIMACIONES TÍTULO ---
    val infiniteTransition = rememberInfiniteTransition(label = "menu_animations")
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_scale"
    )

    // --- LÓGICA DE FÍSICA (FUENTE) ---
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val bubbleColors = listOf(BubbleRed, BubbleBlue, BubbleGreen, BubbleYellow, BubblePurple, BubbleCyan)

    // Creamos burbujas iniciales
    val physicsBubbles = remember {
        List(18) { // 18 burbujas para no saturar
            PhysicsBubble(
                x = Random.nextFloat() * screenWidthPx,
                // Inicializamos en posiciones aleatorias para que no se vea vacío al inicio
                y = Random.nextFloat() * screenHeightPx,
                vx = Random.nextFloat() * 2f - 1f, // Deriva lateral suave (-1 a 1)
                vy = Random.nextFloat() * -15f - 5f, // Velocidad hacia arriba inicial variada
                radius = with(density) { Random.nextInt(12, 22).dp.toPx() },
                color = bubbleColors.random()
            )
        }
    }

    // Bucle de física (Gravedad y Rebote)
    LaunchedEffect(Unit) {
        val gravity = 0.4f // Fuerza de gravedad

        while (isActive) {
            withFrameNanos { _ ->
                physicsBubbles.forEach { bubble ->
                    // 1. Aplicar velocidad a posición
                    bubble.x += bubble.vx
                    bubble.y += bubble.vy

                    // 2. Aplicar gravedad a la velocidad vertical
                    bubble.vy += gravity

                    // 3. REINICIO (Cuando cae por debajo de la pantalla)
                    if (bubble.y > screenHeightPx + bubble.radius * 2) {
                        // La lanzamos de nuevo desde ABAJO
                        bubble.y = screenHeightPx + bubble.radius
                        bubble.x = Random.nextFloat() * screenWidthPx
                        // ¡IMPULSO HACIA ARRIBA! (Negativo es arriba)
                        bubble.vy = -(Random.nextFloat() * 15f + 15f) // Entre -15 y -30 de fuerza
                        bubble.vx = Random.nextFloat() * 4f - 2f // Un poco más de deriva lateral al salir
                    }

                    // Rebote lateral simple (opcional, para que no se vayan de lado)
                    if (bubble.x < -bubble.radius) bubble.x = screenWidthPx + bubble.radius
                    if (bubble.x > screenWidthPx + bubble.radius) bubble.x = -bubble.radius
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BgTop, BgBottom)))
            .systemBarsPadding()
    ) {
        // --- CAPA 1: BURBUJAS FÍSICAS (CANVAS) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            physicsBubbles.forEach { bubble ->
                val center = Offset(bubble.x, bubble.y)
                val radius = bubble.radius

                // DIBUJO ESTILO GEMA
                val goldDark = Color(0xFFB8860B)
                val goldLight = Color(0xFFFFD700)
                val gemBase = bubble.color
                val gemDark = bubble.color.copy(red = bubble.color.red * 0.5f, green = bubble.color.green * 0.5f, blue = bubble.color.blue * 0.5f)
                val gemHighlight = Color.White.copy(alpha = 0.8f)

                drawCircle(
                    brush = Brush.sweepGradient(listOf(goldDark, goldLight, goldDark, goldLight, goldDark), center = center),
                    radius = radius + 2f, center = center, style = Stroke(width = 3f)
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(gemBase, gemDark), center = center.copy(y = center.y - radius * 0.2f), radius = radius * 0.9f),
                    radius = radius * 0.85f, center = center
                )
                drawOval(color = gemHighlight, topLeft = Offset(center.x - radius * 0.5f, center.y - radius * 0.7f), size = Size(radius * 0.6f, radius * 0.4f))
            }
        }

        // --- CAPA 2: DECORACIÓN ESTÁTICA FLOTANTE (Fondo lejano) ---
        val floatOffset1 by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 20f,
            animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
            label = "f1"
        )

        VisualBubble(color = BubbleBlue, modifier = Modifier.align(Alignment.TopStart).offset(x = (-20).dp, y = 100.dp).graphicsLayer { translationY = floatOffset1 })
        VisualBubble(color = BubbleRed, modifier = Modifier.align(Alignment.TopEnd).offset(x = 20.dp, y = 150.dp).graphicsLayer { translationY = -floatOffset1 })

        // --- CAPA 3: UI DEL MENÚ ---
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ORBBLAZE",
                style = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 4.sp, shadow = Shadow(color = Color.Black, offset = Offset(4f, 4f), blurRadius = 8f)),
                modifier = Modifier.graphicsLayer { scaleX = titleScale; scaleY = titleScale }
            )

            Spacer(modifier = Modifier.height(60.dp))

            MenuButton(text = "JUGAR", onClick = onPlayClick)
            Spacer(modifier = Modifier.height(16.dp))
            MenuButton(text = "MI PUNTUACIÓN", onClick = onScoreClick)
            Spacer(modifier = Modifier.height(16.dp))
            MenuButton(text = "CONFIGURACIÓN", onClick = onSettingsClick)
            Spacer(modifier = Modifier.height(16.dp))
            MenuButton(text = "SALIR", onClick = onExitClick, isSecondary = true)
        }

        Text(text = "v1.0", color = Color.White.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit, isSecondary: Boolean = false) {
    val backgroundColor = if (isSecondary) Color.Transparent else Color.White
    val contentColor = if (isSecondary) Color.White else Color(0xFF1A237E)
    val borderColor = Color.White

    Box(
        modifier = Modifier
            .width(260.dp).height(55.dp)
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(50))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = contentColor, letterSpacing = 1.5.sp))
    }
}